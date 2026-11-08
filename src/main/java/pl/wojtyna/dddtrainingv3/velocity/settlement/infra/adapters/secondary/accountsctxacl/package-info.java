/// Anti-corruption layer towards Accounts' Open Host Service for *Block Account* / *Unblock Account* with reason
/// *outstanding debt* (`POST /accounts/riders/{riderId}/blocks`, `DELETE /accounts/riders/{riderId}/blocks/OUTSTANDING_DEBT`;
/// CML `Accounts [U,OHS,PL]->[D,ACL] Settlement`). The only package where Accounts' REST resources appear.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.accountsctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
