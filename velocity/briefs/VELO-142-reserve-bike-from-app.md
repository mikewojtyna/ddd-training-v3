# VELO-142: Reserve a bike from the app before reaching the station

| Field | Value |
|---|---|
| Type | Story |
| Status | Ready for refinement |
| Priority | High |
| Reporter | Product (Rider Experience) |
| Labels | rider-app, rentals, q1-okr |
| Epic | VELO-120 Rider retention |
| Created | 2026-11-03 |

<!-- Training note: this is a fictional ticket, deliberately written the way real tickets often are.
     It mixes the business need with solution ideas, leaves rules vague and contradicts the vision on purpose
     (the vision says a reservation lasts "the seconds it takes to unlock", for a rider standing near the bike).
     It's the input for step 3 of the AI-assisted DDD flow (intent-creator). -->

## Description

Riders keep telling us that the bike they saw on the map was gone by the time they walked to the station. Support logged **412 contacts** about this in October, and it's the #2 reason in the churn survey ("I can never count on a bike being there").

We want riders to be able to **reserve a bike from the app** while they're on the way, so it's still there when they arrive. Competitors (Bolt, Nextbike) already have this.

From the product meeting (2026-11-02):
- A reservation should hold the bike "for about 10–15 minutes". Ops needs to confirm the exact number, since bikes sitting reserved don't earn money.
- **One reservation at a time per rider.** We don't want people blocking half a station.
- The rider should be able to **cancel** if they change their mind.
- If the rider doesn't show up, the bike has to go back to the pool automatically.
- Only riders with a fully activated account should be able to reserve (same as for unlocking today).

Ops (Fleet) added in the comments:
> Technicians asked for the same thing: they'd like to hold a broken bike while driving to it, so riders' reports don't send two technicians to the same bike. Can we do both in one go?

Finance asked whether reservations should cost something (e.g. PLN 1 that's deducted from the ride). **Not for this release**, but keep the door open.

## Acceptance criteria (draft)

- [ ] `POST /api/v2/reservations` with `bikeId` returns 201 and a `reservationId`
- [ ] Reservation stored in Redis with a TTL of 15 min
- [ ] 409 if the rider already has an active reservation
- [ ] `DELETE /api/v2/reservations/{id}` cancels it
- [ ] Push notification "Your reservation expires in 2 minutes"
- [ ] Reserved bike is hidden from the map for other riders
- [ ] Analytics event `reservation_created` sent to Mixpanel

## Notes from engineering

- We could reuse the unlock flow and just add a `RESERVED` status to the bike table.
- The mobile team needs the API contract by sprint 24.

## Success metrics

- Support contacts about "bike gone when I arrived" down by 50% within two months of release.
- At least 30% of rentals start from a reservation.
