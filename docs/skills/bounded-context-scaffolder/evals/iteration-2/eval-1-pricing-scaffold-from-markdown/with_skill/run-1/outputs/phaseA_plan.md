# Pricing bounded context: implementation plan

- **System / package**: `velocity.pricing`
- **Mode**: scaffold
- **Source canvas**: `.claude/skills/bounded-context-scaffolder/evals/files/pricing-canvas.md` (see question Q1 about moving it to `docs/bounded-contexts/velocity/pricing/canvas.md`)
- **Last updated**: 2026-11-01
- **Status**: draft - awaiting review

<!-- Reviewers: edit this plan directly or answer in the conversation, then set Status to "approved" (or say so).
     Invoking the skill again resumes from this file. -->

## Review notes
- 2026-11-01: draft written, awaiting review. No code generated yet.

## Canvas summary
Pricing turns a finished ride into a fair, predictable fee: a free period for short trips, progressively more
expensive hours for long trips, and always the price list valid when the ride started. It is a **core** domain,
**revenue** business model, **custom built**. Its single domain role is **Execution Model** (worksheet trait: "performs
or tracks a job"; here: calculates the total fee for every rental session).

Validation: Name, Purpose and Ubiquitous Language (5 defined terms) are present. All classification values and the
domain role are on the allowed lists. All four business decisions are strict Given/When/Then with concrete values.

### Consistency check of the GWT examples (worked by hand)
Definitions used: Billing Period = one *started* hour of a ride *after* the free period.

| Example | Ride | Minutes after free period | Started billing periods | Fee |
|---|---|---|---|---|
| Free ride | 8 min | 0 (8 < 10) | 0 | 0.00 PLN, matches |
| First hour | 45 min | 35 | 1 | 5.00 PLN, matches |
| Tiered hours | 3 h 20 min = 200 min | 190 | 4 (60+60+60+10) | 5 + 7 + 9 + 12 = 33.00 PLN, matches |
| Price valid at ride start | 08:50 to 09:30 = 40 min, list A | 30 (with a 10 min free period) | 1 | 5.00 PLN (list A, not 6.00 from B), matches |

No contradictions found. Two notes:
- The tiered example gives 4 periods under both readings of "billing period" (counted after the free period: 190 min;
  counted from ride start: 200 min). The examples don't distinguish the readings, so the code follows the **definition**
  ("after the free period"). See Q3 for a confirming example.
- The "Price valid at ride start" example doesn't state list A's free period. 5.00 PLN holds for any free period
  shorter than 40 minutes, so the test fixture will use 10 minutes (the value from the other examples). This doesn't
  change the rule.

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| System | `velocity` | Canvas is a VeloCity context; "Pricing" is a `BoundedContext` in `VeloCity-*.cml`. |
| Package | `pl.wojtyna.dddtrainingv3.velocity.pricing` | Derived from the name. Package doesn't exist yet, so scaffold mode. |
| `@BoundedContext` name | `"Pricing"` | Canvas name equals the CML name. |
| Modules | 1 module = 1 context | Default; nothing in the canvas asks for more. |
| ACL towards Rentals (Get Return Charges) | yes, `infra/adapters/secondary/rentalsctxacl` | Canvas says "ACL? yes", and it's the skill default. Rentals' language (drop zone, unauthorized zone, sessions) stays out of Pricing's model. |
| Ride Priced to Settlement | published integration event `api/events/RidePriced` (primitives only), separate from the domain event | Published language; Settlement doesn't exist in code, so nobody consumes it yet. |
| Rental Session Ended from Rentals | use case `PriceRideUseCase` consuming our own command record `PriceRide`; **no listener yet** | Rentals exists in code but has no `RentalSessionEnded` event (only `BikeRented`, `BikeReturned`). Inventing Rentals' published type would leak a guess into another context. The listener in `infra/adapters/primary/rentalsctxacl` is a follow-up. |
| Money | `org.joda.money.Money` in PLN | Canvas amounts are in PLN; `joda-money` is on the classpath and CrowdSorcery uses it. |
| Aggregate | `PriceList` (aggregate root, immutable after publishing) | See "Business rules → aggregates". |
| Choosing the price list | first-class collection `PriceLists` (`@ValueObject`) with `validAt(Instant)`; the repository returns it | Rule 4 compares price lists with each other, which a single `PriceList` can't decide alone. Keeping the choice in the domain (not in a repository query) keeps the rule testable and out of infra. Same pattern as CrowdSorcery's `Deposits`. |
| Who emits Ride Priced | `PriceList.priceRide(...)` returns `DomainEvents` with `PriceListEvent.RidePriced` | Follows the convention "aggregate methods return `DomainEvents`"; the use case publishes them. Pricing stays a pure calculator: pricing a ride changes no state. |
| Publish Price List events | none | The canvas has no outbound message for it and no consumer; the use case creates and saves the `PriceList`. |
| Estimate Fee query | handler uses `PriceListRepository` + `Clock`, prices with the list valid *now*, **without** return charges | An estimate is a calculation, not a stored view, so a read-model port would only duplicate the domain. Return charges depend on where the ride ends, which isn't known for an estimate. Both choices are assumptions; see Q4. |
| REST adapters | none | The canvas names Back office and Rider app as collaborators but doesn't describe an API. Use cases and query handler are the primary ports; endpoints are a follow-up. |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Rentals | Event | Rental Session Ended (session id, rider id, start, end) | `api/usecases/PriceRide` (record: `rentalSessionId`, `riderId`, `startedAt`, `endedAt`) + `api/usecases/PriceRideUseCase` (`@PrimaryPort`). Listener in `infra/adapters/primary/rentalsctxacl` = follow-up (Rentals has no such event yet). |
| Back office | Command | Publish Price List | `api/usecases/PublishPriceList` (record: `priceListId`, `validFrom`, `freePeriodMinutes`, `firstHourFee`, `secondHourFee`, `thirdHourFee`, `subsequentHourlyFee`) + `api/usecases/PublishPriceListUseCase` |
| Rider app | Query | Estimate Fee (ride of N minutes) | `api/queries/EstimateFee` (record: `rideMinutes`) + `api/queries/FeeEstimate` (record: `amount`, `currency`, `priceListId`) + `api/queries/EstimateFeeQueryHandler` (`@PrimaryPort`) |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Ride Priced | Event | Settlement | domain event `domain/pricelist/PriceListEvent.RidePriced` | `infra/adapters/secondary/IntegrationEventsPublisher` publishes `api/events/RidePriced` (`rentalSessionId`, `riderId`, `totalFeeAmount`, `currency`, `priceListId`) |
| Get Return Charges | Query | Rentals | `domain/pricelist/ReturnChargesProvider` (`@SecondaryPort`): `ReturnCharges returnChargesFor(RentalSessionId)` | `infra/adapters/secondary/rentalsctxacl/RentalsReturnChargesAdapter` (`@AntiCorruptionLayer` package), **stub**: Rentals has no return-charges query in code |

### Ubiquitous language
| Term | Code type | Kind |
|---|---|---|
| Price List | `PriceList` (+ `PriceListId`) | aggregate root (+ identifier record) |
| Free Period | `FreePeriod` (wraps a `Duration`, not negative) | value object record |
| Billing Period | `BillingPeriod` (1-based ordinal; `PriceList` knows the fee for each ordinal: 1st, 2nd, 3rd, then subsequent) | value object record |
| Total Fee | `TotalFee` (wraps `Money`, PLN, not negative) | value object record |
| Return Charges | `ReturnCharges` (wraps `Money`, `none()` = 0.00 PLN) | value object record |
| (from messages) rental session id, rider id | `RentalSessionId`, `RiderId` | identifier records |
| (from messages) ride | `Ride` (`startedAt`, `endedAt`; duration derived) | value object record |
| Ride Priced | `PriceListEvent.RidePriced` / `api.events.RidePriced` | domain event / integration event |

### Business rules → aggregates
Reasoning: rules 1 to 3 have the Given "a price list with ..." and the When "a ride is priced"; the state they need is one
price list's fees and free period, so `PriceList` is the consistency boundary. Rule 4's Given is *two* price lists with
validity moments; it needs the set of price lists, which `PriceLists` holds. No rule needs anything mutable, so
`PriceList` is immutable after it is published.

| Rule (GWT) | Invariant | Aggregate | Test |
|---|---|---|---|
| **Free ride**: given a price list with a 10 min free period, when a ride of 8 min is priced, then the total fee is 0.00 PLN | No billing period starts inside the free period | `PriceList` | `PriceListTest#freeRide` |
| **First hour**: given a 10 min free period and first-hour fee 5.00 PLN, when a ride of 45 min is priced, then the total fee is 5.00 PLN | The first started hour after the free period costs the first-hour fee | `PriceList` | `PriceListTest#firstHour` |
| **Tiered hours**: given a 10 min free period, fees 5.00 / 7.00 / 9.00 / 12.00 PLN, when a ride of 3 h 20 min is priced, then the total fee is 33.00 PLN | Each started billing period costs its tier's fee; periods after the third cost the subsequent hourly fee | `PriceList` | `PriceListTest#tieredHours` |
| **Price valid at ride start**: given price list A (first-hour fee 5.00 PLN) valid from 08:00 and B (6.00 PLN) valid from 09:00, when a ride from 08:50 to 09:30 is priced, then the total fee is 5.00 PLN | The price list used is the latest one whose valid-from is not after the ride start | `PriceLists` (selection) + `PriceList` (fee) | `PriceListsTest#priceValidAtRideStart` |

Also implemented from the **Total Fee** definition (undisputed, but no GWT example): total fee = sum of billing period
fees + return charges. The four tests use `ReturnCharges.none()`. See Q2 for a suggested example.

The tests check `TotalFee` through the `RidePriced` event returned by `PriceList.priceRide(...)`, using
`DomainEvents.hasAtLeastOneEventMatching(...)`.

### Rules awaiting GWT
None of the canvas rules is blocked. These are boundary cases that the definitions leave open; the code will follow
the reading marked "default" unless you say otherwise, and won't add tests for them until you confirm:
- **Q2. Return charges in the total fee** (from the Total Fee definition). Suggested GWT: given a price list with a 10 min free period and first-hour fee 5.00 PLN, and return charges of 3.00 PLN, when a ride of 45 min is priced, then the total fee is 8.00 PLN. Status: awaiting confirmation.
- **Q3. Billing periods start after the free period** (from the Billing Period definition). Suggested GWT: given a 10 min free period, first-hour fee 5.00 PLN and second-hour fee 7.00 PLN, when a ride of 65 min is priced, then the total fee is 5.00 PLN (default, "after the free period": 55 min = 1 started hour). The other reading, "hours counted from ride start", gives 2 started hours = 12.00 PLN. Status: awaiting confirmation.
- **Q5. Ride exactly as long as the free period.** Suggested GWT: given a 10 min free period, when a ride of exactly 10 min is priced, then the total fee is 0.00 PLN (default: the free period is inclusive). Status: awaiting confirmation.
- **Q6. Ride ending exactly on an hour boundary.** Suggested GWT: given a 10 min free period, first-hour fee 5.00 PLN and second-hour fee 7.00 PLN, when a ride of exactly 70 min is priced, then the total fee is 5.00 PLN (default: 60 min after the free period = 1 started hour, the second hour hasn't started). Status: awaiting confirmation.
- **Q7. Ride starting exactly when a new price list becomes valid.** Suggested GWT: given price list A valid from 08:00 and B valid from 09:00, when a ride starting at 09:00 is priced, then price list B is used (default: valid-from is inclusive). Status: awaiting confirmation.
- **Q8. No price list valid at ride start** (for example the ride starts before the first list's valid-from). Not in the canvas. Default: pricing is rejected with `NoPriceListValidAt`. Please confirm or give the expected outcome.

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| Pricing reacts to Rentals' *Rental Session Ended* and publishes *Ride Priced* to Settlement | Rentals sends *Rental Session Ended* to Settlement, which *calls* Pricing (`Pricing [U,OHS]->[D,CF] Settlement : PriceCalculation`) | **Yes**: "Pricing reacting to Rental Session Ended instead of being called by Settlement". |
| Pricing queries Rentals for *Return Charges* (drop-zone / unauthorized-zone charges reported by Rentals), so Rentals is upstream of Pricing | No Rentals to Pricing relationship. Pricing itself owns "drop zone / unauthorized zone fees", using the City's zone map | **No**. Responsibility for zone fees moves from Pricing to Rentals. |
| No relationship with the City | `City [U]->[D,CF] Pricing : ZoneMap` | **No**. Follows from the row above: if Rentals reports return charges, Pricing doesn't need the zone map. |
| Penalty for maximum rental time is an open question | Pricing adds the penalty ("Max rental time is detected by Rentals ... Pricing adds the penalty") | **No**. Not modelled; the canvas's open question is kept. |
| Term **Price List** | Term **tariff** ("tariff valid at session start", CML responsibility "Tariffs") | **No** (the "Billing" alternative is about the context name, not this term). Same concept; code uses the canvas term `PriceList`. |
| Fees: first / second / third hour fee and subsequent hourly fee; **Return Charges** | "tiered hourly fees", "hourly fee", "drop zone fee", "penalty" | Not a conflict in substance; terminology differs. Code uses the canvas terms. |
| Back office publishes price lists; Rider app estimates fees | Not shown (the map only shows context-to-context relationships) | n/a, an addition, not a conflict. |

The reference map, CML and `velocity-context-map.md` are **not** changed by this plan.

## Files
```
docs/bounded-contexts/velocity/pricing/
  plan.md                                         this file
src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/
  package-info.java                               @BoundedContext(name = "Pricing") + @Module, canvas javadoc
  api/usecases/
    PriceRide.java                                record (our form of "Rental Session Ended")
    PriceRideUseCase.java                         @PrimaryPort: load PriceLists, ask return charges, priceRide, publish
    PublishPriceList.java                         record
    PublishPriceListUseCase.java                  @PrimaryPort: build PriceList, save
  api/queries/
    EstimateFee.java                              record
    FeeEstimate.java                              record
    EstimateFeeQueryHandler.java                  @PrimaryPort
  api/events/
    RidePriced.java                               integration event record (primitives), implements IntegrationEvent
  domain/pricelist/
    PriceList.java                                @AggregateRoot
    PriceListId.java                              @ValueObject record
    PriceLists.java                               @ValueObject first-class collection, validAt(Instant)
    FreePeriod.java                               @ValueObject record
    BillingPeriod.java                            @ValueObject record
    TotalFee.java                                 @ValueObject record
    ReturnCharges.java                            @ValueObject record
    Ride.java                                     @ValueObject record
    RentalSessionId.java                          @ValueObject record
    RiderId.java                                  @ValueObject record
    PriceListEvent.java                           sealed, extends DomainEvent; record RidePriced
    NoPriceListValidAt.java                       exception (Q8 default)
    PriceListRepository.java                      @Repository @SecondaryPort: save(PriceList), all(): PriceLists
    ReturnChargesProvider.java                    @SecondaryPort (our language for "Get Return Charges")
  infra/adapters/secondary/
    InMemoryPriceListRepository.java              @SecondaryAdapter, implemented
    IntegrationEventsPublisher.java               @SecondaryAdapter, PriceListEvent.RidePriced -> api.events.RidePriced
  infra/adapters/secondary/rentalsctxacl/
    package-info.java                             @AntiCorruptionLayer
    RentalsReturnChargesAdapter.java              @SecondaryAdapter, stub ("Get Return Charges" -> Rentals)
  infra/config/
    PricingContextConfig.java                     @Configuration; beans pricingPriceListRepository, pricingReturnChargesProvider,
                                                  pricingPriceRideUseCase, pricingPublishPriceListUseCase,
                                                  pricingEstimateFeeQueryHandler, pricingIntegrationEventsPublisher, pricingClock
src/test/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/pricelist/
  PriceListTest.java                              freeRide, firstHour, tieredHours
  PriceListsTest.java                             priceValidAtRideStart
```

## Stubs and follow-ups
- `RentalsReturnChargesAdapter` is a stub (`UnsupportedOperationException("implement this")`): Rentals has no
  return-charges query in code. Until it exists, `PriceRideUseCase` fails at runtime for real rides; the domain tests
  don't depend on it.
- Listener for Rentals' *Rental Session Ended* (`infra/adapters/primary/rentalsctxacl`): to add when Rentals publishes
  that event in `rentals/api/events`.
- REST endpoints for Back office (Publish Price List) and Rider app (Estimate Fee): not described by the canvas.
- Settlement doesn't exist in code, so nobody consumes `api.events.RidePriced` yet.
- CML (`src/main/cml/VeloCity-*.cml`) and `velocity/velocity-context-map.md` would need updating to match the canvas
  (Pricing reacting to Rentals, Rentals upstream of Pricing for return charges, no City relationship). Not done; ask first.
- Open question from the canvas, kept as is: "Does the maximum-rental-time penalty belong to Pricing or Rentals?"
- Canvas assumption, kept as is: "Return charges are always known by the time the session ends."

## Questions for the reviewer
- **Q1.** The canvas lives under the skill's `evals/files/`. Should I copy it to the default location
  `docs/bounded-contexts/velocity/pricing/canvas.md` and point the plan and javadoc there?
- **Q2, Q3, Q5 to Q8**: see "Rules awaiting GWT".
- **Q4.** Estimate Fee: price with the list valid at the time of the query, and without return charges? Or should
  the rider app pass a planned start time?
- Are you OK with the differences from the reference context map (zone fees moving to Rentals, no City
  relationship)? They are built as the canvas says.

## Changelog
### 2026-11-01: plan drafted (no code yet)
- Added: this plan.
- Triggered by: initial canvas `pricing-canvas.md`.
