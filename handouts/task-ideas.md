# Task ideas for your team's skill

Pick a task you do at least monthly, whose output has a shape, and where reviewers keep finding the same mistakes.
Each idea below has a one-line seed for the template, the script check and the critic.

| Task | Template (`assets/`) | Script check (`scripts/`) | Critic (`agents/`) |
|---|---|---|---|
| **ADR writing**: record an architecture decision (the worked example, `ex13/solution`) | MADR-like: Context, Decision drivers, Considered options, Decision, Consequences (+/−), Status | Numbering unique and sequential; required sections present; a superseded ADR links its successor; `accept`/`supersede` change statuses | At least two real options; negative consequences listed; decision traceable to the drivers |
| **API change review**: judge a diff of an OpenAPI file or a public interface | Change list: endpoint/operation, change kind (added/changed/removed), breaking yes/no, migration note, version bump | Diff two OpenAPI files: a removed field, a new required field or a changed type without a major version bump is an ERROR | Is each "non-breaking" claim really safe for existing clients? Are deprecations dated? |
| **Celery task scaffold (Python)**: add a background task the team's way | Task module + test: `@shared_task(bind=True, autoretry_for=..., max_retries=...)`, idempotency key, structured logging | AST check: every task sets retries and a time limit, takes only JSON-serialisable args, has a matching test file | Is the task idempotent if it runs twice? What happens on retry after a partial write? |
| **Compliance / data-safety check of a diff**: before a PR or before sending code to an external model | Findings table: file, line, category (secret, personal data, export control, licence), severity, action | Regex/entropy scan for keys and tokens, e-mail and personal-ID patterns, forbidden licence headers; ERROR blocks the PR | False positives vs. real risk; is the classification right for the data that may go to which model? |
| **PoC / spike write-up**: close a spike with a decision, not a slide deck | Question asked, hypothesis, what was built, measurements, verdict (promote / drop / extend), follow-ups | Every measurement has a number and a unit; verdict is one of three values; time-box stated and not exceeded | Does the evidence support the verdict? What wasn't measured that the decision depends on? |
| **Incident post-mortem**: blameless write-up after an outage | Timeline (UTC), impact, root cause, contributing factors, actions with owner and date | Timeline sorted and in UTC; every action has an owner and a due date; no names in "root cause" | Is the root cause a cause or a symptom? Do the actions prevent recurrence or just detect it? |

Tips:
- The script holds what can be decided mechanically. The critic holds what needs judgement. Nothing important stays
  only in the prompt.
- If the output has a status (proposed, accepted, approved), the script changes it, and only on the human's word.
- A good tricky eval: the request lacks one deciding fact. The skill should ask, the baseline usually invents it.
