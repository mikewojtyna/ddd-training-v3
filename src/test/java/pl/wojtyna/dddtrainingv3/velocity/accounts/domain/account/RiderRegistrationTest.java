package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.AccountRegistered;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("Rider registration examples")
class RiderRegistrationTest {

    private static final EmailAddress ANNA_EMAIL = new EmailAddress("anna@example.com");
    private static final RiderId ANNA = new RiderId("anna");
    private static final RiderId BEN = new RiderId("ben");
    private static final Instant AT_10_00 = Instant.parse("2026-11-08T10:00:00Z");
    private static final Instant AT_11_00 = Instant.parse("2026-11-08T11:00:00Z");

    private final Set<EmailAddress> registered = new HashSet<>();
    private final RiderRegistration registration = new RiderRegistration(registered::contains);

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
    void personRegistersWithUnusedEmail() {
        // when
        var newAccount = registration.register(ANNA_EMAIL).as(ANNA).at(AT_10_00);

        // then
        assertThat(newAccount.events().stream()).containsExactly(new AccountRegistered(ANNA, ANNA_EMAIL, AT_10_00));
        assertThat(newAccount.created().riderId()).isEqualTo(ANNA);
        assertThat(newAccount.created().rentalEligibility()).isEqualTo(RentalEligibility.ONBOARDING_INCOMPLETE);
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
    void duplicateEmailIsRejected() {
        // given
        registration.register(ANNA_EMAIL).as(ANNA).at(AT_10_00);
        registered.add(ANNA_EMAIL);

        // when
        var thrown = catchThrowable(() -> registration.register(new EmailAddress("Anna@Example.com")).as(BEN).at(AT_11_00));

        // then
        assertThat(thrown).isInstanceOf(EmailAlreadyRegistered.class);
    }
}
