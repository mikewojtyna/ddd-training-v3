# Accounts bounded context: implementation plan

- **System / package**: `velocity.accounts`
- **Mode**: scaffold
- **Source canvas**: `docs/bounded-contexts/velocity/accounts/canvas.md`
- **Last updated**: 2026-11-08
- **Status**: implemented

## Review notes
- 2026-11-08: approved via docs/plans/person-registers-with-velocity.md, docs/plans/rider-confirms-contact-details.md, docs/plans/rider-provides-personal-data.md, docs/plans/rider-adds-payment-method.md, docs/plans/rider-signs-user-agreement.md, docs/plans/finance-blocks-rider-with-invalid-card.md, docs/plans/finance-retries-failed-charge.md and docs/plans/rider-pays-off-outstanding-debt.md (autonomous run, 2026-11-08). Structural decisions and the TDD-loop choice (off, D1 in every plan) come from those plans.
- Decisions the intent plans left open, taken here (autonomous run): see the Decisions table rows marked *(new)*.

## Canvas summary
Accounts walks a person through onboarding in a fixed order until activation, and places and lifts account blocks
(invalid card, outstanding debt); it answers "may this rider rent right now?". Supporting domain, compliance, custom
built; role: Approver.

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| Modules | 1 module = 1 context | default |
| ACL towards Payments (outbound, Verify Payment Method) | yes: `infra/adapters/secondary/paymentsctxacl/RestPaymentMethodVerifier` over REST, own request/response records | CML `Payments [U,OHS,PL]->[D,CF] Accounts`; the repo's single place for cross-context calls |
| ACL towards Payments (inbound, Payment Method Invalidated) | yes: `infra/adapters/primary/paymentsctxacl/PaymentMethodInvalidatedListener` (`@Async @EventListener`) → `BlockAccountUseCase` | integration-contracts.md |
| ACL towards the messaging provider | yes: port `RiderNotifications`, simulated recording adapter `infra/adapters/secondary/messagingprovideracl/RecordingRiderNotifications` (tests of any context read `sentTo(riderId)`) | CONTRACT GAP of `finance-blocks-rider-with-invalid-card` |
| Boundary exceptions | none | |
| Domain layer | delegated to `ddd-tactical-building-blocks` (always) | |
| TDD checkpoint loop | off (trainer's autonomy request, 2026-11-08, plans' D1) | domain still built test-first, one example per cycle, red seen before green |
| Aggregate style | state-based (plans' D2) | |
| Aggregate | one `Account` per rider | onboarding order, blocks and eligibility all guard the same rider's state |
| Email uniqueness (DE-98) | factory `RiderRegistration` with port `RegisteredEmails` (implemented by the repository); the repository also refuses a second new account with a taken email (race guard) | rule over all accounts can't live in one `Account` (plan `person-registers-with-velocity` D5) |
| Block notification | policy `NotifyRiderWhenBlockedPolicy` (whenever Account Blocked → `RiderNotifications`), routed by `infra/event/AccountsLocalEventsHandler` | one notification per newly placed reason falls out of the aggregate's idempotent `placeBlock` (DE-7, DE-94) |
| Verifying the card *(new)* | the aggregate calls the port: `account.addPaymentMethod(card).at(now).using(verifier)` | the onboarding precondition is checked before Payments is asked (E1.2), inside the domain |
| Adding a card *(new)* | accepted whenever the personal data step is complete (onboarding, after activation, while blocked) | plan `finance-blocks-rider-with-invalid-card` D7; `rider-adds-payment-method` Q6 for an unblocked onboarding rider answered the same way |
| Eligibility order *(new)* | not activated → `ONBOARDING_INCOMPLETE`, else blocked → `BLOCKED`, else `ELIGIBLE` | E1.3: before signing Carla is "incomplete", after signing "blocked" |
| Blocks over REST *(new)* | `POST .../blocks` accepts only `OUTSTANDING_DEBT` (else `400`); only `DELETE .../blocks/OUTSTANDING_DEBT` exists | the *invalid card* block comes from Payments' event and is lifted only by a verified card (DE-6) |
| Unknown rider in the invalidation listener *(new)* | logged and ignored; an `OptimisticLockingException` is retried up to 3 times | Payments' own tests invalidate cards of riders unknown to Accounts; the async listener races with the rider's REST calls |
| Names *(new)* | domain enum `RentalEligibility`, api result `RentalEligibilityView` (plan said `RentalEligibility`); domain events `PaymentMethodAdded` / `PaymentMethodDeclined` in Accounts' language (Payments keeps its own Verified/Declined) | avoid a name clash between domain and api |
| "Today" for the age limit | `LocalDate` of `velocityClock` in `Europe/Warsaw`, passed in by the use case | plan `rider-provides-personal-data` D5 |
| Persistence | `InMemoryAccountRepository` (memento `Account.Snapshot`, `Version` optimistic locking), also implements `RegisteredEmails` and the read model `AccountsReadModel`; views are computed by the restored `Account` | plans' D4 |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Rider app | Command | Register | `RegisterRider` + `RegisterRiderUseCase`; `POST /accounts/riders` → `201 {riderId}` (`AccountsController`) |
| Rider app | Command | Confirm Email | `ConfirmEmail` + `ConfirmEmailUseCase`; `POST /accounts/riders/{riderId}/email-confirmation` |
| Rider app | Command | Verify Phone | `VerifyPhone` + `VerifyPhoneUseCase`; `POST .../phone-verification` |
| Rider app | Command | Provide Personal Data | `ProvidePersonalData` + `ProvidePersonalDataUseCase`; `POST .../personal-data` |
| Rider app | Command | Add Payment Method | `AddPaymentMethod` + `AddPaymentMethodUseCase` → `PaymentMethodAddition`; `POST .../payment-methods` |
| Rider app | Command | Sign User Agreement | `SignUserAgreement` + `SignUserAgreementUseCase`; `POST .../agreement-signature` |
| Rider app | Query | Account Status | `AccountStatusQuery`, `AccountStatus`, `AccountStatusQueryHandler`; `GET /accounts/riders/{riderId}` (`404` unknown) |
| Rentals | Query | Rental Eligibility | `RentalEligibilityQuery`, `RentalEligibilityView`, `RentalEligibilityQueryHandler`; `GET .../rental-eligibility` (`UNKNOWN_RIDER` with `200`) |
| Settlement | Command | Block Account (outstanding debt) | `BlockAccount` + `BlockAccountUseCase`; `POST .../blocks` |
| Settlement | Command | Unblock Account (outstanding debt) | `UnblockAccount` + `UnblockAccountUseCase`; `DELETE .../blocks/OUTSTANDING_DEBT` |
| Payments | Event | Payment Method Invalidated | `PaymentMethodInvalidatedListener` → `BlockAccount(riderId, INVALID_CARD)` |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Verify Payment Method | Command | Payments | `domain/account/PaymentMethodVerifier` | `RestPaymentMethodVerifier` (`infra/adapters/secondary/paymentsctxacl`) |
| Block Notification | Command | Messaging provider | `domain/account/RiderNotifications` | `RecordingRiderNotifications` (`infra/adapters/secondary/messagingprovideracl`, simulated) |
| Account Registered/Activated/Blocked/Unblocked, Email Confirmed, Phone Verified, Personal Data Provided, Payment Method Added/Declined | Event | none outside Accounts | n/a | domain events only (`AccountEvent`) |

### Ubiquitous language
| Term | Code type | Where it appears |
|---|---|---|
| Rider | `RiderId` | domain, api records |
| Account | `Account` (aggregate root) | domain |
| Onboarding step / next onboarding step | `OnboardingStep`, `Account#nextOnboardingStep` | domain, `AccountStatus.onboardingStep` |
| Payment method | `PaymentMethodId`, `CardToken`, `CardVerification` | domain |
| Activated | `Account#isActivated`, `AccountActivated` | domain, `AccountStatus.activated` |
| Block | `BlockReason`, `Account#placeBlock`/`liftBlock` | domain, `AccountStatus.blocks` |
| Rental eligibility | `RentalEligibility`, `Account#rentalEligibility` | domain, `RentalEligibilityView` |

### Business rules → delegated to `ddd-tactical-building-blocks`
| Rule (GWT) | Status | Building block | Test |
|---|---|---|---|
| A person registers with an email address | green | `RiderRegistration` (factory) | `RiderRegistrationTest#personRegistersWithUnusedEmail` |
| One account per email address | green | `RiderRegistration` + `RegisteredEmails` | `RiderRegistrationTest#duplicateEmailIsRejected` |
| A new account waits for the email confirmation | green | `Account` | `AccountOnboardingTest#newAccountIsIncompleteAndWaitsForEmailConfirmation` |
| Onboarding steps are completed in order | green | `Account` | `AccountOnboardingTest#phoneVerificationRejectedBeforeEmailConfirmed`, `#personalDataRejectedBeforeEmailConfirmed`, `#personalDataRejectedBeforePhoneVerified`, `#cardRejectedBeforePersonalData`, `#signingRejectedWithoutVerifiedCard`, `#signingTwiceIsRejected` |
| Email confirmed; twice changes nothing | green | `Account` | `#riderConfirmsEmailWithLink`, `#secondEmailConfirmationChangesNothing` |
| Phone verified after email | green | `Account` | `#riderVerifiesPhoneAfterEmail` |
| Personal data, any residency | green | `Account` | `#riderWithVerifiedPhoneProvidesPersonalData`, `#anyCountryOfResidencyIsAccepted` |
| At least 16 years old | green | `Account` | `#fifteenYearOldIsRejected`, `#riderTurningSixteenTodayIsAccepted`, `#riderBornOnLeapDayTurnsSixteenOnFirstOfMarch` |
| Verified card completes the payment step; declined doesn't | green | `Account` + port `PaymentMethodVerifier` | `#verifiedCardCompletesPaymentStep`, `#declinedCardKeepsRiderAtPaymentStep`, `#secondCardVerifiedAfterDecline` |
| An activated rider can add a new card | green | `Account` | `#activatedRiderCanAddNewCard` |
| Signing activates the account | green | `Account` | `#signingAfterVerifiedCardActivatesAccount`, `#signingTwoWeeksLaterActivatesAccount` |
| Only an activated, unblocked rider is eligible | green | `Account` | `AccountBlocksTest#incompleteAccountIsNotEligible`, `#invalidCardBlocksActivatedRider` |
| An invalid card blocks (also during onboarding) | green | `Account` | `AccountBlocksTest#invalidCardBlocksActivatedRider`, `#onboardingRiderBlockedAndNotEligibleAfterSigning` |
| Second reason placed; same reason twice changes nothing | green | `Account` | `#secondReasonIsPlacedNextToTheFirst`, `#sameReasonTwiceChangesNothing` |
| A notification per placed reason | green | `NotifyRiderWhenBlockedPolicy` (policy) | `NotifyRiderWhenBlockedPolicyTest#riderGetsOneBlockNotification`, `#secondReasonSendsSecondNotification` |
| Verified card lifts only the card block; declined keeps it | green | `Account` | `#verifiedCardLiftsInvalidCardBlock`, `#verifiedCardKeepsDebtBlock`, `#declinedCardKeepsBlock` |
| Cleared debt lifts the debt block; not the card block; nothing to lift changes nothing | green | `Account` | `#clearedDebtLiftsDebtBlock`, `#debtUnblockDoesNotLiftCardBlock`, `#liftingReasonNotInPlaceChangesNothing` |
| A block doesn't end a running ride | waits for Rentals | none in Accounts | `finance-blocks-rider-with-invalid-card` S10 (todo) |

### Rules awaiting GWT
- none

## Deferred until tactical blocks are built
- Nothing left: use cases, query handlers, REST adapter, listener, repository, policy routing and wiring were generated after the domain was green.

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| none | | |

## Files
```
src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/
  package-info.java
  api/usecases/AccountCommands.java (shared load → command → save → publish)
  api/usecases/RegisterRider(.java|UseCase.java), ConfirmEmail(..), VerifyPhone(..), ProvidePersonalData(..),
    AddPaymentMethod(..), PaymentMethodAddition.java, SignUserAgreement(..), BlockAccount(..), UnblockAccount(..)
  api/queries/AccountStatusQuery.java, AccountStatus.java, AccountStatusQueryHandler.java,
    RentalEligibilityQuery.java, RentalEligibilityView.java, RentalEligibilityQueryHandler.java, AccountsReadModel.java
  domain/account/Account.java, AccountEvent.java, AccountRepository.java, RiderRegistration.java,
    NotifyRiderWhenBlockedPolicy.java, PaymentMethodVerifier.java, RiderNotifications.java, RegisteredEmails.java,
    FluentLanguage.java, RiderId.java, EmailAddress.java, PhoneNumber.java, PersonalData.java, CardToken.java,
    PaymentMethodId.java, CardVerification.java, OnboardingStep.java, BlockReason.java, RentalEligibility.java,
    OnboardingStepOutOfOrder.java, RiderTooYoung.java, EmailAlreadyRegistered.java, AccountNotFound.java
  infra/adapters/primary/AccountsController.java
  infra/adapters/primary/paymentsctxacl/package-info.java, PaymentMethodInvalidatedListener.java
  infra/adapters/secondary/InMemoryAccountRepository.java
  infra/adapters/secondary/paymentsctxacl/package-info.java, RestPaymentMethodVerifier.java
  infra/adapters/secondary/messagingprovideracl/package-info.java, RecordingRiderNotifications.java
  infra/event/AccountsLocalEventsHandler.java
  infra/config/AccountsContextConfig.java
src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/
  AccountsArchitectureTest.java, AccountsDriver.java, PaymentsView.java
  PersonRegistersWithVelocitySpecTest.java, RiderConfirmsContactDetailsSpecTest.java, RiderProvidesPersonalDataSpecTest.java,
  RiderAddsPaymentMethodSpecTest.java, RiderSignsUserAgreementSpecTest.java, FinanceBlocksRiderWithInvalidCardSpecTest.java,
  FinanceRetriesFailedChargeSpecTest.java (Accounts side), RiderPaysOffOutstandingDebtSpecTest.java (Accounts side)
  domain/account/AccountOnboardingTest.java, AccountBlocksTest.java, RiderRegistrationTest.java,
    NotifyRiderWhenBlockedPolicyTest.java, FakePaymentMethodVerifier.java, RiderRegistrationFixture.java
  infra/adapters/secondary/InMemoryAccountRepositoryTest.java
```

## Stubs and follow-ups
- The messaging provider is simulated (`RecordingRiderNotifications`); a real provider needs its own adapter.
- `finance-blocks-rider-with-invalid-card` S10 (E1.2, running ride continues) waits for Rentals.
- Settlement's `accountsctxacl` (Block / Unblock over REST) and its end-to-end tests: Settlement wave.
- No CML change needed.

## Changelog
### 2026-11-08: scaffolded and implemented
- Added: everything listed under Files.
- Triggered by: `docs/plans/person-registers-with-velocity.md` (S1-S7), `rider-confirms-contact-details.md` (S1-S5), `rider-provides-personal-data.md` (S1-S5), `rider-adds-payment-method.md` (S5-S10), `rider-signs-user-agreement.md` (S1-S5), `finance-blocks-rider-with-invalid-card.md` (S5-S9), `finance-retries-failed-charge.md` (S2), `rider-pays-off-outstanding-debt.md` (S2-S5).
- Canvas: business decisions tagged with spec IDs, the new decisions of those plans added, UL "Next onboarding step" added, "Block" widened to onboarding riders (DE-93), outbound Block Notification names its port and ACL.

#### TDD log (checkpoint loop off, all implemented by Claude)
Tests were written per rule group against compile-only scaffolding (dumb bodies returning `null`, `false` or
`DomainEvents.empty()`), each red observed as an assertion failure, then made green one increment at a time.

| # | Scenario(s) | Block | Test(s) | Red seen |
|---|---|---|---|---|
| 1 | registration with an unused email | `RiderRegistration#register` | `personRegistersWithUnusedEmail` | yes (no events) |
| 2 | duplicate email rejected | `RiderRegistration` + `RegisteredEmails` | `duplicateEmailIsRejected` | yes (no exception) |
| 3 | new account waits for email; email confirmed; twice changes nothing; phone after email; phone before email rejected | `Account#nextOnboardingStep`, `#confirmEmail`, `#verifyPhone` | `newAccountIsIncompleteAndWaitsForEmailConfirmation`, `riderConfirmsEmailWithLink`, `secondEmailConfirmationChangesNothing`, `riderVerifiesPhoneAfterEmail`, `phoneVerificationRejectedBeforeEmailConfirmed` | yes (null step, no events, no exception) |
| 4 | personal data recorded; before email / before phone rejected; 15 rejected; 16 today accepted; leap-day birthday; any residency | `Account#providePersonalData` | `riderWithVerifiedPhoneProvidesPersonalData`, `personalDataRejectedBeforeEmailConfirmed`, `personalDataRejectedBeforePhoneVerified`, `fifteenYearOldIsRejected`, `riderTurningSixteenTodayIsAccepted`, `riderBornOnLeapDayTurnsSixteenOnFirstOfMarch`, `anyCountryOfResidencyIsAccepted` | yes (no events / no exception) |
| 5 | verified card completes the step; before personal data rejected without asking Payments; declined keeps the step; verified after decline; activated rider adds cards; signing activates; signing without a verified card / twice rejected; signing later | `Account#addPaymentMethod`, `#signUserAgreement`, `#isActivated` | `verifiedCardCompletesPaymentStep`, `cardRejectedBeforePersonalData`, `declinedCardKeepsRiderAtPaymentStep`, `secondCardVerifiedAfterDecline`, `activatedRiderCanAddNewCard`, `signingAfterVerifiedCardActivatesAccount`, `signingRejectedWithoutVerifiedCard`, `signingTwiceIsRejected`, `signingTwoWeeksLaterActivatesAccount` | yes |
| 6 | incomplete account not eligible; lifting a reason not in place | `Account#rentalEligibility`, `#liftBlock` | `incompleteAccountIsNotEligible`, `liftingReasonNotInPlaceChangesNothing` | no: green at once (implied by cycle 5 / degenerate case), kept as specification |
| 7 | invalid card blocks; block during onboarding; second reason; same reason twice | `Account#placeBlock`, `#blocks`, `#rentalEligibility` | `invalidCardBlocksActivatedRider`, `onboardingRiderBlockedAndNotEligibleAfterSigning`, `secondReasonIsPlacedNextToTheFirst`, `sameReasonTwiceChangesNothing` | yes (no events, empty blocks) |
| 8 | verified card lifts the card block (eligible yes / no with debt); declined keeps it; debt unblock; debt unblock keeps card block | `Account#liftBlock`, `#addPaymentMethod` | `verifiedCardLiftsInvalidCardBlock`, `verifiedCardKeepsDebtBlock`, `declinedCardKeepsBlock`, `clearedDebtLiftsDebtBlock`, `debtUnblockDoesNotLiftCardBlock` | yes (no Account Unblocked) |
| 9 | one notification per placed reason | `NotifyRiderWhenBlockedPolicy` | `riderGetsOneBlockNotification`, `secondReasonSendsSecondNotification` | yes (nothing sent) |
| - | `Version`, memento, repository with optimistic locking and email race guard | `Account.Snapshot`, `InMemoryAccountRepository` | `InMemoryAccountRepositoryTest` | written after the domain (persistence after domain) |
