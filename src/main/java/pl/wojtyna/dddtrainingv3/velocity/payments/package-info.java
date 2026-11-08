/// # Payments bounded context
///
/// ## Purpose
/// Shields VeloCity from the external payment processor: verifies a rider's card with a small pre-authorization,
/// charges amounts to the rider's current card, and tells VeloCity when a card stops being usable. Inside VeloCity a
/// card is simply *verified* or not, and a charge simply *succeeds* or *fails*.
///
/// ## Strategic classification
/// - Domain: generic
/// - Business model: compliance
/// - Evolution: product
///
/// ## Domain roles
/// - Gateway Interchange: translates between VeloCity and the processor's model.
///
/// ## Ubiquitous language
/// - **Payment method**: a rider's card, known by the processor's card token.
/// - **Pre-authorization**: holding PLN 1.00 on the card to prove it works (DE-70), released at once.
/// - **Verified**: the pre-authorization succeeded; the newest verified card is the rider's *current* card (DE-5).
/// - **Invalidated**: the processor told us the card is no longer usable (expired, cancelled, reported lost).
/// - **Charge**: taking an amount from the rider's current card; it *succeeds* or *fails*.
///
/// ## Business decisions
/// All enforced by `RiderPaymentMethods` (aggregate root, `domain/paymentmethods`), see `RiderPaymentMethodsTest`.
/// - **A card that passes pre-authorization is verified and becomes current**: given Anna has no payment method, when
///   her card tok_visa_4242 is verified and the processor accepts the PLN 1.00 pre-authorization, then it is verified,
///   is her current card and the PLN 1.00 is released. See `#cardPassingPreAuthorizationIsVerifiedAndBecomesCurrent`.
/// - **A declined pre-authorization leaves the card unverified**: given Anna has no payment method, when tok_declined_0002
///   is declined, then the verification fails and she still has no current card. See
///   `#declinedPreAuthorizationLeavesCardUnverified`, `#chargeWithoutCurrentCardFailsWithoutCallingProcessor`.
/// - **A card verified after a decline becomes current**: see `#cardVerifiedAfterDeclineBecomesCurrent`.
/// - **A newly verified card replaces the current one** (DE-5): given tok_visa_4242 is current, when tok_mc_5555 is
///   verified, then tok_mc_5555 is current and tok_visa_4242 is no longer charged. See `#newlyVerifiedCardReplacesCurrentOne`.
/// - **A declined card doesn't replace the current one**: see `#declinedCardDoesNotReplaceCurrentOne`.
/// - **A charge goes to the current card**: given tok_visa_4242 is current, when PLN 5.00 is charged for ride S-1 and the
///   processor accepts it, then the charge succeeded. See `#chargeGoesToCurrentCard`.
/// - **A declined charge fails with reason declined**: see `#chargeDeclinedByProcessorFailsWithReasonDeclined`.
/// - **A charge without a current card fails** (DE-71): with reason *no valid payment method*, and the processor isn't
///   called. See `#chargeWithoutCurrentCardFailsWithoutCallingProcessor`, `#invalidatedCurrentCardIsReportedAndLeavesNoCurrentCard`.
/// - **An invalidated card is reported to VeloCity** (DE-72): given tok_visa_4242 is current, when the processor reports
///   it expired, then it is invalidated (Payment Method Invalidated, published) and Anna has no current card. See
///   `#invalidatedCurrentCardIsReportedAndLeavesNoCurrentCard`.
/// - **An old card being invalidated isn't reported** (DE-73): it is recorded as invalidated, Payment Method Invalidated
///   isn't published and the current card stays. See `#oldCardBeingInvalidatedIsNotReported`.
/// - **A second report for the same card isn't reported again** (DE-94): see `#secondReportForSameCardIsNotReportedAgain`;
///   a card Payments doesn't know changes nothing, see `#reportForUnknownCardChangesNothing`.
///
/// ## Assumptions
/// - DE-70: PLN 1.00 pre-authorization.
/// - Until a real processor is connected, a simulated processor (`infra/adapters/secondary/paymentprocessoracl`)
///   declines cards whose token starts with `tok_declined` and fails charges on tokens starting with `tok_nofunds`.
///
/// ## Verification metrics
/// - Share of declined pre-authorizations.
/// - Share of failed charges.
///
/// ## Open questions
/// - Refunds (after billing disputes) are not covered.
///
/// Source canvas: `docs/bounded-contexts/velocity/payments/canvas.md`
@BoundedContext(name = "Payments")
@Module
package pl.wojtyna.dddtrainingv3.velocity.payments;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
