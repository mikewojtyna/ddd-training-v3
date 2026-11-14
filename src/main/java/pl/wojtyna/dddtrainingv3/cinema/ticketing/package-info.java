/// # Ticketing bounded context
///
/// ## Purpose
/// Lets customers reserve seats for a screening, prices their tickets and lets ushers scan tickets at the entrance.
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
/// - **Screening**: one showing of a film in a hall at a given date and time, e.g. S1. Customers reserve its seats.
/// - **Seat number**: a row letter and a seat number in that row, e.g. A7. Halls have rows A–J and seats 1–12.
/// - **Seat**: a place in the hall for one screening; it is either free or reserved for a customer.
/// - **Ticket**: the proof of a reservation, numbered T-1, T-2, … It is issued for a screening and a seat and scanned
///   once at the entrance.
/// - **Ticket price**: what a ticket costs in PLN, depending on the screening's day.
/// - **Discount**: a reduction of the ticket price for some customers, e.g. students. Marketing adds new ones often.
///
/// ## Business decisions
/// - **A seat number is a row A–J and a seat** (value object `SeatNumber`): given "A7", when a seat number is made from
///   it, then it is row A, seat 7 (`SeatNumberTest#seatNumberIsARowAndASeat`); given "K3", then it is rejected
///   (`SeatNumberTest#rowOutsideTheHallIsRejected`); two seat numbers A7 are equal
///   (`SeatNumberTest#seatNumbersAreEqualByValue`). Seats 1–12 are not checked yet: no scenario asked for it.
/// - **A seat is reserved only once** (state-based aggregate `Screening`, guarded by its `Seat` entity): given S1 with A5
///   reserved, when Bob reserves A5, then it is rejected (`ScreeningTest#seatIsReservedOnlyOnce`).
/// - **No empty seats between reserved seats in a row** (aggregate `Screening`): given S1 with A5 reserved, A6 is
///   reserved (`ScreeningTest#seatNextToAReservedOneCanBeReserved`), A7 is rejected
///   (`ScreeningTest#reservationCannotLeaveAnEmptySeatBetweenReservedSeats`), A9 is rejected too: reserved seats in a row
///   form one block (`ScreeningTest#reservationCannotLeaveSeveralEmptySeatsBetweenReservedSeats`), and B7 is reserved
///   (`ScreeningTest#gapRuleAppliesWithinARowOnly`).
/// - **A ticket costs 25 PLN on weekdays and 30 PLN at weekends** (domain service `TicketPriceCalculator`): Wednesday
///   2026-11-18 → 25 PLN (`TicketPriceCalculatorTest#weekdayTicketCosts25Pln`), Saturday 2026-11-21 → 30 PLN
///   (`TicketPriceCalculatorTest#weekendTicketCosts30Pln`).
/// - **Students pay 20% less; more discounts will follow** (policy as a strategy, `Discount`, passed into
///   `TicketPriceCalculator`): Saturday, no discount → 30 PLN (`TicketPriceCalculatorTest#customerWithoutDiscountPaysFullPrice`),
///   Saturday, student → 24 PLN (`TicketPriceCalculatorTest#studentPays20PercentLess`).
/// - **A ticket is scanned at the entrance only once** (event-sourced aggregate `Ticket`): given T-1 issued, when it is
///   scanned, then `TicketScanned` (`TicketTest#issuedTicketIsScanned`); given T-1 already scanned, then the second scan
///   is rejected (`TicketTest#ticketIsScannedOnlyOnce`).
/// - **A new ticket gets the next number of the cinema's ticket sequence** (factory `TicketFactory` with the
///   `TicketSequence` port): given the sequence at 41, when a ticket is issued for S1 / A7, then T-42 is issued
///   (`TicketFactoryTest#newTicketGetsTheNextNumberFromTheSequence`).
/// - **Tickets are stored as event streams; a stale write is refused** (repository port `TicketRepository`, adapter
///   `InMemoryTicketRepository`, `Version` on `Ticket`): a saved, scanned ticket loads back already scanned
///   (`InMemoryTicketRepositoryTest#savedTicketLoadsBackWithItsHistory`); of two copies scanned concurrently, the second
///   save fails (`InMemoryTicketRepositoryTest#staleWriteIsRefused`).
///
/// Source: `EXERCISE.md` (exercise 16). There is no canvas, context map or CML model for this system.
@BoundedContext(name = "Ticketing")
@Module
package pl.wojtyna.dddtrainingv3.cinema.ticketing;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
