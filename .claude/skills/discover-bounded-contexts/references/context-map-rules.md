# Context map patterns and rules

The patterns come from the ddd-crew Context Map Cheat Sheet (`docs/context-map-cheat-sheet.png`). The rules are the trainer's own. They decide what counts as a *good* map in this skill, and they override generic DDD advice.

## Team relationships
- **Upstream/Downstream (U/D):** the upstream team's actions influence the downstream team, not the other way round. This influence covers code but also schedule and responsiveness.
- **Mutually dependent:** two contexts must be delivered together to work. They have a close, reciprocal link.
- **Free:** a change in one context doesn't affect the other's success or failure. There's no organisational or technical link.

## Patterns
| Pattern | Side | Meaning |
|---|---|---|
| Open Host Service (OHS) | upstream | A defined set of services, such as a public API, that any downstream can integrate with. |
| Published Language (PL) | upstream (or shared) | A well-documented shared language for translating in and out, such as an event schema, iCalendar or vCard. Often combined with OHS. |
| Conformist (CF) | downstream | The downstream adopts the upstream model with no translation, which couples its domain model to the upstream. |
| Anticorruption Layer (ACL) | downstream | A translation layer that isolates the downstream model. Only the adapter is coupled to the upstream. |
| Customer/Supplier | U/D | The downstream (customer) requirements factor into the upstream (supplier) planning. |
| Partnership | symmetric | Coordinated planning and joint management of the integration. |
| Shared Kernel (SK) | symmetric | A shared subset of the model, for example a shared JAR or a database schema. |
| Separate Ways | none | No integration: the contexts go their own way. |
| Big Ball of Mud | (a boundary) | A mess of mixed models and inconsistent boundaries. |

## Relationship rules
1. **Big Ball of Mud is always wrong.** Don't propose it. If you find one in an existing map, say so as a finding, not as an option.
2. **Partnership is good only in early stages.** Once a boundary is well established, prefer Upstream/Downstream. If you propose Partnership, say which stage you assume and what would trigger moving to U/D.
3. **Upstream/Downstream is very often healthy, so prefer it.** U/D going both ways between the same contexts (a cycle) is almost always wrong. It usually means:
   - an event drawn the wrong way
   - a responsibility in the wrong context
   - two candidates that are really one context (go back to H3)

   An indirect cycle (A → B → C → A) needs checking too. Is each arrow a real model dependency, or is one of them just the direction an event travels?
4. **Separate Ways only for R&D.** Use it only when you deliberately build an experimental silo and *maybe* integrate later if the experiment is promising. Two contexts that simply don't talk aren't "Separate Ways". They just have no relationship.
5. **Customer/Supplier is healthy for teams in the same organisation, domain or product,** because they work towards a common goal. It's the default flavour of U/D between internal teams.

## Integration pattern rules
1. **Conformist only towards a well-established domain and language.** Examples are a payment provider's standard API or an industry standard. Otherwise the upstream model pollutes the downstream. Justify every CF.
2. **Shared Kernel is almost always wrong.** It forces constant synchronisation, and over time the "common" code collects rules specific to one context that aren't really common. Propose it only with an explicit reason the user accepted.
3. **An ACL is almost always a good idea downstream.** It's the default for a downstream unless it's a justified Conformist.
4. **OHS is healthy and applies only upstream.** It's often combined with PL, because clients need a language to talk to the service.
5. **Published Language is good for publishing events or schemas.** An upstream that publishes events usually offers PL.

## How to derive relationships
- Take each business process or user story, and map each step to the owning context.
- Every hand-off between two contexts is a candidate relationship. The context whose model the other must understand is **upstream**.
- Direction follows **dependency on the model**, not the direction of a call or an event.
  - A context that publishes facts others react to is upstream of those subscribers.
  - A context that serves queries or commands through an API it defines is upstream (OHS) of its clients.
- Name each relationship after what flows across it (`RentalSessionEnded`, `PriceCalculation`), and add a comment with the messages.
- Mark anything the expert hasn't confirmed as an assumption in a CML comment, for example `// default assumption, NOT confirmed with the domain expert`.

## Breaking a cycle
A cycle means some arrow follows the wrong thing. The trainer's rule says U/D going both ways is almost always wrong, so cycles get resolved, not explained away.

**Direct cycle (A → B and B → A): never keep it.** Resolve it in this order:
1. **One side becomes a client of the other's API.** If A sends commands to B ("refund this charge", "block this account"), B publishes an OHS in its own language, and A calls it through an ACL. B is upstream. Sending a command doesn't make the sender upstream: direction follows whose model is the contract, not which way the message travels.
2. **Move the responsibility, or carry the data.** If a context needs data that only exists because of a decision the other one makes, the decision may be in the wrong context (go back to H3). Or the needed data can travel inside a message the other side already gets: a refund request carrying its own cap, a lesson-outcome event carrying the price. That removes the back-dependency.
3. **Partnership.** If the two teams genuinely have to plan and release together, say so honestly with `[P]<->[P]`, and note the trigger for moving to U/D later.
4. **Merge.** Last resort. Two contexts that can't live apart may be one. Reopen H3 with the expert.

**Indirect cycle (A → B → C → A): try the same flips on each link.** If one fits the facts, take it. If none does, put the cycle and each candidate flip to the expert **in business terms**, and keep the cycle only if they explicitly choose to. Record the flips, the expert's answer and the reason in the report and in a CML comment.

**Guards, for every flip:**
- A flip must agree with the facts the expert has stated. "Scheduling needs the tutor's eligibility from Trust & Safety" fixes that arrow.
- Ask neutral questions about what actually happens ("When a complaint is upheld with a refund, who decides the amount, and who executes it?"). Don't ask leading questions that propose the flip.
- Never rest a direction or a kept cycle on an improvised answer, or on a forced either/or choice. Ask for the actual business fact.

## What `validate_cml.sh` flags
The script prints `RULE` lines for these smells:
- `shared-kernel`
- `partnership`
- `ud-cycle` (direct)
- `ud-cycle-indirect`
- `no-acl` (a downstream with neither ACL nor CF)
- `conformist-check` (every CF, to justify)
- `isolated` (a context with no relationship)

These are prompts for judgement, not verdicts. Resolve each one, or justify it in the report.
