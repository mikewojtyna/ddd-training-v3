package pl.wojtyna.dddtrainingv3.velocity.common;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;

/// Replaces the production clock with a `MutableClock` in every VeloCity Spring test.
@TestConfiguration(proxyBeanMethods = false)
public class VelocityTestConfig {

    @Bean
    @Primary
    public MutableClock velocityTestClock() {
        return new MutableClock(Instant.parse("2026-11-08T10:00:00Z"));
    }
}
