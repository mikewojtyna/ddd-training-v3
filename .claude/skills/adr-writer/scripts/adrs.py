#!/usr/bin/env python3
"""Validate Architecture Decision Records in docs/adr and change their status.

Usage (from the repository root):
    python3 .claude/skills/adr-writer/scripts/adrs.py check     [<id|file>] [--dir docs/adr]
    python3 .claude/skills/adr-writer/scripts/adrs.py accept    <id>        [--dir docs/adr]
    python3 .claude/skills/adr-writer/scripts/adrs.py supersede <old> <new> [--dir docs/adr]
    python3 .claude/skills/adr-writer/scripts/adrs.py deprecate <id>        [--dir docs/adr]

check      validates one ADR, or all of them; numbering and supersede links are always checked across the folder.
           Prints ERROR/WARN lines; exit code 1 if any ERROR.
accept     proposed -> accepted, only if check has no errors for that ADR.
supersede  approves <new> as the replacement of <old>: <new> -> accepted (supersedes: <old>),
           <old> -> superseded (superseded_by: <new>). Only if check has no errors for <new>.
deprecate  accepted -> deprecated (the decision no longer applies and nothing replaces it).

An <id> is a number (7, 0007), a file name (0007-use-postgresql.md) or a path. Standard library only.
"""
from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

REQUIRED_FIELDS = ["title", "status", "date"]
STATUSES = {"proposed", "accepted", "superseded", "deprecated"}
SECTIONS = ["Context", "Decision drivers", "Considered options", "Decision", "Consequences", "Links"]
MUST_HAVE_CONTENT = {"Context", "Decision drivers", "Considered options", "Decision", "Consequences"}
FILE_NAME = re.compile(r"^(\d{4})-[a-z0-9]+(?:-[a-z0-9]+)*\.md$")
PLACEHOLDER = re.compile(r"<[^<>\n]{2,}>")
SOLUTION_FIRST = re.compile(r"\b(we (?:will|chose|decided|use)|decision is)\b", re.IGNORECASE)


@dataclass
class Adr:
    path: Path
    number: int | None
    meta: dict
    title_line: str
    sections: dict = field(default_factory=dict)
    subsections: dict = field(default_factory=dict)


def strip_comment(value: str) -> str:
    return re.sub(r"\s+#.*$", "", value).strip()


def parse(path: Path) -> Adr:
    text = path.read_text(encoding="utf-8")
    meta, body = {}, text
    m = re.match(r"^---\s*\n(.*?)\n---\s*\n?(.*)$", text, re.DOTALL)
    if m:
        for line in m.group(1).splitlines():
            if ":" in line and not line.lstrip().startswith("#"):
                k, v = line.split(":", 1)
                meta[k.strip()] = strip_comment(v)
        body = m.group(2)
    body = re.sub(r"<!--.*?-->", "", body, flags=re.DOTALL)
    title_line, sections, subs, cur, cur_sub = "", {}, {}, None, None
    for line in body.splitlines():
        if not title_line and re.match(r"^#\s+\S", line):
            title_line = line[1:].strip()
            continue
        h2 = re.match(r"^##\s+(?!#)(.+?)\s*$", line)
        h3 = re.match(r"^###\s+(.+?)\s*$", line)
        if h2:
            cur, cur_sub = h2.group(1), None
            sections[cur], subs[cur] = [], {}
        elif cur is not None:
            sections[cur].append(line)
            if h3:
                cur_sub = h3.group(1)
                subs[cur][cur_sub] = []
            elif cur_sub is not None:
                subs[cur][cur_sub].append(line)
    fm = FILE_NAME.match(path.name)
    return Adr(path, int(fm.group(1)) if fm else None, meta, title_line,
               {k: "\n".join(v).strip() for k, v in sections.items()},
               {k: {s: "\n".join(t).strip() for s, t in v.items()} for k, v in subs.items()})


def ref(value: str) -> int | None:
    """'3', '0003', 'ADR-0003', '0003-foo.md' -> 3."""
    m = re.search(r"(\d+)", value or "")
    return int(m.group(1)) if m else None


class Report:
    def __init__(self):
        self.errors = self.warnings = 0
        self.errors_by_file: dict[str, int] = {}

    def err(self, where, msg):
        self.errors += 1
        self.errors_by_file[where] = self.errors_by_file.get(where, 0) + 1
        print(f"ERROR {where}: {msg}")

    def warn(self, where, msg):
        self.warnings += 1
        print(f"WARN  {where}: {msg}")


def check_adr(a: Adr, by_number: dict[int, list[Adr]], rep: Report) -> None:
    w = a.path.name
    if a.number is None:
        rep.err(w, "file name must be NNNN-<kebab-case-slug>.md, e.g. 0007-use-postgresql.md")
    for f in REQUIRED_FIELDS:
        if not a.meta.get(f):
            rep.err(w, f"frontmatter field '{f}' is missing or empty")
    status = a.meta.get("status", "")
    if status and status not in STATUSES:
        rep.err(w, f"status '{status}' is not one of {sorted(STATUSES)}")
    if a.meta.get("date") and not re.fullmatch(r"\d{4}-\d{2}-\d{2}", a.meta["date"]):
        rep.err(w, f"date '{a.meta['date']}' is not YYYY-MM-DD")
    if not a.title_line:
        rep.err(w, "no '# NNNN. <title>' heading")
    elif a.number is not None and ref(a.title_line) != a.number:
        rep.err(w, f"heading '{a.title_line}' doesn't start with the file's number {a.number:04d}")

    for s in SECTIONS:
        if s not in a.sections:
            rep.err(w, f"section '## {s}' is missing")
        elif s in MUST_HAVE_CONTENT and not re.sub(r"(?m)^#+.*$", "", a.sections[s]).strip():
            rep.err(w, f"section '## {s}' is empty")

    options = [o for o, text in a.subsections.get("Considered options", {}).items()]
    if "Considered options" in a.sections and len(options) < 2:
        rep.err(w, f"'## Considered options' lists {len(options)} option(s) as '### <name>'; an ADR needs at least two")
    cons = a.subsections.get("Consequences", {})
    if "Consequences" in a.sections:
        if not cons.get("Negative", "").strip(" -\n"):
            rep.err(w, "'### Negative' consequences are missing or empty; every decision has a cost")
        if not cons.get("Positive", "").strip(" -\n"):
            rep.warn(w, "'### Positive' consequences are missing or empty")
    first_context_line = a.sections.get("Context", "").split(".")[0]
    if SOLUTION_FIRST.search(first_context_line):
        rep.warn(w, "Context opens with the decision; describe the problem and forces first")
    for s, text in a.sections.items():
        hits = PLACEHOLDER.findall(text)
        if hits:
            rep.warn(w, f"section '## {s}' still has template placeholders: {', '.join(hits[:3])}")

    # supersede links, both ways
    sup_by, sup = ref(a.meta.get("superseded_by", "")), ref(a.meta.get("supersedes", ""))
    if status == "superseded":
        if sup_by is None:
            rep.err(w, "status is superseded but 'superseded_by' is empty")
        elif sup_by not in by_number:
            rep.err(w, f"superseded_by {sup_by:04d}, but no such ADR exists")
        else:
            succ = by_number[sup_by][0]
            if ref(succ.meta.get("supersedes", "")) != a.number:
                rep.err(w, f"successor {succ.path.name} doesn't say 'supersedes: {a.number}'")
            if succ.meta.get("status") != "accepted":
                rep.err(w, f"successor {succ.path.name} is '{succ.meta.get('status')}', not accepted")
    elif sup_by is not None:
        rep.err(w, f"superseded_by is set but status is '{status}'; use adrs.py supersede")
    if sup is not None:
        if sup == a.number:
            rep.err(w, "an ADR can't supersede itself")
        elif sup not in by_number:
            rep.err(w, f"supersedes {sup:04d}, but no such ADR exists")
        else:
            old = by_number[sup][0]
            if status == "accepted" and old.meta.get("status") != "superseded":
                rep.err(w, f"accepted, but the ADR it supersedes ({old.path.name}) is still "
                           f"'{old.meta.get('status')}'; use adrs.py supersede {sup} {a.number}")
            if status == "accepted" and f"{sup:04d}" not in a.sections.get("Links", ""):
                rep.warn(w, f"'## Links' doesn't mention ADR-{sup:04d}")


def check_numbering(adrs: list[Adr], by_number: dict[int, list[Adr]], rep: Report) -> None:
    for n, same in sorted(by_number.items()):
        if len(same) > 1:
            for a in same:
                rep.err(a.path.name, f"number {n:04d} is used by {len(same)} files: "
                                     f"{', '.join(x.path.name for x in same)}")
    numbers = sorted(by_number)
    for expected, actual in zip(range(1, len(numbers) + 1), numbers):
        if expected != actual:
            a = by_number[actual][0]
            rep.err(a.path.name, f"numbering isn't sequential: expected {expected:04d}, found {actual:04d}")
            break


def load(directory: Path) -> tuple[list[Adr], dict[int, list[Adr]]]:
    adrs = [parse(p) for p in sorted(directory.glob("*.md")) if p.name.lower() != "readme.md"]
    by_number: dict[int, list[Adr]] = {}
    for a in adrs:
        if a.number is not None:
            by_number.setdefault(a.number, []).append(a)
    return adrs, by_number


def find(adrs: list[Adr], key: str) -> Adr | None:
    name = Path(key).name
    for a in adrs:
        if a.path.name == name:
            return a
    n = ref(name)
    matches = [a for a in adrs if a.number == n]
    return matches[0] if len(matches) == 1 else None


def set_field(path: Path, key: str, value: str) -> None:
    text = path.read_text(encoding="utf-8")
    m = re.match(r"^---\s*\n(.*?)\n---", text, re.DOTALL)
    if not m:
        raise SystemExit(f"ERROR {path}: no frontmatter")
    fm = m.group(1)
    pattern = re.compile(rf"(?m)^({re.escape(key)}:)[^\n#]*?(\s*#.*)?$")
    if pattern.search(fm):
        fm = pattern.sub(lambda x: f"{x.group(1)} {value}" + (x.group(2) or ""), fm, count=1)
    else:
        fm = fm + f"\n{key}: {value}"
    path.write_text(text[:m.start(1)] + fm + text[m.end(1):], encoding="utf-8")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("command", choices=["check", "accept", "supersede", "deprecate"])
    ap.add_argument("ids", nargs="*")
    ap.add_argument("--dir", default="docs/adr", type=Path)
    args = ap.parse_args()
    needed = {"check": (0, 1), "accept": (1, 1), "deprecate": (1, 1), "supersede": (2, 2)}[args.command]
    if not needed[0] <= len(args.ids) <= needed[1]:
        print(f"ERROR {args.command} takes {needed[0]}-{needed[1]} id(s), got {len(args.ids)}")
        return 1
    if not args.dir.is_dir():
        print(f"ERROR {args.dir} does not exist")
        return 1
    adrs, by_number = load(args.dir)
    targets = []
    for key in args.ids:
        a = find(adrs, key)
        if a is None:
            print(f"ERROR no single ADR matches '{key}' in {args.dir}")
            return 1
        targets.append(a)

    rep = Report()
    for a in (targets[-1:] if args.command == "check" and targets else adrs):
        check_adr(a, by_number, rep)
    check_numbering(adrs, by_number, rep)
    print(f"{len(adrs)} ADR(s), {rep.errors} error(s), {rep.warnings} warning(s)")
    if args.command == "check":
        return 1 if rep.errors else 0

    def blocked(a: Adr) -> bool:
        if rep.errors_by_file.get(a.path.name):
            print(f"Not changed: fix the errors in {a.path.name} first.")
            return True
        return False

    if args.command == "accept":
        a = targets[0]
        if a.meta.get("status") != "proposed":
            print(f"ERROR {a.path.name} is '{a.meta.get('status')}'; only a proposed ADR can be accepted")
            return 1
        if ref(a.meta.get("supersedes", "")) is not None:
            print(f"ERROR {a.path.name} supersedes an ADR; use: supersede {a.meta['supersedes']} {a.number}")
            return 1
        if blocked(a):
            return 1
        set_field(a.path, "status", "accepted")
        print(f"accepted {a.path.name}")
    elif args.command == "deprecate":
        a = targets[0]
        if a.meta.get("status") != "accepted":
            print(f"ERROR {a.path.name} is '{a.meta.get('status')}'; only an accepted ADR can be deprecated")
            return 1
        set_field(a.path, "status", "deprecated")
        print(f"deprecated {a.path.name}")
    else:
        old, new = targets
        if old is new:
            print("ERROR an ADR can't supersede itself")
            return 1
        if old.meta.get("status") != "accepted":
            print(f"ERROR {old.path.name} is '{old.meta.get('status')}'; only an accepted ADR can be superseded")
            return 1
        if new.meta.get("status") not in {"proposed", "accepted"}:
            print(f"ERROR {new.path.name} is '{new.meta.get('status')}'; the successor must be proposed or accepted")
            return 1
        if blocked(new):
            return 1
        set_field(new.path, "supersedes", str(old.number))
        set_field(new.path, "status", "accepted")
        set_field(old.path, "superseded_by", str(new.number))
        set_field(old.path, "status", "superseded")
        print(f"{new.path.name} accepted, supersedes {old.path.name}; {old.path.name} superseded")
    return 0


if __name__ == "__main__":
    sys.exit(main())
