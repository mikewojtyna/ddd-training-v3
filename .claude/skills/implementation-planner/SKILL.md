---
name: implementation-planner
description: Turn an approved intent + spec pair (docs/intents/<intent>.md + docs/specs/<intent>.md) into an implementation plan in docs/plans/<intent>.md, then build it. It decides which bounded context owns each command, query, event and rule using the CML context map, canvases and discovery reports (existing context, several contexts with an integration, or a new context via discover-bounded-contexts), translates the spec into each context's ubiquitous language and REST/event contracts, writes canvas changes and an ordered list of steps, stops for review, and after approval drives bounded-context-scaffolder and ddd-tactical-building-blocks (spec examples as TDD scenarios) until every example has a passing acceptance test and the intent is delivered. Use this whenever the user wants to plan, implement, build, deliver or "start working on" a specified intent or approved spec in VeloCity, CrowdSorcery, Orders or another system in this repo, asks "where should this live?", "which bounded context gets reservations?", "the spec is approved, what's next", "make a plan for rider-reserves-bike", or wants to resume or revise an implementation plan, even if they never say "plan". Don't use it to write intents (intent-creator) or specs (spec-writer), or to scaffold a context from a canvas the user brings directly (bounded-context-scaffolder).
---

# Implementation Planner

The third link in the chain **intent → spec → plan → code**. The intent says what the business needs, the spec makes it concrete on examples, and the plan says **where in the architecture it goes and in what order it gets built**. Then this skill drives the build through the skills that own each layer.

A plan is 1:1 with its intent and spec: `docs/plans/<intent>.md`. It isn't the same as a context's `docs/bounded-contexts/<system>/<context>/plan.md`, which `bounded-context-scaffolder` keeps per context. One intent plan can touch several context plans.

The plan's most valuable part is **placement**: deciding which bounded context owns each piece of the spec. That's strategic design applied to one requirement. Get it reviewed before any code exists, because a misplaced rule is cheap to move in a plan and expensive to move out of an aggregate.

Read when needed:
- `references/placement-heuristics.md`: who owns rules, commands, events and queries; the four outcomes; smells; translation; context-map changes. **Read it before Step 2.**
- `agents/plan-critic.md`: the independent critic's brief (Step 5).
- `assets/plan-template.md`: the plan's structure. Copy it exactly; `scripts/plans.py` relies on its headings.
- The project's `CLAUDE.md` and the system's `CLAUDE.md` (e.g. `src/main/java/pl/wojtyna/dddtrainingv3/velocity/CLAUDE.md`): package layout and conventions.

## Who does what

This skill **plans and orchestrates**. It doesn't duplicate other skills' rules:

| Work | Owner |
|---|---|
| New context, or a new context-map relationship | `discover-bounded-contexts` (validate mode) |
| Canvas creation, module structure, `api`, ACLs, REST, wiring, architecture test | `bounded-context-scaffolder` |
| Which building block each rule needs, and the TDD loop that builds it | `ddd-tactical-building-blocks` |
| Placement, translation, canvas diffs, step order, acceptance tests, statuses | this skill |

If one of those skills is missing, say so and suggest installing it from `.claude/skills/`. Don't silently do its job.

## Step 0: Resume or start

Look for `docs/plans/<intent>.md`. Find the intent by name, or by matching the user's words against `docs/intents/README.md` and the specs in `docs/specs/`.

- **No plan:** go to Step 1.
- **`draft - awaiting review`:** read it and collect feedback: from the conversation, from edits the user made in the file, and from spec changes (see the drift check). Revise, add a line under "Review notes", and stop at Step 5 again, unless the user has approved this version.
- **`approved` or `in progress`:** run the drift check, then continue at the first step whose status isn't `done` or `skipped` (Step 6).
- **`done`:** a new request means a spec change. Set the plan back to `draft - awaiting review`, add a changelog entry, and revise only what the spec diff requires.

**Drift check:** if the spec's `date` is newer than the plan's `spec_date`, or the spec is back in `draft`, the spec changed after planning. Diff the rule and example IDs. Revise the affected placement, canvas changes and steps, and get the revision approved before building on it. A spec in `draft` blocks building: send the user back to `spec-writer` to re-approve it.

## Step 1: Gate and read

- **Gate:** the spec is `approved` and the intent is `specified`. Otherwise stop and say what's missing: an unapproved spec goes back to `spec-writer`, a draft or accepted intent needs its spec first. Open questions the user accepted as open are fine; carry them into "Risks and open questions".
- Read the intent, the spec, the system vision, and the CML map (`src/main/cml/<System>-*.cml`; for VeloCity `VeloCity-Context-Map.cml`).
- Read every canvas in `docs/bounded-contexts/<system>/`, and discovery reports in `docs/context-discovery/<system>/` if there are any.
- List which contexts exist in code (`src/main/java/pl/wojtyna/dddtrainingv3/<system>/*/package-info.java`). For the candidate contexts, skim the domain package and tests: what they already enforce, and what they call things.
- **Check the code against what it claims to deliver.** The spec relies on related intents marked `delivered`: renting must exist before reserving makes sense. Check that the code really behaves the way those intents and their specs say. For example, does `rent(bike)` rent *that* bike? A gap here is a finding, not something to fix quietly. Add a step that fixes it, and flag it at review, because it changes behaviour someone believes is finished.
- **Work the spec examples through once more against each other.** A contradiction is easy to miss when examples are read one at a time and only shows up when you think about time, order or state across them, e.g. a month published at 00:05 that should include a ride ending at 00:20. Don't plan around a contradiction: send the spec back to `spec-writer` with the conflicting example IDs, and say so in the report. Other planning work can still go into the draft.
- Read existing plans in `docs/plans/` for intents listed under the intent's `## Related intents`. Their placement decisions are precedent: follow them, or say why this case differs.

**VeloCity is a workshop exercise.** The CML and `velocity/velocity-context-map.md` are the reference solution. Use them for placement, but if the user seems to be a participant who is still discovering contexts, ask before leaning on the reference map.

## Step 2: Place every spec element

Follow `references/placement-heuristics.md`. Fill the Placement table: one row for every rule `R<n>`, command, query and event, each with an owner and a reason that cites a heuristic and a source. Then name the outcome:

1. **One existing context.** Continue.
2. **Several existing contexts.** For each hand-off, name the upstream, the downstream, the message, and the pattern: event plus policy through an ACL, or a query to an OHS. Check the direction against the CML. A missing relationship becomes a proposed context-map change (CML snippet in the plan). A reversed one is a cycle: re-check the placement first.
3. **A new context.** Stop planning code. Invoke `discover-bounded-contexts` in validate mode, passing:
   - the candidate context with its key question;
   - the spec as the narrative it must support;
   - the existing CML.

   When discovery approves the context and the map and the canvas exists, come back here. The plan resumes with that context as an owner.
4. **Ambiguous.** Ask, using `AskUserQuestion` if available. Give the options and your recommendation with its reason.

For outcomes 1 and 2, write the placement into the draft as a **proposal** and carry on. The user confirms it at the review stop (Step 5), where it's the first question. Placement is the decision this skill exists for, so the user should make it consciously, and seeing it next to its consequences (steps, canvas changes) makes that decision easier. A separate stop just for placement would only add a round trip.

## Step 3: Translate and map the contract

- **Translation table:** each spec vocabulary term → the owning context's term (from its canvas or code), and where the translation happens. New terms go into that context's canvas changes.
- **Contract mapping:** every spec command, query and event → the code that realises it, following the repo's conventions:
  - a command becomes a use case and command record in `api/usecases`, plus a REST endpoint (primary adapter) when an actor outside the system issues it. Path style: `/<context-kebab>/<resource>/{id}/...`, as in `/fleet-maintenance/bikes/{bikeId}/fault-reports`.
  - a query becomes a query and result record in `api/queries`, plus a handler and a `GET` endpoint.
  - an event becomes a domain event. It becomes an integration event in `api/events` too, but only if another context listens: publishing costs a published language to maintain.
  - data shapes stay as notes for the tactical skill and the scaffolder. Don't design value objects here.
- Technical hints in the spec's `## Notes for the plan` (an endpoint path, a broker) are input, not orders. Follow them where they fit the repo's conventions, and record where you deviate and why.

## Step 4: Canvas changes, decisions and steps

**Canvas changes** per owning context: ubiquitous-language additions, inbound and outbound messages, and business decisions. Each spec example becomes a canvas business decision in its Given/When/Then form, tagged with its ID, e.g. `### R2 One active reservation per borrower (spec: rider-reserves-bike-at-station#E2.1)`. Use the context's terms. The tags keep the chain traceable from canvas to spec to intent.

**A context exists in code but has no canvas** (VeloCity's Rentals, for instance): the scaffolder works from canvases, so the first step for that context backfills one from its code, CML responsibilities and relationships, using `bounded-context-scaffolder` in canvas-only mode. Plan the reservation rules on top of the backfilled canvas.

**Decisions**, which the scaffolder and the tactical skill take from the plan, so approving the plan approves them:
- **TDD checkpoint loop:** on (strongly recommended) or off. Ask at review.
- **Aggregate style**, if the spec or the user implies one. Otherwise leave it to the tactical skill.
- **ACLs:** one per outbound query or command by default. Record any exception the user insists on, with its risk.
- **Context-map changes**, and where the CML is written.

**Steps**, ordered so that every step ends green:
1. Context-map changes and new contexts (discovery) come first.
2. Upstream contexts before downstream ones: a listener can't be tested before its event exists.
3. Within one context:
   - canvas update;
   - `bounded-context-scaffolder` update mode (6a);
   - `ddd-tactical-building-blocks` with the spec examples as scenarios, **in runs of at most 3 rules** (the tactical skill's gate), grouping rules that guard the same state;
   - scaffolder 6b (use cases, REST, wiring).
4. **Acceptance tests last.** One test per spec example, at use-case level: it drives the use case or query handler with in-memory adapters, and it's named after the example ID with the scenario in `@DisplayName`. Put it at `src/test/java/.../<system>/<context>/<IntentCamelCase>SpecTest.java`. A cross-context intent adds one Spring-context test for the event hand-off. These tests prove the spec, not the design: the unit tests from the TDD loop prove the design.

Every example ID must be covered by at least one step, and the traceability table maps each rule and example to its steps.

## Step 5: Write, validate, stop for review

Write `docs/plans/<intent>.md` from `assets/plan-template.md`, with `status: draft - awaiting review` and `spec_date` set to the spec's `date`. Then:

```bash
python3 .claude/skills/implementation-planner/scripts/plans.py check <intent>
```

Fix every error. Errors include an uncovered example, an unplaced command, a spec ID that doesn't exist, and a status mismatch.

**Independent critic.** The script checks coverage, not judgement. Before the user sees the plan, spawn **one** plan critic with the Agent tool (general-purpose). Its prompt contains:
- the absolute path of `agents/plan-critic.md`, with the instruction to read it first;
- the paths of the plan, spec, intent, CML map(s) and canvases;
- the `plans.py check` output;
- the user's answers so far;
- the round number.

Give it the evidence, not your reasoning, so that it judges placement fresh.
- **AGREE:** continue.
- **DISAGREE:**
  - fix the blocking findings, re-run `check`, and run the critic again;
  - carry its user questions into the review questions.
- **At most 2 rounds.** If you still disagree, present both positions at the review stop and let the user decide.
- Record each round as one line under "Review notes": verdict and what changed.
- Run the critic again on a revision only if placement, integration or step order changed.
- **No Agent tool:** say so, and run the checklist in `agents/plan-critic.md` yourself as a separate pass.

Then **stop, without writing code or editing canvases or CML**. Give the user:
- the plan path, and the placement in one or two sentences (owner contexts, outcome, integrations);
- context-map changes, if any;
- the step list, one line per step;
- the critic's verdict, plus any disagreement left for the user;
- the questions to answer: the placement confirmation if not yet given, the TDD checkpoint loop (keep it, recommended), and anything ambiguous.

Why the hard stop: placement and structure are visible and cheap to change here. Once code and canvases exist, every change is a refactoring.

## Step 6: Build, after approval

Approval comes from the user, in the conversation or by setting `status: approved` in the file. Then:

```bash
python3 .claude/skills/implementation-planner/scripts/plans.py approve <intent>   # plan approved, intent planned
python3 .claude/skills/intent-creator/scripts/intents.py build
```

Then work through the steps in order. For each step:

1. Mark it in progress: `plans.py step <intent> S<n> "in progress"`. The first one moves the plan to `in progress`.
2. Do the step through its owning skill:
   - **Canvas changes:** apply the plan's canvas diff to the context's canvas, exactly as approved.
   - **`bounded-context-scaffolder`:** invoke it with the context's canvas. Tell it that this approved intent plan (give its path) already settles the structural decisions and the TDD-loop choice. The scaffolder then writes its context plan with `Status: approved` and a review note "approved via docs/plans/<intent>.md", and doesn't ask the same questions again. It hands the rules to `ddd-tactical-building-blocks` itself.
   - **`ddd-tactical-building-blocks`:** the spec examples are the scenarios, already confirmed by the domain expert, so don't rewrite them. Its checkpoint loop stops after every test for the user. That's the point, so don't skip it.
   - **Acceptance tests:** write them yourself, from the spec examples.
3. When the step's "Done when" holds and `./mvnw test` is green, mark it done (`plans.py step <intent> S<n> done`). Fill in the files in the traceability and acceptance-test tables, and add a changelog line.

If a step reveals that the plan is wrong (a rule doesn't fit the context, a needed command is missing upstream), **stop**. Revise the plan and set it back to `draft - awaiting review`. A plan quietly diverging from the code is worse than no plan.

## Step 7: Deliver

When every step is `done` or `skipped` and the full suite is green:

```bash
./mvnw test
python3 .claude/skills/implementation-planner/scripts/plans.py done <intent>      # plan done, intent delivered
python3 .claude/skills/intent-creator/scripts/intents.py build
```

Report briefly:
- the contexts touched;
- the acceptance tests, one per example;
- context-map and canvas changes;
- anything deferred, such as candidate intents from the spec's Out of scope.
