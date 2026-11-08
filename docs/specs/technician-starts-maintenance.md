---
name: technician-starts-maintenance
intent: technician-starts-maintenance
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A technician wants to unlock a reserved broken bike and start maintenance

Concrete rules, examples and contract for [the intent](../intents/technician-starts-maintenance.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Technician's reservation | A technician's exclusive hold on one broken bike, lasting 30 minutes (DE-11). |
| Unlock | Opening the bike's lock for the holder of its reservation. |
| Maintenance | The work a technician does on a broken bike after unlocking it; never billed (vision). |
| Under maintenance | A broken bike unlocked by a technician; still not offered to riders. |

## Rules

### R1: A technician's unlock of the broken bike they reserved starts maintenance (vision)

- **E1.1** Given bike B-17 is broken and reserved for technician Tom from 09:00 until 09:30, When Tom unlocks B-17 at 09:20 and the lock opens, Then B-17 is unlocked for Tom (Bike Unlocked) and maintenance on B-17 started at 09:20 (Maintenance Started).

### R2: A technician's unlock never starts a rental session and is never billed (vision)

- **E2.1** Given Tom unlocked B-17 at 09:20 and maintenance started, When Tom works on B-17 until 11:00, Then no rental session exists for B-17, and no ride is priced or charged for Tom.

### R3: Only the technician holding an unexpired reservation of the bike can unlock it (vision, Reservation)

- **E3.1** Given B-17 is reserved for Tom until 09:30, When technician Tara unlocks B-17 at 09:20, Then the unlock is rejected and B-17 stays reserved for Tom.
- **E3.2** Given Tom's reservation of B-17 expired at 09:30 (Reservation Expired) and B-17 is still broken, When Tom unlocks B-17 at 09:35, Then the unlock is rejected; Tom has to reserve B-17 again.

### R4: A failed unlock releases the reservation and the bike stays broken (vision)

- **E4.1** Given B-17 is broken and reserved for Tom until 09:30, When Tom unlocks B-17 at 09:20 and the lock doesn't open, Then Tom's reservation is released (Unlock Failed), no maintenance starts, B-17 is still broken and not offered to riders, and any technician can reserve it again.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Unlock Broken Bike | Technician | technician, bike | Bike Unlocked, Maintenance Started | R3 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Bike Unlocked | the lock opened for the technician holding the reservation | holder (technician), bike, unlocked at |
| Maintenance Started | a technician's unlock of a broken bike succeeded | technician, bike, started at |
| Unlock Failed | the lock didn't open; the reservation is released | holder (technician), bike, failed at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Maintenance | technician | Identifier (Technician) | the holder of the reservation |
| Maintenance | bike | Identifier (Bike) | a broken bike |
| Maintenance | started at | Timestamp | the unlock time |

## Out of scope

- Reserving the broken bike and the reservation's expiry (`technician-reserves-broken-bike`).
- Returning the fixed bike to circulation (`technician-returns-fixed-bike-to-circulation`).
- A rider's unlock and rental session (`rider-unlocks-reserved-bike`).

## Open questions

- **Q1** Should repeated failed technician unlocks of the same bike be flagged, e.g. because the lock itself needs repair on site? (R4; not blocking)

## Notes for the plan

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run); the intent had no technical notes.
