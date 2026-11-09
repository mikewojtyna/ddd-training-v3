# Exercise 8: Plan and place it

**Module:** M4 Implementing a module with AI · **Time:** 45 min · **Form:** pairs · **Start:** tag `ex08/start`

## Situation

The four reservation intents now have approved specs (`docs/specs/`), and their intents are `specified`. If your
own spec from Exercise 7 isn't finished, continue from here: these are the reference specs.

Nobody has decided yet **where** each rule lives. VeloCity already has eight bounded contexts (see
`src/main/cml/VeloCity-Context-Map.cml` and the canvases in `docs/bounded-contexts/velocity/`).

## Your task

1. Keep the intent you specified in Exercise 7. If you didn't finish, pick one now.
2. Ask the agent to plan it, for example *"Make a plan for technician-reserves-broken-bike"*.
   The `implementation-planner` skill runs, and the plan-critic reviews the draft before you see it.
3. Discuss the placement before reading the steps:
   - Does `technician-reserves-broken-bike` belong in **Rentals** (who holds the bike) or in **Fleet Maintenance**
     (who repairs it)? Or both, with an integration between them?
   - Does `fleet-operations-releases-expired-reservations` need a **policy**, a **scheduled job**, or both? Who
     tells Fleet Maintenance that a technician's reservation expired?
   - Does the plan respect the context map: the relationship direction, ACL downstream, only `api` packages across
     contexts?
4. Challenge at least one of the plan-critic's findings, or a step you disagree with. **Change one thing in the
   plan** before you approve it:
   ```bash
   python3 .claude/skills/implementation-planner/scripts/plans.py approve <intent>
   ```

## Done when

- `docs/plans/<intent>.md` exists with status `approved`, and the intent's status is `planned`.
- `python3 .claude/skills/implementation-planner/scripts/plans.py check <intent>` reports no ERROR.
- You can explain the placement decision in two sentences, and name the change you made.

## Debrief questions

- Where did the agent's placement differ from your first instinct? Who was right, and why?
- Which steps touch more than one bounded context? What does that cost the agent in context and tokens?
- Compare with the reference plans: `git diff ex08/start ex08/solution -- docs/plans`.

## Rules

- Approval is yours. Don't edit status lines by hand. Use the scripts.
- Don't build anything yet. Building is Exercise 9.
