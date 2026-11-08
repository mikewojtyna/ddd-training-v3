---
name: rider-signs-user-agreement
intent: rider-signs-user-agreement
spec: rider-signs-user-agreement
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A rider wants to sign the user agreement and have their account activated

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent lands in **Accounts**: the last onboarding step, which activates the account at once (DE-4, CML
"Account activation"). It relies on the payment step from `rider-adds-payment-method` (verified card through Payments),
but adds no new integration: signing only reads Accounts' own onboarding state. Order: canvas tags → scaffolder update →
domain rules test-first → use case and REST → acceptance tests. Prerequisite: `rider-adds-payment-method` S8.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 Signing the user agreement after the card is verified activates the account at once | Accounts | canvas business decision "Signing the agreement activates the account"; CML responsibility "Account activation" (DE-4) |
| R2 The agreement can't be signed before a card is verified | Accounts | onboarding order, read from Accounts' own record of the completed payment step (`rider-adds-payment-method` D7); no call to Payments needed |
| R3 A rider can sign later; the account waits incomplete until then | Accounts | same account state |
| Command: Sign User Agreement | Accounts | rejected by R2, an Accounts rule; canvas inbound "Rider app → Sign User Agreement" |
| Event: Account Activated | Accounts | the fact happens in Accounts; canvas outbound lists no listener outside Accounts, and Rentals asks eligibility through the Rental Eligibility query instead |

**Outcome:** one existing context.

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| User agreement | Accounts | User agreement (signature recorded with its time) | same word |
| Activated | Accounts | Activated | `activated: true` in Account Status |
| Next onboarding step | Accounts | `onboardingStep` (`NONE` once activated) | Account Status |
| "card ending 4242 was verified" | Accounts | payment step completed with a verified payment method | set up over REST with `tok_visa_4242` |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Sign User Agreement | Accounts | `SignUserAgreementUseCase` + `SignUserAgreement(riderId)`; `POST /accounts/riders/{riderId}/agreement-signature` (no body); R2 → `409 {"error": "OnboardingStepOutOfOrder"}` |
| Event: Account Activated | Accounts | domain event `AccountActivated(riderId, agreementSignedAt, activatedAt)` (both the same instant, DE-4); no `api/events` record (no listener) |

## Canvas changes

### Accounts (`docs/bounded-contexts/velocity/accounts/canvas.md`)

- **Business decisions, tag existing:** "Signing the agreement activates the account" `(spec: rider-signs-user-agreement#E1.1)`.
- **Business decisions, add:**
  - `No signature before a verified card (spec: rider-signs-user-agreement#E2.1)`: Given Ben provided his personal data and his only card tok_declined_0002 was declined, When he signs the user agreement, Then the step is rejected, his account stays incomplete and his next onboarding step is still adding a payment method.
  - `Signing can wait (spec: rider-signs-user-agreement#E3.1)`: Given Carla's card tok_visa_7777 was verified on 2026-11-01, When she signs on 2026-11-15, Then her account is activated on 2026-11-15 (Account Activated).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle. |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md; no cross-context call in this intent |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | reuses the Accounts repository |
| D5 | Activation needs a verified card *once*, not a current card now | signing checks that the payment step was completed; a card invalidated later doesn't reopen the step | matches `finance-blocks-rider-with-invalid-card` E1.3 (DE-93): an invalidated card blocks, it doesn't undo onboarding |

## Steps

### S1: Tag the Accounts canvas with the signing examples
- **Status:** done
- **Context:** Accounts
- **Skill:** none (this skill: apply the canvas diff above)
- **Covers:** R1, R2, R3
- **Done when:** the Canvas changes above are applied.

### S2: Add Sign User Agreement to the Accounts api
- **Status:** done
- **Context:** Accounts
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Sign User Agreement
- **Done when:** prerequisite `rider-adds-payment-method` S8 done; `api/usecases/SignUserAgreement` record; `AccountsArchitectureTest` green.

### S3: Signing and activation rules
- **Status:** done
- **Context:** Accounts
- **Skill:** ddd-tactical-building-blocks (test-first, checkpoint loop off per D1)
- **Covers:** R1, R2, R3, E1.1, E2.1, E3.1, Account Activated
- **Done when:** one unit test per example passes; `./mvnw test` green.

### S4: Use case and REST for signing
- **Status:** done
- **Context:** Accounts
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Sign User Agreement, Account Activated
- **Done when:** `SignUserAgreementUseCase` publishes `AccountActivated`; `POST /accounts/riders/{riderId}/agreement-signature`; Account Status shows `activated: true`, `onboardingStep: NONE`; `./mvnw test` green. This unblocks `rider-adds-payment-method` S10.

### S5: Acceptance tests for signing
- **Status:** done
- **Context:** Accounts
- **Skill:** none (this skill)
- **Covers:** all examples
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/RiderSignsUserAgreementSpecTest.java`, `@VelocitySpringTest`; each test onboards its rider over REST (the card step goes through real Payments with the simulated processor); `./mvnw test` green.

## Acceptance tests

| Example | Test |
|---|---|
| E1.1 | `RiderSignsUserAgreementSpecTest#e1_1_signing_after_verified_card_activates_account` |
| E2.1 | `RiderSignsUserAgreementSpecTest#e2_1_signing_rejected_without_verified_card` |
| E3.1 | `RiderSignsUserAgreementSpecTest#e3_1_signing_two_weeks_later_activates_account` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1, S3, S4, S5 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountOnboardingTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/api/usecases/SignUserAgreementUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/infra/adapters/primary/AccountsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/RiderSignsUserAgreementSpecTest.java` |
| R2 / E2.1 | S1, S3, S4, S5 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountOnboardingTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/RiderSignsUserAgreementSpecTest.java` |
| R3 / E3.1 | S1, S3, S5 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountOnboardingTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/RiderSignsUserAgreementSpecTest.java` |

## Risks and open questions

- Spec Q2 (agreement versions) open: no version recorded; adding one later extends `AccountActivated`.
- Spec Q3 (an activated rider signs again) open: the plan answers with the same `409 OnboardingStepOutOfOrder` as any out-of-order step (no step left); no example asserts it. Flag for review.
- Rental eligibility (activated and unblocked) is built with the blocks in `finance-blocks-rider-with-invalid-card` S8, not here, because it reads both.
- No CONTRACT GAP.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (one context; R2 reads Accounts' own state, no reverse call to Payments; D5 keeps E1.3 of the blocking spec consistent).
- 2026-11-08 (Accounts wave, autonomous run): Accounts steps built; context plan and TDD log in `docs/bounded-contexts/velocity/accounts/plan.md`. Decisions taken there: the aggregate `Account` calls the `PaymentMethodVerifier` port itself (`addPaymentMethod(card).at(now).using(verifier)`), so the onboarding order is checked before Payments is asked; the api result of Rental Eligibility is `RentalEligibilityView` (the domain enum is `RentalEligibility`); block notifications come from the policy `NotifyRiderWhenBlockedPolicy` on Account Blocked; `POST .../blocks` accepts only `OUTSTANDING_DEBT` and only `DELETE .../blocks/OUTSTANDING_DEBT` exists (the card block is lifted only by a verified card, DE-6). Signing twice answers `409 OnboardingStepOutOfOrder` (Q3), tested in `RiderSignsUserAgreementSpecTest#signing_twice_is_rejected`.

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: Accounts steps done
- Accounts module (`velocity/accounts`) built test-first; acceptance tests in `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/`. Details and TDD log: `docs/bounded-contexts/velocity/accounts/plan.md`. All steps done, full suite green.
