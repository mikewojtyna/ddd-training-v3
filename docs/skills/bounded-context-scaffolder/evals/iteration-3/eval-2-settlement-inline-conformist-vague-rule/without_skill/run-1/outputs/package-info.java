/**
 * Settlement bounded context. Once a ride is priced, it charges the rider through their payment method so that every
 * ride eventually gets paid. Failed charges are retried a few times shortly after the ride; if all attempts fail, the
 * unpaid amount is recorded as outstanding debt and handed over to DebtRecovery.
 * <p>
 * Upstream: Pricing (Ride Priced) and Payments (Charge Rider). Settlement conforms to Payments' open host service and
 * uses it directly, without an anti-corruption layer. Downstream: DebtRecovery (Debt Recorded).
 */
@BoundedContext(name = "Settlement")
@Module
package pl.wojtyna.dddtrainingv3.velocity.settlement;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
