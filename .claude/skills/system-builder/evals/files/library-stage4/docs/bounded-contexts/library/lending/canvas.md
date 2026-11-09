# Bounded Context Canvas: Lending

## Name
Lending

## Purpose
See the CML domain vision statement of Lending in src/main/cml/Library-Context-Map.cml.

## Ubiquitous Language
See the contract catalogue (docs/bounded-contexts/library/integration-contracts.md); terms as in the CML.

## Business Decisions
### R1 Only lendable copies can be borrowed
Given copy C-1 is reference-only in Catalog, when member M-1 borrows it, then the loan is rejected (CopyNotLendable).
### R2 A loan lasts 21 days (DE-2)
Given M-1 borrows C-2 on 2026-11-09, then the loan is due on 2026-11-30.
### R3 A late return is published
Given a loan due on 2026-11-30, when the copy is returned on 2026-12-03, then Book Returned Late is published with 3 days late.
