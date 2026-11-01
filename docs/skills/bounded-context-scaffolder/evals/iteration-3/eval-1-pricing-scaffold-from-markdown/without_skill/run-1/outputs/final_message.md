I scaffolded the Pricing bounded context at `velocity/pricing` from the canvas. All 4 business decisions are now tests and they pass. `./mvnw test` passed: 15 tests, including the Spring context-load test. Nothing has been committed.

## How it's built

- **Domain** (`domain/pricecatalog`):
  - `PriceCatalog` is the aggregate root. It holds every published `PriceList` and picks the one valid when the ride started. It rejects a second price list that becomes valid at the same moment.
  - Pricing a ride follows the Rentals fluent style: `catalog.price(ride).withReturnCharges(charges)` returns `DomainEvents` with `PricingEvent.RidePriced`.
  - `PriceList.feeFor(duration)` charges nothing during the free period. After that, each started hour is one billing period, charged at the first, second, third, then subsequent hourly fee.
  - Value objects: `Ride`, `FreePeriod`, `HourlyFees`, `ReturnCharges`, plus the id types. Money uses joda-money.
  - `PriceCatalogRepository` and `ReturnChargesProvider` (for the Get Return Charges query) are `@SecondaryPort`s.
- **API**:
  - `PublishPriceListUseCase` handles the back-office command.
  - `PriceRideUseCase` prices a finished ride (Rental Session Ended).
  - `EstimateFee` plus `EstimateFeeQueryHandler` answer the rider-app query.
  - `api/events/RidePriced` is the event published for Settlement.
- **Infra**:
  - Two REST adapters: `POST /velocity/pricing/price-lists` and `GET /velocity/pricing/fee-estimates?rideMinutes=N`.
  - Two ACL packages, both `rentalsctxacl`:
    - Inbound: `RentalSessionEndedListener` receives Rental Session Ended.
    - Outbound: `RentalsReturnChargesProvider` covers Get Return Charges. It is a stub that throws `UnsupportedOperationException`, because Rentals has no such query yet.
  - An in-memory repository.
  - A translator that turns the domain `RidePriced` into the published `RidePriced`.
  - All of it is wired in `PricingContextConfig`.
- **Tests**:
  - `PriceCatalogTest` covers Free ride, First hour, Tiered hours and Price valid at ride start, plus checks for return charges, a missing price list, duplicate publishing and estimates.
  - `PriceRideUseCaseTest` runs the use case end to end.

**Outside the Pricing package:**
- I added `rentals/api/events/RentalSessionEnded`, so Pricing has a Rentals event to listen to. Rentals doesn't publish it yet.
- I updated `velocity/CLAUDE.md` to say Pricing now exists and to describe it.

## Decisions for you
1. **The canvas and the reference context map disagree, and I left the CML and `velocity-context-map.md` unchanged.**
   - The reference map has Settlement calling Pricing and the City supplying the zone map to Pricing. It has no Rentals → Pricing link.
   - The canvas has Pricing reacting to Rental Session Ended, publishing Ride Priced, and querying Rentals for return charges.
   - "Pricing reacting" is one of the map's accepted alternatives. Should I update both CML files and the map doc to match the canvas, or bring the canvas in line with the map?
2. **Where do return charges come from?** The canvas assumes they are "always known by the time the session ends". If so, Rentals could put them in Rental Session Ended, and the extra Get Return Charges query would not be needed.
3. **Is it OK to add `RentalSessionEnded` to Rentals' `api/events`?** The alternative is to leave the Rentals package untouched for the workshop.
4. **The aggregate name `PriceCatalog` isn't in the ubiquitous language.** Does the domain expert have a word for "all price lists"? The CML uses "Tariffs".
5. **Edge rules the canvas doesn't cover:**
   - A ride of exactly 10 minutes costs 0. A ride of 10 minutes and 1 second already pays for the first hour.
   - Should publishing a price list with a past `validFrom` be rejected? Allowing it would change the price of rides that have already started.
   - The estimate uses the price list valid now and leaves out return charges.
   - Currency comes from the price list; the canvas only shows PLN.
6. **The open question from the canvas is still open:** does the maximum-rental-time penalty belong to Pricing or Rentals? Note that the reference map gives it to Pricing. I didn't model it.
