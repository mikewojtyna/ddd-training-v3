# <Name> bounded context: implementation plan

- **System / package**: `<system>.<context>`
- **Mode**: scaffold | update
- **Source canvas**: `<path>` (or "inline, <date>")
- **Last updated**: <YYYY-MM-DD>
- **Status**: draft - awaiting review | approved | implemented

<!-- Reviewers: edit this plan directly or answer in the conversation, then set Status to "approved" (or say so).
     Invoking the skill again resumes from this file. -->

## Review notes
<!-- One entry per review round: what the user asked to change, what was changed, who approved and when. -->
- <YYYY-MM-DD>: draft written, awaiting review.

## Canvas summary
<Two or three sentences: purpose, classification, roles.>

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| Modules | 1 module = 1 context | default |
| ACL towards <ctx> | yes / no (conformist, user insisted) | <reason; risk if no> |
| ... | | |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|

### Ubiquitous language
| Term | Code type | Kind (aggregate / entity / VO / event / javadoc only) |
|---|---|---|

### Business rules → aggregates
| Rule (GWT) | Invariant | Aggregate | Test |
|---|---|---|---|

### Rules awaiting GWT
- <rule as written>, suggested GWT: <...>, status: awaiting confirmation

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|

## Files
<Tree of the created or modified files.>

## Stubs and follow-ups
- <ACL adapter X is a stub: collaborator not in code yet>
- <CML / context map needs updating?>
- <Open questions from the canvas>

## Changelog
### <YYYY-MM-DD>: <scaffolded | updated>
- Added: ...
- Changed: ...
- Removed (confirmed by user): ...
- Proposed removals (not applied): ...
- Triggered by: <canvas change>
