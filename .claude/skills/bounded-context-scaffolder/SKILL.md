---
name: bounded-context-scaffolder
description: Scaffold a new bounded context, or update an existing one, from a Bounded Context Canvas (ddd-crew v5) into a ports-and-adapters Java module with jMolecules annotations, records, ACLs, aggregates and Given/When/Then tests, plus a plan markdown file with a changelog. Use this whenever the user wants to create, add, generate, scaffold, bootstrap or update a bounded context, a context module, or a new subpackage for a system such as VeloCity or CrowdSorcery. Also use it when they share a bounded context canvas (a markdown file or pasted text) and want code from it, or say things like "add the Pricing context", "turn this canvas into code", or "the Settlement canvas changed, update the module", even if they never say "scaffold". Also use it to resume from a saved bounded-context plan: "I reviewed the Pricing plan, go ahead", "plan approved", "I edited plan.md, continue".
---

# Bounded Context Scaffolder

Turns a Bounded Context Canvas into a module that follows this repository's ports-and-adapters conventions. It covers two modes with the same rules:

- **Scaffold mode**: the context package doesn't exist yet. Create it.
- **Update mode**: the context package already exists. Change it to match the canvas, and record what changed.

The skill always produces two artifacts:
1. **A plan markdown file** that maps every canvas element to code. In update mode it also has a changelog.
2. **The module**: the bounded context package, its tests and Spring wiring.

The canvas is the input contract. An empty context has no value: a package with nothing in it teaches nothing and decides nothing. So don't scaffold without a canvas that has real content.

Read these files when you need them:
- `references/canvas-mapping.md`: how each canvas section turns into packages, classes and javadoc. Read it before writing the plan.
- `references/code-templates.md`: Java shapes for every element type. Read it before generating code.
- `assets/canvas-template.md`: the input template to hand to the user.
- `assets/plan-template.md`: the structure of the plan file.

Also read the project's `CLAUDE.md` and the target system's `CLAUDE.md` (for example `src/main/java/pl/wojtyna/dddtrainingv3/velocity/CLAUDE.md`). They define the package layout and the system-specific conventions, and they take priority over generic DDD habits.

---

## Step 0: Look for an existing plan, and resume from it

The work happens in two sessions with a review in between, so the plan file is the hand-off point. Before anything else, check the default plan location (below), or a plan path the user gives you. Look at its `Status` line:

- **No plan:** start at Step 1.
- **`draft - awaiting review`:** a previous run wrote the plan and stopped for review. Don't rebuild it from scratch. Read it, then collect the user's feedback: from this conversation, from edits or comments they made in the plan file itself, and from changes to the canvas since the plan's date. Revise the plan, record what changed under "Review notes", and then:
  - if the user has already approved it, go to Step 6
  - otherwise stop at Step 4 again
- **`approved`:** the user approved the plan but the code isn't generated yet. Check that the canvas hasn't changed since. If it has, revise the plan and ask again. Otherwise go to Step 6 and implement **the plan as approved**. Don't redesign it while generating code.
- **`implemented`:** the context exists. A new request means update mode. Add a new changelog entry, set the status back to `draft - awaiting review`, and go through the review gate again.

## Step 1: Get the canvas

Find out what input the user gave:

- **A markdown file path.** This is the preferred input. Read it.
- **Inline text.** Accept it, but recommend saving it as a markdown file first. A file gives the canvas a structure, room for detail, and history in git, and it becomes the reference the plan points to. Offer to save the text into the template structure at the default location (below). If the user declines, go on with the inline text and note "source: inline" in the plan.
- **Nothing, or only a name** ("scaffold a Pricing context"). Stop. Explain that the skill builds from a canvas, then copy `assets/canvas-template.md` to the default location, pre-filled with whatever you already know (the name, and terms from the system's vision or context-map docs if they exist). Ask the user to fill it in. Don't generate code yet. A guessed context would encode your assumptions, not their domain knowledge.

Default locations, which the user can override:
- Canvas: `docs/bounded-contexts/<system>/<context-package>/canvas.md`
- Plan: `docs/bounded-contexts/<system>/<context-package>/plan.md`

## Step 2: Validate the canvas

Check it in this order of importance.

**Required. Stop and ask if any of these is missing:**
- **Name.** It becomes `@BoundedContext(name = "...")`. For VeloCity, check it against the `BoundedContext` names in `src/main/cml/VeloCity-*.cml`. If the canvas name differs from the CML name, ask which one is right.
- **Purpose.** The business benefit, not a technical description.
- **Ubiquitous language.** At least one domain term **with a definition**. Terms become class names, so the definitions matter.

**Important, used when present:**
- **Strategic classification.** Domain: core / supporting / generic. Business model: revenue / engagement / compliance / cost reduction. Evolution: genesis / custom built / product / commodity. Flag any value outside these lists and ask about it.
- **Domain roles.** These must be traits from `docs/model-traits-worksheet.md`: Specification/Draft Model, Execution Model, Analysis/Audit Model, Approver, Enforcer, Octopus Enforcer, Interchanger, Gateway, Gateway Interchange, Dogfood Context, Bubble Context, Autonomous Bubble, Brain Context, Funnel Context, Engagement Context. Map the canvas's short forms ("execution context") to the worksheet names. If a role isn't on the list, ask the user to pick the closest trait or confirm a custom one. If the role is **Brain Context**, warn the user: the worksheet marks it as a likely anti-pattern.
- **Inbound and outbound communication.** Each message needs a collaborator and a type (query, command or event).
- **Business decisions.** See the next section.

**Least important, copied into the javadoc as written:** assumptions, verification metrics, open questions.

### Business rules must be strict Given/When/Then

Aggregates are designed from business rules, and a rule only becomes an aggregate invariant if it is precise enough to test. For every business decision in the canvas:

- **Written as a Given/When/Then example with concrete values** (for example "given a station with 0 available bikes, when a rider unlocks a bike, then the unlock is rejected"): use it to design aggregates, and it becomes a test.
- **Vague or prose-only** ("rentals can't be too long"): don't build an aggregate from it. Suggest a strict GWT version with concrete values and ask the user to confirm it. Until they do, list the rule in the plan under "Rules awaiting GWT" and keep it in the javadoc's business decisions, but don't model it in code. Guessing a threshold such as "11 hours" silently would bury an assumption in the domain model.

If there are no strict rules at all, scaffold the ports and adapters without aggregates and say so plainly in the plan.

### Check the canvas for internal consistency

Work through every GWT example by hand, using the ubiquitous-language definitions. Canvases are written by people under time pressure, and an example that contradicts its own definitions is common. For example, a "billing period = each started hour" definition can't be squared with a fee that counts only four periods for a ride that has five. When you find a contradiction:
- don't pick one side silently, and don't write a test that hard-codes either reading
- implement what the *undisputed* rules and definitions require
- list the conflicting example under "Rules awaiting GWT", with both readings and the arithmetic, and ask the user which one is wrong

### Compare with the reference context map (VeloCity)

`velocity/velocity-context-map.md` and `src/main/cml/VeloCity-*.cml` are the training's reference solution. Compare the canvas with them: the context name, the inbound and outbound relationships, the message names, and terms that mean the same thing ("tariff" against "price list"). Build what the **canvas** says, because it's the user's current thinking. List every difference in the plan under "Differences from the reference context map", and say whether the map's "Accepted Alternatives" section covers it. Don't change the reference map unless the user asks.

## Step 3: Make the structural decisions, and push back where the skill says to

Settle these before writing the plan. Each one ends up in the plan's "Decisions" section with its reason.

1. **System.** Which system package (`velocity`, `crowdsorcery`, or a new one) gets the context? If the canvas or the conversation doesn't make it obvious, ask.
2. **Package name.** Lowercase, no separators, derived from the name ("Debt Recovery" → `debtrecovery`). In update mode, use the existing package.
3. **One module per context (default).** A bounded context maps 1:1 to a module: one `package-info.java` with `@BoundedContext` and `@Module`. If the user asks for several modules, first ask whether those modules are really separate bounded contexts. If each would map 1:1 to its own context, suggest splitting the canvas into several canvases instead. If they still want several modules in one context, do it. They know their domain. Put the modules in subpackages under the context root, each with its own `package-info.java` carrying `@Module` only (the `@BoundedContext` stays on the root). Record the decision.
4. **ACL for every outbound query or command (default).** Each one gets a secondary port named in *this* context's language, and an adapter inside an `@AntiCorruptionLayer` package that translates to the collaborator's language. That way the collaborator's model can't leak into this one. If the user says explicitly that they don't want an ACL (typically a Conformist relationship), push back once and explain the cost: upstream changes ripple straight into this domain model, and the ubiquitous language gets mixed. Suggest the ACL again. If they insist, go without it, and record it in the plan as a conscious decision with that risk spelled out.
5. **Mode.** If `src/main/java/pl/wojtyna/dddtrainingv3/<system>/<package>/` exists, use update mode (Step 5).

## Step 4: Save the plan, then stop for review

Write the plan file using `assets/plan-template.md`, with `Status: draft - awaiting review`. It's a traceability document: every canvas element points to the file that implements it, and every generated file points back to the canvas element that justifies it.

Then **stop, and don't generate code in this turn**. Give the user:
- the plan's path
- a short summary: counts of use cases, queries, events, ports, ACLs, aggregates and tests
- the decisions you made and the pushbacks you gave
- the questions you need answered (rules awaiting GWT, contradictions, differences from the context map)

Ask them to review the plan. They can reply in the conversation or edit the plan file directly. Tell them that invoking the skill again picks up from the saved plan.

Why the hard stop: the plan is where the domain decisions become visible (aggregate boundaries, ACLs, which rules are modelled). Reviewing them there costs minutes. Reviewing them later means picking apart generated code. The stop applies even if the request sounded urgent, and even if no one can answer right away. A saved draft waits safely, and generated code built on an unreviewed plan doesn't.

Go on to Step 5 or 6 only when the user explicitly approves *this* plan, in the conversation or by setting `Status: approved` in the file. When you go on, set the status to `approved` and record the approval and any last changes under "Review notes".

## Step 5 (update mode only): Compare with the existing code before changing it

1. Read the whole existing context package and its tests.
2. Build a diff between the canvas and the code:
   - **Added**: in the canvas, not in the code.
   - **Changed**: renamed terms, changed messages, changed rules.
   - **Removed**: in the code, not in the canvas.
   - **Untouched.**
3. Apply additions and changes, and keep the style already in the code. For example, if Rentals uses a curried fluent API, new commands in Rentals use it too.
4. **Never delete existing code on your own.** Hand-written code and workshop stubs (for example `UnsupportedOperationException("implement this")`) can be deliberate. List the proposed removals in the plan and remove them only after the user confirms.
5. Don't convert existing classes to records unless the user asks. Records are the rule for code you create.
6. Add a dated entry to the plan's **Changelog**: what changed, which canvas change caused it, and which files it touched. If a plan file already exists, keep it and add to it. Its history is the point.

## Step 6: Generate the module

Follow `references/canvas-mapping.md` and `references/code-templates.md`. The key rules, with the reasons behind them:

- **Records instead of classes with getters** for value objects, identifiers, commands, queries, query results, domain events, integration events and REST request and response bodies. Records state intent, are immutable, and spare you hand-written accessors and `equals`/`hashCode`. Lombok isn't available here, because it crashes javac on JDK 27.
- **Aggregate roots and entities stay classes.** They protect invariants over changing state. Expose behaviour (methods that return `DomainEvents`), not getters. The only accessor is the identity, annotated with `@Identity`. An entity inside an aggregate that never changes after creation can be a record.
- **jMolecules annotations on everything**: `@AggregateRoot`, `@Entity`, `@ValueObject`, `@Identity`, `@Repository`, `@PrimaryPort`, `@SecondaryPort`, `@PrimaryAdapter`, `@SecondaryAdapter`. Only `jmolecules-ddd` and `jmolecules-hexagonal-architecture` are on the classpath, so `@DomainEvent` (from `jmolecules-events`) isn't available. Domain events are marked by implementing `common.domain.DomainEvent` instead.
- **Reuse the top-level `common`**: `DomainEvents`, `DomainEvent`, `DomainEventPublisher`, `DomainPolicy`, `IntegrationEvent`, `@AntiCorruptionLayer`. Never duplicate them.
- **No Spring in `domain/` or `api/`.** All wiring goes in `infra/config/<Context>ContextConfig`. Give bean methods context-prefixed names (for example `pricingTariffRepository`), because bean names are global and Spring Boot fails on duplicates.
- **Tests for every strict GWT rule**, in `src/test/java/...` in the same package as the aggregate. Use the `BikeStationTest` style: the scenario in a `@DisplayName` text block, and `// given`, `// when`, `// then` sections. Implement the aggregate so these tests pass. The rules are precise, so make them real.
- **The `package-info.java` javadoc follows the rules in the code.** Its "Business decisions" section is the readable form of the rules the module actually enforces. Each rule that is implemented appears in its Given/When/Then form and names its test (for example "see `PriceListTest#freeRide`"). Each rule that isn't implemented yet is marked *not modelled yet* and points to the plan. Whenever a change adds, changes or removes a rule or its test, update this section **in the same change**, along with the plan's rule tables. This applies in update mode and when a rule moves from "awaiting GWT" to implemented. If the javadoc says something the tests don't enforce, it misleads the next reader.
- **Stubs are allowed only at the edges**: ACL adapters to contexts that don't exist in code yet, and REST endpoints the canvas doesn't describe. They throw `UnsupportedOperationException("implement this")` and carry a comment naming the canvas message. In-memory repositories are implemented, not stubbed.

## Step 7: Verify

```bash
./mvnw compile
./mvnw test -Dtest='<NewOrChangedTestClasses>'
./mvnw test        # the Spring context must still start with the new config
```

Fix any failures before reporting. If something can't pass for a reason outside the module, say so explicitly. Don't hide it.

Before reporting, check that the javadoc matches the tests. Every GWT test should have its rule in the javadoc's "Business decisions", and every rule listed there as implemented should have a test. Then set the plan's status to `implemented` and add the dated changelog entry.

## Step 8: Report and follow up

Tell the user:
- the paths of the plan and the module
- the decisions made and the pushbacks given (ACL, multiple modules, non-GWT rules)
- the open points: rules awaiting GWT (including contradictions found in the canvas), collaborators that don't exist in code yet, open questions from the canvas
- the differences from the reference context map, if any
- for VeloCity: whether `src/main/cml/VeloCity-*.cml` and `velocity/velocity-context-map.md` need updating to match. The VeloCity `CLAUDE.md` asks to keep them in sync. Offer to do it, but don't do it unasked, because the context map may be a workshop exercise.
