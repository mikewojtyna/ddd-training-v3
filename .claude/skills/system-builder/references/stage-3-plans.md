# Stage 3: contract catalogue and plans

**Input:**
- the intents and specs;
- the context map;
- the canvases.

**Output:**
- `integration-contracts.md` with its addendum;
- one plan per intent, all `plans.py check` clean and approved through the script;
- a draft communication matrix;
- the stage-3 commit.

Lessons that apply: P1–P7, O4, O5, O7, O12.

## 1. The contract catalogue: written by the orchestrator, before any planner starts

Write `integration-contracts.md` from `assets/integration-contracts-template.md` (P1). It is the technical side of the
context map:
- **Integration style**, from the user's prompt or the project's conventions. The baseline (the VeloCity run):
  - commands and queries between contexts go over REST, through `@AntiCorruptionLayer` adapters in the caller;
  - integration events go over the in-process Spring bus, with `@Async @EventListener` in an ACL primary adapter of the
    consumer;
  - tests wait with Awaitility.
- **Error semantics** (409 rule broken / 404 unknown / 400 malformed, with one body shape), **formats** (money, times,
  positions) and the **shared clock**.
- **Per context:** every Open Host Service resource (message, method + path, body, result), every published event
  with its payload and its listeners, and what it consumes.
- **Event-only relationships:** the map relationships where the downstream only listens. They feed
  `waves.py --event-only`.
- **Payload sufficiency (P7):** for each event and each listener, walk the listener's rule once. Every piece of data
  it needs to decide and act is in the payload, in the listener's own state, or behind a query in the catalogue. If
  not, decide it now (a field, a query, a DE answer).

Path style and naming come from the project's conventions, for example `/<context-kebab>/<resource>/{id}/...`.

Then initialise the communication matrix: `python3 <skill>/scripts/comm_matrix.py init <map.cml> > <docs>/communication-matrix.md`.
Fill in its *Message*, *Kind* and *Effect* columns from the catalogue. The *Proving test* column waits for Stage 5.

## 2. Shared decisions

Fix the run-wide decisions once (P2) and list them in the manifest, numbered:
- **D1** the TDD checkpoint loop: from the dial;
- **D2** the aggregate style default;
- **D3** one ACL per outbound call;
- **D4** in-memory repositories, with `Version` added only after the domain is green;
- anything the user's prompt fixes, such as the integration style.

Every plan cites them in its Decisions section.

## 3. Parallel planners

Use the same areas as in Stage 2. Spawn one agent per area in a single message. The brief:
- follow `implementation-planner`'s SKILL.md exactly, up to and including writing the plan and running
  `plans.py check`. **Don't build, and don't approve.**
- inputs: its intents, their specs, the CML, all canvases, `integration-contracts.md` (binding), the shared decisions,
  the DE answers;
- place every rule, command, query and event with a reason; translate the spec vocabulary into the owning context's
  language; map the contract to use cases, REST paths and events from the catalogue; list missing canvas examples;
  order the steps upstream first;
- recompute every numeric example (P4). A wrong example goes back as a spec bug, with the arithmetic;
- report back:
  - **contract gaps** (a missing response field, an endpoint the catalogue lacks);
  - **spec bugs**;
  - risks (deletions that break references, P5);
  - the `plans.py check` output.
- critic: the planner skill's own critic in the loop. In the autonomous setting the stage critic replaces it (below);
  tell the planners to skip theirs.

## 4. Fold the results back

- Fold every contract gap into one dated **addendum** of the catalogue (P3). Resume the affected planners
  (SendMessage) to adjust their plans.
- Spec bugs: set the spec back to `draft`, fix it, re-approve it through `specs.py approve`, and resume its planner.
  Never plan around a wrong example.
- Don't apply the canvas changes the plans list. Plans *propose* them, and the builders apply them in Stage 4 as their
  plans' first steps.

## 5. Critic, checks, approval

- **Autonomous:** one stage critic (`agents/stage-critic.md`, `stage: 3`) over the plans' placement and the
  catalogue, given the evidence.
- `plans.py check` across all plans: 0 errors.
- `python3 <skill>/scripts/catalogue_coverage.py <docs>/integration-contracts.md docs/plans`: 0 uncovered (P6). Every
  OHS resource and published event must appear in a plan that builds it. For each uncovered one, add a step to the
  producing context's plan (resume its planner), or drop the contract from the catalogue if nobody needs it.
- **Approval:**
  - **autonomous:** `plans.py approve <intent>` for each plan, under the standing instruction (O7), then
    `intents.py build`;
  - **in the loop:** present the placement summary per area, the catalogue and the shared decisions. Stop. The user
    approves.

## 6. Commit

Commit: `<System> stage 3: implementation plans for all <n> intents`. Record the contract-gap count, the spec bugs found
and the risks.
