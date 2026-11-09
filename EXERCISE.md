# Exercise 5: Find the boundaries

**Module:** M3 Modularization for safe AI collaboration · **Time:** 60 min · **Form:** pairs · **Start:** tag `ex05/start`

## Situation

VeloCity, a city bike rental service, needs to be split into bounded contexts before anybody builds it with agents.
You have two inputs:

- `docs/domains/velocity/velocity-processes.jpg`: the Event Storming board of the business processes (or your own
  board from the M6 exercise);
- `docs/domains/velocity/velocity-vision.md`: the product vision. One of you plays the domain expert from it.

**Don't open `src/` or the git history.** The code already contains an answer, and the point is to find your own.

## Your task

1. Start the discovery, for example *"Find the bounded contexts of VeloCity from
   docs/domains/velocity/velocity-processes.jpg; I'm a workshop participant, the vision is in
   docs/domains/velocity/velocity-vision.md"*. The `discover-bounded-contexts` skill runs.
2. The skill applies the heuristics in order: **pivotal events, then swimlanes, then single responsibility**. It
   asks you questions. The domain expert answers from the vision; when the vision is silent or contradicts itself, the
   expert decides and you note the decision.
3. An independent critic agent reviews every proposed context. You **approve or reject each context yourselves**.
   Overrule the AI at least once when you disagree, and write down why.
4. Let it draw the context map, then validate it:
   ```bash
   .claude/skills/discover-bounded-contexts/scripts/validate_cml.sh <your-file>.cml
   ```

## Done when

- A discovery report exists: `docs/context-discovery/velocity/<date>-<your-team>/discovery.md`, with the heuristic
  trail, the Q&A and the critic log.
- A CML context map exists and `validate_cml.sh` passes: no shared kernel, no upstream/downstream cycles, ACL on the
  downstream side, OHS/PL on the upstream side.
- You can name the contexts where you overruled the AI, and why.

## Debrief questions

- Which pivotal events gave you the first cuts?
- Where did the agent want to cut along technical lines (a "Notifications" or "Database" context)?
- Compare with the reference map: `git diff ex05/start ex05/solution --stat`, then open
  `src/main/cml/VeloCity-Context-Map.cml` and `docs/domains/velocity/velocity-context-map.md` on `ex05/solution`.
  How many contexts match? Which differences are defensible?

**Extension:** run `residuality-analysis` on your map with ten stressors ("the city bans drop zones", "a second
payment processor", "corporate accounts", …) and find the hot spots.
