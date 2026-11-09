#!/usr/bin/env python3
"""Check that every contract in the catalogue is built by some plan.

The contract catalogue (integration-contracts.md) promises Open Host Service resources and published integration
events. Planners place spec rules, so a contract that no spec mentions (for example an event added to the catalogue
for a downstream listener) can end up in no plan at all. Nobody builds it, and the consumer's builder waits for it.
This script finds those holes before Stage 4.

Usage:
    python3 catalogue_coverage.py <integration-contracts.md> <plans-dir> [--json]

It extracts:
- resources: every backticked `METHOD /path` (GET, POST, PUT, PATCH, DELETE);
- published events: backticked names in a line starting with "Publishes" or in a table that follows a "Publishes" line.

A resource counts as covered when some plan mentions the same method and path (path parameters may have any name:
`{id}` matches `{loanId}`). An event counts as covered when some plan mentions its name. Exit code 1 when anything is
uncovered. Standard library only.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

RESOURCE = re.compile(r"`\s*(GET|POST|PUT|PATCH|DELETE)\s+(/[^`\s]*)[^`]*`")
BACKTICKED_NAME = re.compile(r"`([A-Z][A-Za-z0-9]+)(?:\s*\{[^`]*\})?`")
PUBLISHES = re.compile(r"^[-*\s]*\**publish(es)?\b", re.IGNORECASE)
CONSUMES = re.compile(r"\bconsume(s)?\b|\blisten", re.IGNORECASE)


def path_regex(path: str) -> re.Pattern:
    parts = re.split(r"(\{[^}]*\})", path.rstrip("/"))
    rx = "".join(r"\{[^}/]*\}" if p.startswith("{") else re.escape(p) for p in parts if p)
    return re.compile(rx + r"(?![A-Za-z0-9_-])")


def parse_catalogue(text: str):
    resources: list[tuple[str, str, int]] = []
    events: dict[str, int] = {}
    in_publish_table = False
    for n, line in enumerate(text.splitlines(), 1):
        stripped = line.strip()
        for m in RESOURCE.finditer(line):
            resources.append((m.group(1), m.group(2), n))
        if stripped.startswith("#"):
            in_publish_table = False
            continue
        if PUBLISHES.search(stripped) and not stripped.startswith("|"):
            in_publish_table = True
            for m in BACKTICKED_NAME.finditer(stripped):
                events.setdefault(m.group(1), n)
            continue
        if stripped.startswith("|") and in_publish_table:
            first_cell = stripped.strip("|").split("|")[0]
            for m in BACKTICKED_NAME.finditer(first_cell):
                events.setdefault(m.group(1), n)
            continue
        if stripped and not stripped.startswith("|") and (CONSUMES.search(stripped) or in_publish_table):
            in_publish_table = False
    return resources, events


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("catalogue", type=Path)
    ap.add_argument("plans", type=Path)
    ap.add_argument("--json", action="store_true")
    args = ap.parse_args()

    resources, events = parse_catalogue(args.catalogue.read_text(encoding="utf-8"))
    plans = {p.stem: p.read_text(encoding="utf-8") for p in sorted(args.plans.glob("*.md")) if p.name != "README.md"}
    if not plans:
        print(f"ERROR: no plans in {args.plans}", file=sys.stderr)
        return 2

    report = {"resources": [], "events": []}
    for method, path, line in resources:
        rx = path_regex(path)
        hits = [name for name, text in plans.items()
                if any(rx.search(l) and method in l for l in text.splitlines())]
        report["resources"].append({"item": f"{method} {path}", "line": line, "plans": hits})
    for name, line in events.items():
        hits = [p for p, text in plans.items() if re.search(rf"\b{re.escape(name)}\b", text)]
        report["events"].append({"item": name, "line": line, "plans": hits})

    uncovered = [r for kind in report.values() for r in kind if not r["plans"]]
    if args.json:
        print(json.dumps({**report, "uncovered": len(uncovered)}, indent=2))
    else:
        for kind, rows in report.items():
            print(f"{kind}:")
            for r in rows:
                status = ", ".join(r["plans"]) if r["plans"] else "NOT IN ANY PLAN"
                print(f"  {r['item']}  (catalogue line {r['line']}): {status}")
        for r in uncovered:
            print(f"ERROR: {r['item']} (catalogue line {r['line']}) is promised by the catalogue but no plan builds it")
        print(f"\n{len(uncovered)} uncovered of {len(report['resources']) + len(report['events'])}")
    return 1 if uncovered else 0


if __name__ == "__main__":
    sys.exit(main())
