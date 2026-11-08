---
name: rider-adds-payment-method
intent: rider-adds-payment-method
spec: rider-adds-payment-method
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: A rider wants to add a payment method

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent spans **Payments** (upstream) and **Accounts** (downstream), along the existing relationship
`Payments [U,OHS,PL]->[D,CF] Accounts : PaymentMethodVerification`. The rider adds a card in Accounts, which owns the
onboarding order (no pre-authorization before personal data, E1.2); Accounts asks Payments to verify the card through an
ACL calling `POST /payments/payment-methods`. Payments owns the PLN 1.00 pre-authorization through the external processor,
the verified/declined decision and the rider's *current card* (replacement, R3). Payments is built first (module scaffold
with the simulated processor), then Accounts. Acceptance tests for R3 wait for activation from `rider-signs-user-agreement`.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 A rider who provided their personal data adds a card, and it is verified by a PLN 1.00 pre-authorization | Accounts (onboarding precondition, step completion) + Payments (pre-authorization, verification) | the precondition reads onboarding state (Accounts canvas "Onboarding steps are completed in order"); pre-authorization and *verified* are Payments' language (Payments canvas, CML "Payment method verification", DE-70). Command owner Accounts, Payments' rule is reached through a command to its OHS |
| R2 A declined card isn't verified; the rider stays at the payment step and can add another card | Payments (declined) + Accounts (step stays) | Payments decides declined (canvas "A declined pre-authorization leaves the card unverified"); Accounts keeps the onboarding step (canvas "A declined card doesn't complete the payment step", DE-3) |
| R3 After activation, a newly verified card replaces the current card | Payments | *current card* is Payments' term and state (Payments canvas "A newly verified card replaces the current one", DE-5); Settlement charges the current card through Payments |
| Command: Add Payment Method | Accounts | Accounts canvas inbound "Rider app → Add Payment Method"; rejected by R1's onboarding precondition, an Accounts rule; Accounts then sends Verify Payment Method to Payments |
| Event: Payment Method Verified | Payments | the fact happens where the pre-authorization succeeds (Payments canvas outbound; no listener outside Payments) |
| Event: Payment Method Declined | Payments | the processor's decline is decided in Payments |

**Outcome:** several existing contexts. Hand-off: Accounts (downstream, CF) → Payments (upstream, OHS/PL), message
*Verify Payment Method* `{riderId, cardToken}` → `{paymentMethodId, verified, declineReason?}`, pattern: synchronous
command to an OHS through an ACL adapter. Direction matches the CML. Payments → PaymentProcessor (external) through
`paymentprocessoracl` (CML `PaymentProcessor [U,OHS,PL]->[D,ACL] Payments`).

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Rider | Payments | rider id (Payments knows no rider beyond the id) | REST body `riderId` |
| Card / payment method | Accounts | Payment method (a reference Payments gives us: `paymentMethodId`) | ACL adapter `paymentsctxacl` |
| Card / payment method | Payments | Payment method, known by the processor's card token | REST body `cardToken` |
| "card ending 4242" | both | card token `tok_visa_4242`, `tok_mc_5555`, `tok_visa_1111`; a declined one `tok_declined_0002` | test data only; the simulated processor declines tokens starting with `tok_declined` |
| Pre-authorization | Payments | Pre-authorization (PLN 1.00, released at once) | port `PaymentProcessor` in Payments' language |
| Verified / Declined | Accounts | verified or not (the payment step is completed or stays open) | ACL adapter maps `verified=false` to a declined result, not an exception |
| Current card | Payments | Current card (newest verified) | same word |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Add Payment Method | Accounts | `AddPaymentMethodUseCase` + `AddPaymentMethod(riderId, cardToken)`; `POST /accounts/riders/{riderId}/payment-methods` `{cardToken}` → `200 {verified, paymentMethodId?}`; onboarding precondition (R1) → `409 {"error": "OnboardingStepOutOfOrder"}` and Payments isn't called. Secondary port `PaymentMethodVerifier` (Accounts' language) with adapter in `accounts/infra/adapters/secondary/paymentsctxacl` (`@AntiCorruptionLayer`, `RestClient` + `VelocityServiceUrls`, own request/response records) |
| Command: Add Payment Method (Payments side: Verify Payment Method) | Payments | `VerifyPaymentMethodUseCase` + `VerifyPaymentMethod(riderId, cardToken)`; `POST /payments/payment-methods` `{riderId, cardToken}` → `200 {paymentMethodId, verified, declineReason?}`. Secondary port `PaymentProcessor` with simulated adapter in `payments/infra/adapters/secondary/paymentprocessoracl` (pre-authorize PLN 1.00 then release; declines `tok_declined*`) |
| Event: Payment Method Verified | Payments | domain event `PaymentMethodVerified(riderId, paymentMethodId, cardToken, verifiedAt)`; no `api/events` record (no listener in integration-contracts.md) |
| Event: Payment Method Declined | Payments | domain event `PaymentMethodDeclined(riderId, paymentMethodId, cardToken, declinedAt)`; no `api/events` record |
| (test support) current card | Payments | `PaymentMethodsQuery(riderId)` + `RiderPaymentMethods` in `payments/api/queries`, handler and `GET /payments/riders/{riderId}/payment-methods` (CONTRACT GAP below) |

## Canvas changes

### Payments (`docs/bounded-contexts/velocity/payments/canvas.md`)

- **Business decisions, tag existing:** "A card that passes pre-authorization is verified and becomes current" `(spec: rider-adds-payment-method#E1.1, #E2.2)`; "A declined pre-authorization leaves the card unverified" `(spec: rider-adds-payment-method#E2.1)`; "A newly verified card replaces the current one" `(spec: rider-adds-payment-method#E3.1)`.
- **Business decisions, add:**
  - `A declined card doesn't replace the current one (spec: rider-adds-payment-method#E3.2)`: Given Anna's current card is tok_visa_4242, When her card tok_declined_0002 is verified and the processor declines it, Then tok_visa_4242 stays her current card (Payment Method Declined).
- **Inbound, add:** Rider app / test | Query | Rider's payment methods (current card and each card's status), see CONTRACT GAP.

### Accounts (`docs/bounded-contexts/velocity/accounts/canvas.md`)

- **Business decisions, tag existing:** "A declined card doesn't complete the payment step" `(spec: rider-adds-payment-method#E2.1)`.
- **Business decisions, add:**
  - `A verified card completes the payment step (spec: rider-adds-payment-method#E1.1, #E2.2)`: Given Anna provided her personal data, When she adds tok_visa_4242 and Payments verifies it, Then her next onboarding step is signing the user agreement.
  - `No card before personal data (spec: rider-adds-payment-method#E1.2)`: Given Ben verified his phone but hasn't provided his personal data, When he adds tok_visa_1111, Then the step is rejected, Payments isn't asked and his next onboarding step is still providing personal data.
  - `An activated rider can add a new card (spec: rider-adds-payment-method#E3.1, #E3.2)`: Given Anna is activated, When she adds a card, verified or declined, Then her account stays activated (DE-5).

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle. |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md. Here: `accounts/.../paymentsctxacl` (Verify Payment Method) and `payments/.../paymentprocessoracl` (external processor) |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain" |
| D5 | A declined card is a result, not a rejection | Payments answers `200 {verified:false}`, Accounts answers `200 {verified:false}`; only the onboarding precondition answers `409` | spec contract: "a declined card ends with Payment Method Declined instead" |
| D6 | Payments module created here | this plan's S2 scaffolds Payments (processor port + simulated adapter); `finance-charges-finished-ride` adds charging in update mode. Whichever plan runs first scaffolds; the other switches to update mode | sequence 4 precedes the charging intents |
| D7 | Accounts keeps only the reference | Accounts records that the payment step is complete and the latest verified `paymentMethodId`; it never holds the card token's meaning or the current-card rule | current card is Payments' state (R3); avoids two sources of truth |

## Steps

### S1: Tag the Payments canvas
- **Status:** done
- **Context:** Payments
- **Skill:** none (this skill: apply the canvas diff above)
- **Covers:** R1, R2, R3
- **Done when:** the Payments canvas changes above are applied.

### S2: Scaffold the Payments module with the simulated processor
- **Status:** done
- **Context:** Payments
- **Skill:** bounded-context-scaffolder (new context, or update mode if `finance-charges-finished-ride` scaffolded it first; 6a)
- **Covers:** Add Payment Method
- **Done when:** `payments/package-info.java` `@BoundedContext(name = "Payments")`; `api/usecases/VerifyPaymentMethod`, `api/queries/PaymentMethodsQuery` records; `PaymentProcessor` port + simulated adapter in `infra/adapters/secondary/paymentprocessoracl` (declines `tok_declined*`, records held/released pre-authorizations); the existing `api/events/PaymentMethodInvalidated` kept; `PaymentsArchitectureTest` green.

### S3: Verification and current card in Payments
- **Status:** done
- **Context:** Payments
- **Skill:** ddd-tactical-building-blocks (test-first, checkpoint loop off per D1)
- **Covers:** R1, R2, R3, E1.1, E2.1, E2.2, E3.1, E3.2, Payment Method Verified, Payment Method Declined
- **Done when:** Payments-side unit tests per example (pre-authorize PLN 1.00 and release; declined isn't current; newest verified is current; declined keeps the current one) pass; `./mvnw test` green.

### S4: Verify Payment Method use case, query and REST
- **Status:** done
- **Context:** Payments
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Add Payment Method, Payment Method Verified, Payment Method Declined
- **Done when:** `VerifyPaymentMethodUseCase`; `POST /payments/payment-methods`; `GET /payments/riders/{riderId}/payment-methods`; in-memory repository with `Version`; wiring in `payments/infra/config`; `./mvnw test` green.

### S5: Tag the Accounts canvas
- **Status:** done
- **Context:** Accounts
- **Skill:** none (this skill: apply the canvas diff above)
- **Covers:** R1, R2, R3
- **Done when:** the Accounts canvas changes above are applied.

### S6: Add Payment Method command and the Payments ACL in Accounts
- **Status:** done
- **Context:** Accounts
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Add Payment Method
- **Done when:** prerequisite `rider-provides-personal-data` S4 done; `api/usecases/AddPaymentMethod` record; port `PaymentMethodVerifier`; adapter `infra/adapters/secondary/paymentsctxacl/RestPaymentMethodVerifier` with its own request/response records (no import from `payments`); `AccountsArchitectureTest` green.

### S7: The payment step in Accounts
- **Status:** done
- **Context:** Accounts
- **Skill:** ddd-tactical-building-blocks (test-first, checkpoint loop off per D1)
- **Covers:** R1, R2, R3, E1.1, E1.2, E2.1, E2.2, E3.1, E3.2
- **Done when:** unit tests: verified completes the step, the precondition rejects before personal data without calling the verifier (E1.2), declined keeps the step, an activated account stays activated on either result; `./mvnw test` green.

### S8: Add Payment Method use case and REST
- **Status:** done
- **Context:** Accounts
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Add Payment Method
- **Done when:** `AddPaymentMethodUseCase` (load, check step, call `PaymentMethodVerifier`, record, save, publish); `POST /accounts/riders/{riderId}/payment-methods`; `./mvnw test` green.

### S9: Acceptance tests for onboarding with a card
- **Status:** done
- **Context:** Accounts, Payments
- **Skill:** none (this skill)
- **Covers:** E1.1, E1.2, E2.1, E2.2
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/RiderAddsPaymentMethodSpecTest.java`, `@VelocitySpringTest`, REST into Accounts; the Accounts → Payments hand-off is asserted on Payments' side (`GET /payments/riders/{riderId}/payment-methods`, simulated processor: PLN 1.00 held and released, none for E1.2), wrapped in Awaitility as for every cross-context assertion; `./mvnw test` green.

### S10: Acceptance tests for replacing the card after activation
- **Status:** done
- **Context:** Accounts, Payments
- **Skill:** none (this skill)
- **Covers:** E3.1, E3.2
- **Done when:** prerequisite `rider-signs-user-agreement` S4 (Sign User Agreement endpoint) done; tests activate Anna over REST, add `tok_mc_5555` / `tok_declined_0002`, and assert Payments' current card with Awaitility; `./mvnw test` green.

## Acceptance tests

| Example | Test | Payments-side test (Payments wave, 2026-11-08) |
|---|---|---|
| E1.1 | `RiderAddsPaymentMethodSpecTest#e1_1_card_passes_pre_authorization_and_becomes_current` | `velocity/payments/RiderAddsPaymentMethodSpecTest#e1_1_card_passes_pre_authorization_and_becomes_current` |
| E1.2 | `RiderAddsPaymentMethodSpecTest#e1_2_card_rejected_before_personal_data` | none (no Payments behaviour) |
| E2.1 | `RiderAddsPaymentMethodSpecTest#e2_1_declined_card_keeps_rider_at_payment_step` | `velocity/payments/RiderAddsPaymentMethodSpecTest#e2_1_declined_card_keeps_rider_at_payment_step` |
| E2.2 | `RiderAddsPaymentMethodSpecTest#e2_2_second_card_verified_after_decline` | `velocity/payments/RiderAddsPaymentMethodSpecTest#e2_2_second_card_verified_after_decline` |
| E3.1 | `RiderAddsPaymentMethodSpecTest#e3_1_new_verified_card_replaces_current_card` | `velocity/payments/RiderAddsPaymentMethodSpecTest#e3_1_new_verified_card_replaces_current_card` |
| E3.2 | `RiderAddsPaymentMethodSpecTest#e3_2_declined_card_keeps_current_card` | `velocity/payments/RiderAddsPaymentMethodSpecTest#e3_2_declined_card_keeps_current_card` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1, S3, S4, S5, S7, S8, S9 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethods.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethodsTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/payments/api/usecases/VerifyPaymentMethodUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/payments/infra/adapters/primary/PaymentsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/RiderAddsPaymentMethodSpecTest.java` (Payments side), `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountOnboardingTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/api/usecases/AddPaymentMethodUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/infra/adapters/secondary/paymentsctxacl/RestPaymentMethodVerifier.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/infra/adapters/primary/AccountsController.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/RiderAddsPaymentMethodSpecTest.java` (Accounts) |
| R1 / E1.2 | S5, S7, S8, S9 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountOnboardingTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/api/usecases/AddPaymentMethodUseCase.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/RiderAddsPaymentMethodSpecTest.java` (Accounts) |
| R2 / E2.1 | S1, S3, S5, S7, S9 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethods.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethodsTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/RiderAddsPaymentMethodSpecTest.java` (Payments side), `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountOnboardingTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/infra/adapters/secondary/paymentsctxacl/RestPaymentMethodVerifier.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/RiderAddsPaymentMethodSpecTest.java` (Accounts) |
| R2 / E2.2 | S1, S3, S5, S7, S9 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethods.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethodsTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/RiderAddsPaymentMethodSpecTest.java` (Payments side), `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountOnboardingTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/RiderAddsPaymentMethodSpecTest.java` (Accounts) |
| R3 / E3.1 | S1, S3, S7, S10 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethods.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethodsTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/RiderAddsPaymentMethodSpecTest.java` (Payments side), `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountOnboardingTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/RiderAddsPaymentMethodSpecTest.java` (Accounts) |
| R3 / E3.2 | S1, S3, S7, S10 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethods.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethodsTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/RiderAddsPaymentMethodSpecTest.java` (Payments side), `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountOnboardingTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/RiderAddsPaymentMethodSpecTest.java` (Accounts) |

## Risks and open questions

- CONTRACT GAP: Payments has no query showing a rider's current card or a card's status, yet E2.1 ("Anna has no current card"), E3.1/E3.2 (which card is current) and `finance-blocks-rider-with-invalid-card` E1.1/E3.1 need it. Planned as `GET /payments/riders/{riderId}/payment-methods` → `{riderId, currentPaymentMethodId?, paymentMethods: [{paymentMethodId, cardToken, status: "VERIFIED"|"DECLINED"|"REPLACED"|"INVALIDATED"}]}` (`200` with an empty list for an unknown rider).
- Spec Q4 (several cards, choose which is charged) open: the plan keeps exactly one current card (DE-5).
- Spec Q5 (limit on declined attempts) open: no limit planned.
- Spec Q6 (replace the card during onboarding, after verification but before signing) open: Accounts' onboarding order would reject it once the payment step is complete; `finance-blocks-rider-with-invalid-card` (DE-93) needs it for a blocked onboarding rider. Recorded there as a decision; flag for review.
- The pre-authorization's release is asserted on the simulated processor bean (test reads it); a real processor adapter would need its own contract test.
- Coordination: `finance-charges-finished-ride` also touches Payments (Charge uses the current card from S3). Its plan should list this plan's S4 as a prerequisite.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (split follows the canvases and the CF relationship; Accounts can say no only to the onboarding order, Payments owns verified/current; upstream Payments built first; R3 tests deferred to after activation exists; CONTRACT GAP recorded).
- 2026-11-08 (Payments wave, autonomous run): S1-S4 built. Decisions: the query result record is named `RiderPaymentMethodsView` (not `RiderPaymentMethods`) because the aggregate is `RiderPaymentMethods`; the domain event `PaymentMethodReplaced` was added next to Verified/Declined (no published record). The planned acceptance tests live in Accounts (S9, S10, later wave); Payments' own Spring tests with the same method names are in `velocity/payments/RiderAddsPaymentMethodSpecTest` (REST into Payments). S5-S10 stay todo for the Accounts wave.
- 2026-11-08 (Accounts wave, autonomous run): Accounts steps built; context plan and TDD log in `docs/bounded-contexts/velocity/accounts/plan.md`. Decisions taken there: the aggregate `Account` calls the `PaymentMethodVerifier` port itself (`addPaymentMethod(card).at(now).using(verifier)`), so the onboarding order is checked before Payments is asked; the api result of Rental Eligibility is `RentalEligibilityView` (the domain enum is `RentalEligibility`); block notifications come from the policy `NotifyRiderWhenBlockedPolicy` on Account Blocked; `POST .../blocks` accepts only `OUTSTANDING_DEBT` and only `DELETE .../blocks/OUTSTANDING_DEBT` exists (the card block is lifted only by a verified card, DE-6). The planned Accounts tests are in `velocity/accounts/RiderAddsPaymentMethodSpecTest` (REST into Accounts, Payments side asserted with Awaitility on `GET /payments/riders/{riderId}/payment-methods` and the simulated processor). Q6 answered by `finance-blocks-rider-with-invalid-card` D7: a card can be added whenever the personal data step is complete.

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: Payments steps S1-S4 done
- Canvas tagged; `velocity/payments` scaffolded (package-info, api records, `PaymentProcessor` port, simulated processor in `paymentprocessoracl`, `PaymentsArchitectureTest`); `RiderPaymentMethods` built test-first; `VerifyPaymentMethodUseCase`, `POST /payments/payment-methods`, `GET /payments/riders/{riderId}/payment-methods`, in-memory repository with `Version`. Details and TDD log: `docs/bounded-contexts/velocity/payments/plan.md`.

### 2026-11-08: Accounts steps done
- Accounts module (`velocity/accounts`) built test-first; acceptance tests in `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/`. Details and TDD log: `docs/bounded-contexts/velocity/accounts/plan.md`. All steps done, full suite green.
