/// Anti-corruption layer towards Customer Support. This is the only package where Customer Support's language
/// (incidents) may appear. It translates Fleet Maintenance's `FixNotifications` into a call on Customer
/// Support's `api`. Customer Support is upstream; Fleet Maintenance calls its command (plan D9).
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.customersupportctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
