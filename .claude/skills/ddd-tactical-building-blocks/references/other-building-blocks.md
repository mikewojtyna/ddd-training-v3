# Other tactical building blocks

Conventions for everything around the aggregate. The canonical in-repo examples are in CrowdSorcery's `deposit` context (`domain/account`, `domain/payment`, `api/usecases`, `infra/event/LocalEventsHandler`, `infra/config/DepositContextConfig`). Prefer those shapes, and fix their known training gaps (methods that throw `UnsupportedOperationException`, `CreateAccountUseCase` returning `null`) rather than copying them.

## Contents
1. Domain events vs integration events
2. Use-case orchestration: load → command → save → publish
3. Event publishing
4. Policies
5. Repositories
6. Factories
7. Wiring

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
        orders.save(order);              // event-sourced: orders.save(order, events)
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
- It contains no invariant checks. Those belong to the aggregate whose command it calls. If the policy needs a new aggregate command, that command goes through the aggregate gate (invariant + GWT) like any other.
- Routing events to policies is infrastructure: a `LocalEventsHandler`-style class in `<ctx>/infra/event/` with Spring `@EventListener` methods that delegate to `policy.handle(event)`.
- Test policies in GWT style with the in-memory repository and a recording publisher: *given* an aggregate in some state, *when* the policy handles the event, *then* the expected events were published and/or the reloaded aggregate behaves accordingly.

## 5. Repositories

- There is one repository per aggregate, and only aggregates get repositories (not entities, not value objects).
- The port is an interface next to the aggregate in `domain/<aggregate>/`, annotated with `@Repository @SecondaryPort`, with `Optional<A> load(AId id)` and `void save(A aggregate)`. Event-sourced aggregates use `void save(A aggregate, DomainEvents newEvents)`.
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

If a creation rule is an invariant ("an order is opened with at least one item"), it goes through the aggregate gate like any other invariant, and the factory method's test is its GWT scenario.

## 7. Wiring

Domain classes, policies, factories and use cases carry no Spring annotations. Register them as `@Bean`s in the context's `infra/config/<Context>Config` (see `DepositContextConfig`), together with the repository adapter and the event-routing handler. Tests construct everything by hand. Spring is not needed to test domain behaviour.
