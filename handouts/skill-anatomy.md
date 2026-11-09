# Anatomy of a skill: `spec-writer`

A skill is a folder the agent loads on demand. Only the `description` is always in context; the rest is read when the
skill fires.

```
.claude/skills/spec-writer/
  SKILL.md                frontmatter (name, description) + steps and stop points
  assets/spec-template.md the shape of the output
  scripts/specs.py        deterministic checks + status changes
  agents/spec-critic.md   brief for an independent critic
  evals/evals.json        prompts + assertions, run with and without the skill
```

## 1. `description` = the trigger

The agent decides from the description alone whether to load the skill. `spec-writer`'s (shortened):

> Turn one accepted business intent (docs/intents/&lt;intent&gt;.md) into a lean, context-agnostic specification in
> docs/specs/&lt;intent&gt;.md - business rules with concrete Given/When/Then examples ... Use this whenever the user wants to
> specify, spec out, detail, sharpen, "add examples to", "write acceptance criteria for", "make concrete" or "do example
> mapping for" an intent ... or says "the intent is accepted, what's next", ... even if they never say "spec". Don't use it
> to capture new business needs (intent-creator), to decide bounded contexts ... (implementation-planner ...).

What makes it sharp:
- **Input and output with paths:** one accepted intent in, one file at a known path out.
- **The user's real phrases**, in quotes, including ones that never say "spec".
- **"Even if they never say ..."** pushes the agent to fire on indirect requests (models tend to under-trigger).
- **Negative triggers** name the neighbouring skills, so two skills don't fight over one request.

## 2. Body = steps and stop points

`SKILL.md` is a workflow, not an essay: read and gate on status → map rules → write from the template → run the script →
run the critic → report → **stop for review**. It explains *why* (three non-negotiables) instead of shouting MUST.
Every step that needs a human ends with "then stop". Approval is the user's, never the agent's.

## 3. `assets/` = templates

`spec-template.md` fixes headings, IDs (`### R1:`, `**E1.1**`) and table columns. HTML comments explain each section to
the model. The script relies on the same headings, so the template is also a contract.

## 4. `scripts/` = deterministic checks and status changes

`specs.py check` validates frontmatter, IDs, events and banned technical words; prints `ERROR`/`WARN`; exits 1 on an
error. `specs.py approve <intent>` refuses to approve while `check` fails, then sets the spec to `approved` and the
intent to `specified`.

**Statuses change through scripts, not the model.** A model can "approve" a spec by editing one line, skip a check or
forget the paired intent. A script can't: it runs the same rules every time, it updates both files together, and it
leaves a command in the log that a human typed or allowed.

## 5. `agents/` = a critic with a clean context

The writer can't judge its own draft: it's anchored on its reading and on the user's last answer. `SKILL.md` Step 4b
spawns **one** critic (Agent tool, general-purpose) and passes the *path* of `agents/spec-critic.md` plus the evidence
(spec, intent, vision, user answers), not the writer's conclusions. The brief has:
- a mandate: disagree only when it changes what gets built;
- a numbered checklist (faithful to the intent, no invented facts, examples work, ...);
- a fixed verdict format (`AGREE | DISAGREE`, BLOCKING, QUESTIONS, NOTES);
- a round limit: at most 2, then the disagreement goes to the user.

Model choice: the critic's model is the `model` you pass when spawning it, or the `model:` frontmatter if you promote
the brief to a project subagent in `.claude/agents/`. A checklist review rarely needs the writer's top model.

## 6. `evals/` = with/without comparison

`evals/evals.json` holds prompts written like a real user, an `expected_output` and checkable `assertions`.
`skill-creator` runs every prompt with and without the skill in fresh subagents and grades both.
Results: `.claude/skills/spec-writer-workspace/iteration-1/benchmark.md` (100% vs 88%).
`docs/skills/discover-bounded-contexts/design-history.md` shows four iterations: 100/70, 100/52, 94/38, 98/45. The skill
was improved where the comparison showed it, and the assertions were sharpened every round.
