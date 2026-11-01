package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.*;
import pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.repository.InMemoryPriceCatalogRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Price ride use case examples")
class PriceRideUseCaseTest {

    // @formatter:off
    @DisplayName(
        """
         given a published price list with first-hour fee 5.00 PLN
          and Rentals reporting 20.00 PLN drop-zone charges for the session,
         when a 45 minute ride of George is priced,
         then ride priced with total fee 25.00 PLN is published
        """
    )
    // @formatter:on
    @Test
    void test0() {
        // given
        var repository = new InMemoryPriceCatalogRepository();
        var publisher = new RecordingPublisher();
        new PublishPriceListUseCase(repository, publisher).publish(
            new PriceList(new PriceListId("A"), Instant.parse("2026-11-01T00:00:00Z"), FreePeriod.ofMinutes(10),
                          new HourlyFees(pln("5.00"), pln("7.00"), pln("9.00"), pln("12.00"))));
        ReturnChargesProvider rentals = sessionId -> new ReturnCharges(pln("20.00"));
        var useCase = new PriceRideUseCase(repository, rentals, publisher);

        // when
        useCase.price(new Ride(new RentalSessionId("session01"), new RiderId("george"),
                               Instant.parse("2026-11-01T08:00:00Z"), Instant.parse("2026-11-01T08:45:00Z")));

        // then
        assertThat(publisher.published)
            .contains(new PricingEvent.RidePriced(new RentalSessionId("session01"), new RiderId("george"),
                                                  new PriceListId("A"), pln("25.00")));
    }

    private static Money pln(String amount) {
        return Money.parse("PLN " + amount);
    }

    private static class RecordingPublisher implements DomainEventPublisher {

        private final List<DomainEvent> published = new ArrayList<>();

        @Override
        public void publish(DomainEvents events) {
            events.stream().forEach(published::add);
        }

        @Override
        public void publish(DomainEvent event) {
            published.add(event);
        }
    }
}
