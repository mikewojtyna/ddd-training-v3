---
name: rider-provides-personal-data
sequence: 3
title: A rider wants to provide their personal data
system: velocity
actor: Rider
activity: Join VeloCity
task: Provide personal data
status: specified
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "User Onboarding" (step 4), "Background"; docs/bounded-contexts/velocity/domain-expert-answers.md DE-2; autonomous run 2026-11-08
---

# A rider wants to provide their personal data

## Intent

As a **Rider**, I want to **provide my basic personal data** so that **VeloCity knows who I am and my onboarding moves on to adding a payment method**.

## Why

Onboarding step 4 of the vision: "They provide basic personal data (name, date of birth, residency)" (vision "User Onboarding"). A rider proves who they are when they join ("Trust is earned once, then honored", vision "Background"). The domain expert added an age limit: a rider must be at least 16 years old (DE-2).

## Business outcome

- Personal Data Provided: the rider's full name, date of birth and country of residency are known, and the next onboarding step is adding a payment method.

## Success signal

- (suggested) Share of riders who provide their personal data right after verifying their phone.
- (suggested) Number of under-age riders who try to join (expected to be low).

## Business rules mentioned

- "basic personal data (name, date of birth, residency)" (vision "User Onboarding").
- "Each step must succeed before the next begins — we never skip ahead" (vision "User Onboarding").
- "A rider must be at least 16 years old on the day they provide the data. Any residency is accepted" (DE-2).

## Scope

- **In:** providing full name, date of birth and country of residency after the phone is verified; the age limit.
- **Out:** confirming contact details (`rider-confirms-contact-details`); adding a payment method (`rider-adds-payment-method`); correcting personal data later (not in the vision).

## Related intents

- builds on: `rider-confirms-contact-details`
- enables: `rider-adds-payment-method`

## Open questions

- Can a rider correct their personal data after providing it?
- What can an under-age person do with their incomplete account (wait until their 16th birthday, or nothing)?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
