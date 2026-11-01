package pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

/**
 * Open host service of Payments for charging a rider's payment method.
 */
@PrimaryPort
public interface ChargeRiderUseCase {

    ChargeResult charge(ChargeRider command);
}
