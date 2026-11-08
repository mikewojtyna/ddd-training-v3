/// Anti-corruption layer for what Rentals publishes: `rentals.api.events.RentalSessionEnded` is translated here into
/// Settlement's own command, *Charge Ride* (CML `Rentals [U,PL]->[D,CF] Settlement`). The only place Rentals'
/// published language enters Settlement.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.primary.rentalsctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
