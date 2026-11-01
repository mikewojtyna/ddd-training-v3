I've updated the intents in `docs/intents/`. The pilot's return rule is superseded rather than rewritten, and reservations are two new draft intents. `intents.py check` reports 0 errors and 0 warnings.

## What changed

**1. Returning to any station (a changed need, so the old intent is superseded)**
- New: `docs/intents/rider-returns-bike-to-any-station.md`, #3. velocity → Rider → *Return the bike* → *Return at a station*. Status `draft`, `supersedes: rider-returns-bike-to-origin-station`.
  - `## Why` gives the reason: operations dropped the pilot's origin-station rule because "rebalancing trucks are cheaper than angry riders".
- Changed: `docs/intents/rider-returns-bike-to-origin-station.md`. It was `accepted`, so it's part of the history and I didn't rewrite it. The only edits are `status: superseded` and `superseded_by: rider-returns-bike-to-any-station`.

**2. Reservations (an epic, split into a two-intent first slice)**
- New: `docs/intents/rider-reserves-bike-at-station.md`, #4. velocity → Rider → *Get a bike* → *Reserve a bike*. Outcome: **Bike Reserved**.
- New: `docs/intents/fleet-operations-releases-expired-reservation.md`, #5. velocity → Fleet Operations → *Get a bike* → *Release an expired reservation*. Outcomes: **Reservation Expired**, **Bike Available Again**.
- Why two: "hold a bike for 15 minutes" means something has to happen when the 15 minutes run out. Without the release, an unused reservation blocks a bike indefinitely. These two are the smallest slice that works end to end.
- I named the actor who wants the release (Fleet Operations) instead of saying "the system".
- Your two rules are copied as you stated them ("hold a bike for 15 minutes", "one active reservation per rider"). I didn't turn them into precise rules; that happens in the spec.

**Other files**
- `docs/intents/story-map.md`: added the actor *Fleet Operations* and the tasks *Reserve a bike* and *Release an expired reservation* under *Get a bike*, before *Unlock a bike*.
- `docs/intents/README.md`: regenerated. The timeline now shows the return-rule change (#2 crossed out → #3) followed by reservations (#4, #5).

I left `rider-rents-bike-from-station` (delivered) untouched. Its "enables" link still points to the origin-station return, which is accurate history.

## Candidate intents not written yet
- `rider-cancels-reservation`: the rider releases their own reservation early.
- `rider-reservation-released-after-failed-unlock`: from the vision ("the reservation is then released and the rider is free to try another bike").
- `technician-reserves-broken-bike`: the vision says technicians reserve bikes too, and an expired technician reservation does not put the broken bike back in the pool.
- `fleet-operations-rebalances-stations`: the trucks operations is now accepting as the price of any-station returns.

Which of these should come next?

## Open questions (I made a call where noted; please confirm)
1. **Reservation length contradicts the vision.** The vision describes a reservation as lasting only "the seconds it takes to unlock" and as a step inside every rental. Product wants a 15-minute hold. I wrote it as a separate step that comes before renting and left the contradiction open. Is reserving optional, or does every rental now start with a reservation?
2. **New actor.** I named the owner of the expiry rule *Fleet Operations* and added it to the story map. Should it be someone else, for example product or City Operations?
3. **Reserving a second time.** If a rider who already holds a reservation tries again, is the attempt refused or does the new reservation replace the old one? Can a rider with an active rental also hold a reservation?
4. **Cost.** Are reservations free? Is there a fee or a cool-down for riders whose reservations keep expiring unused?
5. **Where the rider can reserve.** Must the rider be near the station (the vision checks location when renting), or can they reserve from anywhere?
6. **Technicians.** Do the 15-minute and one-reservation rules apply to technicians too?
7. **Expiry notice.** Should the rider be told when their reservation expires, or warned shortly before?
8. **Any-station returns.** Is the price the same when a rider returns to a different station? What happens when the chosen station is full? Does the lock-failure-at-return case from the vision belong in this intent or its own?

## Next step
Please review the three new drafts and move the ones you agree with from `draft` to `accepted`. Each accepted intent then gets a spec, which is a separate skill. I haven't written any spec, plan or code.
