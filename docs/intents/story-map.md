# Story map

The backbone of each system: who the actors are and the ordered activities of their journey. Intents (`docs/intents/*.md`) hang under these activities through their `activity` and `task` fields. The order of activities here **is** the value stream, and `README.md` uses it to lay intents out. Keep activity names stable, because intents reference them word for word.

Format rules, which `intents.py` parses:
- `## <system>` for each system, matching the `system` field of the intents.
- `### <n>. <Activity>` for each activity, numbered in journey order.
- Under an activity, one `- <Task>` line per known user task. Tasks with no intent yet are allowed and help show the gaps.
- An optional `Actors:` line under the system heading.

## velocity

Actors: Rider, Technician, Support Agent, Collections Agent, Fleet Manager, Fleet Operations, Pricing Manager, Finance, Collections

### 1. Join VeloCity
- Register
- Confirm contact details
- Provide personal data
- Add a payment method
- Sign the user agreement

### 2. Get a bike
- Reserve a bike
- Unlock a bike
- Release unused reservations

### 3. Ride
- Ride within the maximum rental time

### 4. Return the bike
- Return at a station
- End the ride in a drop zone

### 5. Pay for the ride
- Price the ride
- Charge the ride
- Retry failed charges
- Pay off outstanding debt

### 6. Stay in good standing
- Block a rider whose card is invalid

### 7. Recover unpaid fees
- Start debt recovery
- Attempt a collection
- Pause debt recovery
- Resume debt recovery

### 8. Get help
- Report an incident
- Decide on an incident
- Escalate an incident
- Reopen a rejected incident
- Resolve an incident

### 9. Keep the fleet healthy
- Add a bike to the fleet
- Take broken bikes out of circulation
- Report a fault
- Reserve a broken bike
- Start maintenance
- Return a fixed bike to circulation

### 10. Set prices
- Publish a tariff
