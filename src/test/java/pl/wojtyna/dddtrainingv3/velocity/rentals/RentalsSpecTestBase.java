package pl.wojtyna.dddtrainingv3.velocity.rentals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.bikelocksacl.SimulatedBikeLocks;

import java.time.Duration;
import java.util.UUID;

/// What every Rentals acceptance test needs: the driver, the clock, the simulated locks, and ids of its own (the
/// Spring context is shared and cached, so bikes and riders get a per-test suffix).
abstract class RentalsSpecTestBase {

    static final Duration HAND_OFF = Duration.ofSeconds(10);

    @Autowired
    RestClient restClient;
    @Autowired
    VelocityServiceUrls urls;
    @Autowired
    ApplicationEventMulticaster multicaster;
    @Autowired
    MutableClock clock;
    @Autowired
    SimulatedBikeLocks locks;

    RentalsDriver rentals;
    final String suffix = UUID.randomUUID().toString().substring(0, 8);

    @BeforeEach
    void startDriver() {
        rentals = new RentalsDriver(restClient, urls, multicaster);
    }

    @AfterEach
    void stopDriver() {
        rentals.stop();
    }

    String bike(String name) {
        return name + "-" + suffix;
    }

    String rider(String name) {
        return rentals.activatedRider(name + "-" + suffix);
    }

    void at(String isoInstant) {
        clock.setTo(isoInstant);
    }
}
