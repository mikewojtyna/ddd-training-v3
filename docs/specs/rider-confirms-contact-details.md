---
name: rider-confirms-contact-details
intent: rider-confirms-contact-details
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A rider wants to confirm their email and phone number

Concrete rules, examples and contract for [the intent](../intents/rider-confirms-contact-details.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Email confirmation | The rider followed the link sent to the email address they registered with (DE-1). |
| Phone verification | The rider gives their phone number at this step and types the SMS code sent to it (DE-1, DE-98). |
| Next onboarding step | The first onboarding step the account hasn't completed yet (vision "User Onboarding"). |

## Rules

### R1: A registered rider confirms their email by following the emailed link

- **E1.1** Given Anna registered on 2026-11-08 at 10:00 with anna@example.com, When Anna follows the confirmation link sent to anna@example.com at 10:03, Then Anna's email is confirmed and her next onboarding step is verifying her phone (Email Confirmed).
- **E1.2** Given Anna confirmed anna@example.com at 10:03, When Anna follows the same confirmation link again at 10:20, Then nothing changes: her email stays confirmed, her next onboarding step is still verifying her phone, and no second Email Confirmed is recorded (DE-98).

### R2: A rider verifies their phone by typing the SMS code, but only after the email is confirmed (vision)

- **E2.1** Given Anna confirmed anna@example.com at 10:03, When Anna gives phone number +48 600 100 200 at 10:04 and types the SMS code sent to it at 10:05, Then Anna's phone is verified and her next onboarding step is providing personal data (Phone Verified).
- **E2.2** Given Ben registered with ben@example.com and hasn't confirmed his email, When Ben gives phone number +48 600 300 400 and types the SMS code sent to it, Then the step is rejected and Ben's next onboarding step is still confirming his email.

### R3: A confirmation can be completed later; the account waits incomplete (vision)

- **E3.1** Given Anna registered on 2026-11-01 and confirmed her email the same day, When Anna types the SMS code on 2026-11-06 (? code validity, Q2), Then Anna's phone is verified (Phone Verified).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Confirm Email | Rider (following the emailed link) | rider, email address | Email Confirmed | none |
| Verify Phone | Rider (typing the SMS code) | rider, phone number | Phone Verified | R2 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Email Confirmed | the rider followed the emailed link | rider, email address, confirmed at |
| Phone Verified | the rider typed the SMS code, after the email was confirmed | rider, phone number, verified at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Email confirmation | email address | Text | the address the rider registered with |
| Phone verification | phone number | Text | given by the rider at this step (DE-98) |

## Out of scope

- Registering (`person-registers-with-velocity`) and providing personal data (`rider-provides-personal-data`).
- Checking the link and the SMS code themselves: the messaging provider does it, VeloCity records the result (DE-1).
- Changing the email or phone number after registration.

## Open questions

- ~~**Q1** How are email and phone confirmed?~~ The rider follows the link in the email and types the SMS code; both are trusted confirmations from the messaging provider, recorded in order (role-played domain expert, DE-1, 2026-11-08)
- **Q2** How long do the email link and the SMS code stay valid, and can the rider ask for a new one? (E3.1) If a code expires, E3.1 needs a fresh code first.
- ~~**Q3** What happens when the rider follows the email link again after the email is confirmed?~~ Nothing changes (role-played domain expert, DE-98, 2026-11-08); see E1.2.
- ~~**Q4** Is the phone number given at registration or at the phone step?~~ At the phone step (role-played domain expert, DE-98, 2026-11-08)

## Notes for the plan

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
- DE-1: the link and the code are checked by the messaging provider; VeloCity only records the confirmations, in order.
