/// Anti-corruption layer towards Customer Support's Open Host Service (CML
/// `CustomerSupport [U,OHS,PL]->[D,ACL] FleetMaintenance`): *Resolve Fleet Issue*
/// (`POST /customer-support/incidents/{incidentId}/fleet-resolution`). The only package where Customer Support's
/// resource appears; it imports no Customer Support classes.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.customersupportctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
