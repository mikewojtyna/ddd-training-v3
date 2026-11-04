# <Name> bounded context: implementation plan

- **System / package**: `<system>.<context>`
- **Mode**: scaffold | update
- **Source canvas**: `<path>` (or "inline, <date>")
- **Last updated**: <YYYY-MM-DD>
- **Status**: draft - awaiting review | approved | awaiting tactical blocks | implemented

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
| Boundary exceptions | none (default) / D<n>: <exact package allowed> (user insisted after challenge, <date>) | <risk>; excluded in `<Context>ArchitectureTest`, comment at the usage site |
| Domain layer | delegated to `ddd-tactical-building-blocks` (always) | this skill doesn't design tactical building blocks |
| TDD checkpoint loop | on (recommended) / off (user opted out, <date>) | on: the user stays in control of each domain step and focused on the heart of the software |
| Aggregate style (if known) | state-based / event-sourced / left to the tactical skill | |
| ... | | |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|

### Ubiquitous language
| Term | Code type (filled in after the hand-back; until then "javadoc, named by the tactical skill") | Where it appears (api record / javadoc / domain type) |
|---|---|---|

### Business rules → delegated to `ddd-tactical-building-blocks`
<!-- The building block and test are filled in by ddd-tactical-building-blocks when it hands back. -->
| Rule (GWT) | Status (delegated / green) | Building block (chosen by the tactical skill) | Test |
|---|---|---|---|

### Rules awaiting GWT
- <rule as written>, suggested GWT: <...>, status: awaiting confirmation

## Deferred until tactical blocks are built
<!-- Generated in Step 6b, after every rule above is green: use cases, query handlers, REST adapters, inbound listeners, repository adapters, integration-event publishers, their wiring. -->
- <item>: <why it needs the domain>

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|

## Files
<Tree of the created or modified files, including `src/test/.../<Context>ArchitectureTest.java`, and `pom.xml` if ArchUnit had to be added.>

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
