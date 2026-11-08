# Pricing bounded context: implementation plan

- **System / package**: `velocity.pricing`
- **Mode**: scaffold
- **Source canvas**: `docs/bounded-contexts/velocity/pricing/canvas.md`
- **Last updated**: 2026-11-08
- **Status**: implemented

## Review notes
- 2026-11-08: approved via docs/plans/pricing-manager-publishes-tariff.md, docs/plans/finance-prices-ride-by-tiered-tariff.md, docs/plans/finance-prices-free-floating-return-by-zone.md, docs/plans/finance-penalizes-overdue-rental.md (autonomous run, 2026-11-08). Structural decisions and the TDD-loop choice (off, D1, the trainer's explicit autonomy request) come from those plans; the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`.

## Canvas summary
Pricing turns a finished rental session into a fair, explainable total fee (free period, tiered billing hours,
return fee by zone, penalty) and keeps only tariffs. Core domain, revenue, custom built; role: Execution Model.

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| Modules | 1 module = 1 context | default |
| ACL towards the City (external) | yes: port `ZoneMap`, adapter in `infra/adapters/secondary/cityacl` | CML `City [U]->[D,ACL] Pricing`; the City has no REST resource in this app, so the adapter reads a simulated City zone map (plan `finance-prices-free-floating-return-by-zone` D3, D6) |
| Inbound from Settlement | OHS/PL `GET /pricing/session-price` | integration-contracts.md; Settlement's ACL is built with `finance-charges-finished-ride` |
| Boundary exceptions | none | |
| Domain layer | delegated to `ddd-tactical-building-blocks` (always) | |
| TDD checkpoint loop | off (trainer opted out, 2026-11-08, D1 of every Pricing plan) | autonomous run; still test-first, one example per cycle, red before green |
| Aggregate style | state-based | D2 of the plans |
| Launch tariff | seeded at start-up in `PricingContextConfig` via `TariffSchedule.startingWith(LaunchTariff.tariff())`, id `LAUNCH`, valid from 2026-01-01T00:00:00Z | plan `pricing-manager-publishes-tariff` D5 |
| Persistence | `InMemoryTariffScheduleRepository` (ConcurrentHashMap, `Version` optimistic locking, memento `TariffSchedule.Snapshot`) | D4 |
| Price for Session is a query | no domain or integration event for Ride Priced | plan `finance-prices-ride-by-tiered-tariff` D5 |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Settlement | Query | Price for Session | `api/queries/PriceForSession`, `SessionPrice`, `PriceForSessionQueryHandler`; `GET /pricing/session-price` in `PricingController` |
| Pricing manager | Command | Publish Tariff | `api/usecases/PublishTariff`, `PublishTariffUseCase`; `POST /pricing/tariffs` (`PublishTariffRequest`) |
| Pricing manager | Query | Tariffs | `api/queries/TariffsQuery`, `TariffView`, `TariffsQueryHandler`; `GET /pricing/tariffs` |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Zone at position (at a moment) | Query | City (external) | `domain/zone/ZoneMap` | `infra/adapters/secondary/cityacl/CityZoneMapAdapter` over `SimulatedCityZoneMap` |

### Ubiquitous language
| Term | Code type | Where it appears |
|---|---|---|
| Tariff | `domain/tariff/Tariff` (value object), `TariffId` | domain, `TariffView` |
| Valid from | `Tariff.validFrom` | domain, api |
| Launch tariff | `domain/tariff/LaunchTariff` | domain, config |
| Free period | `Tariff.freePeriod` | domain (`freeMinutes` on the wire) |
| Billing hour, hourly fee | `FeeCalculator` (billing hours), `Tariff.hourlyFeeOf`, `SessionFee.hourlyFees` | domain, `SessionPrice.billingHours`/`hourlyFees` |
| Ending | `domain/fee/Ending` | domain, `PriceForSession.ending` |
| Drop zone, unauthorized zone | `domain/zone/Zone` | domain |
| Zone map | `domain/zone/ZoneMap` | port |
| Return fee | `ReturnFeeKind`, `SessionFee.returnFee` | domain, `SessionPrice` |
| Penalty | `Tariff.penalty`, `SessionFee.penalty` | domain, api |
| Total fee | `SessionFee.total()` | domain, `SessionPrice.total` |
| (tariff schedule) | `domain/tariff/TariffSchedule` aggregate | domain |

### Business rules → delegated to `ddd-tactical-building-blocks`
| Rule (GWT) | Status | Building block | Test |
|---|---|---|---|
| Complete tariff valid from a future moment is published (publish E1.1) | green | `TariffSchedule` aggregate | `TariffScheduleTest#completeTariffValidFromFutureMomentIsPublished` |
| Valid from "now" is rejected (publish E3.2) | green | `TariffSchedule` | `TariffScheduleTest#tariffValidFromNowIsRejected` |
| Retroactive tariff rejected, ride keeps launch tariff (publish E3.1) | green | `TariffSchedule` | `TariffScheduleTest#retroactiveTariffIsRejectedAndStartedRideKeepsLaunchTariff` |
| Two tariffs can't share a valid from (publish E5.1) | green | `TariffSchedule` | `TariffScheduleTest#secondTariffWithSameValidFromIsRejected` |
| A tariff states every price (publish E4.1) | green | `Tariff` value object | `TariffTest#tariffWithoutPenaltyIsRejected` |
| Fees can't be negative (publish E4.2) | green | `Tariff` value object | `TariffTest#tariffWithNegativeDropZoneFeeIsRejected` |
| Tariff valid at session start: before / after a new tariff (publish E2.1, E2.2) | green | `TariffSchedule.tariffValidAt` | `TariffScheduleTest#rideStartedBeforeNewTariffKeepsLaunchTariff`, `#rideStartedAfterNewTariffUsesIt` |
| A tariff id is unique (spec Data) | green | `TariffSchedule` | `TariffScheduleTest#secondTariffWithSameIdIsRejected` |
| Free period, started billing hours, tiered fees (tiered E1.1-E3.2) | green | `FeeCalculator` domain service | `FeeCalculatorTest` (7 tests) |
| Tariff valid at session start; station return and lock failure pay no return fee (tiered E4.1-E5.2) | green | `FeeCalculator` | `FeeCalculatorTest` (4 tests) |
| Return fee by zone, border, zone map at the end, lock failure, tariff at start (zone E1.1-E4.2) | green | `FeeCalculator` (+ `CityZoneMapAdapter` for geometry) | `ReturnFeeTest` (9 tests), `CityZoneMapAdapterTest` |
| Penalty for maximum rental time, no return fee, 12 h at a station, tariff at start (penalty E1.1-E4.1) | green | `FeeCalculator` | `PenaltyTest` (5 tests) |

### Rules awaiting GWT
- None. Spec Q4 (unauthorized zone fee higher than drop zone fee?) is open and not modelled.

## Deferred until tactical blocks are built
- All generated in 6b: `PublishTariffUseCase`, `TariffsQueryHandler`, `PriceForSessionQueryHandler`, `PricingController`, `InMemoryTariffScheduleRepository`, `PricingContextConfig`.

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| City → Pricing through an ACL | `[D,ACL]` (map updated during stage 1) | n/a, same |

## Files
```
src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/
  package-info.java
  api/usecases/PublishTariff.java, PublishTariffUseCase.java
  api/queries/PriceForSession.java, SessionPrice.java, PriceForSessionQueryHandler.java,
              TariffsQuery.java, TariffView.java, TariffsQueryHandler.java
  domain/tariff/Tariff.java, TariffId.java, TariffSchedule.java, TariffScheduleId.java, TariffScheduleRepository.java,
                TariffEvent.java, PricingManagerId.java, LaunchTariff.java, FluentLanguage.java,
                ValidFromNotInFutureException.java, DuplicateValidFromException.java, DuplicateTariffIdException.java,
                IncompleteTariffException.java, NegativeFeeException.java, NoTariffValidException.java
  domain/fee/FeeCalculator.java, Session.java, Ending.java, SessionFee.java, ReturnFeeKind.java
  domain/zone/ZoneMap.java, Zone.java, Position.java
  infra/adapters/primary/PricingController.java, PublishTariffRequest.java
  infra/adapters/secondary/InMemoryTariffScheduleRepository.java
  infra/adapters/secondary/cityacl/package-info.java, SimulatedCityZoneMap.java, CityZoneMapAdapter.java
  infra/config/PricingContextConfig.java
src/test/java/pl/wojtyna/dddtrainingv3/velocity/pricing/
  PricingArchitectureTest.java, PricingDriver.java
  PricingManagerPublishesTariffSpecTest.java, FinancePricesRideByTieredTariffSpecTest.java,
  FinancePricesFreeFloatingReturnByZoneSpecTest.java, FinancePenalizesOverdueRentalSpecTest.java
  domain/tariff/TariffScheduleTest.java, TariffTest.java, TariffFixtures.java
  domain/fee/FeeCalculatorTest.java, ReturnFeeTest.java, PenaltyTest.java
  infra/adapters/secondary/InMemoryTariffScheduleRepositoryTest.java
  infra/adapters/secondary/cityacl/CityZoneMapAdapterTest.java
```

## Stubs and follow-ups
- No stubs. The City zone map is simulated (`SimulatedCityZoneMap`) until a real feed exists (canvas Assumptions).
- Settlement's `pricingctxacl` (asking `GET /pricing/session-price`) belongs to `finance-charges-finished-ride`.
- Open questions from the canvas: tariff retraction; station surroundings as drop zones; spec Q4.

## Changelog
### 2026-11-08: scaffolded and implemented
- Added: the whole module (files above), canvas UL terms and tagged business decisions from the four Pricing plans.
- Triggered by: intents `pricing-manager-publishes-tariff`, `finance-prices-ride-by-tiered-tariff`,
  `finance-prices-free-floating-return-by-zone`, `finance-penalizes-overdue-rental` (docs/plans/<intent>.md).
- TDD checkpoint loop off (D1); every cycle below implemented by Claude, red observed before green unless noted.

Per-cycle TDD log (scenario → block → test → red → green):

| # | Scenario | Block | Test | Red seen | Green |
|---|---|---|---|---|---|
| 1 | publish E1.1 | `TariffSchedule` | `TariffScheduleTest#completeTariffValidFromFutureMomentIsPublished` | yes (no event) | yes |
| 2 | publish E3.2 | `TariffSchedule` | `#tariffValidFromNowIsRejected` | yes (no exception) | yes (fake: equality) |
| 3 | publish E3.1 | `TariffSchedule` | `#retroactiveTariffIsRejectedAndStartedRideKeepsLaunchTariff` | yes | yes (`!isAfter`) |
| 4 | publish E5.1 | `TariffSchedule` | `#secondTariffWithSameValidFromIsRejected` | yes | yes |
| 5 | publish E4.1 | `Tariff` | `TariffTest#tariffWithoutPenaltyIsRejected` | yes | yes |
| 6 | publish E4.2 | `Tariff` | `TariffTest#tariffWithNegativeDropZoneFeeIsRejected` | yes | yes |
| 7 | publish E2.1 | `TariffSchedule` | `#rideStartedBeforeNewTariffKeepsLaunchTariff` | yes (null) | yes (fake: first tariff) |
| 8 | publish E2.2 | `TariffSchedule` | `#rideStartedAfterNewTariffUsesIt` | yes | yes (latest valid-from not after) |
| 9 | tiered E2.1 | `FeeCalculator` | `FeeCalculatorTest#oneSecondAfterFreePeriodStartsFirstBillingHour` | yes (0 hours) | yes (fake: one condition) |
| 10 | tiered E1.1 | `FeeCalculator` | `#rideOfTwelveMinutesIsFree` | no, passed straight away | kept |
| 11 | tiered E1.2 | `FeeCalculator` | `#rideOfExactlyFifteenMinutesIsFree` | no, passed straight away | kept |
| 12 | tiered E2.2 | `FeeCalculator` | `#seventyFiveMinutesIsOneBillingHour` | no, passed straight away | kept |
| 13 | tiered E2.3 | `FeeCalculator` | `#oneSecondMoreStartsSecondBillingHour` | yes | yes (started hours; hours 1-2) |
| 14 | tiered E3.1 | `FeeCalculator`/`Tariff` | `#threeBillingHoursCostFifteen` | yes | yes (hour 3) |
| 15 | tiered E3.2 | `FeeCalculator`/`Tariff` | `#fourthAndFurtherHoursCostTheSame` | yes | yes (further hours) |
| 16-19 | tiered E4.1, E4.2, E5.1, E5.2 | `FeeCalculator` | `#rideStartedBeforeNewTariffUsesLaunchTariff`, `#rideStartedAfterNewTariffUsesIt`, `#stationReturnPaysNoReturnFee`, `#lockFailurePaysHourlyFeesOnly` | no, passed straight away (tariff at start already from cycle 7-8; no return fee yet) | kept |
| 20 | zone E1.1 | `FeeCalculator` | `ReturnFeeTest#returnInDropZoneAddsDropZoneFee` | yes | yes (fake: free-floating → drop zone fee) |
| 21 | zone E2.1 | `FeeCalculator` | `#returnOutsideDropZonesAddsUnauthorizedZoneFee` | yes | yes (by zone) |
| 22-28 | zone E1.2, E1.3, E2.2, E3.1, E3.2, E4.1, E4.2 | `FeeCalculator` | remaining `ReturnFeeTest` methods | no, passed straight away | kept |
| 29 | penalty E1.1 | `FeeCalculator` | `PenaltyTest#overdueRentalPaysTwelveHoursPlusPenalty` | yes (penalty 0) | yes; refactor of `FeeCalculator` into hourly fees / return fee / penalty |
| 30-33 | penalty E1.2, E2.1, E3.1, E4.1 | `FeeCalculator` | remaining `PenaltyTest` methods | no, passed straight away | kept |
| 34 | tariff id unique (spec Data) | `TariffSchedule` | `#secondTariffWithSameIdIsRejected` | yes | yes |

After the domain: `Version`, `TariffScheduleRepository`, memento and `InMemoryTariffScheduleRepository` with its
optimistic-locking test; `ZoneMap` + `cityacl` with `CityZoneMapAdapterTest` (inside, Riverside Park, border 149 m,
just outside 155.7 m, far away, withdrawal window); 6b use case, query handlers, REST, wiring; four acceptance test
classes (35 tests). Full suite green.
