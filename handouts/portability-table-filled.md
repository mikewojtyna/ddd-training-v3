# Portability table: reference solution

**As of 2026-11-10.** Researched from the official docs listed per row and under [Sources](#sources).

> **This changes every few weeks. Re-check it one week before the training** (list under [Re-check before the training](#re-check-before-the-training)). "Not confirmed" means I couldn't find it in an official source. It doesn't mean the feature is missing.

Legend: **✅ 1:1**: works as is, or only the file name or location changes. **⚠️ needs changes**: works, but you rewrite something or lose a guarantee. **❌ missing**: no equivalent.

"Copilot" means GitHub Copilot. Support differs by surface: VS Code, Visual Studio, JetBrains, Copilot CLI, and the cloud agent and code review on github.com. VS Code is the reference below, and the other surfaces are noted where they differ. The repo is Java, so many participants will be on **JetBrains, where most customizations are still in preview** ([cheat sheet](https://docs.github.com/en/copilot/reference/customization-cheat-sheet)).

## The table

| # | Capability | Claude Code (reference) | GitHub Copilot | Cursor | What you had to change |
|---|---|---|---|---|---|
| 1 | **Project instruction file** | `CLAUDE.md` at the root, loaded at every session start. `@README.md` imports the README (relative paths, up to 4 hops). | ⚠️ `.github/copilot-instructions.md` or root `AGENTS.md`. github.com also accepts a single root `CLAUDE.md` or `GEMINI.md` instead of `AGENTS.md`. VS Code reads root `CLAUDE.md` when `chat.useClaudeMdFile` is on. **`@README.md` is not expanded**: VS Code includes linked files only when `chat.includeReferencedInstructions` is on (off by default). | ⚠️ Root `AGENTS.md` or `CLAUDE.md` is read automatically and always applied. Alternatively, `.cursor/rules/*.mdc` with `alwaysApply: true`. `@path` imports: not confirmed. | The rules carry over word for word. Inline the README parts the agent needs (layout, conventions, build commands) or link them, and don't rely on `@README.md`. The cheapest cross-tool setup is one `AGENTS.md` for Copilot and Cursor (Claude Code can also read `AGENTS.md`). |
| 2 | **Per-directory / path-scoped instructions** | `velocity/CLAUDE.md`, loaded once Claude reads or edits a file in that folder. Alternative: `.claude/rules/*.md` with `paths:`. | ⚠️ `.github/instructions/velocity.instructions.md` with `applyTo: "src/main/java/pl/wojtyna/dddtrainingv3/velocity/**"`. Nested `AGENTS.md` (github.com: the nearest wins; VS Code: needs `chat.useNestedAgentsMdFiles`, **experimental, off by default**). VS Code also reads `.claude/rules/*.md` with `paths`. Nested `CLAUDE.md`: not listed. On github.com, path-specific files apply only to the cloud agent and code review. | ⚠️ Nested `src/main/java/.../velocity/AGENTS.md` (combined with the parent, the more specific file wins). Or `.cursor/rules/velocity.mdc` with `globs: src/main/java/pl/wojtyna/dddtrainingv3/velocity/**` (auto-attached when a matching file is in context). Nested `CLAUDE.md`: not confirmed. | Rename or copy the file; the content is unchanged. Trigger: Claude loads the file **when a file in the folder is touched**, while `applyTo`/`globs` attach it **when a matching file is in context**. Close, but test it with a VeloCity-only question. |
| 3 | **Skill**: on-demand instructions loaded by description, plus templates and scripts | `.claude/skills/spec-writer/SKILL.md` (`name`, `description`), `assets/`, `scripts/`, `agents/`, `evals/`. Description always in context, body on match, files when referenced. Also `/spec-writer`. | ⚠️ **Reads `.claude/skills/` directly**, as well as `.github/skills/` and `.agents/skills/`. Same `SKILL.md` format (Agent Skills standard), progressive loading, `/spec-writer`, bundled scripts. Supported in VS Code, Visual Studio, JetBrains (preview), CLI, the cloud agent and code review. **`description` max 1024 characters, and spec-writer's has 1075.** Behaviour above the limit: not confirmed. | ✅ **Reads `.claude/skills/`** "for compatibility", plus `.cursor/skills/` and `.agents/skills/`. Same format, `scripts/` supported, `/spec-writer`, `disable-model-invocation`. Third-party configs may need *Settings → Rules, Skills, Subagents → Include third-party Plugins, Skills, and other configs* (named on Cursor's third-party hooks page). | Copilot: shorten the description to 1024 characters or less (the open standard has the same limit; 7 of this repo's 9 skills are over). If you move the skill to `.github/skills/`, fix the hard-coded `python3 .claude/skills/spec-writer/scripts/...` paths. The description names sibling skills (`intent-creator`, `implementation-planner`); they must exist too. |
| 4 | **Script execution from instructions** (deterministic steps such as `specs.py`) | The Bash tool runs `specs.py check`, then `approve` + `intents.py build` on the user's word. Permission prompt unless allowed in settings. | ✅ Agent mode runs terminal commands after approval. VS Code: per-command auto-approve rules (regex), and all subcommands must match. CLI: `--allow-tool='shell(python3)'`. The cloud agent runs in its own environment; whether `python3` is available there is not confirmed for this repo. | ✅ Agent runs terminal commands. Run Modes: *Auto-review* (default: allowlist, then sandbox, then a classifier), *Allowlist*, *Run Everything*. Pre-approve with `terminalAllowlist` in `.cursor/permissions.json`. | Nothing in the skill. The point carries over: **the status change is in the script, not in the model**, so `approve` still refuses while `check` has errors. Decide whether `python3 .claude/skills/...` gets auto-approved. |
| 5 | **Subagent / critic with its own clean context** | Step 4b spawns a general-purpose subagent with the Agent tool, which reads `agents/spec-critic.md`. Fresh context, no history. Returns `VERDICT`. | ⚠️ VS Code subagents run **in their own context, stateless**: the `runSubagent` tool (enable `agent/runSubagent`) or `task` in the Copilot harness. Register the critic as `.github/agents/spec-critic.agent.md` with `user-invocable: false`. VS Code also reads Claude-format `.claude/agents/*.md`. Cloud agent calling a custom agent as a subagent: not confirmed. | ✅ Subagents in `.cursor/agents/` (also reads `.claude/agents/`). "Each subagent has its own context window." `readonly: true` stops edits. Called automatically from the `description`, or explicitly with `/spec-critic`. | Rewrite Step 4b: "Spawn one critic with the Agent tool (general-purpose)" names a Claude Code tool. Write instead: "Delegate to the `spec-critic` subagent". Keep the brief as the single source and let the agent file point to it (see the worked examples). Check that the critic still gets **paths, not the writer's conclusions**. |
| 6 | **Per-agent model choice** | `model:` in `.claude/agents/*.md` (`sonnet`, `opus`, `haiku`, `inherit` or an ID), `model` on the Agent call, `model:` in a skill. The critic here inherits the session model. | ⚠️ `model:` in `.agent.md` (a string, or a list in preference order). Documented for VS Code, JetBrains, Eclipse and Xcode; honoured on github.com or by the cloud agent: not confirmed. VS Code checks a subagent's model against the main model's cost tier. Prompt files have `model:` too. | ✅ `model:` in the subagent file: `inherit` (default) or a model ID. `model` in a skill: not confirmed. | Add the `model` line to the critic file. Model names differ per tool, so copy them from the tool's picker. This is the Exercise 13 extension (a cheaper critic) in another tool. |
| 7 | **Slash commands / prompt files** | Every skill is `/name`. Legacy `.claude/commands/*.md` still work. | ✅ Skills appear under `/`. Prompt files: `.github/prompts/*.prompt.md` with `description`, `name`, `argument-hint`, `agent`, `model`, `tools`, and `${input:...}`. Prompt files run in VS Code, Visual Studio and JetBrains (preview), **not on github.com or in the CLI**. | ✅ Skills appear under `/` (`disable-model-invocation: true` makes them manual-only). `.cursor/commands/*.md` appears only on the older docs.cursor.com page. Current docs offer `/migrate-to-skills` to turn commands into skills. | Nothing, if you use the skill. A `write-spec.prompt.md` is only a shortcut. |
| 8 | **Permissions & sandbox** (deny rules like `.claude/settings.json`) | `permissions.allow/ask/deny`, e.g. `Read(./.env)`, `Bash(git push *)`, committed and shared; deny wins. Optional OS sandbox. (In this repo: only a plugin is enabled. The deny example is M8.) | ⚠️ **No committed deny file that covers every surface.** VS Code: command auto-approve rules; `chat.tools.edits.autoApprove` with `"**/.env": false` (edits only, **no read-deny**); OS-level agent sandboxing. CLI: `--deny-tool='shell(git push)'` (deny beats allow, even `--allow-all`), saved per user in `~/.copilot/permissions-config.json`; a repo-level file is not confirmed. Org-wide: **content exclusion** (repo, org or enterprise settings on GitHub) also applies to IDE agents and the CLI, but "might not detect indirect file access". | ⚠️ IDE: `.cursor/permissions.json` has **allowlists only** (`terminalAllowlist`, `mcpAllowlist`, `autoRun` steering), "not a security guarantee". Sandbox: macOS Seatbelt, Linux Landlock (kernel 6.2 or later), network off by default. `.cursorignore` hides files. For a real deny, use hooks (row 9). CLI: `.cursor/cli.json` `permissions.deny`, e.g. `Shell(rm)`, `Read(.env)`; deny wins. | The rules have to be **re-expressed in another mechanism**, sometimes per surface, sometimes by an admin. This is the row to bring to M8. |
| 9 | **Hooks** | `hooks` in `.claude/settings.json`. A `PreToolUse` hook can block. (Not used in this repo.) | ⚠️ `.github/hooks/*.json`, also in `.agent.md` `hooks`. `preToolUse` can return `permissionDecision: "deny"`. Also reads the repo's `.claude/settings.json` hooks, but "sharing a hook file location does not establish runtime compatibility". VS Code hooks are **preview**; JetBrains and Visual Studio have none. github.com and the CLI support them. | ✅ `.cursor/hooks.json`. `preToolUse`, `beforeShellExecution`, `beforeReadFile` and `beforeMCPExecution` can deny; exit code 2 blocks, as in Claude Code. Can load Claude hooks from `.claude/settings.json` once third-party configs are enabled (a forum report on 2.6.11 says project hooks didn't load). | Event names and payloads differ (Copilot `preToolUse`, `agentStop`; Cursor `beforeShellExecution`, `stop`). Test every hook in the target tool; don't assume the Claude file works. |
| 10 | **MCP servers** | `.mcp.json` (project) or `~/.claude.json` (user), approved per project. (This repo's `.mcp.json` is local and gitignored.) | ✅ VS Code `mcp.json` (`.vscode/mcp.json`; the security page says a workspace-root `.mcp.json` follows workspace trust too). CLI `~/.copilot/mcp-config.json`. github.com: repository MCP settings (cloud agent, code review). Per agent: `mcp-servers`. | ✅ `.cursor/mcp.json` or `~/.cursor/mcp.json`, `mcpServers` with `command`/`args`/`env` or `url`/`headers`. | Copy the server entries into the tool's file. A third-party MCP server is code you run, which is the M8 supply-chain point. |

### Summary

| | ✅ | ⚠️ | ❌ |
|---|---|---|---|
| **GitHub Copilot** (VS Code reference) | 4, 7, 10 | 1, 2, 3, 5, 6, 8, 9 | none |
| **Cursor** | 3, 4, 5, 6, 7, 9, 10 | 1, 2, 8 | none |

Nothing is fully missing any more: both tools have skills, subagents, per-agent models, hooks and MCP. The friction is in the details: the README import, nested files, the description length, a Claude-specific sentence in Step 4b, and above all **deny rules that don't travel**. Copilot's ⚠️s are mostly surface-dependent: a feature exists in VS Code but is preview in JetBrains or absent on github.com or in the CLI.

Two more notes:
- **VS Code can also host a Claude harness** that reads `CLAUDE.md`, `.claude/rules`, `.claude/agents` and `.claude/skills` natively. The VS Code hooks page notes that the Claude and Codex harnesses are **disabled when Copilot enterprise-managed settings apply**. Check that before promising it to a corporate team.
- **Gemini users:** agents.md lists Gemini CLI as an `AGENTS.md` reader, and GitHub accepts `GEMINI.md`. Not researched further.

## Worked example: spec-writer in Copilot (VS Code)

```
AGENTS.md                                   ← root CLAUDE.md "Rules for Claude" + README essentials inlined (no @import)
.github/
  instructions/velocity.instructions.md     ← velocity/CLAUDE.md, with applyTo
  agents/spec-critic.agent.md               ← registers the critic; points to the existing brief
  prompts/write-spec.prompt.md              ← optional /write-spec shortcut
.claude/skills/spec-writer/                 ← unchanged location (Copilot reads it). Edit: description ≤ 1024 chars, Step 4b wording
```

`.github/instructions/velocity.instructions.md`:
```markdown
---
applyTo: "src/main/java/pl/wojtyna/dddtrainingv3/velocity/**"
---
<content of src/main/java/pl/wojtyna/dddtrainingv3/velocity/CLAUDE.md>
```

`.github/agents/spec-critic.agent.md`:
```markdown
---
name: spec-critic
description: Independent reviewer of a draft spec in docs/specs. Use only when the spec-writer skill asks for a critic review.
tools: ['read', 'search']
model: <a cheaper model from your picker>
user-invocable: false
---
Read `.claude/skills/spec-writer/agents/spec-critic.md` first and follow it exactly.
You get paths, not conclusions. Return the VERDICT block it defines, nothing else.
```

Step 4b in `SKILL.md`, the first bullet becomes:
```markdown
- Delegate to the `spec-critic` subagent. Its prompt contains: the paths of this skill's SKILL.md,
  the spec, the intent, the vision and related specs; the user's answers so far; the round number.
```
In VS Code, the agent that runs the skill needs the subagent tool (`agent/runSubagent`, or `agents: ['spec-critic']` plus the `agent` tool set in a custom agent).

## Worked example: spec-writer in Cursor

```
AGENTS.md                                                  ← or keep CLAUDE.md: Cursor reads it (README import still not expanded)
src/main/java/pl/wojtyna/dddtrainingv3/velocity/AGENTS.md  ← copy of velocity/CLAUDE.md
.cursor/agents/spec-critic.md                              ← registers the critic
.claude/skills/spec-writer/                                ← unchanged (Cursor reads it); Step 4b wording as for Copilot
```

`.cursor/agents/spec-critic.md`:
```markdown
---
name: spec-critic
description: Independent reviewer of a draft spec in docs/specs. Use only when the spec-writer skill asks for a critic review.
model: inherit          # or a cheaper model ID
readonly: true
---
Read `.claude/skills/spec-writer/agents/spec-critic.md` first and follow it exactly.
You get paths, not conclusions. Return the VERDICT block it defines, nothing else.
```

For a real deny (`git push`, reading `.env`), add `.cursor/hooks.json` with a `beforeShellExecution` / `beforeReadFile` hook. `.cursor/permissions.json` can only allow.

## Re-check before the training

Do this one week before. Each item is a claim above that is new, preview or surface-dependent:

1. Copilot JetBrains: are instructions, prompt files, custom agents and skills still **preview**? Hooks still unsupported? ([cheat sheet](https://docs.github.com/en/copilot/reference/customization-cheat-sheet))
2. Copilot skill `description` limit (1024) and what happens above it. Shorten the repo's skills if it now fails to load.
3. VS Code `chat.useNestedAgentsMdFiles`: still experimental and off by default? Nested `CLAUDE.md` supported yet?
4. VS Code `chat.includeReferencedInstructions` (README link) default.
5. Copilot `.agent.md` `model`: honoured by the cloud agent and on github.com?
6. Copilot cloud agent: can it call custom agents as subagents?
7. VS Code hooks: still preview? Does the Copilot harness still read `.claude/settings.json` hooks?
8. Copilot CLI: is there a **repo-level** permissions/deny file by now?
9. Cursor: is the *Include third-party Plugins, Skills, and other configs* toggle still needed for `.claude/skills`, `.claude/agents` and `.claude/settings.json` hooks?
10. Cursor `.cursor/permissions.json`: still allowlist-only (no `deny`)? Is the CLI's `cli.json` deny now honoured in the IDE?
11. Cursor `.cursor/commands`: still supported or fully replaced by skills?
12. The participants' company Copilot policies: preview features, MCP, which harnesses are allowed (enterprise-managed settings disable the Claude and Codex harnesses in VS Code).

## Sources

All accessed 2026-11-10.

**GitHub Copilot**
- Customization cheat sheet (surface matrix): https://docs.github.com/en/copilot/reference/customization-cheat-sheet
- Repository custom instructions (`copilot-instructions.md`, `.instructions.md`, `AGENTS.md`, `CLAUDE.md`/`GEMINI.md`): https://docs.github.com/en/copilot/how-tos/configure-custom-instructions/add-repository-instructions
- About agent skills (locations, surfaces): https://docs.github.com/en/copilot/concepts/agents/about-agent-skills
- Create custom agents (cloud agent, `model`, `target`, `mcp-servers`): https://docs.github.com/en/copilot/how-tos/use-copilot-agents/cloud-agent/create-custom-agents
- Copilot CLI: https://docs.github.com/en/copilot/how-tos/use-copilot-agents/use-copilot-cli
- Copilot CLI allowing tools (`--allow-tool`, `--deny-tool`): https://docs.github.com/en/copilot/how-tos/copilot-cli/use-copilot-cli/allowing-tools
- Copilot CLI configuration: https://docs.github.com/en/copilot/how-tos/copilot-cli/set-up-copilot-cli/configure-copilot-cli
- Copilot CLI command reference: https://docs.github.com/en/copilot/reference/copilot-cli-reference/cli-command-reference
- Content exclusion: https://docs.github.com/en/copilot/how-tos/configure-content-exclusion/exclude-content-from-copilot

**VS Code (Copilot)**
- Customization overview: https://code.visualstudio.com/docs/copilot/customization/overview
- Custom instructions: https://code.visualstudio.com/docs/copilot/customization/custom-instructions
- Custom instructions reference (settings `chat.useAgentsMdFile`, `chat.useNestedAgentsMdFiles`, `chat.useClaudeMdFile`, `chat.includeReferencedInstructions`): https://code.visualstudio.com/docs/agents/reference/custom-instructions-reference
- Agent skills (1024-character description, `context: fork`): https://code.visualstudio.com/docs/copilot/customization/agent-skills
- Custom agents: https://code.visualstudio.com/docs/copilot/customization/custom-agents
- Custom agent file reference (`model`, `user-invocable`, `.claude/agents`, `.chatmode.md` rename): https://code.visualstudio.com/docs/agents/reference/custom-agent-file-reference
- Subagents (own context, model resolution): https://code.visualstudio.com/docs/copilot/agents/subagents
- Prompt files: https://code.visualstudio.com/docs/copilot/customization/prompt-files
- Hooks (preview, `.claude/settings.json`, enterprise note): https://code.visualstudio.com/docs/copilot/customization/hooks
- Security (auto-approve, sandboxing, edit protection): https://code.visualstudio.com/docs/copilot/security

**Cursor**
- Rules (`.mdc`, `alwaysApply`, `globs`, nested `AGENTS.md`): https://cursor.com/docs/context/rules
- `CLAUDE.md` support: https://cursor.com/help/customization/rules
- Skills (`.claude/skills` compatibility, `/migrate-to-skills`): https://cursor.com/docs/context/skills
- Subagents (`.cursor/agents`, `.claude/agents`, `model`, `readonly`): https://cursor.com/docs/agent/subagents
- Hooks: https://cursor.com/docs/agent/hooks
- Third-party (Claude Code) hooks: https://cursor.com/docs/reference/third-party-hooks
- MCP: https://cursor.com/docs/context/mcp
- `permissions.json` (IDE): https://cursor.com/docs/reference/permissions
- Run modes and sandbox: https://cursor.com/docs/agent/security/run-modes
- CLI permissions (`cli.json`, deny): https://cursor.com/docs/cli/reference/permissions
- Commands (older docs): https://docs.cursor.com/en/agent/chat/commands
- Forum report, Claude hooks not loading (2.6.11): https://forum.cursor.com/t/claude-hooks-dont-work/153614

**Claude Code and standards**
- Memory (`CLAUDE.md`, `@imports`, subdirectory loading, `.claude/rules`, `AGENTS.md`): https://code.claude.com/docs/en/memory
- Skills: https://code.claude.com/docs/en/skills
- Subagents: https://code.claude.com/docs/en/sub-agents
- Settings and permissions: https://code.claude.com/docs/en/settings
- AGENTS.md: https://agents.md/
- Agent Skills specification (1024-character description): https://agentskills.io/specification
