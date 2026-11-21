# REST API Reference

Every service exposes its own REST API and its own Swagger UI. This page lists all endpoints by service. Kafka events are documented separately in [KAFKA.md](KAFKA.md).

Back to the [main README](../README.md).

## Conventions

**Base URLs**

| Environment | URL |
|---|---|
| Local, direct to a service | `http://localhost:<port>` (ports below) |
| Local, through the frontend proxy | `http://localhost:5173/api` |
| AWS | `https://d3lwgdw2frxsbt.cloudfront.net/api` |

**Swagger UI:** `http://localhost:<port>/swagger-ui/index.html` on each service (OpenAPI JSON at `/v3/api-docs`).

**Authentication**

| Value in the Auth column | Meaning |
|---|---|
| Public | No token needed |
| Logged in | Any valid JWT |
| Customer / Agency / Admin | A JWT with that role |
| Internal | Service-to-service only: header `X-Internal-Key` must match `INTERNAL_API_KEY` |
| Stripe | Called by Stripe; verified with the `Stripe-Signature` header |

Get a token from `POST /users/login` and send it on every protected request:

```
Authorization: Bearer <token>
```

**Common status codes**

| Code | When |
|---|---|
| 200 OK | Successful read or update |
| 201 Created | A resource was created |
| 204 No Content | Success with no body (delete, activate, deactivate) |
| 400 Bad Request | Validation failed, a parameter is missing or has the wrong type, or a business rule rejected the request |
| 401 Unauthorized | Missing, invalid or expired token, or wrong login credentials |
| 403 Forbidden | Valid token but the wrong role, or the resource belongs to someone else |
| 404 Not Found | The resource does not exist |
| 409 Conflict | The request clashes with existing data (duplicate email or username, overlapping booking, payment already started) |
| 429 Too Many Requests | Rate limit hit on login and registration |

**Error body** (same in every service):

```json
{
  "timestamp": "2026-11-21T10:15:30",
  "status": 409,
  "error": "Conflict",
  "message": "Car is not available at this date",
  "path": "/bookings/118/approve"
}
```

**Paged responses** (`PageResponse`) accept `page` (from 0), `size` and `sort` (for example `sort=id,desc`) and return:

```json
{ "content": [ ... ], "page": 0, "size": 10, "totalElements": 57, "totalPages": 6 }
```

---

## user-service (port 8081)

### Accounts: `/users`

| Method | Endpoint | Auth | Request | Response | Status codes |
|---|---|---|---|---|---|
| POST | `/users/register` | Public | `RegisterRequest` | `RegisterResponse` | 201, 400, 409 |
| POST | `/users/register/agency` | Public | `RegisterRequest` | `RegisterResponse` (status `UNAPPROVED_AGENCY`) | 201, 400, 409 |
| POST | `/users/login` | Public | `LoginRequest` | `LoginResponse` | 200, 400, 401, 429 |
| GET | `/users/verify?token=` | Public | query `token` | `{ "message": "Account verified!" }` | 200, 400 |
| POST | `/users/resend-verification?email=` | Public | query `email` | text message | 200, 400, 404 |
| POST | `/users/forgot-password?email=` | Public | query `email` | text message | 200, 400, 404 |
| POST | `/users/reset-password` | Public | `ResetPasswordToken` | text message | 200, 400 |
| POST | `/users/change-password` | Logged in | `ChangePasswordRequest` | text message | 200, 400, 401 |
| GET | `/users` | Admin | query `role`, `status`, `search`, paging | `PageResponse<UserSummaryResponse>` | 200, 401, 403 |
| PATCH | `/users/{id}/activate` | Admin | none | none (also approves a pending agency) | 204, 403, 404 |
| PATCH | `/users/{id}/deactivate` | Admin | none | none | 204, 403, 404 |

```jsonc
// RegisterRequest: password 8–72 characters
{ "userName": "sara", "email": "sara@example.com", "password": "Secret123!" }

// RegisterResponse
{ "id": 25, "userName": "sara", "email": "sara@example.com", "status": "ACTIVE", "message": "Registration successful. Please check your email to verify your account." }

// LoginRequest
{ "email": "sara@example.com", "password": "Secret123!" }

// LoginResponse
{ "token": "eyJhbGciOi...", "email": "sara@example.com", "role": "CUSTOMER" }

// ResetPasswordToken
{ "token": "e1c3010a-...", "password": "NewSecret123!" }

// ChangePasswordRequest
{ "oldPassword": "Secret123!", "newPassword": "NewSecret123!" }

// UserSummaryResponse
{ "id": 25, "userName": "sara", "email": "sara@example.com", "role": "CUSTOMER", "status": "ACTIVE", "verified": true, "createdAt": "2026-11-21T10:00:00" }
```

### Customer profile: `/profiles/customer`

| Method | Endpoint | Auth | Request | Response | Status codes |
|---|---|---|---|---|---|
| POST | `/profiles/customer` | Customer | `UserProfileRequest` | `UserProfileResponse` | 201, 400, 409 |
| GET | `/profiles/customer/me` | Customer | none | `UserProfileResponse` | 200, 404 |
| PUT | `/profiles/customer/me` | Customer | `UserProfileRequest` | `UserProfileResponse` | 200, 400, 404 |
| POST | `/profiles/customer/picture` | Customer | multipart `file` (JPG or PNG) | none | 201, 400 |
| GET | `/profiles/customer/picture` | Logged in | none | image bytes | 200, 404 |

```jsonc
// UserProfileRequest: firstName, lastName and phoneNumber required
{ "firstName": "Sara", "lastName": "Ali", "phoneNumber": "+97333000000", "address": "Manama", "licenseNumber": "BH-12345" }
```

### Agency profile: `/profiles/agency`

| Method | Endpoint | Auth | Request | Response | Status codes |
|---|---|---|---|---|---|
| POST | `/profiles/agency` | Agency | `AgencyProfileRequest` | `AgencyProfileResponse` | 201, 409 |
| GET | `/profiles/agency/me` | Agency | none | `AgencyProfileResponse` | 200, 404 |
| PUT | `/profiles/agency/me` | Agency | `AgencyProfileRequest` | `AgencyProfileResponse` | 200, 404 |
| POST | `/profiles/agency/logo` | Agency | multipart `file` (JPG or PNG) | none | 201, 400 |
| GET | `/profiles/agency/logo` | Logged in | none | image bytes | 200, 404 |

```jsonc
// AgencyProfileRequest
{ "businessName": "Gulf Rentals", "phoneNumber": "+97317000000" }
```

---

## car-service (port 8082)

### Cars: `/cars`

| Method | Endpoint | Auth | Request | Response | Status codes |
|---|---|---|---|---|---|
| GET | `/cars/` | Logged in | query filters below, paging | `PageResponse<CarResponse>` | 200, 400 |
| GET | `/cars/{id}` | Logged in | none | `CarResponse` | 200, 404 |
| GET | `/cars/my-cars` | Agency | none | `CarResponse[]` | 200 |
| POST | `/cars/create` | Agency | `CarRequest` | `CarResponse` | 201, 400, 404, 409 |
| PUT | `/cars/update/{id}` | Agency (owner) | `CarRequest` | `CarResponse` | 200, 400, 403, 404 |
| DELETE | `/cars/delete/{id}` | Agency (owner) | none | none (soft delete) | 204, 403, 404 |
| POST | `/cars/{id}/images` | Agency (owner) | multipart `file` (one or more JPG/PNG) | none | 201, 400, 403, 404 |

Note the trailing slash on `GET /cars/`.

**Search filters** (all optional and combined): `countryId`, `cityId`, `categoryId`, `makeId`, `minPrice`, `maxPrice`, `minSeats`, `transmission` (`AUTOMATIC`, `MANUAL`), `fuelType` (`PETROL`, `DIESEL`, `ELECTRIC`, `HYBRID`), and `startDate` + `endDate` (`yyyy-MM-dd`, both or neither). With dates, only cars with no approved booking overlapping that period are returned.

```jsonc
// CarRequest
{
  "makeId": 1, "model": "Corolla", "categoryId": 2, "cityId": 5,
  "year": 2024,                 // 1980–2100
  "licensePlate": "BH-1234",
  "transmission": "AUTOMATIC",
  "fuelType": "PETROL",
  "seats": 5,                   // 1–20
  "pricePerDay": 25.000         // > 0
}

// CarResponse
{
  "id": 42, "make": "Toyota", "model": "Corolla", "category": "Sedan",
  "city": "Manama", "country": "Bahrain", "year": 2024, "licensePlate": "BH-1234",
  "transmission": "AUTOMATIC", "fuelType": "PETROL", "seats": 5, "pricePerDay": 25.000,
  "available": true, "ownerId": 7, "ownerUsername": "gulfrentals",
  "imageIds": [101, 102], "createdAt": "2026-11-21T10:00:00", "updatedAt": "2026-11-21T10:00:00"
}
```

### Images: `/images`

| Method | Endpoint | Auth | Request | Response | Status codes |
|---|---|---|---|---|---|
| GET | `/images/{imageId}` | Logged in | none | image bytes | 200, 404 |

### Catalogue: `/makes`, `/categories`

| Method | Endpoint | Auth | Request | Response | Status codes |
|---|---|---|---|---|---|
| GET | `/makes` | Logged in | none | `MakeResponse[]` | 200 |
| GET | `/makes/{id}` | Admin | none | `MakeResponse` | 200, 404 |
| POST | `/makes` | Admin | `{ "name": "Lexus" }` | `MakeResponse` | 201, 400, 409 |
| PUT | `/makes/{id}` | Admin | `{ "name": "Lexus" }` | `MakeResponse` | 200, 400, 404, 409 |
| GET | `/categories` | Logged in | none | `CategoryResponse[]` | 200 |
| GET | `/categories/{id}` | Admin | none | `CategoryResponse` | 200, 404 |
| POST | `/categories` | Admin | `{ "name": "Van" }` | `CategoryResponse` | 201, 400, 409 |
| PUT | `/categories/{id}` | Admin | `{ "name": "Van" }` | `CategoryResponse` | 200, 400, 404, 409 |

`MakeResponse` and `CategoryResponse`: `{ "id": 1, "name": "Toyota" }`

### Locations: `/countries`, `/cities`

| Method | Endpoint | Auth | Request | Response | Status codes |
|---|---|---|---|---|---|
| GET | `/countries` | Public | none | `[{ "id": 1, "name": "Bahrain", "iso2": "BH" }]` | 200 |
| GET | `/cities?countryId=` | Public | query `countryId` | `[{ "id": 5, "name": "Manama" }]` | 200, 400 |

---

## booking-service (port 8083)

| Method | Endpoint | Auth | Request | Response | Status codes |
|---|---|---|---|---|---|
| POST | `/bookings` | Customer | `BookingRequest` | `BookingResponse` (status `PENDING`) | 201, 400, 404, 409 |
| GET | `/bookings/me` | Customer | none | `BookingResponse[]` | 200 |
| GET | `/bookings/my-cars` | Agency | none | `BookingResponse[]` | 200 |
| PATCH | `/bookings/{id}/approve` | Agency (owner) | none | `BookingResponse` | 200, 400, 403, 404, 409 |
| PATCH | `/bookings/{id}/reject` | Agency (owner) | none | `BookingResponse` | 200, 400, 403, 404 |
| PATCH | `/bookings/{id}/cancel` | Customer (renter) or Agency (owner) | none | `BookingResponse` (refunds if paid) | 200, 400, 403, 404 |
| GET | `/bookings` | Admin | query `status`, `ownerId`, `renterId`, `carId`, `paid`, `from`, `to`, paging | `PageResponse<BookingResponse>` | 200, 400, 403 |
| GET | `/bookings/car/{carId}/dates` | Logged in | none | `[{ "startDate": "...", "endDate": "..." }]` | 200, 404 |
| GET | `/bookings/{id}` | Logged in | none | `BookingResponse` | 200, 404 |
| PATCH | `/bookings/{id}/paid` | Internal | header `X-Internal-Key` | none | 200, 403, 404 |
| PATCH | `/bookings/{id}/refund` | Internal | header `X-Internal-Key` | none | 200, 403, 404 |

Booking statuses: `PENDING`, `APPROVED`, `REJECTED`, `CANCELLED`, `COMPLETED`. The calendar ranges block `startDate` up to but not including `endDate`, so the return day is free for the next pick-up.

```jsonc
// BookingRequest: startDate cannot be in the past and must be before endDate
{ "carId": 42, "startDate": "2026-12-01", "endDate": "2026-12-05" }

// BookingResponse
{
  "id": 118, "carId": 42, "renterId": 12, "ownerId": 7,
  "startDate": "2026-12-01", "endDate": "2026-12-05", "totalPrice": 100.000,
  "status": "APPROVED", "paid": false, "refunded": false,
  "make": "Toyota", "model": "Corolla",
  "createdAt": "2026-11-21T10:00:00", "updatedAt": "2026-11-21T10:20:00"
}
```

---

## payment-service (port 8084)

| Method | Endpoint | Auth | Request | Response | Status codes |
|---|---|---|---|---|---|
| POST | `/pay/{bookingId}` | Customer (renter) | none | `PaymentResponse` with the Stripe Checkout URL | 200, 400, 403, 404, 409 |
| POST | `/pay/webhook` | Stripe | raw Stripe event, header `Stripe-Signature` | none | 204, 400 |
| POST | `/pay/refund/{bookingId}` | Internal | header `X-Internal-Key` | none | 200, 403, 404 |
| GET | `/pay/payments` | Admin, Agency, Customer | query `ownerId`, `renterId`, `status`, paging | `PageResponse<PaymentSummaryResponse>` | 200, 400 |

- `POST /pay/{bookingId}` only works for an approved, unpaid booking. It returns 409 if a payment is already pending or done. The browser then goes to `sessionUrl`, and Stripe redirects back to `/payment/success` or `/payment/cancel` on the frontend.
- The webhook handles `checkout.session.completed` (payment `PAID`) and `checkout.session.expired` (payment `FAILED`).
- `GET /pay/payments`: admins see every payment and can filter by `ownerId` and `renterId`. Agencies see payments for their cars, customers see their own. Statuses: `PENDING`, `PAID`, `FAILED`, `REFUNDED`.

```jsonc
// PaymentResponse
{ "status": "open", "message": "SESSION_CREATED", "sessionId": "cs_test_...", "sessionUrl": "https://checkout.stripe.com/c/pay/cs_test_..." }

// PaymentSummaryResponse
{
  "id": 31, "bookingId": 118, "renterId": 12, "ownerId": 7,
  "amount": 100.000, "currency": "USD", "status": "PAID",
  "make": "Toyota", "model": "Corolla",
  "createdAt": "2026-11-21T10:25:00", "updatedAt": "2026-11-21T10:26:10"
}
```

---

## notification-service (port 8085)

| Method | Endpoint | Auth | Request | Response | Status codes |
|---|---|---|---|---|---|
| GET | `/notifications/stream` | Logged in | none | `text/event-stream` (Server-Sent Events) | 200, 401 |

The connection stays open and receives one SSE event per notification. The event name is the booking event type or notification name (for example `BOOKING_APPROVED`, `PAYMENT_RECEIVED`), and the data is JSON. A keep-alive is sent every 25 seconds. The events come from Kafka; see [KAFKA.md](KAFKA.md).

```
event: BOOKING_APPROVED
data: {"id":118,"carId":42,"status":"APPROVED", ...}
```

---

## audit-service (port 8086)

| Method | Endpoint | Auth | Request | Response | Status codes |
|---|---|---|---|---|---|
| GET | `/audit-logs` | Admin | query `action`, `entityType`, `userId`, paging (default newest first, 20 per page) | `PageResponse<AuditLogResponse>` | 200, 403 |

```jsonc
// AuditLogResponse
{ "id": 901, "userId": 7, "action": "CAR_CREATED", "entityType": "Car", "entityId": 42, "details": "User 7 created Car 42", "createdAt": "2026-11-21T10:15:30" }
```

---

## Changes in this version

Compared with the single-application version, the API paths stayed the same, so existing clients only needed a new base URL per service. New or changed endpoints:

| Endpoint | Change |
|---|---|
| `GET /users` | **New:** admin user search |
| `GET /users/verify` | **Changed:** returns JSON `{ "message": ... }` instead of plain text |
| `POST /users/register` | **Changed:** 409 when the username is already taken |
| `POST /makes`, `PUT /makes/{id}`, `POST /categories`, `PUT /categories/{id}` | **New:** catalogue management |
| `GET /bookings` | **Changed:** more filters (`ownerId`, `renterId`, `carId`, `paid`, `from`, `to`) |
| `GET /bookings/{id}`, `PATCH /bookings/{id}/paid`, `PATCH /bookings/{id}/refund`, `POST /pay/refund/{bookingId}` | **New:** service-to-service endpoints |
| `GET /pay/payments` | **New:** payment history, scoped by role |
| `GET /audit-logs` | **New:** audit log for admins |
| Audit and notification REST endpoints | **Removed:** replaced by Kafka topics |
