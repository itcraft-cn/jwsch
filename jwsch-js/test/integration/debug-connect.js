const { JwschClient } = require('../../lib/jwsch.cjs.js');

const WS_URL = 'ws://localhost:8080/ws';

async function main() {
  console.log('=== Debug CONNECT_RESPONSE ===');
  
  const client = new JwschClient({ url: WS_URL, debug: true });
  
  client.on('connected', (connectionId) => {
    console.log(`Event: connected, connectionId=${connectionId}`);
  });
  
  console.log('Connecting...');
  await client.connect();
  console.log(`Connected: isConnected=${client.isConnected}, connectionId=${client.connectionId}`);
  
  // Wait for CONNECT_RESPONSE
  await new Promise(resolve => setTimeout(resolve, 1000));
  console.log(`After 1s: connectionId=${client.connectionId}`);
  
  client.disconnect();
  console.log('Done');
  process.exit(0);
}

main().catch(err => {
  console.error('Error:', err);
  process.exit(1);
});
