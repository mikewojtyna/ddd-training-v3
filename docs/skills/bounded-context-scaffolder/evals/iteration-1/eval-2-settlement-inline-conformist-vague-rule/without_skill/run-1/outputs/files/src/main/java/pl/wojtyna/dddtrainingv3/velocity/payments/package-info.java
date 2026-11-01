/**
 * Payments bounded context. Verifies payment methods and charges them through the external payment processor. So far
 * only the open host service that other contexts use exists (the {@code api} package). The payment processor
 * integration is not implemented yet.
 */
@BoundedContext(name = "Payments")
@Module
package pl.wojtyna.dddtrainingv3.velocity.payments;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
