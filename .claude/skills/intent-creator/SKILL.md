---
name: intent-creator
description: Capture business needs as fine-grained, business-oriented intent files in docs/intents/<intent-name>.md, placed on a user story map so the project's whole history and value stream can be read from the business side. Use this whenever the user wants to add, write, record, capture, draft, split, backfill or change an intent, or describes a new feature, business need, user need, process change or product idea for VeloCity, CrowdSorcery or another system in this repo. That includes "riders should be able to reserve bikes", "we need withdrawals for investors", "product wants X", "turn this story map into intents", or "write down what Rentals already does". Every new feature or business need starts with at least one intent, so use this skill before any spec or plan exists, even when the user never says "intent". Also use it when a request is phrased technically (an endpoint, a table, a cache) and the business need behind it has to be found first. Don't use it to write specs, Given/When/Then rules, plans or code: other skills turn intents into those.
---

# Intent Creator

An **intent** is one small business need, written down: *who* wants *what*, *why*, and *what is true in the business once it's met*. It's the first link in the chain **intent → spec → plan → code**. This skill only writes intents. Specs (`spec.md`) and plans (`plan.md`) belong to other skills, so leave them out. Don't write acceptance criteria, Given/When/Then rules, designs or tasks here.

Intents are written to be read in order. Sorted by `sequence` they tell the product's history ("first riders could rent, then return, then reserve…"). Grouped by story-map activity they rebuild the value stream from start to finish. Every rule below protects one of those two readings.

## The two non-negotiables

### 1. Fine-grained: one actor, one goal, one outcome

An intent sits at the **user story level of a story map**. It's a single thing an actor wants to get done, with value of its own, that a domain expert would recognise in one sentence. Examples:
- "A rider wants to rent a bike from a bike station"
- "A rider wants to reserve a bike at a bike station"
- "An investor wants to make a deposit"

**Too coarse** (an activity or an epic, so split it). Signals:
- verbs like *manage, handle, support, administer, enable*
- "and" joining two goals
- more than one actor
- more than one business outcome
- a CRUD bundle ("create/edit/delete projects")

Each piece becomes its own intent. For example, "Reservations" splits into *rider reserves a bike*, *rider cancels a reservation* and *unused reservation is released*.

**Too fine** (a step inside someone else's goal, so fold it in). Signals:
- a screen or button
- a field validation
- one branch of an error path that's worthless on its own
- a technical step

Mention it in the parent intent's scope instead of giving it a file.

Quick test: *could this be shown to a domain expert as "now X can Y", and would they care?* If yes, it's an intent. If they'd say "that's just part of Y", fold it. If they'd say "that's several things", split it.

### 2. Business-oriented: the problem, not the solution

Write in the domain's ubiquitous language, from the actor's point of view. The intent says what the business needs and why. It never says how software does it.

- **Forbidden in intents:** endpoints, HTTP verbs, URLs, tables, databases, queues, Kafka, caches, frameworks, classes, aggregates, bounded contexts, modules, services, "the system stores…". These are design choices that the spec and plan make later. If they appear in the intent, they freeze a solution before anyone has agreed on the problem.
- **Actors are people or business roles:** Rider, Technician, Support Agent, Investor, Borrower, Project Verifier, City Operations, Finance. Never "the system" or "the backend". When something happens on its own (a timeout, a nightly run), name the role that *wants* it to happen and why. For example: "Fleet operations wants unused reservations released so that bikes aren't blocked."
- **A technical request means a business need is hiding behind it.** "Add `POST /deposits` and a deposits table" means "an investor wants to make a deposit". Write that intent, then list the technical hints under `## Notes for the spec` so nothing the user said is lost. If a request has **no** business driver (a library upgrade, a pure refactoring, a cache with no user-visible pain), it doesn't need an intent. Say so, and suggest that it belongs in the plan of an existing intent. If there may be a driver ("the station list is slow, riders complain"), ask about the business pain, or write the intent around it ("a rider wants to see available bikes at nearby stations without waiting"), with the slowness as the motivation.

## Where intents live

```
docs/intents/
  story-map.md        backbone per system: actors and ordered activities (hand-maintained by this skill)
  README.md           generated index: timeline + value stream (never edit by hand)
  <intent-name>.md    one file per intent
```

- **File name:** the kebab-case `<intent-name>` is actor + verb + object, in the third-person present tense: `rider-reserves-bike-at-station.md`, `investor-makes-deposit.md`. Keep it short (3–7 words) and use domain words. If two systems would produce the same name, prefix the system: `crowdsorcery-investor-makes-deposit.md`.
- **Template:** `assets/intent-template.md`. Copy its structure exactly. Its comments explain each field.
- **Story map:** `assets/story-map-template.md`, which shows the format of `story-map.md`.

## Workflow

### Step 1: Read what already exists

Read these before writing anything, because each intent has to fit the history:
- `docs/intents/story-map.md`, and the frontmatter of every `docs/intents/*.md`. You need the next `sequence` number (max + 1, shared across all systems), the existing actor and activity names (reuse them word for word, or the value stream fragments), and possible duplicates.
- The system's vision for its vocabulary and actors: `velocity/velocity-vision.md` or `crowdsorcery/CrowdSorcery.md`, plus the system's `CLAUDE.md`. The VeloCity vision is deliberately inconsistent, so don't resolve its contradictions inside an intent. Record them under `## Open questions`.
- Any story map, notes or document the user points to.

If `docs/intents/` doesn't exist yet, create it with `story-map.md` and generate the index in Step 5.

### Step 2: Put the request on the story map

For each need in the request, find:
- **System**, e.g. `velocity` or `crowdsorcery`.
- **Actor**: who wants it.
- **Activity**: the backbone step it belongs to, such as *Join VeloCity*, *Get a bike*, *Ride*, *Return the bike*, *Pay for the ride*. Activities are big, ordered steps of the actor's journey, named as goals in the actor's words. Reuse an existing activity when one fits. Add a new one to `story-map.md` only when nothing fits, and put it at the right place in the journey, because the order is the value stream.
- **Task**: the user task under the activity that the intent delivers or improves, e.g. *Reserve a bike*.

If the user gave a story map, take the actors, activities and tasks from it instead of inventing your own.

A broad request (an epic, a whole feature, "all of reservations") turns into **several** intents. Lay them out as a small story-map slice first: which tasks, in which order. Write all of them when there are up to about seven. If there are more, write the first release slice (the smallest set that gives the actor end-to-end value), list the rest as *candidate intents* in the report and in `## Scope`, and ask which slice comes next. A huge batch of drafts is hard to review and usually gets thrown away.

### Step 3: Check against the two non-negotiables, and against history

- Apply the granularity test and the business-orientation rules above to every intent. Rewrite or split until each one passes.
- **Duplicate:** an existing intent already covers the need. Don't write a new one. Point the user to it.
- **Changed need:** the business changed its mind about an existing intent (a new rule, a different goal, a dropped need). Intents are history, so **don't rewrite an accepted or delivered intent**. Write a new intent that `supersedes` the old one, explain *what changed and why* under `## Why`, and in the old file change only `status: superseded` and add `superseded_by: <new-intent-name>`. Drafts that have never been accepted are the exception: you can still edit those in place.
- **Extension:** the new intent builds on an existing one without replacing it (reservations build on renting). Link them under `## Related intents` with the relation (`builds on`, `enables`, `follows`, `alternative to`).

### Step 4: Write the intent files

Fill in the template. Guidance for the parts people get wrong:
- **Intent statement:** "As a *Rider*, I want to *reserve a bike at a station* so that *nobody takes it while I'm unlocking it*." The *so that* is the real value. If you can't name it, you don't understand the need yet, so ask.
- **Why:** the business reason and the trigger (a complaint, a regulation, a revenue goal, a vision section). Quote or cite the source.
- **Business outcome:** the business events that are true once the intent is met, named as past-tense domain facts (`Bike Reserved`, `Reservation Expired`, `Deposit Made`). Name the event in the actor's language. It isn't a class or a message design. Read across all intents in sequence order, these events are the value stream.
- **Success signal:** how the business will know the intent paid off (fewer "no bike at station" complaints, share of rides started from a reservation). If nobody said, write a suggestion marked *(suggested)*. Don't present a guess as a requirement.
- **Business rules mentioned:** copy the rules the user stated, in their words ("reservations last 15 minutes", "one active reservation per rider"). Don't sharpen them into Given/When/Then and don't invent thresholds. That's the spec's job, and an invented number here would end up in the spec as fact. Where a rule is vague or contradicts the vision, add an open question.
- **Scope:** *In* and *Out*, where *Out* points to the neighbouring intents (existing or candidate) by name. This makes the boundary between fine-grained intents explicit.
- **Notes for the spec:** technical hints, constraints or solution ideas the user mentioned. Keep them here, out of every other section.
- **Status:** new intents are `draft`. The user moves them to `accepted`. Other skills later set `specified` (`spec-writer`), `planned` and `delivered` (`implementation-planner`). Use `delivered` only when backfilling intents for behaviour that already exists in code, and say `backfilled` in `## Why`.
- **Date:** today's date (`YYYY-MM-DD`). When backfilling, use the date the behaviour appeared if git history shows it, and otherwise today's date.

**Backfilling** (the user wants intents for what already exists): derive the needs from the vision, the code and the git history. Give them sequence numbers in the order they were delivered, or, when that's unknown, in value-stream order. Mark them `delivered`. Keep the code out of the intent text: the intent describes the business need the code serves, not the code.

### Step 5: Regenerate and validate the index

```bash
python3 .claude/skills/intent-creator/scripts/intents.py build    # rewrites docs/intents/README.md
python3 .claude/skills/intent-creator/scripts/intents.py check    # validates all intents; non-zero exit on errors
```

`check` catches:
- missing frontmatter fields
- duplicate or non-contiguous sequence numbers
- a file name that doesn't match `name`
- an activity missing from `story-map.md`
- broken `supersedes` and related links
- technical vocabulary in the intent text

Fix every error. Warnings about technical words outside `## Notes for the spec` usually mean the intent has drifted towards a solution, so rewrite the sentence. Run the script from the repository root.

### Step 6: Report, then stop for review

Tell the user:
- the files created or changed, and where each new intent sits (system → actor → activity → task, sequence number)
- splits and folds you made, and why (granularity)
- reframings you made (technical → business), and requests you declined to turn into intents
- candidate intents you didn't write yet
- open questions, especially rules you didn't sharpen and contradictions with the vision

Then stop. Intents are product decisions, so the user reviews them and moves them from `draft` to `accepted`. Don't go on to write a spec, a plan or code, even if the request sounded like "build reservations". Mention that the next step is a spec for each accepted intent, written with `spec-writer`. After that, `implementation-planner` turns the intent and its spec into a plan.
