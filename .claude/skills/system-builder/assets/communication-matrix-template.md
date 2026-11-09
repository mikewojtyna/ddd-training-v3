# <System>: communication matrix

Proof that every relationship in `<map.cml>` really works in code. One row per message crossing a relationship.

- **event:** the row proves that the *consumer reacted*: its state changed, or it published its own event. Proving
  only that the producer published doesn't count. Assert with Awaitility.
- **command:** the provider's state changed, and the caller handles the provider's rejection.
- **query:** the caller's decision follows from the provider's answer.

Generate the skeleton with `comm_matrix.py init <map.cml>`. Check it with
`comm_matrix.py check <this file> --cml <map.cml> --tests src/test`.

| # | Relationship | Message | Kind | Effect in the receiving context | Proving test |
|---|---|---|---|---|---|
| M1 | Rentals -> Settlement : RentalSessionEnded | RentalSessionEnded | event | Settlement prices the ride in Pricing and charges it: ride status SETTLED, fee 5.00 | `RideToMoneyJourneyTest#rideIsPricedAndCharged` |
| M2 | Accounts -> Rentals : AccountStatus | Rental Eligibility | query | a blocked rider's reservation is rejected with BLOCKED | `RiderReservesBikeSpecTest#e3_1_blockedRiderIsRejected` |
| M3 | Accounts -> Settlement : AccountBlocking | Block Account | command | after a third failed charge, Accounts shows the OUTSTANDING_DEBT block | `UnpaidRideToDebtRecoveryJourneyTest#...` |
