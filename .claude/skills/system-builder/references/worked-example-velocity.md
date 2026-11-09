# Worked example: VeloCity, 2026-11-08

A condensed record of the run this skill was distilled from. Read it once per session to get the rhythm and the scale.
The full record is `docs/bounded-contexts/velocity/conversation.md` on the frozen branch `velocity-full-implementation`
of the ddd-training-v3 repository, if it is available.

## The prompt (abridged, the essentials verbatim)

> I want you to fully implement all bounded contexts and relationships between them (including adapters and cross-bc
> communication with these adapters) of VeloCity. I'll be gone, so you need to do your best and implement everything
> without asking. […]
> - Divide the work in 4 stages. After each stage prepare a meaningful commit.
> - Record the entire session (including my initial prompt) in a file, so it can be used for educational purposes.
> - Feel free to totally update or even remove the existing velocity code, old bounded context canvases […]
> - If you have any doubts, fake the domain expert's answer. You can update bounded context canvas and context map if
>   you've found some new insights/inconsistency.
>
> Stage 1: canvases (input: context map, vision). Stage 2: intents and specs (input: context map, vision). Stage 3: a
> plan for each intent-spec pair. Stage 4: implement, with REST adapters for commands and queries and the in-process
> Spring event bus for integration events; bounded-context integration tests must be Spring tests with real beans;
> cross-context tests use Awaitility.

This prompt maps onto the dials as follows:
- expert questions: role-play;
- stage gates: go on (standing instruction);
- TDD checkpoint loop: off.

## What happened per stage

| Stage | The orchestrator itself | Parallel agents | Result |
|---|---|---|---|
| 1 | Collected every gap into DE-1…DE-73 (🎭), wrote all 8 canvases, updated the CML (one relationship confirmed, two CF → ACL), ran one critic | – | 8 canvases. The critic said DISAGREE, with 2 blocking findings in Rentals (commands with no rules; technician vs rider variants), fixed |
| 2 | Story map (10 activities), 35-intent manifest, shared event list | 5 area agents (intents + specs) | Agents found gaps → DE-80…DE-98, agents resumed. One intent renamed to satisfy `specs.py`. 35/35 clean, approved |
| 3 | Contract catalogue, shared decisions D1–D4 | 5 area planners | ~25 contract gaps → addendum. A spec bug found by haversine (155.7 m vs "on the border") → spec fixed and re-approved. 35/35 plans clean, approved |
| 4 prep | Clock, REST client with the port resolved at call time, `@EnableAsync`, scheduling switch, error body, `@VelocitySpringTest`, Awaitility, all published events, a smoke test (caught the `@Async` proxy trap), old code removed | – | One prep commit |
| 4 build | Merged the branches, checked each report | 8 builders in worktrees, in waves | Merges conflict-free. The first builders had to fast-forward themselves off `master` |
| 5 | – | 1 journey writer | 5 journeys, all green first time. 612 tests green twice. Skills repointed, `CLAUDE.md` rewritten, PR opened |

In this skill, stage 5 belongs to the orchestrator and adds the communication matrix, the API-only scan and the
map-vs-code validation with `discover-bounded-contexts`. Those are the steps the VeloCity run lacked.

## Waves

`waves.py src/main/cml/VeloCity-Context-Map.cml --event-only "Rentals->Settlement"` reproduces them:

| Wave | Contexts | Why then |
|---|---|---|
| 1 | Payments, Pricing, Customer Support | no outbound calls to other VeloCity contexts |
| 2 | Accounts | verifies cards in Payments |
| 3 | Rentals, Settlement | Rentals asks Accounts for eligibility. Settlement calls Pricing, Payments and Accounts, and only *listens* to Rentals |
| 4 | Fleet Maintenance, Debt Recovery | they call Rentals + Customer Support, and Settlement |

Accounts started as soon as Payments was merged.

## Results per context

| Context | Aggregates / blocks | Domain tests | Spring acceptance tests | A decision worth knowing |
|---|---|---|---|---|
| Payments | `RiderPaymentMethods` | 12 | 13 | only invalidating the *current* card is published |
| Pricing | `TariffSchedule` + domain service `FeeCalculator` | 34 | 35 | launch tariff seeded at start-up; billing time cut to whole seconds |
| Customer Support | `Incident` | 46 | 39 | uncontested rejections resolved by a scheduled job |
| Accounts | `Account` + factory `RiderRegistration` | 36 | 43 | the invalid-card block comes only from Payments' event |
| Rentals | `Bike` + factory `Fleet` | 67 | 51 | "one rider, one bike" checked across bikes by the repository; a race is accepted |
| Settlement | `RideSettlement`, `RiderDebt` + policies | 21 | 17 (+3) | retries only rides with a failed attempt (no double charge) |
| Debt Recovery | `RecoveryProcess` | 26 | 26 | a "debt cleared" event dated before the recovery doesn't end it |
| Fleet Maintenance | `Bike` (broken / maintenance) | 24 | 35 | rules checked before calling Rentals; saved only after Rentals accepted |

Final: 612 tests:
- 271 domain unit tests;
- 259 Spring acceptance tests;
- 5 journeys;
- 18 architecture tests;
- 59 adapter, repository and application tests.

All 35 plans `done`, all 35 intents `delivered`.

## What the reviewer was told to look at first

1. The role-played expert answers. Every number the vision didn't give is a decision, e.g. started hours to the
   second, eligibility checked again at unlock.
2. The context-map changes.
3. The process deviations: loop off, approvals under the standing instruction, batched TDD cycles in two contexts.
4. The known follow-ups:
   - a technical failure on the first charge call isn't retried;
   - a failed withdrawal request after an incident is only logged;
   - simultaneous reservations can race.
5. The open questions left in the specs.
