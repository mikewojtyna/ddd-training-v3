package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.primary.settlementctxacl;

import au.com.dius.pact.consumer.MessagePactBuilder;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.consumer.junit5.ProviderType;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Interaction;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.RecordDebtCleared;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.RecordDebtClearedUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.StartDebtRecovery;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.StartDebtRecoveryUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.OutstandingDebtCleared;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.RentalFeeUnpaid;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.DEBT_RECOVERY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.EVENT_JSON;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.INSTANT;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.SETTLEMENT_EVENTS;

/// Consumer side of Settlement's integration events (CML `Settlement [U,S,PL]->[D,C] DebtRecovery`): an unpaid ride
/// starts debt recovery; a cleared debt ends it.
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = SETTLEMENT_EVENTS, providerType = ProviderType.ASYNCH, pactVersion = PactSpecVersion.V4)
@PactDirectory(PACT_DIRECTORY)
@DisplayName("Contract: Settlement → Debt Recovery (RentalFeeUnpaid, OutstandingDebtCleared)")
class SettlementEventsPactTest {

    private static final String AT = "2026-11-08T10:40:00Z";

    @Pact(consumer = DEBT_RECOVERY)
    V4Pact rentalFeeUnpaid(MessagePactBuilder builder) {
        return builder.expectsToReceive("a ride's fee became outstanding debt")
                      .withContent(new PactDslJsonBody().stringType("riderId", "rider-debtor")
                                                        .stringType("sessionId", "S-1")
                                                        .decimalType("amount", new BigDecimal("3.00"))
                                                        .decimalType("outstandingDebt", new BigDecimal("3.00"))
                                                        .stringMatcher("occurredAt", INSTANT, AT))
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = DEBT_RECOVERY)
    V4Pact outstandingDebtCleared(MessagePactBuilder builder) {
        return builder.expectsToReceive("a rider's outstanding debt was cleared")
                      .withContent(new PactDslJsonBody().stringType("riderId", "rider-debtor").stringMatcher("occurredAt", INSTANT, AT))
                      .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "rentalFeeUnpaid")
    void unpaidRideStartsDebtRecovery(V4Interaction.AsynchronousMessage message) throws Exception {
        var startDebtRecovery = mock(StartDebtRecoveryUseCase.class);

        new RentalFeeUnpaidListener(startDebtRecovery).on(EVENT_JSON.readValue(message.contentsAsBytes(), RentalFeeUnpaid.class));

        var command = ArgumentCaptor.forClass(StartDebtRecovery.class);
        verify(startDebtRecovery).handle(command.capture());
        assertThat(command.getValue().riderId()).isEqualTo("rider-debtor");
        assertThat(command.getValue().sessionId()).isEqualTo("S-1");
        assertThat(command.getValue().unpaidAmount()).isEqualByComparingTo("3.00");
        assertThat(command.getValue().outstandingDebt()).isEqualByComparingTo("3.00");
        assertThat(command.getValue().unpaidAt()).isEqualTo(Instant.parse(AT));
    }

    @Test
    @PactTestFor(pactMethod = "outstandingDebtCleared")
    void clearedDebtIsRecorded(V4Interaction.AsynchronousMessage message) throws Exception {
        var recordDebtCleared = mock(RecordDebtClearedUseCase.class);

        new OutstandingDebtClearedListener(recordDebtCleared).on(EVENT_JSON.readValue(message.contentsAsBytes(), OutstandingDebtCleared.class));

        verify(recordDebtCleared).handle(new RecordDebtCleared("rider-debtor", Instant.parse(AT)));
    }
}
