#!/usr/bin/env node
'use strict';

/**
 * Jwsch Node.js 订阅端压测工具。
 *
 * <p>通过 WebSocket 连接 jwsch 服务端，订阅指定 topic，统计：
 * <ul>
 *   <li>吞吐：接收消息总数与每秒速率（msg/s）</li>
 *   <li>延时：基于 NTP 墙钟的端到端单向延时（发送端写入 epoch 毫秒，本端用 Date.now() 相减）</li>
 * </ul>
 *
 * <p>消息体约定（由 Java LatencyPublisher 的 wall 时钟模式生成）：
 * <pre>
 * | SendTimeMillis(8B, big-endian) | Sequence(8B, big-endian) | Payload(NB) |
 * </pre>
 *
 * <p>使用示例：
 * <pre>
 * node bench-subscriber.js --wsUrl ws://192.168.77.102:8080/ws \
 *   --topic /topic/latency --clients 1 --duration 30 --report 5 --warmup 2
 * </pre>
 *
 * <p>依赖：需在 jwsch-js 目录执行 `npm install ws`（Node 环境），
 * 或在仓库已构建的 node_modules 中提供 ws。
 *
 * <p>注意：跨机单向延时依赖三台主机时钟经 NTP 同步，测量结果包含时钟偏差，
 * 通常以「min」作为偏差下界参考。
 */

const { JwschClient } = require('../lib/jwsch.cjs.js');

// 消息体前 16 字节为时间戳与序列号。
const BODY_HEADER_BYTES = 16;

// 协议命令类型。
const CMD_PUSH = 3;
const CMD_BROADCAST = 4;

/**
 * 解析命令行参数。
 *
 * @param {string[]} argv 进程参数（不含 node 与脚本名）
 * @returns {object} 解析后的配置对象
 */
function parseArgs(argv) {
    const opts = {
        wsUrl: 'ws://127.0.0.1:8080/ws',
        topic: '/topic/latency',
        clients: 1,
        duration: 30,
        report: 5,
        warmup: 2,
        label: 'dist-test',
        reservoir: 200000,
        reconnect: false
    };

    for (let i = 0; i < argv.length; i++) {
        const arg = argv[i];
        if (arg.indexOf('--') !== 0) {
            continue;
        }
        let key = arg.slice(2);
        let value;
        const eq = key.indexOf('=');
        if (eq >= 0) {
            value = key.slice(eq + 1);
            key = key.slice(0, eq);
        } else {
            value = argv[++i];
        }

        switch (key) {
            case 'wsUrl':
                opts.wsUrl = value;
                break;
            case 'topic':
                opts.topic = value;
                break;
            case 'clients':
                opts.clients = parseInt(value, 10);
                break;
            case 'duration':
                opts.duration = parseInt(value, 10);
                break;
            case 'report':
                opts.report = parseInt(value, 10);
                break;
            case 'warmup':
                opts.warmup = parseInt(value, 10);
                break;
            case 'label':
                opts.label = value;
                break;
            case 'reservoir':
                opts.reservoir = parseInt(value, 10);
                break;
            case 'reconnect':
                opts.reconnect = value === 'true';
                break;
            case 'help':
            case 'h':
                printHelp();
                process.exit(0);
                break;
            default:
                console.error('[WARN] Unknown option: --' + key);
        }
    }
    return opts;
}

/**
 * 打印帮助信息。
 */
function printHelp() {
    console.log('Usage: node bench-subscriber.js [options]');
    console.log('');
    console.log('Options:');
    console.log('  --wsUrl <url>        WebSocket URL (default: ws://127.0.0.1:8080/ws)');
    console.log('  --topic <topic>      Subscribe topic (default: /topic/latency)');
    console.log('  --clients <n>        Number of concurrent clients (default: 1)');
    console.log('  --duration <sec>     Measurement duration in seconds (default: 30)');
    console.log('  --report <sec>       Interval report period in seconds (default: 5)');
    console.log('  --warmup <sec>       Latency warmup to skip in seconds (default: 2)');
    console.log('  --label <name>       Result label (default: dist-test)');
    console.log('  --reservoir <n>      Max latency samples kept for percentile (default: 200000)');
    console.log('  --reconnect <bool>   Enable auto reconnect (default: false)');
    console.log('  --help, -h           Show this help');
}

/**
 * 延时统计器。
 *
 * <p>精确维护 count/sum/min/max；百分位采用蓄水池抽样，避免高吞吐下样本数组无限增长。
 */
class LatencyStats {
    constructor(reservoirSize) {
        this.count = 0;
        this.sum = 0;
        this.min = Infinity;
        this.max = -Infinity;
        this.negative = 0;
        this.seen = 0;
        this.reservoirSize = reservoirSize;
        this.samples = [];
    }

    record(latencyMs) {
        this.count++;
        this.sum += latencyMs;
        if (latencyMs < this.min) {
            this.min = latencyMs;
        }
        if (latencyMs > this.max) {
            this.max = latencyMs;
        }
        if (latencyMs < 0) {
            this.negative++;
        }
        this.seen++;
        if (this.samples.length < this.reservoirSize) {
            this.samples.push(latencyMs);
        } else {
            const r = Math.floor(Math.random() * this.seen);
            if (r < this.reservoirSize) {
                this.samples[r] = latencyMs;
            }
        }
    }

    percentile(sorted, p) {
        if (sorted.length === 0) {
            return 0;
        }
        let idx = Math.ceil(p / 100.0 * sorted.length) - 1;
        idx = Math.max(0, Math.min(idx, sorted.length - 1));
        return sorted[idx];
    }

    summary() {
        if (this.count === 0) {
            return null;
        }
        const sorted = this.samples.slice().sort((a, b) => a - b);
        return {
            count: this.count,
            avg: this.sum / this.count,
            min: this.min,
            max: this.max,
            p50: this.percentile(sorted, 50),
            p90: this.percentile(sorted, 90),
            p95: this.percentile(sorted, 95),
            p99: this.percentile(sorted, 99),
            negative: this.negative
        };
    }
}

/**
 * 格式化毫秒数值，保留 3 位小数。
 */
function ms(v) {
    return Number(v).toFixed(3);
}

/**
 * 主流程。
 */
async function main() {
    const opts = parseArgs(process.argv.slice(2));
    const stats = new LatencyStats(opts.reservoir);

    console.log('=== Jwsch Node Subscriber Benchmark ===');
    console.log('WebSocket URL: ' + opts.wsUrl);
    console.log('Topic: ' + opts.topic);
    console.log('Clients: ' + opts.clients);
    console.log('Duration: ' + opts.duration + 's');
    console.log('Report Interval: ' + opts.report + 's');
    console.log('Warmup: ' + opts.warmup + 's');
    console.log('Label: ' + opts.label);

    let received = 0;
    let latencyActive = false;
    let firstRecvNanos = 0n;
    let lastRecvNanos = 0n;

    const onMessage = (packet) => {
        if (packet.command !== CMD_PUSH && packet.command !== CMD_BROADCAST) {
            return;
        }
        const body = packet.body;
        if (!body || body.byteLength < BODY_HEADER_BYTES) {
            return;
        }
        const nowNanos = process.hrtime.bigint();
        if (received === 0) {
            firstRecvNanos = nowNanos;
        }
        lastRecvNanos = nowNanos;
        received++;
        if (latencyActive) {
            const dv = new DataView(body);
            const sendMs = Number(dv.getBigInt64(0, false));
            stats.record(Date.now() - sendMs);
        }
    };

    const clients = [];
    for (let i = 0; i < opts.clients; i++) {
        const client = new JwschClient({ url: opts.wsUrl, reconnect: opts.reconnect });
        await client.connect();
        await client.subscribe(opts.topic, onMessage);
        clients.push(client);
    }

    console.log('SUBSCRIBER_READY');

    const startNanos = process.hrtime.bigint();
    const elapsedSec = () => Number(process.hrtime.bigint() - startNanos) / 1e9;

    // 预热结束后开始记录延时，避免连接建立与 JIT 抖动污染样本。
    const warmupTimer = setTimeout(() => {
        latencyActive = true;
    }, Math.max(0, opts.warmup) * 1000);

    let lastReceived = 0;
    let lastMark = 0;
    const reportTimer = setInterval(() => {
        const now = elapsedSec();
        const rate = (received - lastReceived) / Math.max(1e-9, now - lastMark);
        const info = stats.summary();
        const lat = info ? ('avg=' + ms(info.avg) + 'ms p99=' + ms(info.p99) + 'ms') : 'lat=warming';
        console.log('[REPORT] t=' + now.toFixed(1) + 's recv=' + received +
            ' rate=' + rate.toFixed(0) + ' msg/s ' + lat);
        lastReceived = received;
        lastMark = now;
    }, Math.max(1, opts.report) * 1000);

    // 定时结束，打印最终结果。
    const done = new Promise((resolve) => {
        setTimeout(resolve, Math.max(1, opts.duration) * 1000);
    });
    await done;

    clearInterval(reportTimer);
    clearTimeout(warmupTimer);

    const totalSec = elapsedSec();
    const throughput = received / Math.max(1e-9, totalSec);

    for (const client of clients) {
        try {
            client.disconnect();
        } catch (e) {
            // 忽略关闭异常
        }
    }

    // 等待少量时间让最后一批回调处理完成。
    await new Promise((resolve) => setTimeout(resolve, 200));

    console.log('');
    console.log('=== Node Subscriber Result ===');
    console.log('label: ' + opts.label);
    console.log('clients: ' + opts.clients);
    console.log('duration_s: ' + totalSec.toFixed(3));
    console.log('received: ' + received);
    console.log('throughput_msg_s: ' + throughput.toFixed(1));
    // 活跃窗口吞吐：以首末消息到达时刻为区间，排除发布端启动前的空档。
    if (received > 1) {
        const activeSec = Number(lastRecvNanos - firstRecvNanos) / 1e9;
        const activeThroughput = (received - 1) / Math.max(1e-9, activeSec);
        console.log('active_window_s: ' + activeSec.toFixed(3));
        console.log('active_throughput_msg_s: ' + activeThroughput.toFixed(1));
    }
    const info = stats.summary();
    if (info) {
        console.log('latency_samples: ' + info.count);
        console.log('lat_avg_ms: ' + ms(info.avg));
        console.log('lat_min_ms: ' + ms(info.min));
        console.log('lat_p50_ms: ' + ms(info.p50));
        console.log('lat_p90_ms: ' + ms(info.p90));
        console.log('lat_p95_ms: ' + ms(info.p95));
        console.log('lat_p99_ms: ' + ms(info.p99));
        console.log('lat_max_ms: ' + ms(info.max));
        console.log('lat_negative: ' + info.negative);
    } else {
        console.log('latency_samples: 0');
    }
    console.log('RESULT_DONE');

    process.exit(0);
}

main().catch((err) => {
    console.error('[FATAL] ' + (err && err.stack ? err.stack : err));
    process.exit(1);
});
