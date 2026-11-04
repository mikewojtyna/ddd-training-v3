# How the Orders system was built

*A recorded Claude Code session, 2026-11-04.*

This is the conversation in which the `orders` system and its **Ordering** bounded context were built, from an empty idea to a tested, event-sourced `Order` aggregate exposed over REST. It shows the training's workflow end to end:

1. **Canvas**: the `bounded-context-scaffolder` skill refuses to generate code without a Bounded Context Canvas, so it creates one and the trainer fills it in.
2. **Plan review**: the skill writes a plan and **stops** until the trainer approves it.
3. **Scaffold**: `package-info.java`, an ArchUnit boundary test and the system `CLAUDE.md`.
4. **The aggregate, one Given/When/Then scenario per cycle** (`ddd-tactical-building-blocks`, using the `tdd` skill's red/green rules). After every failing test Claude stops and asks: *"I'll write it myself"* or *"Scaffold it for me"*. In cycle 1 the trainer wrote the code; in cycles 2 and 4 Claude did, and every change waited for confirmation.
5. **Persistence after the domain**: `Version`, the repository, the use case and the REST adapter come only once every domain test is green.

Results: [canvas](canvas.md), [plan](plan.md), the code in `src/main/java/pl/wojtyna/dddtrainingv3/orders/`, and the tests in `OrderTest`.

**Reading tips:** questions Claude asked, and the answer the trainer picked, are shown as quotes. Tool calls (commands, file writes, test runs) are in collapsible sections, so expand them to see the exact commands and outputs. Claude's internal reasoning isn't included. Local paths are shortened to repo-relative paths.

---

### 👤 User

I want to try the new aggregate skill. Let's imagine creating something extremely simple: an Order aggregate with two invariants:

1. Sum of all items cannot exceed max threshold
2. There cannot be more than 3 items of the same type


### 🤖 Claude

*Loaded skill `ddd-tactical-building-blocks` (see `.claude/skills/ddd-tactical-building-blocks/SKILL.md`).*

<details><summary>🔧 Bash: List bounded contexts and look for existing Order classes</summary>

```bash
cd src/main/java/pl/wojtyna/dddtrainingv3 && ls velocity crowdsorcery; grep -ril "class Order\|record Order" . | head
```

**Result:**

```
crowdsorcery:
CLAUDE.md
common
contextmap
deposit
fundraising
interest
investment
payment
profile
simulation

velocity:
CLAUDE.md
common
contextmap
fleetmaintenance
rentals
```

</details>

> **❓ Claude asks:**
>
> **Order invariant 1, 'the sum of all items cannot exceed a max threshold', has no Given/When/Then scenario and no concrete threshold. I can't build it without them. Is this proposal right?**
>
> - *Yes, 100 PLN, both*: (a) Given an empty order, When an item for 100 PLN is added, Then it is added. (b) Given an order with items totalling 90 PLN, When an item for 20 PLN is added, Then it is rejected.
> - *Only the rejection*: Keep just (b), with the 100 PLN threshold
>
> **✅ User picked:** Yes, 100 PLN, both
>
> **Order invariant 2, 'no more than 3 items of the same type', has no Given/When/Then scenario. Is this proposal right?**
>
> - *Yes, both*: (a) Given an order with 2 'Helmet' items, When another 'Helmet' is added, Then it is added. (b) Given an order with 3 'Helmet' items, When another 'Helmet' is added, Then it is rejected.
> - *Only the rejection*: Keep just (b)
>
> **✅ User picked:** Yes, both
>
> **Should the Order aggregate be state-based or event-sourced?**
>
> - *State-based*: Fields hold current state, like BikeStation
> - *Event-sourced*: State rebuilt from events; commands return all pending events
>
> **✅ User picked:** Event-sourced
>
> **There's no Order in VeloCity or CrowdSorcery, and this skill only works inside an existing bounded context. Where should Order live?**
>
> - *velocity/rentals (Recommended)*: New package rentals/domain/order. Quickest way to try the skill, even if Order is a loose fit for Rentals
> - *velocity/fleetmaintenance*: New package fleetmaintenance/domain/order, e.g. ordering spare parts
> - *New bounded context*: Scaffold an Ordering context first with bounded-context-scaffolder, then come back
>
> **✅ User picked:** New bounded context
>

*Loaded skill `bounded-context-scaffolder` (see `.claude/skills/bounded-context-scaffolder/SKILL.md`).*

<details><summary>🔧 Bash: Look for existing plans and Order mentions in docs</summary>

```bash
cd . && ls docs/bounded-contexts/*/ 2>/dev/null; grep -ril "order" src/main/cml velocity/velocity-context-map.md crowdsorcery/CrowdSorcery.md 2>/dev/null | head
```

**Result:**

```
fleetmaintenance
```

</details>

> **❓ Claude asks:**
>
> **There's no Ordering plan or canvas yet, and the scaffolder builds only from a canvas. Neither the VeloCity nor the CrowdSorcery docs mention orders. Which system should the new Ordering context belong to?**
>
> - *VeloCity (Recommended)*: velocity/ordering, e.g. riders ordering accessories such as helmets. Fits the 'Helmet' scenarios
> - *CrowdSorcery*: crowdsorcery/ordering
>
> **✅ User picked:** create something totally new - let's call it orders
>
> **The canvas needs a business purpose, the benefit rather than a technical description. Which one should I write into the Ordering canvas?**
>
> - *Accessory orders*: Riders order accessories (helmets, locks, lights) with a rental. Order value and quantity per item type are capped to limit fraud and stock exhaustion.
> - *I'll write it myself*: I create the canvas with the known parts filled in, and you add the purpose in the file
>
> **✅ User picked:** I'll write it myself
>

<details><summary>🔧 Bash: Read the canvas template</summary>

```bash
cat .claude/skills/bounded-context-scaffolder/assets/canvas-template.md
```

**Result:**

```
# Bounded Context Canvas: <Name>

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become aggregates and tests only if you write them as strict Given/When/Then examples with concrete values.
-->

## Name
<Name, exactly as it should appear in @BoundedContext (and in the CML model, if there is one)>

## Purpose
<What benefits does this context provide, and how? Describe it from a business perspective.>

## Strategic Classification
- Domain: <core | supporting | generic | other>
- Business model: <revenue | engagement | compliance | cost reduction>
- Evolution: <genesis | custom built | product | commodity>

## Domain Roles
<!-- Pick traits from docs/model-traits-worksheet.md -->
- <e.g. Execution Model>: <why>

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| <Rider app> | Command | <Unlock bike> | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| <Rental ended> | Event | <Pricing> | n/a for events |

## Ubiquitous Language
- **<Term>**: <definition>

## Business Decisions
<!-- One rule per block. Concrete values, please. -->
### <Rule name>
- **Given** <state>
- **When** <command or event>
- **Then** <outcome: event recorded, or rejection>

## Assumptions
- <unverified assumption>

## Verification Metrics
- <metric that would (in)validate this context's boundaries>

## Open Questions
- <question>
```

</details>

<details><summary>🔧 Write `docs/bounded-contexts/orders/ordering/canvas.md`</summary>

```
# Bounded Context Canvas: Ordering

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become aggregates and tests only if you write them as strict Given/When/Then examples with concrete values.

Pre-filled by Claude from the conversation on 2026-11-04. Lines marked TODO need your input.
System: a new top-level system `orders` (package pl.wojtyna.dddtrainingv3.orders).
Context package: `ordering` (rename it here if you prefer another name).
-->

## Name
Ordering

## Purpose
TODO: What benefits does this context provide, and how? Describe it from a business perspective.

## Strategic Classification
- Domain: TODO <core | supporting | generic | other>
- Business model: TODO <revenue | engagement | compliance | cost reduction>
- Evolution: TODO <genesis | custom built | product | commodity>

## Domain Roles
<!-- Pick traits from docs/model-traits-worksheet.md -->
- TODO <e.g. Execution Model>: <why>

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| TODO <Customer app?> | Command | Add item to order | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| TODO | | | |

## Ubiquitous Language
<!-- Proposed definitions. Confirm or correct them: terms become class names. -->
- **Order**: TODO confirm: a customer's collection of items, whose total value and per-type quantities are capped.
- **Item**: TODO confirm: one unit added to an order, with an item type and a price in PLN.
- **Item type**: TODO confirm: the kind of product an item is, e.g. "Helmet". Items of the same type count towards the same limit.
- **Order total**: TODO confirm: the sum of the prices of all items in the order.

## Business Decisions
<!-- Confirmed in the conversation on 2026-11-04. Implemented as an event-sourced Order aggregate. -->
### Order total cannot exceed 100 PLN
- **Given** an empty order
- **When** an item for 100 PLN is added
- **Then** the item is added

- **Given** an order with items totalling 90 PLN
- **When** an item for 20 PLN is added
- **Then** adding the item is rejected

### No more than 3 items of the same type
- **Given** an order with 2 "Helmet" items
- **When** another "Helmet" is added
- **Then** the item is added

- **Given** an order with 3 "Helmet" items
- **When** another "Helmet" is added
- **Then** adding the item is rejected

## Assumptions
- TODO

## Verification Metrics
- TODO

## Open Questions
- TODO
```

**Result:**

```
File created successfully at: docs/bounded-contexts/orders/ordering/canvas.md (file state is current in your context — no need to Read it back)
```

</details>

<details><summary>🔧 Bash: Open the canvas in IntelliJ</summary>

```bash
idea --line 18 docs/bounded-contexts/orders/ordering/canvas.md >/dev/null 2>&1 & sleep 0; echo started
```

**Result:**

```
started
```

</details>

I created the canvas for the new context and tried to open it in IntelliJ, at the Purpose section. It's at `docs/bounded-contexts/orders/ordering/canvas.md:18`. I haven't written any code yet, because the scaffolder only builds from a canvas with real content.

Setup:
- **System:** a new top-level `orders` package, `pl.wojtyna.dddtrainingv3.orders`.
- **Context:** `ordering`. Rename it in the canvas if you'd like another name.

**Already filled in:** the two rules you confirmed, written as Given/When/Then: the 100 PLN order total and the limit of 3 items per type.

**To fill in (marked TODO):**
- **Purpose:** required. You said you'd write it.
- **Ubiquitous language:** required. I proposed definitions for *Order*, *Item*, *Item type* and *Order total*. Confirm or correct them, because they become class names.
- **Optional:** strategic classification, domain roles, inbound and outbound messages, assumptions, verification metrics and open questions. Anything left empty means a smaller module: no ACLs, and possibly no REST endpoint.

When you're done, say "canvas ready". I'll write the plan and stop so you can review it, then generate the module. After that, the Order aggregate gets built with the new skill, one scenario at a time with a stop after each test, as you wanted.


---

### 👤 User

*(The user connected IntelliJ with `/ide`.)*

canvas ready


### 🤖 Claude

<details><summary>🔧 Bash: Read plan template, canvas mapping and an example plan</summary>

```bash
cat .claude/skills/bounded-context-scaffolder/assets/plan-template.md; echo ====; cat .claude/skills/bounded-context-scaffolder/references/canvas-mapping.md; echo ====; grep -n "archunit" pom.xml; ls docs/bounded-contexts/fleetmaintenance/; sed -n 1,200p docs/bounded-contexts/fleetmaintenance/*/plan.md 2>/dev/null | head -60
```

**Result:**

````
# <Name> bounded context: implementation plan

- **System / package**: `<system>.<context>`
- **Mode**: scaffold | update
- **Source canvas**: `<path>` (or "inline, <date>")
- **Last updated**: <YYYY-MM-DD>
- **Status**: draft - awaiting review | approved | implemented

<!-- Reviewers: edit this plan directly or answer in the conversation, then set Status to "approved" (or say so).
     Invoking the skill again resumes from this file. -->

## Review notes
<!-- One entry per review round: what the user asked to change, what was changed, who approved and when. -->
- <YYYY-MM-DD>: draft written, awaiting review.

## Canvas summary
<Two or three sentences: purpose, classification, roles.>

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| Modules | 1 module = 1 context | default |
| ACL towards <ctx> | yes / no (conformist, user insisted) | <reason; risk if no> |
| Boundary exceptions | none (default) / D<n>: <exact package allowed> (user insisted after challenge, <date>) | <risk>; excluded in `<Context>ArchitectureTest`, comment at the usage site |
| ... | | |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|

### Ubiquitous language
| Term | Code type | Kind (aggregate / entity / VO / event / javadoc only) |
|---|---|---|

### Business rules → aggregates
| Rule (GWT) | Invariant | Aggregate | Test |
|---|---|---|---|

### Rules awaiting GWT
- <rule as written>, suggested GWT: <...>, status: awaiting confirmation

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|

## Files
<Tree of the created or modified files, including `src/test/.../<Context>ArchitectureTest.java`, and `pom.xml` if ArchUnit had to be added.>

## Stubs and follow-ups
- <ACL adapter X is a stub: collaborator not in code yet>
- <CML / context map needs updating?>
- <Open questions from the canvas>

## Changelog
### <YYYY-MM-DD>: <scaffolded | updated>
- Added: ...
- Changed: ...
- Removed (confirmed by user): ...
- Proposed removals (not applied): ...
- Triggered by: <canvas change>
====
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
…(53 lines truncated)
````

</details>

<details><summary>🔧 Bash: Read the FleetMaintenance example plan</summary>

```bash
sed -n 1,80p docs/bounded-contexts/velocity/fleetmaintenance/plan.md
```

**Result:**

```
# FleetMaintenance bounded context: implementation plan

- **System / package**: `velocity.fleetmaintenance`
- **Mode**: scaffold
- **Source canvas**: `docs/bounded-contexts/velocity/fleetmaintenance/canvas.md`
- **Last updated**: 2026-11-03
- **Status**: implemented

<!-- Reviewers: edit this plan directly or answer in the conversation, then set Status to "approved" (or say so).
     Invoking the skill again resumes from this file. -->

## Review notes
- 2026-11-03: draft written, awaiting review. Questions for the reviewer are marked **Q1–Q8** below.
- 2026-11-03, review round 1: the user said that Fleet Maintenance must not publish *Bike Fixed* to Customer Support. Fleet Maintenance stays downstream and calls a command on Customer Support instead. Changed: canvas outbound table (new *Notify Bike Fixed* command, *Bike Fixed* event now internal), D9, outbound mapping, new rule R6, files (no `api/events`, no `IntegrationEventsPublisher`; new port `FixNotifications` + `customersupportctxacl` adapter + policy), differences from the map. Q8 is resolved. Q1–Q7 still open.
- 2026-11-03: approved by the user as written, with Q1–Q7 still open. The suggested defaults apply: Q3 not modelled (no `TakeBrokenBikeOutOfCirculationPolicy`), Q5 and Q6 ignored, Q7 any technician; U1 and U2 implemented as derived rules.

## Canvas summary
Fleet Maintenance keeps the fleet healthy: a broken bike gets fixed as quickly as possible, and this context decides when it goes back into circulation. It is a **supporting** domain with a **cost reduction** business model, **custom built**. Its role is **Enforcer**: it makes sure Rentals offers only working bikes, by telling Rentals to withdraw a broken bike and to make it available again only once it is fixed.

## Decisions
| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | System / package | `velocity` / `fleetmaintenance` | CML `BoundedContext FleetMaintenance`; `@BoundedContext(name = "FleetMaintenance")` matches the CML and the canvas |
| D2 | Modules | 1 module = 1 context | default |
| D3 | ACL towards Rentals (outbound commands) | yes: port `Circulation` in our language, adapter in `infra/adapters/secondary/rentalsctxacl` | canvas says ACL; Rentals speaks *available / withdrawn*, we speak *broken / back in circulation* |
| D4 | ACL for inbound events from Rentals and Customer Support | yes: listeners in `infra/adapters/primary/rentalsctxacl` and `.../customersupportctxacl` | default. **Not generated now**: Rentals has no `api/events` yet and Customer Support doesn't exist in code (see follow-ups). Only our use cases and our own command records are created |
| D5 | Aggregate | one aggregate root **`Bike`** in `domain/bike`, one instance per bike, with its own `BikeId` record | every rule's *Given* is about the condition of one bike (broken, maintenance started), so one bike is the smallest consistency boundary. `BikeId` is our own type, not Rentals' `BikeId`, because the boundary rule forbids using Rentals' domain types |
| D6 | Reservations | Fleet Maintenance **does not track reservations**. Rentals owns reserve / unlock (canvas assumption, and the reference map) | Rule R4 only needs "the bike stays broken and any technician can start maintenance". Tracking who reserved a bike would duplicate Rentals' state. Consequence: the inbound row *Reservation Expired / Unlock Failed* needs no handling here, see **Q1** |
| D7 | Calls to Rentals | made by **policies** reacting to domain events (`BikeFixed` → put back into circulation), not by the use case or the aggregate | "whenever X then Y" belongs in a `DomainPolicy`; the aggregate stays free of outbound calls, the same way deposit's `DepositInitiatedPolicy` works |
| D8 | Unknown bike | a bike Fleet Maintenance has never heard of is **in circulation** (not broken). The repository returns `Optional`, and use cases fall back to `Bike.inCirculation(id)` | Fleet Maintenance learns about a bike from its first fault report; it doesn't need a copy of the fleet register |
| D9 | Published events | **none**. `BikeFixed` and `MaintenanceStarted` are internal domain events. Customer Support learns about a fix through a **command** Fleet Maintenance calls: port `FixNotifications` in our language, adapter in `infra/adapters/secondary/customersupportctxacl` (ACL). No `api/events` package | review round 1: Customer Support stays upstream of Fleet Maintenance in both relationships, so there's no cycle |
| D10 | REST for the technician app | implemented (not a stub) in `infra/adapters/primary/TechnicianController` | the canvas describes both technician commands |
| D11 | Boundary exceptions | none | no request to break the `api`-only rule |
| D12 | ArchUnit | add `com.tngtech.archunit:archunit-junit5:1.5.1` (test scope) to `pom.xml` | not on the classpath yet; older versions can't read JDK 27 class files |
| D13 | Outbound *Reserve Bike / Unlock Bike (for a technician)* | **not modelled** | nothing in the canvas triggers it, and it contradicts the assumption "Fleet Maintenance never locks or unlocks bikes itself". See **Q2** |

## Mapping: canvas → code
Paths relative to `src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/`.

### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Rentals | Event | Bike Withdrawn (fault detected / lock failed) | `api/usecases/ReportFaultUseCase` + record `ReportFault(String bikeId)`. Listener in `infra/adapters/primary/rentalsctxacl`: **follow-up** (Rentals publishes no events yet) |
| Rentals | Event | Bike Unlocked (by a technician) | `api/usecases/StartMaintenanceUseCase` + record `BikeUnlockedByTechnician(String bikeId, String technicianId)`. Listener: **follow-up** |
| Rentals | Event | Reservation Expired / Unlock Failed (technician) | **no code** (D6, **Q1**) |
| Customer Support | Event | Fleet Issue Incident Reported (unconfirmed) | same `ReportFaultUseCase`. Listener in `infra/adapters/primary/customersupportctxacl`: **follow-up** (context not in code) |
| Technician app | Command | Report Fault | `ReportFaultUseCase`, REST `POST /fleet-maintenance/bikes/{bikeId}/fault-reports` in `TechnicianController` |
| Technician app | Command | Return Bike to Circulation | `api/usecases/ReturnBikeToCirculationUseCase` + record `ReturnBikeToCirculation(String bikeId, String technicianId)`, REST `POST /fleet-maintenance/bikes/{bikeId}/return-to-circulation` |

Use-case flow (as in deposit): load the `Bike` (or `Bike.inCirculation(id)`, D8), call its behaviour, save it, `DomainEventPublisher.publish(events)`.

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Withdraw Bike | Command | Rentals | `domain/bike/Circulation#takeOut(BikeId)` | `infra/adapters/secondary/rentalsctxacl/RentalsCirculationAdapter`, **stub** (Rentals has no withdraw in its `api`). Called by policy `TakeBrokenBikeOutOfCirculationPolicy` **only if Q3 is confirmed** |
| Make Bike Available | Command | Rentals | `Circulation#putBack(BikeId)` | same adapter, **stub**. Called by policy `ReturnFixedBikeToCirculationPolicy` |
| Reserve Bike / Unlock Bike | Command | Rentals | none | none (D13, **Q2**) |
| Maintenance Started | Event | none | domain event `BikeMaintenanceEvent.MaintenanceStarted` | internal only |
| Bike Fixed | Event | none | domain event `BikeMaintenanceEvent.BikeFixed` | internal only |
| Notify Bike Fixed | Command | Customer Support | `domain/bike/FixNotifications#bikeFixed(BikeId)` | `infra/adapters/secondary/customersupportctxacl/CustomerSupportFixNotificationsAdapter`, **stub** (Customer Support isn't in code). Called by policy `NotifySupportOfFixedBikePolicy` |

### Ubiquitous language
| Term | Code type | Kind |
|---|---|---|
| Bike | `domain/bike/Bike`, `BikeId` | aggregate root, identity (VO record) |
| Broken | private state of `Bike`; event `BikeReportedBroken` | aggregate state + domain event |
| Fault Report | command record `ReportFault`; `Bike#reportFault()` | command / behaviour |
| Technician | `domain/bike/TechnicianId` | VO record |
| Maintenance | private state of `Bike` | aggregate state (javadoc) |
| Maintenance Started | `BikeMaintenanceEvent.MaintenanceStarted(BikeId, TechnicianId)` | domain event |
| Bike Fixed | `BikeMaintenanceEvent.BikeFixed(BikeId, TechnicianId)`; `FixNotifications#bikeFixed` | domain event + outbound port |
| Back in Circulation | `Bike#returnToCirculation(TechnicianId)`; `Circulation#putBack` | behaviour / port |

Rejection: `domain/bike/MaintenanceNotStarted` (exception). Events: sealed `BikeMaintenanceEvent extends DomainEvent`.

### Business rules → aggregates
All rules live in the `Bike` aggregate (D5). Test class: `src/test/java/.../velocity/fleetmaintenance/domain/bike/BikeTest`.

| Rule (GWT) | Invariant | Aggregate | Test |
|---|---|---|---|
```

</details>

<details><summary>🔧 Write `docs/bounded-contexts/orders/ordering/plan.md`</summary>

````
# Ordering bounded context: implementation plan

- **System / package**: `orders.ordering` (new system `pl.wojtyna.dddtrainingv3.orders`)
- **Mode**: scaffold
- **Source canvas**: `docs/bounded-contexts/orders/ordering/canvas.md`
- **Last updated**: 2026-11-04
- **Status**: draft - awaiting review

<!-- Reviewers: edit this plan directly or answer in the conversation, then set Status to "approved" (or say so).
     Invoking the skill again resumes from this file. -->

## Review notes
- 2026-11-04: draft written, awaiting review. Questions for the reviewer are marked **Q1–Q3** below.

## Canvas summary
Ordering lets a customer prepare an order of items, with the order total capped at 100 PLN and at most 3 items of each item type. It is a **core** domain with a **revenue** business model, **custom built**. Its role is **Execution Model**.

## Decisions
| # | Decision | Choice | Reason |
|---|---|---|---|
| D1 | System / package | new system `orders`, context `ordering`; `@BoundedContext(name = "Ordering")` | the user asked for a new system called `orders`; the canvas name is "Ordering" |
| D2 | Modules | 1 module = 1 context | default |
| D3 | Order aggregate | **not generated by this skill.** It is built afterwards with `ddd-tactical-building-blocks`, event-sourced, one Given/When/Then scenario per cycle with the user in the loop | explicit user request: the point of this exercise is to try the interactive aggregate skill. The scaffolder would otherwise implement the aggregate and its tests in one go |
| D4 | Use case `AddItemToOrder`, repository, REST | **deferred** until the aggregate exists and its domain tests are green | the use case loads, calls and saves the aggregate, so it can't exist before it. It also follows the "persistence after the domain" order |
| D5 | ACLs | none | the canvas has no outbound communication and no inbound messages from other contexts |
| D6 | Boundary exceptions | none | no request to break the `api`-only rule |
| D7 | ArchUnit | already in `pom.xml`, nothing to add | |
| D8 | System `CLAUDE.md` | create a short `orders/CLAUDE.md` | the repo's `CLAUDE.md` says each system has its own, with system-specific context |

## Mapping: canvas → code
Paths relative to `src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/`.

### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| User directly calling it | Command | Add item to order | `api/usecases/AddItemToOrderUseCase` + record `AddItemToOrder`: **deferred** (D4). REST endpoint: see **Q2** |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| none | | | | |

### Ubiquitous language
Types are created by `ddd-tactical-building-blocks` (D3). Expected shape:

| Term | Code type | Kind |
|---|---|---|
| Order | `domain/order/Order`, `OrderId` | aggregate root (event-sourced), identity |
| Item | `domain/order/Item` | value object (item type + price) |
| Item type | `domain/order/ItemType` | value object |
| Order total | derived from the items, in PLN (joda `Money`) | aggregate state, javadoc |

### Business rules → aggregates
All rules guard the same state (the items of one order), so they belong in one `Order` aggregate. Test class: `src/test/java/.../orders/ordering/domain/order/OrderTest`, written one scenario at a time (D3).

| Rule (GWT) | Invariant | Aggregate | Test |
|---|---|---|---|
| R1a: given an empty order, when an item for 100 PLN is added, then the item is added | order total ≤ 100 PLN | `Order` | `OrderTest`, cycle 1 |
| R1b: given an order with items totalling 90 PLN, when an item for 20 PLN is added, then adding the item is rejected | order total ≤ 100 PLN | `Order` | `OrderTest`, cycle 2 |
| R2a: given an order with 2 "Helmet" items, when another "Helmet" is added, then the item is added | ≤ 3 items per item type | `Order` | `OrderTest`, cycle 3 |
| R2b: given an order with 3 "Helmet" items, when another "Helmet" is added, then adding the item is rejected | ≤ 3 items per item type | `Order` | `OrderTest`, cycle 4 |

### Rules awaiting GWT
- none

### Canvas consistency check
- R1a and R1b agree: 100 PLN is allowed (≤), 110 PLN is rejected.
- R2a and R2b agree: the 3rd item is allowed, the 4th is rejected.
- **Q1**: R2 gives no prices for the Helmets. Any prices work as long as the total stays ≤ 100 PLN (e.g. 10 PLN each). That's a test detail, not a domain question, unless you want a specific price.

## Differences from the reference context map
None: `orders` is a new system with no reference map or CML model.

## Files
```
src/main/java/pl/wojtyna/dddtrainingv3/orders/
  CLAUDE.md                                   system context (D8)
  ordering/package-info.java                  @BoundedContext(name = "Ordering") + @Module, canvas javadoc;
                                              business decisions marked "being built with ddd-tactical-building-blocks"
src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/
  OrderingArchitectureTest.java               api-only boundary, both directions
```
Added later by `ddd-tactical-building-blocks`: `domain/order/*`, `OrderTest`; after the domain is green: `Version`, repository; then the deferred use case (D4).

## Stubs and follow-ups
- Build the `Order` aggregate with `ddd-tactical-building-blocks` (D3).
- After that: `AddItemToOrderUseCase`, an in-memory repository, `infra/config/OrderingContextConfig`, and maybe REST (**Q2**).
- **Q2**: "User directly calling it": is that a REST endpoint, or is the use case itself the entry point for now?
- **Q3**: the purpose "Allows to prepare orders." describes what the context does, not the business benefit. A suggestion: "Lets customers put together an order within safe limits (at most 100 PLN, at most 3 items per type), so we sell without risking large unpaid orders or emptying the stock of one product." Keep yours, take this one, or write another. Not blocking.

## Changelog
### 2026-11-04: plan drafted
- Triggered by: new canvas.
````

**Result:**

```
File created successfully at: docs/bounded-contexts/orders/ordering/plan.md (file state is current in your context — no need to Read it back)
```

</details>

*Opened `docs/bounded-contexts/orders/ordering/plan.md` in IntelliJ.*

The plan for the new Ordering context is written, and I've opened it in IntelliJ: `docs/bounded-contexts/orders/ordering/plan.md`. I've stopped here for your review and haven't generated any code.

**What the scaffold creates (3 files):**
- `orders/ordering/package-info.java`: the canvas as javadoc, with the 4 rules marked "being built with ddd-tactical-building-blocks".
- `OrderingArchitectureTest`: checks that other code reaches Ordering only through its `api` package, and the reverse.
- A short `orders/CLAUDE.md` for the new system, because the repo convention is one per system.

**Decisions to review:**
- **The scaffolder doesn't build the Order aggregate (D3).** Normally it would write the aggregate and its tests in one go. Here we hand over to the new aggregate skill, which builds it one scenario at a time with a stop after each test. I expect 4 cycles: 100 PLN accepted, 110 PLN rejected, a 3rd Helmet accepted, a 4th Helmet rejected. All 4 rules guard the items of one order, so a single aggregate is enough.
- **The "Add item to order" use case and the repository come after the aggregate is green (D4).** The use case can't exist before the aggregate, and the order also matches your persistence-after-domain rule.
- **No anti-corruption layers.** The canvas has no other context to translate to or from.
- **ArchUnit** is already in `pom.xml`, so nothing needs adding there.

**Questions (none blocking):**
- **Q1:** the Helmet scenarios give no price. I'd use 10 PLN each so the 100 PLN limit never interferes. OK?
- **Q2:** you wrote "User directly calling it". Do you want a REST endpoint, or is the use case enough as the entry point for now?
- **Q3:** "Allows to prepare orders." says what the context does rather than the business benefit. The plan has a suggested rewrite; keep yours if you prefer.

Answer here or edit the plan file directly. Once you approve, I'll generate the 3 files and then start cycle 1 of the Order aggregate.


---

### 👤 User

Q1: yes, Q2: use case + rest adapter, Q3: keep my description


### 🤖 Claude

<details><summary>🔧 Bash: Check plan status and question lines after user edits</summary>

```bash
git diff --no-index /dev/null docs/bounded-contexts/orders/ordering/plan.md | grep -n "Status\|Q1\|Q2\|Q3" ; grep -n "Status" docs/bounded-contexts/orders/ordering/plan.md
```

**Result:**

```
13:+- **Status**: approved
15:+<!-- Reviewers: edit this plan directly or answer in the conversation, then set Status to "approved" (or say so).
19:+- 2026-11-04: draft written, awaiting review. Questions for the reviewer are marked **Q1–Q3** below.
42:+| User directly calling it | Command | Add item to order | `api/usecases/AddItemToOrderUseCase` + record `AddItemToOrder`: **deferred** (D4). REST endpoint: see **Q2** |
75:+- **Q1**: R2 gives no prices for the Helmets. Any prices work as long as the total stays ≤ 100 PLN (e.g. 10 PLN each). That's a test detail, not a domain question, unless you want a specific price.
93:+- After that: `AddItemToOrderUseCase`, an in-memory repository, `infra/config/OrderingContextConfig`, and maybe REST (**Q2**).
94:+- **Q2**: "User directly calling it": is that a REST endpoint, or is the use case itself the entry point for now?
95:+- **Q3**: the purpose "Allows to prepare orders." describes what the context does, not the business benefit. A suggestion: "Lets customers put together an order within safe limits (at most 100 PLN, at most 3 items per type), so we sell without risking large unpaid orders or emptying the stock of one product." Keep yours, take this one, or write another. Not blocking.
7:- **Status**: approved
9:<!-- Reviewers: edit this plan directly or answer in the conversation, then set Status to "approved" (or say so).
```

</details>

The plan's status is now `approved`. I'll record your answers in the plan, then generate the scaffold.

<details><summary>🔧 Bash: Record review answers and read existing arch test and examples</summary>

````bash
python3 - <<'EOF'
p='docs/bounded-contexts/orders/ordering/plan.md'; s=open(p).read()
reps=[
("- 2026-11-04: draft written, awaiting review. Questions for the reviewer are marked **Q1–Q3** below.",
 "- 2026-11-04: draft written, awaiting review. Questions for the reviewer are marked **Q1–Q3** below.\n- 2026-11-04: approved by the user (Status set in the file). Answers: Q1 yes, 10 PLN per Helmet in the tests; Q2 use case + REST adapter (both still after the aggregate, D4); Q3 keep the purpose as written."),
("`api/usecases/AddItemToOrderUseCase` + record `AddItemToOrder`: **deferred** (D4). REST endpoint: see **Q2** |",
 "`api/usecases/AddItemToOrderUseCase` + record `AddItemToOrder`, REST adapter `infra/adapters/primary/OrderController`: both **deferred** until the aggregate is green (D4, Q2) |"),
("`infra/config/OrderingContextConfig`, and maybe REST (**Q2**).",
 "`infra/config/OrderingContextConfig` and the REST adapter `OrderController` (Q2)."),
("- **Q2**: \"User directly calling it\": is that a REST endpoint, or is the use case itself the entry point for now?",
 "- **Q2** (answered): use case + REST adapter."),
("Keep yours, take this one, or write another. Not blocking.",
 "Keep yours, take this one, or write another. Not blocking. **Answered: keep the original.**"),
("That's a test detail, not a domain question, unless you want a specific price.",
 "That's a test detail, not a domain question, unless you want a specific price. **Answered: 10 PLN each.**"),
]
for a,b in reps:
    assert s.count(a)==1,a[:50]; s=s.replace(a,b)
open(p,'w').write(s)
EOF
sed -n '/Architecture/,/^```$/p;' .claude/skills/bounded-context-scaffolder/references/code-templates.md | head -5; cat src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/FleetMaintenanceArchitectureTest.java; cat src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/package-info.java | tail -15; sed -n 1,40p src/main/java/pl/wojtyna/dddtrainingv3/velocity/CLAUDE.md
````

**Result:**

````
## Architecture test: the context boundary

This was verified against this repository with ArchUnit 1.5.1. Each forbidden direction fails exactly one rule, an `api` dependency passes, and an exception silences only the package it names. Put it in `src/test/java/pl/wojtyna/dddtrainingv3/<system>/<context>/<Context>ArchitectureTest.java`. Only `CONTEXT` and the names change per context.

```java
package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.util.Set;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/// Context boundary rules for the FleetMaintenance bounded context: other contexts may depend on FleetMaintenance only through
/// its `api` package, and FleetMaintenance may depend on other contexts only through their `api` packages.
@AnalyzeClasses(packages = "pl.wojtyna.dddtrainingv3", importOptions = ImportOption.DoNotIncludeTests.class)
class FleetMaintenanceArchitectureTest {

    private static final String ROOT = "pl.wojtyna.dddtrainingv3";
    private static final String CONTEXT = ROOT + ".velocity.fleetmaintenance";

    @ArchTest
    static final ArchRule other_contexts_use_only_fleet_maintenance_api =
        noClasses().that().resideOutsideOfPackage(CONTEXT + "..")
                   .should().dependOnClassesThat(resideInAPackage(CONTEXT + "..")
                                                     .and(resideOutsideOfPackage(CONTEXT + ".api..")));

    @ArchTest
    static final ArchRule fleet_maintenance_uses_only_other_contexts_api =
        noClasses().that().resideInAPackage(CONTEXT + "..")
                   .should().dependOnClassesThat(nonApiClassOfAnotherContext());

    /// A class of another bounded context (`<root>.<system>.<context>..`) outside its `api` package.
    /// Shared code (`<root>.common..`, `<root>.<system>.common..`, `<root>.<system>.contextmap..`) is not a context.
    private static DescribedPredicate<JavaClass> nonApiClassOfAnotherContext() {
        return DescribedPredicate.describe("non-api class of another bounded context", javaClass -> {
            var pkg = javaClass.getPackageName();
            if (!pkg.startsWith(ROOT + ".") || pkg.equals(CONTEXT) || pkg.startsWith(CONTEXT + ".")) {
                return false;
            }
            var parts = pkg.substring(ROOT.length() + 1).split("\\.");
            if (parts.length < 2 || parts[0].equals("common") || Set.of("common", "contextmap").contains(parts[1])) {
                return false;
            }
            return !(parts.length >= 3 && parts[2].equals("api"));
        });
    }
}
/// - Time from "reported broken" to "back in circulation".
/// - Share of bikes reported broken again within 7 days of returning to circulation.
///
/// ## Open questions
/// - Can a bike turn out to be unrepairable (retired from the fleet)? Who decides?
/// - Is there a repair priority (e.g. bikes at busy stations first)?
/// - Does Customer Support need an incident id with *Notify Bike Fixed* to resolve the right incident?
///
/// Source canvas: `docs/bounded-contexts/velocity/fleetmaintenance/canvas.md`
@BoundedContext(name = "FleetMaintenance")
@Module
package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
# VeloCity system

VeloCity is a city bike rental service and the current exercise of the training. The repo-root `CLAUDE.md` covers the package conventions; this file adds VeloCity-specific context.

## Sources of truth

- `velocity/velocity-vision.md` (repo root) is the domain-expert role-play guide. It is **deliberately inconsistent and has gaps**, because participants are meant to discover and resolve them. Don't "fix" the vision. When the vision and the code disagree, ask instead of picking one.
- `velocity/velocity-context-map.md` is the **reference solution** for the context-mapping exercise. Its "Accepted Alternatives" section lists other valid answers. Treat it as a spoiler: don't push it on someone who is still doing the discovery exercise.
- `velocity/bounded-context-discovery-session.md` records how the bounded contexts were found, using three heuristics: pivotal events, swimlanes, and single responsibility.
- `src/main/cml/VeloCity-Bounded-Contexts.cml` (contexts only) and `VeloCity-Context-Map.cml` (with relationships) are the Context Mapper models of the reference map. `contextmap/` renders them to PNG. If you change a context or a relationship, keep the CML, `velocity-context-map.md`, and the code in sync.

## Bounded contexts

The reference map has these contexts: Accounts, Rentals, Pricing, Settlement, Payments, DebtRecovery, CustomerSupport and FleetMaintenance. It also has two external parties: the PaymentProcessor and the City, which provides the zone map. **Only `rentals` exists in code so far.** Add the others as new subpackages that follow the root layout.

## Rentals

- `BikeStation` (`domain/bikestation`) is the aggregate root. It holds the bikes as a private inner `@Entity` called `BikeInStation`. `BikeStationRepo` is its `@SecondaryPort`.
- Commands use a curried fluent API built from the `FluentLanguage` interfaces: `station.rent(bike).by(borrower).forDuration(duration)` returns `DomainEvents`. Follow this style for new commands.
- Events are the sealed `BikeRentalEvent` hierarchy (`BikeRented`, `BikeReturned`). Handlers such as `BikeRentedEventHandler` use an exhaustive `switch`, so adding an event forces every handler to deal with it.
- Business rules are enforced with dedicated exceptions: `BikeAlreadyInStation`, `BikeNotAvailableException`, and `MaximumRentalDurationExceededException` (thrown when a rental is longer than 11 hours).
- Some parts are intentional workshop stubs: `BikeStation.returnBike` throws `UnsupportedOperationException`, and `RentBikeUseCase` only contains the load, execute, save, publish steps as comments.
- `rentals/package-info.java` declares the context with `@BoundedContext(name = "Rentals")` and `@Module`. The name matches the CML. Give each new VeloCity context the same kind of `package-info.java`, named after its CML `BoundedContext`.
- Tests (`src/test/.../velocity/rentals/domain/bikestation/BikeStationTest`) are written as given/when/then examples, with the scenario in `@DisplayName`, and live in the same package as the domain.
````

</details>

Now I'm writing the 3 files:

<details><summary>🔧 Bash: Generate package-info, architecture test and system CLAUDE.md, then run the test</summary>

```bash
M=src/main/java/pl/wojtyna/dddtrainingv3/orders && T=src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering && mkdir -p $M/ordering $T
cat > $M/ordering/package-info.java <<'EOF'
/// # Ordering bounded context
///
/// ## Purpose
/// Allows to prepare orders.
///
/// ## Strategic classification
/// - Domain: core
/// - Business model: revenue
/// - Evolution: custom built
///
/// ## Domain roles
/// - Execution Model
///
/// ## Ubiquitous language
/// - **Order**: a customer's collection of items, whose total value and per-type quantities are capped.
/// - **Item**: one unit added to an order, with an item type and a price in PLN.
/// - **Item type**: the kind of product an item is, e.g. "Helmet". Items of the same type count towards the same limit.
/// - **Order total**: the sum of the prices of all items in the order.
///
/// ## Business decisions
/// - **Order total cannot exceed 100 PLN** (*not modelled yet*, being built with `ddd-tactical-building-blocks`):
///   given an empty order, when an item for 100 PLN is added, then the item is added; given an order with items
///   totalling 90 PLN, when an item for 20 PLN is added, then adding the item is rejected.
/// - **No more than 3 items of the same type** (*not modelled yet*, being built with `ddd-tactical-building-blocks`):
///   given an order with 2 "Helmet" items, when another "Helmet" is added, then the item is added; given an order with
///   3 "Helmet" items, when another "Helmet" is added, then adding the item is rejected.
///
/// Source canvas: `docs/bounded-contexts/orders/ordering/canvas.md`
@BoundedContext(name = "Ordering")
@Module
package pl.wojtyna.dddtrainingv3.orders.ordering;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
EOF
sed -e 's/velocity\.fleetmaintenance/orders.ordering/' -e 's/FleetMaintenanceArchitectureTest/OrderingArchitectureTest/' -e 's/FleetMaintenance/Ordering/g' -e 's/fleet_maintenance/ordering/g' src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/FleetMaintenanceArchitectureTest.java > $T/OrderingArchitectureTest.java
cat > $M/CLAUDE.md <<'EOF'
# Orders system

Orders is a small practice system, created on 2026-11-04 to try the `ddd-tactical-building-blocks` skill on a simple aggregate. The repo-root `CLAUDE.md` covers the package conventions; this file adds Orders-specific context.

## Sources of truth

- `docs/bounded-contexts/orders/ordering/canvas.md` is the canvas, and `plan.md` next to it is the implementation plan.
- There is no vision document, context map or CML model for this system.

## Bounded contexts

- **Ordering** (`ordering`): customers prepare orders. The `Order` aggregate (event-sourced) guards two rules: the order total cannot exceed 100 PLN, and an order holds at most 3 items of the same item type. It is built one Given/When/Then scenario at a time. `Version`, the repository, the `AddItemToOrder` use case and the REST adapter come only after all domain tests are green.
EOF
grep -n "Ordering\|CONTEXT =" $T/OrderingArchitectureTest.java | head; ./mvnw -q test -Dtest=OrderingArchitectureTest 2>&1 | grep -E "Tests run|FAIL|ERROR|BUILD" | head
```

**Result:**

```
16:/// Context boundary rules for the Ordering bounded context: other contexts may depend on Ordering only through
17:/// its `api` package, and Ordering may depend on other contexts only through their `api` packages.
19:class OrderingArchitectureTest {
22:    private static final String CONTEXT = ROOT + ".orders.ordering";
```

</details>

<details><summary>🔧 Bash: Run full test suite and show skill file changes</summary>

```bash
./mvnw test 2>&1 | grep -E "Tests run:|FAIL|ERROR|BUILD" | tail -3; git diff --stat; git diff .claude/skills/ddd-tactical-building-blocks/SKILL.md | head -80
```

**Result:**

```
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.023 s -- in pl.wojtyna.dddtrainingv3.orders.ordering.OrderingArchitectureTest
[INFO] Tests run: 18, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

</details>

All 18 tests pass, and the skill file is unchanged since the commit. Next I'm marking the plan as implemented, then switching to the aggregate skill for cycle 1.

<details><summary>🔧 Bash: Mark plan implemented and read event-sourced template</summary>

```bash
python3 - <<'EOF'
p='docs/bounded-contexts/orders/ordering/plan.md'; s=open(p).read()
s=s.replace("- **Status**: approved","- **Status**: implemented",1)
s=s.replace("### 2026-11-04: plan drafted\n- Triggered by: new canvas.\n",
"### 2026-11-04: plan drafted\n- Triggered by: new canvas.\n\n### 2026-11-04: scaffolded\n- Added: `orders/CLAUDE.md`, `orders/ordering/package-info.java`, `OrderingArchitectureTest` (2 tests, green). Full suite: 18 tests, 0 failures.\n- Next: the `Order` aggregate with `ddd-tactical-building-blocks` (D3), then the use case, repository and REST adapter (D4).\n",1)
open(p,'w').write(s)
EOF
sed -n '/^## 6/,/^## 7/p' .claude/skills/ddd-tactical-building-blocks/references/aggregate-templates.md | head -150; cat src/main/java/pl/wojtyna/dddtrainingv3/common/domain/DomainEvents.java | head -60; ls src/main/java/pl/wojtyna/dddtrainingv3/common/domain/
```

**Result:**

````
## 6. Event-sourced aggregate

Commands validate against current state, create events, `apply` them to their own state, append them to the aggregate's **pending events**, and return **all pending events**: everything emitted since the aggregate was created or rebuilt, not only this command's events. `apply` is the only place that mutates state, and it never validates or throws, because past events are facts. Rebuilding the aggregate replays history through the same `apply`, without adding it to the pending events.

Why every command returns all pending events: if commands return only their own events, two commands on the same instance spread the new facts over two `DomainEvents`. A caller that keeps only the last one stores and publishes half the story, and a rebuild then disagrees with the state the aggregate had in memory, while `apply` accepts the gap silently because it never validates. When every command returns the whole pending list, the latest result is always complete. **Always use this approach for event-sourced aggregates.**

```java
@AggregateRoot
public class Order {

    private static final Money UNCONFIRMED_LIMIT = Money.of(CurrencyUnit.USD, 100);

    private final OrderId id;
    private final Version version;
    private final List<OrderEvent> pendingEvents = new ArrayList<>(); // emitted since open/rebuild, in order
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

    /** Applies the event, records it as pending and returns all pending events, not only this one. */
    private DomainEvents emit(OrderEvent event) {
        apply(event);
        pendingEvents.add(event);
        return DomainEvents.of(pendingEvents.toArray(DomainEvent[]::new));
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
- **Pending events:** `rebuild` leaves `pendingEvents` empty, because history is already stored. `open` starts with `OrderOpened` pending, so the first command after `open` returns `[OrderOpened, ItemAdded]`. Don't add a public "clear" or "mark committed" method. After a save, callers reload (see the versioning contract), which starts a fresh, empty pending list.
- **Test it:** when a scenario runs more than one command on the same instance, assert the last result with `containsExactly(...)` over *all* the pending events in order, e.g. `assertThat(events.stream()).containsExactly(new ItemAdded(...p1...), new ItemAdded(...p2...))`. Add such an assertion to at least one test of every event-sourced aggregate.
- In ES tests, the "given" can be a history: `Order.rebuild(id, List.of(new ItemAdded(...)))`. That's often clearer than replaying commands.

## 7. Tests
package pl.wojtyna.dddtrainingv3.common.domain;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DomainEvents {

    private final List<DomainEvent> eventList;

    public DomainEvents() {
        eventList = new LinkedList<>();
    }

    private DomainEvents(List<DomainEvent> eventList) {
        this.eventList = eventList;
    }

    public static DomainEvents empty() {
        return new DomainEvents();
    }

    public Stream<DomainEvent> stream() {
        return eventList.stream();
    }

    public static DomainEvents of(DomainEvent... eventsArray) {
        return new DomainEvents(Arrays.stream(eventsArray).collect(Collectors.toList()));
    }

    public boolean hasOccurredEventOfType(Class<? extends DomainEvent> type) {
        return stream().map(DomainEvent::getClass).anyMatch(type::isAssignableFrom);
    }

    public boolean hasAtLeastOneEventMatching(Predicate<DomainEvent> predicate) {
        return eventList.stream().anyMatch(predicate);
    }
}
DomainEvent.java
DomainEventPublisher.java
DomainEvents.java
DomainPolicy.java
````

</details>

Cycle 1 of the Order aggregate. Here's the design sketch:

<details><summary>🔧 Bash: Check joda-money dependency and test style</summary>

```bash
grep -n "joda" pom.xml | head -2; sed -n 1,60p src/test/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bikestation/BikeStationTest.java
```

**Result:**

```
66:			<groupId>org.joda</groupId>
67:			<artifactId>joda-money</artifactId>
package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

@DisplayName("Bike station examples")
class BikeStationTest {

    // @formatter:off
    @DisplayName(
        """
         given bike station with 1 bike,
         when George rents a bike,
         then bike is rent
        """
    )
    // @formatter:on
    @Test
    void test0() {
        // given
        var bikeStation = new BikeStation(new BikeStationId("Warsaw01"));
        var bike = new Bike(new BikeId("bike01"));
        bikeStation.placeNew(bike);
        var borrower = new Borrower(new BorrowerId("george"));

        // when
        var events = bikeStation
            .rent(bike)
            .by(borrower)
            .forDuration(new RentalDuration(Duration.ofHours(6)));

        // then
        events.hasAtLeastOneEventMatching(event -> event instanceof BikeRentalEvent.BikeRented);
    }

    // @formatter:off
    @DisplayName(
        """
         given bike station with 0 bikes,
         when George rents a bike,
         then bike is not rent
        """
    )
    // @formatter:on
    @Test
    void test1() {
        // given
        var bikeStation = new BikeStation(new BikeStationId("Warsaw01"));
        var bike = new Bike(new BikeId("bike01"));
        var borrower = new Borrower(new BorrowerId("george"));

        // when
        var exception = catchThrowable(() -> bikeStation.rent(bike).by(borrower)
                                                        .forDuration(new RentalDuration(Duration.ofHours(6))));
```

</details>

<details><summary>🔧 Bash: Write cycle-1 test with compile-only scaffolding and run it</summary>

```bash
D=src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order && T=src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order && mkdir -p $D $T
cat > $D/OrderId.java <<'EOF'
package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

import org.jmolecules.ddd.annotation.ValueObject;

@ValueObject
public record OrderId(String value) {
}
EOF
cat > $D/ItemType.java <<'EOF'
package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

import org.jmolecules.ddd.annotation.ValueObject;

/// The kind of product an item is, e.g. "Helmet". Items of the same type count towards the same limit.
@ValueObject
public record ItemType(String name) {
}
EOF
cat > $D/Item.java <<'EOF'
package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;

/// One unit added to an order, with an item type and a price.
@ValueObject
public record Item(ItemType type, Money price) {
}
EOF
cat > $D/OrderEvent.java <<'EOF'
package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

public sealed interface OrderEvent extends DomainEvent {

    record OrderOpened(OrderId orderId) implements OrderEvent {
    }

    record ItemAdded(OrderId orderId, Item item) implements OrderEvent {
    }
}
EOF
cat > $D/Order.java <<'EOF'
package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

/// Event-sourced order. Guards: the order total cannot exceed 100 PLN, and at most 3 items of the same item type.
@AggregateRoot
public class Order {

    private final OrderId id;

    private Order(OrderId id) {
        this.id = id;
    }

    public static NewOrder open(OrderId id) {
        return new NewOrder(new Order(id), DomainEvents.empty());
    }

    public record NewOrder(Order created, DomainEvents events) {
    }

    public DomainEvents add(Item item) {
        return DomainEvents.empty();
    }

    @Identity
    public OrderId id() {
        return id;
    }
}
EOF
cat > $T/OrderTest.java <<'EOF'
package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderEvent.ItemAdded;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderEvent.OrderOpened;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Order examples")
class OrderTest {

    private static final OrderId ORDER_ID = new OrderId("order01");
    private static final CurrencyUnit PLN = CurrencyUnit.of("PLN");

    // @formatter:off
    @DisplayName(
        """
         given an empty order,
         when an item for 100 PLN is added,
         then the item is added
        """
    )
    // @formatter:on
    @Test
    void itemForExactly100PlnFitsIntoEmptyOrder() {
        // given
        var order = Order.open(ORDER_ID).created();
        var helmet = new Item(new ItemType("Helmet"), Money.of(PLN, 100));

        // when
        var events = order.add(helmet);

        // then
        assertThat(events.stream()).containsExactly(new OrderOpened(ORDER_ID), new ItemAdded(ORDER_ID, helmet));
    }
}
EOF
./mvnw -q test -Dtest='OrderTest#itemForExactly100PlnFitsIntoEmptyOrder' 2>&1 | grep -E "Tests run:|FAIL|ERROR\]|Expecting|could not find|BUILD|OrderTest\." | head -8; grep -n "void itemForExactly" $T/OrderTest.java
```

**Result:**

```
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.080 s <<< FAILURE! -- in Order examples
[ERROR] pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderTest.itemForExactly100PlnFitsIntoEmptyOrder -- Time elapsed: 0.062 s <<< FAILURE!
Expecting actual:
but could not find the following elements:
	at pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderTest.itemForExactly100PlnFitsIntoEmptyOrder(OrderTest.java:37)
[ERROR] Failures: 
[ERROR]   OrderTest.itemForExactly100PlnFitsIntoEmptyOrder:37 
Expecting actual:
28:    void itemForExactly100PlnFitsIntoEmptyOrder() {
```

</details>

The test fails on its assertion, which is the right reason. Next I'm opening it in IntelliJ:

*Opened `src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/OrderTest.java` in IntelliJ.*

The Ordering context is in place, and cycle 1 of the `Order` aggregate is at its first stop. The test is written and failing for the right reason, and I've opened `OrderTest.java` in IntelliJ.

**Ordering context (scaffold done):** I generated the 3 files from the plan: `orders/CLAUDE.md`, `ordering/package-info.java` and `OrderingArchitectureTest`. The plan's status is `implemented`, and your answers to Q1–Q3 are recorded there. The full suite passes with 18 tests.

**Order aggregate, event-sourced:**
1. Order total ≤ 100 PLN. (a) An empty order accepts an item for 100 PLN. (b) An order at 90 PLN rejects an item for 20 PLN.
2. At most 3 items of the same type. (a) A 3rd Helmet is accepted. (b) A 4th Helmet is rejected. Helmets cost 10 PLN each in the tests.

The cycle order is 1a → 1b → 2a → 2b. `Version` and the repository come after all four are green, followed by the `AddItemToOrder` use case and the REST adapter.

| Invariant | Command | State needed to check it |
|---|---|---|
| Total ≤ 100 PLN | `order.add(item)` | the order total |
| ≤ 3 items per type | `order.add(item)` | number of items per type |

`add` takes a single `Item` (item type + price), as your ubiquitous language defines it, so it's a plain method rather than a fluent chain.

### Cycle 1: rule 1a, the test fails as expected

`OrderTest#itemForExactly100PlnFitsIntoEmptyOrder`, `src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/OrderTest.java:28`

```java
@DisplayName("""
     given an empty order,
     when an item for 100 PLN is added,
     then the item is added
    """)
@Test
void itemForExactly100PlnFitsIntoEmptyOrder() {
    // given
    var order = Order.open(ORDER_ID).created();
    var helmet = new Item(new ItemType("Helmet"), Money.of(PLN, 100));

    // when
    var events = order.add(helmet);

    // then
    assertThat(events.stream()).containsExactly(new OrderOpened(ORDER_ID), new ItemAdded(ORDER_ID, helmet));
}
```

**Failure:** `OrderTest.java:37`, `Expecting actual: [] ... but could not find the following elements`. The assertion fails, so the test fails for the right reason.

The assertion expects **every pending event** in order (`OrderOpened`, then `ItemAdded`). Because the aggregate is event-sourced, `add` must return everything emitted since `open`, not only its own event.

**Code added just to make the test compile** (in `orders/ordering/domain/order/`, with no logic):
- `OrderId`, `ItemType` and `Item(type, price)`, as records.
- A sealed `OrderEvent` with `OrderOpened` and `ItemAdded`.
- `Order` (`@AggregateRoot`): only an `id`. `open(id)` returns `NewOrder` with no events, and `add(item)` returns `DomainEvents.empty()`. There's no pending-events list and no `apply` yet.

> **❓ Claude asks:**
>
> **OrderTest#itemForExactly100PlnFitsIntoEmptyOrder (src/test/java/.../orders/ordering/domain/order/OrderTest.java) is red: Order.add(item) returns no events, but the test expects [OrderOpened, ItemAdded]. Who writes the implementation that makes it pass?**
>
> - *I'll write it myself*: You implement Order (open, add, pending events) and tell me when you're done. I'll run the tests.
> - *Scaffold it for me*: I write the minimal implementation and show it to you to confirm before cycle 2
>
> **✅ User picked:** I'll write it myself
>

Implement `Order` so that `OrderTest#itemForExactly100PlnFitsIntoEmptyOrder` passes, then tell me when you're done. I'll run the `Order` tests and the full suite.

For this test you only need `open` to emit `OrderOpened` and `add` to emit `ItemAdded`, with `add` returning both events in order. The 100 PLN limit isn't needed yet; cycle 2's test will force it.


---

### 👤 User

done, run the tests


### 🤖 Claude

<details><summary>🔧 Bash: Show the user's Order implementation and run Order and full tests</summary>

```bash
git status --short; git diff --stat; cat src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/Order.java; ls src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/; ./mvnw -q test -Dtest=OrderTest 2>&1 | grep -E "Tests run:|FAIL|ERROR\]|BUILD" | head -5; echo "--- full"; ./mvnw test 2>&1 | grep -E "Tests run:|FAIL|BUILD" | tail -2
```

**Result:**

```
M src/main/java/pl/wojtyna/dddtrainingv3/common/domain/DomainEvents.java
?? docs/bounded-contexts/orders/
?? src/main/java/pl/wojtyna/dddtrainingv3/orders/
?? src/test/java/pl/wojtyna/dddtrainingv3/orders/
 .../java/pl/wojtyna/dddtrainingv3/common/domain/DomainEvents.java     | 4 ++++
 1 file changed, 4 insertions(+)
package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

/// Event-sourced order. Guards: the order total cannot exceed 100 PLN, and at most 3 items of the same item type.
@AggregateRoot
public class Order {

    private final OrderId id;
    private final DomainEvents pendingEvents;

    private Order(OrderId id, DomainEvents domainEvents) {
        this.id = id;
        this.pendingEvents = domainEvents;
    }

    public static Order open(OrderId id) {
        return new Order(id, DomainEvents.of(new OrderEvent.OrderOpened(id)));
    }

    public DomainEvents add(Item item) {
        return pendingEvents.append(DomainEvents.of(new OrderEvent.ItemAdded(id, item)));
    }

    @Identity
    public OrderId id() {
        return id;
    }
}
Item.java
ItemType.java
Order.java
OrderEvent.java
OrderId.java
--- full
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

</details>

<details><summary>🔧 Bash: Show DomainEvents change and the current test body</summary>

```bash
git diff src/main/java/pl/wojtyna/dddtrainingv3/common/domain/DomainEvents.java; sed -n 26,40p src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/OrderTest.java
```

**Result:**

```
diff --git a/src/main/java/pl/wojtyna/dddtrainingv3/common/domain/DomainEvents.java b/src/main/java/pl/wojtyna/dddtrainingv3/common/domain/DomainEvents.java
index 9e80a16..30ee0f5 100644
--- a/src/main/java/pl/wojtyna/dddtrainingv3/common/domain/DomainEvents.java
+++ b/src/main/java/pl/wojtyna/dddtrainingv3/common/domain/DomainEvents.java
@@ -38,4 +38,8 @@ public class DomainEvents {
     public boolean hasAtLeastOneEventMatching(Predicate<DomainEvent> predicate) {
         return eventList.stream().anyMatch(predicate);
     }
+
+    public DomainEvents append(DomainEvents events) {
+        return new DomainEvents(Stream.concat(eventList.stream(), events.stream()).collect(Collectors.toList()));
+    }
 }
    // @formatter:on
    @Test
    void itemForExactly100PlnFitsIntoEmptyOrder() {
        // given
        var order = Order.open(ORDER_ID);
        var helmet = new Item(new ItemType("Helmet"), Money.of(PLN, 100));

        // when
        var events = order.add(helmet);

        // then
        assertThat(events.stream()).containsExactly(new OrderOpened(ORDER_ID), new ItemAdded(ORDER_ID, helmet));
    }
}
```

</details>

<details><summary>🔧 Bash: Read existing domain exception style</summary>

```bash
cat src/main/java/pl/wojtyna/dddtrainingv3/velocity/rentals/domain/bikestation/BikeNotAvailableException.java
```

**Result:**

```
package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation;

public class BikeNotAvailableException extends RuntimeException {

    public BikeNotAvailableException(String message) {
        super(message);
    }

    public BikeNotAvailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

</details>

Your implementation passes: `OrderTest` is green, and the full suite passes with 19 tests. On to cycle 2:

<details><summary>🔧 Bash: Write cycle-2 test and exception, run it for red</summary>

```bash
D=src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order && T=src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/OrderTest.java
cat > $D/OrderTotalLimitExceededException.java <<'EOF'
package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

/// Thrown when adding an item would make the order total exceed 100 PLN.
public class OrderTotalLimitExceededException extends RuntimeException {

    public OrderTotalLimitExceededException(String message) {
        super(message);
    }
}
EOF
python3 - "$T" <<'EOF'
import sys
p=sys.argv[1]; s=open(p).read()
s=s.replace("import static org.assertj.core.api.Assertions.assertThat;","import static org.assertj.core.api.Assertions.assertThat;\nimport static org.assertj.core.api.Assertions.catchThrowable;")
new='''
    // @formatter:off
    @DisplayName(
        """
         given an order with items totalling 90 PLN,
         when an item for 20 PLN is added,
         then adding the item is rejected
        """
    )
    // @formatter:on
    @Test
    void itemPushingTotalAbove100PlnIsRejected() {
        // given
        var order = Order.open(ORDER_ID);
        order.add(new Item(new ItemType("Helmet"), Money.of(PLN, 50)));
        order.add(new Item(new ItemType("Lock"), Money.of(PLN, 40)));
        var light = new Item(new ItemType("Light"), Money.of(PLN, 20));

        // when
        var exception = catchThrowable(() -> order.add(light));

        // then
        assertThat(exception).isInstanceOf(OrderTotalLimitExceededException.class);
    }
}
'''
i=s.rstrip().rfind("}"); s=s[:i].rstrip()+"\n"+new
open(p,'w').write(s)
EOF
./mvnw -q test -Dtest='OrderTest#itemPushingTotalAbove100PlnIsRejected' 2>&1 | grep -E "Tests run:|OrderTest\.|Expecting|but was|instance of|BUILD" | head -6; grep -n "void itemPushing" $T
```

**Result:**

```
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.071 s <<< FAILURE! -- in Order examples
[ERROR] pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderTest.itemPushingTotalAbove100PlnIsRejected -- Time elapsed: 0.052 s <<< FAILURE!
Expecting actual not to be null
	at pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderTest.itemPushingTotalAbove100PlnIsRejected(OrderTest.java:62)
[ERROR]   OrderTest.itemPushingTotalAbove100PlnIsRejected:62 
Expecting actual not to be null
51:    void itemPushingTotalAbove100PlnIsRejected() {
```

</details>

*Opened `src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/OrderTest.java` in IntelliJ.*

Cycle 2 is at its first stop. The test is written and fails because no exception is thrown. I've opened `OrderTest.java` in IntelliJ.

### Cycle 1: all tests pass
`OrderTest` passes, and so does the full suite (19 tests). There are three things in your implementation to look at. I haven't changed any of them.

- **You changed the test.** It now calls `Order.open(ORDER_ID)` instead of `Order.open(ORDER_ID).created()`, because `open` now returns `Order` instead of `NewOrder`. What the test checks is unchanged, but the tests are the spec, so I'm asking you to confirm this below.
- **Events from a second `add` on the same order get lost.** `pendingEvents` is `final`, and `append` returns a new `DomainEvents` without updating it. So `order.add(a); order.add(b)` returns `[OrderOpened, ItemAdded(b)]`, and `ItemAdded(a)` is gone. That's a fine shortcut for cycle 1, and cycle 2 depends on that state, so this is where it starts to matter.
- **`DomainEvents.append` lives in the shared top-level `common`.** That's the right place for it if every system should have it. It's immutable, which is good.

### Cycle 2: rule 1b, the test fails as expected
`OrderTest#itemPushingTotalAbove100PlnIsRejected`, `src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/OrderTest.java:51`

```java
@DisplayName("""
     given an order with items totalling 90 PLN,
     when an item for 20 PLN is added,
     then adding the item is rejected
    """)
@Test
void itemPushingTotalAbove100PlnIsRejected() {
    // given
    var order = Order.open(ORDER_ID);
    order.add(new Item(new ItemType("Helmet"), Money.of(PLN, 50)));
    order.add(new Item(new ItemType("Lock"), Money.of(PLN, 40)));
    var light = new Item(new ItemType("Light"), Money.of(PLN, 20));

    // when
    var exception = catchThrowable(() -> order.add(light));

    // then
    assertThat(exception).isInstanceOf(OrderTotalLimitExceededException.class);
}
```

**Failure:** `OrderTest.java:62`, `Expecting actual not to be null`. No exception is thrown, so the test fails for the right reason.

The 90 PLN is made of 2 items of different types (50 + 40), so the rule of at most 3 items per type can't interfere. To pass, `Order` has to remember the total across calls, which means fixing the lost-events problem above.

**Added only so the test compiles:** `OrderTotalLimitExceededException`, a `RuntimeException` in the same style as `BikeNotAvailableException`.

> **❓ Claude asks:**
>
> **OrderTest#itemPushingTotalAbove100PlnIsRejected (src/test/java/.../orders/ordering/domain/order/OrderTest.java) is red: adding a 20 PLN item to an order at 90 PLN throws nothing. Who writes the implementation?**
>
> - *I'll write it myself*: You make Order track its total and throw OrderTotalLimitExceededException, then tell me when you're done
> - *Scaffold it for me*: I write the minimal implementation and show it to you to confirm before cycle 3
>
> **✅ User picked:** I'll write it myself
>
> **In cycle 1 you changed itemForExactly100PlnFitsIntoEmptyOrder from Order.open(ORDER_ID).created() to Order.open(ORDER_ID), because open now returns Order instead of NewOrder. What the test checks didn't change. Do you accept the change?**
>
> - *Yes, keep it*: open returns Order. Later tests use that shape too, as cycle 2 already does
> - *Revert to NewOrder*: Keep open returning NewOrder(created, events), as in the skill's event-sourced template
>
> **✅ User picked:** Yes, keep it
>

The change to the cycle 1 test stays: `open` returns `Order`.

Over to you for cycle 2. Make `OrderTest#itemPushingTotalAbove100PlnIsRejected` pass and keep the cycle 1 test green, then tell me when you're done and I'll run the `Order` tests and the full suite.


---

### 👤 User

YOu know what, do the implementation for me. Im too lazy, but I want to control what happens.


### 🤖 Claude

<details><summary>🔧 Bash: Re-read current Order before changing it</summary>

```bash
cat src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/Order.java
```

**Result:**

```
package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

/// Event-sourced order. Guards: the order total cannot exceed 100 PLN, and at most 3 items of the same item type.
@AggregateRoot
public class Order {

    private final OrderId id;
    private final DomainEvents pendingEvents;

    private Order(OrderId id, DomainEvents domainEvents) {
        this.id = id;
        this.pendingEvents = domainEvents;
    }

    public static Order open(OrderId id) {
        return new Order(id, DomainEvents.of(new OrderEvent.OrderOpened(id)));
    }

    public DomainEvents add(Item item) {
        return pendingEvents.append(DomainEvents.of(new OrderEvent.ItemAdded(id, item)));
    }

    @Identity
    public OrderId id() {
        return id;
    }
}
```

</details>

<details><summary>🔧 Bash: Implement the 100 PLN limit minimally and run tests</summary>

```bash
F=src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/Order.java && cp $F /tmp/Order.before.java 2>/dev/null; python3 - "$F" <<'EOF'
import sys
p=sys.argv[1]; s=open(p).read()
reps=[
("import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;\n",
 "import org.joda.money.CurrencyUnit;\nimport org.joda.money.Money;\nimport pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;\n"),
("public class Order {\n\n    private final OrderId id;",
 "public class Order {\n\n    private static final Money ORDER_TOTAL_LIMIT = Money.of(CurrencyUnit.of(\"PLN\"), 100);\n\n    private final OrderId id;"),
("    private final DomainEvents pendingEvents;\n",
 "    private final DomainEvents pendingEvents;\n    private Money total = Money.zero(CurrencyUnit.of(\"PLN\"));\n"),
("    public DomainEvents add(Item item) {\n        return pendingEvents.append(DomainEvents.of(new OrderEvent.ItemAdded(id, item)));\n    }\n",
 "    public DomainEvents add(Item item) {\n        if (total.plus(item.price()).isGreaterThan(ORDER_TOTAL_LIMIT)) {\n            throw new OrderTotalLimitExceededException(\"Order total cannot exceed \" + ORDER_TOTAL_LIMIT);\n        }\n        var itemAdded = new OrderEvent.ItemAdded(id, item);\n        apply(itemAdded);\n        return pendingEvents.append(DomainEvents.of(itemAdded));\n    }\n\n    private void apply(OrderEvent.ItemAdded event) {\n        total = total.plus(event.item().price());\n    }\n"),
]
for a,b in reps:
    assert s.count(a)==1,a[:40]; s=s.replace(a,b)
open(p,'w').write(s)
EOF
./mvnw -q test -Dtest=OrderTest 2>&1 | grep -E "Tests run:|FAIL|ERROR\]" | head -4; echo "--- full"; ./mvnw test 2>&1 | grep -E "Tests run:|FAIL|BUILD" | tail -2; diff -u /tmp/Order.before.java $F; rm -f /tmp/Order.before.java
```

**Result:**

```
--- full
[INFO] Tests run: 20, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
--- /tmp/Order.before.java	2026-11-04 19:51:46.386792516 +0100
+++ src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/Order.java	2026-11-04 19:51:46.398603111 +0100
@@ -2,14 +2,19 @@
 
 import org.jmolecules.ddd.annotation.AggregateRoot;
 import org.jmolecules.ddd.annotation.Identity;
+import org.joda.money.CurrencyUnit;
+import org.joda.money.Money;
 import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
 
 /// Event-sourced order. Guards: the order total cannot exceed 100 PLN, and at most 3 items of the same item type.
 @AggregateRoot
 public class Order {
 
+    private static final Money ORDER_TOTAL_LIMIT = Money.of(CurrencyUnit.of("PLN"), 100);
+
     private final OrderId id;
     private final DomainEvents pendingEvents;
+    private Money total = Money.zero(CurrencyUnit.of("PLN"));
 
     private Order(OrderId id, DomainEvents domainEvents) {
         this.id = id;
@@ -21,7 +26,16 @@
     }
 
     public DomainEvents add(Item item) {
-        return pendingEvents.append(DomainEvents.of(new OrderEvent.ItemAdded(id, item)));
+        if (total.plus(item.price()).isGreaterThan(ORDER_TOTAL_LIMIT)) {
+            throw new OrderTotalLimitExceededException("Order total cannot exceed " + ORDER_TOTAL_LIMIT);
+        }
+        var itemAdded = new OrderEvent.ItemAdded(id, item);
+        apply(itemAdded);
+        return pendingEvents.append(DomainEvents.of(itemAdded));
+    }
+
+    private void apply(OrderEvent.ItemAdded event) {
+        total = total.plus(event.item().price());
     }
 
     @Identity
```

</details>

*Opened `src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/Order.java` in IntelliJ.*

> **❓ Claude asks:**
>
> **Cycle 2 implementation in Order.java: a total field, a 100 PLN check in add(), and the total updated in apply(ItemAdded). OrderTest is 2/2 green and the full suite 20/20. What next?**
>
> - *Looks good, next scenario*: Go to cycle 3: given 2 Helmets, a 3rd Helmet is added
> - *Change something*: Tell me what to change and I'll re-run the tests and show it again
> - *Refactor: keep pending events*: Make pendingEvents a list that every command appends to, so a second add() returns all events. Includes a test that checks that
>
> **✅ User picked:** Refactor: keep pending events
>

I'm writing the test first so we can see it fail:

<details><summary>🔧 Bash: Add a pending-events test and run it for red</summary>

```bash
T=src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/OrderTest.java && python3 - "$T" <<'EOF'
import sys
p=sys.argv[1]; s=open(p).read()
new='''
    // @formatter:off
    @DisplayName(
        """
         given an order with a Helmet for 50 PLN added,
         when a Lock for 40 PLN is added,
         then all events since the order was opened are returned, in order
        """
    )
    // @formatter:on
    @Test
    void everyCommandReturnsAllPendingEvents() {
        // given
        var order = Order.open(ORDER_ID);
        var helmet = new Item(new ItemType("Helmet"), Money.of(PLN, 50));
        order.add(helmet);
        var lock = new Item(new ItemType("Lock"), Money.of(PLN, 40));

        // when
        var events = order.add(lock);

        // then
        assertThat(events.stream()).containsExactly(new OrderOpened(ORDER_ID),
                                                    new ItemAdded(ORDER_ID, helmet),
                                                    new ItemAdded(ORDER_ID, lock));
    }
}
'''
i=s.rstrip().rfind("}"); s=s[:i].rstrip()+"\n"+new
open(p,'w').write(s)
EOF
./mvnw -q test -Dtest='OrderTest#everyCommandReturnsAllPendingEvents' 2>&1 | grep -E "OrderTest\.ever|could not find|Tests run" | head -4
```

**Result:**

```
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.080 s <<< FAILURE! -- in Order examples
[ERROR] pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderTest.everyCommandReturnsAllPendingEvents -- Time elapsed: 0.062 s <<< FAILURE!
but could not find the following elements:
	at pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderTest.everyCommandReturnsAllPendingEvents(OrderTest.java:86)
```

</details>

The test fails as expected: the Helmet's `ItemAdded` is missing from the second result. Now the fix:

<details><summary>🔧 Bash: Keep pending events across commands and run tests</summary>

```bash
F=src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/Order.java && python3 - "$F" <<'EOF'
import sys
p=sys.argv[1]; s=open(p).read()
reps=[("    private final DomainEvents pendingEvents;\n","    private DomainEvents pendingEvents; // every event emitted since open, in order\n"),
("        return pendingEvents.append(DomainEvents.of(itemAdded));\n","        pendingEvents = pendingEvents.append(DomainEvents.of(itemAdded));\n        return pendingEvents;\n")]
for a,b in reps:
    assert s.count(a)==1; s=s.replace(a,b)
open(p,'w').write(s)
EOF
./mvnw -q test -Dtest=OrderTest 2>&1 | grep -E "Tests run:|FAIL" | head -3; echo "--- full"; ./mvnw test 2>&1 | grep -E "Tests run:|BUILD" | tail -2
```

**Result:**

```
--- full
[INFO] Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

</details>

*Opened `src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/Order.java` in IntelliJ.*

> **❓ Claude asks:**
>
> **Refactor done in Order.java: pendingEvents now builds up across commands, and the new OrderTest#everyCommandReturnsAllPendingEvents passes (OrderTest 3/3, full suite 21/21). What next?**
>
> - *Looks good, next scenario*: Go to cycle 3 (rule 2a): given an order with 2 Helmets, when another Helmet is added, then it is added
> - *Change something*: Tell me what to change and I'll re-run the tests and show it again
>
> **✅ User picked:** Looks good, next scenario
>

On to cycle 3:

<details><summary>🔧 Bash: Write cycle-3 test and run it</summary>

```bash
T=src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/OrderTest.java && python3 - "$T" <<'EOF'
import sys
p=sys.argv[1]; s=open(p).read()
new='''
    // @formatter:off
    @DisplayName(
        """
         given an order with 2 "Helmet" items,
         when another "Helmet" is added,
         then the item is added
        """
    )
    // @formatter:on
    @Test
    void thirdItemOfTheSameTypeIsAdded() {
        // given
        var order = Order.open(ORDER_ID);
        var helmet = new Item(new ItemType("Helmet"), Money.of(PLN, 10));
        order.add(helmet);
        order.add(helmet);

        // when
        var events = order.add(helmet);

        // then
        assertThat(events.stream()).containsExactly(new OrderOpened(ORDER_ID),
                                                    new ItemAdded(ORDER_ID, helmet),
                                                    new ItemAdded(ORDER_ID, helmet),
                                                    new ItemAdded(ORDER_ID, helmet));
    }
}
'''
i=s.rstrip().rfind("}"); s=s[:i].rstrip()+"\n"+new
open(p,'w').write(s)
EOF
./mvnw -q test -Dtest='OrderTest#thirdItemOfTheSameTypeIsAdded' 2>&1 | grep -E "Tests run|FAIL" | head -3; echo "exit=$?"; grep -n "void thirdItem" $T
```

**Result:**

```
exit=0
101:    void thirdItemOfTheSameTypeIsAdded() {
```

</details>

*Opened `src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/OrderTest.java` in IntelliJ.*

> **❓ Claude asks:**
>
> **OrderTest#thirdItemOfTheSameTypeIsAdded (rule 2a: given 2 Helmets, a 3rd Helmet is added) passed on its first run, so there's nothing to implement. What next?**
>
> - *Keep it, next scenario*: Go to cycle 4 (rule 2b): given 3 Helmets, a 4th Helmet is rejected
> - *Change the test*: Tell me what to change in the scenario or the test
>
> **✅ User picked:** Keep it, next scenario
>

<details><summary>🔧 Bash: Write cycle-4 test and exception, run for red</summary>

```bash
D=src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order && T=$D/../../../../../../../../test/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/OrderTest.java
T=src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/OrderTest.java
cat > $D/ItemTypeLimitExceededException.java <<'EOF'
package pl.wojtyna.dddtrainingv3.orders.ordering.domain.order;

/// Thrown when adding an item would put more than 3 items of the same item type into the order.
public class ItemTypeLimitExceededException extends RuntimeException {

    public ItemTypeLimitExceededException(String message) {
        super(message);
    }
}
EOF
python3 - "$T" <<'EOF'
import sys
p=sys.argv[1]; s=open(p).read()
new='''
    // @formatter:off
    @DisplayName(
        """
         given an order with 3 "Helmet" items,
         when another "Helmet" is added,
         then adding the item is rejected
        """
    )
    // @formatter:on
    @Test
    void fourthItemOfTheSameTypeIsRejected() {
        // given
        var order = Order.open(ORDER_ID);
        var helmet = new Item(new ItemType("Helmet"), Money.of(PLN, 10));
        order.add(helmet);
        order.add(helmet);
        order.add(helmet);

        // when
        var exception = catchThrowable(() -> order.add(helmet));

        // then
        assertThat(exception).isInstanceOf(ItemTypeLimitExceededException.class);
    }
}
'''
i=s.rstrip().rfind("}"); s=s[:i].rstrip()+"\n"+new
open(p,'w').write(s)
EOF
./mvnw -q test -Dtest='OrderTest#fourthItemOfTheSameTypeIsRejected' 2>&1 | grep -E "OrderTest\.fourth|Expecting|Tests run" | head -3; grep -n "void fourthItem" $T
```

**Result:**

```
[ERROR] Tests run: 1, Failures: 1, Errors: 0, Skipped: 0, Time elapsed: 0.070 s <<< FAILURE! -- in Order examples
[ERROR] pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderTest.fourthItemOfTheSameTypeIsRejected -- Time elapsed: 0.051 s <<< FAILURE!
Expecting actual not to be null
128:    void fourthItemOfTheSameTypeIsRejected() {
```

</details>

*Opened `src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/OrderTest.java` in IntelliJ.*

> **❓ Claude asks:**
>
> **OrderTest#fourthItemOfTheSameTypeIsRejected (src/test/java/.../orders/ordering/domain/order/OrderTest.java:128) is red: a 4th Helmet is accepted instead of throwing ItemTypeLimitExceededException. Who writes the implementation?**
>
> - *I'll write it myself*: You make Order count items per type and reject the 4th, then tell me when you're done
> - *Scaffold it for me*: I write the minimal implementation and show it to you to confirm
>
> **✅ User picked:** Scaffold it for me
>

<details><summary>🔧 Bash: Implement per-type item limit and run tests</summary>

```bash
F=src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/Order.java && cp $F /tmp/Order.before.java && python3 - "$F" <<'EOF'
import sys
p=sys.argv[1]; s=open(p).read()
reps=[
("import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;\n",
 "import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;\n\nimport java.util.HashMap;\nimport java.util.Map;\n"),
("    private static final Money ORDER_TOTAL_LIMIT = Money.of(CurrencyUnit.of(\"PLN\"), 100);\n",
 "    private static final Money ORDER_TOTAL_LIMIT = Money.of(CurrencyUnit.of(\"PLN\"), 100);\n    private static final int ITEMS_PER_TYPE_LIMIT = 3;\n"),
("    private Money total = Money.zero(CurrencyUnit.of(\"PLN\"));\n",
 "    private Money total = Money.zero(CurrencyUnit.of(\"PLN\"));\n    private final Map<ItemType, Integer> itemsPerType = new HashMap<>();\n"),
("        var itemAdded = new OrderEvent.ItemAdded(id, item);\n",
 "        if (itemsPerType.getOrDefault(item.type(), 0) >= ITEMS_PER_TYPE_LIMIT) {\n            throw new ItemTypeLimitExceededException(\"Order cannot hold more than \" + ITEMS_PER_TYPE_LIMIT + \" items of type \" + item.type().name());\n        }\n        var itemAdded = new OrderEvent.ItemAdded(id, item);\n"),
("        total = total.plus(event.item().price());\n",
 "        total = total.plus(event.item().price());\n        itemsPerType.merge(event.item().type(), 1, Integer::sum);\n"),
]
for a,b in reps:
    assert s.count(a)==1,a[:40]; s=s.replace(a,b)
open(p,'w').write(s)
EOF
./mvnw -q test -Dtest=OrderTest 2>&1 | grep -E "Tests run:|FAIL" | head -3; echo "--- full"; ./mvnw test 2>&1 | grep -E "Tests run:|BUILD" | tail -2; diff -u /tmp/Order.before.java $F | sed 1,2d; rm /tmp/Order.before.java
```

**Result:**

```
--- full
[INFO] Tests run: 23, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
@@ -6,15 +6,20 @@
 import org.joda.money.Money;
 import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
 
+import java.util.HashMap;
+import java.util.Map;
+
 /// Event-sourced order. Guards: the order total cannot exceed 100 PLN, and at most 3 items of the same item type.
 @AggregateRoot
 public class Order {
 
     private static final Money ORDER_TOTAL_LIMIT = Money.of(CurrencyUnit.of("PLN"), 100);
+    private static final int ITEMS_PER_TYPE_LIMIT = 3;
 
     private final OrderId id;
     private DomainEvents pendingEvents; // every event emitted since open, in order
     private Money total = Money.zero(CurrencyUnit.of("PLN"));
+    private final Map<ItemType, Integer> itemsPerType = new HashMap<>();
 
     private Order(OrderId id, DomainEvents domainEvents) {
         this.id = id;
@@ -29,6 +34,9 @@
         if (total.plus(item.price()).isGreaterThan(ORDER_TOTAL_LIMIT)) {
             throw new OrderTotalLimitExceededException("Order total cannot exceed " + ORDER_TOTAL_LIMIT);
         }
+        if (itemsPerType.getOrDefault(item.type(), 0) >= ITEMS_PER_TYPE_LIMIT) {
+            throw new ItemTypeLimitExceededException("Order cannot hold more than " + ITEMS_PER_TYPE_LIMIT + " items of type " + item.type().name());
+        }
         var itemAdded = new OrderEvent.ItemAdded(id, item);
         apply(itemAdded);
         pendingEvents = pendingEvents.append(DomainEvents.of(itemAdded));
@@ -37,6 +45,7 @@
 
     private void apply(OrderEvent.ItemAdded event) {
         total = total.plus(event.item().price());
+        itemsPerType.merge(event.item().type(), 1, Integer::sum);
     }
 
     @Identity
```

</details>

*Opened `src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/Order.java` in IntelliJ.*

> **❓ Claude asks:**
>
> **Cycle 4 implementation in Order.java: an itemsPerType count updated in apply(ItemAdded) and a limit of 3 per type checked in add(). OrderTest is 5/5 green and the full suite 23/23. This was the last scenario. What next?**
>
> - *Looks good, continue*: All domain scenarios are done. Move on to Version + repository, then the use case and REST adapter
> - *Change something*: Tell me what to change and I'll re-run the tests and show it again
>
> **✅ User picked:** Looks good, continue
>

<details><summary>🔧 Bash: Read Version, repository and use-case templates</summary>

```bash
R=.claude/skills/ddd-tactical-building-blocks/references; sed -n '/^## 1\./,/^## 2\./p' $R/aggregate-templates.md; sed -n '/^## 8/,$p' $R/aggregate-templates.md; sed -n '/[Uu]se case/,+25p' $R/other-building-blocks.md | head -50
```

**Result:**

````
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

For **event-sourced** aggregates, `save` takes the new events, because they are what gets stored. Pass the result of the **last** command: it already holds all pending events (§6), so nothing from earlier commands on the same instance is lost:

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
A use case is a `@PrimaryPort` class in `<ctx>/api/usecases/`, one per use case. It's a plain class: Spring wiring happens in `infra/config`. It runs exactly one aggregate command per transaction:
…(49 lines truncated)
````

</details>

<details><summary>🔧 Bash: Read FleetMaintenance use case, config and controller examples</summary>

```bash
cd src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance && find . -name "*.java" | sort; cat api/usecases/ReportFaultUseCase.java infra/config/*.java infra/adapters/primary/TechnicianController.java
```

**Result:**

```
./api/usecases/BikeUnlockedByTechnician.java
./api/usecases/ReportFault.java
./api/usecases/ReportFaultUseCase.java
./api/usecases/ReturnBikeToCirculation.java
./api/usecases/ReturnBikeToCirculationUseCase.java
./api/usecases/StartMaintenanceUseCase.java
./domain/bike/BikeId.java
./domain/bike/Bike.java
./domain/bike/BikeMaintenanceEvent.java
./domain/bike/BikeRepository.java
./domain/bike/Circulation.java
./domain/bike/FixNotifications.java
./domain/bike/MaintenanceNotStarted.java
./domain/bike/NotifySupportOfFixedBikePolicy.java
./domain/bike/ReturnFixedBikeToCirculationPolicy.java
./domain/bike/TechnicianId.java
./infra/adapters/primary/ReturnToCirculationRequest.java
./infra/adapters/primary/TechnicianController.java
./infra/adapters/secondary/customersupportctxacl/CustomerSupportFixNotificationsAdapter.java
./infra/adapters/secondary/customersupportctxacl/package-info.java
./infra/adapters/secondary/InMemoryBikeRepository.java
./infra/adapters/secondary/rentalsctxacl/package-info.java
./infra/adapters/secondary/rentalsctxacl/RentalsCirculationAdapter.java
./infra/config/FleetMaintenanceContextConfig.java
./infra/event/FleetMaintenanceLocalEventsHandler.java
./package-info.java
package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.Bike;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeRepository;

@PrimaryPort
public class ReportFaultUseCase {

    private final BikeRepository bikes;
    private final DomainEventPublisher eventPublisher;

    public ReportFaultUseCase(BikeRepository bikes, DomainEventPublisher eventPublisher) {
        this.bikes = bikes;
        this.eventPublisher = eventPublisher;
    }

    public void handle(ReportFault command) {
        var id = new BikeId(command.bikeId());
        var bike = bikes.load(id).orElseGet(() -> Bike.inCirculation(id));
        var events = bike.reportFault();
        bikes.save(bike);
        eventPublisher.publish(events);
    }
}
package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReportFaultUseCase;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReturnBikeToCirculationUseCase;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.StartMaintenanceUseCase;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.*;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.InMemoryBikeRepository;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.customersupportctxacl.CustomerSupportFixNotificationsAdapter;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.rentalsctxacl.RentalsCirculationAdapter;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.event.FleetMaintenanceLocalEventsHandler;

@Configuration
public class FleetMaintenanceContextConfig {

    @Bean
    public BikeRepository fleetMaintenanceBikeRepository() {
        return new InMemoryBikeRepository();
    }

    @Bean
    public Circulation fleetMaintenanceCirculation() {
        return new RentalsCirculationAdapter();
    }

    @Bean
    public FixNotifications fleetMaintenanceFixNotifications() {
        return new CustomerSupportFixNotificationsAdapter();
    }

    @Bean
    public ReportFaultUseCase fleetMaintenanceReportFaultUseCase(BikeRepository fleetMaintenanceBikeRepository,
                                                                 DomainEventPublisher eventPublisher) {
        return new ReportFaultUseCase(fleetMaintenanceBikeRepository, eventPublisher);
    }

    @Bean
    public StartMaintenanceUseCase fleetMaintenanceStartMaintenanceUseCase(BikeRepository fleetMaintenanceBikeRepository,
                                                                           DomainEventPublisher eventPublisher) {
        return new StartMaintenanceUseCase(fleetMaintenanceBikeRepository, eventPublisher);
    }

    @Bean
    public ReturnBikeToCirculationUseCase fleetMaintenanceReturnBikeToCirculationUseCase(
        BikeRepository fleetMaintenanceBikeRepository, DomainEventPublisher eventPublisher) {
        return new ReturnBikeToCirculationUseCase(fleetMaintenanceBikeRepository, eventPublisher);
    }

    @Bean
    public ReturnFixedBikeToCirculationPolicy fleetMaintenanceReturnFixedBikeToCirculationPolicy(
        Circulation fleetMaintenanceCirculation) {
        return new ReturnFixedBikeToCirculationPolicy(fleetMaintenanceCirculation);
    }

    @Bean
    public NotifySupportOfFixedBikePolicy fleetMaintenanceNotifySupportOfFixedBikePolicy(
        FixNotifications fleetMaintenanceFixNotifications) {
        return new NotifySupportOfFixedBikePolicy(fleetMaintenanceFixNotifications);
    }

    @Bean
    public FleetMaintenanceLocalEventsHandler fleetMaintenanceLocalEventsHandler(
        ReturnFixedBikeToCirculationPolicy fleetMaintenanceReturnFixedBikeToCirculationPolicy,
        NotifySupportOfFixedBikePolicy fleetMaintenanceNotifySupportOfFixedBikePolicy) {
        return new FleetMaintenanceLocalEventsHandler(fleetMaintenanceReturnFixedBikeToCirculationPolicy,
                                                      fleetMaintenanceNotifySupportOfFixedBikePolicy);
    }
}
package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReportFault;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReportFaultUseCase;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReturnBikeToCirculation;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReturnBikeToCirculationUseCase;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.MaintenanceNotStarted;

/// REST API for the technician app: the canvas commands *Report Fault* and *Return Bike to Circulation*.
@PrimaryAdapter
@RestController
@RequestMapping("/fleet-maintenance/bikes/{bikeId}")
public class TechnicianController {

    private final ReportFaultUseCase reportFault;
    private final ReturnBikeToCirculationUseCase returnBikeToCirculation;

    public TechnicianController(ReportFaultUseCase reportFault,
                                ReturnBikeToCirculationUseCase returnBikeToCirculation) {
        this.reportFault = reportFault;
        this.returnBikeToCirculation = returnBikeToCirculation;
    }

    @PostMapping("/fault-reports")
…(18 lines truncated)
```

</details>

<details><summary>🔧 Bash: Check shared types, Order constructor and test examples</summary>

```bash
cd . && ls src/main/java/pl/wojtyna/dddtrainingv3/common/domain/; cat src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/Order.java | sed -n 20,40p; cat src/main/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/infra/adapters/primary/ReturnToCirculationRequest.java; ls src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance -R | head -20
```

**Result:**

```
DomainEvent.java
DomainEventPublisher.java
DomainEvents.java
DomainPolicy.java
    private DomainEvents pendingEvents; // every event emitted since open, in order
    private Money total = Money.zero(CurrencyUnit.of("PLN"));
    private final Map<ItemType, Integer> itemsPerType = new HashMap<>();

    private Order(OrderId id, DomainEvents domainEvents) {
        this.id = id;
        this.pendingEvents = domainEvents;
    }

    public static Order open(OrderId id) {
        return new Order(id, DomainEvents.of(new OrderEvent.OrderOpened(id)));
    }

    public DomainEvents add(Item item) {
        if (total.plus(item.price()).isGreaterThan(ORDER_TOTAL_LIMIT)) {
            throw new OrderTotalLimitExceededException("Order total cannot exceed " + ORDER_TOTAL_LIMIT);
        }
        if (itemsPerType.getOrDefault(item.type(), 0) >= ITEMS_PER_TYPE_LIMIT) {
            throw new ItemTypeLimitExceededException("Order cannot hold more than " + ITEMS_PER_TYPE_LIMIT + " items of type " + item.type().name());
        }
        var itemAdded = new OrderEvent.ItemAdded(id, item);
package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary;

public record ReturnToCirculationRequest(String technicianId) {
}
src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance:
FleetMaintenanceArchitectureTest.java
domain

src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain:
bike

src/test/java/pl/wojtyna/dddtrainingv3/velocity/fleetmaintenance/domain/bike:
BikeTest.java
NotifySupportOfFixedBikePolicyTest.java
ReturnFixedBikeToCirculationPolicyTest.java
```

</details>

First, `Version` and `rebuild` in `Order`, plus the shared `Version` and `OptimisticLockingException` types:

<details><summary>🔧 Bash: Add Version, rebuild and exhaustive apply to Order</summary>

```bash
C=src/main/java/pl/wojtyna/dddtrainingv3/common/domain && F=src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/domain/order/Order.java
cat > $C/Version.java <<'EOF'
package pl.wojtyna.dddtrainingv3.common.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/// Version of an aggregate as it was loaded. Repositories compare it on save to detect concurrent modification.
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
EOF
cat > $C/OptimisticLockingException.java <<'EOF'
package pl.wojtyna.dddtrainingv3.common.domain;

/// Thrown by repositories when an aggregate was modified by someone else after it was loaded.
public class OptimisticLockingException extends RuntimeException {

    public OptimisticLockingException(String message) {
        super(message);
    }
}
EOF
python3 - "$F" <<'EOF'
import sys
p=sys.argv[1]; s=open(p).read()
reps=[
("import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;\n",
 "import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;\nimport pl.wojtyna.dddtrainingv3.common.domain.Version;\n"),
("import java.util.HashMap;\nimport java.util.Map;\n","import java.util.HashMap;\nimport java.util.List;\nimport java.util.Map;\n"),
("    private final OrderId id;\n","    private final OrderId id;\n    private final Version version; // the version this order was opened or loaded at; commands never change it\n"),
("""    private Order(OrderId id, DomainEvents domainEvents) {
        this.id = id;
        this.pendingEvents = domainEvents;
    }

    public static Order open(OrderId id) {
        return new Order(id, DomainEvents.of(new OrderEvent.OrderOpened(id)));
    }
""","""    private Order(OrderId id, Version version, DomainEvents domainEvents) {
        this.id = id;
        this.version = version;
        this.pendingEvents = domainEvents;
    }

    public static Order open(OrderId id) {
        return new Order(id, Version.initial(), DomainEvents.of(new OrderEvent.OrderOpened(id)));
    }

    /// Restores an order from its stored history. The history is already stored, so it is not pending.
    public static Order rebuild(OrderId id, List<OrderEvent> history) {
        var order = new Order(id, Version.initial().plus(history.size()), DomainEvents.empty());
        history.forEach(order::apply);
        return order;
    }
"""),
("""    private void apply(OrderEvent.ItemAdded event) {
        total = total.plus(event.item().price());
        itemsPerType.merge(event.item().type(), 1, Integer::sum);
    }
""","""    private void apply(OrderEvent event) {
        switch (event) {
            case OrderEvent.OrderOpened opened -> {
            }
            case OrderEvent.ItemAdded added -> {
                total = total.plus(added.item().price());
                itemsPerType.merge(added.item().type(), 1, Integer::sum);
            }
        }
    }
"""),
]
for a,b in reps:
    assert s.count(a)==1,a[:50]; s=s.replace(a,b)
s=s.rstrip()
assert s.endswith("}")
s=s[:-1].rstrip()+"\n\n    public Version version() {\n        return version;\n    }\n}\n"
open(p,'w').write(s)
EOF
./mvnw -q test -Dtest=OrderTest 2>&1 | grep -E "Tests run:|FAIL|ERROR" | head -3; echo ok
```

**Result:**

```
ok
```

</details>

The domain tests still pass with `Version`, `rebuild` and the exhaustive `apply`. Next: the repository, the in-memory adapter, the use case, the Spring wiring and the REST adapter.

<details><summary>🔧 Bash: Add repository, adapter, use case, REST, config and tests, then run the full suite</summary>

```bash
B=src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering && TB=src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering && P=pl.wojtyna.dddtrainingv3.orders.ordering
mkdir -p $B/api/usecases $B/infra/adapters/primary $B/infra/adapters/secondary $B/infra/config $TB/infra/adapters/secondary $TB/api/usecases
cat > $B/domain/order/OrderRepository.java <<EOF
package $P.domain.order;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;

import java.util.Optional;

@Repository
@SecondaryPort
public interface OrderRepository {

    Optional<Order> load(OrderId id);

    /// Appends {@code newEvents}, the result of the last command (it holds all pending events).
    ///
    /// @throws OptimisticLockingException unless the stored stream is still at {@code order.version()}
    void save(Order order, DomainEvents newEvents);
}
EOF
cat > $B/infra/adapters/secondary/InMemoryOrderRepository.java <<EOF
package $P.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import $P.domain.order.Order;
import $P.domain.order.OrderEvent;
import $P.domain.order.OrderId;
import $P.domain.order.OrderRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Event store in memory: one event stream per order.
@SecondaryAdapter
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, List<OrderEvent>> streams = new ConcurrentHashMap<>();

    @Override
    public Optional<Order> load(OrderId id) {
        return Optional.ofNullable(streams.get(id)).map(history -> Order.rebuild(id, List.copyOf(history)));
    }

    @Override
    public synchronized void save(Order order, DomainEvents newEvents) {
        var stream = streams.computeIfAbsent(order.id(), id -> new ArrayList<>());
        if (stream.size() != order.version().value()) {
            throw new OptimisticLockingException("Order " + order.id().value() + " was modified concurrently");
        }
        newEvents.stream().map(OrderEvent.class::cast).forEach(stream::add);
    }
}
EOF
cat > $B/api/usecases/AddItemToOrder.java <<EOF
package $P.api.usecases;

import java.math.BigDecimal;

/// Canvas command *Add item to order*. The price is in PLN.
public record AddItemToOrder(String orderId, String itemType, BigDecimal pricePln) {
}
EOF
cat > $B/api/usecases/AddItemToOrderUseCase.java <<EOF
package $P.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import $P.domain.order.Item;
import $P.domain.order.ItemType;
import $P.domain.order.Order;
import $P.domain.order.OrderId;
import $P.domain.order.OrderRepository;

/// Adds an item to an order. An order that doesn't exist yet is opened by its first item.
@PrimaryPort
public class AddItemToOrderUseCase {

    private static final CurrencyUnit PLN = CurrencyUnit.of("PLN");

    private final OrderRepository orders;
    private final DomainEventPublisher eventPublisher;

    public AddItemToOrderUseCase(OrderRepository orders, DomainEventPublisher eventPublisher) {
        this.orders = orders;
        this.eventPublisher = eventPublisher;
    }

    public void handle(AddItemToOrder command) {
        var id = new OrderId(command.orderId());
        var order = orders.load(id).orElseGet(() -> Order.open(id));
        var events = order.add(new Item(new ItemType(command.itemType()), Money.of(PLN, command.pricePln())));
        orders.save(order, events);
        eventPublisher.publish(events);
    }
}
EOF
cat > $B/infra/adapters/primary/AddItemRequest.java <<EOF
package $P.infra.adapters.primary;

import java.math.BigDecimal;

public record AddItemRequest(String itemType, BigDecimal pricePln) {
}
EOF
cat > $B/infra/adapters/primary/OrderController.java <<EOF
package $P.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import $P.api.usecases.AddItemToOrder;
import $P.api.usecases.AddItemToOrderUseCase;
import $P.domain.order.ItemTypeLimitExceededException;
import $P.domain.order.OrderTotalLimitExceededException;

/// REST API for the canvas command *Add item to order*.
@PrimaryAdapter
@RestController
@RequestMapping("/ordering/orders/{orderId}")
public class OrderController {

    private final AddItemToOrderUseCase addItemToOrder;

    public OrderController(AddItemToOrderUseCase addItemToOrder) {
        this.addItemToOrder = addItemToOrder;
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void addItem(@PathVariable("orderId") String orderId, @RequestBody AddItemRequest request) {
        addItemToOrder.handle(new AddItemToOrder(orderId, request.itemType(), request.pricePln()));
    }

    @ExceptionHandler({OrderTotalLimitExceededException.class, ItemTypeLimitExceededException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public String limitExceeded(RuntimeException exception) {
        return exception.getMessage();
    }

    @ExceptionHandler(OptimisticLockingException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String concurrentModification(OptimisticLockingException exception) {
        return exception.getMessage();
    }
}
EOF
cat > $B/infra/config/OrderingContextConfig.java <<EOF
package $P.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import $P.api.usecases.AddItemToOrderUseCase;
import $P.domain.order.OrderRepository;
import $P.infra.adapters.secondary.InMemoryOrderRepository;

@Configuration
public class OrderingContextConfig {

    @Bean
    public OrderRepository orderingOrderRepository() {
        return new InMemoryOrderRepository();
    }

    @Bean
    public AddItemToOrderUseCase orderingAddItemToOrderUseCase(OrderRepository orderingOrderRepository,
                                                               DomainEventPublisher eventPublisher) {
        return new AddItemToOrderUseCase(orderingOrderRepository, eventPublisher);
    }
}
EOF
cat > $TB/infra/adapters/secondary/InMemoryOrderRepositoryTest.java <<EOF
package $P.infra.adapters.secondary;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import $P.domain.order.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("In-memory order repository")
class InMemoryOrderRepositoryTest {

    private static final OrderId ORDER_ID = new OrderId("order01");
    private static final CurrencyUnit PLN = CurrencyUnit.of("PLN");

    private final InMemoryOrderRepository repository = new InMemoryOrderRepository();

    // @formatter:off
    @DisplayName(
        """
         given an order saved with 3 Helmets,
         when it is loaded,
         then it is at version 4 and still rejects a 4th Helmet
        """
    )
    // @formatter:on
    @Test
    void loadedOrderKeepsItsState() {
        // given
        var helmet = new Item(new ItemType("Helmet"), Money.of(PLN, 10));
        var order = Order.open(ORDER_ID);
        order.add(helmet);
        order.add(helmet);
        repository.save(order, order.add(helmet));

        // when
        var loaded = repository.load(ORDER_ID).orElseThrow();

        // then
        assertThat(loaded.version()).isEqualTo(new Version(4));
        assertThat(catchThrowable(() -> loaded.add(helmet))).isInstanceOf(ItemTypeLimitExceededException.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given the same saved order loaded twice and an item added to each copy,
         when both copies are saved,
         then the second save is rejected
        """
    )
    // @formatter:on
    @Test
    void secondConcurrentSaveIsRejected() {
        // given
        var order = Order.open(ORDER_ID);
        repository.save(order, order.add(new Item(new ItemType("Helmet"), Money.of(PLN, 10))));
        var first = repository.load(ORDER_ID).orElseThrow();
        var second = repository.load(ORDER_ID).orElseThrow();
        var firstEvents = first.add(new Item(new ItemType("Lock"), Money.of(PLN, 20)));
        var secondEvents = second.add(new Item(new ItemType("Light"), Money.of(PLN, 15)));
        repository.save(first, firstEvents);

        // when
        var exception = catchThrowable(() -> repository.save(second, secondEvents));

        // then
        assertThat(exception).isInstanceOf(OptimisticLockingException.class);
    }
}
EOF
cat > $TB/api/usecases/AddItemToOrderUseCaseTest.java <<EOF
package $P.api.usecases;

import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import $P.domain.order.Item;
import $P.domain.order.ItemType;
import $P.domain.order.OrderEvent.ItemAdded;
import $P.domain.order.OrderEvent.OrderOpened;
import $P.domain.order.OrderId;
import $P.infra.adapters.secondary.InMemoryOrderRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Add item to order")
class AddItemToOrderUseCaseTest {

    private final List<DomainEvent> published = new ArrayList<>();
    private final DomainEventPublisher publisher = new DomainEventPublisher() {
        @Override
        public void publish(DomainEvents events) {
            events.stream().forEach(published::add);
        }
    };
    private final InMemoryOrderRepository orders = new InMemoryOrderRepository();
    private final AddItemToOrderUseCase useCase = new AddItemToOrderUseCase(orders, publisher);

    // @formatter:off
    @DisplayName(
        """
         given no order "order01",
         when a Helmet for 10 PLN is added to it,
         then the order is opened with the Helmet, stored and the events are published
        """
    )
    // @formatter:on
    @Test
    void firstItemOpensTheOrder() {
        // when
        useCase.handle(new AddItemToOrder("order01", "Helmet", BigDecimal.TEN));

        // then
        var id = new OrderId("order01");
        var helmet = new Item(new ItemType("Helmet"), Money.of(CurrencyUnit.of("PLN"), 10));
        assertThat(published).containsExactly(new OrderOpened(id), new ItemAdded(id, helmet));
        assertThat(orders.load(id)).isPresent();
    }
}
EOF
grep -n "interface\|void publish" src/main/java/pl/wojtyna/dddtrainingv3/common/domain/DomainEventPublisher.java
./mvnw test 2>&1 | grep -E "Tests run:|FAIL|ERROR\]|BUILD" | tail -6
```

**Result:**

```
6:public interface DomainEventPublisher {
8:    void publish(DomainEvents events);
10:    void publish(DomainEvent event);
[ERROR] 
[ERROR] To see the full stack trace of the errors, re-run Maven with the -e switch.
[ERROR] Re-run Maven using the -X switch to enable full debug logging.
[ERROR] 
[ERROR] For more information about the errors and possible solutions, please read the following articles:
[ERROR] [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
```

</details>

<details><summary>🔧 Bash: Show compile errors</summary>

```bash
./mvnw -q test-compile 2>&1 | grep -E "ERROR.*\.java" | head -5
```

**Result:**

```
[ERROR] src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/api/usecases/AddItemToOrderUseCaseTest.java:[27,79] <anonymous pl.wojtyna.dddtrainingv3.orders.ordering.api.usecases.AddItemToOrderUseCaseTest$1> is not abstract and does not override abstract method publish(pl.wojtyna.dddtrainingv3.common.domain.DomainEvent) in pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher
[ERROR] src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/api/usecases/AddItemToOrderUseCaseTest.java:[27,79] <anonymous pl.wojtyna.dddtrainingv3.orders.ordering.api.usecases.AddItemToOrderUseCaseTest$1> is not abstract and does not override abstract method publish(pl.wojtyna.dddtrainingv3.common.domain.DomainEvent) in pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher
```

</details>

<details><summary>🔧 Edit `src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/api/usecases/AddItemToOrderUseCaseTest.java`</summary>

```
events.stream().forEach(published::add);
        }
    };
```

replaced with

```
events.stream().forEach(published::add);
        }

        @Override
        public void publish(DomainEvent event) {
            published.add(event);
        }
    };
```

**Result:**

```
The file src/test/java/pl/wojtyna/dddtrainingv3/orders/ordering/api/usecases/AddItemToOrderUseCaseTest.java has been updated successfully.
```

</details>

<details><summary>🔧 Bash: Run the full suite after the fix</summary>

```bash
./mvnw test 2>&1 | grep -E "Tests run:|FAIL|ERROR\]|BUILD" | grep -v "Tests run: [0-9]*, Failures: 0, Errors: 0, Skipped: 0, Time" | tail -6
```

**Result:**

```
[INFO] Tests run: 26, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

</details>

All 26 tests pass. To finish, I'm updating the context's javadoc, the plan and `orders/CLAUDE.md` so they describe what's actually built:

<details><summary>🔧 Bash: Update javadoc, plan and system CLAUDE.md, rerun suite</summary>

```bash
python3 - <<'EOF'
p='src/main/java/pl/wojtyna/dddtrainingv3/orders/ordering/package-info.java'; s=open(p).read()
old=s[s.index("/// ## Business decisions"):s.index("/// Source canvas")]
new='''/// ## Business decisions
/// - **Order total cannot exceed 100 PLN**: given an empty order, when an item for 100 PLN is added, then the item is
///   added (see `OrderTest#itemForExactly100PlnFitsIntoEmptyOrder`); given an order with items totalling 90 PLN, when
///   an item for 20 PLN is added, then adding the item is rejected (see `OrderTest#itemPushingTotalAbove100PlnIsRejected`).
///   Enforced by `Order`.
/// - **No more than 3 items of the same type**: given an order with 2 "Helmet" items, when another "Helmet" is added,
///   then the item is added (see `OrderTest#thirdItemOfTheSameTypeIsAdded`); given an order with 3 "Helmet" items, when
///   another "Helmet" is added, then adding the item is rejected (see `OrderTest#fourthItemOfTheSameTypeIsRejected`).
///   Enforced by `Order`.
///
'''
s=s.replace(old,new); open(p,'w').write(s)

p='docs/bounded-contexts/orders/ordering/plan.md'; s=open(p).read()
for a,b in [("| `OrderTest`, cycle 1 |","| `OrderTest#itemForExactly100PlnFitsIntoEmptyOrder` |"),
            ("| `OrderTest`, cycle 2 |","| `OrderTest#itemPushingTotalAbove100PlnIsRejected` |"),
            ("| `OrderTest`, cycle 3 |","| `OrderTest#thirdItemOfTheSameTypeIsAdded` |"),
            ("| `OrderTest`, cycle 4 |","| `OrderTest#fourthItemOfTheSameTypeIsRejected` |")]:
    assert s.count(a)==1; s=s.replace(a,b)
s=s.rstrip()+'''

### 2026-11-04: Order aggregate and persistence
- Added (with `ddd-tactical-building-blocks`, one scenario per cycle): `domain/order/` `Order` (event-sourced), `OrderId`, `Item`, `ItemType`, sealed `OrderEvent` (`OrderOpened`, `ItemAdded`), `OrderTotalLimitExceededException`, `ItemTypeLimitExceededException`; `OrderTest` (5 tests, including `everyCommandReturnsAllPendingEvents`).
  - Cycle log: R1a: red, implemented by the user, green. R1b: red, scaffolded by Claude and confirmed, then a refactor so pending events build up across commands (new test, red, then green). R2a: green from the start, kept. R2b: red, scaffolded by Claude and confirmed.
- Added after the domain was green (D4): `Version` and `OptimisticLockingException` (top-level `common/domain`), `Order.rebuild`/`version()`, `OrderRepository`, `InMemoryOrderRepository` (+ test), `AddItemToOrderUseCase` + `AddItemToOrder` (+ test), `OrderController` + `AddItemRequest` (`POST /ordering/orders/{orderId}/items`; 422 when a limit is exceeded, 409 on concurrent modification), `OrderingContextConfig`.
- Changed (by the user): `common/domain/DomainEvents#append`.
- Decision: an order that doesn't exist yet is opened by its first item (no separate "open order" command in the canvas).
- Full suite: 26 tests, 0 failures.
'''
s=s.replace("- Build the `Order` aggregate with `ddd-tactical-building-blocks` (D3).\n- After that: `AddItemToOrderUseCase`, an in-memory repository, `infra/config/OrderingContextConfig` and the REST adapter `OrderController` (Q2).\n",
            "- Done: the `Order` aggregate (D3), then `AddItemToOrderUseCase`, the in-memory repository, `OrderingContextConfig` and `OrderController` (D4, Q2). See the changelog.\n")
open(p,'w').write(s+"\n")

p='src/main/java/pl/wojtyna/dddtrainingv3/orders/CLAUDE.md'; s=open(p).read()
s=s.replace("It is built one Given/When/Then scenario at a time. `Version`, the repository, the `AddItemToOrder` use case and the REST adapter come only after all domain tests are green.",
"It was built one Given/When/Then scenario at a time (`OrderTest`). Commands return all pending events since `open`/`rebuild`. Persistence is an in-memory event store (`InMemoryOrderRepository`) with optimistic locking through `Version`. Entry point: `AddItemToOrderUseCase`, exposed as `POST /ordering/orders/{orderId}/items`; the first item opens the order.")
open(p,'w').write(s)
EOF
./mvnw -q test 2>&1 | grep -E "FAIL|ERROR" | head -3; git status --short
```

**Result:**

```
M src/main/java/pl/wojtyna/dddtrainingv3/common/domain/DomainEvents.java
?? docs/bounded-contexts/orders/
?? src/main/java/pl/wojtyna/dddtrainingv3/common/domain/OptimisticLockingException.java
?? src/main/java/pl/wojtyna/dddtrainingv3/common/domain/Version.java
?? src/main/java/pl/wojtyna/dddtrainingv3/orders/
?? src/test/java/pl/wojtyna/dddtrainingv3/orders/
```

</details>

The `Order` aggregate is finished, and both rules are enforced: the order total can't exceed 100 PLN, and an order can't hold more than 3 items of the same type. The ordering app now accepts items over REST at `POST /ordering/orders/{orderId}/items`, and all 26 tests pass. Nothing is committed.

**How the 4 scenarios went:**

| Scenario | Who implemented it |
|---|---|
| 1a: 100 PLN accepted | You. You also added `DomainEvents.append` to the shared `common` package |
| 1b: 20 PLN on top of 90 PLN rejected | Me, confirmed by you. Then the refactor you picked: a second `add` now returns every event since the order was opened, with its own test |
| 2a: 3rd Helmet accepted | Passed on its first run; you kept the test |
| 2b: 4th Helmet rejected | Me, confirmed by you |

**After the domain was green:**
- **`Version`, `rebuild` and the repository:** `Order` got `Version` (optimistic locking) and `rebuild(history)`. There's an `OrderRepository` port and an in-memory implementation, tested for reloading a saved order and for rejecting a second concurrent save.
- **Use case:** `AddItemToOrderUseCase`, with a test. An order that doesn't exist yet is opened by its first item, because the canvas has no separate "open order" command.
- **REST:** `OrderController` returns 422 when a limit is exceeded and 409 when someone else saved the order first. I haven't called it over HTTP, only through the use case test and the Spring startup in the suite.
- **Docs:** the context's javadoc, the plan and `orders/CLAUDE.md` now describe what's built, and the plan has a log of each cycle.

**How the skill did on its first real run:** the stops worked as you wanted. Every test was shown before any code, and I waited for your choice each time. Two things you might want changed in the skill:
- **Fakes vs. real code.** When I scaffolded, I wrote the real rule both times rather than a fake that just passes the test, such as "reject a 20 PLN item". The skill's "fake it first" rule allows those fakes, but here they would have looked silly. If you want those fakes on purpose, the skill should say so more firmly. If not, I'd loosen that rule.
- **No stop before the persistence work.** The skill had me build everything after the last scenario in one go, with no checkpoint. Since you want control, a single checkpoint before that work might fit better.

**Branch:** your checkout is still on `ddd-tactical-tdd-checkpoints`, the branch of PR #10, so all of this sits uncommitted on top of the skill change. I'd commit it on a separate branch so it stays out of that PR. Should I do that?
