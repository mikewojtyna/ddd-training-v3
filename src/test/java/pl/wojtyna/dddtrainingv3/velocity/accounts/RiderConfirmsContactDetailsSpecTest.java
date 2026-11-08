package pl.wojtyna.dddtrainingv3.velocity.accounts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.EmailConfirmed;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.PhoneVerified;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/// Acceptance tests of `docs/specs/rider-confirms-contact-details.md`, over Accounts' REST resources.
@VelocitySpringTest
@DisplayName("Spec rider-confirms-contact-details")
class RiderConfirmsContactDetailsSpecTest {

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
        clock.setTo("2026-11-08T10:00:00Z");
    }

    @AfterEach
    void tearDown() {
        accounts.stop();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna registered on 2026-11-08 at 10:00 with anna@example.com,
         when Anna follows the confirmation link at 10:03,
         then Anna's email is confirmed and her next onboarding step is verifying her phone (Email Confirmed)
        """
    )
    // @formatter:on
    @Test
    void e1_1_rider_confirms_email_with_link() {
        // given
        var anna = accounts.registered("anna-" + suffix + "@example.com");
        clock.setTo("2026-11-08T10:03:00Z");

        // when
        var status = accounts.confirmEmail(anna);

        // then
        assertThat(status).isEqualTo(200);
        assertThat(accounts.statusOf(anna).onboardingStep()).isEqualTo("VERIFY_PHONE");
        assertThat(accounts.eventsOf(anna, EmailConfirmed.class)).singleElement()
                                                                 .extracting(EmailConfirmed::confirmedAt)
                                                                 .isEqualTo(Instant.parse("2026-11-08T10:03:00Z"));
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given Anna confirmed anna@example.com at 10:03,
         when Anna follows the same confirmation link again at 10:20,
         then nothing changes: her next onboarding step is still verifying her phone, and no second Email Confirmed is recorded
        """
    )
    // @formatter:on
    @Test
    void e1_2_second_email_confirmation_changes_nothing() {
        // given
        var anna = accounts.registered("anna-" + suffix + "@example.com");
        clock.setTo("2026-11-08T10:03:00Z");
        accounts.confirmEmail(anna);
        clock.setTo("2026-11-08T10:20:00Z");

        // when
        var status = accounts.confirmEmail(anna);

        // then
        assertThat(status).isEqualTo(200);
        assertThat(accounts.statusOf(anna).onboardingStep()).isEqualTo("VERIFY_PHONE");
        assertThat(accounts.eventsOf(anna, EmailConfirmed.class)).hasSize(1);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna confirmed anna@example.com at 10:03,
         when Anna gives +48 600 100 200 and types the SMS code at 10:05,
         then Anna's phone is verified and her next onboarding step is providing personal data (Phone Verified)
        """
    )
    // @formatter:on
    @Test
    void e2_1_rider_verifies_phone_after_email() {
        // given
        var anna = accounts.registered("anna-" + suffix + "@example.com");
        clock.setTo("2026-11-08T10:03:00Z");
        accounts.confirmEmail(anna);
        clock.setTo("2026-11-08T10:05:00Z");

        // when
        var status = accounts.verifyPhone(anna, "+48 600 100 200");

        // then
        assertThat(status).isEqualTo(200);
        assertThat(accounts.statusOf(anna).onboardingStep()).isEqualTo("PROVIDE_PERSONAL_DATA");
        assertThat(accounts.eventsOf(anna, PhoneVerified.class)).singleElement().satisfies(event -> {
            assertThat(event.phoneNumber().value()).isEqualTo("+48 600 100 200");
            assertThat(event.verifiedAt()).isEqualTo(Instant.parse("2026-11-08T10:05:00Z"));
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Ben registered with ben@example.com and hasn't confirmed his email,
         when Ben gives +48 600 300 400 and types the SMS code,
         then the step is rejected and Ben's next onboarding step is still confirming his email
        """
    )
    // @formatter:on
    @Test
    void e2_2_phone_verification_rejected_before_email_confirmed() {
        // given
        var ben = accounts.registered("ben-" + suffix + "@example.com");

        // when
        var status = accounts.verifyPhone(ben, "+48 600 300 400");

        // then
        assertThat(status).isEqualTo(409);
        assertThat(accounts.statusOf(ben).onboardingStep()).isEqualTo("CONFIRM_EMAIL");
        assertThat(accounts.eventsOf(ben, PhoneVerified.class)).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Anna registered on 2026-11-01 and confirmed her email the same day,
         when Anna types the SMS code on 2026-11-06,
         then Anna's phone is verified (Phone Verified)
        """
    )
    // @formatter:on
    @Test
    void e3_1_phone_verified_days_later() {
        // given
        clock.setTo("2026-11-01T10:00:00Z");
        var anna = accounts.registered("anna-" + suffix + "@example.com");
        accounts.confirmEmail(anna);
        clock.setTo("2026-11-06T10:00:00Z");

        // when
        var status = accounts.verifyPhone(anna, "+48 600 100 200");

        // then
        assertThat(status).isEqualTo(200);
        assertThat(accounts.statusOf(anna).onboardingStep()).isEqualTo("PROVIDE_PERSONAL_DATA");
    }
}
