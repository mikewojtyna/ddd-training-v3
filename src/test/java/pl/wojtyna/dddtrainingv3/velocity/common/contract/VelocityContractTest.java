package pl.wojtyna.dddtrainingv3.velocity.common.contract;

import org.junit.jupiter.api.Tag;
import org.springframework.context.annotation.Import;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Meta-annotation for Pact provider verifications: the whole application on a random port (`@VelocitySpringTest`),
/// plus the event recorder and the provider-state helper. Every provider test shares this one cached context, so the
/// provider states create their own riders, bikes and incidents and pass their ids to the pact (`fromProviderState`).
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@VelocitySpringTest
@Import(CapturedIntegrationEvents.Config.class)
@Tag(VelocityPacts.PROVIDER_TAG)
public @interface VelocityContractTest {
}
