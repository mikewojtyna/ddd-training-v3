/// Anti-corruption layer towards the Rentals bounded context. This is the only package where Rentals' published
/// language (drop zones, unauthorized zones, rental sessions as Rentals models them) may appear. It translates it into
/// Pricing's `ReturnChargesProvider` and `ReturnCharges`.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.rentalsctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
