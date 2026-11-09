#!/usr/bin/env python3
"""Create and check the communication matrix: proof that every context-map relationship really works in code.

One row per message that crosses a relationship (event, command or query), with the effect it must cause on the
receiving side and the test that proves it. An event row is proven only when a test shows the *consumer reacted*
(its state or its own published event changed), not merely that the producer published.

Usage (from the repository root):
    python3 .claude/skills/system-builder/scripts/comm_matrix.py init  <map.cml> [more.cml] [--external]
    python3 .claude/skills/system-builder/scripts/comm_matrix.py check <matrix.md> --cml <map.cml> [more.cml]
                                                                     [--tests src/test] [--async-marker await,eventually]

init   prints a matrix skeleton (markdown table) with one TODO row per internal relationship, using the CML
       comment above each relationship as a hint. --external adds relationships with external parties too.
check  validates a filled matrix:
       ERROR  an internal relationship of the map has no row (untested relationship);
       ERROR  a row names a relationship that isn't in the map (code and map drifted apart - fix one of them);
       ERROR  kind not one of event / command / query, empty message or effect, TODO left in a row;
       ERROR  no proving test, or the test class / method can't be found under --tests;
       WARN   an event row whose test file mentions none of --async-marker (comma-separated, default
              "await,eventually"): async consumers must be awaited (Awaitility), never asserted immediately.
       Exit code 1 on any ERROR.

Matrix columns (see assets/communication-matrix-template.md):
| # | Relationship | Message | Kind | Effect in the receiving context | Proving test |
Relationship is written as in the map: `Upstream -> Downstream`. Proving test: `ClassName#method`, `ClassName`
or a path; several separated by commas.

Standard library only.
"""

import argparse
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from cml_relations import parse  # noqa: E402

KINDS = {"event", "command", "query"}
HEADER = ("| # | Relationship | Message | Kind | Effect in the receiving context | Proving test |\n"
          "|---|---|---|---|---|---|")


def init(args):
    cmap = parse(args.cml)
    rels = cmap.relationships if args.external else cmap.internal_relationships()
    print(HEADER)
    for number, rel in enumerate(rels, start=1):
        hint = f"TODO (map: {rel.comment})" if rel.comment else "TODO"
        name = f" : {rel.name}" if rel.name else ""
        print(f"| M{number} | {rel.label()}{name} | {hint} | TODO | TODO | TODO |")
    return 0


def read_rows(path):
    rows = []
    with open(path, encoding="utf-8") as handle:
        for number, line in enumerate(handle, start=1):
            if not line.lstrip().startswith("|"):
                continue
            cells = [c.strip() for c in line.strip().strip("|").split("|")]
            if len(cells) < 6 or cells[0] in ("#", "") or set(cells[0]) <= set("-: "):
                continue
            rows.append((number, cells))
    return rows


def rel_key(text):
    text = re.sub(r"`", "", text).split(":", 1)[0].strip()
    match = re.match(r"^([A-Za-z_][\w]*)\s*(<->|->|<-)\s*([A-Za-z_][\w]*)$", text)
    if not match:
        return None
    a, arrow, b = match.groups()
    if arrow == "<-":
        return (b, a, False)
    if arrow == "<->":
        return tuple(sorted((a, b))) + (True,)
    return (a, b, False)


def index_tests(root):
    files = {}
    for dirpath, _, names in os.walk(root):
        for name in names:
            if name.endswith((".java", ".kt", ".groovy", ".scala", ".py", ".ts", ".js", ".cs", ".go")):
                files.setdefault(os.path.splitext(name)[0], []).append(os.path.join(dirpath, name))
    return files


def find_test(ref, files, root):
    ref = ref.strip().strip("`").strip()
    if not ref:
        return None, "empty test reference"
    if "/" in ref or os.sep in ref:
        path = ref.split("#", 1)[0]
        return (path, None) if os.path.exists(path) else (None, f"file not found: {path}")
    cls, _, method = ref.partition("#")
    cls = cls.split(".")[-1]
    candidates = files.get(cls)
    if not candidates:
        return None, f"test class {cls} not found under {root}"
    if method:
        method = method.split("(")[0]
        for path in candidates:
            with open(path, encoding="utf-8", errors="replace") as handle:
                if re.search(rf"\b{re.escape(method)}\b", handle.read()):
                    return path, None
        return None, f"method {method} not found in {cls}"
    return candidates[0], None


def check(args):
    cmap = parse(args.cml)
    rels = cmap.internal_relationships()
    expected = {}
    for rel in rels:
        key = tuple(sorted((rel.upstream, rel.downstream))) + (True,) if rel.symmetric else (rel.upstream, rel.downstream, False)
        expected[key] = rel
    known = {}
    for rel in cmap.relationships:
        key = tuple(sorted((rel.upstream, rel.downstream))) + (True,) if rel.symmetric else (rel.upstream, rel.downstream, False)
        known[key] = rel
    files = index_tests(args.tests) if os.path.isdir(args.tests) else {}
    if not files:
        print(f"WARN: no test sources found under {args.tests}")

    errors = warnings = 0
    covered = set()
    rows = read_rows(args.matrix)
    if not rows:
        print(f"ERROR: no matrix rows found in {args.matrix}")
        return 1
    for line, cells in rows:
        row_id, relationship, message, kind, effect, tests = cells[:6]
        where = f"{args.matrix}:{line} {row_id}"
        key = rel_key(relationship)
        if key is None:
            print(f"ERROR {where}: relationship '{relationship}' must look like 'Up -> Down'")
            errors += 1
            continue
        if key not in known:
            print(f"ERROR {where}: '{relationship}' isn't a relationship in the map (code and map drifted apart)")
            errors += 1
        else:
            covered.add(key)
        if any("TODO" in c for c in (message, kind, effect, tests)):
            print(f"ERROR {where}: TODO left in the row")
            errors += 1
            continue
        if kind.lower() not in KINDS:
            print(f"ERROR {where}: kind '{kind}' must be one of event, command, query")
            errors += 1
        if not message.strip() or not effect.strip():
            print(f"ERROR {where}: message and effect must be filled in")
            errors += 1
        refs = [r for r in re.split(r",\s*", tests) if r.strip(" `")]
        if not refs:
            print(f"ERROR {where}: no proving test")
            errors += 1
            continue
        for ref in refs:
            path, problem = find_test(ref, files, args.tests)
            if problem:
                print(f"ERROR {where}: {problem}")
                errors += 1
            elif kind.lower() == "event" and path and args.async_marker:
                markers = [m.strip() for m in args.async_marker.split(",") if m.strip()]
                with open(path, encoding="utf-8", errors="replace") as handle:
                    content = handle.read()
                    if not any(m in content for m in markers):
                        print(f"WARN {where}: event test {ref} never mentions '{args.async_marker}' - "
                              f"async consumers must be awaited")
                        warnings += 1
    for key, rel in expected.items():
        if key not in covered:
            print(f"ERROR: relationship {rel.label()}{(' : ' + rel.name) if rel.name else ''} "
                  f"(map line {rel.line}) has no row: nothing proves it works")
            errors += 1
    print(f"{len(rows)} rows, {len(expected)} internal relationships, {errors} errors, {warnings} warnings")
    return 1 if errors else 0


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest="command", required=True)
    p_init = sub.add_parser("init")
    p_init.add_argument("cml", nargs="+")
    p_init.add_argument("--external", action="store_true")
    p_check = sub.add_parser("check")
    p_check.add_argument("matrix")
    p_check.add_argument("--cml", nargs="+", required=True)
    p_check.add_argument("--tests", default="src/test")
    p_check.add_argument("--async-marker", default="await,eventually")
    args = parser.parse_args()
    return init(args) if args.command == "init" else check(args)


if __name__ == "__main__":
    sys.exit(main())
