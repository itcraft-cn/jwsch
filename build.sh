#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

MVND="/home/helly/app/maven-mvnd/bin/mvnd"
if [ ! -x "$MVND" ]; then
    MVND="mvn"
fi

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

check_command() {
    if ! command -v "$1" &> /dev/null; then
        log_error "$1 is not installed"
        exit 1
    fi
}

print_banner() {
    echo ""
    echo "============================================"
    echo "  jwsch Complete Build System"
    echo "  Version: 1.0.0-SNAPSHOT"
    echo "============================================"
    echo ""
}

build_jwsch_js() {
    log_info "Building jwsch-js (JavaScript client library)..."
    
    local JS_DIR="$SCRIPT_DIR/jwsch-js"
    if [ ! -d "$JS_DIR" ]; then
        log_warn "jwsch-js directory not found, skipping..."
        return 0
    fi
    
    cd "$JS_DIR"
    
    if [ ! -f "package.json" ]; then
        log_warn "jwsch-js/package.json not found, skipping..."
        cd "$SCRIPT_DIR"
        return 0
    fi
    
    log_info "  Running npm install..."
    npm install --loglevel=error
    
    log_info "  Running npm run build (Rollup)..."
    npm run build
    
    if [ -f "lib/jwsch.umd.js" ]; then
        log_success "  Built: lib/jwsch.umd.js"
    fi
    if [ -f "lib/jwsch.esm.js" ]; then
        log_success "  Built: lib/jwsch.esm.js"
    fi
    if [ -f "lib/jwsch.cjs.js" ]; then
        log_success "  Built: lib/jwsch.cjs.js"
    fi
    
    cd "$SCRIPT_DIR"
    log_success "jwsch-js build completed"
}

build_java_core() {
    log_info "Building Java core modules (common, cli, srv, jwschd, bench)..."
    
    $MVND clean package \
        -pl jwsch-common,jwsch-cli,jwsch-srv,jwschd,jwsch-bench \
        -am \
        -Dmaven.test.skip=true \
        -Dmaven.javadoc.skip=true \
        -q
    
    log_success "Java core modules built"
}

build_sample_webapp_frontend() {
    log_info "Building sample-webapp frontend (Vue 3 + Vite)..."
    
    local WEBAPP_DIR="$SCRIPT_DIR/jwsch-sample/sample-webapp"
    local FRONTEND_DIR="$WEBAPP_DIR/frontend"
    
    if [ ! -d "$FRONTEND_DIR" ]; then
        log_warn "Frontend directory not found, skipping..."
        return 0
    fi
    
    cd "$FRONTEND_DIR"
    
    log_info "  Copying jwsch-js source to frontend/src/lib..."
    mkdir -p src/lib
    
    if [ -d "$SCRIPT_DIR/jwsch-js/src" ]; then
        cp -r "$SCRIPT_DIR/jwsch-js/src/"* src/lib/
        log_info "  Copied jwsch-js source files"
    fi
    
    log_info "  Running npm install..."
    npm install --loglevel=error
    
    log_info "  Running npm run build (Vite)..."
    npm run build
    
    if [ -d "dist" ]; then
        log_success "  Frontend built to frontend/dist/"
        ls -la dist/ 2>/dev/null | head -5
    fi
    
    cd "$SCRIPT_DIR"
    log_success "sample-webapp frontend build completed"
}

build_sample_modules() {
    log_info "Building sample modules (server, webapp, pusher)..."
    
    local WEBAPP_DIR="$SCRIPT_DIR/jwsch-sample/sample-webapp"
    
    if [ -d "$WEBAPP_DIR/frontend/dist" ]; then
        log_info "  Copying frontend dist to static resources..."
        mkdir -p "$WEBAPP_DIR/src/main/resources/static"
        cp -r "$WEBAPP_DIR/frontend/dist/"* "$WEBAPP_DIR/src/main/resources/static/"
    fi
    
    $MVND package \
        -pl jwsch-sample/sample-server,jwsch-sample/sample-webapp,jwsch-sample/sample-pusher \
        -am \
        -Dmaven.test.skip=true \
        -Dmaven.javadoc.skip=true \
        -Dskip.frontend=false \
        -q
    
    log_success "Sample modules built"
}

build_jwsch_demo() {
    log_info "Building jwsch-demo..."
    
    local DEMO_DIR="$SCRIPT_DIR/jwsch-demo"
    
    if [ ! -d "$DEMO_DIR" ]; then
        log_warn "jwsch-demo directory not found, skipping..."
        return 0
    fi
    
    if [ -f "$DEMO_DIR/PricePublisher.java" ] && [ -f "$DEMO_DIR/PricePublisherMain.java" ]; then
        if [ -f "$DEMO_DIR/compile.sh" ]; then
            bash "$DEMO_DIR/compile.sh"
            log_success "jwsch-demo built"
        fi
    else
        log_info "  jwsch-demo Java sources not found (web-only demo), copying JS artifacts..."
        if [ -f "$SCRIPT_DIR/jwsch-js/lib/jwsch.umd.js" ]; then
            cp "$SCRIPT_DIR/jwsch-js/lib/jwsch.umd.js" "$DEMO_DIR/"
            log_success "  Copied jwsch.umd.js to jwsch-demo/"
        fi
        log_success "jwsch-demo ready (web-only mode)"
    fi
}

collect_artifacts() {
    log_info "Collecting build artifacts..."
    
    local DIST_DIR="$SCRIPT_DIR/dist"
    mkdir -p "$DIST_DIR"
    
    log_info "  Creating jwschd distribution..."
    mkdir -p "$DIST_DIR/jwschd"
    cp "$SCRIPT_DIR/jwschd/target/jwschd-1.0.0-SNAPSHOT.jar" "$DIST_DIR/jwschd/jwschd.jar"
    
    log_info "  Creating jwsch-bench distribution..."
    mkdir -p "$DIST_DIR/bench"
    cp "$SCRIPT_DIR/jwsch-bench/target/jwsch-bench-1.0.0-SNAPSHOT.jar" "$DIST_DIR/bench/jwsch-bench.jar"
    
    log_info "  Creating sample-server distribution..."
    mkdir -p "$DIST_DIR/sample-server"
    cp "$SCRIPT_DIR/jwsch-sample/sample-server/target/sample-server-1.0.0-SNAPSHOT.jar" "$DIST_DIR/sample-server/sample-server.jar"
    
    log_info "  Creating sample-webapp distribution..."
    mkdir -p "$DIST_DIR/sample-webapp"
    cp "$SCRIPT_DIR/jwsch-sample/sample-webapp/target/sample-webapp-1.0.0-SNAPSHOT.jar" "$DIST_DIR/sample-webapp/sample-webapp.jar"
    
    log_info "  Creating sample-pusher distribution..."
    mkdir -p "$DIST_DIR/sample-pusher"
    cp "$SCRIPT_DIR/jwsch-sample/sample-pusher/target/sample-pusher-1.0.0-SNAPSHOT.jar" "$DIST_DIR/sample-pusher/sample-pusher.jar"
    
    log_info "  Creating jwsch-js distribution..."
    if [ -d "$SCRIPT_DIR/jwsch-js/lib" ]; then
        mkdir -p "$DIST_DIR/jwsch-js"
        cp -r "$SCRIPT_DIR/jwsch-js/lib" "$DIST_DIR/jwsch-js/"
        if [ -d "$SCRIPT_DIR/jwsch-js/src" ]; then
            cp -r "$SCRIPT_DIR/jwsch-js/src" "$DIST_DIR/jwsch-js/"
        fi
    fi
    
    echo ""
    log_success "Artifacts collected to $DIST_DIR/"
    echo ""
    ls -la "$DIST_DIR/" 2>/dev/null
}

print_summary() {
    echo ""
    echo "============================================"
    echo "  Build Summary"
    echo "============================================"
    echo ""
    
    echo "Core modules:"
    echo "  - jwschd.jar      (WebSocket server daemon)"
    echo "  - jwsch-bench.jar (Benchmark tool)"
    echo ""
    
    echo "Sample applications:"
    echo "  - sample-server.jar  (WebSocket:8080, TCP:9090)"
    echo "  - sample-webapp.jar  (Web application:3000)"
    echo "  - sample-pusher.jar  (Message pusher)"
    echo ""
    
    echo "JavaScript client:"
    echo "  - jwsch-js/lib/jwsch.umd.js  (Browser UMD)"
    echo "  - jwsch-js/lib/jwsch.esm.js  (ES Module)"
    echo "  - jwsch-js/lib/jwsch.cjs.js  (CommonJS)"
    echo ""
    
    echo "Quick start:"
    echo "  ./start-sample.sh    # Start all samples"
    echo "  ./start-demo.sh      # Start EURUSD demo"
    echo ""
    
    log_success "Build completed successfully!"
}

main() {
    print_banner
    
    check_command "npm"
    check_command "java"
    
    build_jwsch_js
    
    build_java_core
    
    build_sample_webapp_frontend
    
    build_sample_modules
    
    build_jwsch_demo
    
    collect_artifacts
    
    print_summary
}

case "${1:-}" in
    --js)
        build_jwsch_js
        ;;
    --java)
        build_java_core
        ;;
    --frontend)
        build_sample_webapp_frontend
        ;;
    --sample)
        build_sample_modules
        ;;
    --demo)
        build_jwsch_demo
        ;;
    --collect)
        collect_artifacts
        ;;
    --help|-h)
        echo "Usage: $0 [OPTION]"
        echo ""
        echo "Options:"
        echo "  --js        Build only jwsch-js"
        echo "  --java      Build only Java core modules"
        echo "  --frontend  Build only sample-webapp frontend"
        echo "  --sample    Build only sample modules"
        echo "  --demo      Build only jwsch-demo"
        echo "  --collect   Collect artifacts to dist/"
        echo "  --help      Show this help"
        echo ""
        echo "Run without options for full build."
        ;;
    *)
        main
        ;;
esac