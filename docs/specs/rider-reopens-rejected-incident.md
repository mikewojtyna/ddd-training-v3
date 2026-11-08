---
name: rider-reopens-rejected-incident
intent: rider-reopens-rejected-incident
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A rider wants to reopen an incident that was rejected

Concrete rules, examples and contract for [the intent](../intents/rider-reopens-rejected-incident.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Rejected | The assigned agent turned the rider's report down, with a reason. |
| Reopening | Reactivating a rejected incident so it can be worked again toward a resolution (vision, "Domain Vocabulary"). |
| Reopening window | 14 calendar days from the rejection: rejected on 9 November, reopening is possible until the end of 23 November (DE-54, DE-56). |
| Reopened | A rejected incident the rider pushed back on; it stays with the agent who rejected it and can't be rejected again (DE-54, DE-56). |

## Rules

### R1: The rider can reopen a rejected incident within 14 calendar days of the rejection; it stays with the agent who rejected it

- **E1.1** Given Ben's incident I-6 was rejected by support agent Zoe at 10:00 on 2026-11-09, When Ben reopens I-6 at 23:59 on 2026-11-23, Then I-6 is reopened and assigned to Zoe (Incident Reopened).
- **E1.2** Given I-6 was rejected by Zoe at 10:00 on 2026-11-09, When Ben reopens I-6 at 00:00 on 2026-11-24, Then the reopening is rejected and I-6 stays rejected (Q2).

### R2: Only the rider who reported the incident can reopen it (vision)

- **E2.1** Given Ben's incident I-6 was rejected at 10:00 on 2026-11-09, When Anna reopens I-6 at 12:00 on 2026-11-10, Then the reopening is rejected and I-6 stays rejected.

### R3: Only a rejected incident can be reopened

- **E3.1** Given Anna's incident I-2 was accepted by Zoe, When Anna reopens I-2, Then the reopening is rejected and I-2 stays accepted.
- **E3.2** Given Ben reopened I-6 at 09:00 on 2026-11-12, When Ben reopens I-6 again at 09:30 on 2026-11-12, Then the reopening is rejected and I-6 stays reopened.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Reopen Incident | Rider | incident, rider | Incident Reopened | R1, R2, R3 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| Rider's Incident | Rider | incident, rider | incident type, state, rejection reason and rejected at (if rejected), reopenable until (if rejected) |

### Events

| Event | When | Data |
|-------|------|------|
| Incident Reopened | the rider reopened a rejected incident within the window | incident, rider, assigned agent, reopened at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Reopening | rider | Identifier (Rider) | the rider who reported the incident |
| Incident | reopenable until | Timestamp | end of the 14th calendar day after the rejection day (DE-54, DE-56, Q2) |

## Out of scope

- Rejecting an incident, and the rule that a reopened incident can't be rejected again (`support-agent-decides-on-incident`).
- Resolving a reopened incident (`support-agent-resolves-incident`).
- Resolving a rejected incident automatically when its window passes without a reopening (`support-agent-resolves-incident`, DE-87).

## Open questions

- ~~**Q1** How long can a rider push back on a rejection, and who works a reopened incident?~~ 14 calendar days from the rejection, until the end of the 14th day; it stays assigned to the agent who rejected it (role-played domain expert, DE-54, DE-56, 2026-11-08)
- **Q2** In which time zone does "the end of the day" count (city local time assumed in E1.1, E1.2)? (E1.2, Data)
- ~~**Q3** What happens to a rejected incident the rider never reopens?~~ When the 14-day window ends it is resolved automatically with "rejection not contested" (role-played domain expert, DE-87, 2026-11-08); specified in `support-agent-resolves-incident`
- **Q4** Does the rider give a reason when pushing back? (Contract)

## Notes for the plan

- None.
