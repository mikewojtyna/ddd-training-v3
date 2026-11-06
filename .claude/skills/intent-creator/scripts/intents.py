#!/usr/bin/env python3
"""Build the index of, and validate, the intents in docs/intents.

Usage (from the repository root):
    python3 .claude/skills/intent-creator/scripts/intents.py build  [--dir docs/intents]
    python3 .claude/skills/intent-creator/scripts/intents.py check  [--dir docs/intents]

build  rewrites <dir>/README.md: a global timeline (by sequence) and, per system,
       the value stream (story-map activities in journey order, intents and their
       business outcomes under each).
check  validates every intent; prints ERROR/WARN lines; exit code 1 if any ERROR.

Standard library only.
"""
from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

REQUIRED_FIELDS = ["name", "sequence", "title", "system", "actor", "activity",
                   "task", "status", "date", "source"]
STATUSES = {"draft", "accepted", "specified", "planned", "delivered", "superseded", "dropped"}
SECTIONS = ["Intent", "Why", "Business outcome", "Success signal",
            "Business rules mentioned", "Scope", "Related intents",
            "Open questions", "Notes for the spec"]
SPEC_NOTES = "Notes for the spec"
SPECIAL_FILES = {"README.md", "story-map.md"}

# Solution-space vocabulary that should not appear outside "Notes for the spec".
TECH_CASE_SENSITIVE = re.compile(r"\b(POST|GET|PUT|PATCH|DELETE|REST|SQL|JSON|UI|DB)\b")
TECH_WORDS = re.compile(
    r"\b(endpoints?|api|apis|http|urls?|database|db table|tables? in|postgres(?:ql)?|mysql|"
    r"mongo(?:db)?|redis|kafka|rabbitmq|message queue|queue topic|topic|cache|caching|"
    r"microservices?|backend|frontend|front-end|back-end|spring|java|class(?:es)?|"
    r"aggregates?|bounded contexts?|modules?|repositor(?:y|ies)|schema|"
    r"the system (?:stores|saves|sends|calls|persists)|persist(?:s|ed|ence)?|"
    r"use ?cases?|webhooks?|cron)\b",
    re.IGNORECASE)


@dataclass
class Intent:
    path: Path
    meta: dict
    body: str
    sections: dict = field(default_factory=dict)

    @property
    def name(self) -> str:
        return self.meta.get("name", self.path.stem)

    @property
    def seq(self) -> int | None:
        try:
            return int(self.meta.get("sequence", ""))
        except ValueError:
            return None


def strip_comment(value: str) -> str:
    return re.sub(r"\s+#.*$", "", value).strip()


def parse_intent(path: Path) -> Intent:
    text = path.read_text(encoding="utf-8")
    meta, body = {}, text
    m = re.match(r"^---\s*\n(.*?)\n---\s*\n?(.*)$", text, re.DOTALL)
    if m:
        for line in m.group(1).splitlines():
            if ":" in line and not line.lstrip().startswith("#"):
                k, v = line.split(":", 1)
                meta[k.strip()] = strip_comment(v)
        body = m.group(2)
    sections, current = {}, None
    for line in body.splitlines():
        h = re.match(r"^##\s+(.+?)\s*$", line)
        if h:
            current = h.group(1)
            sections[current] = []
        elif current is not None:
            sections[current].append(line)
    return Intent(path, meta, body, {k: "\n".join(v) for k, v in sections.items()})


def parse_story_map(path: Path) -> dict:
    """Return {system: [(activity, [tasks])]} preserving order."""
    result: dict = {}
    if not path.exists():
        return result
    system = activity = None
    for line in path.read_text(encoding="utf-8").splitlines():
        s = re.match(r"^##\s+(?!#)(.+?)\s*$", line)
        a = re.match(r"^###\s+(?:\d+\.\s*)?(.+?)\s*$", line)
        t = re.match(r"^\s*[-*]\s+(.+?)\s*$", line)
        if s:
            system, activity = s.group(1).strip().lower(), None
            result.setdefault(system, [])
        elif a and system is not None:
            activity = (a.group(1).strip(), [])
            result[system].append(activity)
        elif t and activity is not None:
            activity[1].append(t.group(1).strip())
    return result


def load(directory: Path) -> list[Intent]:
    return [parse_intent(p) for p in sorted(directory.glob("*.md"))
            if p.name not in SPECIAL_FILES]


def without_comments(text: str) -> str:
    return re.sub(r"<!--.*?-->", "", text, flags=re.DOTALL)


def outcomes(intent: Intent) -> list[str]:
    out = []
    for line in without_comments(intent.sections.get("Business outcome", "")).splitlines():
        m = re.match(r"^\s*[-*]\s+\**([^:*]+?)\**\s*(?::|—|-|$)", line)
        if m:
            out.append(m.group(1).strip())
    return out


def check(directory: Path) -> int:
    intents = load(directory)
    story = parse_story_map(directory / "story-map.md")
    names = {i.name for i in intents}
    by_name = {i.name: i for i in intents}
    errors = warnings = 0

    def err(i, msg):
        nonlocal errors
        errors += 1
        print(f"ERROR {i.path.name}: {msg}")

    def warn(i, msg):
        nonlocal warnings
        warnings += 1
        print(f"WARN  {i.path.name}: {msg}")

    if not (directory / "story-map.md").exists():
        errors += 1
        print(f"ERROR {directory}/story-map.md is missing")

    seqs: dict = {}
    for i in intents:
        for f in REQUIRED_FIELDS:
            if not i.meta.get(f):
                err(i, f"frontmatter field '{f}' is missing or empty")
        if i.meta.get("name") and i.meta["name"] != i.path.stem:
            err(i, f"name '{i.meta['name']}' does not match file name '{i.path.stem}'")
        if not re.fullmatch(r"[a-z0-9]+(?:-[a-z0-9]+)*", i.path.stem):
            err(i, "file name must be kebab-case")
        if i.seq is None:
            err(i, "sequence must be an integer")
        else:
            seqs.setdefault(i.seq, []).append(i.name)
        status = i.meta.get("status", "")
        if status and status not in STATUSES:
            err(i, f"status '{status}' not in {sorted(STATUSES)}")
        if i.meta.get("date") and not re.fullmatch(r"\d{4}-\d{2}-\d{2}", i.meta["date"]):
            err(i, "date must be YYYY-MM-DD")

        system = i.meta.get("system", "").lower()
        if system and story:
            if system not in story:
                err(i, f"system '{system}' has no '## {system}' section in story-map.md")
            else:
                acts = {a: t for a, t in story[system]}
                act = i.meta.get("activity", "")
                if act and act not in acts:
                    err(i, f"activity '{act}' is not in story-map.md under '{system}' "
                           f"(known: {', '.join(acts) or 'none'})")
                elif act and i.meta.get("task") and i.meta["task"] not in acts[act]:
                    warn(i, f"task '{i.meta['task']}' is not listed under '{act}' in story-map.md")

        sup = i.meta.get("supersedes", "")
        if sup:
            old = by_name.get(sup)
            if not old:
                err(i, f"supersedes unknown intent '{sup}'")
            else:
                if old.meta.get("status") != "superseded":
                    err(old, f"is superseded by '{i.name}' but status is '{old.meta.get('status')}'")
                if old.meta.get("superseded_by") != i.name:
                    err(old, f"superseded_by should be '{i.name}'")
        sb = i.meta.get("superseded_by", "")
        if sb and sb not in names:
            err(i, f"superseded_by unknown intent '{sb}'")
        if status == "superseded" and not sb:
            err(i, "status is superseded but superseded_by is empty")

        for s in SECTIONS:
            if s not in i.sections:
                warn(i, f"section '## {s}' is missing")
        stmt = i.sections.get("Intent", "")
        if stmt and not (re.search(r"\bAs an?\b", stmt) and re.search(r"\bso that\b", stmt)):
            warn(i, "intent statement should read 'As a <actor>, I want to <goal> so that <value>'")
        if not outcomes(i):
            warn(i, "no business outcome (past-tense business fact) listed")
        if i.meta.get("actor", "").lower() in {"system", "the system", "backend", "user"}:
            err(i, f"actor '{i.meta['actor']}' is not a business role")

        related = without_comments(i.sections.get("Related intents", ""))
        for ref in re.findall(r"`([a-z0-9-]+)`", related):
            if ref not in names:
                line = next((l for l in related.splitlines() if f"`{ref}`" in l), "")
                if "candidate" not in line.lower():
                    warn(i, f"related intent '{ref}' does not exist (mark it 'candidate' if planned)")

        for field_name in ("title", "name"):
            v = i.meta.get(field_name, "")
            hits = set(TECH_WORDS.findall(v.replace("-", " "))) | set(TECH_CASE_SENSITIVE.findall(v))
            if hits:
                err(i, f"{field_name} uses solution vocabulary: {', '.join(sorted(hits))}")
        for sec, text in i.sections.items():
            if sec == SPEC_NOTES:
                continue
            text = re.sub(r"`[a-z0-9-]+`", "", without_comments(text))
            hits = set(TECH_WORDS.findall(text)) | set(TECH_CASE_SENSITIVE.findall(text))
            if hits:
                warn(i, f"section '{sec}' uses solution vocabulary ({', '.join(sorted(hits))}); "
                        f"rephrase in business terms or move to '## {SPEC_NOTES}'")

    for seq, ns in sorted(seqs.items()):
        if len(ns) > 1:
            errors += 1
            print(f"ERROR sequence {seq} is used by more than one intent: {', '.join(ns)}")
    if seqs:
        missing = sorted(set(range(1, max(seqs) + 1)) - set(seqs))
        if missing:
            warnings += 1
            print(f"WARN  sequence numbers missing: {missing}")

    print(f"{len(intents)} intents, {errors} error(s), {warnings} warning(s)")
    return 1 if errors else 0


def build(directory: Path) -> int:
    intents = sorted(load(directory), key=lambda i: (i.seq is None, i.seq or 0, i.name))
    story = parse_story_map(directory / "story-map.md")
    lines = [
        "# Intents",
        "",
        "<!-- Generated by .claude/skills/intent-creator/scripts/intents.py build. Do not edit by hand. -->",
        "",
        "Each intent is one fine-grained business need. Read the **timeline** to see the product's "
        "history, and the **value stream** to see the journey each system supports. The backbone "
        "lives in [story-map.md](story-map.md).",
        "",
        "## Timeline",
        "",
        "| # | Date | Intent | System | Actor | Activity | Status | Outcome |",
        "|---|------|--------|--------|-------|----------|--------|---------|",
    ]
    for i in intents:
        m = i.meta
        title = m.get("title", i.name)
        if m.get("status") in {"superseded", "dropped"}:
            title = f"~~{title}~~"
        lines.append(f"| {m.get('sequence', '?')} | {m.get('date', '')} | [{title}]({i.path.name}) | "
                     f"{m.get('system', '')} | {m.get('actor', '')} | {m.get('activity', '')} | "
                     f"{m.get('status', '')} | {', '.join(outcomes(i))} |")

    systems = list(story) + sorted({i.meta.get("system", "").lower() for i in intents} - set(story))
    for system in systems:
        lines += ["", f"## Value stream: {system}", ""]
        sys_intents = [i for i in intents if i.meta.get("system", "").lower() == system]
        known = set()
        for n, (act, tasks) in enumerate(story.get(system, []), 1):
            known.add(act)
            lines.append(f"### {n}. {act}")
            lines.append("")
            act_intents = [i for i in sys_intents if i.meta.get("activity") == act]
            ordered_tasks = tasks + sorted({i.meta.get("task", "") for i in act_intents} - set(tasks))
            for task in ordered_tasks:
                ti = [i for i in act_intents if i.meta.get("task") == task]
                if not ti:
                    lines.append(f"- {task}: *(no intent yet)*")
                    continue
                lines.append(f"- {task}")
                for i in ti:
                    flag = "" if i.meta.get("status") not in {"superseded", "dropped"} else " ~~(" + i.meta["status"] + ")~~"
                    out = ", ".join(outcomes(i))
                    lines.append(f"  - #{i.meta.get('sequence')} [{i.meta.get('title', i.name)}]({i.path.name})"
                                 f" ({i.meta.get('actor', '')}, {i.meta.get('status', '')}){flag}"
                                 + (f" → {out}" if out else ""))
            lines.append("")
        unmapped = [i for i in sys_intents if i.meta.get("activity") not in known]
        if unmapped:
            lines += ["### Not on the story map", ""]
            lines += [f"- #{i.meta.get('sequence')} [{i.meta.get('title', i.name)}]({i.path.name}) "
                      f"(activity: {i.meta.get('activity', '?')})" for i in unmapped]
            lines.append("")
    (directory / "README.md").write_text("\n".join(lines).rstrip() + "\n", encoding="utf-8")
    print(f"wrote {directory / 'README.md'} ({len(intents)} intents)")
    return 0


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("command", choices=["build", "check"])
    ap.add_argument("--dir", default="docs/intents", type=Path)
    args = ap.parse_args()
    if not args.dir.is_dir():
        print(f"ERROR {args.dir} does not exist")
        return 1
    return build(args.dir) if args.command == "build" else check(args.dir)


if __name__ == "__main__":
    sys.exit(main())
