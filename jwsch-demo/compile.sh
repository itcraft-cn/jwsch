#!/bin/bash
set -e

cd /disk2/helly_data/code/java/jwsch

# Build dependencies if needed
/home/helly/app/maven-mvnd/bin/mvnd compile -pl jwsch-common,jwsch-cli -q

# Find JARs
COMMON_JAR=$(find jwsch-common/target -name "*.jar" | grep -v sources | grep -v test)
CLI_JAR=$(find jwsch-cli/target -name "*.jar" | grep -v sources | grep -v test)
NETTY_JAR=$(find ~/.m2/repository/io/netty/netty-all -name "netty-all-*.jar" | head -1)
SLF4J_JAR=$(find ~/.m2/repository/org/slf4j/slf4j-api -name "slf4j-api-*.jar" | head -1)

# Compile demo classes
mkdir -p jwsch-demo/target/classes
javac -d jwsch-demo/target/classes \
    -cp "$COMMON_JAR:$CLI_JAR:$NETTY_JAR:$SLF4J_JAR" \
    jwsch-demo/PricePublisher.java jwsch-demo/PricePublisherMain.java

echo "Compiled successfully"

# Create runnable JAR
jar cfe jwsch-demo/target/price-publisher.jar \
    cn.itcraft.jwsch.demo.PricePublisherMain \
    -C jwsch-demo/target/classes .

echo "JAR created: jwsch-demo/target/price-publisher.jar"
