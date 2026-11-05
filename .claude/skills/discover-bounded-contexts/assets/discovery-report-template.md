# Bounded context discovery: <System> (<scope>)

- **Date:** <YYYY-MM-DD>
- **Mode:** discover | validate
- **Status:** <in progress: heuristics | contexts approved | map approved | canvases handed off>
- **Input:** <file paths / "inline" / screenshot paths>
- **Context map:** <src/main/cml/<System>-Context-Map.cml | not requested>

## Reading of the input
<How the facilitator read the board or narrative, and what the expert corrected.>

## Heuristic trail
### H1: Pivotal events
| Event | Status (candidate/confirmed/rejected) | Before | After | Expert's answer |
|---|---|---|---|---|

### H2: Swimlanes
| Flow | Verdict (lane/variant) | Why |
|---|---|---|

### H3: Single responsibility
| Duplicated responsibility | Where it appeared | Resolution |
|---|---|---|

## Bounded contexts
| Context | Question it answers | Owns (events, decisions, rules) | Heuristic justification | Classification (core/supporting/generic, if known) |
|---|---|---|---|---|

## Critic log
| Subject | Round | Verdict | Blocking points / resolution |
|---|---|---|---|

## Context map
### Process walkthroughs
#### <Process name>
| Step | Owning context | Hand-off / relationship |
|---|---|---|

### Relationships
| Upstream | Downstream | Patterns | Messages | Justification (rule) |
|---|---|---|---|---|

### Validator output
<Paste the RELATIONSHIPS and RULE lines from validate_cml.sh, and say how each RULE line was resolved or justified.>

## Q&A with the domain expert
<Numbered rounds: the questions asked and the answers given, verbatim where possible.>

## Open questions and assumptions
- <Anything unconfirmed. These are also marked in the CML.>

## Suggested board changes
- <Missing events, renames.>

## Next steps
- Canvases: <docs/bounded-contexts/<system>/<context>/canvas.md for each context, via bounded-context-scaffolder>
