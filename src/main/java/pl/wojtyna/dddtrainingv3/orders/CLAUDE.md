# Orders system

Orders is a small practice system, created on 2026-11-04 to try the `ddd-tactical-building-blocks` skill on a simple aggregate. The repo-root `CLAUDE.md` covers the package conventions; this file adds Orders-specific context.

## Sources of truth

- `docs/bounded-contexts/orders/ordering/canvas.md` is the canvas, and `plan.md` next to it is the implementation plan.
- `docs/bounded-contexts/orders/ordering/conversation.md` is the recorded Claude Code session that built the system, kept for students.
- There is no vision document, context map or CML model for this system.

## Bounded contexts

- **Ordering** (`ordering`): customers prepare orders. The `Order` aggregate (event-sourced) guards two rules: the order total cannot exceed 100 PLN, and an order holds at most 3 items of the same item type. It was built one Given/When/Then scenario at a time (`OrderTest`). Commands return all pending events since `open`/`rebuild`. Persistence is an in-memory event store (`InMemoryOrderRepository`) with optimistic locking through `Version`. Entry point: `AddItemToOrderUseCase`, exposed as `POST /ordering/orders/{orderId}/items`; the first item opens the order.
