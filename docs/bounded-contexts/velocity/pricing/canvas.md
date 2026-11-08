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
- **Valid from**: the moment from which a tariff applies to newly started sessions; it lies in the future when the tariff is published (DE-96).
- **Launch tariff**: the tariff VeloCity starts with (DE-30): 15 min free; PLN 3.00, 5.00, 7.00, then 9.00 per hour; drop zone fee PLN 2.00; unauthorized zone return fee PLN 40.00; penalty PLN 150.00; valid from 2026-01-01.
- **Zone map**: the City's drop zones valid at a moment; Pricing uses the one valid at the end of the session (DE-84). A position on a zone's border is inside.

## Business Decisions

### A ride within the free period is free (spec: finance-prices-ride-by-tiered-tariff#E1.2)
- **Given** the launch tariff (free period 15 min)
- **When** a session from 10:00 to 10:15 ending at a station is priced
- **Then** the total fee is PLN 0.00

### Every started billing hour is charged (spec: finance-prices-ride-by-tiered-tariff#E2.1)
- **Given** the launch tariff (hour 1 PLN 3.00)
- **When** a session from 10:00 to 10:15:01 ending at a station is priced
- **Then** the total fee is PLN 3.00 (DE-31)

### Hourly fees are tiered (spec: finance-prices-ride-by-tiered-tariff#E3.2)
- **Given** the launch tariff (PLN 3.00, 5.00, 7.00, then 9.00 per hour)
- **When** a session from 10:00 to 14:20 ending at a station is priced
- **Then** the hourly fees are 3.00 + 5.00 + 7.00 + 9.00 + 9.00 and the total fee is PLN 33.00 (4 h 5 min after the free period: 5 started hours)

### A free-floating return in a drop zone adds the drop zone fee (spec: finance-prices-free-floating-return-by-zone#E1.1)
- **Given** the launch tariff and drop zone Old Town Square around (52.2297, 21.0122), radius 150 m
- **When** a session from 10:00 to 10:30 ending free-floating at (52.2298, 21.0123) is priced
- **Then** the total fee is PLN 3.00 + PLN 2.00 = PLN 5.00

### A free-floating return outside every drop zone adds the unauthorized zone return fee (spec: finance-prices-free-floating-return-by-zone#E2.1)
- **Given** the launch tariff
- **When** a session from 10:00 to 10:30 ending free-floating at (52.2500, 21.1000) is priced
- **Then** the total fee is PLN 3.00 + PLN 40.00 = PLN 43.00

### A quick free-floating trip still pays the return fee (spec: finance-prices-free-floating-return-by-zone#E1.2)
- **Given** the launch tariff
- **When** a session from 10:00 to 10:10 ending free-floating at (52.2298, 21.0123) is priced
- **Then** the total fee is PLN 2.00: no hourly fee, the drop zone fee (DE-84)

### A lock failure costs nothing extra (spec: finance-prices-ride-by-tiered-tariff#E5.2, finance-prices-free-floating-return-by-zone#E3.1)
- **Given** the launch tariff
- **When** a session from 10:00 to 10:30 that ended in a lock failure at (52.2500, 21.1000) is priced
- **Then** the total fee is PLN 3.00: no return fee (DE-32)

### Exceeding the maximum rental time adds the penalty (spec: finance-penalizes-overdue-rental#E1.1)
- **Given** the launch tariff
- **When** a session from 08:00 to 20:00 ended for exceeding the maximum rental time is priced
- **Then** the total fee is 3 + 5 + 7 + 9 × 9 = PLN 96.00 for 12 started billing hours (11 h 45 min) plus the PLN 150.00 penalty = PLN 246.00 (DE-33)

### The tariff valid at session start applies (spec: finance-prices-ride-by-tiered-tariff#E4.1)
- **Given** the launch tariff, and a tariff valid from 12:00 with hour 1 at PLN 4.00
- **When** a session from 11:50 to 12:30 ending at a station is priced
- **Then** the total fee is PLN 3.00 (launch tariff)

### R1 A complete tariff valid from a future moment is published (spec: pricing-manager-publishes-tariff#E1.1)
- **Given** it is 2026-11-08 10:00
- **When** pricing manager Paula publishes T-2 valid from 2026-12-01 00:00
- **Then** T-2 is published and is listed among the tariffs with its valid from (Tariff Published)

### R2 A session that started before a new tariff keeps the old one (spec: pricing-manager-publishes-tariff#E2.1)
- **Given** T-2 was published valid from 2026-12-01 00:00
- **When** a session from 2026-11-30 23:50 to 2026-12-01 00:30 ending at a station is priced
- **Then** the launch tariff applies and the total fee is PLN 3.00

### R2 A session that started after a new tariff uses it (spec: pricing-manager-publishes-tariff#E2.2)
- **Given** T-2 (hour 1 PLN 3.50) valid from 2026-12-01 00:00
- **When** a session from 2026-12-01 00:05 to 00:40 ending at a station is priced
- **Then** T-2 applies and the total fee is PLN 3.50

### R3 No retroactive tariff (spec: pricing-manager-publishes-tariff#E3.1)
- **Given** it is 2026-11-08 10:00 and a session started at 09:30
- **When** Paula publishes T-3 valid from 2026-11-08 09:00
- **Then** the publication is rejected and the session keeps the launch tariff

### R3 Valid from "now" is not in the future (spec: pricing-manager-publishes-tariff#E3.2)
- **Given** it is 2026-11-08 10:00
- **When** Paula publishes T-3 valid from 2026-11-08 10:00
- **Then** the publication is rejected

### R4 A tariff without a penalty is rejected (spec: pricing-manager-publishes-tariff#E4.1)
- **Given** it is 2026-11-08 10:00
- **When** Paula publishes T-4 valid from 2026-12-01 00:00 with every price of T-2 except the penalty
- **Then** the publication is rejected

### R4 Fees can't be negative (spec: pricing-manager-publishes-tariff#E4.2)
- **Given** it is 2026-11-08 10:00
- **When** Paula publishes T-4 valid from 2026-12-01 00:00 with a drop zone fee of PLN -1.00
- **Then** the publication is rejected (DE-96)

### R5 Two tariffs can't share a valid from (spec: pricing-manager-publishes-tariff#E5.1)
- **Given** T-2 was published valid from 2026-12-01 00:00
- **When** on 2026-11-08 at 11:00 Paula publishes T-5 valid from 2026-12-01 00:00
- **Then** the publication is rejected and T-2 stays the tariff valid from that moment

### R1 A short ride is free (spec: finance-prices-ride-by-tiered-tariff#E1.1)
- **Given** the launch tariff
- **When** a session from 10:00 to 10:12 ending at a station is priced
- **Then** the hourly fees are PLN 0.00 and the total fee is PLN 0.00

### R2 75 minutes is still one billing hour (spec: finance-prices-ride-by-tiered-tariff#E2.2)
- **Given** the launch tariff
- **When** a session from 10:00:00 to 11:15:00 ending at a station is priced
- **Then** it has 1 billing hour and the total fee is PLN 3.00

### R2 One second more starts billing hour 2 (spec: finance-prices-ride-by-tiered-tariff#E2.3)
- **Given** the launch tariff
- **When** a session from 10:00:00 to 11:15:01 ending at a station is priced
- **Then** it has 2 billing hours, hourly fees PLN 3.00 + 5.00 and the total fee is PLN 8.00

### R3 Three billing hours (spec: finance-prices-ride-by-tiered-tariff#E3.1)
- **Given** the launch tariff
- **When** a session from 10:00 to 13:15 ending at a station is priced
- **Then** it has 3 billing hours, hourly fees PLN 3.00 + 5.00 + 7.00 and the total fee is PLN 15.00

### R4 A session started after a new tariff uses it (spec: finance-prices-ride-by-tiered-tariff#E4.2)
- **Given** the launch tariff and T-2 valid from 12:00 with hour 1 at PLN 4.00
- **When** a session from 12:05 to 12:40 ending at a station is priced
- **Then** T-2 applies and the total fee is PLN 4.00

### R5 A station return pays no return fee (spec: finance-prices-ride-by-tiered-tariff#E5.1)
- **Given** the launch tariff
- **When** a session from 10:00 to 10:30 ending at a station is priced
- **Then** the return fee is PLN 0.00 and the total fee is PLN 3.00

### R1 A position on a drop zone's border is inside (spec: finance-prices-free-floating-return-by-zone#E1.3)
- **Given** the launch tariff
- **When** a session from 10:00 to 10:30 ending free-floating at (52.23104, 21.0122), 149 m from the centre of Old Town Square (radius 150 m), is priced
- **Then** the return fee is the drop zone fee PLN 2.00 and the total fee is PLN 5.00 (DE-84)

### R2 The zone map valid at the end of the session applies (spec: finance-prices-free-floating-return-by-zone#E2.2)
- **Given** Old Town Square was removed from the zone map at 10:20
- **When** a session from 10:00 to 10:30 ending free-floating at (52.2298, 21.0123) is priced
- **Then** the return fee is the unauthorized zone return fee PLN 40.00 and the total fee is PLN 43.00 (DE-84)

### R3 A lock failure inside a drop zone adds no drop zone fee (spec: finance-prices-free-floating-return-by-zone#E3.2)
- **Given** the launch tariff
- **When** a session from 10:00 to 10:30 that ended in a lock failure at (52.2298, 21.0123) is priced
- **Then** no drop zone fee is added and the total fee is PLN 3.00

### R4 The return fee comes from the tariff valid at session start (spec: finance-prices-free-floating-return-by-zone#E4.1)
- **Given** the launch tariff and T-2 valid from 12:00 with a drop zone fee of PLN 3.00
- **When** a session from 11:50 to 12:30 ending free-floating at (52.2298, 21.0123) is priced
- **Then** the return fee is PLN 2.00 and the total fee is PLN 5.00

### R4 A session started under a new tariff pays its return fee (spec: finance-prices-free-floating-return-by-zone#E4.2)
- **Given** the same T-2
- **When** a session from 12:05 to 12:40 ending free-floating at (52.2298, 21.0123) is priced
- **Then** the return fee is PLN 3.00 and the total fee is PLN 6.00

### R1 An overdue session noticed late is priced to the 12-hour mark (spec: finance-penalizes-overdue-rental#E1.2)
- **Given** a session started at 08:00:00 whose overrun was noticed at 20:20 and which Rentals therefore ended at the 12-hour mark, 20:00:00, for exceeding the maximum rental time
- **When** it is priced
- **Then** the hourly fees are PLN 96.00, the penalty PLN 150.00 and the total fee PLN 246.00 (DE-82)

### R2 An overdue session pays no return fee (spec: finance-penalizes-overdue-rental#E2.1)
- **Given** a session from 08:00:00 to 20:00:00 ended for exceeding the maximum rental time, with the bike last at (52.2500, 21.1000) outside every drop zone
- **When** it is priced
- **Then** the return fee is PLN 0.00 and the total fee is PLN 246.00

### R3 Exactly 12 hours ending at a station is not overdue (spec: finance-penalizes-overdue-rental#E3.1)
- **Given** the launch tariff
- **When** a session from 08:00:00 to 20:00:00 ending at a station is priced
- **Then** the hourly fees are PLN 96.00, there is no penalty and the total fee is PLN 96.00

### R4 The penalty comes from the tariff valid at session start (spec: finance-penalizes-overdue-rental#E4.1)
- **Given** T-2 valid from 12:00 with a penalty of PLN 200.00
- **When** a session from 08:00:00 to 20:00:00 ended for exceeding the maximum rental time is priced
- **Then** the penalty is PLN 150.00 and the total fee is PLN 246.00

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
