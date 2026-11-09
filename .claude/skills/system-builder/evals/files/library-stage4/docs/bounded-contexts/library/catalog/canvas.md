# Bounded Context Canvas: Catalog

## Name
Catalog

## Purpose
See the CML domain vision statement of Catalog in src/main/cml/Library-Context-Map.cml.

## Ubiquitous Language
See the contract catalogue (docs/bounded-contexts/library/integration-contracts.md); terms as in the CML.

## Business Decisions
### R1 A reference-only copy is not lendable
Given copy C-1 added as reference-only, when Lending asks for its availability, then lendable is false.
