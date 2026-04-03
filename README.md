# Agriscope - Farm Management System

A farm management platform for monitoring fields, tracking crops, and receiving intelligent alerts.

## Key Features
- **Farm Management:** Manage multiple farms using a single account
- **Field Management:** Visual grid system with crop planting and harvest tracking
- **Real-time Alerts:** WebSocket notifications for frost, heat, irrigation, disease risks, and much more
- **Weather Forecast:** Weather forecast widget displayed on the farm page
- **Email Notifications:** Email alerts for farm notifications and profile changes
- **Feedback System:** After successful harvests, users can provide feedback about their farming experience with the application, which helps customize the rule-based engine connected to their profile
- **Profile Management:** User management features such as profile editing, profile picture upload, password reset, and more
- **Analytics Dashboard:** Charts displaying feedback statistics, alert distribution, crop vulnerability, and water savings
- **Seeds Catalog:** Comprehensive crop database that powers the rule-based recommendation engine

## Application overview

<div align="center">
  <img src="frontend/src/assets/images/workflow.png" alt="Agriscope Workflow" width="700"/>
  <p><em>Application workflow</em></p>
</div>

📹 [Watch Demo Video](https://drive.google.com/file/d/1cfG_x4XYp9j8F1dizdWV9Mr_yhIkngml/view?usp=sharing)

*Full walkthrough of Agriscope features*

## Tech Stack

-  **Frontend:** Angular 21, TypeScript, Chart.js 4.5, ng2-charts 8.0  
-  **Backend:** Spring Boot 3, Java 17+  
-  **Databases:** MongoDB 6.0+, PostgreSQL 14+  
-  **Message Queue:** RabbitMQ 3.12+  
-  **Communication:** REST API, WebSocket (STOMP), Email (SMTP)


## Prerequisites

- **Node.js**: 20.19+ or 22.12+ or 24.0+
- **npm**: 9+ (comes with Node.js)
- **Angular CLI**: 18+
- **Java JDK**: 17 or higher
- **Maven**: 3.8+
- **MongoDB**: 6.0+
- **RabbitMQ**: 3.12+
- **PostgreSQL**: 14+ (for user/farm data)


## Quick Setup

```bash
cd 25ws-ase-pr-inso-01

# Backend services
cd backend
docker-compose up -d --build

# Frontend

cd ../frontend 
npm install
ng serve 

```
Each microservice and whole frontend application should be build after execution of this command.

Application should be than accessible at **http://localhost:4200**


## Project Structure

```
.
├── backend
│   ├── api-gateway
│   ├── farm-service
│   ├── init-farms.sh
│   ├── init-mongo.sh
│   ├── notification-service
│   ├── README.md
│   ├── rule-engine
│   ├── seeds.json
│   ├── sensor-ingestion-service
│   ├── service-registry
│   └── user-service
├── docker-compose.yml
├── frontend
│   ├── angular.json
│   ├── package.json
│   ├── package-lock.json
│   ├── public
│   ├── README.md
│   ├── src
│   ├── tsconfig.app.json
│   ├── tsconfig.json
│   └── tsconfig.spec.json
├── out
│   └── production
├── package.json
├── package-lock.json
└── README.md
```

## API Endpoints

### User Service (Port 8081)

#### Authentication
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/authentication` | User login |

#### User Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/users` | Create new user (register) |
| GET | `/api/users/me` | Get current authenticated user |
| GET | `/api/users/by-email/{email}` | Get user by email address |
| GET | `/api/users/{userId}` | Get user by ID |
| PUT | `/api/users/me` | Update current user profile |
| PUT | `/api/users/me/delete` | Delete current user account |

#### Password Reset
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/users/password-reset` | Request password reset (send email) |
| POST | `/api/users/password-reset` | Confirm password reset with token |

---

### Notification Service (Port 8083)

#### Notifications
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/notifications/weather/latest/{farmId}` | Get latest weather notification for farm |
| GET | `/api/notifications/alerts/{farmId}` | Get all alerts for specific farm |
| PUT | `/api/notifications/alerts/{alertId}/read` | Mark specific alert as read |

#### Analytics
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/analytics/dashboard/{farmId}` | Get dashboard analytics for farm |

#### WebSocket
| Protocol | Endpoint | Description |
|----------|----------|-------------|
| WS | `/ws-notifications` | WebSocket connection for real-time alerts |

---

### Farm Service (Port 8082)

#### Farm Management
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/farms` | Create new farm |
| GET | `/api/farms/check` | Check farm availability/status |
| GET | `/api/farms` | Get all farms for current user |
| GET | `/api/farms/{farmId}` | Get specific farm by ID |
| PUT | `/api/farms/{farmId}/fields` | Update farm fields configuration |

#### Field Operations
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/farms/{farmId}/fields/{fieldId}/harvest` | Harvest a specific field |

#### Harvest History
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/farms/{farmId}/harvest-history` | Get harvest history for farm |
| DELETE | `/api/farms/harvest-history/{historyId}` | Delete specific harvest history entry |
| DELETE | `/api/farms/{farmId}/harvest-history` | Delete all harvest history for farm |

#### Feedback
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/farms/harvest-history/{historyId}/feedback` | Submit feedback for harvest |
| GET | `/api/farms/{farmId}/feedback-factors` | Get feedback factors for farm |
| GET | `/api/farm-analytics/feedback-stats/{farmId}` | Get feedback statistics for farm |

#### Seeds Catalog
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/seeds/getAll` | Get all available seeds |
| GET | `/api/seeds/getByName/{name}` | Get specific seed by name |