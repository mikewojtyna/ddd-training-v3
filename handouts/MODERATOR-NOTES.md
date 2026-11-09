# Moderator notes: who writes the tests?

Trainer only. Not part of the participants' start tag.

## Goal of the 30 minutes

Move the room from **"who types the test"** to **"who chooses the scenarios and who reviews them"**. The survey showed
7 of 9 don't verify what agents produce. The debate is the hook for Exercises 11 and 12, where they practise both
halves: choosing scenarios (TDD ping-pong) and reviewing an agent's tests (review).

## Before you start

- Split the room so each camp has people who **disagree** with its motion. Put the strongest AI sceptic in Camp B.
- Put the format table on the wall and a visible timer on screen.
- Have three tabs open: `docs/specs/rider-reserves-bike.md`, `RiderReservesBikeSpecTest.java`, and
  `git show ex12/start:src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeCancellationTest.java`.

## Phase by phase

### Prep (8 min)

Walk between the camps. Steering questions:
- "Which file proves that? Open it."
- Camp A: "Who wrote `RiderReservesBikeSpecTest`? Would you delete it?"
- Camp B: "The second agent: what does it read? The spec, the code, or both?"
- Both: "What will the other camp say about `BikeCancellationTest`?"

### Opening (2 × 3 min)

Cut off claims without a file: "Which file?" If a camp has none, let the silence work and move on.

### Rebuttal (2 × 3 min)

Make sure each camp answers the other's strongest argument, not the weakest. Ask: "What's their best point? Answer
that one."

### Open floor (8 min)

This is where the resolution forms. Push in this order:
1. "In `RiderReservesBikeSpecTest`, who decided 10:02?" The honest answer: the agent wrote the test **and** the
   example came from DE-10, role-played by Claude. The test is only as good as the decision behind it. In a real
   team, the human product owner approves that example. That's the step that makes the test trustworthy.
2. "In `BikeCancellationTest`, what did the agent check?" `hold()` is `Free` and `version()` didn't change: internal
   state. No spec rule is named in it. And spec rule R2 ("only the rider holding the reservation can cancel") has no
   test at all. Look at `Bike.cancelReservationOf` on `ex12/start`: it never checks who cancels. The agent's tests
   passed because they only tested what the agent built. (Don't reveal R2 if Exercise 12 follows right after;
   saying "one rule has no test" is enough.)
3. "In the TDD skill, who wrote the test?" The agent typed it; the human chose the scenario and judged the fake.
   Neither camp's motion describes this, and it's what the repo actually does.

### Closing (2 × 1 min)

Write each camp's concession on the board. Then lead the room to its one sentence.

## Likely arguments and counters

| Camp | Argument | Counter |
|---|---|---|
| A | "An agent testing its own code just confirms its own bugs." (`BikeCancellationTest`) | True when the agent also picks what to test. With an approved spec example, the agent can't silently pick: E1.1 says 10:02, so the test says 10:02. |
| A | "Tests are the spec; the spec must come from a human." | Agreed on the **content**. Typing `Instant.parse("2026-11-08T10:02:00Z")` isn't where the value is created. |
| A | "Humans then must at least read every test." | Yes: review is the non-negotiable part. That's an argument for review, not for typing. |
| A | "We can't trust AI at all." | Then you can't trust the AI's production code either. The question is what makes either trustworthy: scenarios + review + guardrails like `RentalsArchitectureTest`. |
| B | "A second agent with a clean context won't share the first agent's blind spots." | Partly: the critics in `.claude/skills/*/agents/` do catch things. But if both agents read the same vague spec, both inherit its gaps. If the second agent reads the code, it tests the code. |
| B | "Separation of duties, like dev vs. QA." | QA works because QA knows the business. A second agent knows only what it's given. Who gives it the scenarios? |
| B | "It scales: humans can't write all the tests." | Humans don't need to type them; they need to choose and review the scenarios. Example Mapping produces them cheaply (Exercise 7). |
| B | "Without access to the implementation it can only test behaviour." | Good, that helps. But it doesn't know which behaviour is wanted, only what the spec says. A missing rule stays missing. |
| both | "It depends." | "On what? Give one example of each side." |

## The resolution to land on

> Tests express **observable behaviour that a human defined**: the spec examples. Who types them matters less than
> **who chose the scenarios** and **who reviews them**. A separate agent without access to the implementation helps,
> but isn't enough on its own.

Supporting points, if the room needs them:
- **Traceability is the review tool.** Spec example E1.1 → test method `e1_1_...` with the example as
  `@DisplayName` → one line in `Bike`. A reviewer checks one list against another: every example has a test, every
  test names an example. `BikeCancellationTest` names none.
- **Tests should assert what a domain expert can read**: events, rejections, the state visible through the API.
  Not internal fields, not version numbers.
- **The human owns two moments**: approving the examples (before code) and reviewing the tests against them (after).
  Everything between can be typed by an agent.
- **A second agent is a reviewer, not an oracle.** Use it to find gaps (a rule without a test, a test without a
  rule), not to decide what the rules are.

## Optional: an objective referee

Mutation testing (PIT) changes the production code (e.g. flips `>` to `>=`, removes a call) and checks whether a
test fails. A test that executes code but checks nothing lets the mutant survive. PIT isn't set up in `pom.xml` yet;
if you add it (behind a profile), run it on `Bike` with only `BikeCancellationTest` to show surviving mutants in
`cancelReservationOf`. It settles "this test checks something" without opinions, but not "this test checks the right
thing": that still needs the spec.

## Links to the next exercises

- **Exercise 11, TDD ping-pong** (tag `ex11/start`): the pair chooses each scenario, the agent types the test and
  the smallest code, and the pair must reject one fake. That's the resolution in practice: human scenarios, AI typing.
- **Exercise 12, Review the agent** (tag `ex12/start`): the full commit behind `BikeCancellationTest`, with three
  planted defects (a spec rule without a test, domain logic in the REST adapter, a test of the implementation). That's
  the other half: reviewing what the agent wrote, then comparing with a skeptic agent.
