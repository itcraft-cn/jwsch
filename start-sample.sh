#!/bin/bash

echo "=== Jwsch Sample Startup Script ==="
echo ""

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "1. Building project..."
mvnd package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -Dskip.frontend=true -q

echo ""
echo "2. Starting sample-server (WebSocket:8080, TCP:9090)..."
java -jar "$BASE_DIR/jwsch-sample/sample-server/target/sample-server-1.0.0-SNAPSHOT.jar" > /tmp/jwsch-server.log 2>&1 &
SERVER_PID=$!
sleep 3

echo ""
echo "3. Starting sample-webapp (http://localhost:3000)..."
java -jar "$BASE_DIR/jwsch-sample/sample-webapp/target/sample-webapp-1.0.0-SNAPSHOT.jar" > /tmp/jwsch-webapp.log 2>&1 &
WEBAPP_PID=$!
sleep 3

echo ""
echo "4. Checking service health..."
for i in {1..10}; do
    if curl -s http://localhost:3000/api/health > /dev/null 2>&1; then
        echo "Services ready!"
        break
    fi
    echo "Waiting... ($i/10)"
    sleep 1
done

echo ""
echo "5. Opening auto-subscribe test page..."
echo "   URL: http://localhost:3000/test.html"
echo "   (This page will auto-connect WebSocket and subscribe to /topic/news)"

# Try to open browser
if command -v xdg-open > /dev/null; then
    xdg-open "http://localhost:3000/test.html" 2>/dev/null &
elif command -v open > /dev/null; then
    open "http://localhost:3000/test.html" 2>/dev/null &
fi

echo ""
echo "6. Waiting 3 seconds for WebSocket connection..."
sleep 3

echo ""
echo "7. Starting sample-pusher (topic: /topic/news, interval: 2000ms)..."
java -jar "$BASE_DIR/jwsch-sample/sample-pusher/target/sample-pusher-1.0.0-SNAPSHOT.jar" \
  --topic /topic/news --interval 2000 2>&1 | tee /tmp/jwsch-pusher.log &
PUSHER_PID=$!

echo ""
echo "=== All services started ==="
echo ""
echo "Server PID: $SERVER_PID (log: /tmp/jwsch-server.log)"
echo "Webapp PID: $WEBAPP_PID (log: /tmp/jwsch-webapp.log)"
echo "Pusher PID: $PUSHER_PID (log: /tmp/jwsch-pusher.log)"
echo ""
echo "Test page: http://localhost:3000/test.html"
echo "Main page: http://localhost:3000/"
echo ""
echo "The test page will:"
echo "  1. Auto-connect to WebSocket"
echo "  2. Auto-subscribe to /topic/news"
echo "  3. Display all received messages"
echo ""
echo "Press Ctrl+C to stop all services"

cleanup() {
    echo ""
    echo "Stopping services..."
    kill -9 $SERVER_PID $WEBAPP_PID $PUSHER_PID 2>/dev/null
    wait $SERVER_PID $WEBAPP_PID $PUSHER_PID 2>/dev/null
    echo "All services stopped"
}

trap cleanup EXIT

wait
