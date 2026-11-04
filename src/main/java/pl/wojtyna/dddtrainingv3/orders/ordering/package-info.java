/// # Ordering bounded context
///
/// ## Purpose
/// Allows to prepare orders.
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
/// - **Order**: a customer's collection of items, whose total value and per-type quantities are capped.
/// - **Item**: one unit added to an order, with an item type and a price in PLN.
/// - **Item type**: the kind of product an item is, e.g. "Helmet". Items of the same type count towards the same limit.
/// - **Order total**: the sum of the prices of all items in the order.
///
/// ## Business decisions
/// - **Order total cannot exceed 100 PLN**: given an empty order, when an item for 100 PLN is added, then the item is
///   added (see `OrderTest#itemForExactly100PlnFitsIntoEmptyOrder`); given an order with items totalling 90 PLN, when
///   an item for 20 PLN is added, then adding the item is rejected (see `OrderTest#itemPushingTotalAbove100PlnIsRejected`).
///   Enforced by `Order`.
/// - **No more than 3 items of the same type**: given an order with 2 "Helmet" items, when another "Helmet" is added,
///   then the item is added (see `OrderTest#thirdItemOfTheSameTypeIsAdded`); given an order with 3 "Helmet" items, when
///   another "Helmet" is added, then adding the item is rejected (see `OrderTest#fourthItemOfTheSameTypeIsRejected`).
///   Enforced by `Order`.
///
/// Source canvas: `docs/bounded-contexts/orders/ordering/canvas.md`
@BoundedContext(name = "Ordering")
@Module
package pl.wojtyna.dddtrainingv3.orders.ordering;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
