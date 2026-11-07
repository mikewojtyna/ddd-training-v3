---
name: residuality-analysis
description: Stress-test a software architecture with Barry O'Reilly's residuality theory. Generate many diverse stressors (business, regulatory, technical, people, absurd), find the attractors the business falls into, record the residue each stressor leaves, build an incidence matrix to expose hot spots and hidden coupling, propose an augmented architecture, and check it with an independent test set of stressors and a residual index. The usual input is a context map, bounded contexts, CML or code of VeloCity or CrowdSorcery. Use this whenever the user wants to validate, stress, challenge or harden an architecture or context map, asks "what happens if...", "will this design survive...", "where is this design fragile", wants a stressor analysis, incidence matrix, attractors, residues or a residual index, or mentions residuality, residues, hyperliminal systems or Barry O'Reilly. Use it even if they only say "poke holes in our context map" or "is this boundary robust to change".
---

# Residuality Analysis

Residuality theory (Barry O'Reilly) says an architecture shouldn't be designed against a list of *predicted* requirements and risks. The business environment is a complex, unpredictable system. So you stress the architecture with many varied stressors, most of which will never happen, and change it until what's left after each stressor (the **residue**) still keeps the business going. An architecture that survives many random stressors also tends to survive the ones nobody thought of. Probability doesn't matter here. A stressor isn't a risk to be ranked. It's a probe that shows how the structure behaves.

Read `references/residuality-theory.md` for the concepts and vocabulary before your first analysis in a session: stressor, attractor, residue, incidence matrix, contagion, residual index, NKP. The analysis is only as good as the vocabulary is precise. Read `references/stressor-generation.md` before generating stressors.

Also read the project's `CLAUDE.md` and the target system's `CLAUDE.md`. They tell you where the architecture is documented and which documents are workshop material.

The skill produces two files in one folder (default `docs/residuality/<system>/<YYYY-MM-DD>-<scope-slug>/`):
- `analysis.json`: the structured data (components, relationships, stressors, attractors, test stressors). It's the input of the matrix script, and you edit it when you re-run the analysis.
- `analysis.md`: the report, built from `assets/report-template.md`, with the script's tables pasted in.

---

## Step 0: Look for an existing analysis

Check the default folder, or a path the user gives you. If an `analysis.json` for the same system and scope exists, this run is an **update**: the user adds stressors, disputes a verdict, or changed the architecture. Load the JSON, apply the changes, re-run the script, and add a dated entry under "Changelog" in the report. The stressor history is valuable, so never throw away earlier stressors. If one turns out to be nonsense, mark it `"withdrawn": true` with a reason instead of deleting it.

## Step 1: Pin down the naive architecture

The **naive architecture** is the one under test, as it is today, before any residue is added. Find out what it is:

- **A file or pasted text from the user** (their own context map, canvas, sketch): use it. It's their current thinking.
- **"Our architecture" / "the context map" in VeloCity:** the documented architecture is `docs/domains/velocity/velocity-context-map.md` plus `src/main/cml/VeloCity-Context-Map.cml`. That map is the **reference solution of a workshop exercise**. If the user seems to be in the middle of the discovery or context-mapping exercise, ask whether they want their own map analysed instead, so you don't hand them the spoiler. If they mean the reference map, go ahead.
- **CrowdSorcery:** `docs/domains/crowdsorcery/CrowdSorcery.md` plus the code under `pl.wojtyna.dddtrainingv3.crowdsorcery`. Its `contextmap/` has the context map built in code.
- **A single bounded context** ("stress-test Rentals"): the components are its aggregates, ports, adapters and its relationships to neighbours. Read the package.

Extract a list of **components** (bounded contexts, external parties and, when the scope is code, the important aggregates, ports or adapters), and a list of **relationships** with their DDD pattern (OHS, PL, CF, ACL, U/D, Partnership, Shared Kernel). Relationships matter as much as the components. They are the paths that stress spreads along, and the pattern decides how far it spreads: a Conformist passes upstream changes straight through, while an ACL absorbs them.

Give every component a short stable ID (`ACC`, `REN`, `PAY`...). Write the start of `analysis.json` now (schema in `references/analysis-schema.md`).

## Step 2: Generate the training stressors

Follow `references/stressor-generation.md`. In short:

- Aim for **25-40 stressors** for a whole system map, 15-25 for a single context. Fewer stressors give a matrix too sparse to show patterns. Many more turn into repetition.
- Cover every category in the reference: business model, market and competition, regulation and politics, partners and suppliers, technology and infrastructure, people and organisation, operations and physical world, data and security, and a few deliberately **absurd** ones. Absurd stressors are useful: they reach the same attractors as sensible ones by unexpected routes, and they break the habit of only thinking about what seems likely.
- Make each stressor **concrete and specific to this domain**. "Regulation changes" teaches nothing. "The city requires every ride to end inside a geofenced parking bay, with photo proof" does.
- Don't filter by probability, and don't drop a stressor because the architecture obviously handles it. Survived stressors count too, in the residual index.
- Ask the user for their own stressors too, especially ones based on domain knowledge you don't have. Fold their stressors in, tagged `"source": "user"`. Don't block on the request: generate your own set, and tell them they can add more in a re-run.

## Step 3: Analyse each stressor

For each stressor, record in `analysis.json`:

1. **Attractor.** The state the *business* ends up in under this stressor: "ride-to-money flow stops", "operator runs on manual workarounds", "a city-specific variant of the product". Describe it in business terms, not in terms of the code. Many stressors land on the same few attractors, so reuse an attractor's ID whenever its business state really is the same. Finding the attractors is the main point of the exercise: there are far fewer attractors than stressors, so designing for attractors covers stressors you never listed.
2. **Impacted components.** Every component that must change, or that stops working, when the stressor hits. Follow the relationships: if `PAY` must change and `ACC` and `SET` are Conformists of `PAY`, then they are impacted too, unless the change stays behind Payments' published language. Write down that reasoning in `impact_notes`. Contagion along CF links is exactly what the analysis should reveal.
3. **Naive verdict.** `naive_survives: true` only if the business keeps working in the attractor with a change local to **one** component, or with no change at all. If the change ripples through several contexts, or a capability simply disappears, the naive architecture doesn't survive it. Judge honestly. A survival verdict needs a reason just as much as a failure verdict.
4. **Residue.** The smallest change to the architecture that lets the business survive this stressor: a new boundary, an ACL instead of a CF, a fallback path, moving a responsibility, a published event instead of a synchronous call, removing a shared dependency. Name the components it touches.

## Step 4: Build and read the incidence matrix

Run the script:

```bash
python3 .claude/skills/residuality-analysis/scripts/residuality_matrix.py <folder>/analysis.json
```

It validates the JSON (unknown component IDs, missing attractors) and prints markdown sections: the incidence matrix with row and column totals, the components ranked by impact, the stressors grouped by attractor, **co-impact pairs** (components often hit by the same stressor) marked when there is *no* direct relationship between them, and the residual index once test stressors exist. Paste its output into the report. Don't count by hand: the arithmetic is error-prone, and the user wants a matrix they can trust.

Interpret it, which the script can't do:
- **Column totals (hot spots).** A component hit by a large share of the stressors either carries too many responsibilities or sits at a critical junction. Ask why.
- **Row totals.** A stressor that hits half the map shows a shared assumption baked in everywhere: one payment processor, one city, one currency, one legal entity.
- **Co-impact without a relationship.** Two contexts that change together but have no link on the context map share a hidden assumption, or a hidden coupling. That's the finding the map alone can't show you. Name the assumption.
- **Contagion.** For the top stressors, trace the path through CF relationships, and say where an ACL or a published language would have contained it.

## Step 5: Propose the augmented (residual) architecture

Combine the residues into a set of concrete architecture changes. For each change, give:
- what changes (context, relationship pattern, new or merged context, moved responsibility)
- which stressors and **which attractors** motivate it. A change that serves an attractor (many stressors) is worth more than one that serves a single stressor.
- what it costs (more contexts, more translation, more operational work). Residuality doesn't mean hardening everything: an architecture that tries to survive everything becomes too complex to survive anything. Prefer changes that close several residues at once, and drop residues whose cost outweighs what they protect.

Record the augmented architecture's components and relationships in `analysis.json` under `residual_architecture`, so the next step can judge it.

## Step 6: Test with independent stressors

The augmented architecture is only shown to be better if it survives stressors **that weren't used to design it**. Otherwise the result is circular.

1. Generate a **test set of 10-20 new stressors** independently of the training set. The best way, when you have the Agent tool: spawn one subagent with only the system description, the stressor categories and instructions to write concrete stressors, *without* showing it your training stressors or residues. Otherwise use the techniques in `references/stressor-generation.md` (a different angle, other actors, a random-word prompt), and drop any test stressor that duplicates a training stressor.
2. Judge each test stressor against **both** architectures: `naive_survives` and `residual_survives`, with a one-line reason for each.
3. The script computes the **residual index** `Ri = (Y − X) / S`. X is the number of test stressors the naive architecture survives, Y the number the residual architecture survives, and S the size of the test set. A positive Ri means the augmentation improved robustness to stressors it never saw. Zero or negative means it didn't. Report it as it is, and say what that implies (for example, residues that only fit their own stressors).

## Step 7: Write the report and stop

Fill in `assets/report-template.md` and save `analysis.md` next to `analysis.json`. Then tell the user:
- the paths of both files
- the headline numbers: stressor count, attractor count, naive survival rate on training, Ri on test
- the 3-5 most important findings: hot spots, hidden couplings, attractors
- the proposed architecture changes, ranked
- what you need from them: stressors to add, verdicts they'd dispute, changes they'd accept or reject

**Don't change the architecture artifacts** (CML, `velocity-context-map.md`, code) in this run. The analysis proposes, and the team decides. In VeloCity the context map is also workshop reference material. Offer to apply the accepted changes in a follow-up, keeping the CML, the map document and the code in sync as the VeloCity `CLAUDE.md` asks. Code changes to a context belong to the `bounded-context-scaffolder` or `ddd-tactical-building-blocks` skills.
