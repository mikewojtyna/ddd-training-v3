/// # Pricing bounded context
///
/// ## Purpose
/// Turns a finished ride into a fair, predictable fee. Riders are encouraged to take short trips (free period),
/// long trips get progressively more expensive, and the rider always pays the price list that was valid when the
/// ride started.
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
/// - **Free ride**: given a price list with a 10 minute free period, when a ride of 8 minutes is priced, then the
///   total fee is 0.00 PLN. Enforced by `PriceLists`, see `PriceListsTest#freeRide`.
/// - **First hour**: given a price list with a 10 minute free period and first-hour fee 5.00 PLN, when a ride of 45
///   minutes is priced, then the total fee is 5.00 PLN. Enforced by `PriceLists`, see `PriceListsTest#firstHour`.
/// - **Tiered hours**: given a price list with a 10 minute free period, first-hour fee 5.00 PLN, second-hour fee
///   7.00 PLN, third-hour fee 9.00 PLN and subsequent hourly fee 12.00 PLN, when a ride of 3 hours 20 minutes is
///   priced, then the total fee is 33.00 PLN. Enforced by `PriceLists`, see `PriceListsTest#tieredHours`.
/// - **Price valid at ride start**: given price list A (first-hour fee 5.00 PLN) valid from 08:00 and price list B
///   (first-hour fee 6.00 PLN) valid from 09:00, when a ride that started at 08:50 and ended at 09:30 is priced, then
///   the total fee is 5.00 PLN. Enforced by `PriceLists`, see `PriceListsTest#priceValidAtRideStart`.
/// - **Billing hours start after the free period** (added in plan review, 2026-11-01): given a price list with a 10
///   minute free period, first-hour fee 5.00 PLN and second-hour fee 7.00 PLN, when a ride of 65 minutes is priced,
///   then the total fee is 5.00 PLN. Enforced by `PriceLists`, see
///   `PriceListsTest#billingHoursStartAfterFreePeriod`.
/// - **Return charges** (added in plan review, 2026-11-01): given a price list with a 10 minute free period and
///   first-hour fee 5.00 PLN, when a ride of 45 minutes with a 3.00 PLN drop-zone return charge is priced, then the
///   total fee is 8.00 PLN. Enforced by `PriceLists`, see `PriceListsTest#returnCharges`.
/// - **No price list valid at ride start** (added in plan review, 2026-11-01): given a price list valid from 08:00,
///   when a ride that started at 07:50 and ended at 08:30 is priced, then pricing is rejected with
///   `NoPriceListValidAt`. Enforced by `PriceLists`, see `PriceListsTest#noPriceListValidAtRideStart`.
/// - **Republished price list replaces the earlier one** (added in plan review, 2026-11-01): given price list A
///   (first-hour fee 5.00 PLN) valid from 08:00, and then price list B (first-hour fee 6.00 PLN) published with the
///   same valid-from 08:00, when a ride 08:30-09:00 is priced, then the total fee is 6.00 PLN. Enforced by
///   `PriceLists`, see `PriceListsTest#republishedPriceListReplacesEarlierOne`.
/// - **Maximum-rental-time penalty** (*not modelled yet*): open question whether it belongs to Pricing or Rentals.
///   Awaiting an owner decision and a strict Given/When/Then example, see the plan.
/// - **A ride is priced only once** (*not modelled yet*): a duplicate *Rental Session Ended* is priced again.
///   Awaiting a strict Given/When/Then example, see the plan.
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
