/// Anti-corruption layer for what Settlement publishes: `settlement.api.events.RentalFeeUnpaid` and
/// `settlement.api.events.OutstandingDebtCleared` are translated here into Debt Recovery's own commands, *Start Debt
/// Recovery* and *Record Debt Cleared* (CML `Settlement [U,S,PL]->[D,C] DebtRecovery`). The only place Settlement's
/// published events enter Debt Recovery.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.primary.settlementctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
