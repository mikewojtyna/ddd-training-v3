---
name: person-registers-with-velocity
intent: person-registers-with-velocity
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A person wants to register with VeloCity

Concrete rules, examples and contract for [the intent](../intents/person-registers-with-velocity.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Account | A person's identity in VeloCity. It is incomplete until every onboarding step is done, then activated (vision "Domain Vocabulary"). |
| Onboarding | The fixed sequence registered → email confirmed → phone verified → personal data provided → payment method added and verified → user agreement signed → activated. A step counts only when every earlier step is complete (vision "User Onboarding"). |
| Next onboarding step | The first step of the sequence the account hasn't completed yet. |
| Email address | The address the person registers with; one account per email address (DE-98). The phone number is given later, at the phone step (DE-98). |

## Rules

### R1: A person can register with an email address

- **E1.1** Given nobody has registered with anna@example.com, When Anna registers on 2026-11-08 at 10:00 with anna@example.com, Then Anna has an incomplete account (Account Registered).

### R2: A newly registered account is incomplete and waits for the email confirmation (vision)

- **E2.1** Given Anna registered on 2026-11-08 at 10:00, When Anna looks at her onboarding progress at 10:01, Then her account is incomplete and her next onboarding step is confirming her email.
- **E2.2** Given Anna registered on 2026-11-08 at 10:00 and hasn't confirmed her email, When Anna tries to provide her personal data at 10:05, Then the step is rejected and her next onboarding step is still confirming her email.

### R3: An incomplete account waits for its missing step without a deadline (vision)

- **E3.1** Given Anna registered on 2026-11-01 and did nothing else, When Anna opens the app on 2026-11-20, Then her account is still incomplete and her next onboarding step is confirming her email.

### R4: An email address that already has an account can't be registered again (DE-98)

- **E4.1** Given Anna registered with anna@example.com on 2026-11-08 at 10:00, When Ben registers with anna@example.com at 11:00, Then the registration is rejected and no second account is created.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Register | Person (future Rider) | email address | Account Registered | R4 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| Onboarding Progress | Rider | rider | account state (incomplete, activated), completed steps, next onboarding step |

### Events

| Event | When | Data |
|-------|------|------|
| Account Registered | a person registers | rider, email address, registered at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Registration | email address | Text | required, an email address; not yet used by another account (DE-98) |
| Onboarding Progress | account state | one of {incomplete, activated} | incomplete right after registration |
| Onboarding Progress | next onboarding step | one of {confirm email, verify phone, provide personal data, add payment method, sign user agreement, none} | confirm email right after registration |

## Out of scope

- Confirming the email and verifying the phone (`rider-confirms-contact-details`).
- Providing personal data (`rider-provides-personal-data`), adding a payment method (`rider-adds-payment-method`), signing the user agreement and activation (`rider-signs-user-agreement`). E2.2 only shows that registration alone doesn't open the later steps.
- Blocked accounts (`finance-blocks-rider-with-invalid-card`).
- Closing an account, changing contact details, corporate or group accounts (vision "Background": individual riders only).

## Open questions

- ~~**Q1** Can a second person register with an email address that already has an account?~~ No, a duplicate email at registration is rejected (role-played domain expert, DE-98, 2026-11-08)
- ~~**Q2** Is the phone number given at registration, or only at the phone verification step?~~ At the phone step (role-played domain expert, DE-98, 2026-11-08)
- **Q3** The vision's vocabulary calls a rider "a registered, activated individual", while "User Onboarding" calls the person registering a rider. Is the person a rider before activation? (Vocabulary only; doesn't change R1-R3.)

## Notes for the plan

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
- The canvases name the onboarding question that others ask "may this rider rent right now?"; Onboarding Progress here is the rider's own view of the same account.
