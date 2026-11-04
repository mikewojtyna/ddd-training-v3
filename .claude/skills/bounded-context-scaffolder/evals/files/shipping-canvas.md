# Bounded Context Canvas: Shipping

## Name
Shipping

## Purpose
Gets paid orders to customers quickly and at a predictable cost, so customers trust that what they ordered arrives and know upfront what delivery costs.

## Strategic Classification
- Domain: supporting
- Business model: revenue
- Evolution: custom built

## Domain Roles
- Execution Model: runs every delivery from start to hand-over.

## Inbound Communication
| Collaborator | Type | Message | Notes |
|---|---|---|---|
| Ordering | Event | Order Paid | carries order id and order total |
| Warehouse app | Command | Add Parcel To Shipment | |
| Customer app | Query | Quote Delivery Fee | customer sees the fee before paying |

## Outbound Communication
| Message | Type | Collaborator | ACL? |
|---|---|---|---|
| Delivery Started | Event | Ordering | n/a |

## Ubiquitous Language
- **Shipment**: the parcels travelling together to one customer for one order.
- **Parcel**: one box in a shipment.
- **Delivery**: the process of getting a shipment from the warehouse to the customer.
- **Delivery fee**: what the customer pays for delivery, depending on the order total.

## Business Decisions
### A shipment holds at most 5 parcels
- **Given** a shipment with 5 parcels
- **When** the warehouse adds another parcel
- **Then** adding the parcel is rejected

### Paid orders start their delivery
- **Given** order O1 has no delivery yet
- **When** order O1 is paid
- **Then** the delivery of order O1 is started

### Delivery is free above 50 PLN
- **Given** an order total of 60.00 PLN
- **When** the delivery fee is quoted
- **Then** the delivery fee is 0.00 PLN

- **Given** an order total of 40.00 PLN
- **When** the delivery fee is quoted
- **Then** the delivery fee is 9.99 PLN
