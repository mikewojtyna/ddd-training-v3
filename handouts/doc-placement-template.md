# Doc placement: what the agent reads, when, and how we keep it true

Team(s): ______________________ · Date: __________

One row per **kind** of document your teams have (ADR, spec, README, API contract, coding guidelines, runbook,
architecture diagram, onboarding page, wiki page, ...).

| Document type | Read by the agent when (always / on demand: trigger / never) | Location (repo, path) | Size limit (lines or tokens) | Kept current by (role) | How staleness is detected |
|---|---|---|---|---|---|
| Example: ADR | on demand: when the task touches the module the ADR names | `docs/adr/NNNN-<slug>.md` in the code repo | ≤ 80 lines, one decision | the author of the PR that changes the decision | status field `superseded by` + reviewer checks the ADR list in PRs that touch the module |
| | | | | | |
| | | | | | |
| | | | | | |
| | | | | | |
| | | | | | |
| | | | | | |
| | | | | | |

## Hints for the columns

- **When:** "always" costs tokens in every session. For "on demand", write the trigger: *"when the task mentions
  billing"*, *"when it edits `payments/`"*, *"when a skill about X runs"*.
- **Location:** if the agent can't reach it (wiki, SharePoint, another repo), write that down. That's a finding.
- **Size limit:** rule of thumb ~10 tokens per line. An always-loaded file of 500 lines costs ~5,000 tokens before
  the agent does anything.
- **Kept current by:** a role, not "everyone". "Everyone" means nobody.
- **Staleness detected by:** best is something that fails (a test, a script, a link check, a CI step). Next best is
  a review rule. "We'd notice" isn't an answer.

## Total "always loaded" budget

| File | Lines | ≈ Tokens |
|---|---|---|
| | | |
| | | |
| **Total** | | |

Is that total paid in every session worth it?
