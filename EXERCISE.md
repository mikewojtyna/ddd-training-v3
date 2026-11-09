# Exercise 14: Port it to your tool

**Module:** M7 Advanced agent setup & SDD · **Time:** 30 min · **Form:** pairs · **Start:** tag `ex14/start`

## Situation

Everything you've used so far was built for Claude Code. Your team uses GitHub Copilot (VS Code, IntelliJ, github.com) or Cursor. The question for Monday is: how much of this setup survives the move, and what do you have to rebuild?

You port four things:

| What | Where it is now | What it does |
|---|---|---|
| Project instructions | `CLAUDE.md` (root) | Repo rules. Pulls in `README.md` with the line `@README.md`. |
| Per-directory instructions | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/CLAUDE.md` | VeloCity context. Claude Code loads it only when it touches a file in that folder. |
| One skill | `.claude/skills/spec-writer/` | `SKILL.md` (the `description` is the trigger), `assets/spec-template.md`, `scripts/specs.py` (checks the spec and changes statuses), `evals/`. |
| Its critic | `.claude/skills/spec-writer/agents/spec-critic.md` | A brief for a second agent that reviews the draft in its own clean context. The skill spawns it in Step 4b. |

Also look at `.claude/settings.json` (the shared settings: here it only enables a plugin; deny rules come in M8). Note that statuses never change by hand: the skill calls `specs.py approve` and `intents.py build`.

## Your task

1. **Pick your tool** (5 min): Copilot in VS Code, Copilot in IntelliJ, Copilot CLI, or Cursor. Open `handouts/portability-table.md`. The Claude Code column is filled in as the reference. Skim the four files above.
2. **Port** (15 min). Work on a scratch branch. Use the tool's own docs, not memory: this area changes every month.
   - The instruction file. Copilot: `.github/copilot-instructions.md` or `AGENTS.md`. Cursor: `AGENTS.md` or a rule in `.cursor/rules/`. What happens to `@README.md`?
   - The per-directory file. Copilot: a path-specific `.github/instructions/*.instructions.md` with `applyTo`, or a nested `AGENTS.md`. Cursor: a rule with `globs`, or a nested `AGENTS.md`.
   - The skill. Find out first whether your tool already reads `.claude/skills/`. If it does, what in `SKILL.md` is still Claude-specific?
   - The critic. Copilot: a custom agent in `.github/agents/`. Cursor: a subagent in `.cursor/agents/`. Can you give it a cheaper model and read-only tools?
3. **Try one prompt** (5 min). Copy a test intent in and ask for its spec:
   ```bash
   cp .claude/skills/spec-writer/evals/files/seed-intents/rider-reserves-bike-at-station.md docs/intents/
   ```
   Then, in agent mode: *"Write the spec for rider-reserves-bike-at-station"*. Watch for:
   - Did the skill trigger by itself, or did you have to name it?
   - Did it run `specs.py check`? Did it ask you before running the script?
   - Did the critic run in a separate context, and did you see its verdict?
   - Did it leave the reservation period as `?` with an open question, instead of inventing 15 minutes?
   - Did it stop for your review, or did it approve the spec itself?
4. **Fill in the table** (5 min): for each row, ✅ 1:1, ⚠️ needed changes, ❌ missing, and one line on what you changed.

## Done when

- Your tool has the four ported pieces in its own locations (or you've shown it already reads the Claude ones).
- You ran the prompt once and can say which of the five checks above passed.
- Every row of the table has a mark and a "what you had to change" note for your tool.

## Debrief questions

- What carried over for free? What would you have to maintain twice?
- Which rows are about **enforcement** (scripts, permissions, hooks) and which only about **advice** to the model? Which of those can your tool actually enforce?
- Is the critic still independent in your tool, or does it share the writer's context?
- Which file would you put in a team-wide repo so it works for Copilot, Cursor and Claude Code at once?
- Compare with the reference: `git diff ex14/start ex14/solution`.

## Rules

- Don't commit the ported files or the test intent to the training repo. Use a scratch branch and throw it away.
- The tools change fast. Every claim in your table needs a link to the doc page you checked today.
