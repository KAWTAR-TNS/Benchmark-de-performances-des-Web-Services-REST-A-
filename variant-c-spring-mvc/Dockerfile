FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /build

# Copy parent pom
COPY pom.xml ./pom.xml

# Copy all module poms (needed for parent build)
COPY variant-a-jersey/pom.xml ./variant-a-jersey/pom.xml
COPY variant-c-spring-mvc/pom.xml ./variant-c-spring-mvc/pom.xml
COPY variant-d-spring-data-rest/pom.xml ./variant-d-spring-data-rest/pom.xml

# Copy variant C source
COPY variant-c-spring-mvc/src ./variant-c-spring-mvc/src

# Build
RUN apk add --no-cache maven && \
    mvn clean install -DskipTests -pl variant-c-spring-mvc -am

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=builder /build/variant-c-spring-mvc/target/variant-c-spring-mvc.jar /app/app.jar

ENV JAVA_OPTS="-Xms512m -Xmx1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SERVER_PORT=8080

EXPOSE 8080

CMD java $JAVA_OPTS -jar /app/app.jar
