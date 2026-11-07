---
name: rider-reserves-bike-at-station        # = file name without .md
sequence: 3                                  # next number across ALL intents (max + 1); never reused
title: A rider wants to reserve a bike at a bike station
system: velocity                             # top-level system package: velocity | crowdsorcery | ...
actor: Rider                                 # a person or business role, never "the system"; reuse names from story-map.md
activity: Get a bike                         # backbone activity from story-map.md, spelled exactly the same
task: Reserve a bike                         # user task under the activity
status: draft                                # draft | accepted | specified | planned | delivered | superseded | dropped
date: 2026-11-01                             # YYYY-MM-DD the intent was captured (or delivered, when backfilling)
supersedes:                                  # name of the intent this replaces, or empty
superseded_by:                               # filled in only on the OLD intent when a new one replaces it
source: docs/domains/velocity/velocity-vision.md "Renting a Bike"; conversation 2026-11-01
---

# A rider wants to reserve a bike at a bike station

## Intent

As a **Rider**, I want to **reserve a bike at a bike station** so that **nobody else takes it in the moment between choosing it and unlocking it**.

## Why

<!-- The business reason and the trigger: complaint, opportunity, regulation, vision section, changed policy.
     For a superseding intent: what changed compared to the old intent and why.
     For a backfilled intent: write "Backfilled: this behaviour already exists." plus the reason it exists. -->

## Business outcome

<!-- Past-tense business facts that are true once the intent is met, in the actor's language.
     These are what you read across intents to see the value stream. -->
- Bike Reserved: the rider holds one specific bike at the station.

## Success signal

<!-- How the business will know it worked. Mark guesses with "(suggested)". -->
- (suggested) Fewer support contacts about a bike being taken during unlocking.

## Business rules mentioned

<!-- Rules as the user / vision stated them, verbatim or close to it. Do NOT turn them into Given/When/Then
     and do NOT invent thresholds — the spec does that. Write "None stated." if none. -->
- "The reservation is short-lived" (vision). Duration not stated, see open questions.

## Scope

- **In:** reserving one available bike at a station.
- **Out:** cancelling a reservation (`rider-cancels-reservation`, candidate); releasing an expired reservation (`fleet-releases-expired-reservation`, candidate); unlocking the bike (`rider-rents-bike-from-station`).

## Related intents

<!-- - builds on: <intent-name> | enables: <intent-name> | follows: <intent-name> | alternative to: <intent-name> -->
- builds on: `rider-rents-bike-from-station`

## Open questions

- How long does a reservation last?

## Notes for the spec

<!-- Technical hints, constraints or solution ideas the user mentioned. The only section where technical words belong. -->
- None.
