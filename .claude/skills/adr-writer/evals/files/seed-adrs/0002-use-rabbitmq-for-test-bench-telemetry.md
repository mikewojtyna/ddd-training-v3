---
title: Use RabbitMQ for test-bench telemetry
status: accepted
date: 2025-03-14
supersedes:
superseded_by:
---

# 0002. Use RabbitMQ for test-bench telemetry

## Context

Test benches emit telemetry while a test runs. Several analysis jobs consume it, and a bench must not block when a job
is slow or down. Today the benches write CSV files to a shared drive that the jobs poll, which loses data when a job
starts late and couples every job to the file layout.

## Decision drivers

- Benches never block on slow consumers.
- New analysis jobs can subscribe without changing the benches.
- The team can operate it without a platform team.

## Considered options

### RabbitMQ

A broker the team already runs for build notifications. Fan-out exchanges give every job its own queue; messages are
removed once consumed.

### Keep CSV files on the shared drive

No new infrastructure, but jobs that start late miss data and every job depends on the file layout.

## Decision

We chose RabbitMQ, because it decouples benches from jobs and new jobs can subscribe without bench changes, and the
team already operates it.

## Consequences

### Positive

- Benches publish and forget; slow jobs no longer lose data while they're running.
- A new analysis job only needs a new queue binding.

### Negative

- Consumed messages are gone: a job that fails can't re-read telemetry it already took off its queue.
- The team operates one more production broker instance.

## Links

- Bench telemetry format: see the test-bench handbook.
