package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

import java.math.BigDecimal;
import java.time.Instant;

/// *Start Debt Recovery*: a rider's rental fee stayed unpaid (heard from Settlement's *Rental Fee Unpaid*). Starts a
/// recovery process, or joins the rider's open one. Money in PLN; `outstandingDebt` is the rider's whole debt.
public record StartDebtRecovery(String riderId, String sessionId, BigDecimal unpaidAmount, BigDecimal outstandingDebt,
                                Instant unpaidAt) {
}
