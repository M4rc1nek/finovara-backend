# 💰 Finovara — Backend
> **Backend REST API** for a personal finance management application built with Java 25 and Spring Boot 4.

---

## 📖 About the Project

**Finovara** is a personal finance platform designed to help users take full control of their money. The backend exposes a secure REST API that powers tracking of income and expenses, budget management, savings goals, and financial reporting — all wrapped in a bank-grade security model based on JWT authentication.

The application is designed with scalability in mind and is fully containerized via Docker, with separate production and test database environments managed through Docker Compose.

---

## 🎯 Key Features

- 🔐 **Authentication & Authorization** — JWT-based stateless security with Spring Security; access and refresh token flow with device/user-agent detection
- 💸 **Income & Expense Tracking** — full CRUD for financial operations with category tagging
- 📊 **Statistics & Reports** — aggregated financial summaries, spending trends, and exportable PDF reports
- 🏦 **Virtual Wallet** — concept of a personal digital wallet for day-to-day financial management
- 🐷 **Savings Goals (Piggy Banks)** — define and track progress toward financial targets
- 🚧 **Spending Limits** — budget control with configurable category-level limits
- 📬 **Email Notifications** — transactional emails via Spring Mail
- 🔄 **Scheduled Tasks** — distributed cron jobs with locking to prevent duplicate execution across instances
- ⚙️ **API Documentation** — interactive Swagger UI via SpringDoc OpenAPI
- 🛡️ **Rate Limiting** — token-bucket based request throttling
- 🌐 **External HTTP Clients** — declarative REST clients via Spring Cloud OpenFeign

---

## 🏛️ Architecture & Design

Finovara follows a **microservices architecture** — each service is independently deployable, containerized, and communicates through the API Gateway.

The codebase is lightly inspired by **Domain-Driven Design (DDD)** principles — domain logic is encapsulated within dedicated services and abstractions (e.g. `NotificationCreator`, `ThresholdReachedService`), domain objects carry their own behavior, and bounded contexts are reflected in the package structure per service.

This is not strict DDD, but the influence is visible in how the domains are separated and modeled across services.

---

## 📦 Technologies

- **Java 25**
- **Spring Boot 4**
- **Spring Cloud Gateway**
- **Spring Security** (JWT)
- **Spring MVC**
- **Spring Data JPA**
- **Spring Mail**
- **Spring Cloud OpenFeign**
- **Hibernate**
- **Liquibase**
- **PostgreSQL**
- **Docker / Docker Compose**
- **Maven**
