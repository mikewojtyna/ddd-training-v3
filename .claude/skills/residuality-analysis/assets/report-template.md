# Residuality analysis: <System> — <scope>

- **Date:** <YYYY-MM-DD>
- **Naive architecture:** <source files or "pasted by user">
- **Data:** `analysis.json` (re-run `scripts/residuality_matrix.py` after edits)
- **Status:** draft - awaiting review

## Summary

| Metric | Value |
|---|---|
| Training stressors | <n> (<n> from user) |
| Attractors | <n> |
| Naive architecture survives (training) | <x>/<n> |
| Test stressors | <S> |
| Residual index Ri = (Y − X) / S | <value> (X = <x>, Y = <y>) |
| Relationships naive → residual | <k1> → <k2> |

<3-5 sentences: the most important findings and the recommended changes.>

## 1. Naive architecture

<Components table (ID, name, kind) and relationships table (from, to, pattern). Note any assumption made while reading the source.>

## 2. Attractors

<One row per attractor: ID, business state, the stressors that lead there, whether the naive architecture copes.>

## 3. Stressors

<One row per stressor: ID, stressor, category, attractor, impacted components, naive survives (yes/no + reason), residue.>

## 4. Incidence matrix

<Paste the script output: matrix, component ranking, attractor grouping, co-impact pairs.>

### Interpretation

- **Hot spots:** <components with the highest totals and why>
- **Cross-cutting stressors:** <stressors with the highest row totals and the shared assumption they expose>
- **Hidden coupling:** <co-impact pairs without a relationship, and the shared assumption behind each>
- **Contagion:** <how the top stressors spread through CF / PL links and where it could be contained>

## 5. Augmented (residual) architecture

<One row per change: ID, change, stressors, attractors, cost. Then the resulting relationships and a short comment on link count / criticality.>

## 6. Empirical test

<How the test set was generated, and why it is independent. One row per test stressor: ID, stressor, naive (yes/no + reason), residual (yes/no + reason). Then Ri with its arithmetic and what it means.>

## 7. Open questions for the team

- <stressors to add, verdicts you're unsure about, changes that need a business decision>

## Changelog

- <YYYY-MM-DD>: Initial analysis.
