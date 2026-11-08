---
name: rider-signs-user-agreement
sequence: 5
title: A rider wants to sign the user agreement and have their account activated
system: velocity
actor: Rider
activity: Join VeloCity
task: Sign the user agreement
status: planned
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "User Onboarding" (steps 7-8), "Key Business Rules to Know", "Background"; docs/bounded-contexts/velocity/domain-expert-answers.md DE-4; autonomous run 2026-11-08
---

# A rider wants to sign the user agreement and have their account activated

## Intent

As a **Rider**, I want to **sign the user agreement** so that **my account is activated and I can reserve and unlock bikes**.

## Why

Onboarding steps 7 and 8 of the vision: "They sign the user agreement" (covering the terms, liability and city rules) and "The account is activated" (vision "User Onboarding"). "Only an activated account can reserve and unlock a bike", and "No unlock without a fully activated account" (vision "User Onboarding", "Key Business Rules to Know"). The domain expert decided that activation isn't a separate step: the account is activated the moment the rider signs, because every earlier step is then complete (DE-4). Signing is the last thing a rider does before their first ride.

## Business outcome

- Account Activated: the rider completed every onboarding step and may reserve and unlock bikes.

## Success signal

- (suggested) Share of riders with a verified card who sign the agreement the same day.
- (suggested) Share of activated riders who start their first ride within a week.

## Business rules mentioned

- "Each step must succeed before the next begins — we never skip ahead" (vision "User Onboarding").
- "Only an activated account can reserve and unlock a bike" (vision "User Onboarding").
- "The account is activated automatically the moment the rider signs the user agreement, because every earlier step is then complete" (DE-4).

## Scope

- **In:** signing the agreement after the card is verified; the account being activated at that moment; signing after a pause in onboarding.
- **Out:** adding a payment method (`rider-adds-payment-method`); reserving a bike as an activated rider (`rider-reserves-bike`); blocking an activated rider (`finance-blocks-rider-with-invalid-card`); new versions of the agreement that riders must sign again (not in the vision).

## Related intents

- builds on: `rider-adds-payment-method`
- enables: `rider-reserves-bike`

## Open questions

- Is there a version of the user agreement, and must riders sign again when it changes?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
