# Evaluation results: discover-bounded-contexts

These are snapshots of the skill-creator eval runs described in `../design-history.md`.

- `iteration-N/review.html`: open it in a browser. The "Outputs" tab shows every run's prompt, transcript, final message, discovery report and CML; from iteration 2 on, the previous iteration's output is shown collapsed. The "Benchmark" tab shows pass rates, timing, tokens and analyst notes. It's a static page, so the feedback box doesn't save.
- `iteration-N/benchmark.md` and `benchmark.json`: the aggregated scores, plus the analyst notes.
- `iteration-4/feedback.json`: the user's review feedback, verbatim. All boxes are empty, meaning every output looked fine.
- `iteration-N/eval-*/<config>/run-1/`:
  - `outputs/transcript.md`: every facilitator message and simulated-expert reply, verbatim, plus one line per critic verdict.
  - `outputs/final_message.md`: the last message to the user.
  - `outputs/files/`: the files the run would have written, at their repo paths (discovery report, CML, canvases). They are copies, not part of the build.
  - `grading.json`: the merged mechanical and judgment checks, with evidence.
  - `timing.json`: time and tokens. Tokens don't include the nested critic and expert subagents.
- The test prompts, the simulated-expert answer sheets and the grading script are in `.claude/skills/discover-bounded-contexts/evals/`.

The assertions changed between iterations (they were sharpened after each round), so pass rates are only comparable within an iteration. Each configuration ran once, and the simulated expert sometimes answered the same question differently across runs.
