/// Anti-corruption layer towards Accounts for *Rental Eligibility* (`GET /accounts/riders/{riderId}/rental-eligibility`,
/// CML `Accounts [U,OHS,PL]->[D,ACL] Rentals : AccountStatus`). The only package where Accounts' resource and payload
/// appear; it keeps its own response record and imports no Accounts classes. Accounts' reasons (`ONBOARDING_INCOMPLETE`,
/// `BLOCKED`, `UNKNOWN_RIDER`) all translate to Rentals' *not eligible*.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.accountsctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
