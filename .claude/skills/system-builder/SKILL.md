---
name: system-builder
description: Build a whole system of bounded contexts end to end, from a context map and a product vision to tested, integrated code, by orchestrating the AI-assisted DDD flow. It covers canvases, then intents and specs, then a contract catalogue and plans, then parallel builder agents (one per context, each using bounded-context-scaffolder) in dependency waves, then merging, journey tests, a communication matrix proving every relationship works, an API-only check and a map-vs-code validation with discover-bounded-contexts. It runs fully autonomously (role-played domain expert, approvals under a standing instruction, one commit per stage, a PR at the end) or with the user in the loop: answering expert questions, approving each stage, and driving the TDD checkpoints relayed from the parallel builders. It always writes a session record. Use this whenever the user wants to implement, build, deliver or rebuild all, several or "the rest of" the bounded contexts of a system, or "the whole system", from its context map, canvases or vision. Also use it for "implement everything while I'm away", "do the full flow for CrowdSorcery", "build all contexts and their integrations", or to resume or continue a system build that is already under way, even if they never say "orchestrate". Don't use it for one intent (implementation-planner), one context from its canvas (bounded-context-scaffolder) or only finding boundaries (discover-bounded-contexts).
---

# System builder

Builds a whole system: from the context map and the vision, every bounded context ends up in code, integrated with the
others, and proven by tests. It **orchestrates** the flow skills and keeps the cross-cutting work they can't do alone:
- one domain-expert voice for all artifacts;
- one published language;
- the build order;
- the parallel builders and their merges;
- the proof that the contexts really work together.

It was distilled from a real autonomous run that built VeloCity's eight contexts in one session (612 green tests).
**`references/lessons.md` holds everything that run taught. It is the reason this skill exists.** Read the section for
each stage before starting that stage.

Read when needed:
- `references/lessons.md`: every hard-won lesson, by stage (IDs such as O3, B13 are cited below).
- `references/stage-1-canvases.md` … `stage-5-integration.md`: the procedure for each stage.
- `references/worked-example-velocity.md`: the reference run in two pages. Read it once per session.
- `agents/context-builder.md`: the brief every builder agent reads. `agents/stage-critic.md`: the stage critic's brief.
- `assets/`: templates for the manifest, the expert answers, the contract catalogue, the communication matrix and the
  session record.
- `scripts/waves.py`: the build waves from the CML. `scripts/comm_matrix.py`: the communication matrix, generated and
  checked. `scripts/catalogue_coverage.py`: every contract of the catalogue is built by some plan.

## Who does what

| Work | Owner |
|---|---|
| Expert answers, canvases, story map and intent manifest, contract catalogue, shared decisions, plumbing, published events, waves, merges, journeys, communication matrix, API-only scan, as-built map, session record, PR | **this skill (the orchestrator)** |
| Finding or validating contexts and the map (also map vs code at the end) | `discover-bounded-contexts` |
| Intents / specs | `intent-creator` / `spec-writer`, through parallel area agents |
| Placement and step plans | `implementation-planner` (plan only), through parallel planners |
| One context's module (canvas → code) | `bounded-context-scaffolder`, through one **builder agent per context** |
| The domain of a context, test-first | `ddd-tactical-building-blocks`, which the scaffolder hands rules to |

Don't do a flow skill's job yourself. If a skill is missing, say so (Step 0).

## The dials

Three independent switches, set once at the start and recorded in the manifest:

| Dial | Autonomous preset | In-the-loop preset |
|---|---|---|
| **Domain-expert questions** | role-play 🎭: collected and answered centrally, cited everywhere (O3) | ask the user in batched rounds, each question with a proposed answer |
| **Stage gates** | go on: approve through the flow scripts "under the standing instruction of <date>", commit, continue. The PR is the review (O7) | stop after every stage for the user's approval |
| **TDD checkpoint loop** | off: still test-first, one example per cycle, any batching disclosed (O10) | on: builders stop at each checkpoint, and the orchestrator relays the checkpoints to the user in batches |

Critics follow from the dials: one stage critic per stage when autonomous (O8), each skill's own critics in the loop.

**Reading the dials from the prompt.** "I'll be away", "don't ask me", "fully autonomous", "fake the expert" grant
the autonomous settings. "I want to answer the questions", "I'll drive the TDD", "stop after each stage" set the
in-the-loop ones. Mixed is fine, for example autonomous artifacts with the TDD loop on. Quote the words that set
each dial in the manifest.
- **Approval is the user's.** Going on past the gates needs an explicit standing instruction in the prompt.
- If the prompt is ambiguous, ask once with AskUserQuestion, offering the two presets and "custom".
- If you can't ask, default to the in-the-loop gates and say why.
- The project's `CLAUDE.md` may say that every step stops for the user's review. A standing instruction is the user's
  own decision to waive that review for this run. Say so in the record.

## Step 0: Start or resume

1. **Resume?** Look for the manifest (`<system docs>/system-build.md`, by default
   `docs/bounded-contexts/<system>/system-build.md`). If it exists, read it and the session record, check the branch
   and the builder agents it lists, and continue at the first unfinished item. Don't redo finished stages. A user who
   says "go on" or "plans are approved, build it" usually means resume.
2. **Detect the project's conventions.** Read the project's `CLAUDE.md`, README and the system's `CLAUDE.md`, the build
   files and an existing context. Collect:
   - the build/test command;
   - the base package and context layout;
   - the docs locations;
   - the CML location;
   - the integration style;
   - what the user allowed to delete.

   Where the project says nothing, **use this baseline**:
   - jMolecules annotations;
   - ports and adapters (`api/{usecases,events,queries}`, `domain`, `infra/{adapters,config}`);
   - records for messages;
   - aggregates return domain events and use cases publish them;
   - ACLs for cross-context calls;
   - an ArchUnit test per context;
   - Spring wiring in `infra/config`;
   - REST + in-process `@Async` events + Awaitility.

   Record every value, with its source, in the manifest. The project's rules always win over the baseline.
3. **Check the skills.** Required: `bounded-context-scaffolder`, `ddd-tactical-building-blocks`,
   `discover-bounded-contexts`. Used when present: `intent-creator`, `spec-writer`, `implementation-planner`.
   - If a required skill is missing, stop and say which.
   - If an optional one is missing, agree with the user to skip that stage's artifacts, or to follow the skill's
     documented shape. Never imitate a skill silently.
   - Note the flow scripts (status changes go **only** through them).
4. **Set up.**
   - **In the loop:** if the prompt leaves the setup open (what may be removed, the integration style, the branch),
     ask those questions first, each with a proposed answer, in the first stop. Write the files below uncommitted and
     create the branch only after the answers (O11).
   - Create the integration branch (O1).
   - Run the full suite once as the baseline.
   - Create the manifest (`assets/manifest-template.md`), the session record (`assets/session-record-template.md`,
     with the user's prompt **verbatim**, O2) and the expert-answer file (`assets/domain-expert-answers-template.md`).

## Stage 1: canvases

Follow `references/stage-1-canvases.md`.
- **No CML yet:** run `discover-bounded-contexts` first.
- Collect every expert question first and answer it in one file. Answers are role-played or proposed, depending on
  the dial.
- **Write all the canvases yourself** (C1), as strict Given/When/Then with concrete values. Every inbound command gets
  a rule.
- Read the canvases against each other (C3, C4).
- Update and validate the CML where the canvases prove it wrong (C6).
- Run the stage critic, then commit.

## Stage 2: intents and specs

Follow `references/stage-2-intents-specs.md`.
- **You** write the story map, the intent manifest (sequence, actor, activity, task) and the shared event list (S1,
  S2).
- Then spawn one agent per area **in a single message**, all with one brief: follow `intent-creator` and
  `spec-writer` exactly, use the DE answers, self-review against the spec critic's checklist, and fix every check
  error.
- Answer the gaps they find as a new DE round, and resume the same agents with SendMessage (O4).
- Run the checks, approve per the dial, and commit.

## Stage 3: contract catalogue and plans

Follow `references/stage-3-plans.md`.
- **Write the contract catalogue before any planner starts** (P1):
  - OHS resources;
  - published events with payloads and listeners;
  - error semantics and formats;
  - event-only relationships.
- Check that every event carries what each listener needs to act (P7).
- Fix the shared decisions D1…Dn (P2).
- Draft the communication matrix (`comm_matrix.py init`).
- Spawn the parallel planners: `implementation-planner` up to the written, checked plan, with no building and no
  approving. They recompute every number (P4).
- Fold the contract gaps into one addendum (P3), and send spec bugs back to the spec.
- **Check that every contract is built:** `catalogue_coverage.py <catalogue> <plans-dir>` reports 0 uncovered (P6).
  An event or resource that no plan produces gets a step in the producer's plan.
- Run the checks, approve per the dial, and commit.

## Stage 4: parallel builders

Follow `references/stage-4-build.md`.
1. **Prep, yourself, in one commit** (B1–B10, B18, B19):
   - the shared clock and REST client, named and qualified per system if the application hosts other systems (B19);
   - the REST client resolves the port at call time;
   - `@EnableAsync`;
   - the scheduling switch;
   - the error body;
   - the system test annotation with a mutable clock;
   - Awaitility;
   - **all published integration events**;
   - a throwaway smoke test of the risky plumbing;
   - a **dry run of every flow script the builders call** (`plans.py step` on a copy of the docs, one real step per
     plan), with any format problem fixed now (B18);
   - **re-run `catalogue_coverage.py`**: a published event that no plan step produces is never built (P6);
   - the allowed removals.
2. **Waves:** `python3 <this skill>/scripts/waves.py <map.cml> --event-only "<from the catalogue>"` (B11).
3. **Spawn one builder agent per context** that is startable now, **in parallel, in a single message**:
   `subagent_type: general-purpose`, `isolation: "worktree"`, named `builder-<context>`. Each builder:
   - reads `agents/context-builder.md`;
   - starts with `git merge --ff-only <integration branch>` (B13);
   - builds its context with **`bounded-context-scaffolder`**, which hands the domain to
     `ddd-tactical-building-blocks`;
   - touches only what it owns (B14).

   Give each builder its work package, the dial values, the smoke test's traps and its own temporary directory
   (parallel agents that share a scratch folder overwrite each other, O12).
4. **TDD loop on:** relay the checkpoints. Collect the checkpoint reports of the running builders, ask the user one
   batched question grouped per context, and resume each builder with SendMessage (stage-4 reference, §4).
5. **Merge as each builder finishes.**
   - Verify the report (O9): the full suite, the diff limited to the builder's ownership, the claimed tests.
   - Merge, then run the full suite.
   - Then `waves.py --merged …` and spawn whatever has become startable (B12).
   - Decide contract gaps centrally.

## Stage 5: integration and verification, yourself

Follow `references/stage-5-integration.md`, in order:
1. **API-only, through the agreed channel:** every context has its architecture test, and a global scan of imports
   between contexts comes up empty, or with recorded exceptions only. With REST for commands and queries, a context
   may import another context only from its ACL packages and only that context's `api.events`. An import of another
   context's `api.usecases` or `api.queries` is an in-process shortcut around REST and the ACL: a violation (V3).
   Run the architecture tests; don't only read them.
2. **Communication matrix:** every relationship of the map has a row per message, and each row has a test proving
   the *receiving context's effect* (with Awaitility for events). A variant with a different effect gets its own row;
   a test that publishes the producer's event itself proves only the consumer side (V2). Write the missing tests. Run
   `comm_matrix.py check` until there are 0 errors.
3. **Journeys:** one per business process, only through the public API, with every effect in another context
   awaited (V1).
4. **Map vs code:** extract the as-built map (ACL calls, listeners), then invoke **`discover-bounded-contexts` in
   validate mode** with its critic. Fix the code or the CML, as its verdict says.
5. **Deliver and verify:**
   - `plans.py done` for each finished plan, then the three flow checks at 0 errors;
   - the full suite **twice** (V5);
   - housekeeping (V6);
   - finish the record with the "look here first" list (V7);
   - open the PR. Don't merge it.

## Gates and reporting

- **At each stage gate:** update the manifest, write the stage into the session record, then commit.
  - Gates on stop: give the user a short summary (paths, decisions that need their eye, critic verdict, questions)
    and stop.
  - Gates on go: continue.
- **While agents run:** keep the user posted in one line per event (a wave started, a builder merged, a gap
  decided). Never predict an agent's result. Wait for its notification.
- **The final message:**
  - the PR link;
  - contexts, intents and test counts by kind;
  - map changes;
  - the reviewer's look-here-first list;
  - what is still open.

  Keep it short: the record holds the details.

## Principles

- **One voice, one language, one order.** The orchestrator alone decides expert answers, message names, contracts and
  the build order. Agents report gaps and never settle them among themselves.
- **Parallel where independent, sequential where a decision is shared.** Canvases, the catalogue and the plumbing are
  written in one hand. Specs, plans and contexts fan out.
- **Every faked or defaulted decision is visible:** 🎭 in the artifacts, the standing-instruction note on the
  approvals, deviations in the record.
- **Trust tests, not reports.** Verify each agent's claims before merging. A relationship counts as working only
  when a test shows the other side reacted.
