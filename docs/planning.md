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

## Way of Working

- Each feature was built on its own Git branch and merged into `main` when it worked.
- Inside a feature, the order was always entity and repository, then service, then controller, with a commit after each step.
- A cleanup pass was done before the Swagger documentation so that every endpoint had consistent return types and status codes.
