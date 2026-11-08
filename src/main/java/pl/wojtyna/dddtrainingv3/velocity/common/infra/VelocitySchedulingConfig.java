package pl.wojtyna.dddtrainingv3.velocity.common.infra;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/// Turns on the `@Scheduled` jobs that drive time-based use cases (reservation expiry, overdue sessions, charge
/// retries, collection attempts). Tests switch it off with `velocity.scheduling.enabled=false` and call the use
/// cases with a controlled clock instead.
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "velocity.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class VelocitySchedulingConfig {
}
