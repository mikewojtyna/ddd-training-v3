/**
 * Rentals bounded context. Owns bike availability, reserving and unlocking bikes, rental sessions, station returns
 * and the maximum rental time. Currently, it's implemented as a single module.
 */
@BoundedContext(name = "Rentals")
@Module
package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
