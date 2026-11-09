# Exercise 6: Layered vs. vertical

**Module:** M3 Modularization for safe AI collaboration · **Time:** 30 min · **Form:** pairs · **Start:** tag `ex06/start`

## Situation

You said that in your systems every feature touches every layer: a Celery task, the frontend, the infra config,
often in different repos. An agent working on such a feature has to read all of it before it can change a line,
and you pay for every token it reads.

This exercise uses **your own system**, not VeloCity. You take one feature you know well, look at what it touches
today, and redraw it around business boundaries.

## Your task

Work through `handouts/worksheet.md`, in pairs, on one feature from the system of one of you.

1. **Part A, before (10 min):** pick a typical feature that recently changed. List every repo, service, layer and
   file it touched.
2. **Part B, after (12 min):** redraw the same feature as bounded contexts. Start from the business events: which
   event marks the moment one part of the business hands the work over to another?
3. **Part C, the agent's context (8 min):** estimate what an agent has to read for the change, before and after.
   Use the rule of thumb on the worksheet, or measure if you have the code at hand.

## Done when

- Part A lists at least five places the feature touches, with the repo or layer of each.
- Part B shows at least two contexts and the pivotal event between them, and says which context owns the change.
- Part C has two numbers (before and after), each marked **measured** or **estimated**.

## Debrief questions

- Which part of your feature had no business reason to be where it was, only a technical one?
- Did any pivotal event surprise you? Was it already a message on a queue, a Celery task, a webhook?
- How much smaller did the agent's context get? What stayed big, and why?
- What would you have to move first in your real system to get there?
- Compare with the trainer's worked example for VeloCity: `git show ex06/solution:handouts/WORKED-EXAMPLE.md`.

## Rules

- Your system, not VeloCity. If you can't show real code, sketch from memory, but be specific: names, not "the backend".
- "Frontend", "backend" and "infra" are not bounded contexts.
