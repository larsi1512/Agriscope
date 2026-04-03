# Use Java 21
FROM eclipse-temurin:21

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml for caching
COPY pom.xml .
COPY mvnw .
COPY .mvn ./.mvn
RUN ./mvnw dependency:go-offline

# Copy source code and build JAR
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Expose API gateway port
EXPOSE 8080

# Run the Spring Boot app
ENTRYPOINT ["java", "-jar", "/app/target/api-gateway-0.0.1-SNAPSHOT.jar"]
