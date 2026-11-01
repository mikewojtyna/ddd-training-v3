#!/usr/bin/env python3
"""Grade intent-creator eval runs. Usage: grade.py <iteration-dir>

Writes grading.json (expectations: text/passed/evidence) into every run dir.
"""
import json
import re
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
SKILL = HERE.parent.parent
SEED = HERE.parent / "files" / "seed-velocity-intents"
INTENTS_PY = SKILL / "scripts" / "intents.py"

TECH_I = re.compile(r"\b(endpoints?|api|http|postgres(?:ql)?|database|kafka|redis|tables?|"
                    r"microservices?|backend|aggregates?|bounded contexts?|repositor(?:y|ies)|"
                    r"spring|java|class(?:es)?)\b", re.I)
TECH_CS = re.compile(r"\b(POST|GET|PUT|DELETE|REST|SQL)\b")


def tech_hits(text):
    return sorted(set(TECH_I.findall(text)) | set(TECH_CS.findall(text)))
SPECIAL = {"README.md", "story-map.md", "INDEX.md", "index.md"}


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
    for line in body.splitlines():
        h = re.match(r"^#{2,3}\s+(.+?)\s*$", line)
        if h:
            cur = h.group(1)
            out[cur] = []
        else:
            out[cur].append(line)
    return {k: "\n".join(v) for k, v in out.items()}


def business_text(body):
    secs = sections(re.sub(r"<!--.*?-->", "", body, flags=re.S))
    return "\n".join(v for k, v in secs.items()
                     if not re.search(r"spec|technical|implementation|notes for", k, re.I))


def load_intents(out):
    d = out / "files" / "docs" / "intents"
    if not d.is_dir():
        return d, []
    res = []
    for p in sorted(d.glob("*.md")):
        if p.name in SPECIAL:
            continue
        meta, body = split(p.read_text(encoding="utf-8"))
        title = meta.get("title") or (re.search(r"^#\s+(.+)$", body, re.M) or [None, p.stem])[1]
        res.append({"path": p, "meta": meta, "body": body, "title": title})
    return d, res


def actor_of(i):
    if i["meta"].get("actor"):
        return i["meta"]["actor"]
    m = re.search(r"\*\*Actors?:?\*\*:?\s*([A-Za-z ]+)", i["body"]) or re.search(r"As an?\s+\**([A-Za-z ]+?)\**[,\s]", i["body"])
    return m.group(1) if m else ""


def E(text, passed, evidence):
    return {"text": text, "passed": bool(passed), "evidence": evidence}


def common(out, intents, d):
    ex = []
    names = [i["path"].name for i in intents]
    ex.append(E("Intent files created in docs/intents/<kebab-name>.md", intents and all(
        re.fullmatch(r"[a-z0-9]+(-[a-z0-9]+)*\.md", n) for n in names), ", ".join(names) or "none"))
    actors = {i["path"].name: actor_of(i) for i in intents}
    ex.append(E("Every intent names one business-role actor (not system/user)", intents and all(
        a and a.lower() not in {"system", "the system", "user", "backend"} for a in actors.values()), str(actors)))
    ex.append(E("Every intent states the business value ('so that')", intents and all(
        re.search(r"so that", i["body"], re.I) for i in intents),
        str({i["path"].name: bool(re.search(r"so that", i["body"], re.I)) for i in intents})))
    hits = {i["path"].name: tech_hits(business_text(i["body"])) for i in intents}
    hits = {k: v for k, v in hits.items() if v}
    ex.append(E("No solution vocabulary outside a spec/technical-notes section", intents and not hits,
                str(hits) or "clean"))
    gwt = [i["path"].name for i in intents if re.search(r"^\s*[-*]?\s*\**Given\b.*\n?.*\bWhen\b", i["body"], re.M | re.I)
           or re.search(r"^#+\s*Acceptance criteria", i["body"], re.M | re.I)]
    ex.append(E("No Given/When/Then acceptance criteria in intents (left to the spec)", intents and not gwt,
                str(gwt) or "none"))
    coarse = [i["title"] for i in intents if re.search(r"\b(and|manage|handle|support)\b", i["title"], re.I)]
    ex.append(E("Each intent title is a single goal (no 'and'/'manage'/'handle'/'support')", intents and not coarse,
                str(coarse) or "all single-goal"))
    seqs = [i["meta"].get("sequence") for i in intents]
    ex.append(E("Each intent has a unique sequence number (project history order)",
                intents and all(s and s.isdigit() for s in seqs) and len(set(seqs)) == len(seqs), str(seqs)))
    placed = [i["path"].name for i in intents if i["meta"].get("activity") or re.search(r"story map|activity", i["body"], re.I)]
    ex.append(E("Each intent is placed on a story map (activity recorded)", intents and len(placed) == len(intents),
                f"{len(placed)}/{len(intents)}"))
    diff = (out / "changes.diff").read_text(encoding="utf-8", errors="replace") if (out / "changes.diff").exists() else ""
    status = re.split(r"^\W*(?:git )?diff\b.*$", diff, maxsplit=1, flags=re.M)[0]
    bad = re.findall(r"(src/\S+|\S*spec\.md|\S*plan\.md)", status)
    ex.append(E("No spec.md, plan.md or code produced", not bad, str(sorted(set(bad))) or "none"))
    if d.is_dir():
        r = subprocess.run([sys.executable, str(INTENTS_PY), "check", "--dir", str(d)], capture_output=True, text=True)
        ex.append(E("Intent set passes intents.py check (0 errors)", r.returncode == 0, r.stdout.strip()[-600:]))
    else:
        ex.append(E("Intent set passes intents.py check (0 errors)", False, "no docs/intents"))
    return ex


def eval1(out, intents, final):
    ex = [E("At least 3 separate intent files (epic split)", len(intents) >= 3, str(len(intents)))]
    tech = [i for i in intents if "technician" in actor_of(i).lower()]
    ex.append(E("Technician reservation is its own intent, separate from the rider's", tech and any(
        "rider" in actor_of(i).lower() for i in intents), str([i["path"].name for i in tech])))
    inv = [i["path"].name for i in intents if re.search(r"\b\d+\s*-?\s*(?:min(?:ute)?s?|sec(?:ond)?s?)\b",
                                                        business_text(sections(i["body"]).get("_pre", "") and
                                                                      "\n".join(v for k, v in sections(i["body"]).items()
                                                                                if not re.search(r"open question", k, re.I))), re.I)]
    ex.append(E("Reservation duration is not invented (no concrete minutes/seconds outside open questions)",
                not inv, str(inv) or "none"))
    return ex


def eval2(out, intents, final):
    inv = [i for i in intents if "investor" in actor_of(i).lower()]
    ex = [E("One or two intents, actor Investor, about withdrawing money", 1 <= len(intents) <= 2 and inv and any(
        re.search(r"withdraw", i["title"], re.I) for i in inv), str([i["path"].name for i in intents]))]
    alltext = "\n".join(i["body"] for i in intents)
    ex.append(E("Technical hints (endpoint/table/Kafka) preserved somewhere for the spec",
                re.search(r"kafka|POST|postgres|table", alltext, re.I), "found" if re.search(r"kafka|POST|postgres", alltext, re.I) else "lost"))
    ex.append(E("Rules 'verified bank account' and 'available balance' captured",
                re.search(r"verified bank account", alltext, re.I) and re.search(r"available balance", alltext, re.I), ""))
    ex.append(E("Title/file name is business-worded (no endpoint/table/event names)", intents and not any(
        tech_hits(i["title"] + " " + i["path"].stem.replace("-", " ")) or "WithdrawalRequested" in i["title"] for i in intents),
        str([i["title"] for i in intents])))
    return ex


def eval3(out, intents, final):
    ex = []
    by = {i["path"].stem: i for i in intents}
    old = by.get("rider-returns-bike-to-origin-station")
    seed_meta, seed_body = split((SEED / "rider-returns-bike-to-origin-station.md").read_text())
    if old:
        ex.append(E("Accepted return intent's text is preserved (not rewritten)", old["body"].strip() == seed_body.strip(),
                    "body identical" if old["body"].strip() == seed_body.strip() else "body changed"))
        ex.append(E("Old return intent marked superseded with a pointer to the new one",
                    old["meta"].get("status") == "superseded" and old["meta"].get("superseded_by") in by,
                    f"status={old['meta'].get('status')} superseded_by={old['meta'].get('superseded_by')}"))
    else:
        ex.append(E("Accepted return intent's text is preserved (not rewritten)", False, "file missing/renamed"))
        ex.append(E("Old return intent marked superseded with a pointer to the new one", False, "file missing"))
    new = [i for i in intents if i["path"].stem not in {"rider-returns-bike-to-origin-station", "rider-rents-bike-from-station"}]
    ret = [i for i in new if re.search(r"return", i["title"], re.I)]
    ex.append(E("New return-to-any-station intent references the intent it replaces", ret and any(
        "rider-returns-bike-to-origin-station" in (i["meta"].get("supersedes", "") + i["body"]) for i in ret),
        str([i["path"].name for i in ret])))
    res = [i for i in new if re.search(r"reserv", i["title"], re.I)]
    rt = "\n".join(i["body"] for i in res)
    ex.append(E("Reservation intent keeps '15 minutes' and 'one active reservation per rider'",
                res and re.search(r"15\s*min", rt, re.I) and re.search(r"one active reservation", rt, re.I),
                str([i["path"].name for i in res])))
    seqs = sorted(int(i["meta"].get("sequence")) for i in new if (i["meta"].get("sequence") or "").isdigit())
    ex.append(E("New intents continue the sequence after 2 (3, 4, ...)", seqs and seqs == list(range(3, 3 + len(new))), str(seqs)))
    ex.append(E("Reservation intent links to the rent intent", res and any("rider-rents-bike-from-station" in i["body"] for i in res), ""))
    return ex


def eval4(out, intents, final):
    diff = (out / "changes.diff").read_text(errors="replace") if (out / "changes.diff").exists() else ""
    return [
        E("No intent file created for a purely technical upgrade", not intents, str([i["path"].name for i in intents])),
        E("Final message explains there is no business need behind it", re.search(
            r"business|not an intent|no intent|isn't an intent|is not a business", final, re.I), final[:300]),
        E("pom.xml untouched", "pom.xml" not in re.split(r"^\W*(?:git )?diff\b.*$", diff, maxsplit=1, flags=re.M)[0], ""),
    ]


SPECIFIC = {1: eval1, 2: eval2, 3: eval3, 4: eval4}


def main():
    it = Path(sys.argv[1])
    for ed in sorted(it.glob("eval-*")):
        eid = int(ed.name.split("-")[1])
        for run in sorted(ed.glob("*/run-*")):
            out = run / "outputs"
            d, intents = load_intents(out)
            final = (out / "final_message.md").read_text(errors="replace") if (out / "final_message.md").exists() else ""
            ex = ([] if eid == 4 else common(out, intents, d)) + SPECIFIC[eid](out, intents, final)
            passed = sum(e["passed"] for e in ex)
            g = {"expectations": ex, "summary": {"passed": passed, "failed": len(ex) - passed, "total": len(ex),
                                                 "pass_rate": round(passed / len(ex), 3) if ex else 0}}
            (run / "grading.json").write_text(json.dumps(g, indent=2))
            print(f"{ed.name}/{run.parent.name}: {passed}/{len(ex)}")


if __name__ == "__main__":
    main()
