# Bounded Context Canvas: Payouts

<!--
Pre-filled by discover-bounded-contexts on 2026-11-05 (report: docs/context-discovery/tutorhub/2026-11-05-event-storming/discovery.md;
map: src/main/cml/TutorHub-Context-Map.cml). Based on the ddd-crew Bounded Context Canvas v5.
Status: draft for review. No plan or code yet. bounded-context-scaffolder builds code only from rules written as strict Given/When/Then.
Rule references (R<round>-<answer>) point to the Q&A in the discovery report.
-->

## Name
Payouts

## Purpose
Answers: **What do we owe each tutor, and what do we pay them this Monday?** Holds each tutor's balance and pays it out every Monday through Stripe Connect, on its own clock independent of lessons (R1-5).

## Strategic Classification
- Domain: not discussed yet (open question)
- Business model: not discussed yet
- Evolution: not discussed yet

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md; suggested by discovery, to confirm -->
- Execution Model: performs the weekly payout run.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Billing | Event | Tutor Earnings Credited / Tutor Earnings Reversed | Billing [U,S,PL]->[D,C,ACL] Payouts : TutorEarnings |
| Tutor (app) | Command | Set up Stripe Connect payout account; upload tax forms |  |
| Stripe | API | Connect onboarding (country), payout results | Stripe [U,OHS,PL]->[D,CF] Payouts : ConnectPayouts (Conformist, R6-5) |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Send payout | Command | Stripe (Connect) | no: Conformist by decision (R6-5) |

## Ubiquitous Language
- **Tutor balance**: what TutorHub owes the tutor: credited earnings minus reversals minus payouts
- **Payout**: the Monday transfer of the balance
- **Minimum payout**: 20 EUR; smaller balances wait
- **Clawback**: a reversal taken off the next payout
- **Tax forms**: documents some countries require before a tutor can be paid; checked by Payouts

## Business Decisions
<!-- In the expert's words from discovery. None is a strict Given/When/Then yet: each needs a concrete example before bounded-context-scaffolder can delegate it. -->
### Weekly payout
- Expert: "Every Monday they pay out each tutor's accumulated earnings through Stripe Connect." (R1-5)
- Status: **needs Given/When/Then** (concrete; write GWT)

### Minimum
- Expert: "The minimum payout is 20 EUR." (R1-5)
- Status: **needs Given/When/Then** (concrete; write GWT with 18 EUR / 20 EUR)

### Retry
- Expert: "A failed payout is retried the next Monday." (R1-5)
- Status: **needs Given/When/Then** (concrete; write GWT)

### Tax forms
- Expert: "Tutors in some countries have to send in tax forms before they can be paid." (R1-5); "The Payouts team checks the forms" (R8-2)
- Status: **needs Given/When/Then** (vague: which countries? needs GWT)

### Suspended tutors are paid
- Expert: "a suspended tutor still gets their Monday payout, whatever the reason" (R8-3)
- Status: **needs Given/When/Then** (write GWT)

### Clawback
- Expert: refund minus commission "is taken off their next payout" (R4-4)
- Status: **needs Given/When/Then** (write GWT)

## Assumptions
- none recorded

## Verification Metrics
- Tutors asking "why isn't my money paid?" get the answer from Payouts alone.

## Open Questions
- What happens when a clawback is larger than the next payout (negative balance, limit, write-off)?
- Which countries require tax forms?
- Strategic classification.
