# RentRide Frontend

Single-page web client for the RentRide microservices, built with React 19, TypeScript and Vite. Prices are shown in Bahraini Dinar (BHD).

For the overall architecture and backend setup, see the [main README](../README.md).

## Requirements

- **Node.js 18 or newer** (check with `node -v`)
- **The backend running.** The simplest way is `docker compose up -d` from the repository root, which starts PostgreSQL, Kafka and the six services on ports 8081–8086.

## Run it

```bash
cd frontend
npm install        # first time only
npm run dev        # http://localhost:5173
```

| Script | Purpose |
|---|---|
| `npm run dev` | Development server with hot reload |
| `npm run build` | Type check and production build into `dist/` |
| `npm run preview` | Serve the production build on port 4173 (also proxies `/api`) |

## Configuration

Copy `.env.example` to `.env` (git ignores it) to override the defaults:

| Variable | Default | Purpose |
|---|---|---|
| `USER_SERVICE_URL` … `AUDIT_SERVICE_URL` | `http://localhost:8081` … `8086` | Where the dev proxy forwards each service's paths |
| `VITE_API_BASE` | `/api` | Prefix for every API call |
| `VITE_DEMO_CUSTOMER_EMAIL`, `VITE_DEMO_AGENCY_EMAIL`, `VITE_DEMO_ADMIN_EMAIL` | seeded emails | Accounts behind the one-click demo buttons on the login page |
| `VITE_DEMO_SEED_PASSWORD`, `VITE_DEMO_ADMIN_PASSWORD` | empty | Passwords for those buttons. They must match `SEED_PASSWORD` and `ADMIN_PASSWORD` in user-service |

## How it talks to the backend

The browser only ever calls its own origin under `/api`. The first path segment decides which service receives the request, and the `/api` prefix is removed before forwarding:

| Path | Service |
|---|---|
| `/api/users`, `/api/profiles` | user-service (8081) |
| `/api/cars`, `/api/images`, `/api/makes`, `/api/categories`, `/api/countries`, `/api/cities` | car-service (8082) |
| `/api/bookings` | booking-service (8083) |
| `/api/pay` | payment-service (8084) |
| `/api/notifications` | notification-service (8085) |
| `/api/audit-logs` | audit-service (8086) |

- **Development:** the Vite proxy (`vite.config.ts`) does the routing.
- **Production:** the built app is hosted on S3. CloudFront serves it and routes `/api/*` to the services; a CloudFront function strips the prefix. Releases are automated by the CI/CD pipeline (see [docs/DEPLOYMENT.md](../docs/DEPLOYMENT.md)).

Implementation notes:

- **Authentication.** The JWT from `POST /users/login` is stored in `localStorage` and sent as `Authorization: Bearer …`. The app logs out when the token expires or is rejected.
- **Protected images.** Car photos, profile pictures and agency logos need the JWT, which an `<img>` tag cannot send. The app downloads them with the token and caches them for the session.
- **Live notifications.** `GET /notifications/stream` (Server-Sent Events) is read with `fetch`, because `EventSource` cannot send an `Authorization` header. The stream reconnects automatically.
- **Payments.** "Pay now" opens Stripe Checkout. Stripe then redirects back to the payment success or cancel page.

## Features by role

| Role | Pages and actions |
|---|---|
| Everyone | Log in, register as a customer or agency, verify email, forgot and reset password, change password, live notification bell |
| Customer | Search cars with combined filters and date availability, car details with a photo gallery and an availability calendar, request a booking with a price preview, pay with Stripe, view and cancel bookings, payment history, profile with photo |
| Agency | Manage the fleet (add, edit and delete cars, upload several photos at once), approve, reject or cancel booking requests, payments received, agency profile with logo |
| Admin | Users (search, filter, activate, deactivate, approve agencies), all bookings with filters, all payments, catalogue of makes and categories, audit log |

## Project layout

```
src/
  api/            client.ts (fetch wrapper, errors), endpoints.ts (one function per endpoint), types.ts (DTOs)
  auth/           AuthContext (session and JWT), RequireAuth (role-based routes), profile image context
  notifications/  live notification stream and pop-ups
  components/     Layout, CarForm, CarPhoto (carousels), AvailabilityCalendar, PhotoPicker, shared UI pieces
  pages/          auth/, customer/, agency/, admin/, AccountPage, MyPaymentsPage
  lib/            formatting (BHD), constants, useAsync, useCatalog and useLocations hooks
```

## Troubleshooting

| Problem | Cause and fix |
|---|---|
| "Cannot reach the server" | A service is not running. Check it with `docker compose ps`, or point the matching `*_SERVICE_URL` at the right address. |
| Demo buttons fail to log in | `VITE_DEMO_*` passwords do not match the backend's `SEED_PASSWORD` or `ADMIN_PASSWORD`. |
| "Too many requests, try again in N seconds" | user-service rate-limits login and registration per client address. Wait a minute. |
| Payment stays unpaid | Stripe cannot reach the webhook. Locally, run `stripe listen --forward-to localhost:8084/pay/webhook`. |
| App opens on port 5174 | Another dev server is already running on 5173. |
