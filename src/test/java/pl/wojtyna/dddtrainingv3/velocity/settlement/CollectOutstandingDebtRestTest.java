package pl.wojtyna.dddtrainingv3.velocity.settlement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl.SimulatedPaymentProcessor;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.RetryFailedChargesUseCase;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.SettlementDriver.at;

/// Settlement's side of `docs/plans/collections-attempts-debt-collection.md` S2: *Collect Outstanding Debt* over
/// `POST /settlement/riders/{riderId}/debt-collections`, as Debt Recovery calls it (Settlement canvas decisions "A
/// collected debt is cleared and the account unblocked" and "A failed collection leaves the debt").
@VelocitySpringTest
@DisplayName("Settlement offers Collect Outstanding Debt to Debt Recovery")
class CollectOutstandingDebtRestTest {

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
    private SimulatedPaymentProcessor processor;

    private SettlementDriver settlement;
    private final String suffix = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        settlement = new SettlementDriver(restClient, urls, eventBus, multicaster, clock, retryFailedCharges);
        clock.setTo(at("10:00"));
    }

    @AfterEach
    void tearDown() {
        settlement.stop();
    }

    // @formatter:off
    @DisplayName(
        """
         Canvas: given Anna's outstanding debt is PLN 5.00,
         when Debt Recovery asks to collect it and Payments charges PLN 5.00 successfully,
         then Anna's debt is PLN 0.00 (Outstanding Debt Cleared) and Settlement unblocks Anna for outstanding debt
        """
    )
    // @formatter:on
    @Test
    void collected_debt_is_cleared_and_account_unblocked() {
        // given
        var anna = annaOwing5Pln();
        var visa = "tok_visa_" + suffix;
        settlement.verifyCard(anna, visa);
        clock.setTo(at("12:00"));

        // when
        var answer = settlement.collect(anna);

        // then
        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.collected()).isTrue();
        assertThat(answer.collectedAmount()).isEqualTo("5.00");
        assertThat(answer.outstandingDebt()).isEqualTo("0.00");
        assertThat(processor.capturesOf(visa)).singleElement()
                                              .satisfies(capture -> assertThat(capture.amount()).isEqualByComparingTo(new BigDecimal("5.00")));
        assertThat(settlement.debtsClearedOf(anna)).hasSize(1);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(settlement.accountOf(anna).blocks()).isEmpty());
    }

    // @formatter:off
    @DisplayName(
        """
         Canvas: given Anna's outstanding debt is PLN 5.00,
         when the collection charge fails,
         then Anna still owes PLN 5.00 and stays blocked
        """
    )
    // @formatter:on
    @Test
    void failed_collection_leaves_debt_and_block() {
        // given
        var anna = annaOwing5Pln();
        clock.setTo(at("12:00"));

        // when
        var answer = settlement.collect(anna);

        // then
        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.collected()).isFalse();
        assertThat(answer.collectedAmount()).isEqualTo("0.00");
        assertThat(answer.outstandingDebt()).isEqualTo("5.00");
        assertThat(settlement.debtsClearedOf(anna)).isEmpty();
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> assertThat(settlement.accountOf(anna).blocks()).containsExactly("OUTSTANDING_DEBT"));
    }

    @Test
    @DisplayName("a rider who owes nothing: nothing collected, outstanding debt 0.00 (Debt Recovery reads it as succeeded)")
    void nothing_to_collect_answers_zero_debt() {
        // given
        var eva = settlement.registeredRider("eva");

        // when
        var answer = settlement.collect(eva);

        // then
        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.collected()).isFalse();
        assertThat(answer.outstandingDebt()).isEqualTo("0.00");
    }

    private String annaOwing5Pln() {
        var anna = settlement.registeredRider("anna");
        settlement.verifyCard(anna, "tok_nofunds_" + suffix);
        clock.setTo(at("10:30"));
        var s1 = "S-1-" + suffix;
        settlement.ride5PlnEnded(s1, anna, "10:00", "10:30");
        settlement.awaitRide(s1, "PENDING", 1);
        settlement.retryFailedChargesAt("10:35");
        settlement.awaitRide(s1, "PENDING", 2);
        settlement.retryFailedChargesAt("10:40");
        settlement.awaitRide(s1, "UNPAID", 3);
        assertThat(settlement.balanceOf(anna).outstandingDebt()).isEqualTo("5.00");
        await().atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> assertThat(settlement.accountOf(anna).blocks()).containsExactly("OUTSTANDING_DEBT"));
        return anna;
    }
}
