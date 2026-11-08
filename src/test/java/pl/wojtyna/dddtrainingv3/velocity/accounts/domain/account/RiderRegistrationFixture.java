package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import java.time.Instant;

/// Registers a rider through the real factory, with no other account registered.
final class RiderRegistrationFixture {

    private RiderRegistrationFixture() {
    }

    static Account register(RiderId rider, EmailAddress email, Instant at) {
        return new RiderRegistration(any -> false).register(email).as(rider).at(at).created();
    }
}
