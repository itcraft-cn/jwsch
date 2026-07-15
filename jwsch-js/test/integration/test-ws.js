const WebSocket = require('ws');

const WS_URL = 'ws://127.0.0.1:8080/ws';

console.log('=== Testing raw WebSocket ===');
console.log('Connecting to:', WS_URL);

const ws = new WebSocket(WS_URL);

ws.on('open', () => {
  console.log('WebSocket opened');
});

ws.on('message', (data, isBinary) => {
  console.log(`Received message: ${data.length} bytes, isBinary=${isBinary}`);
  if (isBinary) {
    console.log('Raw bytes:', [...data.slice(0, 30)].map(b => b.toString(16).padStart(2, '0')).join(' '));
  } else {
    console.log('Text:', data.toString());
  }
});

ws.on('error', (err) => {
  console.error('WebSocket error:', err.message);
});

ws.on('close', (code, reason) => {
  console.log(`WebSocket closed: code=${code}, reason=${reason}`);
});

setTimeout(() => {
  console.log('Timeout after 5s');
  ws.close();
  process.exit(0);
}, 5000);