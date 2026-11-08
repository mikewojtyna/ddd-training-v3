package pl.wojtyna.dddtrainingv3.velocity.common;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Meta-annotation for VeloCity Spring tests: the whole application with real beans on a random port (so REST
/// ACL adapters really call the other contexts over HTTP), scheduling switched off (tests drive time-based use
/// cases themselves) and the controllable `MutableClock`.
///
/// Every test class using it shares one cached application context, so tests must use their own identifiers
/// (riders, bikes, incidents) and must not assume empty repositories. Cross-context effects arrive asynchronously:
/// assert them with Awaitility (`await().untilAsserted(...)`), never immediately.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(VelocityTestConfig.class)
public @interface VelocitySpringTest {
}
