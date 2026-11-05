# Discovery input: <System>

<!--
Fill in whatever you have. The best input is behaviour, meaning events in time order, not data models or org charts.
Another format works too: Miro/event-storming screenshots, BPMN (.bpmn XML), JSON, or a plain list.
-->

## Intention
<Why are you looking for boundaries? For example: a new system, splitting a monolith, team topology, a workshop exercise, validating an existing map.>

## Narrative
<!-- A sequence of business events that tells the story end to end. Past tense, one per line. Order matters. Mark parallel flows. -->
1. User Registered
2. Payment Method Added
3. User Account Activated
4. ...

<!-- Flows that run in parallel or on their own clock (e.g. maintenance, debt recovery): -->
- Parallel: <event> → <event> → ...

## Processes (optional, but they make the context map possible)
<!-- A process is a part of the narrative with a start, an end and a business value. -->
### <Process name, e.g. Rent a bike>
- Starts with: <event or command>
- Ends with: <event>
- Value: <what the user or business gets>
- Steps: <event> → <event> → ...

## Actors, rules and external systems (optional)
- Actors: <rider, ops staff, ...>
- Rules you already know: <e.g. "a station holds at most 20 bikes", "debt recovery retries every 3 days">
- External systems: <payment processor, city API, ...>

## Existing contexts or context map to validate (optional)
<Paste a list or point to a .cml file.>
