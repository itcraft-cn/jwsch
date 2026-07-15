FROM openjdk:8-jre-slim

LABEL maintainer="Itcraft Team"
LABEL description="jwschd - Enterprise Java WebSocket Exchange Daemon"

WORKDIR /app

RUN groupadd -r jwsch && useradd -r -g jwsch jwsch

COPY jwschd/target/jwschd-1.0.0-SNAPSHOT.jar /app/jwschd.jar
COPY jwsch-common/target/jwsch-common-1.0.0-SNAPSHOT.jar /app/lib/
COPY jwsch-cli/target/jwsch-cli-1.0.0-SNAPSHOT.jar /app/lib/
COPY jwsch-srv/target/jwsch-srv-1.0.0-SNAPSHOT.jar /app/lib/

RUN chown -R jwsch:jwsch /app

USER jwsch

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

EXPOSE 8080 8081 8082 9090

HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8081/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -cp /app/jwschd.jar:/app/lib/* cn.itcraft.jwschd.JwschdApplication"]