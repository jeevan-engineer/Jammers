# BrightLoop – Frontend API Integration Guide

> **Module:** Member 4 – Subscription Management, Automated Billing & Payment Processing  
> **Generated from:** Actual backend source code inspection (not assumptions)  
> **Last Updated:** October 2026

---

## Table of Contents

1. [Backend Overview](#1-backend-overview)
2. [Environment & Connection Details](#2-environment--connection-details)
3. [Authentication & Security](#3-authentication--security)
4. [CORS Configuration](#4-cors-configuration)
5. [Axios Client Setup](#5-axios-client-setup)
6. [Error Response Format](#6-error-response-format)
7. [Enum Reference](#7-enum-reference)
8. [Module 1 – Subscriptions API](#8-module-1--subscriptions-api)
9. [Module 2 – Invoices API](#9-module-2--invoices-api)
10. [Module 3 – Payments API](#10-module-3--payments-api)
11. [Quotation Templates API (Supporting)](#11-quotation-templates-api-supporting)
12. [Reports / Dashboard API (Supporting)](#12-reports--dashboard-api-supporting)
13. [Inter-Module Relationships](#13-inter-module-relationships)
14. [Pagination Reference](#14-pagination-reference)
15. [Complete API Reference Table](#15-complete-api-reference-table)
16. [Missing Implementations & Blockers](#16-missing-implementations--blockers)

---

## 1. Backend Overview

| Property           | Value                                    |
|--------------------|------------------------------------------|
| Framework          | Spring Boot 4.1.2 (Spring 7)            |
| Language           | Java 21                                  |
| ORM                | Spring Data JPA / Hibernate              |
| Database           | MySQL 9.0 (`brightloop`)                |
| Migration          | Flyway                                   |
| Build Tool         | Maven (wrapper: `mvnw.cmd`)             |
| Port               | `8080` (default)                         |

---

## 2. Environment & Connection Details

| Item                | Value                                         |
|---------------------|-----------------------------------------------|
| **Base URL**        | `http://localhost:8080`                       |
| **API Base Path**   | `/api/v1`                                     |
| **Full API Root**   | `http://localhost:8080/api/v1`               |
| **Content-Type**    | `application/json`                            |
| **Date Format**     | ISO 8601 — `YYYY-MM-DD` for dates, `YYYY-MM-DDTHH:mm:ss` for timestamps |
| **Monetary Fields** | `BigDecimal` serialized as JSON numbers (scale 2, rounding HALF_UP) |
| **Currency**        | Default `"USD"` — always a 3-letter ISO 4217 string |

---

## 3. Authentication & Security

The backend security filter chain (`SecurityConfig.java`) is configured to **permit all requests** to `/api/v1/**`:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/v1/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
    .anyRequest().permitAll()
);
```

**What this means for the frontend developer:**

- **No authentication headers are required** for any `/api/v1` endpoint.
- No JWT token, no Basic Auth, no session cookies needed.
- CSRF protection is explicitly disabled.
- You can call all endpoints directly without any `Authorization` header.

> **Note:** If authentication is added later, the team will need to implement a login endpoint and pass a `Bearer` token in the `Authorization` header.

---

## 4. CORS Configuration

Every controller uses:

```java
@CrossOrigin(origins = "*")
```

This means:
- **All origins are allowed** — your Vite dev server (`http://localhost:5173`) will work without issues.
- No special proxy configuration is needed in `vite.config.js`.
- All HTTP methods (GET, POST, PUT, PATCH, DELETE) are permitted.

---

## 5. Axios Client Setup

### Install Axios

```bash
npm install axios
```

### Create API Client — `src/api/client.js`

```javascript
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 15000,
});

// Response interceptor for centralized error handling
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const { status, data } = error.response;
      console.error(`API Error [${status}]:`, data.message || data);

      if (status === 400 && data.validationErrors) {
        console.error('Validation Errors:', data.validationErrors);
      }
    } else if (error.request) {
      console.error('Network Error: No response from backend. Is the server running?');
    }
    return Promise.reject(error);
  }
);

export default apiClient;
```

---

## 6. Error Response Format

All errors follow a consistent JSON structure from `GlobalExceptionHandler`:

### Validation Error (400)

```json
{
  "timestamp": "2026-10-31T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Input payload failed validation constraints",
  "validationErrors": {
    "customerId": "Customer ID is required",
    "lines": "Subscription must have at least one line item"
  },
  "path": "/api/v1/subscriptions"
}
```

### Business Logic Error (400)

```json
{
  "timestamp": "2026-10-31T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid status transition from DRAFT to ACTIVE",
  "path": "/api/v1/subscriptions/1/status"
}
```

### Not Found Error (404)

```json
{
  "timestamp": "2026-10-31T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Subscription not found with id: 999",
  "path": "/api/v1/subscriptions/999"
}
```

### Duplicate Record Error (400)

```json
{
  "timestamp": "2026-10-31T10:30:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Duplicate billing error: An invoice already exists for this subscription and billing period.",
  "path": "/api/v1/invoices"
}
```

### Internal Server Error (500)

```json
{
  "timestamp": "2026-10-31T10:30:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected server error occurred",
  "path": "/api/v1/subscriptions"
}
```

---

## 7. Enum Reference

### SubscriptionStatus

| Value        | Display Name | Description                                                |
|--------------|-------------|------------------------------------------------------------|
| `DRAFT`      | Draft       | Initial draft of the subscription contract                 |
| `QUOTATION`  | Quotation   | Proposed quote sent to customer for review and approval    |
| `CONFIRMED`  | Confirmed   | Customer accepted quote; awaiting activation or first cycle|
| `ACTIVE`     | Active      | Subscription is currently active and in billing lifecycle  |
| `CLOSED`     | Closed      | Subscription has ended, expired, or been terminated        |

**Valid Transitions:**
- `DRAFT` → `QUOTATION` or `CLOSED`
- `QUOTATION` → `CONFIRMED` or `CLOSED`
- `CONFIRMED` → `ACTIVE` or `CLOSED`
- `ACTIVE` → `CLOSED`
- `CLOSED` → *(terminal — no further transitions)*

### InvoiceStatus

| Value            | Display Name   | Description                                    |
|------------------|---------------|------------------------------------------------|
| `DRAFT`          | Draft         | Invoice is being drafted and can be edited     |
| `CONFIRMED`      | Confirmed     | Invoice is confirmed and finalized             |
| `UNPAID`         | Unpaid        | Invoice is posted/issued and awaiting payment  |
| `PARTIALLY_PAID` | Partially Paid| Invoice has received partial payment           |
| `PAID`           | Paid          | Invoice has been paid in full                  |
| `CANCELLED`      | Cancelled     | Invoice has been cancelled/voided              |

**Payable statuses** (can receive payments): `CONFIRMED`, `UNPAID`, `PARTIALLY_PAID`

### PaymentMethodType

| Value           | Display Name       |
|-----------------|--------------------|
| `CASH`          | Cash               |
| `CARD`          | Card (Credit/Debit)|
| `CREDIT_CARD`   | Credit Card        |
| `BANK_TRANSFER` | Bank Transfer      |
| `UPI`           | UPI                |
| `OTHER`         | Other              |

---

## 8. Module 1 – Subscriptions API

**Base Path:** `/api/v1/subscriptions`

---

### 8.1 Create Subscription

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/subscriptions` |
| **Status** | `201 Created` |

#### Request Body — `SubscriptionCreateRequest`

| Field                | Type               | Required | Default     | Validation                             |
|----------------------|--------------------|----------|-------------|----------------------------------------|
| `customerId`         | `number (Long)`    | Yes      |             | `@NotNull`                              |
| `customerName`       | `string`           | No       |             |                                         |
| `customerEmail`      | `string`           | No       |             |                                         |
| `quotationTemplateId`| `number (Long)`    | No       |             | Links to a Quotation Template           |
| `paymentMethodId`    | `number (Long)`    | No       |             |                                         |
| `status`             | `string (enum)`    | No       | `"DRAFT"`   | One of: `DRAFT`, `QUOTATION`, `CONFIRMED`, `ACTIVE`, `CLOSED` |
| `billingInterval`    | `string`           | No       | `"MONTHLY"` |                                         |
| `intervalCount`      | `number (Integer)` | No       | `1`         |                                         |
| `startDate`          | `string (date)`    | No       |             | ISO format: `YYYY-MM-DD`               |
| `endDate`            | `string (date)`    | No       |             | ISO format: `YYYY-MM-DD`               |
| `trialStartDate`     | `string (date)`    | No       |             |                                         |
| `trialEndDate`       | `string (date)`    | No       |             |                                         |
| `quoteValidUntil`    | `string (date)`    | No       |             |                                         |
| `currency`           | `string`           | No       | `"USD"`     |                                         |
| `autoRenew`          | `boolean`          | No       | `true`      |                                         |
| `terms`              | `string`           | No       |             |                                         |
| `notes`              | `string`           | No       |             |                                         |
| `lines`              | `array`            | Yes      | `[]`        | `@NotEmpty` — at least one line item    |

#### `lines[]` — `SubscriptionLineDto`

| Field         | Type            | Required | Default | Validation                    |
|---------------|-----------------|----------|---------|-------------------------------|
| `productId`   | `number (Long)` | No       |         |                               |
| `productName` | `string`        | Yes      |         | `@NotBlank`                   |
| `description` | `string`        | No       |         |                               |
| `quantity`    | `number`        | Yes      | `1`     | `@DecimalMin("0.01")`         |
| `unitPrice`   | `number`        | Yes      | `0`     | `@DecimalMin("0.00")`         |
| `discount`    | `number`        | No       | `0`     |                               |
| `taxRate`     | `number`        | No       | `0`     |                               |

#### Axios Example

```javascript
import apiClient from './client';

const createSubscription = async () => {
  const response = await apiClient.post('/subscriptions', {
    customerId: 1,
    customerName: "Acme Corp",
    customerEmail: "billing@acme.com",
    billingInterval: "MONTHLY",
    intervalCount: 1,
    startDate: "2026-11-01",
    endDate: "2027-10-31",
    currency: "USD",
    autoRenew: true,
    lines: [
      {
        productName: "Enterprise License",
        description: "Annual enterprise subscription",
        quantity: 1,
        unitPrice: 499.99,
        discount: 10,
        taxRate: 18
      }
    ]
  });
  console.log('Created:', response.data);
  return response.data; // SubscriptionResponse
};
```

#### Response — `SubscriptionResponse`

```json
{
  "id": 1,
  "subscriptionNumber": "SUB-2026-A1B2C",
  "customerId": 1,
  "customerName": "Acme Corp",
  "customerEmail": "billing@acme.com",
  "quotationTemplateId": null,
  "quotationTemplateName": null,
  "paymentMethodId": null,
  "status": "DRAFT",
  "billingInterval": "MONTHLY",
  "intervalCount": 1,
  "startDate": "2026-11-01",
  "endDate": "2027-10-31",
  "trialStartDate": null,
  "trialEndDate": null,
  "currentPeriodStart": "2026-11-01",
  "currentPeriodEnd": "2026-11-30",
  "nextBillingDate": "2026-11-01",
  "cancelledAt": null,
  "cancellationReason": null,
  "pausedAt": null,
  "quoteValidUntil": null,
  "subtotal": 449.99,
  "taxAmount": 81.00,
  "discountAmount": 50.00,
  "totalAmount": 530.99,
  "currency": "USD",
  "autoRenew": true,
  "terms": null,
  "notes": null,
  "lines": [
    {
      "id": 1,
      "productId": null,
      "productName": "Enterprise License",
      "description": "Annual enterprise subscription",
      "quantity": 1,
      "unitPrice": 499.99,
      "discount": 10,
      "taxRate": 18,
      "taxAmount": 81.00,
      "subtotal": 449.99,
      "totalAmount": 530.99
    }
  ],
  "statusHistories": [
    {
      "id": 1,
      "previousStatus": null,
      "newStatus": "DRAFT",
      "changedAt": "2026-10-31T10:30:00",
      "changedBy": "SYSTEM",
      "reason": "Subscription created",
      "comment": null
    }
  ],
  "createdAt": "2026-10-31T10:30:00",
  "updatedAt": "2026-10-31T10:30:00"
}
```

---

### 8.2 List All Subscriptions (Paginated)

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/subscriptions` |
| **Status** | `200 OK` |

#### Query Parameters

| Param        | Type     | Required | Default | Description                          |
|-------------|----------|----------|---------|--------------------------------------|
| `status`    | `string` | No       |         | Filter by `SubscriptionStatus` enum  |
| `customerId`| `number` | No       |         | Filter by customer ID                |
| `page`      | `number` | No       | `0`     | Zero-indexed page number             |
| `size`      | `number` | No       | `20`    | Results per page                     |
| `sort`      | `string` | No       |         | e.g. `createdAt,desc`               |

#### Axios Example

```javascript
const listSubscriptions = async (status, customerId, page = 0, size = 20) => {
  const params = { page, size };
  if (status) params.status = status;
  if (customerId) params.customerId = customerId;

  const response = await apiClient.get('/subscriptions', { params });
  return response.data; // Page<SubscriptionResponse>
};
```

---

### 8.3 Get Subscription by ID

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/subscriptions/{id}` |
| **Status** | `200 OK` |

```javascript
const getSubscription = async (id) => {
  const response = await apiClient.get(`/subscriptions/${id}`);
  return response.data; // SubscriptionResponse
};
```

---

### 8.4 Get Subscription by Number

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/subscriptions/number/{subscriptionNumber}` |
| **Status** | `200 OK` |

```javascript
const getSubscriptionByNumber = async (subscriptionNumber) => {
  const response = await apiClient.get(`/subscriptions/number/${subscriptionNumber}`);
  return response.data; // SubscriptionResponse
};
```

---

### 8.5 Update Subscription Status (Lifecycle Transition)

| Property | Value |
|----------|-------|
| **Method** | `PATCH` |
| **URL** | `/api/v1/subscriptions/{id}/status` |
| **Status** | `200 OK` |

#### Request Body — `SubscriptionStatusUpdateRequest`

| Field       | Type            | Required | Description                           |
|-------------|-----------------|----------|---------------------------------------|
| `status`    | `string (enum)` | Yes      | Target status (must be a valid transition) |
| `changedBy` | `string`        | No       | Who initiated the change               |
| `reason`    | `string`        | No       | Reason for the transition              |
| `comment`   | `string`        | No       | Additional comments                    |

#### Axios Example

```javascript
const advanceSubscriptionStatus = async (id, status, reason) => {
  const response = await apiClient.patch(`/subscriptions/${id}/status`, {
    status: status,   // e.g. "QUOTATION", "CONFIRMED", "ACTIVE"
    changedBy: "admin",
    reason: reason || "Status updated via UI"
  });
  return response.data; // SubscriptionResponse
};

// Example: DRAFT -> QUOTATION -> CONFIRMED -> ACTIVE
await advanceSubscriptionStatus(1, "QUOTATION", "Quote sent to customer");
await advanceSubscriptionStatus(1, "CONFIRMED", "Customer accepted");
await advanceSubscriptionStatus(1, "ACTIVE", "Billing cycle started");
```

---

### 8.6 Close Subscription

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/subscriptions/{id}/close` |
| **Status** | `200 OK` |

#### Query Parameters

| Param    | Type     | Required | Default                    |
|----------|----------|----------|----------------------------|
| `reason` | `string` | No       | `"Closed via API request"` |

```javascript
const closeSubscription = async (id, reason) => {
  const response = await apiClient.post(`/subscriptions/${id}/close`, null, {
    params: { reason: reason || "Customer requested cancellation" }
  });
  return response.data;
};
```

---

### 8.7 Pause Subscription

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/subscriptions/{id}/pause` |
| **Status** | `200 OK` |

```javascript
const pauseSubscription = async (id, reason) => {
  const response = await apiClient.post(`/subscriptions/${id}/pause`, null, {
    params: { reason: reason || "Temporary pause" }
  });
  return response.data;
};
```

---

### 8.8 Resume Subscription

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/subscriptions/{id}/resume` |
| **Status** | `200 OK` |

```javascript
const resumeSubscription = async (id) => {
  const response = await apiClient.post(`/subscriptions/${id}/resume`);
  return response.data;
};
```

---

### 8.9 Renew Subscription

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/subscriptions/{id}/renew` |
| **Status** | `200 OK` |

#### Request Body — `SubscriptionRenewalRequest` (optional)

| Field          | Type            | Required | Description                         |
|----------------|-----------------|----------|-------------------------------------|
| `duration`     | `number`        | No       | New contract duration               |
| `durationUnit` | `string`        | No       | `"MONTHS"` or `"YEARS"`            |
| `newEndDate`   | `string (date)` | No       | Explicit new end date               |
| `autoRenew`    | `boolean`       | No       | Whether to auto-renew again         |
| `notes`        | `string`        | No       |                                      |

```javascript
const renewSubscription = async (id) => {
  const response = await apiClient.post(`/subscriptions/${id}/renew`, {
    duration: 12,
    durationUnit: "MONTHS",
    autoRenew: true,
    notes: "Annual renewal"
  });
  return response.data;
};
```

---

### 8.10 Generate Invoice for Single Subscription

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/subscriptions/{id}/generate-invoice` |
| **Status** | `201 Created` |

```javascript
const generateInvoice = async (subscriptionId) => {
  const response = await apiClient.post(`/subscriptions/${subscriptionId}/generate-invoice`);
  return response.data;
  // { success: true, message: "...", invoiceId: 5 }
};
```

**Response:**
```json
{
  "success": true,
  "message": "Invoice generated successfully for subscription",
  "invoiceId": 5
}
```

---

### 8.11 Batch Generate Billing Invoices

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/subscriptions/generate-billing` |
| **Status** | `200 OK` |

```javascript
const generateBillingInvoices = async () => {
  const response = await apiClient.post('/subscriptions/generate-billing');
  return response.data;
  // { success: true, message: "...", invoicesGeneratedCount: 3, invoiceIds: [5, 6, 7] }
};
```

---

## 9. Module 2 – Invoices API

**Base Path:** `/api/v1/invoices`

---

### 9.1 Create Invoice (Manual / Ad-hoc)

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/invoices` |
| **Status** | `201 Created` |

#### Request Body — `InvoiceCreateRequest`

| Field               | Type            | Required | Default   | Validation                        |
|---------------------|-----------------|----------|-----------|-----------------------------------|
| `subscriptionId`    | `number (Long)` | No       |           | Links to parent subscription      |
| `customerId`        | `number (Long)` | Yes      |           | `@NotNull`                         |
| `customerName`      | `string`        | No       |           |                                    |
| `customerEmail`     | `string`        | No       |           |                                    |
| `billingAddress`    | `string`        | No       |           |                                    |
| `billingPeriodStart`| `string (date)` | Yes      |           | `@NotNull` — ISO `YYYY-MM-DD`    |
| `billingPeriodEnd`  | `string (date)` | Yes      |           | `@NotNull` — ISO `YYYY-MM-DD`    |
| `status`            | `string (enum)` | No       | `"DRAFT"` | `InvoiceStatus` enum              |
| `issueDate`         | `string (date)` | No       |           |                                    |
| `dueDate`           | `string (date)` | No       |           |                                    |
| `currency`          | `string`        | No       | `"USD"`   |                                    |
| `notes`             | `string`        | No       |           |                                    |
| `terms`             | `string`        | No       |           |                                    |
| `lines`             | `array`         | Yes      | `[]`      | `@NotEmpty` — at least one line   |

#### `lines[]` — `InvoiceLineDto`

| Field         | Type            | Required | Default | Validation            |
|---------------|-----------------|----------|---------|-----------------------|
| `productId`   | `number (Long)` | No       |         |                       |
| `productName` | `string`        | Yes      |         | `@NotBlank`           |
| `description` | `string`        | No       |         |                       |
| `quantity`    | `number`        | Yes      | `1`     | `@DecimalMin("0.01")` |
| `unitPrice`   | `number`        | Yes      | `0`     | `@DecimalMin("0.00")` |
| `discount`    | `number`        | No       | `0`     |                       |
| `taxRate`     | `number`        | No       | `0`     |                       |

#### Axios Example

```javascript
const createInvoice = async () => {
  const response = await apiClient.post('/invoices', {
    subscriptionId: 1,
    customerId: 1,
    customerName: "Acme Corp",
    customerEmail: "billing@acme.com",
    billingPeriodStart: "2026-11-01",
    billingPeriodEnd: "2026-11-30",
    issueDate: "2026-11-01",
    dueDate: "2026-11-15",
    currency: "USD",
    lines: [
      {
        productName: "Enterprise License",
        description: "Monthly billing",
        quantity: 1,
        unitPrice: 499.99,
        discount: 10,
        taxRate: 18
      }
    ]
  });
  return response.data; // InvoiceResponse
};
```

#### Response — `InvoiceResponse`

```json
{
  "id": 1,
  "invoiceNumber": "INV-2026-X7Y8Z",
  "subscriptionId": 1,
  "subscriptionNumber": "SUB-2026-A1B2C",
  "customerId": 1,
  "customerName": "Acme Corp",
  "customerEmail": "billing@acme.com",
  "billingAddress": null,
  "billingPeriodStart": "2026-11-01",
  "billingPeriodEnd": "2026-11-30",
  "status": "DRAFT",
  "issueDate": "2026-11-01",
  "dueDate": "2026-11-15",
  "paidAt": null,
  "subtotal": 449.99,
  "taxAmount": 81.00,
  "discountAmount": 50.00,
  "totalAmount": 530.99,
  "amountPaid": 0.00,
  "amountDue": 530.99,
  "currency": "USD",
  "notes": null,
  "terms": null,
  "lines": [],
  "payments": [],
  "createdAt": "2026-10-31T10:30:00",
  "updatedAt": "2026-10-31T10:30:00"
}
```

---

### 9.2 List All Invoices (Paginated)

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/invoices` |
| **Status** | `200 OK` |

#### Query Parameters

| Param        | Type     | Required | Default |
|-------------|----------|----------|---------|
| `status`    | `string` | No       |         |
| `customerId`| `number` | No       |         |
| `page`      | `number` | No       | `0`     |
| `size`      | `number` | No       | `20`    |
| `sort`      | `string` | No       |         |

```javascript
const listInvoices = async (status, customerId, page = 0, size = 20) => {
  const params = { page, size };
  if (status) params.status = status;
  if (customerId) params.customerId = customerId;

  const response = await apiClient.get('/invoices', { params });
  return response.data; // Page<InvoiceResponse>
};
```

---

### 9.3 Get Invoice by ID

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/invoices/{id}` |
| **Status** | `200 OK` |

```javascript
const getInvoice = async (id) => {
  const response = await apiClient.get(`/invoices/${id}`);
  return response.data; // InvoiceResponse (includes nested payments[])
};
```

---

### 9.4 Get Invoice by Number

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/invoices/number/{invoiceNumber}` |
| **Status** | `200 OK` |

```javascript
const getInvoiceByNumber = async (invoiceNumber) => {
  const response = await apiClient.get(`/invoices/number/${invoiceNumber}`);
  return response.data;
};
```

---

### 9.5 Get Invoices by Subscription ID

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/invoices/subscription/{subscriptionId}` |
| **Status** | `200 OK` |

Returns `InvoiceResponse[]` (not paginated).

```javascript
const getInvoicesBySubscription = async (subscriptionId) => {
  const response = await apiClient.get(`/invoices/subscription/${subscriptionId}`);
  return response.data; // InvoiceResponse[]
};
```

---

### 9.6 Get Overdue Invoices

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/invoices/overdue` |
| **Status** | `200 OK` |

Returns `InvoiceResponse[]` (not paginated).

```javascript
const getOverdueInvoices = async () => {
  const response = await apiClient.get('/invoices/overdue');
  return response.data; // InvoiceResponse[]
};
```

---

### 9.7 Update Invoice Status (PATCH)

| Property | Value |
|----------|-------|
| **Method** | `PATCH` |
| **URL** | `/api/v1/invoices/{id}/status` |
| **Status** | `200 OK` |

#### Request Body

| Field    | Type     | Required | Description                              |
|----------|----------|----------|------------------------------------------|
| `status` | `string` | Yes      | Either `"CONFIRMED"` or `"CANCELLED"`   |
| `reason` | `string` | No       | Required context when cancelling         |

> **Important:** Only `CONFIRMED` and `CANCELLED` are supported via this PATCH endpoint. Other status transitions happen automatically (e.g., payments trigger `UNPAID` -> `PARTIALLY_PAID` -> `PAID`).

```javascript
// Confirm an invoice
const confirmInvoice = async (id) => {
  const response = await apiClient.patch(`/invoices/${id}/status`, {
    status: "CONFIRMED"
  });
  return response.data;
};

// Cancel an invoice
const cancelInvoice = async (id, reason) => {
  const response = await apiClient.patch(`/invoices/${id}/status`, {
    status: "CANCELLED",
    reason: reason || "Cancelled by admin"
  });
  return response.data;
};
```

---

### 9.8 Confirm Invoice (Convenience Endpoint)

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/invoices/{id}/confirm` |
| **Status** | `200 OK` |

```javascript
const confirmInvoice = async (id) => {
  const response = await apiClient.post(`/invoices/${id}/confirm`);
  return response.data;
};
```

---

### 9.9 Cancel Invoice (Convenience Endpoint)

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/invoices/{id}/cancel` |
| **Status** | `200 OK` |

| Param    | Type     | Required | Default               |
|----------|----------|----------|-----------------------|
| `reason` | `string` | No       | `"Cancelled via API"` |

```javascript
const cancelInvoice = async (id, reason) => {
  const response = await apiClient.post(`/invoices/${id}/cancel`, null, {
    params: { reason }
  });
  return response.data;
};
```

---

### 9.10 Print Invoice (Plain Text)

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/invoices/{id}/print` |
| **Content-Type** | `text/plain` |
| **Status** | `200 OK` |

Returns a printable ASCII-formatted invoice string.

```javascript
const printInvoice = async (id) => {
  const response = await apiClient.get(`/invoices/${id}/print`, {
    responseType: 'text',
    headers: { 'Accept': 'text/plain' }
  });
  return response.data; // plain text string
};
```

---

### 9.11 Send Invoice Notification

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/invoices/{id}/send` |
| **Status** | `200 OK` |

```javascript
const sendInvoice = async (id) => {
  const response = await apiClient.post(`/invoices/${id}/send`);
  return response.data;
  // { success: true, message: "Invoice notification dispatched successfully" }
};
```

---

## 10. Module 3 – Payments API

**Base Path:** `/api/v1/payments`

---

### 10.1 Record Payment

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/payments` |
| **Status** | `201 Created` |

#### Request Body — `PaymentRecordRequest`

| Field                  | Type              | Required | Default | Validation                    |
|------------------------|-------------------|----------|---------|-------------------------------|
| `invoiceId`            | `number (Long)`   | Yes      |         | `@NotNull`                    |
| `paymentMethodId`      | `number (Long)`   | No       |         |                               |
| `customerId`           | `number (Long)`   | Yes      |         | `@NotNull`                    |
| `amount`               | `number`          | Yes      |         | `@DecimalMin("0.01")`         |
| `currency`             | `string`          | No       | `"USD"` |                               |
| `paymentDate`          | `string (datetime)`| No      |         | ISO format                    |
| `transactionReference` | `string`          | No       |         | Must be unique if provided    |
| `gatewayResponse`      | `string`          | No       |         |                               |
| `notes`                | `string`          | No       |         |                               |

> **Business Logic:** Recording a payment automatically updates the parent invoice:
> - If `amountPaid >= totalAmount` then Invoice status becomes `PAID`
> - If `amountPaid > 0 && amountPaid < totalAmount` then Invoice status becomes `PARTIALLY_PAID`
> - The invoice must be in a payable status (`CONFIRMED`, `UNPAID`, or `PARTIALLY_PAID`)

#### Axios Example

```javascript
const recordPayment = async (invoiceId, customerId, amount) => {
  const response = await apiClient.post('/payments', {
    invoiceId: invoiceId,
    customerId: customerId,
    amount: amount,
    currency: "USD",
    paymentDate: new Date().toISOString(),
    transactionReference: `TXN-${Date.now()}`,
    notes: "Payment via frontend"
  });
  return response.data; // PaymentResponse
};
```

#### Response — `PaymentResponse`

```json
{
  "id": 1,
  "paymentNumber": "PAY-2026-M9N0P",
  "invoiceId": 1,
  "invoiceNumber": "INV-2026-X7Y8Z",
  "paymentMethodId": null,
  "paymentMethodType": null,
  "customerId": 1,
  "amount": 530.99,
  "currency": "USD",
  "paymentDate": "2026-10-31T10:45:00",
  "status": "COMPLETED",
  "transactionReference": "TXN-1730000000000",
  "gatewayResponse": null,
  "notes": "Payment via frontend",
  "createdAt": "2026-10-31T10:45:00",
  "updatedAt": "2026-10-31T10:45:00"
}
```

---

### 10.2 Get Payment by ID

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/payments/{id}` |
| **Status** | `200 OK` |

```javascript
const getPayment = async (id) => {
  const response = await apiClient.get(`/payments/${id}`);
  return response.data; // PaymentResponse
};
```

---

### 10.3 Get Payment by Number

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/payments/number/{paymentNumber}` |
| **Status** | `200 OK` |

```javascript
const getPaymentByNumber = async (paymentNumber) => {
  const response = await apiClient.get(`/payments/number/${paymentNumber}`);
  return response.data;
};
```

---

### 10.4 Get Payments by Invoice ID

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/payments/invoice/{invoiceId}` |
| **Status** | `200 OK` |

Returns `PaymentResponse[]` (not paginated).

```javascript
const getPaymentsByInvoice = async (invoiceId) => {
  const response = await apiClient.get(`/payments/invoice/${invoiceId}`);
  return response.data; // PaymentResponse[]
};
```

---

### 10.5 Get Payments by Customer ID (Paginated)

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/payments/customer/{customerId}` |
| **Status** | `200 OK` |

| Param  | Type     | Required | Default |
|--------|----------|----------|---------|
| `page` | `number` | No       | `0`     |
| `size` | `number` | No       | `20`    |
| `sort` | `string` | No       |         |

```javascript
const getPaymentsByCustomer = async (customerId, page = 0, size = 20) => {
  const response = await apiClient.get(`/payments/customer/${customerId}`, {
    params: { page, size }
  });
  return response.data; // Page<PaymentResponse>
};
```

---

## 11. Quotation Templates API (Supporting)

**Base Path:** `/api/v1/quotation-templates`

Used to define reusable billing plan templates. A quotation template can be used to auto-generate a subscription with pre-filled line items.

---

### 11.1 Create Template

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/quotation-templates` |
| **Status** | `201 Created` |

#### Request Body — `QuotationTemplateDto`

| Field               | Type            | Required | Default     | Validation                     |
|---------------------|-----------------|----------|-------------|--------------------------------|
| `name`              | `string`        | Yes      |             | `@NotBlank`                     |
| `description`       | `string`        | No       |             |                                 |
| `recurringInterval` | `string`        | Yes      | `"MONTHLY"` | `@NotBlank`                     |
| `duration`          | `number`        | Yes      | `12`        | `@Min(1)`                       |
| `durationUnit`      | `string`        | Yes      | `"MONTHS"`  | `@NotBlank`                     |
| `validityDays`      | `number`        | No       | `30`        | `@Min(1)`                       |
| `autoClose`         | `boolean`       | No       | `false`     |                                 |
| `active`            | `boolean`       | No       | `true`      |                                 |
| `termsAndConditions`| `string`        | No       |             |                                 |
| `notes`             | `string`        | No       |             |                                 |
| `lines`             | `array`         | No       | `[]`        |                                 |

#### `lines[]` — `LineDto`

| Field         | Type     | Required | Default | Validation                                       |
|---------------|----------|----------|---------|--------------------------------------------------|
| `productId`   | `number` | No       |         |                                                   |
| `productName` | `string` | Yes      |         | `@NotBlank`                                       |
| `description` | `string` | No       |         |                                                   |
| `quantity`    | `number` | Yes      | `1`     | `@DecimalMin("0.01")`                             |
| `unitPrice`   | `number` | Yes      | `0`     | `@DecimalMin("0.00")`                             |
| `discount`    | `number` | No       | `0`     | `@DecimalMin("0.00")`, `@DecimalMax("100.00")`   |
| `taxRate`     | `number` | No       | `0`     | `@DecimalMin("0.00")`                             |
| `sequence`    | `number` | No       | `0`     | Display ordering                                  |

```javascript
const createTemplate = async () => {
  const response = await apiClient.post('/quotation-templates', {
    name: "Enterprise Monthly Plan",
    description: "Standard enterprise subscription plan",
    recurringInterval: "MONTHLY",
    duration: 12,
    durationUnit: "MONTHS",
    validityDays: 30,
    lines: [
      { productName: "Base License", quantity: 1, unitPrice: 299.99, taxRate: 18 },
      { productName: "Premium Support", quantity: 1, unitPrice: 99.99, taxRate: 18 }
    ]
  });
  return response.data;
};
```

---

### 11.2 List All Templates

| `GET` | `/api/v1/quotation-templates` | `200 OK` |

Returns `QuotationTemplateDto[]` (not paginated).

---

### 11.3 Get Template by ID

| `GET` | `/api/v1/quotation-templates/{id}` | `200 OK` |

---

### 11.4 Update Template

| `PUT` | `/api/v1/quotation-templates/{id}` | `200 OK` |

Same body as create. Replaces all line items.

---

### 11.5 Delete Template

| `DELETE` | `/api/v1/quotation-templates/{id}` | `204 No Content` |

> **Constraint:** Cannot delete a template that is linked to existing subscriptions. Returns `400 Bad Request`.

---

### 11.6 Generate Quotation from Template

| Property | Value |
|----------|-------|
| **Method** | `POST` |
| **URL** | `/api/v1/quotation-templates/{id}/generate-quotation` |
| **Status** | `201 Created` |

Creates a new Subscription in `QUOTATION` status with line items copied from the template.

#### Request Body — `QuotationGenerateRequest`

| Field          | Type            | Required | Description                |
|----------------|-----------------|----------|----------------------------|
| `customerId`   | `number (Long)` | Yes      |                             |
| `customerName` | `string`        | No       |                             |
| `customerEmail`| `string`        | No       |                             |
| `startDate`    | `string (date)` | No       | Subscription start date    |
| `validityDays` | `number`        | No       | Override template default  |
| `currency`     | `string`        | No       |                             |
| `notes`        | `string`        | No       |                             |

**Returns:** `SubscriptionResponse` with status `QUOTATION`.

```javascript
const generateQuotation = async (templateId) => {
  const response = await apiClient.post(`/quotation-templates/${templateId}/generate-quotation`, {
    customerId: 1,
    customerName: "Acme Corp",
    customerEmail: "billing@acme.com",
    startDate: "2026-12-01",
    validityDays: 15
  });
  return response.data; // SubscriptionResponse with status "QUOTATION"
};
```

---

## 12. Reports / Dashboard API (Supporting)

**Base Path:** `/api/v1/reports`

---

### 12.1 Dashboard Report

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/reports/dashboard` |
| **Status** | `200 OK` |

#### Query Parameters

| Param       | Type            | Required | Description                    |
|-------------|-----------------|----------|--------------------------------|
| `startDate` | `string (date)` | No       | ISO `YYYY-MM-DD` filter start |
| `endDate`   | `string (date)` | No       | ISO `YYYY-MM-DD` filter end   |

#### Response — `DashboardReportResponse`

```json
{
  "totalSubscriptions": 42,
  "activeSubscriptions": 30,
  "draftSubscriptions": 8,
  "closedSubscriptions": 4,
  "totalRevenue": 125000.00,
  "amountOwed": 15000.00,
  "monthlyRecurringRevenue": 12500.00,
  "totalInvoices": 120,
  "paidInvoices": 95,
  "partiallyPaidInvoices": 5,
  "unpaidInvoices": 15,
  "overdueInvoices": 5,
  "recentActivities": [
    {
      "eventType": "PAYMENT",
      "referenceNumber": "PAY-2026-XXXXX",
      "description": "Payment received",
      "amount": 499.99,
      "timestamp": "2026-10-31T10:45:00"
    }
  ]
}
```

```javascript
const getDashboard = async (startDate, endDate) => {
  const params = {};
  if (startDate) params.startDate = startDate; // "2026-01-01"
  if (endDate) params.endDate = endDate;       // "2026-12-31"

  const response = await apiClient.get('/reports/dashboard', { params });
  return response.data;
};
```

---

### 12.2 Payment History (Paginated Ledger)

| Property | Value |
|----------|-------|
| **Method** | `GET` |
| **URL** | `/api/v1/reports/payments` |
| **Status** | `200 OK` |

| Param  | Type     | Required | Default |
|--------|----------|----------|---------|
| `page` | `number` | No       | `0`     |
| `size` | `number` | No       | `20`    |

Returns `Page<PaymentResponse>`.

```javascript
const getPaymentHistory = async (page = 0, size = 20) => {
  const response = await apiClient.get('/reports/payments', {
    params: { page, size }
  });
  return response.data;
};
```

---

## 13. Inter-Module Relationships

```
+---------------------------+
|   Quotation Template      |
|   (reusable plan)         |
+-----------+---------------+
            | generates (POST /{id}/generate-quotation)
            v
+---------------------------+       1 : N        +--------------------+
|    Subscription           | ------------------> |     Invoice        |
| (SUB-2026-XXXXX)          |                     | (INV-2026-XXXXX)   |
|                           |                     |                    |
| status: DRAFT ->          |                     | status: DRAFT ->   |
|   QUOTATION -> CONFIRMED  |                     |  CONFIRMED ->      |
|   -> ACTIVE -> CLOSED     |                     |  UNPAID ->         |
|                           |                     |  PARTIALLY_PAID -> |
| customerId                |                     |  PAID / CANCELLED  |
+---------------------------+                     +--------+-----------+
                                                           | 1 : N
                                                           v
                                                  +--------------------+
                                                  |    Payment         |
                                                  | (PAY-2026-XXXXX)   |
                                                  |                    |
                                                  | invoiceId          |
                                                  | customerId         |
                                                  +--------------------+
```

### Key Relationships

| Parent Entity     | Child Entity         | Foreign Key                          | Cardinality |
|-------------------|---------------------|--------------------------------------|-------------|
| Subscription      | Invoice             | `invoice.subscriptionId`             | 1 : N       |
| Invoice           | Payment             | `payment.invoiceId`                  | 1 : N       |
| Invoice           | InvoiceLine         | `invoiceLine.invoiceId`              | 1 : N       |
| Subscription      | SubscriptionLine    | `subscriptionLine.subscriptionId`    | 1 : N       |
| QuotationTemplate | Subscription        | `subscription.quotationTemplateId`   | 1 : N       |

### Data Flow for Frontend

1. **Create/Browse Quotation Templates** — user selects a plan
2. **Generate Quotation** — creates a Subscription in `QUOTATION` status
3. **Advance Subscription** — `QUOTATION` -> `CONFIRMED` -> `ACTIVE`
4. **Generate Invoice** — manually or via batch billing when subscription is `ACTIVE`
5. **Confirm Invoice** — `DRAFT` -> `CONFIRMED` (makes it payable)
6. **Record Payment** — reduces `amountDue`, transitions invoice to `PARTIALLY_PAID` or `PAID`
7. **Dashboard** — aggregate view of all subscription/invoice/payment metrics

---

## 14. Pagination Reference

All paginated endpoints return a Spring `Page` object:

```json
{
  "content": [],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": {
      "empty": true,
      "sorted": false,
      "unsorted": true
    },
    "offset": 0,
    "paged": true,
    "unpaged": false
  },
  "totalPages": 5,
  "totalElements": 92,
  "last": false,
  "first": true,
  "size": 20,
  "number": 0,
  "numberOfElements": 20,
  "sort": { "empty": true, "sorted": false, "unsorted": true },
  "empty": false
}
```

### Frontend Usage Pattern

```javascript
// Generic pagination hook pattern
const usePaginatedData = (fetchFn) => {
  const [data, setData] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);

  const loadPage = async (pageNum = 0, size = 20) => {
    setLoading(true);
    try {
      const result = await fetchFn(pageNum, size);
      setData(result.content);
      setPage(result.number);
      setTotalPages(result.totalPages);
      setTotalElements(result.totalElements);
    } finally {
      setLoading(false);
    }
  };

  return { data, page, totalPages, totalElements, loading, loadPage, setPage };
};
```

---

## 15. Complete API Reference Table

| #  | Method   | Endpoint                                              | Returns                    | Paginated | Status |
|----|----------|-------------------------------------------------------|----------------------------|-----------|--------|
| 1  | `POST`   | `/api/v1/subscriptions`                               | `SubscriptionResponse`     | No        | 201    |
| 2  | `GET`    | `/api/v1/subscriptions`                               | `Page<SubscriptionResponse>` | Yes     | 200    |
| 3  | `GET`    | `/api/v1/subscriptions/{id}`                          | `SubscriptionResponse`     | No        | 200    |
| 4  | `GET`    | `/api/v1/subscriptions/number/{subscriptionNumber}`   | `SubscriptionResponse`     | No        | 200    |
| 5  | `PATCH`  | `/api/v1/subscriptions/{id}/status`                   | `SubscriptionResponse`     | No        | 200    |
| 6  | `POST`   | `/api/v1/subscriptions/{id}/close`                    | `SubscriptionResponse`     | No        | 200    |
| 7  | `POST`   | `/api/v1/subscriptions/{id}/pause`                    | `SubscriptionResponse`     | No        | 200    |
| 8  | `POST`   | `/api/v1/subscriptions/{id}/resume`                   | `SubscriptionResponse`     | No        | 200    |
| 9  | `POST`   | `/api/v1/subscriptions/{id}/renew`                    | `SubscriptionResponse`     | No        | 200    |
| 10 | `POST`   | `/api/v1/subscriptions/{id}/generate-invoice`         | `{success, invoiceId}`     | No        | 201    |
| 11 | `POST`   | `/api/v1/subscriptions/generate-billing`              | `{success, invoiceIds}`    | No        | 200    |
| 12 | `POST`   | `/api/v1/invoices`                                    | `InvoiceResponse`          | No        | 201    |
| 13 | `GET`    | `/api/v1/invoices`                                    | `Page<InvoiceResponse>`    | Yes       | 200    |
| 14 | `GET`    | `/api/v1/invoices/{id}`                               | `InvoiceResponse`          | No        | 200    |
| 15 | `GET`    | `/api/v1/invoices/number/{invoiceNumber}`             | `InvoiceResponse`          | No        | 200    |
| 16 | `GET`    | `/api/v1/invoices/subscription/{subscriptionId}`      | `InvoiceResponse[]`        | No        | 200    |
| 17 | `GET`    | `/api/v1/invoices/overdue`                            | `InvoiceResponse[]`        | No        | 200    |
| 18 | `PATCH`  | `/api/v1/invoices/{id}/status`                        | `InvoiceResponse`          | No        | 200    |
| 19 | `POST`   | `/api/v1/invoices/{id}/confirm`                       | `InvoiceResponse`          | No        | 200    |
| 20 | `POST`   | `/api/v1/invoices/{id}/cancel`                        | `InvoiceResponse`          | No        | 200    |
| 21 | `GET`    | `/api/v1/invoices/{id}/print`                         | `text/plain`               | No        | 200    |
| 22 | `POST`   | `/api/v1/invoices/{id}/send`                          | `{success, message}`       | No        | 200    |
| 23 | `POST`   | `/api/v1/payments`                                    | `PaymentResponse`          | No        | 201    |
| 24 | `GET`    | `/api/v1/payments/{id}`                               | `PaymentResponse`          | No        | 200    |
| 25 | `GET`    | `/api/v1/payments/number/{paymentNumber}`             | `PaymentResponse`          | No        | 200    |
| 26 | `GET`    | `/api/v1/payments/invoice/{invoiceId}`                | `PaymentResponse[]`        | No        | 200    |
| 27 | `GET`    | `/api/v1/payments/customer/{customerId}`              | `Page<PaymentResponse>`    | Yes       | 200    |
| 28 | `POST`   | `/api/v1/quotation-templates`                         | `QuotationTemplateDto`     | No        | 201    |
| 29 | `GET`    | `/api/v1/quotation-templates`                         | `QuotationTemplateDto[]`   | No        | 200    |
| 30 | `GET`    | `/api/v1/quotation-templates/{id}`                    | `QuotationTemplateDto`     | No        | 200    |
| 31 | `PUT`    | `/api/v1/quotation-templates/{id}`                    | `QuotationTemplateDto`     | No        | 200    |
| 32 | `DELETE` | `/api/v1/quotation-templates/{id}`                    | `(empty)`                  | No        | 204    |
| 33 | `POST`   | `/api/v1/quotation-templates/{id}/generate-quotation` | `SubscriptionResponse`     | No        | 201    |
| 34 | `GET`    | `/api/v1/reports/dashboard`                           | `DashboardReportResponse`  | No        | 200    |
| 35 | `GET`    | `/api/v1/reports/payments`                            | `Page<PaymentResponse>`    | Yes       | 200    |

**Total: 35 endpoints across 5 controllers.**

---

## 16. Missing Implementations & Blockers

### Fully Implemented

- All 35 endpoints listed above are fully implemented with working controllers, services, repositories, and Flyway migrations.
- Subscription lifecycle management with status history tracking.
- Invoice generation (manual and automated batch billing).
- Payment recording with automatic invoice status updates (partial/full).
- Quotation template management with quotation generation.
- Dashboard reporting with date-range filtering.
- Printable invoice view and notification dispatch boundary.
- Global exception handling with consistent JSON error responses.
- CORS enabled for all origins.

### Potential Frontend Considerations

1. **No Authentication Required** — All endpoints are open. If auth is added later, the frontend will need to implement a login flow and attach `Authorization: Bearer <token>` headers.
2. **No File Upload/Download** — The print invoice endpoint returns `text/plain`. There is no PDF generation endpoint.
3. **No WebSocket/SSE** — Real-time updates are not available. The frontend should poll or refresh data manually.
4. **No Customer CRUD** — Customer data (`customerId`, `customerName`, `customerEmail`) is embedded directly in subscription/invoice requests. There is no standalone `/api/v1/customers` endpoint. The frontend must manage customer IDs independently or receive them from another team member's module.
5. **No Payment Method CRUD** — `paymentMethodId` is accepted but there is no `/api/v1/payment-methods` endpoint to manage them. Use the `PaymentMethodType` enum values for display purposes.

---

> **For questions or issues, contact the Member 4 backend developer.**
