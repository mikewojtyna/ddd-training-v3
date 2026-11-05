# Transcript

## User

can you sanity-check the VeloCity context map in src/main/cml/VeloCity-Context-Map.cml? I wrote it as the reference solution for the workshop and I want to know if it breaks any of our context mapping rules before I show it to the group.

## Assistant -> Expert (message 1)

Before I judge the map, I need the rules I'm checking against — they're not written down anywhere in the repo I can see. Could you list the context mapping rules you want the reference solution to obey? Specifically:

1. Cycles: is the rule "no upstream/downstream cycles of any length" (e.g. A->B->C->A counts), or only "no two contexts with arrows in both directions"?
2. Patterns: which patterns are allowed on which side? (e.g. ACL only downstream, OHS/PL only upstream, Conformist only downstream; Shared Kernel / Partnership allowed or banned?)
3. Must every relationship name at least one pattern on each side, or is a bare [U]->[D] acceptable?
4. Is an unconfirmed / assumed relationship acceptable in a reference solution?
5. Any other rules (e.g. external systems must be shielded by an ACL, conformist to an external party allowed, one owner per responsibility, naming)?

Also, one domain question: in the current map, Accounts is upstream of Rentals ("is account active?"), Rentals is upstream of Settlement (Rental Session Ended) and Settlement is upstream of Accounts (Rental Fee Unpaid -> block / Outstanding Debt Cleared -> unblock). Does Rentals really call Accounts in Accounts' language to check if the account is active, and does Accounts really react to Settlement's events in Settlement's language?

## Expert reply 1

On the rules (1-5): I'm the business side here. Questions about which mapping patterns go where, and how relationships should be labelled, belong to your method, not to me. Please use the rules your own approach already defines. You only asked me to choose on question 1, so I'll answer that one: cycles of any length count. A->B->C->A is a cycle just like two contexts pointing at each other. On question 4, the business fact is this: nobody on our side has confirmed how fleet-related support incidents get routed to maintenance, so that part really is still an assumption. Whether an assumption may appear in a reference solution is your call.

On the domain question:

- **Checking whether an account can rent.** Before a rider can reserve or unlock a bike, the rental side has to know whether that rider's account is activated and not blocked. The account people own those words and what they mean: incomplete, activated, blocked, and which onboarding steps are needed. The rental side doesn't define "activated" and doesn't care how onboarding works. It asks "can this rider rent right now?" and accepts the account side's answer as it is. The account side never asks the rental side anything.

- **Blocking and unblocking for debt.** The account side doesn't decide when someone owes money. That fact comes from the people who charge for rides. A charge can still fail after the automatic retries, and then the unpaid amount becomes outstanding debt. When they say "this rider's fee is unpaid, there is outstanding debt", the account side takes that as given and blocks the account with the reason "outstanding debt". When those same people say "the debt is cleared", the account side lifts that block. The account side doesn't reinterpret those facts or add rules of its own on top. It just applies the block or the unblock. The people who charge for rides never ask the account side anything; they only announce what happened to the money.

- **A ride ending.** When a ride ends, the rental side announces it: start, end, location and why it ended. The people who charge for rides take it from there. The rental side never hears back about money.

## Assistant -> User (final)

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
