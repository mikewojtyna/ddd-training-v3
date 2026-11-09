---
title: Use PostgreSQL for the simulation-results index   # the decision, as a short imperative phrase
status: proposed           # proposed | accepted | superseded | deprecated  (changed only by scripts/adrs.py)
date: 2026-11-10           # YYYY-MM-DD the ADR was written or last revised
supersedes:                # number of the ADR this one replaces, e.g. 3; empty if none
superseded_by:             # set by 'adrs.py supersede'; never by hand
---

# 0007. Use PostgreSQL for the simulation-results index

<!-- The file is docs/adr/NNNN-<slug>.md. NNNN is the next free number (adrs.py check reports gaps and duplicates).
     The heading repeats the number and the title. -->

## Context

<!-- The problem and the forces at play, in a few sentences: what happens today, what hurts, what constrains us
     (load, team skills, cost, regulation, deadlines). Describe the problem, not the chosen solution:
     "we need X because Y", not "we will use Z". -->

## Decision drivers

<!-- 2-5 bullets: the criteria the options are judged by. The Decision must refer back to them.
     Use the user's words and numbers; an unknown number is "?" plus an entry under Links/Open questions. -->
- <driver 1, e.g. "ad-hoc queries over 200 M rows return in under 2 s">
- <driver 2>

## Considered options

<!-- At least two real options, each as "### <name>" with 1-3 lines on how it fares against the drivers.
     "Do nothing / keep the current setup" counts as a real option when it is one. A straw man doesn't. -->

### <Option A>

### <Option B>

## Decision

<!-- "We chose <option>, because <drivers it satisfies best>." One short paragraph. Name the drivers it trades off. -->

## Consequences

<!-- What becomes easier and what becomes harder. Both subsections must have content: every decision has a cost. -->

### Positive

- <...>

### Negative

- <...>

## Links

<!-- Related ADRs ("Supersedes ADR-0003", "Superseded by ADR-0009"), specs, tickets, benchmarks, open questions. -->
