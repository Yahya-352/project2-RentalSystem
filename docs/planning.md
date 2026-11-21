# RentRide Planning

## Scope

**In scope**
- Accounts for three roles (customer, agency, admin) with JWT authentication, email verification and password recovery
- Agency approval by an admin before the agency can log in
- Car listings with filtering, pagination, soft delete and image upload
- Bookings with automatic pricing, overlap checking and an approve / reject / cancel flow
- Customer and agency profiles, and a customer profile picture
- Real-time booking notifications (Server-Sent Events)
- Audit logging, rate limiting, global JSON error handling and database seeding
- Swagger / OpenAPI documentation, unit tests, README, ERD and user stories

**Out of scope** (listed under Future Improvements in the README)
- Online payments (Stripe)
- Reviews and ratings
- Date-based car search
- Docker and integration tests

## Timeline

The project was planned as 9 working days.

| Phase | Duration | Days | Status |
|---|---|---|---|
| 1. Planning | 1 day | Day 1 | Done |
| 2. JWT and authentication | 2 days | Days 2-3 | Done |
| 3. User profiles | 1 day | Day 4 | Done |
| 4. Booking logic and car listing | 2 days | Days 5-6 | Done |
| 5. Notifications and rate limiting | 1 day | Day 7 | Done |
| 6. Searching and pagination | 1 day | Day 8 | Done |
| 7. Documentation and testing | 1 day | Day 9 | In progress |

## Deliverables and Progress

### Phase 1: Planning (Day 1)
- [x] Requirements analysis and scope
- [x] User stories
- [x] ERD
- [x] Project setup (Spring Boot, PostgreSQL, Maven)
- [x] Planning document

### Phase 2: JWT and authentication (Days 2-3)
- [x] Entities and repositories for users
- [x] Spring Security with JWT and role-based access
- [x] Customer and agency registration
- [x] Email verification and resend
- [x] Login
- [x] Forgot password, reset password and change password
- [x] Agency approval before login
- [x] Admin activate and deactivate users
- [x] Global exception handling with a consistent JSON error body
- [x] Database seeding (admin, categories, makes, test accounts)

### Phase 3: User profiles (Day 4)
- [x] Customer profile
- [x] Customer profile picture upload
- [x] Agency profile

### Phase 4: Booking logic and car listing (Days 5-6)
- [x] Create, view, update and delete cars (soft delete)
- [x] Car image upload (JPG and PNG, validated)
- [x] View my cars
- [x] Create a booking with automatic price calculation
- [x] Overlap checking for approved bookings
- [x] Approve, reject and cancel bookings
- [x] View my bookings and bookings on my cars
- [x] Audit logging

### Phase 5: Notifications and rate limiting (Day 7)
- [x] Real-time notifications (SSE)
- [x] Rate limiting

### Phase 6: Searching and pagination (Day 8)
- [x] Filtering cars by location, category and make
- [x] Pagination for cars
- [x] Admin view of all bookings with a status filter and pagination

### Phase 7: Documentation and testing (Day 9)
- [x] Consistent return types and status codes (201, 204, 200)
- [x] Swagger / OpenAPI with JWT authorization
- [x] Second configuration profile with production-style settings
- [x] README
- [ ] Unit tests (in progress)
- [ ] JavaDoc
- [ ] Postman collection
- [ ] Final merge into `main`

## Project 3: Concurrency and Payments

### Scope
Make the booking and payment workflows safe under concurrent requests, and add the features that need it. Three items that were out of scope in Project 2 are now in scope: **Stripe payments**, **date-based car search** and **integration tests**.

**New features:** online payments with Stripe Checkout and automatic full refunds, combined car search with date availability, countries and cities, a booking calendar, scheduled booking jobs, and payment/cancel notifications.

### Timeline

| Day | Work | Status |
|---|---|---|
| 1 | Testing multithreading and seeing it fail | Done |
| 2 | Implementing locks | Done |
| 3 | Implementing Stripe | Done |
| 4 | Stripe session lock, calendar dates, updated search | Done |
| 5 | Integration test and refunds | Done |
| 6 | Notifications, audit logs, scheduled jobs, API docs, cleanup | Done |
| 7 | Documentation | In progress |

### Tasks and Progress

**Day 1: Testing multithreading and seeing it fail**
- [x] Unit test that approves two overlapping bookings from two threads (`ExecutorService`, `AtomicInteger`)
- [x] Reproduce the race condition: both approvals succeed and the car is double-booked

**Day 2: Implementing locks**
- [x] One `ReentrantLock` per car (`ConcurrentHashMap`) around the availability check and save in `approveBooking`
- [x] The unit test now passes: exactly one approval, the other gets a `ConflictException`
- [x] Asynchronous emails and notifications on two thread pools (`ThreadsConfig`, `@Async`)

**Day 3: Implementing Stripe**
- [x] `Payment` model and repository (status `PENDING` / `PAID` / `FAILED`)
- [x] `POST /pay/{bookingId}`: create a Stripe Checkout session
- [x] `POST /pay/webhook`: verify the signature and mark the payment paid or failed

**Day 4: Stripe session lock, calendar dates and search**
- [x] Pessimistic lock on the booking (`findByIdForUpdate`) + pending/paid checks, so a double click cannot create two sessions
- [x] Only approved bookings can be paid; the amount is calculated on the server
- [x] `GET /bookings/car/{carId}/dates` for the booking calendar
- [x] `Country` and `City` models, seeded from a JSON dataset; cars linked to a city instead of a text location
- [x] `GET /countries` and `GET /cities?countryId=`
- [x] Combined car search with JPA Specifications, including availability between two dates (`NOT EXISTS` subquery)

**Day 5: Integration test and refunds**
- [x] Integration test: N concurrent `PATCH /bookings/{id}/approve` requests (`CountDownLatch`, `Future`) → 1 × `200`, N−1 × `409`
- [x] `paid` flag on bookings, set by the webhook
- [x] Full refund through Stripe when a paid booking is cancelled, with an idempotency key; `refunded` flag and `REFUNDED` status

**Day 6: Notifications, audit logs, scheduled jobs and cleanup**
- [x] Notifications and audit logs for payments (received, paid, failed, refunded) and cancellations
- [x] Daily scheduled jobs: complete finished bookings, reject unanswered requests
- [x] Swagger summaries and JavaDoc for the new and changed endpoints
- [x] Database cleanup: drop the old `location` column, `city_id NOT NULL`, fix enum check constraints

**Day 7: Documentation**
- [x] README: Project 3 changes, concurrency explanation, testing, updated API, challenges, credits
- [x] Race condition write-ups (approval and double click on "Pay") in `docs/race-condition.md`
- [x] Updated planning document
- [ ] Updated ERD (countries, cities, payments)
- [ ] Final testing and presentation preparation

### Changes by Area
- **Concurrency:** per-car `ReentrantLock` for approvals, pessimistic database lock for checkout, Stripe idempotency key for refunds, single-statement updates in the scheduled jobs
- **Database:** new tables `countries`, `cities`, `payments`; `cars.city_id` replaces `location`; `bookings.paid` and `bookings.refunded`
- **API:** new `/countries`, `/cities`, `/bookings/car/{carId}/dates`, `/pay/{bookingId}`, `/pay/webhook`; changed `GET /cars/` (filters), car create/update (`cityId`), cancel (refund)
- **Testing:** unit test and integration test for concurrent approvals, manual Stripe tests with the test card and `stripe listen`

## Project 4: Microservices and Cloud

### Scope
Split the application into independently deployable microservices, connect them with Apache Kafka, containerise them, and deliver them to AWS through infrastructure as code and an automated CI/CD pipeline.

### Timeline

| Day | Work | Status |
|---|---|---|
| 1 | Identify microservice boundaries, design the architecture, split the monolith into six services | Done |
| 2 | Dockerfile, Docker Compose, container networking | Done |
| 3 | Kafka producers and consumers, event-driven workflow | Done |
| 4 | AWS infrastructure with Terraform | Done |
| 5 | New features and tests | Done |
| 6 | CI/CD with Jenkins | Done |
| 7 | Deployment, monitoring and logging | Done |
| 8 | Fix issues, documentation, final presentation | In progress |

### Tasks and Progress

**Day 1: Microservice boundaries, architecture and refactoring**
- [x] Identify the service boundaries by business capability: users, cars, bookings, payments, notifications, audit
- [x] Design the architecture: database ownership per service, REST for calls that need an answer, Kafka for the rest
- [x] Split the monolith: user-service (8081), car-service (8082), booking-service (8083), payment-service (8084), notification-service (8085), audit-service (8086)
- [x] `common` module: JWT filter, security configuration, global exception handler, shared DTOs
- [x] Synchronous calls between services with `RestClient` (booking → car, payment → booking)
- [x] Internal endpoints protected with an internal API key
- [x] One `.env` per service for secrets

**Day 2: Docker**
- [x] One multi-stage `Dockerfile`, parameterised by service name and port
- [x] `docker-compose.yml` with PostgreSQL, Kafka and the six services
- [x] Container networking: services reach each other by name (`car-service:8082`, `kafka:19092`)
- [x] Health checks, so services start only after the database and Kafka are ready
- [x] Persistent volume for the database and bind mounts for uploaded images

**Day 3: Kafka and event-driven workflow**
- [x] Kafka 4 in KRaft mode (no ZooKeeper), with an internal and an external listener
- [x] Topics `audit-events`, `notification-events` and `booking-events`, with event records in `common/events`
- [x] Audit logs and notifications moved from REST calls to events
- [x] `booking-events` published on every booking state change and consumed by three services
- [x] `eventId` on audit and booking events, and duplicate audit events ignored
- [x] Old REST audit and notification endpoints removed

**Day 4: AWS infrastructure**
- [x] Terraform: VPC, subnets and security groups
- [x] RDS PostgreSQL in a private subnet
- [x] EC2 host with Docker installed by `user_data`, IMDSv2, and access through SSM (no SSH)
- [x] ECR repositories with scan on push, immutable tags and lifecycle rules
- [x] Secrets in SSM Parameter Store, read through the instance IAM role
- [x] S3 and CloudFront for the frontend, with `/api/*` routed to the services
- [x] Production compose file without a database (RDS) and with memory limits

**Day 5: Features and tests**
- [x] Car reservations read model in car-service, fed by `booking-events` (CQRS)
- [x] Admin pages: users, bookings with filters, payments, catalogue, audit log
- [x] Payment history for customers and agencies
- [x] Booking search with JPA Specifications
- [x] Unit tests for the booking service (JUnit 5, Mockito) and a concurrency test for overlapping approvals
- [x] Ten Cucumber scenarios for the booking lifecycle

**Day 6: CI/CD**
- [x] Jenkins in Docker with the Docker CLI, AWS CLI and Node.js
- [x] Pipeline: build `common`, test the services in parallel, publish JUnit reports, build images
- [x] Release script: push images to ECR, deploy with SSM Run Command, publish the frontend to S3, invalidate CloudFront
- [x] Jenkins uses an IAM role instead of stored access keys

**Day 7: Deployment, monitoring and logging**
- [x] `terraform apply` of the full environment and first release
- [x] Stripe webhook endpoint registered for the public URL
- [x] Automatic deployment on every push to `main`
- [x] CloudWatch log group for all containers, error metric filter, CPU and status-check alarms, optional SNS email alerts
- [x] Checked on the live site: registration, login, car search, bookings and the Stripe webhook

**Day 8: Fixes, documentation and presentation**
- [x] Fixes found after deployment: email links pointed to localhost, 500 on a duplicate username, verify response not JSON
- [x] Swagger UI with JWT authorization on every service
- [x] README, REST API reference, Kafka event documentation and deployment guide
- [ ] Updated ERD and architecture diagram
- [ ] Move the approval lock to the database for multiple instances
- [ ] Remove the old monolith code from the repository
- [ ] Final presentation

## Way of Working

- Each feature was built on its own Git branch and merged into `main` when it worked.
- Inside a feature, the order was always entity and repository, then service, then controller, with a commit after each step.
- A cleanup pass was done before the Swagger documentation so that every endpoint had consistent return types and status codes.
