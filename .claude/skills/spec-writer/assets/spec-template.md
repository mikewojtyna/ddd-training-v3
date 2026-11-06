---
name: rider-reserves-bike-at-station     # = the intent's name = file name without .md (1:1 with the intent)
intent: rider-reserves-bike-at-station   # docs/intents/<intent>.md
system: velocity                         # same as the intent
status: draft                            # draft | approved | superseded
date: 2026-11-06                         # YYYY-MM-DD the spec was written or last revised
---

# Spec: A rider wants to reserve a bike at a bike station

<!-- One line: what this spec makes concrete, and a link to the intent. Nothing else. -->
Concrete rules, examples and contract for [the intent](../intents/rider-reserves-bike-at-station.md).

## Vocabulary

<!-- Only the terms the rules and the contract rely on, in the domain's words (vision + intent).
     No bounded context owns them yet; the implementation plan translates them into each context's ubiquitous language. -->
| Term | Meaning |
|------|---------|
| Reservation | A rider's hold on one specific available bike at a station, for a limited reservation period. |
| Reservation period | How long a reservation holds the bike. Length: see Q1. |

## Rules

<!-- One "### R<n>: <rule in one sentence>" per business rule, the happy path first.
     Each rule has 1-3 examples with IDs E<n>.<k>, written with concrete values (named riders, bikes, stations, times, amounts).
     An unknown value is "?" plus a question reference, e.g. "(? minutes, Q1)". Never pick a number the user didn't give. -->

### R1: A rider can reserve an available bike at a station

- **E1.1** Given bike B-17 is available at station Central and rider Anna has no reservation, When Anna reserves B-17, Then B-17 is reserved for Anna (Bike Reserved).

### R2: A rider holds at most one active reservation

- **E2.1** Given Anna holds a reservation for B-17, When Anna reserves B-22 at Central, Then the reservation is rejected and B-22 stays available.

### R3: A reserved bike can't be reserved or rented by anyone else (rental intent)

- **E3.1** Given B-17 is reserved for Anna, When rider Ben reserves B-17, Then the reservation is rejected.
- **E3.2** Given B-17 is reserved for Anna, When Ben rents B-17, Then the rental is rejected.

## Contract

<!-- What the outside world can ask for and learn, in domain language. No transport, no URLs, no status codes.
     Every "On success" event and every intent Business-outcome event appears under Events.
     "Rejected when" lists rule IDs. -->

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Reserve Bike | Rider | rider, bike, station | Bike Reserved | R2, R3 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| Rider's Active Reservation | Rider | rider | bike, station, expires at (or none) |

### Events

| Event | When | Data |
|-------|------|------|
| Bike Reserved | a reservation is accepted | rider, bike, station, reserved at, expires at |

## Data

<!-- Business shapes only: field, business type, constraint. Types: Text, Number, Money, Duration, Timestamp, Date,
     Identifier (of what), Yes/No, one of {a, b}, list of <shape>. No tables, columns, JSON or classes. -->
| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Reservation | rider | Identifier (Rider) | required |
| Reservation | bike | Identifier (Bike) | required |
| Reservation | expires at | Timestamp | reserved at + reservation period (Q1) |

## Out of scope

<!-- Copied from the intent's Scope "Out", plus anything you deliberately left out. Point to neighbouring intents by name. -->
- Cancelling a reservation (`rider-cancels-reservation`, candidate).
- Releasing an expired reservation (`fleet-releases-expired-reservation`, candidate).

## Open questions

<!-- Q<n>: one question each, naming the rule or example it blocks. Contradictions with the vision or other specs go here too.
     Answered questions stay as struck-through decision records: - ~~**Q1** How long …?~~ 15 minutes (domain expert, 2026-11-06) -->
- **Q1** How long is the reservation period? (R1, Data) The vision only says "short-lived".

## Notes for the plan

<!-- Technical hints carried over from the intent's "Notes for the spec", plus any the user added.
     The only section where technical words belong. -->
- None.
