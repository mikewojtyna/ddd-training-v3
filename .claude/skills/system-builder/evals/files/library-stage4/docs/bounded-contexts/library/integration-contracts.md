# Library: published languages and integration contracts

Written on 2026-11-09 before planning. Integration style: REST via `@AntiCorruptionLayer` adapters for commands and
queries; integration events on the in-process Spring bus, consumed with `@Async @EventListener`; Awaitility in tests.
Rejections: 409 rule / 404 unknown / 400 malformed, body `{"error","message"}`. Money as a decimal string in PLN.

## Event-only relationships

- Lending -> Fines (Fines only listens to `BookReturnedLate`).

## Catalog (OHS/PL)

| Message | Resource | Body / result |
|---|---|---|
| Add Copy (librarian) | `POST /catalog/copies` | `{copyId, isbn, referenceOnly}` → `201` |
| **Copy Availability** (Lending) | `GET /catalog/copies/{copyId}` | `{copyId, isbn, lendable}` or `404` |

## Lending (OHS/PL)

| Message | Resource | Body / result |
|---|---|---|
| Borrow Copy (member app) | `POST /lending/loans` | `{memberId, copyId}` → `201 {loanId, dueOn}`; 409 `CopyNotLendable` |
| Return Copy | `POST /lending/loans/{loanId}/return` | → `200 {late, daysLate}` |

Publishes `BookReturnedLate {loanId, memberId, copyId, daysLate, returnedAt}` (listener: Fines).

## Fines

| Message | Resource | Body / result |
|---|---|---|
| Outstanding Fees (query) | `GET /fines/members/{memberId}/fees` | `{memberId, outstanding, fees: [..]}` |

Consumes `lending.api.events.BookReturnedLate`. Charges through the port `FeePayments` → `PaymentGateway` (simulated:
cards starting `tok_declined` are declined). 🎭 DE-4: PLN 0.50 per day late, max PLN 20.00 per loan.
