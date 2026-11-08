package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.joda.money.Money;

/// Sends a rider a payment reminder for their outstanding debt, part of every collection attempt (DE-40). Wording and
/// channel are out of scope.
@SecondaryPort
public interface PaymentReminders {

    void remind(RiderId rider, Money outstandingDebt);
}
