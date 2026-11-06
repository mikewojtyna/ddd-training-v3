---
name: spec-writer
description: Turn one accepted business intent (docs/intents/<intent>.md) into a lean, context-agnostic specification in docs/specs/<intent>.md - business rules with concrete Given/When/Then examples (Example Mapping style), a domain-language contract of commands, queries and events, business data shapes, and open questions - the bare minimum to discuss and validate the requirement with domain experts on real examples and to plan the implementation. Use this whenever the user wants to specify, spec out, detail, sharpen, "add examples to", "write acceptance criteria for", "make concrete" or "do example mapping for" an intent or business need in VeloCity, CrowdSorcery, Orders or another system in this repo, or says "the intent is accepted, what's next", "write the spec for rider-reserves-bike", or wants to revise an existing spec, even if they never say "spec". Don't use it to capture new business needs (intent-creator), to decide bounded contexts, endpoints or code (implementation-planner, bounded-context-scaffolder), or to design aggregates (ddd-tactical-building-blocks).
---

# Spec Writer

A **spec** makes one intent concrete enough to argue about. The intent says *who wants what and why*; the spec says *which rules apply, shown on real examples, and what can be asked of and learned from the business* once the intent is met. It's the second link in the chain **intent → spec → plan → code**, and it's 1:1 with its intent: `docs/intents/<intent>.md` ↔ `docs/specs/<intent>.md`.

The spec has two readers:
1. **Domain experts and product people**, who validate the requirement by reading examples like "Given Anna holds a reservation for B-17, When she reserves B-22, Then it's rejected" and saying "yes" or "no, actually…". Examples with real values surface disagreements that abstract rules hide.
2. **The implementation planner**, which decides where the intent lives (which bounded context, a new one, several), turns the contract into endpoints and messages, and hands the examples to TDD as test scenarios.

Both readers are served by a spec that is **short, concrete and solution-free**. Every extra line is a line someone has to validate, and every technical word is a design decision made before the planner looked at the context map.

## The three non-negotiables

### 1. Bare minimum

Write only what's needed to validate the intent and plan it:
- Only the rules that decide whether the intent's **business outcome** happens or is rejected. Typically 2–6 rules. More than ~8 usually means the intent is too coarse; say so and suggest splitting it with `intent-creator` instead of writing a giant spec.
- 1–3 examples per rule: the typical case, plus the boundary or the rejection that makes the rule matter. Don't enumerate every permutation; one example per distinct outcome is enough.
- Only the commands, queries and events the intent needs. A query belongs only if an actor in the intent needs that information to reach the outcome.
- No non-functional requirements, UI, flows of screens, error-message texts or implementation hints (those go under `## Notes for the plan` only if the user gave them).

### 2. Concrete examples, never invented facts

- Every example uses **concrete values**: named actors (Anna, Ben), identifiers (bike B-17, station Central), times (10:00, 10:14), amounts (PLN 12.50). "Given a bike is available" is a rule restated, not an example.
- Values that illustrate (names, IDs, clock times) are yours to choose. **Values that decide** (durations, limits, fees, thresholds, percentages) come only from the intent, the vision, existing specs or code, or the user. If nobody stated one, write `?` with a question reference, e.g. "When 16 minutes pass (? reservation period, Q1)". An invented threshold looks authoritative in a spec and ends up in code as a fact nobody decided.
- **Ambiguous boundaries.** "Above 10 000", "within an hour", "after 3 attempts": write the example at the boundary using the literal reading (exactly 10 000 doesn't need approval), and add a question asking the expert to confirm it. Boundary examples are where specs pay off, so don't skip them, and don't settle the reading quietly.
- Work every example through by hand against the vocabulary and the other rules. Contradictions between examples, or with the vision, become open questions, not silent choices.

### 3. Context-agnostic and solution-free

The spec speaks the domain's language, as in the vision and the intent. It doesn't pick bounded contexts, and it doesn't design software.
- **Allowed:** commands, queries, events, actors, business data shapes (`Money ≥ 0`, `Identifier (Bike)`, `one of {open, closed}`). These are the language of Event Storming and Example Mapping; domain experts recognise them.
- **Forbidden outside `## Notes for the plan`:** HTTP verbs, endpoints, URLs, status codes, JSON, tables, columns, databases, queues, Kafka, frameworks, classes, aggregates, entities, repositories, bounded contexts, modules, services, "the system stores…".
- Vocabulary terms are the intent's and the vision's words. Don't rename them to fit some context's ubiquitous language; the planner translates them, and that translation is a decision the planner must see.

## Where specs live

```
docs/specs/<intent>.md      one spec per intent, same file name as the intent
```

Critic brief: `agents/spec-critic.md` (Step 4b).

Template: `assets/spec-template.md`. Copy its structure and section order exactly; its comments explain each section. The check script relies on the headings, the `### R<n>:` rule headings, the `**E<n>.<k>**` example IDs and the contract tables.

## Workflow

### Step 1: Read and gate

Read, in this order:
- The intent `docs/intents/<intent>.md`. Find it by name, or by matching the user's words against intent titles in `docs/intents/README.md`. If several match, ask which one.
- **Gate on status.** The intent must be `accepted` (or `specified`, when revising its spec). A `draft` intent isn't an agreed problem yet, so stop and say so: the user reviews and accepts it first. If the user explicitly tells you the intent is accepted, set its status to `accepted` and continue. `superseded` or `dropped` intents get no spec; point to the replacing intent.
- The system's vision (`velocity/velocity-vision.md`, `crowdsorcery/CrowdSorcery.md`, …) for vocabulary and stated rules. The VeloCity vision is deliberately inconsistent: never resolve a contradiction silently; record it as an open question.
- Specs of the intents listed under the intent's `## Related intents`, and of intents with the same actor or activity. Their rules constrain this one (a reserved bike can't be rented, so the rental spec and the reservation spec must agree). Reuse their vocabulary and event names word for word.
- If related behaviour already exists in code (intents with status `delivered`), skim it for rules the code already enforces, so the spec doesn't contradict reality. Describe what you learn in domain words; don't copy class names.

If `docs/specs/<intent>.md` already exists, you're **revising**: see "Revising a spec" below.

### Step 2: Map the rules

Do a quick Example Mapping pass, in your head or in the conversation:
- **Story:** the intent statement.
- **Rules:** start from the intent's `## Business rules mentioned` and its business outcome, then add the rules implied by related specs and the vision. The happy-path rule ("A rider can reserve an available bike") comes first, as `R1`.
- **Examples:** 1–3 per rule, concrete.
- **Questions:** everything you couldn't decide.

**When to ask before writing.** If you can't even write `R1`'s happy-path example (it's unclear who does what or what the outcome is), ask first: numbered, concrete questions, each one offering options where possible. Otherwise **write the draft first** and ask through it. A draft with concrete examples and `?` markers is the best way to run the conversation with a domain expert: they react to examples much faster than to abstract questions.

### Step 3: Write the spec

Fill in `assets/spec-template.md` at `docs/specs/<intent>.md`. Guidance for the parts people get wrong:
- **Frontmatter:** `name` and `intent` are both the intent's name. `status: draft`. Today's date.
- **Vocabulary:** only terms that the rules or contract use and that a newcomer could misread. Five to ten lines, not a glossary of the whole domain.
- **Rule sources:** a rule that the intent doesn't state, but that you took from the vision, a related spec or the code, gets its source at the end of the heading, e.g. `### R5: Only an activated account can reserve (vision)`. The expert then knows which rules they never said out loud and need to confirm. Include such a rule only if it decides whether this intent's outcome happens. Otherwise it belongs to another intent.
- **Rules:** one sentence per rule heading, stated as a business truth ("A rider holds at most one active reservation"), not as a validation ("check that…"). Example lines read `- **E<rule>.<k>** Given …, When …, Then …`. Put the outcome event in the Then in parentheses when there is one: "Then B-17 is reserved for Anna (Bike Reserved)".
- **Commands:** one per thing an actor asks for. "Rejected when" lists the rule IDs whose examples show a rejection. Every "On success" event appears under Events.
- **Events:** past-tense business facts. Every event in the intent's `## Business outcome` must appear here with the same name. Data lists the facts a listener would need, in business words.
- **Queries:** only if an actor needs the information to reach the outcome. Often there are none, and that's fine: leave a single row `| None. | | | |`.
- **Data:** the shapes the commands, queries and events carry, with constraints the rules imply. Skip shapes that are just one identifier.
- **Out of scope:** the intent's Scope "Out", plus anything you deliberately didn't specify.
- **Open questions:** `**Q<n>**` lines, each naming the rule, example or data constraint it blocks. Every `Q<n>` referenced in the body must be listed here.
- **Notes for the plan:** copy the intent's `## Notes for the spec` items, plus technical hints the user gives now.

### Step 4: Validate

```bash
python3 .claude/skills/spec-writer/scripts/specs.py check            # all specs
python3 .claude/skills/spec-writer/scripts/specs.py check <intent>   # one spec
```

Run it from the repository root. It checks that:
- the frontmatter is complete and the spec and intent pair up 1:1;
- the intent status fits the spec status;
- every rule has examples with matching IDs;
- every rejection references an existing rule;
- every success event and every intent outcome event appears under Events;
- every referenced `Q<n>` is listed;
- technical vocabulary stays out of the spec outside `## Notes for the plan`.

Fix every error. Treat warnings as signs that the spec drifted towards a solution or lost track of the intent.

### Step 4b: Independent critic

The script checks structure. It can't tell whether a threshold was invented or whether two examples contradict each other. You can't reliably tell either, because you wrote them. So before the user sees the draft, have it reviewed by a critic:

- Spawn **one** critic with the Agent tool (general-purpose). Its prompt contains:
  - the absolute path of `agents/spec-critic.md`, with the instruction to read it first;
  - the paths of this skill's `SKILL.md`, the spec, the intent, the vision and related specs;
  - the user's answers so far;
  - the round number.

  Give it the evidence, not your conclusions: the critic's value is that it isn't anchored on you.
- **AGREE:** go on to the report.
- **DISAGREE:**
  - fix the blocking findings, re-run `check`, and run the critic again;
  - turn its domain-expert questions into open questions in the spec.
- **At most 2 rounds.** If you still disagree after that, don't loop: present both positions to the user in the report and let them decide.
- When revising a spec (Step 6), run the critic again only if rules, examples or the contract changed.
- **No Agent tool available:** say so, and run the checklist in `agents/spec-critic.md` yourself as a separate pass.

### Step 5: Report, then stop for review

Tell the user, briefly:
- the spec path, the number of rules and examples, and the commands/queries/events;
- **the open questions**, numbered, with the example each one blocks. This is the most important part: it's the agenda for the conversation with the domain expert;
- contradictions you found with the vision or with related specs;
- anything you left out on purpose (bare minimum), and whether the intent looks too coarse;
- the critic's verdict, one line per round that changed something, plus any disagreement left for the user to decide.

Then stop. Don't write a plan, choose a bounded context or touch code. The next step after approval is `implementation-planner`.

### Step 6: Iterate and approve

The user answers questions, corrects examples or adds rules. Update the spec:
- Replace `?` with the agreed value everywhere it appears: examples, vocabulary, data constraints.
- Add the examples the answers produce. An answered boundary usually deserves one example on each side, e.g. renting at 10:14 is accepted and at 10:16 is rejected.
- Keep each answered question as a one-line decision record, struck through, with who answered and when: `- ~~**Q1** How long is the reservation period?~~ 15 minutes (domain expert, 2026-11-06)`. A spec's decisions are history, and the planner and reviewers will ask where a number came from.
- Don't edit the accepted intent's text to record answers. Intents are history, and the spec is where the answers live.

Re-run `check`.

When the user approves the spec (all blocking questions answered, or explicitly accepted as open), run:

```bash
python3 .claude/skills/spec-writer/scripts/specs.py approve <intent>
python3 .claude/skills/intent-creator/scripts/intents.py build
```

`approve` refuses to approve while `check` reports errors. It sets the spec to `approved` and the intent to `specified`, and the second command refreshes the intents index. Only approve on the user's word: approval is a product decision, not yours.

## Revising a spec

- **Draft spec:** edit in place.
- **Approved spec, nothing planned yet** (no `docs/plans/<intent>.md`): edit in place. Set `status: draft` and update `date`; the user re-approves.
- **Approved spec with a plan or code:** edit in place, set `status: draft`, and tell the user that the implementation plan must be revised. `implementation-planner` diffs the spec against its plan, so mention which rules and examples changed.
- **The business need itself changed:** that's a new intent superseding the old one (`intent-creator`). The new intent gets its own new spec. Set the old spec to `status: superseded`; don't rewrite it, it's history.
