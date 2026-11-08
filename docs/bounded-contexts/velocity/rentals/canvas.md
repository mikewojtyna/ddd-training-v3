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
| Bike Reserved, Rental Session Started, Bike Made Available | Event | none outside Rentals yet | n/a |

## Ubiquitous Language
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

### A rider reserves an available bike nearby
- **Given** bike B-17 is available at station Central (52.2300, 21.0110) and rider Anna is eligible to rent
- **When** Anna, standing 20 m from B-17, reserves it at 10:00
- **Then** B-17 is reserved for Anna until 10:02 (Bike Reserved)

### A rider can't reserve a bike far away
- **Given** B-17 is available at Central
- **When** Anna, standing 400 m away, reserves B-17
- **Then** the reservation is rejected (DE-12)

### One rider, one bike
- **Given** Anna holds a reservation for B-17
- **When** Anna reserves B-22
- **Then** the reservation is rejected (DE-13)

### A reserved bike can't be taken by someone else
- **Given** B-17 is reserved for Anna
- **When** rider Ben reserves or unlocks B-17
- **Then** it is rejected

### A rider's unlock starts a rental session
- **Given** B-17 is reserved for Anna until 10:02
- **When** Anna unlocks B-17 at 10:01 and the lock opens
- **Then** a rental session starts for Anna on B-17 at 10:01 (Rental Session Started)

### An ineligible rider can't unlock
- **Given** B-17 is reserved for Anna until 10:02, and Anna's account was blocked at 10:01
- **When** Anna unlocks B-17 at 10:01:30
- **Then** the unlock is rejected and nothing changes; the reservation runs out on its own (DE-80)

### A failed unlock releases the reservation
- **Given** B-17 is reserved for Anna
- **When** Anna unlocks B-17 and the lock doesn't open
- **Then** the reservation is released and B-17 is available again (Unlock Failed) (DE-15)
- *Technician variant:* withdrawn B-17 reserved for technician Tom, Tom's unlock fails → the reservation is released and B-17 **stays withdrawn** (Unlock Failed).

### An expired reservation releases the bike
- **Given** B-17 is reserved for Anna until 10:02
- **When** the reservations are checked at 10:02:01
- **Then** the reservation expired and B-17 is available to anyone (Reservation Expired)
- *Boundary (DE-81):* checked at 10:02:00 it still holds, and Anna can still unlock at 10:02:00.
- *Technician variant:* withdrawn B-17 reserved for Tom at 09:00 (until 09:30, DE-11); checked at 09:31 → the reservation expired and B-17 **stays withdrawn**; checked at 09:29 → still reserved.

### A fleet manager adds a new bike to the fleet at a station
- **Given** bike B-40 is unknown to Rentals
- **When** a fleet manager puts B-40 into service at station Central
- **Then** B-40 is available at Central's position (Bike Added to Fleet) (DE-18); putting B-40 into service again is rejected

### A station return with a locked bike ends the session
- **Given** Anna's session on B-17 started at 10:01
- **When** Anna returns B-17 at station Old Town at 10:40 and the lock closes
- **Then** the session ended at 10:40 at Old Town with reason *station return* (Rental Session Ended)

### A failed lock ends the session and withdraws the bike
- **Given** Anna's session on B-17 started at 10:01
- **When** Anna returns B-17 at Old Town at 10:40 and the lock doesn't close
- **Then** the session ended at 10:40 with reason *lock failure* (Rental Session Ended) and B-17 is withdrawn with reason *lock failed* (Bike Withdrawn)

### A fault reported at a station return withdraws the bike
- **Given** Anna's session on B-17 started at 10:01
- **When** Anna returns B-17 at Old Town at 10:40 reporting "chain slips", and the lock closes
- **Then** the session ended at 10:40 with reason *station return* (normal fees) and B-17 is withdrawn with reason *fault reported at return* (DE-24)

### A free-floating return ends the ride where the rider is
- **Given** Anna's session on B-17 started at 10:01
- **When** Anna ends the ride at position (52.2297, 21.0122) at 10:30 and the lock closes
- **Then** the session ended at 10:30 at that position with reason *free-floating return*

### A failed lock at a free-floating return is a lock failure
- **Given** Anna's session on B-17 started at 10:01
- **When** Anna ends the ride at (52.2500, 21.1000) at 10:30 and the lock doesn't close
- **Then** the session ended at 10:30 at that position with reason *lock failure* and B-17 is withdrawn with reason *lock failed* (DE-23)

### A fault signalled by the bike withdraws it, but never interrupts a ride
- **Given** B-17 is reserved for Anna
- **When** B-17 signals fault "BRAKE_SENSOR"
- **Then** Anna's reservation is cancelled and B-17 is withdrawn with reason *fault signalled* (DE-25)
- *During a ride:* Anna's session on B-17 runs; B-17 signals a fault → nothing changes yet; when Anna returns B-17 at Old Town with the lock closing, the session ends with reason *station return* and B-17 is withdrawn with reason *fault signalled*.

### A client can withdraw a bike
- **Given** B-17 is reserved for Anna
- **When** Fleet Maintenance asks Rentals to withdraw B-17
- **Then** Anna's reservation is cancelled and B-17 is withdrawn with reason *requested*; withdrawing an already withdrawn bike changes nothing

### A session longer than the maximum rental time is ended automatically
- **Given** Anna's session on B-17 started at 08:00
- **When** sessions are checked at 20:00:01
- **Then** the session ended at 20:00 with reason *maximum rental time exceeded*, and B-17 is withdrawn as *not returned* (DE-20, DE-21)
- *Boundary:* checked at 20:00:00, the session is still running.

### Only a technician can reserve a withdrawn bike
- **Given** B-17 is withdrawn
- **When** Anna reserves B-17
- **Then** it is rejected; technician Tom's reservation of B-17 is accepted, for 30 minutes

### A technician's unlock never starts a session
- **Given** withdrawn B-17 is reserved for technician Tom
- **When** Tom unlocks B-17 and the lock opens
- **Then** B-17 is unlocked for Tom (Bike Unlocked) and no rental session starts

### A client makes a withdrawn bike available again
- **Given** withdrawn B-17 is unlocked for technician Tom
- **When** Fleet Maintenance makes B-17 available at station Riverside
- **Then** B-17 is locked, free and available at Riverside's position (Bike Made Available)
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
