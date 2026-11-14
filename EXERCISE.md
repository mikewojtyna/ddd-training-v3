# Exercise 16: Tactical building blocks in a cinema

**Module:** M4 Implementing a module with AI · **Time:** 30 min · **Form:** pairs · **Start:** tag `ex16/start`

## Situation

A small cinema wants online ticketing. Customers reserve seats for a screening, pay a price that depends on the day
and on their discount, get a numbered ticket and show it at the entrance. The bounded context exists, but it's empty:
`src/main/java/pl/wojtyna/dddtrainingv3/cinema/ticketing/` has only `package-info.java` (read its ubiquitous
language first) and an architecture test.

You'll build **one of each tactical building block**, each in its own small run of the `ddd-tactical-building-blocks`
skill.

## Your task

For each task below, give the agent the rule **in business words** and the scenarios from the table, e.g.:

> *In cinema Ticketing: a seat number is a row A–J and a seat 1–12. Given "A7", when a seat number is made from it,
> then it is row A, seat 7. Given "K3", when …, then it is rejected: there is no row K.*

Don't name the building block in the prompt. The skill classifies every rule before it writes code, so **check its
classification against the "Block" column**. If it differs, ask why before you go on.

Work in the TDD checkpoint loop (the skill's default): one scenario per cycle, and at every checkpoint you decide who
writes the implementation. To fit 30 minutes, let the agent scaffold most cycles, but **write at least one
implementation yourselves** (T3 or T4 are good candidates).

**Core (about 20 min): T1–T5.** **If you have time: T6–T8**, which chain into each other.

| # | Block | Rule | Scenarios (Given / When / Then) |
|---|---|---|---|
| T1 | Value object | A seat number is a row A–J and a seat 1–12. | "A7" → row A, seat 7 · "K3" → rejected (no row K) · two seat numbers A7 are equal |
| T2 | State-based aggregate: root, entity, value object | (R1) A seat is reserved only once. (R2) A reservation never leaves empty seats between reserved seats in a row. | Screening S1 with A5 reserved: Bob reserves A5 → rejected (already reserved) · Ann reserves A6 → `SeatReserved` · Ann reserves A7 → rejected (A6 would stay empty between A5 and A7) · Ann reserves B7 → `SeatReserved` (another row) · **A9 → ? You decide** (see below) |
| T3 | Domain service | A ticket costs 25 PLN on weekdays and 30 PLN at weekends. | screening on Wed 2026-11-18 → 25 PLN · screening on Sat 2026-11-21 → 30 PLN |
| T4 | Policy as a strategy (a function) | Students pay 20% less. Marketing will add more discounts soon. | Sat screening, student → 24 PLN · Sat screening, no discount → 30 PLN |
| T5 | Event-sourced aggregate | A ticket is scanned at the entrance only once. | ticket T-1 issued, scanned → `TicketScanned` · T-1 already scanned, scanned again → rejected (already used) |
| T6 | Factory | A new ticket gets the next number from the cinema's ticket sequence. | sequence at 41, a ticket is issued for S1 / A7 → `TicketIssued` with number T-42 |
| T7 | Repository | Tickets are stored and loaded back; a stale write is refused. | T-42 saved after its scan → loaded T-42 can't be scanned again · two copies of T-42 scanned and saved → the second save fails (optimistic locking) |
| T8 | Policy reacting to an event | Whenever a seat is reserved, a ticket is issued for it. | `SeatReserved` (S1, A7, Ann) is handled → a ticket for S1 / A7 is stored and `TicketIssued` is published |

**T2, the A9 question.** The rule as worded says "no empty seats between reserved seats". With A5 reserved, does
reserving A9 break it (three empty seats between)? Ask the domain expert (the trainer), then give the agent that
scenario too. Watch whether the agent asks you or guesses.

Stop the agent when you see:
- `Screening` holding a `Ticket`, or `Ticket` holding a `Screening`: they reference each other by id only;
- the student discount as an `if (student)` flag inside the price calculator;
- an aggregate publishing its events instead of returning them;
- the repository built before the aggregate's scenarios are green.

## Done when

- Every scenario you got to has its own test, and each test was red before it went green.
- `./mvnw test` is green.
- `ticketing/package-info.java`'s "Business decisions" lists each rule with its building block and test.

## Debrief questions

- What did you decide for A9? Did the agent ask, or did it assume something?
- Where did the agent's classification differ from the "Block" column, and who was right?
- Strategy policy, domain service, or an `if`: what changes in each when marketing adds "Tuesday: half price"?
- Why are `Screening` and `Ticket` two aggregates? What does that make T8 (consistent immediately, or eventually)?
- Event-sourced `Ticket` vs state-based `Screening`: how do their tests and repositories differ?
- Compare with the trainer's run, one commit per task: `git log --oneline ex16/start..ex16/solution`.

## Rules

- The decision at each checkpoint is yours.
- No Lombok (see `CLAUDE.md`). Domain classes aren't Spring beans; no use cases or REST are needed here.
