/// Anti-corruption layer towards Rentals. This is the only package where Rentals' published language
/// (*available / withdrawn*) may appear. It translates Fleet Maintenance's `Circulation` (*broken / back in
/// circulation*) into calls on Rentals' `api`, never its domain types.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.rentalsctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
