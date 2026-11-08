---
name: person-registers-with-velocity
intent: person-registers-with-velocity
spec: person-registers-with-velocity
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A person wants to register with VeloCity

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **Accounts**, which doesn't exist in code yet: this plan creates the module (scaffolder, new context
from its canvas) and builds the first piece of onboarding, registration and the rider's own view of onboarding progress.
Every rule guards the account's onboarding state, which the Accounts canvas owns ("Onboarding" in the CML). No other
context is involved and no context-map change is needed. Order: canvas tags → module scaffold → domain rules test-first →
use case, query, REST, in-memory repository → acceptance tests. E2.2's acceptance test waits for the Provide Personal Data
endpoint from `rider-provides-personal-data`.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A person can register with an email address | Accounts | rule owner: Accounts holds the account and onboarding state (CML responsibility "Onboarding"; canvas inbound "Register") |
| R2 A newly registered account is incomplete and waits for the email confirmation | Accounts | rule reads the onboarding sequence, which is Accounts' ubiquitous language ("Onboarding step", canvas) |
| R3 An incomplete account waits for its missing step without a deadline | Accounts | same state as R2; Accounts' canvas: an account is *incomplete* until activated |
| R4 An email address that already has an account can't be registered again | Accounts | "who can say no?": only Accounts knows which emails have accounts (DE-98) |
| Command: Register | Accounts | rejected by R4, an Accounts rule; canvas inbound "Rider app → Register" |
| Query: Onboarding Progress | Accounts | view of Accounts' own data; integration-contracts.md "Account Status (query)" |
| Event: Account Registered | Accounts | the fact happens in Accounts; no listener elsewhere (integration-contracts.md lists none) |

**Outcome:** one existing context (Accounts, on the map and canvas, not yet in code).

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Person (future Rider) | Accounts | Rider (a person with an account; canvas UL) | REST adapter: `POST /accounts/riders` creates a rider |
| Account | Accounts | Account (*incomplete* / *activated*) | same word |
| Onboarding Progress | Accounts | Account Status (onboarding step, activated, blocks) | `GET /accounts/riders/{riderId}`; spec "next onboarding step" → `onboardingStep` |
| Next onboarding step | Accounts | Onboarding step (next one to complete) | domain; REST values in Contract mapping |
| Email address | Accounts | Email address | same word |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Register | Accounts | `RegisterRiderUseCase` + `RegisterRider(email)` in `accounts/api/usecases`; `POST /accounts/riders` `{email}` → `201 {riderId}`; R4 → `409 {"error": "EmailAlreadyRegistered"}` |
| Query: Onboarding Progress | Accounts | `AccountStatusQuery(riderId)` + `AccountStatus` result in `accounts/api/queries`, `AccountStatusQueryHandler`; `GET /accounts/riders/{riderId}` → `{riderId, onboardingStep, activated, blocks: []}`; unknown rider → `404` |
| Event: Account Registered | Accounts | domain event `AccountRegistered(riderId, email, registeredAt)` returned in `DomainEvents`, published by the use case through `DomainEventPublisher`; no `api/events` record (no listener) |

`onboardingStep` values (next step to complete): `CONFIRM_EMAIL`, `VERIFY_PHONE`, `PROVIDE_PERSONAL_DATA`,
`ADD_PAYMENT_METHOD`, `SIGN_USER_AGREEMENT`, `NONE` (see CONTRACT GAP below). Data shape notes for the tactical skill:
the email address is required and well-formed (malformed → `400`); uniqueness is R4.

## Canvas changes

### Accounts (`docs/bounded-contexts/velocity/accounts/canvas.md`)

- **Ubiquitous language, add:** **Next onboarding step**: the first onboarding step the account hasn't completed yet; right after registration it is confirming the email.
- **Business decisions, add:**
  - `A person registers with an email address (spec: person-registers-with-velocity#E1.1)`: Given nobody has registered with anna@example.com, When Anna registers with anna@example.com on 2026-11-08 at 10:00, Then Anna has an incomplete account (Account Registered).
  - `A new account waits for the email confirmation (spec: person-registers-with-velocity#E2.1)`: Given Anna registered at 10:00, When she looks at her account status at 10:01, Then her account is incomplete and her next onboarding step is confirming her email.
  - `An incomplete account waits without a deadline (spec: person-registers-with-velocity#E3.1)`: Given Anna registered on 2026-11-01 and did nothing else, When she opens the app on 2026-11-20, Then her account is still incomplete and her next onboarding step is confirming her email.
  - `One account per email address (spec: person-registers-with-velocity#E4.1)`: Given Anna registered with anna@example.com, When Ben registers with anna@example.com, Then the registration is rejected and no second account is created (DE-98).
- **Tag existing:** "Onboarding steps are completed in order" gains `(spec: person-registers-with-velocity#E2.2)` as a second example: Given Anna registered and hasn't confirmed her email, When she provides her personal data, Then the step is rejected and her next onboarding step is still confirming her email.

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle. |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md; this intent has no cross-context call |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain" |
| D5 | Email uniqueness (R4) | checked across accounts, not inside one account; the tactical skill picks the building block (candidate: a repository lookup by email in the use case, or a small domain service) | an invariant over many accounts can't live in a single account aggregate |
| D6 | Accounts module created here | this plan's S2 scaffolds the whole Accounts module; the later onboarding plans (`rider-confirms-contact-details`, `rider-provides-personal-data`, `rider-adds-payment-method`, `rider-signs-user-agreement`, `finance-blocks-rider-with-invalid-card`) use scaffolder update mode | sequence 1 of the story map; avoids two plans creating the same module |

## Steps

### S1: Tag the Accounts canvas with the registration examples
- **Status:** done
- **Context:** Accounts
- **Skill:** none (this skill: apply the canvas diff above)
- **Covers:** R1, R2, R3, R4
- **Done when:** the Canvas changes above are in `docs/bounded-contexts/velocity/accounts/canvas.md`.

### S2: Scaffold the Accounts module
- **Status:** done
- **Context:** Accounts
- **Skill:** bounded-context-scaffolder (new context, 6a), structural decisions taken from this plan
- **Covers:** Register, Onboarding Progress
- **Done when:** `accounts/package-info.java` with `@BoundedContext(name = "Accounts")` + `@Module`; `api/usecases/RegisterRider`, `api/queries/AccountStatusQuery` + `AccountStatus` records; `AccountsArchitectureTest` green; context plan `docs/bounded-contexts/velocity/accounts/plan.md` written with "approved via docs/plans/person-registers-with-velocity.md"; `./mvnw test` green.

### S3: Registration and onboarding state (run 1 of 2)
- **Status:** done
- **Context:** Accounts
- **Skill:** ddd-tactical-building-blocks (test-first, checkpoint loop off per D1), via bounded-context-scaffolder
- **Covers:** R1, R2, R3, E1.1, E2.1, E3.1, Account Registered
- **Done when:** one unit test per example passes; `./mvnw test` green. (E2.2's domain test needs the personal-data step, so it is written in `rider-provides-personal-data` S3, which builds the onboarding-order guard for that step.)

### S4: One account per email address (run 2 of 2)
- **Status:** done
- **Context:** Accounts
- **Skill:** ddd-tactical-building-blocks (test-first, checkpoint loop off per D1)
- **Covers:** R4, E4.1
- **Done when:** a test for E4.1 passes; `./mvnw test` green.

### S5: Use case, query, REST and persistence for registration
- **Status:** done
- **Context:** Accounts
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Register, Onboarding Progress, Account Registered
- **Done when:** `RegisterRiderUseCase` publishes `AccountRegistered`; `AccountStatusQueryHandler`; `AccountsController` with `POST /accounts/riders` and `GET /accounts/riders/{riderId}`, rejections mapped to `ErrorResponse` (`409`/`404`/`400`); in-memory account repository with `Version`; wiring in `accounts/infra/config`; `./mvnw test` green.

### S6: Acceptance tests for registration
- **Status:** done
- **Context:** Accounts
- **Skill:** none (this skill)
- **Covers:** E1.1, E2.1, E3.1, E4.1
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/PersonRegistersWithVelocitySpecTest.java`, `@VelocitySpringTest`, one method per example over REST with `MutableClock` (unique emails per test, the context is shared); `./mvnw test` green.

### S7: Acceptance test for E2.2
- **Status:** done
- **Context:** Accounts
- **Skill:** none (this skill)
- **Covers:** E2.2
- **Done when:** prerequisite `rider-provides-personal-data` S4 (Provide Personal Data endpoint) is done; `PersonRegistersWithVelocitySpecTest#e2_2_personal_data_rejected_before_email_confirmed` posts personal data for a freshly registered rider, gets `409`, and `onboardingStep` stays `CONFIRM_EMAIL`; `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `PersonRegistersWithVelocitySpecTest#e1_1_person_registers_with_unused_email` |
| E2.1 | `PersonRegistersWithVelocitySpecTest#e2_1_new_account_is_incomplete_and_waits_for_email_confirmation` |
| E2.2 | `PersonRegistersWithVelocitySpecTest#e2_2_personal_data_rejected_before_email_confirmed` |
| E3.1 | `PersonRegistersWithVelocitySpecTest#e3_1_incomplete_account_waits_without_deadline` |
| E4.1 | `PersonRegistersWithVelocitySpecTest#e4_1_duplicate_email_is_rejected` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1, S3, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/RiderRegistration.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/RiderRegistrationTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/api/usecases/RegisterRiderUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/infra/adapters/primary/AccountsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/PersonRegistersWithVelocitySpecTest.java` |
| R2 / E2.1 | S1, S3, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountOnboardingTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/api/queries/AccountStatusQueryHandler.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/infra/adapters/secondary/InMemoryAccountRepository.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/PersonRegistersWithVelocitySpecTest.java` |
| R2 / E2.2 | S1, S7 (domain: `rider-provides-personal-data` S3) | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountOnboardingTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/api/usecases/ProvidePersonalDataUseCase.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/PersonRegistersWithVelocitySpecTest.java` |
| R3 / E3.1 | S1, S3, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/PersonRegistersWithVelocitySpecTest.java` |
| R4 / E4.1 | S1, S4, S5, S6 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/RiderRegistration.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/RiderRegistrationTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/infra/adapters/secondary/InMemoryAccountRepository.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/PersonRegistersWithVelocitySpecTest.java` |

## Risks and open questions

- CONTRACT GAP: `GET /accounts/riders/{riderId}` names `onboardingStep` but not its values or meaning. Planned as the **next** step to complete: `CONFIRM_EMAIL | VERIFY_PHONE | PROVIDE_PERSONAL_DATA | ADD_PAYMENT_METHOD | SIGN_USER_AGREEMENT | NONE`; `blocks` values `INVALID_CARD | OUTSTANDING_DEBT`. The spec's "completed steps" follow from the fixed order.
- Spec Q3 (is a person a rider before activation?) is vocabulary only; the plan follows the Accounts canvas, where a rider is any person with an account.
- E2.2 needs another intent's command; its acceptance test (S7) is deferred until `rider-provides-personal-data` S4 exists, and its domain test comes with that plan's S3.
- E3.1 is a "nothing happens" rule: there is no expiry job for incomplete accounts; the test moves `MutableClock` 19 days and asserts the status is unchanged.
- The Spring context is shared by every `@VelocitySpringTest` class: tests use their own emails and rider ids.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (single context, matches canvas and CML; every example covered; E2.2 dependency on a later intent made explicit as S7).
- 2026-11-08 (Accounts wave, autonomous run): Accounts steps built; context plan and TDD log in `docs/bounded-contexts/velocity/accounts/plan.md`. Decisions taken there: the aggregate `Account` calls the `PaymentMethodVerifier` port itself (`addPaymentMethod(card).at(now).using(verifier)`), so the onboarding order is checked before Payments is asked; the api result of Rental Eligibility is `RentalEligibilityView` (the domain enum is `RentalEligibility`); block notifications come from the policy `NotifyRiderWhenBlockedPolicy` on Account Blocked; `POST .../blocks` accepts only `OUTSTANDING_DEBT` and only `DELETE .../blocks/OUTSTANDING_DEBT` exists (the card block is lifted only by a verified card, DE-6). Email uniqueness: factory `RiderRegistration` with port `RegisteredEmails`; the repository also refuses a second new account with a taken email (race guard).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: Accounts steps done
- Accounts module (`velocity/accounts`) built test-first; acceptance tests in `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/`. Details and TDD log: `docs/bounded-contexts/velocity/accounts/plan.md`. All steps done, full suite green.
