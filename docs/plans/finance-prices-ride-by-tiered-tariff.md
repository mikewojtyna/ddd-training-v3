---
name: finance-prices-ride-by-tiered-tariff
intent: finance-prices-ride-by-tiered-tariff
spec: finance-prices-ride-by-tiered-tariff
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: Finance wants every finished ride priced by the tiered tariff

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

Every rule lands in **Pricing**, the pure calculator of the total fee (CML responsibility "Total fee calculation").
The spec's command *Price Ride* becomes Pricing's Open Host Service query **Price for Session**
(`GET /pricing/session-price`), which **Settlement** asks when it settles a ride (CML
`Pricing [U,OHS,PL]->[D,CF] Settlement`); *Ride Priced* is that query's answer, not a published event, because Pricing
keeps no rides (canvas Purpose). The Settlement side of the hand-off (asking for the fee on Rental Session Ended) is
built by `finance-charges-finished-ride`. Prerequisite: the Pricing module and its tariff schedule from
`pricing-manager-publishes-tariff` S1-S5. No context-map change.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A ride that lasts no longer than the free period is free | Pricing | fee calculation is Pricing's key question (CML "Total fee calculation"; canvas UL "Free period", decision "A ride within the free period is free") |
| R2 Every started billing hour is charged | Pricing | same; canvas UL "Billing hour", decision "Every started billing hour is charged" (DE-31, DE-83) |
| R3 Hourly fees rise for hours 1 to 3 and stay the same from the 4th hour on | Pricing | same; canvas decision "Hourly fees are tiered" |
| R4 A ride is priced with the tariff that was valid when it started | Pricing | Pricing holds the tariffs (CML "Tariffs"); precedent: plan `pricing-manager-publishes-tariff` R2 |
| R5 A station return and a lock failure pay hourly fees only | Pricing | return fees are part of the total fee (canvas UL "Return fee", decision "A lock failure costs nothing extra"; DE-32, DE-34) |
| Command: Price Ride | Pricing (asked by Settlement) | nothing can reject it ("Rejected when: none"); the calculation is Pricing's. Pricing is stateless towards rides, so the command becomes the query *Price for Session*; Settlement issues it when Rentals' session ended (Settlement canvas Outbound "Price for Session") |
| Event: Ride Priced | Pricing | "who would be lying if it were wrong?": Pricing's calculation. Realised as the Price for Session answer; Settlement records the fee on its ride (`finance-charges-finished-ride`) |

**Outcome:** several existing contexts: Pricing owns every rule; Settlement asks Pricing's OHS (query through a
Settlement ACL, built by `finance-charges-finished-ride`). Direction matches the CML.

## Context map changes

- None. `Pricing [U,OHS,PL]->[D,CF] Settlement : PriceCalculation` already carries the query.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Ride | Pricing | Session (finished rental session: started at, ended at, ending, position) | Settlement's `pricingctxacl` adapter sends the session's facts as query parameters |
| Ending: station return / free-floating return / lock failure / maximum rental time exceeded | Pricing | Ending: station / free-floating / lock failure / maximum rental time (`STATION`, `FREE_FLOATING`, `LOCK_FAILURE`, `MAX_RENTAL_TIME`) | Settlement translates Rentals' end reason (Settlement canvas UL "Ending (when asking Pricing)"); REST adapter parses `ending` |
| Free period, billing hour, hourly fee, return fee, total fee | Pricing | same terms | canvas UL |
| Price Ride / Ride Priced | Pricing | Price for Session (query) / session price (`SessionPrice` result) | REST adapter `GET /pricing/session-price` |
| Tariff | Pricing | Tariff (`tariffId` in the answer) | canvas UL |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Price Ride | Pricing | query `PriceForSession(startedAt, endedAt, ending, latitude?, longitude?)` + result `SessionPrice` in `pricing/api/queries`; `PriceForSessionQueryHandler`; `GET /pricing/session-price?startedAt=&endedAt=&ending=&latitude=&longitude=` (integration-contracts.md); `400` when `endedAt` isn't after `startedAt` or `ending` is unknown |
| Event: Ride Priced | Pricing | the answer `{tariffId, hourlyFees, returnFee, returnFeeKind, penalty, total}` (+ `billingHours`, see CONTRACT GAP); no domain event and no `api/events` record: Pricing keeps no rides and nobody subscribes; Settlement asks |

The hand-off on the Settlement side (secondary port `RideFees`, adapter `settlement/infra/adapters/secondary/pricingctxacl`)
belongs to `finance-charges-finished-ride`.

Data shapes, notes for the tactical skill: billing time measured to the second (DE-83); billing hours = 0 up to and
including the free period, else the number of started hours after it; hourly fees one per billing hour, each from the
tariff valid at the session start.

## Canvas changes

### Pricing (`docs/bounded-contexts/velocity/pricing/canvas.md`)

- **Tag existing decisions:** "A ride within the free period is free" ← E1.2; "Every started billing hour is charged" ← E2.1; "Hourly fees are tiered" ← E3.2; "The tariff valid at session start applies" ← E4.1; "A lock failure costs nothing extra" ← E5.2.
- **Business decisions, add:**
  - `R1 A short ride is free (spec: finance-prices-ride-by-tiered-tariff#E1.1)`: Given the launch tariff, When a session from 10:00 to 10:12 ending at a station is priced, Then the hourly fees are PLN 0.00 and the total fee is PLN 0.00.
  - `R2 75 minutes is still one billing hour (spec: finance-prices-ride-by-tiered-tariff#E2.2)`: Given the launch tariff, When a session from 10:00:00 to 11:15:00 ending at a station is priced, Then it has 1 billing hour and the total fee is PLN 3.00.
  - `R2 One second more starts billing hour 2 (spec: finance-prices-ride-by-tiered-tariff#E2.3)`: Given the launch tariff, When a session from 10:00:00 to 11:15:01 ending at a station is priced, Then it has 2 billing hours, hourly fees PLN 3.00 + 5.00 and the total fee is PLN 8.00.
  - `R3 Three billing hours (spec: finance-prices-ride-by-tiered-tariff#E3.1)`: Given the launch tariff, When a session from 10:00 to 13:15 ending at a station is priced, Then it has 3 billing hours, hourly fees PLN 3.00 + 5.00 + 7.00 and the total fee is PLN 15.00.
  - `R4 A session started after a new tariff uses it (spec: finance-prices-ride-by-tiered-tariff#E4.2)`: Given the launch tariff and T-2 valid from 12:00 with hour 1 at PLN 4.00, When a session from 12:05 to 12:40 ending at a station is priced, Then T-2 applies and the total fee is PLN 4.00.
  - `R5 A station return pays no return fee (spec: finance-prices-ride-by-tiered-tariff#E5.1)`: Given the launch tariff, When a session from 10:00 to 10:30 ending at a station is priced, Then the return fee is PLN 0.00 and the total fee is PLN 3.00.

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history. The calculation itself reads tariffs and changes no state; whether it is a domain service or a method of the tariff is the tactical skill's choice |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style". Pricing calls nobody for this intent |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; nothing new to persist here (tariffs from `pricing-manager-publishes-tariff`) |
| D5 | Price Ride is a query | the spec's command becomes Pricing's OHS query Price for Session; Ride Priced is its answer, recorded by Settlement | Pricing canvas Purpose ("keeps no rides, only tariffs"); integration-contracts.md; a stateless calculator has no fact of its own to publish |
| D6 | Examples' clock times | UTC instants on the example date (2026-11-09 10:00 → `2026-11-09T10:00:00Z`) | integration-contracts.md: ISO-8601 instants |
| D7 | Tariff timeline in the shared Spring context | R4's T-2 is published on this plan's test day **2030-01-09** (valid from `2030-01-09T12:00:00Z`, `MutableClock` set to the day before), followed by "launch prices again" valid from `2030-01-10T00:00:00Z`; all other examples keep 2026-11-09 | same rule as plan `pricing-manager-publishes-tariff` D8: one shared application context, R5 forbids shared valid-froms |

## Steps

### S1: Update the Pricing canvas
- **Status:** done
- **Context:** Pricing
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** none (tags for E1.1, E1.2, E2.1, E2.2, E2.3, E3.1, E3.2, E4.1, E4.2, E5.1, E5.2)
- **Done when:** canvas carries the six new decisions and the five tags. Prerequisite: plan `pricing-manager-publishes-tariff` S1-S5 done (module, tariff schedule, launch tariff).

### S2: Price for Session query records (6a)
- **Status:** done
- **Context:** Pricing
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Price Ride
- **Done when:** `pricing/api/queries/PriceForSession` and `SessionPrice` records (ending as `STATION`, `FREE_FLOATING`, `LOCK_FAILURE`, `MAX_RENTAL_TIME`); `PricingArchitectureTest` green; `./mvnw test` green.

### S3: Free period, billing hours and tiered fees, test-first (run 1 of 2)
- **Status:** done
- **Context:** Pricing
- **Skill:** ddd-tactical-building-blocks (D1: loop off; spec examples verbatim, one per cycle)
- **Covers:** R1, R2, R3, E1.1, E1.2, E2.1, E2.2, E2.3, E3.1, E3.2
- **Done when:** one domain test per example; boundaries 15:00 / 15:01 and 75:00 / 75:01 measured to the second; `./mvnw test` green.

### S4: Tariff at session start and endings without a return fee, test-first (run 2 of 2)
- **Status:** done
- **Context:** Pricing
- **Skill:** ddd-tactical-building-blocks (D1: loop off)
- **Covers:** R4, R5, E4.1, E4.2, E5.1, E5.2
- **Done when:** one domain test per example; R4 reuses "tariff valid at" from `pricing-manager-publishes-tariff` S4; a station return and a lock failure get return fee PLN 0.00 and no penalty; `./mvnw test` green.

### S5: Query handler and REST resource (6b)
- **Status:** done
- **Context:** Pricing
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Price Ride, Ride Priced
- **Done when:** `PriceForSessionQueryHandler` and `GET /pricing/session-price` returning `{tariffId, billingHours, hourlyFees, returnFee, returnFeeKind, penalty, total}` (money as decimal strings), `400` for malformed input; wiring in `pricing/infra/config`; `./mvnw test` green.

### S6: Acceptance tests
- **Status:** done
- **Context:** Pricing
- **Skill:** none (implementation-planner)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/pricing/FinancePricesRideByTieredTariffSpecTest.java`, `@VelocitySpringTest`, one method per example, calling `GET /pricing/session-price` over HTTP; R4 publishes T-2 through `POST /pricing/tariffs` per D7; the Settlement → Pricing hand-off is asserted in `finance-charges-finished-ride` S10 (with Awaitility), not here; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `FinancePricesRideByTieredTariffSpecTest#e1_1_ride_of_twelve_minutes_is_free` |
| E1.2 | `FinancePricesRideByTieredTariffSpecTest#e1_2_ride_of_exactly_fifteen_minutes_is_free` |
| E2.1 | `FinancePricesRideByTieredTariffSpecTest#e2_1_one_second_after_free_period_starts_first_billing_hour` |
| E2.2 | `FinancePricesRideByTieredTariffSpecTest#e2_2_seventy_five_minutes_is_one_billing_hour` |
| E2.3 | `FinancePricesRideByTieredTariffSpecTest#e2_3_one_second_more_starts_second_billing_hour` |
| E3.1 | `FinancePricesRideByTieredTariffSpecTest#e3_1_three_billing_hours_cost_fifteen` |
| E3.2 | `FinancePricesRideByTieredTariffSpecTest#e3_2_fourth_and_further_hours_cost_the_same` |
| E4.1 | `FinancePricesRideByTieredTariffSpecTest#e4_1_ride_started_before_new_tariff_uses_launch_tariff` |
| E4.2 | `FinancePricesRideByTieredTariffSpecTest#e4_2_ride_started_after_new_tariff_uses_it` |
| E5.1 | `FinancePricesRideByTieredTariffSpecTest#e5_1_station_return_pays_no_return_fee` |
| E5.2 | `FinancePricesRideByTieredTariffSpecTest#e5_2_lock_failure_pays_hourly_fees_only` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1, E1.2 | S3, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/fee/FeeCalculator.java`; tests `src/test/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/fee/FeeCalculatorTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/pricing/FinancePricesRideByTieredTariffSpecTest.java` |
| R2 / E2.1, E2.2, E2.3 | S3, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/fee/FeeCalculator.java`; tests `FeeCalculatorTest`, `FinancePricesRideByTieredTariffSpecTest` |
| R3 / E3.1, E3.2 | S3, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/fee/FeeCalculator.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/tariff/Tariff.java` (`hourlyFeeOf`); tests `FeeCalculatorTest`, `FinancePricesRideByTieredTariffSpecTest` |
| R4 / E4.1, E4.2 | S4, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/fee/FeeCalculator.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/tariff/TariffSchedule.java` (`tariffValidAt`); tests `FeeCalculatorTest`, `FinancePricesRideByTieredTariffSpecTest` |
| R5 / E5.1, E5.2 | S4, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/fee/FeeCalculator.java`; tests `FeeCalculatorTest`, `FinancePricesRideByTieredTariffSpecTest` |
## Risks and open questions

- **CONTRACT GAP:** the Price for Session answer has no `billingHours`, and `hourlyFees` is a single value, while the spec's Ride Priced carries billing hours and one hourly fee per billing hour (E2.1-E3.2 assert "it has N billing hours"). Planned as if the answer carried `billingHours` (integer); `hourlyFees` read as the sum of the hourly fees (decimal string). The per-hour breakdown stays derivable from the tariff and is asserted in the domain tests.
- Spec's *Price Ride* command is realised as a query (D5). If reviewers want a recorded "Ride Priced" fact, it belongs to Settlement (which keeps rides), not to Pricing.
- E5.2's position is passed but unused for a lock failure; the zone lookup arrives with `finance-prices-free-floating-return-by-zone` and must keep skipping lock failures (its R3).
- Cross-plan order: `pricing-manager-publishes-tariff` S1-S5 → this plan → `pricing-manager-publishes-tariff` S7.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE. All rules sit in Pricing (CML "Total fee calculation"); the only hand-off (Settlement asks Pricing's OHS) follows the CML direction and is built downstream by `finance-charges-finished-ride`; command → query translation recorded as D5; every example covered and tested; tactical runs ≤ 3 rules; no building block chosen in the plan.

- 2026-11-08 (build): Building block (tactical skill): `FeeCalculator` domain service (stateless; value objects `Session`, `Ending`, `SessionFee`; the tariff valid at the session start is picked from the `TariffSchedule` inside the calculation, so R4 lives in the domain). Billing time truncated to whole seconds (DE-83).
- 2026-11-08 (build): `GET /pricing/session-price` answers `{tariffId, billingHours, hourlyFees, returnFee, returnFeeKind, penalty, total}` (money as decimal strings); `400` when `endedAt` isn't after `startedAt`, the ending is unknown or a time is malformed; `409 NoTariffValid` for a session started before the launch tariff.

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: built and delivered (autonomous run, stage 4)
- Every step done through its skill (D1: TDD checkpoint loop off; one example per cycle, red seen before green, implemented by Claude); per-cycle log in `docs/bounded-contexts/velocity/pricing/plan.md`.
- Acceptance tests and traceability files filled in; full suite `./mvnw test` green.
