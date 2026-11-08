package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

/// The processor's answer to a pre-authorization or a charge, in Payments' words.
public enum ProcessorAnswer {
    ACCEPTED, DECLINED
}
