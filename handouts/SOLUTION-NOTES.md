# Solution notes: Exercise 13

The worked example is an `adr-writer` skill, built to the same anatomy as `spec-writer`.

```bash
git diff ex13/start ex13/solution --stat
python3 .claude/skills/adr-writer/scripts/adrs.py check     # the example log in docs/adr/
```

```
.claude/skills/adr-writer/
  SKILL.md                    trigger + workflow: gather → draft → check → critic → report → stop
  assets/adr-template.md      MADR-like: Context, Decision drivers, Considered options, Decision, Consequences (+/−), Links
  scripts/adrs.py             check | accept | supersede | deprecate (stdlib only)
  agents/adr-critic.md        checklist + verdict format, model: sonnet
  evals/evals.json            2 prompts with assertions; evals/files/seed-adrs/ for the supersede scenario
docs/adr/0001-record-architecture-decisions.md   the first ADR, accepted through adrs.py
```

## Walkthrough

**Description.** It names the output and path (`docs/adr/NNNN-<slug>.md`), lists the user's real phrases ("record this
decision", "we decided to use X instead of Y", "why did we choose X?", "we're replacing X with Y"), adds "even if they
never say ADR", and ends with negative triggers (`spec-writer`, `implementation-planner`, "make the decision for the
user"). "Why did we choose X?" is deliberate: the skill answers from the log first and only offers to record.

**Body.** Four rules with reasons (the user decides; two real options and real costs; no invented facts; statuses only
through the script), then six steps. Step 1 classifies the request: a new decision, a replacement (a new ADR with
`supersedes:`, never a rewrite), a status change only, or a question. Step 5 stops and prints the accept command; Step 6
runs status changes only on the user's explicit word.

**Template.** The HTML comments teach the model what each section is for (Context = problem, not solution; drivers =
criteria the Decision refers back to). `### <option>` and `### Positive` / `### Negative` are a contract with the script.

**Script.** What can be decided mechanically lives here:
- file name `NNNN-slug.md` and heading number agree; required frontmatter and sections;
- at least two `### ` options; a non-empty Negative list (ERROR); leftover placeholders and a Context that opens with
  "we will…" (WARN);
- numbering unique and sequential across the folder;
- supersede links agree on both sides: a superseded ADR names an existing, accepted successor that says
  `supersedes:` back; an accepted ADR can't supersede one that is still accepted.

`accept`, `supersede` and `deprecate` refuse while the target has errors. `supersede <old> <new>` is one approval that
updates both files, which is exactly what a model editing status lines by hand gets wrong: the check then reports
`accepted, but the ADR it supersedes (...) is still 'accepted'; use adrs.py supersede 2 3`.

**Critic.** It covers what needs judgement: real options vs. straw men, real costs vs. generic risks, the decision
traceable to the drivers, Context free of the solution, sourced numbers, supersede consistency, one decision per ADR.
`model: sonnet` because a checklist over a two-page document doesn't need the writer's top model. As in this repo's
other skills, the brief is passed by path to a general-purpose agent, and SKILL.md tells the writer to pass
`model: sonnet` in that Agent call. The frontmatter documents the choice and works as is if the brief is moved to
`.claude/agents/`.

**Evals.** Eval 1 (PostgreSQL instead of MongoDB) tests the happy path, plus two traps: the user only names the winner,
so the skill must still list a second option, and it gives no figures, so none may be invented. Eval 2 (supersede
RabbitMQ with Kafka) tests history (ADR 0002's decision text unchanged) and the status rule: the user says the board
approved, so the skill runs `adrs.py supersede 2 3` rather than editing two files. Expect the baseline to write a
decent ADR for eval 1 but miss "proposed" and the accept command, and in eval 2 to rewrite ADR 0002 or set statuses
by hand.

## What to look for in participants' skills

| Part | Good sign | Common miss |
|---|---|---|
| Description | Output + path, real phrases in quotes, "even if they never say…", negative triggers | A title-like description ("Helps with ADRs"); no negative triggers, so it collides with another skill |
| Body | Numbered steps; an explicit stop before approval; reasons, not MUSTs | An essay; the agent "finalises" the document itself |
| Template | Stable headings the script relies on; comments that teach | A template the script ignores, or a script that checks headings the template doesn't have |
| Script | ERROR/WARN, exit 1; checks what reviewers really find; owns status changes | Only checks "file exists"; status still changed by the model |
| Critic | Clean context, evidence by path, checklist, verdict format, round limit, a `model:` choice with a reason | The writer passes its conclusions; "review this" with no checklist; unlimited rounds |
| Evals | Prompts typed like a colleague, with real details; one tricky case; checkable assertions | Prompts that name the skill; assertions like "output is good"; with/without runs in the same chat |

## Debrief prompts for the trainer

- If the baseline passes almost everything, the task may not need a skill, or the evals only test the easy part. Both
  are useful findings (`spec-writer`: 100% vs 88%; `discover-bounded-contexts`: 98% vs 45%).
- Ask each pair which rule moved from the prompt into the script. That's the "statuses through scripts" point of M7.
- Extension results: a cheaper critic usually keeps the structural findings (missing negatives, one option) and loses
  the subtle ones (a straw-man option, a driver the decision ignores). Ask what that means for their LLM-farm budget.
- Link to Exercise 14: the script and the template port to Copilot or Cursor unchanged; the trigger and the critic
  sub-agent are the parts that need rework.
