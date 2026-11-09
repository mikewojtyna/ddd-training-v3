---
name: adr-writer
description: Write, record, update or supersede an Architecture Decision Record (ADR) in docs/adr/NNNN-<slug>.md - context, decision drivers, at least two considered options, the decision and its positive and negative consequences (MADR-style) - reviewed by an independent critic and validated by a script, with statuses (proposed, accepted, superseded, deprecated) changed only through that script. Use this whenever the user wants to document, record, "write down" or "log" a technical or architecture decision, says "record this decision", "we decided to use X instead of Y", "why did we choose X?", "make an ADR for this", "we're replacing X with Y" or "this decision is no longer valid", wants to accept, supersede or deprecate an existing ADR, or asks to check the ADR log, even if they never say "ADR". Don't use it to write business specs (spec-writer), implementation plans (implementation-planner) or design-history files of skills, or to make the decision for the user.
---

# ADR Writer

An **ADR** records one architecture decision: the problem, the options that were really on the table, the choice and
what it costs. It's written for the reader who asks "why did we choose X?" a year later, and for coding agents that
read the repository to stay inside earlier decisions. An ADR is history: once accepted, it isn't rewritten; a new ADR
supersedes it.

## Rules that always hold

1. **The user decides; you record.** If the decision isn't made yet, help lay out the options, but write
   `status: proposed` and let the user (or the board) accept it. Never accept an ADR on your own.
2. **At least two real options, and real costs.** A record with one option or no negative consequences is a
   justification, not a decision record. If the user only names the winner, ask what else was considered.
3. **No invented facts.** Load figures, latencies, costs, deadlines come from the user or a linked source. An unknown
   number is `?` with a question under `## Links`, not a plausible guess.
4. **Statuses change only through `scripts/adrs.py`.** It checks before it changes and keeps supersede links consistent
   on both files. Editing a `status:` line by hand skips both.

## Where ADRs live

```
docs/adr/NNNN-<slug>.md     one decision per file; NNNN = next free number, slug = kebab-case of the title
```

Template: `assets/adr-template.md`. Keep its headings and the `### <option>` and `### Positive` / `### Negative`
subsections: `adrs.py` relies on them. Critic brief: `agents/adr-critic.md` (Step 4).

## Workflow

### Step 1: Read and gate

- `ls docs/adr/` (create the folder if it's missing) and read the ADRs on the same topic. Run
  `python3 .claude/skills/adr-writer/scripts/adrs.py check` to see the current state.
- Classify the request:
  - **new decision** → Step 2;
  - **replacing a decision** ("we're moving off X", "this no longer holds") → a new ADR with `supersedes: <old number>`;
    never rewrite the accepted one;
  - **status change only** ("accept ADR 7", "ADR 3 is obsolete") → Step 6;
  - **"why did we choose X?"** → find and quote the ADR; if none exists, offer to record it, asking for the reasons
    rather than reconstructing them.
- Gather from the conversation, the code and linked docs: the problem, the constraints, the options, the choice, the
  costs. **Ask before writing** only if you can't name the decision or a second option. Otherwise write the draft
  and ask through it (`?` markers), as people react faster to a draft than to a questionnaire. At most 5 questions.

### Step 2: Draft from the template

Copy `assets/adr-template.md` to `docs/adr/NNNN-<slug>.md` with the next free number. Fill it in:
- **Frontmatter:** `title` = the decision as a short imperative phrase; `status: proposed`; today's date;
  `supersedes:` the old number when replacing; leave `superseded_by:` empty.
- **Context:** the problem and the forces, not the solution. "Simulation results are queried ad hoc by analysts and
  the current store needs a reindex for every new field", not "We use PostgreSQL because…".
- **Decision drivers:** 2-5 criteria in the user's words. The Decision refers back to them.
- **Considered options:** at least two `### <option>` subsections, each judged against the drivers in 1-3 lines.
- **Decision:** "We chose X, because <drivers>", naming what it trades off.
- **Consequences:** both `### Positive` and `### Negative`, specific to this decision.
- **Links:** the superseded ADR, tickets, benchmarks, open questions.

Keep it to one or two pages. One decision per ADR: if the conversation contains two, write two.

### Step 3: Validate

```bash
python3 .claude/skills/adr-writer/scripts/adrs.py check              # whole log
python3 .claude/skills/adr-writer/scripts/adrs.py check <number>     # one ADR (numbering and links still checked)
```

It checks the file name and heading number, the frontmatter, the required sections, at least two options, a
non-empty Negative list, unique and sequential numbering, and that supersede links agree on both sides. Fix every
ERROR. Read every WARN: leftover placeholders and a solution-first Context are usually real problems.

### Step 4: Independent critic

The script checks structure; it can't tell a straw-man option from a real one, and you can't either, because you
wrote it. Before the user sees the draft:

- Spawn **one** critic with the Agent tool (general-purpose, **`model: sonnet`**, as in the brief's frontmatter: a
  checklist review doesn't need your model). Its prompt contains:
  - the absolute path of `agents/adr-critic.md`, with the instruction to read it first;
  - the paths of this `SKILL.md`, the template, the ADR and related ADRs;
  - the user's input and answers so far;
  - the round number.

  Give it the evidence, not your conclusions.
- **AGREE:** go on to the report.
- **DISAGREE:** fix the blocking findings, re-run `check`, run the critic again. Turn its questions into `?` markers and
  questions for the user.
- **At most 2 rounds.** Then present both positions to the user and let them decide.
- **No Agent tool:** say so, and run the checklist in `agents/adr-critic.md` yourself as a separate pass.

### Step 5: Report, then stop for review

Tell the user briefly:
- the ADR path, the title, the options and the chosen one;
- the open questions (`?` markers), numbered;
- the critic's verdict, one line per round that changed something;
- the command that accepts it once they agree:
  `python3 .claude/skills/adr-writer/scripts/adrs.py accept <number>` (or `supersede <old> <new>`).

Then stop. Don't accept the ADR, and don't change code to match it.

### Step 6: Status changes, on the user's word only

```bash
python3 .claude/skills/adr-writer/scripts/adrs.py accept    <number>      # proposed -> accepted
python3 .claude/skills/adr-writer/scripts/adrs.py supersede <old> <new>   # new -> accepted, old -> superseded, links both ways
python3 .claude/skills/adr-writer/scripts/adrs.py deprecate <number>      # accepted -> deprecated, nothing replaces it
```

Each command refuses while `check` reports errors for the ADR it changes. Run them only when the user explicitly
approves ("accept it", "go ahead with the supersede"). A user editing a proposed ADR isn't an approval. After a
supersede, add a line to the old ADR's `## Links`, "Superseded by ADR-NNNN", but don't touch the rest of it.

## Revising an ADR

- **Proposed:** edit in place, re-run Steps 3-5.
- **Accepted:** don't edit the decision. Typos and broken links are fine to fix. A changed decision is a new ADR that
  supersedes this one.
