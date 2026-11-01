# CrowdSorcery system

CrowdSorcery is a crowdfunding/investment platform. Investors put money into assets or projects, either manually or through automated strategies, and borrowers finance projects and repay loans. The vision is in `crowdsorcery/CrowdSorcery.md` at the repo root. This code was copied from an earlier training as a **reference implementation** of tactical DDD and hexagonal architecture. The repo-root `CLAUDE.md` covers the package conventions.

## Bounded contexts

- Deposit, investment, payment, profile, fundraising, interest and simulation each have a `*BoundedContextMarker` interface.
- `deposit` is the most complete context and the canonical example of the package layout. Most of the others are thin sketches.
- The relationships between contexts are built in code in `contextmap/ContextMapperExample` (there is no CML file):
  - Deposit is upstream (OHS) of Investment, Fundraising and Interest.
  - Payment is upstream (OHS + PL) of Deposit.
  - Investment and Simulation are partners.

## Patterns worth knowing

- **Event flow inside deposit:**
  1. Use cases (`api/usecases`) load a `DepositAccount`, run a command that returns `DomainEvents`, save the account, and publish the events through `DomainEventPublisher`.
  2. `infra/event/LocalEventsHandler` receives the events with Spring `@EventListener` and routes them to policies (`DomainPolicy`), for example `DepositInitiatedPolicy` and `PaymentVerifiedPolicy`. The policies trigger the next step.
- **Wiring:** everything is wired by hand in `deposit/infra/config/DepositContextConfig`. The domain and use-case classes have no Spring annotations. Some beans there are placeholder lambdas and anonymous classes.
- **Anti-corruption layers:**
  - `deposit/infra/adapters/secondary/paymentctxacl` (annotated with `@AntiCorruptionLayer`) is the *only* place in deposit where payment-context types (`PaymentGateway`, `PaymentEvent`, ...) may appear. It translates them into deposit's own `PaymentProcessor` and `PaymentVerified`.
  - `investment/transfer/depositctxacl` does the same for Investment, adapting deposit's `TransferUseCase` to Investment's own `TransferOperations`.
  - Keep foreign types inside these packages.
- **Upstream events:** `deposit/api/events/DepositMade` is the published event. It is an `IntegrationEvent` (top-level `common/integration`) that carries primitives (account id, amount, currency), not domain types, and `investment/api/eventhandlers/InvestmentPolicy` consumes it. The internal `domain/account/DepositMade` is a different class.
- **Payment providers:** `payment` hides its providers (`stripe`, `paypal`, `crypto`, `wire`) behind `payment/api/PaymentGateway`.
- **Money:** amounts use `org.joda.money.Money`.
- **Persistence:** `profile` is the one JPA/Spring Data REST example (`Profile`, `ProfileRepository`). H2 is the runtime database.

Many methods throw `UnsupportedOperationException("implement this")` on purpose, because they are training exercises.
