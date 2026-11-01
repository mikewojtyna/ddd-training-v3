I wrote the Pricing plan but haven't generated any code yet. The skill stops here so you can review it first.

**Plan:** `docs/bounded-contexts/velocity/pricing/plan.md` (Status: `draft - awaiting review`)

**Canvas check:** The name, purpose and ubiquitous language are all there, and the classification and role values are valid (core / revenue / custom built; Execution Model). "Pricing" matches the CML. All four business decisions are strict Given/When/Then examples. I worked each one through by hand against the Billing Period definition and none contradicts it: 0.00, 5.00, 5 + 7 + 9 + 12 = 33.00, and 5.00 from price list A.

**What the plan proposes:**
- 2 use cases: `PublishPriceList`, and `PriceRide` triggered by our own `RentalSessionEnded` record
- 1 query: `EstimateFee`
- 1 published event: `RidePriced`, for Settlement
- 2 context-specific secondary ports: `PriceListsRepository` and `ReturnChargesProvider`
- 1 ACL: `rentalsctxacl`, a stub for now
- 1 aggregate: `PriceLists`
- Tests: 4 GWT tests in `PriceListsTest` plus `PricingArchitectureTest`
- `pom.xml` gets ArchUnit 1.5.1

**Decisions I made:**
- **One aggregate for all price lists.** The "price valid at ride start" rule needs two price lists at once, so the aggregate holds the whole set. Each price list is a record that never changes after it's published.
- **Pricing doesn't change state.** It's a pure calculation, so pricing a ride returns `RidePriced` and saves nothing.
- **ACL towards Rentals** for Get Return Charges. It's a stub because Rentals has no such query in its `api`.
- **No listener for Rentals' event yet.** Rentals doesn't publish *Rental Session Ended* in its `api` yet, and listening to its internal events would break the boundary rule.
- **Estimate Fee calls the domain directly** instead of using a read model, so the fee rules aren't duplicated.
- **Thin REST endpoints** for the back office and the rider app. The paths are my proposal.

**Questions (Q1-Q9 in the plan):**
1. What should the aggregate be called? The UL has no term for "all price lists". I proposed `PriceLists`.
2. Are "ride" and "rental session" the same thing?
3. The "price valid at ride start" example doesn't give a free period. Can I use 10 minutes? Also, please confirm a boundary example: a 65-minute ride should cost 5.00 PLN, because billing hours start after the free period. All four of your examples give the same result whether hours are counted after the free period or from the start of the ride.
4. Return charges have no example yet, so they're implemented from the Total Fee definition but untested. Can I add this one: a 45-minute ride plus 2.00 PLN return charges costs 7.00 PLN?
5. Three edge cases need a decision:
   - What happens when no price list is valid at the ride start? I propose rejecting the ride.
   - What if a new price list has the same valid-from as an existing one?
   - Can a price list be published with a valid-from in the past?
6. Should a ride be priced only once?
7. Should Estimate Fee use the price list valid now, without return charges?
8. Are the proposed REST paths OK?
9. Should return charges travel on *Rental Session Ended* instead of a query back to Rentals?

**Differences from the reference context map:**
- **Pricing reacts to the event instead of being called by Settlement.** "Accepted Alternatives" covers this.
- **Return charges come from Rentals.** The map has Pricing calculate the zone fees itself from the City zone map. Not covered.
- **The maximum-rental-time penalty is left open.** The map says Pricing adds it. Not covered.
- **"Price List" instead of the map's "tariff".** Not covered.

I haven't changed the CML or `velocity-context-map.md`. They'd need updating to match the canvas, and I can do that if you want. I can also copy the canvas to `docs/bounded-contexts/velocity/pricing/canvas.md` so it's versioned next to the plan.

To review, reply here or edit the plan file directly. If you edit it, set `Status: approved` when you're done. Running the skill again picks up from the saved plan and generates the module.
