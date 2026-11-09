# Exercise 13: Build your team's skill

**Module:** M7 Advanced agent setup & SDD · **Time:** 75 min · **Form:** pairs · **Start:** tag `ex13/start`

## Situation

You've used `spec-writer`, `implementation-planner` and the critics all week. Each of them is a small folder:
a description that decides *when* it runs, a body with steps and stop points, a template, a script that checks the
output and changes statuses, a critic with a clean context, and evals that prove the skill beats no skill.

Your team has recurring tasks that deserve the same treatment. Today you build one.

Read first (5 min): `handouts/skill-anatomy.md`. Ideas if you have none: `handouts/task-ideas.md`.
Starting files: `handouts/skill-skeleton/`.

## Your task

1. **Pick one recurring task (5 min)** from your real work: something you do at least monthly, where the output has a
   shape and a reviewer usually finds the same mistakes. Write down in one sentence what a good result looks like.
2. **Copy the skeleton** to `.claude/skills/<your-skill>/` (kebab-case name, the same as `name:` in the frontmatter):
   ```bash
   cp -r handouts/skill-skeleton .claude/skills/<your-skill>
   ```
3. **Write it with `skill-creator` (35 min).** Ask the agent, for example: *"Use skill-creator to turn
   .claude/skills/&lt;your-skill&gt; into a skill for &lt;task&gt;. Keep the folder layout. Don't run evals yet."*
   Then make sure you end up with:
   - a **sharp description**: what it produces, the phrases users really say, and when *not* to use it;
   - **one template** in `assets/`;
   - **one script check** in `scripts/check.py` (stdlib only, prints `ERROR`/`WARN`, exits 1 on an error). If your
     output has a status, the script changes it, not the model;
   - **one critic** in `agents/critic.md` with a checklist, a verdict format and a `model:` line;
   - at least one **stop point** where the human decides.
4. **Write two eval prompts (5 min)** in `evals/evals.json`: one typical request, one tricky one (missing information,
   a request the skill should refuse, or a status change). Write prompts the way a colleague would type them, with
   real details, and add 3–6 checkable assertions each.
5. **Run them with and without the skill (20 min)**, see below. Compare the outputs side by side.
6. **Fix one thing** the comparison showed you: usually the description or the critic's checklist.

### How to run the evals with and without the skill

This repo's skills were tested the way `skill-creator` does it (see
`.claude/skills/spec-writer-workspace/iteration-1/` and `docs/skills/discover-bounded-contexts/design-history.md`):
the same prompt runs twice, once with the skill and once without, in fresh contexts, and the outputs are graded
against the same assertions.

- **With Claude Code (recommended):** *"Use skill-creator to run the evals in .claude/skills/&lt;your-skill&gt;/evals/evals.json
  with and without the skill, one run each, and open the review viewer."* It spawns one subagent per prompt and
  configuration and writes `.claude/skills/<your-skill>-workspace/iteration-1/eval-<id>/{with_skill,without_skill}/`.
  Skip the description-optimization loop today, it takes too long.
- **By hand (Copilot, Gemini, or no subagents):** open a fresh chat per run.
  - *Without:* paste only the eval prompt.
  - *With:* paste the SKILL.md (or reference it as a prompt file/instructions) and then the prompt.
  - Save both answers to `.claude/skills/<your-skill>-workspace/iteration-1/eval-<id>/{with_skill,without_skill}/`,
    run your `check.py` on both, and tick the assertions in a small table.
- Run each configuration in a **clean context**. A chat that has seen the skill is no longer a baseline.

## Done when

- `.claude/skills/<your-skill>/` has `SKILL.md`, one template, `scripts/check.py`, `agents/critic.md` and
  `evals/evals.json` with two prompts.
- `python3 .claude/skills/<your-skill>/scripts/check.py` exits 0 on a good output and 1 on a broken one.
- You have a with/without table for both prompts and can name one assertion the skill passes and the baseline fails.
  If there is none, you know whether the skill or the evals are the problem.

## Extension

Run the critic with a cheaper model (`model: haiku` instead of `sonnet`, or the cheapest model your LLM farm offers)
on the same draft. Compare the verdicts, the findings, the tokens and the time. Where did the cheaper critic miss
something, and where was it just as good?

## Debrief questions

- What makes your description sharp? Which phrase would make it fire when it shouldn't?
- What did you move from the prompt into the script, and why does the script decide it better than the model?
- What did the critic catch that the writer missed? What did it miss?
- Did the baseline do better than you expected? What does that say about the skill?
- Compare with the worked example: `git diff ex13/start ex13/solution` (an `adr-writer` skill) and
  `handouts/SOLUTION-NOTES.md`.

## Rules

- Keep the skill small: one template, one script, one critic. Breadth comes later.
- No customer code, credentials or personal data in eval prompts or test files.
- Statuses change only through your script. The skill stops for the human before anything is approved.
