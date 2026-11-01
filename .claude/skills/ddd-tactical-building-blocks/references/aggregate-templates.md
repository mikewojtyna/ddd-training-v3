# Aggregate templates

Java shapes for aggregates in this repo. The running example is an `Order` aggregate with one invariant:

> **The total of an unconfirmed order never exceeds 100 USD.**
> Given an unconfirmed order worth 90 USD, When the customer adds 1 item at 20 USD, Then the item is rejected.

Replace names and packages with the user's ubiquitous language. `<ctx>` is the bounded-context package, e.g. `pl.wojtyna.dddtrainingv3.velocity.rentals`.

## Contents
1. Shared types: `Version`, `OptimisticLockingException`
2. Identity, value objects, exceptions
3. Domain events
4. Fluent (chained) command language
5. State-based aggregate
6. Event-sourced aggregate
7. Tests
8. Repository port and in-memory adapters

---

## 1. Shared types (top-level `common/domain`)

Create these once, if they don't exist yet. They are shared by every system, so don't duplicate them per context.

```java
package pl.wojtyna.dddtrainingv3.common.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/** Version of an aggregate as it was loaded. Repositories compare it on save to detect concurrent modification. */
@ValueObject
public record Version(long value) {

    public Version {
        if (value < 0) {
            throw new IllegalArgumentException("Version cannot be negative");
        }
    }

    public static Version initial() {
        return new Version(0);
    }

    public Version next() {
        return new Version(value + 1);
    }

    public Version plus(long changes) {
        return new Version(value + changes);
    }
}
```

```java
package pl.wojtyna.dddtrainingv3.common.domain;

/** Thrown by repositories when an aggregate was modified by someone else after it was loaded. */
public class OptimisticLockingException extends RuntimeException {

    public OptimisticLockingException(String message) {
        super(message);
    }
}
```

**The versioning contract, the same for both styles:** `aggregate.version()` is the version the aggregate had **when it was loaded or created**. Commands never touch it. The repository accepts a save only if the stored version still equals `aggregate.version()`, and it then stores the next version (state-based: `+1`; event-sourced: `+ number of new events`). After a save, callers reload before issuing more commands. This keeps the domain free of infrastructure while each adapter (in-memory, JDBC `WHERE version = ?`, an event store's expected-version append) enforces the same rule.

## 2. Identity, value objects, exceptions

```java
public record OrderId(String value) {
}

@ValueObject
public record Price(Money amount) {
}
```

Name each exception after the rule it protects, not after a technical failure. Use the same two constructors as the reference:

```java
public class UnconfirmedOrderLimitExceededException extends RuntimeException {

    public UnconfirmedOrderLimitExceededException(String message) {
        super(message);
    }

    public UnconfirmedOrderLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

## 3. Domain events

There is one sealed interface per aggregate, with past-tense records nested inside, as in `BikeRentalEvent`. Events carry identities and value objects only, never the aggregate or an entity.

```java
public sealed interface OrderEvent extends DomainEvent {

    record OrderOpened(OrderId orderId) implements OrderEvent {
    }

    record ItemAdded(OrderId orderId, ProductId productId, Quantity quantity, Price price) implements OrderEvent {
    }

    record OrderConfirmed(OrderId orderId) implements OrderEvent {
    }
}
```

## 4. Fluent (chained) command language

A command with more than one argument is split into steps that read as a sentence:
`order.add(product).times(quantity).at(price)`. Each step is a generic functional interface in the package's `FluentLanguage`, as in the reference. Reuse step types that already exist there, and add new ones next to them.

```java
public interface FluentLanguage {

    @FunctionalInterface
    interface Times<S, R> {

        R times(S s);
    }

    @FunctionalInterface
    interface At<S, R> {

        R at(S s);
    }
}
```

The command method returns the nested chain, and the last lambda does the work:

```java
public Times<Quantity, At<Price, DomainEvents>> add(ProductId product) {
    return quantity -> price -> { /* check invariants, change state, return events */ };
}
```

Nothing happens until the last step is called. That's fine, because the chain is only ever used as one expression. Single-argument commands stay plain methods: `public DomainEvents confirm()`.

## 5. State-based aggregate

```java
@AggregateRoot
public class Order {

    private static final Money UNCONFIRMED_LIMIT = Money.of(CurrencyUnit.USD, 100);

    private final OrderId id;
    private final Version version;
    private Money total;          // needed by: unconfirmed total <= 100 USD
    private boolean confirmed;    // needed by: unconfirmed total <= 100 USD

    private Order(OrderId id, Version version, Money total, boolean confirmed) {
        this.id = id;
        this.version = version;
        this.total = total;
        this.confirmed = confirmed;
    }

    public static Order open(OrderId id) {
        return new Order(id, Version.initial(), Money.zero(CurrencyUnit.USD), false);
    }

    public Times<Quantity, At<Price, DomainEvents>> add(ProductId product) {
        return quantity -> price -> {
            var newTotal = total.plus(price.amount().multipliedBy(quantity.value()));
            if (!confirmed && newTotal.isGreaterThan(UNCONFIRMED_LIMIT)) {
                throw new UnconfirmedOrderLimitExceededException("Unconfirmed order cannot exceed " + UNCONFIRMED_LIMIT);
            }
            total = newTotal;
            return DomainEvents.of(new OrderEvent.ItemAdded(id, product, quantity, price));
        };
    }

    @Identity
    public OrderId id() {
        return id;
    }

    public Version version() {
        return version;
    }
}
```

Notes:
- Comment each field with the invariant that needs it, at least while the design is young. A field without such a comment is a candidate for removal.
- There are no getters for state. Tests observe behaviour through returned events and thrown exceptions, not through fields.
- Rebuilding from storage needs a way to get the state in and out. Add that **only when a persistence adapter is actually built**, as a memento (section 8). Don't add it up front.

## 6. Event-sourced aggregate

Commands validate against current state, create events, `apply` them to their own state, and return them. `apply` is the only place that mutates state, and it never validates or throws, because past events are facts. Rebuilding the aggregate replays history through the same `apply`.

```java
@AggregateRoot
public class Order {

    private static final Money UNCONFIRMED_LIMIT = Money.of(CurrencyUnit.USD, 100);

    private final OrderId id;
    private final Version version;
    private Money total = Money.zero(CurrencyUnit.USD);
    private boolean confirmed;

    private Order(OrderId id, Version version) {
        this.id = id;
        this.version = version;
    }

    /** Creation is an event too: without it, a new aggregate has no stream to store or load. */
    public static NewOrder open(OrderId id) {
        var order = new Order(id, Version.initial());
        return new NewOrder(order, order.emit(new OrderEvent.OrderOpened(id)));
    }

    public record NewOrder(Order created, DomainEvents events) {
    }

    public static Order rebuild(OrderId id, List<OrderEvent> history) {
        var order = new Order(id, Version.initial().plus(history.size()));
        history.forEach(order::apply);
        return order;
    }

    public Times<Quantity, At<Price, DomainEvents>> add(ProductId product) {
        return quantity -> price -> {
            var newTotal = total.plus(price.amount().multipliedBy(quantity.value()));
            if (!confirmed && newTotal.isGreaterThan(UNCONFIRMED_LIMIT)) {
                throw new UnconfirmedOrderLimitExceededException("Unconfirmed order cannot exceed " + UNCONFIRMED_LIMIT);
            }
            return emit(new OrderEvent.ItemAdded(id, product, quantity, price));
        };
    }

    private DomainEvents emit(OrderEvent event) {
        apply(event);
        return DomainEvents.of(event);
    }

    private void apply(OrderEvent event) {
        switch (event) {
            case OrderEvent.OrderOpened opened -> {
            }
            case OrderEvent.ItemAdded added ->
                total = total.plus(added.price().amount().multipliedBy(added.quantity().value()));
            case OrderEvent.OrderConfirmed confirmedEvent -> confirmed = true;
        }
    }

    @Identity
    public OrderId id() {
        return id;
    }

    public Version version() {
        return version;
    }
}
```

Notes:
- The sealed interface plus the exhaustive `switch` makes the compiler flag a new event type that `apply` doesn't handle.
- Events must carry everything `apply` needs to rebuild the state the invariants read, and nothing beyond that.
- **Version = number of events in the stream at load time.** A freshly opened order is at version 0 with one pending `OrderOpened`. After it's saved, the stream holds 1 event and a reload is at version 1.
- The use case for creation saves `newOrder.created()` with `newOrder.events()` and publishes the same events.
- In ES tests, the "given" can be a history: `Order.rebuild(id, List.of(new ItemAdded(...)))`. That's often clearer than replaying commands.

## 7. Tests

The test lives in `src/test/java/<same package>/<Aggregate>Test.java`, in the `BikeStationTest` style, with one test per scenario. Method names describe the rule. Always assert. A bare `events.hasAtLeastOneEventMatching(...)` checks nothing.

```java
@DisplayName("Order examples")
class OrderTest {

    // @formatter:off
    @DisplayName(
        """
         given an unconfirmed order worth 90 USD,
         when the customer adds 1 item at 20 USD,
         then the item is rejected
        """
    )
    // @formatter:on
    @Test
    void unconfirmedOrderCannotExceedLimit() {
        // given
        var order = Order.open(new OrderId("order-1"));
        order.add(new ProductId("p1")).times(new Quantity(1)).at(new Price(Money.of(USD, 90)));

        // when
        var exception = catchThrowable(() -> order.add(new ProductId("p2"))
                                                  .times(new Quantity(1))
                                                  .at(new Price(Money.of(USD, 20))));

        // then
        assertThat(exception).isInstanceOf(UnconfirmedOrderLimitExceededException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given an unconfirmed order worth 90 USD,
         when the customer adds 1 item at 10 USD,
         then the item is added
        """
    )
    // @formatter:on
    @Test
    void unconfirmedOrderCanReachLimit() {
        // given
        var order = Order.open(new OrderId("order-1"));
        order.add(new ProductId("p1")).times(new Quantity(1)).at(new Price(Money.of(USD, 90)));

        // when
        var events = order.add(new ProductId("p2")).times(new Quantity(1)).at(new Price(Money.of(USD, 10)));

        // then
        assertThat(events.hasOccurredEventOfType(OrderEvent.ItemAdded.class)).isTrue();
    }
}
```

Imports: `org.assertj.core.api.Assertions.assertThat` and `catchThrowable`, `org.junit.jupiter.api.*`.

The GWT text in `@DisplayName` should be the user's scenario, nearly word for word. It's their specification.

## 8. Repository port and in-memory adapters

The **port** lives next to the aggregate, as in `AccountRepository` and `BikeStationRepo`:

```java
@Repository
@SecondaryPort
public interface OrderRepository {

    Optional<Order> load(OrderId id);

    /**
     * Stores the order.
     *
     * @throws OptimisticLockingException if the stored version differs from {@code order.version()}
     */
    void save(Order order);
}
```

For **event-sourced** aggregates, `save` takes the new events, because they are what gets stored:

```java
    /** Appends {@code newEvents}; fails with {@link OptimisticLockingException} unless the stream is still at {@code order.version()}. */
    void save(Order order, DomainEvents newEvents);
```

**In-memory adapters** go in `<ctx>/infra/adapters/secondary/` with `@SecondaryAdapter`. Build one only when the user wants it, or when a use case or policy test needs it.

Event-sourced, which is straightforward:

```java
@SecondaryAdapter
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, List<OrderEvent>> streams = new HashMap<>();

    @Override
    public Optional<Order> load(OrderId id) {
        return Optional.ofNullable(streams.get(id)).map(history -> Order.rebuild(id, List.copyOf(history)));
    }

    @Override
    public void save(Order order, DomainEvents newEvents) {
        var stream = streams.computeIfAbsent(order.id(), id -> new ArrayList<>());
        if (stream.size() != order.version().value()) {
            throw new OptimisticLockingException("Order " + order.id().value() + " was modified concurrently");
        }
        newEvents.stream().map(OrderEvent.class::cast).forEach(stream::add);
    }
}
```

State-based adapters need the state out of the aggregate and back in. Use a **memento** record, added only together with the adapter. It's persistence plumbing, so keep it at the bottom of the class and don't use it in domain logic:

```java
// in Order
public record Snapshot(OrderId id, Version version, Money total, boolean confirmed) {
}

public Snapshot snapshot() {
    return new Snapshot(id, version, total, confirmed);
}

public static Order restore(Snapshot snapshot) {
    return new Order(snapshot.id(), snapshot.version(), snapshot.total(), snapshot.confirmed());
}
```

```java
@SecondaryAdapter
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, Order.Snapshot> orders = new HashMap<>();

    @Override
    public Optional<Order> load(OrderId id) {
        return Optional.ofNullable(orders.get(id)).map(Order::restore);
    }

    @Override
    public void save(Order order) {
        var stored = orders.get(order.id());
        var storedVersion = stored == null ? Version.initial() : stored.version();
        if (!storedVersion.equals(order.version())) {
            throw new OptimisticLockingException("Order " + order.id().value() + " was modified concurrently");
        }
        var snapshot = order.snapshot();
        orders.put(order.id(), new Order.Snapshot(snapshot.id(), order.version().next(), snapshot.total(), snapshot.confirmed()));
    }
}
```

When an adapter exists, add one optimistic-locking test for it:
1. Create the aggregate and save it.
2. Load it twice.
3. Run a valid command on each copy.
4. Save the first copy: it succeeds.
5. Save the second copy: it throws `OptimisticLockingException`.

Choose the commands so that both are valid on the loaded state. The conflict should come from the version, not from an invariant.
