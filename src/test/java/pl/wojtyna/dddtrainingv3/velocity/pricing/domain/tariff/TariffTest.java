package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffFixtures.pln;

@DisplayName("Tariff examples")
class TariffTest {

    private static final Instant DECEMBER = Instant.parse("2026-12-01T00:00:00Z");

    // @formatter:off
    @DisplayName(
        """
         given it is 2026-11-08 10:00,
         when Paula publishes tariff T-4 valid from 2026-12-01 00:00 with every price of T-2 except the penalty,
         then the publication is rejected
        """
    )
    // @formatter:on
    @Test
    void tariffWithoutPenaltyIsRejected() {
        // when
        var exception = catchThrowable(() -> new Tariff(new TariffId("T-4"), DECEMBER, Duration.ofMinutes(15),
                                                        pln("3.50"), pln("5.50"), pln("7.50"), pln("9.50"),
                                                        pln("2.50"), pln("45.00"), null));

        // then
        assertThat(exception).isInstanceOf(IncompleteTariffException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given it is 2026-11-08 10:00,
         when Paula publishes tariff T-4 valid from 2026-12-01 00:00 with every price of T-2 but a drop zone fee of PLN -1.00,
         then the publication is rejected, because fees can't be negative (DE-96)
        """
    )
    // @formatter:on
    @Test
    void tariffWithNegativeDropZoneFeeIsRejected() {
        // when
        var exception = catchThrowable(() -> new Tariff(new TariffId("T-4"), DECEMBER, Duration.ofMinutes(15),
                                                        pln("3.50"), pln("5.50"), pln("7.50"), pln("9.50"),
                                                        pln("-1.00"), pln("45.00"), pln("150.00")));

        // then
        assertThat(exception).isInstanceOf(NegativeFeeException.class);
    }
}
