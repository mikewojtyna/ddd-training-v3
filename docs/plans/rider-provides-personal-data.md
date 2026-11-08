---
name: rider-provides-personal-data
intent: rider-provides-personal-data
spec: rider-provides-personal-data
system: velocity
status: approved
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A rider wants to provide their personal data

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **Accounts**: onboarding step 4 with the age limit (DE-2). It builds on the onboarding state from
`person-registers-with-velocity` and `rider-confirms-contact-details`. No other context is involved. Order: canvas tags →
scaffolder update → domain rules test-first → use case and REST → acceptance tests. Prerequisite:
`rider-confirms-contact-details` S4 (phone verification exists, so a rider can reach this step over REST).

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A rider whose phone is verified can provide their full name, date of birth and country of residency | Accounts | onboarding order and personal data are Accounts' (CML "Onboarding"; canvas inbound "Provide Personal Data") |
| R2 A rider must be at least 16 years old on the day they provide the data | Accounts | canvas business decision "A rider must be at least 16 years old" (DE-2); a compliance rule of onboarding |
| R3 Any country of residency is accepted | Accounts | same command, same data (DE-2) |
| Command: Provide Personal Data | Accounts | rejected by R1 and R2, both Accounts rules |
| Event: Personal Data Provided | Accounts | the fact is recorded in Accounts; no listener elsewhere |

**Outcome:** one existing context.

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Personal data | Accounts | Personal data (full name, date of birth, country of residency) | REST body `{fullName, dateOfBirth, countryOfResidency}` |
| Age on the day | Accounts | Age on the day the data is provided | use case: "today" from `velocityClock` (D5) |
| Country of residency | Accounts | Country of residency (free text, any country) | same word |
| Next onboarding step | Accounts | `onboardingStep` | Account Status |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Provide Personal Data | Accounts | `ProvidePersonalDataUseCase` + `ProvidePersonalData(riderId, fullName, dateOfBirth, countryOfResidency)`; `POST /accounts/riders/{riderId}/personal-data`; R1 → `409 {"error": "OnboardingStepOutOfOrder"}`, R2 → `409 {"error": "RiderTooYoung"}`; missing field → `400` |
| Event: Personal Data Provided | Accounts | domain event `PersonalDataProvided(riderId, fullName, dateOfBirth, countryOfResidency, providedAt)`; no `api/events` record (no listener) |

Data notes for the tactical skill: all three fields required; date of birth is a calendar date (`"1995-04-12"`);
no country list (R3); personal data can't be provided twice (DE-98: corrections go through support), which the onboarding
order already rejects.

## Canvas changes

### Accounts (`docs/bounded-contexts/velocity/accounts/canvas.md`)

- **Business decisions, add:**
  - `A rider with a verified phone provides personal data (spec: rider-provides-personal-data#E1.1)`: Given Anna verified her phone at 10:05, When she provides "Anna Nowak", born 1995-04-12, residing in Poland, at 10:08, Then her personal data is recorded and her next onboarding step is adding a payment method (Personal Data Provided).
  - `Personal data needs a verified phone (spec: rider-provides-personal-data#E1.2)`: Given Anna confirmed her email but hasn't verified her phone, When she provides her personal data, Then the step is rejected and her next onboarding step is still verifying her phone.
  - `Any residency is accepted (spec: rider-provides-personal-data#E3.1)`: Given Dieter verified his phone, When on 2026-11-08 he provides "Dieter Weber", born 1988-02-03, residing in Germany, Then his personal data is recorded (Personal Data Provided).
- **Tag existing:** "A rider must be at least 16 years old" gains `(spec: rider-provides-personal-data#E2.1)` for the 2010-11-09 case and `(spec: rider-provides-personal-data#E2.2)` for its *Boundary* line (2010-11-08 accepted).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle. |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md; this intent has no cross-context call |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | reuses the Accounts repository |
| D5 | "The day" of R2 | the calendar date of `velocityClock`'s instant in zone `Europe/Warsaw` (VeloCity's city), passed into the domain by the use case | the domain stays clock-free; all spec examples fall on the same date in UTC and Warsaw |

## Steps

### S1: Tag the Accounts canvas with the personal-data examples
- **Status:** todo
- **Context:** Accounts
- **Skill:** none (this skill: apply the canvas diff above)
- **Covers:** R1, R2, R3
- **Done when:** the Canvas changes above are in the Accounts canvas.

### S2: Add Provide Personal Data to the Accounts api
- **Status:** todo
- **Context:** Accounts
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Provide Personal Data
- **Done when:** prerequisite `rider-confirms-contact-details` S4 done; `api/usecases/ProvidePersonalData` record exists; `AccountsArchitectureTest` green.

### S3: Personal data and the age limit
- **Status:** todo
- **Context:** Accounts
- **Skill:** ddd-tactical-building-blocks (test-first, checkpoint loop off per D1)
- **Covers:** R1, R2, R3, E1.1, E1.2, E2.1, E2.2, E3.1, Personal Data Provided
- **Done when:** one unit test per example passes; `./mvnw test` green.

### S4: Use case and REST for personal data
- **Status:** todo
- **Context:** Accounts
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Provide Personal Data, Personal Data Provided
- **Done when:** `ProvidePersonalDataUseCase` publishes `PersonalDataProvided`; `POST /accounts/riders/{riderId}/personal-data` with `409`/`404`/`400`; `./mvnw test` green. This unblocks `person-registers-with-velocity` S7.

### S5: Acceptance tests for personal data
- **Status:** todo
- **Context:** Accounts
- **Skill:** none (this skill)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/RiderProvidesPersonalDataSpecTest.java`, `@VelocitySpringTest` over REST, `MutableClock` set to 2026-11-08; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `RiderProvidesPersonalDataSpecTest#e1_1_rider_with_verified_phone_provides_personal_data` |
| E1.2 | `RiderProvidesPersonalDataSpecTest#e1_2_personal_data_rejected_before_phone_verified` |
| E2.1 | `RiderProvidesPersonalDataSpecTest#e2_1_fifteen_year_old_is_rejected` |
| E2.2 | `RiderProvidesPersonalDataSpecTest#e2_2_rider_turning_sixteen_today_is_accepted` |
| E3.1 | `RiderProvidesPersonalDataSpecTest#e3_1_any_country_of_residency_is_accepted` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1, S3, S4, S5 | |
| R1 / E1.2 | S1, S3, S4, S5 | |
| R2 / E2.1 | S1, S3, S4, S5 | |
| R2 / E2.2 | S1, S3, S4, S5 | |
| R3 / E3.1 | S1, S3, S5 | |

## Risks and open questions

- Spec Q4 (can an under-age person retry on their 16th birthday with the same account?) stays open. E2.1 leaves the next step at "provide personal data", so a retry later is technically possible; no example forces either answer.
- D5 picks Warsaw's calendar day; a rider providing data between 23:00 and 24:00 UTC is judged on the Warsaw date. Flag for review.
- No CONTRACT GAP.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (one context; canvas already has the age rule; boundary example tagged; every example covered).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.
