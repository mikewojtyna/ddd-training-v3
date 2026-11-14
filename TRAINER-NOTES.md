# Exercise 16: trainer notes

The reference run of T1–T8, one commit per task (`git log --oneline ex16/start..ex16/solution`). Every test was red
before it went green; the per-cycle log is below and in the commit messages. It's a reference run, so the checkpoint
loop's stops were skipped. The red/green discipline was kept.

## Expected classification and where it landed

| Task | Rule | Block | Code (`cinema/ticketing/...`) |
|---|---|---|---|
| T1 | a seat number is a row A–J and a seat | value object | `domain/screening/SeatNumber` (record, validated in the compact constructor) |
| T2 | a seat is reserved only once; no empty seats between reserved seats in a row | state-based aggregate | `domain/screening/Screening` (root), private `@Entity Seat`, `SeatNumber`, `ScreeningEvent.SeatReserved` |
| T3 | 25 PLN weekdays, 30 PLN weekends | domain service | `domain/pricing/TicketPriceCalculator` |
| T4 | students pay 20% less, more discounts coming | policy as a strategy | `domain/pricing/Discount` (`none()`, `student()`, closure `percent(n)`), passed into the calculator |
| T5 | a ticket is scanned only once | event-sourced aggregate | `domain/ticket/Ticket`, `TicketEvent` |
| T6 | next number from the cinema's sequence | factory | `domain/ticket/TicketFactory` + port `TicketSequence` |
| T7 | store, load, refuse stale writes | repository | `domain/ticket/TicketRepository` + `infra/adapters/secondary/InMemoryTicketRepository`, `Version` on `Ticket` |
| T8 | whenever a seat is reserved, issue a ticket | reacting policy | `domain/ticket/IssueTicketWhenSeatReservedPolicy` |

## Decisions in the reference run (good debrief material)

- **A9 is rejected.** "No empty seats between reserved seats" read literally: reserved seats in a row form one
  block. The implementation relies on that: a new seat must touch an already reserved seat in its row. If your
  "expert" decides that only a single orphan seat is forbidden (A9 allowed, A7 not), the scenario changes and so does
  the rule. That's exactly why the skill must not guess it.
- **Seats 1–12 are not checked.** The T1 scenarios only reject a row (K3). Strict TDD leaves the seat range unguarded.
  Ask: "what does `SeatNumber.of("A13")` do?" The fix is a scenario, not a silent extra `if`.
- **The `Seat` entity guards its own part** ("reserved only once"); the root guards the cross-seat rule (gaps in a
  row). Seats are created lazily when someone first reserves them, so `Screening` needs no hall layout.
- **Cycle 3 of T2 is a planted shortcut**: "touch any reserved seat" ignores rows; B7 (cycle 4) kills it. If
  participants' agents get the row right immediately, ask which scenario would have caught it if they hadn't.
- **The discount is chosen by the caller.** The calculator only applies the `Discount` it is given. An
  `if (student)` inside the calculator, or an enum of customer types, is the pitfall to name: every new campaign
  would reopen the service.
- **`Ticket` doesn't remember the customer.** No invariant reads it, so `TicketIssued` carries only number,
  screening and seat. `SeatReserved` carries Ann for whoever needs her (e.g. a notification read model).
- **`Version` and the repository came after the domain** (T7), as the skill demands, and only for `Ticket`.
  `Screening` has no repository: no task needed one. The skill would offer it by default for a new aggregate.
- **No use cases, REST or Spring wiring.** The policy isn't routed by an `@EventListener` yet. That's the extension
  for fast pairs: a `LocalEventsHandler` in `infra/event` and a `TicketingContextConfig`.

## Per-cycle log

| Task | Test | Red seen | Green |
|---|---|---|---|
| T1 | `SeatNumberTest#seatNumberIsARowAndASeat` | expected 'A' but was '?' | parse row and seat |
| T1 | `SeatNumberTest#rowOutsideTheHallIsRejected` | no exception | row check |
| T1 | `SeatNumberTest#seatNumbersAreEqualByValue` | green from the start (record) | – |
| T2 | `ScreeningTest#seatNextToAReservedOneCanBeReserved` | no events | return `SeatReserved` |
| T2 | `ScreeningTest#seatIsReservedOnlyOnce` | no exception | `Seat` entity |
| T2 | `ScreeningTest#reservationCannotLeaveAnEmptySeatBetweenReservedSeats` | no exception | shortcut: touch any reserved seat |
| T2 | `ScreeningTest#gapRuleAppliesWithinARowOnly` | B7 wrongly rejected | look at the seat's row only |
| T2 | `ScreeningTest#reservationCannotLeaveSeveralEmptySeatsBetweenReservedSeats` | green from the start | – |
| T3 | `TicketPriceCalculatorTest#weekdayTicketCosts25Pln` | zero instead of 25 | hard-coded 25 |
| T3 | `TicketPriceCalculatorTest#weekendTicketCosts30Pln` | 25 instead of 30 | weekend check |
| T4 | `TicketPriceCalculatorTest#customerWithoutDiscountPaysFullPrice` | green from the start (compile-only step added the parameter) | – |
| T4 | `TicketPriceCalculatorTest#studentPays20PercentLess` | 30 instead of 24 | `student()` = `percent(20)` |
| T5 | `TicketTest#issuedTicketIsScanned` | no events | emit/apply, all pending events |
| T5 | `TicketTest#ticketIsScannedOnlyOnce` | no exception | `scanned` flag in `apply` |
| T6 | `TicketFactoryTest#newTicketGetsTheNextNumberFromTheSequence` | no events | `"T-" + sequence.next()` |
| T7 | `InMemoryTicketRepositoryTest#savedTicketLoadsBackWithItsHistory` | nothing loaded | event streams |
| T7 | `InMemoryTicketRepositoryTest#staleWriteIsRefused` | no exception | stream size vs `version()` |
| T8 | `IssueTicketWhenSeatReservedPolicyTest#reservedSeatGetsATicket` | nothing stored | factory → save → publish |

## Timing

The 30 minutes assume the agent scaffolds most cycles. T2 is the longest task (5 cycles, ~7 min); T1, T3 and T4 take
2–4 min each. Most pairs finish the core (T1–T5) and one or two of T6–T8. Run the debrief on the core and show
T6–T8 from this branch if needed.

## Debrief answers in short

- *Tuesday half price*: strategy → one new variant `Discount.percent(50)` chosen by the caller, calculator untouched;
  domain service with an `if` → the service changes for every campaign; aggregate → wrong block, nothing to keep
  consistent.
- *Two aggregates*: the ticket's rule (scanned once) and the screening's rules (seats) share no state, and they change
  at different times by different people (customer vs usher). So T8 is eventually consistent: the reservation commits
  first, the ticket follows in its own transaction.
- *ES vs state-based tests*: ES tests can start from a history (`Ticket.rebuild(...)`) and assert the whole pending
  event list; state-based tests replay commands in the "given" and assert only the last command's events. The ES
  repository is append-only with a version per stream; a state-based one would need a snapshot (memento).
