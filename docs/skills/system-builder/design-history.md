# Design history: `system-builder` skill

This file records how the skill at `.claude/skills/system-builder/` was designed: the original prompt verbatim, the
decisions and their reasons, and the evaluation iterations. It's meant as teaching material and as a reference for
future changes to the skill.

- **Author:** Mike Wojtyna, with Claude Code (model: Claude Opus 5.5), using the `skill-creator` plugin
- **Started:** 2026-11-09
- **Repo state at start:** `2fb7e64 README: point to the frozen velocity-full-implementation branch as the full AI-flow reference`
- **Source run:** the frozen branch `velocity-full-implementation` and its session record
  `docs/bounded-contexts/velocity/conversation.md` (VeloCity, 8 contexts, 612 green tests, 2026-11-08)

---

## 1. Conversation timeline

1. **`/skill-creator:skill-creator`** (plan mode). The user chose "Create a new skill" and pointed at the fully
   autonomous VeloCity session: *"You remember we ran this fully autonomous session to implement bounded contexts
   based only on context map and product vision?"*
2. **User:** the scope (verbatim in section 2), then four answers:
   - target systems: any system in the repo;
   - the session record: always written;
   - evals: cheap planning evals (the runs stop before writing code);
   - the TDD loop with parallel builders: relayed through the orchestrator;
   - the settings: 3 dials + 2 presets;
   - the name: `system-builder`.
3. **Claude** wrote the plan (`~/.claude/plans/agile-crunching-sutton.md`), the user approved it, and Claude read the
   frozen branch (session record, DE answers, contract catalogue, plumbing, journeys) to extract the lessons.
4. **Iteration 1**: skill drafted, 4 evals run with and without the skill, graded, viewer opened.
5. **User:** *"done reviewing, apply the five fixes and run iteration 2"*, then *"Don't wait for my review. Run
   autonomously with all the iterations you might need."* Iterations 2 and 3 ran without further review.

## 2. The original prompt (verbatim)

> Like 1 (include **ALL** the great findings and hard-won lessons from the last session), but with **option** (this
> can be still autonomous) for the user to stay in the loop and answer all questions and stop on TDD steps when working
> on domain. Also, I want the skill to be portable, so don't adjust it only to this repo. Use the same conventions,
> rules, code, test style and so on as a default and sensible baseline, but don't hardwire it. Also, one **VERY**
> important thing: while building independent bounded contexts, spawn the builder agents using the bounded context
> scafoolder skill in parallel. These agents are spawned by orchestrator, that collects the results from each
> individual builder and merges the results to ensure all contexts communicate properly. The orchestrator writes
> journey tests, ensure bounded contexts communicate with each other only through api (using the bounded context
> discovery skill), that context in fact communicate (e.g. event produced by context A is consumed by context B, which
> fires the logic of context B to properly implement the process; the same for commands; queries and so on...). The
> orchestrator spawns also the discvoer context map skill to validate the context map vs the implementation using this
> skill and its built-in critic.

## 3. Design decisions and their reasons

### 3.1 An orchestrator that delegates, never imitates
The skill owns only the cross-cutting work no flow skill can do alone: one domain-expert voice, one published language
(the contract catalogue), the build order, the parallel builders and their merges, and the proof that the contexts
work together. Intents, specs, plans and context modules stay with their skills (`intent-creator`, `spec-writer`,
`implementation-planner`, `bounded-context-scaffolder` → `ddd-tactical-building-blocks`, `discover-bounded-contexts`).
A missing skill is reported, never silently imitated. *Why:* those skills carry their own checks and critics, and the
artifacts must stay valid for their scripts.

### 3.2 Three dials, two presets
Domain-expert questions (role-play / ask), stage gates (go on under a standing instruction / stop), and the TDD
checkpoint loop (off / on). The prompt's own words set them, and the manifest quotes those words. *Why:* the user
wanted autonomy as an *option*, and real runs mix (autonomous artifacts with the TDD loop on). The CLAUDE.md rule
"approval is always the user's" holds: going on past a gate needs an explicit standing instruction, recorded on every
approval.

### 3.3 TDD relay through the orchestrator
Builders run in the background and can't talk to the user. With the loop on, each builder ends its turn at a
checkpoint with a fixed-format report. The orchestrator batches the reports per context into one question and resumes
each builder with SendMessage. *Why:* it keeps parallelism and the user's control over every domain cycle.

### 3.4 Lessons as the core, by stage, with IDs
`references/lessons.md` holds every lesson of the VeloCity run (O1–O10, C1–C7, S1–S6, P1–P5, B1–B17, V1–V7, follow-up
classes) as *what happened → rule → why*. SKILL.md and the stage references cite them by ID. Lessons found by the evals
are marked *Found (the skill's evals)* so they're never mistaken for things the real run proved.

### 3.5 Portability: detect, then default to this repo's baseline
Step 0 reads the project's CLAUDE.md, README, build files and an existing context. Where the project is silent, the
baseline is this repo's (jMolecules, ports and adapters, records, ACLs, ArchUnit, Spring wiring, REST + `@Async`
events + Awaitility). Every value is recorded with its source in the manifest.

### 3.6 Scripts for the deterministic parts
- `waves.py`: build waves from the CML (upstream first, partnerships grouped, event-only relationships don't order
  the build, cycles named). It reproduces the VeloCity waves.
- `comm_matrix.py`: the communication matrix skeleton from the CML, and a check that every relationship has rows and
  every row a real proving test (async marker warned).
- `catalogue_coverage.py` (iteration 2): every OHS resource and published event in the catalogue is built by some plan.

### 3.7 Stage 5 belongs to the orchestrator
The VeloCity run delegated journeys to one agent and had no matrix, no API-only scan and no map-vs-code check. The
skill makes these the orchestrator's own work: API-only and channel scan, the communication matrix proving the
*consumer's effect*, journeys through the public API only, the as-built map validated by `discover-bounded-contexts`
in validate mode with its critic, then the suite twice and the PR.

## 4. Evaluations

Four cheap planning evals; each run in its own worktree, stopping before code:
1. **crowdsorcery-autonomous**: set-up and Stage 1 for a system without a CML.
2. **velocity-in-the-loop**: the first stop with the user in the loop.
3. **library-stage4-builders**: resume at stage 4 from a fixture (prep, waves, builder calls).
4. **velocity-stage5-verification**: Stage 5 on the merged VeloCity code.

The baseline is the same prompt without the skill. Baselines are strong here because they can read the frozen
branch's record; outside this repo the gap would likely be bigger.

### Iteration 1
| Eval | With skill | Without |
|---|---|---|
| 1 | 8/9 | 8/9 |
| 2 | 7/7 | 3/7 |
| 3 | 12/12 | 10/12 |
| 4 | 9/9 | 3/9 |
| **Pass rate** | **97%** | **62%** |

What the baselines caught that the skill missed became the five fixes of iteration 2:
1. dry-run the flow scripts in prep (the fixture's plans had a step table `plans.py step` can't parse);
2. check that every catalogue contract is built by some plan (`BookReturnedLate` had no producing step);
3. a stricter API-only rule: only ACL packages may import another context, and only its `api.events`; an import of
   `api.usecases`/`api.queries` is an in-process shortcut around REST;
4. name and qualify shared beans per system when several systems share one application;
5. in the loop, ask the setup questions before creating the branch.

Also: eval 1's inverted assertion replaced; outputs written to absolute paths outside the worktrees (eval 4's
baseline outputs were lost with its worktree).

### Iteration 2
| Eval | With skill | Without |
|---|---|---|
| 1 | 9/9 | 7/9 |
| 2 | 8/8 | 3/8 |
| 3 | 15/15 | 11/15 |
| 4 | 11/11 | 8/11 |
| **Pass rate** | **100%** | **66%** |

All five fixes showed up in the skill runs (eval 3 dry-ran `plans.py step`, rewrote the plans centrally, added the
missing `BookReturnedLate` steps, and qualified `libraryClock`/`libraryRestClient`; eval 2 asked S1–S5 before any
branch). Time +69 s, tokens +18 % on average.

New findings, from the baselines and the runs themselves, became iteration 3:
- **P7** every event carries what its listener needs to act (the eval-3 baseline found that `BookReturnedLate` doesn't
  say what Fines should charge);
- **B19** extended to application-wide switches (`@EnableAsync` lived only in VeloCity's config);
- **O12** each parallel agent gets its own scratch space (runs overwrote each other's files in a shared scratchpad);
- **V2** extended: one matrix row per variant with a different effect, consumer-only "half proofs", and
  `await().during` for "ignored" rows;
- journeys may call a time-driven use case as the scheduler's stand-in, named in the javadoc.
- Assertions sharpened where both configurations passed (non-discriminating), with new ones for the above.

### Iteration 3
Only evals 3 and 4 were rerun with the skill (the changes touch stages 3–5). Evals 1–2 and the baselines were carried
over from iteration 2 (the baselines don't depend on the skill) and the baselines regraded against the new assertions.

| Eval | With skill | Without |
|---|---|---|
| 1 | 9/9 (carried over) | 7/9 (carried over) |
| 2 | 8/8 (carried over) | 3/8 (carried over) |
| 3 | 17/17 | 14/17 |
| 4 | 13/13 | 7/13 |
| **Pass rate** | **100%** | **63%** |

Time +54 s, tokens +16 % on average. The eval-3 run decided the `BookReturnedLate` charging gap centrally (DE-5 and a
catalogue addendum) and gave Library its own `@EnableAsync` and scheduling switch. The eval-4 run built a 59-row matrix
(one row per variant), marked consumer-only and weak "ignored" rows, and found 11 unproven variants. Among them were
the gaps the baseline missed (the `BikeWithdrawn` fault variants, the overdue `RentalSessionEnded` to Settlement) and a
probable lost ride when Payments or Pricing fail technically. That last one isn't verified yet.

**Why iteration stopped here:** the skill passed every assertion in iterations 2 and 3, and the graders' remaining
notes are about the evals (some assertions check the builder-prompt template rather than understanding), not about
the skill. Further gains need harder evals, not more skill text.

## 5. Possible next steps
- Harder evals: a system with a Partnership group, a cyclic map that must be fixed first, a fixture with a real
  contract mismatch between two plans, and an assertion for the failure paths of cross-context calls.
- One real end-to-end run on a small system (Orders + a second context) to test the builder relay with the loop on,
  which the planning evals can't exercise.
- Description optimization (skill-creator's `run_loop`), once the skill's wording settles.

The eval workspace (outputs, gradings, benchmarks, `iteration-3/review.html`) is in
`.claude/skills/system-builder-workspace/` and isn't meant to be committed.
