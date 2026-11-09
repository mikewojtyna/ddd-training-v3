# Exercise 12: reference answers

The agent's commit is `ex12/start`. Line numbers refer to that commit. The fix is the commit `ex12/solution`.

## The three planted defects

### (a) Spec rule R3 has no test

- **Where:** `docs/specs/rider-cancels-reservation.md:32` (E3.1). Neither `RiderCancelsReservationSpecTest` nor
  `BikeCancellationTest` covers it.
- **Rule broken:** every spec example ends up as an acceptance test (README, "The AI-assisted DDD flow").
- **Why it matters:** R3 *is* implemented (`Bike.java:290-292` throws `NoReservationToCancel`), so nothing is broken
  today. But nothing stops the next change from breaking it. Let a running session be cancelled too (for example,
  "a rider may cancel within the first minute of a ride") and every test stays green, while the cancellation frees
  a bike somebody is sitting on.
- **Fix:** `RiderCancelsReservationSpecTest#e3_1_ride_in_progress_cannot_be_cancelled` and
  `BikeCancellationTest#rideInProgressCannotBeCancelled`.

### (b) Rule R2 lives in the REST controller, not in the domain

- **Where:** `RentalsController.java:142-148`. The controller reads the read model (`BikeAvailabilityQuery`) and
  decides who may cancel. `Bike.cancelReservationOf(Holder rider)` (`Bike.java:288`) **ignores its `rider`
  parameter** and cancels anybody's reservation.
- **Rule broken:** business decisions belong in `domain/`. Adapters only translate (README, "Code layout";
  `CLAUDE.md`).
- **Why it matters:**
  - Any other entry point (a message listener, a second endpoint, the next use case that reuses
    `cancelReservationOf`) can cancel **any** reservation, including a technician's reservation of a broken bike.
  - Check-then-act across a read model and a command isn't atomic: between the query and the command, the
    reservation can expire and another rider can reserve the bike. The command then cancels the *new* rider's
    reservation.
  - The REST test E2.1 passes, so the test suite doesn't reveal the problem. Only reading the code does. The hint
    is the unused parameter.
- **Fix:** the holder check moves into `Bike.cancelReservationOf` (throws `NotReservationHolder`). The controller
  only calls the use case. `BikeCancellationTest#otherRiderCannotCancel` proves the check at the domain level.

### (c) A test asserts the implementation instead of the behaviour

- **Where:** `BikeCancellationTest.java:23-24` asserts `snapshot().hold()` equals `Hold.Free` and that the version
  didn't change.
- **Rule broken:** tests express observable behaviour that the spec defines. Here that's *Reservation Cancelled* and
  *Ben can reserve the bike at once*.
- **Why it matters:**
  - It pins an internal representation. Rename `Hold.Free`, or keep the holder on a `Cancelled` hold, and the test
    fails although the behaviour is right.
  - It doesn't check the published event at all. Return `DomainEvents.empty()` and the test stays green, while Fleet
    Maintenance and the read models never hear about the cancellation.
  - The version assertion tests the persistence mechanism, not the business.
  - Its name and `@DisplayName` are in code language ("sets the hold to Free"), not spec language.
- **Fix:** `BikeCancellationTest` rewritten around E1.1–E3.1: the `ReservationCancelled` event, Ben's reservation
  being accepted, and the rejections.

## Bonus findings (not planted, but fair)

- **Process:** code was delivered without a plan, and the intent is left `specified` although the feature works.
  In the flow, `implementation-planner` decides the placement and `plans.py done` sets the intent to `delivered`.
- **No integration event:** `ReservationCancelled` isn't published to other contexts. That's fine today (only Rentals
  cares about a rider's reservation), but it's worth asking.
- **Expired reservation:** cancelling a reservation whose period ended a second ago (but which hasn't been released
  yet) succeeds. The spec doesn't say. That's a question for the product owner, not a defect.

## Why the build was green

Every test that exists passes. The defects are about **missing** tests, tests that check the **wrong thing**, and a
rule in the **wrong place**. A green build only shows that the tests which exist pass. It says nothing about the
tests that aren't there.
