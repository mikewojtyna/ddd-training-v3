/// Anti-corruption layer towards Settlement's *Collect Outstanding Debt* resource
/// (`POST /settlement/riders/{riderId}/debt-collections`; CML `Settlement [U,S,PL]->[D,C] DebtRecovery`, Customer/
/// Supplier with Debt Recovery downstream and conformist to Settlement's published language). The only package where
/// Settlement's REST resources appear; it translates them into Debt Recovery's port `DebtCollection`.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.settlementctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
