---
name: finance-blocks-rider-with-invalid-card
intent: finance-blocks-rider-with-invalid-card
spec: finance-blocks-rider-with-invalid-card
system: velocity
status: done
date: 2026-11-08
spec_date: 2026-11-08
---

# Implementation plan: Finance wants a rider whose card became invalid blocked from new rentals

<!-- Reviewers: edit this file or answer in the conversation, then set status to "approved" (or say so).
     Invoking implementation-planner again resumes from this file. -->

## Summary

The intent spans **Payments** (upstream) and **Accounts** (downstream) along
`Payments [U,OHS,PL]->[D,CF] Accounts : PaymentMethodVerification`. The processor's notification arrives in Payments
(`POST /payments/processor-notifications/card-invalidations`); Payments invalidates the card and, only when it was the
rider's current card and not already invalid, publishes `PaymentMethodInvalidated`. Accounts listens through an ACL
listener, places the *invalid card* block and sends one block notification per reason. A newly verified card, added
through Accounts' existing Add Payment Method, lifts only that block (Account Unblocked, naming the reason and whether
the rider is eligible). This plan also builds Accounts' **Rental Eligibility** query (activated and unblocked), which
`rider-reserves-bike` and `rider-unlocks-reserved-bike` consume. Payments first, then Accounts.

## Placement

| Spec element | Owner | Why |
|---|---|---|
| R1 When the rider's current card is invalidated, the rider is blocked for *invalid card* | Payments (card invalidated, no current card) + Accounts (block) | Payments canvas "An invalidated card is reported to VeloCity" (DE-72); Accounts canvas "An invalid card blocks the account" and CML "Account blocks and unblocks". E1.2 (ride continues): Rentals, by design: Rentals asks eligibility only at reserve and unlock (DE-80) and has no block listener, so nothing is built in Rentals |
| R2 The rider gets a block notification for every block reason placed | Accounts (+ Payments for the duplicate report in E2.3) | Accounts canvas outbound "Block Notification" (DE-7, DE-94); Payments doesn't re-publish an invalidation of an already invalid card (DE-94) |
| R3 Invalidating a card that is no longer the rider's current card changes nothing else | Payments | Payments canvas "An old card being invalidated isn't reported" (DE-73): only Payments knows which card is current |
| R4 A newly verified card lifts the *invalid card* block, and only that block | Accounts | Accounts canvas "A newly verified card lifts the card block", "A debt unblock doesn't lift a card block" (DE-6, DE-94); blocks are Accounts' state |
| Command: Invalidate Payment Method | Payments | processor notifications enter through Payments (Payments canvas inbound "Card Invalidated"; CML `PaymentProcessor -> Payments`) |
| Command: Add Payment Method | Accounts | as placed in `rider-adds-payment-method`; this intent adds the unblocking to it |
| Event: Payment Method Invalidated | Payments | Payments decides the card is unusable; published language in `payments/api/events` (listener: Accounts) |
| Event: Account Blocked | Accounts | the block is placed in Accounts; no listener outside Accounts |
| Event: Account Unblocked | Accounts | the block is lifted in Accounts; no listener outside Accounts |
| Event: Payment Method Verified | Payments | as in `rider-adds-payment-method` |
| Event: Payment Method Declined | Payments | as in `rider-adds-payment-method` |

**Outcome:** several existing contexts. Hand-offs:
1. PaymentProcessor (external) → Payments: processor notification over REST into Payments' primary adapter (ACL toward the external party, CML `[D,ACL]`).
2. Payments (upstream) → Accounts (downstream, CF): event `PaymentMethodInvalidated {riderId, paymentMethodId, reason, occurredAt}`, policy "whenever a rider's payment method is invalidated, block the account for *invalid card*", in the `@Async @EventListener` listener `accounts/infra/adapters/primary/paymentsctxacl`.
3. Accounts (downstream) → Payments (upstream): Verify Payment Method, existing from `rider-adds-payment-method`; Accounts lifts the block on a verified result.

All directions match the CML.

## Context map changes

- None.

## Translation

| Spec term | Context | Context term | Where translated |
|---|---|---|---|
| Card (as reported by the processor) | Payments | card token | REST body `cardToken` |
| Invalidated (expired, cancelled, reported lost) | Payments | Invalidated, reason `EXPIRED` / `CANCELLED` / `REPORTED_LOST` | REST body `reason` |
| Payment Method Invalidated | Accounts | "the rider has no usable payment method" → Block reason *invalid card* | listener in `paymentsctxacl` translates the event into `BlockAccount(riderId, INVALID_CARD)` |
| Block | Accounts | Block with reason `INVALID_CARD` / `OUTSTANDING_DEBT` | Account Status `blocks` |
| Eligible | Accounts | Rental eligibility (activated and no block) | Rental Eligibility query |
| Block notification | Accounts | Block notification (reason, what to do next) | port `RiderNotifications` |

## Contract mapping

| Spec element | Context | Code |
|---|---|---|
| Command: Invalidate Payment Method | Payments | `InvalidatePaymentMethodUseCase` + `InvalidatePaymentMethod(cardToken, reason)`; `POST /payments/processor-notifications/card-invalidations` `{cardToken, reason}` → `200` (also for an unknown or already invalid card: nothing happens) |
| Event: Payment Method Invalidated | Payments | domain event `PaymentMethodInvalidated` (domain), mapped by the use case to the existing `payments/api/events/PaymentMethodInvalidated(riderId, paymentMethodId, reason, occurredAt)` and published through `ApplicationEventPublisher`; only for the current card's first invalidation |
| Event: Payment Method Invalidated (consumer) | Accounts | `PaymentMethodInvalidatedListener` in `accounts/infra/adapters/primary/paymentsctxacl` (`@AntiCorruptionLayer`, `@Async @EventListener`) → `BlockAccountUseCase` |
| Event: Account Blocked | Accounts | `BlockAccountUseCase` + `BlockAccount(riderId, reason)`; domain event `AccountBlocked(riderId, reason, blockedAt)`; no `api/events` record. Also exposed as `POST /accounts/riders/{riderId}/blocks` `{reason}` (the Settlement contract), used here to set up the debt block in E2.2/E4.2 |
| (block notification) | Accounts | secondary port `RiderNotifications.sendBlockNotification(riderId, reason)`, simulated adapter in `accounts/infra/adapters/secondary/messagingprovideracl` that records what it sent (CONTRACT GAP below); sent once per newly placed reason |
| Command: Add Payment Method | Accounts | existing `AddPaymentMethodUseCase` (from `rider-adds-payment-method` S8): on a verified result the account lifts *invalid card* |
| Event: Account Unblocked | Accounts | domain event `AccountUnblocked(riderId, liftedReason, eligibleNow, unblockedAt)`; no `api/events` record |
| Event: Payment Method Verified | Payments | as in `rider-adds-payment-method` (domain event `PaymentMethodVerified`) |
| Event: Payment Method Declined | Payments | as in `rider-adds-payment-method` (domain event `PaymentMethodDeclined`) |
| (eligibility) | Accounts | `RentalEligibilityQuery(riderId)` + `RentalEligibility(riderId, eligible, reason)` in `accounts/api/queries`, handler; `GET /accounts/riders/{riderId}/rental-eligibility` → reason `ELIGIBLE` / `ONBOARDING_INCOMPLETE` / `BLOCKED` / `UNKNOWN_RIDER`; Account Status `blocks` filled |

## Canvas changes

### Payments (`docs/bounded-contexts/velocity/payments/canvas.md`)

- **Business decisions, tag existing:** "An invalidated card is reported to VeloCity" `(spec: finance-blocks-rider-with-invalid-card#E1.1, #E1.3)`; "An old card being invalidated isn't reported" `(spec: finance-blocks-rider-with-invalid-card#E3.1)`.
- **Business decisions, add:**
  - `A second report for the same card isn't reported again (spec: finance-blocks-rider-with-invalid-card#E2.3)`: Given tok_visa_4242 was invalidated at 08:00, When the processor reports tok_visa_4242 expired again at 08:30, Then nothing changes and Payment Method Invalidated isn't published again (DE-94).

### Accounts (`docs/bounded-contexts/velocity/accounts/canvas.md`)

- **Business decisions, tag existing:** "An invalid card blocks the account" `(spec: finance-blocks-rider-with-invalid-card#E1.1, #E2.1)`; "A newly verified card lifts the card block" `(spec: finance-blocks-rider-with-invalid-card#E4.1)`.
- **Business decisions, add:**
  - `A block during onboarding (spec: finance-blocks-rider-with-invalid-card#E1.3)`: Given Carla's card was verified and she hasn't signed, When Payments reports her payment method invalidated, Then she is blocked for *invalid card*, stays incomplete, and once she signs she is activated but not eligible (DE-93).
  - `A block doesn't end a running ride (spec: finance-blocks-rider-with-invalid-card#E1.2)`: Given Anna is riding B-17, When she is blocked for *invalid card*, Then her ride continues (a block prevents new rentals).
  - `A second reason, a second notification (spec: finance-blocks-rider-with-invalid-card#E2.2)`: Given Ben is blocked for *outstanding debt*, When Payments reports his payment method invalidated, Then he is blocked for *invalid card* as well and gets a second block notification.
  - `The same reason twice changes nothing (spec: finance-blocks-rider-with-invalid-card#E2.3)`: Given Anna is blocked for *invalid card*, When the block is placed again, Then nothing changes and no notification is sent (DE-94).
  - `A card unblock doesn't lift a debt block (spec: finance-blocks-rider-with-invalid-card#E4.2)`: Given Ben is blocked for *invalid card* and *outstanding debt*, When he adds tok_visa_2222 and Payments verifies it, Then *invalid card* is lifted (Account Unblocked, eligible: no) and he stays blocked for *outstanding debt*.
  - `A declined card keeps the block (spec: finance-blocks-rider-with-invalid-card#E4.3)`: Given Anna is blocked for *invalid card*, When she adds tok_declined_0002 and Payments declines it, Then she stays blocked.
- **Ubiquitous language, change:** **Block**: "a reason why a rider can't start rentals" (not only an activated rider; DE-93).
- **Outbound, change:** Block Notification goes to the messaging provider through `messagingprovideracl`.

## Decisions

| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | TDD checkpoint loop | **off** | the trainer explicitly asked for a fully autonomous run (2026-11-08); the tactical skill's pushback is recorded in `docs/bounded-contexts/velocity/conversation.md`. Domain still built test-first, one example per cycle. |
| D2 | Aggregate style | state-based | simplest; no requirement for an event history |
| D3 | ACLs | one per outbound query/command, in `@AntiCorruptionLayer` packages, calling REST via `RestClient` + `VelocityServiceUrls`; inbound events via `@Async @EventListener` ACL listeners | integration-contracts.md: `accounts/infra/adapters/primary/paymentsctxacl` (listener), `accounts/infra/adapters/secondary/messagingprovideracl` (notifications) |
| D4 | Persistence | in-memory repositories with `Version` optimistic locking, after the domain is green | memory "aggregate persistence after domain" |
| D5 | Who filters old and duplicate invalidations | Payments: it publishes only for the current card's first invalidation (R3, E2.3). Accounts' block is also idempotent per reason (R2) as a second guard | Payments alone knows which card is current (canvas, DE-73); idempotent consumers survive at-least-once delivery |
| D6 | Onboarding isn't undone by an invalid card | the payment step stays complete; Carla can still sign (E1.3) and the block keeps her ineligible | DE-93; `rider-signs-user-agreement` D5 |
| D7 | Adding a card while blocked during onboarding | Add Payment Method is accepted whenever the personal data step is complete (not only after activation), so a blocked onboarding rider can lift the block (DE-93) | answers `rider-adds-payment-method` Q6 for the blocked case; flag for review |
| D8 | Rental Eligibility and the generic Block Account built here | `RentalEligibilityQuery` + `GET .../rental-eligibility` and `POST /accounts/riders/{riderId}/blocks` are built in S8; `rider-reserves-bike`, `rider-unlocks-reserved-bike` and `finance-retries-failed-charge` consume them (their plans should list S8 as a prerequisite; Unblock for debt stays with the debt intents) | eligibility reads activation and blocks, both finished here; the block use case is the same for both reasons |

## Steps

### S1: Tag the Payments canvas
- **Status:** done
- **Context:** Payments
- **Skill:** none (this skill: apply the canvas diff above)
- **Covers:** R1, R2, R3
- **Done when:** the Payments canvas changes above are applied.

### S2: Add Invalidate Payment Method to the Payments api
- **Status:** done
- **Context:** Payments
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Invalidate Payment Method, Payment Method Invalidated
- **Done when:** prerequisite `rider-adds-payment-method` S4 done; `api/usecases/InvalidatePaymentMethod` record; the existing `api/events/PaymentMethodInvalidated` matches integration-contracts.md; `PaymentsArchitectureTest` green.

### S3: Card invalidation rules in Payments
- **Status:** done
- **Context:** Payments
- **Skill:** ddd-tactical-building-blocks (test-first, checkpoint loop off per D1)
- **Covers:** R1, R2, R3, E1.1, E1.3, E2.3, E3.1, Payment Method Invalidated
- **Done when:** unit tests: the current card's invalidation leaves no current card and raises the event; a second report changes nothing (E2.3); an old card is recorded invalid without the event (E3.1); `./mvnw test` green.

### S4: Processor notification endpoint and event publishing
- **Status:** done
- **Context:** Payments
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Invalidate Payment Method, Payment Method Invalidated
- **Done when:** `InvalidatePaymentMethodUseCase` maps the domain event to `api/events/PaymentMethodInvalidated` and publishes it; `POST /payments/processor-notifications/card-invalidations`; `./mvnw test` green.

### S5: Tag the Accounts canvas
- **Status:** done
- **Context:** Accounts
- **Skill:** none (this skill: apply the canvas diff above)
- **Covers:** R1, R2, R4
- **Done when:** the Accounts canvas changes above are applied.

### S6: Blocks, notifications and eligibility in the Accounts api
- **Status:** done
- **Context:** Accounts
- **Skill:** bounded-context-scaffolder (update mode, 6a)
- **Covers:** Account Blocked, Account Unblocked
- **Done when:** prerequisites `rider-signs-user-agreement` S4 and this plan's S4 done; `api/usecases/BlockAccount`, `api/queries/RentalEligibilityQuery` + `RentalEligibility` records; port `RiderNotifications` with the recording adapter in `infra/adapters/secondary/messagingprovideracl`; `AccountsArchitectureTest` green (Accounts uses only `payments.api.events`).

### S7: Block, notification and unblock rules in Accounts
- **Status:** done
- **Context:** Accounts
- **Skill:** ddd-tactical-building-blocks (test-first, checkpoint loop off per D1)
- **Covers:** R1, R2, R4, E1.1, E1.3, E2.1, E2.2, E2.3, E4.1, E4.2, E4.3, Account Blocked, Account Unblocked
- **Done when:** unit tests per example: block during onboarding and after activation; eligibility = activated and unblocked; one notification per newly placed reason, none for a repeat; a verified card lifts only *invalid card* with `eligibleNow`; a declined card keeps it; `./mvnw test` green.

### S8: Listener, block use case, eligibility query and unblock on a verified card
- **Status:** done
- **Context:** Accounts
- **Skill:** bounded-context-scaffolder (6b)
- **Covers:** Add Payment Method, Account Blocked, Account Unblocked
- **Done when:** `PaymentMethodInvalidatedListener` (`@Async @EventListener`) → `BlockAccountUseCase` (sends notifications for newly placed reasons); `POST /accounts/riders/{riderId}/blocks`; `AddPaymentMethodUseCase` lifts *invalid card* on a verified result and publishes `AccountUnblocked`; `RentalEligibilityQueryHandler` + `GET /accounts/riders/{riderId}/rental-eligibility`; Account Status shows `blocks`; `./mvnw test` green.

### S9: Acceptance tests for the card block
- **Status:** done
- **Context:** Accounts, Payments
- **Skill:** none (this skill)
- **Covers:** E1.1, E1.3, E2.1, E2.2, E2.3, E3.1, E4.1, E4.2, E4.3
- **Done when:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/FinanceBlocksRiderWithInvalidCardSpecTest.java`, `@VelocitySpringTest`; each test onboards its own rider over REST, posts the processor notification to Payments, and asserts the Payments → Accounts hand-off with Awaitility (Account Status `blocks`, Rental Eligibility, recorded notifications, `GET /payments/riders/{riderId}/payment-methods`); E2.3/E3.1 assert that nothing more arrives after a short wait; `./mvnw test` green.

### S10: Acceptance test for a running ride during a block
- **Status:** done
- **Context:** Accounts, Payments, Rentals
- **Skill:** none (this skill)
- **Covers:** E1.2
- **Done when:** prerequisites: `rider-unlocks-reserved-bike` plan's REST/acceptance steps done (a ride can start over `POST /rentals/bikes/{bikeId}/unlock`) and `fleet-manager-adds-bike-to-fleet` (bike B-17 exists); the test starts Anna's ride on its own bike, posts the `REPORTED_LOST` notification, waits with Awaitility until Anna is blocked, then asserts `GET /rentals/riders/{riderId}/active-rental` still returns `kind: SESSION`; `./mvnw test` green.

## Acceptance tests

| Example | Test | Payments-side test (Payments wave, 2026-11-08) |
|---|---|---|
| E1.1 | `FinanceBlocksRiderWithInvalidCardSpecTest#e1_1_current_card_invalidated_blocks_rider` | `velocity/payments/FinanceBlocksRiderWithInvalidCardSpecTest#e1_1_current_card_invalidated_blocks_rider` |
| E1.2 | `FinanceBlocksRiderWithInvalidCardSpecTest#e1_2_running_ride_continues_after_block` | none (no Payments behaviour) |
| E1.3 | `FinanceBlocksRiderWithInvalidCardSpecTest#e1_3_onboarding_rider_blocked_and_not_eligible_after_signing` | Payments behaves as in E1.1 (covered there) |
| E2.1 | `FinanceBlocksRiderWithInvalidCardSpecTest#e2_1_rider_gets_one_block_notification` | none (no Payments behaviour) |
| E2.2 | `FinanceBlocksRiderWithInvalidCardSpecTest#e2_2_second_reason_sends_second_notification` | none (no Payments behaviour) |
| E2.3 | `FinanceBlocksRiderWithInvalidCardSpecTest#e2_3_repeated_invalidation_changes_nothing` | `velocity/payments/FinanceBlocksRiderWithInvalidCardSpecTest#e2_3_repeated_invalidation_changes_nothing` |
| E3.1 | `FinanceBlocksRiderWithInvalidCardSpecTest#e3_1_old_card_invalidated_changes_nothing_else` | `velocity/payments/FinanceBlocksRiderWithInvalidCardSpecTest#e3_1_old_card_invalidated_changes_nothing_else` |
| E4.1 | `FinanceBlocksRiderWithInvalidCardSpecTest#e4_1_verified_card_lifts_invalid_card_block` | verification as in `RiderAddsPaymentMethodSpecTest#e3_1_new_verified_card_replaces_current_card` |
| E4.2 | `FinanceBlocksRiderWithInvalidCardSpecTest#e4_2_verified_card_keeps_debt_block` | none (no Payments behaviour) |
| E4.3 | `FinanceBlocksRiderWithInvalidCardSpecTest#e4_3_declined_card_keeps_block` | verification as in `RiderAddsPaymentMethodSpecTest#e3_2_declined_card_keeps_current_card` |

## Traceability

| Spec ID | Step | Files |
|---|---|---|
| R1 / E1.1 | S1, S3, S4, S5, S7, S8, S9 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethods.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethodsTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/payments/api/usecases/InvalidatePaymentMethodUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/payments/infra/adapters/primary/paymentprocessoracl/ProcessorNotificationsController.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/payments/infra/adapters/secondary/IntegrationEventsPublisher.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/FinanceBlocksRiderWithInvalidCardSpecTest.java` (Payments side), `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountBlocksTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/infra/adapters/primary/paymentsctxacl/PaymentMethodInvalidatedListener.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/api/usecases/BlockAccountUseCase.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/api/queries/RentalEligibilityQueryHandler.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/FinanceBlocksRiderWithInvalidCardSpecTest.java` (Accounts) |
| R1 / E1.2 | S5, S10 | `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/FinanceBlocksRiderWithInvalidCardSpecTest.java` (`e1_2_running_ride_continues_after_block`); Rentals asks eligibility only at reserve and unlock (`src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/api/usecases/ReserveBikeUseCase.java`, `UnlockBikeUseCase.java`) |
| R1 / E1.3 | S1, S3, S5, S7, S9 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethods.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethodsTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountBlocksTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/FinanceBlocksRiderWithInvalidCardSpecTest.java` (Accounts) |
| R2 / E2.1 | S5, S7, S8, S9 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/NotifyRiderWhenBlockedPolicy.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/NotifyRiderWhenBlockedPolicyTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/infra/adapters/secondary/messagingprovideracl/RecordingRiderNotifications.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/FinanceBlocksRiderWithInvalidCardSpecTest.java` (Accounts) |
| R2 / E2.2 | S5, S7, S8, S9 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountBlocksTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/NotifyRiderWhenBlockedPolicy.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/NotifyRiderWhenBlockedPolicyTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/infra/adapters/secondary/messagingprovideracl/RecordingRiderNotifications.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/FinanceBlocksRiderWithInvalidCardSpecTest.java` (Accounts) |
| R2 / E2.3 | S1, S3, S5, S7, S9 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethods.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethodsTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/FinanceBlocksRiderWithInvalidCardSpecTest.java` (Payments side), `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountBlocksTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/FinanceBlocksRiderWithInvalidCardSpecTest.java` (Accounts) |
| R3 / E3.1 | S1, S3, S9 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethods.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/domain/paymentmethods/RiderPaymentMethodsTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/payments/FinanceBlocksRiderWithInvalidCardSpecTest.java` (Payments side), `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/FinanceBlocksRiderWithInvalidCardSpecTest.java` (Accounts) |
| R4 / E4.1 | S5, S7, S8, S9 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountBlocksTest.java`, `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/api/usecases/AddPaymentMethodUseCase.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/FinanceBlocksRiderWithInvalidCardSpecTest.java` (Accounts) |
| R4 / E4.2 | S5, S7, S8, S9 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountBlocksTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/FinanceBlocksRiderWithInvalidCardSpecTest.java` (Accounts) |
| R4 / E4.3 | S5, S7, S9 | `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/Account.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/domain/account/AccountBlocksTest.java`, `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/FinanceBlocksRiderWithInvalidCardSpecTest.java` (Accounts) |

## Risks and open questions

- CONTRACT GAP: the block notification goes to an external messaging provider that integration-contracts.md doesn't describe. Planned as Accounts' port `RiderNotifications` with a simulated adapter in `accounts/infra/adapters/secondary/messagingprovideracl` that records `{riderId, reason, sentAt}` (tests read the bean; no REST).
- CONTRACT GAP (shared with `rider-adds-payment-method`): `GET /payments/riders/{riderId}/payment-methods` for "Anna has no current card" (E1.1) and "4242 recorded as invalid" (E3.1).
- CONTRACT GAP (minor): the processor notification's `reason` values aren't listed; planned as `EXPIRED | CANCELLED | REPORTED_LOST`, and `200` for an unknown card token (nothing happens).
- Spec Q5 (running ride continues) is answered in E1.2 by not building anything in Rentals; S10 depends on Rentals plans and is the last step.
- D7 widens Add Payment Method to blocked onboarding riders; `rider-adds-payment-method` Q6 for unblocked onboarding riders stays open.
- D8 builds Rental Eligibility and `POST .../blocks` here; if the orchestrator schedules `rider-reserves-bike` or `finance-retries-failed-charge` first, move these parts to that plan instead of building them twice.
- Async hand-off: tests share one Spring context; every assertion of a cross-context effect uses Awaitility and its own rider ids and card tokens.

## Review notes

- 2026-11-08: draft written (autonomous run); self-review against plan-critic.md checklist: AGREE (placement follows both canvases and the CF relationship; old/duplicate filtering in Payments, blocks in Accounts; Payments before Accounts; Accounts tactical step holds 3 rules; E1.2 deferred to Rentals' delivery; CONTRACT GAPs recorded).
- 2026-11-08 (Payments wave, autonomous run): S1-S4 built. Decisions: the domain distinguishes `PaymentMethodInvalidated` (current card, mapped to `api.events.PaymentMethodInvalidated` by `IntegrationEventsPublisher`, not by the use case) from `FormerPaymentMethodInvalidated` (old card, not published); the processor notification endpoint sits in the primary ACL package `infra/adapters/primary/paymentprocessoracl` (notifications are the processor's language); an unknown `reason` answers `400`; if several riders ever added the same token, each is invalidated. Payments-side Spring tests in `velocity/payments/FinanceBlocksRiderWithInvalidCardSpecTest`. S5-S10 stay todo for the Accounts (and Rentals) waves.
- 2026-11-08 (Accounts wave, autonomous run): Accounts steps built; context plan and TDD log in `docs/bounded-contexts/velocity/accounts/plan.md`. Decisions taken there: the aggregate `Account` calls the `PaymentMethodVerifier` port itself (`addPaymentMethod(card).at(now).using(verifier)`), so the onboarding order is checked before Payments is asked; the api result of Rental Eligibility is `RentalEligibilityView` (the domain enum is `RentalEligibility`); block notifications come from the policy `NotifyRiderWhenBlockedPolicy` on Account Blocked; `POST .../blocks` accepts only `OUTSTANDING_DEBT` and only `DELETE .../blocks/OUTSTANDING_DEBT` exists (the card block is lifted only by a verified card, DE-6). S10 (E1.2) stays todo until Rentals can start a ride. Eligibility before activation answers `ONBOARDING_INCOMPLETE` even when blocked; after activation `BLOCKED`. The listener ignores (logs) riders Accounts doesn't know and retries an optimistic-locking conflict up to 3 times.
- 2026-11-08 (Rentals wave, autonomous run): Rentals wave: S10 built: `FinanceBlocksRiderWithInvalidCardSpecTest#e1_2_running_ride_continues_after_block` starts Anna's ride over Rentals' REST API, reports the card lost to Payments, awaits the block in Accounts and asserts the ride is still Anna's active rental (`kind: SESSION`).

## Changelog

### 2026-11-08: planned
- Built from spec dated 2026-11-08.

### 2026-11-08: Payments steps S1-S4 done
- Canvas tagged; `InvalidatePaymentMethod` command + use case; current-card / old-card / repeated-report rules test-first in `RiderPaymentMethods`; `POST /payments/processor-notifications/card-invalidations`; `PaymentMethodInvalidated` published. TDD log: `docs/bounded-contexts/velocity/payments/plan.md`.

### 2026-11-08: Accounts steps done
- Accounts module (`velocity/accounts`) built test-first; acceptance tests in `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/`. Details and TDD log: `docs/bounded-contexts/velocity/accounts/plan.md`.

### 2026-11-08: Rentals wave (autonomous run)
- Rentals steps built test-first (TDD checkpoint loop off, D1); per-cycle log in `docs/bounded-contexts/velocity/rentals/plan.md`. Files and tests in the Traceability table; full suite green.
