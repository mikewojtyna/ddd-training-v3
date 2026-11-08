# Bounded Context Canvas: CustomerSupport

<!--
Written by Claude on 2026-11-08 from docs/domains/velocity/velocity-vision.md ("Customer Support & Incidents"),
docs/domains/velocity/velocity-context-map.md and the CML, during an autonomous run. Values from role-played
domain-expert answers (DE-n).
-->

## Name
CustomerSupport

## Purpose
Makes sure no rider's problem is forgotten: every reported incident is routed by its type, worked by a support
agent and driven to a resolved state, whether it was accepted, escalated, rejected or reopened. Fleet issues are
handed straight to the people who fix bikes.

## Strategic Classification
- Domain: supporting
- Business model: engagement
- Evolution: custom built

## Domain Roles
- Execution Model: runs each incident to resolution.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Rider app | Command | Report Incident (category, description, bike?) | Type detected at once (DE-50) |
| Support agent | Command | Pick Up Incident | Assigns it to the agent (DE-51) |
| Support agent | Command | Accept Incident / Reject Incident | |
| Support agent | Command | Escalate Incident (team) | billing, fleet, safety (DE-53) |
| Support agent | Command | Resolve Incident (resolution) | |
| Rider app | Command | Reopen Incident | Within 14 days of rejection (DE-54) |
| Fleet Maintenance | Command | Resolve Fleet Issue (incident, bike fixed) | OHS; resolves from any open state (DE-55) |
| Rider app, Support agent | Query | Incident | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Fleet Issue Reported (incident, bike, description) | Event | Fleet Maintenance | n/a (PL) |
| Incident Reported, Accepted, Rejected, Escalated, Reopened, Resolved | Event | none outside Customer Support yet | n/a |

## Ubiquitous Language
- **Incident**: something a rider reported: a broken bike, a bike they can't unlock, a wrongful charge, a safety issue, anything else.
- **Category**: what the rider picked when reporting: *broken bike*, *can't unlock*, *wrongful charge*, *safety*, *other*.
- **Incident type**: *fleet issue*, *billing dispute*, *safety concern*, *other*; detected from the category (DE-50).
- **Assigned agent**: the support agent who picked the incident up; only they work it.
- **Accepted / Rejected**: the agent's decision on an assigned incident.
- **Escalated**: an accepted incident handed to the billing, fleet or safety team.
- **Reopened**: a rejected incident the rider pushed back on; it must be resolved, never rejected again.
- **Resolved**: the terminal state every incident reaches.

## Business Decisions

### A broken-bike report is a fleet issue handed to Fleet Maintenance
- **Given** no incidents
- **When** rider Anna reports *broken bike* for B-17: "rear brake doesn't work"
- **Then** incident I-1 is reported with type *fleet issue* (Incident Reported, Fleet Issue Reported)

### A wrongful-charge report is a billing dispute
- **Given** no incidents
- **When** Anna reports *wrongful charge*: "charged twice for one ride"
- **Then** the incident's type is *billing dispute* and nothing is handed to Fleet Maintenance

### Only the assigned agent decides
- **Given** I-1 was picked up by agent Zoe
- **When** agent Max accepts I-1
- **Then** it is rejected; when Zoe accepts it, I-1 is accepted (Incident Accepted)

### An escalated incident is resolved by the team's outcome
- **Given** I-1 is accepted by Zoe
- **When** Zoe escalates it to the *billing* team, then resolves it with "refund issued"
- **Then** I-1 is escalated (Incident Escalated), then resolved (Incident Resolved)

### A rider can reopen a rejected incident within 14 days
- **Given** Zoe rejected Anna's incident I-2 on 2026-11-01
- **When** Anna reopens I-2 on 2026-11-15
- **Then** I-2 is reopened (Incident Reopened); reopening on 2026-11-16 would be rejected (DE-54)

### A reopened incident can't be rejected again
- **Given** I-2 is reopened and assigned to Zoe
- **When** Zoe rejects I-2
- **Then** it is rejected; Zoe has to resolve it

### A fixed bike resolves its fleet issue
- **Given** fleet-issue incident I-1 is accepted
- **When** Fleet Maintenance reports B-17 fixed for I-1
- **Then** I-1 is resolved with "bike fixed" (Incident Resolved) (DE-55)

## Assumptions
- 🎭 DE-50, DE-51, DE-52, DE-53, DE-54, DE-55, DE-56 (a reopened incident stays with its agent; the 14 days are calendar days).
- The context map's "fleet-issue incident" link (CustomerSupport → FleetMaintenance) is confirmed by DE-52.

## Verification Metrics
- Time from report to resolution, per type.
- Share of rejected incidents that get reopened.

## Open Questions
- Billing disputes should pause debt recovery (reference map: deferred).
- Is a safety concern escalated automatically?
