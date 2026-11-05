# Bounded context discovery: VeloCity (validation of the reference context map)

- **Date:** 2026-11-05
- **Mode:** validate
- **Status:** map approved (no canvases requested)
- **Input:** `src/main/cml/VeloCity-Context-Map.cml` (trainer's reference solution, left untouched), `velocity/velocity-context-map.md`, `velocity/velocity-vision.md`
- **Context map:** proposed corrected variant `src/main/cml/VeloCity-Context-Map-validated.cml`

## Reading of the input
- 8 internal contexts (Accounts, Rentals, Pricing, Settlement, Payments, DebtRecovery, CustomerSupport, FleetMaintenance) and 2 external parties (PaymentProcessor, City); 11 relationships, all Upstream/Downstream; no Shared Kernel, Partnership or Customer/Supplier.
- The original CML is valid (10 contexts, 11 relationships, 0 errors), but the validator flags 11 `RULE` smells: 1 indirect U/D cycle, 8 Conformists, 2 downstreams with neither ACL nor CF.
- Expert correction: the reference map is "a default I drew, not a set of deliberate decisions"; every default was to be challenged.

## Findings against the rules (original map)
| # | Finding | Where | Rule | Resolution in the proposed map |
|---|---|---|---|---|
| F1 | Indirect U/D cycle Accounts -> Rentals -> Settlement -> Accounts | `Settlement [U,PL]->[D,CF] Accounts : DebtBlocking` | Relationship rule 3 | Expert: blocking is Accounts' concept. Settlement calls Accounts' "block / lift block (reason: outstanding debt)" service. Relationship flipped to `Accounts [U,OHS,PL]->[D,ACL] Settlement : AccountBlocking`. Cycle gone. |
| F2 | Pricing conforms to City, whose zone map is the city's own changing format | `City [U]->[D,CF] Pricing` | Integration rule 1 | `[D,ACL]`. Pricing translates into station / drop zone / unauthorized zone. |
| F3 | Settlement conforms to Rentals | `RentalSessionEnded` | Integration rule 1 | ACL. Settlement translates into "a ride to settle". C/S (different teams). |
| F4 | FleetMaintenance conforms to Rentals despite a by-design language difference (available/withdrawn vs broken) | `BikeAvailability` | Integration rule 1 | ACL. |
| F5 | Settlement conforms to Pricing | `PriceCalculation` | Integration rule 1 | ACL; Pricing also offers PL. |
| F6 | Accounts conforms to Settlement's debt language | `DebtBlocking` | Integration rule 1 | Disappears with F1. |
| F7 | DebtRecovery conforms to Settlement although languages differ (reminders, plans vs charge, settled) | `DebtCollection` | Integration rule 1 | ACL; C/S. |
| F8 | Rentals downstream of Accounts with neither ACL nor CF | `AccountStatus` | Integration rule 3 | ACL. Rentals has its own "this rider may rent" (account + service area). Accounts upstream gets OHS/PL. |
| F9 | CustomerSupport -> FleetMaintenance unconfirmed, no ACL | `FleetIssueIncident_Unconfirmed` | Integration rule 3; assumption marking | Confirmed by the expert; `_Unconfirmed` dropped; ACL; C/S. |
| F10 | No Customer/Supplier between internal teams, though all contexts are different teams of one company | whole map | Relationship rule 5 | C/S where one customer drives the supplier: Rentals -> Settlement, Settlement -> DebtRecovery, CustomerSupport -> FleetMaintenance. Upstreams serving several clients with a generic API (Payments, Accounts, Rentals' availability, Pricing) stay U/D with OHS/PL, because CML forbids OHS in C/S. |
| F11 | Accounts CF Payments, Settlement CF Payments | `PaymentMethodVerification`, `Charging` | Integration rule 1 | **Kept**: expert confirmed Payments' thin, generic payments vocabulary as deliberate and well established. Justified in a CML comment. |

What was fine: no Shared Kernel, no Partnership, no Big Ball of Mud, no Separate Ways, no direct cycle, OHS only upstream, ACL towards the external PaymentProcessor, external parties modelled as `SYSTEM`.

## Heuristic trail
Validate mode: the contexts came from the reference discovery session (`velocity/bounded-context-discovery-session.md`). Each was re-checked by the critic against H1-H3 with the expert's round-1 answers.

### H1: Pivotal events
| Event | Status | Before | After | Expert's answer |
|---|---|---|---|---|
| Account Activated | confirmed (reference session) | onboarding (Accounts) | renting (Rentals) | - |
| Rental Session Ended (incl. max-time and lock-failure end) | confirmed | ride (Rentals) | settlement (Settlement) | end reason travels to Settlement |
| Rental Fee Unpaid | confirmed | quick retries (Settlement) | active recovery (DebtRecovery) | one chain with settlement |

### H2: Swimlanes
| Flow | Verdict | Why |
|---|---|---|
| Debt recovery | lane | own clock (days), own loops (pause/resume/exhausted) |
| Bike fault to maintenance | lane | follows the bike, three entry points |
| Incident lifecycle | lane | any time, own loops; only its fleet-issue exit matters for the map |
| Station vs drop-zone return | variant | only the fee differs (Pricing) |

### H3: Single responsibility
| Duplicated responsibility | Where it appeared | Resolution |
|---|---|---|
| Account blocking | Accounts and Settlement (`DebtBlocking`) | Accounts owns the decision and language; Settlement reports the fact/reason |
| "Withdrawn" vs "broken" | Rentals and FleetMaintenance | different concepts in different languages; Rentals withdraws, FleetMaintenance marks broken and decides return to circulation |
| Zone classification | Rentals (return) vs Pricing (fee) | Rentals accepts every return and records the location; Pricing classifies against the City zone map |

## Bounded contexts
| Context | Question it answers | Owns | Justification | Critic |
|---|---|---|---|---|
| Accounts | Can this rider use the service? | onboarding, activation, blocks/unblocks with reason, block notification | Account Activated pivot; single owner of blocking (H3) | AGREE r1 |
| Rentals | Who has which bike, and when? | eligibility, availability, reserve/unlock, sessions, returns, locking, forced end, withdrawing on faults | Rental Session Ended pivot; preparation has no own rules | AGREE r1 |
| Pricing | What does this cost? | fee calculation, tariffs, zone classification | evolves independently; estimate and total same rules | AGREE r1 |
| Settlement | What does the rider owe, and is it settled? | charging, retries, balance, outstanding debt | far side of Rental Session Ended | AGREE r1 |
| Payments | Did the verification / charge go through? | verification, charging, processor token | purely mechanical shield | AGREE r1 |
| DebtRecovery | How do we recover what's owed? | recovery process, reminders, payment plans, pause/resume, exhausted | own clock and rules (H2, H3) | AGREE r1 |
| CustomerSupport | What went wrong for the rider, and is it resolved? | incident lifecycle | lane with own loops | AGREE r1 |
| FleetMaintenance | Is this bike in working order? | broken, maintenance, fixed, back in circulation | lane following the bike | AGREE r1 |

## Critic log
| Subject | Round | Verdict | Blocking points / resolution |
|---|---|---|---|
| Accounts | 1 | AGREE | none; note: how an invalid-card block is lifted (replace/verify payment method) |
| Rentals | 1 | AGREE | none; note: make Bike Withdrawn, Locking Failed, end reasons explicit |
| Pricing | 1 | AGREE | none; question on live estimate (non-blocking); City should be ACL |
| Settlement | 1 | AGREE | none; note: Accounts should be upstream for blocking (matches F1) |
| Payments | 1 | AGREE | none; CF justified |
| DebtRecovery | 1 | AGREE | none; questions on exhausted debt and payment plans (answered round 2) |
| CustomerSupport | 1 | AGREE | none; drop `_Unconfirmed`, add ACL |
| FleetMaintenance | 1 | AGREE | none; ACL on both inbound relationships |
| Map + Onboarding and activation | 1 | AGREE | none; question on payment-method token (answered: Payments) |
| Map + Rent and return a bike | 1 | AGREE | none; comment refinements (PriceCalculation inputs) applied |
| Map + Settle a ride and recover debt | 1 | AGREE | none; questions on exhausted recovery and payment plans (answered); comment refinements applied |
| Map + Bike fault to maintenance | 1 | AGREE | none; technician-unlock comment applied |
| Map + all processes (final pass) | 2 | AGREE | none; note on zone classification ownership (answered: Pricing) |

## Context map
### Process walkthroughs
#### 1. Onboarding and activation
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| register ... sign agreement, activated | Accounts | - |
| payment method verified (pre-authorization) | Payments (for Accounts) | PaymentMethodVerification; PaymentProcessing |
| card invalid -> Payment Method Invalidated | Payments | PaymentProcessing -> PaymentMethodVerification |
| account blocked (invalid card), notification | Accounts | - |

#### 2. Rent and return a bike
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| eligibility (account active, not blocked; service area) | Rentals | AccountStatus |
| reserve, unlock, session, returns, locking, forced end | Rentals | - |
| lock failure -> bike withdrawn | Rentals | BikeAvailability (to FleetMaintenance) |
| Rental Session Ended | Rentals | RentalSessionEnded |

#### 3. Settle a ride and recover debt
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| total fee | Pricing | PriceCalculation; ZoneMap |
| charge, retries | Settlement / Payments | Charging; PaymentProcessing |
| Rental Fee Unpaid -> block (outstanding debt) | Settlement -> Accounts | AccountBlocking |
| recovery started, reminders, plans, pause/resume | DebtRecovery | DebtCollection |
| collect (one request per instalment) | Settlement | DebtCollection; Charging |
| Outstanding Debt Cleared -> lift block, close recovery | Settlement | AccountBlocking; DebtCollection |
| recovery exhausted | DebtRecovery | - (debt stays outstanding, account stays blocked) |

#### 4. Bike fault to maintenance
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| fault detected / bike signal -> withdrawn | Rentals | BikeAvailability |
| fleet-issue incident | CustomerSupport | FleetIssueIncident |
| marked broken, withdraw request | FleetMaintenance | BikeAvailability |
| technician reserve/unlock -> Maintenance Started | FleetMaintenance via Rentals | BikeAvailability |
| fixed -> make available | FleetMaintenance | BikeAvailability |

### Relationships (proposed map)
| Upstream | Downstream | Patterns | Messages | Justification |
|---|---|---|---|---|
| PaymentProcessor | Payments | U / D,ACL | charge, pre-authorization | external; integration rule 3 |
| City | Pricing | U / D,ACL | zone map | not a stable standard (expert); integration rules 1, 3 |
| Payments | Accounts | U,OHS,PL / D,CF | verify payment method; Payment Method Invalidated | CF justified: deliberate generic payments language |
| Payments | Settlement | U,OHS,PL / D,CF | charge; succeeded/failed | same |
| Accounts | Rentals | U,OHS,PL / D,ACL | active and not blocked? | Rentals' own eligibility notion |
| Accounts | Settlement | U,OHS,PL / D,ACL | block / lift block (reason) | blocking is Accounts' language; breaks cycle |
| Rentals | Settlement | U,S,PL / D,C,ACL | Rental Session Ended | "a ride to settle"; relationship rule 5 |
| Rentals | FleetMaintenance | U,OHS,PL / D,ACL | Bike Withdrawn, Technician Unlocked; withdraw/make available, reserve/unlock | generic API for any authorised client; language differs |
| Pricing | Settlement | U,OHS,PL / D,ACL | price for session | calculator usable by anyone |
| Settlement | DebtRecovery | U,S,PL / D,C,ACL | Rental Fee Unpaid, Outstanding Debt Cleared; collect | relationship rule 5; languages differ |
| CustomerSupport | FleetMaintenance | U,S,PL / D,C,ACL | Fleet Issue Reported | confirmed; relationship rule 5 |

### Validator output (final)
```
WARNING line 50: Are you sure you need an ANTICORRUPTION LAYER here? This pattern should not be necessarily needed in a Customer-Supplier relationship.
WARNING line 68: Are you sure you need an ANTICORRUPTION LAYER here? This pattern should not be necessarily needed in a Customer-Supplier relationship.
WARNING line 73: Are you sure you need an ANTICORRUPTION LAYER here? This pattern should not be necessarily needed in a Customer-Supplier relationship.
RELATIONSHIPS:
  PaymentProcessor [] -> Payments [ACL] : PaymentProcessing
  City [] -> Pricing [ACL] : ZoneMap
  Payments [OHS, PL] -> Accounts [CF] : PaymentMethodVerification
RULE conformist-check: Accounts conforms to Payments. OK only if Payments's language is well established; otherwise it pollutes Accounts.
  Payments [OHS, PL] -> Settlement [CF] : Charging
RULE conformist-check: Settlement conforms to Payments. OK only if Payments's language is well established; otherwise it pollutes Settlement.
  Accounts [OHS, PL] -> Rentals [ACL] : AccountStatus
  Accounts [OHS, PL] -> Settlement [ACL] : AccountBlocking
  Rentals [PL] -> Settlement [ACL] (customer/supplier) : RentalSessionEnded
  Rentals [OHS, PL] -> FleetMaintenance [ACL] : BikeAvailability
  Pricing [OHS, PL] -> Settlement [ACL] : PriceCalculation
  Settlement [PL] -> DebtRecovery [ACL] (customer/supplier) : DebtCollection
  CustomerSupport [PL] -> FleetMaintenance [ACL] (customer/supplier) : FleetIssueIncident
VALID: 10 bounded contexts, 11 relationships, 0 errors
```
How each line was resolved:
- `conformist-check` Accounts -> Payments and Settlement -> Payments: justified. The expert confirmed Payments' thin, generic vocabulary ("verified or not", "charge succeeded or failed") as a deliberate, well-established language (integration rule 1); the CML comment states it.
- Context Mapper `WARNING` "ACL in a Customer-Supplier relationship" (3x: RentalSessionEnded, DebtCollection, FleetIssueIncident): kept on purpose. The trainer's rules make Customer/Supplier the default flavour between internal teams (relationship rule 5) and an ACL the default downstream (integration rule 3). Context Mapper assumes a supplier that accommodates its customer removes the need for translation, but here the expert explicitly wants each downstream to keep its own language (a ride to settle; broken vs incident; recovery vs charge). Being a customer influences the supplier's planning; it does not make the models identical.
- Original map's `ud-cycle-indirect` and both `no-acl` lines: no longer reported.

## Q&A with the domain expert
Verbatim in the session transcript. Summary:
- Round 1: four processes (P3+P4 merged; P6 internal to Customer Support); blocking is Accounts' language (option b); separate teams, one company; only the Payments CFs deliberate; City zone map not a standard (ACL); Rentals translates account status (ACL); fleet-issue route confirmed, Rentals-detected faults never open incidents; new file name approved.
- Round 2: map approved. Exhausted recovery: nothing outside Debt Recovery; payment plans owned by Debt Recovery, Settlement sees one collect per instalment; service area is VeloCity's own, Pricing classifies return location against the City zone map; Payments keeps the payment-method token.

## Open questions and assumptions
- No unconfirmed assumptions remain in the proposed CML.
- Deferred (out of scope, recorded in CML comments): billing disputes pausing Debt Recovery; write-off / external collection after exhausted recovery.
- Not yet discussed: whether the live fee estimate during a ride is shown to riders (would give Pricing a second consumer, no arrow reversed).

## Suggested board changes
- Make explicit: Bike Withdrawn (with reason), Locking Failed, Rental Session Ended with end reason (normal, max time, lock failure).
- Add: Fleet Issue Reported, Bike Reported Broken, Maintenance Started, Bike Fixed, Bike Returned to Circulation.
- Add: Ride Settled, Charge Succeeded / Charge Failed.
- Add: Debt Recovery Started / Paused / Resumed / Exhausted, Payment Plan Agreed.
- Add: Account Unblocked (invalid card) after a new payment method is verified.
- Rename "... Hour Fee Applied" to "Fee Estimated".

## Suggestions for the reference material (not applied)
- If the trainer adopts the proposed map, keep `velocity/velocity-context-map.md` (diagram, "No cycles" decision, "Debt Recovery -> Accounts" alternative) and `VeloCity-Context-Map.cml` in sync, as the VeloCity CLAUDE.md requires. The md's "No cycles" check only looked at direct pairs.

## Next steps
- Canvases: not requested.
