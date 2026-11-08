package pl.wojtyna.dddtrainingv3.velocity.accounts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.AccountRegistered;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/// Acceptance tests of `docs/specs/person-registers-with-velocity.md`, over Accounts' REST resources.
@VelocitySpringTest
@DisplayName("Spec person-registers-with-velocity")
class PersonRegistersWithVelocitySpecTest {

    @Autowired
    private RestClient restClient;
    @Autowired
    private VelocityServiceUrls urls;
    @Autowired
    private ApplicationEventMulticaster multicaster;
    @Autowired
    private MutableClock clock;

    private AccountsDriver accounts;
    private final String annaEmail = "anna-" + UUID.randomUUID() + "@example.com";

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
         E1.1 given nobody has registered with anna@example.com,
         when Anna registers on 2026-11-08 at 10:00 with anna@example.com,
         then Anna has an incomplete account (Account Registered)
        """
    )
    // @formatter:on
    @Test
    void e1_1_person_registers_with_unused_email() {
        // when
        var registration = accounts.register(annaEmail);

        // then
        assertThat(registration.status()).isEqualTo(201);
        var anna = registration.riderId();
        assertThat(accounts.statusOf(anna).activated()).isFalse();
        assertThat(accounts.eventsOf(anna, AccountRegistered.class)).singleElement().satisfies(event -> {
            assertThat(event.email().value()).isEqualTo(annaEmail);
            assertThat(event.registeredAt()).isEqualTo(Instant.parse("2026-11-08T10:00:00Z"));
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna registered on 2026-11-08 at 10:00,
         when Anna looks at her onboarding progress at 10:01,
         then her account is incomplete and her next onboarding step is confirming her email
        """
    )
    // @formatter:on
    @Test
    void e2_1_new_account_is_incomplete_and_waits_for_email_confirmation() {
        // given
        var anna = accounts.registered(annaEmail);
        clock.setTo("2026-11-08T10:01:00Z");

        // when
        var status = accounts.statusOf(anna);

        // then
        assertThat(status).isEqualTo(new AccountsDriver.Status(anna, "CONFIRM_EMAIL", false, java.util.List.of()));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Anna registered on 2026-11-08 at 10:00 and hasn't confirmed her email,
         when Anna tries to provide her personal data at 10:05,
         then the step is rejected and her next onboarding step is still confirming her email
        """
    )
    // @formatter:on
    @Test
    void e2_2_personal_data_rejected_before_email_confirmed() {
        // given
        var anna = accounts.registered(annaEmail);
        clock.setTo("2026-11-08T10:05:00Z");

        // when
        var status = accounts.providePersonalData(anna, "Anna Nowak", "1995-04-12", "Poland");

        // then
        assertThat(status).isEqualTo(409);
        assertThat(accounts.statusOf(anna).onboardingStep()).isEqualTo("CONFIRM_EMAIL");
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Anna registered on 2026-11-01 and did nothing else,
         when Anna opens the app on 2026-11-20,
         then her account is still incomplete and her next onboarding step is confirming her email
        """
    )
    // @formatter:on
    @Test
    void e3_1_incomplete_account_waits_without_deadline() {
        // given
        clock.setTo("2026-11-01T10:00:00Z");
        var anna = accounts.registered(annaEmail);
        clock.setTo("2026-11-20T10:00:00Z");

        // when
        var status = accounts.statusOf(anna);

        // then
        assertThat(status.activated()).isFalse();
        assertThat(status.onboardingStep()).isEqualTo("CONFIRM_EMAIL");
    }

    // @formatter:off
    @DisplayName(
        """
         E4.1 given Anna registered with anna@example.com on 2026-11-08 at 10:00,
         when Ben registers with anna@example.com at 11:00,
         then the registration is rejected and no second account is created
        """
    )
    // @formatter:on
    @Test
    void e4_1_duplicate_email_is_rejected() {
        // given
        accounts.registered(annaEmail);
        clock.setTo("2026-11-08T11:00:00Z");

        // when
        var bensRegistration = accounts.register(annaEmail);

        // then
        assertThat(bensRegistration.status()).isEqualTo(409);
        assertThat(bensRegistration.riderId()).isNull();
    }
}
