# Cinema system

Cinema is a tiny practice system for exercise 16: one of each DDD tactical building block, built with the
`ddd-tactical-building-blocks` skill. The repo-root `CLAUDE.md` covers the package conventions; this file adds
Cinema-specific context.

## Sources of truth

- `EXERCISE.md` lists the rules and their Given/When/Then scenarios.
- `ticketing/package-info.java` holds the ubiquitous language and, once built, the business decisions.
- There is no vision document, canvas, context map or CML model for this system.

## Bounded contexts

- **Ticketing** (`ticketing`): customers reserve seats for screenings, tickets are priced, issued and scanned at the
  entrance. It starts empty: only `package-info.java` and `TicketingArchitectureTest`.

## Conventions for this system

- Money is Joda Money in PLN (`org.joda.money.Money`), as elsewhere in the repo.
- One domain package per aggregate or area, e.g. `ticketing/domain/screening`, `ticketing/domain/pricing`,
  `ticketing/domain/ticket`.
- No use cases, REST or Spring wiring are needed for the exercise: the domain and its tests are the point.
