package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/// The reopening window: 14 calendar days after the rejection day, until the end of the 14th day, city time
/// (DE-54, DE-56; plan rider-reopens-rejected-incident D5: Europe/Warsaw).
@DisplayName("Reopening window examples")
class ReopeningWindowTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    // @formatter:off
    @DisplayName(
        """
         given an incident rejected at 10:00 on 2026-11-09 (Warsaw),
         when the rider wants to reopen it at 23:59 on 2026-11-23 or at 00:00 on 2026-11-24 (Warsaw),
         then the window is open at 23:59 and closed from 00:00 on 2026-11-24
        """
    )
    // @formatter:on
    @Test
    void windowEndsAtTheEndOfTheFourteenthCalendarDayInWarsaw() {
        // given
        var window = ReopeningWindow.afterRejectionAt(Instant.parse("2026-11-09T09:00:00Z"), WARSAW);

        // when / then
        assertThat(window.closesAt()).isEqualTo(Instant.parse("2026-11-23T23:00:00Z"));
        assertThat(window.isOpenAt(Instant.parse("2026-11-23T22:59:00Z"))).isTrue();
        assertThat(window.isOpenAt(Instant.parse("2026-11-23T23:00:00Z"))).isFalse();
    }

    // @formatter:off
    @DisplayName(
        """
         given Zoe rejected an incident on 2026-11-01 (DE-56),
         when the rider reopens it on 2026-11-15 or on 2026-11-16,
         then reopening is possible on the 15th, not on the 16th
        """
    )
    // @formatter:on
    @Test
    void rejectedOnFirstNovemberIsReopenableUntilTheEndOfFifteenthNovember() {
        // given
        var window = ReopeningWindow.afterRejectionAt(Instant.parse("2026-11-01T15:00:00Z"), WARSAW);

        // when / then
        assertThat(window.isOpenAt(Instant.parse("2026-11-15T12:00:00Z"))).isTrue();
        assertThat(window.isOpenAt(Instant.parse("2026-11-16T12:00:00Z"))).isFalse();
    }
}
