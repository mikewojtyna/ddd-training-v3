package pl.wojtyna.dddtrainingv3.velocity.debtrecovery;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.AttemptDueCollectionsUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.messagingacl.SimulatedPaymentReminders;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl.SimulatedPaymentProcessor;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.RetryFailedChargesUseCase;

import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/// Real beans for Debt Recovery's acceptance tests (each concrete class is a `@VelocitySpringTest`); every test gets
/// a fresh `UnpaidRideFixture` and sets the `MutableClock` explicitly.
abstract class DebtRecoverySpecTestSupport {

    static final String KATE = "kate";

    @Autowired
    private RestClient restClient;
    @Autowired
    private VelocityServiceUrls urls;
    @Autowired
    private ApplicationEventPublisher eventBus;
    @Autowired
    private ApplicationEventMulticaster multicaster;
    @Autowired
    private MutableClock clock;
    @Autowired
    private RetryFailedChargesUseCase retryFailedCharges;
    @Autowired
    private AttemptDueCollectionsUseCase attemptDueCollections;
    @Autowired
    private SimulatedPaymentReminders reminders;
    @Autowired
    private SimulatedPaymentProcessor processor;

    UnpaidRideFixture fixture;

    @BeforeEach
    void setUpFixture() {
        clock.setTo(UnpaidRideFixture.at("11-08T09:00"));
        fixture = new UnpaidRideFixture(restClient, urls, eventBus, multicaster, clock, retryFailedCharges,
                                        attemptDueCollections, reminders, processor);
    }

    @AfterEach
    void stopFixture() {
        fixture.stop();
    }

    /// The value stays as expected for a while: nothing arriving asynchronously changes it.
    static <T> void staysSo(Supplier<T> actual, T expected) {
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> assertThat(actual.get()).isEqualTo(expected));
    }
}
