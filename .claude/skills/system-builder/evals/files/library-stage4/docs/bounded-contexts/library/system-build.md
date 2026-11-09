# Library: system build manifest

## Run

| Key | Value |
|---|---|
| Started | 2026-11-09 |
| Integration branch | `library-full-implementation` |
| User's prompt | in `docs/bounded-contexts/library/conversation.md` (not part of the fixture) |
| Current stage | 4 build (prep not started) |

## Dials

| Dial | Value | Granted by |
|---|---|---|
| Domain-expert questions | role-play 🎭 | "fake the expert if you need to" |
| Stage gates | go on (standing instruction) | "don't ask me, just open a PR at the end" |
| TDD checkpoint loop | off | "fully autonomous" |

## Project conventions

Same as the repository's (`CLAUDE.md`, README): Maven wrapper, `pl.wojtyna.dddtrainingv3.library`, ports and adapters,
jMolecules, ArchUnit, Spring wiring in `infra/config`. Integration style: REST via ACL for commands/queries,
in-process `@Async @EventListener` for events, Awaitility.

## Shared decisions

| ID | Decision |
|---|---|
| D1 | TDD checkpoint loop off (autonomous run) |
| D2 | State-based aggregates |
| D3 | One ACL per outbound call |
| D4 | In-memory repositories, `Version` after the domain is green |

## Stages

| Stage | Status | Commit |
|---|---|---|
| 1 canvases | done | `a1` |
| 2 intents + specs | done | `a2` |
| 3 catalogue + plans | done (3 plans approved under the standing instruction) | `a3` |
| 4 prep | todo | |
| 4 build | todo | |
| 5 integration | todo | |

## Plans

- `docs/plans/librarian-adds-copy-to-catalog.md` (Catalog)
- `docs/plans/member-borrows-copy.md` (Lending, asks Catalog)
- `docs/plans/finance-charges-late-fee.md` (Fines, listens to Lending, charges via PaymentGateway)
