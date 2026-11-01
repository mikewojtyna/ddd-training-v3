# Design history: `bounded-context-scaffolder` skill

This file records how the skill at `.claude/skills/bounded-context-scaffolder/` was designed. It covers the conversation, the original prompt verbatim, the decisions made along the way with their reasons, and the evaluation iterations. It's meant as teaching material and as a reference for future changes to the skill.

- **Author:** Mike Wojtyna, with Claude Code (model: Claude Opus 5.5), using the `skill-creator` plugin
- **Started:** 2026-11-01
- **Repo state at start:** `070ea2a Add per-system CLAUDE.md files and Rentals package-info`

---

## 1. Conversation timeline

1. **`/skill-creator:skill-creator`** (no arguments). Claude asked what the skill should do and suggested four ideas grounded in this repo: scaffolding a bounded context, adding a context relationship with an ACL, the domain-expert role-play, and a business-archetype analysis.
2. **User:** *"Can I later package it as a plugin installed in this project?"*
   **Claude:** Yes. A skill doesn't depend on how it's delivered. There are two ways to share it:
   - **A:** a project skill in `.claude/skills/<name>/SKILL.md`, committed to git.
   - **B:** a plugin in an in-repo marketplace (`.claude-plugin/marketplace.json` plus `plugins/<name>/.claude-plugin/plugin.json` plus `skills/`), installed at project scope so it's recorded in `.claude/settings.json`.

   Moving from A to B later only means moving the skill folder. Claude then tried to ask two multiple-choice questions (which skill, which packaging). The user declined that dialog.
3. **`/skill-creator:skill-creator`** again. Claude asked the user to describe the skill in their own words, and said packaging can wait because the skill is kept self-contained.
4. **User:** the full specification (verbatim in section 2).
5. **Claude** checked whether the skill should be split, decided it shouldn't (section 3.1), drafted the skill, and launched the evaluations (section 5).

## 2. The original prompt (verbatim)

> I'd like to scaffold a new bounded context or update the existing one (so, the skill serves two needs - if it should be splitted according to the best practices - stop, and let me know about it before creating the skill). But instead of using classes with getters create records. Also, the skill requires an input. From a pragmatic point of view, there's little value in scafolding an empty context, so the skill should require from user to provide some input. Input can be a markdown file or an inline text. Markdown should be preferred, since it's much easier to structure the content and include more details. Encourage the user to provide such file or guide him to create one before moving forward.
>
> The input should be based on the bounded context canvas as defined in this file: @docs/bounded-context-canvas-v5.jpg (more details available here: https://github.com/ddd-crew/bounded-context-canvas). At minimum, the input provided should include name, domain terms in the ubitquious language and purpose. Asuumptions, verification metrics and open questions are of least importance. Acceptable domain roles are defined here @docs/model-traits-worksheet.md.
>
> The Inbound communication maps to api.usecases (that can consume a command or an event) or api.queries. The outbound communication maps to api.events (if event is published by bounded context) or some secondary ports implemented as secondary adapters in case of outbound query or command (note, that this interpretation assumes the necessity of creating an ACL, so the internal model is not polluted). However, if user strictly mentions he doesn't want to use some ACL (e.g. when bounded context is a conformist), then allow him to do that but question his decision suggesting ACL as a better alternative.
>
> Name must be reflect in @BoundedContext annotation. Javadoc on the package.info.java must include Purpose, strategic classification (if available), domain roles (if available), ubiquituos language, business decisions (if available), assumptions (if available), verification metrics (if available), open questions (if available).
>
> If concrete business rules are provided, try to create aggregates based on these rules. The rules must be strict and expressed using Given/When/Then examples.
>
> The skill produces two artifacts:
> - the created plan markdown file (including a changelog if updating the existing bounded context)
> - the new module representing the scaffolded or updated bounded context.
>
> By default assume that bounded context is mapped 1:1 to module. If user specifically wants to create multiple modules, suggest him splitting the context into smaller ones if this maps 1:1. If user still insists on creating multiple modules, let him do that (he knows better). The code structure of the produced module should follow the code conventions defined in the project (which you can find in the CLAUDE.md) - basically my interpretaion of ports and adapters.
>
> After creating the skill, save our conversation and this prompt, so the history of design is clear and be used to teach others and work as a reference point for my work.

Attached references: `docs/bounded-context-canvas-v5.jpg` (ddd-crew canvas v5) and `docs/model-traits-worksheet.md`.

## 3. Design decisions and their reasons

### 3.1 One skill, not two (scaffold and update)
The user asked to stop if the skill should be split. Claude assessed this before writing anything and decided **not to split**:
- Both modes take the **same input** (the canvas), use the **same mapping rules** (canvas → packages, ACLs, javadoc) and produce the **same two artifacts**. Update is scaffold plus a comparison with existing code plus a changelog.
- Two skills would repeat about 80% of the instructions. When the shared rules changed, they would drift apart.
- Their trigger phrases overlap ("bounded context", "canvas", "module"), so two skills would compete when Claude decides which one to load.
- So the skill **detects the mode automatically**: if `<system>/<context>/` already exists, it runs in update mode.

### 3.2 Records instead of classes with getters, but not for aggregates
*How the prompt was read:* "instead of using classes with getters create records".
- **Records** are used for every data-carrying type: value objects, IDs, commands, queries, query results, domain and integration events, REST bodies.
- **Aggregate roots and changing entities stay classes**, but **without getters**: they expose behaviour that returns `DomainEvents`, plus one `@Identity` accessor. An aggregate protects invariants over changing state. A record can't hold changing state, and making the aggregate immutable would be a different design that the prompt didn't ask for.
- Existing classes are **not** converted in update mode unless the user asks.
- *If this interpretation is wrong, it's one paragraph in SKILL.md Step 6 to change.*

### 3.3 Input gate: no canvas, no code
- Required: **Name, Purpose, Ubiquitous Language** (terms *with definitions*). If one is missing, the skill stops and asks.
- Markdown is preferred. For inline text, the skill offers to save it into `assets/canvas-template.md` format. With no input, the skill copies the template, pre-filled with what it knows, to `docs/bounded-contexts/<system>/<context>/canvas.md`, and stops.
- Order of importance, from the prompt: required, then classification, roles, communication and decisions, then assumptions, metrics and open questions.

### 3.4 Domain roles are validated against the Model Traits worksheet
The short forms on the canvas ("execution context") are mapped to worksheet traits ("Execution Model"). Unknown roles are flagged. **Brain Context** gets a warning, because the worksheet itself marks it as a likely anti-pattern.

### 3.5 Only strict GWT rules become aggregates
A vague rule ("retry a few times") is **not** modelled in code. The skill suggests a concrete GWT version and lists the rule under "Rules awaiting GWT" in the plan. *Why:* guessing a threshold silently buries an assumption inside the domain model, which is exactly what a DDD training wants to expose. Strict rules turn into aggregate invariants and **tests in the `BikeStationTest` style**, and the aggregate is implemented so that those tests pass.

### 3.6 Mapping of communication (the user's ports-and-adapters interpretation)
- Inbound command or event → `api/usecases`. Inbound query → `api/queries` (handler plus read-model port).
- Inbound events from another context are translated by an `@AntiCorruptionLayer` **primary** adapter (`infra/adapters/primary/<ctx>ctxacl`), so use cases consume records in the context's *own* language. This applies the user's ACL rule symmetrically to the inbound side.
- Outbound event → `api/events` (an `IntegrationEvent` record carrying primitives only). It's kept separate from the internal domain event, the same way deposit separates its two `DepositMade` classes.
- Outbound command or query → a `@SecondaryPort` in our language plus a `@SecondaryAdapter` in an `@AntiCorruptionLayer` package. If the user refuses the ACL (Conformist), the skill **pushes back once, then complies** and records the risk in the plan and in the package javadoc.

### 3.7 One module per context by default
If the user asks for several modules, the skill first suggests splitting the context. If the user insists, the modules become subpackages with `@Module` only, and `@BoundedContext` stays on the root.

### 3.8 The plan is a traceability document
Every canvas element maps to the file that implements it, and back. In update mode, the plan file keeps its history and gains a dated **Changelog** entry. Proposed deletions are listed, never applied without confirmation, because workshop stubs such as `UnsupportedOperationException("implement this")` can be deliberate.

### 3.9 Details found while drafting
- `package-info.java` uses **Markdown doc comments (`///`)**, supported since Java 23 (the project targets 25), so the canvas sections stay readable.
- `@DomainEvent` (`jmolecules-events`) is **not** on the classpath. Domain events implement `common.domain.DomainEvent` instead.
- Spring bean names are global, so bean methods are prefixed with the context name to avoid duplicate-bean failures.
- For VeloCity, the skill **offers** to sync the CML and `velocity-context-map.md` (as the VeloCity `CLAUDE.md` asks) but doesn't do it unasked, because the context map can be a workshop exercise.

### 3.10 Packaging
The skill is a project skill in `.claude/skills/bounded-context-scaffolder/`, so it's self-contained and ready to move into an in-repo plugin later (see the timeline, step 2).

## 4. Skill layout

```
.claude/skills/bounded-context-scaffolder/
  SKILL.md                      workflow: get canvas → validate → decide/push back → plan → (diff) → generate → verify → report
  references/canvas-mapping.md  canvas section → package/class/javadoc rules
  references/code-templates.md  Java shapes (records, aggregate, ports, ACLs, tests, wiring)
  assets/canvas-template.md     input template handed to users
  assets/plan-template.md       plan + changelog structure
  evals/evals.json              test prompts
  evals/files/*.md              test canvases (Pricing, Rentals update)
```

## 5. Evaluation iterations

The full results of every run (outputs, diffs, plans, grades, the review pages and the verbatim feedback) are kept in [`evals/`](evals/README.md). Open `evals/iteration-N/review.html` in a browser.

### Iteration 1 (2026-11-01)
Four test prompts, each run **with** the skill and **without** it (baseline), every run in an isolated git worktree:

| Eval | What it tests |
|---|---|
| `pricing-scaffold-from-markdown` | Full markdown canvas, 4 strict GWT rules → aggregate + tests, ACL to Rentals, integration event, complete javadoc |
| `settlement-inline-conformist-vague-rule` | Inline input (should recommend markdown), "skip the ACL" (pushback, then comply), vague retry rule (should not be modelled) |
| `debtrecovery-no-canvas` | No canvas → must not generate code, must hand over a pre-filled template |
| `rentals-update-with-changelog` | Update mode on existing `rentals`: keep style, implement `returnBike` from GWT, plan with changelog, existing tests still pass |

**A process slip, recorded for honesty.** The session was still in *plan mode* when the skill was drafted. Claude wrote the skill files anyway, which plan mode doesn't allow, and the first 8 eval subagents inherited plan mode, so they could only produce plans. Their plans still surfaced findings, so they weren't wasted:
- **A bug in the test canvas:** the "Tiered hours" example said 4h20m costs 33 PLN, but by the canvas's own definition it's 45 PLN. It was fixed to 3h20m. Both the with-skill and the baseline run caught it.
- **Two checks added to the skill:**
  - *Check the canvas for internal consistency*: work through each GWT example against the definitions, and ask instead of picking a side.
  - *Compare with the reference context map*: list the differences in the plan, and never "fix" the workshop solution unasked.

  The plan template gained a "Differences from the reference context map" table.
- **Missing docs:** the untracked `docs/model-traits-worksheet.md` and the canvas image aren't in worktrees, so the eval prompts pass their absolute paths.

After plan mode was exited, all 8 runs were repeated. Every build was **re-verified independently** with `./mvnw -B test` in each worktree; all passed.

| Eval | With skill | Baseline |
|---|---|---|
| pricing-scaffold-from-markdown | 11/11 | 9/11 (no plan, no canvas javadoc) |
| settlement-inline-conformist-vague-rule | 9/9 | 3/9 (no pushback on ACL, guessed "3 retries × 15 min", no plan) |
| debtrecovery-no-canvas | 4/4 (stopped, wrote pre-filled canvas) | 0/4 (invented a full context with 9 guessed rules) |
| rentals-update-with-changelog | 10/10 | 8/10 (no plan/changelog) |
| **Total** | **100%** | **49%** |

On average the with-skill runs were about 80 s faster and used about 9k fewer tokens. **What the numbers say:** both setups write competent Java (builds, records, GWT tests, `@BoundedContext`), so those checks don't tell them apart. The skill's value is the **discipline around the code**: no input means no code, vague rules aren't modelled, ACL decisions are challenged, and every change is traced in a plan.

**Grader lessons:** two failures were grader bugs, not skill bugs:
- the event regex didn't match multi-line records
- the ACL check counted a javadoc that *mentioned* `@AntiCorruptionLayer`

Always look at failing evidence before believing a score.

**User review feedback (verbatim, from `feedback.json`):**

| Run | Feedback |
|---|---|
| pricing, with skill | "Before creating the code, ask the user to verify the plan and provide his feedback. The plan must be saved. If skill sees the plan is available, it should continue from there." |
| pricing, baseline | "I prefered the markdown in javadoc" |
| settlement, with skill | "When rules change in code (based on tests), the skill must also update the javadoc markdown on the package-info accordingly" |
| settlement, baseline | "not enough feedback or pushing back for user" |
| debtrecovery, with skill | "Great it suggests creating a canvas first and partly fills it" |
| debtrecovery, baseline | "No nudge towards creating the canvas" |
| rentals, with skill | "good one" |
| rentals, baseline | "poor" |

**How each point was read, and what changed:**
1. **A review gate, plus resuming from the plan.** Step 4 is now a hard stop: save the plan with `Status: draft - awaiting review`, summarise, ask for review, and generate no code in that turn. The old "non-interactive → just proceed" escape was removed. The reason is written into the skill: reviewing domain decisions in a plan is cheap, and picking them out of generated code is not. A new **Step 0** reads an existing plan's status and continues from it:
   - `draft`: apply the feedback and ask again
   - `approved`: implement exactly what was approved
   - `implemented`: start a new update round

   The plan template gained a `Status` line and a `Review notes` section, and the skill description now also triggers on "plan approved, go ahead".
2. **"I preferred the markdown in javadoc"** was left on the baseline, whose `package-info.java` was a single prose paragraph. It was read as confirming the skill's `///` Markdown sections, so nothing changed.
3. **Javadoc kept in sync with the rules.** "Business decisions" in `package-info.java` is now defined as the readable form of the enforced rules. Each implemented rule appears in GWT form and names its test (`SomeTest#method`). Rules not implemented yet are marked *not modelled yet*. The section is updated in the same change as any rule or test change. Step 7 checks that every GWT test has its rule in the javadoc, and the reverse.

### Iteration 2 (2026-11-01)
To test the gate and the resume, each code-generating eval now runs in **two phases**:
- **Phase A:** the original request. The skill must stop with a saved draft plan.
- **Phase B:** a fresh session in the same worktree, given the user's approval, sometimes with a change:
  - Pricing: an extra return-charges rule
  - Settlement: the strict retry rules that were missing in phase A
  - Rentals: a plain approval

Baselines were reused from iteration 1, since a run without the skill can't be affected by changes to the skill.

| Eval | With skill (A + B) | Baseline |
|---|---|---|
| pricing | 18/18 | 9/11 |
| settlement | 16/16 | 3/9 |
| debtrecovery | 4/4 | 0/4 |
| rentals | 17/17 | 8/10 |

All with-skill builds were re-verified independently (`./mvnw -B test`). Observations:
- **Phase A** stopped with a draft plan in all three cases and wrote no Java.
- **Phase B** continued from the plan files with no memory of phase A, and carried the user's changes all the way through: canvas, plan (Review notes, changelog, Status `implemented`), javadoc and tests.
- **Settlement:** the vague rule became two tested invariants once the user supplied GWT. Retry *timing* stayed marked as *not modelled yet* in the javadoc, which is exactly the sync the feedback asked for.
- **Watch item:** resumed sessions sometimes needed small decisions the plan didn't cover (an extra use case, how to read "defaults are fine"). They reported each one as a judgement call or an open question rather than hiding it.

**User review feedback (verbatim):**
- pricing, with skill: "ok"
- pricing, baseline: "bad"
- settlement, with skill: "ok"
- all other runs: no comments (empty feedback means acceptable)

There were no further change requests, so the review loop ended after iteration 2. The "watch item" (resumed sessions making small calls outside the approved plan and reporting them) drew no objection, so it stays as it is.

### Iteration 3 (2026-11-01): architecture tests for context boundaries

**User request (verbatim).** Asked to "add one more thing", the user picked *Architecture tests* from four suggestions, declined a follow-up multiple-choice question about rule sets, and specified instead:

> Let's make it simple: module/context A can call or depend only on module/context B by using module B's api types. There's one exception, though: if every explicitly says it and wants to break the rules (e.g. by mentioning we want to use a shared kernel or be a conformist), challenge his decision but don't enforce it. In such a case - add the corresponding comments to the code mentioning this decision explicitly. If user doesn't say that - don't make any exceptions and keep archunit rules simple.

**Design:**
- **One test per context the skill touches:** `<Context>ArchitectureTest`, with exactly **two ArchUnit rules**.
  - *Incoming:* other contexts use only this context's `api`.
  - *Outgoing:* this context uses only other contexts' `api`.
  - "Another context" is decided from the package name. The top-level `common` and a system's `common` and `contextmap` are shared code, not contexts.
- **Exceptions:** only on an explicit user request, which is challenged once. If the user insists, the skill adds:
  - a D-numbered plan decision stating the risk
  - one `.and(DescribedPredicate.not(resideInAPackage("<exact package>")))` clause with a `// DECISION Dn` comment
  - a `DECISION Dn` comment where the foreign type is used

  Otherwise there are no exceptions, ignore lists or `@ArchIgnore`.
- **Conformists:** a conformist that uses only the upstream's `api` already follows the rule, so it needs no exception.
- **Dependency:** only `com.tngtech.archunit:archunit-junit5` (test scope), added to the pom by the skill when it's missing. `jmolecules-archunit` was not used, to keep it simple.

**Verified before writing the template** (scratch worktree, then removed):
- ArchUnit **1.4.1 can't read the class files of the locally installed JDK 27** ("Unsupported class file major version 71"). **1.5.1** can, so the skill pins 1.5.1.
- Mutation checks with the template applied to Rentals:
  - Rentals → `deposit.domain` fails the outgoing rule.
  - `deposit` → `rentals.domain` fails the incoming rule.
  - Rentals → `deposit.api` passes.
  - An exception for `deposit.domain.account` silences only that package; `deposit.domain.payment` still fails.

**Changes:**
- `SKILL.md`: Step 3 gained item 6 (boundary rule, challenge, recorded exceptions), Step 6 gained a bullet requiring the test, and Step 7 runs it.
- `code-templates.md`: the verified test template and the exception example.
- `canvas-mapping.md`: §6a, which also says ACLs translate *B's api types*, never B's domain types.
- The plan template: a "Boundary exceptions" decision row.
- The evals: a new eval 5, `accounts-shared-kernel-exception`, where the user explicitly wants Rentals' `BorrowerId` as a shared kernel; follow-up prompts are now stored in `evals.json`; and the grader has the new checks.

**Results.** The evals rerun were pricing and settlement (both two-phase) and the new accounts eval (two-phase), plus a fresh baseline for accounts. Every build was re-verified independently with `./mvnw -B test`.

| Eval | With skill (A + B) | Baseline |
|---|---|---|
| pricing | 22/22 | 9/11 (reused) |
| settlement | 20/20 | 3/9 (reused) |
| accounts-shared-kernel-exception | 20/20 | 5/13 |

- **The architecture tests:** every with-skill run generated `<Context>ArchitectureTest` with the two plain rules, and added ArchUnit 1.5.1.
  - **Mutation check** on the generated `PricingArchitectureTest`: a probe class that referenced `rentals.domain…BikeId` made it fail, and reverting made it pass.
  - **No exceptions where none were asked for:** Pricing and Settlement have none. Settlement's plan explicitly reasoned that conforming through Payments' `api` needs no exception.
- **Accounts, where the user explicitly asked for a shared kernel:**
  - **Challenged once, with concrete costs:** coupling to Rentals' internals, and a dependency cycle (Rentals consumes Accounts' event), which contradicts the reference map's "No cycles". It offered alternatives (move `BorrowerId` to `velocity.common`, publish it in Rentals' `api`, or give Accounts its own id).
  - **Then complied** with D1, scoped to the single class `BorrowerId`.
  - **D1 is visible where it matters:** one commented exception in the test, and `DECISION D1` comments in `domain/account/package-info.java` and on `Account`.
  - **Proved its own exception:** the run checked that a different Rentals type (`BikeId`) is still rejected.
- **Accounts baseline:** it honoured the shared kernel, but didn't challenge it and wrote no architecture test, so nothing would stop the exception from spreading.

**Skill change after the run.** The template only showed a package-wide exception. The run chose a class-level one, because the decision named one class. The skill now requires exceptions to be **exactly as narrow as the decision**, and the template shows the class form, `HasName.Predicates.name(...)`. `JavaClass.Predicates.name` doesn't exist in ArchUnit 1.5.1.

**Grader lessons, again:**
- The Settlement check "retry rule no longer not modelled" was too broad. It flagged the retry *timing*, which is correctly still not modelled because the user never specified it. The check now looks only at the rule the user supplied.
- The Accounts baseline was at first graded without the architecture checks, which flattered it. It now gets them too.

**Process note:** test worktrees are created from `070ea2a`, not from the skill branch, so they don't contain the committed worksheet. The runs read the skill and the worksheet from absolute paths in the main checkout, so the results aren't affected.

**User review feedback:** all 6 runs were reviewed and none got a comment (`evals/iteration-3/feedback.json`), which means accepted as is. The architecture-test addition is done.

## 6. Status

- **2026-11-01:** skill accepted after 2 iterations. The eval results were copied into the repo (`evals/`).
- **2026-11-01:** a trigger eval set of 20 requests (10 that should load the skill, 10 near-misses) was written and approved by the user. It's saved as `.claude/skills/bounded-context-scaffolder/evals/trigger-eval.json`. The user decided to **keep the current description** and skip the optimisation loop. To run it later: `python -m scripts.run_loop --eval-set <that file> --skill-path .claude/skills/bounded-context-scaffolder --model claude-opus-5-5 --max-iterations 5`, run from the skill-creator plugin directory.
- Possible next steps:
  - optimise the description for triggering (skill-creator's `run_loop`)
  - package it as an in-repo plugin (see the timeline, step 2)
