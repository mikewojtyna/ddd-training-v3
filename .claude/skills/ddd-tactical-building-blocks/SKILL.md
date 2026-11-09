---
name: ddd-tactical-building-blocks
description: Create, update or refine DDD tactical building blocks in Java - aggregates (state-based or event-sourced), domain services, policies, factories, domain events, repositories and domain event publishing. It first decides which block each business rule needs (an aggregate only for a real invariant), then builds it test-first in a TDD checkpoint loop - one Given/When/Then test per cycle, then a stop to ask whether the user writes the implementation or Claude scaffolds a minimal one for confirmation - following this repo's Rentals `Bike` reference and jMolecules conventions. Use this whenever the user wants to design, add, model, implement, refactor, slim down or add a rule to an aggregate, entity, domain service, policy ("whenever X then Y"), factory, repository or event publisher inside an existing bounded context of VeloCity, CrowdSorcery or Orders, even if they never say "tactical" or "aggregate" - e.g. "a station can't hold more than 20 bikes, model that", "when an order is paid, start the delivery", "the delivery fee is free above 50 PLN", "add optimistic locking to DepositAccount", "make Reservation event-sourced". bounded-context-scaffolder hands its business rules to this skill. For creating a whole new bounded context module from a canvas, use bounded-context-scaffolder instead.
---

# DDD tactical building blocks

Builds and refines the domain layer of a bounded context: aggregates, domain services, policies, factories, domain events, repositories and event publishing. It owns all tactical design in this repo, so `bounded-context-scaffolder` delegates every business rule to it.

The reference implementation is `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bike` (the `Bike` aggregate, its `FluentLanguage`, events and rule exceptions) with its tests, e.g. `BikeReservationTest`. Read both before writing code, along with the repo's `CLAUDE.md` and the target system's `CLAUDE.md`. They set package layout and conventions and take priority over generic DDD habits.

Read these when you need them:
- `references/aggregate-templates.md`: Java shapes for state-based and event-sourced aggregates, `Version`, fluent (chained) commands, exceptions, tests, repository port and in-memory adapter. Read before writing any aggregate code.
- `references/other-building-blocks.md`: domain services, policies, factories, domain events, repositories, event publishing and the use-case orchestration around them. Read before writing any of those.

---

## Step 1: Work out what is being asked

Identify:
- **Mode**: *create* (the class doesn't exist) or *refine* (it exists: add a rule, slim it down, add versioning, convert style, and so on).
- **Where**: system and bounded context, e.g. `velocity` / `rentals`. Domain code goes into `<context>/domain/<area>/`, like `rentals/domain/bike/`. If the context package doesn't exist yet, point the user to `bounded-context-scaffolder` and stop. This skill works inside existing contexts.
- **Which building block each business rule needs.** Classify every rule before designing anything:

| Rule shape | Building block | Example |
|---|---|---|
| Something must **always** hold over state that one boundary owns, checked on every command that could break it | **Aggregate** (the rule is an invariant) | "an order total never exceeds 100 PLN" |
| **Whenever** X happens, **then** do Y, to another aggregate, another context or a port, eventually consistent | **Policy** | "when an order is paid, start the delivery" |
| A business decision or calculation from given values, owning no state of its own | **Domain service** | "delivery is free above 50 PLN, otherwise 9.99 PLN" |
| Creating something has rules of its own, or needs input from outside | **Factory** | "a new order gets a number from the daily sequence" |

Give the user a one-line reason per rule. An aggregate exists only to guard invariants, so **don't create an aggregate just to hold a policy or a calculation**: a reaction or a stateless decision has nothing to keep consistent. If a rule fits none of these or several, ask instead of guessing. A rule may also need a small change to an existing aggregate, for example a new command that a policy calls. That command then goes through the gate as its own rule.

**Requests from the scaffolder.** If the context's `plan.md` (default `docs/bounded-contexts/<system>/<context>/plan.md`) has `Status: awaiting tactical blocks`, the request comes from `bounded-context-scaffolder`. Take the rules, their Given/When/Then scenarios, any aggregate style and the **TDD checkpoint loop** choice from the plan rather than asking again. Classify the rules yourself, because the scaffolder doesn't.

Requests that only touch repositories, optimistic locking (`Version`), domain events or publishing, without a business rule, skip to Step 5.

## Step 2: The rule gate

Every building block that carries a business rule exists to enforce it. Code written without a precise rule encodes guesses rather than the domain expert's knowledge, and it can't be tested meaningfully. So before writing any domain code, collect all of the following. If something is missing, ask for it. If the user can't provide it, stop and say why.

Check the gate against the request first, before reading reference files or exploring the code: a request that stops here doesn't need them. When you stop, put every blocking gap in **one** message. Side questions (naming, placement) can go along with it, but keep them visibly separate from the blockers.

1. **Rules, 1 to 3 for this run**, whatever their building block. Small iterations keep each design decision reviewable, and the rest can come in the next run.
   - **None given**: refuse to create or change the block. Explain that it exists only to enforce rules and ask for at least one.
   - **More than 3**: stop. Ask the user to pick at most 3 for this run, and suggest which ones belong together, e.g. the invariants that guard the same state. If one plainly belongs to another aggregate (e.g. "free docks" is the station's state, not the bike's), say so as part of the suggestion. Don't silently pick 3 yourself or drop one on your own.
   - **Things that aren't rules at all**: "store the bike's colour", "show the rental history". They don't belong in the domain model. Suggest where they do belong (a read model, another context) and continue with the real rules.
   - **Rules that aren't invariants** ("send an email when a ride ends", "the fee is 9.99 PLN") aren't turned away: they become a policy or a domain service (Step 1). Say why they don't need an aggregate.

2. **A Given/When/Then scenario for every rule**, with concrete values, e.g. "Given a station with 1 bike that is already rented, When George rents a bike, Then the rental is rejected". Each scenario becomes a test, and these tests are how the user specifies behaviour. **Every rule needs at least one scenario. If any rule has none, stop and refuse to build it.** List the rules that are missing scenarios and ask for them. Don't write scenarios on the user's behalf and proceed. You may *propose* one for the user to confirm or correct, but nothing gets built until they confirm. A guessed threshold, such as "11 hours", silently buries an assumption in the domain model.
   - If a scenario is vague ("when the rental is too long"), ask for the concrete value.
   - Scenarios that show the rule holding (the happy path) are welcome. If the user gives only the violating case, suggest a happy-path one too, but don't block on it.

3. **Aggregates only: style, state-based or event-sourced.** Ask if it isn't stated. In refine mode, keep the existing style unless the user asks to convert. Both styles accept commands and return `DomainEvents`. They differ in how state is stored and rebuilt (see `references/aggregate-templates.md`).

When the gate is passed, restate the rules with their scenarios in a short numbered list, the building block each one gets and why, and the chosen style for aggregates. Then carry on to the design and the first test. Don't wait for approval at this point: the user has already given you everything. The checkpoints come in Step 4, after each test.

## Step 3: Design minimally

### Aggregates

Write down, for yourself and briefly for the user, an **invariant → state table**: which fields each invariant needs, and nothing more.

| Invariant | Command(s) that could break it | State needed to check it |
|---|---|---|
| A station never rents a bike it doesn't have | `rent(bike).by(borrower).forDuration(d)` | set of bike ids + availability |

Rules that shape the design:

- **Only state that some invariant reads.** If no invariant needs a field (name, address, description, timestamps "for later"), leave it out. Other information belongs in read models or other aggregates. In refine mode, point out fields that no invariant uses and propose removing them, but ask before deleting, because something outside the aggregate might use them.
- **State owned by another context.** If a scenario's "given" relies on information another context owns (e.g. "given a rider owing 5 PLN" when debt lives in Settlement), there are two options:
  - Keep a local copy, updated by a command that a policy triggers. The aggregate then truly guards the rule, at the cost of eventual consistency.
  - Pass the value in as a command argument. This is simpler, but the aggregate then trusts its caller.

  Default to the argument when the owning context has no code yet; otherwise pick whichever fits. Either way, list it in the summary as a decision for the user to review. Don't bury it.
- **Counterpart commands without an invariant** (e.g. `endRental` when the rule only limits *starting* rentals). Don't build them in this run: no invariant demands them. List them in the summary as candidates for the next run, because without them the state may only ever grow.
- **Sketch the final command signatures up front** from all of this run's invariants, before cycle 1, so later cycles don't reshape them. If a later invariant still forces a signature change, update the earlier tests mechanically to the new call shape in that cycle's red step, keeping their Given/When/Then meaning unchanged.
- **Names and packages.** Check the context for existing classes first (e.g. `bike.Position` is already a value object). Put the aggregate in its own `domain/<name>/` package and pick a name that doesn't clash. Identity types of other aggregates in the same context (e.g. `bike.BikeId`) can be reused as references. Don't duplicate them.
- **Small and concise.** One root, few fields, short methods. Internal entities are private static nested classes, like `BikeInStation`. If the table shows two groups of invariants that share no state, they are probably two aggregates. Say so.
- **No direct dependencies on other aggregates.** Refer to other aggregates by identity only (`BorrowerId`, not `Borrower` as an entity with behaviour). Never hold, load or call another aggregate, and never inject repositories or services into an aggregate. If an invariant seems to need another aggregate's state, either the needed value is passed in as a command argument (a value object computed by the use case, possibly with a domain service), or the rule spans aggregates and is eventually consistent, which makes it a policy. Explain which applies.
- **Commands in, events out.** Every state-changing public method is a command named in the ubiquitous language. It validates the invariants, changes state and *returns* `DomainEvents` (top-level `common/domain`). The aggregate never publishes. Violations throw a domain exception named after the broken rule, e.g. `BikeNotAvailableException` or `MaximumRentalDurationExceededException`.
- **Event-sourced commands return all pending events.** An event-sourced aggregate always keeps a private list of pending events: every event emitted since it was created or rebuilt, in order. Every command appends its events to that list and returns **the whole list**, not just its own events. So whatever the caller keeps (usually the last result) holds every new fact, however many commands ran on the instance. That stops the stored or published events from drifting away from the in-memory state. `rebuild` replays history without adding it to the pending list. Always use this approach for event-sourced aggregates, in both create and refine mode (in refine mode, convert an aggregate that returns only per-command events). See `references/aggregate-templates.md` §6.
- **Chained (fluent) commands**, like `bikeStation.rent(bike).by(borrower).forDuration(duration)`. Commands with more than one argument read as a sentence: each step is a functional interface in the package's `FluentLanguage` interface (create it if missing, reuse step types that already exist), and the last step returns `DomainEvents`. Single-argument commands stay plain: `returnBike(bike)`.
- **Optimistic locking through a `Version`, with no infrastructure dependencies.** Every aggregate ends up carrying a `Version` (record in top-level `common/domain`, created on first need), exposed through `version()`. It is the version the aggregate was loaded at. The repository compares it on save and rejects stale writes. Details for both styles are in the templates. No JPA `@Version` and no Spring in the domain. **`Version` is persistence support, not a business rule, so it is not part of the TDD cycles.** Leave it out of the design sketch, the scaffolding and the cycles, and add it together with the repository in Step 5. (In refine mode, an existing `Version` stays as it is.)

Mind the reference's size. `Bike` guards a whole lifecycle (reservation, unlock, rental session, return, withdrawal) and grew over many intents; a first aggregate for a rule or two should be much smaller. Follow its *style* (chained commands, returned `DomainEvents`, rule-named exceptions, `Version`, a comment on every field naming the invariant that needs it), not its size.

### Domain services, policies and factories

Details and shapes are in `references/other-building-blocks.md`. In short:
- **Domain service**: a stateless `@Service` class (jMolecules) in `domain/<area>/`, named in the ubiquitous language after what it decides or calculates (`DeliveryFeeCalculator`, `TechnicianAssignment`). Value objects in, a value object or decision out. It holds no state and loads, saves or publishes nothing: the use case loads the inputs and passes them in. If the calculation only reads one aggregate's own state, it's an aggregate method, not a service.
- **Policy**: "whenever event X, then command Y" in a `DomainPolicy` class with `handle(X)`, living in the `domain` package of what it acts on. It contains no invariant checks: those belong to the aggregate whose command it calls.
- **Factory**: a static factory method on the aggregate by default. Use a separate `@Factory` class only when creation has rules of its own or needs a port, such as an id generator.

### For every block

- **jMolecules annotations**: `@AggregateRoot` on the root, `@Entity` on internal entities, `@ValueObject` on value objects, `@Identity` on the id accessor, `@Service` on domain services, `@Factory` on factory classes. Records for ids, value objects and events. No Lombok, ever, because it crashes javac on JDK 27.
- No Spring annotations in the domain. Wiring happens in `infra/config`.

## Step 4: The TDD checkpoint loop, one scenario per cycle

Every rule-carrying block, whether **aggregate, domain service, policy or factory**, grows **one Given/When/Then scenario at a time**, and the user decides who writes each piece of production code. This is a training repo, so writing the implementation is often exactly the exercise the user wants to do. The loop also keeps the user's attention on the domain logic, the heart of the software, and keeps them in control of every design step. Writing it all yourself takes that away from them, and so does moving on to the next scenario before they've seen and accepted the current one. So every cycle ends at a **checkpoint where you stop and wait for the user**. That includes the first cycle, a cycle whose code looks trivial, and a run where the user has already supplied all the scenarios.

**The loop is on by default. Skipping it is possible, but strongly discouraged.** If the user asks to skip the checkpoints ("just build it", "don't stop after each test"), or the scaffolder's plan records `TDD checkpoint loop: off`, push back once, clearly, before you comply:
- they give up control over each design decision, and the domain model is the part of the system where that matters most;
- the reviewable unit grows from one scenario to the whole block, so wrong turns surface late and cost more;
- in this training repo, writing the implementation yourself is often the point of the exercise.

Recommend keeping the loop. If they still insist, respect it: still work test-first, one scenario per cycle with a red seen before each green, but don't stop between cycles. Record the opt-out in the final summary and, if there is one, in the plan, and report the full per-cycle log at the end. If the opt-out came from the plan, mention the pushback in one line and go on; the user already chose there.

The red/green rules are the ones from the repo's `tdd` skill (`.claude/skills/tdd/SKILL.md`). Read its baseline, red, green, whole-suite and refactor rules (Steps 2, 3, 5b, 6 and 9) once before cycle 1 and apply them here: baseline suite check, one test per cycle, compile-only scaffolding, fail for the right reason, the "fake it first" ladder for the smallest change, whole-suite run, light refactor. Don't invoke it as a separate skill, and don't ask its Step 0 mode question: this step's own checkpoint rules decide when to stop. This step is the domain-specific, interactive version of the same loop, and where the two differ, this step wins.

**Order of scenarios.** Go rule by rule in the order given, or in dependency order: an aggregate command a policy needs comes before the policy. Within a rule, start with the scenario that forces the least code, usually the happy path or the degenerate case, then the violating one. Show the planned order once, right after the gate restatement, so the user knows what's coming.

**Before cycle 1:** run the full suite as the baseline (`./mvnw -q test`). If it's already red, stop and report, as the `tdd` skill says.

### Each cycle

**1. Red: write exactly one test.**
- Put it in `<ClassName>Test` under `src/test/java`, in the block's package, in the style of the reference's tests (e.g. `BikeReservationTest`): the user's scenario wording with its concrete values in a `@DisplayName` text block, `// given`, `// when`, `// then` sections, a descriptive method name (not `test0`), and AssertJ assertions that can actually fail.
- Test shape per block (details in the references):
  - **aggregate**: commands on the aggregate, asserting returned events or a thrown domain exception;
  - **domain service**: call it directly with value objects and assert the decision or value it returns;
  - **policy**: in-memory repository and a recording publisher, or a fake secondary port. Given some state, when the policy handles the event, then the expected command, call or event happened;
  - **factory**: call the factory and assert the created aggregate's behaviour or events, or the rejection.
- Add only the production code needed to **compile**: the class, records, the method signature from your Step 3 sketch, and the exception type. Give new methods the dumbest body that compiles, so the test fails on its assertion rather than in setup: return `null`, zero, an empty value or `DomainEvents.empty()`. For a rejection scenario whose "given" calls the same command, that means a pass-through that changes state and returns the event without checking the rule. Never use a body that throws `UnsupportedOperationException`. Write no rule logic and no state beyond what compiling needs: that is the implementation the user may want to write.
- Run the single test and confirm it **fails for the right reason**: an assertion fails, or an expected exception isn't thrown. A compile error or a crash in "given" doesn't count, so fix the test or the scaffolding and run it again.
- **If it passes straight away** (common for a happy path), don't touch production code. Say so at the checkpoint, and offer "keep it and move on" instead of "who implements".

**2. Checkpoint A: show the test, then stop and ask.**
- **Open the test in the IDE** at the test method's line. Try, in order: an IntelliJ/JetBrains MCP tool for opening a file, if one is connected (e.g. from the `idea` server); then the `idea` launcher, `idea --line <N> <absolute path>`, which opens it in the running IDE; otherwise give the clickable `path:line`. Don't let a failed attempt block the cycle: fall through to the next option.
- **Show the test method** in the message: the code itself, plus the red you observed (the key line of the failure message) and a list of the compile-only scaffolding you added.
- **Ask with the `AskUserQuestion` tool** if it's available. Otherwise ask in plain text and end your turn. Make the question self-contained: name the test (`RiderTest#rejectsThirdActiveRental`) and its file, because the user may see only the question dialog. Offer:
  1. **"I'll write the implementation myself"**: the user codes it and tells you when it's done.
  2. **"Scaffold it for me"**: you write the minimal implementation and show it for confirmation.

  The user may also answer something else, such as changing the scenario or the test. Treat that as a change to this cycle's red step: adjust, run it again, and come back to Checkpoint A.
- **Then end your turn.** Don't write any implementation, and don't start the next test, until the user has answered.

**3a. The user writes the implementation.** When they say they're done:
- Run the block's test class, then the full suite.
- **All green:** briefly acknowledge what they built. Point out anything that breaks the Step 3 rules, such as aggregate state no invariant reads, a dependency on another aggregate, a domain service holding state, or publishing from the domain, as a suggestion rather than a change. Then go on to the next scenario's red step. Their green is their confirmation.
- **Not green:** show the failing test(s) and the key line of the failure. Don't fix their code unasked. Ask again: "I'll fix it myself" or "Fix it for me" (then follow 3b).
- If they changed a test to make it pass, point it out. Tests are the spec, so a changed test needs their explicit OK.

**3b. You scaffold the implementation.**
- Write the **smallest** change that makes the new test pass while every existing test stays green. Use the `tdd` skill's ladder (constant or hard-coded value, then one condition, then an existing field, then new state), taking new aggregate state only from the invariant → state table. Don't refactor in the same step.
- Run the block's test class, then the full suite. If another test breaks, revise the production change rather than the tests, as the `tdd` skill says.
- **Checkpoint B: show the implementation, then stop and ask.** Show the production diff (or the changed methods), the test results (`N tests, 0 failures`), and any deliberate shortcuts, such as a hard-coded threshold or an ignored parameter. Open the main changed production file in the IDE the same way as at Checkpoint A. Ask with `AskUserQuestion`: **"Looks good, next scenario"** / **"Change something"**. If you see a worthwhile refactor, offer it as a third option and describe it in one line. End your turn. Don't continue until they confirm. If they ask for changes, apply them, rerun the tests and come back to Checkpoint B.

**4. Refactor** only when the user picked it, or in code this cycle introduced, and keep the suite green. Bigger clean-ups go into the final summary as suggestions.

Commands, from the repo root:
```bash
./mvnw -q test -Dtest='<ClassName>Test#<method>'   # red check
./mvnw -q test -Dtest=<ClassName>Test               # the block's tests
./mvnw -q test                                      # full suite
```
If something fails that neither you nor the user touched, report it and don't "fix" unrelated code.

Keep a one-line log per cycle for the final summary: scenario, building block, test name, red seen, who implemented it (user or Claude), and green seen. Never claim a red you didn't observe.

When the last scenario of the run is green and confirmed, run the full suite once more and continue with Step 5 only if it is green. Persistence support (`Version`, repository, adapters) comes strictly **after** the domain: all of this run's scenarios implemented and all domain tests passing. That holds even when the user's request asked for the repository up front: acknowledge it in the gate restatement and say it comes after the domain cycles. Building persistence while the domain is still moving means reworking it every cycle, and it pulls attention away from the rules. Step 5 work isn't a scenario cycle and needs no per-scenario checkpoints, but list it in the summary.

## Step 5: Persistence and plumbing

After the domain cycles, or directly when the request is only about these, apply `references/other-building-blocks.md`. In short:

- **Version and repository**: only once every domain scenario of the run is implemented and the full suite is green (see the end of Step 4). For a **new aggregate**, then add the `Version` (templates §1 and the style's section) and a basic repository port by default (`load(id)` / `save(aggregate)`, `@Repository @SecondaryPort`, next to the aggregate) with the `Version` contract in its javadoc, and say so in the summary so the user can drop them. Build an in-memory adapter, with its optimistic-locking test, only if the user asked for one, a policy test needs it, or the scaffolder's plan lists it; otherwise suggest it. Run the full suite again afterwards: adding `Version` must not break any domain test.
- **Domain events**: past-tense records in a sealed interface per aggregate (like `BikeRentalEvent`), implementing `common.domain.DomainEvent`, carrying identities and value objects only. They appear as cycles need them.
- **Event publishing**: reuse the shared `DomainEventPublisher`. The use case does load → command → save → publish. Never add a per-context publisher.

## Step 6: Wrap up and hand over for review

- If the context's `package-info.java` has a "Business decisions" section (written by `bounded-context-scaffolder`), add or update each implemented rule there in its Given/When/Then form, naming its building block and its test, in the same change.
- **Hand back to the scaffolder.** If the context's `plan.md` has `Status: awaiting tactical blocks` and every rule in it now has a passing test, update the plan's rules table with each rule's building block and test, append this run's per-cycle log to its changelog, say so, and continue with `bounded-context-scaffolder`. It resumes from the plan and generates the deferred use cases, adapters, REST endpoints and wiring. Don't build those here. If rules from the plan are still missing (for example because of the 3-per-run limit), list them and offer the next run.
- Finish with a short summary:
  - the rules with their building block, tests and the per-cycle log (red seen, who implemented it, green seen)
  - the invariant → state table as built, for aggregates
  - files created or changed
  - whether the checkpoint loop was skipped, and at whose request
  - suggestions not acted on: repository, factory, fields proposed for removal, rules deferred to the next run
- Don't commit unless asked.
