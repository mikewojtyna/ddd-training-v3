#!/usr/bin/env python3
"""Mechanical grader for discover-bounded-contexts evals.

Usage: grade.py <run_dir> <eval_name> <repo_root>
Checks files under <run_dir>/outputs/files (the run's mirror of the repo) and writes
<run_dir>/grading_mechanical.json. Judgment assertions (heuristic order, boundaries found,
critic use) are graded separately by a grader agent and merged into grading.json.
"""
import json
import re
import subprocess
import sys
from pathlib import Path

run, name, repo = Path(sys.argv[1]), sys.argv[2], Path(sys.argv[3])
out = run / "outputs"
files = out / "files"
validator = repo / ".claude/skills/discover-bounded-contexts/scripts/validate_cml.sh"
results = []


def check(text, passed, evidence):
    results.append({"text": text, "passed": bool(passed), "evidence": str(evidence)[:500]})


def found(pattern):
    return sorted(p for p in out.rglob(pattern) if "scratch" not in p.relative_to(out).parts)


def rel(paths):
    return [str(p.relative_to(out)) for p in paths]


cmls = found("*.cml")
reports = sorted(p for p in found("*.md") if "context-discovery" in str(p) or p.name.startswith("discovery"))
canvases = found("canvas.md")
plans = found("plan.md")
javas = found("*.java")


def validate(cml):
    r = subprocess.run([str(validator), str(cml)], capture_output=True, text=True, cwd=repo)
    return r.returncode, r.stdout


def tracked_unchanged(path):
    r = subprocess.run(["git", "-C", str(repo), "diff", "--quiet", "--", path])
    return r.returncode == 0


if name == "velocity-board-screenshot":
    check("Discovery report written under docs/context-discovery/velocity/",
          any("docs/context-discovery/velocity" in str(p) for p in reports), rel(reports) or "no report")
    check("No CML and no canvases created (the expert declined both)", not cmls and not canvases,
          f"cml={rel(cmls)} canvases={rel(canvases)}")

elif name == "tutorhub-narrative-to-cml":
    cml = next((p for p in cmls if "src/main/cml" in str(p)), cmls[0] if cmls else None)
    check("CML context map written under src/main/cml/", cml is not None and "src/main/cml" in str(cml),
          rel(cmls) or "no .cml")
    code, stdout = validate(cml) if cml else (None, "")
    check("CML passes validate_cml.sh (exit 0)", code == 0, f"exit={code} {stdout[-300:]}")
    text = cml.read_text() if cml else ""
    check("No Shared Kernel in the CML", cml is not None and "[SK]" not in text and "Shared-Kernel" not in text,
          "SK found" if "[SK]" in text else "none")
    check("No direct upstream/downstream cycle (validator reports no ud-cycle)",
          code == 0 and "RULE ud-cycle:" not in stdout,
          [l for l in stdout.splitlines() if "ud-cycle" in l] or "none")
    video = [l for l in stdout.splitlines() if re.search(r"video", l, re.I) and "->" in l]
    check("The context downstream of the video provider uses an ACL",
          any("[ACL" in l.split("->")[1] or "ACL]" in l.split("->")[1] for l in video), video or "no video relationship")
    check("Discovery report written under docs/context-discovery/tutorhub/",
          any("context-discovery/tutorhub" in str(p).lower() for p in reports), rel(reports) or "no report")
    check("At least 5 canvases created under docs/bounded-contexts/", len(canvases) >= 5, rel(canvases))
    check("Canvas hand-off only: no plan.md and no Java code generated", not plans and not javas,
          f"plans={rel(plans)} java={len(javas)}")

elif name == "validate-velocity-reference-cml":
    check("Original src/main/cml/VeloCity-Context-Map.cml left unmodified",
          tracked_unchanged("src/main/cml/VeloCity-Context-Map.cml") and
          not any(str(p).endswith("files/src/main/cml/VeloCity-Context-Map.cml") for p in cmls),
          "real file: " + ("git diff clean" if tracked_unchanged("src/main/cml/VeloCity-Context-Map.cml") else "modified")
          + "; run output " + ("overwrites the original path" if any(str(p).endswith("files/src/main/cml/VeloCity-Context-Map.cml") for p in cmls) else "uses a separate file"))
    original = (repo / "src/main/cml/VeloCity-Context-Map.cml").read_text()
    proposed = [p for p in cmls if p.read_text() != original]
    ok = [p for p in proposed if validate(p)[0] == 0]
    check("Proposed corrected map delivered as a separate CML file that passes validate_cml.sh", bool(ok),
          f"proposed={rel(proposed)} valid={rel(ok)}")

if name in ("velocity-board-screenshot", "tutorhub-narrative-to-cml", "validate-velocity-reference-cml"):
    final = out / "final_message.md"
    words = len(final.read_text().split()) if final.exists() else None
    check("Final message is under 400 words", words is not None and words < 400, f"{words} words")

if name == "tutorhub-narrative-to-cml" and code == 0:
    external = re.compile(r"stripe|video|payment(provider|processor)", re.I)
    internal_cf = [l.strip() for l in stdout.splitlines()
                   if "->" in l and "CF" in l.split("->")[1] and not external.search(l.split("->")[0])]
    check("No Conformist between internal contexts (CF only towards external Stripe/video provider)",
          not internal_cf, internal_cf or "only external upstreams are conformed to")

(run / "grading_mechanical.json").write_text(json.dumps({"expectations": results}, indent=2))
print(json.dumps(results, indent=2))
