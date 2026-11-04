# Other tactical building blocks

Conventions for the blocks besides the aggregate: domain services, policies, factories, events, repositories and the use cases around them. The canonical in-repo examples are in CrowdSorcery's `deposit` context (`domain/account`, `domain/payment`, `api/usecases`, `infra/event/LocalEventsHandler`, `infra/config/DepositContextConfig`). Prefer those shapes, and fix their known training gaps (methods that throw `UnsupportedOperationException`, `CreateAccountUseCase` returning `null`) rather than copying them.

## Contents
1. Domain events vs integration events
2. Use-case orchestration: load → command → save → publish
3. Event publishing
4. Policies
5. Repositories
6. Factories
7. Wiring
8. Domain services

---

## 1. Domain events vs integration events

| | Domain event | Integration event |
|---|---|---|
| Purpose | Something that happened inside this context | Published language for other contexts |
| Type | record implementing `common.domain.DomainEvent`, nested in the aggregate's sealed `<Aggregate>Event` interface | record implementing `common.integration.IntegrationEvent` |
| Location | `<ctx>/domain/<aggregate>/` | `<ctx>/api/events/` |
| Payload | identities and value objects of this context | primitives only (strings, numbers, ISO codes), so consumers don't depend on our domain types |
| Name | past tense, ubiquitous language: `BikeRented`, `DepositConfirmed` | past tense, often coarser: `DepositMade` |

Rules:
- An event states a fact that happened, so name it in the past tense. `RentBike` is a command, not an event.
- Carry what consumers need to react without loading the aggregate back, but never the aggregate or a mutable entity itself.
- Translating a domain event into an integration event is the job of a small policy or handler in `infra`, not of the aggregate.
- `@DomainEvent` from `jmolecules-events` isn't on the classpath. Implementing `DomainEvent` is the marker.

## 2. Use-case orchestration

A use case is a `@PrimaryPort` class in `<ctx>/api/usecases/`, one per use case. It's a plain class: Spring wiring happens in `infra/config`. It runs exactly one aggregate command per transaction:

```java
@PrimaryPort
public class AddItemToOrderUseCase {

    private final OrderRepository orders;
    private final DomainEventPublisher eventPublisher;

    public AddItemToOrderUseCase(OrderRepository orders, DomainEventPublisher eventPublisher) {
        this.orders = orders;
        this.eventPublisher = eventPublisher;
    }

    public void addItem(OrderId orderId, ProductId product, Quantity quantity, Price price) {
        var order = orders.load(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        var events = order.add(product).times(quantity).at(price);
        orders.save(order);              // event-sourced: orders.save(order, events), where events = all pending events
        eventPublisher.publish(events);
    }
}
```

- **One aggregate per transaction.** If a use case wants to change two aggregates, change one and let a policy react to its event and change the other (eventual consistency). This is the same reason aggregates don't reference each other.
- Inputs that an invariant needs from elsewhere, such as a price from a pricing service, are fetched by the use case through a secondary port and passed into the command as value objects. The aggregate never calls the port itself.
- Save before publishing, so that consumers never react to something that wasn't stored. If losing an event between those two steps matters in production, the answer is a transactional outbox inside the repository adapter. Mention it as a suggestion, and don't build it unless asked.
- `OptimisticLockingException` propagates out of the use case. Retrying (reload and re-run the command) is a decision for the caller or an infra decorator, not for the domain.

## 3. Event publishing

- There is exactly one publishing abstraction: `common.domain.DomainEventPublisher` (`@SecondaryPort`), implemented by `common.infra.spring.SpringEventPublisher` and wired in `CommonInfraConfig`. Inject it, and don't create another publisher per context or per aggregate.
- Aggregates never publish. They return `DomainEvents`, and use cases and policies publish them.
- If the user asks for a "new event publisher", find out what they actually need:
  - **A different transport** (Kafka, an outbox, a message broker) is a new adapter implementing `DomainEventPublisher`, in `common/infra/<tech>` if it's shared or in `<ctx>/infra/adapters/secondary` if it's context-specific. Swap it in through Spring config.
  - **Publishing to other contexts** means mapping a domain event to an `IntegrationEvent` in an infra handler and publishing that.
  - **A test double** is a recording publisher in `src/test`, e.g. `class RecordingEventPublisher implements DomainEventPublisher` that keeps a list of everything published.

## 4. Policies

A policy is a business rule of the form **"whenever <event>, then <command>"**. It's the glue for rules that span aggregates or contexts, and those are eventually consistent.

```java
public class RideEndedPolicy implements DomainPolicy {

    private final RiderAccountRepository accounts;
    private final DomainEventPublisher eventPublisher;

    public RideEndedPolicy(RiderAccountRepository accounts, DomainEventPublisher eventPublisher) {
        this.accounts = accounts;
        this.eventPublisher = eventPublisher;
    }

    public void handle(RideEvent.RideEnded rideEnded) {
        accounts.load(rideEnded.riderId()).ifPresent(account -> {
            var events = account.charge(rideEnded.fee());
            accounts.save(account);
            eventPublisher.publish(events);
        });
    }
}
```

- The class implements `common.domain.DomainPolicy`, lives in the `domain` package of the aggregate it *acts on*, has no Spring annotations, and is named `<Event>Policy` or after the business rule.
- One policy reacts to one event type and triggers one command, or calls one secondary port (e.g. `DepositInitiatedPolicy` → `PaymentProcessor`). If it branches into several unrelated actions, split it.
- It contains no invariant checks. Those belong to the aggregate whose command it calls. If the policy needs a new aggregate command, that command goes through the rule gate (invariant + GWT) like any other.
- Routing events to policies is infrastructure: a `LocalEventsHandler`-style class in `<ctx>/infra/event/` with Spring `@EventListener` methods that delegate to `policy.handle(event)`.
- A policy carries a business rule, so it is built in the TDD checkpoint loop like any other rule (SKILL.md Step 4), one scenario per cycle.
- Test policies in GWT style with the in-memory repository and a recording publisher, or a fake secondary port: *given* an aggregate in some state, *when* the policy handles the event, *then* the expected events were published, the port was called, and/or the reloaded aggregate behaves accordingly. Both test doubles live in `src/test`.

```java
// @formatter:off
@DisplayName(
    """
     given a paid order O1,
     when OrderPaid is handled,
     then the delivery of O1 is started
    """
)
// @formatter:on
@Test
void paidOrderStartsDelivery() {
    // given
    var deliveries = new RecordingDeliveries();            // fake of the Deliveries secondary port
    var policy = new StartDeliveryWhenOrderPaidPolicy(deliveries);

    // when
    policy.handle(new OrderEvent.OrderPaid(new OrderId("O1")));

    // then
    assertThat(deliveries.started()).containsExactly(new OrderId("O1"));
}
```

For the compile-only red step, `handle` gets an empty body: the fake records nothing, so the assertion fails for the right reason.

## 5. Repositories

- There is one repository per aggregate, and only aggregates get repositories (not entities, not value objects).
- The port is an interface next to the aggregate in `domain/<aggregate>/`, annotated with `@Repository @SecondaryPort`, with `Optional<A> load(AId id)` and `void save(A aggregate)`. Event-sourced aggregates use `void save(A aggregate, DomainEvents newEvents)`, where `newEvents` is the last command's result, which holds all pending events (`aggregate-templates.md` §6).
- Use the collection-like vocabulary already in the repo (`load` / `save`). Don't add `findAllBy...` query methods: lists and searches for screens are read models (`api/queries`), not aggregate loading.
- `save` enforces the `Version` contract (see `aggregate-templates.md` §1 and §8) and throws `OptimisticLockingException` on conflict. Write that contract in the port's javadoc so every adapter honours it.
- Adapters go in `<ctx>/infra/adapters/secondary/` with `@SecondaryAdapter`. Start with in-memory; JPA/JDBC adapters come later and map to their own persistence classes, so the domain class never gets JPA annotations.

## 6. Factories

Pick the lightest option that keeps creation rules in the domain:

1. **Static factory method on the aggregate** (`Order.open(id)`, `Order.rebuild(id, history)`). This is the default. Its name comes from the ubiquitous language, so `open`, `register` or `install` rather than `create`. Use it when creation needs only the arguments.
2. **A separate `@Factory` class** (`org.jmolecules.ddd.annotation.Factory`) in `domain/<aggregate>/`. Use it when creation itself has rules or needs information from outside, such as generating identities through a port or choosing a variant from a policy. It may take secondary ports in its constructor. It returns the new aggregate together with its creation events:

```java
@Factory
public class OrderFactory {

    private final OrderIdGenerator ids;

    public OrderFactory(OrderIdGenerator ids) {
        this.ids = ids;
    }

    public NewOrder openFor(CustomerId customer) {
        var order = Order.open(ids.next());
        return new NewOrder(order, DomainEvents.of(new OrderEvent.OrderOpened(order.id(), customer)));
    }

    public record NewOrder(Order created, DomainEvents events) {
    }
}
```

If a creation rule is an invariant ("an order is opened with at least one item"), it goes through the rule gate like any other invariant, and the factory method's test is its GWT scenario.

A factory with rules is built in the TDD checkpoint loop (SKILL.md Step 4). Its test calls the factory and asserts the creation events, the created aggregate's first behaviour, or the rejection: *given* the inputs (and a fake id generator, if there is a port), *when* the factory creates the aggregate, *then* `NewOrder.events()` contains exactly `OrderOpened` with the expected id. For the compile-only red step the factory returns `null` events or `DomainEvents.empty()`.

## 7. Wiring

Domain classes, policies, factories and use cases carry no Spring annotations. Register them as `@Bean`s in the context's `infra/config/<Context>Config` (see `DepositContextConfig`), together with the repository adapter and the event-routing handler. Tests construct everything by hand. Spring is not needed to test domain behaviour.

## 8. Domain services

A domain service holds a **business decision or calculation that owns no state** and isn't naturally the job of one aggregate. Typical signals: the rule combines values from several places ("the delivery fee depends on the order total and the zone"), or it is a pure piece of domain knowledge the expert can state on its own ("delivery is free above 50 PLN, otherwise 9.99 PLN").

Pick it only when the alternatives don't fit:
- **Aggregate method** instead, if the rule only reads that aggregate's own state, or guards it (that's an invariant).
- **Policy** instead, if the rule is a reaction: "whenever X happens, then do Y".

Shape:
- A plain class annotated `@Service` (`org.jmolecules.ddd.annotation.Service`) in `<ctx>/domain/<area>/`, next to the types it works with, named after what it decides or calculates in the ubiquitous language (`DeliveryFeeCalculator`, `TechnicianAssignment`), not `*Manager`/`*Helper`.
- **Stateless.** Value objects in, a value object or a decision out. No fields except constants or other stateless collaborators.
- **No repositories, no publishing, no Spring.** The use case loads what the service needs, calls it, and passes the result into an aggregate command if a state change follows. If a service seems to need data it can't be given, it may depend on a secondary port in our language (like a pricing table), but ask the user first: that's a design decision.

```java
@Service
public class DeliveryFeeCalculator {

    private static final Money FREE_DELIVERY_THRESHOLD = Money.of(CurrencyUnit.of("PLN"), 50);
    private static final Money STANDARD_FEE = Money.of(CurrencyUnit.of("PLN"), new BigDecimal("9.99"));

    public Money feeFor(Money orderTotal) {
        return orderTotal.isGreaterThan(FREE_DELIVERY_THRESHOLD) ? Money.zero(CurrencyUnit.of("PLN")) : STANDARD_FEE;
    }
}
```

Tests: one GWT test per scenario in `<Service>Test`, calling the service directly, built in the TDD checkpoint loop (SKILL.md Step 4). For the compile-only red step the method returns `null` or a zero value, so the assertion fails for the right reason.

```java
// @formatter:off
@DisplayName(
    """
     given an order total of 60 PLN,
     when the delivery fee is calculated,
     then delivery is free
    """
)
// @formatter:on
@Test
void deliveryIsFreeAbove50Pln() {
    // given
    var calculator = new DeliveryFeeCalculator();

    // when
    var fee = calculator.feeFor(Money.of(PLN, 60));

    // then
    assertThat(fee).isEqualTo(Money.zero(PLN));
}
```

Wiring: like policies and factories, a domain service is registered as a `@Bean` in the context's `infra/config` only if a use case needs it injected; it can also simply be constructed by the use case's config.

