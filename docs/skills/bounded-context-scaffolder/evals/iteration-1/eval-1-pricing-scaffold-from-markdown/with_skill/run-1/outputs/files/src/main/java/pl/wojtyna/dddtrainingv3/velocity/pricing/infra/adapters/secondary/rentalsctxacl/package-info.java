/// Anti-corruption layer towards the Rentals bounded context. This is the only package in Pricing where Rentals'
/// language (classes, APIs or other published language) may appear. It translates Rentals' answers into Pricing's
/// `ReturnCharges`.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.rentalsctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
