# Exercise 17: Capture the intents

**Module:** M4 Implementing a module with AI · **Time:** 45 min · **Form:** pairs · **Start:** tag `ex17/start`

Do this exercise **before Exercise 7**. Its reference solution holds the intents Exercise 7 starts from.

## Situation

Product has just filed `handouts/VELO-207-reservations.md`, an epic called "Manage reservations". Like most tickets, it
mixes business needs with solutions, opinions and leftovers. Nobody has turned it into intents yet: `docs/intents/`
holds only the story map, and there are no specs or plans.

The domain-expert answers about reservations have been removed from
`docs/bounded-contexts/velocity/domain-expert-answers.md`. **You are the business.**

## Your task

1. Read the ticket together, then "Renting a Bike" and "Broken Bikes & Maintenance" in
   `docs/domains/velocity/velocity-vision.md`.
2. Ask the agent to capture the intents, for example *"Capture the intents for handouts/VELO-207-reservations.md"*.
   The `intent-creator` skill runs.
3. Answer its questions **as the business**. When you don't know, or the ticket and the vision disagree, say so: it
   should become an open question, not a decision the agent makes for you.
4. Review every draft intent against this checklist:
   - one actor, a business role (not "the system", not "user"), and one goal; no "and", "manage" or "handle" in the title;
   - a clear *so that*: the business value, not a feature;
   - no Given/When/Then, no endpoint, table, Redis or Kafka outside *Notes for the spec*;
   - no number the business didn't give. The ticket has two competing durations. Where did they end up?
   - every line of the ticket is accounted for: an intent, an out-of-scope item, a note for the spec, or deliberately dropped.
5. **Change the split before you accept:** merge two intents, split one, or reject one, and say why.
6. Accept the intents you would sign as product owner. Ask the agent to set them to `accepted` (never let it decide
   that on its own), then:
   ```bash
   python3 .claude/skills/intent-creator/scripts/intents.py build
   python3 .claude/skills/intent-creator/scripts/intents.py check
   ```

## Done when

- At least three intents in `docs/intents/` have status `accepted` and hang under an activity of `story-map.md`.
- `intents.py check` reports no ERROR.
- You can name one line of the ticket that did **not** become an intent, and why.

## Debrief questions

- Which split did you argue about, between yourselves or with the agent?
- Did the agent invent a number, an actor or a rule? Did it notice the 15 minutes vs "the seconds it takes to unlock"?
- What happened to the Redis lock, the countdown timer, cancelling and Postgres 17?
- Compare with the reference solution: `git diff ex17/start ex17/solution -- docs/intents`.
- The canvases in `docs/bounded-contexts/velocity/` already name intents. Does your split match what the rest of the
  system expects? `grep -rn "reserv" docs/bounded-contexts/velocity/*/canvas.md`
- Next: Exercise 7 writes the spec of one of these intents.

## Rules

- Approval is yours. Never let the agent accept its own intents.
- Don't write specs, plans or code in this exercise.
