# <System>: system build manifest

The orchestrator's working file and **the resume point**. Whoever continues this build, whether a new session or
another person, starts here. Keep it current after every step.

## Run

| Key | Value |
|---|---|
| Started | <YYYY-MM-DD> |
| Integration branch | `<branch>` (from `<default branch>` at `<sha>`) |
| User's prompt | quoted verbatim in the session record: `<path>/conversation.md` |
| Current stage | 1 canvases / 2 intents+specs / 3 plans / 4 build / 5 integration / done |
| PR | – |

## Dials

| Dial | Value | Granted by |
|---|---|---|
| Domain-expert questions | role-play 🎭 / ask the user | "<quote from the prompt>" |
| Stage gates | go on (standing instruction) / stop after each stage | "<quote>" |
| TDD checkpoint loop | off / on (relay through the orchestrator) | "<quote>" |
| Critics | one stage critic per stage / each skill's own critics | derived from the dials |

## Project conventions (detected or defaulted)

| Item | Value | Source |
|---|---|---|
| Build / test command | `./mvnw test` | detected: `mvnw` |
| Base package, system package | `<base>.<system>` | CLAUDE.md |
| Context layout | ports and adapters: `api/{usecases,events,queries}`, `domain`, `infra/{adapters,config}` | README |
| Context map (CML) | `src/main/cml/<System>-Context-Map.cml` | |
| Docs | canvases `docs/bounded-contexts/<system>/`, intents `docs/intents/`, specs `docs/specs/`, plans `docs/plans/` | |
| Integration style | REST via ACL for commands/queries, in-process `@Async @EventListener` for events, Awaitility | prompt / default |
| Flow skills present | intent-creator, spec-writer, implementation-planner, bounded-context-scaffolder, ddd-tactical-building-blocks, discover-bounded-contexts | `.claude/skills/` |
| Allowed removals | <what the user allowed to delete> | "<quote>" |

## Shared decisions (cited by every plan)

| ID | Decision | Reason |
|---|---|---|
| D1 | TDD checkpoint loop: <on/off> | dial |
| D2 | Aggregate style default: state-based | |
| D3 | One ACL per outbound command or query | |
| D4 | In-memory repositories; `Version` added after the domain is green | |

## Stages

| Stage | Status | Commit | Notes |
|---|---|---|---|
| 1 canvases | todo | | |
| 2 intents + specs | todo | | |
| 3 catalogue + plans | todo | | |
| 4 prep | todo | | |
| 4 build | todo | | |
| 5 integration | todo | | |

## Contexts and waves

`waves.py` output (with `--event-only ...`):

```
<paste>
```

| Context | Package | Wave | Upstreams | Plans / steps owned | Builder agent | Worktree branch | Status | Verified tests (domain / spec / other) |
|---|---|---|---|---|---|---|---|---|
| | | | | | | | todo / building / at checkpoint / done / merged | |

## Intent manifest (stage 2)

| Seq | Intent | Actor | Activity | Task | Area | Owning context(s) |
|---|---|---|---|---|---|---|

Shared business events: *Event One*, *Event Two*, …

## Open items

- Contract gaps not yet folded:
- Expert questions waiting:
- Shared changes requested by builders:
- Known follow-ups:
