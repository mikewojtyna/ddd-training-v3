---
name: ddd-tactical-building-blocks
description: Create, update or refine DDD tactical building blocks in Java - aggregates (state-based or event-sourced), domain events, policies, repositories, factories and domain event publishing - test-first, one business invariant at a time, following this repo's BikeStation reference aggregate and jMolecules conventions. Use this whenever the user wants to design, add, model, implement, refactor, slim down or add a rule/invariant to an aggregate, aggregate root or entity, or add a domain event, policy ("whenever X then Y"), repository, factory or event publisher inside an existing bounded context of VeloCity or CrowdSorcery, even if they never say "tactical" or "aggregate" - e.g. "a station can't hold more than 20 bikes, model that", "add optimistic locking to DepositAccount", "make Reservation event-sourced", "when a ride ends, charge the rider". For creating a whole new bounded context module from a canvas, use bounded-context-scaffolder instead.
---

# DDD tactical building blocks

Builds and refines the domain layer of a bounded context: aggregates, domain events, policies, repositories, factories and event publishing. Aggregates get the most attention because they are the hardest block to get right and the one everything else hangs off.

The reference implementation is `src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bikestation` with its test `BikeStationTest`. Read both before writing code, along with the repo's `CLAUDE.md` and the target system's `CLAUDE.md`. They set package layout and conventions and take priority over generic DDD habits.

Read these when you need them:
- `references/aggregate-templates.md`: Java shapes for state-based and event-sourced aggregates, `Version`, fluent (chained) commands, exceptions, tests, repository port and in-memory adapter. Read before writing any aggregate code.
- `references/other-building-blocks.md`: domain events, policies, repositories, factories, event publishing and the use-case orchestration around them. Read before writing any of those.

---

## Step 1: Work out what is being asked

Identify:
- **Which building block(s)**: aggregate, domain event, policy, repository, factory, event publisher.
- **Mode**: *create* (the class doesn't exist) or *refine* (it exists: add an invariant, slim it down, add versioning, convert style, and so on).
- **Where**: system and bounded context, e.g. `velocity` / `rentals`. Domain code goes into `<context>/domain/<aggregate-name-lowercase>/`, like `rentals/domain/bikestation/`. If the context package doesn't exist yet, point the user to `bounded-context-scaffolder` and stop. This skill works inside existing contexts.

If the request only touches events, policies, repositories, factories or publishing, skip to Step 5. Anything that creates or changes an aggregate goes through the aggregate gate in Step 2 first, and that includes a policy that needs a new aggregate command.

## Step 2: The aggregate gate

An aggregate is the guardian of business invariants and nothing more. Without an invariant there is nothing to guard. Code written without one encodes guesses rather than the domain expert's knowledge, and it can't be tested meaningfully. So before writing any aggregate code, collect all of the following. If something is missing, ask for it. If the user can't provide it, stop and say why.

Check the gate against the request first, before reading reference files or exploring the code: a request that stops here doesn't need them. When you stop, put every blocking gap in **one** message. Side questions (naming, placement) can go along with it, but keep them visibly separate from the blockers.

1. **Invariants, 1 to 3 for this run.** Each is a statement that must *always* be true, e.g. "the total price of an unconfirmed order cannot exceed 100 USD" or "a bike station never rents out a bike it doesn't have". Good examples are in `BikeStationTest`.
   - **None given**: refuse to create or change the aggregate. Explain that the aggregate exists only to enforce invariants and ask for at least one.
   - **More than 3**: stop. Ask the user to pick at most 3 for this run, and suggest which ones belong together, e.g. the ones that guard the same state. If one plainly guards another aggregate's state (e.g. "free docks" is the station's state, not the bike's), say so as part of the suggestion. Small iterations keep each design decision reviewable, and the rest can come in the next run. Don't silently pick 3 yourself or drop one on your own.
   - **Things that aren't invariants**: "store the bike's colour", "show the rental history", "send an email". Say they aren't invariants, so they don't belong in the aggregate. Suggest where they do belong (a read model, a policy, another context) and continue with the real invariants.

2. **A Given/When/Then scenario for every invariant**, with concrete values, e.g. "Given a station with 1 bike that is already rented, When George rents a bike, Then the rental is rejected". Each scenario becomes a test, and these tests are how the user specifies behaviour. **Every invariant needs at least one scenario. If any invariant has none, stop and refuse to build the aggregate.** List the invariants that are missing scenarios and ask for them. Don't write scenarios on the user's behalf and proceed. You may *propose* one for the user to confirm or correct, but nothing gets built until they confirm. A guessed threshold, such as "11 hours", silently buries an assumption in the domain model.
   - If a scenario is vague ("when the rental is too long"), ask for the concrete value.
   - Scenarios that show the invariant holding (the happy path) are welcome. If the user gives only the violating case, suggest a happy-path one too, but don't block on it.

3. **Style: state-based or event-sourced.** Ask if it isn't stated. In refine mode, keep the existing style unless the user asks to convert. Both styles accept commands and return `DomainEvents`. They differ in how state is stored and rebuilt (see `references/aggregate-templates.md`).

When the gate is passed, restate the invariants with their scenarios in a short numbered list, plus the chosen style, and carry on. Don't wait for approval at this point: the user has already given you everything.

## Step 3: Design the aggregate, minimally

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
- **Names and packages.** Check the context for existing classes first (e.g. `bikestation.Bike` is already a value object). Put the aggregate in its own `domain/<name>/` package and pick a name that doesn't clash. Identity types of other aggregates in the same context (e.g. `bikestation.BikeId`) can be reused as references. Don't duplicate them.
- **Small and concise.** One root, few fields, short methods. Internal entities are private static nested classes, like `BikeInStation`. If the table shows two groups of invariants that share no state, they are probably two aggregates. Say so.
- **No direct dependencies on other aggregates.** Refer to other aggregates by identity only (`BorrowerId`, not `Borrower` as an entity with behaviour). Never hold, load or call another aggregate, and never inject repositories or services into an aggregate. If an invariant seems to need another aggregate's state, either the needed value is passed in as a command argument (a value object computed by the use case), or the rule spans aggregates and is eventually consistent, which makes it a policy (see `references/other-building-blocks.md`). Explain which applies.
- **Commands in, events out.** Every state-changing public method is a command named in the ubiquitous language. It validates the invariants, changes state and *returns* `DomainEvents` (top-level `common/domain`). The aggregate never publishes. Violations throw a domain exception named after the broken rule, e.g. `BikeNotAvailableException` or `MaximumRentalDurationExceededException`.
- **Event-sourced commands return all pending events.** An event-sourced aggregate always keeps a private list of pending events: every event emitted since it was created or rebuilt, in order. Every command appends its events to that list and returns **the whole list**, not just its own events. So whatever the caller keeps (usually the last result) holds every new fact, however many commands ran on the instance. That stops the stored or published events from drifting away from the in-memory state. `rebuild` replays history without adding it to the pending list. Always use this approach for event-sourced aggregates, in both create and refine mode (in refine mode, convert an aggregate that returns only per-command events). See `references/aggregate-templates.md` §6.
- **Chained (fluent) commands**, like `bikeStation.rent(bike).by(borrower).forDuration(duration)`. Commands with more than one argument read as a sentence: each step is a functional interface in the package's `FluentLanguage` interface (create it if missing, reuse step types that already exist), and the last step returns `DomainEvents`. Single-argument commands stay plain: `returnBike(bike)`.
- **Optimistic locking through a `Version`, with no infrastructure dependencies.** Every aggregate carries a `Version` (record in top-level `common/domain`, created on first need), exposed through `version()`. It is the version the aggregate was loaded at. The repository compares it on save and rejects stale writes. Details for both styles are in the templates. No JPA `@Version` and no Spring in the domain.
- **jMolecules annotations**: `@AggregateRoot` on the root, `@Entity` on internal entities, `@ValueObject` on value objects, `@Identity` on the id accessor. Records for ids, value objects and events. No Lombok, ever, because it crashes javac on JDK 27.

Don't copy the reference's known gaps. In `BikeStation`, `rent(bike)` ignores its `bike` argument and picks any available bike, there's no `@Identity` or `Version`, and `BikeStationTest#test0` calls `hasAtLeastOneEventMatching` without asserting the result. Follow its *style*, not those slips.

## Step 4: TDD, one invariant at a time

Take the invariants in the order given, or in dependency order if one builds on another. Run a full cycle for each before starting the next. This keeps every piece of state traceable to the invariant that demanded it.

For each invariant:

1. **Red.** Write the test(s) for its scenarios in `src/test/java/...`, in the aggregate's package and in the `BikeStationTest` style: the scenario in a `@DisplayName` text block, `// given`, `// when`, `// then` sections, and a descriptive method name (not `test0`). Add only the production code needed to compile and run the scenario *without* the rule. For a new command, that is a pass-through that changes state and returns the event without checking the invariant. Don't use a body that throws `UnsupportedOperationException`: "given" steps often call the same command, so the test would fail before reaching "when", for the wrong reason. Run the test class and confirm it **fails for the right reason**: the rejection scenario fails because the expected exception wasn't thrown, or an assertion fails. A compile error or a stub exception doesn't count. Only the scenarios that violate the invariant are expected to go red. A happy-path scenario passing against the pass-through is fine; record it as "green from the start".
2. **Green.** Write the simplest code that passes, adding only state from the invariant → state table. Run the whole test class: the new tests and every earlier one must pass.
3. **Refactor.** Remove duplication and unused state, sharpen names toward the ubiquitous language, apply the chained-command shape, and check the rules from Step 3. Run the tests again.

Run tests from the repo root:
```bash
./mvnw -q test -Dtest=<AggregateName>Test
```
Run all of them once at the end: `./mvnw -q test`. If something fails that you didn't touch, report it and don't "fix" unrelated code.

Keep a one-line log per cycle (invariant, test names, red seen, green seen) for the final summary. If you couldn't observe a red, for instance because the rule already held, say so instead of claiming a red.

## Step 5: Supporting building blocks

After the aggregate cycles, or directly when the request is only about these, apply `references/other-building-blocks.md`. In short:

- **Repository**: for a **new aggregate**, create a basic repository port by default (`load(id)` / `save(aggregate)`, `@Repository @SecondaryPort`, next to the aggregate) with the `Version` contract in its javadoc, and say so in the summary so the user can drop it. Build an in-memory adapter only if the user asked for one; otherwise suggest it.
- **Factory**: suggest one only when creation itself has rules or needs input from outside the aggregate. Otherwise a constructor or static factory method is enough.
- **Domain events**: past-tense records in a sealed interface per aggregate (like `BikeRentalEvent`), implementing `common.domain.DomainEvent`, carrying identities and value objects only.
- **Policies**: "whenever event X, then command Y". A `DomainPolicy` class with `handle(X)`.
- **Event publishing**: reuse the shared `DomainEventPublisher`. The use case does load → command → save → publish. Never add a per-context publisher.

## Step 6: Wrap up and hand over for review

- If the context's `package-info.java` has a "Business decisions" section (written by `bounded-context-scaffolder`), add or update each implemented invariant there in its Given/When/Then form, naming its test, in the same change.
- Finish with a short summary:
  - the invariants with their tests and the red → green log
  - the invariant → state table as built
  - files created or changed
  - suggestions not acted on: repository, factory, fields proposed for removal, invariants deferred to the next run, rules that turned out to be policies
- Don't commit unless asked.
