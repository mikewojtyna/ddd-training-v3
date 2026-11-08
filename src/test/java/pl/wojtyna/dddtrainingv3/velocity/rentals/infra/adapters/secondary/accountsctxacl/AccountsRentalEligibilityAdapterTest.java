package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.accountsctxacl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Holder;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RentalEligibility.Eligibility;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RentalEligibilityUnknown;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/// The ACL translates Accounts' Rental Eligibility answer into Rentals' eligibility, against stubbed HTTP responses.
class AccountsRentalEligibilityAdapterTest {

    private MockRestServiceServer accounts;
    private AccountsRentalEligibilityAdapter adapter;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        accounts = MockRestServiceServer.bindTo(builder).build();
        var urls = new VelocityServiceUrls(new MockEnvironment().withProperty("velocity.services.base-url", "http://accounts.test"));
        adapter = new AccountsRentalEligibilityAdapter(builder.build(), urls);
    }

    @Test
    @DisplayName("given Accounts answers eligible=true for anna, when Rentals asks, then anna is eligible")
    void eligibleRiderIsEligible() {
        // given
        accounts.expect(requestTo("http://accounts.test/accounts/riders/anna/rental-eligibility"))
                .andRespond(withSuccess("{\"riderId\":\"anna\",\"eligible\":true,\"reason\":\"ELIGIBLE\"}", MediaType.APPLICATION_JSON));

        // when / then
        assertThat(adapter.of(Holder.rider("anna"))).isEqualTo(Eligibility.ELIGIBLE);
    }

    @Test
    @DisplayName("given Accounts answers eligible=false (BLOCKED, ONBOARDING_INCOMPLETE or UNKNOWN_RIDER), when Rentals asks, then the rider is not eligible")
    void blockedRiderIsNotEligible() {
        // given
        accounts.expect(requestTo("http://accounts.test/accounts/riders/carla/rental-eligibility"))
                .andRespond(withSuccess("{\"riderId\":\"carla\",\"eligible\":false,\"reason\":\"BLOCKED\"}", MediaType.APPLICATION_JSON));

        // when / then
        assertThat(adapter.of(Holder.rider("carla"))).isEqualTo(Eligibility.NOT_ELIGIBLE);
    }

    @Test
    @DisplayName("given Accounts fails, when Rentals asks, then the answer is unknown (Rentals refuses: fail closed)")
    void accountsFailureIsUnknown() {
        // given
        accounts.expect(requestTo("http://accounts.test/accounts/riders/ben/rental-eligibility"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        // when / then
        assertThatThrownBy(() -> adapter.of(Holder.rider("ben"))).isInstanceOf(RentalEligibilityUnknown.class);
    }
}
