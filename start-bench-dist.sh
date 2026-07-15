#!/bin/bash

set -e

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

SERVER_JVM=${SERVER_JVM:-"-Xmx1g"}
SUB_JVM=${SUB_JVM:-"-Xmx256m"}
PUB_JVM=${PUB_JVM:-"-Xmx256m"}

WS_PORT=${WS_PORT:-8080}
TCP_PORT=${TCP_PORT:-9090}
WORKERS=${WORKERS:-16}
PUBLISHERS=${PUBLISHERS:-1}
SUBSCRIBERS=${SUBSCRIBERS:-50}
TOPIC=${TOPIC:-/topic/bench}
INTERVAL=${INTERVAL:-10}
PAYLOAD_SIZE=${PAYLOAD_SIZE:-2}
DURATION=${DURATION:-5}
REPORT=${REPORT:-5}

usage() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Multi-Process Benchmark: server, publisher, subscriber run in separate JVMs."
    echo ""
    echo "Options:"
    echo "  --wsPort <port>       WebSocket port (default: 8080)"
    echo "  --tcpPort <port>      TCP port (default: 9090)"
    echo "  --workers <n>         Server worker threads (default: 16)"
    echo "  --publishers <n>      Number of publishers (default: 1)"
    echo "  --subscribers <n>     Number of subscribers (default: 50)"
    echo "  --topic <topic>       Topic to test (default: /topic/bench)"
    echo "  --interval <us>       Send interval in microseconds (default: 10)"
    echo "  --payloadSize <bytes> Payload size, 2-512KB (default: 2)"
    echo "  --duration <min>      Duration in minutes, 0=unlimited (default: 5)"
    echo "  --report <sec>        Report interval (default: 5)"
    echo "  --help                Show this help"
    echo ""
    echo "JVM Options (env vars):"
    echo "  SERVER_JVM            Server JVM options (default: -Xmx1g)"
    echo "  SUB_JVM               Subscriber JVM options (default: -Xmx256m)"
    echo "  PUB_JVM               Publisher JVM options (default: -Xmx256m)"
    exit 0
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --wsPort) WS_PORT="$2"; shift 2 ;;
        --tcpPort) TCP_PORT="$2"; shift 2 ;;
        --workers) WORKERS="$2"; shift 2 ;;
        --publishers) PUBLISHERS="$2"; shift 2 ;;
        --subscribers) SUBSCRIBERS="$2"; shift 2 ;;
        --topic) TOPIC="$2"; shift 2 ;;
        --interval) INTERVAL="$2"; shift 2 ;;
        --payloadSize) PAYLOAD_SIZE="$2"; shift 2 ;;
        --duration) DURATION="$2"; shift 2 ;;
        --report) REPORT="$2"; shift 2 ;;
        --help|-h) usage ;;
        *) echo "Unknown option: $1"; usage ;;
    esac
done

echo "=== Jwsch Multi-Process Benchmark ==="
echo ""
echo "Configuration:"
echo "  WebSocket Port: $WS_PORT"
echo "  TCP Port: $TCP_PORT"
echo "  Worker Threads: $WORKERS"
echo "  Publishers: $PUBLISHERS"
echo "  Subscribers: $SUBSCRIBERS"
echo "  Topic: $TOPIC"
echo "  Send Interval: ${INTERVAL}μs"
echo "  Payload Size: ${PAYLOAD_SIZE} bytes"
echo "  Duration: ${DURATION} min"
echo "  Report Interval: ${REPORT}s"
echo ""
echo "JVM Options:"
echo "  Server: $SERVER_JVM"
echo "  Subscriber: $SUB_JVM"
echo "  Publisher: $PUB_JVM"
echo ""

JAR="$BASE_DIR/jwsch-bench/target/jwsch-bench-1.0.0-SNAPSHOT.jar"

if [ ! -f "$JAR" ]; then
    echo "JAR not found: $JAR"
    echo "Building project..."
    mvnd package -pl jwsch-bench -am -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -q
    if [ $? -ne 0 ]; then
        echo "Build failed!"
        exit 1
    fi
fi

PIDS=()

cleanup() {
    echo ""
    echo "[ORCH] Shutting down all processes..."
    for pid in "${PIDS[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null || true
        fi
    done
    sleep 2
    for pid in "${PIDS[@]}"; do
        if kill -0 "$pid" 2>/dev/null; then
            kill -9 "$pid" 2>/dev/null || true
        fi
    done
    echo "[ORCH] All processes terminated."
    exit 0
}

trap cleanup SIGINT SIGTERM

echo "[ORCH] Starting server..."
SERVER_LOG=$(mktemp)
java $SERVER_JVM -jar "$JAR" server \
    --wsPort $WS_PORT \
    --tcpPort $TCP_PORT \
    --workers $WORKERS \
    > "$SERVER_LOG" 2>&1 &
SERVER_PID=$!
PIDS+=($SERVER_PID)

echo "[ORCH] Waiting for SERVER_READY..."
while ! grep -q "SERVER_READY" "$SERVER_LOG"; do
    if ! kill -0 $SERVER_PID 2>/dev/null; then
        echo "[ORCH] Server process died!"
        cat "$SERVER_LOG"
        exit 1
    fi
    sleep 0.1
done
echo "[ORCH] Server ready (PID: $SERVER_PID)"

sleep 1

WS_URL="ws://localhost:$WS_PORT/ws"

echo "[ORCH] Starting $SUBSCRIBERS subscribers..."
SUB_LOG=$(mktemp)
java $SUB_JVM -jar "$JAR" subscriber \
    --wsUrl "$WS_URL" \
    --subscribers $SUBSCRIBERS \
    --topic "$TOPIC" \
    --report $REPORT \
    --duration $DURATION \
    > "$SUB_LOG" 2>&1 &
SUB_PID=$!
PIDS+=($SUB_PID)

echo "[ORCH] Waiting for SUBSCRIBER_READY..."
while ! grep -q "SUBSCRIBER_READY" "$SUB_LOG"; do
    if ! kill -0 $SUB_PID 2>/dev/null; then
        echo "[ORCH] Subscriber process died!"
        cat "$SUB_LOG"
        exit 1
    fi
    sleep 0.1
done
echo "[ORCH] Subscribers ready (PID: $SUB_PID)"

sleep 1

echo "[ORCH] Starting $PUBLISHERS publishers..."
PUB_LOG=$(mktemp)
java $PUB_JVM -jar "$JAR" publisher \
    --host localhost \
    --tcpPort $TCP_PORT \
    --publishers $PUBLISHERS \
    --topic "$TOPIC" \
    --interval $INTERVAL \
    --payloadSize $PAYLOAD_SIZE \
    --report $REPORT \
    --duration $DURATION \
    > "$PUB_LOG" 2>&1 &
PUB_PID=$!
PIDS+=($PUB_PID)
echo "[ORCH] Publishers started (PID: $PUB_PID)"

echo ""
echo "[ORCH] Benchmark running. Press Ctrl+C to stop."
echo ""

tail -f "$SERVER_LOG" "$SUB_LOG" "$PUB_LOG" 2>/dev/null &
TAIL_PID=$!
PIDS+=($TAIL_PID)

wait $SERVER_PID $SUB_PID $PUB_PID 2>/dev/null || true

cleanup