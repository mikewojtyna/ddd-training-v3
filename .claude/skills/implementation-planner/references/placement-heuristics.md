# Placement heuristics: which bounded context owns each spec element

A spec is context-agnostic on purpose. Placement is where its commands, queries, events and rules meet the context map, so every decision here is a strategic-design decision. Write each one down with its reason. The reviewer has to be able to disagree with a sentence, not reverse-engineer it from code.

## Sources, in order of authority

1. **The user's answers in this session.** They're the domain expert.
2. **Canvases**, at `docs/bounded-contexts/<system>/<context>/canvas.md`: purpose, the context's question, ubiquitous language, business decisions, inbound and outbound messages. These record the current agreed thinking.
3. **The CML context map**, at `src/main/cml/<System>-*.cml`: `BoundedContext` blocks with `responsibilities`, and relationships with direction and patterns.
4. **Discovery reports**, at `docs/context-discovery/<system>/*/discovery.md`: each context's key question, and the heuristic trail that produced it.
5. **Code**: which contexts exist, and which rules they already enforce.

The code is last on purpose. "The class is already there" is the most common bad reason to put a rule somewhere.

## Who owns what

| Spec element | Owner | Test question |
|---|---|---|
| **Rule** (invariant or decision) | The context whose key question the rule helps answer, and which holds, or should hold, the state the rule reads | "If this rule changes, which team changes code, and who would the business ask about it?" |
| **Command** | The context that owns the rules able to reject it (its "Rejected when" rules). If those rules sit in two contexts, the command belongs to one and the other context's rule becomes a precondition: a local copy fed by events, or a query through an ACL | "Who can say no?" |
| **Event** | The context in which the fact happens, the one that decided it. Other contexts react to it; they don't own it | "Who would be lying if this event were wrong?" |
| **Query** | The context that owns the data. If the answer needs data from several contexts, prefer a read model in the asking context fed by upstream events, or a query to an upstream OHS. Never a reverse dependency | "Whose data is this a view of?" |
| **Data shape** | The context that owns the command or event carrying it. Each context models its own version; the integration event (`api/events`) is the published language between them | "Whose language is this field named in?" |

## The four outcomes

1. **One existing context.** Every element maps to the same context. This is the common case for a fine-grained intent.
2. **Several existing contexts.** Elements split across contexts. Name the integration for each hand-off:
   - the upstream publishes an event or offers an OHS query (published language in `api/events` or `api/queries`);
   - the downstream reacts with a policy, through an ACL (an `@AntiCorruptionLayer` package) that translates into its own language.

   **The direction has to follow the CML.** If the hand-off needs a relationship that the map doesn't have, propose adding it (see "Context map changes"). If it needs the reverse direction of an existing relationship, you'd create a cycle. Before proposing it, check whether you placed something wrongly: usually a rule sits on the wrong side, or an event is drawn the wrong way.
3. **A new context.** Hand the decision to `discover-bounded-contexts` (validate mode, with the candidate context and the spec as the narrative). Signals that something needs a new context:
   - no existing context's key question covers the intent's rules;
   - putting the rules in the nearest context would give that context a second key question (single responsibility, H3);
   - the intent introduces a pivotal event that separates "before" and "after" with different rules, owners or reasons to change (H1);
   - the work runs in parallel, on its own clock, beside an existing flow (H2).

   Don't create the context yourself, and don't plan its code until discovery has approved it and its canvas exists.
4. **Ambiguous.** Two contexts could own it and the sources don't settle it. Ask the user with the options and your recommendation. Don't pick silently.

## Smells to push back on

- **"Put it where the code is."** Only if that context also owns the rule by the table above.
- **A context that only grows.** Each intent adds "just one more rule" to the same context until it answers every question, which is the Brain Context anti-pattern in `docs/model-traits-worksheet.md`. If a context's canvas question doesn't cover the rule, say so.
- **Shared tables or shared domain classes** to avoid an integration. That's a shared kernel, which the repo's rules reject unless the user explicitly insists.
- **The top-level `common` as an owner.** `common` holds technical building blocks (`DomainEvents`, publishers), never business rules.
- **A conformist downstream** without a well-established upstream language. The default is an ACL (see `discover-bounded-contexts/references/context-map-rules.md`).

## Translation

The spec speaks the vision's vocabulary. Each owning context has its own ubiquitous language (its canvas, its code). For each spec term:
- **Same word, same meaning:** reuse it.
- **Same meaning, different word** in the context ("Rider" in the spec, "Borrower" in Rentals' code): the context's word wins inside the context. The translation sits at that context's edge: REST adapter, ACL or published event. Record it.
- **New term:** add it to the owning context's canvas ubiquitous language, with a definition.
- **Same word, different meaning in two contexts:** that's normal across a boundary. Each context keeps its own meaning, and the integration event names the published one.

## Context map changes

Change the map only when placement needs a relationship that doesn't exist, or a new context. Then:
- **Never edit the reference CML in place** (for VeloCity, `src/main/cml/VeloCity-*.cml` is the training's reference solution). Propose the change as a CML snippet in the plan. After approval, write it, to the existing file if the user agrees, otherwise to a new file `src/main/cml/<System>-Context-Map-<intent>.cml`.
- Validate with `.claude/skills/discover-bounded-contexts/scripts/validate_cml.sh <file.cml>`, and check the rules in `discover-bounded-contexts/references/context-map-rules.md`: no cycles, no shared kernel, ACL downstream, OHS/PL upstream.
- Keep `velocity/velocity-context-map.md` and the CML in sync if the user accepts the change into the reference.
