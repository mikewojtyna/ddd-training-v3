package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.fee;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.Tariff;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffId;

import java.util.List;

/// The explained total fee of a session: one hourly fee per billing hour, the return fee and the penalty, all from
/// one tariff.
@ValueObject
public record SessionFee(TariffId tariffId,
                         List<Money> hourlyFees,
                         ReturnFeeKind returnFeeKind,
                         Money returnFee,
                         Money penalty) {

    public SessionFee {
        hourlyFees = List.copyOf(hourlyFees);
    }

    public int billingHours() {
        return hourlyFees.size();
    }

    public Money hourlyFeesTotal() {
        return Money.total(Tariff.PLN, hourlyFees);
    }

    /// Total fee = hourly fees + return fee + penalty.
    public Money total() {
        return hourlyFeesTotal().plus(returnFee).plus(penalty);
    }
}
