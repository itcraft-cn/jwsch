const WebSocket = require('ws');
const { Decoder } = require('../../lib/jwsch.cjs.js');

const WS_URL = 'ws://localhost:8080/ws';

async function main() {
  console.log('=== Raw WebSocket Debug ===');
  
  const ws = new WebSocket(WS_URL);
  const decoder = new Decoder();
  
  ws.on('open', () => {
    console.log('WebSocket connected, waiting for CONNECT_RESPONSE...');
  });
  
  ws.on('message', (data) => {
    console.log(`Received ${data.length} bytes`);
    console.log('Raw bytes:', [...data.slice(0, 30)].map(b => b.toString(16).padStart(2, '0')).join(' '));
    
    decoder.feed(data);
    const packets = decoder.decode();
    
    for (const packet of packets) {
      console.log('Packet:', {
        command: packet.command,
        sourceId: packet.sourceId,
        targetId: packet.targetId,
        topic: packet.topic,
        hasBody: packet.hasBody()
      });
    }
  });
  
  ws.on('error', (err) => {
    console.error('WebSocket error:', err);
    process.exit(1);
  });
  
  // Wait 5 seconds for messages
  setTimeout(() => {
    console.log('Timeout - no message received');
    ws.close();
    process.exit(0);
  }, 5000);
}

main().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});
