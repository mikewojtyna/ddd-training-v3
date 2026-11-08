package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Asks Payments to verify the rider's card. Accounts conforms to Payments' *verified* (CML
/// `Payments [U,OHS,PL]->[D,CF] Accounts`). A declined card is an answer, not an error.
@SecondaryPort
public interface PaymentMethodVerifier {

    CardVerification verify(RiderId rider, CardToken card);
}
