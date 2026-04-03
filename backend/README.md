# Backend - Microservices Architecture

This directory contains the core intelligence and data management layers of the Agriscope platform. The system is built using a Microservices Architecture pattern, leveraging Spring Cloud for orchestration and RabbitMQ for event-driven communication.


## Service Breakdown

### Infrastructure & Edge

- Service Registry (Eureka): Acts as the central phonebook for the system. Every microservice registers here, allowing them to find each other dynamically without hardcoded URLs.

- API Gateway: The single entry point for the frontend. It handles request routing, load balancing, and cross-cutting concerns. It works in tandem with the User Service to ensure only authorized requests reach the internal services.

### Core Business Services

- User Service: Manages identity and access. It handles user registration, JWT issuance, and profile management (including profile picture processing). It publishes events to trigger welcome workflows or to reset password.

- Farm Service: The source of truth for agricultural data. It manages the hierarchy of Farms and Fields, tracks crop types (Seeds), and growth stages. It also stores historical harvest data and user feedback which is used to fine-tune the recommendation engine.

### Intelligent Processing

- Sensor Ingestion Service: Orchestrates the flow of environmental data. It consumes raw metrics from IoT sensors (simulated or real) and prepares them for rule evaluation by publishing standardized weather events to the message broker.

- Rule Engine (Drools): The "brain" of Agriscope. It uses a state-of-the-art Drools-based engine to evaluate complex agricultural rules (e.g., frost risk, irrigation needs, disease windows). It calculates water deficits by comparing real-time sensor data against specific crop coefficients.

### Communication

- Notification Service: Manages multi-channel communication. It consumes recommendations from the Rule Engine and dispatches them via WebSockets for real-time dashboard alerts and JavaMail (SMTP) for persistent email notifications. It includes an in-memory caching layer (Caffeine) to prevent "alert fatigue" by suppressing duplicate notifications.


## Messaging & Event Flow 

The system relies heavily on RabbitMQ to maintain a decoupled architecture.
 
 
### Synchronous Communication (REST)
While most communication is event-driven, some interactions require immediate data consistency:

| **Source Service** | **Destination Service** | **Exchange** | **Routing Key** | **Queue Name** | **Purpose** |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Sensor Ingestion** | Rule Engine | `weather_exchange` | `weather.#` | `weather_rule_queue` | Forwards standardized weather data (current, hourly, daily) for rule evaluation. |
| **Sensor Ingestion** | Notification Service | `weather_exchange` | `weather.current` | `notification_weather_queue` | Pushes real-time weather updates to the user's dashboard widget via WebSocket. |
| **Sensor Ingestion** | *Self* (Internal Loop) | `farm_events` | `farm.created` | `weather_ingestion_new_farms` | Triggers immediate weather fetch when a new farm is created. |
| **Sensor Ingestion** | *Self* (Internal Loop) | `farm_events` | `farm.updated` | `weather_ingestion_updates` | Triggers weather update when farm fields are modified. |
| **Rule Engine** | Notification Service | `alert_exchange` | `alert.recommendation` | `alert_queue` | Dispatches generated recommendations (e.g., "Irrigate Now", "Frost Alert") to be sent to the user. |
| **Farm Service** | Notification Service | `farm_events` | `field.harvested` | `notification_harvest_queue` | Notifies the system to clear alert caches for a specific field after harvest (reset state). |
| **User Service** | Notification Service | `email_exchange` | `user.registered` | `user_registered_queue` | Triggers a "Welcome" email when a new user successfully registers. |
| **User Service** | Notification Service | `email_exchange` | `email.generic` | `email_queue` | Handles generic email requests, such as password reset links. |

- Rule Engine → Farm Service: The Rule Engine calls `GET /api/farms/{farmId}/feedback-factors`  to fetch user feedback adjustments before evaluating rules.

## Deployment & Development
Database Management

All databases are containerized for environment consistency. The system uses:

- MongoDB: Distributed across 4 instances for high-availability storage of Users, Farms, Seeds, and Notifications.

- Caffeine Cache: Integrated within the Notification Service for high-performance, in-memory deduplication of alerts.


##  Monitoring & Dashboards

When the system is running via Docker Compose, several management interfaces are available for monitoring the health of the microservices and the message broker.

| Service | URL | Credentials (Default) | Description |
| :--- | :--- | :--- | :--- |
| **Eureka Dashboard** | [http://localhost:8761](http://localhost:8761) | *None* | View registered service instances and their health status. |
| **RabbitMQ Manager** | [http://localhost:15672](http://localhost:15672) | `guest` / `guest` | Monitor queues, exchanges, and message rates in real-time. |
| **API Gateway** | [http://localhost:8080](http://localhost:8080) | *None* | The public entry point for all backend API requests. |

---

##  Running Tests

Since the project uses a polyglot architecture (Java & Python), different tools are used for testing.

### Java Services (User, Farm, Rule-engine, Notification)
Run unit and integration tests using Maven:
```bash
cd backend/notification-service
./mvnw test 
```

### Python Service (Sensor Ingestion)

The sensor simulation service is tested using pytest.
```bash
cd backend/sensor-ingestion-service
pip install -r requirements.txt
pytest
```