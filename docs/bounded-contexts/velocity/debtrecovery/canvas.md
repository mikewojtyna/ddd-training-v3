# Bounded Context Canvas: DebtRecovery

<!--
Written by Claude on 2026-11-08 from docs/domains/velocity/velocity-vision.md ("Account Blocks & Debt Recovery"),
docs/domains/velocity/velocity-context-map.md and the CML, during an autonomous run. Values from role-played
domain-expert answers (DE-n).
-->

## Name
DebtRecovery

## Purpose
Actively pursues money riders owe after the quick automatic retries gave up: reminds the rider and asks Settlement
to collect, on a schedule, until the debt is paid or recovery is exhausted, and lets the business pause the pursuit
for disputes, payment-plan talks or regulatory holds without abandoning it.

## Strategic Classification
- Domain: supporting
- Business model: revenue
- Evolution: custom built

## Domain Roles
- Execution Model: runs each recovery process.

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Settlement | Event | Rental Fee Unpaid | Starts recovery, or joins a running one (DE-44) |
| Settlement | Event | Outstanding Debt Cleared | Recovery succeeded (also when the rider paid themselves) |
| Collections (clock) | Command | Attempt Due Collections | First attempt at start, then every 3 days (DE-40) |
| Collections agent | Command | Pause Recovery (reason) | dispute, payment plan, regulatory hold (DE-42) |
| Collections agent | Command | Resume Recovery | |
| Collections agent | Query | Recovery Process | |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Collect Outstanding Debt | Command | Settlement | yes (Conformist to Settlement's PL, Customer/Supplier) |
| Payment Reminder | Command | Rider notifications (messaging provider) | yes |
| Recovery Started, Recovery Succeeded, Recovery Exhausted, Recovery Paused, Recovery Resumed | Event | none outside Debt Recovery yet | n/a |

## Ubiquitous Language
- **Recovery process**: the pursuit of one rider's outstanding debt; one open process per rider.
- **Open**: a recovery process that is running or paused; it has neither succeeded nor been exhausted. A rider has at most one open recovery process.
- **Collection attempt**: a payment reminder plus a request to Settlement to collect the whole outstanding debt.
- **Due**: the next collection attempt's time has come; every 3 days after the previous one (DE-40).
- **Paused**: no attempts are made; reason *dispute*, *payment plan* or *regulatory hold*.
- **Resumed**: attempts continue; the next one is due at once (DE-42).
- **Succeeded**: the debt was cleared.
- **Exhausted**: 5 collection attempts failed (DE-41).

## Business Decisions

### An unpaid fee starts recovery with an immediate attempt (spec: collections-starts-debt-recovery#E1.1)
- **Given** Anna has no recovery process
- **When** Anna's rental fee of PLN 5.00 is unpaid at 10:50 on 2026-11-08
- **Then** a recovery process starts for Anna (Recovery Started) with the first collection attempt due at once

### A successful collection ends recovery (spec: collections-attempts-debt-collection#E2.1, #E1.1)
- **Given** Anna's recovery process is due
- **When** a collection attempt is made and Settlement collects the debt
- **Then** Anna received a reminder and the recovery succeeded (Recovery Succeeded)

### A failed collection schedules the next attempt in 3 days (spec: collections-attempts-debt-collection#E3.1)
- **Given** Anna's recovery process had no failed attempts
- **When** a collection attempt on 2026-11-08 10:50 fails
- **Then** Anna has 1 failed attempt and the next is due on 2026-11-11 10:50

### Recovery is exhausted after the fifth failed attempt (spec: collections-attempts-debt-collection#E4.1, #E4.2; collections-agent-resumes-debt-recovery#E2.2)
- **Given** Anna's recovery process has 4 failed attempts
- **When** the fifth attempt fails
- **Then** the recovery is exhausted (Recovery Exhausted) and no further attempts are made

### A paused recovery makes no attempts (spec: collections-agent-pauses-debt-recovery#E3.1)
- **Given** Anna's recovery was paused for a *dispute* and an attempt would have been due on 2026-11-11
- **When** due collections are attempted on 2026-11-12
- **Then** no attempt is made for Anna

### A resumed recovery is due at once and keeps its count (spec: collections-agent-resumes-debt-recovery#E1.1, #E2.1)
- **Given** Anna's recovery is paused with 2 failed attempts
- **When** a collections agent resumes it on 2026-11-20 09:00
- **Then** the recovery is resumed (Recovery Resumed), still with 2 failed attempts, and the next attempt is due on 2026-11-20 09:00

### A second unpaid fee joins the running recovery (spec: collections-starts-debt-recovery#E2.1)
- **Given** Anna's recovery process runs with 2 failed attempts for PLN 5.00
- **When** another rental fee of PLN 43.00 is unpaid
- **Then** no new process starts; the running one keeps its 2 failed attempts and pursues Anna's whole outstanding debt (DE-44); its next attempt stays due when it was

### A cleared debt ends recovery (spec: collections-attempts-debt-collection#E5.1)
- **Given** Anna's recovery process is paused
- **When** Anna's outstanding debt is cleared (she paid in the app)
- **Then** the recovery succeeded

- *After exhaustion:* Anna's recovery is exhausted and she then pays in the app → the recovery succeeded (DE-45) (spec: collections-attempts-debt-collection#E5.2). A second clearance changes nothing (spec: collections-attempts-debt-collection#E5.3).

### An unpaid fee joins a paused recovery, which stays paused (spec: collections-starts-debt-recovery#E2.2)
- **Given** Anna's recovery was paused for a *dispute* with 1 failed attempt
- **When** another rental fee of PLN 3.00 is unpaid
- **Then** no new recovery starts; the fee joins Anna's recovery, which stays paused for *dispute* with 1 failed attempt (DE-88)

### After success or exhaustion a new unpaid fee starts a new recovery (spec: collections-starts-debt-recovery#E3.1, #E3.2)
- **Given** Anna's recovery was exhausted on 2026-11-20 with PLN 5.00 still outstanding
- **When** her rental fee of PLN 3.00 is unpaid at 12:00 on 2026-11-25 (outstanding debt PLN 8.00)
- **Then** a new recovery starts for her whole outstanding debt of PLN 8.00, with 0 failed attempts and the first attempt due at 12:00 on 2026-11-25 (Recovery Started). The same after a succeeded recovery (E3.1).

### An attempt isn't made a minute early (spec: collections-attempts-debt-collection#E1.2)
- **Given** Anna's recovery is running with 1 failed attempt and its next attempt due at 10:50 on 2026-11-11
- **When** due collections are attempted at 10:49 on 2026-11-11
- **Then** no attempt is made for Anna (72 hours to the minute, DE-90)

### Pausing keeps the failed attempts (spec: collections-agent-pauses-debt-recovery#E1.1, #E2.1)
- **Given** Anna's recovery is running with 1 failed attempt and its next attempt due at 10:50 on 2026-11-11
- **When** collections agent Kate pauses it for a *dispute* at 14:00 on 2026-11-09
- **Then** Anna's recovery is paused for *dispute* with 1 failed attempt (Recovery Paused)

### Only the three pause reasons are accepted (spec: collections-agent-pauses-debt-recovery#E2.2)
- **Given** Ben's recovery is running
- **When** Kate pauses it giving the reason "rider is on holiday"
- **Then** the pause is rejected and Ben's recovery keeps running

### Only a running recovery can be paused (spec: collections-agent-pauses-debt-recovery#E4.1, #E4.2, #E4.3)
- **Given** Anna's recovery is paused for a *dispute*
- **When** Kate pauses it for a *regulatory hold*
- **Then** the pause is rejected and the reason stays *dispute* (DE-89). The same for an exhausted recovery (E4.1) and a rider without one (E4.2).

### Only a paused recovery can be resumed (spec: collections-agent-resumes-debt-recovery#E3.1, #E3.2)
- **Given** Anna's recovery is running with 1 failed attempt and its next attempt due at 10:50 on 2026-11-11
- **When** Kate resumes it at 15:00 on 2026-11-09
- **Then** the resume is rejected and the next attempt stays due at 10:50 on 2026-11-11. The same for a recovery that succeeded while paused (E3.2).

## Assumptions
- 🎭 DE-40, DE-41, DE-42, DE-43, DE-44.
- 🎭 DE-88: a second unpaid fee joins any open recovery, running or paused; after success or exhaustion a new unpaid fee starts a new process. (Exhausted debt stays with the rider.)
- 🎭 DE-89, DE-90: closed list of pause reasons; only a running recovery can be paused, only a paused one resumed; "every 3 days" is 72 hours.

## Verification Metrics
- Share of recovery processes that succeed.
- Average days to recover a debt.

## Open Questions
- Billing disputes raised in Customer Support should pause recovery automatically (reference map: deferred).
- Payment-plan instalments (DE-43: out of scope).
