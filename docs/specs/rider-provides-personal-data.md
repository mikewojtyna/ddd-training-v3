---
name: rider-provides-personal-data
intent: rider-provides-personal-data
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A rider wants to provide their personal data

Concrete rules, examples and contract for [the intent](../intents/rider-provides-personal-data.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Personal data | Full name, date of birth and country of residency (DE-2). |
| Age on the day | The rider's age in completed years on the calendar day they provide the data (DE-2). |
| Next onboarding step | The first onboarding step the account hasn't completed yet (vision "User Onboarding"). |

## Rules

### R1: A rider whose phone is verified can provide their full name, date of birth and country of residency

- **E1.1** Given Anna verified her phone on 2026-11-08 at 10:05, When Anna provides "Anna Nowak", born 1995-04-12, residing in Poland, at 10:08, Then her personal data is recorded and her next onboarding step is adding a payment method (Personal Data Provided).
- **E1.2** Given Anna registered and confirmed her email but hasn't verified her phone, When Anna provides her personal data, Then the step is rejected and her next onboarding step is still verifying her phone.

### R2: A rider must be at least 16 years old on the day they provide the data (DE-2)

- **E2.1** Given Ben verified his phone, When on 2026-11-08 Ben provides "Ben Kowalski", born 2010-11-09, residing in Poland, Then the step is rejected because Ben is 15, and his next onboarding step is still providing personal data.
- **E2.2** Given Carla verified her phone, When on 2026-11-08 Carla provides "Carla Rossi", born 2010-11-08, residing in Poland, Then her personal data is recorded because she turns 16 that day (Personal Data Provided).

### R3: Any country of residency is accepted (DE-2)

- **E3.1** Given Dieter verified his phone, When on 2026-11-08 Dieter provides "Dieter Weber", born 1988-02-03, residing in Germany, Then his personal data is recorded (Personal Data Provided).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Provide Personal Data | Rider | rider, full name, date of birth, country of residency | Personal Data Provided | R1, R2 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Personal Data Provided | a rider with a verified phone, aged 16 or more, provided their personal data | rider, full name, date of birth, country of residency, provided at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Personal data | full name | Text | required |
| Personal data | date of birth | Date | required; the rider is at least 16 on the day the data is provided (DE-2) |
| Personal data | country of residency | Text | required; any country (DE-2) |

## Out of scope

- Confirming contact details (`rider-confirms-contact-details`) and adding a payment method (`rider-adds-payment-method`).
- Correcting personal data after it was provided: not possible, corrections go through support (DE-98).

## Open questions

- ~~**Q1** Which personal data is required, and is there an age limit?~~ Full name, date of birth and country of residency; at least 16 years old on the day the data is provided; any residency (role-played domain expert, DE-2, 2026-11-08)
- ~~**Q2** Can a rider correct their personal data after providing it?~~ No; corrections go through support, out of scope (role-played domain expert, DE-98, 2026-11-08)
- ~~**Q3** A rider born on 29 February: on which day do they turn 16 in a non-leap year?~~ On 1 March (role-played domain expert, DE-98, 2026-11-08). In practice 16 years after a leap year is again a leap year (until 2100), so the case doesn't arise for the age-16 check; no example added.
- **Q4** What can an under-age person do after E2.1: try again once they turn 16, keeping the same account? (E2.1)

## Notes for the plan

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
