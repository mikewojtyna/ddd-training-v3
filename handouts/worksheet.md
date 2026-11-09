# Worksheet: layered vs. vertical

Pair: ______________________ · System: ______________________

The feature (one sentence, in business words, e.g. *"a simulation run can be paused and resumed"*):

> ______________________________________________________________________

Pick a change that really happened, recently. You need to remember the files, not to imagine them.

---

## Part A: before (10 min)

List every place the feature touched. One row per file or component you'd have to open.

| # | Repo / service | Layer (UI, API, service, task/worker, data, infra, config) | File or component | Why it had to change |
|---|---|---|---|---|
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |
| 5 | | | | |
| 6 | | | | |
| 7 | | | | |
| 8 | | | | |

Count them:

| Repos | Services / deployables | Layers | Files | Teams that had to review |
|---|---|---|---|---|
| | | | | |

Mark with a ★ every row that changed **only because another row changed** (a DTO copied, a task signature, a
config key). Those are the seams a boundary should hide.

---

## Part B: after (12 min)

Redraw the same feature as bounded contexts.

**Step 1: the business events.** Write the feature as a timeline of things that happened, in the past tense, in
business words (*"Simulation Requested"*, *"Mesh Generated"*, *"Results Published"*). No "API called", no "row saved".

| # | Business event | Who cares about it next |
|---|---|---|
| 1 | | |
| 2 | | |
| 3 | | |
| 4 | | |
| 5 | | |

**Step 2: the pivotal events.** Which events mark a hand-over: after it, a different part of the business takes
over, uses different words, or works on a different clock? Circle them. They're your candidate boundaries.

Ask yourselves:
- After this event, does the same word mean something else? ("job" for the scheduler vs. "run" for the engineer)
- Could the work before and after it be done by two different teams without a meeting?
- Is there already a queue, a Celery task or a webhook here? Is it there for a business reason or a technical one?

**Step 3: the contexts.** Draw boxes between the pivotal events. Name each box with a business name.

```
 [ context 1: ____________ ] --(pivotal event: ____________)--> [ context 2: ____________ ] --(____________)--> [ ... ]
```

For each context, what does it own **end to end**?

| Context | Its API (what the UI or others call) | Its async jobs / tasks | Its data | Its config / infra |
|---|---|---|---|---|
| | | | | |
| | | | | |
| | | | | |

**Step 4: the change again.** In the new layout, which context owns the change from Part A? Which rows from Part A
disappear or move inside that one context?

> Owner: ____________ · Rows from Part A that remain: ____________ · Rows that disappear: ____________

---

## Part C: the agent's context, before and after (8 min)

What does an agent have to **read** to make the change safely: the code it changes, the tests it must update, the
callers it must check, the docs and instructions it loads?

| | Before (Part A) | After (Part B) |
|---|---|---|
| Files the agent must read | | |
| Lines of code (sum) | | |
| Estimated tokens | | |
| Measured or estimated? | | |
| Repos the agent needs access to | | |
| Can it run all affected tests in one place? | | |

### Rule of thumb

- **About 10 tokens per line of code** (Java, Python, TypeScript). Or about **4 characters per token**: file size in
  bytes ÷ 4.
- Add what the agent loads anyway: instruction files (`CLAUDE.md`, `AGENTS.md`, `.github/copilot-instructions.md`),
  the files it opens by mistake, and the test output it reads.
- Reading is usually the larger part of the bill. A change of 1 line that needs 5,000 lines of reading costs
  ~50,000 tokens, every time the agent starts fresh.

Quick count of lines:

```bash
wc -l path/to/file1 path/to/file2 ...            # lines per file and total
cat path/to/files... | wc -c                       # bytes, ÷ 4 for tokens
```

### How to measure instead of estimate

- **Claude Code:** run `/context` after the agent finished the change: it shows how much of the context window is
  used and by what. The session's token count (`/cost` or the status line, depending on your setup) shows the total.
- **GitHub Copilot (chat / agent mode):** the answer lists the **referenced files**. Count them and add up their
  lines.
- **Any tool:** ask the agent at the end: *"List every file you read for this change."* Then `wc -l` them. It's an
  estimate, but based on what it really opened.

---

## Your conclusion (one sentence)

> The agent's context shrinks from ______ to ______ because ______________________________________________.
