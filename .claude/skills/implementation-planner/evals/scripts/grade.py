#!/usr/bin/env python3
"""Grade implementation-planner eval runs. Usage: grade.py <iteration-dir>

Expects <iteration-dir>/eval-<id>-<name>/<config>/run-<n>/outputs/ with:
  files/docs/{plans,specs,intents,bounded-contexts}/   (copied from the run's repo)
  changes.diff                                         (git status --short, blank line, git diff against the seed)
  final.md                                             (the run's final message)
Writes grading.json (expectations: text/passed/evidence) into every run dir.
"""
import json
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
PLANS_PY = HERE.parent.parent / "scripts" / "plans.py"
INTENT = {1: "rider-reserves-bike-at-station", 2: "crowdsorcery-investor-withdraws-money",
          3: "rider-reserves-bike-at-station", 4: "city-operations-reviews-monthly-zone-usage"}


def E(text, passed, evidence=""):
    return {"text": text, "passed": bool(passed), "evidence": str(evidence)[:600]}


def split(text):
    meta, body = {}, text
    m = re.match(r"^---\s*\n(.*?)\n---\s*\n?(.*)$", text, re.S)
    if m:
        for line in m.group(1).splitlines():
            if ":" in line:
                k, v = line.split(":", 1)
                meta[k.strip()] = re.sub(r"\s+#.*$", "", v).strip()
        body = m.group(2)
    return meta, body


def changed(diff):
    status = diff.split("\n\n", 1)[0]
    return [l[3:].strip() for l in status.splitlines() if len(l) > 3]


def load(out, eid):
    docs = out / "files" / "docs"
    diff = (out / "changes.diff").read_text(errors="replace") if (out / "changes.diff").exists() else ""
    files = changed(diff)
    final = (out / "final.md").read_text(errors="replace") if (out / "final.md").exists() else ""
    plan = docs / "plans" / f"{INTENT[eid]}.md"
    if not plan.exists():  # baseline may put it elsewhere
        cands = [f for f in files if f.endswith(".md") and re.search(r"plan", f, re.I)
                 and not f.startswith(".claude/")]
        alt = [out / "files" / f for f in cands if (out / "files" / f).exists()]
        plan = alt[0] if alt else None
    text = plan.read_text(errors="replace") if plan else ""
    spec = docs / "specs" / f"{INTENT[eid]}.md"
    stext = spec.read_text(errors="replace") if spec.exists() else ""
    imeta = split((docs / "intents" / f"{INTENT[eid]}.md").read_text())[0] if (docs / "intents" / f"{INTENT[eid]}.md").exists() else {}
    return docs, plan, text, stext, imeta, files, final


def spec_ids(stext):
    rules = sorted(set(re.findall(r"^###\s+(R\d+)\b", stext, re.M)))
    examples = sorted(set(re.findall(r"\*\*(E\d+\.\d+)\*\*", stext)))
    def names(sub, key):
        m = re.search(rf"^###\s+{sub}\s*$(.*?)(?=^##)", stext, re.M | re.S)
        rows = [l for l in (m.group(1) if m else "").splitlines() if l.strip().startswith("|")][2:]
        return [r.strip("|").split("|")[0].strip() for r in rows if r.strip("|").split("|")[0].strip().lower().rstrip(".") not in {"none", ""}]
    return rules, examples, names("Commands", "command") + names("Events", "event")


def code_written(files):
    return sorted(f for f in files if f.startswith("src/") or f.endswith(".cml") or f.startswith("docs/bounded-contexts/")
                  or f.startswith("docs/context-discovery/"))


def common(eid, docs, plan, text, stext, imeta, files, final):
    ex = [E(f"Plan file at docs/plans/{INTENT[eid]}.md (1:1 with intent and spec)",
            plan is not None and plan.name == f"{INTENT[eid]}.md" and plan.parent.name == "plans", plan or files)]
    rules, examples, contract = spec_ids(stext)
    miss_r = [r for r in rules if not re.search(rf"\b{r}\b", text)]
    ex.append(E("Every spec rule is placed in the plan", text and not miss_r, f"missing {miss_r} of {rules}"))
    norm = lambda s: re.sub(r"[^a-z0-9]", "", s.lower())
    miss_c = [c for c in contract if norm(c) not in norm(text)]
    ex.append(E("Every spec command and event appears in the plan", text and not miss_c, f"missing {miss_c}"))
    miss_e = [e for e in examples if not re.search(rf"\b{re.escape(e)}\b", text)]
    ex.append(E("Every spec example is traced to a step/acceptance test", text and not miss_e, f"missing {miss_e}"))
    ex.append(E("Domain rules are delegated to ddd-tactical-building-blocks (TDD)",
                re.search(r"ddd-tactical-building-blocks", text), ""))
    cw = code_written(files)
    ex.append(E("Stopped for review: no code, canvas, CML or discovery files written", not cw, cw or "none"))
    ex.append(E("Intent not moved past 'specified' before approval", imeta.get("status") == "specified", imeta.get("status")))
    if plan is not None and plan.parent.name == "plans":
        r = subprocess.run([sys.executable, str(PLANS_PY), "check", plan.stem, "--dir", str(plan.parent),
                            "--specs", str(docs / "specs"), "--intents", str(docs / "intents")], capture_output=True, text=True)
        ex.append(E("Plan passes plans.py check (0 errors)", r.returncode == 0, r.stdout.strip()[-500:]))
    else:
        ex.append(E("Plan passes plans.py check (0 errors)", False, "no plan in docs/plans"))
    return ex


def eval1(text, files, final):
    steps = re.findall(r"^###\s+S\d+.*?(?=^###\s|^##\s|\Z)", text, re.M | re.S)
    tactical = [s for s in steps if "tactical" in s.lower()]
    big = [s.splitlines()[0] for s in tactical if len(set(re.findall(r"\bR\d+\b", s))) > 3]
    placement = (re.search(r"^## Placement(.*?)^## ", text, re.M | re.S) or [None, text])[1]
    return [
        E("All placement rows name Rentals as owner", re.search(r"Rentals", placement) and not re.search(
            r"\b(Pricing|Settlement|Accounts|FleetMaintenance|Payments)\b\s*\|", placement), placement[:300]),
        E("Notices Rentals has no canvas and plans to backfill one first", re.search(r"backfill|no canvas|create.*canvas", text, re.I), ""),
        E("Translates spec 'Rider' to Rentals' 'Borrower'", re.search(r"Borrower", text), ""),
        E("Tactical work split into runs of at most 3 rules", tactical and not big, big or f"{len(tactical)} tactical steps"),
        E("Final message asks to confirm placement and the TDD checkpoint loop",
          re.search(r"placement|Rentals", final, re.I) and re.search(r"TDD|checkpoint", final, re.I), final[:300]),
    ]


def eval2(text, files, final):
    return [
        E("Places withdrawal in the existing deposit context", re.search(r"\bdeposit\b", text, re.I), ""),
        E("Builds on existing withdraw code (WithdrawUseCase / DepositAccount) instead of duplicating it",
          re.search(r"WithdrawUseCase|DepositAccount", text), ""),
        E("Places the Finance approval rule with a reason", re.search(r"Finance", text) and re.search(r"R4", text), ""),
        E("Explains how the ticket's Kafka/Postgres hints are treated", re.search(r"kafka|postgres", text, re.I), ""),
        E("Accepted open questions carried into risks/open questions",
          re.search(r"Q1|open question", text, re.I), ""),
    ]


def eval3(plan, files, final):
    created = [f for f in files if not f.startswith(".claude/")]
    return [
        E("No plan created for a draft spec", plan is None, plan),
        E("No repository files changed", not created, created or "none"),
        E("Final message says the spec is not approved yet and points back to the spec/its questions",
          re.search(r"draft|not (yet )?approved|approv", final, re.I) and re.search(r"spec", final, re.I), final[:300]),
    ]


def eval4(text, files, final):
    both = text + "\n" + final
    return [
        E("No code written for the new context", not [f for f in files if f.startswith("src/")], [f for f in files if f.startswith("src/")]),
        E("Reference CML untouched", not [f for f in files if f.endswith(".cml")], [f for f in files if f.endswith(".cml")]),
        E("Proposes a candidate new bounded context", re.search(r"new (bounded )?context|candidate", both, re.I), ""),
        E("Hands the new-context decision to discover-bounded-contexts (named explicitly)", re.search(r"discover-bounded-contexts", both), ""),
        E("No canvas or implementation plan written for the unapproved new context",
          not [f for f in files if f.startswith("docs/bounded-contexts/") or f.startswith("docs/plans/")],
          [f for f in files if f.startswith("docs/bounded-contexts/") or f.startswith("docs/plans/")] or "none"),
        E("Explains why existing contexts don't own it (Rentals and Settlement considered)",
          re.search(r"Rentals", both) and re.search(r"Settlement", both), ""),
    ]


def main():
    it = Path(sys.argv[1])
    for ed in sorted(it.glob("eval-*")):
        eid = int(ed.name.split("-")[1])
        for run in sorted(ed.glob("*/run-*")):
            out = run / "outputs"
            docs, plan, text, stext, imeta, files, final = load(out, eid)
            if eid == 3:
                ex = eval3(plan, files, final)
            elif eid == 4:
                ex = eval4(text, files, final)
                ex.append(E("Intent not moved past 'specified' before approval", imeta.get("status") == "specified", imeta.get("status")))
            else:
                ex = common(eid, docs, plan, text, stext, imeta, files, final)
                ex += eval1(text, files, final) if eid == 1 else eval2(text, files, final)
            passed = sum(e["passed"] for e in ex)
            (run / "grading.json").write_text(json.dumps({"expectations": ex, "summary": {
                "passed": passed, "failed": len(ex) - passed, "total": len(ex),
                "pass_rate": round(passed / len(ex), 3) if ex else 0.0}}, indent=2))
            print(f"{ed.name}/{run.parent.name}: {passed}/{len(ex)}")


if __name__ == "__main__":
    main()
