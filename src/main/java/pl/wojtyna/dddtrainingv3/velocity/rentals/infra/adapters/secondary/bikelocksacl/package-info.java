/// Anti-corruption layer towards the bikes' lock hardware (an external party, not a bounded context). The only place
/// where the hardware's language may appear; Rentals sees only its port `BikeLocks`. The hardware is simulated here.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.bikelocksacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
