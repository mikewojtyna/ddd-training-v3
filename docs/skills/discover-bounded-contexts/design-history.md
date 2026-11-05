# Design history: `discover-bounded-contexts` skill

This file records how the skill at `.claude/skills/discover-bounded-contexts/` was designed. It covers the conversation, the original prompt verbatim, the decisions made along the way with their reasons, and the four evaluation iterations. It's meant as teaching material and as a reference for future changes to the skill.

- **Author:** Mike Wojtyna, with Claude Code (model: Claude Opus 5.5), using the `skill-creator` plugin
- **Date:** 2026-11-05
- **Repo state at start:** `a0d501e Scaffolder delegates domain design; TDD checkpoint loop for every rule (#12)`
- **Sources the skill was distilled from:** `velocity/bounded-context-discovery-session.md` (a recorded discovery session holding the trainer's heuristics) and `docs/context-map-cheat-sheet.png` (ddd-crew Context Map Cheat Sheet)

---

## 1. Conversation timeline

1. **User:** "Prepare a new skill named discover-bounded-contexts based on the @context-mapping-skill-intent.md" (in plan mode).
2. **Claude** read the intent, the discovery session, the cheat sheet, the sibling skills and the reference CML, and asked three questions. The answers:
   - The critic is bundled in the skill (`agents/critic.md`), not a project subagent.
   - Reports go to `docs/context-discovery/…`, and the map to `src/main/cml/<System>-Context-Map.cml`, never silently overwriting an existing file.
   - Run the full skill-creator eval loop.
3. **Plan approved.** Claude built and tested the CML validator first, then the references, the critic, the templates and `SKILL.md`, then three evals with simulated domain experts.
4. **Iterations 1–4**, each followed by the user's instruction: "make fixes 1-4 and run iteration 2", "fix those three and run iteration 3", "fix cycles", "run iteration 4". The user then reviewed iteration 4 in the viewer, left all feedback empty, and chose to commit.

## 2. The original prompt (verbatim)

> Invoke skill-creator skill with the below prompt
>
> # Idea
> I'm thinking about building a skill to identify bounded contexts and create context maps and validate the existing bounded context/context map based on my set of heuristics. I've saved an old coversation between you and me in the file @velocity/bounded-context-discovery-session.md . I find this file very valuable since it contains most of my heuristics and shows the entire process of finding right bounded contexts. Now, I want to turn this conversation into a reusable skill to find bounded contexts. One caveat: except of just identyfying bounded contexts, the same skill should also create a context map in the Context Mapper (.cml) format.
>
> ## Steps
>
> ###  1. Ask for input
> The input is required. If user hasn't given you any input, guide him to create one suggesting one of the acceptable formats and asking for the intention.
>
> The best way to identify bounded contexts is to start with narratives or processes. The diff between narrative and process is that narrative is just a sequence of events that tell some story, while process is a narrative that also has some business semantics and scope. So, basically, narrative is prerequisite for process, and then the narrative can be chopped into processes having a starting point, ending point and some business meaning delivering a concrete business/user value. Processes can span bounded contexts, and that's why are usually not the best way to find the boundaries. Although, if these processes are independent (can happen in parallel), they're often not part of the same bounded context (the swmilanes heuristics). I won't get into the details here - everything is in the conversation I've mentioned at the beginning of this session.
>
> #### What format is accepted?
> - Miro screenshots showing sticky notes representing processes/narratives
> - Written narratives or processes in a plain text (accept flexible formats)
> - BPMN formats
> - Structured data just as .json or anything similar
>
> ### 2. Apply bounded context heuristics
> These heuristics are behavior based. I've mentioned the heuristics in the mentioned conversation. Use them to initiaite the discovery session with the user and find good candidates for bounded contexts.
>
> For each identified bounded context, start the critic agent to re-evaluate your findings. Feed the critic with your findings and bounded context heuristics, so it can have some basic knowledge how to evaluate your bounded contexts. Since critic is essential, create it as well as part of this session. Return the identified bounded context only after the critic agreed with your bounded context.
>
> Finally, after you've identified bounded contexts together with the help of the critic, return a suggested list of bounded context candidates with justification based on the used heuristics.
>
> Only after user agrees with you, go to the next step.
>
> ### 3. Ask the user if he wants to create a context map
> If user answered yes, then start mapping the relationships between the bounded context and create a context map using the Context Mapper format (the .cml file).
>
> ### 4. Context Map creation and validation
> The best way to create the context map is to start with business processes or user stories and map each step to the identified bounded contexts. Based on this, create the relationships in the context map. After you've created the first version of the context, ask the critic agent that will evaluate the quality of the context map. Feed the critic with all necessary data and rules (which you can find at the bottom of this section) regarding good relationships. If critic decided the context map is designed poorly, create a new version of the context map with the input from critic to improve it. Then, pass to the critic again. Repat until you fully agree with the critic.
>
> Repeat this critic loop for every business scenario/business process/user story.
>
> Ask user the question whenever you're unable to answer on your own and require an input.
>
> Finish this step after you've created the best version of context map that supports all business processes/scenarios/user stories.
>
> **Types of relationships and integration patterns**
> The relationships and integration patterns are listed in the cheat sheet (@docs/context-map-cheat-sheet.png).
>
> **Good relationships and integration rules**
> Big Ball of mud is always wrong. Don't even consider it.
>
> Relationship rules
> 1. Partnership is good only for early stages. Upstream/Downstream should be preferred in well established boundaries.
> 2. Upstream/Downstream is very often a healthy relationship and should be preferred. If context map shows Upstream/Downstream going both ways (cycles), your design is almost always wrong.
> 3. Separate ways can be used only in Research and Development projects where we're experimenting and want to create a silo on purpose and MAYBE integrate if project is promising after the experimentation.
> 4. Upstream/Downstream with customer/supplier specialization is healthy for teams working in the same organization/domain/product, since they're workign towards the common goal.
>
> Integration pattern rules
> 1. Conformist is good only when conforming to a well-established domain and language. In other cases it might polute the downstream.
> 2. Shared kernel is almost always wrong because it leads into a lot of synchronization and over time the common code contains rules specific just for one context, and not really common.
> 3. Anti corruption layer (ACL) is almost always a good idea on the downstream.
> 4. Open Host Service (OHS) is a healthy pattern nad is applicable only to the Upstream. Often combined with Published Language (since clients need to somehow communicate with the server).
> 5. Published Language is good e.g. when publishing events or schemas.
>
> ### 5. Final artifacts
> - Prepare the .cml file with the context map. 
> - Prepare bounded context canvas for each bounded context. Use the bounded context scaffolder skill to create it. If skill is unavailable, state it explicitly, suggest installing business context scaffolder skill and if user refuses, try to do your best on your own. Provide the business context scaffolder all the necessary information you've extracted during the exploration session.

## 3. Design decisions and their reasons

### 3.1 Heuristics in a fixed order, interactively
The session transcript shows the trainer's method: pivotal events first, then swimlanes, then single responsibility as validation, with numbered questions to the expert in every round. The skill reproduces the rhythm, not the VeloCity answer. `references/worked-example.md` condenses the session, and warns not to use it as an answer key, because VeloCity is also a workshop exercise.

### 3.2 A bundled critic with a fixed verdict format
The critic is a brief (`agents/critic.md`), spawned as a general-purpose agent and given the evidence, not the facilitator's conclusion. It has two modes, `context` and `map`, and returns `AGREE`/`DISAGREE` with blocking findings, questions for the expert and notes. Disagreement is allowed only for reasons that change a boundary, an ownership or a relationship. Rounds are capped at 3, and after that the disagreement goes to the user.

### 3.3 A real CML validator instead of eyeballing
`scripts/validate_cml.sh` runs a single-file Java program (`ValidateCml.java`) against the Context Mapper DSL already in `pom.xml`, so no repository code was added. It reports syntax and semantic errors, plus `RULE` lines for the trainer's rules: shared kernel, partnership, direct and indirect U/D cycles, missing ACL, Conformists to justify, and isolated contexts. Writing it surfaced two facts that went into `references/cml-syntax.md`:
- CML rejects OHS and Conformist inside Customer/Supplier, and only warns about an ACL there.
- Short names such as `C`, `D` and `X` are keywords.

It also found an undocumented indirect cycle in the VeloCity reference map: Accounts → Rentals → Settlement → Accounts.

### 3.4 The trainer's rules override generic DDD advice
`references/context-map-rules.md` holds the cheat-sheet patterns as text plus the user's relationship and integration rules, verbatim in substance.

### 3.5 Canvas hand-off, canvases only
At the end the skill invokes `bounded-context-scaffolder` for canvases only, pre-filled with everything the discovery found. The scaffolder's own review gate handles plans and code later.

## 4. Skill layout

```
.claude/skills/discover-bounded-contexts/
  SKILL.md                          workflow (steps 0-6)
  agents/critic.md                  critic brief, context and map modes
  references/heuristics.md          H1-H3, cross-cutting checks, questions that work
  references/worked-example.md      condensed VeloCity session
  references/context-map-rules.md   patterns, trainer's rules, breaking a cycle
  references/cml-syntax.md          verified CML forms and gotchas
  assets/input-template.md          narrative/process template for users with no input
  assets/discovery-report-template.md
  scripts/validate_cml.sh, scripts/ValidateCml.java
  evals/evals.json, evals/files/*   prompts and simulated-expert answer sheets
  evals/scripts/grade.py            mechanical grader
```

## 5. Evaluation iterations

There were three evals:
1. Discovery from the VeloCity board screenshot.
2. Discovery plus a CML map plus canvases from a fresh TutorHub narrative.
3. Validation of the VeloCity reference CML.

Each ran with the skill and without it. A simulated expert subagent answered from a hidden answer sheet, and mechanical checks (`grade.py`) were combined with grader-agent judgment checks. Snapshots are in `evals/iteration-N/` (see `evals/README.md`).

| Iteration | With skill | Without skill | What changed before it |
|---|---|---|---|
| 1 | 100% | 70% | First draft |
| 2 | 100% | 52% | Lighter validate mode (critics only for flagged contexts, one map pass, unconfirmed links resolved by asking); flip each link before keeping a cycle; plain yes/no map question; short final message; sharper assertions; the expert may no longer name contexts |
| 3 | 94% | 38% | Flips must agree with stated facts and come from neutral questions; critic re-runs only for changed contexts; at most 5 questions per round |
| 4 | 98% | 45% | Direct cycles are never kept (resolution order: API client, then move or carry data, then Partnership, then merge); indirect cycles are kept only if the expert explicitly chooses to after seeing the options |

Assertions were sharpened after every round, so the pass rates are comparable only within an iteration.

What each iteration taught:
- **Iteration 1.** The skill found the VeloCity Settlement gap itself, and mapped internal teams as Customer/Supplier with an ACL. But validate mode spent 13 critic calls that changed nothing, and it kept a cycle it could have broken.
- **Iteration 2.** The push to break cycles produced flips that contradicted stated facts, on improvised answers to leading questions.
- **Iteration 3.** "Keep a cycle when no fact-consistent flip exists" swung too far. TutorHub kept two direct two-way relationships.
- **Iteration 4.** There were no cycles left in TutorHub, resolved by sound moves. In the VeloCity validation, the expert chose the cycle fix from three options.

Known remaining issues, not flagged by the user:
- TutorHub's cycles were resolved without offering the expert options.
- Stripe got an ACL, although the expert accepted conforming to it.
- After a declined map, the report still sketched relationships.
- Validate mode drifts in scope.
- The skill costs about 2.3× the tokens and 3× the time of a plain session.
