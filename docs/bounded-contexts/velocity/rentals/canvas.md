# Bounded Context Canvas: Rentals

<!--
Written by Claude on 2026-11-08 from docs/domains/velocity/velocity-vision.md ("Renting a Bike", "Returns",
"Broken Bikes & Maintenance", "Billing"), docs/domains/velocity/velocity-context-map.md and the CML, during an
autonomous run. Gaps were filled with role-played domain-expert answers (DE-n), see
docs/bounded-contexts/velocity/domain-expert-answers.md. Replaces the old BikeStation-based code, which was not
treated as a source of truth.
-->

## Name
Rentals

## Purpose
Gets a bike into someone's hands in seconds and makes sure every bike is used by one person at a time: it knows
which bikes are available, reserves and unlocks them for riders and technicians, runs rental sessions from unlock
to return, and makes sure every session ends: by a return at a station, a free-floating return, a failed lock, or
the maximum rental time. It speaks of bikes as *available* or *withdrawn*, never *broken*: why a bike is withdrawn
is its clients' business.

## Strategic Classification
- Domain: core
- Business model: revenue
- Evolution: custom built

## Domain Roles
- Execution Model: runs the reservation → unlock → session → return lifecycle.
- Enforcer: guarantees one bike, one active holder.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Rider app | Command | Reserve Bike (for a ride) | Checks rental eligibility in Accounts |
| Rider app | Command | Unlock Bike | A rider's unlock starts a rental session; eligibility re-checked (DE-80) |
| Rider app | Command | Return Bike at Station | Optional fault description (DE-24) |
| Rider app | Command | End Ride Here (free-floating return) | At the rider's position |
| Rider app | Query | Rider's Active Rental | Reservation or session of a rider |
| Fleet Maintenance | Command | Reserve Bike (for service) | For a technician; only withdrawn bikes (DE-16) |
| Fleet Maintenance | Command | Unlock Bike (for service) | A technician's unlock never starts a session |
| Fleet Maintenance | Command | Withdraw Bike | e.g. a fleet-issue incident or a technician's fault report |
| Fleet Maintenance | Command | Make Bike Available | Back in circulation at a station or position |
| Bike (telemetry) | Command | Signal Fault | The bike itself detected a fault (DE-25) |
| Fleet manager | Command | Add Bike to Fleet | A new bike at a station (DE-18) |
| Fleet operations (clock) | Command | Expire Reservations | Reservation period passed |
| Fleet operations (clock) | Command | End Overdue Sessions | Maximum rental time exceeded |
| Anyone | Query | Bike Availability | |
| Accounts | Query (outbound, see below) | | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Rental Eligibility | Query | Accounts | yes |
| Unlock / Lock | Command | Bike locks (hardware) | yes |
| Rental Session Ended (session, rider, bike, started at, ended at, end reason, station or position) | Event | Settlement | n/a for events (PL) |
| Bike Withdrawn (bike, reason) | Event | Fleet Maintenance | n/a for events (PL) |
| Bike Unlocked (bike, holder) | Event | Fleet Maintenance | n/a |
| Reservation Expired / Unlock Failed (bike, holder) | Event | Fleet Maintenance | n/a |
| Bike Added to Fleet, Bike Reserved, Rental Session Started, Bike Made Available | Event | none outside Rentals yet | n/a |

## Ubiquitous Language
- **Fleet**: all bikes Rentals can lend or that wait for repair; a bike joins it once, when a fleet manager adds it at a station.
- **Bike**: a vehicle Rentals can lend; it is *available* or *withdrawn*, and at the same time free, *reserved* or *unlocked*.
- **Available**: may be reserved by a rider.
- **Withdrawn**: not offered to riders; only a technician may reserve it. Rentals withdraws a bike itself on a fault it detects (failed lock, fault reported at return, fault signalled by the bike, not returned) or when a client asks.
- **Holder**: whoever reserved or unlocked a bike: a *rider* or a *technician*.
- **Reservation**: a short exclusive hold on one bike for one holder: 2 minutes for a rider (DE-10), 30 minutes for a technician (DE-11).
- **Unlock**: opening the bike's lock for its reservation holder. A rider's unlock starts a rental session.
- **Rental session**: a rider's ride, from unlock until the session ends.
- **End reason**: *station return*, *free-floating return*, *lock failure*, *maximum rental time exceeded*.
- **Station**: an official dock where bikes are locked; Rentals knows where each station is.
- **Maximum rental time**: 12 hours (DE-20).
- **Near**: within 50 metres (DE-12).

## Business Decisions

### A rider reserves an available bike nearby (spec: rider-reserves-bike#E1.1)
- **Given** bike B-17 is available at station Central (52.2300, 21.0110) and rider Anna is eligible to rent
- **When** Anna, standing 20 m from B-17, reserves it at 10:00
- **Then** B-17 is reserved for Anna until 10:02 (Bike Reserved)

### A rider standing exactly 50 m away is near (spec: rider-reserves-bike#E2.2)
- **Given** B-17 is available at Central and Anna is eligible to rent
- **When** Anna, standing exactly 50 m from B-17, reserves it at 10:00
- **Then** B-17 is reserved for Anna until 10:02 (Bike Reserved)

### A rider can't reserve a bike far away (spec: rider-reserves-bike#E2.1)
- **Given** B-17 is available at Central
- **When** Anna, standing 400 m away, reserves B-17
- **Then** the reservation is rejected (DE-12)

### An ineligible rider can't reserve (spec: rider-reserves-bike#E3.1, #E3.2)
- **Given** Ben hasn't finished onboarding (no payment method yet), and Carla's account is blocked for outstanding debt
- **When** either of them, standing 10 m from available B-17, reserves it
- **Then** the reservation is rejected (Accounts says *not eligible*)

### One rider, one bike (spec: rider-reserves-bike#E4.1)
- **Given** Anna holds a reservation for B-17
- **When** Anna reserves B-22
- **Then** the reservation is rejected (DE-13)

### A rider in a running session can't reserve (spec: rider-reserves-bike#E4.2)
- **Given** Anna's session on B-17 started at 09:30 and runs
- **When** Anna, standing 15 m from available B-22, reserves B-22 at 10:00
- **Then** the reservation is rejected

### A reserved bike can't be taken by someone else (spec: rider-reserves-bike#E5.1, rider-unlocks-reserved-bike#E3.1, technician-reserves-broken-bike#E3.1)
- **Given** B-17 is reserved for Anna
- **When** rider Ben reserves or unlocks B-17
- **Then** it is rejected

### A rider's unlock starts a rental session (spec: rider-unlocks-reserved-bike#E1.1)
- **Given** B-17 is reserved for Anna until 10:02
- **When** Anna unlocks B-17 at 10:01 and the lock opens
- **Then** a rental session starts for Anna on B-17 at 10:01 (Rental Session Started)

### An ineligible rider can't unlock (spec: rider-unlocks-reserved-bike#E4.1)
- **Given** B-17 is reserved for Anna until 10:02, and Anna's account was blocked at 10:01
- **When** Anna unlocks B-17 at 10:01:30
- **Then** the unlock is rejected and nothing changes; the reservation runs out on its own (DE-80)

### A failed unlock releases the reservation (spec: rider-unlocks-reserved-bike#E2.1, technician-starts-maintenance#E4.1)
- **Given** B-17 is reserved for Anna
- **When** Anna unlocks B-17 and the lock doesn't open
- **Then** the reservation is released and B-17 is available again (Unlock Failed) (DE-15)
- *Technician variant:* withdrawn B-17 reserved for technician Tom, Tom's unlock fails → the reservation is released and B-17 **stays withdrawn** (Unlock Failed).

### After a failed unlock the rider may reserve another bike (spec: rider-unlocks-reserved-bike#E2.2)
- **Given** Anna's unlock of B-17 failed at 10:01
- **When** Anna, standing 10 m from available B-22, reserves B-22 at 10:01:30
- **Then** B-22 is reserved for Anna

### No unlock without a reservation that still holds (spec: rider-unlocks-reserved-bike#E3.2, #E3.3)
- **Given** Anna holds no reservation for available B-22, and Anna's reservation of B-17 ran until 10:02 but wasn't released yet
- **When** Anna unlocks B-22, or unlocks B-17 at 10:02:30
- **Then** the unlock is rejected and the lock stays closed

### An expired reservation releases the bike (spec: fleet-operations-releases-expired-reservations#E1.1, #E1.2, #E2.1, #E2.2, technician-reserves-broken-bike#E4.1, #E4.2, #E4.3)
- **Given** B-17 is reserved for Anna until 10:02
- **When** the reservations are checked at 10:02:01
- **Then** the reservation expired and B-17 is available to anyone (Reservation Expired)
- *Boundary (DE-81):* checked at 10:02:00 it still holds, and Anna can still unlock at 10:02:00 (spec: rider-unlocks-reserved-bike#E1.2).
- *Technician variant:* withdrawn B-17 reserved for Tom at 09:00 (until 09:30, DE-11); checked at 09:30:01 → the reservation expired and B-17 **stays withdrawn**; checked at exactly 09:30:00 → still reserved.

### The next rider can reserve a bike whose reservation expired (spec: fleet-operations-releases-expired-reservations#E1.3)
- **Given** Anna's reservation of B-17 expired at 10:02:01
- **When** Ben, standing 10 m from B-17, reserves it at 10:04
- **Then** B-17 is reserved for Ben until 10:06

### A reservation that ended by an unlock doesn't expire (spec: fleet-operations-releases-expired-reservations#E3.1, #E3.2)
- **Given** Anna unlocked B-17 at 10:01 (ride running), or Anna's unlock of B-17 failed at 10:01
- **When** the reservations are checked at 10:03
- **Then** nothing expires

### A fleet manager adds a new bike to the fleet at a station (spec: fleet-manager-adds-bike-to-fleet#E1.1, #E2.1)
- **Given** bike B-40 is unknown to Rentals
- **When** a fleet manager puts B-40 into service at station Central
- **Then** B-40 is available at Central's position (Bike Added to Fleet) (DE-18); putting B-40 into service again is rejected

### A bike is added only at a station (spec: fleet-manager-adds-bike-to-fleet#E3.1)
- **Given** B-41 is unknown to Rentals and "Harbour Café" is not a station
- **When** a fleet manager puts B-41 into service at "Harbour Café"
- **Then** it is rejected and B-41 isn't part of the fleet

### A station return with a locked bike ends the session (spec: rider-returns-bike-at-station#E1.1)
- **Given** Anna's session on B-17 started at 10:01
- **When** Anna returns B-17 at station Old Town at 10:40 and the lock closes
- **Then** the session ended at 10:40 at Old Town with reason *station return* (Rental Session Ended) and B-17 is available at Old Town

### A failed lock ends the session and withdraws the bike (spec: rider-returns-bike-at-station#E2.1)
- **Given** Anna's session on B-17 started at 10:01
- **When** Anna returns B-17 at Old Town at 10:40 and the lock doesn't close
- **Then** the session ended at 10:40 with reason *lock failure* (Rental Session Ended) and B-17 is withdrawn with reason *lock failed* (Bike Withdrawn)

### A fault reported at a station return withdraws the bike (spec: rider-returns-bike-at-station#E3.1)
- **Given** Anna's session on B-17 started at 10:01
- **When** Anna returns B-17 at Old Town at 10:40 reporting "chain slips", and the lock closes
- **Then** the session ended at 10:40 with reason *station return* (normal fees) and B-17 is withdrawn with reason *fault reported at return* (DE-24)

### The ride ends at the station the rider names (spec: rider-returns-bike-at-station#E4.1, #E4.2)
- **Given** Anna's session on B-17 runs
- **When** Anna returns B-17 naming station Central and the lock closes
- **Then** the session ended at Central, without checking her position; naming "Harbour Café", which is no station, is rejected and the session keeps running

### Only the rider of a running session can return the bike (spec: rider-returns-bike-at-station#E5.1, #E5.2)
- **Given** Anna's session on B-17 runs
- **When** Ben returns B-17 at Old Town, or Anna (without a session on B-22) returns B-22
- **Then** the return is rejected and Anna's session keeps running

### A free-floating return ends the ride where the rider is (spec: rider-ends-ride-in-drop-zone#E1.1)
- **Given** Anna's session on B-17 started at 10:01
- **When** Anna ends the ride at position (52.2297, 21.0122) at 10:30 and the lock closes
- **Then** the session ended at 10:30 at that position with reason *free-floating return* and B-17 is available there

### A free-floating return in an unauthorized zone still ends the ride (spec: rider-ends-ride-in-drop-zone#E1.2)
- **Given** Anna's session on B-17 started at 10:01
- **When** Anna ends the ride at (52.2500, 21.1000), outside every drop zone, at 10:30 and the lock closes
- **Then** the session ended there at 10:30 with reason *free-floating return*; the zone only decides the price

### A failed lock at a free-floating return is a lock failure (spec: rider-ends-ride-in-drop-zone#E2.1)
- **Given** Anna's session on B-17 started at 10:01
- **When** Anna ends the ride at (52.2500, 21.1000) at 10:30 and the lock doesn't close
- **Then** the session ended at 10:30 at that position with reason *lock failure* and B-17 is withdrawn with reason *lock failed* (DE-23)

### Only the rider of a running session can end it here (spec: rider-ends-ride-in-drop-zone#E3.1, #E3.2)
- **Given** Anna's session on B-17 runs
- **When** Ben ends the ride on B-17, or Anna (without a session on B-22) ends a ride on B-22
- **Then** it is rejected and Anna's session keeps running

### A fault signalled by the bike withdraws it, but never interrupts a ride (spec: fleet-operations-withdraws-bike-signalling-fault#E2.1, #E3.1, #E3.2)
- **Given** B-17 is reserved for Anna
- **When** B-17 signals fault "BRAKE_SENSOR"
- **Then** Anna's reservation is cancelled, Anna may reserve another bike, and B-17 is withdrawn with reason *fault signalled* (DE-25)
- *During a ride:* Anna's session on B-17 runs; B-17 signals a fault → nothing changes yet; when Anna returns B-17 at Old Town with the lock closing, the session ends with reason *station return* and B-17 is withdrawn with reason *fault signalled*.

### A free bike that signals a fault is withdrawn at once (spec: fleet-operations-withdraws-bike-signalling-fault#E1.1, #E1.2)
- **Given** B-17 is available at Central and nobody holds it
- **When** B-17 signals fault "BRAKE_SENSOR" at 09:00
- **Then** B-17 is withdrawn with reason *fault signalled* (Bike Withdrawn), and Anna's reservation of B-17 at 09:05 is rejected

### A withdrawn bike that signals a fault stays withdrawn (spec: fleet-operations-withdraws-bike-signalling-fault#E4.1)
- **Given** B-17 was withdrawn at 08:00 with reason *lock failed*
- **When** B-17 signals fault "BRAKE_SENSOR" at 09:00
- **Then** nothing changes and no second Bike Withdrawn happens

### A client can withdraw a bike (spec: fleet-operations-takes-broken-bike-out-of-circulation#E2.2)
- **Given** B-17 is reserved for Anna
- **When** Fleet Maintenance asks Rentals to withdraw B-17
- **Then** Anna's reservation is cancelled and B-17 is withdrawn with reason *requested*; withdrawing an already withdrawn bike changes nothing
- *During a ride:* Anna's session on B-17 runs; Fleet Maintenance asks to withdraw B-17 → B-17 is withdrawn with reason *requested* and the session goes on until Anna returns B-17 (spec: fleet-operations-takes-broken-bike-out-of-circulation#E2.3, DE-86).

### A session longer than the maximum rental time is ended automatically (spec: fleet-operations-ends-overdue-rental#E1.1, #E1.2, #E2.1)
- **Given** Anna's session on B-17 started at 08:00
- **When** sessions are checked at 20:00:01
- **Then** the session ended at 20:00 with reason *maximum rental time exceeded*, and B-17 is withdrawn as *not returned* (DE-20, DE-21)
- *Boundary:* checked at 20:00:00, the session is still running.

### An overdue session noticed late still ends at the 12-hour mark (spec: fleet-operations-ends-overdue-rental#E1.3)
- **Given** Anna's session on B-17 started at 08:00 and runs
- **When** sessions are checked only at 22:15
- **Then** the session ended at 20:00 with reason *maximum rental time exceeded* (DE-82; until now only an Assumption)

### A session that ended in time is left alone (spec: fleet-operations-ends-overdue-rental#E3.1)
- **Given** Anna's session on B-17 started at 08:00 and she returned B-17 at Old Town at 19:59
- **When** sessions are checked at 20:00:01
- **Then** nothing happens: the session stays ended at Old Town at 19:59 and B-17 stays available

### Only a technician can reserve a withdrawn bike (spec: rider-reserves-bike#E5.2, fleet-operations-takes-broken-bike-out-of-circulation#E4.1)
- **Given** B-17 is withdrawn
- **When** Anna reserves B-17
- **Then** it is rejected; technician Tom's reservation of B-17 is accepted, for 30 minutes
- *No position, several holds:* withdrawn B-22 with no known position; Tom reserves it at 09:00 without a position → reserved for Tom until 09:30; Tom, still holding B-17, reserves B-22 at 09:05 → reserved until 09:35 (spec: technician-reserves-broken-bike#E1.2, #E1.3, DE-26, DE-92). *Not withdrawn:* Tom reserves available B-40 → rejected (spec: technician-reserves-broken-bike#E2.1).

### A technician's unlock never starts a session (spec: technician-starts-maintenance#E1.1, #E2.1)
- **Given** withdrawn B-17 is reserved for technician Tom
- **When** Tom unlocks B-17 and the lock opens
- **Then** B-17 is unlocked for Tom (Bike Unlocked) and no rental session starts
- *Wrong or expired holder:* B-17 reserved for Tom until 09:30; Tara unlocks at 09:20 → rejected, still reserved for Tom; after Tom's reservation expired at 09:30, Tom unlocks at 09:35 → rejected (spec: technician-starts-maintenance#E3.1, #E3.2).

### A client makes a withdrawn bike available again (spec: technician-returns-fixed-bike-to-circulation#E1.1)
- **Given** withdrawn B-17 is unlocked for technician Tom
- **When** Fleet Maintenance makes B-17 available at station Riverside
- **Then** B-17 is locked, free and available at Riverside's position (Bike Made Available)
- *At a position:* made available at (52.2297, 21.0122) → available at that position, no station (spec: technician-returns-fixed-bike-to-circulation#E1.2).
- *Rejected:* making B-17 available while it is reserved for a technician but not unlocked, or while a rider's session runs on it.

## Assumptions
- 🎭 DE-14 as revised by DE-80: eligibility is checked at reservation and again at a rider's unlock.
- 🎭 DE-82: an overdue session ends at the 12-hour mark, whenever it is noticed.
- 🎭 DE-25: a fault signalled during a ride withdraws the bike when the session ends.
- 🎭 DE-22: the client-reported station and position are trusted.
- 🎭 DE-26: the 50 m rule applies to riders only. Technicians reserve without a position; finding a bike that wasn't returned (its last signalled position) is the technician's job and out of scope.
- The rider is "told why" a session ended (vision, lock failure) through the answer to their return command, which states the end reason.
- 🎭 DE-86: a bike that is withdrawn while a rider rides it keeps the ride going; it simply isn't available after the return.

## Verification Metrics
- Seconds from reservation to session start.
- Share of reservations that expire or fail to unlock.

## Open Questions
- Should repeated failed unlocks of the same bike withdraw it?
- Should a rider be warned before the maximum rental time is reached?
