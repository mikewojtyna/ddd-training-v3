#!/usr/bin/env python3
"""Validate implementation plans in docs/plans and move them through their lifecycle.

Usage (from the repository root):
    python3 .claude/skills/implementation-planner/scripts/plans.py check   [<intent>]
    python3 .claude/skills/implementation-planner/scripts/plans.py approve <intent>          # plan approved, intent planned
    python3 .claude/skills/implementation-planner/scripts/plans.py step    <intent> S<n> <todo|in progress|done|skipped>
    python3 .claude/skills/implementation-planner/scripts/plans.py done    <intent>          # plan done, intent delivered
Options: --dir docs/plans --specs docs/specs --intents docs/intents

check validates: frontmatter, the intent/spec/plan trio (1:1 names, statuses), spec drift, that every spec rule,
command, query and event is placed, every command/query/event is mapped to code, every step covers only existing
spec IDs, every example is covered by a step, and every rule/example appears in the traceability table.

Standard library only. Reuses the spec parser from .claude/skills/spec-writer/scripts/specs.py.
"""
from __future__ import annotations

import argparse
import re
import sys
from pathlib import Path

SPEC_SCRIPTS = Path(__file__).resolve().parents[2] / "spec-writer" / "scripts"
sys.path.insert(0, str(SPEC_SCRIPTS))
try:
    from specs import Report, col, norm, parse, set_status, subsections, table  # noqa: E402
except ImportError:  # pragma: no cover
    sys.exit(f"ERROR spec-writer's specs.py not found at {SPEC_SCRIPTS}; install the spec-writer skill")

REQUIRED_FIELDS = ["name", "intent", "spec", "system", "status", "date", "spec_date"]
STATUSES = {"draft - awaiting review", "approved", "in progress", "done", "superseded"}
STEP_STATUSES = {"todo", "in progress", "done", "skipped"}
SECTIONS = ["Summary", "Placement", "Context map changes", "Translation", "Contract mapping",
            "Canvas changes", "Decisions", "Steps", "Acceptance tests", "Traceability",
            "Risks and open questions", "Review notes", "Changelog"]
INTENT_OK = {
    "draft - awaiting review": {"specified", "planned"},
    "approved": {"planned"},
    "in progress": {"planned"},
    "done": {"delivered"},
    "superseded": {"specified", "planned", "delivered", "superseded", "dropped"},
}


def frontmatter_status(path: Path) -> str:
    m = re.search(r"(?m)^status:\s*(.*?)\s*(?:#.*)?$", path.read_text(encoding="utf-8"))
    return m.group(1).strip() if m else ""


def set_plan_status(path: Path, status: str) -> None:
    text = path.read_text(encoding="utf-8")
    new, n = re.subn(r"(?m)^(status:\s*)[^#\n]*?(\s*(?:#.*)?)$", lambda m: f"{m.group(1)}{status}{m.group(2)}",
                     text, count=1)
    if n == 0:
        raise SystemExit(f"ERROR {path}: no 'status:' line in frontmatter")
    path.write_text(new, encoding="utf-8")


def spec_ids(spec) -> dict:
    rules, examples = set(), set()
    for heading, text in subsections(spec.sections.get("Rules", "")).items():
        m = re.match(r"^R(\d+)\b", heading)
        if m:
            rules.add(f"R{m.group(1)}")
            examples |= {f"E{a}.{b}" for a, b in re.findall(r"\*\*E(\d+)\.(\d+)\*\*", text)}
    contract = subsections(spec.sections.get("Contract", ""))
    names = lambda sub, key: [col(r, key) for r in table(contract.get(sub, "")) if col(r, key)]
    return {"rules": rules, "examples": examples, "commands": names("Commands", "command"),
            "queries": names("Queries", "query"), "events": names("Events", "event")}


def steps(plan) -> list[dict]:
    out = []
    for heading, text in subsections(plan.sections.get("Steps", "")).items():
        m = re.match(r"^S(\d+)\s*[:.\-—]\s*(.+)$", heading)
        field = lambda k: (re.search(rf"\*\*{k}:\*\*\s*(.+)", text) or [None, ""])[1].strip()
        out.append({"heading": heading, "id": f"S{m.group(1)}" if m else None, "status": field("Status").lower(),
                    "skill": field("Skill"), "covers": field("Covers"), "done_when": field("Done when")})
    return out


def mentions(text: str, name: str) -> bool:
    return norm(name) in norm(text)


def check_plan(plan, specs_dir: Path, intents_dir: Path, rep: Report) -> None:
    w = plan.path.name
    for f in REQUIRED_FIELDS:
        if not plan.meta.get(f):
            rep.err(w, f"frontmatter field '{f}' is missing or empty")
    stem = plan.path.stem
    for f in ("name", "intent", "spec"):
        if plan.meta.get(f) and plan.meta[f] != stem:
            rep.err(w, f"{f} '{plan.meta[f]}' must equal the file name '{stem}' (intent, spec and plan are 1:1)")
    status = frontmatter_status(plan.path)
    if status and status not in STATUSES:
        rep.err(w, f"status '{status}' not in {sorted(STATUSES)}")
    for f in ("date", "spec_date"):
        if plan.meta.get(f) and not re.fullmatch(r"\d{4}-\d{2}-\d{2}", plan.meta[f]):
            rep.err(w, f"{f} must be YYYY-MM-DD")

    ipath, spath = intents_dir / f"{stem}.md", specs_dir / f"{stem}.md"
    if not ipath.exists():
        rep.err(w, f"intent {ipath} does not exist")
    else:
        istatus = parse(ipath).meta.get("status", "")
        if status in INTENT_OK and istatus not in INTENT_OK[status]:
            rep.err(w, f"plan is '{status}' but intent is '{istatus}' (expected {sorted(INTENT_OK[status])})")
    if not spath.exists():
        rep.err(w, f"spec {spath} does not exist")
        return
    spec = parse(spath)
    sstatus = spec.meta.get("status", "")
    if status != "superseded" and sstatus != "approved":
        (rep.err if status in {"approved", "in progress"} else rep.warn)(
            w, f"spec is '{sstatus}', not approved: send it back to spec-writer before building")
    if spec.meta.get("date") and plan.meta.get("spec_date") and spec.meta["date"] > plan.meta["spec_date"]:
        (rep.err if status in {"approved", "in progress"} else rep.warn)(
            w, f"spec changed on {spec.meta['date']} after this plan was built from {plan.meta['spec_date']}: "
               f"revise the plan (drift)")

    for s in SECTIONS:
        if s not in plan.sections:
            rep.warn(w, f"section '## {s}' is missing")

    ids = spec_ids(spec)
    placement = plan.sections.get("Placement", "")
    for r in sorted(ids["rules"], key=lambda x: int(x[1:])):
        if not re.search(rf"(?m)^\|\s*\**(?:Rule:?\s*)?{r}\b", placement):
            rep.err(w, f"rule {r} is not placed (no row in '## Placement')")
    for kind in ("commands", "queries", "events"):
        for n in ids[kind]:
            if not mentions(placement, n):
                rep.err(w, f"{kind[:-1] if kind != 'queries' else 'query'} '{n}' is not placed in '## Placement'")
            if not mentions(plan.sections.get("Contract mapping", ""), n):
                rep.err(w, f"'{n}' is not mapped to code in '## Contract mapping'")
    if not re.search(r"\*\*Outcome:\*\*", placement):
        rep.warn(w, "placement outcome missing (e.g. '**Outcome:** one existing context')")

    known = ids["rules"] | ids["examples"]
    covered = set()
    st = steps(plan)
    if not st:
        rep.err(w, "no steps found (expected '### S1: …' under '## Steps')")
    seen = set()
    for s in st:
        where = s["id"] or s["heading"]
        if not s["id"]:
            rep.warn(w, f"step heading '### {s['heading']}' should read '### S<n>: <title>'")
        elif s["id"] in seen:
            rep.err(w, f"step {s['id']} is defined twice")
        seen.add(s["id"])
        if s["status"] not in STEP_STATUSES:
            rep.err(w, f"step {where} status '{s['status']}' not in {sorted(STEP_STATUSES)}")
        if not s["skill"]:
            rep.warn(w, f"step {where} names no skill")
        if not s["done_when"]:
            rep.warn(w, f"step {where} has no 'Done when'")
        refs = set(re.findall(r"\b(R\d+|E\d+\.\d+)\b", s["covers"]))
        for ref in sorted(refs - known):
            rep.err(w, f"step {where} covers {ref}, which is not in the spec")
        covered |= refs
        if re.search(r"\ball (examples|rules)\b", s["covers"], re.I):
            covered |= ids["examples"] | ids["rules"]
    for e in sorted(ids["examples"] - covered, key=lambda x: [int(p) for p in x[1:].split(".")]):
        rep.err(w, f"example {e} is not covered by any step")
    trace = plan.sections.get("Traceability", "")
    for x in sorted(known):
        if not re.search(rf"\b{re.escape(x)}\b", trace):
            rep.warn(w, f"{x} is missing from '## Traceability'")
    if status == "done":
        open_steps = [s["id"] for s in st if s["status"] not in {"done", "skipped"}]
        if open_steps:
            rep.err(w, f"plan is done but steps are open: {', '.join(open_steps)}")


def main() -> int:
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("command", choices=["check", "approve", "step", "done"])
    ap.add_argument("intent", nargs="?")
    ap.add_argument("step_id", nargs="?")
    ap.add_argument("step_status", nargs="?")
    ap.add_argument("--dir", default="docs/plans", type=Path)
    ap.add_argument("--specs", default="docs/specs", type=Path)
    ap.add_argument("--intents", default="docs/intents", type=Path)
    a = ap.parse_args()
    if not a.dir.is_dir():
        print(f"ERROR {a.dir} does not exist")
        return 1
    if a.command != "check" and not a.intent:
        print(f"ERROR {a.command} needs an intent name")
        return 1
    paths = [a.dir / f"{a.intent}.md"] if a.intent else sorted(a.dir.glob("*.md"))
    paths = [p for p in paths if p.name != "README.md"]
    if a.intent and not paths[0].exists():
        print(f"ERROR {paths[0]} does not exist")
        return 1

    if a.command == "step":
        if not a.step_id or a.step_status not in STEP_STATUSES:
            print(f"ERROR usage: step <intent> S<n> <{'|'.join(sorted(STEP_STATUSES))}>")
            return 1
        p = paths[0]
        text = p.read_text(encoding="utf-8")
        m = re.search(rf"(?ms)^###\s+{re.escape(a.step_id)}\s*[:.\-—].*?(?=^###\s|^##\s|\Z)", text)
        if not m:
            print(f"ERROR step {a.step_id} not found in {p}")
            return 1
        block, n = re.subn(r"(\*\*Status:\*\*\s*)[^\n]+", rf"\g<1>{a.step_status}", m.group(0), count=1)
        if n == 0:
            print(f"ERROR step {a.step_id} has no '- **Status:**' line")
            return 1
        p.write_text(text[:m.start()] + block + text[m.end():], encoding="utf-8")
        if a.step_status in {"in progress", "done"} and frontmatter_status(p) == "approved":
            set_plan_status(p, "in progress")
        print(f"{a.step_id} -> {a.step_status}; plan status: {frontmatter_status(p)}")
        return 0

    rep = Report()
    plans = [parse(p) for p in paths]
    for pl in plans:
        check_plan(pl, a.specs, a.intents, rep)
    print(f"{len(plans)} plan(s), {rep.errors} error(s), {rep.warnings} warning(s)")
    if a.command == "check":
        return 1 if rep.errors else 0

    p = paths[0]
    ipath = a.intents / f"{p.stem}.md"
    if rep.errors:
        print(f"Not changed: fix the errors first.")
        return 1
    if a.command == "approve":
        set_plan_status(p, "approved")
        set_status(ipath, "planned")
        print(f"approved {p}; intent {ipath} status: planned")
    else:  # done
        open_steps = [s["id"] for s in steps(plans[0]) if s["status"] not in {"done", "skipped"}]
        if open_steps:
            print(f"Not done: open steps {', '.join(open_steps)}")
            return 1
        set_plan_status(p, "done")
        set_status(ipath, "delivered")
        print(f"done {p}; intent {ipath} status: delivered")
    return 0


if __name__ == "__main__":
    sys.exit(main())
