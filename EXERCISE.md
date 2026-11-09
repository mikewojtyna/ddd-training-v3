# Exercise 9: Build it test-first

**Module:** M4 Implementing a module with AI · **Time:** 90 min · **Form:** pairs · **Start:** tag `ex09/start`

## Situation

Three of the four reservation intents are delivered. The fourth, **`fleet-operations-releases-expired-reservations`**,
has an approved spec and an approved plan, but no code:

- `docs/specs/fleet-operations-releases-expired-reservations.md`: the rules and examples (R1 riders, R2 technicians,
  R3 reservations that ended by an unlock don't expire);
- `docs/plans/fleet-operations-releases-expired-reservations.md`: placement, contract mapping and the steps S1–S6,
  all `todo`.

Right now a reservation that nobody uses is never released. The bike stays held forever.

## Your task

1. Ask the agent to build the plan, for example *"The plan for fleet-operations-releases-expired-reservations is
   approved, build it"*. `implementation-planner` drives `bounded-context-scaffolder` and
   `ddd-tactical-building-blocks` step by step.
2. Work in the **TDD checkpoint loop**: one Given/When/Then example per cycle. At every checkpoint the agent stops
   and asks who writes the implementation. **Decide each time**: you or the agent. Do at least one cycle yourselves.
3. Watch for the usual pitfalls and stop the agent when you see one:
   - domain logic in the scheduled job, the REST controller or the listener instead of in the aggregate;
   - a test that checks the implementation (a getter, a private field) instead of a spec example;
   - Rentals calling Fleet Maintenance's domain directly instead of publishing an event (the architecture test will
     tell you);
   - the agent marking a step `done` before its tests are green.
4. Keep the plan honest: steps change status only through
   `python3 .claude/skills/implementation-planner/scripts/plans.py step <intent> S<n> <status>`.

## Done when

- Every example of the spec (E1.1–E3.2) has a passing test in
  `src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/FleetOperationsReleasesExpiredReservationsSpecTest.java`.
- `./mvnw test` is green.
- `plans.py done fleet-operations-releases-expired-reservations` succeeded, and the intent is `delivered`.

**Extension:** the technician side. When a technician's reservation expires, Fleet Maintenance must forget the
reservation so that any technician can reserve the broken bike again (`technician-reserves-broken-bike` E4.1–E4.3,
`technician-starts-maintenance` E3.2).

## Debrief questions

- Which checkpoints did you take over, and why those?
- Where did the agent want to put the expiry rule first? Where did it end up?
- Compare with the reference: `git diff ex09/start ex09/solution -- src`.

## Rules

- Approval and the decision at each checkpoint are yours.
- No Lombok (see `CLAUDE.md`). Domain classes aren't Spring beans.
