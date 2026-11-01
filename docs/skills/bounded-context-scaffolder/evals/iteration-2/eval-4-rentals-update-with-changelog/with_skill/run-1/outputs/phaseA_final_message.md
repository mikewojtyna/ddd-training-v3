I've written the update plan for Rentals and stopped there for your review. No code has changed yet.

**Plan:** `docs/bounded-contexts/velocity/rentals/plan.md` (status: `draft - awaiting review`, with a dated changelog entry)

## What the update would do
Rentals already exists, so this is an update. The plan compares the new canvas with the code:

- **Use cases: 2.** Rent Bike already exists, but `RentBikeUseCase` only has comments, so it would be implemented. Return Bike is new.
- **Queries: 1, new.** Available Bikes At Station: the query and result records, a handler, a read-model port, and an in-memory read model built from the domain events.
- **Events:**
  - Bike Returned is published to Pricing as an integration event.
  - The existing `BikeRented` and `BikeReturned` domain events gain station and bike ids.
  - One new domain event, `BikePlacedInStation`, so the query's read model knows which bikes are in a station (Q8).
- **Ports:** `BikeStationRepo`, which exists but has no implementation yet, gets an in-memory one. `AvailableBikesReadModel` is new.
- **ACLs: 0.** The only outbound message is an event, and nothing comes in from another context.
- **Aggregates: 1**, the existing `BikeStation`. Every Given in the canvas is about the state of one station.
- **Tests:** 2 new ones (`returnRentedBike`, `returnBikeThatIsNotRented`). In the existing `test0` and `test2`, the result of the event check is never asserted, so they check nothing about the event today. I'd make them assert it.
- **Wiring:** a new `RentalsContextConfig`, with bean names that start with `rentals`.
- **Deleted: nothing.**

## Decisions
- I kept the existing styles: the chained `rent(bike).by(borrower).forDuration(duration)` call, the `returnBike(Bike)` signature, use cases that take plain parameters instead of command records, and the existing class and test names.
- **`rent` would book the bike you ask for.** Today it ignores its `bike` argument and rents any available bike. The new return rule ("bike01 was rented by George") needs to know which bike was rented.
- No REST endpoints, because the canvas describes none.

## I need your answers
- **Q2, workshop stubs:** both features replace stubs that the VeloCity `CLAUDE.md` calls intentional: `BikeStation.returnBike` and the comment-only `RentBikeUseCase`. Can I fill them in, or must they stay for the workshop?
- **Q3, the 11-hour limit:** "max 11 hours" doesn't match the code. `toHours() > 11` rounds down, so a rental of 11 h 30 min is accepted today. I suggest these examples, and I won't change the code until you confirm them:
  - 11 h is rented
  - 11 h 30 min is rejected
- **Q4, a rule missing from the canvas:** the code and `test1`/`test2` reject renting when no bike is available, but the canvas no longer lists that rule. Was that on purpose? I'd keep the code and add the rule back to the canvas.
- **Q5, returns to another station:** the canvas has no example. I'd leave it unmodelled for now, so returning a bike to a station that doesn't know it is rejected as "not rented". If it should work, please give me a Given/When/Then example.
- **Q6, Bike Returned contents:** the canvas gives no fields. I'd send station, bike and borrower ids. Pricing will probably need start and end times, which Rentals doesn't track yet.
- **Q7, query result:** I'd return a list of bike ids rather than a count.
- **Q8, how the query learns about bikes:** `placeNew` would start returning a `BikePlacedInStation` event so the read model can track placements.
- **Q1, canvas location:** can I copy the canvas from the skill's eval folder to `docs/bounded-contexts/velocity/rentals/canvas.md`, so the plan and the code docs point to a file in the repo?

I worked through every example in the canvas, and none of them contradict each other or the term definitions.

## Differences from the reference context map
| Topic | Canvas | Reference map | Covered by "Accepted Alternatives"? |
|---|---|---|---|
| Ride-ended message | Bike Returned, sent to Pricing | Rental Session Ended, sent to Settlement | Partly: Pricing reacting to the event is accepted, the other message name and the missing Settlement link aren't |
| 11-hour limit | The requested length is rejected up front | Rentals ends the session itself when time runs out, and Pricing adds a penalty | No |
| Accounts | No link | Accounts tells Rentals whether an account is active | No |
| Fleet Maintenance | No link | Rentals and Fleet Maintenance exchange faults and withdraw / make-available calls | No |
| Language | rent, return, Borrower | reserve, unlock, rental session, withdrawn | No |

I'll build what the canvas says. I won't change `src/main/cml/VeloCity-*.cml` or `velocity/velocity-context-map.md` unless you ask, because the map may be a workshop exercise. I can update them after the review.

To go ahead, answer the questions here or edit the plan file directly, then approve it (say so, or set `Status: approved`). Running the skill again will pick up from the saved plan.
