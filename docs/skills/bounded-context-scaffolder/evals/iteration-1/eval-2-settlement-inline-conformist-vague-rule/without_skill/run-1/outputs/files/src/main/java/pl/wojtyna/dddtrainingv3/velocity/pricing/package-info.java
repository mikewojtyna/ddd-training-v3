/**
 * Pricing bounded context. Calculates the total fee of a ride. So far only the published language that other
 * contexts use exists (the {@code api} package). The calculation itself is not implemented yet.
 */
@BoundedContext(name = "Pricing")
@Module
package pl.wojtyna.dddtrainingv3.velocity.pricing;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
