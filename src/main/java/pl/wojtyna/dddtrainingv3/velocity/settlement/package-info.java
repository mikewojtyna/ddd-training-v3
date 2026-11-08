/// # Settlement bounded context
///
/// ## Purpose
/// Makes sure every ride is eventually settled: charges the rider for each finished ride, retries a failed charge a
/// few times, and turns what still can't be collected into the rider's outstanding debt, blocking the account until
/// the debt is cleared. It is the only context that moves money (through Payments).
///
/// ## Strategic classification
/// - Domain: core
/// - Business model: revenue
/// - Evolution: custom built
///
/// ## Domain roles
/// - Execution Model: runs the charge → retry → debt lifecycle per ride.
/// - Enforcer: makes Accounts block riders with outstanding debt and unblock them once it is cleared.
///
/// ## Ubiquitous language
/// - **Ride**: a finished rental session to be settled, identified by the session (`RideSettlement`, `SessionId`).
/// - **Fee**: the total fee Pricing calculated for the ride (port `RideFees`).
/// - **Charge attempt**: one try to charge the fee to the rider's card through Payments (port `RiderCards`).
/// - **Settled**: the fee was charged, or the fee was PLN 0.00 (DE-37).
/// - **Next attempt due at**: 5 minutes after a failed attempt, while fewer than 3 attempts failed (DE-35).
/// - **Rental fee unpaid**: all 3 charge attempts failed (DE-35); the fee becomes outstanding debt.
/// - **Outstanding debt**: the sum the rider owes from unpaid rides (`RiderDebt`).
/// - **Collection**: charging the whole outstanding debt at once, asked for by Debt Recovery or by the rider
///   (`DebtCollection`).
/// - **Ending (when asking Pricing)**: *station return* → *station*, *free-floating return* → *free-floating*,
///   *lock failure* → *lock failure*, *maximum rental time exceeded* → *maximum rental time* (`pricingctxacl`).
/// - **Outstanding debt cleared**: the outstanding debt went down to PLN 0.00.
///
/// ## Business decisions
/// Rides: enforced by the aggregate `RideSettlement` (`domain/ride`), see `RideSettlementTest`.
/// - **A finished ride is charged its fee**: given Anna's ride S-1 ended at 10:30, priced at PLN 5.00, when S-1 is
///   charged and Payments accepts PLN 5.00, then S-1 is settled (Ride Settled). See `#finishedRideChargedItsFeeIsSettled`.
/// - **A free ride is settled without charging** (DE-37): given S-2 is priced at PLN 0.00, when it is settled, then it
///   is settled at once and no charge is due. See `#freeRideIsSettledWithoutCharging`.
/// - **A failed first charge schedules the next attempt**: given S-3 priced at PLN 43.00, when the charge at 10:30 is
///   declined, then attempt 1 of 3 failed and the next is due at 10:35 (Charge Attempt Failed). See
///   `#declinedFirstChargeSchedulesNextAttemptFiveMinutesLater`, `#chargeWithoutValidPaymentMethodSchedulesNextAttempt`.
/// - **A ride is charged at most once** (DE-95): given S-1 is settled, when it ended "again", then no charge is due.
///   See `#settledRideIsNotChargedAgain` (and `InMemorySettlementRepositoriesTest#secondNewRideForSameSessionIsRefused`).
/// - **No retry before it is due / a retry 5 minutes later can settle the ride** (DE-35): see
///   `#noRetryBeforeFiveMinutesPassed`, `#retryFiveMinutesLaterSettlesRide`.
/// - **A failed charge is retried 5 minutes later**: attempt 2 failing at 10:35 makes attempt 3 due at 10:40. See
///   `#secondFailureSchedulesThirdAttempt`.
/// - **After the third failed attempt the fee is unpaid**: see `#thirdFailureMakesFeeUnpaid`.
///
/// Outstanding debt: enforced by the aggregate `RiderDebt` (`domain/debt`), see `RiderDebtTest`, and by policies, see
/// `OutstandingDebtPoliciesTest`.
/// - **An unpaid fee becomes outstanding debt and the rider is blocked**: given Ben owes nothing, when S-3's fee
///   (PLN 43.00) is unpaid, then he owes PLN 43.00 (Rental Fee Unpaid) and Accounts is asked to block him for
///   *outstanding debt*. See `RiderDebtTest#firstUnpaidFeeBecomesOutstandingDebt`, policies
///   `UnpaidRideFeeBecomesOutstandingDebtPolicy` (`#unpaidRideFeeBecomesOutstandingDebt`) and
///   `BlockRiderWithOutstandingDebtPolicy` (`#unpaidFeeBlocksRiderEveryTime`).
/// - **More unpaid fees add up, without a new block notification** (DE-94, Accounts' rule): PLN 43.00 + PLN 3.00 =
///   PLN 46.00. See `RiderDebtTest#furtherUnpaidFeeAddsToOutstandingDebt`, `#furtherUnpaidRideFeeAddsToStoredDebt`.
/// - **A rider pays off / Debt Recovery collects the whole debt in one charge** (DE-36): success clears it
///   (Outstanding Debt Cleared) and Accounts is asked to lift the *outstanding debt* block. See
///   `RiderDebtTest#collectedDebtIsCleared`, `#wholeDebtFromSeveralRidesIsCollectedInOneCharge`,
///   `UnblockRiderWhenDebtClearedPolicy` (`#clearedDebtLiftsDebtBlock`).
/// - **Rides still in their retries aren't part of the payoff** (DE-95): only unpaid rides are debt; a collection
///   clears only the rides it charged. See `RiderDebtTest#collectionClearsOnlyTheRidesItCharged`.
/// - **A failed collection leaves the debt**: see `RiderDebtTest#declinedPaymentLeavesDebt`,
///   `#paymentWithoutValidCardLeavesDebt`.
/// - **Nothing to pay off**: a rider owing PLN 0.00 can't pay off (`NoOutstandingDebtException`). See
///   `RiderDebtTest#riderWithoutDebtHasNothingToPayOff`.
///
/// ## Assumptions
/// - DE-35: 3 attempts, 5 minutes apart.
/// - DE-36: riders can pay their debt from the app.
///
/// ## Verification metrics
/// - Share of rides settled on the first attempt.
/// - Outstanding debt per month.
///
/// ## Open questions
/// - Refunds after a successful billing dispute: Settlement or Payments?
///
/// Source canvas: `docs/bounded-contexts/velocity/settlement/canvas.md`
@BoundedContext(name = "Settlement")
@Module
package pl.wojtyna.dddtrainingv3.velocity.settlement;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
