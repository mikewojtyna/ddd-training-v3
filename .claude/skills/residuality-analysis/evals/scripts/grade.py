#!/usr/bin/env python3
"""Programmatic grader for residuality-analysis evals.

Usage: grade.py <run_dir> <eval_name> <repo_root>
Reads <run_dir>/outputs/** (analysis.json, analysis.md, final_message.md, any other .md)
and writes <run_dir>/grading.json. Runs without analysis.json (baselines) fall back
to heuristics on the markdown where an assertion allows it.
"""
import json
import re
import subprocess
import sys
from pathlib import Path

run, name, repo = Path(sys.argv[1]), sys.argv[2], Path(sys.argv[3])
out = run / "outputs"
script = repo / ".claude/skills/residuality-analysis/scripts/residuality_matrix.py"

jsons = [p for p in out.rglob("analysis.json")]
mds = [p for p in out.rglob("*.md")]
report_md = next((p for p in out.rglob("analysis.md")), None)
data = json.loads(jsons[0].read_text()) if jsons else None
all_md = "\n".join(p.read_text(errors="ignore") for p in mds)
report = report_md.read_text(errors="ignore") if report_md else all_md

results = []


def check(text, passed, evidence):
    results.append({"text": text, "passed": bool(passed), "evidence": str(evidence)[:400]})


stressors = [s for s in (data or {}).get("stressors", []) if not s.get("withdrawn")]
comps = (data or {}).get("naive_architecture", {}).get("components", [])
comp_text = " ".join(f"{c.get('id','')} {c.get('name','')}" for c in comps).lower()


def md_stressor_count():
    return len(set(re.findall(r"\bS(\d{1,3})\b", all_md)))


def files_created():
    check("analysis.json and analysis.md both created", jsons and report_md,
          f"json={[str(p.relative_to(out)) for p in jsons]}, md={report_md and report_md.relative_to(out)}")


def validation_ok():
    if not jsons:
        return check("analysis.json passes residuality_matrix.py validation (exit code 0)", False, "no analysis.json")
    r = subprocess.run([sys.executable, str(script), str(jsons[0])], capture_output=True, text=True)
    first = r.stdout.split("### Incidence")[0].strip()
    check("analysis.json passes residuality_matrix.py validation (exit code 0)", r.returncode == 0,
          f"exit={r.returncode} {first[:300]}")


def stressor_count(lo, hi=None, label=None):
    n = len(stressors) if data else md_stressor_count()
    ok = n >= lo and (hi is None or n <= hi)
    text = label or (f"At least {lo} non-withdrawn training stressors" if hi is None else f"Between {lo} and {hi} training stressors")
    check(text, ok, f"{n} stressors ({'analysis.json' if data else 'S<n> ids in markdown'})")


def categories():
    cats = {str(s.get("category", "")).lower() for s in stressors}
    absurd = any("absurd" in c for c in cats)
    check("Stressors span at least 7 distinct categories, including an absurd one", len(cats) >= 7 and absurd,
          f"categories={sorted(cats) if data else 'no json'}")


def attractors_reused():
    if data:
        na, ns = len(data.get("attractors", [])), len(stressors)
    else:
        na, ns = len(set(re.findall(r"\bA(\d{1,2})\b", all_md))), md_stressor_count()
    check("Fewer attractors than half the number of stressors (attractors are reused)", 0 < na < ns / 2,
          f"attractors={na}, stressors={ns}")


def verdicts_complete():
    bad = [s.get("id") for s in stressors if not isinstance(s.get("naive_survives"), bool)
           or not s.get("naive_reason") or not s.get("residue")]
    check("Every stressor has a naive verdict with a reason and a residue", data and stressors and not bad,
          f"incomplete: {bad[:10]}" if data else "no json")


def matrix():
    has = re.search(r"incidence", report, re.I) and re.search(r"\|\s*\**(Σ|Total|Sum)\**\s*\|", report)
    check("Report contains an incidence matrix with column totals", has, "found incidence + totals row" if has else "missing")


def hidden_coupling():
    m = re.search(r"hidden coupling", report, re.I)
    check("Report names at least one hidden coupling (co-impact pair without a direct relationship)", m,
          report[max(0, m.start() - 80): m.end() + 120] if m else "phrase not found")


def changes_traced():
    ch = ((data or {}).get("residual_architecture") or {}).get("changes", [])
    ok = ch and all(c.get("stressors") and c.get("attractors") for c in ch)
    check("Residual architecture changes reference both stressors and attractors", ok, f"{len(ch)} changes")


def test_set():
    ts = [t for t in (data or {}).get("test_stressors", [])
          if isinstance(t.get("naive_survives"), bool) and isinstance(t.get("residual_survives"), bool)]
    check("Separate test stressor set (>=10) with naive and residual verdicts", len(ts) >= 10, f"{len(ts)} complete test stressors")


def ri():
    m = re.search(r"Ri[^\n]{0,40}=\s*\(?[^\n]{0,30}[−-][^\n]{0,30}\)?\s*/\s*\d+", all_md)
    check("Residual index Ri reported with its arithmetic", m, m.group(0) if m else "no Ri arithmetic found")


def git_untouched(paths, label):
    r = subprocess.run(["git", "-C", str(repo), "status", "--porcelain", "--", *paths], capture_output=True, text=True)
    check(label, not r.stdout.strip(), r.stdout.strip() or "clean")


def rentals_components():
    hay = comp_text or all_md.lower()
    found = [k for k in ("bikestation", "bikestationrepo", "rentbikeusecase") if k in hay.replace(" ", "")]
    check("Components include Rentals-internal elements (e.g. BikeStation aggregate, BikeStationRepo, RentBikeUseCase)",
          len(found) >= 2, f"found {found}")


def user_stressors():
    user = [s for s in stressors if str(s.get("source", "")).lower() == "user"]
    txt = " ".join(s.get("text", "") for s in user).lower()
    geo = re.search(r"geofenc|photo|bay", txt)
    ebike = re.search(r"e-bike|ebike|charg", txt)
    check("Both user stressors (geofenced bay/photo, e-bike charging) present and marked as from the user", geo and ebike,
          f"user stressors: {[s.get('id') for s in user]}" if data else "no json")


def crowdsorcery_components():
    hay = comp_text or all_md.lower()
    found = [k for k in ("deposit", "investment", "payment", "fundraising") if k in hay]
    check("Components include the CrowdSorcery contexts found in code (e.g. deposit, investment, payment, fundraising)",
          len(found) >= 3, f"found {found}")


def ranking():
    m = re.search(r"rank", report, re.I)
    check("Report ranks components by impact (answers which falls over first)", m, "found 'rank'" if m else "missing")


if name == "velocity-reference-map-full":
    files_created(); validation_ok(); stressor_count(25); categories(); attractors_reused(); verdicts_complete()
    matrix(); hidden_coupling(); changes_traced(); test_set(); ri()
    git_untouched(["src/main/cml", "velocity/velocity-context-map.md"], "CML and velocity-context-map.md not modified")
elif name == "rentals-single-context-user-stressors":
    files_created(); rentals_components(); user_stressors(); stressor_count(15, 30); matrix(); ri()
    git_untouched(["src"], "No Java source files modified")
elif name == "crowdsorcery-poke-holes":
    files_created(); crowdsorcery_components(); stressor_count(20); ranking(); matrix(); ri()

passed = sum(r["passed"] for r in results)
(run / "grading.json").write_text(json.dumps({
    "expectations": results,
    "summary": {"passed": passed, "failed": len(results) - passed, "total": len(results),
                "pass_rate": round(passed / len(results), 2) if results else 0},
}, indent=2))
print(f"{run}: {passed}/{len(results)}")
