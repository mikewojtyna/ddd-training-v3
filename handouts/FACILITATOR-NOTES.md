# Facilitator notes: living docs

Trainer only. Not part of the participants' start tag.

## Goal of the 20 minutes

Participants leave with a placement rule for **their** documents: what the agent loads always, what on demand,
what never, and what fails when a doc goes stale. This repo is the worked answer. Show it **after** they filled in
their own table, not before, or they'll copy it.

Note: this repo has **no ADRs**. Decisions live in the domain-expert answers (`DE-n`), spec examples, plan steps,
canvases and the skills' design histories. Say so openly: it's a fair question whether an ADR folder would add
anything here, and it's a good bridge to their ADR on screen.

## Steering the ADR on screen

- "Which sentence of this ADR would change the code an agent writes?" Usually one or two: the decision and the
  constraints. The context and the rejected options are for humans.
- "Is it still true? Show me where in the code." If nobody can, that's the staleness question, live.
- "If the agent read only the title and the status, what would it get wrong?"
- "Where would it be loaded from? Does the agent know it exists?" An ADR the agent never finds is a human-only doc.

## The answer this repo gives

Verified against `README.md`, the root `CLAUDE.md` and the per-system `CLAUDE.md` files on master of 2026-11-10.

### Loaded always: small, rules only

- **Root `CLAUDE.md`** (37 lines). It imports the README with `@README.md` (115 lines) instead of copying it, and
  adds only rules: no Lombok, jMolecules, aggregates return `DomainEvents`, cross-context calls only through
  `@AntiCorruptionLayer`, statuses only through scripts, approval is the human's.
- Together 152 lines, 12,192 bytes: **~3,000 tokens** in every session (bytes ÷ 4, measured sizes).

### Loaded when the agent works in a system: per-system `CLAUDE.md`

- `velocity/CLAUDE.md` (42 lines), `crowdsorcery/CLAUDE.md` (29), `orders/CLAUDE.md` (13), next to the code.
  Claude Code picks up a nested `CLAUDE.md` when it works on files in that folder; the root file also lists them.
- They hold what differs per system: sources of truth (and which file is a **spoiler** not to push on participants),
  the context table, integration conventions, the reference implementation for the skills.
- Root + VeloCity together: 194 lines, 17,721 bytes, ~4,400 tokens.

### Loaded on demand: skills, triggered by their description

- `.claude/skills/<name>/SKILL.md`. Only the **description** (the frontmatter) is in context all the time; the body
  loads when a task matches it. The body points further to `references/`, `assets/` (templates), `scripts/` and
  `agents/` (critics), which load only when the step needs them.
- Why it matters: the nine SKILL.md bodies are 10–28 KB each, 153 KB together (~38,000 tokens). Loading them always
  would cost more than ten times the always-loaded `CLAUDE.md` set.
- Kept current by: whoever changes the skill, with evals and, for four skills, a design history (`docs/skills/<skill>/design-history.md`).

### The flow's artifacts: 1:1:1, statuses changed only by scripts

- `docs/intents/<intent>.md` → `docs/specs/<intent>.md` → `docs/plans/<intent>.md`, same file name.
- The intent's status tracks progress: `draft → accepted → specified → planned → delivered`. Statuses change only
  through `intents.py`, `specs.py approve`, `plans.py approve|step|done`, never by hand and never by the model on its
  own judgment.
- Read on demand by the skills that need them (the planner reads the spec, the spec writer reads the intent).
- Staleness detection: `intents.py check`, `specs.py check`, `plans.py check` print ERROR/WARN and exit non-zero.
  `docs/intents/README.md` is **generated** by `intents.py build` and says "Do not edit by hand".

### Per context: the canvas

- `docs/bounded-contexts/<system>/<context>/canvas.md` (and the scaffolder's `plan.md` with its TDD log).
- Read on demand by the scaffolder, the planner and the tactical skill when they work in that context.
- Kept current by the planner: a plan includes its canvas changes.
- Note the duplication: every context's `package-info.java` **carries the canvas as javadoc** too (see
  `velocity/CLAUDE.md`). Good for an agent reading the code, but it's a second copy. Use it as a live example of
  the "same rule in two places" trap.

### The context map: CML, kept in sync with the code

- `src/main/cml/VeloCity-Context-Map.cml` and `VeloCity-Bounded-Contexts.cml`; validated with
  `.claude/skills/discover-bounded-contexts/scripts/validate_cml.sh`.
- The root `CLAUDE.md` rule: if you change a relationship, keep the CML in sync.
- What enforces the boundaries in code: `<Context>ArchitectureTest` (ArchUnit) checks that other contexts use only
  a context's `api` package. Honest limit: the architecture tests don't read the CML. The map-vs-code comparison is
  done by `discover-bounded-contexts` in validation mode, which `system-builder` runs at the end of a build.

### Executable docs: acceptance tests named after spec examples

- Each spec example becomes one test: `src/test/java/.../<system>/<context>/<IntentCamelCase>SpecTest.java`, method
  `e1_1_...`, with the example's text as `@DisplayName`. Example: `RiderReservesBikeSpecTest` ↔
  `docs/specs/rider-reserves-bike.md`, E1.1.
- This is the doc that **can't go stale silently**: if the code changes, the test fails. If the spec changes and the
  test doesn't, the reviewer sees an example without a matching test name.

### Records for humans: read never, or only when asked

- `docs/bounded-contexts/velocity/conversation.md` (session record, 23 KB), `docs/context-discovery/.../discovery.md`
  (heuristic trail, critic log), `docs/skills/*/design-history.md`. History, not instructions.
- `docs/bounded-contexts/velocity/domain-expert-answers.md` is in between: specs cite `DE-n`, so the agent reads it
  when a decision is questioned.

## The traps, with examples from this repo

| Trap | Why it hurts with agents | Example here | What helps |
|---|---|---|---|
| A stale doc the agent trusts more than the code | The agent reads the doc first and "fixes" the code to match it | The VeloCity vision is **deliberately** inconsistent; `velocity/CLAUDE.md` tells the agent not to fix it and to ask when vision and code disagree | Say in the always-loaded file which source wins; prefer docs that fail when wrong (tests, `check` scripts) |
| Docs too big to load | Skipped, or they eat the budget in every session | SKILL.md bodies: 153 KB in total, so they're on demand; the always-loaded set is ~3,000 tokens | Keep "always" small; split by trigger; link instead of inlining |
| The same rule in two places | Two copies drift; the agent picks whichever it read last | Canvas in `canvas.md` and in `package-info.java`; README conventions partly restated in `CLAUDE.md` rules. Exercise 6: the 2-minute reservation appears in 17 files | One owner per rule; import (`@README.md`) instead of copying; let the other copy point to it |
| Generated docs nobody reviews | Look authoritative, encode whatever the generator assumed | `docs/intents/README.md` is generated, which is fine because it's rebuilt by a script; but the 35 specs and the domain-expert answers of the autonomous run were written and approved under a standing instruction and still need a human review | Mark generated files; regenerate instead of editing; keep human approval on anything the agent wrote |
| An "always" file that grew to cover everything | Every task pays for every rule | The root `CLAUDE.md` keeps only rules and points to per-system files | Review the always-loaded files like code; set a size budget |

## A filled doc-placement table for this repo

| Document type | Read by the agent when | Location | Size limit | Kept current by | How staleness is detected |
|---|---|---|---|---|---|
| Project rules | always | `CLAUDE.md` (imports `README.md`) | small: 37 + 115 lines, ~3,000 tokens | trainer (repo owner), in PR review | review only; nothing fails automatically |
| System context | when working in that system's folder | `src/main/java/.../<system>/CLAUDE.md` | 13–42 lines | whoever changes the system's contexts or conventions | review; partly by architecture tests (the conventions it describes) |
| Skills (how to do a step) | on demand, by description match | `.claude/skills/<name>/SKILL.md` + `references/`, `assets/`, `scripts/`, `agents/` | body 10–28 KB; details pushed into `references/` | skill author | evals (`evals/`), design history, critic agents |
| Intent (business need) | on demand: by the spec writer and planner | `docs/intents/<intent>.md` + `story-map.md` | one need per file | product owner, via `intent-creator`; status by `intents.py` | `intents.py check`; status field |
| Spec (rules + examples) | on demand: by the planner and TDD loop | `docs/specs/<intent>.md` | lean: rules, examples, contract | product owner approves (`specs.py approve`) | `specs.py check`; every example has a `*SpecTest` method |
| Plan | on demand: while building that intent | `docs/plans/<intent>.md` | ordered steps | planner, steps ticked by `plans.py step` | `plans.py check`; status `delivered` only when all steps are done |
| Canvas | on demand: when working in that context | `docs/bounded-contexts/<system>/<context>/canvas.md` (copy in `package-info.java`) | one context | planner / scaffolder, as part of a plan | review; no automatic check (drift with the javadoc copy is possible) |
| Context map | on demand: when a relationship or boundary changes | `src/main/cml/*.cml` | the whole system | whoever changes a relationship (CLAUDE.md rule) | `validate_cml.sh`; architecture tests for the code side; map-vs-code validation |
| Acceptance tests | on demand: as the executable spec | `src/test/.../<IntentCamelCase>SpecTest.java` | one class per spec | written in the TDD loop, reviewed by the human | they fail when the code changes |
| Session records, discovery reports, design histories | never by default; on request | `docs/bounded-contexts/.../conversation.md`, `docs/context-discovery/`, `docs/skills/` | none (history) | written once | not maintained: dated, read as history |

## Points to land

1. **Always-loaded is a budget.** Rules only, small, no history. Everything else is on demand, with a trigger the
   agent can recognise.
2. **Put the doc where the agent will find it**: next to the code it describes, or linked from an always-loaded file.
3. **Prefer docs that fail when they're wrong**: spec examples as tests, `check` scripts, architecture tests. A doc
   nothing checks will go stale; decide in advance who notices.
4. **One rule, one place.** Import or link instead of copying.
5. **Statuses by scripts, approval by humans.** The agent may draft any doc, but a human decides when it's true.

For their ADRs, a concrete proposal: one decision per file in the code repo, ≤ ~80 lines, a status with
`superseded by`, loaded on demand (linked from the module's `CLAUDE.md`/`AGENTS.md` or a skill that touches that
area), and a PR rule: a change to the module lists the ADRs it affects.
