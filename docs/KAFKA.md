# Kafka Events

RentRide uses Apache Kafka 4 (KRaft mode, single broker) for every operation that does not need an immediate answer: audit records, live notifications and booking state changes. The REST API is documented separately in [API.md](API.md).

Back to the [main README](../README.md).

## Overview

| Topic | Message key | Event record | Producers | Consumers (group id) |
|---|---|---|---|---|
| `audit-events` | none | `AuditEvent` | user, car, booking, payment | audit-service (`audit-service`) |
| `booking-events` | booking id | `BookingEvent` | booking-service | car-service (`car-service`), notification-service (`notification-service`), audit-service (`audit-service`) |
| `notification-events` | user id | `NotificationEvent` | payment-service | notification-service (`notification-service`) |

```
user ─────┐
car ──────┼──▶ audit-events ─────────▶ audit-service
booking ──┤
payment ──┘

booking ─────▶ booking-events ─┬─────▶ car-service           (reservation read model)
                               ├─────▶ notification-service  (live notification)
                               └─────▶ audit-service         (audit trail)

payment ─────▶ notification-events ──▶ notification-service
```

## Configuration

| Setting | Value |
|---|---|
| Topic names | constants in `common/src/main/java/com/ga/common/events/Topics.java` |
| Event records | `common/src/main/java/com/ga/common/events/` (shared by every service) |
| Partitions | 3 per topic (`KAFKA_NUM_PARTITIONS`), topics are created automatically on first use |
| Replication factor | 1 (single broker) |
| Serialisation | JSON (`JacksonJsonSerializer` / `JacksonJsonDeserializer`), trusted package `com.ga.common.events` |
| Offset reset | `earliest`, so a consumer that was down reads the events it missed |
| Bootstrap servers | `kafka:19092` inside Docker, `localhost:9094` from the host |

**Keys and ordering.** Booking events use the booking id as the key, so every event of one booking goes to the same partition and is consumed in order (for example, `APPROVED` is always processed before `CANCELLED`). Notification events use the user id for the same reason.

**Failure handling.** Producers send asynchronously and only log a failure. Publishing an event never fails the user's request. Consumers that were offline catch up from their last committed offset.

**Idempotency.** `AuditEvent` and `BookingEvent` carry a unique `eventId` (UUID). audit-service stores it and skips any event it has already recorded. car-service's read model writes by booking id, so processing the same event twice gives the same result.

---

## `audit-events`

Records who did what, for the platform-wide audit log.

**Record:** `AuditEvent`

| Field | Type | Description |
|---|---|---|
| `userId` | Long | The user who performed the action (null for scheduled jobs) |
| `action` | String | What happened, for example `CAR_CREATED` |
| `entityType` | String | The kind of entity affected, for example `Car` |
| `entityId` | Long | The id of the affected entity |
| `details` | String | Human-readable description |
| `occurredAt` | LocalDateTime | When the action happened |
| `eventId` | String | Unique id used to ignore duplicates |

**Actions by producer**

| Producer | Entity type | Actions |
|---|---|---|
| user-service | `User` | `USER_REGISTERED`, `EMAIL_VERIFIED`, `PASSWORD_RESET`, `PASSWORD_CHANGED`, `USER_DEACTIVATED`, `USER_ACTIVATED`, `AGENCY_APPROVED` |
| user-service | `UserProfile`, `AgencyProfile` | `PROFILE_CREATED`, `PROFILE_UPDATED` |
| car-service | `Car` | `CAR_CREATED`, `CAR_UPDATED`, `CAR_DELETED` |
| booking-service | `Booking` | `BOOKINGS_COMPLETED`, `BOOKINGS_AUTO_REJECTED` (scheduled jobs) |
| payment-service | `Payment` | `PAYMENT_COMPLETED`, `PAYMENT_FAILED`, `PAYMENT_REFUNDED` |

Booking lifecycle changes reach the audit log through `booking-events` instead (see below).

**Consumer:** audit-service saves one `audit_logs` row per event. Admins read them with `GET /audit-logs`.

**Example**

```json
{
  "userId": 7,
  "action": "CAR_CREATED",
  "entityType": "Car",
  "entityId": 42,
  "details": "User 7 created Car 42",
  "occurredAt": "2026-11-21T10:15:30",
  "eventId": "3f2b8c1e-9a4d-4e57-b0a2-6d1f8e2c7a90"
}
```

---

## `booking-events`

Published by booking-service every time a booking changes state.

**Record:** `BookingEvent`

| Field | Type | Description |
|---|---|---|
| `eventId` | String | Unique id (UUID) |
| `eventType` | String | One of the event types below |
| `bookingId` | Long | The booking that changed (also the message key) |
| `actorId` | Long | The user who caused the change |
| `renterId` | Long | The customer who made the booking |
| `ownerId` | Long | The agency that owns the car |
| `message` | String | Human-readable description |
| `booking` | object | Full booking snapshot (`BookingResponse`: dates, price, status, paid, refunded, make, model) |
| `occurredAt` | LocalDateTime | When it happened |

**Event types and what each consumer does**

| `eventType` | Trigger | car-service | notification-service | audit-service |
|---|---|---|---|---|
| `BOOKING_CREATED` | Customer requests a booking | ignored | notifies the agency | records it |
| `BOOKING_APPROVED` | Agency approves | adds a row to `car_reservations` | notifies the customer | records it |
| `BOOKING_REJECTED` | Agency rejects | ignored | notifies the customer | records it |
| `BOOKING_CANCELLED` | Customer or agency cancels | deletes the row from `car_reservations` | notifies the other party | records it |

**car-service read model (CQRS).** car-service keeps its own `car_reservations` table (booking id, car id, start date, end date), built only from these events. The car search uses it to filter out cars that are booked between two dates, without calling booking-service.

**Example**

```json
{
  "eventId": "b7c1d2e3-4f56-4a78-9b0c-1d2e3f4a5b6c",
  "eventType": "BOOKING_APPROVED",
  "bookingId": 118,
  "actorId": 7,
  "renterId": 12,
  "ownerId": 7,
  "message": "Booking 118 approved by Agency 7",
  "booking": {
    "id": 118,
    "carId": 42,
    "renterId": 12,
    "ownerId": 7,
    "startDate": "2026-12-01",
    "endDate": "2026-12-05",
    "totalPrice": 100.000,
    "status": "APPROVED",
    "paid": false,
    "refunded": false,
    "make": "Toyota",
    "model": "Corolla"
  },
  "occurredAt": "2026-11-21T10:20:00"
}
```

---

## `notification-events`

Direct messages to one user, pushed to the browser over Server-Sent Events (`GET /notifications/stream`).

**Record:** `NotificationEvent`

| Field | Type | Description |
|---|---|---|
| `userId` | Long | The recipient (also the message key) |
| `eventName` | String | The SSE event name the frontend listens for |
| `data` | object | Payload shown to the user |

**Event names**

| `eventName` | Recipient | Trigger |
|---|---|---|
| `PAYMENT_RECEIVED` | customer | Stripe confirmed the payment (`checkout.session.completed`) |
| `BOOKING_PAID` | agency | Same payment, sent to the car's owner |
| `PAYMENT_FAILED` | customer | The Checkout session expired (`checkout.session.expired`) |

**Consumer:** notification-service looks up the user's open SSE connection and sends the event. A user who is offline does not receive it later.

**Example**

```json
{
  "userId": 12,
  "eventName": "PAYMENT_RECEIVED",
  "data": { "bookingId": 118, "paymentId": 31, "status": "PAID", "make": "Toyota", "model": "Corolla" }
}
```

---

## Inspecting events locally

With `docker compose up` running:

```bash
# list topics
docker compose exec kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:19092 --list

# watch booking events as they happen
docker compose exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server kafka:19092 --topic booking-events --from-beginning

# see how far each consumer group has read
docker compose exec kafka /opt/kafka/bin/kafka-consumer-groups.sh \
  --bootstrap-server kafka:19092 --describe --all-groups
```
