---
name: discover-bounded-contexts
description: Discover bounded contexts from narratives, processes or event-storming boards, and turn them into a Context Mapper (.cml) context map, through an interactive session with the domain expert that uses the trainer's behavioural heuristics (pivotal events, then swimlanes, then single responsibility), with an independent critic agent approving every context and every map version. It also validates an existing list of bounded contexts or an existing context map/CML against the same heuristics and relationship rules (no shared kernel, no upstream/downstream cycles, ACL downstream, OHS/PL upstream). At the end it hands each context to bounded-context-scaffolder for its canvas. Use this whenever the user wants to find, identify, discover, split, draw or check bounded contexts or boundaries, shares Miro or event-storming screenshots, a BPMN file, a process list or a narrative and asks "where are the boundaries?", wants a context map or a .cml file, or asks to review, validate or critique a context map or bounded context split for VeloCity, CrowdSorcery, Orders or a new system, even if they never say "bounded context". Don't use it to stress-test a finished architecture with stressors (residuality-analysis) or to generate code from a canvas (bounded-context-scaffolder).
---

# Discover bounded contexts

This skill reproduces a facilitated discovery session. You are the facilitator, and the user is the domain expert. The boundaries come from the expert's answers, not from your assumptions. So the session is interactive by design: apply one heuristic, ask numbered questions, let the answers move the boundaries, and repeat. An independent critic agent checks every context and every context-map version before the user sees it. You're anchored on your own reasoning and on the expert's latest answer, and the critic isn't.

Read these files when you need them:
- `references/heuristics.md`: the three heuristics, the cross-cutting checks and question patterns. Read it before your first heuristic round.
- `references/worked-example.md`: a condensed real session, which shows the rhythm. Read it once per session.
- `references/context-map-rules.md`: the patterns and the trainer's rules for good relationships. Read it before building or validating a map.
- `references/cml-syntax.md`: verified CML forms and gotchas. Read it before writing CML.
- `agents/critic.md`: the critic's brief. Its path goes into every critic prompt.
- `assets/input-template.md`: hand it to a user who has no input yet.
- `assets/discovery-report-template.md`: the structure of the report file.
- `scripts/validate_cml.sh <file.cml>`: parses the CML with the repo's Context Mapper DSL. Run it from the repository root. It prints errors, a relationship list and `RULE` smells. Exit code 0 means valid.

Also read the project's `CLAUDE.md`, and the target system's `CLAUDE.md` if there is one.

The skill produces up to three things:
1. **A discovery report:** `docs/context-discovery/<system>/<YYYY-MM-DD>-<scope-slug>/discovery.md`. It holds the heuristic trail, the Q&A, the critic log, the contexts and the map rationale. It's also the resume point between sessions.
2. **A context map:** `src/main/cml/<System>-Context-Map.cml`, only if the user wants one.
3. **One canvas per context**, through `bounded-context-scaffolder`.

---

## Step 0: Resume, or pick the mode

Discovery spans several rounds of questions, often across sessions, so the report is the hand-off point. Look for a report for this system in `docs/context-discovery/<system>/`, or at a path the user gives you. If one exists, read it and continue from its `Status` and its last open questions. Don't restart the heuristics.

Then pick the mode:
- **Discover:** the user has behaviour (a board, a narrative, processes) and no agreed context list yet.
- **Validate:** the user brings contexts, a context map or a `.cml` file and wants it checked. Rebuild the narrative the contexts must support: ask for it, or derive it from the docs and confirm. Report findings. **Don't modify the user's CML.** Propose a corrected version only as a new file, or as a diff in the report, unless the user asks you to change it. Validation is a review, not a second discovery, so scale the critics to what's actually in doubt:
  - Run the heuristics over the existing contexts yourself. Spawn a context critic (Step 3) **only for a context you flagged**: a duplicated responsibility, a straddled pivot, an unowned decision, or a `RULE` line that points at its boundary. If you flagged none, say so and skip the context critics.
  - Run the map through **one** critic pass over all processes (Step 5.4's final pass), not one critic per process. Add another round only if that pass disagrees.
  - A relationship marked unconfirmed is an open question, not a finding to relabel. Ask the expert, and record whether it was confirmed, changed or removed. Don't add new unconfirmed relationships to the proposed map. If you think one is missing, ask about it.

**Hand-off from `implementation-planner`.** The planner may bring a spec (`docs/specs/<intent>.md`) that no existing context owns, plus a candidate context. Run validate mode on the existing map with the candidate added, using the spec's rules, commands and events as the narrative. Judge the candidate with the heuristics, as you would any other context: it may turn out to belong to an existing context after all. If you approve it, update the map (respecting the CML rules below) and have its canvas created. Then hand back to the planner, naming the approved context and its relationships.

**VeloCity is a workshop exercise.** `docs/domains/velocity/velocity-context-map.md`, `src/main/cml/VeloCity-*.cml` and `docs/domains/velocity/bounded-context-discovery-session.md` are the reference solution. If the user seems to be a participant discovering VeloCity, don't lean on them, don't quote them, and don't overwrite them. Their own answers decide the outcome. If you're unsure, ask whether they're the trainer.

## Step 1: Get the input

Accepted formats:
- **Miro/event-storming screenshots:** read the images.
- **Plain-text narratives or processes**, in any reasonable format.
- **BPMN** (`.bpmn`/XML): tasks, events, lanes and gateways become events, actors and parallel flows.
- **Structured data:** JSON, YAML or CSV.

**No input?** Don't invent a domain. Explain briefly that the best input is behaviour:
- a **narrative** is a sequence of events that tells a story
- a **process** is a part of the narrative with a start, an end and a business value

Processes can span contexts, so they aren't boundaries themselves. But independent processes running in parallel hint at different contexts, and processes are what the context map gets tested against later. Offer `assets/input-template.md`: copy it into the report folder if they want. Ask what they want to achieve (a new system, splitting a monolith, a workshop, validating a map). Then stop.

**Got input?** Before applying any heuristic, write back **how you read it**:
- the main timeline
- the branches
- the disconnected clusters
- anything ambiguous: duplicated stickies, unclear arrows

Screenshots lose meaning, and a misreading costs the least to fix now. Create the report file from the template, set `Status: in progress: heuristics`, and fill in "Reading of the input".

## Step 2: Apply the heuristics, one round at a time

Follow `references/heuristics.md`, in this order:
1. **H1, pivotal events (always first).** Propose candidate pivots, each with what comes before and after it.
2. **H2, swimlanes.** Look for parallel, repeating or unordered flows. Tell lanes from variants.
3. **H3, single responsibility (validation).** Write each candidate's single question, find duplicated responsibilities and decisions, and resolve them without breaking a confirmed pivot. Check who owns the rules, and whether a candidate changes independently.

Rules for the rounds:
- **End every round with numbered questions**, then stop and wait for the answers. Each question says which boundary it decides. Don't confirm a pivot or a lane on your own. The expert confirms it.
- **Keep each round small: at most 5 questions, and a message well under 500 words.** The expert is a busy person answering live. Ask the questions whose answers decide the most boundaries now, and carry the rest to the next round. A long message with nine questions gets short, careless answers, and those answers then become boundaries.
- **Let the answers move the boundaries visibly.** Say what changed and why ("preparation has no rules of its own, so it merges into Rental").
- **Before you present a draft, run the cross-cutting checks yourself.** Is every step and decision owned exactly once? Does each confirmed pivot have an owner on both sides? Are any events missing or misleadingly named? In the worked example the expert had to ask "who actually makes the settlement?". Ask that kind of question before they do.
- Collect suggested board changes (missing events, renames) as you go.
- After each round, update the report: the heuristic trail and the Q&A, with answers verbatim where possible.

A small, clear input may need only two rounds. A messy board may need five. Stop iterating when the answers no longer move boundaries.

## Step 3: The critic loop, per context

When you have a candidate set the heuristics support, have **each context** reviewed by a critic before you present anything.

- Spawn one critic per context with the Agent tool (general-purpose), **in parallel in a single message**. Each prompt contains:
  - `mode: context`
  - the context to judge
  - the full candidate set, each with its question and owned events, decisions and rules
  - the input, or its path
  - the expert's answers so far
  - the round number
  - the absolute paths of `agents/critic.md` and `references/heuristics.md`, with the instruction to read them first

  Give it the evidence, not your conclusion. The critic's value is that it isn't anchored on you.
- **On DISAGREE:**
  - **Blocking findings:** fix them and re-run the critic for that context, and for any neighbour whose ownership the fix actually changed. **Don't re-run critics that agreed on a context that hasn't changed.** Their verdict still stands, and re-running them only costs time.
  - **Expert questions:** put them to the user, and wait.
- **Cap at 3 rounds per context.** If you and the critic still disagree, don't loop. Present the disagreement to the user with both positions and let them decide. Record it.
- Log every verdict in the report's critic log.

## Step 4: Present the candidates, and stop

Present only contexts the critic agreed with, or disagreements the user has to settle. For each context give:
- its name, and the **one question** it answers
- what it owns: events, decisions, rules
- the heuristic justification: which pivot or lane separates it, and how H3 confirmed it
- the critic's verdict, plus any non-blocking notes worth knowing

Add the open assumptions and the suggested board changes. Ask the user to approve the list. **Don't continue until they agree.** When they do, set `Status: contexts approved`.

## Step 5: Context map (only if the user wants one)

In the same message as the approval request, or right after it, ask one plain yes/no question: "Do you want a context map in Context Mapper (.cml) format?" **Don't ask anything about the map yet** (team relationships, who adapts to whom, processes, file names). Those questions only make sense once the user has said yes, and asking them early makes the approval message long and presumes the answer. If they say no, go to Step 6.

If yes:
1. **Processes first.** Collect the business processes or user stories, from the input or by asking. The map must support every one of them.
2. **Walk each process.** Map every step to its owning context. Every hand-off between contexts is a candidate relationship. The direction follows **model dependency**: the context whose model the other must understand is upstream. Choose patterns by `references/context-map-rules.md`:
   - ACL downstream by default
   - either OHS/PL upstream for APIs and events (ACL or a justified CF downstream), or Customer/Supplier between internal teams (PL allowed upstream; OHS and CF are CML errors in C/S; an ACL only warns). They're alternatives, not layers: see "Choosing between OHS and Customer/Supplier" in the rules
   - CF only towards a well-established language
   - no Shared Kernel, no Big Ball of Mud, no U/D cycles

   When a fact is missing (who adapts to whom, whether a team is external), **ask**. Don't guess silently. If you must assume, mark the assumption in a CML comment.
3. **Write the CML** using `references/cml-syntax.md`. Put each context's question in its `domainVisionStatement`. Run `scripts/validate_cml.sh <file>`. Fix every error. Fix or justify every `RULE` line.

   **Cycles: direct ones are never kept, indirect ones only with the expert's explicit yes.** Follow "Breaking a cycle" in `references/context-map-rules.md`.
   - **Direct `ud-cycle` (A upstream of B and B upstream of A) is a design error.** Don't keep it, however well you could justify it. Resolve it in this order:
     1. One side becomes a client of the other's API. Direction follows whose model is the contract, not which way the messages travel.
     2. Move a responsibility, or drop a dependency by carrying the needed data in a message the other side already gets.
     3. Declare a Partnership, if the two teams really must evolve together.
     4. Merge the two contexts. That's a last resort: it reopens H3.
   - **Indirect `ud-cycle-indirect`:** try the same flips. If none fits the facts, show the expert the cycle and each flip **in business terms** ("Option A: Complaints asks Billing for the refund through Billing's own refund service..."). Keep the cycle only if the expert explicitly chooses to. Your own judgment that "no flip fits" isn't enough.
   - **Every flip must agree with the facts the expert has stated.** "X needs Y's answer" means Y is upstream of X, and no flip may reverse that.
   - **Ask neutral questions about the business, not leading ones that propose a flip.** Never rest a direction or a kept cycle on an answer the expert improvised, or on a forced either/or choice. Ask what actually happens instead.
4. **Critic loop, per process.** Spawn one critic per process. They can run in parallel, because the final pass catches interactions between processes. Give each critic:
   - `mode: map`
   - the CML path and the validator output
   - that process
   - the context list
   - the expert's answers
   - the round number
   - the paths of `agents/critic.md`, `references/context-map-rules.md` and `references/cml-syntax.md`

   On DISAGREE, revise the CML, re-validate, and send it back (cap: 3 rounds per process, then escalate to the user as in Step 3). A later process can change the map, so finish with **one final critic pass over all processes** on the final CML.
5. Present the map:
   - the relationships table with justifications
   - how each `RULE` line was resolved
   - the open assumptions

   Ask for approval. Then set `Status: map approved`.

**Where the CML goes:** `src/main/cml/<System>-Context-Map.cml`. If that file already exists, **ask first** before overwriting. Offer a new name instead, such as `<System>-Context-Map-<slug>.cml`. For VeloCity the existing file is the workshop's reference solution. During validation, never touch the user's file without being asked.

## Step 6: Final artifacts

1. **Report:** complete `discovery.md` with all sections. Every context, relationship and assumption must be traceable to a heuristic, a rule or an expert answer.
2. **CML:** if a map was made, it must be valid, with the validator's last output pasted into the report.
3. **Canvases:** invoke the `bounded-context-scaffolder` skill and use it to create one canvas per approved context, at its default location `docs/bounded-contexts/<system>/<context-package>/canvas.md`. Pre-fill each canvas with everything the discovery extracted:
   - name, matching the CML exactly
   - purpose and the context's question
   - ubiquitous language, from events, terms and expert answers
   - inbound and outbound communication, from the map's relationships and messages
   - known business rules, in the expert's words. Mark vague ones as needing Given/When/Then.
   - strategic classification, if known
   - open questions

   Ask the scaffolder for **canvases only**: no plan and no code yet. The user reviews the canvases first, and the scaffolder's own review gate handles the rest later.

   **If `bounded-context-scaffolder` isn't available**, say so explicitly, and suggest installing it (it lives in this repo at `.claude/skills/bounded-context-scaffolder/`). If the user declines, write the canvases yourself, following the ddd-crew Bounded Context Canvas v5 sections listed above.
4. Set `Status: canvases handed off`, and end with a **short** final message. The report holds the details, so don't repeat it. Give the user:
   - the paths
   - the number of contexts and relationships
   - the critic rounds that actually changed something, one line each
   - what's still open

   Aim for well under 300 words.

## Principles

- **The expert decides business facts, and the heuristics decide what follows from them.** Don't overrule an answer. Show where it leads ("then blocking has two owners, which H3 says can't be").
- **Ask rather than assume.** A guessed boundary encodes your assumptions, not their domain. When you must assume, label it everywhere it appears.
- **Contexts answer business questions.** Technical mechanisms (notifications, storage), external parties and single process steps aren't bounded contexts.
- **Keep the user's attention on boundaries, not on tooling.** Run the validator and the critics, but report their results briefly.
