package pl.wojtyna.dddtrainingv3.velocity.journeys;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.messagingprovideracl.RecordingRiderNotifications;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl.SimulatedPaymentProcessor;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl.SimulatedPaymentProcessor.ResponseCode;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.primary.FailedChargeRetryJob;

import java.math.BigDecimal;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.CENTRAL;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.expect;
import static pl.wojtyna.dddtrainingv3.velocity.journeys.VelocityApps.unique;

/// An unpaid ride turns into debt and is recovered: Rentals → Settlement (`RentalSessionEnded`), Settlement → Pricing
/// and → Payments (3 failed charges), Settlement → Accounts (block, REST) and → Debt Recovery (`RentalFeeUnpaid`),
/// Rentals → Accounts (eligibility), Accounts → Payments (new card), Settlement → Accounts (unblock) and → Debt
/// Recovery (`OutstandingDebtCleared`).
@VelocitySpringTest
class UnpaidRideToDebtRecoveryJourneyTest extends JourneyTestBase {

    @Autowired
    SimulatedPaymentProcessor paymentProcessor;

    @Autowired
    RecordingRiderNotifications riderNotifications;

    @Autowired
    FailedChargeRetryJob failedChargeRetryJob;

    @Test
    @DisplayName("""
        A rider whose card has no funds returns a bike at a station after 30 minutes; the PLN 3.00 charge fails \
        right away and on both retries 5 minutes apart, so the fee becomes debt: the account is blocked for the \
        outstanding debt (rider notified, not eligible), debt recovery runs and another reservation is rejected. \
        The rider adds a working card and pays the debt off in the app: the debt is cleared, the account is \
        unblocked and eligible again, and debt recovery has succeeded""")
    void unpaidRideIsRecovered() {
        clock.setTo("2027-04-14T08:00:00Z");
        var riderId = apps.onboardedRider("tok_nofunds_" + unique("debtor"));
        var bikeId = unique("bike-debt");
        expect(apps.addBikeToFleet(bikeId, CENTRAL.stationId()), 201);

        // a 30-minute ride ending at a station: hour 1, PLN 3.00, no return fee
        expect(apps.reserve(bikeId, riderId, CENTRAL.tenMetresNorth()), 201);
        var sessionId = expect(apps.unlock(bikeId, riderId), 200).get("sessionId");
        clock.advanceBy(Duration.ofMinutes(30));
        assertThat(expect(apps.returnAtStation(bikeId, riderId, CENTRAL.stationId()), 200).get("endReason"))
            .isEqualTo("STATION_RETURN");

        // the first charge, right after the ride, fails
        awaitRide(sessionId, "PENDING", 1);

        // finance retries 5 minutes after each failed attempt (DE-35)
        clock.advanceBy(Duration.ofMinutes(5));
        failedChargeRetryJob.retryFailedCharges();
        awaitRide(sessionId, "PENDING", 2);
        clock.advanceBy(Duration.ofMinutes(5));
        failedChargeRetryJob.retryFailedCharges();
        var unpaid = awaitRide(sessionId, "UNPAID", 3);
        assertThat(new BigDecimal(unpaid.get("fee"))).isEqualByComparingTo("3.00");

        // the fee is outstanding debt; Accounts blocks the rider and notifies them; Debt Recovery starts
        assertThat(new BigDecimal(expect(apps.balance(riderId), 200).get("outstandingDebt"))).isEqualByComparingTo("3.00");
        eventually(() -> {
            assertThat(expect(apps.account(riderId), 200).list("blocks")).containsExactly("OUTSTANDING_DEBT");
            var eligibility = expect(apps.rentalEligibility(riderId), 200);
            assertThat(eligibility.get("eligible")).isEqualTo("false");
            assertThat(eligibility.get("reason")).isEqualTo("BLOCKED");
            assertThat(riderNotifications.sentTo(riderId)).extracting(RecordingRiderNotifications.SentNotification::reason)
                                                          .containsExactly("OUTSTANDING_DEBT");
        });
        eventually(() -> {
            var recovery = expect(apps.recovery(riderId), 200);
            assertThat(recovery.get("status")).isEqualTo("RUNNING");
            assertThat(new BigDecimal(recovery.get("outstandingDebt"))).isEqualByComparingTo("3.00");
        });

        // a blocked rider can't reserve another bike
        var anotherBike = unique("bike-debt-2");
        expect(apps.addBikeToFleet(anotherBike, CENTRAL.stationId()), 201);
        assertThat(apps.reserve(anotherBike, riderId, CENTRAL.tenMetresNorth()).status()).isEqualTo(409);

        // the rider adds a working card and pays the debt off in the app
        var workingCard = "tok_visa_" + unique("debtor-new");
        assertThat(expect(apps.addCard(riderId, workingCard), 200).get("verified")).isEqualTo("true");
        var payment = expect(apps.payOffDebt(riderId), 200);
        assertThat(payment.get("collected")).isEqualTo("true");
        assertThat(new BigDecimal(payment.get("collectedAmount"))).isEqualByComparingTo("3.00");
        assertThat(paymentProcessor.capturesOf(workingCard))
            .singleElement()
            .satisfies(capture -> {
                assertThat(capture.amount()).isEqualByComparingTo("3.00");
                assertThat(capture.responseCode()).isEqualTo(ResponseCode.APPROVED);
            });

        // debt cleared: unblocked, eligible, recovery succeeded
        assertThat(new BigDecimal(expect(apps.balance(riderId), 200).get("outstandingDebt"))).isEqualByComparingTo("0");
        eventually(() -> {
            assertThat(expect(apps.account(riderId), 200).list("blocks")).isEmpty();
            assertThat(expect(apps.rentalEligibility(riderId), 200).get("eligible")).isEqualTo("true");
        });
        eventually(() -> assertThat(expect(apps.recovery(riderId), 200).get("status")).isEqualTo("SUCCEEDED"));

        // and can ride again
        expect(apps.reserve(anotherBike, riderId, CENTRAL.tenMetresNorth()), 201);
    }

    private VelocityApps.Reply awaitRide(String sessionId, String status, int failedAttempts) {
        eventually(() -> {
            var ride = expect(apps.rideSettlement(sessionId), 200);
            assertThat(ride.get("status")).isEqualTo(status);
            assertThat(ride.get("failedAttempts")).isEqualTo(String.valueOf(failedAttempts));
        });
        return apps.rideSettlement(sessionId);
    }
}
