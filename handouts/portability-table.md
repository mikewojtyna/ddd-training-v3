# Portability table: Claude Code → your tool

Mark each cell **✅ 1:1** (works as is, or only the file name/location changes), **⚠️ needs changes** (works, but you had to rewrite or lost something), **❌ missing** (no equivalent).
Write *how*: the file name and location. Put the link to the doc page you checked in the last column.

Date checked: __________ · Tool and version: __________

| # | Capability | Claude Code (reference, as used in this repo) | GitHub Copilot | Cursor | What you had to change · source |
|---|---|---|---|---|---|
| 1 | Project instruction file | `CLAUDE.md` at the root, loaded at every session start. `@README.md` inside it imports the README. | | | |
| 2 | Per-directory / path-scoped instructions | `velocity/CLAUDE.md`, loaded once Claude reads or edits a file in that folder. Alternative: `.claude/rules/*.md` with `paths:` globs. | | | |
| 3 | Skill: on-demand instructions loaded by description, plus templates and scripts | `.claude/skills/spec-writer/SKILL.md` with `name` + `description` frontmatter. Only the description is always in context; the body loads when the request matches. Next to it: `assets/spec-template.md`, `scripts/specs.py`, `agents/spec-critic.md`, `evals/`. Also callable as `/spec-writer`. | | | |
| 4 | Script execution from instructions (deterministic steps) | The skill tells the agent to run `python3 .claude/skills/spec-writer/scripts/specs.py check` and, on the user's word, `specs.py approve` + `intents.py build`. Statuses change only through scripts. Runs via the Bash tool, with a permission prompt unless allowed. | | | |
| 5 | Subagent / critic with its own clean context | Step 4b spawns one general-purpose subagent (Agent tool) and tells it to read `agents/spec-critic.md` first. It gets paths, not the writer's conclusions, and returns `VERDICT: AGREE/DISAGREE`. Fresh context, no conversation history. | | | |
| 6 | Per-agent model choice | `model:` in a `.claude/agents/*.md` subagent file (`sonnet`, `opus`, `haiku`, `inherit`, or an ID), a `model` parameter on the Agent call, or `model:` in a skill. This repo doesn't set one: the critic inherits the session model. | | | |
| 7 | Slash commands / prompt files | Every skill is also a slash command (`/spec-writer <intent>`). Legacy `.claude/commands/*.md` still work. | | | |
| 8 | Permissions & sandbox (deny rules) | `.claude/settings.json` `permissions.allow / ask / deny`, e.g. `"deny": ["Read(./.env)", "Bash(git push *)"]`; deny wins. Optional OS sandbox for Bash. In this repo the committed file only enables the `skill-creator` plugin; personal allow rules live in the gitignored `.claude/settings.local.json`. | | | |
| 9 | Hooks | `hooks` in `.claude/settings.json` (`PreToolUse`, `PostToolUse`, `Stop`, …). A `PreToolUse` hook can block a call. Not used in this repo. | | | |
| 10 | MCP servers | `.mcp.json` at the root (project) or `~/.claude.json` (user); approved per project. This repo's `.mcp.json` (IntelliJ server `idea`) is gitignored, local only. | | | |

## Your notes

- What carried over for free:
- What you'd have to maintain twice:
- What's missing and how you'd work around it:
