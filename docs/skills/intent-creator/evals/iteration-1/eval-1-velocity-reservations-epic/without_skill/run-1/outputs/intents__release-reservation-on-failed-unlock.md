# Release a reservation when the unlock fails

- **Status:** Draft
- **System:** VeloCity
- **Bounded context:** Rentals
- **Actor:** Rider or technician (the reservation holder); the bike's lock reports the failure
- **Part of:** Bike reservations. The related intents are [reserve-bike-for-ride](reserve-bike-for-ride.md), [reserve-broken-bike-for-maintenance](reserve-broken-bike-for-maintenance.md) and [expire-bike-reservation](expire-bike-reservation.md).

## Why

An unlock can fail because of a mechanical issue, a connectivity problem or any other reason. The holder must not be stuck with a bike they can't use. A rider should be free to try another bike right away, and a technician should be able to try again later.

## Desired outcome

When the holder's unlock of a reserved bike fails, the reservation is released immediately. No rental session and no maintenance starts.

## Scenarios

### Rider's unlock fails

- **Given** an available bike reserved by a rider
- **When** the rider's unlock fails
- **Then** **Bike Reservation Released** is published with the reason "unlock failed", no rental session starts, and the rider is free to reserve another bike

### Technician's unlock fails

- **Given** a broken bike reserved by a technician
- **When** the technician's unlock fails
- **Then** **Bike Reservation Released** is published, no maintenance starts, the bike **stays broken**, and a technician tries again later

## Business rules

- A failed unlock always releases the reservation. It never leaves it hanging until expiry.
- A failed unlock never starts a rental session or maintenance, and it is never billed.
- After a release the rider may reserve another bike at once. The one-reservation-per-rider rule doesn't block them.
- A broken bike stays withdrawn after a failed technician unlock.

## Assumptions (decisions made without the domain expert)

- **What happens to an available bike after a rider's failed unlock is undecided.** The vision only says the reservation is released. A failed unlock may well mean the bike is faulty. This intent leaves the bike's availability as it was and lists the question below. Compare *Returns*, where a failed **lock** makes the bike broken.

## Out of scope

- Diagnosing why the unlock failed.
- Reporting the bike as broken (that belongs to the fault-report flow and Fleet Maintenance).

## Open questions

1. When a rider's unlock fails, does the bike go back to the pool, or is it withdrawn as possibly faulty? The vision says a failed *lock* at return makes the bike broken, but says nothing about a failed *unlock*. Releasing a faulty bike straight back to riders would let the next rider hit the same failure.
2. Is a failed unlock a fault signal that Rentals should send to Fleet Maintenance ("the bike itself signals a problem")?
3. How many times may a technician retry, and does Fleet Maintenance escalate after repeated failures?

## Related

- `velocity/velocity-vision.md`: *Renting a Bike* ("The unlocking fails…"), *Broken Bikes & Maintenance* ("The unlock fails…"), *Returns* (lock failure makes the bike broken).
- `velocity/velocity-context-map.md`: *Rentals withdraws a bike itself when it detects a fault*.
