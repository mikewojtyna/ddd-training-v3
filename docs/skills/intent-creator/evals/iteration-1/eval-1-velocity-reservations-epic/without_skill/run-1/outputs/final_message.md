I wrote four intent files for VeloCity reservations under `docs/intents/`. Each one covers one thing reservations do:

- `reserve-bike-for-ride.md`: a rider reserves an available bike. It's refused if the bike is reserved, rented or withdrawn, if the account is incomplete or blocked, or if the rider is out of the service area or not near the bike. Only the person holding the reservation can unlock the bike, and a successful unlock uses up the reservation and starts the rental session.
- `reserve-broken-bike-for-maintenance.md`: a technician reserves a broken (withdrawn) bike. Riders still can't take it, and the technician's unlock starts maintenance, never a rental.
- `expire-bike-reservation.md`: a reservation that isn't used in time expires. A rider's bike goes back to the pool; a broken bike stays out of circulation and waits for another technician.
- `release-reservation-on-failed-unlock.md`: if the unlock fails, the reservation is released at once and no rental or maintenance starts. A broken bike stays broken.

Each file has the why, the outcome, given/when/then scenarios, business rules, assumptions, what's out of scope, open questions, and links to the vision, the context map and the code. In line with the reference context map, the reservation lives in Rentals and uses its words, available and withdrawn. Fleet Maintenance asks Rentals for a technician's reservation and starts maintenance when that technician unlocks the bike. Nothing is committed, and I didn't touch any code, CML or vision files.

**Calls I made for you to confirm:**
- A rider can hold only one reservation at a time, and can't reserve while already on a ride.
- Technicians reserve only broken bikes. They skip the account and location checks because they reserve before travelling to the bike, and they may hold several reservations at once.
- Reserving is free, and so is letting a reservation expire.
- After a rider's unlock fails, the bike's availability stays as it was. This is still an open question (below).

**Questions for the domain expert:**
1. How long does a reservation last? For riders the vision says "the seconds it takes to unlock", but for technicians it covers the time "while the technician is on the way". Is this one concept with a different length per holder, or two different holds? I recorded this as an open question rather than resolving it, since the vision's gaps are meant to be discovered in the workshop.
2. When a rider's unlock fails, does the bike go back to the pool, or is it withdrawn as possibly faulty and reported to Fleet Maintenance? A failed lock at return makes the bike broken, but the vision says nothing about a failed unlock.
3. Can riders or technicians cancel a reservation on purpose?
4. Can technicians reserve bikes that aren't broken, for example for an inspection or to move them between stations? How many can one technician hold, and who decides which technician gets which bike?
5. What does "near the bike" mean? And can bikes left in a drop zone be reserved too? The current `BikeStation` aggregate only knows about bikes docked in a station.
6. Should a rider who keeps letting reservations expire face any consequence?
