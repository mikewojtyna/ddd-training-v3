# Exercise 7: Write the spec

**Module:** M4 Implementing a module with AI · **Time:** 60 min · **Form:** pairs · **Start:** tag `ex07/start`

## Situation

VeloCity is running, but four business needs around reservations have been accepted and nobody has specified them yet:

| Intent | Difficulty |
|---|---|
| `docs/intents/rider-reserves-bike.md` | easy |
| `docs/intents/rider-unlocks-reserved-bike.md` | medium |
| `docs/intents/fleet-operations-releases-expired-reservations.md` | medium |
| `docs/intents/technician-reserves-broken-bike.md` | hard: reuses reservation for a different purpose |

All four have status `accepted`. There's no spec and no plan for any of them yet.

## Your task

1. Pick one intent. Read it together, then read the related parts of `docs/domains/velocity/velocity-vision.md`.
2. Ask the agent to write its spec, for example *"Write the spec for fleet-operations-releases-expired-reservations"*.
   The `spec-writer` skill runs and the spec-critic reviews the draft before you see it.
3. Answer the agent's questions **as the business**. When you don't know the answer, write it down as an open question.
4. Review the draft. **You must reject at least one rule or add at least one missing example.** Look for:
   - the exact moment something ends: at 10:02:00 or only from 10:02:01?
   - what happens to a bike that is held by somebody else;
   - a rider versus a technician: same rule or a different one?
5. Approve it only when you would sign it as the product owner:
   ```bash
   python3 .claude/skills/spec-writer/scripts/specs.py approve <intent>
   ```

## Done when

- `docs/specs/<intent>.md` exists with status `approved`, and the intent's status is `specified`.
- `python3 .claude/skills/spec-writer/scripts/specs.py check <intent>` reports no ERROR.
- You can name the rule you changed and why.

## Debrief questions

- Which question from the agent surprised you? Which question did it **not** ask?
- What did the critic catch that you'd have missed?
- Compare with the reference solution: `git diff ex07/start ex07/solution -- docs/specs`.

## Rules

- Approval is yours. Never let the agent approve its own spec.
- Don't edit status lines by hand. Use the scripts.
