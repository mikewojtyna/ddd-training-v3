# Stage 5: integration and verification, by the orchestrator

After the last merge, the orchestrator proves that the contexts form one system:
- they talk **only through their `api`**;
- every relationship of the map **really works**: an event from A is consumed by B and triggers B's logic, and B's
  calls to A's commands and queries change or return what they should;
- the business processes run end to end;
- the map matches the code.

Lessons that apply: V1–V7 (V3 in its strict form), O9.

Do steps 1–4 in this order: each one finds problems the next would otherwise trip over.

## 1. API-only communication

1. Every context has its architecture test (the scaffolder generates `<Context>ArchitectureTest` with an *incoming*
   and an *outgoing* rule). List the contexts that lack one, and add it from the scaffolder's template.
2. **Global scan.** For every pair of contexts, find imports from one into the other that don't go through the
   other's `api` package:

   ```bash
   # for each context X: imports of other contexts' non-api packages
   grep -rhoE "import <base>\.<system>\.[a-z]+\.[a-z.]+" <src>/<system>/<X> \
     | grep -v "<system>\.<X>\." | grep -vE "<system>\.[a-z]+\.api\.|<system>\.common\.|<system>\.contextmap\."
   ```

   An empty result is the goal. Every hit is either a violation (fix it or send it back to the context's builder) or a
   recorded boundary exception (a D-number in the context plan, matched by a commented clause in its architecture test).
3. **The channel scan (V3).** "Only `api`" is too loose when the catalogue says commands and queries go over REST. A
   context may import another context's classes only from its ACL packages, and only `api.events` records. List
   every other import of another context's `api`:

   ```bash
   # for each context X: imports of other contexts' api packages
   grep -rnE "import <base>\.<system>\.[a-z]+\.api\." <src>/<system>/<X> | grep -v "<system>\.<X>\." \
     | grep -vE "/[a-z]*acl/[^:]*:.*\.api\.events\."
   ```

   Every hit is a violation: an `api.usecases` or `api.queries` import is an in-process call around the REST resource
   and the ACL translation, and an `api.events` import outside an ACL package skips the translation. Fix it (a REST
   ACL adapter, or a listener in an ACL primary adapter), or record it as a boundary exception the user must see.
   Check that the architecture tests encode the same rule. If they only say "other contexts' `..api..`", tighten them:
   outside the ACL packages, nothing of another context; inside them, only `..api.events..`.
4. **Run** all the architecture tests and report the result. Reading them is not enough.

## 2. The communication matrix

Complete `communication-matrix.md` (drafted in Stage 3, `assets/communication-matrix-template.md`). It has one row per
message crossing a relationship:
- **event:** the producer publishes it, the consumer's listener translates it, and the consumer's use case changes the
  consumer's state or publishes its own event. **The effect is what the row proves.**
- **command:** the caller's ACL calls the provider's resource, the provider's state changes, and the caller handles
  the provider's rejection (409/404) through its own exceptions.
- **query:** the caller's ACL gets the provider's answer and the caller's decision follows from it (for example "not
  eligible" → reservation rejected).

Three traps make a matrix look complete when it isn't (V2):
- **Variants:** if an event or a command has variants (a reason, a type) and the consumer reacts differently to each,
  give each variant its own row. Read the consumer's listener for the branches.
- **Half proofs:** a test that publishes the producer's event itself (or calls the producer's use case in-process)
  proves only the consumer. Mark such a row *consumer-only*. It is proven when a test drives the producer through its
  public API and the consumer's effect follows: a hand-off test, or a journey.
- **"Ignored" rows:** prove that the consumer ignores something with `await().during(...)` (the state stays unchanged
  for a while), not with `untilAsserted`, which passes at once.

For each row, find the test that proves it (often the downstream builder's hand-off test, or a journey). If none
exists, write one: a system Spring test with real beans that drives the producer or caller through its public API and
asserts the effect in the other context with Awaitility (`await().untilAsserted(...)`). For events that means never
asserting right after publishing.

```bash
python3 <skill>/scripts/comm_matrix.py check <docs>/communication-matrix.md --cml <map.cml> --tests src/test
```

0 errors before going on.
- An error *"isn't a relationship in the map"* means the code has a hand-off the map doesn't. Step 4 settles it.
- An error *"has no row"* means a relationship that nothing proves. Either test it, or it doesn't exist in code (also
  step 4).

## 3. Journey tests

One journey per business process from the vision or the discovery report's processes, in a `journeys` test package
(V1). In VeloCity:
- a ride to money;
- an unpaid ride to debt recovery and payoff;
- a lock failure to maintenance and back into circulation;
- a fleet-issue incident resolved when the bike is fixed;
- an invalid card blocking and unblocking a rider.

The rules:
- **Only the public API**, through a small client helper (one method per resource, named like the apps' actions:
  `register`, `reserve`, `unlock`, `endRideHere`). No repositories, no use cases, no internal beans, except the
  simulated external adapters (a payment processor) that the journey scripts or inspects, and the time-driven use
  cases that a `@Scheduled` job would trigger (scheduling is off in tests, so the journey advances the mutable clock
  and calls the job's use case, as the scheduler would). Name each such trigger in the journey's javadoc.
- **Every effect in another context** is asserted with Awaitility (`eventually(() -> ...)`).
- The time is controlled through the mutable clock. Ids come from `unique(...)`. Each journey uses its own time slot if
  it touches global reference data (B15).
- A `@DisplayName` tells the story with the concrete numbers ("rides 30 minutes and ends in the drop zone: settled at
  PLN 5.00").

A journey that fails is a finding. Locate which hand-off broke (the matrix helps), fix it in the owning context
(yourself, or by resuming its builder), and record it.

## 4. Map vs code, with `discover-bounded-contexts`

1. **Extract the as-built map** from the code. For each context:
   - the outbound ACL packages and the REST resources their adapters call (commands, queries → provider);
   - its event listeners and the event types they take (events → producer);
   - any boundary exception.

   Write it as a table: *from, to, kind, messages, where in the code*. Put it in `as-built-map.md` next to the manifest.
2. **Invoke `discover-bounded-contexts` in validate mode** (with the Skill tool, or by spawning an agent that does,
   if you want its context kept out of yours). Pass it:
   - the CML;
   - the as-built table as the evidence the map must match, and the processes (journeys) as the narrative;
   - the instruction to compare relationship by relationship: missing in the map, missing in the code, wrong
     direction, wrong pattern (a downstream that translates but is marked CF, an OHS with no resources);
   - the expert-question setting:
     - **role-play:** its expert questions are answered from the DE file, new answers get new DE IDs;
     - **ask the user:** it asks.

   It runs its own map critic. Let it.
3. Apply its verdict:
   - fix the code where the code is wrong (an unplanned call between contexts);
   - fix the CML where the map is wrong, re-validate it, and update the markdown map and the matrix;
   - record each change for the reviewer (V7).

   Re-run `comm_matrix.py check` afterwards.

## 5. Deliver the intents

For every plan whose steps are all `done` or `skipped`, run `plans.py done <intent>` (intent → `delivered`), then
`intents.py build`. Then run all three checks (`intents.py`, `specs.py`, `plans.py`): 0 errors.

## 6. Final verification and housekeeping

- Run the full suite **twice** on the integration branch (V5). Record the counts by kind: domain unit tests, per-intent
  Spring acceptance tests, journeys, architecture tests, adapter tests.
- Repoint the skills' references and rewrite the system's `CLAUDE.md` for the new code (V6). Commit.
- Finish the session record: results tables, the verification numbers, and "what the reviewer should look at first"
  (V7).
- Open the PR (`gh pr create`) with the summary and the look-here-first list. Don't merge it.
