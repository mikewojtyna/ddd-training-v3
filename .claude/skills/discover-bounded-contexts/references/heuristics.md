# Bounded context heuristics

These are behaviour-based heuristics. They look at what happens in the business (events, flows, decisions, rules), not at data, nouns or org charts. Apply them in this order, because each one builds on the previous: H1 → H2 → H3. Ask the domain expert questions at every step. A board of sticky notes shows the *what*; the boundaries hide in the *what it means*, *what comes before/after* and *who decides*.

## Contents
- Input: narratives vs processes
- H1: pivotal events
- H2: swimlanes
- H3: single responsibility
- Cross-cutting checks (gaps, rule ownership, size)
- Questions that work

---

## Input: narratives vs processes

- A **narrative** is a sequence of events that tells a story ("User Registered → … → Bike Returned → Fee Calculated").
- A **process** is a narrative with business semantics and scope: a start, an end, and a concrete business or user value delivered ("Rent a bike", "Recover a debt").

A narrative comes first, and you cut it into processes afterwards. Processes are not boundaries. One process usually crosses several bounded contexts, so never equate "one process = one context". Processes still help in two ways:
- **Independent processes** that can run in parallel are rarely in the same context. This is the swimlane heuristic seen from the process side.
- **Processes are the test bench for the context map.** Walk every step of every process through the contexts to derive the relationships.

If the input is only a heap of events with no order, first rebuild a narrative from it, and ask the expert to confirm the order.

---

## H1: Pivotal events (always first)

A **pivotal event** changes the business phase. What happens before it and what happens after it usually belong to different contexts. Example: *Order Finalized* is produced by order preparation and consumed by order delivery.

How to find them:
- Look for events after which the vocabulary, the actors, the rules or the clock change ("before it everything is about who the user is; after it everything is about bikes and rides").
- Look for hand-offs: an event that one part of the board produces and another part reacts to.

How to confirm them, by asking the expert:
- "What does <event> mean in business terms?"
- "What happens right before it? What happens right after it?"
- "Is <event A> the real phase change, or is it <event B>?" (Bike Reserved vs Rental Session Started)

Only treat an event as pivotal once the expert has confirmed it separates important business phases. Until then, call it a *candidate*.

Watch out for these:
- **A pivot isn't automatically a boundary.** In VeloCity, *Rental Session Started* was a confirmed phase change, but H3 showed that nothing before it had its own rules. Preparation and ride stayed in one context. Use H3 to decide whether a pivot becomes a boundary.
- **An empty side means a missing context.** If a confirmed pivot has nothing behind it, or it hands off to something that only calculates or only executes, ask who does the work after the pivot. This is how Settlement was found in VeloCity.
- **Look for missing events.** A pivot often reveals events the board lacks: a forced end, Payment Succeeded/Failed, Rental Settled/Unsettled. Name them, and suggest adding them to the board.
- **Look for misleading names.** "First Hour Fee Applied" turned out to be an *estimate*, not a charge. Ask, and suggest a rename ("Fee Estimated").

---

## H2: Swimlanes (second)

Flows that run **in parallel**, **repeat on their own clock**, or have **no clear before/after order** relative to each other are strong candidates for separate contexts.

Signals:
- A flow repeats on its own clock while the main flow carries on (the price estimate ticking during a ride).
- A flow follows a *different thing* from the main flow (a bike's condition rather than a particular rental).
- A flow can be triggered from several places, and has its own loops (incidents: escalate, reject, reopen).
- A flow runs on a much longer timeline (debt recovery over days, rides over minutes).
- The expert can't say whether X happens before or after Y.

How to confirm:
- "Does <flow> run independently of <main flow>? Who or what starts it?"
- "Can it pause and resume? Over what time scale?"
- "When it finishes, does anything flow back into the main timeline?"
- "Are <variant A> and <variant B> two different business models with different rules, or two endings of the same thing?" (station returns vs zone returns: same rules, so it's one ride, not two lanes)

Two flows that differ only in a detail (a fee, a location type) are **variants**, not lanes. Keep them together.

---

## H3: Single responsibility (third, as validation)

Every bounded context answers **its own question**. Two contexts can't answer the same question or own the same decision. If one responsibility is spread across several candidates, they're probably one context: merge them, or move the responsibility to a single owner.

H3 assumes you already have candidates from H1 and H2. Use it to validate and correct them.

The procedure:
1. For each candidate, write the one question it answers: "Can this rider use the service?", "Who has which bike, and when?", "What does this cost?". If you can't write one question, the candidate is vague. If you need two unrelated questions, it may be two contexts.
2. Look for the same question, decision or state in two places. In VeloCity, account blocking appeared in onboarding *and* in debt handling, and the estimate and the final price were the same calculation.
3. Resolve each duplication without breaking a confirmed pivot. If merging two candidates would contradict a confirmed pivotal event, move ownership of the responsibility instead. In VeloCity, Rider Accounts became the single owner of blocking, and the others publish facts it reacts to.

Supporting tests:
- **Who owns the rules?** A context that "knows nothing about any rules" (VeloCity's Payments) is purely mechanical. Any rule-driven work living inside it (debt recovery with its own actions and schedule) belongs somewhere else. Ask: "Does X have its own rules, such as schedules, retries or escalation, or does it just do what it's told?"
- **Does it change independently, or by different people?** Pricing "evolves independently" and is "a simple calculator usable by anyone". It stays separate even though it's small. If a candidate changes on the same schedule and for the same reasons as its neighbour, consider folding it in.
- **Simple rules still count.** Settlement's rules were "not very complex, mostly about schedule" but independent of the ride. Simplicity makes something a *supporting* context, not a non-context.
- **Combining answers isn't owning them.** Rental's eligibility check asks other contexts and combines their answers. That doesn't make it the owner of account status.

---

## Cross-cutting checks

Before you present the candidates, ask these questions:
- **Is every step owned?** Walk the whole narrative and name the owning context for every event, decision and rule. An event nobody owns is a gap. A step that "Rental calls Pricing and Payments charges" glosses over is often a hidden context.
- **Is every decision owned exactly once?** Run H3 on decisions, not only on events.
- **Do any events need renaming or adding?** Collect suggestions for the board.
- **What are the external parties?** Payment processors, city authorities and similar parties sit outside your contexts. Keep them as external systems, not contexts you design.
- **Does a technical concern pose as a context?** "Notifications" or "Database" are mechanisms, not business questions. Push back unless the expert gives them their own rules.

---

## Questions that work

End every round with **numbered** questions. Ask only what your current heuristic needs answered, and say why each question matters for the boundary.

- *Meaning:* "What's the difference between Account Activated and Account Validated?"
- *Ordering:* "Does the return stop the clock and produce the total, or is the total calculated earlier?"
- *Phase:* "Does Bike Reserved mark a phase with its own rules, time limits and fees, or is it just the first step of renting?"
- *Duplicates:* "Rental Session Started appears twice. Is it the same event?"
- *Ownership:* "Who decides that this rider can't use the service right now? One owner whatever the reason, or does each department decide its own block?"
- *Rules:* "Does debt recovery have its own rules, like a retry schedule or escalation?"
- *Feedback:* "When an incident is resolved, does anything flow back, such as a refund, a maintenance order or an unblock?"
- *Gap:* "So who actually makes the settlement here?" Ask yourself this kind of question before the expert does.
