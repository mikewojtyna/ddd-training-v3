# Bounded Context Canvas: Pricing

## Name
Pricing

## Purpose
Turns a finished ride into a fair, predictable fee. Riders are encouraged to take short trips (free period),
long trips get progressively more expensive, and the rider always pays the price list that was valid when the ride started.

## Strategic Classification
- Domain: core
- Business model: revenue
- Evolution: custom built

## Domain Roles
- Execution Model: calculates the total fee for every rental session.

## Inbound Communication
| Collaborator | Type | Message | Notes |
|---|---|---|---|
| Rentals | Event | Rental Session Ended | carries session id, rider id, start and end time |
| Back office | Command | Publish Price List | new price list valid from a given moment |
| Rider app | Query | Estimate Fee | rider wants to know how much a ride of N minutes would cost |

## Outbound Communication
| Message | Type | Collaborator | ACL? |
|---|---|---|---|
| Ride Priced | Event | Settlement | n/a |
| Get Return Charges | Query | Rentals | yes |

## Ubiquitous Language
- **Price List**: set of fees (free period length, first/second/third hour fee, subsequent hourly fee) valid from a given moment.
- **Free Period**: the initial minutes of a rental session during which no fee is applied.
- **Billing Period**: one started hour of a ride after the free period.
- **Total Fee**: sum of billing period fees plus return charges.
- **Return Charges**: drop-zone or unauthorized-zone charges reported by Rentals.

## Business Decisions
### Free ride
- **Given** a price list with a 10 minute free period
- **When** a ride of 8 minutes is priced
- **Then** the total fee is 0.00 PLN

### First hour
- **Given** a price list with a 10 minute free period and first-hour fee 5.00 PLN
- **When** a ride of 45 minutes is priced
- **Then** the total fee is 5.00 PLN

### Tiered hours
- **Given** a price list with a 10 minute free period, first-hour fee 5.00 PLN, second-hour fee 7.00 PLN, third-hour fee 9.00 PLN and subsequent hourly fee 12.00 PLN
- **When** a ride of 3 hours 20 minutes is priced
- **Then** the total fee is 33.00 PLN

### Price valid at ride start
- **Given** price list A (first-hour fee 5.00 PLN) valid from 08:00 and price list B (first-hour fee 6.00 PLN) valid from 09:00
- **When** a ride that started at 08:50 and ended at 09:30 is priced
- **Then** the total fee is 5.00 PLN

### Billing hours start after the free period
<!-- Added in plan review, 2026-11-01: suggested in the plan (Q3), accepted by the reviewer. -->
- **Given** a price list with a 10 minute free period, first-hour fee 5.00 PLN and second-hour fee 7.00 PLN
- **When** a ride of 65 minutes is priced
- **Then** the total fee is 5.00 PLN

### Return charges
<!-- Added in plan review, 2026-11-01: given by the reviewer. -->
- **Given** a price list with a 10 minute free period and first-hour fee 5.00 PLN
- **When** a ride of 45 minutes with a 3.00 PLN drop-zone return charge is priced
- **Then** the total fee is 8.00 PLN

### No price list valid at ride start
<!-- Added in plan review, 2026-11-01: suggested in the plan (Q5), accepted by the reviewer. -->
- **Given** a price list valid from 08:00
- **When** a ride that started at 07:50 and ended at 08:30 is priced
- **Then** pricing is rejected because no price list was valid at ride start

### Republished price list replaces the earlier one
<!-- Added in plan review, 2026-11-01: suggested in the plan (Q5), accepted by the reviewer. -->
- **Given** price list A (first-hour fee 5.00 PLN) valid from 08:00, and then price list B (first-hour fee 6.00 PLN) published with the same valid-from 08:00
- **When** a ride that started at 08:30 and ended at 09:00 is priced
- **Then** the total fee is 6.00 PLN

## Assumptions
- Return charges are always known by the time the session ends.

## Verification Metrics
- Number of pricing disputes raised in Customer Support per 1000 rides.

## Open Questions
- Does the maximum-rental-time penalty belong to Pricing or Rentals?
