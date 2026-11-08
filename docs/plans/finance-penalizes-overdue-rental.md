---
name: finance-penalizes-overdue-rental
intent: finance-penalizes-overdue-rental
spec: finance-penalizes-overdue-rental
system: velocity
status: approved
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: Finance wants a penalty charged for a rental that exceeded the maximum rental time

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

Every pricing rule lands in **Pricing** (CML: "penalty"; canvas UL "Penalty", decision "Exceeding the maximum rental
time adds the penalty"). The penalty is added to the **Price for Session** answer for the ending *maximum rental time*.
*When* an overdue session ends (the 12-hour mark, DE-82) is decided by **Rentals** ("Max rental time end", plan
`fleet-operations-ends-overdue-rental`); Pricing prices the end time it is given. No new structure is needed: the query
already carries `ending=MAX_RENTAL_TIME` and `penalty`, so this plan is canvas → domain → acceptance tests.
Prerequisites: `finance-prices-ride-by-tiered-tariff` S5 and `finance-prices-free-floating-return-by-zone` S5.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 An overdue rental pays the hourly fees for the full 12 hours plus the penalty | Pricing | the penalty is part of the total fee (CML Pricing vision "penalty"; canvas UL "Penalty", DE-33). The 12-hour end time in E1.2 is Rentals' fact (CML Rentals "Max rental time end", DE-82), carried in `RentalSessionEnded.endedAt` |
| R2 An overdue rental pays no return fee | Pricing | canvas UL "Return fee: only for free-floating endings" (DE-33, DE-34) |
| R3 A ride of exactly the maximum rental time is not overdue | Pricing for the price (penalty only for the ending *maximum rental time*); Rentals for the ending itself | the ending is decided by Rentals (DE-20, plan `fleet-operations-ends-overdue-rental`); Pricing's rule is "no penalty unless the ending is maximum rental time" |
| R4 The penalty comes from the tariff valid when the ride started | Pricing | Pricing holds the tariffs; precedent `finance-prices-ride-by-tiered-tariff` R4 |
| Command: Price Ride | Pricing (asked by Settlement) | as in plan `finance-prices-ride-by-tiered-tariff` (D5): the query Price for Session |
| Event: Ride Priced | Pricing | as in plan `finance-prices-ride-by-tiered-tariff`: the Price for Session answer, with `penalty` |

**Outcome:** several existing contexts: Pricing owns the price; Rentals (upstream of Settlement) supplies the 12-hour
end time; Settlement asks Pricing. No new hand-off is introduced by this plan.

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Overdue rental / ending *maximum rental time exceeded* | Pricing | Ending *maximum rental time* (`MAX_RENTAL_TIME`) | Settlement's `pricingctxacl` (from Rentals' `MAX_RENTAL_TIME_EXCEEDED`) |
| Maximum rental time (12 h) | Rentals | Maximum rental time | Rentals' term; Pricing doesn't need the number |
| Penalty | Pricing | Penalty | canvas UL |
| Hourly fees, return fee, total fee | Pricing | same | canvas UL |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Price Ride | Pricing | `PriceForSession` / `PriceForSessionQueryHandler` / `GET /pricing/session-price` from `finance-prices-ride-by-tiered-tariff`, with `ending=MAX_RENTAL_TIME` |
| Event: Ride Priced | Pricing | the answer's `penalty` (and `returnFee` `0.00`, `returnFeeKind` `NONE`); no domain or integration event (D5 of the tiered plan) |

## Canvas changes

### Pricing (`docs/bounded-contexts/velocity/pricing/canvas.md`)

- **Tag existing decision:** "Exceeding the maximum rental time adds the penalty" ← E1.1.
- **Business decisions, add:**
  - `R1 An overdue session noticed late is priced to the 12-hour mark (spec: finance-penalizes-overdue-rental#E1.2)`: Given a session started at 08:00:00 whose overrun was noticed at 20:20 and which Rentals therefore ended at the 12-hour mark, 20:00:00, for exceeding the maximum rental time, When it is priced, Then the hourly fees are PLN 96.00, the penalty PLN 150.00 and the total fee PLN 246.00 (DE-82).
  - `R2 An overdue session pays no return fee (spec: finance-penalizes-overdue-rental#E2.1)`: Given a session from 08:00:00 to 20:00:00 ended for exceeding the maximum rental time, with the bike last at (52.2500, 21.1000) outside every drop zone, When it is priced, Then the return fee is PLN 0.00 and the total fee is PLN 246.00.
  - `R3 Exactly 12 hours ending at a station is not overdue (spec: finance-penalizes-overdue-rental#E3.1)`: Given the launch tariff, When a session from 08:00:00 to 20:00:00 ending at a station is priced, Then the hourly fees are PLN 96.00, there is no penalty and the total fee is PLN 96.00.
  - `R4 The penalty comes from the tariff valid at session start (spec: finance-penalizes-overdue-rental#E4.1)`: Given T-2 valid from 12:00 with a penalty of PLN 200.00, When a session from 08:00:00 to 20:00:00 ended for exceeding the maximum rental time is priced, Then the penalty is PLN 150.00 and the total fee is PLN 246.00.

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md "Integration style". No new call here |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain"; nothing new to persist |
| D5 | Pricing does not cap overdue sessions at 12 h | Pricing prices the `endedAt` it receives; the 12-hour mark is Rentals' rule (DE-82), and E1.2's late notice is asserted by `fleet-operations-ends-overdue-rental`'s acceptance tests (session ends at started + 12 h) | one rule, one owner; a second cap in Pricing would hide a Rentals bug |
| D6 | Examples' clock times | UTC instants on the example date | integration-contracts.md: ISO-8601 instants |
| D7 | Shared Spring context | R4's T-2 on test day **2030-03-09** (valid from `2030-03-09T12:00:00Z`, launch prices again from `2030-03-10T00:00:00Z`) | same rule as plan `pricing-manager-publishes-tariff` D8 |

## Steps

### S1: Update the Pricing canvas
- **Status:** todo
- **Context:** Pricing
- **Skill:** none (apply the approved canvas diff above)
- **Covers:** none (tags for E1.1, E1.2, E2.1, E3.1, E4.1)
- **Done when:** canvas carries the tag and the four new decisions. Prerequisites: plans `finance-prices-ride-by-tiered-tariff` S5 and `finance-prices-free-floating-return-by-zone` S5 done.

### S2: Penalty by ending, test-first (run 1 of 2)
- **Status:** todo
- **Context:** Pricing
- **Skill:** ddd-tactical-building-blocks (D1: loop off; spec examples verbatim, one per cycle)
- **Covers:** R1, R2, R3, E1.1, E1.2, E2.1, E3.1
- **Done when:** one domain test per example; the ending *maximum rental time* adds the tariff's penalty and no return fee even with a position outside every drop zone; 12 h ending at a station has no penalty; `./mvnw test` green. If the domain needs a change to the query records, run the scaffolder in update mode first and note it in the changelog.

### S3: Penalty from the tariff at session start, test-first (run 2 of 2)
- **Status:** todo
- **Context:** Pricing
- **Skill:** ddd-tactical-building-blocks (D1: loop off)
- **Covers:** R4, E4.1
- **Done when:** one domain test; `./mvnw test` green.

### S4: Acceptance tests
- **Status:** todo
- **Context:** Pricing
- **Skill:** none (implementation-planner)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/pricing/FinancePenalizesOverdueRentalSpecTest.java`, `@VelocitySpringTest`, one method per example over `GET /pricing/session-price` (E1.2 with `endedAt` = 20:00:00 per D5; E2.1 with the position); E4.1 publishes T-2 per D7; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `FinancePenalizesOverdueRentalSpecTest#e1_1_overdue_rental_pays_twelve_hours_plus_penalty` |
| E1.2 | `FinancePenalizesOverdueRentalSpecTest#e1_2_overdue_rental_noticed_late_is_priced_to_twelve_hour_mark` |
| E2.1 | `FinancePenalizesOverdueRentalSpecTest#e2_1_overdue_rental_pays_no_return_fee` |
| E3.1 | `FinancePenalizesOverdueRentalSpecTest#e3_1_ride_of_exactly_twelve_hours_has_no_penalty` |
| E4.1 | `FinancePenalizesOverdueRentalSpecTest#e4_1_penalty_from_tariff_valid_at_start` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1, E1.2 | S2, S4 | |
| R2 / E2.1 | S2, S4 | |
| R3 / E3.1 | S2, S4 | |
| R4 / E4.1 | S3, S4 | |

## Risks and open questions

- E1.2's point (hourly fees stop at the 12-hour mark although the overrun was noticed at 20:20) is only meaningful end to end. With D5, Pricing's test for E1.2 equals E1.1 in its inputs; the guarantee comes from Rentals ending the session at started + 12 h (plan `fleet-operations-ends-overdue-rental`). Reviewers may want an additional cross-context test (Rentals overdue job at 20:20 → Settlement → ride fee 246.00); it would live with `finance-charges-finished-ride`'s Settlement tests.
- No spec questions are open.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE. Price rules in Pricing, end-time rule left with Rentals (D5) so no rule is duplicated; no new hand-off; every example covered and tested; tactical runs ≤ 3 rules; skipping the scaffolder is justified (contract already carries the ending and the penalty).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.
