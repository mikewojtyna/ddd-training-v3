---
name: finance-blocks-rider-with-invalid-card
sequence: 20
title: Finance wants a rider whose card became invalid blocked from new rentals
system: velocity
actor: Finance
activity: Stay in good standing
task: Block a rider whose card is invalid
status: planned
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "Account Blocks & Debt Recovery", "Payment Processors", "Key Business Rules to Know"; docs/bounded-contexts/velocity/domain-expert-answers.md DE-5, DE-6, DE-7, DE-71, DE-72, DE-73; autonomous run 2026-11-08
---

# Finance wants a rider whose card became invalid blocked from new rentals

## Intent

As a **Finance** team, I want to **block a rider whose current card is no longer usable from new rentals until they add a card that works** so that **no new ride starts that we already know we can't charge**.

## Why

The vision names an invalid card as the first of two reasons to block an account: "The payment method on file is no longer usable (expired, canceled, reported lost)" (vision "Account Blocks & Debt Recovery"). "Every ride is eventually settled" and "A blocked account cannot start new rentals. Blocks are lifted only when the reason behind them is cleared" (vision "Key Business Rules to Know"). A rider without a usable card would ride, fail to be charged and fall into outstanding debt (DE-71); blocking them first protects both VeloCity's revenue and the rider. The domain expert decided that the payment processor tells VeloCity when a card becomes invalid (DE-72), that only the rider's current card matters (DE-73), that the rider is notified of every block (DE-7), and that the block is lifted when a new card is verified, independently of a debt block (DE-6).

## Business outcome

- Payment Method Invalidated: the rider's current card is no longer usable, and the rider has no card VeloCity can charge.
- Account Blocked: the rider is blocked for the reason *invalid card*, can't start new rentals and has been notified.
- Account Unblocked: the rider added a new card that was verified, and the *invalid card* block is lifted.

## Success signal

- (suggested) No ride starts for a rider whose current card was reported invalid.
- (suggested) Share of riders blocked for an invalid card who add a new card within a week.

## Business rules mentioned

- "Invalid card. The payment method on file is no longer usable (expired, canceled, reported lost)" (vision "Account Blocks & Debt Recovery").
- "Whenever an account is blocked, a block notification is sent to the rider so they know what happened and what to do next" (vision); one notification per block reason (DE-7).
- "The rider stays blocked until the underlying reason is cleared" (vision).
- "When the rider adds a new card and it is verified [the card block is lifted]. ... The two blocks are independent: one being lifted never lifts the other" (DE-6).
- "Only invalidating the rider's current card leaves them without a way to pay; an old card being invalidated is recorded and changes nothing else" (DE-73).

## Scope

- **In:** the processor reporting the rider's current card invalid; blocking the rider with reason *invalid card* and notifying them; an old, replaced card being invalidated; lifting the block when a new card is verified.
- **Out:** adding and verifying a card as such (`rider-adds-payment-method`); blocking for outstanding debt and lifting that block (`finance-retries-failed-charge`, `rider-pays-off-outstanding-debt`); a blocked rider being refused a reservation (`rider-reserves-bike`); charging a rider without a usable card (`finance-charges-finished-ride`).

## Related intents

- builds on: `rider-adds-payment-method`
- follows: `rider-signs-user-agreement`
- enables: `rider-reserves-bike` (a blocked rider can't reserve)

## Open questions

- Does a blocked rider's running ride continue? The vision says a block prevents *new* rentals.
- What happens when the card of a rider still in onboarding becomes invalid?
- Can a rider who reserved a bike just before being blocked still unlock it? The domain expert said eligibility isn't checked again at unlock (DE-14), while the vision says a blocked account can't start new rentals.

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
- DE-72: the payment processor notifies VeloCity (expired, cancelled, reported lost).
