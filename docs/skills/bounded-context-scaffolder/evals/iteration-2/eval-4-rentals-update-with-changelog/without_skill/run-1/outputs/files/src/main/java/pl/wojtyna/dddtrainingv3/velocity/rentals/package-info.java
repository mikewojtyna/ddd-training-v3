/**
 * Rentals bounded context. Owns bike availability at stations, renting and returning bikes, and the maximum rental
 * time, so riders can always find and return a bike and bikes are never held forever. Core domain, revenue
 * business model, custom built; domain role: execution model. Currently, it's implemented as a single module.
 */
@BoundedContext(name = "Rentals")
@Module
package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
