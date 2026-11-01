#!/usr/bin/env python3
"""Build the residuality incidence matrix and residual index from analysis.json.

Usage: residuality_matrix.py <analysis.json> [--top N]

Prints markdown to stdout. Validation problems are printed under "Validation"
and make the exit code 1, but the tables are still printed so you can see
the partial picture.
"""
import argparse
import itertools
import json
import sys
from collections import defaultdict


def load(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def validate(data):
    problems = []
    naive = data.get("naive_architecture", {})
    comps = naive.get("components", [])
    comp_ids = [c.get("id") for c in comps]
    known = set(comp_ids)
    if not comps:
        problems.append("naive_architecture.components is empty")
    dup = {c for c in comp_ids if comp_ids.count(c) > 1}
    if dup:
        problems.append(f"duplicate component ids: {sorted(dup)}")
    for r in naive.get("relationships", []):
        for end in ("from", "to"):
            if r.get(end) not in known:
                problems.append(f"relationship {r.get('from')}->{r.get('to')}: unknown component '{r.get(end)}'")
    attractor_ids = {a.get("id") for a in data.get("attractors", [])}
    seen = set()
    for s in data.get("stressors", []):
        sid = s.get("id", "?")
        if sid in seen:
            problems.append(f"duplicate stressor id {sid}")
        seen.add(sid)
        if s.get("withdrawn"):
            continue
        for c in s.get("impacts", []):
            if c not in known:
                problems.append(f"{sid}: unknown impacted component '{c}'")
        for c in s.get("residue_components", []):
            if not c.startswith("+") and c not in known:
                problems.append(f"{sid}: unknown residue component '{c}' (prefix new components with '+')")
        if s.get("attractor") not in attractor_ids:
            problems.append(f"{sid}: attractor '{s.get('attractor')}' is not defined")
        if not isinstance(s.get("naive_survives"), bool):
            problems.append(f"{sid}: naive_survives must be true/false")
        if not s.get("impacts"):
            problems.append(f"{sid}: no impacted components")
    for t in data.get("test_stressors", []):
        for key in ("naive_survives", "residual_survives"):
            if not isinstance(t.get(key), bool):
                problems.append(f"{t.get('id', '?')}: {key} not set; excluded from residual index")
    return problems


def short(text, n=70):
    text = " ".join(str(text).split())
    return text if len(text) <= n else text[: n - 1] + "…"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("path")
    ap.add_argument("--top", type=int, default=10, help="number of co-impact pairs to list")
    args = ap.parse_args()
    data = load(args.path)
    problems = validate(data)

    naive = data.get("naive_architecture", {})
    comps = [c["id"] for c in naive.get("components", []) if "id" in c]
    names = {c["id"]: c.get("name", c["id"]) for c in naive.get("components", []) if "id" in c}
    related = set()
    for r in naive.get("relationships", []):
        related.add(frozenset((r.get("from"), r.get("to"))))
    stressors = [s for s in data.get("stressors", []) if not s.get("withdrawn")]
    withdrawn = [s for s in data.get("stressors", []) if s.get("withdrawn")]
    attractors = {a["id"]: a.get("name", a["id"]) for a in data.get("attractors", []) if "id" in a}

    out = []
    out.append("### Incidence matrix (training stressors)\n")
    out.append("| Stressor | " + " | ".join(comps) + " | Σ | Naive survives |")
    out.append("|---|" + "|".join([":-:"] * len(comps)) + "|:-:|:-:|")
    col = defaultdict(int)
    for s in stressors:
        hits = set(s.get("impacts", []))
        row = []
        for c in comps:
            if c in hits:
                row.append("1")
                col[c] += 1
            else:
                row.append("·")
        surv = "yes" if s.get("naive_survives") is True else "no" if s.get("naive_survives") is False else "?"
        out.append(f"| {s.get('id')} | " + " | ".join(row) + f" | {len(hits & set(comps))} | {surv} |")
    out.append("| **Σ** | " + " | ".join(f"**{col[c]}**" for c in comps) + f" | | |")
    n = len(stressors)
    survived = sum(1 for s in stressors if s.get("naive_survives") is True)
    out.append("")
    out.append(f"Training stressors: {n} (withdrawn: {len(withdrawn)}). "
               f"Naive architecture survives {survived}/{n}"
               + (f" ({survived / n:.0%})." if n else "."))

    out.append("\n### Components ranked by impact\n")
    out.append("| Rank | Component | Stressors hitting it | Share |")
    out.append("|---|---|---|---|")
    for i, c in enumerate(sorted(comps, key=lambda c: (-col[c], c)), 1):
        share = f"{col[c] / n:.0%}" if n else "-"
        out.append(f"| {i} | {c} ({names[c]}) | {col[c]} | {share} |")

    out.append("\n### Stressors by attractor\n")
    out.append("| Attractor | Stressors | Count | Naive survives |")
    out.append("|---|---|---|---|")
    by_attr = defaultdict(list)
    for s in stressors:
        by_attr[s.get("attractor")].append(s)
    for aid in sorted(by_attr, key=lambda a: (-len(by_attr[a]), str(a))):
        group = by_attr[aid]
        ok = sum(1 for s in group if s.get("naive_survives") is True)
        label = f"{aid} {attractors.get(aid, '(undefined)')}"
        out.append(f"| {label} | {', '.join(s.get('id') for s in group)} | {len(group)} | {ok}/{len(group)} |")
    unused = sorted(set(attractors) - set(by_attr))
    if unused:
        out.append(f"\nAttractors with no stressor: {', '.join(unused)}")

    out.append("\n### Co-impact pairs (components hit by the same stressor)\n")
    pairs = defaultdict(list)
    for s in stressors:
        hits = sorted(set(s.get("impacts", [])) & set(comps))
        for a, b in itertools.combinations(hits, 2):
            pairs[(a, b)].append(s.get("id"))
    ranked = sorted(pairs.items(), key=lambda kv: (-len(kv[1]), kv[0]))
    ranked = [kv for kv in ranked if len(kv[1]) >= 2][: args.top]
    if ranked:
        out.append("| Pair | Shared stressors | Count | Direct relationship? |")
        out.append("|---|---|---|---|")
        for (a, b), sids in ranked:
            rel = "yes" if frozenset((a, b)) in related else "**no: hidden coupling candidate**"
            out.append(f"| {a} + {b} | {', '.join(sids)} | {len(sids)} | {rel} |")
    else:
        out.append("No pair of components shares two or more stressors.")

    tests = data.get("test_stressors", [])
    out.append("\n### Residual index (test stressors)\n")
    complete = [t for t in tests if isinstance(t.get("naive_survives"), bool) and isinstance(t.get("residual_survives"), bool)]
    if not complete:
        out.append("No test stressors with both verdicts yet.")
    else:
        out.append("| Test stressor | Naive | Residual |")
        out.append("|---|:-:|:-:|")
        for t in complete:
            out.append(f"| {t.get('id')} {short(t.get('text', ''))} | "
                       f"{'yes' if t['naive_survives'] else 'no'} | {'yes' if t['residual_survives'] else 'no'} |")
        S = len(complete)
        X = sum(1 for t in complete if t["naive_survives"])
        Y = sum(1 for t in complete if t["residual_survives"])
        ri = (Y - X) / S
        regressions = [t.get("id") for t in complete if t["naive_survives"] and not t["residual_survives"]]
        out.append("")
        out.append(f"S = {S}, X (naive survives) = {X}, Y (residual survives) = {Y}")
        out.append(f"**Ri = (Y − X) / S = ({Y} − {X}) / {S} = {ri:+.2f}**")
        if regressions:
            out.append(f"Regressions (naive survives, residual does not): {', '.join(regressions)}")

    rel_naive = len(naive.get("relationships", []))
    res = data.get("residual_architecture") or {}
    if res.get("relationships") is not None:
        out.append(f"\nRelationships: naive {rel_naive} → residual {len(res['relationships'])}; "
                   f"components: naive {len(comps)} → residual {len(res.get('components', []))}")

    if problems:
        out.insert(0, "### Validation\n\n" + "\n".join(f"- {p}" for p in problems) + "\n")
    print("\n".join(out))
    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
