# Lessons from building a whole system in one session

Every lesson below comes from a real run: VeloCity, eight bounded contexts, from a context map and a product vision to
612 green tests in one autonomous session (see `worked-example-velocity.md`). Each lesson says **what happened**, the
**rule** that follows, and **why**. The stage references point to where the rule is applied. Read the section for a
stage before you start it. The rules are defaults: when the project says otherwise, the project wins. Record the
deviation in the manifest.

## Contents
1. Orchestration (all stages)
2. Stage 1: canvases
3. Stage 2: intents and specs
4. Stage 3: plans and contracts
5. Stage 4: building the contexts
6. Stage 5: integration and verification
7. Known follow-up classes: what builders tend to leave behind

---

## 1. Orchestration

**O1. Work on a branch, commit once per stage, open a PR, don't merge.**
- *Happened:* the run used a new branch with one meaningful commit per stage, plus a PR at the end.
- *Rule:* create the integration branch first, commit after each stage with a message that names the stage and its
  output, and open a PR (`gh pr create`) at the end. Never merge it yourself.
- *Why:* the PR is where the human really reviews an autonomous run. One commit per stage lets the reviewer go
  through canvases, then intents and specs, then plans, then code, in the order the decisions were made.

**O2. Record the session from the verbatim prompt onwards.**
- *Happened:* `conversation.md` held the trainer's prompt verbatim and a narrated log per stage. It became the
  teaching material and the reviewer's map.
- *Rule:* always keep the session record (`assets/session-record-template.md`). Update it at the end of every
  stage, not at the end of the run, because a run that dies halfway still leaves a readable record.
- *Why:* without the record, a reviewer sees 400 changed files and no reasons.

**O3. Fake the domain expert once and centrally, before writing any artifact.**
- *Happened:* every gap and contradiction in the vision went into one file of role-played answers (DE-1 … DE-73)
  before the first canvas was written. Eight canvases, and later 35 specs written by five parallel agents, all cite
  the same answers.
- *Rule:* in the role-play setting, collect the questions first, answer them in
  `domain-expert-answers.md` with stable IDs and 🎭, and only then write artifacts. Every artifact that relies on an
  answer cites its ID, and puts it in that artifact's assumptions or decision record. With the expert in the loop,
  the same file carries **proposed** answers that the user confirms or overrides in batches.
- *Why:* parallel agents that improvise answers produce eight different systems. Central answers keep them
  consistent, and give the reviewer one place to overrule a decision and follow its references.

**O4. Expect a second round of questions, and send agents back with their context intact.**
- *Happened:* the stage-2 agents found gaps that the canvases had papered over. Claude answered them as DE-80 … DE-98
  and resumed each agent with `SendMessage`, so the agent kept its context. It also updated the canvases to match.
- *Rule:* number later rounds in a new range (DE-80+), update every artifact already written that is affected,
  and resume the same agent rather than spawning a fresh one.
- *Why:* a fresh agent rereads everything and may re-decide what was already settled. Resuming is cheaper and keeps
  the decision trail.

**O5. When a skill's check script rejects an artifact, change the artifact, not the script.**
- *Happened:* `specs.py` treats "service" as a technical word, so `fleet-manager-puts-bike-into-service` failed.
  Claude renamed the intent to `fleet-manager-adds-bike-to-fleet` in every artifact, with the event *Bike Added to
  Fleet*.
- *Rule:* never loosen a skill's validator to make your output pass. Rename or rephrase. If the script is truly
  wrong, record it as a follow-up for the user.
- *Why:* the scripts encode the flow's rules. Loosening one during an unattended run silently changes the rules
  for everyone.

**O6. Leave non-blocking open questions open.**
- *Happened:* questions such as "can an agent hand an incident to a colleague?" stayed open in the specs.
- *Rule:* fake (or ask) only what a happy path or a contract needs. Anything else stays open in the spec, and is
  listed under "what to look at first".
- *Why:* every faked answer is a decision that the reviewer must check. Don't make more of them than the build needs.

**O7. Record every approval given under the standing instruction.**
- *Happened:* each approved artifact said "approved under the trainer's standing instruction of 2026-11-08, to be
  reviewed in the PR". Statuses were moved only through the flow scripts.
- *Rule:* the autonomous stage-gate setting is allowed only when the user's prompt explicitly grants it. Then move
  statuses only through the flow scripts (`specs.py approve`, `plans.py approve|step|done`, `intents.py build`),
  and add the standing-instruction note to every artifact approved that way. Intents have no approve script:
  setting `status: accepted` is the user's acceptance, which the standing instruction grants. Say so in the
  record.
- *Why:* the flow's promise is that approval is the human's. The note keeps that promise honest: it tells the
  reviewer exactly what was not yet reviewed.

**O8. Replace dozens of critic rounds by one cross-check per stage, in the autonomous setting.**
- *Happened:* each flow skill has its own critic, and with 35 specs and 35 plans that would have meant dozens of
  critic rounds, whose questions no human could answer anyway. One independent cross-check per stage
  (`agents/stage-critic.md`) caught the real problems, such as two blocking findings in Rentals' canvas.
- *Rule:* autonomous runs get one stage critic per stage, given evidence and not conclusions. In the loop, keep the
  skills' own critics.
- *Why:* a critic is valuable when someone acts on its questions. Unattended, one broad pass gives most of the
  value at a fraction of the cost.

**O9. Treat each sub-agent's report as a claim, and verify it.**
- *Happened:* every builder's report (test counts, decisions) was checked against the code and a test run when its
  branch was merged.
- *Rule:* before merging, check out the branch, run the full suite, and spot-check the claimed rules and tests.
  Record the verified numbers, not the claimed ones.
- *Why:* agents report success in the same confident tone as failure.

**O10. Disclose every process deviation.**
- *Happened:* Rentals and Settlement wrote some TDD cycles in small batches (each batch seen failing before the code
  was written). Their context plans' TDD logs said so, and so did the record.
- *Rule:* whatever you or a builder did differently from the flow (batched cycles, a skipped critic, a stub), write
  it where the reviewer looks: the context plan and the record's "what to look at first".
- *Why:* a hidden deviation looks like conformance until it breaks.

**O11. In the loop, ask the setup questions before creating anything in git.**
- *Found* (the skill's evals): with the user in the loop, the run created the integration branch first and only then
  asked whether the old code and canvases may be removed and which integration style to use.
- *Rule:* when the user is in the loop and the prompt leaves the setup open (what may be removed, the integration
  style, the branch name or base), put those questions, each with a proposed answer, **first** in the first stop,
  ahead of the expert questions. Write the manifest, the record and the DE file as uncommitted files (they follow the
  checkout), and create the branch only after the answers.
- *Why:* the answers decide the branch's base and what the first commit contains. A branch made on a guess has to be
  redone, and the user sees a decision made before they were asked.

**O12. Give each parallel agent its own scratch space.**
- *Found* (the skill's evals): parallel agents shared the orchestrator's scratch directory. One agent's
  `summary.md` and drafts overwrote another's, and files from one run ended up in another run's outputs.
- *Rule:* every brief for agents that run in parallel names their own temporary directory (`<scratch>/<agent-name>/`)
  or says to keep temporary files inside their worktree. When you copy an agent's results, take them from its
  worktree or its own directory, never from a shared folder.
- *Why:* in a parallel build, a file clobbered by a neighbour looks like a builder's mistake and costs an
  investigation.

---

## 2. Stage 1: canvases

**C1. The orchestrator writes all the canvases itself.**
- *Happened:* Claude wrote all eight canvases instead of delegating them.
- *Rule:* don't parallelize the canvases. Write them in one hand, in the scaffolder's canvas template.
- *Why:* the canvases fix the message names (commands, events, queries) that every later artifact and parallel
  agent reuses. One author keeps one published language.

**C2. Every business decision is a strict Given/When/Then with concrete values.**
- *Rule:* no prose rules in a canvas. "Given a rider with a reservation, when they reserve a second bike, then it is
  rejected (DE-13)".
- *Why:* the tactical skill takes these examples as written. A vague rule stalls a builder or makes it guess.

**C3. Put every inbound command under a rule.**
- *Happened:* the canvas critic's first blocking finding was that Rentals listed inbound commands (Withdraw Bike,
  Make Bike Available, Signal Fault, Add Bike to Fleet) with no rules for them, although the DE answers had already
  decided their behaviour.
- *Rule:* for every inbound command and query, check that at least one business decision says what it does and when
  it is rejected.
- *Why:* a command with no rule gets no test, and the builder will invent its behaviour.

**C4. Check variants across contexts: the same command from a different actor.**
- *Happened:* the second blocking finding was that Rentals said "an expired reservation or failed unlock makes the bike
  available to anyone". That contradicted Fleet Maintenance for technicians' reservations of *withdrawn* bikes,
  which must stay withdrawn.
- *Rule:* when two contexts touch the same thing, work through each example for every actor (rider vs technician)
  and every state (available vs withdrawn).
- *Why:* cross-context contradictions are invisible inside one canvas. They show up only when you read the
  canvases against each other.

**C5. Say how one context's reasons translate into another's.**
- *Happened:* Settlement had to state how Rentals' session end reasons map onto Pricing's endings (station,
  free-floating, lock failure, maximum rental time).
- *Rule:* wherever an enumerated value crosses a boundary, the downstream canvas holds the translation table.
- *Why:* that translation is the ACL's job. If it isn't written down, every builder improvises it.

**C6. Apply map insights to the CML, validate it, and record them.**
- *Happened:* writing the canvases confirmed one relationship the reference map had left unconfirmed (Customer
  Support → Fleet Maintenance as OHS/ACL, so no cycle). It also turned two conformists into ACLs, because those
  downstreams really translate. `validate_cml.sh` stayed VALID.
- *Rule:* change the CML when the canvases prove a relationship wrong or missing. Validate it with the discovery
  skill's validator, cite the DE answer in a CML comment, and list the change for the reviewer.
- *Why:* the map drives the build order (waves) and the communication matrix. A stale map means a wrong build.

**C7. Check domain roles against the worksheet.**
- *Happened:* three domain roles didn't match the model-traits worksheet's meaning and were corrected after the
  critic's review.
- *Rule:* use the trait names the scaffolder accepts, with their worksheet meaning.

---

## 3. Stage 2: intents and specs

**S1. The orchestrator owns the story map, the slicing and the intent manifest.**
- *Happened:* Claude extended the story map with ten activities and cut the vision into 35 intents. It fixed each
  intent's **sequence number, actor, activity and task** in a working manifest before any agent started. Five area
  agents then wrote the files without colliding.
- *Rule:* write the manifest (in `system-build.md`) first. Agents get their rows and may not renumber.
- *Why:* sequence numbers are global and never reused. Parallel agents that pick their own numbers collide.

**S2. One shared list of business event names.**
- *Rule:* before the agents start, fix the event names (*Bike Reserved*, *Rental Fee Unpaid*, …). Check afterwards
  that the specs' contracts use them word for word.
- *Why:* the same event under two names becomes two integration events, or a broken listener.

**S3. Give time-driven needs a business actor.**
- *Happened:* reservation expiry became *Fleet Operations wants expired reservations released*, retries became
  *Finance wants failed charges retried*, and debt pursuit became *Collections wants debt pursued*.
- *Rule:* never write "the system does X at midnight". Name the role who wants it.
- *Why:* `intent-creator` requires a business actor, and the actor tells you whose rule it is (placement).

**S4. Group the parallel agents by area, with one shared brief.**
- *Happened:* five agents (onboarding & card, rentals, pricing & settlement, debt recovery & support, fleet
  maintenance). Each one followed `intent-creator` and `spec-writer` exactly, used the DE answers instead of `?`,
  applied the spec critic's checklist as a **separate self-review pass**, and fixed every `specs.py check` error.
- *Rule:* group the agents by area, not one per intent, so that related intents share context. Give them one brief.
- *Why:* fewer agents mean fewer dialects, and related intents need the same expert answers.

**S5. Expect the specs to find gaps the canvases papered over.**
- *Happened:* examples it found:
  - "eligibility checked only at reservation" contradicted "a blocked account cannot start new rentals";
  - does a reservation still hold at the exact second it ends?
  - is "15:01" minutes or seconds?
  - a rider's *can't unlock* report made a bike broken, but a failed unlock alone didn't;
  - a rejected incident the rider never reopens stays open forever, against "every incident ends resolved".
- *Rule:* look for these classes on purpose:
  - a rule checked at only one point of a process;
  - boundary instants (`<` vs `<=`);
  - units;
  - a report vs the event it reports;
  - states that never end.

  Answer them centrally (O4) and update the canvases.
- *Why:* examples force precision that canvases don't, so this is where the domain gets settled.

**S6. Approve through the scripts, and keep blocking vs non-blocking questions apart.**
- *Rule:* `intents.py check` and `specs.py check` at 0 errors; then `specs.py approve <intent>` per spec (autonomous:
  under the standing instruction, O7); then `intents.py build`. Non-blocking open questions stay in the specs (O6).

---

## 4. Stage 3: plans and contracts

**P1. Write the contract catalogue before any planner starts.**
- *Happened:* before planning, Claude wrote `integration-contracts.md`: every Open Host Service's resources, every
  published integration event with its payload, error semantics (409/404/400 with a body), formats (money as decimal
  strings, times as ISO instants), the shared clock and the integration style.
- *Rule:* the catalogue is the technical side of the context map, and the orchestrator writes it alone
  (`assets/integration-contracts-template.md`).
- *Why:* without one catalogue, five parallel planners invent five dialects of the same published language.

**P2. Put the shared decisions into every plan.**
- *Happened:* every plan carried the same decisions:
  - **D1** TDD checkpoint loop (off in that run);
  - **D2** state-based aggregates;
  - **D3** one ACL per outbound call;
  - **D4** in-memory repositories with `Version` added after the domain is green.
- *Rule:* decide the run-wide decisions once, number them, and have each plan cite them.
- *Why:* the scaffolder and the tactical skill take their decisions from the approved plan. Shared decisions keep
  eight builders building one system.

**P3. Fold contract gaps into one addendum.**
- *Happened:* the planners sent back about 25 gaps, for example:
  - response bodies for unlock and returns ("the rider must be told why the session ended");
  - an optional position for a bike that was never returned;
  - pause/resume bodies naming the collections agent.
- *Rule:* planners don't change the catalogue themselves. They report gaps. The orchestrator folds them into one
  dated addendum and tells the affected planners.
- *Why:* one owner keeps the published language consistent.

**P4. Do the arithmetic, and send spec bugs back to the spec.**
- *Happened:* a spec claimed a point lies "exactly on the border" of a 150 m drop zone. The planner computed
  155.7 m (haversine). Instead of planning around it, it sent the bug back. Claude moved the point to 149 m, set the
  spec back to draft, and re-approved it.
- *Rule:* recompute every numeric example (distances, fees, durations, boundary instants) during planning. A wrong
  example goes back to the spec (draft, fix, re-approve), never into a workaround.
- *Why:* that example becomes an acceptance test. A wrong number there either fails forever or bakes the bug in.

**P5. Watch for deletions that break other skills' references.**
- *Happened:* deleting the old `BikeStation` code would remove the reference implementation that the tactical and
  TDD skills pointed to. The deletion was kept (the trainer allowed it), and the skills were repointed to the new
  aggregate afterwards.
- *Rule:* before deleting code, grep `.claude/skills/` and `CLAUDE.md` files for references to it. Plan the repointing as a
  step.

**P6. Every contract in the catalogue must be built by some plan.**
- *Found* (the skill's evals): the Library catalogue promised `BookReturnedLate` with Fines as its listener, but no
  plan had a step that publishes it. Planners place spec rules, and an event added to the catalogue only for a
  downstream listener can fall between two plans. Nobody builds it, and the listener's builder waits forever.
- *Rule:* before approving the plans, run `scripts/catalogue_coverage.py <catalogue> <plans-dir>`: every OHS resource
  and every published event must appear in at least one plan. Add the missing step to the producing context's plan,
  or remove the contract from the catalogue.

**P7. Every event carries what its listener needs to act.**
- *Found* (the skill's evals): `BookReturnedLate {loanId, memberId, copyId, daysLate, returnedAt}` was to make Fines
  charge a fee, but nothing in it, or in any query Fines could call, said what to charge. The listener's builder would
  have had to invent a lookup or a field.
- *Rule:* for each published event and each of its listeners, walk the listener's rule once: which data does it need
  to decide and to act? Each piece is in the payload, in the listener's own state, or behind a query named in the
  catalogue. If none of them, decide it centrally (a payload field, a query, or a DE answer) before the plans.
- *Why:* a missing field found by a builder becomes a contract change in the middle of a parallel build, with the
  producer possibly already merged.

---

## 5. Stage 4: building the contexts

**Prep, by the orchestrator, before any builder starts.** Builders working in parallel must find the shared plumbing
already there. The baseline below is Spring/Java; translate it for another stack.

**B1. One shared clock.** All contexts read "now" from one `Clock` bean. Tests replace it with a mutable clock.
*Why:* time-driven rules (expiry, retries, overdue) across contexts must agree on "now", and tests must control it.

**B2. A REST client that resolves the application's own port at call time.**
- *Rule:* ACL adapters call the other contexts' REST resources. The base URL defaults to `http://localhost:<port>`
  and is resolved per call, because a random test port is known only after start-up. Make it overridable by
  property, for when the contexts are deployed separately.

**B3. `@EnableAsync`, with integration-event listeners as `@Async @EventListener`.**
- *Why:* events are then consumed later, on another thread, the way a broker would deliver them. That makes the
  tests honest: they keep working when the bus becomes a broker.

**B4. A switch for scheduled jobs.** Time-driven work is a use case taking "now", triggered by a `@Scheduled` job
that a property turns off in tests. Tests call the use case with the mutable clock instead.

**B5. One error body for rejections.** `409` for a broken rule, `404` for an unknown resource, `400` for bad input.
Body `{error, message}`. ACL adapters translate it into the caller's own exceptions or results.

**B6. A system test annotation.**
- *Rule:* one meta-annotation (e.g. `@<System>SpringTest`) that brings up the whole application with real beans, on
  a random port, with the mutable clock and scheduling off. Add Awaitility to the build.
- *Why:* one Spring context, cached and shared, keeps the suite fast, and the REST calls between contexts are real.

**B7. Smoke-test the risky plumbing before many agents rely on it.**
- *Happened:* a throwaway test proved a JSON REST self-call and an `@Async` listener running on another thread
  (`task-1`) before eight agents depended on them. It also caught a trap.
- *Rule:* write and run a throwaway test for each piece of plumbing that every builder will use, then delete it.

**B8. `@Async` beans are CGLIB proxies.** A test that reads such a bean's *fields* sees `null`. Assert through
methods, or through the effect in another bean. Put this in every builder's brief.

**B9. Write all published integration events up front.**
- *Happened:* every `api/events` record from the catalogue existed before wave 1. Contexts built in different waves
  compiled against each other's published language from the start.
- *Rule:* the orchestrator writes every integration-event record (primitives and strings only) during prep.
- *Why:* event-only relationships then stop ordering the build (`waves.py --event-only`). A downstream's tests
  publish the event themselves.

**B10. Remove obsolete code only when the user allowed it, and repoint its references (P5).**

**B18. Dry-run every flow script the builders will call.**
- *Found* (the skill's evals): the plans kept their steps in a shape `plans.py step` couldn't parse. Every builder
  would have failed on its first status change, in parallel, and each would have "fixed" the plan its own way.
- *Rule:* during prep, run each script the builders use (`plans.py step` and any other) once against a **copy** of
  the docs (`--dir`, `--specs`, `--intents` pointing at a temp copy), on a real step of every plan. Fix the format
  problem centrally, in the prep commit, before anyone starts.
- *Why:* a broken shared tool hit by eight agents at once costs eight diverging workarounds.

**B19. Name and qualify shared beans per system when several systems share one application.** (This includes
application-wide switches.)
- *Found* (the skill's evals): a new system's `Clock` and `RestClient` beans would sit in the same Spring application
  as another system's. Two unqualified beans of one type break every injection point of both systems, and two
  `@Primary` test clocks are just as ambiguous.
- *Rule:* in prep, look for beans of the same type in the application (`grep -rn "Clock\|RestClient" --include=*.java`).
  If there are any, give the system's beans system-specific names (`libraryClock`, `libraryRestClient`) and inject
  them with a qualifier annotation of the system (`@LibraryClock`) or `@Qualifier`. Make the test clock replace the
  system's clock by name rather than by `@Primary`. Prove it in the smoke test, with the other system's beans loaded.
- *Also found:* `@EnableAsync` lived in another system's config, so the new system's async listeners would have
  worked only by accident, and stopped the day that system was removed. Application-wide switches (`@EnableAsync`,
  `@EnableScheduling`, executors) that the system relies on are declared in its own config (they are idempotent), or
  moved to the application-wide common package with the user's consent. Never edit another system's wiring silently.

**Waves and builders.**

**B11. Build in waves, upstream first.**
- *Happened:*
  - wave 1: Payments, Pricing, Customer Support;
  - wave 2: Accounts;
  - wave 3: Rentals and Settlement;
  - wave 4: Fleet Maintenance and Debt Recovery.
- *Rule:* compute the waves with `scripts/waves.py`, passing the event-only relationships from the catalogue.
- *Why:* a context's Spring acceptance tests call its upstreams' **real** REST resources, so they must exist.

**B12. Start a downstream as soon as its upstreams are merged.**
- *Happened:* Accounts started the moment Payments was merged, while the rest of wave 1 was still running.
- *Rule:* after each merge, run `waves.py --merged <merged contexts>` and spawn whatever has become startable.
- *Why:* waves are a ceiling, not a barrier. Waiting for a whole wave wastes hours.

**B13. Agent worktrees start from the default branch.**
- *Happened:* the first builders found themselves on `master` without the stage-1–3 work, and fast-forwarded
  themselves.
- *Rule:* the first instruction of every builder brief is `git merge --ff-only <integration-branch>`, followed by
  a check that the expected plans exist.

**B14. Strict ownership makes the merges conflict-free.**
- *Happened:* every merge was conflict-free, because each builder touched only its own context's packages, its own
  context's docs and its own plans' steps.
- *Rule:* the brief names exactly what the builder may change. Anything shared (the common package, the build file,
  published events, other contexts' canvases or plans) is out of bounds: the builder reports the need and the
  orchestrator makes the change.

**B15. One cached Spring context is shared by all tests.**
- *Happened:* all Spring tests share one application context. Tests had to use unique ids and must not assume empty
  repositories. Pricing's tests published their tariffs in 2030, so other contexts kept their rides out of that
  year.
- *Rule:* unique ids per test (a `unique("bike")` helper), no global counts, and time partitions for global
  reference data that a test changes.
- *Why:* tests that pass alone and fail together are the most expensive flakiness to debug.

**B16. Each builder follows the scaffolder and tactical skills, test-first.**
- *Rule:* each builder works through its plans' steps with `bounded-context-scaffolder` (which delegates the domain
  to `ddd-tactical-building-blocks`), one Given/When/Then example per cycle. It writes the context `plan.md` with a
  per-cycle TDD log, updates its canvas, moves its plans' steps with `plans.py step`, and runs the **whole** suite
  before committing.

**B17. Verify each report and merge in order.** (O9) Merge each builder's branch into the integration branch as it
finishes. Run the full suite after each merge. Update the manifest.

---

## 6. Stage 5: integration and verification

**V1. Journey tests through the public API only.**
- *Happened:* five end-to-end journeys (a ride to money; an unpaid ride to debt recovery and payoff; a lock failure
  to maintenance and back; a fleet-issue incident resolved automatically when the bike is fixed; an invalid card
  blocking and unblocking a rider). They drove the system only through its public REST, like the riders', technicians'
  and agents' apps would. Every effect in another context was asserted with Awaitility. All five passed first time.
- *Rule:* one journey per business process from the vision or the discovery report. Only public resources, no
  repository peeking, and an Awaitility assertion for every effect in another context.
- *Why:* journeys test what the per-context acceptance tests can't: that the hand-offs compose into the process.

**V2. Prove each relationship really works: the communication matrix.**
- *Rule:* one row per message crossing a relationship, with the consumer's *effect* and the test that proves it
  (`scripts/comm_matrix.py check`). An event counts only when the consumer reacted: its state changed or it published
  its own event.
- *Also found* (the skill's evals), the three ways a matrix looks complete and isn't:
  - **variants:** `BikeWithdrawn` had three reasons, and the consumer did something different for each (register a
    broken bike, register it with another fault source, ignore it). One row hid two unproven variants. A variant that
    changes the consumer's effect gets its own row;
  - **half proofs:** a consumer's test that publishes the producer's event itself proves the listener, not the
    hand-off. The payload the producer really sends is never checked. Such a row is "consumer-only" until a test
    drives the producer through its public API (a hand-off test or a journey);
  - **"ignored" rows:** "the consumer ignores X" can't be proven by `await().untilAsserted`, which passes at once.
    Use `await().during(...)` with the state unchanged, after something that shows the event was handled.
- *Why:* "the event is published" and "the process continues" are different claims. Only the second matters.

**V3. Prove the contexts talk only through `api`, and through the agreed channel.**
- *Rule:* each context's architecture test enforces it, and a global scan of imports between contexts catches the
  context that has no architecture test. "Only `api`" is not strict enough when commands and queries go over REST:
  - a context may import another context's classes **only from its ACL packages** (`*ctxacl`);
  - and only that context's **`api.events`** records (the in-process event bus);
  - importing another context's `api.usecases` or `api.queries` is a violation: it is an in-process call that goes
    around the REST resource and the ACL translation, so the integration the map describes is never exercised.
- *Found* (the skill's evals): the scaffolder's architecture tests allowed any `..api..` package, so an in-process
  call to another context's use case passed them.
- *Why:* the REST integration is what the map and the catalogue promise. A shortcut makes the tests green and the
  map false.

**V4. Check the map against the code.** Extract the as-built relationships from the code (ACL packages, REST client
calls, event listeners) and have `discover-bounded-contexts` validate the map against them, with its critic.
*Why:* builders add calls the map never had, and the map is what the next team will trust.

**V5. Run the full suite twice.** Async tests can be flaky. A second green run on the merged branch is cheap evidence
that they aren't.

**V6. Housekeeping.** Repoint the skills' reference implementations if the code they pointed to changed (P5), and
rewrite the system's `CLAUDE.md` for the new code.

**V7. Tell the reviewer what to look at first.** In the record and the PR:
1. the role-played expert answers (every number the vision didn't give is a decision);
2. the context-map changes;
3. the process deviations (loop off, approvals under the standing instruction, batched cycles);
4. the known follow-ups (section 7);
5. the open questions left for a real expert.

---

## 7. Known follow-up classes

These are the gaps the VeloCity builders left behind. Put them on every builder's checklist (`agents/context-builder.md`)
so that each one is handled or recorded deliberately:

| Class | VeloCity case | Ask of every builder |
|---|---|---|
| First technical failure isn't retried | Settlement: if the very first call to Payments fails technically (not a declined charge), the ride stays pending with no failed attempt and is never retried | for every outbound call, what happens on a timeout or 5xx, as opposed to a business rejection? |
| A command after an event fails silently | Fleet Maintenance: a withdrawal request to Rentals that fails after a fleet-issue incident is only logged | when a listener's downstream command fails, who retries, and where is it visible? |
| Races between simultaneous commands | Rentals: two simultaneous reservations by the same rider on different bikes can both succeed | which invariant spans several aggregates, and is a race accepted, or guarded? |
| Late or out-of-order events | Debt Recovery: a "debt cleared" event dated before the recovery started must not end it | can this event arrive late, twice or out of order? |
| Double effects on retry | Settlement: retries pick up only rides with a failed attempt, so an attempt in flight is never charged twice | is every retried action idempotent? |
| Upstream accepted, local save failed (or the other way round) | Fleet Maintenance: rules checked before calling Rentals, state saved only after Rentals accepted | check your own rules first, call the upstream, then save. What if the upstream rejects? |
