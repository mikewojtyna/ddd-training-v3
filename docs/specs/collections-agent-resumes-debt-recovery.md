---
name: collections-agent-resumes-debt-recovery
intent: collections-agent-resumes-debt-recovery
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A collections agent wants to resume a paused debt recovery

Concrete rules, examples and contract for [the intent](../intents/collections-agent-resumes-debt-recovery.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Debt recovery | The active pursuit of one rider's whole outstanding debt; running, paused, succeeded or exhausted. |
| Paused | No collection attempts are made until an agent resumes the recovery. |
| Resumed | The recovery runs again; its next collection attempt is due at once (DE-42). |
| Failed attempts | How many collection attempts of this recovery failed so far; kept across a pause (DE-42). |
| Exhausted | The 5th collection attempt failed (DE-41). |

## Rules

### R1: A collections agent can resume a paused recovery, and its next collection attempt is due at once

- **E1.1** Given Anna's recovery is paused for a *dispute* with 2 failed attempts, When collections agent Kate resumes it at 09:00 on 2026-11-20, Then Anna's recovery is running again with its next attempt due at 09:00 on 2026-11-20 (Recovery Resumed).

### R2: A resumed recovery keeps its failed attempts

- **E2.1** Given Anna's recovery was resumed at 09:00 on 2026-11-20 with 2 failed attempts, When the attempt at 09:00 on 2026-11-20 fails, Then her recovery has 3 failed attempts and the next attempt is due at 09:00 on 2026-11-23.
- **E2.2** Given Ben's recovery was paused for a *regulatory hold* with 4 failed attempts and Kate resumed it at 12:00 on 2026-11-21, When the attempt at 12:00 on 2026-11-21 fails, Then Ben's recovery is exhausted with 5 failed attempts (Recovery Exhausted).

### R3: Only a paused recovery can be resumed

- **E3.1** Given Anna's recovery is running with 1 failed attempt and its next attempt due at 10:50 on 2026-11-11, When Kate resumes it at 15:00 on 2026-11-09, Then the resume is rejected and the next attempt stays due at 10:50 on 2026-11-11.
- **E3.2** Given Anna's recovery succeeded on 2026-11-15 while it was paused, When Kate resumes it on 2026-11-16, Then the resume is rejected and the recovery stays succeeded.

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Resume Recovery | Collections Agent | rider | Recovery Resumed | R3 |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Recovery Resumed | a collections agent resumed a paused recovery | rider, resumed by, resumed at, failed attempts, next attempt due at (= resumed at) |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Resume | rider | Identifier (Rider) | required; the rider's recovery is paused |
| Resume | resumed by | Identifier (Collections Agent) | required; any collections agent (DE-89) |

## Out of scope

- Pausing a recovery and seeing its state (`collections-agent-pauses-debt-recovery`).
- Making the collection attempts and exhausting the recovery (`collections-attempts-debt-collection`).

## Open questions

- ~~**Q1** When is the next attempt due after resuming, and is the attempt count reset?~~ At once; the failed-attempt count is kept (role-played domain expert, DE-42, 2026-11-08)
- ~~**Q2** Can any collections agent resume a recovery?~~ Yes, any collections agent may pause or resume; resuming a recovery that isn't paused is rejected (role-played domain expert, DE-89, 2026-11-08)
- ~~**Q3** Should a recovery resume on its own?~~ No, a pause never ends on its own (role-played domain expert, DE-89, 2026-11-08)

## Notes for the plan

- None.
