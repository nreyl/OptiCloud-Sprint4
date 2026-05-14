FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN apt-get update -y && apt-get install -y --no-install-recommends maven \
    && mvn -B -DskipTests package \
    && rm -rf /var/lib/apt/lists/*

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/target/normalization-service.jar app.jar
ENV SERVER_PORT=8080
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
