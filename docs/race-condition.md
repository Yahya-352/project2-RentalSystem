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

## Limitations

- **Single server only.** A `ReentrantLock` lives in the memory of one running application. If RentRide ran on several servers, each server would have its own locks, and two servers could still approve overlapping bookings. In that case, the lock should move to the database, for example a pessimistic lock (`@Lock(PESSIMISTIC_WRITE)`, which Postgres runs as `SELECT ... FOR UPDATE`) on the car row inside a `@Transactional` method.
- **Booking status is read before the lock.** The booking is loaded before the lock is taken, so two approvals of the **same** booking at the same moment both see it as `PENDING`. Loading the booking again inside the lock would close this gap.
- **Only approvals are protected.** `cancelBooking` and `rejectBooking` do not take the car's lock, so an approval and a cancellation of the same booking at the same moment can still overwrite each other. Optimistic locking with a `@Version` field on `Booking` would solve this.
- **The lock map grows.** One lock is kept for every car that has ever been approved. This is fine for the size of this application.
