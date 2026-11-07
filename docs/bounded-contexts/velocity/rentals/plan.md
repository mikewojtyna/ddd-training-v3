# Rentals bounded context: implementation plan

- **System / package**: `velocity.rentals`
- **Mode**: update
- **Source canvas**: `docs/bounded-contexts/velocity/rentals/canvas.md`
- **Last updated**: 2026-11-07
- **Status**: awaiting tactical blocks

<!-- Reviewers: edit this plan directly or answer in the conversation, then set Status to "approved" (or say so).
     Invoking the skill again resumes from this file. -->

## Review notes
- 2026-11-07: approved via `docs/plans/rider-reserves-bike-at-station.md` on 2026-11-07 (intent plan approved by the user; structural decisions and the TDD checkpoint loop taken from it). No questions asked again.

- 2026-11-07: TDD checkpoint loop switched **off** by the user for the remaining cycles of intent-plan steps S5–S7 (explicit decision after the cost was explained).

## Canvas summary
Answers "who has which bike, and when?": reservation, unlock, rental sessions, returns. Strategic classification and domain roles are still open in the canvas. Upstream: Rider Accounts (eligibility). Downstream: Fleet Maintenance (commands *Withdraw bike*), Settlement and Incident Handling (events, not in code yet).

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| Modules | 1 module = 1 context | default |
| ACL towards Rider Accounts | yes, `rideraccountsctxacl`, stub adapter | intent plan D4; Rider Accounts isn't in code |
| Boundary exceptions | none | default |
| Domain layer | delegated to `ddd-tactical-building-blocks` (always) | this skill doesn't design tactical building blocks |
| TDD checkpoint loop | on | intent plan D1, approved by the user 2026-11-07 |
| Aggregate style | `BikeStation` stays state-based; new aggregates left to the tactical skill | intent plan D2, D3 |
| Integration events | none | intent plan D7 |
| Time | use cases take "now" from an injected clock | intent plan D8 |
| After the window | a reservation stops counting once its 15 minutes have passed; no event yet | intent plan D9 |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Rider app | Command | Reserve bike | `api/usecases/ReserveBike` (use case deferred) |
| Rider app | Command | Unlock bike (rent) | existing `api/usecases/RentBikeUseCase` (to be completed in 6b) |
| Rider app | Query | Available bikes at station | `api/queries/AvailableBikesAtStation`, `AvailableBikes` (handler deferred) |
| Fleet Maintenance | Command | Withdraw bike | `api/usecases/WithdrawBike` (use case deferred) |
| Rider app | Command | Return bike | not in this change |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Can this rider rent? | Query | Rider Accounts | `domain/eligibility/BorrowerEligibility` | `infra/adapters/secondary/rideraccountsctxacl/RiderAccountsEligibilityAdapter` (stub) |
| Estimate fee, Zone lookup, Rental Session events, fault events | | Pricing, City, Settlement, Incident Handling | | not in this change |

### Ubiquitous language
| Term | Code type | Where it appears |
|---|---|---|
| Reservation, Reservation window | javadoc, named by the tactical skill | `package-info.java` |
| Borrower | `domain/bikestation/Borrower`, `BorrowerId` (existing) | domain |
| Eligible borrower | `BorrowerEligibility` | domain port |
| Withdrawn bike | javadoc, named by the tactical skill | `WithdrawBike`, `package-info.java` |

### Business rules → delegated to `ddd-tactical-building-blocks`
| Rule (GWT) | Status | Building block | Test |
|---|---|---|---|
| Rent the requested bike: given B-17 and B-22 available at Central, when George rents B-17, then B-17 is rented and B-22 stays available | green | `BikeStation` invariant (refined) | `BikeStationTest#rentsExactlyTheRequestedBike` |
| R1 [E1.1, E1.2] | green | `BikeStation` invariant | `BikeStationTest#reservesAnAvailableBikeForFifteenMinutes`, `#rejectsReservingARentedBike` |
| R3 [E3.1, E3.2, plus D9: Ben reserves B-17 at 10:16 → accepted] | green | `BikeStation` invariant | `BikeStationTest#rejectsReservingABikeReservedForSomeoneElse`, `#rejectsRentingABikeReservedForSomeoneElseUpToTheEndOfTheWindow`, `#aReservationStopsCountingAfterItsWindow` |
| R5 [E5.1] | green | `BikeStation` invariant (+ `withdraw` command) | `BikeStationTest#rejectsReservingAWithdrawnBike` |
| R2 [E2.1] | delegated (S6) | | |
| R7 [E7.1] | delegated (S6) | | |
| R8 [E8.1, E8.2] | delegated (S6) | | |
| R6 [E6.1] | delegated (S7) | | |

The Given/When/Then text of each rule is in the canvas, tagged with its spec example IDs.

### Rules awaiting GWT
- Max rental time: whether exceeding it force-ends the session (canvas). Not part of this change.

## Deferred until tactical blocks are built
- `ReserveBikeUseCase`, completed `RentBikeUseCase`, `WithdrawBikeUseCase`: need the domain commands.
- `AvailableBikesAtStation` handler (R4): needs the station's reservation state.
- In-memory adapters for every repository the TDD runs introduce (at least `BikeStationRepo`), plus a lookup of a bike's station for `WithdrawBike` (Fleet Maintenance knows only the bike).
- REST: `POST /rentals/stations/{stationId}/bikes/{bikeId}/reservations`, `POST /rentals/stations/{stationId}/bikes/{bikeId}/rentals`, `GET /rentals/stations/{stationId}/available-bikes`; `riderId` → `BorrowerId`.
- Clock bean and the rest of `RentalsContextConfig`.

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| Rider Accounts (via ACL) | Accounts, no ACL | name: not checked; ACL is the repo default |
| Fleet Maintenance commands Withdraw bike (Rentals upstream) | `Rentals [U,OHS,PL]->[D,CF] FleetMaintenance` | same direction; the canvas's ACL sits on Fleet Maintenance's side |

## Files
```
src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/
  package-info.java                                              (javadoc updated)
  api/usecases/ReserveBike.java, WithdrawBike.java               (new)
  api/queries/AvailableBikesAtStation.java, AvailableBikes.java  (new)
  domain/eligibility/BorrowerEligibility.java                    (new port)
  infra/adapters/secondary/rideraccountsctxacl/                  (new ACL, stub adapter)
  infra/config/RentalsContextConfig.java                         (new)
src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RentalsArchitectureTest.java (new)
```

## Stubs and follow-ups
- `RiderAccountsEligibilityAdapter` is a stub: Rider Accounts isn't in code.
- Fleet Maintenance's `RentalsCirculationAdapter.takeOut` can call `WithdrawBike` once its use case exists (Fleet Maintenance's plan, Q3).
- Strategic classification and domain roles are still open in the canvas.

## Changelog
### 2026-11-07: updated (6a)
- Added: `ReserveBike`, `WithdrawBike`, `AvailableBikesAtStation`, `AvailableBikes`, `BorrowerEligibility` port, `rideraccountsctxacl` stub ACL, `RentalsContextConfig`, `RentalsArchitectureTest`; `package-info.java` javadoc rewritten from the canvas.
- Changed: none in domain code.
- Proposed removals (not applied): none.
- Triggered by: canvas update R1–R8 (intent: `docs/plans/rider-reserves-bike-at-station.md`).

### 2026-11-07: tactical run (intent plan S4)
- Cycle 1: rent exactly the requested bike. `BikeStation` invariant. `BikeStationTest#rentsExactlyTheRequestedBike`. Red seen (event named B-22), implemented by Claude (scaffolded on the user's request: one added filter in `rent`), green seen, confirmed by the user. `BikeRented` gained `BikeId bike`.

### 2026-11-07: tactical run 1 (intent plan S5: R1, R3, R5)
- Cycle 1 E1.1, `#reservesAnAvailableBikeForFifteenMinutes`: red seen (no event), Claude scaffolded (on request), green, confirmed.
- Cycle 2 E1.2, `#rejectsReservingARentedBike`: red seen (no exception), Claude scaffolded (on request), green, confirmed.
- Cycle 3 E3.1, `#rejectsReservingABikeReservedForSomeoneElse`: red seen, Claude scaffolded (on request), green. Loop switched off by the user after this cycle.
- Cycle 4 E3.2, `#rejectsRentingABikeReservedForSomeoneElseUpToTheEndOfTheWindow`: passed straight away; only the signature change `rent(…).forDuration(d).at(now)` (9 existing calls updated mechanically).
- Cycle 5 D9, `#aReservationStopsCountingAfterItsWindow`: red seen (BikeNotAvailableException at 10:16), Claude implemented: `BikeInStation.reservedUntil`, availability became `isAvailableAt(now)`; green.
- Cycle 6 E5.1, `#rejectsReservingAWithdrawnBike`: red seen, Claude implemented `withdraw(bike)` (marks the bike unavailable; no event, the spec defines none); green.
- Invariant → state as built: per `BikeInStation`: `available` (rented or withdrawn), `reservedUntil`. The holder isn't stored yet: R8 (run 2) needs it.
