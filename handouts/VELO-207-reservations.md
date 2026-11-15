# VELO-207 Manage reservations

**Type:** Epic · **Reporter:** Product (VeloCity app) · **Priority:** High · **Sprint:** 47

> Fictional ticket for Exercise 17. It is written the way tickets usually arrive: a mix of needs, solutions and
> opinions. Don't hand it to the agent as a spec. It's raw input for intents.

## Description

We need to manage reservations properly before the summer season. Right now people walk up to a bike, start unlocking
and someone else unlocks it a second earlier. Support gets angry calls every day.

Proposed solution (from the backend team): `POST /api/reservations` with `bikeId` and the rider's GPS position. Put a
Redis lock on the bike with a TTL so it expires by itself, and publish to the `reservation_expired` Kafka topic when the
TTL runs out so the app can refresh. The system releases expired reservations automatically, no manual work.

## What we want

- Riders can reserve a bike and then unlock it with one tap. Only when they're close to the bike, obviously.
- Marketing: "Hold the bike for **15 minutes** so riders can see a bike on the map and walk to it. Competitors do 10, we
  want to beat them."
- Ops (Kasia, fleet ops lead): "The vision says the reservation exists only for *the seconds it takes to unlock*. A bike
  held for 15 minutes is a bike nobody can ride. Keep it short."
- If the lock doesn't open, the rider shouldn't be stuck with a reservation on a bike they can't use.
- A reservation that nobody uses should not keep the bike off the street.
- Technicians should reserve broken bikes too, before they drive out to fix them. They need longer, they're driving
  across town.
- Show a countdown timer in the app while the bike is reserved (design: see Figma, frame "Reserve v3").
- Riders may cancel a reservation if they change their mind.
- Tech debt: upgrade the reservations table to Postgres 17 while we're at it.

## Acceptance criteria

- [ ] Reservations work
- [ ] No double unlocks
- [ ] Expired reservations are cleaned up
- [ ] Works for technicians

## Comments

**Backend dev:** Do we block reservations for riders who owe us money? Payments says yes, Product says "not for MVP".

**Support lead:** Most complaints are about the bike being taken while the rider is standing right next to it. Not
about walking to a bike.
