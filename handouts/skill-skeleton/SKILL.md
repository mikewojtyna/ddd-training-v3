---
name: my-skill
description: <What it produces, from what input, at which path - one sentence.> Use this whenever the user wants to <verb>, <verb> or <verb> <the thing>, or says "<a phrase colleagues really type>", "<another one>", even if they never say "<the skill's own word>". Don't use it to <neighbouring task> (<other skill>) or to <another neighbouring task>.
---

# My Skill

<!-- Two or three sentences: what the output is, who reads it, and what "good" means. Explain why, don't shout MUST. -->

## Rules that always hold

<!-- 2-4 non-negotiables with a one-line reason each, e.g. "Never invent a number: unknowns become open questions". -->

## Where the output lives

<!-- The path pattern, e.g. docs/<thing>/NNNN-<slug>.md, and the template: assets/template.md. -->

## Workflow

### Step 1: Read and gate

<!-- What to read first, and when to stop and ask instead of writing (missing input, wrong status). -->

### Step 2: Draft from the template

<!-- Copy assets/template.md, keep its headings; which parts people get wrong. -->

### Step 3: Validate

<!-- python3 .claude/skills/my-skill/scripts/check.py [<file>]; fix every ERROR, read every WARN. -->

### Step 4: Independent critic

<!-- Spawn ONE critic (Agent tool, general-purpose, model from agents/critic.md) with the path of agents/critic.md and the
     evidence, not your conclusions. At most 2 rounds, then leave the disagreement to the user. -->

### Step 5: Report, then stop for review

<!-- What to tell the user in 3-5 bullets. Then stop: the human decides. Statuses change only through scripts/check.py. -->
