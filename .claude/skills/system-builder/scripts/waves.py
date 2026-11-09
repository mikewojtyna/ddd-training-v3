#!/usr/bin/env python3
"""Compute the build waves of a system's bounded contexts from its CML context map.

A context's acceptance tests call its upstreams' real APIs, so upstreams are built (and merged) first.
Wave 1 holds the contexts with no internal upstream; wave n+1 the contexts whose upstreams are all in waves <= n.
External parties (`BoundedContext X { type = SYSTEM }`) are skipped. Partnership / Shared Kernel pairs are built
in the same wave (they evolve together). An upstream/downstream cycle makes waves impossible: the script names it
and exits 1 - fix the map (discover-bounded-contexts) before building.

Usage (from the repository root):
    python3 .claude/skills/system-builder/scripts/waves.py <map.cml> [<contexts.cml> ...]
    python3 .claude/skills/system-builder/scripts/waves.py <map.cml> --merged Payments,Pricing
    python3 .claude/skills/system-builder/scripts/waves.py <map.cml> --event-only "Rentals->Settlement"
    python3 .claude/skills/system-builder/scripts/waves.py <map.cml> --json

--event-only "Up->Down,..."   relationships whose downstream only *listens to* the upstream's published events (no
               command or query calls). They don't order the build: the published event records are written up front
               (stage-4 prep), and the downstream's tests publish the event themselves. Take them from the contract
               catalogue. Without the flag every relationship orders the build, which is safe but slower.
--merged A,B   also print which contexts may start now that A and B are merged (all their upstreams merged),
               so a downstream starts as soon as it can instead of waiting for the whole wave.

Standard library only.
"""

import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from cml_relations import parse  # noqa: E402


class Groups:
    """Union-find for contexts that must be built together (Partnership, Shared Kernel)."""

    def __init__(self, names):
        self.parent = {n: n for n in names}

    def find(self, name):
        while self.parent[name] != name:
            self.parent[name] = self.parent[self.parent[name]]
            name = self.parent[name]
        return name

    def union(self, a, b):
        self.parent[self.find(a)] = self.find(b)


def compute(cmap, event_only=frozenset()):
    contexts = cmap.internal_contexts()
    rels = cmap.internal_relationships()
    groups = Groups(contexts)
    for rel in rels:
        if rel.symmetric:
            groups.union(rel.upstream, rel.downstream)
    upstreams = {c: set() for c in contexts}
    for rel in rels:
        if not rel.symmetric and (rel.upstream, rel.downstream) not in event_only:
            upstreams[rel.downstream].add(rel.upstream)

    def group_members(root):
        return [c for c in contexts if groups.find(c) == root]

    group_up = {}
    for c in contexts:
        root = groups.find(c)
        group_up.setdefault(root, set())
        group_up[root] |= {groups.find(u) for u in upstreams[c]} - {root}

    waves, placed = [], set()
    remaining = set(group_up)
    while remaining:
        ready = sorted(g for g in remaining if group_up[g] <= placed)
        if not ready:
            return waves, upstreams, find_cycle(remaining, group_up, group_members)
        wave = [m for g in ready for m in group_members(g)]
        waves.append(sorted(wave, key=contexts.index))
        placed |= set(ready)
        remaining -= set(ready)
    return waves, upstreams, None


def find_cycle(remaining, group_up, group_members):
    graph = {g: sorted(group_up[g] & remaining) for g in remaining}
    state, stack = {}, []

    def visit(node):
        state[node] = "visiting"
        stack.append(node)
        for nxt in graph[node]:
            if state.get(nxt) == "visiting":
                return stack[stack.index(nxt):] + [nxt]
            if nxt not in state:
                found = visit(nxt)
                if found:
                    return found
        stack.pop()
        state[node] = "done"
        return None

    for node in sorted(graph):
        if node not in state:
            found = visit(node)
            if found:
                # path follows "upstream of" edges backwards; present it as upstream -> downstream
                names = ["+".join(group_members(g)) for g in reversed(found)]
                return names
    return sorted(remaining)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("cml", nargs="+")
    parser.add_argument("--merged", default="", help="comma-separated contexts already merged")
    parser.add_argument("--event-only", default="", help='comma-separated "Up->Down" event-only relationships')
    parser.add_argument("--json", action="store_true")
    args = parser.parse_args()

    cmap = parse(args.cml)
    event_only = set()
    for item in filter(None, (i.strip() for i in args.event_only.split(","))):
        if "->" not in item:
            print(f"ERROR: --event-only entry '{item}' must look like Up->Down")
            return 2
        up, down = (part.strip() for part in item.split("->", 1))
        if not any(r.upstream == up and r.downstream == down and not r.symmetric for r in cmap.relationships):
            print(f"WARN: --event-only {up}->{down} is not a relationship in the map")
        event_only.add((up, down))
    waves, upstreams, cycle = compute(cmap, frozenset(event_only))
    merged = {m.strip() for m in args.merged.split(",") if m.strip()}
    startable = sorted(c for c in upstreams if c not in merged and upstreams[c] <= merged) if merged else []

    if args.json:
        print(json.dumps({
            "waves": waves,
            "upstreams": {c: sorted(u) for c, u in upstreams.items()},
            "external": sorted(cmap.external()),
            "cycle": cycle,
            "startable": startable,
        }, indent=2))
        return 1 if cycle else 0

    if not cmap.relationships:
        print("ERROR: no relationships found in " + ", ".join(args.cml))
        return 1
    for number, wave in enumerate(waves, start=1):
        parts = []
        for c in wave:
            ups = sorted(upstreams[c])
            parts.append(f"{c} (after {', '.join(ups)})" if ups else c)
        print(f"Wave {number}: " + "; ".join(parts))
    if cmap.external():
        print("External (not built): " + ", ".join(sorted(cmap.external())))
    if merged:
        unknown = sorted(merged - set(upstreams))
        if unknown:
            print("WARN: unknown contexts in --merged: " + ", ".join(unknown))
        print("Startable now: " + (", ".join(startable) if startable else "none"))
    if cycle:
        print("ERROR: upstream/downstream cycle, no waves possible beyond this point: " + " -> ".join(cycle))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
