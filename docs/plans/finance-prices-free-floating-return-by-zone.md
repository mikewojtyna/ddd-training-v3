---
name: finance-prices-free-floating-return-by-zone
intent: finance-prices-free-floating-return-by-zone
spec: finance-prices-free-floating-return-by-zone
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: Finance wants a free-floating return priced by the zone where the ride ended

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

Every rule lands in **Pricing** (CML: "drop zone / unauthorized zone fees"). The zone map comes from the external
**City** through Pricing's secondary port `ZoneMap` and the `cityacl` adapter (CML `City [U]->[D,ACL] Pricing`),
backed by a configured City zone map. The return fee is added to the **Price for Session** answer built by
`finance-prices-ride-by-tiered-tariff`, which is a prerequisite, as is Publish Tariff from
`pricing-manager-publishes-tariff` (for R4). **Spec problem:** E1.3's position is not on the Old Town Square border
of the contract's zone map (see Risks); its acceptance test waits for the spec fix.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A free-floating return inside a drop zone adds the drop zone fee | Pricing | return fees are part of the total fee (CML Pricing vision "drop zone / unauthorized zone fees"; canvas UL "Drop zone", "Return fee") |
| R2 A free-floating return outside every drop zone adds the unauthorized zone return fee | Pricing | same; canvas UL "Unauthorized zone"; E2.2's "zone map valid at the ride's end" is Pricing's question to the City (DE-84) |
| R3 A lock failure pays no return fee, wherever the bike is | Pricing | canvas UL "Return fee: only for free-floating endings", decision "A lock failure costs nothing extra" (DE-23, DE-32) |
| R4 The return fee comes from the tariff valid when the ride started | Pricing | Pricing holds the tariffs; precedent: `finance-prices-ride-by-tiered-tariff` R4 |
| Command: Price Ride | Pricing (asked by Settlement) | as in plan `finance-prices-ride-by-tiered-tariff` (D5 there): the query Price for Session |
| Event: Ride Priced | Pricing | as in plan `finance-prices-ride-by-tiered-tariff`: the Price for Session answer, now with `returnFee` and `returnFeeKind` |

**Outcome:** several existing contexts: Pricing owns every rule; it asks the external City for the zone at a position
(ACL), and Settlement asks Pricing (OHS, built downstream by `finance-charges-finished-ride`). Directions match the CML.

## Context map changes

- None. `City [U]->[D,ACL] Pricing : ZoneMap` is in the map.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Free-floating return | Pricing | Ending *free-floating* (`FREE_FLOATING`) | Settlement's `pricingctxacl` (from Rentals' `FREE_FLOATING_RETURN`) |
| Zone map | Pricing | Zone map (new UL term) | `cityacl` adapter: the City's drop-zone areas → Pricing's "drop zone / unauthorized zone at a position" |
| Drop zone, unauthorized zone | Pricing | same terms | canvas UL |
| Return fee kind: none / drop zone / unauthorized zone | Pricing | `returnFeeKind`: `NONE` / `DROP_ZONE` / `UNAUTHORIZED_ZONE` | REST adapter |
| Position (latitude, longitude) | Pricing | Position | REST adapter (`latitude`, `longitude` query parameters) |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Price Ride | Pricing | `PriceForSession` / `PriceForSessionQueryHandler` / `GET /pricing/session-price` from `finance-prices-ride-by-tiered-tariff`, now using `latitude`/`longitude`; `400` when the ending is `FREE_FLOATING` and the position is missing |
| Event: Ride Priced | Pricing | the answer's `returnFee` and `returnFeeKind`; no domain or integration event (D5 of the tiered plan) |
| Zone at position (needed by R1, R2) | Pricing → City | secondary port `ZoneMap` (Pricing's language: drop zone or unauthorized zone at a position, at a moment) in the domain; adapter `pricing/infra/adapters/secondary/cityacl` (`@AntiCorruptionLayer`, `@SecondaryAdapter`) backed by the configured City zone map (Old Town Square 52.2297/21.0122 r 150 m, Riverside Park 52.2400/21.0300 r 200 m, integration-contracts.md) |

## Canvas changes

### Pricing (`docs/bounded-contexts/velocity/pricing/canvas.md`)

- **Ubiquitous language, add:** **Zone map**: the City's drop zones valid at a moment; Pricing uses the one valid at the end of the session (DE-84). A position on a zone's border is inside.
- **Tag existing decisions:** "A free-floating return in a drop zone adds the drop zone fee" ← E1.1; "A quick free-floating trip still pays the return fee" ← E1.2; "A free-floating return outside every drop zone adds the unauthorized zone return fee" ← E2.1; "A lock failure costs nothing extra" ← E3.1.
- **Business decisions, add:**
  - `R1 A position on a drop zone's border is inside (spec: finance-prices-free-floating-return-by-zone#E1.3)`: Given the launch tariff, When a session from 10:00 to 10:30 ending free-floating exactly on the border of Old Town Square is priced, Then the return fee is the drop zone fee PLN 2.00 and the total fee is PLN 5.00 (DE-84). *Position corrected in the spec to (52.23104, 21.0122), 149 m from the centre.*
  - `R2 The zone map valid at the end of the session applies (spec: finance-prices-free-floating-return-by-zone#E2.2)`: Given Old Town Square was removed from the zone map at 10:20, When a session from 10:00 to 10:30 ending free-floating at (52.2298, 21.0123) is priced, Then the return fee is the unauthorized zone return fee PLN 40.00 and the total fee is PLN 43.00 (DE-84).
  - `R3 A lock failure inside a drop zone adds no drop zone fee (spec: finance-prices-free-floating-return-by-zone#E3.2)`: Given the launch tariff, When a session from 10:00 to 10:30 that ended in a lock failure at (52.2298, 21.0123) is priced, Then no drop zone fee is added and the total fee is PLN 3.00.
  - `R4 The return fee comes from the tariff valid at session start (spec: finance-prices-free-floating-return-by-zone#E4.1)`: Given the launch tariff and T-2 valid from 12:00 with a drop zone fee of PLN 3.00, When a session from 11:50 to 12:30 ending free-floating at (52.2298, 21.0123) is priced, Then the return fee is PLN 2.00 and the total fee is PLN 5.00.
  - `R4 A session started under a new tariff pays its return fee (spec: finance-prices-free-floating-return-by-zone#E4.2)`: Given the same T-2, When a session from 12:05 to 12:40 ending free-floating at (52.2298, 21.0123) is priced, Then the return fee is PLN 3.00 and the total fee is PLN 6.00.

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style". Exception: the City is external and has no REST resource in this app, so `cityacl` reads a configured zone map instead of calling REST (integration-contracts.md "Pricing") |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; nothing new to persist here |
| D5 | Zone lookup happens outside the calculation | the query handler asks `ZoneMap` for the zone at the session's end position and end time, and only for a free-floating ending; the calculation receives the zone as a fact | keeps the domain pure (ports aren't called from domain objects); R3 means a lock failure never needs the lookup |
| D6 | Configured City zone map is time-aware | zones carry an optional validity interval; the simulated City map (`SimulatedCityZoneMap`, in `cityacl`) lets a test withdraw a zone for an interval | E2.2 (DE-84: zone map valid at the end of the ride) |
| D7 | Examples' clock times | UTC instants on the example date | integration-contracts.md: ISO-8601 instants |
| D8 | Shared Spring context | R4's T-2 on test day **2030-02-09** (valid from `2030-02-09T12:00:00Z`, launch prices again from `2030-02-10T00:00:00Z`); E2.2 runs on test day **2030-02-20** and withdraws Old Town Square from `10:20` to `2030-02-21T00:00:00Z` only | same rule as plan `pricing-manager-publishes-tariff` D8; a zone withdrawal for good would break every later Old Town Square example |

## Steps

### S1: Update the Pricing canvas
- **Status:** done
- **Context:** Pricing
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** none (tags for E1.1, E1.2, E1.3, E2.1, E2.2, E3.1, E3.2, E4.1, E4.2)
- **Done when:** canvas carries the UL term, the four tags and the five new decisions. Prerequisite: plan `finance-prices-ride-by-tiered-tariff` S5 done.

### S2: Zone map port and the City ACL (6a)
- **Status:** done
- **Context:** Pricing
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** none (structure for R1, R2)
- **Done when:** port `ZoneMap` (domain, `@SecondaryPort`); adapter in `pricing/infra/adapters/secondary/cityacl` (`@AntiCorruptionLayer`) backed by the configured City zone map with Old Town Square and Riverside Park, time-aware (D6), border inclusive; adapter unit test for inside / outside / withdrawn; `PricingArchitectureTest` green; `./mvnw test` green.

### S3: Return fee by ending and zone, test-first (run 1 of 2)
- **Status:** done
- **Context:** Pricing
- **Skill:** ddd-tactical-building-blocks (D1: loop off; spec examples verbatim, one per cycle)
- **Covers:** R1, R2, R3, E1.1, E1.2, E1.3, E2.1, E2.2, E3.1, E3.2
- **Done when:** one domain test per example, the zone given as a fact (D5); E1.3 written with the corrected position; `./mvnw test` green.

### S4: Return fee from the tariff at session start, test-first (run 2 of 2)
- **Status:** done
- **Context:** Pricing
- **Skill:** ddd-tactical-building-blocks (D1: loop off)
- **Covers:** R4, E4.1, E4.2
- **Done when:** one domain test per example; `./mvnw test` green.

### S5: Zone lookup in the query handler (6b)
- **Status:** done
- **Context:** Pricing
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Price Ride, Ride Priced
- **Done when:** `PriceForSessionQueryHandler` asks `ZoneMap` for free-floating endings only (D5) at `endedAt`; `GET /pricing/session-price` returns `returnFee` and `returnFeeKind`; `400` for a free-floating ending without a position; wiring in `pricing/infra/config`; `./mvnw test` green.

### S6: Acceptance tests
- **Status:** done
- **Context:** Pricing
- **Skill:** none (implementation-planner)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/pricing/FinancePricesFreeFloatingReturnByZoneSpecTest.java`, `@VelocitySpringTest`, one method per example over `GET /pricing/session-price`; R4 publishes T-2 through `POST /pricing/tariffs` and E2.2 withdraws the zone through `SimulatedCityZoneMap`, both per D8; E1.3 uses the corrected position (52.23104, 21.0122); `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `FinancePricesFreeFloatingReturnByZoneSpecTest#e1_1_return_in_drop_zone_adds_drop_zone_fee` |
| E1.2 | `FinancePricesFreeFloatingReturnByZoneSpecTest#e1_2_quick_return_in_drop_zone_still_pays_drop_zone_fee` |
| E1.3 | `FinancePricesFreeFloatingReturnByZoneSpecTest#e1_3_return_on_drop_zone_border_counts_as_inside` |
| E2.1 | `FinancePricesFreeFloatingReturnByZoneSpecTest#e2_1_return_outside_drop_zones_adds_unauthorized_zone_fee` |
| E2.2 | `FinancePricesFreeFloatingReturnByZoneSpecTest#e2_2_zone_map_valid_at_ride_end_applies` |
| E3.1 | `FinancePricesFreeFloatingReturnByZoneSpecTest#e3_1_lock_failure_outside_drop_zones_pays_no_return_fee` |
| E3.2 | `FinancePricesFreeFloatingReturnByZoneSpecTest#e3_2_lock_failure_in_drop_zone_pays_no_drop_zone_fee` |
| E4.1 | `FinancePricesFreeFloatingReturnByZoneSpecTest#e4_1_return_fee_from_launch_tariff_when_started_before_new_tariff` |
| E4.2 | `FinancePricesFreeFloatingReturnByZoneSpecTest#e4_2_return_fee_from_new_tariff_when_started_after_it` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1, E1.2, E1.3 | S2, S3, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/fee/FeeCalculator.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/infra/adapters/secondary/cityacl/SimulatedCityZoneMap.java`, `CityZoneMapAdapter.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/api/queries/PriceForSessionQueryHandler.java`; tests `src/test/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/fee/ReturnFeeTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/pricing/infra/adapters/secondary/cityacl/CityZoneMapAdapterTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/pricing/FinancePricesFreeFloatingReturnByZoneSpecTest.java` |
| R2 / E2.1, E2.2 | S2, S3, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/fee/FeeCalculator.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/infra/adapters/secondary/cityacl/SimulatedCityZoneMap.java`, `CityZoneMapAdapter.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/api/queries/PriceForSessionQueryHandler.java`; tests `ReturnFeeTest`, `CityZoneMapAdapterTest`, `FinancePricesFreeFloatingReturnByZoneSpecTest` |
| R3 / E3.1, E3.2 | S3, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/fee/FeeCalculator.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/api/queries/PriceForSessionQueryHandler.java` (no zone lookup for a lock failure); tests `ReturnFeeTest`, `FinancePricesFreeFloatingReturnByZoneSpecTest` |
| R4 / E4.1, E4.2 | S4, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/fee/FeeCalculator.java`; tests `ReturnFeeTest`, `FinancePricesFreeFloatingReturnByZoneSpecTest` |
## Risks and open questions

- **Spec problem, E1.3 (back to `spec-writer`):** (52.2311, 21.0122) is about **155.7 m** north of the Old Town Square centre (52.2297, 21.0122): 0.0014° of latitude × 111.195 km/°. With the 150 m radius of the contract's zone map (integration-contracts.md "Pricing") and of the Pricing canvas, that position is *outside* the drop zone, so E1.3 as written would yield the unauthorized zone fee. Fix options: the spec uses a position exactly 150 m from the centre (e.g. latitude 52.231049, 21.0122, ≈150.0 m), or the example states the zone's border position abstractly and the test builds a zone whose border passes through the point. An exact floating-point border also needs a stated tolerance. **Resolved 2026-11-08 by the orchestrator:** the spec now uses (52.23104, 21.0122), 149.0 m from the centre (haversine, R = 6371 km), just inside the border; an exact floating-point border isn't tested.
- **CONTRACT GAP:** the `ZoneMap` port must answer "zone at position **at a moment**", and the configured City map needs zone validity intervals plus a test seam to withdraw a zone for an interval (D6, E2.2). integration-contracts.md only describes a static configured map. Planned as if it existed.
- Spec **Q3** (a bike left next to a station but not docked) is open. The plan treats it like any position: the zone map decides. If the answer adds station surroundings as drop zones, that's zone-map configuration, not a new rule.
- A lock failure "away from a station" may arrive without a position; R3 means Pricing never needs it, so the handler must not require a position for `LOCK_FAILURE`.
- The configured zone map is a stand-in for the City's real feed (Pricing canvas Assumptions).

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE with one spec finding. Placement in Pricing follows the CML vision; City → Pricing via ACL as in the map; every example covered and tested; E1.3's geometric inconsistency is sent back to the spec rather than planned around (its acceptance test disabled until fixed).

- 2026-11-08 (build): `ZoneMap` port (`domain/zone`, `zoneAt(Position, Instant)`), adapter `CityZoneMapAdapter` over `SimulatedCityZoneMap` (City language: approved drop-off areas, withdrawals for a window; haversine R = 6371 km, border inside) in `infra/adapters/secondary/cityacl`; beans `pricingSimulatedCityZoneMap`, `pricingZoneMap`. The query handler looks up the zone at `endedAt` for `FREE_FLOATING` only (D5); `400` for a free-floating ending without a position.
- 2026-11-08 (build): E4.1 asserts the launch prices and that T-2 doesn't apply rather than the id `LAUNCH` (shared context, D8).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: built and delivered (autonomous run, stage 4)
- Every step done through its skill (D1: TDD checkpoint loop off; one example per cycle, red seen before green, implemented by Claude); per-cycle log in `docs/bounded-contexts/velocity/pricing/plan.md`.
- Acceptance tests and traceability files filled in; full suite `./mvnw test` green.
