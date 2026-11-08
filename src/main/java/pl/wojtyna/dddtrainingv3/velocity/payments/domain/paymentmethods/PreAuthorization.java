package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

import org.jmolecules.ddd.annotation.ValueObject;

/// An amount held on a card to prove it works. `hold` identifies it at the processor, so it can be released.
@ValueObject
public record PreAuthorization(String hold, ProcessorAnswer answer) {

    public boolean accepted() {
        return answer == ProcessorAnswer.ACCEPTED;
    }
}
