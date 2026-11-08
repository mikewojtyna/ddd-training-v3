/// Anti-corruption layer towards Rentals' Open Host Service (CML `Rentals [U,OHS,PL]->[D,ACL] FleetMaintenance`):
/// *Withdraw Bike*, *Reserve Bike for service*, *Unlock Bike*, *Make Bike Available* and *Bike Availability*. The only
/// package where Rentals' resources, payloads and words appear; it keeps its own request/response records and imports
/// no Rentals classes. Fleet Maintenance never says *broken* to Rentals: a broken bike is a *withdrawn* bike there, a
/// technician a holder of type `TECHNICIAN`. Rentals' rejections (`BikeAlreadyHeld`, `NotReservationHolder`,
/// `BikeNotUnlockedForService`, `UnknownStation`, 404) become Fleet Maintenance's exceptions.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.rentalsctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
