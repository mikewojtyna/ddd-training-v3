# Stage 1: from the context map and vision to canvases

**Input:** the system's context map (CML, or a markdown map) and its product vision or process docs.
**Output:**
- one Bounded Context Canvas per internal context, in `bounded-context-scaffolder`'s template, at its default
  location;
- `domain-expert-answers.md`;
- CML updates, if any;
- the stage-1 commit.

Lessons that apply: O3, O6, O8, C1–C7 in `lessons.md`.

## 0. No context map?

If the system has no context map, Stage 1 starts with **`discover-bounded-contexts`** in discover mode, with the vision
as the narrative.
- In the loop, that is its normal interactive session with the user as the expert.
- In the autonomous setting, tell it that the expert's answers come from the central answer file (role-played, 🎭), that
  its critics still run, and that it should write the CML to a new file, never over an existing reference map.

Record the map it produced in the manifest, then continue here. With a markdown map only, write the CML from it first
and validate it.

## 1. Read everything first

- The vision and process docs, end to end. Note every number, rule, actor and contradiction.
- The CML: contexts, `domainVisionStatement`, responsibilities, relationships and the comments on them (they often
  hold the messages).
- Existing canvases and code for this system. Treat them as input, not as the source of truth, unless the user says
  otherwise. Note what the user said may be deleted.
- The flow skills you'll drive (their SKILL.md and templates), so the artifacts you write fit their checks.

## 2. Collect the questions, then answer them in one place

Before writing any canvas, walk through each context's responsibilities against the vision and list every question you
would ask the expert:
- missing numbers (durations, fees, limits, thresholds);
- contradictions (the vision is often inconsistent on purpose in a workshop: pick one reading and say why);
- unconfirmed relationships in the map;
- actor variants (who else can issue this command?);
- the end of every lifecycle (can this state last forever?).

Write `domain-expert-answers.md` from `assets/domain-expert-answers-template.md`, grouped by area, with IDs `DE-1` …:
- **Autonomous:** answer each question as the expert would (🎭). Prefer the reading that is most consistent with the rest
  of the vision. State the reasoning in one clause where it isn't obvious ("nobody rides 59 minutes for free").
- **In the loop:** fill in a **proposed** answer for each question. Then ask the user in rounds of at most ~10 questions,
  grouped by area, with the proposals as the defaults ("reply with the IDs you want to change"). Mark confirmed answers
  👤. Stop after each round and wait.

## 3. Write the canvases yourself

One author, all canvases (C1). For each context:
- name exactly as in the CML; purpose; strategic classification; domain roles from the worksheet (C7);
- ubiquitous language with definitions;
- inbound and outbound messages, each with collaborator and type (query, command or event), named once for the whole
  system. Keep a running list of message names and reuse it;
- business decisions as strict Given/When/Then with concrete values, each citing the DE answers it rests on (C2). Every
  inbound command gets at least one rule (C3);
- translations of enumerated values that cross into this context (C5);
- assumptions (the 🎭 answers this canvas relies on) and open questions (non-blocking only, O6).

Then read the canvases **against each other**: the same command from another actor, the same thing in another state
(C4); outbound messages matching the receiver's inbound list; names matching.

## 4. Update the map if the canvases prove it wrong

Apply confirmed, changed or removed relationships to the CML (C6), with a comment citing the DE answer. Validate it with
the discovery skill's validator (in this repo `.claude/skills/discover-bounded-contexts/scripts/validate_cml.sh <file>`).
No direct upstream/downstream cycles. Keep a markdown version of the map in sync if one exists. If the map is a workshop's
reference solution, change it only if the user's prompt allows it. Otherwise write a new CML file and say so.

## 5. Critic

- **Autonomous:** spawn one stage critic (`agents/stage-critic.md`, `stage: 1`) with the canvases, the vision, the CML
  and the DE answers. Fix the blocking findings. Run a second round only if the fixes weren't simply additive.
- **In the loop:** the same critic, then present its findings with your fixes to the user at the gate.

## 6. Gate and commit

Commit: `<System> stage 1: bounded context canvases for all <n> contexts`.
- **Stage gates on stop:** give the user the canvas paths, the DE answers that need their eye, the map changes and the
  critic's verdict. Then stop.
- **Stage gates on go:** record the stage in the session record and the manifest, then go on.
