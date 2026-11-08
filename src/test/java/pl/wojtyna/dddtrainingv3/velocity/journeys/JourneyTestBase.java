package pl.wojtyna.dddtrainingv3.velocity.journeys;

import org.awaitility.core.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.time.Duration;

import static org.awaitility.Awaitility.await;

/// Shared set-up of the end-to-end journeys: the whole application with real beans on a random port
/// (`@VelocitySpringTest` on each journey), the people's apps talking REST to it, and the clock.
///
/// Integration events are consumed asynchronously, so every effect in another context is awaited, never assumed: the
/// journeys must keep passing when the in-process event bus is replaced by a real broker.
abstract class JourneyTestBase {

    @Autowired
    RestClient velocityRestClient;

    @Autowired
    VelocityServiceUrls velocityServiceUrls;

    @Autowired
    MutableClock clock;

    VelocityApps apps;

    @BeforeEach
    void setUpApps() {
        apps = new VelocityApps(velocityRestClient, velocityServiceUrls);
    }

    static void eventually(ThrowingRunnable assertion) {
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(50)).untilAsserted(assertion);
    }
}
