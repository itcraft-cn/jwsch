/**
 * Jwsch JS Subscriber Benchmark
 * 
 * 测试 JavaScript WebSocket 客户端接收性能
 */

const WebSocket = require('ws');

const WS_URL = process.env.WS_URL || 'ws://127.0.0.1:8080/ws';
const TOPIC = process.env.TOPIC || '/topic/bench';
const REPORT_INTERVAL = parseInt(process.env.REPORT_INTERVAL) || 1000;

let totalCount = 0;
let lastCount = 0;
let lastTime = Date.now();
let minLatency = Infinity;
let maxLatency = 0;
let totalLatency = 0;
let latencyCount = 0;

const ws = new WebSocket(WS_URL);
ws.binaryType = 'arraybuffer';

ws.on('open', () => {
  console.log(`[${new Date().toISOString()}] Connected to ${WS_URL}`);
  
  setTimeout(() => {
    subscribe(TOPIC);
    console.log(`[${new Date().toISOString()}] Subscribed to ${TOPIC}`);
    console.log('');
    console.log('Time\t\t\tCount\t\tTPS\t\tAvgLat(us)\tMinLat(us)\tMaxLat(us)');
    console.log('─'.repeat(100));
  }, 100);
});

ws.on('message', (data, isBinary) => {
  if (!isBinary) return;
  
  totalCount++;
  
  const view = new DataView(data);
  if (data.byteLength < 27) return;
  
  const magic0 = view.getUint8(0);
  const magic1 = view.getUint8(1);
  if (magic0 !== 0xe7 || magic1 !== 0x34) return;
  
  const command = view.getUint8(8);
  
  if (command === 0x03 || command === 0x04) {
    const sourceId = view.getBigInt64(11, false);
    const sendTime = Number(sourceId);
    const now = Date.now();
    
    if (sendTime > 1600000000000 && sendTime < now) {
      const latency = now - sendTime;
      totalLatency += latency;
      latencyCount++;
      if (latency < minLatency) minLatency = latency;
      if (latency > maxLatency) maxLatency = latency;
    }
  }
});

ws.on('error', (err) => {
  console.error(`[${new Date().toISOString()}] Error:`, err.message);
});

ws.on('close', (code, reason) => {
  console.log(`[${new Date().toISOString()}] Connection closed: code=${code}`);
  printSummary();
  process.exit(0);
});

function subscribe(topic) {
  const MAGIC = [0xe7, 0x34];
  const topicBytes = Buffer.from(topic, 'ascii');
  const headerLength = 27 + topicBytes.length;
  
  const buffer = Buffer.alloc(headerLength);
  let offset = 0;
  
  buffer[offset++] = MAGIC[0];
  buffer[offset++] = MAGIC[1];
  buffer.writeUInt16BE(headerLength, offset); offset += 2;
  buffer.writeUInt32BE(0, offset); offset += 4;
  buffer[offset++] = 0x05;
  buffer.writeUInt16BE(0, offset); offset += 2;
  buffer.writeBigUInt64BE(0n, offset); offset += 8;
  buffer.writeBigUInt64BE(0n, offset); offset += 8;
  topicBytes.copy(buffer, offset);
  
  ws.send(buffer);
}

function report() {
  const now = Date.now();
  const elapsed = (now - lastTime) / 1000;
  const count = totalCount - lastCount;
  const tps = count / elapsed;
  
  const avgLat = latencyCount > 0 ? (totalLatency / latencyCount * 1000).toFixed(0) : '-';
  const minLat = minLatency < Infinity ? (minLatency * 1000).toFixed(0) : '-';
  const maxLat = maxLatency > 0 ? (maxLatency * 1000).toFixed(0) : '-';
  
  const time = new Date().toISOString().substring(11, 19);
  console.log(`${time}\t\t${totalCount}\t\t${tps.toFixed(0)}\t\t${avgLat}\t\t${minLat}\t\t${maxLat}`);
  
  lastCount = totalCount;
  lastTime = now;
}

function printSummary() {
  console.log('');
  console.log('='.repeat(100));
  console.log(`Total messages received: ${totalCount}`);
  if (latencyCount > 0) {
    console.log(`Average latency: ${(totalLatency / latencyCount).toFixed(2)} ms`);
    console.log(`Min latency: ${minLatency} ms`);
    console.log(`Max latency: ${maxLatency} ms`);
  }
  console.log('='.repeat(100));
}

const reportTimer = setInterval(report, REPORT_INTERVAL);

process.on('SIGINT', () => {
  console.log('\nReceived SIGINT, shutting down...');
  clearInterval(reportTimer);
  ws.close();
});

process.on('SIGTERM', () => {
  clearInterval(reportTimer);
  ws.close();
});