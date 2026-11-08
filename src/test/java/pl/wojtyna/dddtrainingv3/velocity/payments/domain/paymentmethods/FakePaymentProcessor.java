package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

import org.joda.money.Money;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Test double of the payment processor: declines what it is told to decline and records every call.
class FakePaymentProcessor implements PaymentProcessor {

    record Hold(CardToken card, Money amount, String hold) {
    }

    record Charge(CardToken card, Money amount, ChargeReference reference) {
    }

    private final Set<CardToken> decliningPreAuthorization = new HashSet<>();
    private final Set<CardToken> decliningCharges = new HashSet<>();
    final List<Hold> held = new ArrayList<>();
    final List<String> released = new ArrayList<>();
    final List<Charge> charges = new ArrayList<>();

    FakePaymentProcessor declinesPreAuthorizationOf(CardToken card) {
        decliningPreAuthorization.add(card);
        return this;
    }

    FakePaymentProcessor declinesChargesOf(CardToken card) {
        decliningCharges.add(card);
        return this;
    }

    @Override
    public PreAuthorization preAuthorize(CardToken card, Money amount) {
        var hold = "hold-" + (held.size() + 1);
        held.add(new Hold(card, amount, hold));
        return new PreAuthorization(hold, decliningPreAuthorization.contains(card) ? ProcessorAnswer.DECLINED : ProcessorAnswer.ACCEPTED);
    }

    @Override
    public void release(PreAuthorization preAuthorization) {
        released.add(preAuthorization.hold());
    }

    @Override
    public ProcessorAnswer charge(CardToken card, Money amount, ChargeReference reference) {
        charges.add(new Charge(card, amount, reference));
        return decliningCharges.contains(card) ? ProcessorAnswer.DECLINED : ProcessorAnswer.ACCEPTED;
    }
}
