FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/target/coordinator-1.0.0.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
