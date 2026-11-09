---
title: Record architecture decisions as ADRs in the repository
status: accepted
date: 2026-11-10
supersedes:
superseded_by:
---

# 0001. Record architecture decisions as ADRs in the repository

## Context

Architecture decisions in this repo are scattered: some live in design-history files, some in commit messages, some
only in people's heads. Newcomers and coding agents ask "why did we choose X?" and nobody can point to an answer. The
agents read the repository, not the wiki, so a decision that isn't in the repo doesn't exist for them.

## Decision drivers

- A newcomer or an agent can find why a decision was made, next to the code it affects.
- A decision can be replaced without losing the history of the old one.
- Writing a record takes minutes, not an afternoon.

## Considered options

### Markdown ADRs in `docs/adr/` (MADR-like)

One numbered file per decision, reviewed in the same pull request as the code. Agents read them like any other file.
Superseding keeps both files.

### Pages in the team wiki

Rich editing and comments, but separate from the code and its review. Agents in the IDE can't read it, and pages
drift from the code without anyone noticing.

### Commit messages and PR descriptions only

No extra files, but the reasoning is spread over many commits, can't be superseded explicitly, and is hard to find
with anything other than `git log -S`.

## Decision

We record architecture decisions as Markdown ADRs in `docs/adr/`, written with the `adr-writer` skill, because it is
the only option that keeps the reasoning next to the code for both newcomers and agents, and keeps history through
explicit superseding. It costs a little more ceremony per decision than commit messages alone.

## Consequences

### Positive

- "Why did we choose X?" has one place to look, for people and agents alike.
- Decisions are reviewed together with the code that implements them.
- `adrs.py` keeps numbering and supersede links consistent, so the log stays trustworthy.

### Negative

- Every significant decision needs a short document and a review, which slows down small changes a little.
- Old ADRs stay in the folder; readers must check the status before relying on one.
- People used to the wiki have to learn a new place and format.

## Links

- Michael Nygard, "Documenting Architecture Decisions" (2011).
- MADR, Markdown Architectural Decision Records: https://adr.github.io/madr/
