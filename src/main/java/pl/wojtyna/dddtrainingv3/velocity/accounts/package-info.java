/// # Accounts bounded context
///
/// ## Purpose
/// Earns the rider's trust once: walks a person through onboarding in a fixed order until their account is activated,
/// and keeps track of whether an activated rider may currently rent, by placing and lifting account blocks with a
/// reason. Everyone else asks Accounts one question: *may this rider rent right now?*
///
/// ## Strategic classification
/// - Domain: supporting
/// - Business model: compliance
/// - Evolution: custom built
///
/// ## Domain roles
/// - Approver: decides whether a rider may start rentals (activated and not blocked).
///
/// ## Ubiquitous language
/// - **Rider**: a person with an account at VeloCity.
/// - **Account**: a rider's identity; it is *incomplete*, *activated*, or activated and *blocked*.
/// - **Onboarding**: the fixed sequence of steps from registration to activation; a step is accepted only when every
///   earlier step is complete.
/// - **Onboarding step**: registered → email confirmed → phone verified (with the phone number given at that step) →
///   personal data provided → payment method added and verified → user agreement signed → activated.
/// - **Next onboarding step**: the first onboarding step the account hasn't completed yet.
/// - **Payment method**: the rider's card, known to us only by the reference Payments gives us.
/// - **Verified**: Payments pre-authorized the card successfully.
/// - **Activated**: every onboarding step is complete; set automatically when the agreement is signed.
/// - **Block**: a reason why a rider can't start rentals: *invalid card* or *outstanding debt*. An account can hold
///   both at once.
/// - **Rental eligibility**: activated and holding no block.
///
/// ## Business decisions
/// Enforced by the aggregate `Account` (`domain/account`, tests `AccountOnboardingTest`, `AccountBlocksTest`), the
/// factory `RiderRegistration` (`RiderRegistrationTest`) and the policy `NotifyRiderWhenBlockedPolicy`
/// (`NotifyRiderWhenBlockedPolicyTest`).
/// - **A person registers with an email address**: given nobody registered with anna@example.com, when Anna registers
///   with it, then she has an incomplete account (Account Registered). See `RiderRegistrationTest#personRegistersWithUnusedEmail`.
/// - **One account per email address** (DE-98): see `RiderRegistrationTest#duplicateEmailIsRejected`.
/// - **A new account waits for the email confirmation**: see `AccountOnboardingTest#newAccountIsIncompleteAndWaitsForEmailConfirmation`.
///   An incomplete account waits without a deadline: nothing in Accounts is time-driven (acceptance test only).
/// - **Onboarding steps are completed in order**: given Anna hasn't confirmed her email, when she verifies her phone or
///   provides her personal data, then the step is rejected (`OnboardingStepOutOfOrder`). See
///   `#phoneVerificationRejectedBeforeEmailConfirmed`, `#personalDataRejectedBeforeEmailConfirmed`,
///   `#personalDataRejectedBeforePhoneVerified`, `#cardRejectedBeforePersonalData` (Payments isn't asked),
///   `#signingRejectedWithoutVerifiedCard`, `#signingTwiceIsRejected`.
/// - **Following the emailed link confirms the email; twice changes nothing** (DE-98): see `#riderConfirmsEmailWithLink`,
///   `#secondEmailConfirmationChangesNothing`.
/// - **Typing the SMS code verifies the phone**: see `#riderVerifiesPhoneAfterEmail`.
/// - **A rider with a verified phone provides personal data, any residency**: see
///   `#riderWithVerifiedPhoneProvidesPersonalData`, `#anyCountryOfResidencyIsAccepted`.
/// - **A rider must be at least 16 years old** (DE-2): given today is 2026-11-08, born 2010-11-09 is rejected
///   (`RiderTooYoung`), born 2010-11-08 accepted; born on 29 February turns 16 on 1 March (DE-98). See
///   `#fifteenYearOldIsRejected`, `#riderTurningSixteenTodayIsAccepted`, `#riderBornOnLeapDayTurnsSixteenOnFirstOfMarch`.
/// - **A verified card completes the payment step; a declined one doesn't** (DE-3): see `#verifiedCardCompletesPaymentStep`,
///   `#declinedCardKeepsRiderAtPaymentStep`, `#secondCardVerifiedAfterDecline`.
/// - **An activated rider can add a new card** (DE-5): see `#activatedRiderCanAddNewCard`.
/// - **Signing the agreement activates the account** (DE-4): see `#signingAfterVerifiedCardActivatesAccount`,
///   `#signingTwoWeeksLaterActivatesAccount`.
/// - **Only an activated, unblocked rider is eligible to rent**: see `AccountBlocksTest#incompleteAccountIsNotEligible`,
///   `#invalidCardBlocksActivatedRider`.
/// - **An invalid card blocks the account**: given Anna is activated, when Payments reports her payment method
///   invalidated, then she is blocked for *invalid card*. See `AccountBlocksTest#invalidCardBlocksActivatedRider`; during
///   onboarding too (DE-93), see `#onboardingRiderBlockedAndNotEligibleAfterSigning`.
/// - **A second reason is placed next to the first; the same reason twice changes nothing** (DE-94): see
///   `#secondReasonIsPlacedNextToTheFirst`, `#sameReasonTwiceChangesNothing`.
/// - **A block notification for every block reason placed** (DE-7): see
///   `NotifyRiderWhenBlockedPolicyTest#riderGetsOneBlockNotification`, `#secondReasonSendsSecondNotification`.
/// - **A newly verified card lifts the card block, and only that block** (DE-6): see
///   `AccountBlocksTest#verifiedCardLiftsInvalidCardBlock`, `#verifiedCardKeepsDebtBlock`, `#declinedCardKeepsBlock`.
/// - **A cleared debt lifts the debt block; a debt unblock doesn't lift a card block** (DE-6, DE-94): see
///   `#clearedDebtLiftsDebtBlock`, `#debtUnblockDoesNotLiftCardBlock`, `#liftingReasonNotInPlaceChangesNothing`.
/// - **A block doesn't end a running ride**: nothing to enforce in Accounts (Rentals asks eligibility only at reserve
///   and unlock); its acceptance test waits for Rentals.
///
/// ## Assumptions
/// - DE-1: email and phone confirmations are trusted facts from the messaging provider; no codes are checked here.
/// - DE-4: there is no separate activation step.
/// - DE-7: one block notification per block placed.
/// - DE-94: placing a block that is already in place changes nothing and sends no second notification; Account
///   Unblocked is recorded each time one block reason is lifted.
/// - DE-93: a card invalidated during onboarding still places the block.
///
/// ## Verification metrics
/// - Share of registrations that reach activation, and the step where riders drop out.
/// - Number of rentals rejected because of eligibility (should be low: the app checks it up front).
///
/// ## Open questions
/// - Can a rider close their account? (Not in the vision.)
/// - Should a block for invalid card be placed when the rider has no running rental only? (We block at once.)
///
/// Source canvas: `docs/bounded-contexts/velocity/accounts/canvas.md`
@BoundedContext(name = "Accounts")
@Module
package pl.wojtyna.dddtrainingv3.velocity.accounts;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
