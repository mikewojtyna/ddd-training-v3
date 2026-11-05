# Bounded Context Canvas: Payouts

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values.

PRE-FILLED by Claude (bounded-context-scaffolder, canvas only, no plan or code) from the TutorHub discovery session on 2026-11-05:
docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml.
Please review before any plan or code is generated. System package for later scaffolding: tutorhub (new system).
-->

## Name
Payouts

<!-- Matches `BoundedContext Payouts` in the CML. -->

## Purpose
Answers: **how much do we owe each tutor, and has it been paid out?** Payouts keeps each tutor's running balance and pays it out every Monday through Stripe Connect, "no matter what happens with lessons", while respecting minimums, tax forms and suspensions.

## Strategic Classification
- Domain: supporting (assumption)
- Business model: cost reduction / compliance (assumption)
- Evolution: custom built (on top of Stripe Connect)

## Domain Roles
- Execution Model: performs the weekly payout run.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Billing | Event | Lesson Earnings Determined, Refund Issued (tutor's share) | `LessonEarnings`, our ACL |
| Tutor Vetting | Event | Tutor Suspended, Tutor Reinstated | `TutorSuspension`, our ACL |
| Tutor (app) | Command | Submit tax form | Payouts collects tax forms itself |
| (clock) | Event | Monday | Weekly payout run |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Pay out balance | Command | Stripe (`ConnectPayouts`) | No: Conformist, industry-standard API (expert) |
| Weekly Payout Sent, Payout Withheld, Payout Failed, Payout Retried | Event | (none subscribed yet) | n/a |

## Ubiquitous Language
- **Balance**: what TutorHub owes a tutor now; can go negative after refunds.
- **Weekly payout**: Monday's transfer of the balance via Stripe Connect.
- **Minimum payout**: 20 EUR.
- **Withheld payout**: not paid this Monday (below minimum, missing tax form, tutor suspended).
- **Tax form**: required before payout for tutors in some countries.

## Business Decisions
### Pay the balance on Monday
- **Given** tutor X with a balance of 120 EUR, tax forms not required, not suspended
- **When** the Monday run starts
- **Then** Weekly Payout Sent (120 EUR) is recorded and the balance becomes 0

### Minimum 20 EUR
- **Given** tutor X with a balance of 15 EUR
- **When** the Monday run starts
- **Then** Payout Withheld (below minimum) is recorded and the balance stays 15 EUR

### Tax forms first
- **Given** tutor X in a country that requires tax forms, with no form on file and a balance of 100 EUR
- **When** the Monday run starts
- **Then** Payout Withheld (tax form missing) is recorded

### Held while suspended
- **Given** suspended tutor X with a balance of 100 EUR
- **When** the Monday run starts
- **Then** Payout Withheld (suspended) is recorded

### Failed payout retried next Monday
- **Given** a payout of 80 EUR that failed on Monday 3 March
- **When** the run on Monday 10 March starts
- **Then** the payout is retried (Payout Retried), not before

### Refund after payout makes the balance negative
- **Given** tutor X with a balance of 10 EUR after last Monday's payout
- **When** Refund Issued with a tutor share of 40 EUR is received
- **Then** the balance becomes -30 EUR and future earnings pay it back

## Assumptions
- Payouts learns a tutor's country and Stripe Connect account itself (not confirmed).

## Verification Metrics
- Every Monday, every eligible tutor is paid exactly once.

## Open Questions
- Which countries require tax forms, and who maintains that list?
- Where do the tutor's country and Stripe Connect account come from (Payouts or Tutor Vetting)?
- What happens to a negative balance if the tutor leaves?
