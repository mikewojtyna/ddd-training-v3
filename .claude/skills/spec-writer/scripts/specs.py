#!/usr/bin/env python3
"""Validate and approve the specs in docs/specs (one spec per intent in docs/intents).

Usage (from the repository root):
    python3 .claude/skills/spec-writer/scripts/specs.py check   [<intent>] [--dir docs/specs] [--intents docs/intents]
    python3 .claude/skills/spec-writer/scripts/specs.py approve <intent>  [--dir docs/specs] [--intents docs/intents]

check    validates one spec, or every spec; prints ERROR/WARN lines; exit code 1 if any ERROR.
approve  sets the spec to 'approved' and its intent to 'specified', but only if check has no errors.

Standard library only.
"""
from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

REQUIRED_FIELDS = ["name", "intent", "system", "status", "date"]
STATUSES = {"draft", "approved", "superseded"}
SECTIONS = ["Vocabulary", "Rules", "Contract", "Data", "Out of scope",
            "Open questions", "Notes for the plan"]
PLAN_NOTES = "Notes for the plan"
# intent statuses that fit each spec status
INTENT_OK = {
    "draft": {"accepted", "specified", "planned"},
    "approved": {"specified", "planned", "delivered"},
    "superseded": {"superseded", "dropped", "specified", "planned", "delivered"},
}

TECH_CASE_SENSITIVE = re.compile(r"\b(POST|GET|PUT|PATCH|DELETE|REST|SQL|JSON|DB|HTTP|URL)\b")
TECH_WORDS = re.compile(
    r"\b(endpoints?|apis?|http|urls?|status codes?|database|tables?|columns?|postgres(?:ql)?|mysql|"
    r"mongo(?:db)?|redis|kafka|rabbitmq|queues?|topics?|cache|caching|microservices?|backend|"
    r"frontend|spring|java|class(?:es)?|aggregates?|entit(?:y|ies)|bounded contexts?|modules?|"
    r"repositor(?:y|ies)|services?|controllers?|dtos?|schemas?|"
    r"the system (?:stores|saves|sends|calls|persists)|persist(?:s|ed|ence)?|webhooks?|cron)\b",
    re.IGNORECASE)


@dataclass
class Doc:
    path: Path
    meta: dict
    body: str
    sections: dict = field(default_factory=dict)


def strip_comment(value: str) -> str:
    return re.sub(r"\s+#.*$", "", value).strip()


def without_comments(text: str) -> str:
    return re.sub(r"<!--.*?-->", "", text, flags=re.DOTALL)


def parse(path: Path) -> Doc:
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
    for line in without_comments(body).splitlines():
        h = re.match(r"^##\s+(?!#)(.+?)\s*$", line)
        if h:
            current = h.group(1)
            sections[current] = []
        elif current is not None:
            sections[current].append(line)
    return Doc(path, meta, body, {k: "\n".join(v) for k, v in sections.items()})


def subsections(text: str) -> dict:
    out, cur = {}, None
    for line in text.splitlines():
        h = re.match(r"^###\s+(.+?)\s*$", line)
        if h:
            cur = h.group(1)
            out[cur] = []
        elif cur is not None:
            out[cur].append(line)
    return {k: "\n".join(v) for k, v in out.items()}


def table(text: str) -> list[dict]:
    rows = [l.strip() for l in text.splitlines() if l.strip().startswith("|")]
    if len(rows) < 2:
        return []
    cells = lambda r: [c.strip() for c in r.strip("|").split("|")]
    header = [h.lower() for h in cells(rows[0])]
    result = []
    for r in rows[1:]:
        if re.fullmatch(r"\|?[\s:|-]+\|?", r):
            continue
        values = cells(r)
        row = dict(zip(header, values + [""] * (len(header) - len(values))))
        first = values[0] if values else ""
        if first.lower().rstrip(".") in {"none", ""}:
            continue
        result.append(row)
    return result


def col(row: dict, *names: str) -> str:
    for n in names:
        for k, v in row.items():
            if k.startswith(n):
                return v
    return ""


def norm(s: str) -> str:
    return re.sub(r"[^a-z0-9]+", " ", s.replace("*", "").replace("`", "").lower()).strip()


def intent_outcomes(intent: Doc) -> list[str]:
    out = []
    for line in without_comments(intent.sections.get("Business outcome", "")).splitlines():
        m = re.match(r"^\s*[-*]\s+\**([^:*]+?)\**\s*(?::|—|-|$)", line)
        if m:
            out.append(m.group(1).strip())
    return out


class Report:
    def __init__(self):
        self.errors = self.warnings = 0

    def err(self, where, msg):
        self.errors += 1
        print(f"ERROR {where}: {msg}")

    def warn(self, where, msg):
        self.warnings += 1
        print(f"WARN  {where}: {msg}")


def check_spec(spec: Doc, intents_dir: Path, rep: Report) -> None:
    w = spec.path.name
    for f in REQUIRED_FIELDS:
        if not spec.meta.get(f):
            rep.err(w, f"frontmatter field '{f}' is missing or empty")
    name, intent_name = spec.meta.get("name", ""), spec.meta.get("intent", "")
    if name and name != spec.path.stem:
        rep.err(w, f"name '{name}' does not match file name '{spec.path.stem}'")
    if intent_name and intent_name != spec.path.stem:
        rep.err(w, f"intent '{intent_name}' must equal the spec's file name (spec and intent are 1:1)")
    status = spec.meta.get("status", "")
    if status and status not in STATUSES:
        rep.err(w, f"status '{status}' not in {sorted(STATUSES)}")
    if spec.meta.get("date") and not re.fullmatch(r"\d{4}-\d{2}-\d{2}", spec.meta["date"]):
        rep.err(w, "date must be YYYY-MM-DD")

    intent = None
    ipath = intents_dir / f"{intent_name or spec.path.stem}.md"
    if not ipath.exists():
        rep.err(w, f"intent file {ipath} does not exist")
    else:
        intent = parse(ipath)
        istatus = intent.meta.get("status", "")
        if status in INTENT_OK and istatus not in INTENT_OK[status]:
            rep.err(w, f"spec is '{status}' but intent is '{istatus}' "
                       f"(expected one of {sorted(INTENT_OK[status])})")
        if intent.meta.get("system") and spec.meta.get("system") and \
                intent.meta["system"].lower() != spec.meta["system"].lower():
            rep.err(w, f"system '{spec.meta['system']}' differs from intent's '{intent.meta['system']}'")

    for s in SECTIONS:
        if s not in spec.sections:
            rep.warn(w, f"section '## {s}' is missing")

    # Rules and examples
    rules = {}
    for heading, text in subsections(spec.sections.get("Rules", "")).items():
        m = re.match(r"^R(\d+)\s*[:.\-—]\s*(.+)$", heading)
        if not m:
            rep.warn(w, f"rule heading '### {heading}' should read '### R<n>: <rule>'")
            continue
        n = int(m.group(1))
        if n in rules:
            rep.err(w, f"rule R{n} is defined twice")
        rules[n] = text
        examples = re.findall(r"\*\*E(\d+)\.(\d+)\*\*(.*)", text)
        if not examples:
            rep.err(w, f"R{n} has no example (expected '- **E{n}.1** Given …, When …, Then …')")
        for rn, k, line in examples:
            if int(rn) != n:
                rep.err(w, f"example E{rn}.{k} sits under R{n}")
            if not (re.search(r"\bGiven\b", line) and re.search(r"\bWhen\b", line) and re.search(r"\bThen\b", line)):
                rep.warn(w, f"example E{rn}.{k} should read 'Given …, When …, Then …' on one line")
        if len(examples) > 3:
            rep.warn(w, f"R{n} has {len(examples)} examples; 1-3 usually suffice (one per distinct outcome)")
    if not rules:
        rep.err(w, "no rules found under '## Rules' (expected '### R1: …')")
    elif sorted(rules) != list(range(1, max(rules) + 1)):
        rep.warn(w, f"rule numbers are not contiguous: {sorted(rules)}")
    if len(rules) > 8:
        rep.warn(w, f"{len(rules)} rules; the intent may be too coarse, consider splitting it")

    # Contract
    contract = subsections(spec.sections.get("Contract", ""))
    for sub in ("Commands", "Queries", "Events"):
        if sub not in contract:
            rep.warn(w, f"'### {sub}' is missing under '## Contract'")
    events = {norm(col(r, "event")) for r in table(contract.get("Events", "")) if col(r, "event")}
    commands = table(contract.get("Commands", ""))
    for row in commands:
        cname = col(row, "command") or "?"
        if not col(row, "issued"):
            rep.warn(w, f"command '{cname}' has no 'Issued by' actor")
        success = col(row, "on success")
        for ev in [e for e in re.split(r",|/|\bor\b", success) if e.strip()]:
            if norm(ev) not in events:
                rep.err(w, f"command '{cname}' emits '{ev.strip()}', which is not listed under Events")
        for ref in re.findall(r"R(\d+)", col(row, "rejected")):
            if int(ref) not in rules:
                rep.err(w, f"command '{cname}' is rejected by unknown rule R{ref}")
    if intent is not None:
        for ev in intent_outcomes(intent):
            if norm(ev) not in events:
                rep.err(w, f"intent outcome event '{ev}' is missing under Events")

    # Open questions
    listed = set(re.findall(r"\bQ(\d+)\b", spec.sections.get("Open questions", "")))
    body_wo = "\n".join(v for k, v in spec.sections.items() if k != "Open questions")
    for q in sorted(set(re.findall(r"\bQ(\d+)\b", body_wo)) - listed, key=int):
        rep.err(w, f"Q{q} is referenced but not listed under '## Open questions'")
    still_open = set(re.findall(r"\bQ(\d+)\b", re.sub(r"~~.*?~~", "", spec.sections.get("Open questions", ""))))
    if status == "approved" and still_open:
        rep.warn(w, f"approved with open questions {', '.join('Q' + q for q in sorted(still_open, key=int))}; "
                    f"fine only if the user accepted them as open")
    if "?" in re.sub(r"\bQ\d+|\?\s*\)", "", spec.sections.get("Rules", "")) and not listed:
        rep.warn(w, "rules contain '?' but no open questions are listed")

    # Solution vocabulary
    hits = set(TECH_WORDS.findall(name.replace("-", " "))) | set(TECH_CASE_SENSITIVE.findall(name))
    if hits:
        rep.err(w, f"name uses solution vocabulary: {', '.join(sorted(hits))}")
    for sec, text in spec.sections.items():
        if sec == PLAN_NOTES:
            continue
        text = re.sub(r"`[a-z0-9-]+`", "", text)
        hits = set(TECH_WORDS.findall(text)) | set(TECH_CASE_SENSITIVE.findall(text))
        if hits:
            rep.warn(w, f"section '{sec}' uses solution vocabulary ({', '.join(sorted(hits))}); "
                        f"use domain language or move it to '## {PLAN_NOTES}'")


def load_specs(directory: Path, only: str | None) -> list[Doc]:
    if only:
        p = directory / f"{only}.md"
        return [parse(p)] if p.exists() else []
    return [parse(p) for p in sorted(directory.glob("*.md")) if p.name != "README.md"]


def set_status(path: Path, status: str) -> None:
    text = path.read_text(encoding="utf-8")
    new, n = re.subn(r"(?m)^(status:\s*)\S+", lambda m: m.group(1) + status, text, count=1)
    if n == 0:
        raise SystemExit(f"ERROR {path}: no 'status:' line in frontmatter")
    path.write_text(new, encoding="utf-8")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("command", choices=["check", "approve"])
    ap.add_argument("intent", nargs="?")
    ap.add_argument("--dir", default="docs/specs", type=Path)
    ap.add_argument("--intents", default="docs/intents", type=Path)
    args = ap.parse_args()
    if not args.dir.is_dir():
        print(f"ERROR {args.dir} does not exist")
        return 1
    if args.command == "approve" and not args.intent:
        print("ERROR approve needs an intent name")
        return 1
    specs = load_specs(args.dir, args.intent)
    if args.intent and not specs:
        print(f"ERROR {args.dir}/{args.intent}.md does not exist")
        return 1

    rep = Report()
    for s in specs:
        check_spec(s, args.intents, rep)
    print(f"{len(specs)} spec(s), {rep.errors} error(s), {rep.warnings} warning(s)")

    if args.command == "approve":
        spec = specs[0]
        if rep.errors:
            print("Not approved: fix the errors first.")
            return 1
        set_status(spec.path, "approved")
        ipath = args.intents / f"{spec.meta.get('intent', spec.path.stem)}.md"
        istatus = parse(ipath).meta.get("status", "")
        if istatus in {"accepted", "specified"}:
            set_status(ipath, "specified")
        print(f"approved {spec.path}; intent {ipath} status: "
              f"{'specified' if istatus in {'accepted', 'specified'} else istatus}")
        return 0
    return 1 if rep.errors else 0


if __name__ == "__main__":
    sys.exit(main())
