# Canvas → code mapping

Paths are relative to `src/main/java/pl/wojtyna/dddtrainingv3/<system>/<context>/`. The canonical example of the layout is CrowdSorcery's `deposit` context. Look at it whenever this file leaves something open.

## Contents
1. Package layout
2. Name, purpose, classification, roles, language → `package-info.java`
3. Inbound communication → `api/usecases`, `api/queries`, primary adapters
4. Outbound communication → `api/events`, secondary ports and ACL adapters
5. Business decisions → delegated to `ddd-tactical-building-blocks`
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
  domain/                               built by ddd-tactical-building-blocks; this skill adds only outbound ports in domain/<area>/
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
/// - **<Rule name>**: given <...>, when <...>, then <...>. Enforced by `<DomainType>`, see `<Test>#<method>`.
/// - **<Rule name>** (*being built with `ddd-tactical-building-blocks`*): given <...>, when <...>, then <...>.
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

Use-case flow, same as in deposit: load the aggregate, call the behaviour that returns `DomainEvents`, save the aggregate, then `DomainEventPublisher.publish(events)`. Use cases and query handlers need the domain, so they are generated in Step 6b, after `ddd-tactical-building-blocks` has built it; Step 6a creates only their command, query and result records.

## 4. Outbound communication

| Canvas message | Code |
|---|---|
| **Event** published by this context | Integration event record in `api/events` implementing `common.integration.IntegrationEvent`. It carries **primitives and strings only**, no domain types, because it's the published language. Internally, the domain emits a domain event (built by `ddd-tactical-building-blocks`). `infra/adapters/secondary/IntegrationEventsPublisher`, generated in Step 6b, (`@SecondaryAdapter`) listens to the domain event and publishes the integration record through Spring's `ApplicationEventPublisher`. Keep the two types separate even if they look alike now (see `deposit/api/events/DepositMade` and `deposit/domain/account/DepositMade`). |
| **Command** sent to another context | A `@SecondaryPort` interface in `domain/<area>/` (the area the canvas message belongs to; the tactical skill may move it next to the block that calls it), named after what *we* need (for example `PaymentProcessor`, not `PaymentGatewayClient`). An adapter implementing it in `infra/adapters/secondary/<ctx>ctxacl/`, with a `package-info.java` annotated `@AntiCorruptionLayer` and a javadoc saying it's the only place the collaborator's language may appear. |
| **Query** to another context | Same as a command: a port in our language, a translating adapter in the ACL package. Return our own value objects. |

**Conformist without an ACL** (only after the pushback in SKILL.md Step 3): the adapter goes in `infra/adapters/secondary/<ctx>ctx/` without `@AntiCorruptionLayer`, and the port may use the upstream's published types. Write the decision and its risk in the javadoc of that package's `package-info.java`.

**Collaborator doesn't exist in code** (for example most VeloCity contexts, the City, or the PaymentProcessor): the ACL adapter is a stub that throws `UnsupportedOperationException("implement this")` and has a comment quoting the canvas message. Don't invent the upstream's API.

**External parties** (not a bounded context, for example a payment provider or the City zone map): same rule, a port plus an ACL adapter. Name the package `<party>acl`.

## 5. Business decisions → delegated to `ddd-tactical-building-blocks`

This skill doesn't design the domain. Every strict Given/When/Then rule goes into the plan's rules table as written and is handed to `ddd-tactical-building-blocks` (SKILL.md Step 6a). That skill decides which building block each rule needs (an aggregate only for a real invariant; otherwise a domain service, policy or factory), picks the domain types' names from the ubiquitous language, and builds them, by default in its TDD checkpoint loop.

What stays here:
- **Ubiquitous-language terms** go into the `package-info.java` javadoc with their definitions. Don't create domain types for them: the tactical skill names its types after these terms.
- **Rules** go into the javadoc's "Business decisions" (see §2), marked *being built with `ddd-tactical-building-blocks`* until their tests exist, and into the plan's rules table with status *delegated*. After the hand-back, both record the building block the tactical skill chose and the test that enforces the rule.
- **Value types in `api` records** stay primitives and strings (commands, queries, results, integration events). Mapping them to domain value objects happens in the use case, generated in Step 6b once the domain types exist.

## 6. Wiring

`infra/config/<Context>ContextConfig` is a `@Configuration` with `@Bean` methods building use cases, query handlers, policies, repositories and adapters by hand. In Step 6a it holds only the ACL adapters; the rest is added in Step 6b, around the domain types `ddd-tactical-building-blocks` created. Domain and API classes have no Spring annotations. Prefix bean method names with the context name (`rentalsBikeRepository`), because Spring Boot refuses duplicate bean names across contexts. Inject `DomainEventPublisher`, which `common/infra/spring/CommonInfraConfig` already provides.

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
