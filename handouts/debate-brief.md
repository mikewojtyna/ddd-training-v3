# Debate brief: who writes the tests?

## The two motions

| Camp | Motion |
|---|---|
| **A** | AI must never write tests of business logic, or tests for its own code. |
| **B** | A different agent should write the tests. |

Camp A defends the motion "humans write business-logic tests". Camp B defends "a second agent writes them". You
don't have to attack AI or defend it: you have to defend your motion better than the other camp defends theirs.

## Format (30 min)

| Phase | Time | Who | What |
|---|---|---|---|
| Prep | 8 min | each camp on its own | open the code below, pick two arguments, each with a file; prepare one question for the other camp |
| Opening | 2 × 3 min | A, then B | your two arguments, with the files on screen |
| Rebuttal | 2 × 3 min | B, then A | answer the other camp's arguments; ask your question |
| Open floor | 8 min | everyone | anyone may speak, one at a time, max 1 min each; switching camps is allowed out loud |
| Closing | 2 × 1 min | A, then B | one sentence: what you still hold, what you concede |

The trainer keeps time strictly. When the time is up, finish your sentence and stop.

## Rules

1. **Argue with the code in front of you.** Every claim cites a file, and ideally a line. "I've seen AI write bad
   tests" isn't an argument. "Look at this test, it checks X and not Y" is.
2. **No "it depends" without an example.** If it depends, say on what, and show a case of each.
3. **Concede out loud.** If the other camp is right about something, say so in your closing. It counts in your favour.
4. One speaker at a time. In the open floor, raise a hand.

## Code pointers

Open these during prep. Each camp can use each one; it's up to you which way it cuts.

### 1. From a spec example to a test

- Spec: `docs/specs/rider-reserves-bike.md`, rule R1, example **E1.1** ("... Then B-17 is reserved for Anna until 10:02").
- Test: `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RiderReservesBikeSpecTest.java`, method
  `e1_1_eligible_rider_nearby_reserves_available_bike_for_two_minutes`. Its `@DisplayName` is the example's text.
- Both were written by an agent, and the spec existed before the code. But look where "2 minutes" comes from: R1
  cites **DE-10**, an answer in `docs/bounded-contexts/velocity/domain-expert-answers.md`. That file says the
  domain-expert answers were **role-played by Claude** during an autonomous run, and the spec was approved "under the
  trainer's standing instruction" (see the top of `docs/bounded-contexts/velocity/conversation.md`).

Questions to ask yourself: who decided "10:02"? Is the test trustworthy if the agent also invented the example it
checks? What would a reviewer compare the test against?

### 2. A test that an agent wrote for its own code

```bash
git show ex12/start --stat
git show ex12/start:src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike/BikeCancellationTest.java
git show ex12/start:docs/specs/rider-cancels-reservation.md
```

- An agent delivered `rider-cancels-reservation` in one commit, code and tests together. The build is green.
- Read `BikeCancellationTest`. Then read the spec's rules R1 to R3.

Questions to ask yourself: what does this test check? Would a rider or the product owner recognise it? Which rule of
the spec does it prove? (You'll review this commit properly in Exercise 12. Don't spoil it for the other camp.)

### 3. The TDD skill

- `.claude/skills/tdd/SKILL.md`, "Step 1: Get exactly one scenario".
- The skill **refuses to invent scenarios**: no scenario, vague scenario or several at once means it asks the human.
  It writes the test and the smallest code that passes it, then stops for the next scenario.

Questions to ask yourself: in this loop, who "writes the test"? Is the agent writing tests for its own code here, or
is the human dictating them? Does the answer change if the human only says "go ahead"?

### 4. Optional extra ammunition

- `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/RentalsArchitectureTest.java`: a test nobody types per
  feature, but it guards every feature the agent writes.
- The critic agents: `.claude/skills/spec-writer/agents/` and `.claude/skills/implementation-planner/agents/`. A
  second agent with a clean context already reviews specs and plans in this repo.

## Your prep sheet

| | Argument | File / line |
|---|---|---|
| 1 | | |
| 2 | | |
| Question for the other camp | | |
| What we'll probably have to concede | | |
