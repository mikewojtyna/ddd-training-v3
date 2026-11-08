---
name: fleet-operations-takes-broken-bike-out-of-circulation
intent: fleet-operations-takes-broken-bike-out-of-circulation
system: velocity
status: approved
date: 2026-11-08
---

# Spec: Fleet operations wants every faulty bike known as broken and kept away from riders

Concrete rules, examples and contract for [the intent](../intents/fleet-operations-takes-broken-bike-out-of-circulation.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Withdrawn bike | A bike no longer offered to riders; only a technician may reserve it (DE-16). |
| Withdrawal reason | Why a bike was withdrawn: *lock failed*, *fault reported at return*, *fault signalled*, *not returned*, or *requested* (fleet operations asked for it). |
| Broken bike | A bike reported faulty; it waits for a technician and is never offered to riders until it is back in circulation (vision). |
| Fault source | What reported the bike broken: one of the four fault withdrawal reasons, a fleet-issue incident, or a technician. |
| Fleet-issue incident | A rider's incident about a named bike that is broken or can't be unlocked (DE-50). |
| Incident link | A fleet-issue incident recorded on the broken bike, so it can be resolved when the bike is fixed (DE-55). |

## Rules

### R1: A bike withdrawn because of a detected fault becomes broken

- **E1.1** Given Anna returned bike B-17 at station Old Town at 10:40 and the lock didn't close, so B-17 was withdrawn with reason *lock failed*, When fleet operations learns of the withdrawal, Then B-17 is broken with fault source *lock failed* (Bike Reported Broken).
- **E1.2** Given Ben's rental session on B-22 started at 08:00 and was ended at 20:00 for exceeding the maximum rental time of 12 hours (DE-20), so B-22 was withdrawn with reason *not returned*, When fleet operations learns of the withdrawal, Then B-22 is broken with fault source *not returned* (Bike Reported Broken) (DE-21).

### R2: A fleet-issue incident makes the bike broken and withdraws it from riders at once (DE-52)

- **E2.1** Given B-17 is available at Central and isn't broken, When Anna reports fleet-issue incident I-1 for B-17 at 10:00 ("rear brake doesn't work"), Then B-17 is broken with fault source *fleet issue*, linked to I-1 (Bike Reported Broken), and B-17 is withdrawn with reason *requested* (Bike Withdrawn).
- **E2.2** Given B-17 is reserved for rider Ben until 10:01, When Anna reports fleet-issue incident I-1 for B-17 at 10:00, Then Ben's reservation is cancelled and B-17 is broken and withdrawn (Bike Reported Broken, Bike Withdrawn).
- **E2.3** Given Anna's rental session on B-17 started at 10:00, When Anna reports fleet-issue incident I-1 for B-17 at 10:10 ("brake squeals"), Then B-17 is broken and withdrawn (Bike Reported Broken, Bike Withdrawn) and Anna's ride goes on until she returns B-17 (DE-86).

### R3: A bike that is already broken isn't reported broken again; a new fleet-issue incident is only linked to it

- **E3.1** Given B-17 has been broken since 10:40 with fault source *lock failed*, When Ben reports fleet-issue incident I-2 for B-17 at 11:00, Then I-2 is linked to B-17 and nothing else changes: no second Bike Reported Broken, no second Bike Withdrawn.

### R4: A broken bike can't be reserved by a rider (vision, DE-16)

- **E4.1** Given B-17 is broken since 10:40 with fault source *lock failed* and stands at Old Town, When Ben, standing 10 m from it, reserves B-17 at 11:00, Then the reservation is rejected.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Report Bike Broken | Fleet Operations (on Bike Withdrawn for a fault, or on Fleet Issue Reported) | bike, fault source, incident (for a fleet issue; on an already broken bike only the incident is linked, R3) | Bike Reported Broken | – |
| Withdraw Bike | Fleet Operations (for a fleet-issue incident) | bike | Bike Withdrawn | – |
| Reserve Bike (rider) | Rider | rider, bike | Bike Reserved | R4 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Bike Reported Broken | a bike that wasn't broken is reported faulty | bike, fault source, incident (if any), reported at |
| Bike Withdrawn | a bike stops being offered to riders | bike, withdrawal reason, withdrawn at |
| Bike Reserved | a rider's reservation is accepted (never for a broken bike) | holder, bike, reserved at, expires at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Broken bike | bike | Identifier (Bike) | required; at most one open broken record per bike (R3) |
| Broken bike | fault source | one of {lock failed, fault reported at return, fault signalled, not returned, fleet issue, technician} | the source of the first report |
| Broken bike | incidents | list of Identifier (Incident) | fleet-issue incidents linked to the bike |
| Broken bike | reported at | Timestamp | required |
| Bike Withdrawn | withdrawal reason | one of {lock failed, fault reported at return, fault signalled, not returned, requested} | required |

## Out of scope

- Detecting the fault at a return (`rider-returns-bike-at-station`, `rider-ends-ride-in-drop-zone`).
- Ending an overdue rental (`fleet-operations-ends-overdue-rental`).
- Withdrawing a bike that signals a fault (`fleet-operations-withdraws-bike-signalling-fault`).
- Reporting an incident and detecting its type (`rider-reports-incident`).
- A technician's own fault report (`technician-reports-fault`).
- Reserving, repairing and returning the bike (`technician-reserves-broken-bike`, `technician-starts-maintenance`, `technician-returns-fixed-bike-to-circulation`).

## Open questions

- ~~**Q1** Does a fleet-issue incident make the bike broken at once, or only after an agent accepts it?~~ At once, when the incident is reported and its type detected (role-played domain expert, DE-52, 2026-11-08)
- ~~**Q2** Is a bike that was never returned after the maximum rental time broken?~~ Yes, it has to be recovered and checked (role-played domain expert, DE-21, 2026-11-08)
- ~~**Q3** Who can reserve a broken bike?~~ Only a technician (role-played domain expert, DE-16, 2026-11-08)
- ~~**Q4** A rider reports a fleet issue about the bike they are riding right now: does the ride go on?~~ Yes, no withdrawal interrupts a ride; the bike just isn't available after the return (role-played domain expert, DE-86, 2026-11-08)
- ~~**Q5** A *can't unlock* report makes the bike broken (DE-50, DE-52) while a failed unlock alone doesn't (DE-15). Intended?~~ Yes, a rider's report is a fleet issue and the bike is broken; a single failed unlock signal is not a report (role-played domain expert, DE-85, 2026-11-08)

## Notes for the plan

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run); the intent had no technical notes.
- A withdrawal that fleet operations asked for itself (reason *requested*) is not a new fault report (the fleet-maintenance canvas assumption), so R2 doesn't loop back into R1.
