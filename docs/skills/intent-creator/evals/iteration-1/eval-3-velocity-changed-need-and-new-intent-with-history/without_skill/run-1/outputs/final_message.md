I updated the intents in `docs/intents/`. Nothing is committed.

**Returning to any station (a changed need, so the old intent is kept as history)**
- New: `rider-returns-bike-to-any-station.md` (sequence 3, `accepted`, `supersedes: rider-returns-bike-to-origin-station`). The rule "return to the same station" is replaced by "return to any VeloCity station". The 11-hour limit stays. Drop-zone returns are still out of scope.
- `rider-returns-bike-to-origin-station.md` is now `status: superseded` with `superseded_by: rider-returns-bike-to-any-station`, plus a note saying why. I left the rest of it as it was so the history stays readable.

**Reservations (new intent)**
- New: `rider-reserves-bike.md` (sequence 4, `proposed`, activity "Get a bike", task "Reserve a bike"). It records the two rules you gave me: a 15-minute hold, and one active reservation per rider. It also lists two rules I assumed: nobody else can take the held bike, and the hold ends on its own after 15 minutes. The intent says it enables renting.
- The Notes for the spec point out a clash with `velocity/velocity-vision.md`. The vision already has a "reservation", but it's a hold of a few seconds during unlock, and technicians use it too. This 15-minute rider hold is a longer-lived business concept.

**Other edits**
- `rider-rents-bike-from-station.md`: its links now point to the new return intent and to the reservation intent.
- `story-map.md`: added "Reserve a bike" before "Unlock a bike" under "Get a bike". "Return at a station" already covers any station.

**Decisions I made without you (please confirm)**
1. I marked the any-station return `accepted`, because operations has already decided it. I marked reservations `proposed`, because it's a product request.
2. I scoped reservations to riders only. Technicians reserving broken bikes is left out.
3. I treated the return change as a new intent that supersedes the old one, rather than editing the old one in place.

**Open questions (also written into the intents)**
- Return: what happens when the station you return to has no free dock? Is there a fee for a one-way return? Who triggers rebalancing, and does Rentals need to publish an event for it?
- Reservation: Can a rider cancel? Can a rider reserve while already on a rental? Is it free, and is there a no-show penalty? Is it a specific bike or "any bike at station X"? Is reserving optional before renting? Only activated, non-blocked accounts? And is it the same concept as the short unlock hold in the vision, or a separate one?
