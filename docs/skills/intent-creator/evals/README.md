# Evaluation results: intent-creator

These are snapshots of the skill-creator eval runs described in `../design-history.md`.

- **`iteration-N/review.html`:** open it in a browser.
  - The "Outputs" tab shows each run's prompt, final message, intent files and diff.
  - The "Benchmark" tab shows pass rates, timing, tokens and analyst notes.
  - It's a static page, so the feedback box doesn't save.
- **`iteration-N/benchmark.md` and `benchmark.json`:** the aggregated scores.
- **`iteration-N/eval-*/<config>/run-1/`:**
  - `outputs/files/` holds the changed files at their repo paths. They are copies, not part of the repo's `docs/intents`.
  - `grading.json` holds the checks, and `timing.json` the time and tokens.
- **The test prompts, the seed intents for eval 3 and the grading script** are in `.claude/skills/intent-creator/evals/`.

Each run worked in its own git worktree, so the runs couldn't see each other's `docs/intents`. In eval 3 the seed intents were committed in that worktree first, so the diff shows only what the run changed.
