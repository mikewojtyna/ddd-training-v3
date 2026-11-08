package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.ChargeFailed;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.ChargeSucceeded;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.FormerPaymentMethodInvalidated;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.PaymentMethodDeclined;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.PaymentMethodInvalidated;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.PaymentMethodReplaced;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.PaymentMethodVerified;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Rider payment methods examples")
class RiderPaymentMethodsTest {

    private static final CurrencyUnit PLN = CurrencyUnit.of("PLN");
    private static final RiderId ANNA = new RiderId("anna");
    private static final CardToken VISA_4242 = new CardToken("tok_visa_4242");
    private static final PaymentMethodId VISA_ID = new PaymentMethodId("pm-visa");
    private static final CardToken DECLINED_0002 = new CardToken("tok_declined_0002");
    private static final PaymentMethodId DECLINED_ID = new PaymentMethodId("pm-declined");
    private static final CardToken MC_5555 = new CardToken("tok_mc_5555");
    private static final PaymentMethodId MC_ID = new PaymentMethodId("pm-mc");
    private static final CardToken NOFUNDS_0002 = new CardToken("tok_nofunds_0002");
    private static final PaymentMethodId NOFUNDS_ID = new PaymentMethodId("pm-nofunds");
    private static final Instant AT_10_10 = Instant.parse("2026-11-08T10:10:00Z");
    private static final Instant AT_10_12 = Instant.parse("2026-11-08T10:12:00Z");
    private static final Instant AT_10_30 = Instant.parse("2026-11-09T10:30:00Z");
    private static final Instant AT_08_00 = Instant.parse("2026-11-20T08:00:00Z");
    private static final Instant AT_08_30 = Instant.parse("2026-11-20T08:30:00Z");
    private static final ChargeId CHARGE_ID = new ChargeId("charge-1");
    private static final ChargeReference RIDE_S1 = new ChargeReference("S-1");

    private final FakePaymentProcessor processor = new FakePaymentProcessor();

    // @formatter:off
    @DisplayName(
        """
         given rider Anna has no payment method,
         when Anna's card tok_visa_4242 is verified and the processor accepts the PLN 1.00 pre-authorization,
         then the payment method is verified and is Anna's current card, and the PLN 1.00 is released
        """
    )
    // @formatter:on
    @Test
    void cardPassingPreAuthorizationIsVerifiedAndBecomesCurrent() {
        // given
        var methods = RiderPaymentMethods.none(ANNA);

        // when
        var events = methods.verify(VISA_4242).as(VISA_ID).at(AT_10_10).using(processor);

        // then
        assertThat(events.stream()).containsExactly(new PaymentMethodVerified(ANNA, VISA_ID, VISA_4242, AT_10_10));
        assertThat(processor.held).singleElement()
                                  .satisfies(hold -> assertThat(hold.amount()).isEqualTo(Money.of(PLN, 1)));
        assertThat(processor.released).containsExactly(processor.held.getFirst().hold());
    }

    // @formatter:off
    @DisplayName(
        """
         given rider Anna has no payment method,
         when Anna's card tok_declined_0002 is verified and the processor declines it,
         then the verification fails and nothing stays held on the card
        """
    )
    // @formatter:on
    @Test
    void declinedPreAuthorizationLeavesCardUnverified() {
        // given
        processor.declinesPreAuthorizationOf(DECLINED_0002);
        var methods = RiderPaymentMethods.none(ANNA);

        // when
        var events = methods.verify(DECLINED_0002).as(DECLINED_ID).at(AT_10_10).using(processor);

        // then
        assertThat(events.stream()).containsExactly(new PaymentMethodDeclined(ANNA, DECLINED_ID, DECLINED_0002, AT_10_10));
        assertThat(processor.released).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's current card is tok_visa_4242,
         when PLN 5.00 is charged for ride S-1 and the processor accepts it,
         then the charge succeeded on tok_visa_4242
        """
    )
    // @formatter:on
    @Test
    void chargeGoesToCurrentCard() {
        // given
        var methods = RiderPaymentMethods.none(ANNA);
        methods.verify(VISA_4242).as(VISA_ID).at(AT_10_10).using(processor);

        // when
        var events = methods.charge(Money.of(PLN, 5)).forReference(RIDE_S1).as(CHARGE_ID).at(AT_10_30).using(processor);

        // then
        assertThat(events.stream()).containsExactly(new ChargeSucceeded(ANNA, CHARGE_ID, VISA_ID, Money.of(PLN, 5), RIDE_S1, AT_10_30));
        assertThat(processor.charges).containsExactly(new FakePaymentProcessor.Charge(VISA_4242, Money.of(PLN, 5), RIDE_S1));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's only card tok_declined_0002 was declined, so she has no current card,
         when PLN 5.00 is charged for ride S-1,
         then the charge failed with reason no valid payment method, and the processor isn't called
        """
    )
    // @formatter:on
    @Test
    void chargeWithoutCurrentCardFailsWithoutCallingProcessor() {
        // given
        processor.declinesPreAuthorizationOf(DECLINED_0002);
        var methods = RiderPaymentMethods.none(ANNA);
        methods.verify(DECLINED_0002).as(DECLINED_ID).at(AT_10_10).using(processor);

        // when
        var events = methods.charge(Money.of(PLN, 5)).forReference(RIDE_S1).as(CHARGE_ID).at(AT_10_30).using(processor);

        // then
        assertThat(events.stream()).containsExactly(
            new ChargeFailed(ANNA, CHARGE_ID, Money.of(PLN, 5), RIDE_S1, ChargeFailureReason.NO_VALID_PAYMENT_METHOD, AT_10_30));
        assertThat(processor.charges).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         given Ben's current card is tok_nofunds_0002,
         when PLN 43.00 is charged for ride S-3 and the processor declines it,
         then the charge failed with reason declined
        """
    )
    // @formatter:on
    @Test
    void chargeDeclinedByProcessorFailsWithReasonDeclined() {
        // given
        var ben = new RiderId("ben");
        var rideS3 = new ChargeReference("S-3");
        processor.declinesChargesOf(NOFUNDS_0002);
        var methods = RiderPaymentMethods.none(ben);
        methods.verify(NOFUNDS_0002).as(NOFUNDS_ID).at(AT_10_10).using(processor);

        // when
        var events = methods.charge(Money.of(PLN, 43)).forReference(rideS3).as(CHARGE_ID).at(AT_10_30).using(processor);

        // then
        assertThat(events.stream()).containsExactly(
            new ChargeFailed(ben, CHARGE_ID, Money.of(PLN, 43), rideS3, ChargeFailureReason.DECLINED, AT_10_30));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's current card is tok_visa_4242,
         when Anna's card tok_mc_5555 is verified successfully,
         then tok_mc_5555 is Anna's current card and tok_visa_4242 is no longer charged
        """
    )
    // @formatter:on
    @Test
    void newlyVerifiedCardReplacesCurrentOne() {
        // given
        var methods = RiderPaymentMethods.none(ANNA);
        methods.verify(VISA_4242).as(VISA_ID).at(AT_10_10).using(processor);

        // when
        var events = methods.verify(MC_5555).as(MC_ID).at(AT_10_12).using(processor);

        // then
        assertThat(events.stream()).containsExactly(new PaymentMethodVerified(ANNA, MC_ID, MC_5555, AT_10_12),
                                                    new PaymentMethodReplaced(ANNA, VISA_ID, MC_ID, AT_10_12));
        methods.charge(Money.of(PLN, 5)).forReference(RIDE_S1).as(CHARGE_ID).at(AT_10_30).using(processor);
        assertThat(processor.charges).extracting(FakePaymentProcessor.Charge::card).containsExactly(MC_5555);
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's current card is tok_visa_4242,
         when her card tok_declined_0002 is verified and the processor declines it,
         then tok_visa_4242 stays her current card
        """
    )
    // @formatter:on
    @Test
    void declinedCardDoesNotReplaceCurrentOne() {
        // given
        processor.declinesPreAuthorizationOf(DECLINED_0002);
        var methods = RiderPaymentMethods.none(ANNA);
        methods.verify(VISA_4242).as(VISA_ID).at(AT_10_10).using(processor);

        // when
        var events = methods.verify(DECLINED_0002).as(DECLINED_ID).at(AT_10_12).using(processor);

        // then
        assertThat(events.stream()).containsExactly(new PaymentMethodDeclined(ANNA, DECLINED_ID, DECLINED_0002, AT_10_12));
        methods.charge(Money.of(PLN, 5)).forReference(RIDE_S1).as(CHARGE_ID).at(AT_10_30).using(processor);
        assertThat(processor.charges).extracting(FakePaymentProcessor.Charge::card).containsExactly(VISA_4242);
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's card tok_declined_0002 was declined at 10:10,
         when Anna's card tok_visa_4242 is verified at 10:12 and the processor accepts the pre-authorization,
         then tok_visa_4242 is Anna's current card
        """
    )
    // @formatter:on
    @Test
    void cardVerifiedAfterDeclineBecomesCurrent() {
        // given
        processor.declinesPreAuthorizationOf(DECLINED_0002);
        var methods = RiderPaymentMethods.none(ANNA);
        methods.verify(DECLINED_0002).as(DECLINED_ID).at(AT_10_10).using(processor);

        // when
        var events = methods.verify(VISA_4242).as(VISA_ID).at(AT_10_12).using(processor);

        // then
        assertThat(events.stream()).containsExactly(new PaymentMethodVerified(ANNA, VISA_ID, VISA_4242, AT_10_12));
        methods.charge(Money.of(PLN, 5)).forReference(RIDE_S1).as(CHARGE_ID).at(AT_10_30).using(processor);
        assertThat(processor.charges).extracting(FakePaymentProcessor.Charge::card).containsExactly(VISA_4242);
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's current card is tok_visa_4242,
         when the processor reports tok_visa_4242 expired,
         then the payment method is invalidated and Anna has no current card, so a charge fails with no valid payment method
        """
    )
    // @formatter:on
    @Test
    void invalidatedCurrentCardIsReportedAndLeavesNoCurrentCard() {
        // given
        var methods = RiderPaymentMethods.none(ANNA);
        methods.verify(VISA_4242).as(VISA_ID).at(AT_10_10).using(processor);

        // when
        var events = methods.invalidate(VISA_4242).because(InvalidationReason.EXPIRED).at(AT_08_00);

        // then
        assertThat(events.stream()).containsExactly(
            new PaymentMethodInvalidated(ANNA, VISA_ID, VISA_4242, InvalidationReason.EXPIRED, AT_08_00));
        var charge = methods.charge(Money.of(PLN, 5)).forReference(RIDE_S1).as(CHARGE_ID).at(AT_10_30).using(processor);
        assertThat(charge.stream()).containsExactly(
            new ChargeFailed(ANNA, CHARGE_ID, Money.of(PLN, 5), RIDE_S1, ChargeFailureReason.NO_VALID_PAYMENT_METHOD, AT_10_30));
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's current card is tok_mc_5555 and her earlier card tok_visa_4242 was replaced,
         when the processor reports tok_visa_4242 expired,
         then tok_visa_4242 is invalidated, but Payment Method Invalidated isn't published and Anna keeps her current card
        """
    )
    // @formatter:on
    @Test
    void oldCardBeingInvalidatedIsNotReported() {
        // given
        var methods = RiderPaymentMethods.none(ANNA);
        methods.verify(VISA_4242).as(VISA_ID).at(AT_10_10).using(processor);
        methods.verify(MC_5555).as(MC_ID).at(AT_10_12).using(processor);

        // when
        var events = methods.invalidate(VISA_4242).because(InvalidationReason.EXPIRED).at(AT_08_00);

        // then
        assertThat(events.stream()).containsExactly(
            new FormerPaymentMethodInvalidated(ANNA, VISA_ID, VISA_4242, InvalidationReason.EXPIRED, AT_08_00));
        methods.charge(Money.of(PLN, 5)).forReference(RIDE_S1).as(CHARGE_ID).at(AT_10_30).using(processor);
        assertThat(processor.charges).extracting(FakePaymentProcessor.Charge::card).containsExactly(MC_5555);
    }

    // @formatter:off
    @DisplayName(
        """
         given tok_visa_4242 was invalidated at 08:00,
         when the processor reports tok_visa_4242 expired again at 08:30,
         then nothing changes and Payment Method Invalidated isn't published again
        """
    )
    // @formatter:on
    @Test
    void secondReportForSameCardIsNotReportedAgain() {
        // given
        var methods = RiderPaymentMethods.none(ANNA);
        methods.verify(VISA_4242).as(VISA_ID).at(AT_10_10).using(processor);
        methods.invalidate(VISA_4242).because(InvalidationReason.EXPIRED).at(AT_08_00);

        // when
        var events = methods.invalidate(VISA_4242).because(InvalidationReason.EXPIRED).at(AT_08_30);

        // then
        assertThat(events.stream()).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         given Anna's current card is tok_visa_4242,
         when the processor reports a card she never added, tok_visa_9999, expired,
         then nothing happens
        """
    )
    // @formatter:on
    @Test
    void reportForUnknownCardChangesNothing() {
        // given
        var methods = RiderPaymentMethods.none(ANNA);
        methods.verify(VISA_4242).as(VISA_ID).at(AT_10_10).using(processor);

        // when
        var events = methods.invalidate(new CardToken("tok_visa_9999")).because(InvalidationReason.EXPIRED).at(AT_08_00);

        // then
        assertThat(events.stream()).isEmpty();
    }
}
