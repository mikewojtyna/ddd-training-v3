# Residuality theory: concepts and vocabulary

Sources: Barry M. O'Reilly, *Residues: Time, Change, and Uncertainty in Software Architecture* (Leanpub, 2024); "An Introduction to Residuality Theory: Software Design Heuristics for Complex Systems" (Procedia Computer Science 170, 2020); "Residuality Theory, Random Simulation, and Attractor Networks" (Procedia Computer Science 201, 2022).

## Contents
1. Why: hyperliminal systems
2. Stressor
3. Attractor
4. Residue
5. Naive and residual architecture
6. Incidence matrix
7. Contagion
8. Residual index
9. NKP and criticality
10. What residuality is not
11. How it maps to DDD

---

## 1. Why: hyperliminal systems

A software system is an ordered, mostly predictable thing. It runs inside a business environment that is complex and unpredictable: markets, regulators, partners, people. O'Reilly calls this combination **hyperliminal**. Requirements analysis assumes the environment can be predicted and listed. Residuality accepts that it can't, and asks a different question: *what structure keeps working in many futures, including the ones we didn't predict?*

## 2. Stressor

A **stressor** is any fact about the environment that could change: a new law, the loss of a supplier, a competitor's move, a staff strike, a cloud region outage, a flood, a pandemic, a meteor. Stressors are **not risks**. Their probability doesn't matter, and an impossible-sounding stressor is often as useful as a likely one, because it can reach a real attractor through a route nobody had considered. What counts is how many there are, how varied they are, and how concrete.

## 3. Attractor

A complex system under stress doesn't go just anywhere. It settles into one of a small number of stable states, called **attractors** (the term comes from Kauffman's random Boolean networks). For a business these are states such as "can't take money", "runs on manual workarounds", "splits into regional variants", "loses its supply of X". Many different stressors lead to the same attractor. That is the key insight: **designing for the attractors covers stressors you never listed.** Describe attractors in business terms.

## 4. Residue

The **residue** of a stressor is what's left of the architecture after the stressor hits, and the design work is to make that residue *still useful to the business in the attractor*. In practice you write down the change that makes the residue survive. Examples: a fallback path, a new boundary, an ACL that absorbs an upstream change, a capability moved to the context that will still exist.

## 5. Naive and residual architecture

- **Naive architecture**: the starting design, built to meet the functional requirements. Usually the first context map.
- **Residual (augmented) architecture**: the naive architecture with the selected residues integrated. It's still one coherent architecture, not a list of patches. Residues get merged, generalised, and sometimes dropped when they cost too much.

## 6. Incidence matrix

A matrix with **stressors as rows and components as columns**, where a 1 means "this stressor impacts this component".
- **Column totals** show the components that are most exposed, often doing too much, or sitting at a junction.
- **Row totals** show stressors that cut across the whole system, which usually means a shared assumption.
- **Columns that move together** (components impacted by the same stressors) show coupling. If the context map has no relationship between them, the coupling is *hidden*: a shared assumption, shared data, or a shared external dependency. This is often the most valuable output, because it's invisible on the map.

## 7. Contagion

Stress spreads along relationships. In DDD terms the relationship pattern decides how far it goes:
- a **Conformist** downstream inherits every upstream model change
- **Shared Kernel** and **Partnership** links move changes in both directions
- an **ACL** absorbs upstream change at the boundary
- an **OHS with a Published Language** keeps internal upstream changes from leaking, but changes to the language itself still spread to every consumer

Tracing contagion tells you where a different pattern would contain a stressor.

## 8. Residual index

The empirical test. Generate a **test set** of stressors that weren't used to design the residual architecture, and apply them to both architectures:

```
Ri = (Y − X) / S
```

where X is the number of test stressors the naive architecture survives, Y the number the residual architecture survives, and S the number of test stressors. The range is −1 to 1. Ri > 0 means the residual architecture copes better with stressors it was never designed for, which is the claim residuality makes. A training set that was also the test set would prove nothing.

## 9. NKP and criticality

O'Reilly models architectures as networks with N nodes (components), K links per node and P bias (how predictable each node's behaviour is). With too few links, the system is rigid and can't adapt. With too many, any change cascades into chaos. Robust systems sit near **criticality**, between the two. Practical reading: residues that add links (more integrations, more shared data) push the system toward chaos. Residues that cut or loosen links (events instead of calls, ACLs, removing shared dependencies) usually move it back toward criticality. Count the relationships before and after, and comment on the change.

## 10. What residuality is not

- **Not risk management.** No probabilities, no ranking by likelihood, no mitigation register.
- **Not "handle every case".** You don't need to survive every stressor. The goal is that most stressors, including unseen ones, land on attractors the architecture copes with.
- **Not a one-off.** New stressors keep arriving. The analysis is a living artefact and gets re-run.
- **Not a replacement for domain modelling.** It stresses a model. It doesn't discover one.

## 11. How it maps to DDD

| Residuality | DDD artefact |
|---|---|
| component | bounded context, external party; inside a context: aggregate, port, adapter |
| link | context-map relationship, with its pattern |
| residue | boundary change, pattern change (CF → ACL), new context, moved responsibility, event instead of call |
| hidden coupling | a missing or mis-typed relationship on the context map, a shared assumption in several ubiquitous languages |
| attractor | a business capability state; compare with the core and supporting domain classification |
