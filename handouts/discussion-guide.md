# Discussion guide: living docs

## Start: one real ADR (3 min)

The architect who writes ADRs puts one on screen. While it's shown, everyone notes:

| Question | Notes |
|---|---|
| What decision does it record, in one sentence? | |
| Is the decision still true in the code today? How do you know? | |
| Which part would change what an agent writes? (the decision, the constraints, the rejected options) | |
| Which part is history the agent doesn't need? | |
| How many tokens is it? (lines × ~10, or bytes ÷ 4) | |

## The questions (12 min)

Take them in order. Spend about two minutes on each, and write the answers straight into
`doc-placement-template.md`.

### 1. Which of our ADRs and specs should an agent read?

- Which documents change what the agent produces? (a rule, a constraint, a naming convention, a boundary)
- Which only explain the past? (meeting notes, superseded decisions, discussion threads)
- Is there a document the agent should read **instead of** the code? Is there one it should never read because the
  code is the truth?

### 2. When does it read them?

Three choices for every document:

| When | Means | Costs |
|---|---|---|
| **Always** | loaded at the start of every session (`CLAUDE.md`, `AGENTS.md`, `.github/copilot-instructions.md`) | tokens in every session, for every task |
| **On demand** | loaded when the task needs it: a skill or prompt file that triggers on a description, or a link the agent follows | the agent must recognise it needs it |
| **Never** | kept for humans, or archived | the agent can't use it, even when it would help |

- What deserves to be in "always"? How big can "always" get before it costs too much?
- For "on demand": what makes the agent find the right document? A file name? A link from an always-loaded file?

### 3. Where in the repo?

- Next to the code it describes (`<module>/CLAUDE.md`, a canvas per context), or in one central `docs/` folder?
- In the code repo or in a wiki? Can the agent reach the wiki at all? Is it allowed to (data-safety rules)?
- One document per decision, or one big file?

### 4. Who keeps them current?

- When the code changes, who updates the doc: the developer, the agent, a reviewer, nobody?
- Is updating the doc part of "done"? Is it checked in the PR?
- If the agent updates a doc, who reviews that change?

### 5. How do we notice they are stale?

- Is there anything that fails when a doc and the code disagree? (a test, a script, a link checker, a status field)
- Does each doc have an owner, a status or a date?
- What happens today when somebody finds a stale doc?

## Traps to watch for

Name them when you see them in your own documents:
- a doc the agent trusts more than the code, although the code moved on;
- a doc too big to load, so either it's skipped or it eats the budget;
- the same rule in two places, each slightly different;
- a generated doc nobody reviews;
- an "always loaded" file that grew to cover everything.

## Close (5 min)

Each team picks **one** change for next week:

| Team | The document | The change (move / shrink / split / delete / add a check) | Who |
|---|---|---|---|
| | | | |
| | | | |
| | | | |
