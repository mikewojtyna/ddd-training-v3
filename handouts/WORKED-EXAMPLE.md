# Worked example: "the rider's reservation window becomes 10 minutes"

The worksheet filled in for VeloCity, as the trainer's reference. Today a rider's reservation lasts 2 minutes:

```java
// src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Bike.java, line 32
static final Duration RIDER_RESERVATION = Duration.ofMinutes(2);       // DE-10
```

The business now wants 10 minutes. A technician's reservation (30 minutes, `TECHNICIAN_RESERVATION`, line 33) stays.

**Which numbers are measured and which estimated.** Everything about the real code (Part B) is **measured** on
master of 2026-11-10: line and byte counts with `wc`, the affected tests by actually changing the constant to 10
minutes in a throwaway worktree and running all VeloCity tests. The layered layout (Part A) **doesn't exist**: it's a
sketch, and its numbers are **estimates**. Token numbers are always estimates from sizes (rule of thumb); no agent
session was recorded for either layout. To get session numbers, run the change live and read `/context`.

---

## Part A: before, a layered VeloCity (sketch, estimated)

How VeloCity would typically look if it had been split by technical layers, as one package or one service per layer:

```
velocity/
  controllers/   RentalController, BikeController, MaintenanceController, AccountController, PaymentController, ...
  services/      BikeService            (reserve, unlock, end ride, withdraw, repair: rider AND technician paths)
                 ReservationService     (create, expire, cancel; called by rentals and maintenance)
                 RentalService, PricingService, SettlementService, ...
  entities/      Bike                   (ONE class for renting and for maintenance), Reservation, Rider, ...
  repositories/  BikeRepository, ReservationRepository, ...
  jobs/          ReservationExpiryJob, OverdueRentalJob, ChargeRetryJob, ...
  dto/           ReservationDto, BikeDto, ...
  config/        application.yml        (reservation.ttl? or a constant in ReservationService?)
```

In the "Celery + frontend + infra" world you described, the same feature is also spread over repos:

```
backend-repo/    services/reservation.py, models/bike.py, api/views.py
worker-repo/     tasks/expire_reservation.py        (Celery task scheduled with countdown=120)
frontend-repo/   ReservationTimer.tsx               (shows "2:00" counting down)
infra-repo/      helm/values.yaml                   (beat schedule, maybe a Redis TTL of 120 s)
```

| # | Repo / service | Layer | File or component | Why it had to change |
|---|---|---|---|---|
| 1 | backend | service | `ReservationService` | the duration lives here, or is passed in by its callers |
| 2 | backend | service | `BikeService` | calls `ReservationService` for riders **and** technicians: must check the 30-minute path isn't hit |
| 3 | backend | service | `MaintenanceService` | technician reservations go through the same code: read to be sure |
| 4 | backend | entity | `Bike`, `Reservation` | expiry is computed or stored here ★ |
| 5 | backend | job | `ReservationExpiryJob` | its own idea of "expired" ★ |
| 6 | backend | API / DTO | `RentalController`, `ReservationDto` | `expiresAt` returned to the app ★ |
| 7 | worker repo | task | `expire_reservation` Celery task | countdown hard-coded to 120 s ★ |
| 8 | frontend repo | UI | `ReservationTimer` | starts at 2:00 ★ |
| 9 | infra repo | config | beat schedule / TTL | 120 s somewhere in YAML ★ |
| 10 | all | tests | service tests, controller tests, task tests, UI tests | spread over 3–4 repos, mixed rider and technician cases |

| Repos | Services / deployables | Layers | Files to read (estimated) | Teams to review |
|---|---|---|---|---|
| 3–4 | 3 | 6 | 15–25 | 2–3 (backend, frontend, platform) |

Six of the ten rows (★) change only because the value was copied into another layer.

---

## Part B: after, the real Rentals context (measured)

### The business events and the pivotal ones

From the real VeloCity contexts (`src/main/cml/VeloCity-Context-Map.cml`, `rentals/package-info.java`):

| # | Business event | Who cares next |
|---|---|---|
| 1 | Bike Reserved | Rentals itself (the hold) |
| 2 | Reservation Expired | Rentals (bike available again), Fleet Maintenance (listens to Rentals' events) |
| 3 | **Rental Session Started** (bike unlocked) | Rentals |
| 4 | **Rental Session Ended** | **Settlement** (prices and charges the ride) |
| 5 | Rental Fee Unpaid | Debt Recovery |

The pivotal event is **Rental Session Ended**: after it, the business stops talking about bikes and holds and starts
talking about money, tariffs and debts. Reserving, holding, expiring and unlocking happen **before** it, on one
clock (minutes, the rider standing at the bike), with one vocabulary. So the whole reservation window is inside one
context: **Rentals**. Nobody outside Rentals needs to know that a hold lasts 2 or 10 minutes; they only receive
`ReservationExpired` when it happens.

### What Rentals owns end to end

| Context | Its API | Its async jobs | Its data | Its config |
|---|---|---|---|---|
| Rentals | `RentalsController` (`POST` reservation, returns `expiresAt`) | `RentalsClockJobs` → `ReleaseExpiredReservationsUseCase` | `Bike` aggregate + `BikeRepository` | none: the duration is a domain rule in `Bike` |

Fleet Maintenance has its **own** `Bike` aggregate (`fleetmaintenance/domain/bike/Bike.java`), with its own
language (broken, under maintenance). The technician's 30-minute hold in Rentals is a separate constant. There's
no shared `ReservationService` to be careful with.

### The change, file by file

Found with `grep -rn "RIDER_RESERVATION\|10:02\|2 minutes\|2-minute" src docs`, then **checked by running the tests**
with the constant changed to `Duration.ofMinutes(10)`.

**Production code: 1 line in 1 file to change, 1 javadoc to update.**

| File | Lines | What changes |
|---|---|---|
| `src/main/java/.../velocity/rentals/domain/bike/Bike.java` | 369 | line 32 (`ofMinutes(2)` → `ofMinutes(10)`), javadoc line 86 |
| `src/main/java/.../velocity/rentals/package-info.java` | 92 | canvas javadoc, lines 28 and 44 ("2 minutes") |

**Tests: 12 of 601 VeloCity tests fail, all in Rentals, in 7 classes.** Zero failures in the other seven contexts
and in the five cross-context journey tests.

| Test class | Lines | Failing tests |
|---|---|---|
| `rentals/domain/bike/BikeReservationTest` | 180 | 2 (expects `until 10:02`) |
| `rentals/domain/bike/BikeUnlockTest` | 200 | 1 (unlock at 10:02:30 expected rejected) |
| `rentals/domain/bike/ReservationExpiryTest` | 173 | 2 (expiry checked at 10:02:01) |
| `rentals/RiderReservesBikeSpecTest` | 253 | 2 (E1.1, E2.2) |
| `rentals/RiderUnlocksReservedBikeSpecTest` | 256 | 2 (E3.3, E4.1) |
| `rentals/FleetOperationsReleasesExpiredReservationsSpecTest` | 224 | 2 (E1.1, E1.3) |
| `rentals/FleetManagerAddsBikeToFleetSpecTest` | 120 | 1 (expects `07:47`) |
| `rentals/domain/bike/BikeFixture` (read, not failing) | 82 | – |
| **Total** | **1,488** | **12** |

Two lessons from the probe:
- **grep isn't enough.** Grepping for `10:02` found 6 of the 7 failing classes, plus 3 files that mention 10:02 and
  don't fail (`FaultSignalTest`, `FleetOperationsWithdrawsBikeSignallingFaultSpecTest`, a comment in `BikeFixture`).
  It missed `FleetManagerAddsBikeToFleetSpecTest`, which asserts `07:47` (reserved at 07:45). The test suite found it.
- **The blast radius is proven, not guessed.** "No test outside Rentals fails" is the boundary doing its job.

**Docs: the spec examples are the business decision, so they change first.**

| File | Lines | Mentions to change |
|---|---|---|
| `docs/specs/rider-reserves-bike.md` | 98 | 9 (R1 title, E1.1, E2.2, E4.1, E5.1, ...) |
| `docs/specs/rider-unlocks-reserved-bike.md` | 90 | 8 |
| `docs/specs/fleet-operations-releases-expired-reservations.md` | 86 | 7 |
| `docs/intents/rider-reserves-bike.md`, `rider-unlocks-reserved-bike.md`, `fleet-operations-releases-expired-reservations.md` | 62 + 61 + 59 | 1–2 each |
| `docs/bounded-contexts/velocity/rentals/canvas.md` | 281 | 11 |
| **Total** | **737** | |

Also one line each in `docs/specs/technician-reserves-broken-bike.md` (vocabulary) and
`fleet-operations-withdraws-bike-signalling-fault.md` (E2.1 starts from a 10:00–10:02 reservation), and the decision
itself: DE-10 in `docs/bounded-contexts/velocity/domain-expert-answers.md`. All of it is under `docs/`, next to
the context, not in another repo.

### Count

| Repos | Contexts | Layers touched | Files | Teams |
|---|---|---|---|---|
| 1 | 1 (Rentals) | domain only (+ tests, docs) | 2 code, 8 tests, 7 docs | 1 |

The controller, the clock job, the repository and every other context don't change: `expiresAt` is computed by
`Bike` and passed through.

---

## Part C: the agent's context

Rule of thumb: ~10 tokens per line of code, or bytes ÷ 4. Measured sizes, estimated tokens:

| What the agent reads | Lines (measured) | Bytes (measured) | Tokens, 10/line | Tokens, bytes ÷ 4 |
|---|---|---|---|---|
| Instructions loaded anyway: `CLAUDE.md` + `README.md` (imported) + `velocity/CLAUDE.md` | 194 | 17,721 | ~2,000 | ~4,400 |
| Production code: `Bike.java` + `package-info.java` | 461 | 24,732 | ~4,600 | ~6,200 |
| Tests to update: 7 failing classes + `BikeFixture` | 1,488 | 53,995 | ~14,900 | ~13,500 |
| Docs to update: 3 specs, 3 intents, canvas | 737 | 44,428 | ~7,400 | ~11,100 |
| **Total for the vertical change** | **2,880** | **140,876** | **~29,000** | **~35,000** |

For comparison, also measured:

| Scope | Lines | Bytes | Tokens, bytes ÷ 4 |
|---|---|---|---|
| All of Rentals, main + test Java | – | 280,785 | ~70,000 |
| All of VeloCity, main + test Java (8 contexts) | 33,256 | 1,421,285 | ~355,000 |

**Before (layered, estimated):** 15–25 files across 3–4 repos. If the shared services mix the rider and technician
paths and the reservation logic of Rentals and Fleet Maintenance, they'd be at least as big as both `Bike`
models together (Rentals' `Bike.java` 369 lines, Fleet Maintenance's `domain/bike` package 724 lines, measured), plus services, jobs, DTOs, UI and task code, plus tests that mix
rider and technician cases. A realistic guess: **4,000–6,000 lines, ~40,000–60,000 tokens**, and the agent can't run
all affected tests in one place because they live in 3–4 repos. The bigger cost isn't the tokens: it's the
uncertainty. Without a boundary, "did I break the technician's 30 minutes?" can only be answered by reading.

**After (vertical, measured sizes):** ~2,900 lines, **~30,000–35,000 tokens**, one repo, one `./mvnw test`, and the
suite proves nothing outside Rentals is affected.

Honest reading of the numbers:
- The vertical change isn't tiny. **Most of it is tests and spec examples** (2,225 of 2,880 lines), because the
  rule is written down as examples in many places. That's the price of executable specs, and it's the right place
  to pay: those are the lines a human reviews.
- An agent that only flips the constant and runs the tests reads far less (~400 lines of `Bike.java`), but it would
  leave the specs saying 2 minutes. The docs are part of the change.
- The win is the closed scope: the agent knows **where to stop reading**. In the layered layout there's no such
  line, so a careful agent reads more, and a careless one misses the Celery task.

### Conclusion

> The agent's context shrinks from ~15–25 files in 3–4 repos (~40–60k tokens, estimated) to 17 files in one context
> (~30–35k tokens, from measured sizes), because the reservation window is a rule of one context, Rentals, before
> its pivotal event, and nothing outside it holds a copy.

---

## For the debrief: their Celery + frontend + infra symptom

What they described ("every feature touches the Celery tasks, the frontend and the infra, in different repos") is a
**horizontal split**: the repos are cut by technology. Every business feature crosses every cut, so:

- every change needs 3–4 PRs, 2–3 teams and a coordinated release;
- values and rules get copied across the cuts (the ★ rows: 120 s in the task, 2:00 in the UI, a TTL in YAML);
- an agent needs access to all repos and can't run the tests that matter in one place;
- the context window can't be cut down, because the feature itself isn't in one place.

A **vertical slice** cut at pivotal events owns everything one business capability needs:
- **its UI-facing API** (the frontend calls Rentals for `expiresAt`; the countdown reads it, it doesn't hard-code 2:00);
- **its async jobs** (the expiry job is Rentals' own `RentalsClockJobs`, not a shared worker repo; Celery is a
  technical choice inside the slice, not a slice of its own);
- **its data** (Rentals' `Bike` isn't Fleet Maintenance's `Bike`);
- **its config** (or no config at all, when the value is a business rule in the domain).

Infra and the frontend shell can stay shared platforms, the way Spring is shared here. What must not be shared is
the business rule. Questions to ask the room:
- "In your last feature, how many of the changes were copies of one decision?"
- "If your Celery task belonged to the context whose rule it runs, which repo would it live in?"
- "Who could have answered 'did we break the 30 minutes?' without reading everything?"
