# Bounded Context Canvas: Fines

## Name
Fines

## Purpose
See the CML domain vision statement of Fines in src/main/cml/Library-Context-Map.cml.

## Ubiquitous Language
See the contract catalogue (docs/bounded-contexts/library/integration-contracts.md); terms as in the CML.

## Business Decisions
### R1 Late fee is PLN 0.50 per day, at most PLN 20.00 (DE-4)
Given Book Returned Late with 3 days late, then a fee of PLN 1.50 is assessed and charged.
Given Book Returned Late with 60 days late, then a fee of PLN 20.00 is assessed.
### R2 A declined charge leaves the fee outstanding
Given the member's card is declined, when the fee is charged, then the fee stays outstanding.
