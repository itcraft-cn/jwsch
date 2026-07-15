#!/bin/bash
set -e

cd /disk2/helly_data/code/java/jwsch

echo "============================================"
echo "  EURUSD Price Demo System"
echo "============================================"
echo ""

# Function to cleanup on exit
cleanup() {
    echo ""
    echo "[DEMO] Stopping all processes..."
    kill $SERVER_PID $HTTP_PID $PUB_PID 2>/dev/null || true
    exit 0
}
trap cleanup INT TERM

# 1. Start jwsch server
echo "[DEMO] Starting jwsch server..."
java -jar jwsch-sample/sample-server/target/sample-server-1.0.0-SNAPSHOT.jar > /tmp/demo-server.log 2>&1 &
SERVER_PID=$!
sleep 3

if ! grep -q "started successfully" /tmp/demo-server.log; then
    echo "[DEMO] ERROR: Server failed to start"
    cat /tmp/demo-server.log
    exit 1
fi
echo "[DEMO] Server started (PID: $SERVER_PID)"

# 2. Start HTTP server
echo "[DEMO] Starting HTTP server..."
cd jwsch-demo
node server.js > /tmp/demo-http.log 2>&1 &
HTTP_PID=$!
cd ..
sleep 1
echo "[DEMO] HTTP server started (PID: $HTTP_PID)"

# 3. Start price publisher
echo "[DEMO] Starting price publisher..."
java -cp jwsch-bench/target/jwsch-bench-1.0.0-SNAPSHOT.jar \
    cn.itcraft.jwsch.bench.PricePublisherMain \
    --host localhost --port 9090 --interval 10 > /tmp/demo-pub.log 2>&1 &
PUB_PID=$!
sleep 2
echo "[DEMO] Price publisher started (PID: $PUB_PID)"

echo ""
echo "============================================"
echo "  Demo is running!"
echo "============================================"
echo "  Browser: http://localhost:8088"
echo "  WebSocket: ws://localhost:8080/ws"
echo "  Press Ctrl+C to stop"
echo "============================================"
echo ""

# Wait for processes
wait
