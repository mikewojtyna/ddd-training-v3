/**
 * This package contains classes responsible for communication with the Settlement bounded context (upstream,
 * OHS/PL; Debt Recovery is a conformist downstream, see the DebtCollection relationship in the CML). It works as an
 * ACL. This is the only package where Settlement's published language may be used. Settlement does not exist in code
 * yet, so its events are represented by placeholder records here.
 */
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.settlementctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
