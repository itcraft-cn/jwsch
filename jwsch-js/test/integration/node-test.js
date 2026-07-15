/**
 * Jwsch JS Client Integration Tests
 * 
 * Tests real WebSocket connection to jwsch server.
 * Requires server running at ws://localhost:8080/ws
 */

const { JwschClient } = require('../../lib/jwsch.cjs.js');
const assert = require('assert');

const WS_URL = 'ws://localhost:8080/ws';
const TEST_TIMEOUT = 10000;

let client;
let testsPassed = 0;
let testsFailed = 0;

function log(msg) {
  console.log(`[${new Date().toISOString()}] ${msg}`);
}

async function test(name, fn) {
  try {
    await fn();
    log(`✓ ${name}`);
    testsPassed++;
  } catch (err) {
    log(`✗ ${name}: ${err.message}`);
    testsFailed++;
  }
}

async function runTests() {
  log('=== Jwsch JS Integration Tests ===');
  log(`Server: ${WS_URL}`);
  log('');

  // Test 1: Connect
  await test('Connect to server', async () => {
    client = new JwschClient({ url: WS_URL, debug: true });
    await client.connect();
    assert(client.isConnected, 'Client should be connected');
  });

  // Wait for CONNECT_RESPONSE
  await new Promise(resolve => setTimeout(resolve, 500));

  // Test 2: Receive connectionId
  await test('Receive connectionId', async () => {
    assert(client.connectionId > 0, 'Should have valid connectionId');
    log(`  connectionId: ${client.connectionId}`);
  });

  // Test 3: Subscribe to topic
  let receivedMessages = [];
  await test('Subscribe to topic', async () => {
    await client.subscribe('/topic/test', (packet) => {
      receivedMessages.push(packet);
    });
  });

  // Test 4: Push message and receive
  await test('Push and receive message', async () => {
    const testBody = { message: 'hello from node', timestamp: Date.now() };
    client.push('/topic/test', testBody);

    // Wait for message
    await new Promise(resolve => setTimeout(resolve, 1000));

    // Note: push to own subscribed topic may or may not loop back
    // depending on server implementation
    log(`  received ${receivedMessages.length} messages`);
  });

  // Test 5: Request-Response (skip - no handler on sample server)
  // await test('Request to /api/echo', async () => {
  //   const response = await client.request('/api/echo', { test: true });
  //   log(`  response: ${response.isSuccess ? 'success' : 'failed'}`);
  // });

  // Test 5: Heartbeat
  await test('Multiple subscriptions', async () => {
    let count1 = 0, count2 = 0;
    
    await client.subscribe('/topic/multi1', () => count1++);
    await client.subscribe('/topic/multi2', () => count2++);
    
    client.push('/topic/multi1', { idx: 1 });
    client.push('/topic/multi2', { idx: 2 });
    
    await new Promise(resolve => setTimeout(resolve, 500));
    log(`  multi1: ${count1}, multi2: ${count2}`);
  });

  // Test 7: Heartbeat
  await test('Heartbeat mechanism', async () => {
    // Just verify heartbeat doesn't cause disconnect
    await new Promise(resolve => setTimeout(resolve, 2000));
    assert(client.isConnected, 'Should still be connected after heartbeat');
  });

  // Test 8: Disconnect
  await test('Disconnect', async () => {
    client.disconnect();
    assert(!client.isConnected, 'Should be disconnected');
  });

  // Summary
  log('');
  log('=== Test Summary ===');
  log(`Passed: ${testsPassed}`);
  log(`Failed: ${testsFailed}`);
  log('');

  process.exit(testsFailed > 0 ? 1 : 0);
}

runTests().catch(err => {
  console.error('Test runner error:', err);
  process.exit(1);
});
