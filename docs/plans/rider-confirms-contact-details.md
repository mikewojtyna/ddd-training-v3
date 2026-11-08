---
name: rider-confirms-contact-details
intent: rider-confirms-contact-details
spec: rider-confirms-contact-details
system: velocity
status: approved
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A rider wants to confirm their email and phone number

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **Accounts**: onboarding steps 2 and 3 extend the account's onboarding state created by
`person-registers-with-velocity`. The link and the SMS code are checked by the messaging provider (DE-1), so Accounts
only records the two confirmations, in order; no ACL to the messaging provider is needed for this intent. Order: canvas
tags → scaffolder update (api records) → domain rules test-first → use cases and REST → acceptance tests. Prerequisite:
`person-registers-with-velocity` S5 (Accounts module, registration and Account Status exist).

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A registered rider confirms their email by following the emailed link | Accounts | onboarding state is Accounts' (CML "Onboarding"; canvas inbound "Confirm Email") |
| R2 A rider verifies their phone by typing the SMS code, but only after the email is confirmed | Accounts | "who can say no?": the onboarding order is an Accounts rule (canvas "Onboarding steps are completed in order") |
| R3 A confirmation can be completed later; the account waits incomplete | Accounts | same account state as R1/R2 |
| Command: Confirm Email | Accounts | canvas inbound "Rider app → Confirm Email"; the confirmation is recorded on the account |
| Command: Verify Phone | Accounts | rejected by R2, an Accounts rule; canvas inbound "Verify Phone" |
| Event: Email Confirmed | Accounts | the fact is recorded in Accounts; no listener elsewhere |
| Event: Phone Verified | Accounts | the fact is recorded in Accounts; no listener elsewhere |

**Outcome:** one existing context.

## Context map changes

- None. The messaging provider checks links and codes outside VeloCity (DE-1); the rider app passes the trusted result to Accounts.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Rider | Accounts | Rider | same word |
| Email confirmation | Accounts | Email confirmed (onboarding step) | domain |
| Phone verification | Accounts | Phone verified (onboarding step), phone number recorded at this step | domain; `{phoneNumber}` in the REST body |
| Next onboarding step | Accounts | `onboardingStep` in Account Status | `GET /accounts/riders/{riderId}` |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Confirm Email | Accounts | `ConfirmEmailUseCase` + `ConfirmEmail(riderId)`; `POST /accounts/riders/{riderId}/email-confirmation` (no body: the email is the one the rider registered with); a repeat returns `200` and records nothing (E1.2); unknown rider → `404` |
| Command: Verify Phone | Accounts | `VerifyPhoneUseCase` + `VerifyPhone(riderId, phoneNumber)`; `POST /accounts/riders/{riderId}/phone-verification` `{phoneNumber}`; R2 → `409 {"error": "OnboardingStepOutOfOrder"}` |
| Event: Email Confirmed | Accounts | domain event `EmailConfirmed(riderId, email, confirmedAt)`; no `api/events` record (no listener) |
| Event: Phone Verified | Accounts | domain event `PhoneVerified(riderId, phoneNumber, verifiedAt)`; no `api/events` record (no listener) |

## Canvas changes

### Accounts (`docs/bounded-contexts/velocity/accounts/canvas.md`)

- **Business decisions, add:**
  - `Following the emailed link confirms the email (spec: rider-confirms-contact-details#E1.1)`: Given Anna registered with anna@example.com at 10:00, When she follows the confirmation link at 10:03, Then her email is confirmed and her next onboarding step is verifying her phone (Email Confirmed).
  - `Confirming the email twice changes nothing (spec: rider-confirms-contact-details#E1.2)`: Given Anna confirmed her email at 10:03, When she follows the same link again at 10:20, Then nothing changes and no second Email Confirmed is recorded (DE-98).
  - `Typing the SMS code verifies the phone (spec: rider-confirms-contact-details#E2.1)`: Given Anna confirmed her email, When she gives +48 600 100 200 and types the SMS code at 10:05, Then her phone is verified and her next onboarding step is providing personal data (Phone Verified).
  - `A confirmation can wait (spec: rider-confirms-contact-details#E3.1)`: Given Anna confirmed her email on 2026-11-01, When she types the SMS code on 2026-11-06, Then her phone is verified (Phone Verified).
- **Tag existing:** "Onboarding steps are completed in order" gains `(spec: rider-confirms-contact-details#E2.2)`; it already is that example (phone before email is rejected).
- **Ubiquitous language, change:** **Onboarding step** list: "phone verified" records the phone number given at that step (DE-98).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle. |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md; this intent has no cross-context call |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | reuses the Accounts repository from `person-registers-with-velocity` S5 |
| D5 | Link and code checks | not modelled: the REST call itself is the trusted confirmation from the messaging provider (DE-1) | spec Out of scope; avoids a fake messaging integration |
| D6 | Repeated email confirmation | accepted silently (`200`, no event) rather than rejected | DE-98, E1.2 |

## Steps

### S1: Tag the Accounts canvas with the contact-detail examples
- **Status:** todo
- **Context:** Accounts
- **Skill:** none (this skill: apply the canvas diff above)
- **Covers:** R1, R2, R3
- **Done when:** the Canvas changes above are in the Accounts canvas.

### S2: Add the contact-detail commands to the Accounts api
- **Status:** todo
- **Context:** Accounts
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Confirm Email, Verify Phone
- **Done when:** prerequisite `person-registers-with-velocity` S5 done; `api/usecases/ConfirmEmail` and `VerifyPhone` records exist; `AccountsArchitectureTest` green; context plan changelog updated.

### S3: Email confirmation and phone verification rules
- **Status:** todo
- **Context:** Accounts
- **Skill:** ddd-tactical-building-blocks (test-first, checkpoint loop off per D1)
- **Covers:** R1, R2, R3, E1.1, E1.2, E2.1, E2.2, E3.1, Email Confirmed, Phone Verified
- **Done when:** one unit test per example passes; `./mvnw test` green.

### S4: Use cases and REST for the confirmations
- **Status:** todo
- **Context:** Accounts
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Confirm Email, Verify Phone, Email Confirmed, Phone Verified
- **Done when:** `ConfirmEmailUseCase`, `VerifyPhoneUseCase` publish their events through `DomainEventPublisher`; both endpoints in `AccountsController` with `409`/`404` mapping; `./mvnw test` green.

### S5: Acceptance tests for the confirmations
- **Status:** todo
- **Context:** Accounts
- **Skill:** none (this skill)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/RiderConfirmsContactDetailsSpecTest.java`, `@VelocitySpringTest` over REST with `MutableClock`; E1.2 counts `EmailConfirmed` with `@RecordApplicationEvents`; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `RiderConfirmsContactDetailsSpecTest#e1_1_rider_confirms_email_with_link` |
| E1.2 | `RiderConfirmsContactDetailsSpecTest#e1_2_second_email_confirmation_changes_nothing` |
| E2.1 | `RiderConfirmsContactDetailsSpecTest#e2_1_rider_verifies_phone_after_email` |
| E2.2 | `RiderConfirmsContactDetailsSpecTest#e2_2_phone_verification_rejected_before_email_confirmed` |
| E3.1 | `RiderConfirmsContactDetailsSpecTest#e3_1_phone_verified_days_later` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1, S3, S4, S5 | |
| R1 / E1.2 | S1, S3, S4, S5 | |
| R2 / E2.1 | S1, S3, S4, S5 | |
| R2 / E2.2 | S1, S3, S4, S5 | |
| R3 / E3.1 | S1, S3, S5 | |

## Risks and open questions

- Spec Q2 (validity of link and code) stays open; with D5 VeloCity never sees an expired code, so E3.1 holds as written. If the provider's validity becomes a VeloCity rule, a new example and an ACL to the messaging provider are needed.
- E1.2 is asserted through recorded application events: the shared Spring context means the test filters events by its own rider id.
- No CONTRACT GAP: the two REST paths in integration-contracts.md suffice.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (one context; canvas example already exists for E2.2; every example covered; ≤3 rules in the tactical step).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.
