# Bounded Context Canvas: Rentals

## Name
Rentals

## Purpose
Owns bike availability at stations, renting and returning bikes, and the maximum rental time,
so riders can always find and return a bike and bikes are never held forever.

## Strategic Classification
- Domain: core
- Business model: revenue
- Evolution: custom built

## Domain Roles
- Execution Model

## Inbound Communication
| Collaborator | Type | Message | Notes |
|---|---|---|---|
| Rider app | Command | Rent Bike | existing |
| Rider app | Command | Return Bike | NEW |
| Rider app | Query | Available Bikes At Station | NEW |

## Outbound Communication
| Message | Type | Collaborator | ACL? |
|---|---|---|---|
| Bike Returned | Event | Pricing | n/a |

## Ubiquitous Language
- **Bike Station**: a physical dock holding bikes.
- **Bike**: a rentable bicycle identified by its id.
- **Borrower**: the rider renting a bike.
- **Rental Duration**: requested length of a rental; max 11 hours.

## Business Decisions
### Rent available bike
- **Given** a bike station with 1 available bike
- **When** George rents a bike for 6 hours
- **Then** the bike is rented

### Max rental duration
- **Given** a bike station with 1 available bike
- **When** George rents a bike for 12 hours
- **Then** the rental is rejected

### Return rented bike (NEW)
- **Given** a bike station where bike "bike01" was rented by George
- **When** bike "bike01" is returned to the station
- **Then** the bike is returned and is available again

### Return bike that is not rented (NEW)
- **Given** a bike station where bike "bike01" is available
- **When** bike "bike01" is returned to the station
- **Then** the return is rejected

### No available bike (restored from the existing code, see plan Q4)
- **Given** a bike station with 0 available bikes
- **When** George rents a bike for 6 hours
- **Then** the rental is rejected
