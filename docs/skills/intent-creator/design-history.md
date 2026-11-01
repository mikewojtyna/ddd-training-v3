# Design history: `intent-creator` skill

How the skill in `.claude/skills/intent-creator/` was designed.

- **Author:** Mike Wojtyna, with Claude Code (model: Claude Opus 5.5), using the `skill-creator` plugin
- **Started:** 2026-11-01, branch `intent-creator-skill` (from `master` at `b56076c`)

## 1. The original prompt (verbatim)

> Create the intent creator skill. Intent is an <name-of-intent>.md file places in the docs/intents directory. I have just two critical requirements:
> 1. Intent must be fine-grained (e.g. a player wants to make a deposit, a rider wants to ride a bike from a bike station, a rider wants to reserve a bike from a bike station)
> 2. Intents must be business-oriented: it's a good idea to create them based on user story maps.
>
> The goal of the intent is to be able to see the full proejct history from business/product perspective. Basically, each new feature/business need/process requires at least one intent. You should be able to recreate the entire value stream just by looking at sequence of events.
>
> The intent is then translated into a spec.md (specification) which is then transalted into plan.md. But these are the concerns of another skills. Don't bother with specs of plan.

The user also asked Claude to work autonomously in a new worktree and branch made from `master`, up to the point where review is needed.

## 2. Design decisions and their reasons

### 2.1 Two readings of the intent set: history and value stream
"See the full project history" and "recreate the entire value stream" are two different orderings of the same files:
- **History:** every intent carries a global, never-reused `sequence` number and a `date`. Sorted by sequence, the intents read as the product's history.
- **Value stream:** every intent carries `activity` and `task`, which place it on the story map. `docs/intents/story-map.md` holds the backbone of ordered activities per system. Grouped by activity in backbone order, the intents read as the journey.
- **"Sequence of events":** each intent lists its **business outcome** as past-tense domain facts (`Bike Reserved`, `Deposit Made`). Read in order, these events are the value stream in event-storming terms. This covers both readings of the phrase.

The file name stays `<intent-name>.md`, as the user specified. The order lives in the frontmatter, not in a filename prefix.

### 2.2 Fine-grained = user-story level of the story map
- One actor, one goal, one outcome.
- The skill gives signals for "too coarse" (*manage/handle/support*, "and", several actors, CRUD bundles) and "too fine" (a screen, a validation, one error branch).
- It adds a domain-expert test: "now X can Y", and would they care?
- A broad request is turned into a story-map slice first. With more than about seven intents, only the first release slice is written, so the review stays manageable.

### 2.3 Business-oriented = problem, not solution
- Endpoints, tables, queues, frameworks, classes, aggregates and bounded contexts are kept out of every intent section except `## Notes for the spec`. That way a technical request doesn't lose detail, but doesn't freeze a solution either.
- Actors are business roles, never "the system". Time-triggered needs name the role that wants them.
- A request with no business driver (for example a dependency upgrade) gets no intent. The skill explains why and asks for the driver.

### 2.4 Intents are history: supersede, don't rewrite
- An accepted or delivered intent is never rewritten. A changed need becomes a new intent with `supersedes`.
- The old intent gets only `status: superseded` and `superseded_by`.
- Drafts can still be edited in place.

### 2.5 Leave specs alone
- Business rules are copied as the user stated them. They aren't turned into Given/When/Then and no thresholds are invented. Turning a rule into a precise one is the spec skill's job, and a guessed number in an intent would reach the spec as fact.
- The skill stops for review after writing drafts.

### 2.6 A bundled script for the index and validation
`scripts/intents.py`:
- **`build`** regenerates `docs/intents/README.md` (the timeline plus a value-stream view per system).
- **`check`** validates frontmatter, sequence numbers, story-map references and supersede links, and flags solution vocabulary.

This keeps the two readings mechanical, so they can't drift from the files.

## 3. Evaluation iterations

### Iteration 1 (2026-11-01)
Four prompts, with and without the skill. Each run used an isolated worktree; see `evals/README.md`.

| Eval | With skill | Without skill |
|------|-----------|---------------|
| 1. VeloCity reservations epic | 13/13 | 7/13 |
| 2. CrowdSorcery withdrawal, technically phrased | 14/14 | 6/14 |
| 3. Changed need + new intent on seeded history | 16/16 | 14/16 |
| 4. Spring Boot upgrade (no business need) | 3/3 | 1/3 |

Findings:
- Without the skill, intents drift into specs: Given/When/Then scenarios, bounded-context assignment, technical shape, status codes.
- Without the skill, a technical-only request also produced an "intent".
- Open point for review: the skill's runs disagreed on the actor for reservation expiry (Rider in eval 1, Fleet Operations in eval 3).

User review: *pending*.
