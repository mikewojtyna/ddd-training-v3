# Bounded Context Canvas: Pricing

<!--
Written by Claude on 2026-11-08 from docs/domains/velocity/velocity-vision.md ("Billing", "Returns"),
docs/domains/velocity/velocity-context-map.md and the CML, during an autonomous run. Values from role-played
domain-expert answers (DE-n), see docs/bounded-contexts/velocity/domain-expert-answers.md.
-->

## Name
Pricing

## Purpose
Turns a finished rental session into a fair, explainable total fee: cheap short trips, progressively more expensive
long ones, a clear signal for breaking network rules. It is a pure calculator: it moves no money and keeps no
rides, only tariffs.

## Strategic Classification
- Domain: core
- Business model: revenue
- Evolution: custom built

## Domain Roles
- Execution Model: performs one job, calculating and explaining the fee from the facts it is given.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Settlement | Query | Price for Session (started at, ended at, ending, position) | OHS/PL; uses the tariff valid at session start |
| Pricing manager | Command | Publish Tariff (valid from) | A new tariff never affects rides started before it |
| Pricing manager | Query | Tariffs | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Zone at position | Query | City (zone map, external) | yes (`cityacl`); the map now says `[D,ACL]` too |

## Ubiquitous Language
- **Tariff**: the prices valid from a point in time: free period, hourly fees per hour, drop zone fee, unauthorized zone return fee, penalty.
- **Free period**: the first 15 minutes of a session, free of charge (DE-30).
- **Billing hour**: each started hour after the free period (DE-31). Hours 1, 2, 3 have their own fee; every hour from the 4th on costs the same.
- **Hourly fee**: the fee of one billing hour.
- **Ending**: how the session ended: *station*, *free-floating*, *lock failure*, *maximum rental time*.
- **Drop zone**: a city-approved area for free-floating returns.
- **Unauthorized zone**: any position outside every drop zone.
- **Return fee**: drop zone fee or unauthorized zone return fee; only for free-floating endings (DE-34).
- **Penalty**: charged when the maximum rental time was exceeded (DE-33).
- **Total fee**: hourly fees + return fee + penalty.

## Business Decisions

### A ride within the free period is free
- **Given** the launch tariff (free period 15 min)
- **When** a session from 10:00 to 10:15 ending at a station is priced
- **Then** the total fee is PLN 0.00

### Every started billing hour is charged
- **Given** the launch tariff (hour 1 PLN 3.00)
- **When** a session from 10:00 to 10:15:01 ending at a station is priced
- **Then** the total fee is PLN 3.00 (DE-31)

### Hourly fees are tiered
- **Given** the launch tariff (PLN 3.00, 5.00, 7.00, then 9.00 per hour)
- **When** a session from 10:00 to 14:20 ending at a station is priced
- **Then** the hourly fees are 3.00 + 5.00 + 7.00 + 9.00 + 9.00 and the total fee is PLN 33.00 (4 h 5 min after the free period: 5 started hours)

### A free-floating return in a drop zone adds the drop zone fee
- **Given** the launch tariff and drop zone Old Town Square around (52.2297, 21.0122), radius 150 m
- **When** a session from 10:00 to 10:30 ending free-floating at (52.2298, 21.0123) is priced
- **Then** the total fee is PLN 3.00 + PLN 2.00 = PLN 5.00

### A free-floating return outside every drop zone adds the unauthorized zone return fee
- **Given** the launch tariff
- **When** a session from 10:00 to 10:30 ending free-floating at (52.2500, 21.1000) is priced
- **Then** the total fee is PLN 3.00 + PLN 40.00 = PLN 43.00

### A quick free-floating trip still pays the return fee
- **Given** the launch tariff
- **When** a session from 10:00 to 10:10 ending free-floating at (52.2298, 21.0123) is priced
- **Then** the total fee is PLN 2.00: no hourly fee, the drop zone fee (DE-84)

### A lock failure costs nothing extra
- **Given** the launch tariff
- **When** a session from 10:00 to 10:30 that ended in a lock failure at (52.2500, 21.1000) is priced
- **Then** the total fee is PLN 3.00: no return fee (DE-32)

### Exceeding the maximum rental time adds the penalty
- **Given** the launch tariff
- **When** a session from 08:00 to 20:00 ended for exceeding the maximum rental time is priced
- **Then** the total fee is 3 + 5 + 7 + 9 × 9 = PLN 96.00 for 12 started billing hours (11 h 45 min) plus the PLN 150.00 penalty = PLN 246.00 (DE-33)

### The tariff valid at session start applies
- **Given** the launch tariff, and a tariff valid from 12:00 with hour 1 at PLN 4.00
- **When** a session from 11:50 to 12:30 ending at a station is priced
- **Then** the total fee is PLN 3.00 (launch tariff)

## Assumptions
- 🎭 DE-30: launch tariff values.
- 🎭 DE-31, DE-83: started hours are charged, measured to the second, resolving the vision's "when it completes".
- 🎭 DE-84: a position on a zone border is inside; the zone map valid at the end of the ride applies.
- 🎭 DE-96: a tariff's valid-from lies in the future when published; two tariffs with the same valid-from are rejected.
- Money is in PLN only.
- The zone map comes from the City; until a real feed exists, it is a configured list of circular drop zones.

## Verification Metrics
- Share of rides inside the free period (should be high: short trips are encouraged).
- Share of free-floating returns paying the unauthorized zone fee (should fall over time).

## Open Questions
- Are tariffs ever retracted before they become valid?
- Should a station also be a valid free-floating return (bike left next to a full station)?
