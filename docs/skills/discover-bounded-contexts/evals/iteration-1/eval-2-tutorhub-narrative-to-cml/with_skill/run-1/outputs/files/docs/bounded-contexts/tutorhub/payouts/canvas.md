# Bounded Context Canvas: Payouts

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values.

PRE-FILLED by Claude (discover-bounded-contexts -> bounded-context-scaffolder, canvas only) on 2026-11-05 from
docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md and src/main/cml/TutorHub-Context-Map.cml.
Status: draft for review by the domain expert. No plan and no code yet.
Rules marked "(needs Given/When/Then)" are in the expert's words and not precise enough to test yet.
-->

## Name
Payouts

<!-- Matches `BoundedContext Payouts` in src/main/cml/TutorHub-Context-Map.cml. -->

## Purpose
Pays tutors what they've earned, on its own weekly clock independent of lessons: keeps each tutor's earnings balance, pays it out every Monday via Stripe Connect, and handles minimums, tax forms, failures and negative balances.

The one question this context answers: **When and how much do we pay each tutor?**

## Strategic Classification
- Domain: supporting ("just has to work")
- Business model: engagement (tutors get paid reliably)
- Evolution: custom built (on Stripe Connect)

## Domain Roles
<!-- Traits from docs/model-traits-worksheet.md -->
- Execution Model: performs the weekly payout run and tracks the balance

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Billing | Event | Lesson Earnings Recorded / Reversed | LessonEarnings |
| TutorTrustAndSafety | Event/Query | Tutor country | TutorCountry -> who needs tax forms |
| Tutor (app) | Command | Set up payout account / Submit tax forms |  |
| Stripe | Event | Payout paid / failed | Conformist (ConnectPayouts) |
| (clock) | Event | Monday | weekly payout run |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Weekly Payout Sent | Event | (tutor notification) | n/a |
| Payout Failed | Event | (tutor notification) | n/a |

## Ubiquitous Language
- **Earnings balance**: the sum of the tutor's recorded earnings minus reversals and payouts; may be negative
- **Weekly payout**: the Monday transfer of the balance via Stripe Connect
- **Minimum payout**: 20 EUR; smaller balances wait
- **Tax forms**: required before payout for tutors in some countries
- **Payout failed**: a transfer Stripe rejected; retried next Monday

## Business Decisions
<!-- One rule per block. Concrete values, please. -->
### Weekly payout
- **Given** Given tutor T with a balance of 120 EUR and tax forms not required
- **When** it's Monday
- **Then** Weekly Payout Sent for 120 EUR and the balance is 0

### Minimum payout
- **Given** Given tutor T with a balance of 15 EUR
- **When** it's Monday
- **Then** no payout is sent; the balance stays 15 EUR

### Retry failed payout
- **Given** Given a payout of 120 EUR failed on Monday 2026-11-09
- **When** it's Monday 2026-11-16
- **Then** the payout is retried

### Negative balance carried forward
- **Given** Given tutor T with balance 0 EUR after Monday's payout
- **When** Lesson Earnings Reversed for 34 EUR
- **Then** the balance is -34 EUR and is deducted from the next payout

### Tax forms
(needs Given/When/Then) tutors in some countries need tax forms before a payout (needs the list of countries and the exact rule).

### Suspended tutors are paid
(needs Given/When/Then) suspension stops new lessons, not money already earned (expert round 4).

## Assumptions
- Payouts collects tax forms and sets up the Stripe Connect account (expert round 6).

## Verification Metrics
- Share of tutors paid on the Monday their balance first reached 20 EUR.

## Open Questions
- Which countries need tax forms?
- Is the tutor notified of a failed payout ("probably an email")?
- Missing events: Payout Skipped (below minimum), Payout Held (tax form missing).
