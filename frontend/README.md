# Agriscope Frontend

```bash
# Install dependencies
npm install

# Start development server
ng serve

# Open http://localhost:4200
```

## Frontend Structure

```
├── src
│   ├── app
│   │   ├── app.config.ts
│   │   ├── app.css
│   │   ├── app.html
│   │   ├── app.routes.ts
│   │   ├── app.spec.ts
│   │   ├── app.ts
│   │   ├── components
│   │   │   ├── alerts-notification
│   │   │   ├── feedback
│   │   │   ├── field-grid
│   │   │   ├── forgot-password
│   │   │   ├── home
│   │   │   ├── landing-page
│   │   │   ├── login
│   │   │   ├── map
│   │   │   ├── new-farm-component
│   │   │   ├── profile
│   │   │   ├── recommendations
│   │   │   ├── reset-password
│   │   │   ├── seeds
│   │   │   ├── sidebar
│   │   │   ├── signup
│   │   │   ├── statistic
│   │   │   ├── topbar
│   │   │   └── weather-widget
│   │   ├── dtos
│   │   │   ├── auth-request.ts
│   │   │   ├── farm.ts
│   │   │   ├── field.ts
│   │   │   ├── signup.ts
│   │   │   └── user.ts
│   │   ├── global
│   │   │   └── globals.ts
│   │   ├── guard
│   │   │   ├── auth-guard.spec.ts
│   │   │   ├── auth-guard.ts
│   │   │   ├── no-auth-guard.spec.ts
│   │   │   └── no-auth-guard.ts
│   │   ├── models
│   │   │   ├── Farm.ts
│   │   │   ├── Feedback.ts
│   │   │   ├── FieldStatus.ts
│   │   │   ├── Field.ts
│   │   │   ├── GrowthStage.ts
│   │   │   ├── Recommendation.ts
│   │   │   ├── Seed.ts
│   │   │   ├── SeedType.ts
│   │   │   └── SoilType.ts
│   │   └── services
│   │       ├── analytics-service
│   │       ├── auth-service
│   │       ├── farm-service
│   │       ├── notification-service
│   │       ├── seed-service
│   │       ├── signup-service
│   │       ├── user-service
│   │       └── websocket-service
│   ├── assets
│   │   ├── data
│   │   │   └── feedback-questions.json
│   │   ├── icons
│   │   │   ├── ...
│   │   └── images
│   │       ├── ...
│   ├── index.html
│   ├── main.ts
```
