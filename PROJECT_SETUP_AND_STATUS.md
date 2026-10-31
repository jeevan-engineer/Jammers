# Brightloop – Project Setup & Status

> **Last updated:** 2026-10-31  
> **Stack:** React + Vite (frontend) · Spring Boot 4.1.2 + MySQL 8.4 (backend)

---

## Table of Contents
1. [Architecture](#1-architecture)
2. [Technology Stack](#2-technology-stack)
3. [Database Setup (macOS)](#3-database-setup-macos)
4. [Environment Variables](#4-environment-variables)
5. [Running the Project](#5-running-the-project)
6. [API Endpoints](#6-api-endpoints)
7. [Authentication & Roles](#7-authentication--roles)
8. [Module Status](#8-module-status)
9. [Known Issues & Limitations](#9-known-issues--limitations)
10. [Git & Collaboration](#10-git--collaboration)

---

## 1. Architecture

```
┌──────────────────────────────────────────────────────────┐
│                      Browser (React)                      │
│  Auth → Admin Dashboard / Staff Portal / Customer Portal  │
│         Axios (JWT Bearer)  |  Vite dev server :5173      │
└───────────────────────┬──────────────────────────────────┘
                        │ HTTP/REST (JSON)
┌───────────────────────▼──────────────────────────────────┐
│           Spring Boot 4.1.2  — port 8080                  │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐ │
│  │ Auth module │  │ Core modules │  │  Subscription &  │ │
│  │ (JWT, BCrypt│  │ Products     │  │  Invoice modules │ │
│  │  DataSeeder)│  │ Plans, Taxes │  │  Payments, Reports│ │
│  └─────────────┘  │ Discounts    │  └──────────────────┘ │
│                    │ Customers    │                        │
│                    │ Users        │                        │
│                    └──────────────┘                        │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  Spring Security (JWT filter) + CORS config         │  │
│  └─────────────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────┐  │
│  │  Flyway migrations (V1 → V4.2) + Hibernate validate │  │
│  └─────────────────────────────────────────────────────┘  │
└───────────────────────┬──────────────────────────────────┘
                        │ JDBC / MySQL protocol
┌───────────────────────▼──────────────────────────────────┐
│           MySQL 8.4  —  database: brightloop              │
│  19 tables: users, customers, products, plans, taxes,      │
│  discounts, subscriptions, invoices, payments, …          │
└──────────────────────────────────────────────────────────┘
```

### Package structure (backend)
| Package | Purpose |
|---|---|
| `com.brightloop.subscription.feature.*` | Auth, Customer, User management (JWT-secured) |
| `com.brightloop.subscription.security.*` | JwtService, JwtAuthFilter, UserDetails |
| `com.brightloop.subscription.config.*` | **Canonical** SecurityConfig, CorsConfig, DataSeeder |
| `com.brightloop.controller.*` | Products, Plans, Taxes, Discounts, Invoices, Payments, Subscriptions, Reports |
| `com.brightloop.service.*` + `com.brightloop.entity.*` | Core business logic + JPA entities |
| `com.brightloop.config.*` | JacksonConfig only (SecurityConfig disabled — see note) |

> **Note:** `com.brightloop.config.SecurityConfig` has been disabled (no `@Configuration`).  
> The authoritative security config is `com.brightloop.subscription.config.SecurityConfig`.

---

## 2. Technology Stack

### Frontend
| Technology | Version | Notes |
|---|---|---|
| React | 18+ | Component-based UI |
| Vite | 5+ | Dev server & bundler |
| JavaScript (ES2022) | — | No TypeScript |
| Axios | 1+ | HTTP client with JWT interceptors |
| Lucide React | — | Icons |
| Recharts | — | Charts (dashboard) |
| CSS | — | Custom CSS (no Tailwind) |

### Backend
| Technology | Version | Notes |
|---|---|---|
| Java | 21 | LTS |
| Spring Boot | 4.1.2 | Latest |
| Spring Security | 7.x | JWT stateless |
| Spring Data JPA | — | Hibernate 7.x |
| MySQL Connector/J | 8.x | |
| Flyway | — | Database migrations |
| jjwt | 0.13.0 | `jjwt-api` + `jjwt-impl` + `jjwt-jackson` |
| Lombok | 1.18.48 | Code generation |
| Springdoc OpenAPI | — | Swagger UI |
| Maven | 3.x | Build tool |

---

## 3. Database Setup (macOS)

### Prerequisites
- MySQL 8.4.x installed (Homebrew recommended)
- Java 21 installed

### Steps

```bash
# 1. Install MySQL (if not installed)
brew install mysql

# 2. Start MySQL service
brew services start mysql

# 3. Verify MySQL is running
mysql -u root -e "SELECT VERSION();"

# 4. The database is created automatically on first backend startup
#    (createDatabaseIfNotExist=true in JDBC URL)
#    No manual CREATE DATABASE needed.

# 5. Verify after startup
mysql -u root brightloop -e "SHOW TABLES;"
```

### Expected tables after first startup
```
customers, discount_products, discount_usages, discounts,
flyway_schema_history, invoice_lines, invoices, payment_methods,
payments, plans, product_variants, products, quotation_template_lines,
quotation_templates, subscription_lines, subscription_status_history,
subscriptions, taxes, users
```

### Seeded accounts (created automatically)
| Email | Password | Role |
|---|---|---|
| `admin@brightloop.com` | `Admin@1234` | ADMIN |
| `admin@jammers.com` | `Admin@1234` | ADMIN |
| `staff@jammers.com` | `Staff@1234` | INTERNAL (Staff) |
| `customer@jammers.com` | `Customer@1234` | CUSTOMER |

---

## 4. Environment Variables

The backend uses environment variable overrides. For local development, defaults work without any `.env` file.

### Backend (`application.properties` — no changes needed for local dev)
| Variable | Default | Description |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/brightloop?...` | MySQL JDBC URL |
| `DB_USER` | `root` | MySQL username |
| `DB_PASSWORD` | *(empty)* | MySQL password |
| `JWT_SECRET` | `brightloop-super-secret-key-...` | JWT signing key (min 32 chars) |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Token TTL |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Frontend origins |
| `ADMIN_EMAIL` | `admin@brightloop.com` | Seed admin email |
| `ADMIN_PASSWORD` | `Admin@1234` | Seed admin password |

### Frontend (optional — `.env.local` in `frontend/`)
```env
VITE_API_BASE_URL=http://localhost:8080/api
```
If not set, defaults to `http://localhost:8080/api` via the fallback in `src/api/axios.js`.

---

## 5. Running the Project

### Backend

```bash
# Navigate to backend
cd /Users/jeevannaik/Jammers/backend

# Build (skip tests for speed)
./mvnw package -DskipTests

# Run
java -jar target/subscription-system-0.0.1-SNAPSHOT.jar

# Or directly (compiles + runs, skips tests)
./mvnw spring-boot:run -Dmaven.test.skip=true
```

- Server starts on **http://localhost:8080**
- Swagger UI: **http://localhost:8080/swagger-ui.html**
- API docs: **http://localhost:8080/v3/api-docs**

### Frontend

```bash
# Navigate to frontend
cd /Users/jeevannaik/Jammers/frontend

# Install dependencies (first time only)
npm install

# Start development server
npm run dev
# → http://localhost:5173

# Production build
npm run build
# Output: frontend/dist/
```

### Start both together (two terminals)

**Terminal 1 — Backend:**
```bash
cd /Users/jeevannaik/Jammers/backend
java -jar target/subscription-system-0.0.1-SNAPSHOT.jar
```

**Terminal 2 — Frontend:**
```bash
cd /Users/jeevannaik/Jammers/frontend
npm run dev
```

Open **http://localhost:5173** in your browser.

---

## 6. API Endpoints

All endpoints require `Authorization: Bearer <jwt>` except auth routes.

### Authentication
| Method | Path | Description | Auth |
|---|---|---|---|
| POST | `/api/auth/login` | Login — returns JWT | Public |
| POST | `/api/auth/signup` | Register new customer account | Public |
| GET | `/api/auth/me` | Current user profile | JWT |

**Login request:**
```json
{ "email": "admin@brightloop.com", "password": "Admin@1234" }
```
**Login response:**
```json
{
  "token": "eyJ...",
  "tokenType": "Bearer",
  "expiresIn": 86400000,
  "userId": 1,
  "fullName": "System Administrator",
  "email": "admin@brightloop.com",
  "role": "ADMIN"
}
```

### Products — `/api/products`
| Method | Path | Description |
|---|---|---|
| GET | `/api/products?activeOnly=true&page=0&size=10` | Paginated list |
| POST | `/api/products` | Create product |
| GET | `/api/products/{id}` | Get by ID |
| PUT | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Deactivate product |

**Product body:** `{ name, type: "SERVICE"|"CONSUMABLE"|"STORABLE", salesPrice, costPrice, notes, active }`

### Plans — `/api/plans`
| Method | Path | Description |
|---|---|---|
| GET | `/api/plans?productId=&activeOnly=true` | Paginated list |
| POST | `/api/plans` | Create plan |
| GET | `/api/plans/{id}` | Get by ID |
| PUT | `/api/plans/{id}` | Update plan |

**Plan body:** `{ productId, name, price, billingPeriod: "MONTHLY"|"QUARTERLY"|"YEARLY"|"WEEKLY", minQuantity, active }`

### Taxes — `/api/taxes`
| Method | Path | Description |
|---|---|---|
| GET | `/api/taxes?activeOnly=true` | All taxes |
| POST | `/api/taxes` | Create tax |
| GET | `/api/taxes/{id}` | Get by ID |
| PUT | `/api/taxes/{id}` | Update tax |
| DELETE | `/api/taxes/{id}` | Deactivate |
| POST | `/api/taxes/{id}/calculate` | Calculate tax on amount |

**Tax body:** `{ name, percentage (0–100), applicability: "SERVICE"|"CONSUMABLE"|"STORABLE", active }`

### Discounts — `/api/discounts`
| Method | Path | Description |
|---|---|---|
| GET | `/api/discounts?activeOnly=true` | Paginated list |
| POST | `/api/discounts` | Create discount |
| GET | `/api/discounts/{id}` | Get by ID |
| GET | `/api/discounts/code/{code}` | Look up by code |
| PUT | `/api/discounts/{id}` | Update |
| DELETE | `/api/discounts/{id}` | Deactivate |

### Customers — `/api/customers` *(ADMIN/INTERNAL only)*
| Method | Path | Description |
|---|---|---|
| GET | `/api/customers?search=&active=true&page=0&size=20` | Paginated list |
| POST | `/api/customers` | Create customer profile |
| GET | `/api/customers/{id}` | Get by ID |
| PUT | `/api/customers/{id}` | Update |
| GET | `/api/customers/me` | Current customer's own profile |

### Users — `/api/users` *(ADMIN only)*
| Method | Path | Description |
|---|---|---|
| GET | `/api/users?page=0&size=20` | All users |
| POST | `/api/users/staff` | Create staff user |
| GET | `/api/users/{id}` | Get by ID |
| PUT | `/api/users/{id}` | Update |
| PATCH | `/api/users/{id}/role` | Change role |
| DELETE | `/api/users/{id}` | Deactivate |

### Subscriptions — `/api/v1/subscriptions`
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/subscriptions?status=&customerId=` | Paginated list |
| POST | `/api/v1/subscriptions` | Create subscription |
| GET | `/api/v1/subscriptions/{id}` | Get by ID |
| GET | `/api/v1/subscriptions/number/{number}` | Get by subscription number |
| PATCH | `/api/v1/subscriptions/{id}/status` | Update status |
| POST | `/api/v1/subscriptions/{id}/close` | Close |
| POST | `/api/v1/subscriptions/{id}/pause` | Pause |
| POST | `/api/v1/subscriptions/{id}/resume` | Resume |
| POST | `/api/v1/subscriptions/{id}/renew` | Renew |
| POST | `/api/v1/subscriptions/{id}/generate-invoice` | Generate invoice |
| POST | `/api/v1/subscriptions/generate-billing` | Bulk billing run |

### Invoices — `/api/v1/invoices`
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/invoices?status=&customerId=` | Paginated list |
| POST | `/api/v1/invoices` | Create manual invoice |
| GET | `/api/v1/invoices/{id}` | Get by ID |
| GET | `/api/v1/invoices/number/{number}` | Get by invoice number |
| GET | `/api/v1/invoices/subscription/{id}` | Invoices for subscription |
| GET | `/api/v1/invoices/overdue` | Overdue invoices |
| PATCH | `/api/v1/invoices/{id}/status` | Update status |
| POST | `/api/v1/invoices/{id}/confirm` | Confirm draft |
| POST | `/api/v1/invoices/{id}/cancel` | Cancel |
| GET | `/api/v1/invoices/{id}/print` | Printable text |
| POST | `/api/v1/invoices/{id}/send` | Send notification |

### Payments — `/api/v1/payments` *(via PaymentController)*
| Method | Path | Description |
|---|---|---|
| POST | `/api/v1/payments` | Record payment |
| GET | `/api/v1/payments/invoice/{id}` | Payments for invoice |

### Reports — `/api/v1/reports`
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/reports/dashboard` | Dashboard summary |
| GET | `/api/v1/reports/payments` | Payment history (paginated) |

**Dashboard response fields:** `totalSubscriptions, activeSubscriptions, totalRevenue, amountOwed, monthlyRecurringRevenue, totalInvoices, paidInvoices, unpaidInvoices, overdueInvoices, recentActivities`

### Product Variants — `/api/v1/products/{id}/variants`
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/products/{id}/variants` | List variants |
| POST | `/api/v1/products/{id}/variants` | Create variant |
| PUT | `/api/v1/products/{productId}/variants/{variantId}` | Update |
| DELETE | `/api/v1/products/{productId}/variants/{variantId}` | Deactivate |

---

## 7. Authentication & Roles

### JWT Flow
```
1. POST /api/auth/login → { token, role, ... }
2. Store token in localStorage (key: "bl_token")
3. All subsequent requests: Authorization: Bearer <token>
4. Token expiry: 24 hours (86400000ms)
5. On 401: auto-logout + redirect to login
```

### Role Matrix
| Role | Enum | Frontend | Backend access |
|---|---|---|---|
| `ADMIN` | `Role.ADMIN` | Admin dashboard (full) | All endpoints |
| `INTERNAL` | `Role.INTERNAL` | Staff portal | Customers, Subscriptions, Invoices, Payments |
| `CUSTOMER` | `Role.CUSTOMER` | Customer portal | Own subscription, own invoices, own profile |

### Demo Credentials
| Role | Email | Password |
|---|---|---|
| Admin | `admin@brightloop.com` | `Admin@1234` |
| Admin (Alt) | `admin@jammers.com` | `Admin@1234` |
| Staff | `staff@jammers.com` | `Staff@1234` |
| Customer | `customer@jammers.com` | `Customer@1234` |

---

## 8. Module Status

### ✅ Fully Working

| Module | Backend | Frontend | Notes |
|---|---|---|---|
| Authentication (login) | ✅ JWT | ✅ Real API | `POST /api/auth/login` |
| Authentication (signup) | ✅ | ✅ Real API | Creates CUSTOMER + profile |
| Session persistence | — | ✅ localStorage | Survives page refresh |
| Role-based dashboards | — | ✅ | Admin, Staff, Customer portals |
| Products CRUD | ✅ | ✅ Real API | Paginated, search, create, deactivate |
| Plans CRUD | ✅ | ✅ Real API | Linked to products |
| Taxes CRUD | ✅ | ✅ Real API | + tax calculation endpoint |
| Discounts CRUD | ✅ | ✅ Real API | Code lookup, usage tracking |
| Customers CRUD | ✅ | ✅ Real API | Admin/Internal only |
| Users management | ✅ | ✅ Real API | Admin only |
| Database migrations | ✅ All 6 applied | — | V1–V4.2 + V3 (taxes/discounts) |
| Flyway | ✅ | — | 19 tables created |
| Hibernate validate | ✅ | — | All entities match schema |
| Dashboard report | ✅ | ⚠️ Partial | `/api/v1/reports/dashboard` wired, stats show — |

### ⚠️ Partially Implemented

| Module | Backend | Frontend | What's Missing |
|---|---|---|---|
| Subscriptions | ✅ Full backend | ⚠️ Sample data | Wire GET/POST to `/api/v1/subscriptions` |
| Invoices | ✅ Full backend | ⚠️ Sample data | Wire to `/api/v1/invoices` |
| Payments | ✅ Full backend | ⚠️ Sample data | Wire to `/api/v1/payments` |
| Dashboard stats | ✅ `/api/v1/reports/dashboard` | ⚠️ Shows "—" | Fetch real totals and render |
| Product Variants | ✅ Full backend | ❌ Not in UI | No UI for variants in Products page |
| Quotation Templates | ✅ Full backend | ❌ Not in UI | No UI created |
| Payment Methods | ✅ Schema + entities | ❌ No controller | `PaymentMethodController` not created |

### ❌ Not Yet Implemented (Frontend only)

| Feature | Notes |
|---|---|
| Dashboard live stats | Replace "—" with `/api/v1/reports/dashboard` data |
| Subscription create form | Complex multi-step (customer + plan + dates) |
| Invoice generate / send UI | Backend ready, frontend form needed |
| Payment recording UI | Backend ready, needs form |
| Staff portal API wiring | Uses sample data; wire to customers/subscriptions endpoints |
| Customer portal API wiring | Uses sample data; wire to `/api/customers/me`, invoices |
| Forgot password | Not implemented in backend |
| Email notifications | Backend wired to mail config but no real mail server |

---

## 9. Known Issues & Limitations

### Backend

1. **Duplicate package architectures** — `com.brightloop.*` (members 3 & 4) and `com.brightloop.subscription.*` (members 1 & 2) coexist. This works because `spring.main.allow-bean-definition-overriding=true` and SecurityConfig uses `@Configuration("subscriptionSecurityConfig")`. Not a bug, but increases complexity.

2. **`com.brightloop.config.SecurityConfig` disabled** — This class was stripped of `@Configuration` to prevent duplicate `SecurityFilterChain` bean conflict. The active one is `com.brightloop.subscription.config.SecurityConfig`.

3. **Missing V3 migration** was created as `V3__taxes_discounts_product_variants.sql` — this was not in the original repo.

4. **`jjwt-jackson` missing from original pom.xml** — Added `jjwt-api`, `jjwt-impl` (runtime), and `jjwt-jackson` (runtime). Without `jjwt-jackson`, JWT signing threw `UnavailableImplementationException`.

5. **Test class import errors** — Both `HighVolumePerformanceTest` and `SubscriptionBillingIntegrationTest` used wrong `AutoConfigureMockMvc` import package for Spring Boot 4.x. Fixed to `org.springframework.boot.webmvc.test.autoconfigure`.

6. **`/api/reports/dashboard`** → wrong path. Correct: `/api/v1/reports/dashboard`.

7. **Mail config** — `spring.mail.password=change_me` — email sending will fail unless a real SMTP server is configured.

### Frontend

1. **No React Router** — navigation is `useState("Dashboard")`. Adding React Router later is straightforward but requires refactoring all navigation.

2. **Staff and Customer portals** — currently use sample data from `src/data/staffData.js` and `src/data/customerData.js`. Backend integration pending.

3. **No error boundary** — uncaught React errors will crash the whole page.

4. **LocalStorage JWT** — Acceptable for a hackathon demo; production should use `httpOnly` cookies.

---

## 10. Git & Collaboration

```bash
# Pull latest from teammates
git fetch origin
git merge origin/main

# Check your uncommitted changes
git status
git diff

# Stage and commit your changes
git add -A
git commit -m "feat: describe your changes"

# Push (only after review)
git push origin main
```

### Branch naming convention (recommended)
- `feature/your-name/feature-name`
- `fix/your-name/bug-description`

### ⚠️ Rules
- **Never** `git reset --hard` or `git push --force`
- **Never** delete teammates' files without discussion
- Always `git fetch && git merge` before starting work
- Resolve merge conflicts carefully — check both sides before choosing

---

## Commit History Summary (latest 10)
```
66d7e2e  Merge branch 'main' — teammate merge
a274a19  feat: add tax management and product variants
5f60339  feat: subscription billing, invoice, payment, reporting (member 4)
15160b3  feat: role-based dashboards for admin, staff, customers
0359a5a  feat: db V2 migration (products and plans)
b9df53d  chore: stop tracking build output (target/)
ac44c5b  Merge branch 'main'
4bcab69  Add discount module
74751b3  Add discount module
a8c042a  Merge branch 'main'
```

