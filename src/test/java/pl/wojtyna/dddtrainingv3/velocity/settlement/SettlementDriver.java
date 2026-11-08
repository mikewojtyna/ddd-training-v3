package pl.wojtyna.dddtrainingv3.velocity.settlement;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
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

/// Drives Settlement the way its collaborators do: Rentals' `RentalSessionEnded` on the event bus, Accounts and
/// Payments over their REST resources (to have real riders and cards), Settlement's own REST resources (rider app,
/// Debt Recovery, queries). Records Settlement's published language, which is published on the server's threads.
///
/// Rides the launch tariff (DE-30) prices on 2026-11-09 at the examples' fees: PLN 5.00 = 30 minutes, free-floating
/// in the Old Town Square drop zone; PLN 43.00 = 30 minutes, free-floating outside any zone; PLN 3.00 = 30 minutes
/// (or 18), station return; PLN 0.00 = 12 minutes, station return.
class SettlementDriver implements ApplicationListener<ApplicationEvent> {

    static final String DAY = "2026-11-09T";

    record Ride(String sessionId, String riderId, String fee, String status, int failedAttempts,
                Instant nextAttemptDueAt, String lastFailureReason) {
    }

    record UnpaidRide(String sessionId, String amount) {
    }

    record Balance(String riderId, String outstandingDebt, List<UnpaidRide> unpaidRides) {
    }

    record CollectionAnswer(int status, Boolean collected, String collectedAmount, String outstandingDebt, String error) {
    }

    record AccountStatus(String riderId, List<String> blocks) {
    }

    private record Registered(String riderId) {
    }

    private record CollectionBody(Boolean collected, String collectedAmount, String outstandingDebt, String error) {
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;
    private final ApplicationEventPublisher eventBus;
    private final ApplicationEventMulticaster multicaster;
    private final MutableClock clock;
    private final RetryFailedChargesUseCase retryFailedCharges;
    private final List<IntegrationEvent> published = new CopyOnWriteArrayList<>();

    SettlementDriver(RestClient restClient, VelocityServiceUrls urls, ApplicationEventPublisher eventBus,
                     ApplicationEventMulticaster multicaster, MutableClock clock,
                     RetryFailedChargesUseCase retryFailedCharges) {
        this.restClient = restClient;
        this.urls = urls;
        this.eventBus = eventBus;
        this.multicaster = multicaster;
        this.clock = clock;
        this.retryFailedCharges = retryFailedCharges;
        multicaster.addApplicationListener(this);
    }

    void stop() {
        multicaster.removeApplicationListener(this);
    }

    static Instant at(String time) {
        return Instant.parse(DAY + time + ":00Z");
    }

    // --- Accounts and Payments: a real rider with a card ---

    String registeredRider(String name) {
        return restClient.post().uri(urls.url("/accounts/riders")).contentType(MediaType.APPLICATION_JSON)
                         .body(Map.of("email", name + "-" + UUID.randomUUID() + "@example.com"))
                         .retrieve().body(Registered.class).riderId();
    }

    void verifyCard(String riderId, String cardToken) {
        restClient.post().uri(urls.url("/payments/payment-methods")).contentType(MediaType.APPLICATION_JSON)
                  .body(Map.of("riderId", riderId, "cardToken", cardToken))
                  .retrieve().toBodilessEntity();
    }

    void processorReportsCardInvalidated(String cardToken) {
        restClient.post().uri(urls.url("/payments/processor-notifications/card-invalidations"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(Map.of("cardToken", cardToken, "reason", "EXPIRED"))
                  .retrieve().toBodilessEntity();
    }

    AccountStatus accountOf(String riderId) {
        return restClient.get().uri(urls.url("/accounts/riders/" + riderId)).retrieve().body(AccountStatus.class);
    }

    // --- Rentals' published language ---

    void freeFloatingRideEnded(String sessionId, String riderId, String startedAt, String endedAt, String latitude,
                               String longitude) {
        eventBus.publishEvent(new RentalSessionEnded(sessionId, "bike-" + sessionId, riderId, at(startedAt), at(endedAt),
                                                     "FREE_FLOATING_RETURN", null, new BigDecimal(latitude),
                                                     new BigDecimal(longitude)));
    }

    void stationRideEnded(String sessionId, String riderId, String startedAt, String endedAt) {
        eventBus.publishEvent(new RentalSessionEnded(sessionId, "bike-" + sessionId, riderId, at(startedAt), at(endedAt),
                                                     "STATION_RETURN", "Central", null, null));
    }

    /// PLN 5.00: 30 minutes ending in the Old Town Square drop zone.
    void ride5PlnEnded(String sessionId, String riderId, String startedAt, String endedAt) {
        freeFloatingRideEnded(sessionId, riderId, startedAt, endedAt, "52.2298", "21.0123");
    }

    /// PLN 43.00: 30 minutes ending outside every zone (PLN 3.00 + PLN 40.00 unauthorized zone fee).
    void ride43PlnEnded(String sessionId, String riderId, String startedAt, String endedAt) {
        freeFloatingRideEnded(sessionId, riderId, startedAt, endedAt, "52.2500", "21.1000");
    }

    // --- Settlement ---

    Optional<Ride> ride(String sessionId) {
        return restClient.get().uri(urls.url("/settlement/rides/" + sessionId))
                         .exchange((request, response) -> response.getStatusCode().value() == 404
                                                          ? Optional.<Ride>empty()
                                                          : Optional.ofNullable(response.bodyTo(Ride.class)));
    }

    Ride awaitRide(String sessionId, String status, int failedAttempts) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(ride(sessionId)).hasValueSatisfying(ride -> {
            assertThat(ride.status()).isEqualTo(status);
            assertThat(ride.failedAttempts()).isEqualTo(failedAttempts);
        }));
        return ride(sessionId).orElseThrow();
    }

    Balance balanceOf(String riderId) {
        return restClient.get().uri(urls.url("/settlement/riders/" + riderId + "/balance")).retrieve().body(Balance.class);
    }

    CollectionAnswer payOff(String riderId) {
        return collection("/settlement/riders/" + riderId + "/debt-payment");
    }

    CollectionAnswer collect(String riderId) {
        return collection("/settlement/riders/" + riderId + "/debt-collections");
    }

    void retryFailedChargesAt(String time) {
        clock.setTo(at(time));
        retryFailedCharges.handle(new RetryFailedCharges(at(time)));
    }

    /// A rider whose card is always declined and whose rides (each `{sessionId, startedAt, endedAt}`, 30 minutes or
    /// 18 minutes at a station, so PLN 3.00 each, or the PLN 43.00 ride when `fee43` is set) stayed unpaid after all
    /// three attempts: first attempt at the ride's end, retries 5 and 10 minutes later.
    void ridesUnpaid(String riderId, boolean fee43, String sessionId, String startedAt, String endedAt) {
        clock.setTo(at(endedAt));
        if (fee43) {
            ride43PlnEnded(sessionId, riderId, startedAt, endedAt);
        } else {
            stationRideEnded(sessionId, riderId, startedAt, endedAt);
        }
        var ride = awaitRide(sessionId, "PENDING", 1);
        retryFailedChargesAt(time(ride.nextAttemptDueAt()));
        var second = awaitRide(sessionId, "PENDING", 2);
        retryFailedChargesAt(time(second.nextAttemptDueAt()));
        awaitRide(sessionId, "UNPAID", 3);
    }

    // --- Settlement's published language ---

    List<RentalFeeUnpaid> rentalFeesUnpaidOf(String riderId) {
        return published.stream().filter(RentalFeeUnpaid.class::isInstance).map(RentalFeeUnpaid.class::cast)
                        .filter(event -> event.riderId().equals(riderId)).toList();
    }

    List<OutstandingDebtCleared> debtsClearedOf(String riderId) {
        return published.stream().filter(OutstandingDebtCleared.class::isInstance).map(OutstandingDebtCleared.class::cast)
                        .filter(event -> event.riderId().equals(riderId)).toList();
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof PayloadApplicationEvent<?> payload
            && (payload.getPayload() instanceof RentalFeeUnpaid || payload.getPayload() instanceof OutstandingDebtCleared)) {
            published.add((IntegrationEvent) payload.getPayload());
        }
    }

    private CollectionAnswer collection(String path) {
        return restClient.post().uri(urls.url(path))
                         .exchange((request, response) -> {
                             var body = response.bodyTo(CollectionBody.class);
                             return new CollectionAnswer(response.getStatusCode().value(), body.collected(),
                                                         body.collectedAmount(), body.outstandingDebt(), body.error());
                         });
    }

    private static String time(Instant instant) {
        return instant.toString().substring(DAY.length(), DAY.length() + 5);
    }
}
