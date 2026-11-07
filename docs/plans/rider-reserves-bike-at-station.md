---
name: rider-reserves-bike-at-station
intent: rider-reserves-bike-at-station
spec: rider-reserves-bike-at-station
system: velocity
status: in progress
date: 2026-11-07
spec_date: 2026-11-07
---

# Implementation plan: A rider wants to reserve a bike at a bike station

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in the existing **Rentals** context (called *Rental* in the discovery map). It owns every rule in the spec: it answers "who has which bike, and when?" and lists "Reservation" among its responsibilities. Two collaborators supply facts Rentals decides on:
- **Rider Accounts** answers whether a rider's account is activated (R6). This goes through an ACL, over a relationship both maps already have. Rider Accounts doesn't exist in code yet, so the adapter is a stub and tests use a fake.
- **Fleet Maintenance** tells Rentals that a bike is out of service (R5).

One discovery-map relationship flips (S1, Q2 option A). Before the reservation rules can be built, the delivered renting code has to be fixed: it ignores which bike the rider asks for (finding F1).

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A rider can reserve an available bike without being at the station | Rentals | owns bike availability and reservation (discovery CML Rental: "Reservation"; reference CML Rentals: "Reserve and unlock") |
| R2 At most one active reservation per rider | Rentals | a rule over reservations, which only Rentals holds |
| R3 Nobody else can reserve or rent a reserved bike | Rentals | guards the state of a bike at a station; "who would be lying if this were wrong?": Rentals |
| R4 A reserved bike isn't offered to other riders | Rentals | a view of Rentals' own data (bike availability) |
| R5 A broken bike can't be reserved | Rentals (decides) / Fleet Maintenance (supplies "out of service") | Fleet Maintenance decides that a bike is broken; Rentals decides what can be reserved. Rentals keeps a local "withdrawn" state, fed by Fleet Maintenance's *Withdraw Bike* command (Q2, option A) |
| R6 Only an activated account can reserve | Rentals (decides) / Rider Accounts (supplies "activated") | "who can say no to Reserve Bike?": Rentals. The account status belongs to Rider Accounts and is asked for through an ACL: `RiderAccounts -> Rental : RiderEligibility` (discovery), `Accounts -> Rentals : AccountStatus` (reference) |
| R7 A rider who is riding can't reserve | Rentals | rental sessions are Rentals' state |
| R8 A reservation holder can rent only the reserved bike; renting it ends the reservation | Rentals | rules on Rent Bike, which Rentals owns |
| Command: Reserve Bike | Rentals | its rules (R1–R3, R5–R7) can reject it |
| Command: Rent Bike | Rentals | existing command; gains rules R3 and R8 |
| Query: Available Bikes at Station | Rentals | whose data is this a view of: Rentals' |
| Event: Bike Reserved | Rentals | the fact happens in Rentals |
| Event: Bike Rented | Rentals | existing event; gains data (bike, station, started at, from reservation) |

**Outcome:** several existing contexts. Rentals owns every rule. It asks Rider Accounts through an ACL (existing relationship). Fleet Maintenance commands Rentals to withdraw a bike (Q2, option A), which flips one discovery relationship (S1).

## Context map changes

**Governing map:** the discovery map `src/main/cml/VeloCity-Context-Map-Discovery.cml`, the output of step 2, with the code's name *Rentals* for the context (Q1). The reference map `VeloCity-Context-Map.cml` is the training's solution and isn't edited.

- **Account status (R6):** no change. The discovery map has `RiderAccounts [U,S,PL]->[D,C,ACL] Rental : RiderEligibility`. The plan's ACL matches it. The reference map's `Accounts [U]->[D] Rentals` shows no ACL, so the ACL is a pattern the reference map doesn't draw.
- **Bike out of service (R5):**
  - **Decided (Q2, option A, 2026-11-07): Rentals is upstream and offers *Withdraw Bike*.** This matches the existing Fleet Maintenance code, whose `rentalsctxacl/RentalsCirculationAdapter` stub already calls a Rentals command, and the reference map. It flips the discovery relationship (no cycle results: Rentals has no other relationship with Fleet Maintenance), with your consent, in S1:
    ```
    // bike withdrawn / made available again: Fleet Maintenance commands Rentals through Rentals' API
    Rental [U,S,PL]->[D,C,ACL] FleetMaintenance : BikeAvailability
    ```
    replacing `FleetMaintenance [U,S,PL]->[D,C,ACL] Rental : BikeAvailability`.
  - Option B (Rentals listens to a Fleet Maintenance event) was declined.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Rider | Rentals | Borrower (`BorrowerId`, existing code) | REST adapter: `riderId` → `BorrowerId` |
| Reservation period | Rentals | Reservation window (canvas) | same meaning; the canvas keeps "reservation window", defined as 15 minutes including the end time |
| Reservation | Rentals | Reservation (canvas, existing term) | none |
| Available bike | Rentals | available (existing `BikeInStation.available`) | none; now also "not reserved, not withdrawn" |
| Broken bike | Rentals | withdrawn bike (Fleet Maintenance's *Withdraw Bike*) | Fleet Maintenance says "broken / out of service", Rentals says "withdrawn": translated at the boundary |
| Activated account | Rentals | eligible rider (canvas: "Rider eligibility") | ACL `rideraccountsctxacl`: Rider Accounts' account status → Rentals' eligibility |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Reserve Bike | Rentals | `ReserveBikeUseCase` + `ReserveBike` record in `rentals/api/usecases`; `POST /rentals/stations/{stationId}/bikes/{bikeId}/reservations` (body: `riderId`) |
| Command: Rent Bike | Rentals | existing `RentBikeUseCase` (today comments only) completed; `POST /rentals/stations/{stationId}/bikes/{bikeId}/rentals` (body: `riderId`, `duration`) |
| Query: Available Bikes at Station | Rentals | `AvailableBikesAtStation` query + result record in `rentals/api/queries`, handler; `GET /rentals/stations/{stationId}/available-bikes` |
| Event: Bike Reserved | Rentals | new domain event `BikeReserved` in the sealed `BikeRentalEvent` hierarchy (the exhaustive `switch` in `BikeRentedEventHandler` must handle it). No integration event: nothing outside Rentals listens |
| Event: Bike Rented | Rentals | existing domain event `BikeRented`, extended with bike, station, started at, from reservation. No integration event yet |
| (supporting) Withdraw Bike | Rentals | `WithdrawBike` record in `rentals/api/usecases` + use case, the command Fleet Maintenance's canvas and stub already expect (R5). |
| (supporting) Rider eligibility | Rentals | secondary port in Rentals' language + stub adapter in `rentals/infra/adapters/secondary/rideraccountsctxacl` (Rider Accounts isn't in code) |

Ticket hints from the spec's Notes for the plan, and what this plan does with them:
- **`POST /api/v2/reservations`:** not adopted. The repo's path style is `/<context>/<resource>/{id}/...`, and the bike id belongs in the path. 409 for R2 is kept as the REST mapping of that rejection.
- **Redis TTL, `RESERVED` column:** not adopted. The stack has neither Redis nor tables for Rentals. Persistence is in memory, and expiry belongs to `fleet-operations-releases-unused-reservation`.
- **Mixpanel `reservation_created`:** out of scope, since there's no analytics integration. `Bike Rented … from reservation` makes the success signal measurable later.
- **Mobile contract by sprint 24:** the REST contract above is that contract.

## Canvas changes

### Rentals (`docs/bounded-contexts/velocity/rental/canvas.md` → moved to `docs/bounded-contexts/velocity/rentals/canvas.md`, see Q1)

- **Name:** Rentals (decided, Q1). The discovery CML's `Rental` stays as is; the difference is recorded in the canvas.
- **Ubiquitous language, change:** **Reservation window**: 15 minutes from the moment of reserving, including the end time (product owner, 2026-11-07). **Location check**: doesn't apply to reserving (product owner, 2026-11-07).
- **Ubiquitous language, add:** **Withdrawn bike**: a bike Fleet Maintenance has taken out of service; it isn't available, so it can't be reserved. **Borrower**: the rider holding or renting a bike (existing code term). **Eligible borrower**: a borrower whose account Rider Accounts reports as activated.
- **Inbound, add:** Rider app | Query | Available bikes at station.
- **Inbound, replace:** "Fleet Maintenance | Event | Bike Out Of Service / Bike Back In Circulation" becomes "Fleet Maintenance | Command | Withdraw bike".
- **Business decisions, replace "Reservation window" with** (each in the canvas's Given/When/Then form):
  - `R1 Reserve without being at the station (spec: rider-reserves-bike-at-station#E1.1, E1.2)`
  - `R2 One active reservation per borrower (spec: …#E2.1)`
  - `R3 A reserved bike is held for its borrower (spec: …#E3.1, E3.2)`
  - `R4 Reserved bikes aren't offered to others (spec: …#E4.1)`
  - `R5 Withdrawn bikes can't be reserved (spec: …#E5.1)`
  - `R6 Only eligible borrowers reserve (spec: …#E6.1)`
  - `R7 No reserving while riding (spec: …#E7.1)`
  - `R8 A holder rents only the reserved bike (spec: …#E8.1, E8.2)`
- **Open questions, resolve:** "Name: Rental or Rentals?" (Q1).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | on (recommended) | you stay in control of every domain step |
| D2 | Aggregate style | `BikeStation` stays state-based; any new aggregate's style is left to the tactical skill | refine mode keeps the existing style |
| D3 | Per-rider rules (R2, R7, R8a) | left to the tactical skill. Candidates: a per-borrower aggregate, or a check through a query in the use case | `BikeStation` sees only its own bikes, while these rules span stations |
| D4 | ACL to Rider Accounts | yes, `rideraccountsctxacl`, stub adapter | default ACL; Rider Accounts has no code |
| D5 | Context name | Rentals (decided, Q1) | code package, `@BoundedContext(name = "Rentals")`, the reference CML and Fleet Maintenance's `rentalsctxacl` all use it |
| D6 | Fix delivered renting behaviour | yes, as its own step S4 (F1, confirmed) | R3, R8 and E8.2 need renting a specific bike |
| D7 | Integration events | none | no listener outside Rentals yet |
| D8 | Time | the examples need a controllable clock; the use cases take "now" from an injected clock | E3.2 tests exactly 10:15 |
| D9 | After the window | a reservation stops counting once its 15 minutes have passed; no event until `fleet-operations-releases-unused-reservation` (Q3) | keeps bikes from being blocked forever |

## Steps

### S1: Flip the Fleet Maintenance relationship in the discovery CML
- **Status:** done
- **Context:** Rentals, Fleet Maintenance (context map)
- **Skill:** none (this skill; the change was approved with Q2, option A)
- **Covers:** none
- **Done when:** `src/main/cml/VeloCity-Context-Map-Discovery.cml` has `Rental [U,S,PL]->[D,C,ACL] FleetMaintenance : BikeAvailability` instead of the reverse. Its comment says that Fleet Maintenance still decides "out of service" and commands Rentals' *Withdraw Bike* API, and points to this plan and to the discovery report the original direction came from; `validate_cml.sh` passes; the reference CML is untouched.

### S2: Update the Rentals canvas
- **Status:** done
- **Context:** Rentals
- **Skill:** none (this skill applies the canvas diff above)
- **Covers:** none
- **Done when:** canvas moved to `docs/bounded-contexts/velocity/rentals/canvas.md` and updated exactly as in Canvas changes; paths pointing to the old location (canvas header comment, discovery report) updated.

### S3: Scaffold the Rentals module around the domain (scaffolder 6a)
- **Status:** done
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (update mode, approved via this plan)
- **Covers:** none
- **Done when:** `ReserveBike` and `WithdrawBike` records, `AvailableBikesAtStation` query/result records, the Rider Accounts eligibility port + stub ACL adapter, and `RentalsArchitectureTest` exist; `./mvnw test` green.

### S4: Rent the requested bike (fix of `rider-rents-bike-from-station`, F1)
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (refine `BikeStation`, TDD checkpoint loop)
- **Covers:** none (delivered behaviour; one confirmed scenario: "Given B-17 and B-22 are available at Central, When George rents B-17, Then B-17 is rented and B-22 stays available", and `BikeRented` carries the bike)
- **Done when:** the scenario's test passes, the existing `BikeStationTest` examples still pass (adjusted only mechanically), `./mvnw test` green.

### S5: TDD run 1, a bike held at a station: R1, R3, R5
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (TDD checkpoint loop)
- **Covers:** R1, R3, R5, E1.1, E1.2, E3.1, E3.2, E5.1, plus one scenario for D9 (confirmed 2026-11-07): "Given B-17 was reserved for Anna at 10:00, When Ben reserves B-17 at 10:16, Then the reservation is accepted"
- **Done when:** one test per example passes; `./mvnw test` green.

### S6: TDD run 2, one bike per rider: R2, R7, R8
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (TDD checkpoint loop; D3 decided here)
- **Covers:** R2, R7, R8, E2.1, E7.1, E8.1, E8.2
- **Done when:** one test per example passes; `./mvnw test` green.

### S7: TDD run 3, eligibility: R6
- **Status:** done
- **Context:** Rentals
- **Skill:** ddd-tactical-building-blocks (TDD checkpoint loop)
- **Covers:** R6, E6.1
- **Done when:** the example's test passes; `./mvnw test` green.

### S8: Use cases, query, REST and wiring (scaffolder 6b)
- **Status:** todo
- **Context:** Rentals
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** R4, E4.1
- **Done when:**
  - `ReserveBikeUseCase`, completed `RentBikeUseCase`, the `WithdrawBike` use case, and the `AvailableBikesAtStation` handler exist;
  - so do in-memory adapters for every repository the TDD runs introduced (at least `BikeStationRepo`), the REST endpoints from Contract mapping, the clock bean and `RentalsContextConfig`;
  - `./mvnw test` green.

### S9: Acceptance tests for the spec examples
- **Status:** todo
- **Context:** Rentals
- **Skill:** none (this skill)
- **Covers:** all examples
- **Done when:** `RiderReservesBikeAtStationSpecTest` has one use-case-level test per example (E1.1–E8.2) named after its ID, with in-memory adapters and a fake eligibility port; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1, E1.2, E2.1, E3.1, E3.2, E4.1, E5.1, E6.1, E7.1, E8.1, E8.2 | `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReservesBikeAtStationSpecTest.java`, one method per example (to be filled in S9) |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1, E1.2 | S5, S9 | |
| R2 / E2.1 | S6, S9 | |
| R3 / E3.1, E3.2 | S5, S9 | |
| R4 / E4.1 | S8, S9 | |
| R5 / E5.1 | S5, S9 | |
| R6 / E6.1 | S7, S9 | |
| R7 / E7.1 | S6, S9 | |
| R8 / E8.1, E8.2 | S6, S9 | |

## Risks and open questions

- **F1, delivered code doesn't match its intent (fix confirmed 2026-11-07).** `BikeStation.rent(bike)` rents *any* available bike, not the requested one, and `BikeRented` carries no bike. `rider-rents-bike-from-station` is marked `delivered`. S4 fixes it as its own step.
- ~~**Q1, context name.**~~ *Rentals* (decided 2026-11-07). The canvas moves to `rentals/`; the discovery CML keeps *Rental*, and the canvas notes the difference.
- ~~**Q2, Fleet Maintenance direction.**~~ Option A, Rentals offers *Withdraw Bike* (decided 2026-11-07); S1 flips the discovery relationship.
- ~~**Q3, after 15 minutes.**~~ Decided 2026-11-07: until `fleet-operations-releases-unused-reservation` is built, a reservation stops counting once its window has passed. Other riders can then reserve or rent the bike, and the holder can reserve again. No *Reservation Expired* event is published (D9). The tactical runs S5/S6 build this as part of R2/R3.
- **Nothing withdraws a bike in the running app yet.** R5 works through Rentals' new *Withdraw Bike* use case, but Fleet Maintenance's `Circulation.takeOut` / `RentalsCirculationAdapter` stays a stub. Wiring it is a follow-up in Fleet Maintenance's own plan (its Q3: when *Withdraw Bike* is sent).
- **Withdrawn bikes never come back.** Only *Withdraw Bike* is planned. *Make Bike Available* (Fleet Maintenance's `putBack`) stays a stub: a follow-up for the repair intents.
- **Rider Accounts doesn't exist in code.** The eligibility adapter is a stub, so the running app can't reserve until that context exists. Tests use a fake.
- **Renting without a reservation** keeps its existing 11-hour limit. "A rider who is riding can't rent a second bike" is out of scope (spec) and belongs to the renting intent.

## Review notes

- 2026-11-07: draft written, awaiting review.
- 2026-11-07: critic round 1, DISAGREE. The plan mixed the discovery canvas with the reference map's Fleet Maintenance direction and claimed no map change. Fixed: governing map stated (discovery), options A and B with a CML snippet, canvas row replaced instead of added, Q3 (after 15 minutes) added, scaffold step moved before the rent fix, adapter and wording notes applied.

- 2026-11-07: user answers: Q1 Rentals, Q2 option A (with consent to flip the discovery CML relationship), Q3 a reservation stops counting after its window, F1 fix confirmed. Added S1 (CML change); steps renumbered.
- 2026-11-07: critic round 2, AGREE. Notes applied: summary fixed, S1 comment wording, D9 scenario added to S5 for confirmation, Fleet Maintenance `takeOut` stub listed as a risk, withdrawn-bike wording, canvas path updates, D8/D9 order.
- 2026-11-07: approved by the user. D9 scenario confirmed, TDD checkpoint loop on.
- 2026-11-07: user switched the TDD checkpoint loop off for the rest of S5–S7 (after cycle 3 of S5), after being told the cost. Work stays test-first; per-cycle log reported at the end of each run.

## Changelog

### 2026-11-07: planned
- Built from the spec dated 2026-11-07 (approved).
- S1 done: discovery CML relationship flipped (`Rental -> FleetMaintenance : BikeAvailability`), validate_cml.sh VALID; reference CML untouched.
- S2 done: canvas moved to `docs/bounded-contexts/velocity/rentals/canvas.md`, renamed Rentals, R1–R8 decisions added; discovery report path updated.
- S3 done: scaffolder 6a. `ReserveBike`, `WithdrawBike`, `AvailableBikesAtStation`/`AvailableBikes`, `BorrowerEligibility` port + `rideraccountsctxacl` stub, `RentalsContextConfig`, `RentalsArchitectureTest`; context plan `docs/bounded-contexts/velocity/rentals/plan.md` (approved via this plan). `./mvnw test` green.
- S4 done: `BikeStation.rent` rents exactly the requested bike; `BikeRented` carries the bike. Test `BikeStationTest#rentsExactlyTheRequestedBike` (scaffolded by Claude on the user's request, confirmed). Suite green.
- S5 done: R1, R3, R5 + D9 in `BikeStation` (11 tests in `BikeStationTest`); `rent` gained `.at(now)`; `withdraw(bike)` added. Loop off from cycle 4 (user). Suite green.
- S6 done: R2, R7, R8 via new aggregate `Borrowing` (D3 decided: per-borrower aggregate) plus holder renting in `BikeStation`; `ReservationWindow` value object; `BikeRented.fromReservation`. Suite green.
- S7 done: R6 in `Borrowing`, eligibility as a command argument (`Eligibility`). Persistence after the domain: `Version`, snapshots, `BorrowingRepository`, in-memory adapters with optimistic-locking tests. Suite green.
