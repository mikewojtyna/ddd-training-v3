# Canvas → code mapping

Paths are relative to `src/main/java/pl/wojtyna/dddtrainingv3/<system>/<context>/`. The canonical example of the layout is CrowdSorcery's `deposit` context. Look at it whenever this file leaves something open.

## Contents
1. Package layout
2. Name, purpose, classification, roles, language → `package-info.java`
3. Inbound communication → `api/usecases`, `api/queries`, primary adapters
4. Outbound communication → `api/events`, secondary ports and ACL adapters
5. Ubiquitous language and business decisions → `domain/`
6. Wiring → `infra/config`
7. Several modules in one context (only when the user insists)

---

## 1. Package layout

```
<context>/
  package-info.java                     @BoundedContext(name = "<Canvas Name>") + @Module, canvas javadoc
  api/usecases/                         one @PrimaryPort class per inbound command/event, plus its command record
  api/queries/                          one @PrimaryPort handler per inbound query, plus query and result records and a read-model port
  api/events/                           published integration events (records implementing IntegrationEvent)
  domain/<aggregate>/                   aggregates, entities, value objects, domain events, repositories, policies, outbound ports
  infra/adapters/primary/               REST controllers (@PrimaryAdapter)
  infra/adapters/primary/<ctx>ctxacl/   listeners for upstream events that translate them into our commands (@AntiCorruptionLayer)
  infra/adapters/secondary/             repositories (in-memory), the integration-event publisher
  infra/adapters/secondary/<ctx>ctxacl/ adapters for outbound queries/commands to <ctx> (@AntiCorruptionLayer)
  infra/config/<Context>ContextConfig   Spring @Configuration, hand wiring
```

Create only the packages the canvas needs. Empty packages are noise, and Java doesn't keep them anyway.

## 2. `package-info.java`

`@BoundedContext(name = ...)` takes the canvas **Name** exactly, for example `"Debt Recovery"`. Write the javadoc as a Markdown doc comment (`///`, supported since Java 23), so headings and lists stay readable in the source and in the IDE. Use the sections below, in this order, and leave out any section that has no content in the canvas. Don't write "N/A" placeholders.

```
/// # <Name> bounded context
///
/// ## Purpose
/// <purpose, business perspective>
///
/// ## Strategic classification
/// - Domain: <core|supporting|generic|...>
/// - Business model: <revenue|engagement|compliance|cost reduction>
/// - Evolution: <genesis|custom built|product|commodity>
///
/// ## Domain roles
/// - <worksheet trait>: <one-line reason if the canvas gives one>
///
/// ## Ubiquitous language
/// - **<Term>**: <definition>
///
/// ## Business decisions
/// - **<Rule name>**: given <...>, when <...>, then <...>. Enforced by `<Aggregate>`, see `<Test>#<method>`.
/// - **<Rule name>** (*not modelled yet*): <rule as written>. Awaiting a strict Given/When/Then example, see the plan.
///
/// ## Assumptions
/// - ...
///
/// ## Verification metrics
/// - ...
///
/// ## Open questions
/// - ...
///
/// Source canvas: `docs/bounded-contexts/<system>/<context>/canvas.md`
@BoundedContext(name = "<Name>")
@Module
package pl.wojtyna.dddtrainingv3.<system>.<context>;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
```

## 3. Inbound communication

| Canvas message | Code |
|---|---|
| **Command** from a user or UI | Command record + `<Verb><Noun>UseCase` in `api/usecases`. Optionally a REST `@PrimaryAdapter` in `infra/adapters/primary` if the collaborator is a person or app. |
| **Command** from another context | Same use case. Add a translating primary adapter in `infra/adapters/primary/<ctx>ctxacl` only if the sender uses its own published types. |
| **Event** from another context | A use case in `api/usecases` that consumes **our own** record for the fact (named in our language). An `@AntiCorruptionLayer` primary adapter in `infra/adapters/primary/<ctx>ctxacl` listens (`@EventListener`) to the upstream `api.events` type and translates it. If the upstream context doesn't exist in code yet, create only the use case and our own record, and list the listener as a follow-up in the plan. |
| **Query** | Query record + result record + `<Noun>QueryHandler` (`@PrimaryPort`) in `api/queries`. The handler depends on a read-model `@SecondaryPort` declared in `api/queries`, implemented in memory under `infra/adapters/secondary`. |

Use-case flow, same as in deposit: load the aggregate, call the behaviour that returns `DomainEvents`, save the aggregate, then `DomainEventPublisher.publish(events)`.

## 4. Outbound communication

| Canvas message | Code |
|---|---|
| **Event** published by this context | Integration event record in `api/events` implementing `common.integration.IntegrationEvent`. It carries **primitives and strings only**, no domain types, because it's the published language. Internally, the aggregate emits a domain event. `infra/adapters/secondary/IntegrationEventsPublisher` (`@SecondaryAdapter`) listens to the domain event and publishes the integration record through Spring's `ApplicationEventPublisher`. Keep the two types separate even if they look alike now (see `deposit/api/events/DepositMade` and `deposit/domain/account/DepositMade`). |
| **Command** sent to another context | A `@SecondaryPort` interface in `domain/<aggregate>/`, named after what *we* need (for example `PaymentProcessor`, not `PaymentGatewayClient`). An adapter implementing it in `infra/adapters/secondary/<ctx>ctxacl/`, with a `package-info.java` annotated `@AntiCorruptionLayer` and a javadoc saying it's the only place the collaborator's language may appear. |
| **Query** to another context | Same as a command: a port in our language, a translating adapter in the ACL package. Return our own value objects. |

**Conformist without an ACL** (only after the pushback in SKILL.md Step 3): the adapter goes in `infra/adapters/secondary/<ctx>ctx/` without `@AntiCorruptionLayer`, and the port may use the upstream's published types. Write the decision and its risk in the javadoc of that package's `package-info.java`.

**Collaborator doesn't exist in code** (for example most VeloCity contexts, the City, or the PaymentProcessor): the ACL adapter is a stub that throws `UnsupportedOperationException("implement this")` and has a comment quoting the canvas message. Don't invent the upstream's API.

**External parties** (not a bounded context, for example a payment provider or the City zone map): same rule, a port plus an ACL adapter. Name the package `<party>acl`.

## 5. Ubiquitous language and business decisions → `domain/`

- Every UL term that the rules or messages use becomes a type with **exactly that name**. Don't rename terms for technical reasons. If a term really collides with a Java keyword, ask the user.
- Group types into `domain/<aggregate>/` packages, one per aggregate (a lowercase noun such as `tariff` or `rental`).
- **Finding aggregates from strict GWT rules.** A rule's *Given* names the state that must be consistent, and its *When* names the command. Rules whose Givens touch the same state belong in the same aggregate, and that state is the consistency boundary. Pick the smallest boundary that lets every rule be checked inside one aggregate without loading others. Write the reasoning in the plan: rule → invariant → aggregate.
- Outcomes:
  - A *Then* that rejects something → a dedicated exception (`<Something>Exception`, or a domain-named one like `BikeAlreadyInStation`) thrown from the aggregate.
  - A *Then* that records a fact → a domain event record in a sealed `<Aggregate>Event` interface that extends `common.domain.DomainEvent`. The sealed hierarchy forces handlers to use an exhaustive `switch`.
- UL terms that rules don't use, but messages do, become value-object records. Terms that neither uses stay in the javadoc only. Don't create types nobody uses.
- Policies ("whenever X then Y" decisions inside the context) → classes implementing `common.domain.DomainPolicy`, triggered by a local `@EventListener` handler in `infra/event`, the same way deposit's `LocalEventsHandler` does it.

## 6. Wiring

`infra/config/<Context>ContextConfig` is a `@Configuration` with `@Bean` methods building use cases, query handlers, policies, repositories and adapters by hand. Domain and API classes have no Spring annotations. Prefix bean method names with the context name (`rentalsBikeStationRepository`), because Spring Boot refuses duplicate bean names across contexts. Inject `DomainEventPublisher`, which `common/infra/spring/CommonInfraConfig` already provides.

## 6a. Context boundaries

Every message between contexts goes through the other context's `api` package: its use cases, queries, events and their records. This applies to inbound and outbound messages, and to ACL adapters too. An ACL translates *B's api types* into our language, never B's domain types. `<Context>ArchitectureTest` enforces it in both directions (template in `code-templates.md`). The only exceptions are the explicit, challenged, recorded decisions from SKILL.md Step 3, item 6.

## 7. Several modules in one context

Only after the pushback in SKILL.md Step 3:

```
<context>/
  package-info.java            @BoundedContext + @Module (the context root)
  <moduleA>/package-info.java  @Module only
    api/ domain/ infra/        same layout as above, per module
  <moduleB>/package-info.java  @Module only
```

The canvas javadoc stays on the context root. Each module's javadoc gives the module's slice of the purpose and which canvas messages it owns.
