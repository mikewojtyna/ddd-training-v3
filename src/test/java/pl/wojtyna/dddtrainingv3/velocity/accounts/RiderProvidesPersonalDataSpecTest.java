package pl.wojtyna.dddtrainingv3.velocity.accounts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.PersonalDataProvided;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/// Acceptance tests of `docs/specs/rider-provides-personal-data.md`, over Accounts' REST resources.
@VelocitySpringTest
@DisplayName("Spec rider-provides-personal-data")
class RiderProvidesPersonalDataSpecTest {

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
        clock.setTo("2026-11-08T10:05:00Z");
    }

    @AfterEach
    void tearDown() {
        accounts.stop();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna verified her phone on 2026-11-08 at 10:05,
         when Anna provides "Anna Nowak", born 1995-04-12, residing in Poland, at 10:08,
         then her personal data is recorded and her next onboarding step is adding a payment method (Personal Data Provided)
        """
    )
    // @formatter:on
    @Test
    void e1_1_rider_with_verified_phone_provides_personal_data() {
        // given
        var anna = accounts.phoneVerified("anna-" + suffix + "@example.com");
        clock.setTo("2026-11-08T10:08:00Z");

        // when
        var status = accounts.providePersonalData(anna, "Anna Nowak", "1995-04-12", "Poland");

        // then
        assertThat(status).isEqualTo(200);
        assertThat(accounts.statusOf(anna).onboardingStep()).isEqualTo("ADD_PAYMENT_METHOD");
        assertThat(accounts.eventsOf(anna, PersonalDataProvided.class)).singleElement().satisfies(event -> {
            assertThat(event.fullName()).isEqualTo("Anna Nowak");
            assertThat(event.dateOfBirth()).isEqualTo(LocalDate.parse("1995-04-12"));
            assertThat(event.countryOfResidency()).isEqualTo("Poland");
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given Anna registered and confirmed her email but hasn't verified her phone,
         when Anna provides her personal data,
         then the step is rejected and her next onboarding step is still verifying her phone
        """
    )
    // @formatter:on
    @Test
    void e1_2_personal_data_rejected_before_phone_verified() {
        // given
        var anna = accounts.registered("anna-" + suffix + "@example.com");
        accounts.confirmEmail(anna);

        // when
        var status = accounts.providePersonalData(anna, "Anna Nowak", "1995-04-12", "Poland");

        // then
        assertThat(status).isEqualTo(409);
        assertThat(accounts.statusOf(anna).onboardingStep()).isEqualTo("VERIFY_PHONE");
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Ben verified his phone,
         when on 2026-11-08 Ben provides "Ben Kowalski", born 2010-11-09, residing in Poland,
         then the step is rejected because Ben is 15, and his next onboarding step is still providing personal data
        """
    )
    // @formatter:on
    @Test
    void e2_1_fifteen_year_old_is_rejected() {
        // given
        var ben = accounts.phoneVerified("ben-" + suffix + "@example.com");

        // when
        var status = accounts.providePersonalData(ben, "Ben Kowalski", "2010-11-09", "Poland");

        // then
        assertThat(status).isEqualTo(409);
        assertThat(accounts.statusOf(ben).onboardingStep()).isEqualTo("PROVIDE_PERSONAL_DATA");
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Carla verified her phone,
         when on 2026-11-08 Carla provides "Carla Rossi", born 2010-11-08, residing in Poland,
         then her personal data is recorded because she turns 16 that day (Personal Data Provided)
        """
    )
    // @formatter:on
    @Test
    void e2_2_rider_turning_sixteen_today_is_accepted() {
        // given
        var carla = accounts.phoneVerified("carla-" + suffix + "@example.com");

        // when
        var status = accounts.providePersonalData(carla, "Carla Rossi", "2010-11-08", "Poland");

        // then
        assertThat(status).isEqualTo(200);
        assertThat(accounts.statusOf(carla).onboardingStep()).isEqualTo("ADD_PAYMENT_METHOD");
        assertThat(accounts.eventsOf(carla, PersonalDataProvided.class)).hasSize(1);
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Dieter verified his phone,
         when on 2026-11-08 Dieter provides "Dieter Weber", born 1988-02-03, residing in Germany,
         then his personal data is recorded (Personal Data Provided)
        """
    )
    // @formatter:on
    @Test
    void e3_1_any_country_of_residency_is_accepted() {
        // given
        var dieter = accounts.phoneVerified("dieter-" + suffix + "@example.com");

        // when
        var status = accounts.providePersonalData(dieter, "Dieter Weber", "1988-02-03", "Germany");

        // then
        assertThat(status).isEqualTo(200);
        assertThat(accounts.eventsOf(dieter, PersonalDataProvided.class)).singleElement()
                                                                       .extracting(PersonalDataProvided::countryOfResidency)
                                                                       .isEqualTo("Germany");
    }

    // @formatter:off
    @DisplayName(
        """
         Contract: given Eva verified her phone,
         when Eva sends her personal data without a date of birth,
         then the request is malformed (400) and nothing is recorded
        """
    )
    // @formatter:on
    @Test
    void missing_field_is_bad_request() {
        // given
        var eva = accounts.phoneVerified("eva-" + suffix + "@example.com");

        // when
        var status = restClient.post().uri(urls.url("/accounts/riders/" + eva + "/personal-data"))
                               .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                               .body(java.util.Map.of("fullName", "Eva Green", "countryOfResidency", "Poland"))
                               .exchange((request, response) -> response.getStatusCode().value());

        // then
        assertThat(status).isEqualTo(400);
        assertThat(accounts.statusOf(eva).onboardingStep()).isEqualTo("PROVIDE_PERSONAL_DATA");
    }
}
