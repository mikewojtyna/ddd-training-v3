# Stage 4: building the contexts in parallel

**Input:**
- the approved plans;
- the contract catalogue;
- the canvases;
- the CML.

**Output:**
- every context in code, built by parallel builder agents that each use `bounded-context-scaffolder`;
- each builder's branch merged into the integration branch;
- the full suite green after every merge;
- the stage-4 commits.

Lessons that apply: B1–B19, P6, O9, O10, section 7 of `lessons.md`.

## 1. Prep, by the orchestrator (one commit before any builder starts)

Builders run in parallel and may not touch shared code (B14), so everything shared must exist first. The baseline is
Spring/Java, as in this repo. For another stack, build the equivalent and name it in the manifest.

0. **Look at what the application already hosts.** If one application runs several systems, find the beans of the
   types you're about to add (`Clock`, `RestClient`, task executors, `@Primary` test beans). If any exist, the
   system's beans get system-specific names (`<system>Clock`) and are injected through a system qualifier annotation
   (`@<System>Clock`) or `@Qualifier`; the test clock replaces the system's clock by name, not by `@Primary` (B19).
   Builders inject the qualified beans. Put the qualifier names into the builder brief. Check the application-wide
   switches too: if `@EnableAsync` or `@EnableScheduling` lives only in another system's config, declare it in this
   system's config as well (they are idempotent). Don't edit the other system's wiring without the user.
1. **Shared runtime plumbing** in the system's common package (`<system>/common/infra`), wired in a config class:
   - the one clock bean (B1);
   - a REST client plus a service-URL helper that resolves the app's own port at call time and can be overridden by
     a property (B2);
   - `@EnableAsync` (B3);
   - a scheduling config behind a property, `matchIfMissing = true` (B4);
   - the error-body record (B5).
2. **Shared test plumbing:**
   - the system test meta-annotation: `@SpringBootTest(webEnvironment = RANDOM_PORT)`, importing a test config with a
     `@Primary` mutable clock, with scheduling off (B6);
   - a `unique(prefix)` id helper;
   - Awaitility in the build file;
   - its javadoc states the rules of the shared context: unique ids, no assumptions of empty repositories,
     Awaitility for any cross-context effect (B15).
3. **Every published integration event** from the catalogue, as records in the publishers' `api/events` packages
   (primitives and strings only, plus the shared `IntegrationEvent` marker if the project has one) (B9).
4. **A smoke test** of the risky plumbing: a REST self-call through the client, and an `@Async` listener that
   provably runs on another thread, with the whole application (other systems' beans included) loaded. Run it, note
   the traps it reveals (for example B8, `@Async` proxies) in the builder brief, then delete it (B7).
5. **Dry-run the flow scripts the builders will call** (B18). Copy the docs to a temp dir and run, for one real step
   of every plan:

   ```bash
   T=$(mktemp -d) && cp -r docs/plans docs/specs docs/intents "$T"/
   python3 .claude/skills/implementation-planner/scripts/plans.py --dir "$T/plans" --specs "$T/specs" \
     --intents "$T/intents" step <intent> S1 "in progress"
   ```

   Every plan must accept it. If one doesn't (a step table in the wrong shape, missing frontmatter), fix the plan's
   format now, in the prep commit, without changing its content, and re-run `plans.py check`. Builders must never
   meet a broken shared tool: each would work around it differently.
6. **Re-run the coverage check** (P6): `catalogue_coverage.py <catalogue> docs/plans`. A published event or resource
   that no plan step produces won't be built by any builder. Add the step to the producer's plan before spawning.
   Also check that each published event you wrote in step 3 has a producing step, and each listener a consuming step.
7. **Removals** the user allowed (old code, old canvases), with the skills' and `CLAUDE.md` references to them
   listed for repointing (P5, B10).
8. Run the full suite, then commit: `<System> stage 4 (prep): shared plumbing and published languages`.

Before spawning, write each context's **work package** into the manifest:
- its plans and the plan steps that touch this context (the builder owns those steps);
- its canvas;
- the catalogue sections it provides and consumes;
- its upstreams.

Steps that span contexts (a hand-off test) go to the downstream context's builder, because the upstream exists by then.

## 2. Waves

```bash
python3 <skill>/scripts/waves.py <map.cml> [<contexts.cml>] --event-only "<Up->Down>,..."
```

The event-only list comes from the catalogue. The output is the build order (B11). Spawn every wave-1 context at once,
in a single message. After each merge run `waves.py ... --merged <all merged so far>` and spawn what has become
startable, without waiting for the rest of the wave (B12). A cycle in the output stops the stage: fix the map first
(Stage 1 §4).

## 3. Spawning a builder

Each builder is one Agent call (`subagent_type: general-purpose`, `isolation: "worktree"`, in the background). Its prompt:
- the absolute path of `agents/context-builder.md`, with the instruction to read it first and follow it;
- the context name and package, and the integration branch name;
- its work package (from the manifest), plus the paths of the catalogue, its canvas, its plans, the DE answers and
  `lessons.md` section 7;
- the dial values: the TDD checkpoint loop, and the expert-question setting;
- the traps the smoke test found, and the qualified bean names to inject (B19);
- its own temporary directory, or "keep temporary files in your worktree" (O12);
- the upstreams already merged, and the fact that their real REST resources are what its Spring tests call.

Name each agent after its context (for example `builder-rentals`), so you can address it with SendMessage. Record the
agent name, worktree and branch in the manifest.

## 4. The TDD relay (TDD checkpoint loop on)

Builders run in the background and can't talk to the user. With the loop on, each builder works up to its next TDD
checkpoint (a failing test written, with the implementation choice open, or a minimal implementation scaffolded for
confirmation) and then **ends its turn with a checkpoint report**, in the format in `agents/context-builder.md`.

The orchestrator then:
1. Collects the checkpoint reports of all builders currently at a checkpoint. Don't hold a ready report for long while
   waiting for slower builders: present when everything running is at a checkpoint, or after a short while.
2. Presents them to the user as **one batched question**, grouped per context. Each item shows the scenario, the test,
   and the choice: *I implement it* / *scaffold a minimal implementation* / *the scaffolded one is fine* / *change it like
   this*. Use AskUserQuestion when there are at most 4 items. Otherwise use a numbered list and let the user answer by
   number.
3. Resumes each builder with `SendMessage` (to its agent name), passing the user's answer verbatim. When the user
   writes the code themselves, tell the builder which files to re-read and to rerun the test.
4. Records each round's decisions in the session record.

Expert questions that come up during building travel the same way. With the expert-question setting on role-play, the
builder answers them from the DE file, or reports them back for a new DE entry.

With the loop off (autonomous), builders work test-first without stopping, one example per cycle, and disclose any
batching in their TDD log (O10).

## 5. Merging

When a builder reports done:
1. **Verify it (O9).** In the builder's worktree or branch:
   - the full suite runs green;
   - the claimed rules have tests;
   - it touched only its own packages, docs and plan steps (`git diff --stat <integration-branch>...`);
   - it imports no other context's `api.usecases` or `api.queries`, and `api.events` only in its ACL packages
     (the stage-5 channel scan, run on its diff now, while the builder still remembers why);
   - no edits to shared code, the build file, other contexts or the skills' scripts. If there are such edits, send the
     builder back, or move the change into the orchestrator's own commit with a reason.
2. Merge it into the integration branch (`git merge --no-ff <builder-branch>`, message
   `<System> stage 4: <Context> context (<one-line summary>)`). Run the full suite on the integration branch.
3. Update the manifest: merged, the verified test counts, the decisions, the follow-ups the builder reported.
4. Spawn whatever has now become startable (§2).

A builder that reports a contract gap or a missing upstream command: decide it centrally. Update the catalogue
addendum or the upstream (if it's merged, make the change yourself in a small commit, or send the upstream's builder
back), then resume the builder. Never let two builders negotiate a contract between themselves.

When everything is merged, Stage 5 starts.
