---
name: person-registers-with-velocity
sequence: 1
title: A person wants to register with VeloCity
system: velocity
actor: Rider
activity: Join VeloCity
task: Register
status: planned
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "User Onboarding", "Who Uses the System" (Riders); docs/bounded-contexts/velocity/domain-expert-answers.md DE-1, DE-4; autonomous run 2026-11-08
---

# A person wants to register with VeloCity

## Intent

As a **Rider** (a person who wants to ride with VeloCity), I want to **register with VeloCity** so that **I have an account and can start the onboarding that leads to my first ride**.

## Why

The vision's first idea is "Trust is earned once, then honored": a rider proves who they are and how they will pay when they join, and after that unlocking a bike is a single tap. Registration is the first of the eight onboarding steps ("The person registers with VeloCity (creates an account)", vision "User Onboarding"). Nothing else in VeloCity can happen for a person before they have an account.

## Business outcome

- Account Registered: the person has an incomplete VeloCity account, and the next onboarding step is confirming their email.

## Success signal

- (suggested) Share of people who open the app and complete registration.
- (suggested) Share of registered accounts that later reach activation, and the step where people drop out.

## Business rules mentioned

- "Each step must succeed before the next begins — we never skip ahead" (vision "User Onboarding").
- "If the rider drops out mid-onboarding, their account stays in an incomplete state until they finish the missing step. They can resume later" (vision "User Onboarding").
- "We serve individual riders only. There is no corporate or group offering at launch" (vision "Background").

## Scope

- **In:** a person creating their account with the contact details that the next steps confirm; the account starting in the incomplete state; resuming onboarding later.
- **Out:** confirming the email and verifying the phone (`rider-confirms-contact-details`); personal data (`rider-provides-personal-data`); adding a payment method (`rider-adds-payment-method`); signing the user agreement and activation (`rider-signs-user-agreement`); closing an account (not in the vision).

## Related intents

- enables: `rider-confirms-contact-details`
- enables: `rider-provides-personal-data`
- enables: `rider-adds-payment-method`
- enables: `rider-signs-user-agreement`

## Open questions

- Can two accounts share one email address or phone number? The vision doesn't say.
- Is the phone number given at registration, or only at the phone verification step?
- The vision's vocabulary calls a rider "a registered, activated individual", while the onboarding section calls the person registering a rider. Who counts as a rider before activation?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
