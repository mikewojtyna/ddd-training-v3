#!/usr/bin/env python3
"""Check the documents produced by this skill (and change their status).

Usage (from the repository root):
    python3 .claude/skills/my-skill/scripts/check.py check   [<file>] [--dir docs/my-thing]
    python3 .claude/skills/my-skill/scripts/check.py approve <file>   [--dir docs/my-thing]

check    validates one file, or every file in --dir; prints ERROR/WARN lines; exit code 1 if any ERROR.
approve  sets status: approved, but only if check has no errors.

Standard library only.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

REQUIRED_FIELDS = ["title", "status", "date"]          # TODO: your frontmatter fields
STATUSES = {"draft", "approved"}                        # TODO: your statuses
SECTIONS = ["Section 1", "Section 2", "Open questions"]  # TODO: your template's '## ' headings


class Report:
    def __init__(self):
        self.errors = self.warnings = 0

    def err(self, where, msg):
        self.errors += 1
        print(f"ERROR {where}: {msg}")

    def warn(self, where, msg):
        self.warnings += 1
        print(f"WARN  {where}: {msg}")


def parse(path: Path) -> tuple[dict, dict]:
    """Return (frontmatter, {section heading: text}). HTML comments are ignored."""
    text = path.read_text(encoding="utf-8")
    meta, body = {}, text
    m = re.match(r"^---\s*\n(.*?)\n---\s*\n?(.*)$", text, re.DOTALL)
    if m:
        for line in m.group(1).splitlines():
            if ":" in line and not line.lstrip().startswith("#"):
                k, v = line.split(":", 1)
                meta[k.strip()] = re.sub(r"\s+#.*$", "", v).strip()
        body = m.group(2)
    sections, current = {}, None
    for line in re.sub(r"<!--.*?-->", "", body, flags=re.DOTALL).splitlines():
        h = re.match(r"^##\s+(?!#)(.+?)\s*$", line)
        if h:
            current = h.group(1)
            sections[current] = []
        elif current is not None:
            sections[current].append(line)
    return meta, {k: "\n".join(v).strip() for k, v in sections.items()}


def check_file(path: Path, rep: Report) -> None:
    meta, sections = parse(path)
    w = path.name
    for f in REQUIRED_FIELDS:
        if not meta.get(f):
            rep.err(w, f"frontmatter field '{f}' is missing or empty")
    if meta.get("status") and meta["status"] not in STATUSES:
        rep.err(w, f"status '{meta['status']}' is not one of {sorted(STATUSES)}")
    for s in SECTIONS:
        if s not in sections:
            rep.err(w, f"section '## {s}' is missing")
        elif not sections[s]:
            rep.warn(w, f"section '## {s}' is empty")
    # TODO: one or two checks that encode what reviewers keep finding, e.g. "at least two options", "no TODO left".


def set_status(path: Path, status: str) -> None:
    text = path.read_text(encoding="utf-8")
    new, n = re.subn(r"(?m)^(status:\s*)\S+", lambda m: m.group(1) + status, text, count=1)
    if n == 0:
        raise SystemExit(f"ERROR {path}: no 'status:' line in frontmatter")
    path.write_text(new, encoding="utf-8")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("command", choices=["check", "approve"])
    ap.add_argument("file", nargs="?", type=Path)
    ap.add_argument("--dir", default="docs/my-thing", type=Path)  # TODO: your output folder
    args = ap.parse_args()
    files = [args.file] if args.file else sorted(args.dir.glob("*.md"))
    if args.command == "approve" and not args.file:
        print("ERROR approve needs a file")
        return 1
    rep = Report()
    for f in files:
        if not f.exists():
            rep.err(str(f), "does not exist")
            continue
        check_file(f, rep)
    print(f"{len(files)} file(s), {rep.errors} error(s), {rep.warnings} warning(s)")
    if args.command == "approve":
        if rep.errors:
            print("Not approved: fix the errors first.")
            return 1
        set_status(args.file, "approved")
        print(f"approved {args.file}")
    return 1 if rep.errors else 0


if __name__ == "__main__":
    sys.exit(main())
