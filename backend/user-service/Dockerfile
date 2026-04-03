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

# Expose user service port and debug port
EXPOSE 8081 5005

# Run the Spring Boot app
ENTRYPOINT ["java", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005", "-jar", "/app/target/user-service-0.0.1-SNAPSHOT.jar"]
