package pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;

import java.util.Objects;

/**
 * Command to take {@code amount} from the rider's payment method on file. {@code chargeReference} identifies the
 * charge on the caller's side, for example a settlement and its attempt number.
 */
@ValueObject
public record ChargeRider(String riderId, String chargeReference, Money amount) {

    public ChargeRider {
        Objects.requireNonNull(riderId, "riderId");
        Objects.requireNonNull(chargeReference, "chargeReference");
        Objects.requireNonNull(amount, "amount");
    }
}
