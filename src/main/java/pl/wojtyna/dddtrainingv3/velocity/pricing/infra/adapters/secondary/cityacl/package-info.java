/// Anti-corruption layer towards the City, the external provider of the zone map (CML `City [U]->[D,ACL] Pricing`).
///
/// This is the only package where the City's language appears: *approved drop-off areas* (circles with a centre and
/// a radius in metres) that the City may *withdraw* for a time window. `CityZoneMapAdapter` translates it into
/// Pricing's `ZoneMap` (drop zone / unauthorized zone at a position, at a moment).
///
/// The City has no real feed yet: `SimulatedCityZoneMap` stands in for it, configured with *Old Town Square*
/// (52.2297/21.0122, r = 150 m) and *Riverside Park* (52.2400/21.0300, r = 200 m), as integration-contracts.md
/// says. Tests withdraw an area for a window through it.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.cityacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
