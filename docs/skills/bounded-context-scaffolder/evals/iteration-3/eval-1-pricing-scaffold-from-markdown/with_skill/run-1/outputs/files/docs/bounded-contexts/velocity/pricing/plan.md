# Pricing bounded context: implementation plan

- **System / package**: `velocity.pricing` (`pl.wojtyna.dddtrainingv3.velocity.pricing`)
- **Mode**: scaffold
- **Source canvas**: `docs/bounded-contexts/velocity/pricing/canvas.md` (copied on 2026-11-01 from the original file outside the repo docs, with the rules agreed in review added; see Review notes)
- **Last updated**: 2026-11-01
- **Status**: implemented

<!-- Reviewers: edit this plan directly or answer in the conversation, then set Status to "approved" (or say so).
     Invoking the skill again resumes from this file. -->

## Review notes
- 2026-11-01: draft written, awaiting review. No code generated yet. Questions Q1-Q9 below need answers (or an explicit "go with the proposal").
- 2026-11-01: **approved** by the reviewer, with these answers:
  - Copy the canvas to the default docs location: done, `docs/bounded-contexts/velocity/pricing/canvas.md`.
  - "Your suggested defaults for the edge cases are fine": Q1-Q9 resolved with the proposals (see "Questions for the reviewer", now answered). In particular the 65 min boundary GWT (Q3) and the two Q5 defaults are now rules with tests.
  - New rule from the reviewer, **Return charges**: given 10 min free period and first-hour fee 5.00 PLN, when a 45 min ride with a 3.00 PLN drop-zone return charge is priced, then 8.00 PLN. Hand check: 35 min → 1 period → 5.00 + 3.00 = 8.00 ✓. It replaces the suggested 2.00 PLN / 7.00 PLN example from Q4.
  - The four agreed rules were added to the copied canvas under "Business Decisions", each marked with an HTML comment "Added in plan review, 2026-11-01", so canvas, plan, javadoc and tests say the same thing.
  - Last change while implementing: REST controllers are picked up by component scanning (`@RestController`), like CrowdSorcery's `WithdrawRestApi`, instead of `@Bean` methods (D14).

## Canvas summary
Pricing turns a finished ride into a fair, predictable fee: a free period encourages short trips, hourly fees grow tier by tier, and the rider always pays the price list that was valid when the ride started. Classification: **core** domain, **revenue** business model, **custom built**. Domain role: **Execution Model** (worksheet trait, matches the canvas as written): it calculates the total fee for every rental session.

Validation result: Name ("Pricing", matches `BoundedContext Pricing` in both `src/main/cml/VeloCity-*.cml`), Purpose and Ubiquitous Language (5 defined terms) are present. All classification values are on the allowed lists. All four canvas business decisions are strict Given/When/Then examples with concrete values, and I worked each one through by hand against the UL definitions (see "Business rules → aggregates"); none contradicts the definitions. Four more rules were agreed in review (2026-11-01) and added to the canvas.

## Decisions
| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | System | `velocity` | Canvas collaborators (Rentals, Settlement, rider app) are VeloCity's; Pricing is in the VeloCity reference map. |
| D2 | Package | `pricing` | Derived from the canvas name. Package does not exist yet, so scaffold mode. |
| D3 | Modules | 1 module = 1 context | Default. One `package-info.java` with `@BoundedContext(name = "Pricing")` + `@Module`. |
| D4 | ACL towards Rentals (outbound "Get Return Charges") | yes, `infra/adapters/secondary/rentalsctxacl` | Canvas says ACL: yes; also the skill default. Port in Pricing's language (`ReturnChargesProvider`), adapter translates Rentals' api. |
| D5 | Inbound "Rental Session Ended" listener | **not generated**, follow-up | Rentals exists in code but publishes no `rentals.api.events.RentalSessionEnded` yet (only internal domain events `BikeRented`/`BikeReturned`). Listening to Rentals' domain types would break the boundary rule. We create our own `RentalSessionEnded` record + use case now; the `rentalsctxacl` listener comes when Rentals publishes the event in its `api`. |
| D6 | Aggregate boundary | one aggregate `PriceLists` holding every published Price List (proposal, see Q1) | The "Price valid at ride start" rule's *Given* spans two price lists, so the timeline of price lists is the state that must be consistent. Each `PriceList` is immutable once published, so it is a record inside the aggregate. Alternative considered: `PriceList` as its own aggregate and selection in the repository; rejected because the selection rule would then live in infra and not be testable in the domain. |
| D7 | Pricing a ride does not change state | `PriceLists.price(...)` returns `DomainEvents` (`RidePriced`) without mutating; `PriceRideUseCase` loads, calls, publishes, and skips the save | Canvas and reference map describe Pricing as a pure calculator. If a "ride is priced once" invariant is wanted, it would need a GWT (Q6). |
| D8 | Estimate Fee query | `EstimateFeeQueryHandler` loads `PriceLists` from its repository and calls `PriceLists.estimate(...)`; no separate read-model port | The estimate is a calculation, not a projection. A read model would duplicate the fee rules outside the domain. Deviation from the default query shape, recorded here. |
| D9 | Money | `org.joda.money.Money` (already on the classpath, used by CrowdSorcery), currency PLN from the examples | Canvas amounts are in PLN. |
| D10 | REST adapters | thin, implemented: `POST /velocity/pricing/price-lists` (back office) and `GET /velocity/pricing/fee-estimate?minutes=N` (rider app) | Back office and rider app are apps, not contexts. The canvas doesn't describe endpoints, so the paths and bodies are my proposal (Q8). |
| D11 | Time source | `java.time.Clock` injected into `EstimateFeeQueryHandler`, bean `pricingClock` | "Now" for the estimate without Spring in `api`. |
| D12 | Boundary exceptions | none (default) | No request to break the `api`-only rule. `PricingArchitectureTest` has the two plain rules. |
| D13 | ArchUnit | add `com.tngtech.archunit:archunit-junit5:1.5.1` (test scope) to `pom.xml` | Not on the classpath yet; 1.5.1 is the first that reads JDK 27 class files. Done. |
| D14 | REST controller registration | `@RestController` + component scanning; every other bean in `PricingContextConfig` | Same as CrowdSorcery's `WithdrawRestApi`. Registering them as `@Bean` too would create duplicate request mappings. |
| D15 | Edge-case defaults (Q5, Q7) | no price list valid at ride start → `NoPriceListValidAt`; same valid-from → later publication replaces; past valid-from accepted; estimate uses the price list valid now, no return charges | Reviewer accepted the proposals, 2026-11-01. The first two are rules with tests; "past valid-from accepted" is simply the absence of a check. |

## Mapping: canvas → code
Paths relative to `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/`.

### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Rentals | Event | Rental Session Ended (session id, rider id, start, end) | `api/usecases/RentalSessionEnded` (our record: `rentalSessionId`, `riderId`, `startedAt`, `endedAt`) + `api/usecases/PriceRideUseCase` (`@PrimaryPort`). Listener `infra/adapters/primary/rentalsctxacl/RentalsEventsListener`: **follow-up** (D5). |
| Back office | Command | Publish Price List | `api/usecases/PublishPriceList` (record: `validFrom`, `freePeriodMinutes`, `firstHourFee`, `secondHourFee`, `thirdHourFee`, `subsequentHourlyFee`, `currency`) + `api/usecases/PublishPriceListUseCase` (`@PrimaryPort`); REST `infra/adapters/primary/PriceListRestApi` + `PublishPriceListRequest` record. |
| Rider app | Query | Estimate Fee (ride of N minutes) | `api/queries/EstimateFee` (record: `minutes`) + `api/queries/FeeEstimate` (record: `amount`, `currency`) + `api/queries/EstimateFeeQueryHandler` (`@PrimaryPort`); REST `infra/adapters/primary/FeeEstimateRestApi`. |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Ride Priced | Event | Settlement | domain event `PriceListsEvent.RidePriced` → integration record `api/events/RidePriced` (`rentalSessionId`, `riderId`, `totalFee` as `BigDecimal`, `currency`) | `infra/adapters/secondary/IntegrationEventsPublisher` (`@SecondaryAdapter`, `@EventListener`). No ACL: we publish our own language. Settlement doesn't exist in code. |
| Get Return Charges | Query | Rentals | `domain/pricelists/ReturnChargesProvider` (`@SecondaryPort`, `ReturnCharges forSession(RentalSessionId)`) | `infra/adapters/secondary/rentalsctxacl/RentalsReturnChargesAdapter` (`@AntiCorruptionLayer` package). **Stub** throwing `UnsupportedOperationException("implement this")`: Rentals has no return-charges query in its `api`. |

### Ubiquitous language
| Term | Code type | Kind |
|---|---|---|
| Price List | `domain/pricelists/PriceList` | `@Entity` record inside the aggregate: immutable once published, identified by `validFrom`; holds `FreePeriod` and the four fees |
| (collection of Price Lists) | `domain/pricelists/PriceLists` | aggregate root, name not in the UL, agreed in review (Q1) |
| Free Period | `domain/pricelists/FreePeriod` | value object (record, `Duration`, non-negative) |
| Billing Period | `domain/pricelists/BillingPeriod` | value object (record, 1-based ordinal); `PriceList.feeFor(BillingPeriod)` picks the tier |
| Total Fee | `domain/pricelists/TotalFee` | value object (record, `Money`) = sum of billing period fees + return charges |
| Return Charges | `domain/pricelists/ReturnCharges` | value object (record, `Money`, `none(currency)` factory) |
| Ride (used in rules and "Ride Priced", not defined) | `domain/pricelists/Ride` | value object (record: `RentalSessionId`, `RiderId`, `startedAt`, `endedAt`) (Q2) |
| Rental Session (id) | `domain/pricelists/RentalSessionId` | identifier (record); ride = rental session (Q2, agreed) |
| Rider (id) | `domain/pricelists/RiderId` | identifier (record) |

Other domain types: `PriceListsId` (record, identity of the aggregate), sealed `PriceListsEvent` (`PriceListPublished`, `RidePriced`), `PriceListsRepository` (`@Repository @SecondaryPort`), `NoPriceListValidAt` (exception, Q5).

### Business rules → aggregates
All arithmetic below uses the Billing Period definition "one started hour of a ride after the free period": periods = ceil((ride duration − free period) / 60 min), 0 if the ride is within the free period. Period 1 = first-hour fee, 2 = second-hour fee, 3 = third-hour fee, 4+ = subsequent hourly fee.

| Rule (GWT) | Hand check | Invariant | Aggregate | Test |
|---|---|---|---|---|
| **Free ride**: given a price list with a 10 min free period, when a ride of 8 min is priced, then the total fee is 0.00 PLN | 8 ≤ 10 → 0 periods → 0.00 ✓ | no fee inside the free period | `PriceLists` | `PriceListsTest#freeRide` |
| **First hour**: given 10 min free period and first-hour fee 5.00 PLN, when a ride of 45 min is priced, then 5.00 PLN | 45 − 10 = 35 min → 1 period → 5.00 ✓ | each started hour after the free period is one billing period | `PriceLists` | `PriceListsTest#firstHour` |
| **Tiered hours**: given 10 min free, 5.00 / 7.00 / 9.00 PLN for hours 1-3, 12.00 PLN subsequent, when a ride of 3 h 20 min is priced, then 33.00 PLN | 200 − 10 = 190 min → 4 periods → 5 + 7 + 9 + 12 = 33.00 ✓ | tier per billing period, subsequent fee from the 4th | `PriceLists` | `PriceListsTest#tieredHours` |
| **Price valid at ride start**: given A (first-hour 5.00 PLN) valid from 08:00 and B (first-hour 6.00 PLN) valid from 09:00, when a ride 08:50-09:30 is priced, then 5.00 PLN | ride starts under A; 40 min − free period → 1 period → 5.00 ✓ (holds for any free period < 40 min; the test uses 10 min, Q3) | the price list is chosen by ride start, never by ride end | `PriceLists` | `PriceListsTest#priceValidAtRideStart` |
| **Billing hours start after the free period** (review, Q3): given 10 min free, 5.00 / 7.00 PLN for hours 1-2, when a ride of 65 min is priced, then 5.00 PLN | 65 − 10 = 55 min → 1 period → 5.00 ✓ | billing periods are counted after the free period, not from ride start | `PriceLists` | `PriceListsTest#billingHoursStartAfterFreePeriod` |
| **Return charges** (review, from the reviewer): given 10 min free and first-hour fee 5.00 PLN, when a ride of 45 min with a 3.00 PLN drop-zone return charge is priced, then 8.00 PLN | 35 min → 1 period → 5.00 + 3.00 = 8.00 ✓ | Total Fee = billing period fees + return charges | `PriceLists` | `PriceListsTest#returnCharges` |
| **No price list valid at ride start** (review, Q5): given a price list valid from 08:00, when a ride 07:50-08:30 is priced, then pricing is rejected | no price list with valid-from ≤ 07:50 ✓ | a ride is never priced with a price list that started after the ride | `PriceLists` | `PriceListsTest#noPriceListValidAtRideStart` (`NoPriceListValidAt`) |
| **Republished price list replaces the earlier one** (review, Q5): given A (5.00 PLN) valid from 08:00, then B (6.00 PLN) published with valid-from 08:00, when a ride 08:30-09:00 is priced, then 6.00 PLN | 30 − 10 = 20 min → 1 period → B's 6.00 ✓ | one price list per valid-from; the later publication wins | `PriceLists` | `PriceListsTest#republishedPriceListReplacesEarlierOne` |

The tests assert on the `TotalFee` carried by the `RidePriced` event. All but "Return charges" use `ReturnCharges.none(PLN)`, because those examples mention no return charges.

### Rules awaiting GWT
- ~~Return charges in the total fee~~: resolved in review with the reviewer's own example (3.00 PLN → 8.00 PLN), now a rule and a test.
- ~~Billing hours start after the free period~~: resolved in review (Q3), now a rule and a test.
- **Maximum-rental-time penalty** (canvas open question; the reference map says Pricing adds it). Not modelled. Needs the owner decision and a GWT with the penalty amount.
- **A ride is priced only once** (Q6). Not modelled: a duplicate *Rental Session Ended* is priced again and publishes a second *Ride Priced*. Needs a GWT if it should be an invariant.

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| Pricing **reacts** to Rentals' *Rental Session Ended* and publishes *Ride Priced* to Settlement | Rentals → Settlement (*Rental Session Ended*); Settlement **calls** Pricing (`Pricing [U,OHS]->[D,CF] Settlement : PriceCalculation`) | Yes: "Pricing reacting to Rental Session Ended instead of being called by Settlement". |
| Pricing **queries Rentals** for *Return Charges* (drop-zone / unauthorized-zone charges "reported by Rentals") | Pricing calculates drop zone / unauthorized zone fees itself, using the **City** zone map (`City [U]->[D,CF] Pricing : ZoneMap`); no Rentals → Pricing relationship | No. Also the City zone map relationship is absent from the canvas. |
| Maximum-rental-time penalty: open question | "Max rental time is detected by Rentals ...; Pricing adds the penalty" | No (the canvas leaves it open). |
| Term **Price List** | Term **tariff** ("tariff valid at session start", CML responsibility "Tariffs") | No (only "Billing" as a context name is listed). Same meaning, different word. |
| *Rental Session Ended* carries session id, rider id, start, end | carries start, end, location, reason | No. Location/reason not needed if Rentals reports return charges. |
| Inbound back office (Publish Price List) and rider app (Estimate Fee) | not on the map (apps, not contexts) | n/a |

The code follows the canvas. The reference map is not changed (see follow-ups).

## Files
```
pom.xml                                                         (+ archunit-junit5 1.5.1, test scope)
src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/
  package-info.java                                             @BoundedContext(name = "Pricing") + @Module, canvas javadoc
  api/usecases/RentalSessionEnded.java                          record
  api/usecases/PriceRideUseCase.java                            @PrimaryPort
  api/usecases/PublishPriceList.java                            record
  api/usecases/PublishPriceListUseCase.java                     @PrimaryPort
  api/queries/EstimateFee.java                                  record
  api/queries/FeeEstimate.java                                  record
  api/queries/EstimateFeeQueryHandler.java                      @PrimaryPort
  api/events/RidePriced.java                                    record, IntegrationEvent
  domain/pricelists/PriceLists.java                             @AggregateRoot
  domain/pricelists/PriceListsId.java                           record
  domain/pricelists/PriceList.java                              record (immutable entity)
  domain/pricelists/FreePeriod.java                             @ValueObject record
  domain/pricelists/BillingPeriod.java                          @ValueObject record
  domain/pricelists/TotalFee.java                               @ValueObject record
  domain/pricelists/ReturnCharges.java                          @ValueObject record
  domain/pricelists/Ride.java                                   @ValueObject record
  domain/pricelists/RentalSessionId.java                        record
  domain/pricelists/RiderId.java                                record
  domain/pricelists/PriceListsEvent.java                        sealed, PriceListPublished + RidePriced
  domain/pricelists/NoPriceListValidAt.java                     exception
  domain/pricelists/PriceListsRepository.java                   @Repository @SecondaryPort
  domain/pricelists/ReturnChargesProvider.java                  @SecondaryPort
  infra/adapters/primary/PriceListRestApi.java                  @PrimaryAdapter
  infra/adapters/primary/PublishPriceListRequest.java           record
  infra/adapters/primary/FeeEstimateRestApi.java                @PrimaryAdapter
  infra/adapters/secondary/InMemoryPriceListsRepository.java    @SecondaryAdapter
  infra/adapters/secondary/IntegrationEventsPublisher.java      @SecondaryAdapter
  infra/adapters/secondary/rentalsctxacl/package-info.java      @AntiCorruptionLayer
  infra/adapters/secondary/rentalsctxacl/RentalsReturnChargesAdapter.java  stub
  infra/config/PricingContextConfig.java                        @Configuration, beans prefixed "pricing"
src/test/java/pl/wojtyna/dddtrainingv3/velocity/pricing/
  PricingArchitectureTest.java                                  2 ArchUnit rules
  domain/pricelists/PriceListsTest.java                         8 GWT tests
docs/bounded-contexts/velocity/pricing/canvas.md                copied canvas + 4 rules agreed in review
```
Counts: 2 use cases, 1 query, 1 published event, 3 secondary ports (repository, return charges, plus the shared `DomainEventPublisher`), 1 ACL (Rentals, stub), 1 aggregate, 8 GWT tests + 1 architecture test class with 2 rules.

## Questions for the reviewer (answered 2026-11-01: proposals accepted)
- **Q1** Aggregate name. The UL has no word for "all price lists over time". Proposal: `PriceLists`. Alternatives: `PriceListHistory`, or adopt the reference map's "Tariff" for the collection. Which one (or add a term to the canvas)?
- **Q2** Are "ride" and "rental session" synonyms in Pricing? The canvas uses both. Proposal: `Ride` identified by `RentalSessionId`.
- **Q3** Free period in "Price valid at ride start" is not given. Proposal: 10 min (result is 5.00 PLN for any free period < 40 min). And confirm the 65 min boundary example (billing hours start after the free period).
- **Q4** Confirm the return-charges GWT (45 min + 2.00 PLN → 7.00 PLN) so it becomes a test. *Answer: superseded by the reviewer's own example, 45 min + 3.00 PLN drop-zone charge → 8.00 PLN.*
- **Q5** No price list valid at ride start (ride before the first price list): proposal is to reject with `NoPriceListValidAt`. Also: publishing a price list with the same valid-from as an existing one, or one valid from the past: reject, replace, or allow? None of these is in the canvas, so none is modelled as a rule. Proposal until you give a GWT: a later publication with the same valid-from replaces the earlier one, and past valid-from dates are accepted. Please decide.
- **Q6** Should a ride be priced only once (duplicate *Rental Session Ended*)? Not modelled without a GWT.
- **Q7** Estimate Fee: priced with the price list valid **now** and without return charges. OK?
- **Q8** REST paths and bodies in D10 are a proposal. OK, or should they be stubs?
- **Q9** The assumption "Return charges are always known by the time the session ends" suggests Rentals could put them on *Rental Session Ended* instead of Pricing querying back. Keep the query (canvas) or move the data to the event? *Answer: keep the query (proposal accepted).*

## Stubs and follow-ups
- `RentalsReturnChargesAdapter` is a stub: Rentals has no return-charges query in its `api` yet.
- `RentalsEventsListener` (primary ACL for *Rental Session Ended*) is not generated: Rentals publishes no such `api` event yet (D5).
- Settlement doesn't exist in code; nobody consumes `api/events/RidePriced` yet.
- CML (`src/main/cml/VeloCity-Context-Map.cml`) and `velocity/velocity-context-map.md` differ from the canvas (see table). The VeloCity `CLAUDE.md` asks to keep them in sync; not changed unasked, because the map is a workshop reference.
- Canvas open question: does the maximum-rental-time penalty belong to Pricing or Rentals?

## Changelog
### 2026-11-01: plan drafted
- Added: this plan (scaffold mode). No code yet.
- Triggered by: initial canvas.

### 2026-11-01: scaffolded
- Added: the `velocity.pricing` module (31 files under `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/`, see "Files"), `PriceListsTest` (8 GWT tests), `PricingArchitectureTest` (2 rules), `archunit-junit5:1.5.1` (test scope) in `pom.xml`, and `docs/bounded-contexts/velocity/pricing/canvas.md`.
- Changed: canvas copy gained four rules agreed in review (billing hours after free period, return charges, no price list valid at ride start, republished price list replaces the earlier one).
- Verified: `./mvnw compile`, `./mvnw test -Dtest='PriceListsTest,PricingArchitectureTest'` (10 tests), `./mvnw test` (15 tests, Spring context starts). All green.
- Triggered by: plan approval with review answers, 2026-11-01.
