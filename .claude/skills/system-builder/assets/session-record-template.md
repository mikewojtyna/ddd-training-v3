# How <System> was built in one session

*A recorded Claude Code session, <YYYY-MM-DD>.*

This is the record of a single session in which Claude built every <System> bounded context, from <the context map
and vision> through canvases, intents, specs and plans to tested code. It's meant for review and for teaching. It shows
the AI-assisted DDD flow run end to end, and it shows where a human would normally have stopped the flow and what
happened instead.

**Reading tips.**
- The user's prompt is quoted verbatim. Everything after it is Claude's narrated log: what it read, what it decided and
  why, which agents it started, what the checks reported. Tool outputs are summarised, not pasted.
- <Role-play setting:> Where the vision was silent or contradictory, Claude **role-played the domain expert**. Every
  faked answer is marked 🎭 and repeated in the artifact it shaped, so a reviewer can find and overrule each one.
- <Standing instruction:> Wherever a status was moved forward under the user's standing instruction, the artifact says
  so. The real review is the pull request.
- <In the loop:> The user answered the expert questions (👤), approved each stage and drove the TDD checkpoints. Each
  round is summarised where it happened.

---

## 👤 User

> <the prompt, verbatim, every line quoted>

---

## 🤖 Claude

### Getting oriented
- Branch, inputs read, skills read, baseline test run, detected conventions, dials and how they were granted.
- How the skills' review gates were handled under these dials.

### Stage 1: bounded context canvases
**Inputs:** … **Output:** …
1. The expert answers: how many, examples of the decisions that matter (🎭 DE-n …).
2. Writing the canvases.
3. Context-map insights, and the validator result.
4. Critic: verdict, the blocking findings, what changed.

### Stage 2: intents and specs
Story map and slicing, parallel agents, the gaps they found (second-round answers), naming clashes with the tooling,
checks and approval.

### Stage 3: implementation plans
Contract catalogue, parallel planners, what they sent back (contract gaps, spec bugs found by arithmetic, risks),
checks and approval.

### Stage 4: implementation
Prep (the plumbing, the smoke test and what it caught, the published events). A waves table. The surprises. A
results table per context:

| Context | Aggregates | Domain tests | Spring acceptance tests | Notable decisions |
|---|---|---|---|---|

<In the loop: a summary of the TDD relay rounds, the decisions the user made at checkpoints.>

### Stage 5: integration and verification
API-only scan, communication matrix (rows, how many tests had to be added), journeys (and what they found), map vs
code (the discovery verdict and the changes), the final verification numbers by test kind, the checks of the flow
scripts.

### What the reviewer should look at first
1. **The expert answers** (`domain-expert-answers.md`): the decisions that shape the most behaviour.
2. **Context-map changes.**
3. **Process deviations:** the loop setting, approvals under the standing instruction, batched cycles, skipped critics.
4. **Known follow-ups**, listed in the context plans.
5. **Open questions** left in the specs for a real domain expert.
