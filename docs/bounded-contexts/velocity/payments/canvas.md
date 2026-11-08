# Bounded Context Canvas: Payments

<!--
Written by Claude on 2026-11-08 from docs/domains/velocity/velocity-vision.md ("Payment Processors", onboarding
steps 5-6, "Settling the Ride", "Account Blocks"), docs/domains/velocity/velocity-context-map.md and the CML, during
an autonomous run. Values from role-played domain-expert answers (DE-n).
-->

## Name
Payments

## Purpose
Shields VeloCity from the external payment processor: verifies a rider's card with a small pre-authorization,
charges amounts to the rider's current card, and tells VeloCity when a card stops being usable. Inside VeloCity a
card is simply *verified* or not, and a charge simply *succeeds* or *fails*.

## Strategic Classification
- Domain: generic
- Business model: compliance
- Evolution: product

## Domain Roles
- Gateway Interchange: translates between VeloCity and the processor's model.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Accounts | Command | Verify Payment Method (rider, card token) | OHS/PL |
| Settlement | Command | Charge (rider, amount, reference) | OHS/PL; charges the rider's current verified card |
| Payment Processor | Event | Card Invalidated (card token, reason) | Processor notification (DE-72), through the processor ACL |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Pre-authorize, Charge | Command | Payment Processor (external) | yes (`paymentprocessoracl`) |
| Payment Method Invalidated (rider, payment method, reason) | Event | Accounts | n/a (PL); only for the rider's current card (DE-73) |
| Payment Method Verified, Charge Succeeded, Charge Failed | Event | none outside Payments yet | n/a |

## Ubiquitous Language
- **Payment method**: a rider's card, known by the processor's card token.
- **Pre-authorization**: holding PLN 1.00 on the card to prove it works (DE-70), released at once.
- **Verified**: the pre-authorization succeeded; the newest verified card is the rider's *current* card (DE-5).
- **Invalidated**: the processor told us the card is no longer usable (expired, cancelled, reported lost).
- **Charge**: taking an amount from the rider's current card; it *succeeds* or *fails*.

## Business Decisions

### A card that passes pre-authorization is verified and becomes current
- **Given** rider Anna has no payment method
- **When** Anna's card tok_visa_4242 is verified and the processor accepts the PLN 1.00 pre-authorization
- **Then** the payment method is verified and is Anna's current card (Payment Method Verified)

### A newly verified card replaces the current one
- **Given** Anna's current card is tok_visa_4242
- **When** Anna's card tok_mc_5555 is verified successfully
- **Then** tok_mc_5555 is Anna's current card and tok_visa_4242 is no longer charged (DE-5)

### A declined pre-authorization leaves the card unverified
- **Given** rider Anna has no payment method
- **When** Anna's card tok_declined_4000 is verified and the processor declines it
- **Then** the verification fails and Anna still has no current card

### A charge goes to the current card
- **Given** Anna's current card is tok_visa_4242
- **When** PLN 5.00 is charged for ride S-1 and the processor accepts it
- **Then** the charge succeeded (Charge Succeeded)

### A charge without a current card fails
- **Given** Anna's only card was invalidated
- **When** PLN 5.00 is charged for ride S-1
- **Then** the charge failed with reason *no valid payment method*, and the processor isn't called (DE-71)

### An invalidated card is reported to VeloCity
- **Given** Anna's current card is tok_visa_4242
- **When** the processor reports tok_visa_4242 expired
- **Then** the payment method is invalidated (Payment Method Invalidated) and Anna has no current card

### An old card being invalidated isn't reported
- **Given** Anna's current card is tok_mc_5555 and her earlier card tok_visa_4242 was replaced
- **When** the processor reports tok_visa_4242 expired
- **Then** tok_visa_4242 is invalidated, but Payment Method Invalidated isn't published and Anna keeps her current card (DE-73)

## Assumptions
- 🎭 DE-70: PLN 1.00 pre-authorization.
- Until a real processor is connected, a simulated processor declines cards whose token starts with `tok_declined` and fails charges on tokens starting with `tok_nofunds`.

## Verification Metrics
- Share of declined pre-authorizations.
- Share of failed charges.

## Open Questions
- Refunds (after billing disputes) are not covered.
