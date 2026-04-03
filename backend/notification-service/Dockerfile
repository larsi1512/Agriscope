FROM eclipse-temurin:21

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn ./.mvn
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw clean package -DskipTests

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/target/notification-service-0.0.1-SNAPSHOT.jar"]