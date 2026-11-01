package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import org.joda.money.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.ChargeResult;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.ChargeRider;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.ChargeRiderUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.ChargeRetryPolicy;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.RideId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementEvent;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.repository.InMemorySettlementRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Settlement use case examples")
class SettlementUseCasesTest {

    private static final Instant PRICED_AT = Instant.parse("2026-11-01T10:00:00Z");
    private static final Money FEE = Money.parse("EUR 4.50");

    private final ScriptedPayments payments = new ScriptedPayments();
    private final RecordingPublisher publisher = new RecordingPublisher();
    private final MutableClock clock = new MutableClock(PRICED_AT);
    private SettleRideUseCase settleRide;
    private RetryDueChargeAttemptsUseCase retryDue;

    @BeforeEach
    void setUp() {
        var repository = new InMemorySettlementRepository();
        var attemptCharge = new AttemptChargeUseCase(repository, payments, publisher, clock);
        settleRide = new SettleRideUseCase(repository, attemptCharge, new ChargeRetryPolicy(3, Duration.ofMinutes(15)));
        retryDue = new RetryDueChargeAttemptsUseCase(repository, attemptCharge, clock);
    }

    // @formatter:off
    @DisplayName(
        """
         given Payments declines every charge,
         when George's ride is priced and retries run 15 and 30 minutes later,
         then Payments is charged 3 times and debt is recorded
        """
    )
    // @formatter:on
    @Test
    void test0() {
        // given
        payments.willReturn(new ChargeResult.Failed("card expired"), new ChargeResult.Failed("card expired"),
                            new ChargeResult.Failed("card expired"));

        // when
        settleRide.settle(new RideId("ride01"), new RiderId("george"), FEE, PRICED_AT);
        clock.advance(Duration.ofMinutes(10));
        retryDue.retryDue();
        clock.advance(Duration.ofMinutes(5));
        retryDue.retryDue();
        clock.advance(Duration.ofMinutes(15));
        retryDue.retryDue();
        clock.advance(Duration.ofHours(1));
        retryDue.retryDue();

        // then
        assertThat(payments.received).hasSize(3);
        assertThat(publisher.events).filteredOn(SettlementEvent.ChargeAttemptFailed.class::isInstance).hasSize(2);
        assertThat(publisher.events).filteredOn(SettlementEvent.DebtRecorded.class::isInstance).hasSize(1);
    }

    // @formatter:off
    @DisplayName(
        """
         given Payments accepts the charge,
         when the same Ride Priced event is received twice,
         then George is charged only once
        """
    )
    // @formatter:on
    @Test
    void test1() {
        // given
        payments.willReturn(new ChargeResult.Succeeded("charge01"), new ChargeResult.Succeeded("charge02"));

        // when
        settleRide.settle(new RideId("ride01"), new RiderId("george"), FEE, PRICED_AT);
        settleRide.settle(new RideId("ride01"), new RiderId("george"), FEE, PRICED_AT);

        // then
        assertThat(payments.received).hasSize(1);
        assertThat(publisher.events).filteredOn(SettlementEvent.RiderCharged.class::isInstance).hasSize(1);
    }

    private static class ScriptedPayments implements ChargeRiderUseCase {

        private final Deque<ChargeResult> results = new ArrayDeque<>();
        private final List<ChargeRider> received = new ArrayList<>();

        void willReturn(ChargeResult... scripted) {
            results.addAll(List.of(scripted));
        }

        @Override
        public ChargeResult charge(ChargeRider command) {
            received.add(command);
            return results.removeFirst();
        }
    }

    private static class RecordingPublisher implements DomainEventPublisher {

        private final List<DomainEvent> events = new ArrayList<>();

        @Override
        public void publish(DomainEvents domainEvents) {
            domainEvents.stream().forEach(events::add);
        }

        @Override
        public void publish(DomainEvent event) {
            events.add(event);
        }
    }

    private static class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
