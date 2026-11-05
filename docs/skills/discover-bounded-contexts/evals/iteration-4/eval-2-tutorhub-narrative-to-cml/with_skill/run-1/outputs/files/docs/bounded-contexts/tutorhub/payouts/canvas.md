# Bounded Context Canvas: Payouts

<!-- Pre-filled by discover-bounded-contexts (2026-11-05) via bounded-context-scaffolder, canvases only: no plan and no code yet. Source: docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml. Items marked (to re-confirm) were answered on the spot by the expert. -->

## Name
Payouts

## Purpose
Answers: **how much do we owe each tutor, and when is it paid out?** The Payouts team keeps each tutor's earnings balance and pays it out every Monday through Stripe Connect, on a clock that is independent of lessons ("Payouts happen every Monday, no matter what happens with lessons").

## Strategic Classification
- Domain: not discussed yet (open question)
- Business model: not discussed yet
- Evolution: not discussed yet

## Domain Roles
- Execution Model (proposed): performs the weekly payout run.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Billing | Event | Lesson Earnings Recorded / Lesson Earnings Reversed | Credits and clawbacks |
| TrustAndSafety | Event | Tutor Approved (with country) | For the tax-form rule (to re-confirm) |
| Tutor | Command | Submit tax forms / Set up payout account (Stripe Connect onboarding) | |
| Stripe | Event | Payout succeeded / failed | Through ACL |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Pay out | Command | Stripe (Connect) | yes |
| Weekly Payout Sent / Payout Failed | Event | (tutor notification) | n/a |

## Ubiquitous Language
- **Earnings balance**: the running total TutorHub owes a tutor.
- **Weekly payout**: Monday's payment of the balance.
- **Minimum payout**: 20 EUR.
- **Payout failure**: the money stays on the balance, the payout is retried next Monday, and the tutor is told to fix their payout details.
- **Clawback**: earnings taken back after a refund, from the balance or the next payout (to re-confirm).
- **Tax form**: required before payouts for tutors in some countries.

## Business Decisions
### Minimum payout
- **Given** a tutor with a balance of 19 EUR on Monday, **When** the weekly payout runs, **Then** no payout is sent and the balance stays at 19 EUR.
### Normal payout
- **Given** a tutor with a balance of 120 EUR and payout details set up, **When** the weekly payout runs, **Then** Weekly Payout Sent of 120 EUR and the balance becomes 0.
### Failed payout
- **Given** a payout of 120 EUR that Stripe reports as failed, **When** the failure arrives, **Then** Payout Failed, the balance is back to 120 EUR, and it's retried next Monday.
### Tax forms (needs GWT)
- Which countries need tax forms, and does a missing form skip the payout?
### Clawback (needs GWT, to re-confirm)
- What happens when a clawback is larger than the next payout: can the balance go negative?

## Assumptions
- A suspension doesn't affect credited earnings or payouts (to re-confirm).

## Verification Metrics
- Lesson or booking changes never require Payouts changes.

## Open Questions
- Is the balance allowed to go negative and be carried over?
