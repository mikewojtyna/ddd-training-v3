The Pricing context is built, following the plan you approved. The build and all 15 tests pass, and the app still starts with the new wiring. Your return-charges rule is now a test.

**Files**
- Plan: `docs/bounded-contexts/velocity/pricing/plan.md`. Its status is now `implemented`, and it records your answers and a changelog entry.
- Canvas: `docs/bounded-contexts/velocity/pricing/canvas.md`, copied as you asked.
- Code: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/pricing/` (31 files).
- Tests: `src/test/java/pl/wojtyna/dddtrainingv3/velocity/pricing/`.

**I added four rules to the copied canvas.** Each is marked "Added in plan review, 2026-11-01", so the canvas, plan, code docs and tests all say the same thing. Please check that's what you meant:
- **Return charges** (your rule): 45 min ride with a 3.00 PLN drop-zone charge → 5.00 + 3.00 = 8.00 PLN. It replaces the example I had suggested (2.00 PLN → 7.00 PLN).
- **Billing hours start after the free period** (Q3): a 65 min ride costs 5.00 PLN, not 12.00 PLN.
- **No price list valid at ride start** (Q5): pricing is rejected.
- **A price list published again with the same start time replaces the earlier one** (Q5).

The other defaults you accepted are built in but have no tests:
- The single aggregate is named `PriceLists`.
- A ride and a rental session are the same thing.
- Price lists starting in the past are accepted.
- The fee estimate uses the price list valid now, with no return charges.
- The REST endpoints are as proposed.
- Pricing asks Rentals for the return charges, as the canvas says.

**What was built**
- 2 use cases: Publish Price List and Price Ride.
- 1 query: Estimate Fee.
- 1 published event: Ride Priced.
- 1 aggregate (`PriceLists`) with 8 Given/When/Then tests.
- 1 anti-corruption layer towards Rentals.
- 1 architecture test (2 rules) that keeps other contexts using Pricing only through its public `api` package.

To make that test possible I added ArchUnit 1.5.1 as a test dependency in `pom.xml`. It's the first version that can read class files from your local JDK 27.

**Decisions and pushbacks**
- I gave no pushbacks: you asked for no exceptions to the anti-corruption layer or to the context boundary.
- Unlike the plan, the two REST controllers are found by Spring's component scanning, the way CrowdSorcery's `WithdrawRestApi` is. Registering them in the config class as well would have mapped each endpoint twice.

**Still open**
- The adapter that asks Rentals for return charges is a placeholder. Rentals has no such query in its public `api` yet. Until it does, pricing a real ride fails at that step.
- Nothing listens for *Rental Session Ended* yet, because Rentals doesn't publish that event in its `api`.
- Settlement doesn't exist in code, so nothing consumes *Ride Priced*.
- Not modelled, each needs a decision and a Given/When/Then example:
  - the maximum-rental-time penalty (still an open question in the canvas)
  - pricing a ride only once if the same event arrives twice

**Differences from the reference context map** (unchanged since the plan):
- Pricing reacts to the event instead of being called by Settlement. "Accepted Alternatives" covers this.
- Pricing asks Rentals for return charges instead of using the City zone map. Not covered.
- The penalty is still an open question here, while the map gives it to Pricing.
- "Price List" here, "tariff" in the map.
- The *Rental Session Ended* event carries different data.

**Context map:** the VeloCity `CLAUDE.md` asks to keep `src/main/cml/VeloCity-*.cml` and `velocity/velocity-context-map.md` in sync with the code. They now differ on the points above. I haven't changed them, because the map is the workshop's reference solution. Should I update them to match the canvas?
