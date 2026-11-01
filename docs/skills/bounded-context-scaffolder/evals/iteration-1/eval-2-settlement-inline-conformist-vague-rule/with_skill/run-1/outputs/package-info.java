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
/// - If a charge fails, we retry a few times shortly after the ride, then record debt.
///   *Not modelled yet*: the rule has no concrete number of attempts or retry times. It waits for a strict
///   Given/When/Then version (see the plan).
///
/// ## Integration
/// - Inbound: **Ride Priced** event from Pricing.
/// - Outbound: **Charge Rider** command to Payments (conformist, no anti-corruption layer, see
///   `infra/adapters/secondary/paymentsctx`), **Debt Recorded** event to DebtRecovery.
///
/// Source canvas: `docs/bounded-contexts/velocity/settlement/canvas.md`
/// Plan: `docs/bounded-contexts/velocity/settlement/plan.md`
@BoundedContext(name = "Settlement")
@Module
package pl.wojtyna.dddtrainingv3.velocity.settlement;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
