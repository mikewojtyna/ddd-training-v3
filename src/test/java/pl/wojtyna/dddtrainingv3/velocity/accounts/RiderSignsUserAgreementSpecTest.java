package pl.wojtyna.dddtrainingv3.velocity.accounts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.AccountActivated;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/// Acceptance tests of `docs/specs/rider-signs-user-agreement.md`. Each rider is onboarded over REST; the card step
/// goes through real Payments with the simulated processor.
@VelocitySpringTest
@DisplayName("Spec rider-signs-user-agreement")
class RiderSignsUserAgreementSpecTest {

    @Autowired
    private RestClient restClient;
    @Autowired
    private VelocityServiceUrls urls;
    @Autowired
    private ApplicationEventMulticaster multicaster;
    @Autowired
    private MutableClock clock;

    private AccountsDriver accounts;
    private final String suffix = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        accounts = new AccountsDriver(restClient, urls, multicaster);
        clock.setTo("2026-11-08T10:10:00Z");
    }

    @AfterEach
    void tearDown() {
        accounts.stop();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna's card ending 4242 was verified on 2026-11-08 at 10:10,
         when Anna signs the user agreement at 10:12,
         then the signature is recorded and Anna's account is activated at 10:12 (Account Activated)
        """
    )
    // @formatter:on
    @Test
    void e1_1_signing_after_verified_card_activates_account() {
        // given
        var anna = accounts.cardVerified("anna-" + suffix + "@example.com", "tok_visa_4242_" + suffix);
        clock.setTo("2026-11-08T10:12:00Z");

        // when
        var status = accounts.signUserAgreement(anna);

        // then
        assertThat(status).isEqualTo(200);
        assertThat(accounts.statusOf(anna)).isEqualTo(new AccountsDriver.Status(anna, "NONE", true, java.util.List.of()));
        var at1012 = Instant.parse("2026-11-08T10:12:00Z");
        assertThat(accounts.eventsOf(anna, AccountActivated.class)).containsExactly(
            new AccountActivated(new pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RiderId(anna), at1012, at1012));
        assertThat(accounts.rentalEligibilityOf(anna)).isEqualTo(new AccountsDriver.Eligibility(anna, true, "ELIGIBLE"));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Ben provided his personal data and his only card ending 0002 was declined,
         when Ben signs the user agreement,
         then the step is rejected, Ben's account stays incomplete and his next onboarding step is still adding a payment method
        """
    )
    // @formatter:on
    @Test
    void e2_1_signing_rejected_without_verified_card() {
        // given
        var ben = accounts.cardVerified("ben-" + suffix + "@example.com", "tok_declined_0002_" + suffix);

        // when
        var status = accounts.signUserAgreement(ben);

        // then
        assertThat(status).isEqualTo(409);
        var account = accounts.statusOf(ben);
        assertThat(account.activated()).isFalse();
        assertThat(account.onboardingStep()).isEqualTo("ADD_PAYMENT_METHOD");
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Carla's card ending 7777 was verified on 2026-11-01 and she stopped there,
         when Carla signs the user agreement on 2026-11-15,
         then Carla's account is activated on 2026-11-15 (Account Activated)
        """
    )
    // @formatter:on
    @Test
    void e3_1_signing_two_weeks_later_activates_account() {
        // given
        clock.setTo("2026-11-01T10:00:00Z");
        var carla = accounts.cardVerified("carla-" + suffix + "@example.com", "tok_visa_7777_" + suffix);
        clock.setTo("2026-11-15T10:00:00Z");

        // when
        var status = accounts.signUserAgreement(carla);

        // then
        assertThat(status).isEqualTo(200);
        assertThat(accounts.statusOf(carla).activated()).isTrue();
        assertThat(accounts.eventsOf(carla, AccountActivated.class)).singleElement()
                                                                  .extracting(AccountActivated::activatedAt)
                                                                  .isEqualTo(Instant.parse("2026-11-15T10:00:00Z"));
    }

    // @formatter:off
    @DisplayName(
        """
         Contract (plan Q3): given Anna's account is activated,
         when Anna signs the user agreement again,
         then the request is rejected with 409
        """
    )
    // @formatter:on
    @Test
    void signing_twice_is_rejected() {
        // given
        var anna = accounts.activated("anna-" + suffix + "@example.com", "tok_visa_4242_" + suffix);

        // when
        var status = accounts.signUserAgreement(anna);

        // then
        assertThat(status).isEqualTo(409);
    }
}
