/**
 * Pricing bounded context. Turns a finished ride into a fair, predictable fee: a free period encourages short
 * trips, long trips get progressively more expensive, and the rider always pays the price list that was valid when
 * the ride started. Core domain, revenue business model, custom built. Currently, it's implemented as a single module.
 */
@BoundedContext(name = "Pricing")
@Module
package pl.wojtyna.dddtrainingv3.velocity.pricing;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
