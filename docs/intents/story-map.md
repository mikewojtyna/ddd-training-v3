# Story map

The backbone of each system: who the actors are and the ordered activities of their journey. Intents (`docs/intents/*.md`) hang under these activities through their `activity` and `task` fields. The order of activities here **is** the value stream, and `README.md` uses it to lay intents out. Keep activity names stable, because intents reference them word for word.

## velocity

Actors: Rider, Technician, Fleet Operations, Support Agent, City Operations

### 1. Join VeloCity
- Sign up
- Activate account

### 2. Get a bike
- Find an available bike
- Reserve a bike
- Cancel a reservation
- Unlock a bike

### 3. Ride
- Ride to destination

### 4. Return the bike
- Return at a station
- Return in a drop zone

### 5. Pay for the ride
- Get charged for the ride

### 6. Keep the fleet available
- Release unused reservations
- Reserve a broken bike for repair
- Repair a bike
