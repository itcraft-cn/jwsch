const WebSocket = require('ws');

const WS_URL = 'ws://127.0.0.1:8080/ws';

console.log('=== Detailed WebSocket Debug ===');
console.log('ws library version:', require('ws/package.json').version);

const ws = new WebSocket(WS_URL);

console.log('Setting binaryType...');
ws.binaryType = 'arraybuffer';
console.log('binaryType:', ws.binaryType);

ws.on('open', () => {
  console.log('[open] Connection established');
  console.log('  readyState:', ws.readyState);
});

ws.on('message', (data, isBinary) => {
  console.log('[message] Received:', data.length, 'bytes, isBinary=', isBinary);
  console.log('  data type:', data.constructor.name);
  if (data instanceof ArrayBuffer) {
    console.log('  ArrayBuffer bytes:', [...new Uint8Array(data)].slice(0, 10));
  } else if (Buffer.isBuffer(data)) {
    console.log('  Buffer bytes:', [...data.slice(0, 10)]);
  }
});

ws.on('error', (err) => {
  console.log('[error]', err.message);
});

ws.on('close', (code, reason) => {
  console.log('[close] code=', code, 'reason=', reason.toString());
});

ws.on('unexpected-response', (req, res) => {
  console.log('[unexpected-response]', res.statusCode, res.statusMessage);
});

ws.on('ping', (data) => {
  console.log('[ping]', data);
});

ws.on('pong', (data) => {
  console.log('[pong]', data);
});

ws.on('upgrade', (res) => {
  console.log('[upgrade] HTTP', res.statusCode);
  console.log('  headers:', JSON.stringify(res.headers, null, 2));
});

ws.on('connectionError', (err) => {
  console.log('[connectionError]', err.message);
});

setTimeout(() => {
  console.log('\n=== Timeout after 5s ===');
  console.log('readyState:', ws.readyState);
  console.log('bufferedAmount:', ws.bufferedAmount);
  ws.close();
  process.exit(0);
}, 5000);