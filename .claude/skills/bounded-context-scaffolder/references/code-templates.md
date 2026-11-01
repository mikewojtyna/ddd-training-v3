# Code templates

These are shapes, not text to paste. Fit the names to the ubiquitous language. In the examples below, `<base>` = `pl.wojtyna.dddtrainingv3.<system>.<context>`.

## Value objects and identifiers: records

```java
package <base>.domain.tariff;

import org.jmolecules.ddd.annotation.ValueObject;

import java.math.BigDecimal;
import java.util.Objects;

@ValueObject
public record PricePerMinute(BigDecimal amount) {

    public PricePerMinute {
        Objects.requireNonNull(amount, "amount");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Price per minute cannot be negative");
        }
    }
}
```

Validate inside the compact constructor, so an invalid value can't exist. For money, CrowdSorcery uses `org.joda.money.Money`. Use it when the canvas talks about amounts in a currency.

## Aggregate root: a class with behaviour, no getters

```java
package <base>.domain.tariff;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

@AggregateRoot
public class Tariff {

    private final TariffId id;
    private TariffStatus status;

    public Tariff(TariffId id) {
        this.id = id;
        this.status = TariffStatus.DRAFT;
    }

    public DomainEvents publish() {
        if (status == TariffStatus.PUBLISHED) {
            throw new TariffAlreadyPublished(id);
        }
        status = TariffStatus.PUBLISHED;
        return DomainEvents.of(new TariffEvent.TariffPublished(id));
    }

    @Identity
    public TariffId id() {
        return id;
    }
}
```

An entity that changes inside the aggregate is a private static nested class annotated `@Entity`, with `equals`/`hashCode` on its id (see `BikeStation.BikeInStation`). An entity that never changes may be a record.

## Domain events: a sealed hierarchy of records

```java
package <base>.domain.tariff;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

public sealed interface TariffEvent extends DomainEvent {

    record TariffPublished(TariffId tariffId) implements TariffEvent {
    }
}
```

## Rule violation: a dedicated exception

```java
package <base>.domain.tariff;

public class TariffAlreadyPublished extends RuntimeException {

    public TariffAlreadyPublished(TariffId id) {
        super("Tariff " + id.value() + " is already published");
    }
}
```

## Repository: a secondary port, and an in-memory adapter

```java
@Repository
@SecondaryPort
public interface TariffRepository {
    Optional<Tariff> load(TariffId id);
    void save(Tariff tariff);
}
```

```java
package <base>.infra.adapters.secondary;

@SecondaryAdapter
public class InMemoryTariffRepository implements TariffRepository {
    private final Map<TariffId, Tariff> tariffs = new ConcurrentHashMap<>();
    @Override public Optional<Tariff> load(TariffId id) { return Optional.ofNullable(tariffs.get(id)); }
    @Override public void save(Tariff tariff) { tariffs.put(tariff.id(), tariff); }
}
```

## Use case: a command record and a primary port

```java
package <base>.api.usecases;

public record PublishTariff(String tariffId) {
}
```

```java
package <base>.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;

@PrimaryPort
public class PublishTariffUseCase {

    private final TariffRepository tariffs;
    private final DomainEventPublisher eventPublisher;

    public PublishTariffUseCase(TariffRepository tariffs, DomainEventPublisher eventPublisher) {
        this.tariffs = tariffs;
        this.eventPublisher = eventPublisher;
    }

    public void handle(PublishTariff command) {
        var id = new TariffId(command.tariffId());
        var tariff = tariffs.load(id).orElseThrow(() -> new TariffNotFound(id));
        var events = tariff.publish();
        tariffs.save(tariff);
        eventPublisher.publish(events);
    }
}
```

If the context already uses another command style (for example Rentals' curried fluent API), follow that style in the aggregate. The use case still has the same four steps.

## Query: query, result and read-model port

```java
public record CurrentTariffQuery(String zoneId) {}
public record CurrentTariffView(String tariffId, BigDecimal pricePerMinute) {}

@SecondaryPort
public interface TariffReadModel {
    Optional<CurrentTariffView> currentFor(String zoneId);
}

@PrimaryPort
public class CurrentTariffQueryHandler {
    private final TariffReadModel readModel;
    public CurrentTariffQueryHandler(TariffReadModel readModel) { this.readModel = readModel; }
    public Optional<CurrentTariffView> handle(CurrentTariffQuery query) { return readModel.currentFor(query.zoneId()); }
}
```

## Published integration event and its publisher

```java
package <base>.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

@ValueObject
public record TariffPublished(String tariffId) implements IntegrationEvent {
}
```

```java
package <base>.infra.adapters.secondary;

@SecondaryAdapter
public class IntegrationEventsPublisher {
    private final ApplicationEventPublisher publisher;
    public IntegrationEventsPublisher(ApplicationEventPublisher publisher) { this.publisher = publisher; }

    @EventListener
    public void on(TariffEvent.TariffPublished event) {
        publisher.publishEvent(new <base>.api.events.TariffPublished(event.tariffId().value()));
    }
}
```

## Outbound ACL: a port in our language, an adapter in theirs

```java
// domain/rental/ZoneMap.java: our language
@SecondaryPort
public interface ZoneMap {
    boolean isInsideServiceArea(Location location);
}
```

```java
// infra/adapters/secondary/cityacl/package-info.java
/// Anti-corruption layer towards the City zone-map provider. This is the only package where
/// the City's published language may appear. It translates it into Rentals' `ZoneMap`.
@AntiCorruptionLayer
package <base>.infra.adapters.secondary.cityacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
```

```java
// infra/adapters/secondary/cityacl/CityZoneMapAdapter.java
@SecondaryAdapter
public class CityZoneMapAdapter implements ZoneMap {
    @Override
    public boolean isInsideServiceArea(Location location) {
        // Canvas outbound query: "Is location inside service area?" → City
        throw new UnsupportedOperationException("implement this");
    }
}
```

## Inbound event ACL: a primary adapter that translates

```java
// infra/adapters/primary/paymentsctxacl/PaymentsEventsListener.java
@PrimaryAdapter
public class PaymentsEventsListener {
    private final SettleRentalUseCase settleRental;
    public PaymentsEventsListener(SettleRentalUseCase settleRental) { this.settleRental = settleRental; }

    @EventListener
    public void on(pl.wojtyna.dddtrainingv3.<system>.payments.api.events.PaymentCaptured event) {
        settleRental.handle(new PaymentReceived(event.rentalId(), event.amount()));
    }
}
```

## Wiring

```java
@Configuration
public class PricingContextConfig {

    @Bean
    public TariffRepository pricingTariffRepository() {
        return new InMemoryTariffRepository();
    }

    @Bean
    public PublishTariffUseCase pricingPublishTariffUseCase(TariffRepository pricingTariffRepository,
                                                            DomainEventPublisher eventPublisher) {
        return new PublishTariffUseCase(pricingTariffRepository, eventPublisher);
    }
}
```

## Test for a strict GWT rule

```java
package <base>.domain.tariff;

@DisplayName("Tariff examples")
class TariffTest {

    // @formatter:off
    @DisplayName(
        """
         given a published tariff,
         when the tariff is published again,
         then publishing is rejected
        """
    )
    // @formatter:on
    @Test
    void publishedTariffCannotBePublishedAgain() {
        // given
        var tariff = new Tariff(new TariffId("T-1"));
        tariff.publish();

        // when
        var exception = catchThrowable(tariff::publish);

        // then
        assertThat(exception).isInstanceOf(TariffAlreadyPublished.class);
    }
}
```

Use AssertJ (`assertThat`, `catchThrowable`). Assert on the events with `DomainEvents.hasOccurredEventOfType(...)` or `hasAtLeastOneEventMatching(...)`, and wrap the result in `assertThat(...).isTrue()`. Don't leave the boolean unused.
