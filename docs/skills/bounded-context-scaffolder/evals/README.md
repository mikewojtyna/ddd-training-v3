# Evaluation results: bounded-context-scaffolder

These are snapshots of the skill-creator eval runs described in `../design-history.md`.

- `iteration-N/review.html`: open it in a browser. The "Outputs" tab has every run's prompt, final message, plan, `package-info.java`, diff and build log. The "Benchmark" tab has pass rates, timing, tokens and analyst notes. It's a static page, so the feedback box doesn't save.
- `iteration-N/benchmark.md` and `benchmark.json`: the aggregated scores.
- `iteration-N/feedback.json`: the user's review feedback, verbatim.
- `iteration-N/eval-*/<config>/run-1/`:
  - `outputs/`: what the run produced. `files/` holds the changed files at their repo paths; they are copies, not compiled, and not part of the build.
  - `grading.json`: the checks.
  - `timing.json`: time and tokens.
- The test prompts, canvases and grading script are in `.claude/skills/bounded-context-scaffolder/evals/`.

Iteration 2 reuses the iteration-1 baselines, and each with-skill run there has two phases: `phaseA_*` is the stop for review, the rest is the resumed implementation.
