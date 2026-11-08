package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/// Payments in a box: verifies every card except those it was told to decline, and records what it was asked.
class FakePaymentMethodVerifier implements PaymentMethodVerifier {

    final List<CardToken> asked = new ArrayList<>();
    private final Set<CardToken> declined = new HashSet<>();

    void declines(CardToken card) {
        declined.add(card);
    }

    static PaymentMethodId referenceOf(CardToken card) {
        return new PaymentMethodId("pm-" + card.value());
    }

    @Override
    public CardVerification verify(RiderId rider, CardToken card) {
        asked.add(card);
        return declined.contains(card) ? CardVerification.declined() : CardVerification.verifiedAs(referenceOf(card));
    }
}
