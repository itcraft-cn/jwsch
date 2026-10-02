#!/bin/bash
#
# 三机分布式压测编排脚本。
#
# 拓扑：
#   vboxdeb001 (172.22.133.251)  发布端 -> Java LatencyPublisherMain (TCP PUSH, --clock wall)
#   vboxdeb002 (172.22.133.252)  服务端 -> jwsch-bench server (WebSocket + TCP)
#   vboxdeb003 (172.22.133.253)  订阅端 -> Node.js bench-subscriber.js (WebSocket)
#
# 测试内容：逐档加压，统计 Node 订阅端的吞吐（msg/s）与端到端单向延时（ms）。
#
# 前置条件：
#   1. 三台主机已安装 JDK8（~/lang/dragonwell-8.29.28）与 Node.js
#   2. 101/102 已上传 ~/jwsch/bench.jar，103 已上传 ~/jwsch/jwsch-js
#   3. 本机可免密 ssh 到三台主机
#
# 用法：./run-distributed-test.sh [scenario ...]
#   默认依次执行 low mid high max 四档；也可指定任意子集。
#
set -u

SSH_OPTS="-o BatchMode=yes -o ConnectTimeout=8"
PUB_HOST=${PUB_HOST:-vboxdeb001}
SRV_HOST=${SRV_HOST:-vboxdeb002}
SUB_HOST=${SUB_HOST:-vboxdeb003}

SRV_IP=${SRV_IP:-172.22.133.252}
WS_PORT=${WS_PORT:-8080}
TCP_PORT=${TCP_PORT:-9090}
WORKERS=${WORKERS:-8}
TOPIC=${TOPIC:-/topic/latency}
# 压测流控：默认关闭入站限流（否则恒为 10K/s），并放大出站队列避免订阅端被过早断连。
INBOUND_RATE_LIMIT=${INBOUND_RATE_LIMIT:-0}
OUTBOUND_QUEUE=${OUTBOUND_QUEUE:-65536}
OUTBOUND_DISCONNECT=${OUTBOUND_DISCONNECT:-262144}

# 远端绝对路径（避免依赖远端 $HOME 展开，简化引号处理）。
RH=/home/vboxuser
JAVA_BIN=$RH/lang/dragonwell-8.29.28/bin/java
JAR=$RH/jwsch/bench.jar
LOGDIR=$RH/jwsch/logs
NODE_BIN=/usr/bin/node
SUB_SCRIPT=$RH/jwsch/jwsch-js/bench/bench-subscriber.js

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
RESULT_DIR="$SCRIPT_DIR/results/$(date +%Y%m%d-%H%M%S)"
mkdir -p "$RESULT_DIR"

ALL_SCENARIOS="low mid h50 h100 h200 over"

scenario_params() {
    case "$1" in
        low)  echo "1000 64 30 1" ;;
        mid)  echo "100 64 30 1" ;;
        h50)  echo "20 64 30 1" ;;
        h100) echo "10 64 30 1" ;;
        h200) echo "5 64 30 1" ;;
        over) echo "1 100 30 1" ;;
        *)    return 1 ;;
    esac
}

scenario_desc() {
    local p
    p=$(scenario_params "$1") || return 1
    set -- $p
    echo "interval=$1us payload=$2B duration=$3s clients=$4"
}

run_remote() {
    local host="$1"
    shift
    ssh $SSH_OPTS "vboxuser@$host" "$@"
}

# 轮询远端日志直到出现指定标记（或超时）。
wait_marker() {
    local host="$1" log="$2" marker="$3" timeout="${4:-30}"
    local i
    for ((i = 0; i < timeout * 5; i++)); do
        if run_remote "$host" "grep -q '$marker' '$log' 2>/dev/null"; then
            return 0
        fi
        sleep 0.2
    done
    return 1
}

# 抓取远端日志到本地结果目录。
fetch_log() {
    local host="$1" remote="$2" local_file="$3"
    run_remote "$host" "cat '$remote' 2>/dev/null" > "$local_file"
}

start_server() {
    echo "[ORCH] 启动服务端 $SRV_HOST ..."
    run_remote "$SRV_HOST" "pkill -f '[b]ench.jar server' 2>/dev/null; true"
    sleep 1
    run_remote "$SRV_HOST" "setsid nohup $JAVA_BIN -Dio.netty.leakDetection.level=disabled -Djava.net.preferIPv4Stack=true -Xmx1g \
        -jar $JAR server --wsPort $WS_PORT --tcpPort $TCP_PORT --workers $WORKERS \
        --inboundRateLimit $INBOUND_RATE_LIMIT --outboundQueue $OUTBOUND_QUEUE --outboundDisconnect $OUTBOUND_DISCONNECT \
        > $LOGDIR/server.log 2>&1 < /dev/null & echo started"
    if wait_marker "$SRV_HOST" "$LOGDIR/server.log" SERVER_READY 30; then
        echo "[ORCH] 服务端就绪"
    else
        echo "[ORCH] 服务端启动失败"
        fetch_log "$SRV_HOST" "$LOGDIR/server.log" "$RESULT_DIR/server.log"
        exit 1
    fi
}

stop_server() {
    run_remote "$SRV_HOST" "pkill -f '[b]ench.jar server' 2>/dev/null; true"
}

start_subscriber() {
    local name="$1" duration="$2" clients="$3"
    echo "[ORCH] 启动 Node 订阅端 $SUB_HOST (label=$name, duration=${duration}s) ..."
    run_remote "$SUB_HOST" "pkill -f '[b]ench-subscriber.js' 2>/dev/null; true"
    sleep 1
    run_remote "$SUB_HOST" "setsid nohup $NODE_BIN $SUB_SCRIPT \
        --wsUrl ws://$SRV_IP:$WS_PORT/ws --topic $TOPIC \
        --clients $clients --duration $duration --report 5 --warmup 2 --label $name \
        > $LOGDIR/sub-$name.log 2>&1 < /dev/null & echo started"
    if wait_marker "$SUB_HOST" "$LOGDIR/sub-$name.log" SUBSCRIBER_READY 30; then
        echo "[ORCH] 订阅端就绪"
    else
        echo "[ORCH] 订阅端启动失败"
        fetch_log "$SUB_HOST" "$LOGDIR/sub-$name.log" "$RESULT_DIR/sub-$name.log"
        exit 1
    fi
}

start_publisher() {
    local name="$1" interval="$2" payload="$3"
    echo "[ORCH] 启动发布端 $PUB_HOST (interval=${interval}us, payload=${payload}B) ..."
    run_remote "$PUB_HOST" "pkill -f '[L]atencyPublisherMain' 2>/dev/null; true"
    sleep 1
    run_remote "$PUB_HOST" "setsid nohup $JAVA_BIN -Dio.netty.leakDetection.level=disabled -Djava.net.preferIPv4Stack=true -Xmx256m \
        -cp $JAR cn.itcraft.jwsch.bench.latency.LatencyPublisherMain \
        --host $SRV_IP --tcpPort $TCP_PORT --topic $TOPIC \
        --interval $interval --payloadSize $payload --clock wall --duration 0 \
        > $LOGDIR/pub-$name.log 2>&1 < /dev/null & echo started"
    if wait_marker "$PUB_HOST" "$LOGDIR/pub-$name.log" PUBLISHER_READY 30; then
        echo "[ORCH] 发布端就绪"
    else
        echo "[ORCH] 发布端启动失败"
        fetch_log "$PUB_HOST" "$LOGDIR/pub-$name.log" "$RESULT_DIR/pub-$name.log"
        exit 1
    fi
}

stop_publisher() {
    local name="$1"
    # 触发优雅停机，使发布端输出累计发送量。
    run_remote "$PUB_HOST" "pkill -f '[L]atencyPublisherMain' 2>/dev/null; true"
    sleep 2
    fetch_log "$PUB_HOST" "$LOGDIR/pub-$name.log" "$RESULT_DIR/pub-$name.log"
}

run_scenario() {
    local name="$1"
    read -r interval payload duration clients <<< "$(scenario_params "$name")"
    echo ""
    echo "======================================================"
    echo "[ORCH] 场景 $name: $(scenario_desc "$name")"
    echo "======================================================"

    start_subscriber "$name" "$duration" "$clients"
    start_publisher "$name" "$interval" "$payload"

    echo "[ORCH] 运行 ${duration}s ..."
    sleep $((duration + 3))

    stop_publisher "$name"

    if wait_marker "$SUB_HOST" "$LOGDIR/sub-$name.log" RESULT_DONE 30; then
        echo "[ORCH] 订阅端结束"
    else
        run_remote "$SUB_HOST" "pkill -f '[b]ench-subscriber.js' 2>/dev/null; true"
    fi
    fetch_log "$SUB_HOST" "$LOGDIR/sub-$name.log" "$RESULT_DIR/sub-$name.log"

    echo "[ORCH] 场景 $name 结果："
    grep -E 'throughput_msg_s|active_throughput_msg_s|lat_avg_ms|lat_p50_ms|lat_p90_ms|lat_p95_ms|lat_p99_ms|lat_max_ms|lat_min_ms|lat_negative|received:' "$RESULT_DIR/sub-$name.log" | sed 's/^/    /'
    grep -E 'Total sent' "$RESULT_DIR/pub-$name.log" | sed 's/^/    PUB /'
}

cleanup() {
    local name="${CURRENT_SCENARIO:-unknown}"
    run_remote "$PUB_HOST" "pkill -f '[L]atencyPublisherMain' 2>/dev/null; true"
    run_remote "$SUB_HOST" "pkill -f '[b]ench-subscriber.js' 2>/dev/null; true"
    stop_server
    echo "[ORCH] 清理完成"
}
trap cleanup EXIT

SCENARIOS="${*:-$ALL_SCENARIOS}"

echo "======================================================"
echo " jwsch 三机分布式压测"
echo " 发布端: $PUB_HOST   服务端: $SRV_HOST   订阅端: $SUB_HOST"
echo " 结果目录: $RESULT_DIR"
echo "======================================================"

# 确保远端日志目录存在，并清理可能残留的进程。
for host in "$PUB_HOST" "$SRV_HOST" "$SUB_HOST"; do
    run_remote "$host" "mkdir -p $LOGDIR"
done
# 强制校时：发布端与订阅端均以服务端(102)为 NTP 参考，测试前 step 一次消除累积漂移。
run_remote "$PUB_HOST" "sudo chronyc makestep >/dev/null 2>&1; true"
run_remote "$SUB_HOST" "sudo chronyc makestep >/dev/null 2>&1; true"
run_remote "$PUB_HOST" "pkill -f '[L]atencyPublisherMain' 2>/dev/null; true"
run_remote "$SRV_HOST" "pkill -f '[b]ench.jar server' 2>/dev/null; true"
run_remote "$SUB_HOST" "pkill -f '[b]ench-subscriber.js' 2>/dev/null; true"
sleep 1

start_server

for name in $SCENARIOS; do
    if ! scenario_params "$name" >/dev/null; then
        echo "[ORCH] 未知场景: $name (可选: $ALL_SCENARIOS)"
        continue
    fi
    CURRENT_SCENARIO="$name"
    run_scenario "$name"
done

fetch_log "$SRV_HOST" "$LOGDIR/server.log" "$RESULT_DIR/server.log"
echo ""
echo "[ORCH] 全部完成，结果保存在 $RESULT_DIR"
ls -la "$RESULT_DIR"
