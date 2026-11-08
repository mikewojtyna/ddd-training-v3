package pl.wojtyna.dddtrainingv3.velocity.debtrecovery;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.AttemptDueCollections;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases.AttemptDueCollectionsUseCase;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RecoveryEvent;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.messagingacl.SimulatedPaymentReminders;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl.SimulatedPaymentProcessor;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.events.RentalSessionEnded;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.OutstandingDebtCleared;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.RentalFeeUnpaid;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.RetryFailedCharges;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.RetryFailedChargesUseCase;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/// Creates REAL outstanding debt for Debt Recovery's acceptance tests (plan `collections-attempts-debt-collection` D8)
/// and drives Debt Recovery the way its collaborators do.
///
/// - A rider registered in Accounts (`POST /accounts/riders`) whose current card at Payments is a `tok_nofunds_*`
///   token, so every charge is declined.
/// - A ride ends (Rentals' published `RentalSessionEnded` on the event bus, Rentals is built in parallel); Settlement
///   charges it and retries it 5 and 10 minutes later with the `MutableClock`; after the 3rd failure Settlement
///   publishes `RentalFeeUnpaid`, which Debt Recovery's listener turns into a recovery process (awaited).
/// - Launch tariff (DE-30) fees: PLN 5.00 = 30 minutes free-floating in the Old Town Square drop zone; PLN 43.00 =
///   30 minutes free-floating outside every zone; PLN 3.00 = 30 minutes, station return.
/// - A collection succeeds once the rider verified a new valid card at Payments (DE-5).
///
/// Also records Debt Recovery's domain events and reads Settlement's, Accounts' and Debt Recovery's REST resources.
class UnpaidRideFixture implements ApplicationListener<ApplicationEvent> {

    enum Fee {
        PLN_5, PLN_43, PLN_3
    }

    record Recovery(String riderId, String status, String outstandingDebt, int failedAttempts, Instant nextAttemptDueAt,
                    String pauseReason) {
    }

    record Answer(int status, String error, Recovery recovery) {
    }

    record Balance(String riderId, String outstandingDebt) {
    }

    record AccountStatus(String riderId, List<String> blocks) {
    }

    private record Ride(String sessionId, String status, int failedAttempts, Instant nextAttemptDueAt) {
    }

    private record Registered(String riderId) {
    }

    private record ErrorBody(String error, String message) {
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;
    private final ApplicationEventPublisher eventBus;
    private final ApplicationEventMulticaster multicaster;
    private final MutableClock clock;
    private final RetryFailedChargesUseCase retryFailedCharges;
    private final AttemptDueCollectionsUseCase attemptDueCollections;
    private final SimulatedPaymentReminders reminders;
    private final SimulatedPaymentProcessor processor;
    private final List<RecoveryEvent> recoveryEvents = new CopyOnWriteArrayList<>();
    private final String suffix = UUID.randomUUID().toString();

    UnpaidRideFixture(RestClient restClient, VelocityServiceUrls urls, ApplicationEventPublisher eventBus,
                      ApplicationEventMulticaster multicaster, MutableClock clock,
                      RetryFailedChargesUseCase retryFailedCharges, AttemptDueCollectionsUseCase attemptDueCollections,
                      SimulatedPaymentReminders reminders, SimulatedPaymentProcessor processor) {
        this.restClient = restClient;
        this.urls = urls;
        this.eventBus = eventBus;
        this.multicaster = multicaster;
        this.clock = clock;
        this.retryFailedCharges = retryFailedCharges;
        this.attemptDueCollections = attemptDueCollections;
        this.reminders = reminders;
        this.processor = processor;
        multicaster.addApplicationListener(this);
    }

    void stop() {
        multicaster.removeApplicationListener(this);
    }

    /// `"11-08T10:50"` → 2026-11-08T10:50:00Z
    static Instant at(String dateTime) {
        return Instant.parse("2026-" + dateTime + ":00Z");
    }

    // --- riders and cards ---

    /// A rider registered in Accounts whose card is always declined.
    String riderWithDecliningCard(String name) {
        var riderId = restClient.post().uri(urls.url("/accounts/riders")).contentType(MediaType.APPLICATION_JSON)
                                .body(Map.of("email", name + "-" + UUID.randomUUID() + "@example.com"))
                                .retrieve().body(Registered.class).riderId();
        verifyCard(riderId, decliningCardOf(riderId));
        return riderId;
    }

    String decliningCardOf(String riderId) {
        return "tok_nofunds_" + riderId;
    }

    /// The rider replaces their card with a valid one, so the next collection succeeds (DE-5).
    void riderAddsValidCard(String riderId) {
        verifyCard(riderId, "tok_visa_" + riderId);
    }

    private void verifyCard(String riderId, String cardToken) {
        restClient.post().uri(urls.url("/payments/payment-methods")).contentType(MediaType.APPLICATION_JSON)
                  .body(Map.of("riderId", riderId, "cardToken", cardToken))
                  .retrieve().toBodilessEntity();
    }

    // --- real unpaid rides ---

    /// A ride from `startedAt` to `endedAt` whose fee stays unpaid after Settlement's 3 attempts (at the end, +5, +10
    /// minutes); returns the ride's session id. Waits until Debt Recovery heard the Rental Fee Unpaid with the rider's
    /// `expectedOutstandingDebt`.
    String rideStaysUnpaid(String riderId, Fee fee, String startedAt, String endedAt, String expectedOutstandingDebt) {
        var sessionId = "S-" + UUID.randomUUID();
        clock.setTo(at(endedAt));
        eventBus.publishEvent(rideEnded(sessionId, riderId, fee, at(startedAt), at(endedAt)));
        var ride = awaitRide(sessionId, "PENDING", 1);
        retryFailedChargesAt(ride.nextAttemptDueAt());
        var second = awaitRide(sessionId, "PENDING", 2);
        retryFailedChargesAt(second.nextAttemptDueAt());
        awaitRide(sessionId, "UNPAID", 3);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(
            () -> assertThat(recoveryOf(riderId)).hasValueSatisfying(
                recovery -> assertThat(recovery.outstandingDebt()).isEqualTo(expectedOutstandingDebt)));
        return sessionId;
    }

    private static RentalSessionEnded rideEnded(String sessionId, String riderId, Fee fee, Instant startedAt, Instant endedAt) {
        var bikeId = "bike-" + sessionId;
        return switch (fee) {
            case PLN_5 -> new RentalSessionEnded(sessionId, bikeId, riderId, startedAt, endedAt, "FREE_FLOATING_RETURN",
                                                 null, new BigDecimal("52.2298"), new BigDecimal("21.0123"));
            case PLN_43 -> new RentalSessionEnded(sessionId, bikeId, riderId, startedAt, endedAt, "FREE_FLOATING_RETURN",
                                                  null, new BigDecimal("52.2500"), new BigDecimal("21.1000"));
            case PLN_3 -> new RentalSessionEnded(sessionId, bikeId, riderId, startedAt, endedAt, "STATION_RETURN",
                                                 "Central", null, null);
        };
    }

    private void retryFailedChargesAt(Instant now) {
        clock.setTo(now);
        retryFailedCharges.handle(new RetryFailedCharges(now));
    }

    private Ride awaitRide(String sessionId, String status, int failedAttempts) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(ride(sessionId)).hasValueSatisfying(ride -> {
            assertThat(ride.status()).isEqualTo(status);
            assertThat(ride.failedAttempts()).isEqualTo(failedAttempts);
        }));
        return ride(sessionId).orElseThrow();
    }

    private Optional<Ride> ride(String sessionId) {
        return restClient.get().uri(urls.url("/settlement/rides/" + sessionId))
                         .exchange((request, response) -> response.getStatusCode().value() == 404
                                                          ? Optional.<Ride>empty()
                                                          : Optional.ofNullable(response.bodyTo(Ride.class)));
    }

    // --- Debt Recovery ---

    /// Collections' clock: due collections are attempted at `dateTime`.
    void dueCollectionsAttemptedAt(String dateTime) {
        clock.setTo(at(dateTime));
        attemptDueCollections.handle(new AttemptDueCollections(at(dateTime)));
    }

    /// Every attempt fails: `count` attempts, each when it is due (72 hours apart), the first at `firstAt`.
    void failedAttempts(String riderId, int count, String firstAt) {
        var due = at(firstAt);
        for (var i = 0; i < count; i++) {
            clock.setTo(due);
            attemptDueCollections.handle(new AttemptDueCollections(due));
            assertThat(recoveryOf(riderId)).hasValueSatisfying(recovery -> assertThat(recovery.failedAttempts()).isPositive());
            due = due.plus(Duration.ofHours(72));
        }
    }

    Optional<Recovery> recoveryOf(String riderId) {
        return restClient.get().uri(urls.url("/debt-recovery/riders/" + riderId + "/recovery"))
                         .exchange((request, response) -> response.getStatusCode().value() == 404
                                                          ? Optional.<Recovery>empty()
                                                          : Optional.ofNullable(response.bodyTo(Recovery.class)));
    }

    Recovery awaitRecovery(String riderId, String status) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(
            () -> assertThat(recoveryOf(riderId)).hasValueSatisfying(recovery -> assertThat(recovery.status()).isEqualTo(status)));
        return recoveryOf(riderId).orElseThrow();
    }

    Answer pause(String riderId, String agentId, String reason, String dateTime) {
        clock.setTo(at(dateTime));
        return post("/debt-recovery/riders/" + riderId + "/recovery/pause", Map.of("agentId", agentId, "reason", reason));
    }

    Answer resume(String riderId, String agentId, String dateTime) {
        clock.setTo(at(dateTime));
        return post("/debt-recovery/riders/" + riderId + "/recovery/resume", Map.of("agentId", agentId));
    }

    private Answer post(String path, Map<String, String> body) {
        return restClient.post().uri(urls.url(path)).contentType(MediaType.APPLICATION_JSON).body(body)
                         .exchange((request, response) -> {
                             var status = response.getStatusCode().value();
                             if (status == 200) {
                                 return new Answer(status, null, response.bodyTo(Recovery.class));
                             }
                             var error = response.bodyTo(ErrorBody.class);
                             return new Answer(status, error == null ? null : error.error(), null);
                         });
    }

    List<SimulatedPaymentReminders.SentReminder> remindersTo(String riderId) {
        return reminders.sentTo(riderId);
    }

    /// Captures Settlement asked the processor for on Debt Recovery's behalf (collections, not ride charges).
    List<SimulatedPaymentProcessor.Capture> collectionsCharged(String riderId) {
        return processor.captures().stream()
                        .filter(capture -> capture.merchantReference().startsWith("DEBT-" + riderId + "-"))
                        .toList();
    }

    <E extends RecoveryEvent> List<E> recoveryEventsOf(String riderId, Class<E> type) {
        return recoveryEvents.stream().filter(type::isInstance).map(type::cast)
                             .filter(event -> event.rider().value().equals(riderId)).toList();
    }

    // --- Settlement and Accounts ---

    /// The rider pays off the outstanding debt in the app (Settlement's *Pay Outstanding Debt*).
    void riderPaysOffInApp(String riderId, String dateTime) {
        clock.setTo(at(dateTime));
        var status = restClient.post().uri(urls.url("/settlement/riders/" + riderId + "/debt-payment"))
                               .exchange((request, response) -> response.getStatusCode().value());
        assertThat(status).isEqualTo(200);
    }

    Balance balanceOf(String riderId) {
        return restClient.get().uri(urls.url("/settlement/riders/" + riderId + "/balance")).retrieve().body(Balance.class);
    }

    AccountStatus accountOf(String riderId) {
        return restClient.get().uri(urls.url("/accounts/riders/" + riderId)).retrieve().body(AccountStatus.class);
    }

    /// Settlement's published language, published as Settlement would (for examples whose amounts the tariff can't
    /// produce, or a repeated delivery).
    void settlementPublishes(Object event) {
        if (!(event instanceof RentalFeeUnpaid) && !(event instanceof OutstandingDebtCleared)) {
            throw new IllegalArgumentException("not Settlement's published language: " + event);
        }
        eventBus.publishEvent(event);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof PayloadApplicationEvent<?> payload && payload.getPayload() instanceof RecoveryEvent recoveryEvent) {
            recoveryEvents.add(recoveryEvent);
        }
    }

    String suffix() {
        return suffix;
    }
}
