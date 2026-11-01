# Story map

The backbone of each system: who the actors are and the ordered activities of their journey. Intents (`docs/intents/*.md`) hang under these activities through their `activity` and `task` fields. The order of activities here **is** the value stream, and `README.md` uses it to lay intents out. Keep activity names stable, because intents reference them word for word.

Format rules, which `intents.py` parses:
- `## <system>` for each system, matching the `system` field of the intents.
- `### <n>. <Activity>` for each activity, numbered in journey order.
- Under an activity, one `- <Task>` line per known user task. Tasks with no intent yet are allowed and help show the gaps.
- An optional `Actors:` line under the system heading.

## velocity

Actors: Rider, Technician, Support Agent, City Operations

Source: `velocity/velocity-vision.md`. Activities 1 to 5 are the rider's journey. Activity 6 is the technician's journey, which reuses the rider's reserve-and-unlock steps (vision, "Broken Bikes & Maintenance").

### 1. Join VeloCity
- Register
- Complete onboarding
- Activate account

### 2. Get a bike
- Find an available bike
- Reserve a bike
- Unlock a bike

### 3. Ride
- Ride to destination

### 4. Return the bike
- Return at a station
- Return in a drop zone
- End the ride elsewhere

### 5. Pay for the ride
- Get charged for the ride

### 6. Repair a broken bike
- Report a broken bike
- Reserve a broken bike
- Unlock a broken bike
- Fix the bike
- Return the bike to circulation
