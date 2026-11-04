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
Allows to prepare orders.

## Strategic Classification
- Domain: core
- Business model: revenue
- Evolution: custom built

## Domain Roles
<!-- Pick traits from docs/model-traits-worksheet.md -->
- Execution Model: don't know why and I don't need it right now

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| User directly calling it | Command | Add item to order | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| | | | |

## Ubiquitous Language
<!-- Proposed definitions. Confirm or correct them: terms become class names. -->
- **Order**: a customer's collection of items, whose total value and per-type quantities are capped.
- **Item**: one unit added to an order, with an item type and a price in PLN.
- **Item type**: the kind of product an item is, e.g. "Helmet". Items of the same type count towards the same limit.
- **Order total**: the sum of the prices of all items in the order.

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
-

## Verification Metrics
-

## Open Questions
-
