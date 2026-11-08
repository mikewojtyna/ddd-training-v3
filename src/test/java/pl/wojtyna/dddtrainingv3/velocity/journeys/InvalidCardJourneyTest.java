package pl.wojtyna.dddtrainingv3.velocity.journeys;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.messagingprovideracl.RecordingRiderNotifications;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.CENTRAL;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.expect;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.unique;

/// The payment processor invalidates a rider's card: processor → Payments (notification, REST), Payments → Accounts
/// (`PaymentMethodInvalidated`), Rentals → Accounts (eligibility), Accounts → Payments (new card).
@VelocitySpringTest
class InvalidCardJourneyTest extends JourneyTestBase {

    @Autowired
    RecordingRiderNotifications riderNotifications;

    @Test
    @DisplayName("""
        The payment processor reports an activated rider's current card expired: Accounts blocks the account for \
        an invalid card and notifies the rider, and a reservation is rejected. The rider adds a new card: the \
        account is unblocked and the reservation is accepted""")
    void invalidCardBlocksUntilANewCardIsAdded() {
        clock.setTo("2027-07-07T16:00:00Z");
        var cardToken = "tok_visa_" + unique("expiring");
        var riderId = apps.onboardedRider(cardToken);
        var bikeId = unique("bike-card");
        expect(apps.addBikeToFleet(bikeId, CENTRAL.stationId()), 201);

        // the processor (an external party) notifies Payments that the card expired
        var notified = velocityRestClient.post()
                                         .uri(velocityServiceUrls.url("/payments/processor-notifications/card-invalidations"))
                                         .contentType(MediaType.APPLICATION_JSON)
                                         .body(Map.of("cardToken", cardToken, "reason", "EXPIRED"))
                                         .retrieve().toBodilessEntity();
        assertThat(notified.getStatusCode().value()).isEqualTo(200);

        // Accounts blocks the rider and tells them why
        eventually(() -> {
            assertThat(expect(apps.account(riderId), 200).list("blocks")).containsExactly("INVALID_CARD");
            assertThat(expect(apps.rentalEligibility(riderId), 200).get("eligible")).isEqualTo("false");
            assertThat(riderNotifications.sentTo(riderId)).extracting(RecordingRiderNotifications.SentNotification::reason)
                                                          .containsExactly("INVALID_CARD");
        });

        // no new rentals
        assertThat(apps.reserve(bikeId, riderId, CENTRAL.tenMetresNorth()).status()).isEqualTo(409);

        // the rider adds a new card in the app
        assertThat(expect(apps.addCard(riderId, "tok_visa_" + unique("replacement")), 200).get("verified")).isEqualTo("true");
        eventually(() -> {
            assertThat(expect(apps.account(riderId), 200).list("blocks")).isEmpty();
            assertThat(expect(apps.rentalEligibility(riderId), 200).get("eligible")).isEqualTo("true");
        });
        expect(apps.reserve(bikeId, riderId, CENTRAL.tenMetresNorth()), 201);
    }
}
