package pl.wojtyna.dddtrainingv3.velocity.common.contract;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.primary.FailedChargeRetryJob;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.awaitility.Awaitility.await;

/// Records every integration event published on the in-process bus, so message-provider verifications can hand the
/// event a context really published to Pact.
public class CapturedIntegrationEvents {

    private final List<IntegrationEvent> events = new CopyOnWriteArrayList<>();

    @EventListener
    public void on(IntegrationEvent event) {
        events.add(event);
    }

    /// Waits for the first published event of `type` that matches `which` (events may come from asynchronous listeners).
    public <E extends IntegrationEvent> E awaitFirst(Class<E> type, Predicate<E> which) {
        var found = new AtomicReference<E>();
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(50)).until(() -> {
            events.stream().filter(type::isInstance).map(type::cast).filter(which).findFirst().ifPresent(found::set);
            return found.get() != null;
        });
        return found.get();
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {

        @Bean
        public CapturedIntegrationEvents capturedIntegrationEvents() {
            return new CapturedIntegrationEvents();
        }

        @Bean
        public ProviderStates providerStates(RestClient velocityRestClient, VelocityServiceUrls urls, MutableClock clock,
                                             FailedChargeRetryJob failedChargeRetryJob) {
            return new ProviderStates(velocityRestClient, urls, clock, failedChargeRetryJob);
        }
    }
}
