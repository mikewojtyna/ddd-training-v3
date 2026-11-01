The Pricing context is built from the approved plan, with your Return charges rule added. The build and all 15 tests pass, and the Spring context starts with the new Pricing wiring.

**Where things are**
- Plan: `docs/bounded-contexts/velocity/pricing/plan.md`. Status is `implemented`; your answers are under "Review notes" and the work is in the changelog.
- Canvas: `docs/bounded-contexts/velocity/pricing/canvas.md`. It's a copy of the original with your Return charges rule added under Business Decisions. The plan and the javadoc now point here.
- Code: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/`
- Tests: `src/test/java/pl/wojtyna/dddtrainingv3/velocity/pricing/domain/pricelist/`

**What's in it**
- **Primary ports (3):**
  - `PriceRideUseCase`: our own `PriceRide` record stands in for Rentals' "Rental Session Ended".
  - `PublishPriceListUseCase`.
  - `EstimateFeeQueryHandler`.
- **Integration event (1):** `api/events/RidePriced`, for Settlement. It's kept separate from the domain event `PriceListEvent.RidePriced`.
- **Secondary ports (2):**
  - `PriceListRepository`, implemented in memory.
  - `ReturnChargesProvider`, reached through the ACL `infra/adapters/secondary/rentalsctxacl`. The ACL adapter is a stub because Rentals has no return-charges query in code.
- **Aggregate (1):** `PriceList`. `PriceLists` picks the price list that is valid when a ride starts.
- **Wiring:** `PricingContextConfig`. Every bean name starts with `pricing`.

**10 rules, each with a test and listed in the `package-info.java` javadoc**
- From the canvas:
  - Free ride
  - First hour
  - Tiered hours
  - Price valid at ride start
  - Return charges (5.00 + 3.00 = 8.00 PLN): `PriceListTest#returnCharges`
- Your confirmed edge-case defaults:
  - Billing periods start after the free period (65 min → 5.00 PLN)
  - The free period is inclusive (10 min → 0.00 PLN)
  - A ride ending exactly on an hour boundary doesn't start the next hour (70 min → 5.00 PLN)
  - A price list applies from its exact valid-from time (a ride starting at 09:00 uses list B)
  - Pricing is rejected with `NoPriceListValidAt` when no price list is valid at the ride's start

**Still open**
- **Q4 (Estimate Fee):** I read your approval of the defaults as covering this one. The estimate uses the price list valid now and leaves out return charges. Say so if the rider app should send a planned start time instead.
- **Edge cases in the canvas:** the five confirmed edge cases are in the plan and the javadoc but not in `canvas.md`. Should I add them there too?
- **Stubs and missing listener:**
  - Real rides can't be priced at runtime yet. `PriceRideUseCase` needs return charges, and the Rentals ACL adapter that supplies them throws `UnsupportedOperationException("implement this")`.
  - Rentals has no `RentalSessionEnded` event, so there's no listener for it yet.
  - Settlement doesn't exist yet, so nothing receives `RidePriced`.
  - There are no REST endpoints, because the canvas doesn't describe an API.
- **Open question from the canvas:** does the maximum-rental-time penalty belong to Pricing or Rentals?

**Differences from the reference context map**
- Pricing reacts to "Rental Session Ended" instead of being called by Settlement. The map's Accepted Alternatives allow this.
- These differences aren't covered by Accepted Alternatives:
  - Rentals reports the return (zone) charges, so Rentals is upstream of Pricing.
  - There is no relationship with the City.
  - The penalty for exceeding the maximum rental time isn't modelled.
- The code uses the canvas term "Price List" where the map says "tariff".

`src/main/cml/VeloCity-*.cml` and `velocity/velocity-context-map.md` would need updating to match the canvas. I haven't changed them because the map may be a workshop exercise. Should I update them?
