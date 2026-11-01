/// # Settlement bounded context
///
/// ## Purpose
/// Once a ride is priced, we charge the rider through their payment method, so that every ride eventually gets paid.
///
/// ## Ubiquitous language
/// - **Settlement**: the process of collecting money for one priced ride.
/// - **Charge Attempt**: one try to take money from the rider's payment method.
/// - **Outstanding Debt**: the unpaid amount left after all attempts fail.
///
/// ## Business decisions
/// - **Debt after the 3rd failed charge attempt** (R1): given a priced ride of 12.50 PLN with 2 failed charge
///   attempts, when the 3rd charge attempt fails, then an Outstanding Debt of 12.50 PLN is recorded and Debt Recorded
///   is published. Enforced by `Settlement`, see `SettlementTest#debtRecordedWhenThirdChargeAttemptFails`; the
///   Debt Recorded translation is checked in `IntegrationEventsPublisherTest#outstandingDebtIsPublishedAsDebtRecorded`.
/// - **Successful retry settles the ride** (R2): given a priced ride of 12.50 PLN with 1 failed charge attempt, when the
///   2nd charge attempt succeeds, then the ride is settled and no Outstanding Debt is recorded. Enforced by
///   `Settlement`, see `SettlementTest#rideSettledWhenSecondChargeAttemptSucceeds`.
/// - **When retries happen** (*not modelled yet*): "If a charge fails, we retry a few times shortly after the ride,
///   then record debt." The number of attempts (3) is now fixed by R1; the timing ("shortly after the ride", suggestion S2) is still
///   awaiting a strict Given/When/Then example, see the plan. Nothing schedules a retry yet.
/// - **A finished Settlement takes no more charge attempts** (*not modelled yet*): suggestion S3 in the plan, awaiting
///   confirmation. The aggregate only guards it as a programming error, without a business rule or test.
///
/// Source canvas: `docs/bounded-contexts/velocity/settlement/canvas.md`
@BoundedContext(name = "Settlement")
@Module
package pl.wojtyna.dddtrainingv3.velocity.settlement;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
