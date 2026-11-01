# Bounded Context Canvas: Accounts

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Saved on 2026-11-01 from the user's inline request, structured into the template. Sections the user did not
provide are left out, not guessed. Add them here if you have them; the plan points to this file.
-->

## Name
Accounts

## Purpose
Keeps rider accounts and account blocks so only riders in good standing can rent bikes.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Back office | Command | Block Account | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Account Blocked | Event | Rentals | n/a for events |

## Ubiquitous Language
- **Account**: a rider's VeloCity account, identified by the rider's borrower id.
- **Account Block**: a flag on an account that prevents renting.

## Business Decisions
### Block an active account
- **Given** an active account
- **When** the back office blocks it
- **Then** the account is blocked and Account Blocked is published

### Block an already blocked account
- **Given** a blocked account
- **When** the back office blocks it again
- **Then** the block is rejected

## Additional instructions from the user (not part of the canvas v5 structure)
- Treat Rentals' `BorrowerId` (`pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId`) as a shared
  kernel: use it directly in Accounts' domain, don't wrap it and don't go through Rentals' api. The user acknowledged
  this breaks the context-boundary rule and asked for it anyway.
