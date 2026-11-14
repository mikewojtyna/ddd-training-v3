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
/// None yet. Add each rule here in its Given/When/Then form, with its building block and its test.
///
/// Source: `EXERCISE.md` (exercise 16). There is no canvas, context map or CML model for this system.
@BoundedContext(name = "Ticketing")
@Module
package pl.wojtyna.dddtrainingv3.cinema.ticketing;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
