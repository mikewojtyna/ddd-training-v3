#!/usr/bin/env python3
"""Grade spec-writer eval runs. Usage: grade.py <iteration-dir>

Expects <iteration-dir>/eval-<id>-<name>/<config>/run-<n>/outputs/ with:
  files/docs/intents/*.md, files/docs/specs/*.md   (copied from the run's repo)
  changes.diff                                     (git status + diff against the seed commit)
  final.md                                         (the run's final message)
Writes grading.json (expectations: text/passed/evidence) into every run dir.
"""
import json
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
SKILL = HERE.parent.parent
SPECS_PY = SKILL / "scripts" / "specs.py"

TECH_I = re.compile(r"\b(endpoints?|apis?|http|urls?|postgres(?:ql)?|database|kafka|tables?|columns?|"
                    r"json|rest|microservices?|aggregates?|bounded contexts?|repositor(?:y|ies)|"
                    r"spring|java|class(?:es)?|controllers?|dtos?)\b", re.I)
TECH_CS = re.compile(r"\b(POST|GET|PUT|DELETE|SQL|HTTP)\b")
NOTES = re.compile(r"note|plan|technical|implementation|hint", re.I)
INTENT = {1: "rider-reserves-bike-at-station", 2: "crowdsorcery-investor-withdraws-money",
          3: "rider-reports-faulty-bike", 4: "rider-reserves-bike-at-station"}


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


def sections(body):
    out, cur = {"_pre": []}, "_pre"
    for line in re.sub(r"<!--.*?-->", "", body, flags=re.S).splitlines():
        h = re.match(r"^##\s+(?!#)(.+?)\s*$", line)
        if h:
            cur = h.group(1)
            out[cur] = []
        else:
            out[cur].append(line)
    return {k: "\n".join(v) for k, v in out.items()}


def business_text(body):
    return "\n".join(v for k, v in sections(body).items() if not NOTES.search(k))


def tech_hits(text):
    text = re.sub(r"`[a-z0-9-]+`", "", text)
    return sorted(set(TECH_I.findall(text)) | set(TECH_CS.findall(text)))


def examples(body):
    """Given/When/Then examples, one per line or spread across consecutive lines/table rows."""
    text = re.sub(r"<!--.*?-->", "", body, flags=re.S)
    # one-line or multi-line (Given / When / Then on consecutive lines) examples; tables handled below
    prose = "\n".join(l for l in text.splitlines() if not l.strip().startswith("|"))
    found = re.findall(r"\bGiven\b[^\n]*(?:\n(?!\s*\n)[^\n]*){0,6}?\bWhen\b[^\n]*(?:\n(?!\s*\n)[^\n]*){0,6}?\bThen\b[^\n]*",
                       prose, re.I)
    # Given/When/Then tables: every data row under a header with those columns is one example
    header = None
    for line in text.splitlines():
        cells = [c.strip() for c in line.strip().strip("|").split("|")] if line.strip().startswith("|") else None
        if cells is None:
            header = None
        elif all(any(re.fullmatch(w, c, re.I) for c in cells) for w in ("given", "when", "then")):
            header = [c.lower() for c in cells]
        elif header and not re.fullmatch(r"[\s:|-]+", line.strip()):
            row = dict(zip(header, cells))
            found.append(f"Given {row.get('given','')} When {row.get('when','')} Then {row.get('then','')}")
    return [re.sub(r"\s+", " ", f) for f in found]


def open_questions(body):
    text = "\n".join(v for k, v in sections(body).items() if re.search(r"question", k, re.I))
    return re.sub(r"~~.*?~~", "", text, flags=re.S)


def amounts(text):
    return [float(a.replace(" ", "").replace("\u00a0", "").replace(",", "")) for a in
            re.findall(r"(?<![\d.])(\d{1,3}(?:[ \u00a0,]\d{3})+(?:\.\d+)?|\d{4,}(?:\.\d+)?)(?![\d])", text)]


def not_open_questions(body):
    return "\n".join(v for k, v in sections(body).items() if not re.search(r"question", k, re.I))


def E(text, passed, evidence):
    return {"text": text, "passed": bool(passed), "evidence": str(evidence)[:600]}


def load(out, eid):
    d = out / "files" / "docs"
    spec_dir, intent_dir = d / "specs", d / "intents"
    specs = sorted(p for p in spec_dir.glob("*.md") if p.name != "README.md") if spec_dir.is_dir() else []
    # baseline runs may put specs elsewhere; look at the diff too
    diff = (out / "changes.diff").read_text(errors="replace") if (out / "changes.diff").exists() else ""
    other = sorted(set(re.findall(r"^(?:\?\?|A|M)\s+(\S*spec\S*\.md)", diff, re.M)))
    intent = intent_dir / f"{INTENT[eid]}.md"
    imeta, ibody = split(intent.read_text()) if intent.exists() else ({}, "")
    final = (out / "final.md").read_text(errors="replace") if (out / "final.md").exists() else ""
    return spec_dir, specs, other, imeta, ibody, diff, final


def spec_of(specs, eid):
    exact = [p for p in specs if p.stem == INTENT[eid]]
    return exact[0] if exact else (specs[0] if specs else None)


def common(out, eid, specs, other, imeta, ibody, diff, spec_dir):
    ex = []
    p = spec_of(specs, eid)
    ex.append(E(f"Spec file at docs/specs/{INTENT[eid]}.md (1:1 with the intent name)",
                p is not None and p.stem == INTENT[eid], [s.name for s in specs] + other or "none"))
    if p is None:
        for t in ["Rules are illustrated by at least 3 Given/When/Then examples",
                  "Examples use concrete values (named actors, identifiers or numbers)",
                  "No solution vocabulary outside a technical-notes section",
                  "Every business-outcome event of the intent appears in the spec",
                  "Contract lists commands and events in domain language",
                  "Spec passes specs.py check (0 errors)"]:
            ex.append(E(t, False, "no spec"))
    else:
        meta, body = split(p.read_text())
        exs = examples(body)
        ex.append(E("Rules are illustrated by at least 3 Given/When/Then examples", len(exs) >= 3, len(exs)))
        concrete = [x for x in exs if re.search(r"\d|\b[A-Z][a-z]+\b(?<!Given)(?<!When)(?<!Then)", x[6:])]
        ex.append(E("Examples use concrete values (named actors, identifiers or numbers)",
                    exs and len(concrete) >= 0.8 * len(exs), f"{len(concrete)}/{len(exs)}"))
        hits = tech_hits(business_text(body))
        ex.append(E("No solution vocabulary outside a technical-notes section", not hits, hits or "clean"))
        outcomes = re.findall(r"^\s*[-*]\s+\**([^:*\n]+?)\**\s*:", sections(ibody).get("Business outcome", ""), re.M)
        missing = [o for o in outcomes if o.strip().lower() not in body.lower()]
        ex.append(E("Every business-outcome event of the intent appears in the spec", outcomes and not missing,
                    f"outcomes={outcomes} missing={missing}"))
        if eid in (1, 2):
            words = len(body.split())
            ex.append(E("Spec stays lean (at most 1500 words)", words <= 1500, f"{words} words"))
        ex.append(E("Contract lists commands and events in domain language",
                    re.search(r"command", body, re.I) and re.search(r"event", body, re.I), ""))
        r = subprocess.run([sys.executable, str(SPECS_PY), "check", p.stem, "--dir", str(spec_dir),
                            "--intents", str(spec_dir.parent / "intents")], capture_output=True, text=True)
        ex.append(E("Spec passes specs.py check (0 errors)", r.returncode == 0, r.stdout.strip()[-500:]))
    status_part = re.split(r"^\W*(?:git )?diff\b.*$", diff, maxsplit=1, flags=re.M)[0]
    bad = sorted(set(re.findall(r"(src/\S+|docs/plans/\S+|docs/bounded-contexts/\S+|\S*\.cml)", status_part)))
    ex.append(E("No plan, canvas, context map or code produced", not bad, bad or "none"))
    return ex


def eval1(out, p, imeta, final):
    body = split(p.read_text())[1] if p else ""
    nq = not_open_questions(body)
    inv = re.findall(r"\b\d+\s*-?\s*(?:min(?:ute)?s?|h(?:ours?)?|sec(?:ond)?s?)\b", nq, re.I)
    return [
        E("Reservation duration not invented (no concrete duration outside open questions)", p and not inv, inv or "none"),
        E("Reservation duration recorded as an open question",
          p and re.search(r"(how long|duration|period|last)", open_questions(body), re.I), ""),
        E("Spec states that a reserved bike can't be rented by another rider (agrees with the rent intent)",
          p and re.search(r"\brent", body, re.I) and re.search(r"reject|can't|cannot|not allowed|refus", body, re.I), ""),
        E("Intent left 'accepted' (the spec was not self-approved)", imeta.get("status") == "accepted", imeta.get("status")),
        E("Final message lists open questions for the domain expert",
          re.search(r"question|\bQ1\b|\?", final, re.I) and re.search(r"(how long|duration|period)", final, re.I), final[:300]),
    ]


def eval2(out, p, imeta, final):
    body = split(p.read_text())[1] if p else ""
    secs = sections(body)
    notes = "\n".join(v for k, v in secs.items() if NOTES.search(k))
    exs = examples(body)
    appr = [x for x in exs if re.search(r"approv|finance", x, re.I)]
    at_or_below = [x for x in appr if any(9000 <= a <= 10000 for a in amounts(x))]
    above = [x for x in appr if any(a > 10000 for a in amounts(x) if a < 1e6) and not any(9000 <= a <= 10000 for a in amounts(x))]
    big = at_or_below[:1] + above[:1] if at_or_below and above else []
    return [
        E("Technical hints (endpoint/Postgres/Kafka) preserved in a notes-for-plan section",
          re.search(r"kafka|postgres|POST", notes, re.I), notes[:200] or "no notes section"),
        E("Finance approval rule has examples on both sides of 10 000 PLN (at/below and above)", len(big) >= 2, big or appr[:3]),
        E("2 PLN fee rule has an example", any(re.search(r"\b2(?:[.,]00)?\s*PLN|PLN\s*2(?:[.,]00)?\b|fee", x, re.I) for x in exs), ""),
        E("Verified bank account and available balance rules each have an example",
          any(re.search(r"verif", x, re.I) for x in exs) and any(re.search(r"balance", x, re.I) for x in exs), ""),
        E("Intent left 'accepted' (the spec was not self-approved)", imeta.get("status") == "accepted", imeta.get("status")),
    ]


def eval3(out, specs, other, imeta, final):
    return [
        E("No spec file created for a draft intent", not specs and not other, [s.name for s in specs] + other or "none"),
        E("Final message explains the intent is still a draft and must be accepted first",
          re.search(r"draft", final, re.I) and re.search(r"accept|review|approv", final, re.I), final[:300]),
        E("Intent status unchanged (still draft)", imeta.get("status") == "draft", imeta.get("status")),
    ]


def eval4(out, p, imeta, final, spec_dir):
    meta, body = split(p.read_text()) if p else ({}, "")
    secs = sections(body)
    oq = open_questions(body)
    rules = "\n".join(v for k, v in secs.items() if not re.search(r"question|note", k, re.I))
    return [
        E("15 minutes replaces the unknown duration (no '?' left for it)",
          p and re.search(r"15\s*min|10:15", rules) and not re.search(r"\?\s*\(?Q1|\+\s*\?", rules), ""),
        E("Q1 and Q2 no longer open", p and not re.search(r"how long|exactly the bike|reserved bike, or any", oq, re.I), oq[:200]),
        E("Q2 answer reflected in rules/examples (rider must take exactly the reserved bike)",
          p and re.search(r"(only|exactly|other bike|another bike|different bike|B-22)", rules, re.I)
          and re.search(r"reserv", rules, re.I), ""),
        E("Spec status approved", meta.get("status") == "approved", meta.get("status")),
        E("Intent status specified", imeta.get("status") == "specified", imeta.get("status")),
        E("Intents index regenerated (docs/intents/README.md)", (spec_dir.parent / "intents" / "README.md").exists(), ""),
    ]


def main():
    it = Path(sys.argv[1])
    for ed in sorted(it.glob("eval-*")):
        eid = int(ed.name.split("-")[1])
        for run in sorted(ed.glob("*/run-*")):
            out = run / "outputs"
            spec_dir, specs, other, imeta, ibody, diff, final = load(out, eid)
            p = spec_of(specs, eid)
            if eid == 3:
                ex = eval3(out, specs, other, imeta, final)
            else:
                ex = common(out, eid, specs, other, imeta, ibody, diff, spec_dir)
                ex += {1: lambda: eval1(out, p, imeta, final), 2: lambda: eval2(out, p, imeta, final),
                       4: lambda: eval4(out, p, imeta, final, spec_dir)}[eid]()
            passed = sum(e["passed"] for e in ex)
            g = {"expectations": ex,
                 "summary": {"passed": passed, "failed": len(ex) - passed, "total": len(ex),
                             "pass_rate": round(passed / len(ex), 3) if ex else 0.0}}
            (run / "grading.json").write_text(json.dumps(g, indent=2))
            print(f"{ed.name}/{run.parent.name}: {passed}/{len(ex)}")


if __name__ == "__main__":
    main()
