# Exercise 11: TDD ping-pong

**Module:** M5 Testing strategies with AI · **Time:** 45 min · **Form:** pairs · **Start:** tag `ex11/start`

## Situation

The city has a new demand: **a station docks at most a fixed number of bikes**. Most stations hold 20. A full
station turns away a returned bike, and the rider has to ride on to another one. Nothing in Rentals knows about
capacity yet (`src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/Station.java`).

## Your task

Drive the rule with the `tdd` skill, **one Given/When/Then at a time**. You choose the scenarios and the agent
types the code:

1. Say *"Let's TDD a station capacity rule in Rentals"* and give the first scenario.
2. For each scenario the agent writes **one failing test**, makes the **smallest change** that passes it (faking
   is allowed), runs the whole suite and reports back.
3. **You must reject at least one fake.** When the agent hard-codes something that only your current scenarios
   allow, don't argue with it: give the scenario that forces the real implementation.

Suggested scenarios, in this order (feel free to add your own):

| # | Given | When | Then |
|---|---|---|---|
| 1 | Central docks 19 bikes | a bike is returned at Central | it is admitted |
| 2 | Central docks 20 bikes | a bike is returned at Central | it is turned away: the station is full |
| 3 | ? | ? | ? (*write the scenario that kills the fake from step 2*) |

## Done when

- Every scenario has its own test, and each test was red before it went green.
- `./mvnw test` is green.
- You can point to the moment you rejected a fake and the scenario that forced the real code.

## Debrief questions

- Which part of the rule would the agent have invented if you'd asked for "station capacity" in one prompt?
- Is capacity a property of the station, of the bike, or of a policy between them? Who decided: you or the agent?
- Compare with the trainer's run, one commit per cycle: `git log --oneline ex11/start..ex11/solution`.

**Extension:** use the rule when a bike is returned (`ReturnBikeAtStationUseCase`). Where does the number of docked
bikes come from, given that each `Bike` is its own aggregate?
