# Race Condition: Approving Overlapping Bookings

This document describes a race condition in RentRide's booking approval flow, what goes wrong without concurrency control, and how the application prevents it.

## Background

In RentRide, a customer requests a car for a date range, and the booking is saved as `PENDING`. Several customers can request the same car for the same dates, because pending bookings do not block a car. The agency that owns the car then approves one of them with `PATCH /bookings/{id}/approve`, which calls `BookingService.approveBooking`.

The business rule is: **a car can never have two `APPROVED` bookings with overlapping dates.**

`approveBooking` enforces this rule in two steps:

1. **Check:** `isCarAvailable` loads all bookings for the car and makes sure no `APPROVED` booking overlaps the requested dates.
2. **Act:** if the car is free, the booking's status is set to `APPROVED` and saved.

## 1. What shared resource is being accessed

The shared resource is **the set of approved bookings for a single car**, which is stored in the `booking` table.

Every approval for a car reads this set (to check availability) and then writes to it (by saving a new `APPROVED` booking). Two approvals for the same car therefore read and write the same data.

## 2. Why multiple operations can occur at the same time

Spring Boot handles each HTTP request on its own thread from the server's thread pool. Two approval requests that arrive at about the same time run **in parallel**, on two threads, inside the same `BookingService` instance.

This can happen in RentRide when:

- an agency has two pending requests for the same car and dates, and approves both quickly (for example from two browser tabs), or
- two staff members of the same agency approve different requests for the same car at the same moment, or
- a slow network causes the same approve action to be sent twice.

## 3. What race condition could occur

This is a **check-then-act** race condition. The check (`isCarAvailable`) and the act (`save`) are two separate steps, and without protection another thread can run between them.

Example: bookings **#10** and **#11** are both `PENDING` for car **#1**, for the same dates.

| Step | Thread A: approve booking #10 | Thread B: approve booking #11 |
|---|---|---|
| 1 | Loads booking #10 | Loads booking #11 |
| 2 | `isCarAvailable` → no approved booking → **free** | |
| 3 | | `isCarAvailable` → no approved booking yet → **free** |
| 4 | Saves #10 as `APPROVED` | |
| 5 | | Saves #11 as `APPROVED` |

Both threads passed the check, because each one checked **before** the other had saved. Neither thread did anything wrong on its own; the result depends only on the timing of the two threads.

## 4. What incorrect result could occur without concurrency control

Car #1 ends up with **two approved bookings for the same dates**, which means it is double-booked:

- Both customers receive a `BOOKING_APPROVED` notification and believe they have the car.
- On the start date, two customers arrive for one car, and the agency has to cancel one of them.
- The data in the database breaks the main business rule of the system, and nothing in the application reports an error.

This bug is hard to notice in testing, because it only appears when two requests overlap in a window of a few milliseconds.

## 5. How my implementation prevents the problem

### The idea

The check and the save must happen as **one indivisible step** for each car. While one thread is checking and approving a booking for car #1, no other thread may check or approve a booking for car #1.

### One lock per car

`BookingService` keeps one `ReentrantLock` per car:

```java
private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();
```

In `approveBooking`, the thread first finds out which car the booking belongs to, then takes that car's lock before checking availability:

```java
ReentrantLock lock = locks.computeIfAbsent(booking.getCar().getId(), id -> new ReentrantLock());
lock.lock();
try {
    // status check
    // availability check
    // save as APPROVED
} finally {
    lock.unlock();
}
// audit log + notification
```

### How this fixes the timeline

| Step | Thread A: approve booking #10 | Thread B: approve booking #11 |
|---|---|---|
| 1 | Takes the lock for car #1 | Tries to take the lock for car #1 → **waits** |
| 2 | `isCarAvailable` → **free** | (waiting) |
| 3 | Saves #10 as `APPROVED` | (waiting) |
| 4 | Releases the lock | Takes the lock |
| 5 | | `isCarAvailable` → sees #10 is approved → **not free** |
| 6 | | Throws `ConflictException` (HTTP 409) |

Exactly one booking is approved, and the second agency request gets a clear conflict error.

### Design decisions

- **Why one lock per car, not one lock for everything.** A single lock would make every approval in the system wait for every other approval, even for different cars. Approvals for different cars cannot conflict, so they should not block each other. The `ConcurrentHashMap` maps each car ID to its own lock.
- **Why a `ConcurrentHashMap`.** Many threads use the map at the same time. `computeIfAbsent` is atomic, so two threads asking for the lock of the same car at the same moment are guaranteed to receive the **same** lock object. With a plain `HashMap`, both threads could create their own lock, and the race would come back.
- **Why the lock is a field, not a local variable.** A lock only works if all threads share the same lock object. A lock created inside the method would be new for every call and would protect nothing.
- **Why `try/finally`.** If the booking is not pending, or the car is not available, the method throws an exception while holding the lock. The `finally` block makes sure the lock is always released. Without it, the first rejected approval would keep the car's lock forever, and every later approval for that car would hang.
- **Why the audit log and notification are outside the lock.** The lock only needs to cover the steps that decide whether to approve: the check and the save. Once the booking is saved, the next thread will see it. Writing the audit log and sending notifications can be slow, so they run after the lock is released to keep the critical section short. They only run if no exception was thrown, so a notification is only sent for a booking that was really approved.
- **Why `ReentrantLock` and not a `ReadWriteLock`.** A read-write lock helps when many threads only read and a few write. Here every approval reads **and then** writes, and both steps must be exclusive, so a read lock would allow the same race. `ReentrantLock` also makes the critical section explicit and supports `tryLock` with a timeout if needed later.

### Proof: the concurrency test

`BookingConcurrencyTest.approveBooking_twoOverlappingApprovalsAtTheSameTime_onlyOneIsApproved` reproduces the race:

- Two `PENDING` bookings for the same car and dates are approved from two threads at the same time.
- The mocked `findByCarId` returns a snapshot of the bookings and then waits 500 ms, which simulates a slow database read and widens the gap between check and save.
- The mocked `save` writes the new status back, so the second thread can see the first approval, like a real database.

With the lock, exactly one approval succeeds. When the lock is removed, both approvals succeed, which shows the test really detects the race condition.

### Proof: concurrent REST requests (integration test)

The unit test above checks the service with mocked repositories. `BookingControllerConcurrencyIntegrationTest` checks the whole application instead: real HTTP requests go through the security filter (JWT), the controller, `BookingService` and a real PostgreSQL database (the `test` profile).

**Setup.** Before each test, it creates an agency, a customer and one car, then **N `PENDING` bookings on the same car and the same dates**, and logs in as the agency to get a real JWT. All N bookings compete for the same car, so only one of them can be approved.

**Firing the requests at the same moment.** If the requests were simply sent in a loop, the first one could finish before the last one starts, and the race would never happen. The test uses two `CountDownLatch` objects so that all requests really run at the same time:

```java
ExecutorService service = Executors.newFixedThreadPool(N);
CountDownLatch ready = new CountDownLatch(N);   // "I am at the starting line" × N
CountDownLatch start = new CountDownLatch(1);   // the starting gun

for (int i = 0; i < N; i++) {
    Long id = bookingIds.get(i);                // each thread approves a different booking
    futures.add(service.submit(() -> {
        ready.countDown();
        start.await();                          // every thread waits here...
        return mockMvc.perform(patch("/bookings/{id}/approve", id)
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getStatus();   // ...then sends its request
    }));
}
ready.await();       // wait until all N threads are waiting at the line
start.countDown();   // release them together
```

- The **pool has N threads**, one per request. With fewer threads, some tasks would wait in the queue and never reach the starting line, and `ready.await()` would wait forever.
- Each task is a `Callable<Integer>` that **returns** the HTTP status instead of checking it inside the thread. An assertion that fails inside a worker thread is lost; a returned value can be checked by the main thread.
- The main thread collects every result with `future.get(10, TimeUnit.SECONDS)`. This waits for the requests to finish (so the test cannot end while requests are still running), re-throws any error from a worker thread, and fails instead of hanging forever if something gets stuck.
- The pool is shut down in a `finally` block, so its threads are released even when an assertion fails.

**The assertions.** The business rule is checked through the HTTP responses:

```java
assertEquals(1, Collections.frequency(statuses, 200), "statuses: " + statuses);
assertEquals(N - 1, Collections.frequency(statuses, 409), "statuses: " + statuses);
```

Exactly one request is approved (`200 OK`), and every other one gets `409 Conflict` ("Car is not available at this date"). The message prints the full list of statuses, so a failure shows exactly what each request received.

**Example run with N = 10.** Each thread prints its result when it finishes:

```
pool-3-thread-3  -> booking 108 -> 409
pool-3-thread-10 -> booking 115 -> 409
pool-3-thread-4  -> booking 109 -> 409
pool-3-thread-1  -> booking 106 -> 409
pool-3-thread-7  -> booking 112 -> 409
pool-3-thread-5  -> booking 110 -> 200
pool-3-thread-6  -> booking 111 -> 409
pool-3-thread-8  -> booking 113 -> 409
pool-3-thread-9  -> booking 114 -> 409
pool-3-thread-2  -> booking 107 -> 409
```

The order changes on every run, because the threads really run in parallel and any of them can get the lock first. The winner (thread 5 here) is not the first line: after saving and releasing the lock, it still writes the audit log and sends the notification, while the other threads take the lock one by one, see the approved booking and fail immediately.

**With and without the lock.** With the per-car lock, exactly one request is approved on every run. Without it (`lock.lock()` and `lock.unlock()` commented out), the check and the save are no longer one step: several threads can pass `isCarAvailable` before any of them has saved, more than one request returns `200`, and the car ends up with more than one `APPROVED` booking for the same dates. The assertions then fail, which shows the test detects the race condition and that the lock is what prevents it.

## Limitations

- **Single server only.** A `ReentrantLock` lives in the memory of one running application. If RentRide ran on several servers, each server would have its own locks, and two servers could still approve overlapping bookings. In that case, the lock should move to the database, for example a pessimistic lock (`@Lock(PESSIMISTIC_WRITE)`, which Postgres runs as `SELECT ... FOR UPDATE`) on the car row inside a `@Transactional` method.
- **Booking status is read before the lock.** The booking is loaded before the lock is taken, so two approvals of the **same** booking at the same moment both see it as `PENDING`. Loading the booking again inside the lock would close this gap.
- **Only approvals are protected.** `cancelBooking` and `rejectBooking` do not take the car's lock, so an approval and a cancellation of the same booking at the same moment can still overwrite each other. Optimistic locking with a `@Version` field on `Booking` would solve this.
- **The lock map grows.** One lock is kept for every car that has ever been approved. This is fine for the size of this application.

---

# Race Condition: Double Click on "Pay"

This section describes a second race condition, in the payment flow: a customer paying twice for the same booking.

## Background

Once an agency approves a booking, the customer pays for it with Stripe. Clicking **Pay now** calls `POST /pay/{bookingId}`, which runs `StripeService.checkout`:

1. **Check:** the booking must belong to the logged-in customer, be `APPROVED`, and must not already have a `PENDING` or `PAID` payment.
2. **Act:** create a Stripe Checkout session (a call to Stripe's API) and save a new `Payment` row with status `PENDING` and the session's id.

The browser is then sent to Stripe's payment page. When the customer pays, Stripe calls the webhook, which marks the payment `PAID` and the booking as paid.

The business rule is: **a booking can have at most one open or completed payment.**

## 1. What shared resource is being accessed

The shared resource is **the set of payments of one booking**, stored in the `payments` table. Every checkout for a booking reads it (are there `PENDING` or `PAID` payments?) and then writes to it (inserts a new `PENDING` payment).

## 2. Why multiple operations can occur at the same time

Each HTTP request runs on its own server thread, so two checkout requests for the same booking run in parallel. In RentRide this happens when:

- the customer **double-clicks** "Pay now" (two requests leave the browser a few milliseconds apart),
- the customer opens the booking in two tabs and clicks "Pay" in both, or
- a slow network makes the browser or a proxy send the request again.

Creating the Stripe session is a network call that takes hundreds of milliseconds, so the gap between the check and the save is large, which makes this race easy to hit.

## 3. What race condition could occur

This is again a **check-then-act** race condition. Example: booking **#3** is `APPROVED` and has no payments yet.

| Step | Request A | Request B |
|---|---|---|
| 1 | Checks payments of #3 → none | |
| 2 | | Checks payments of #3 → none (A has not saved yet) |
| 3 | Creates Stripe session **S1** | Creates Stripe session **S2** |
| 4 | Saves payment (S1, `PENDING`) | Saves payment (S2, `PENDING`) |

Both requests passed the check, because each checked before the other saved.

## 4. What incorrect result could occur without concurrency control

- The booking has **two `PENDING` payments** and two open Stripe sessions.
- If the customer completes both (for example both tabs), they are **charged twice** for one booking.
- The rule "one payment per booking" is broken in the database, and refunds become unclear (which payment should be refunded?).

## 5. How my implementation prevents the problem

### Why the lock is on the booking, not on the payments

The natural idea is to lock "the payments of booking #3". But at the moment of the race, those payments **do not exist yet**: both requests are about to *insert* a row, and a database can only lock rows that already exist. Both requests do read the same **booking** row, though. So the booking row is used as the lock for its own payments: whoever locks booking #3 first is the only one allowed to check and create its payment.

### A pessimistic database lock

`BookingRepository` has a query that loads the booking and locks its row:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select b from Booking b where b.id = :id")
Optional<Booking> findByIdForUpdate(Long id);
```

PostgreSQL runs it as `SELECT ... FOR UPDATE`. The row stays locked until the transaction ends, and any other transaction that tries to lock the same row **waits**.

`checkout` loads the booking with this method, and `StripeService` is `@Transactional`, so the lock is held for the whole checkout, through the payment checks, the Stripe call and the save:

```java
Booking booking = bookingRepository.findByIdForUpdate(bookingId)   // locks booking #3
        .orElseThrow(() -> new InformationNotFoundException("Booking not found"));
// ...ownership and APPROVED checks...
if (paymentRepository.existsByBookingIdAndStatus(bookingId, PaymentStatus.PENDING)) {
    throw new ConflictException("Booking " + bookingId + " already has a pending payment");
}
if (paymentRepository.existsByBookingIdAndStatus(bookingId, PaymentStatus.PAID)) {
    throw new ConflictException("Booking " + bookingId + " is already paid");
}
// create the Stripe session, save the PENDING payment
// the transaction commits here and the lock is released
```

### How this fixes the timeline

| Step | Request A | Request B |
|---|---|---|
| 1 | Locks booking #3 | Tries to lock booking #3 → **waits** |
| 2 | Checks payments → none | (waiting) |
| 3 | Creates Stripe session S1, saves payment `PENDING` | (waiting) |
| 4 | Commits, releases the lock | Locks booking #3 |
| 5 | | Checks payments → finds S1 `PENDING` |
| 6 | | Throws `ConflictException` → **409** "already has a pending payment" |

Only one Stripe session is created, and the second click gets a clear error. In the frontend, the "Pay now" button is also disabled while the first request is running, but that alone is not enough: two tabs, a retry or a direct API call would still get through, so the protection has to be on the server.

### Design decisions

- **Why a database lock here, when approvals use a `ReentrantLock`.** A database lock works across several running instances of the application, because every instance talks to the same database. It also comes with the transaction: if anything fails, the lock is released and the payment row is rolled back together. For payments, where a mistake costs money, that guarantee matters more.
- **Why `PESSIMISTIC_WRITE` and not optimistic locking.** With optimistic locking (`@Version`), both requests would run fully, including creating **two Stripe sessions**, and only the second save would fail at the end. The Stripe session created by the losing request would already exist. A pessimistic lock makes the second request wait **before** it talks to Stripe.
- **Why the lock is held during the Stripe call.** The check and the save must be one step. Releasing the lock before calling Stripe would reopen the gap.
- **Why both `PENDING` and `PAID` are checked.** `PENDING` stops a second session while the first is still open; `PAID` stops paying again after a successful payment.

## Limitations

- **The lock is held during a network call.** While Stripe answers (usually well under a second), the booking row stays locked, and any other checkout for the same booking waits. This is acceptable because it only affects that one booking.
- **Only checkout takes the lock.** Other operations on the booking, such as a cancellation, load it without `findByIdForUpdate`, so they do not wait for a checkout in progress. A cancellation at the same moment as a checkout is therefore not protected by this lock.
- **An abandoned checkout blocks retries.** If the customer closes Stripe's page without paying, the payment stays `PENDING` until Stripe expires the session, which takes 24 hours by default, and every new checkout for that booking gets `409`. Setting a short expiry on the session (30 minutes is Stripe's minimum) would fix this.
- **Duplicate webhooks are not locked.** Stripe can deliver the same webhook event more than once. The webhook only changes payments that are still `PENDING`, so a repeated event normally has no effect, but two copies processed at exactly the same moment are not protected by a lock.
