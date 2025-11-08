FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy parent pom
COPY pom.xml ./pom.xml

# Copy all module poms (needed for parent build)
COPY variant-a-jersey/pom.xml ./variant-a-jersey/pom.xml
COPY variant-c-spring-mvc/pom.xml ./variant-c-spring-mvc/pom.xml
COPY variant-d-spring-data-rest/pom.xml ./variant-d-spring-data-rest/pom.xml

# Copy variant A source
COPY variant-a-jersey/src ./variant-a-jersey/src

# Build
RUN apk add --no-cache maven && \
    mvn clean install -DskipTests -pl variant-a-jersey -am

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy JMX Prometheus agent
ADD https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.20.0/jmx_prometheus_javaagent-0.20.0.jar /app/jmx_prometheus_javaagent.jar

# Copy JMX config
COPY docker/jmx-exporter-config.yml /app/jmx-exporter-config.yml

# Copy application jar
COPY --from=builder /build/variant-a-jersey/target/variant-a-jersey.jar /app/app.jar

ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SERVER_PORT=8080
ENV SERVER_HOST=0.0.0.0

EXPOSE 8080 9090

CMD java $JAVA_OPTS \
    -javaagent:/app/jmx_prometheus_javaagent.jar=9090:/app/jmx-exporter-config.yml \
    -jar /app/app.jar
