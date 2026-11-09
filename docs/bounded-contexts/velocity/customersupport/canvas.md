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
| Support (clock) | Command | Resolve Uncontested Rejections | When the 14-day window has closed (DE-87) |
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
- **Open**: reported and not yet picked up by any support agent.
- **Assigned agent**: the support agent who picked the incident up; only they work it.
- **Accepted / Rejected**: the agent's decision on an assigned incident.
- **Escalated**: an accepted incident handed to the billing, fleet or safety team.
- **Reopened**: a rejected incident the rider pushed back on; it must be resolved, never rejected again.
- **Reopenable until**: the end of the 14th calendar day after the day of the rejection, in the city's local time (DE-54, DE-56; spec Q2 open).
- **Resolved**: the terminal state every incident reaches.

## Business Decisions

### A broken-bike report is a fleet issue handed to Fleet Maintenance (spec: rider-reports-incident#E3.1, #E3.3)
- **Given** no incidents
- **When** rider Anna reports *broken bike* for B-17: "rear brake doesn't work"
- **Then** incident I-1 is reported with type *fleet issue* (Incident Reported, Fleet Issue Reported)

### A wrongful-charge report is a billing dispute (spec: rider-reports-incident#E1.1, #E3.2)
- **Given** no incidents
- **When** Anna reports *wrongful charge*: "charged twice for one ride"
- **Then** the incident's type is *billing dispute* and nothing is handed to Fleet Maintenance

### Only the assigned agent decides (spec: support-agent-decides-on-incident#E1.1, #E2.1, #E3.1)
- **Given** I-1 was picked up by agent Zoe
- **When** agent Max accepts I-1
- **Then** Max's acceptance is refused; when Zoe accepts it, I-1 is accepted (Incident Accepted)

### An escalated incident is resolved by the team's outcome (spec: support-agent-escalates-incident#E1.1, support-agent-resolves-incident#E2.1)
- **Given** I-1 is accepted by Zoe
- **When** Zoe escalates it to the *billing* team, then resolves it with "refund issued"
- **Then** I-1 is escalated (Incident Escalated), then resolved (Incident Resolved)

### A rider can reopen a rejected incident within 14 days (spec: rider-reopens-rejected-incident#E1.1, #E1.2)
- **Given** Zoe rejected Anna's incident I-2 on 2026-11-01
- **When** Anna reopens I-2 on 2026-11-15
- **Then** I-2 is reopened (Incident Reopened); reopening on 2026-11-16 would be rejected (DE-54)

### A reopened incident can't be rejected again (spec: support-agent-decides-on-incident#E4.2, support-agent-resolves-incident#E3.1)
- **Given** I-2 is reopened and assigned to Zoe
- **When** Zoe rejects I-2
- **Then** the rejection is refused; Zoe has to accept or resolve it (DE-97)

### An uncontested rejection is resolved when the window closes (spec: support-agent-resolves-incident#E6.1)
- **Given** Zoe rejected Anna's incident I-3 on 2026-11-01 and Anna didn't reopen it
- **When** open incidents are checked on 2026-11-16
- **Then** I-3 is resolved with "rejection not contested" (Incident Resolved) (DE-87); checked on 2026-11-15 it is still rejected

### A fixed bike resolves its fleet issue (spec: technician-returns-fixed-bike-to-circulation#E3.1)
- **Given** fleet-issue incident I-1 is accepted
- **When** Fleet Maintenance reports B-17 fixed for I-1
- **Then** I-1 is resolved with "bike fixed" (Incident Resolved) (DE-55)
- *Several and already resolved:* I-1 (accepted by Zoe) and I-2 (reported) about B-17; Fleet Maintenance reports B-17 fixed → both resolved with "bike fixed"; an incident Zoe already resolved with "brake adjusted on site" keeps her resolution and no second Incident Resolved (spec: technician-returns-fixed-bike-to-circulation#E3.1, #E3.2, DE-55).

### The incident type follows the category (spec: rider-reports-incident#E2.1, #E2.2, #E2.3)
- *can't unlock* for B-22 → *fleet issue* (Fleet Issue Reported); *safety* → *safety concern*; *broken bike* without a bike → *other*, nothing handed over (DE-50).

### A blocked rider can report an incident (spec: rider-reports-incident#E1.2)
- **Given** Ben's account is blocked for *outstanding debt*
- **When** Ben reports *other* with "the app shows a ride I didn't take"
- **Then** incident I-8 is reported for Ben with type *other* (Incident Reported) (DE-97)

### An incident is picked up once (spec: support-agent-decides-on-incident#E1.2)
- **Given** I-2 is assigned to Zoe
- **When** support agent Max picks up I-2
- **Then** the pick-up is rejected and I-2 stays assigned to Zoe

### Nobody decides on an incident before it's picked up (spec: support-agent-decides-on-incident#E3.2)
- **Given** Ben's incident I-4 (*safety concern*) is open
- **When** Zoe accepts I-4 without picking it up
- **Then** the acceptance is rejected and I-4 stays open

### A rejection carries a reason (spec: support-agent-decides-on-incident#E2.2)
- **Given** I-6 is assigned to Zoe
- **When** Zoe rejects it with "the PLN 1.00 was a card check, released at once"
- **Then** I-6 is rejected with that reason (Incident Rejected) (DE-97)

### An accepted incident isn't decided again (spec: support-agent-decides-on-incident#E4.1)
- **Given** I-2 was accepted by Zoe
- **When** Zoe rejects I-2
- **Then** the rejection is rejected and I-2 stays accepted

### A reopened incident can be accepted (spec: support-agent-decides-on-incident#E2.3)
- **Given** I-6 was rejected by Zoe, reopened by Ben and is assigned to Zoe again
- **When** Zoe accepts I-6
- **Then** I-6 is accepted (Incident Accepted) (DE-97)

### Only an accepted incident is escalated, at most once (spec: support-agent-escalates-incident#E2.1, #E2.2, #E2.3)
- **Given** I-2 is escalated to the *billing* team
- **When** Zoe escalates I-2 to the *fleet* team
- **Then** the escalation is rejected and I-2 stays with *billing* (DE-97). A rejected (E2.1) or undecided (E2.2) incident can't be escalated either.

### Only the assigned agent escalates (spec: support-agent-escalates-incident#E3.1)
- **Given** I-2 was accepted by Zoe
- **When** Max escalates I-2 to *billing*
- **Then** the escalation is rejected and I-2 stays accepted

### Escalation goes to billing, fleet or safety, whatever the type (spec: support-agent-escalates-incident#E4.1, #E4.2)
- **Given** I-7 (*other*) was accepted by Zoe
- **When** Zoe escalates it to a *legal* team
- **Then** the escalation is rejected; to *billing* it is escalated (Incident Escalated) (DE-53, DE-97)

### Only the reporting rider reopens (spec: rider-reopens-rejected-incident#E2.1)
- **Given** Ben's incident I-6 was rejected at 10:00 on 2026-11-09
- **When** Anna reopens I-6 at 12:00 on 2026-11-10
- **Then** the reopening is rejected and I-6 stays rejected

### Only a rejected incident is reopened (spec: rider-reopens-rejected-incident#E3.1, #E3.2)
- **Given** Ben reopened I-6 at 09:00 on 2026-11-12
- **When** Ben reopens I-6 again at 09:30
- **Then** the reopening is rejected and I-6 stays reopened; an accepted incident (E3.1) can't be reopened either

### An accepted incident can be resolved directly (spec: support-agent-resolves-incident#E1.1)
- **Given** Ben's incident I-4 (*safety concern*) was accepted by Zoe
- **When** Zoe resolves I-4 at 15:00 on 2026-11-09 with "talked to Ben, crossing reported to the city"
- **Then** I-4 is resolved with that resolution (Incident Resolved)

### Only the assigned agent resolves (spec: support-agent-resolves-incident#E4.1)
- **Given** I-2 is escalated and assigned to Zoe
- **When** Max resolves I-2 with "refunded"
- **Then** the resolution is rejected and I-2 stays escalated

### Only an accepted, escalated or reopened incident is resolved, once (spec: support-agent-resolves-incident#E5.1, #E5.2, #E5.3)
- **Given** I-6 was rejected by Zoe and not reopened
- **When** Zoe resolves I-6
- **Then** the resolution is rejected and I-6 stays rejected; an undecided (E5.1) or already resolved (E5.3) incident can't be resolved either

### A reopened incident isn't resolved by the clock (spec: support-agent-resolves-incident#E6.2)
- **Given** I-6 was rejected at 10:00 on 2026-11-09 and Ben reopened it at 09:00 on 2026-11-12
- **When** the end of 2026-11-23 passes
- **Then** nothing happens and I-6 stays reopened with Zoe

## Assumptions
- 🎭 DE-50, DE-51, DE-52, DE-53, DE-54, DE-55, DE-56 (a reopened incident stays with its agent; the 14 days are calendar days).
- The context map's "fleet-issue incident" link (CustomerSupport → FleetMaintenance) is confirmed by DE-52.

## Verification Metrics
- Time from report to resolution, per type.
- Share of rejected incidents that get reopened.

## Open Questions
- Billing disputes should pause debt recovery (reference map: deferred).
- Is a safety concern escalated automatically?
