---
name: rider-adds-payment-method
sequence: 4
title: A rider wants to add a payment method
system: velocity
actor: Rider
activity: Join VeloCity
task: Add a payment method
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "User Onboarding" (steps 5-6), "Payment Processors", "Background"; docs/bounded-contexts/velocity/domain-expert-answers.md DE-3, DE-5, DE-70, DE-71; autonomous run 2026-11-08
---

# A rider wants to add a payment method

## Intent

As a **Rider**, I want to **add a card that VeloCity verifies** so that **VeloCity knows how I will pay and I can ride without paying at every unlock**.

## Why

Onboarding steps 5 and 6 of the vision: "They add a payment method" and "The payment method is verified (a small pre-authorization against the card)" (vision "User Onboarding"). A rider proves how they will pay when they join ("Trust is earned once, then honored", vision "Background"). VeloCity doesn't hold card details itself: cards are added, verified and later charged through an external payment processor, and a card "is either verified or it isn't" (vision "Payment Processors"). Adding and verifying are one goal for the rider: a card that works. The domain expert decided the pre-authorization is PLN 1.00 (DE-70), a declined card keeps the rider at this step (DE-3), and a rider can replace their card at any time after activation (DE-5).

## Business outcome

- Payment Method Verified: the rider's card passed the pre-authorization and is the card VeloCity charges.
- Payment Method Declined: the card failed the pre-authorization; the rider still has to add a card that works.

## Success signal

- (suggested) Share of riders whose first card is verified.
- (suggested) Share of riders who add another card after a decline instead of dropping out.

## Business rules mentioned

- "a small pre-authorization against the card" (vision "User Onboarding"); PLN 1.00, released at once (DE-70).
- "Each step must succeed before the next begins — we never skip ahead" (vision "User Onboarding").
- "The step 'payment method verified' isn't reached. The rider stays at the payment-method step and adds another card" (DE-3).
- "Yes, at any time after activation. A newly verified card becomes the one we charge" (DE-5).

## Scope

- **In:** adding a card after the personal data step and having it verified; a declined card; replacing the card later with a newly verified one.
- **Out:** providing personal data (`rider-provides-personal-data`); signing the user agreement (`rider-signs-user-agreement`); a card becoming invalid and the block it causes, and lifting that block with a new card (`finance-blocks-rider-with-invalid-card`); charging rides (`finance-charges-finished-ride`).

## Related intents

- builds on: `rider-provides-personal-data`
- enables: `rider-signs-user-agreement`
- enables: `finance-charges-finished-ride`
- enables: `finance-blocks-rider-with-invalid-card`

## Open questions

- Can a rider keep several cards and choose which one is charged, or is there only ever one current card? (DE-5 suggests one.)
- Is there a limit on how many declined cards a rider may try?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
- The vision says VeloCity doesn't hold card details itself; the card is known only through the payment processor.
