# Exercise 15: Living docs

**Module:** M7 Advanced agent setup & SDD · **Time:** 20 min · **Form:** group discussion · **Start:** tag `ex15/start`

## Situation

Your teams already write things down: ADRs, specs, wiki pages, READMEs. An agent can read all of them, or none.
If it reads too much, you pay in tokens and it gets confused. If it reads a stale doc, it trusts the doc more than
the code and builds the wrong thing. If it reads nothing, it guesses.

One of you already writes ADRs. We start from one of them.

## Your task

1. **(3 min)** The architect who uses ADRs shows one real ADR on screen (or describes it, if it can't be shown).
   Everyone else: note what an agent would need from it, and what it wouldn't.
2. **(12 min)** Discuss the questions in `handouts/discussion-guide.md`. Fill in `handouts/doc-placement-template.md`
   together, one row per kind of document your teams have. The trainer writes on the shared screen.
3. **(5 min)** Agree on **one** change each team will try next week: a doc to move, shrink, delete, or put a
   staleness check on.

## Done when

- The placement table has at least five rows, each with all six columns filled.
- For the ADR on screen, the group agreed whether an agent reads it always, on demand or never, and where it lives.
- Each team named its one change for next week.

## Debrief questions

- Which of your documents would you never let an agent read? Why: stale, too big, or secret?
- Which rule is written in two places today? Which copy wins when they disagree?
- How would you notice that a doc became stale **before** the agent acts on it?
- Compare with how this repo does it: `git show ex15/solution:handouts/FACILITATOR-NOTES.md`.

## Rules

- Talk about your real documents, not documents in general.
- Every "the agent should read X" needs an answer to "who keeps X current?".
