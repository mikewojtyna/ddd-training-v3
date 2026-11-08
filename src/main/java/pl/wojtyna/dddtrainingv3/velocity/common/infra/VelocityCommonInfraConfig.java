package pl.wojtyna.dddtrainingv3.velocity.common.infra;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestClient;

import java.time.Clock;

/// Technical plumbing shared by every VeloCity context.
///
/// - `velocityClock`: the one clock every context reads "now" from. Tests replace it with a controllable clock.
/// - `velocityRestClient` + `VelocityServiceUrls`: what ACL adapters use to call another context's REST resources.
/// - `@EnableAsync`: integration-event listeners in other contexts are `@Async`, so events are consumed the way a
///   message broker would deliver them: later, on another thread.
@Configuration
@EnableAsync
public class VelocityCommonInfraConfig {

    @Bean
    public Clock velocityClock() {
        return Clock.systemUTC();
    }

    @Bean
    public VelocityServiceUrls velocityServiceUrls(Environment environment) {
        return new VelocityServiceUrls(environment);
    }

    @Bean
    public RestClient velocityRestClient() {
        return RestClient.builder().build();
    }
}
