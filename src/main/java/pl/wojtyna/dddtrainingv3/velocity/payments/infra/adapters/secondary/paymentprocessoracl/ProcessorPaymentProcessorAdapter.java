package pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.CardToken;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.ChargeReference;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.PaymentProcessor;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.PreAuthorization;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.ProcessorAnswer;

/// Translates Payments' `PaymentProcessor` into the processor's language: a pre-authorization is an authorization,
/// releasing it is a void, a charge is a capture; any response code but `APPROVED` is a decline.
@SecondaryAdapter
public class ProcessorPaymentProcessorAdapter implements PaymentProcessor {

    private final SimulatedPaymentProcessor processor;

    public ProcessorPaymentProcessorAdapter(SimulatedPaymentProcessor processor) {
        this.processor = processor;
    }

    @Override
    public PreAuthorization preAuthorize(CardToken card, Money amount) {
        var authorization = processor.authorize(card.value(), amount.getAmount(), amount.getCurrencyUnit().getCode());
        return new PreAuthorization(authorization.authorizationId(), answerFor(authorization.responseCode()));
    }

    @Override
    public void release(PreAuthorization preAuthorization) {
        processor.voidAuthorization(preAuthorization.hold());
    }

    @Override
    public ProcessorAnswer charge(CardToken card, Money amount, ChargeReference reference) {
        var capture = processor.capture(card.value(), amount.getAmount(), amount.getCurrencyUnit().getCode(), reference.value());
        return answerFor(capture.responseCode());
    }

    private static ProcessorAnswer answerFor(SimulatedPaymentProcessor.ResponseCode responseCode) {
        return responseCode == SimulatedPaymentProcessor.ResponseCode.APPROVED ? ProcessorAnswer.ACCEPTED : ProcessorAnswer.DECLINED;
    }
}
