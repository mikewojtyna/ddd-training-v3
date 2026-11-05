I checked `src/main/cml/VeloCity-Context-Map.cml` (and the matching `velocity/velocity-context-map.md`). Most of it holds up, but one thing breaks a rule outright: **the map has an upstream/downstream cycle**. Fix that before you show it.

## Rule violation: a 3-context cycle

| Relationship in the CML | Direction |
|---|---|
| `Accounts [U]->[D] Rentals : AccountStatus` | Accounts → Rentals |
| `Rentals [U,PL]->[D,CF] Settlement : RentalSessionEnded` | Rentals → Settlement |
| `Settlement [U,PL]->[D,CF] Accounts : DebtBlocking` | Settlement → Accounts |

Accounts → Rentals → Settlement → Accounts closes a loop. You confirmed that cycles of any length count, not only two-way pairs. You also confirmed that each of the three arrows is drawn the right way round. Rentals accepts Accounts' answer to "can this rider rent?". Settlement takes Rental Session Ended as announced. Accounts applies Settlement's "unpaid / debt cleared" facts as given. So no arrow is mislabelled. The cycle is real.

The doc says "No cycles" twice (in the ride-to-money zoom and in Key Design Decisions). The group will catch that claim being false.

Ways to break it (your call as trainer):
1. **Make Accounts the upstream for blocking (recommended).** Accounts publishes an OHS/PL "block / unblock account (reason)" API. Settlement calls it as a conformist downstream: `Accounts [U,OHS,PL]->[D,CF] Settlement : AccountBlocking`. Accounts still decides nothing about debt. Settlement only tells it to apply a block with reason "outstanding debt". This follows your own "the caller is always downstream" rule, which you already use for Pricing and Debt Recovery. Accounts becomes upstream of both Rentals and Settlement, and the cycle disappears.
2. Keep the event direction and turn the Settlement ↔ Accounts pair into an explicit **Partnership**. I'd avoid this: it puts a Partnership into a reference solution just to hide a loop.
3. Move the "blocked for debt" check out of Accounts. For example, Rentals could ask Settlement "does this rider have outstanding debt?". That just creates a new two-way pair between Rentals and Settlement, so it doesn't help.

## Smaller issues (my own conventions; you said the pattern rules are the method's call)

- **`Accounts [U]->[D] Rentals` has no patterns.** Every other internal relationship names its patterns. The expert describes Rentals as accepting Accounts' answer as-is, so this should at least be `[D,CF]`, probably with `[U,OHS]`. If you take option 1 above, both Accounts arrows should look the same.
- **`CustomerSupport [U]->[D] FleetMaintenance : FleetIssueIncident_Unconfirmed`** is still an assumption. The expert confirmed that nobody has said how fleet-related incidents reach maintenance. A reference solution can show it, but then label it as an open question for the debrief. If you keep it, give it patterns or drop it from the CML. Right now it's the only relationship with neither patterns nor a confirmed direction.
- **`City [U]->[D,CF] Pricing`**: conforming to an external party works if the zone map is a stable, simple format. Since you protect Payments from the PaymentProcessor with an ACL, expect someone to ask why City gets no ACL. Have one sentence ready, or change it to `[D,ACL]`.
- **"Charging" appears in both Payments' and Settlement's responsibilities.** The doc means Settlement decides and triggers the charge and Payments executes it against the processor. The CML text doesn't say so. Rewording Payments to something like "Executing charges against the processor" avoids a "two owners of charging" debate.

## Rules the map passes

- No Shared Kernel anywhere.
- ACL appears only on a downstream side (`PaymentProcessor -> Payments`).
- OHS and PL appear only on upstream sides; CF appears only on downstream sides.
- Apart from the Accounts/Rentals/Settlement loop, there are no other cycles and no two-way pairs. The Rentals → FleetMaintenance, Pricing → Settlement, Payments → Settlement and Settlement → DebtRecovery directions all match "the caller is downstream".
- Every bounded context in `contains` is defined, and every defined context is used.

I didn't change any repo files. Want me to draft the corrected CML and the doc/mermaid updates for option 1?
