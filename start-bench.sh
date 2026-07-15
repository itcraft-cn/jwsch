#!/bin/bash

echo "=== Jwsch Benchmark Script ==="
echo ""

BASE_DIR="$(cd "$(dirname "$0")" && pwd)"

WS_PORT=${WS_PORT:-8080}
TCP_PORT=${TCP_PORT:-9090}
WORKERS=${WORKERS:-16}
PUBLISHERS=${PUBLISHERS:-1}
SUBSCRIBERS=${SUBSCRIBERS:-5}
TOPIC=${TOPIC:-/topic/bench}
INTERVAL=${INTERVAL:-10}
PAYLOAD_SIZE=${PAYLOAD_SIZE:-2}
DURATION=${DURATION:-5}
REPORT=${REPORT:-5}

usage() {
    echo "Usage: $0 [options]"
    echo ""
    echo "Options:"
    echo "  --wsPort <port>       WebSocket port (default: 8080)"
    echo "  --tcpPort <port>      TCP port (default: 9090)"
    echo "  --workers <n>         Worker threads (default: 16)"
    echo "  --publishers <n>      Number of publishers (default: 1)"
    echo "  --subscribers <n>     Number of subscribers (default: 5)"
    echo "  --topic <topic>       Topic to test (default: /topic/bench)"
    echo "  --interval <us>       Send interval in microseconds (default: 10)"
    echo "  --payloadSize <bytes> Payload size in bytes, 2-2M (default: 2)"
    echo "  --duration <min>      Duration in minutes, 0 for unlimited (default: 5)"
    echo "  --report <sec>        Report interval in seconds (default: 5)"
    echo "  --help                Show this help"
    exit 0
}

while [[ $# -gt 0 ]]; do
    case $1 in
        --wsPort)
            WS_PORT="$2"
            shift 2
            ;;
        --tcpPort)
            TCP_PORT="$2"
            shift 2
            ;;
        --workers)
            WORKERS="$2"
            shift 2
            ;;
        --publishers)
            PUBLISHERS="$2"
            shift 2
            ;;
        --subscribers)
            SUBSCRIBERS="$2"
            shift 2
            ;;
        --topic)
            TOPIC="$2"
            shift 2
            ;;
        --interval)
            INTERVAL="$2"
            shift 2
            ;;
        --payloadSize)
            PAYLOAD_SIZE="$2"
            shift 2
            ;;
        --duration)
            DURATION="$2"
            shift 2
            ;;
        --report)
            REPORT="$2"
            shift 2
            ;;
        --help|-h)
            usage
            ;;
        *)
            echo "Unknown option: $1"
            usage
            ;;
    esac
done

echo "Configuration:"
echo "  WebSocket Port: $WS_PORT"
echo "  TCP Port: $TCP_PORT"
echo "  Worker Threads: $WORKERS"
echo "  Publishers: $PUBLISHERS"
echo "  Subscribers: $SUBSCRIBERS"
echo "  Topic: $TOPIC"
echo "  Send Interval: ${INTERVAL}μs"
echo "  Payload Size: ${PAYLOAD_SIZE} bytes"
echo "  Duration: ${DURATION} minutes"
echo "  Report Interval: ${REPORT}s"
echo ""

echo "1. Building project..."
mvnd package -pl jwsch-bench -am -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -q

if [ $? -ne 0 ]; then
    echo "Build failed!"
    exit 1
fi

echo ""
echo "2. Starting benchmark..."
echo ""

java -jar "$BASE_DIR/jwsch-bench/target/jwsch-bench-1.0.0-SNAPSHOT.jar" \
    --wsPort $WS_PORT \
    --tcpPort $TCP_PORT \
    --workers $WORKERS \
    --publishers $PUBLISHERS \
    --subscribers $SUBSCRIBERS \
    --topic "$TOPIC" \
    --interval $INTERVAL \
    --payloadSize $PAYLOAD_SIZE \
    --duration $DURATION \
    --report $REPORT

echo ""
echo "Benchmark finished."