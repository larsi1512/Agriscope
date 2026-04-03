# Use Java 21
FROM eclipse-temurin:21

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml first for caching
COPY pom.xml .
COPY mvnw .
COPY .mvn ./.mvn
RUN ./mvnw dependency:go-offline

# Copy source and build JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Expose Eureka port
EXPOSE 8761

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "/app/target/service-registry-0.0.1-SNAPSHOT.jar"]
