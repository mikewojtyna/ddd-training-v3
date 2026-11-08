package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.joda.money.Money;

/// The external payment processor, in Payments' language. Adapter: `infra/adapters/secondary/paymentprocessoracl`.
@SecondaryPort
public interface PaymentProcessor {

    /// Holds `amount` on the card; the answer says whether the processor accepted it.
    PreAuthorization preAuthorize(CardToken card, Money amount);

    /// Releases an accepted pre-authorization.
    void release(PreAuthorization preAuthorization);

    /// Takes `amount` from the card for `reference`.
    ProcessorAnswer charge(CardToken card, Money amount, ChargeReference reference);
}
