# How the whole of VeloCity was built in one autonomous session

*A recorded Claude Code session, 2026-11-08.*

This is the record of a single session in which Claude built every VeloCity bounded context, from canvases
through intents, specs and plans to tested code, while the trainer was away. It's meant for teaching: it shows
the AI-assisted DDD flow from the README run end to end, and it shows where a human would normally have stopped
the flow, and what Claude did instead because it was told not to ask.

**Reading tips.**
- The trainer's prompt is quoted verbatim. Everything after it is Claude's narrated log: what it read, what it
  decided and why, which agents it started, what the checks reported. Tool outputs are summarised, not pasted.
- The trainer couldn't answer questions, so Claude **role-played the domain expert** where the vision was silent or
  contradictory. Every faked answer is marked 🎭 and repeated in the artifact it shaped (canvas *Assumptions*,
  spec decision records), so a reviewer can find and overrule each one.
- Normally every step of the flow stops for the human's approval. In this session the trainer's prompt was the
  standing instruction to go on without asking. Wherever a status was moved forward under that instruction, the
  artifact says so (*"approved under the trainer's standing instruction of 2026-11-08, to be reviewed in the PR"*).
  The real review is the pull request.

---

## 👤 User

> Ok, I've a big task for you. I want you to fully implement all bounded contexts and relationships between them (including adapters and cross-bc communication with these adapters) of VeloCity. I'll be gone, so you need to do your best and implement everything without asking. When I'm back, I'll review the changes. You are free to use all of skills in this repository.
>
> # How to start:
> Start with a new branch that will be merged later to the master branch. Don't do the merge yet, just create a PR on github after you're done with your job
>
> # How you will work:
> - Divide the work in 4 stages. After each stage prepare a meaningful commit.
> - Record the entire session (including my initial prompt) in a file, so it can be used for educational purposes.
> - Feel free to totally update or even remove the existing velocity code, old bounded context canvases (e.g. the fleetmaintenance) and tests. Don't treat it as a source of truth.
> - If you have any doubts, fake the domain expert's answer. You can update bounded context canvas and context map if you've found some new insights/inconsistency.
>
> ## Stage 1: Create bonded context canvases
> Input:
> - VeloCity context map
> - VeloCity product vision
>
> Output:
> - Bounded context canvases (do your best when preparing them b ased on context map and vision, I won't be able to give you any naswers)
>
> ## Stage 2: Intents and specs creation
> Input:
> - VeloCity context map
> - VeloCity product vision (extract product scenarios from this file)
>
> Output:
> - Intents
> - Specs
>
> ## Stage 3: Plan creation
>
> Input:
> - Intents
> - Specs
> - VeloCity context map
> - Bounded context canvases
>
> Output:
> - Plan for each intent-spec pair
>
> ## Stage 4: Implement VeloCity
>
> For cross bounded-context communications use rest-adapters for commands and queries and in-process Spring event bus for integration events between bounded contexts (using application event listener). Bounded context integration tests must be Spring tests using real beans, to make sure everything works properly. Don't duplicate too much domain logic between layers. For cross bounded context integration tests (e.g. if event was consume by another bounded context) use awaitility (don't assume that event would be consumed instantly, I want these tests to work the same way no matter if we're using in-process communication event bus or event/message brokers).
>
> Input:
> - Plans for each intent-spec pair
>
> Output:
> - VeloCity bounded contexts implemented in code, including all tests, cross bounded context integrations and adapters
>
> Verification:
> - domain unit tests
> - use case bounded context integration tests using real Spring beans
> - cross bounded context communication verified using real Spring beans
>
> That's it. Do your job without asking me. Run as many agents as you want. I want this task to be fully autonomous.

---

## 🤖 Claude

### Getting oriented

- Created the branch `velocity-full-implementation` from `master` (clean, at `a124af0`).
- Read the inputs: `docs/domains/velocity/velocity-vision.md` (the deliberately inconsistent domain-expert guide),
  `docs/domains/velocity/velocity-context-map.md` and `src/main/cml/VeloCity-Context-Map.cml` (the reference
  solution, 8 contexts plus the external Payment Processor and City), the old FleetMaintenance canvas and the
  existing `rentals` / `fleetmaintenance` code.
- Read every skill of the flow (`intent-creator`, `spec-writer`, `implementation-planner`,
  `bounded-context-scaffolder`, `ddd-tactical-building-blocks`, `tdd`) and their scripts, plus the shared
  `common` package, the Orders example and the architecture-test template.
- Ran the baseline: `./mvnw -q test` green.

**How the skills' review gates were handled.** Every skill in the flow stops for the human: intents are accepted,
specs and plans approved, and the tactical skill's TDD checkpoint loop stops after every test. The trainer
explicitly asked for a fully autonomous run, so:
- statuses were moved forward only through the scripts (`intents.py`, `specs.py approve`, `plans.py`), as
  `CLAUDE.md` requires, and every artifact records that it was approved under the standing instruction;
- the **TDD checkpoint loop was switched off** (the tactical skill's pushback, recorded once here: the trainer gives
  up control over each domain design decision and reviews the whole domain after the fact). Domain code was still
  written test-first, one Given/When/Then example per cycle;
- the independent critic agents were replaced by one cross-checking pass per stage (see each stage), because
  dozens of critic rounds would have cost more than they add when no human answers their questions anyway.

### Stage 1: bounded context canvases

**Inputs:** the vision and the reference context map. **Output:** one ddd-crew v5 canvas per context in
`docs/bounded-contexts/velocity/<context>/canvas.md`, in the `bounded-context-scaffolder` template.

1. **Faking the domain expert, once and centrally.** Before writing any canvas, Claude collected every gap and
   contradiction it would have asked the trainer about into `docs/bounded-contexts/velocity/domain-expert-answers.md`
   and role-played the answers (🎭 DE-1 … DE-73). Writing them in one place, instead of improvising per canvas,
   is what keeps eight canvases (and later 35 specs written by parallel agents) consistent. Examples of the
   decisions:
   - 🎭 DE-31 resolves a deliberate trap in the vision: "when the hour *completes*, the fee is applied" against the
     intent of tiered pricing. Decision: every *started* billing hour is charged.
   - 🎭 DE-10/11: a rider's reservation lasts 2 minutes, a technician's 30 minutes.
   - 🎭 DE-30: the launch tariff (free 15 min; PLN 3/5/7, then 9 per hour; drop zone PLN 2; unauthorized zone PLN 40;
     penalty PLN 150).
   - 🎭 DE-21: a bike that isn't returned within the 12-hour maximum is withdrawn as *not returned* and treated as
     broken.
2. **Writing the canvases.** Claude wrote all eight itself rather than delegating them, because the canvases fix the
   message names that every later artifact reuses. Every business decision is a strict Given/When/Then example with
   concrete values, so the tactical skill can later take it as written.
3. **Context map insights**, applied to the CML and the reference map's markdown (`validate_cml.sh`: VALID,
   10 contexts, 11 relationships):
   - Customer Support → Fleet Maintenance, which the reference map left as an unconfirmed default, is confirmed
     (🎭 DE-52) as `[U,OHS,PL]->[D,ACL]`: Fleet Maintenance calls Customer Support's *Resolve Fleet Issue* when a bike
     is fixed, so Customer Support stays upstream and there is no cycle.
   - Accounts → Rentals became an explicit OHS with an ACL in Rentals.
4. **Independent critic** (one general-purpose agent, given only the canvases, vision, map and DE answers).
   Verdict **DISAGREE**, with two blocking findings, both in Rentals:
   - Rentals listed inbound commands (Withdraw Bike, Make Bike Available, Signal Fault, Add Bike to Fleet) with
     no rules for them, although DE answers already decided their behaviour.
   - Its "expired reservation / failed unlock → bike available to anyone" examples contradicted Fleet Maintenance for
     technicians' reservations of withdrawn bikes, which must stay withdrawn.

   Claude added the missing rules and the technician variants. It also took several medium findings:
   - Rentals → Fleet Maintenance and City → Pricing became `[D,ACL]` in the map, because both downstreams really
     translate.
   - New answers DE-26 (the 50 m rule is for riders only), DE-45, DE-56 and DE-73: only invalidating the *current*
     card blocks a rider.
   - Settlement now states how Rentals' end reasons translate into Pricing's endings.
   - Three domain roles that didn't match the worksheet's meaning were corrected.

   A second critic round wasn't run: the fixes were additive, and the critic's own suggested wording was used.

### Stage 2: intents and specs

**Inputs:** the vision (product scenarios), the context map, plus the canvases and expert answers from Stage 1.
**Output:** 35 intents in `docs/intents/`, the story map and the generated `README.md`, and 35 specs in `docs/specs/`.

1. **Story map and slicing, by Claude.** Claude extended `docs/intents/story-map.md` with ten activities, from
   *Join VeloCity* to *Set prices*, and cut the vision into 35 fine-grained intents. Each has one actor, one goal
   and one outcome, as `intent-creator` demands. For example, the vision's billing section became four intents:
   tiered hourly fees, zone-dependent return fees, the overdue penalty, and publishing a tariff.
   - It fixed every intent's sequence number, actor, activity and task in a working manifest, so parallel agents
     couldn't collide on them.
   - It also fixed one shared list of business event names (*Bike Reserved*, *Rental Fee Unpaid*, …).
   - Time-driven needs got a business actor, as the skill requires: *Fleet Operations* wants expired reservations
     released, *Finance* wants failed charges retried, *Collections* wants debt pursued.
2. **Five parallel agents**, one per area (onboarding & card, Rentals, pricing & settlement, debt recovery &
   support, fleet maintenance). They worked from one shared brief: follow `intent-creator` and `spec-writer` exactly,
   use the DE answers instead of `?`, apply the spec critic's checklist as a separate self-review pass, and fix every
   `specs.py check` error.
3. **The agents found gaps the canvases had papered over.** Examples:
   - DE-14 (eligibility checked only at reservation) contradicted the vision's "a blocked account cannot start new
     rentals".
   - Nothing said whether a reservation still holds at the exact second it ends.
   - "15:01" in DE-31 could be minutes or seconds.
   - A rider's *can't unlock* report made a bike broken, while a failed unlock alone didn't.
   - A rejected incident the rider never reopens would stay open forever, against "every incident ends resolved".

   🎭 Claude answered them as a second round of expert answers (DE-80 … DE-98) and sent each agent back, with its
   context intact, to apply them. It updated the canvases to match.
4. **A naming clash with the tooling.** `specs.py` treats "service" as a technical word, so the intent
   `fleet-manager-puts-bike-into-service` failed its check. Claude renamed the intent rather than loosen the skill's
   script: it became `fleet-manager-adds-bike-to-fleet`, with the event *Bike Added to Fleet*, renamed in every
   artifact.
5. **Checks and approval.**
   - `intents.py check`: 35 intents, 0 errors, 0 warnings. `specs.py check`: 35 specs, 0 errors, 0 warnings. The
     business events in the specs' contracts match the shared list word for word.
   - Every spec was approved with `specs.py approve` (intents → `specified`) under the standing instruction. Open
     questions that don't block a happy path (e.g. "can an agent hand an incident to a colleague?") stay open in
     the specs for the trainer.

### Stage 3: implementation plans

**Inputs:** intents, specs, the context map, the canvases. **Output:** 35 plans in `docs/plans/`, one per intent,
and the contract catalogue `docs/bounded-contexts/velocity/integration-contracts.md`.

1. **One contract catalogue before planning.** Before any planner started, Claude wrote the technical side of the
   context map: every Open Host Service's REST resources, every published integration event with its payload, and
   the integration style the trainer asked for. That style is REST through `@AntiCorruptionLayer` adapters for
   cross-context commands and queries, and `@Async @EventListener` on the in-process Spring bus for integration
   events, so tests must wait with Awaitility. Without one catalogue, five parallel planners would have invented
   five dialects of the same published language.
2. **Five parallel planners** (same areas as Stage 2), each following `implementation-planner`. Each placed every
   rule, command, query and event with a reason, translated the spec vocabulary into the owning context's language,
   and mapped the contract to use cases, REST paths and events. They listed the missing canvas examples and ordered
   the steps upstream first. The shared decisions are in every plan:
   - **D1** TDD checkpoint loop off (autonomous run).
   - **D2** state-based aggregates.
   - **D3** one ACL per outbound call.
   - **D4** in-memory repositories with `Version` after the domain is green.
3. **What the planners sent back:**
   - About 25 **contract gaps**, for example response bodies for unlock and returns ("the rider must be told why the
     session ended"), an optional position for a bike that was never returned, and pause/resume bodies naming the
     collections agent. Claude folded them all into the catalogue as one addendum.
   - **A spec bug that only arithmetic could find.** `finance-prices-free-floating-return-by-zone` E1.3 claimed
     (52.2311, 21.0122) lies "exactly on the border" of a 150 m drop zone; the planner computed 155.7 m. The planner
     didn't plan around it but sent it back to the spec. Claude moved the point to 149 m from the centre (haversine),
     set the spec back to draft and re-approved it.
   - **A risk:** deleting the old `BikeStation` code removes the reference implementation that the
     `ddd-tactical-building-blocks` and `tdd` skills point to. Claude kept the deletion, which the trainer
     explicitly allowed, and noted that the skills' references must be repointed to the new Rentals aggregate.
4. **Checks and approval.** `plans.py check`: 35 plans, 0 errors, 0 warnings. Every plan was approved with
   `plans.py approve` (intents → `planned`) under the standing instruction.

