# Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
COPY .mvn ./.mvn
COPY mvnw .
RUN chmod +x mvnw
RUN ./mvnw -DskipTests clean package

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/core-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=42.0", \
  "-XX:InitialRAMPercentage=20.0", \
  "-XX:MinRAMPercentage=20.0", \
  "-XX:+UseSerialGC", \
  "-XX:MinHeapFreeRatio=10", \
  "-XX:MaxHeapFreeRatio=25", \
  "-XX:MaxMetaspaceSize=128m", \
  "-XX:CompressedClassSpaceSize=32m", \
  "-XX:ReservedCodeCacheSize=32m", \
  "-XX:MaxDirectMemorySize=32m", \
  "-XX:TieredStopAtLevel=1", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Xss256k", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
