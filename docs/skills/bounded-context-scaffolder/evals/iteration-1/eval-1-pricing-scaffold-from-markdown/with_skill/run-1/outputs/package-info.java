/// # Pricing bounded context
///
/// ## Purpose
/// Turns a finished ride into a fair, predictable fee. Riders are encouraged to take short trips (free period), long
/// trips get progressively more expensive, and the rider always pays the price list that was valid when the ride
/// started.
///
/// ## Strategic classification
/// - Domain: core
/// - Business model: revenue
/// - Evolution: custom built
///
/// ## Domain roles
/// - Execution Model: calculates the total fee for every rental session.
///
/// ## Ubiquitous language
/// - **Price List**: set of fees (free period length, first/second/third hour fee, subsequent hourly fee) valid from
///   a given moment.
/// - **Free Period**: the initial minutes of a rental session during which no fee is applied.
/// - **Billing Period**: one started hour of a ride after the free period.
/// - **Total Fee**: sum of billing period fees plus return charges.
/// - **Return Charges**: drop-zone or unauthorized-zone charges reported by Rentals.
///
/// ## Business decisions
/// - Free ride: with a 10 minute free period, a ride of 8 minutes costs 0.00 PLN.
/// - First hour: with a 10 minute free period and first-hour fee 5.00 PLN, a ride of 45 minutes costs 5.00 PLN.
/// - Tiered hours: with a 10 minute free period and hour fees 5.00 / 7.00 / 9.00 / 12.00 (subsequent) PLN, a ride of
///   3 hours 20 minutes costs 33.00 PLN.
/// - Price valid at ride start: with price list A (first hour 5.00 PLN) valid from 08:00 and price list B (first hour
///   6.00 PLN) valid from 09:00, a ride from 08:50 to 09:30 costs 5.00 PLN.
///
/// ## Assumptions
/// - Return charges are always known by the time the session ends.
///
/// ## Verification metrics
/// - Number of pricing disputes raised in Customer Support per 1000 rides.
///
/// ## Open questions
/// - Does the maximum-rental-time penalty belong to Pricing or Rentals?
///
/// Source canvas: `docs/bounded-contexts/velocity/pricing/canvas.md`
@BoundedContext(name = "Pricing")
@Module
package pl.wojtyna.dddtrainingv3.velocity.pricing;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
