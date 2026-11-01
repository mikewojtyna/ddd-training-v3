I wrote the plan for the Pricing context but haven't generated any code yet. The skill stops here so you can review the plan first.

**Plan:** `docs/bounded-contexts/velocity/pricing/plan.md` (Status: `draft - awaiting review`)

**What it would generate** in `velocity.pricing` (scaffold mode, one module):
- 2 use cases: `PriceRideUseCase` for Rental Session Ended, and `PublishPriceListUseCase`
- 1 query: `EstimateFeeQueryHandler`
- 1 published event: `api.events.RidePriced`, plus the domain event `PriceListEvent.RidePriced`
- 2 secondary ports: `PriceListRepository` (implemented in memory) and `ReturnChargesProvider`
- 1 ACL: `rentalsctxacl/RentalsReturnChargesAdapter`, which is a stub because Rentals has no return-charges query yet
- 1 aggregate, `PriceList`, plus `PriceLists`, a collection that picks the price list valid at ride start
- 4 Given/When/Then tests: `PriceListTest#freeRide`, `#firstHour`, `#tieredHours` and `PriceListsTest#priceValidAtRideStart`

**Canvas check:** all 4 rules are strict GWT examples. I worked each one through by hand and they agree with the definitions (8 min → 0.00, 45 min → 5.00, 3 h 20 min → 5+7+9+12 = 33.00, a ride starting 08:50 uses list A → 5.00). I found no contradictions. Domain role: Execution Model, which is a valid worksheet trait.

**Decisions:**
- **ACL towards Rentals:** yes, because the canvas asks for it and it's the default.
- **Money:** `joda-money` in PLN.
- **Rental Session Ended:** the use case takes Pricing's own `PriceRide` command. There's no listener yet because Rentals doesn't publish that event in code.
- **Estimate Fee:** prices a ride with the list valid now and leaves return charges out.
- **REST endpoints:** none, because the canvas doesn't describe an API.

No pushbacks were needed: no request to skip an ACL and none for several modules.

**Questions for you:**
- **Q1:** should I copy the canvas from the skill's `evals/files/` to `docs/bounded-contexts/velocity/pricing/canvas.md`?
- **Edge cases the definitions leave open.** Each one has a suggested GWT example and a default in the plan:
  - Q2: return charges added to the total fee (45 min + 3.00 → 8.00)
  - Q3: billing periods counted after the free period (65 min → 5.00, not 12.00)
  - Q5: a ride of exactly 10 min is free
  - Q6: a ride of exactly 70 min is 1 billing period
  - Q7: a list's valid-from moment is inclusive
  - Q8: if no price list is valid at ride start, pricing is rejected
- **Q4:** Estimate Fee semantics. Should it use the list valid now, without return charges?
- **Differences from the reference context map:**
  - Pricing reacts to Rentals instead of being called by Settlement. "Accepted Alternatives" covers this one.
  - Rentals reports return charges, so zone fees move out of Pricing and the City relationship goes away. Not covered.
  - The penalty is an open question in the canvas. The reference map puts it in Pricing.
  - The canvas says "Price List" where the map says "tariff".

You can reply here or edit `plan.md` directly. Set `Status: approved` or tell me it's approved, and run the skill again. It picks up from the saved plan and generates the module. I haven't changed the CML or `velocity-context-map.md`.
