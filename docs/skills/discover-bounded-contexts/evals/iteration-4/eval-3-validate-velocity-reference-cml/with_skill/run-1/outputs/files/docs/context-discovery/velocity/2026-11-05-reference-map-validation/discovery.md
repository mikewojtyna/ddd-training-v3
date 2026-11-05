# Bounded context discovery: VeloCity (reference map validation)

- **Date:** 2026-11-05
- **Mode:** validate
- **Status:** in progress: proposed map awaiting the trainer's approval (all critics AGREE)
- **Input:** `src/main/cml/VeloCity-Context-Map.cml` (trainer's reference solution, **unchanged**), `velocity/velocity-context-map.md`, `velocity/velocity-vision.md`
- **Context map:** proposal in `src/main/cml/VeloCity-Context-Map-validation.cml` (new file; the reference CML was not touched)

## Reading of the input
8 internal contexts (Accounts, Rentals, Pricing, Settlement, Payments, DebtRecovery, CustomerSupport, FleetMaintenance), 2 external parties (PaymentProcessor, City), 11 relationships. One relationship (CustomerSupport -> FleetMaintenance) marked unconfirmed. Processes rebuilt from the vision and confirmed through the expert's answers: P1 onboarding, P2 rent a bike, P3 return and settle, P4 invalid card block, P5 debt recovery, P6 broken bike maintenance, P7 incident handling (incl. billing disputes).

## Findings against the reference CML

| # | Finding | Rule | Evidence | Resolution in the proposal |
|---|---|---|---|---|
| 1 | Indirect U/D cycle Accounts -> Rentals -> Settlement -> Accounts. `velocity-context-map.md` claims "No cycles". | Relationship rule 3; validator `ud-cycle-indirect` | Rentals needs "is account active?"; Settlement needs Rental Session Ended; Accounts reacted to Settlement's DebtBlocking events | Expert chose option A (R2 Q1): Accounts offers a place/lift-block-with-reason service; Settlement calls it. `Accounts -> Settlement : AccountBlocking` replaces `Settlement -> Accounts : DebtBlocking`. Map is acyclic. |
| 2 | 7 internal Conformists without a well-established upstream language (Payments->Accounts, Payments->Settlement, Rentals->Settlement, Rentals->FleetMaintenance, Pricing->Settlement, Settlement->Accounts, Settlement->DebtRecovery) | Integration rules 1 and 3 | All teams internal, supplier plans for customer (R1 Q2) | Downstream ACL instead of CF |
| 3 | No Customer/Supplier anywhere | Relationship rule 5 | R1 Q2 | All internal U/D relationships are C/S |
| 4 | Accounts -> Rentals has no pattern on either side | Validator `no-acl`, integration rule 3 | | C/S with PL, Rentals ACL |
| 5 | CustomerSupport -> FleetMaintenance unconfirmed **and drawn the wrong way**: the agent sends a "report bike broken" request into Maintenance's model | Direction follows model, not message; breaking-a-cycle step 1 | R1 Q4, R2 Q2 | Confirmed and reversed: `FleetMaintenance -> CustomerSupport : BrokenBikeReport` |
| 6 | Billing disputes have no relationships: the agent pauses/resumes recovery and asks for a charge correction / refund | Cross-cutting: every hand-off needs a relationship | R1 Q5, R2 Q3, R3 Q2 | `DebtRecovery -> CustomerSupport : RecoveryPause`, `Settlement -> CustomerSupport : ChargeCorrection`; refunds ride on `Payments -> Settlement : Charging` |
| 7 | P2 "location validated" (service area) has no owner and no relationship; City is linked only to Pricing | Cross-cutting: every step owned | R3 Q1 | Rentals owns the location check; `City [U,PL]->[D,CF] Rentals : ServiceArea` |
| 8 | "Charging" listed as a responsibility of both Payments and Settlement | H3 (one owner per responsibility) | Vision: "a charge either succeeds or it fails"; Payments has no rules | Settlement decides charges/retries/corrections/refunds; Payments executes ("Charge and refund execution") |

Kept as is (no finding): City -> Pricing CF (R1 Q3: city publishes one format for every operator, used as is); PaymentProcessor -> Payments ACL; the 8 contexts themselves (the three flagged contexts passed their critics).

Minor, house style: `domainVisionStatement`s do not state the context's question. The proposal adds "Answers: ..." to each.

## Heuristic trail (validation only)
### H1: Pivotal events
| Event | Status | Before | After | Expert's answer |
|---|---|---|---|---|
| Account Activated | confirmed (reference) | onboarding (Accounts) | renting (Rentals) | not re-asked |
| Rental Session Ended | confirmed (reference) | ride (Rentals) | settlement (Settlement) | not re-asked |
| Rental Fee Unpaid / outstanding debt recorded | confirmed | Settlement's retries | debt block (Accounts) + recovery (DebtRecovery) | R1 Q1: "The debt is decided on the money side" |

### H2: Swimlanes
| Flow | Verdict | Why |
|---|---|---|
| Debt recovery vs quick retries | lane | Longer clock, own pause/resume loop (vision; R1 Q1) |
| Invalid-card block vs debt block | variants (reasons) of one block responsibility | R2 Q1: account side keeps reason; same status |

### H3: Single responsibility
| Duplicated responsibility | Where it appeared | Resolution |
|---|---|---|
| Account blocking for debt | Accounts "owns blocks" vs doc: "Settlement drives both block and unblock for debt" | Accounts owns block, reason, notification; Settlement owns the debt fact and asks (R2 Q1) |
| Charging | Payments "Charging" and Settlement "Charging finished rides" | Settlement decides, Payments executes |

## Bounded contexts
Unchanged set; questions as in the proposed CML.
| Context | Question it answers | Owns | Justification |
|---|---|---|---|
| Accounts | Can this rider use the service? | onboarding, activation, blocks per reason, block notification | H1 Account Activated; H3 single block owner (critic AGREE) |
| Rentals | Who has which bike, and when? | availability, location check, reserve/unlock, sessions, returns, max time, fault withdrawal | H1 pivots; R3 Q1 |
| Pricing | What does this ride cost? | fee calculation, tariffs | reference (changes independently) |
| Settlement | What does the rider owe, and is it settled? | charge decisions, retries, balance, outstanding debt, corrections, refunds | H1 empty side of Rental Session Ended; critic AGREE |
| Payments | Did the verification, charge or refund go through? | execution against the processor | H3 mechanical, two consumers; critic AGREE |
| DebtRecovery | How do we recover what's owed? | reminders, payment plans, pause/resume | H2 lane |
| CustomerSupport | What went wrong for the rider, and is it resolved? | incident lifecycle | H2 lane |
| FleetMaintenance | Is this bike in working order? | broken bikes, maintenance, return to circulation | H2 lane |

## Critic log
| Subject | Round | Verdict | Blocking points / resolution |
|---|---|---|---|
| Accounts | 1 | AGREE | Note: track blocks per reason so lifting a debt block doesn't lift an invalid-card block |
| Settlement | 1 | AGREE | Non-blocking question on refunds -> asked (R3 Q2), refunds added |
| Payments | 1 | AGREE | Note: name the responsibility "charge execution" -> applied |
| Map, all processes P1-P7 | 1 | DISAGREE | P2 location check unowned -> asked expert (R3 Q1), City -> Rentals ServiceArea added |
| Map, all processes P1-P7 | 2 | AGREE | Wording notes (refund in key language) applied |

Context critics ran only for flagged contexts (Accounts, Settlement, Payments). Rentals, Pricing, DebtRecovery, CustomerSupport and FleetMaintenance had no flag.

## Context map
### Process walkthroughs
#### P2 Rent a bike
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Location validated (service area, near bike) | Rentals | City -> Rentals ServiceArea |
| Account active? | Accounts | Accounts -> Rentals AccountStatus |
| Reserve, unlock, session started | Rentals | - |

#### P3 Return and settle / P5 Debt recovery
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Return, lock / lock failure / forced end, Rental Session Ended | Rentals | Rentals -> Settlement RentalSessionEnded |
| Price | Pricing (zone map from City) | Pricing -> Settlement PriceCalculation; City -> Pricing ZoneMap |
| Charge, retries | Settlement / Payments | Payments -> Settlement Charging; PaymentProcessor -> Payments |
| Outstanding debt -> block (reason debt), notification | Settlement asks, Accounts blocks | Accounts -> Settlement AccountBlocking |
| Recovery, attempt collection | DebtRecovery | Settlement -> DebtRecovery DebtCollection |
| Balance zero -> Outstanding Debt Cleared -> lift block, recovery ends | Settlement | AccountBlocking, DebtCollection |

#### P7 Incidents
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Fleet issue accepted -> report bike broken | CustomerSupport -> FleetMaintenance | FleetMaintenance -> CustomerSupport BrokenBikeReport |
| Billing dispute -> pause / resume recovery | DebtRecovery | DebtRecovery -> CustomerSupport RecoveryPause |
| Correct / cancel charge, refund, confirmation with amount | Settlement (Payments executes refund) | Settlement -> CustomerSupport ChargeCorrection; Payments -> Settlement Charging |

P1 (PaymentMethodVerification), P4 (Payment Method Invalidated via PaymentMethodVerification) and P6 (Rentals -> FleetMaintenance BikeAvailability) walk through unchanged.

### Relationships (proposal)
| Upstream | Downstream | Patterns | Messages | Justification |
|---|---|---|---|---|
| PaymentProcessor | Payments | U / ACL | verify, charge, refund | external |
| City | Pricing | U,PL / CF | zone map | CF justified, R1 Q3 |
| City | Rentals | U,PL / CF | service area | CF justified, R3 Q1 |
| Payments | Accounts | S,PL / C,ACL | verify; Payment Method Invalidated | C/S rule 5 |
| Payments | Settlement | S,PL / C,ACL | charge, refund, succeeded/failed | C/S rule 5 |
| Accounts | Rentals | S,PL / C,ACL | is account active? | R2 Q1 |
| Accounts | Settlement | S,PL / C,ACL | place / lift block (reason debt) | breaks cycle, R2 Q1 |
| Rentals | Settlement | S,PL / C,ACL | Rental Session Ended | |
| Rentals | FleetMaintenance | S,PL / C,ACL | fault, withdraw/make available, reserve/unlock | |
| Pricing | Settlement | S,PL / C,ACL | price for session | |
| Settlement | DebtRecovery | S,PL / C,ACL | Rental Fee Unpaid, attempt collection, Debt Cleared | |
| Settlement | CustomerSupport | S,PL / C,ACL | correct / cancel / refund + confirmation | R2 Q3, R3 Q2 |
| DebtRecovery | CustomerSupport | S,PL / C,ACL | pause / resume | R1 Q5 |
| FleetMaintenance | CustomerSupport | S,PL / C,ACL | report bike broken | R1 Q4, R2 Q2 |

OHS is dropped from C/S relationships only because Context Mapper rejects OHS in C/S; the CML header comment says which suppliers are OHS in substance.

### Validator output
Reference CML (`src/main/cml/VeloCity-Context-Map.cml`): VALID, 10 contexts, 11 relationships. RULE lines: conformist-check x8 (7 internal = finding 2, City kept), no-acl Accounts->Rentals (finding 4), no-acl CustomerSupport->FleetMaintenance (finding 5), ud-cycle-indirect Accounts -> Rentals -> Settlement -> Accounts (finding 1).

Proposal, final run:
```
RELATIONSHIPS:
  PaymentProcessor [] -> Payments [ACL] : PaymentProcessing
  City [PL] -> Pricing [CF] : ZoneMap
RULE conformist-check: Pricing conforms to City. OK only if City's language is well established; otherwise it pollutes Pricing.
  City [PL] -> Rentals [CF] : ServiceArea
RULE conformist-check: Rentals conforms to City. OK only if City's language is well established; otherwise it pollutes Rentals.
  Payments [PL] -> Accounts [ACL] (customer/supplier) : PaymentMethodVerification
  Payments [PL] -> Settlement [ACL] (customer/supplier) : Charging
  Accounts [PL] -> Rentals [ACL] (customer/supplier) : AccountStatus
  Accounts [PL] -> Settlement [ACL] (customer/supplier) : AccountBlocking
  Rentals [PL] -> Settlement [ACL] (customer/supplier) : RentalSessionEnded
  Rentals [PL] -> FleetMaintenance [ACL] (customer/supplier) : BikeAvailability
  Pricing [PL] -> Settlement [ACL] (customer/supplier) : PriceCalculation
  Settlement [PL] -> DebtRecovery [ACL] (customer/supplier) : DebtCollection
  Settlement [PL] -> CustomerSupport [ACL] (customer/supplier) : ChargeCorrection
  DebtRecovery [PL] -> CustomerSupport [ACL] (customer/supplier) : RecoveryPause
  FleetMaintenance [PL] -> CustomerSupport [ACL] (customer/supplier) : BrokenBikeReport
VALID: 10 bounded contexts, 14 relationships, 0 errors
```
Both conformist-check lines are justified (R1 Q3, R3 Q1). There are also 11 Context Mapper warnings ("ANTICORRUPTION LAYER ... not necessarily needed in a Customer-Supplier relationship"), kept on purpose under the trainer's integration rule 3.

## Q&A with the domain expert
Verbatim in the session transcript. Summary:
- R1: debt is decided by the money side and the block follows (Q1). All teams are internal and the supplier plans for the customer (Q2). The city zone map is one format for every operator, used as is (Q3). The agent reports a broken bike and technicians take over (Q4). Disputes pause recovery and correct or cancel the charge (Q5).
- R2: option A for blocks, B and C rejected (Q1). The agent reports only to the technicians' side (Q2). The agent asks the balance keepers to correct the charge and needs a confirmation with the new amount (Q3).
- R3: the location check is part of reserving and uses the service area from the city map (Q1). For an already paid ride, the balance keepers refund it to the card through the processor, and the agent needs a confirmation with the amount (Q2).

## Open questions and assumptions
- None blocking. No unconfirmed relationship remains in the proposal.
- Not yet asked: what lifts an invalid-card block? Probably a new payment method verified inside Accounts + Payments, so it wouldn't move a boundary.
- Future link, out of scope for P1-P7: the zone map shapes "long-term fleet distribution" (vision), so a possible City -> FleetMaintenance relationship.
- If the proposal is accepted, `velocity/velocity-context-map.md` (diagrams, "Key Design Decisions": "Settlement drives both block and unblock for debt", "Deferred / Open") and `VeloCity-Bounded-Contexts.cml` need the same changes. Its "Accepted Alternatives" entry "Debt Recovery -> Accounts for unblocking" contradicts R1 Q1.

## Suggested board changes
- Add Outstanding Debt Recorded, Charge Corrected, Refund Issued.
- Add Debt Block Requested / Debt Block Lifted, and Invalid-Card Block Lifted.

## Next steps
- Trainer approves the proposal, or picks the findings to apply to the reference files.
- Canvases via bounded-context-scaffolder: not requested, so not produced in this validation.
