package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.messagingacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.PaymentReminders;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RiderId;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/// Stand-in for the messaging provider: records every payment reminder it was asked to deliver, so tests (and a demo)
/// can see who was reminded of how much.
@SecondaryAdapter
public class SimulatedPaymentReminders implements PaymentReminders {

    /// A delivered reminder, in the provider's terms: recipient and amount due (PLN).
    public record SentReminder(String recipient, BigDecimal amountDue, String currency) {
    }

    private final List<SentReminder> sent = new CopyOnWriteArrayList<>();

    @Override
    public void remind(RiderId rider, Money outstandingDebt) {
        sent.add(new SentReminder(rider.value(), outstandingDebt.getAmount(), outstandingDebt.getCurrencyUnit().getCode()));
    }

    public List<SentReminder> sent() {
        return List.copyOf(sent);
    }

    public List<SentReminder> sentTo(String riderId) {
        return sent.stream().filter(reminder -> reminder.recipient().equals(riderId)).toList();
    }
}
