# RentRide — Rental System Frontend

React 19 + TypeScript + Vite web client for the Spring Boot backend in the repository root
(one level above this folder). Prices are shown in Bahraini Dinar (BHD).

## Requirements

- **Node.js 18, 20 or 22+** (npm comes with it) — check with `node -v`
- **The backend running** on `http://localhost:8080`, with its PostgreSQL database and
  environment variables set up. Follow the Installation section of the backend README
  (`README.md` in the repository root) first.

## Run it

```bash
# 1. start the backend (see its README) and wait until it has started
# 2. in a second terminal, from the repository root:
cd frontend
npm install        # first time only: downloads the dependencies
npm run dev        # starts the app on http://localhost:5173
```

Open **http://localhost:5173**. Stop it with `Ctrl + C`. Keep the terminal open while using it.

Backend on another host or port? Copy `.env.example` to `.env` and set `BACKEND_URL`.

### Test accounts and sample data

On first start, the backend's `DataSeeder` creates these accounts (one-click buttons on the
login page in development), 9 makes, 6 categories, 6 cars owned by the agency (plates
`SEED-001`…`SEED-006`) and 6 bookings by the customer covering every status.

| Role     | Email                 | Password    |
|----------|-----------------------|-------------|
| Customer | customer@rental.com   | Test1234!   |
| Agency   | agency@rental.com     | Test1234!   |
| Admin    | value of `ADMIN_USERNAME` (e.g. admin@rental.com) | value of `ADMIN_PASSWORD` (e.g. Admin123!) |

Seeding only adds what is missing, so restarting never duplicates data. Registering a new
account sends a verification email to the Mailtrap inbox configured for the backend.

## Features by role

| Role     | What they can do |
|----------|------------------|
| Everyone | Log in, register (customer or agency), verify email, forgot / reset password, change password, live notification bell |
| Customer | Browse cars (filter by location, category or make; sort; paging), swipeable car photos, car details with a photo gallery, request a booking with a price preview, view / cancel own bookings, profile with photo upload |
| Agency   | My fleet: add / edit / delete cars, upload several photos at once; booking requests: approve / reject / cancel, with stats; agency profile with logo upload |
| Admin    | All bookings (status filter, paging), activate / deactivate users and approve agencies by user id |

Booking events (created, approved, rejected) arrive live as pop-ups and in the bell menu, and
refresh the open bookings page.

## How it talks to the backend

The backend has no CORS configuration, so the browser never calls `:8080` directly. Every
request goes to `/api/...` on the Vite server, which forwards it to the backend with the `/api`
prefix removed (`vite.config.ts`). For a production deployment, serve the built `dist/` folder
behind a reverse proxy (nginx, etc.) that does the same `/api` → backend mapping.

- **Login** — the JWT from `POST /users/login` is kept in `localStorage` and sent as
  `Authorization: Bearer …`. The app logs out when the token expires or the backend rejects it.
- **Images** — car photos (`GET /images/{id}`), the customer photo (`GET /profiles/customer/picture`)
  and the agency logo (`GET /profiles/agency/logo`) require the JWT, which an `<img>` tag can't
  send, so the app downloads them with the token and caches them for the session. Card
  carousels only load the visible photo and the next one.
- **Makes and categories** — loaded from `GET /makes` and `GET /categories` once per page load.
- **Live notifications** — `GET /notifications/stream` (Server-Sent Events) is read with `fetch`
  because `EventSource` can't send an `Authorization` header; it reconnects automatically.

## Backend changes made for this frontend

Everything else in the backend was already there or was written by the backend's author.

- `POST /profiles/agency/logo` — agency logo upload (`AgencyProfileController`,
  `AgencyProfileService.uploadLogo`), a copy of the customer picture upload.
- `DataSeeder.seedCars()` and `seedBookings()` — the sample cars and bookings above.

## Known limitations

1. **Car photos can't be deleted** — the backend has no endpoint for it.
2. **No list of users** — the admin activates / deactivates users by typing their id.
3. **Car search uses one filter at a time** (location, else category, else make), so the UI
   offers one filter at a time.
4. **Browsing requires a login** — `GET /cars/` is protected.
5. **Email links point at `:8080`** — the verify link works as is; for password reset, paste the
   token (or the whole link) into the *Reset password* page.
6. **Bookings never become COMPLETED automatically** and cancelling sends no notification —
   both are backend behaviour.

## Troubleshooting

| Problem | Cause / fix |
|---------|-------------|
| "Cannot reach the server. Is the backend running?" | Start the backend on port 8080, or set `BACKEND_URL` in `.env`. |
| Every login says "Invalid email or password" and takes ~30 s | The backend ran out of database connections: each open notification stream holds one and never returns it. Restart the backend. Permanent fix: in `NotificationController.stream`, take the user from `((MyUserDetails) authentication.getPrincipal()).getUser()` instead of querying the database. |
| "Too many requests, try again in N seconds" | The backend allows 5 login / register / password requests per minute per address. Wait a minute. |
| App opens on port 5174 instead of 5173 | Another copy is already running on 5173. Stop it, or just use 5174. |
| "X is not defined" right after editing code | The dev server picked up a half-saved file. Stop it (`Ctrl + C`), run `npm run dev` again and hard-reload the browser (`Ctrl + F5`). |

## Project layout

```
src/
  api/            client.ts (fetch wrapper, errors), endpoints.ts (one function per endpoint), types.ts (DTOs)
  auth/           AuthContext (session / JWT), RequireAuth (role-based routes), ProfileImageContext (avatar image)
  notifications/  live notification stream + pop-ups
  components/     Layout (nav, bell, user menu), CarForm, CarPhoto (swipeable carousels),
                  PhotoPicker (photo / logo upload), shared UI pieces
  pages/          auth/, customer/, agency/, admin/, AccountPage
  lib/            formatting (BHD), enums, useAsync and useCatalog hooks
```

Scripts: `npm run dev` (development server), `npm run build` (type check + production build into
`dist/`), `npm run preview` (serve the production build on port 4173, also proxying `/api`).
