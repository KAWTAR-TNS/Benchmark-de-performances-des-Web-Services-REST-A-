FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy parent pom
COPY pom.xml ./pom.xml

# Copy all module poms (needed for parent build)
COPY variant-a-jersey/pom.xml ./variant-a-jersey/pom.xml
COPY variant-c-spring-mvc/pom.xml ./variant-c-spring-mvc/pom.xml
COPY variant-d-spring-data-rest/pom.xml ./variant-d-spring-data-rest/pom.xml

# Copy variant D source
COPY variant-d-spring-data-rest/src ./variant-d-spring-data-rest/src

# Build
RUN apk add --no-cache maven && \
    mvn clean install -DskipTests -pl variant-d-spring-data-rest -am

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/variant-d-spring-data-rest/target/variant-d-spring-data-rest.jar /app/app.jar

ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SERVER_PORT=8080

EXPOSE 8080

CMD java $JAVA_OPTS -jar /app/app.jar
