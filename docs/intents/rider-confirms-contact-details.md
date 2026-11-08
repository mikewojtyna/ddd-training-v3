---
name: rider-confirms-contact-details
sequence: 2
title: A rider wants to confirm their email and phone number
system: velocity
actor: Rider
activity: Join VeloCity
task: Confirm contact details
status: planned
date: 2026-11-08
supersedes:
superseded_by:
source: docs/domains/velocity/velocity-vision.md "User Onboarding" (steps 2-3); docs/bounded-contexts/velocity/domain-expert-answers.md DE-1; autonomous run 2026-11-08
---

# A rider wants to confirm their email and phone number

## Intent

As a **Rider**, I want to **confirm my email address and verify my phone number** so that **VeloCity can trust that it can reach me and my onboarding moves on to my personal data**.

## Why

Onboarding steps 2 and 3 of the vision: "Their email is confirmed" and "Their phone number is verified" (vision "User Onboarding"). They are part of earning the rider's trust once ("Trust is earned once, then honored", vision "Background"). The two confirmations are one goal for the rider: proving the contact details they registered with are theirs. The domain expert decided how: the rider follows the link in the email and types the SMS code (DE-1).

## Business outcome

- Email Confirmed: the rider proved the email address they registered with is theirs.
- Phone Verified: the rider proved the phone number they registered with is theirs; the next onboarding step is providing personal data.

## Success signal

- (suggested) Share of registered accounts that verify their phone within a day.
- (suggested) Few support contacts about confirmation links or SMS codes.

## Business rules mentioned

- "Each step must succeed before the next begins — we never skip ahead" (vision "User Onboarding"): the email comes before the phone.
- "The rider follows the link in the email and types the SMS code. Both are trusted confirmations from our messaging provider" (DE-1).

## Scope

- **In:** confirming the email, then verifying the phone, in that order; resuming later.
- **Out:** registering (`person-registers-with-velocity`); providing personal data (`rider-provides-personal-data`); changing the email or phone after registration (not in the vision); how long a link or code stays valid (left to the messaging provider).

## Related intents

- builds on: `person-registers-with-velocity`
- enables: `rider-provides-personal-data`

## Open questions

- How long do the email link and the SMS code stay valid, and can the rider ask for a new one?
- What happens when the rider follows the email link a second time?

## Notes for the spec

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
- DE-1: the link and the code are checked by the messaging provider; VeloCity only records the confirmations, in order.
