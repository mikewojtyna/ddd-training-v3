package pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary;

import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.CardToken;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.ChargeReference;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.PaymentMethodId;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.PaymentProcessor;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.PreAuthorization;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.ProcessorAnswer;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethods;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("In-memory rider payment methods repository")
class InMemoryRiderPaymentMethodsRepositoryTest {

    private static final RiderId ANNA = new RiderId("anna");
    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");

    private final InMemoryRiderPaymentMethodsRepository repository = new InMemoryRiderPaymentMethodsRepository();

    private final PaymentProcessor acceptingProcessor = new PaymentProcessor() {
        @Override
        public PreAuthorization preAuthorize(CardToken card, Money amount) {
            return new PreAuthorization("hold", ProcessorAnswer.ACCEPTED);
        }

        @Override
        public void release(PreAuthorization preAuthorization) {
        }

        @Override
        public ProcessorAnswer charge(CardToken card, Money amount, ChargeReference reference) {
            return ProcessorAnswer.ACCEPTED;
        }
    };

    @Test
    @DisplayName("a second copy loaded at the same version can't overwrite the first one's change")
    void concurrentModificationIsRejected() {
        // given
        var created = RiderPaymentMethods.none(ANNA);
        created.verify(new CardToken("tok_visa_4242")).as(new PaymentMethodId("pm-1")).at(NOW).using(acceptingProcessor);
        repository.save(created);
        var first = repository.load(ANNA).orElseThrow();
        var second = repository.load(ANNA).orElseThrow();
        first.verify(new CardToken("tok_mc_5555")).as(new PaymentMethodId("pm-2")).at(NOW).using(acceptingProcessor);
        second.verify(new CardToken("tok_visa_1111")).as(new PaymentMethodId("pm-3")).at(NOW).using(acceptingProcessor);
        repository.save(first);

        // when
        var exception = catchThrowable(() -> repository.save(second));

        // then
        assertThat(exception).isInstanceOf(OptimisticLockingException.class);
        assertThat(repository.paymentMethodsOf("anna").orElseThrow().currentPaymentMethodId()).isEqualTo("pm-2");
    }

    @Test
    @DisplayName("riders are found by a card they added")
    void ridersAreFoundByCard() {
        // given
        var created = RiderPaymentMethods.none(ANNA);
        created.verify(new CardToken("tok_visa_4242")).as(new PaymentMethodId("pm-1")).at(NOW).using(acceptingProcessor);
        repository.save(created);

        // when
        var holding = repository.loadHolding(new CardToken("tok_visa_4242"));

        // then
        assertThat(holding).extracting(RiderPaymentMethods::riderId).containsExactly(ANNA);
        assertThat(repository.loadHolding(new CardToken("tok_other"))).isEmpty();
    }
}
