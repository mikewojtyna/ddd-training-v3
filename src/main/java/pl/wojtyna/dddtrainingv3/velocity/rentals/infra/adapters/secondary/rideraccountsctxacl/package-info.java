/// Anti-corruption layer towards Rider Accounts. This is the only package where Rider Accounts' published language
/// (*registered / activated / blocked*) may appear. It translates it into Rentals' `BorrowerEligibility`.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.rideraccountsctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
