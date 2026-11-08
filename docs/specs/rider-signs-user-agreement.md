---
name: rider-signs-user-agreement
intent: rider-signs-user-agreement
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A rider wants to sign the user agreement and have their account activated

Concrete rules, examples and contract for [the intent](../intents/rider-signs-user-agreement.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| User agreement | The terms of use, liability and city rules a rider signs (vision "User Onboarding"). |
| Activated | Every onboarding step is complete; set the moment the agreement is signed (DE-4). Only an activated account may reserve and unlock bikes (vision). |
| Next onboarding step | The first onboarding step the account hasn't completed yet (vision "User Onboarding"). |

## Rules

### R1: Signing the user agreement after the card is verified activates the account at once (DE-4)

- **E1.1** Given Anna's card ending 4242 was verified on 2026-11-08 at 10:10, When Anna signs the user agreement at 10:12, Then the signature is recorded and Anna's account is activated at 10:12 (Account Activated).

### R2: The agreement can't be signed before a card is verified (vision)

- **E2.1** Given Ben provided his personal data and his only card ending 0002 was declined, When Ben signs the user agreement, Then the step is rejected, Ben's account stays incomplete and his next onboarding step is still adding a payment method.

### R3: A rider can sign later; the account waits incomplete until then (vision)

- **E3.1** Given Carla's card ending 7777 was verified on 2026-11-01 and she stopped there, When Carla signs the user agreement on 2026-11-15, Then Carla's account is activated on 2026-11-15 (Account Activated).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Sign User Agreement | Rider | rider | Account Activated | R2 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Account Activated | a rider with a verified card signed the user agreement | rider, agreement signed at, activated at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Signature | signed at | Timestamp | equals the activation time (DE-4) |

## Out of scope

- Adding a payment method (`rider-adds-payment-method`).
- Reserving and unlocking bikes as an activated rider (`rider-reserves-bike`, `rider-unlocks-reserved-bike`): the rule "only an activated account can reserve and unlock" is checked there.
- Blocking an activated rider (`finance-blocks-rider-with-invalid-card`).
- Agreement versions and signing again.

## Open questions

- ~~**Q1** Is activation a separate step after signing?~~ No; the account is activated automatically the moment the rider signs, because every earlier step is then complete (role-played domain expert, DE-4, 2026-11-08)
- **Q2** Is there a version of the user agreement, and must riders sign again when it changes? Would add data to Account Activated; doesn't block R1-R3.
- **Q3** What happens when an activated rider signs the agreement again (ignored, or rejected)? Doesn't block R1.

## Notes for the plan

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
