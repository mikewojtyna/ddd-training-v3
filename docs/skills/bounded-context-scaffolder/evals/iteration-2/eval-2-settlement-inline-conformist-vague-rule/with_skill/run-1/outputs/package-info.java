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
/// - **Record debt after the 3rd failed charge attempt**: given a priced ride of 12.50 PLN with 2 failed charge
///   attempts, when the 3rd charge attempt fails, then an Outstanding Debt of 12.50 PLN is recorded and Debt Recorded
///   is published. Enforced by `Settlement`, see `SettlementTest#thirdFailedChargeAttemptRecordsOutstandingDebt`.
/// - **A successful retry settles the ride**: given a priced ride of 12.50 PLN with 1 failed charge attempt, when the
///   2nd charge attempt succeeds, then the ride is settled and no Outstanding Debt is recorded. Enforced by
///   `Settlement`, see `SettlementTest#successfulSecondChargeAttemptSettlesRide`.
/// - **When retries are made** (*not modelled yet*): "we retry a few times shortly after the ride". The timing of the
///   2nd and 3rd Charge Attempt is awaiting a strict Given/When/Then example, see the plan.
///
/// ## Open questions
/// - How does Settlement learn the outcome of a Charge Attempt: a synchronous result of Charge Rider, or events from
///   Payments? (plan, Q5)
/// - What happens to a Charge Attempt outcome reported after the ride is settled or the debt is recorded? (plan, Q6)
/// - Does a ride priced at 0 create a Settlement and a charge at all? (plan, Q4)
///
/// Source canvas: `docs/bounded-contexts/velocity/settlement/canvas.md`
@BoundedContext(name = "Settlement")
@Module
package pl.wojtyna.dddtrainingv3.velocity.settlement;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
