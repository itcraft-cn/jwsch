#!/bin/bash

echo "=== Jwsch Debug Startup Script ==="
echo ""

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "1. Building project..."
mvnd package -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -Dskip.frontend=true -q

echo ""
echo "2. Cleaning old logs..."
rm -f /tmp/jwsch-*.log

echo ""
echo "3. Starting sample-server (WebSocket:8080, TCP:9090)..."
java -jar "$BASE_DIR/jwsch-sample/sample-server/target/sample-server-1.0.0-SNAPSHOT.jar" 2>&1 | tee /tmp/jwsch-server.log &
SERVER_PID=$!
echo "Server PID: $SERVER_PID"
sleep 3

echo ""
echo "4. Starting sample-webapp (http://localhost:3000)..."
java -jar "$BASE_DIR/jwsch-sample/sample-webapp/target/sample-webapp-1.0.0-SNAPSHOT.jar" 2>&1 | tee /tmp/jwsch-webapp.log &
WEBAPP_PID=$!
echo "Webapp PID: $WEBAPP_PID"
sleep 3

echo ""
echo "5. Services started. Logs are being written to:"
echo "   Server: /tmp/jwsch-server.log"
echo "   Webapp: /tmp/jwsch-webapp.log"
echo ""
echo "6. Opening test page in browser..."
if command -v xdg-open > /dev/null; then
    xdg-open "http://localhost:3000/test.html" 2>/dev/null &
elif command -v open > /dev/null; then
    open "http://localhost:3000/test.html" 2>/dev/null &
fi

echo ""
echo "7. Waiting 5 seconds for WebSocket to connect and subscribe..."
sleep 5

echo ""
echo "8. Starting sample-pusher..."
echo "   Watch for 'Pushed message' logs"
echo ""
java -jar "$BASE_DIR/jwsch-sample/sample-pusher/target/sample-pusher-1.0.0-SNAPSHOT.jar" \
  --topic /topic/news --interval 2000 2>&1 | tee /tmp/jwsch-pusher.log &
PUSHER_PID=$!
echo "Pusher PID: $PUSHER_PID"

echo ""
echo "=== All services running ==="
echo ""
echo "To watch server logs in another terminal:"
echo "  tail -f /tmp/jwsch-server.log"
echo ""
echo "To watch pusher logs:"
echo "  tail -f /tmp/jwsch-pusher.log"
echo ""
echo "Press Ctrl+C to stop all services"

cleanup() {
    echo ""
    echo "Stopping services..."
    kill $SERVER_PID $WEBAPP_PID $PUSHER_PID 2>/dev/null
    wait $SERVER_PID $WEBAPP_PID $PUSHER_PID 2>/dev/null
    echo "All services stopped"
}

trap cleanup EXIT

# Keep script running and show server logs
tail -f /tmp/jwsch-server.log &

wait