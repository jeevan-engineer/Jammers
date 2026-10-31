# Jammers – Subscription Management System

A subscription management platform built for the **Odoo x Make-A-Ton 9.0 Hackathon 2026**.
It centralises products, plans, quotations, subscriptions, invoices, payments,
discounts and revenue reporting for a subscription business (Brightloop Software).

## Features

- **Role-based access:** Admin, Internal Staff and Customer (portal)
- **Authentication:** signup, login (JWT), forgot/reset password
- **Products and variants:** sales price, cost price, variant extra prices
- **Plans:** daily, weekly, monthly or yearly billing, minimum quantity, availability dates
- **Quotation templates and subscriptions:** Draft → Quotation → Confirmed → Active → Closed
- **Automatic invoicing:** scheduled job, no double billing, taxes and discounts applied
- **Payments:** multiple methods, partial payments, paid / outstanding / overdue tracking
- **Discounts and taxes:** fixed or percentage rules with limits and dates
- **Dashboard:** active subscriptions, revenue, amount owed, overdue invoices

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React, Vite, Axios, React Router |
| Backend | Java 21, Spring Boot 3, Spring Data JPA, Spring Security, JWT |
| Database | MySQL 8 (Flyway migrations) |
| Tools | Maven, Git, Swagger (OpenAPI) |

## 📁 Project Structure

```
Jammers/
├── frontend/     # React app (Vite)
├── backend/      # Spring Boot app
├── docs/         # ER diagram, architecture diagram, screenshots
├── .gitignore
└── README.md
```

## ⚙️ Getting Started

**Prerequisites:** JDK 21, Maven, Node.js 18+, MySQL 8

### 1. Clone the repository

```bash
git clone https://github.com/jeevan-engineer/Jammers.git
cd Jammers
```

### 2. Create the database

```sql
CREATE DATABASE brightloop CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 3. Set environment variables

| Variable | Description |
|---|---|
| `DB_USER` | MySQL username |
| `DB_PASSWORD` | MySQL password |
| `JWT_SECRET` | Long random string used to sign tokens |
| `MAIL_USER` | Email address used to send mails |
| `MAIL_PASSWORD` | App password for that email |

### 4. Run the backend

Tables are created automatically by Flyway on first start.

```bash
cd backend
./mvnw spring-boot:run
```

(On Windows use `mvnw.cmd spring-boot:run`.)

API docs: http://localhost:8080/swagger-ui.html

### 5. Run the frontend

```bash
cd frontend
npm install
npm run dev
```

App: http://localhost:5173

## 🔐 Demo Credentials

| Role | Email | Password |
|---|---|---|
| Admin | admin@jammers.com | _(add after seeding)_ |
| Staff | staff@jammers.com | _(add after seeding)_ |
| Customer | customer@jammers.com | _(add after seeding)_ |

## 📏 Key Business Rules

- One email can belong to only one account
- Password: more than 8 characters with an uppercase letter, a lowercase letter and