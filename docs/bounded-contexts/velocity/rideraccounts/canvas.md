# Bounded Context Canvas: RiderAccounts

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Required: Name, Purpose, Ubiquitous Language. Everything else makes the generated module richer.
Business decisions become code and tests only if you write them as strict Given/When/Then examples with concrete values. ddd-tactical-building-blocks then decides whether each one needs an aggregate, a domain service, a policy or a factory.

PRE-FILLED by discover-bounded-contexts (canvas only, no plan, no code) on 2026-11-05 from
docs/context-discovery/velocity/2026-11-05-bike-rental-processes/discovery.md and
src/main/cml/VeloCity-Context-Map-Discovery.cml. Expert quotes are verbatim. Review before scaffolding.
-->

## Name
RiderAccounts

<!-- Matches `BoundedContext RiderAccounts` in src/main/cml/VeloCity-Context-Map-Discovery.cml. -->

## Purpose
Lets people become riders and decides whether a rider may use the service at any moment. Owns the whole account lifecycle: registration, activation, account status, and blocking or lifting a block for any reason a client supplies.

Key question (from discovery): **Can this rider use the service?**

## Strategic Classification
- Domain: <to decide: not discussed in discovery>
- Business model: <to decide>
- Evolution: <to decide>

## Domain Roles
<!-- Pick traits from docs/model-traits-worksheet.md. Not discussed in discovery. -->
- <to decide>

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Rider app | Command | Register / confirm email / verify phone / provide basic data / sign agreement |  |
| Payments | Event | Payment Method Verified / Payment Method Not Verified | drives activation and the invalid-card block |
| Rental | Query | Can this rider rent? | before each rental |
| Settlement | Command | Block account (reason: debt) / Lift block | generic blocking; the reason comes from the client |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Verify payment method | Command | Payments | yes |
| User Account Activated | Event | (published) | n/a for events |
| Account Blocked / Account Block Lifted | Event | (published) | n/a for events |
| Account Block Notification Sent | Event | rider | n/a |

## Ubiquitous Language
- **Activated**: "User activated is when user account is validated using payment for the first time."
- **Block**: A generic capability that stops a rider from using the service. "Blocking is a generic capability, it doesn't understand the context on it's owne. The reason is only passed from clients."
- **Block reason**: Supplied by the client that asks for the block, e.g. debt.

## Business Decisions
<!-- Rules marked "Needs GWT" or with "?" need concrete values before scaffolding. -->
### Activation after first payment validation
- **Given** an account whose payment method has just been validated by a payment for the first time
- **When** Payment Method Verified arrives
- **Then** User Account Activated is recorded

### Invalid-card block
- Needs GWT: "The Rider Accounts by itself if credit card wasn't validated by payment." (needs concrete GWT: deadline? which event?)

## Assumptions
- Boundaries and messages come from the 2026-11-05 discovery session; see the report for the expert's verbatim answers.

## Verification Metrics
- <to decide>

## Open Questions
- Can an invalid-card block also happen after activation (expired card, declined charge)?
- What lifts an invalid-card block?
- Ban vs block: is there a permanent ban?
