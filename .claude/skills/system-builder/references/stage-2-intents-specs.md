# Stage 2: intents and specs

**Input:**
- the vision (extract the product scenarios from it);
- the context map;
- the canvases and DE answers from Stage 1.

**Output:**
- every intent, plus the story map and the generated intent index;
- one spec per intent;
- all checks at 0 errors;
- specs approved through the script;
- the stage-2 commit.

Lessons that apply: O3–O8, S1–S6.

If the project has no `intent-creator` / `spec-writer` skill, say so. Agree with the user whether to skip this stage
(Stage 3 then plans from the canvases directly) or to write the artifacts in the documented shape of the missing
skill. Don't imitate a skill silently.

## 1. Story map and slicing: the orchestrator does this alone

1. Read `intent-creator`'s SKILL.md and templates, and the existing story map.
2. Extend the story map with the system's activities, in journey order. A rental system, for example, runs from
   *Join* to *Set prices*.
3. Cut the vision into fine-grained intents: one actor, one goal, one outcome each. A coarse section becomes several
   intents. Time-driven needs get a business actor (S3).
4. Write the **intent manifest** into `system-build.md`: name, sequence number (continue the global sequence), actor,
   activity, task and area for every intent (S1).
5. Write the **shared business-event list**: every past-tense business event the intents will name (S2).

In the loop, show the user the story map and the manifest and stop. Slicing is a product decision.

## 2. Parallel area agents

Group the intents by area (4–6 areas for a system of about 8 contexts). Spawn one general-purpose agent per area, **in a
single message**. Every agent gets the same brief, plus its own manifest rows:
- the absolute paths of `intent-creator` and `spec-writer` SKILL.md, with the instruction to follow them **exactly**
  (templates, sections, frontmatter, scripts);
- its manifest rows (fixed: no renumbering, no new intents; a needed intent is reported back);
- the shared event list (use it word for word);
- the paths of the vision, the canvases and `domain-expert-answers.md`;
- the setting for expert questions:
  - **role-play:** use the DE answers instead of `?`. A gap with no answer is reported back as a question, not
    invented;
  - **ask the user:** report questions, don't answer them;
- apply `spec-writer`'s critic checklist (`agents/spec-critic.md`) as a **separate self-review pass**;
- run `intents.py check` and `specs.py check <intent>` until there are 0 errors. Never edit the scripts (O5);
- don't move any status. The orchestrator approves;
- its own temporary directory (`<scratch>/<agent-name>/`); never a shared scratch folder (O12);
- return: the files written, the questions found (with the example IDs they block), the check output.

## 3. Second round

The agents will find gaps (S5).
- **Role-play:** answer them as the next DE range (DE-80+), update the canvases, and **resume each agent with
  SendMessage** to apply the answers (O4).
- **Ask the user:** batch the questions by area with proposed answers. Stop, then resume the agents with the answers.

## 4. Checks and approval

- `intents.py build` and `intents.py check`, then `specs.py check`: all at 0 errors.
- Check the event names in the specs' contracts against the shared list.
- Intents move `draft → accepted → specified`:
  - **Autonomous:** set `status: accepted` (the standing instruction is the acceptance, O7), then
    `specs.py approve <intent>` for each spec, then `intents.py build`. Add the standing-instruction note to each
    spec's decision record.
  - **In the loop:** present the specs. The user accepts the intents and approves the specs; then run the scripts.
- Non-blocking open questions stay open (O6).

## 5. Commit

Commit: `<System> stage 2: <n> intents with story map, and their specs`. Update the record and the manifest. Gate as
the dial says.
