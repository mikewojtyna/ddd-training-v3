---
name: collections-agent-pauses-debt-recovery
intent: collections-agent-pauses-debt-recovery
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A collections agent wants to pause a rider's debt recovery

Concrete rules, examples and contract for [the intent](../intents/collections-agent-pauses-debt-recovery.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Debt recovery | The active pursuit of one rider's whole outstanding debt; running, paused, succeeded or exhausted. |
| Running | Collection attempts are made whenever one is due. |
| Paused | No collection attempts are made, for a pause reason, until an agent resumes the recovery. |
| Pause reason | Why the recovery is paused: *dispute*, *payment plan* (being negotiated by phone) or *regulatory hold* (DE-42, DE-43). |
| Failed attempts | How many collection attempts of this recovery failed so far; pausing keeps them (DE-42). |

## Rules

### R1: A collections agent can pause a running recovery, keeping its failed attempts

- **E1.1** Given Anna's recovery is running with 1 failed attempt and its next attempt due at 10:50 on 2026-11-11, When collections agent Kate pauses it for a *dispute* at 14:00 on 2026-11-09, Then Anna's recovery is paused for *dispute* with 1 failed attempt (Recovery Paused).

### R2: A recovery is paused only for a dispute, a payment plan being negotiated, or a regulatory hold

- **E2.1** Given Ben's recovery is running with 3 failed attempts, When Kate pauses it for a *payment plan* at 11:00 on 2026-11-10, Then Ben's recovery is paused for *payment plan* with 3 failed attempts (Recovery Paused).
- **E2.2** Given Ben's recovery is running, When Kate pauses it giving the reason "rider is on holiday", Then the pause is rejected and Ben's recovery keeps running.

### R3: A paused recovery makes no collection attempts

- **E3.1** Given Anna's recovery was paused at 14:00 on 2026-11-09 with 1 failed attempt, and its next attempt would have been due at 10:50 on 2026-11-11, When due collections are attempted at 10:50 on 2026-11-12, Then no attempt is made for Anna, she gets no reminder and her recovery still has 1 failed attempt.

### R4: Only a running recovery can be paused

- **E4.1** Given Anna's recovery was exhausted at 10:50 on 2026-11-20, When Kate pauses it for a *dispute*, Then the pause is rejected and the recovery stays exhausted.
- **E4.2** Given rider Ben has no debt recovery, When Kate pauses Ben's recovery for a *regulatory hold*, Then the pause is rejected.
- **E4.3** Given Anna's recovery is paused for a *dispute*, When Kate pauses it for a *regulatory hold*, Then the pause is rejected and the reason stays *dispute* (DE-89).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Pause Recovery | Collections Agent | rider, pause reason | Recovery Paused | R2, R4 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| Rider's Debt Recovery | Collections Agent | rider | state, failed attempts, next attempt due at (if running), pause reason (if paused), outstanding debt (or none) |

### Events

| Event | When | Data |
|-------|------|------|
| Recovery Paused | a collections agent paused a running recovery | rider, pause reason, paused by, paused at, failed attempts |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Pause | rider | Identifier (Rider) | required; the rider has a running recovery |
| Pause | pause reason | one of {dispute, payment plan, regulatory hold} | required; closed list (DE-42, DE-89) |
| Pause | paused by | Identifier (Collections Agent) | required; any collections agent (DE-89) |

## Out of scope

- Resuming a paused recovery (`collections-agent-resumes-debt-recovery`).
- A debt cleared while the recovery is paused (`collections-attempts-debt-collection`, R5).
- A billing dispute reported to support pausing recovery on its own (reference map: deferred, candidate).
- Negotiating and tracking payment-plan instalments (DE-43: out of scope).

## Open questions

- ~~**Q1** Why can recovery be paused, and what happens to the attempt count?~~ For a dispute, a payment plan being negotiated or a regulatory hold; nothing is attempted while paused; the failed-attempt count is kept (role-played domain expert, DE-42, 2026-11-08)
- ~~**Q2** The vision lists the pause reasons "for example". Is the list closed?~~ Yes, the three reasons are the closed list (role-played domain expert, DE-89, 2026-11-08)
- ~~**Q3** Is a pause limited in time?~~ No; it never ends on its own, only an agent resumes it (role-played domain expert, DE-89, 2026-11-08)
- ~~**Q4** What happens when an agent pauses an already paused recovery?~~ It is rejected; only a running recovery can be paused (role-played domain expert, DE-89, 2026-11-08)

## Notes for the plan

- None.
