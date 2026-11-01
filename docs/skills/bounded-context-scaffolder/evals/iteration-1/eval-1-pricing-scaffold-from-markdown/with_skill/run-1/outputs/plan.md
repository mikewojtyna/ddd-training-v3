# Pricing bounded context: implementation plan

- **System / package**: `velocity.pricing`
- **Mode**: scaffold
- **Source canvas**: `docs/bounded-contexts/velocity/pricing/canvas.md` (copied from `.claude/skills/bounded-context-scaffolder/evals/files/pricing-canvas.md`)
- **Last updated**: 2026-11-01

## Canvas summary
Pricing turns a finished ride into a fair, predictable fee: a free period for short trips, progressively more expensive hours, and always the price list valid at ride start. It is a **core** domain, it generates **revenue**, and it is **custom built**. Its domain role is **Execution Model** (`docs/model-traits-worksheet.md`): it calculates the total fee for every rental session.

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| System | `velocity` | The canvas is VeloCity's Pricing, and `Pricing` is a `BoundedContext` in `src/main/cml/VeloCity-*.cml`. |
| Context name | `@BoundedContext(name = "Pricing")` | The canvas name matches the CML name. |
| Package | `velocity.pricing` | Lowercase from the name. The package didn't exist, so this is scaffold mode. |
| Modules | 1 module = 1 context | Default. |
| ACL towards Rentals (Get Return Charges) | yes, `infra/adapters/secondary/rentalsctxacl` | The canvas asks for one, and it's the skill default. The port `ReturnChargesProvider` speaks Pricing's language. |
| Inbound "Rental Session Ended" | own record `api/usecases/RentalSessionEnded` + `PriceRideUseCase`; no listener yet | Rentals publishes no such event in code yet, so there's no upstream type to listen to. |
| Aggregate | `PriceList` (`domain/pricelist`) | See "Business rules → aggregates". |
| Choosing the valid price list | `PriceList.validAt(Collection<PriceList>, Instant)` in the domain; the repository returns `all()` | Keeps the "price valid at ride start" rule in the domain and testable without infrastructure. Loading every price list is fine at this scale. Revisit if price lists pile up. |
| Pricing a ride has no save step | `PriceRideUseCase` loads, prices, publishes | Pricing is a pure calculation, and the price list doesn't change. |
| Estimate Fee handler | uses `PriceListRepository` + `PriceList.estimate(...)` instead of a separate read model | The estimate is a calculation with the price list valid *now*, not a stored view. The clock is passed in (`Clock.systemUTC()`) and isn't a global `Clock` bean. |
| Money | `org.joda.money.Money` | The canvas talks about PLN amounts. CrowdSorcery already uses joda-money. |
| REST adapters | `PriceListRestApi` (POST `/velocity/pricing/price-lists`), `FeeEstimateRestApi` (GET `/velocity/pricing/fee-estimates?rideMinutes=N`) | Back office and rider app are apps. The canvas doesn't describe the endpoints, so path and body shape are scaffolding choices. They're implemented, not stubbed, because they only translate. |
| Billing period counting | started hours counted from the end of the free period | Literal reading of the definition "one started hour of a ride after the free period". See open points. |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Rentals | Event | Rental Session Ended | `api/usecases/RentalSessionEnded` (our record), `api/usecases/PriceRideUseCase`. Listener in `infra/adapters/primary/rentalsctxacl` is a follow-up. |
| Back office | Command | Publish Price List | `api/usecases/PublishPriceList`, `api/usecases/PublishPriceListUseCase`, `infra/adapters/primary/PriceListRestApi` + `PublishPriceListRequest` |
| Rider app | Query | Estimate Fee | `api/queries/EstimateFeeQuery`, `api/queries/FeeEstimate`, `api/queries/EstimateFeeQueryHandler`, `infra/adapters/primary/FeeEstimateRestApi` |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Ride Priced | Event | Settlement | domain event `PriceListEvent.RidePriced` | `infra/adapters/secondary/IntegrationEventsPublisher` publishes `api/events/RidePriced` (published language, no ACL) |
| Get Return Charges | Query | Rentals | `domain/pricelist/ReturnChargesProvider` | `infra/adapters/secondary/rentalsctxacl/RentalsReturnChargesAdapter` (stub) |

### Ubiquitous language
| Term | Code type | Kind |
|---|---|---|
| Price List | `PriceList` (+ `PriceListId`) | aggregate root |
| Free Period | `FreePeriod` | VO |
| Billing Period | `BillingPeriod` | VO |
| Total Fee | `TotalFee` | VO |
| Return Charges | `ReturnCharges` | VO |
| (ride, not in the UL) | `Ride` (+ `RentalSessionId`, `RiderId`) | VO, see open points |
| (Ride Priced, outbound message) | `PriceListEvent.RidePriced` / `api.events.RidePriced` | domain event / integration event |

### Business rules → aggregates
Every rule's *Given* is one or more price lists, and its *When* is "a ride is priced". The state that has to be consistent is the price list's fees and its valid-from moment, so `PriceList` is the consistency boundary. Selecting among price lists (rule 4) reads several aggregates but changes none, so it's a domain function over `PriceList`s, not a bigger aggregate.

| Rule (GWT) | Invariant | Aggregate | Test |
|---|---|---|---|
| Free ride: 10 min free, ride 8 min → 0.00 PLN | no billing period while the ride is within the free period | `PriceList` | `PriceListTest.freeRide` |
| First hour: 10 min free, first hour 5.00, ride 45 min → 5.00 PLN | 35 min after the free period = 1 started hour = first-hour fee | `PriceList` | `PriceListTest.firstHour` |
| Tiered hours: 10 min free, 5/7/9/12 PLN, ride 3h20 → 33.00 PLN | 190 min after the free period = 4 started hours = 5 + 7 + 9 + 12 | `PriceList` | `PriceListTest.tieredHours` |
| Price valid at ride start: A 5.00 from 08:00, B 6.00 from 09:00, ride 08:50–09:30 → 5.00 PLN | the price list with the latest valid-from not after the ride start applies; a price list never prices a ride that started before it was valid (`PriceListNotValidForRide`) | `PriceList` (+ `PriceList.validAt`) | `PriceListTest.priceValidAtRideStart` |

Consistency check, done by hand: all four examples agree with the definitions. No contradictions found.

### Rules awaiting GWT
- **Return charges in the total fee.** The definition "Total Fee = sum of billing period fees plus return charges" is implemented (`PriceList.price` adds them), but no example covers it, so there's no test. Suggested GWT: *given a price list with a 10 minute free period and first-hour fee 5.00 PLN, and Rentals reporting a 3.00 PLN drop-zone charge, when a ride of 45 minutes is priced, then the total fee is 8.00 PLN.* Status: awaiting confirmation.
- **Rule "Price valid at ride start" doesn't give A's free period or other fees.** Any free period under 40 minutes gives 5.00 PLN. The test uses 10 minutes, as in the other examples. Status: please confirm or give the values.

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| Pricing reacts to Rentals' *Rental Session Ended* (Rentals → Pricing) | Rentals → Settlement only; Settlement calls Pricing | Yes: "Pricing reacting to Rental Session Ended instead of being called by Settlement". |
| Pricing publishes *Ride Priced* to Settlement | Pricing is an OHS that Settlement queries ("price for session") | Yes, the same alternative (the orchestration direction is flipped). |
| Pricing queries Rentals for *Return Charges* (drop-zone / unauthorized-zone charges computed by Rentals) | Pricing calculates drop zone / unauthorized zone fees itself using the City zone map (City → Pricing, CF) | No. The canvas has no City relationship, and zone-fee logic moves to Rentals. |
| Term "Price List" | Term "tariff" ("tariff valid at session start") | No. The "Billing" alternative covers the context name only. Same meaning, different word. |
| Fees: first / second / third hour fee + subsequent hourly fee | "tiered hourly fees", "hourly fee" | Compatible, the canvas is more precise. |
| Max-rental-time penalty: open question | Pricing adds the penalty | No. The canvas leaves it open. |
| Back office *Publish Price List*, rider app *Estimate Fee* | Not shown (the map shows contexts only) | n/a, not a contradiction. |

The reference map and CML were **not** changed.

## Files
```
docs/bounded-contexts/velocity/pricing/
  canvas.md
  plan.md
src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/
  package-info.java
  api/events/RidePriced.java
  api/queries/EstimateFeeQuery.java
  api/queries/EstimateFeeQueryHandler.java
  api/queries/FeeEstimate.java
  api/usecases/PriceRideUseCase.java
  api/usecases/PublishPriceList.java
  api/usecases/PublishPriceListUseCase.java
  api/usecases/RentalSessionEnded.java
  domain/pricelist/BillingPeriod.java
  domain/pricelist/FreePeriod.java
  domain/pricelist/NoPriceListValidAt.java
  domain/pricelist/PriceList.java
  domain/pricelist/PriceListEvent.java
  domain/pricelist/PriceListId.java
  domain/pricelist/PriceListNotValidForRide.java
  domain/pricelist/PriceListRepository.java
  domain/pricelist/RentalSessionId.java
  domain/pricelist/ReturnCharges.java
  domain/pricelist/ReturnChargesProvider.java
  domain/pricelist/Ride.java
  domain/pricelist/RiderId.java
  domain/pricelist/TotalFee.java
  infra/adapters/primary/FeeEstimateRestApi.java
  infra/adapters/primary/PriceListRestApi.java
  infra/adapters/primary/PublishPriceListRequest.java
  infra/adapters/secondary/InMemoryPriceListRepository.java
  infra/adapters/secondary/IntegrationEventsPublisher.java
  infra/adapters/secondary/rentalsctxacl/package-info.java
  infra/adapters/secondary/rentalsctxacl/RentalsReturnChargesAdapter.java
  infra/config/PricingContextConfig.java
src/test/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/pricelist/
  PriceListTest.java
```

## Stubs and follow-ups
- `RentalsReturnChargesAdapter` is a stub (`UnsupportedOperationException("implement this")`): Rentals offers no "Get Return Charges" query in code yet. Until it does, `PriceRideUseCase` fails at runtime.
- The listener for Rentals' *Rental Session Ended* (`infra/adapters/primary/rentalsctxacl`) is missing: Rentals publishes no such event yet (its `BikeRentalEvent` has only `BikeRented` / `BikeReturned`, and Rentals has no `api/events`).
- Settlement doesn't exist in code. `api/events/RidePriced` is published, but nobody listens yet.
- **Ubiquitous language gap**: "ride" is used in the purpose and every rule but isn't defined. The code uses `Ride`. Should it be added to the UL, or should the code say "rental session"?
- **Billing period counting**: the examples can't tell "started hours counted from the end of the free period" (implemented, literal definition) from "started hours counted from ride start, with the free period free". They agree on all four examples but differ for, e.g., a 65 minute ride (5.00 vs 12.00 PLN). Please confirm with a GWT example.
- **Republishing a price list id** overwrites it silently. That could change past prices. Should it be rejected? There's no rule in the canvas.
- Open question from the canvas: does the maximum-rental-time penalty belong to Pricing or Rentals?
- CML (`src/main/cml/VeloCity-*.cml`) and `velocity/velocity-context-map.md` don't match the canvas (see the differences above). They weren't changed. Update them on request.

## Changelog
### 2026-11-01: scaffolded
- Added: the whole `velocity.pricing` module (see Files), `PriceListTest` with the 4 canvas rules, and this plan and a copy of the canvas.
- Changed: nothing outside the new module.
- Removed (confirmed by user): none.
- Proposed removals (not applied): none.
- Triggered by: initial canvas `pricing-canvas.md`.
