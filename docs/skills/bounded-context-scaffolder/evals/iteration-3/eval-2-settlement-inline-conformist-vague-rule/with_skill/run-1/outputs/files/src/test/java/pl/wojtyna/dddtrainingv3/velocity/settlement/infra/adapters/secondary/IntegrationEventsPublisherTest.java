package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary;

import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.DebtRecorded;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.OutstandingDebt;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.RideId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementEvent;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Settlement published language")
class IntegrationEventsPublisherTest {

    // @formatter:off
    @DisplayName(
        """
         given an Outstanding Debt of 12.50 PLN recorded for ride R-1,
         when Settlement publishes its integration events,
         then Debt Recorded of 12.50 PLN for ride R-1 is published
        """
    )
    // @formatter:on
    @Test
    void outstandingDebtIsPublishedAsDebtRecorded() {
        // given
        List<Object> published = new ArrayList<>();
        var publisher = new IntegrationEventsPublisher(published::add);
        var recorded = new SettlementEvent.OutstandingDebtRecorded(new RideId("R-1"),
                                                                   new RiderId("rider-1"),
                                                                   new OutstandingDebt(Money.parse("PLN 12.50")));

        // when
        publisher.on(recorded);

        // then
        assertThat(published).containsExactly(new DebtRecorded("R-1", "rider-1", new BigDecimal("12.50"), "PLN"));
    }
}
