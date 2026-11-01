#!/usr/bin/env python3
"""Programmatic grader for bounded-context-scaffolder evals.

Usage: grade.py <run_dir> <eval_name>
Reads <run_dir>/outputs/{changes.diff,final_message.md,build.txt,files/} and writes <run_dir>/grading.json
"""
import json, re, sys
from pathlib import Path

run = Path(sys.argv[1]); name = sys.argv[2]
out = run / "outputs"
read = lambda p: p.read_text(errors="ignore") if p.exists() else ""
diff = read(out / "changes.diff")
final = read(out / "final_message.md")
build = read(out / "build_verified.txt") or read(out / "build.txt")
files_root = out / "files"
files = {str(p.relative_to(files_root)): read(p) for p in files_root.rglob("*") if p.is_file()} if files_root.exists() else {}

# Fall back to the diff for file contents when files/ is incomplete
for m in re.finditer(r"^diff --git a/(\S+) b/\S+\n(.*?)(?=^diff --git |\Z)", diff, re.S | re.M):
    path, body = m.group(1), m.group(2)
    if path not in files:
        files[path] = "\n".join(l[1:] for l in body.splitlines() if l.startswith("+") and not l.startswith("+++"))

def under(prefix, suffix=""):
    return {p: c for p, c in files.items() if prefix in p and p.endswith(suffix)}

results = []
def check(text, passed, evidence):
    results.append({"text": text, "passed": bool(passed), "evidence": evidence})

def build_green():
    ok = "BUILD SUCCESS" in build and "BUILD FAILURE" not in build
    check("Maven build/tests pass (BUILD SUCCESS, no BUILD FAILURE)", ok,
          "build.txt: " + ("success" if ok else (build.strip().splitlines()[-1] if build.strip() else "missing")))

def bc_name(ctx, expected):
    pi = under(f"velocity/{ctx}/package-info.java")
    pi = {p: c for p, c in pi.items() if p.endswith(f"velocity/{ctx}/package-info.java")}
    c = next(iter(pi.values()), "")
    ok = f'@BoundedContext(name = "{expected}")' in c and "@Module" in c
    check(f'package-info.java has @BoundedContext(name = "{expected}") and @Module', ok, next(iter(pi), "no package-info.java found"))
    return c

def plan_file(ctx):
    plans = {p: c for p, c in files.items() if p.endswith(".md") and ctx in p.lower() and "plan" in p.lower()}
    check(f"Plan markdown file created for {ctx}", plans, ", ".join(plans) or "none")
    return next(iter(plans.values()), "")

def no_lombok():
    check("No Lombok used", "lombok" not in diff.lower(), "searched changes.diff")

def no_getters(ctx):
    main = under(f"main/java/pl/wojtyna/dddtrainingv3/velocity/{ctx}/", ".java")
    getters = [p for p, c in main.items() if re.search(r"public \w[\w<>, ]* get[A-Z]\w*\(\)", c)]
    recs = sum(len(re.findall(r"\brecord \w+", c)) for c in main.values())
    check("Data types are records, no getX() getters in generated main code", not getters and recs >= 3,
          f"records={recs}; files with getters: {getters or 'none'}")

def tests(ctx, min_tests, keyword=None):
    t = under(f"test/java/pl/wojtyna/dddtrainingv3/velocity/{ctx}/", ".java")
    blob = "\n".join(t.values())
    n = len(re.findall(r"@Test\b", blob))
    gwt = len(re.findall(r"@DisplayName\(\s*\"\"\"[^\"]*given", blob, re.I | re.S))
    kw = len(re.findall(keyword, blob, re.I)) if keyword else None
    ok = n >= min_tests and gwt >= min_tests and (kw is None or kw >= 1)
    check(f"At least {min_tests} GWT tests (@DisplayName text block with given/when/then)" + (f" covering '{keyword}'" if keyword else ""),
          ok, f"@Test={n}, GWT DisplayNames={gwt}" + (f", '{keyword}' hits={kw}" if keyword else "") + f"; files: {list(t)}")

def has(ctx, sub, pattern, text):
    m = {p: c for p, c in under(f"velocity/{ctx}/{sub}", ".java").items() if re.search(pattern, c)}
    check(text, m, ", ".join(m) or "none")

if name == "pricing-scaffold-from-markdown":
    plan_file("pricing")
    pi = bc_name("pricing", "Pricing")
    secs = ["purpose", "strategic", "domain role", "ubiquitous", "business decision", "assumption", "verification", "open question"]
    missing = [s for s in secs if s not in pi.lower()]
    check("package-info javadoc has all 8 canvas sections", not missing, f"missing: {missing or 'none'}")
    has("pricing", "api/usecases/", r"@PrimaryPort", "Inbound commands/events mapped to @PrimaryPort use cases in api/usecases")
    has("pricing", "api/queries/", r"@PrimaryPort", "Inbound query mapped to @PrimaryPort handler in api/queries")
    has("pricing", "api/events/", r"record \w+\([^)]*\)\s*implements IntegrationEvent", "Outbound event is an IntegrationEvent record in api/events")
    acl = {p: c for p, c in under("velocity/pricing/infra/adapters/secondary/", "package-info.java").items() if re.search(r"^@AntiCorruptionLayer", c, re.M)}
    check("Outbound query to Rentals goes through an @AntiCorruptionLayer secondary-adapter package", acl, ", ".join(acl) or "none")
    no_getters("pricing"); no_lombok(); tests("pricing", 4); build_green()

elif name == "settlement-inline-conformist-vague-rule":
    md = [p for p in files if p.endswith(".md") and "settlement" in p.lower() and "canvas" in p.lower()]
    check("Recommends/creates a markdown canvas for the inline input", md or re.search(r"canvas[^\n]*\.md|markdown", final, re.I),
          f"canvas files: {md}; final message mentions markdown/canvas.md: {bool(re.search(r'canvas[^\\n]*\\.md|markdown', final, re.I))}")
    push = re.search(r"(ACL|anti-corruption)", final, re.I) and re.search(r"(recommend|risk|suggest|reconsider|leak|ripple)", final, re.I)
    check("Pushes back on skipping the ACL (mentions ACL + risk/recommendation)", push, "final_message.md")
    acl_pay = [p for p, c in files.items() if "payment" in p.lower() and p.endswith("package-info.java") and re.search(r"^@AntiCorruptionLayer", c, re.M)]
    check("Complies: Payments integration is NOT wrapped in an @AntiCorruptionLayer package", not acl_pay, f"ACL payment packages: {acl_pay or 'none'}")
    plan = plan_file("settlement")
    check("Conformist decision recorded in the plan", re.search(r"conformist", plan, re.I), "plan mentions conformist" if re.search("conformist", plan, re.I) else "not found")
    vague = re.search(r"(awaiting|not (yet )?modell?ed|needs? .*given/when/then|GWT)", plan + final, re.I)
    check("Vague retry rule flagged as awaiting a strict GWT example", vague, vague.group(0) if vague else "not found")
    main = "\n".join(under("main/java/pl/wojtyna/dddtrainingv3/velocity/settlement/", ".java").values())
    hard = re.search(r"(?i)(max\w*(attempt|retr)\w*\s*=\s*\d|attempts?\w*\s*(>=|<|==|>)\s*\d)", main)
    if not read(out / "phaseA_plan.md"):
      check("Retry count not hard-coded from a guessed threshold", not hard, hard.group(0) if hard else "no hard-coded retry threshold")
    bc_name("settlement", "Settlement"); build_green()

elif name == "debtrecovery-no-canvas":
    java = [p for p in files if "debtrecovery" in p.lower() and p.endswith(".java")]
    check("No Java code generated without a canvas", not java, f"java files: {java or 'none'}")
    canv = {p: c for p, c in files.items() if "debtrecovery" in p.lower() and "canvas" in p.lower() and p.endswith(".md")}
    check("Pre-filled canvas template created for DebtRecovery", canv, ", ".join(canv) or "none")
    c = next(iter(canv.values()), "")
    check("Template contains Given/When/Then business-decision section", re.search(r"given", c, re.I) and re.search(r"then", c, re.I), "checked canvas file")
    check("Final message asks the user to fill in the canvas", re.search(r"fill", final, re.I), "final_message.md")

elif name == "accounts-shared-kernel-exception":
    plan_file("accounts"); bc_name("accounts", "Accounts"); tests("accounts", 2); no_lombok(); build_green()

elif name == "rentals-update-with-changelog":
    plan = plan_file("rentals")
    check("Plan has a dated Changelog entry", re.search(r"changelog", plan, re.I) and re.search(r"20\d\d-\d\d-\d\d", plan), "checked plan")
    bs = next((c for p, c in files.items() if p.endswith("rentals/domain/bikestation/BikeStation.java")), "")
    m = re.search(r"returnBike\(.*?\n    }", bs, re.S)
    check("returnBike implemented (no UnsupportedOperationException)", bs and m and "UnsupportedOperationException" not in m.group(0), "BikeStation.java " + ("modified" if bs else "not modified"))
    tests("rentals", 2, r"return")
    removed_tests = re.findall(r"^-\s+void (test\w*)\(", diff, re.M)
    added_tests = re.findall(r"^\+\s+void (test\w*)\(", diff, re.M)
    lost = sorted(set(removed_tests) - set(added_tests))
    check("Existing BikeStationTest methods preserved", not lost, f"removed: {lost or 'none'}")
    check("No files deleted", "deleted file mode" not in diff, "searched diff")
    has("rentals", "api/events/", r"record \w+\([^)]*\)\s*implements IntegrationEvent", "Bike Returned published as IntegrationEvent record in api/events")
    has("rentals", "api/queries/", r"@PrimaryPort", "Available Bikes query handler in api/queries")
    bc_name("rentals", "Rentals"); build_green()

# --- Iteration 2: review gate + resume + javadoc/rule sync (only when phase A outputs exist) ---
ctx = {"pricing-scaffold-from-markdown": ("pricing", r"8\.00|drop.?zone"),
       "settlement-inline-conformist-vague-rule": ("settlement", r"12\.50|3rd|third"),
       "rentals-update-with-changelog": ("rentals", r"return"),
       "accounts-shared-kernel-exception": ("accounts", r"block")}.get(name)
pa_diff, pa_plan, pa_final = read(out / "phaseA_changes.diff"), read(out / "phaseA_plan.md"), read(out / "phaseA_final_message.md")
if ctx and (pa_diff or pa_plan or pa_final):
    c, follow = ctx
    java_a = re.findall(r"^diff --git a/(\S+\.java) ", pa_diff, re.M)
    check("Phase A: no Java code before the plan is approved", not java_a, f"java files in phase A: {java_a or 'none'}")
    check("Phase A: plan saved with Status 'draft - awaiting review'", re.search(r"status\W*draft", pa_plan, re.I), "phaseA_plan.md")
    check("Phase A: final message asks the user to review/approve the plan", re.search(r"review|approv", pa_final, re.I), "phaseA_final_message.md")
    plan_b = next((v for k, v in files.items() if k.endswith(f"{c}/plan.md")), "")
    check("Phase B: plan Status set to 'implemented'", re.search(r"status\W*implemented", plan_b, re.I), "final plan.md")
    check("Phase B: Review notes record the approval", re.search(r"review notes.*approv", plan_b, re.I | re.S), "final plan.md")
    pi = next((v for k, v in files.items() if k.endswith(f"velocity/{c}/package-info.java")), "")
    bd = pi.split("Business decisions", 1)[1] if "Business decisions" in pi else ""
    refs = re.findall(r"\w+Test#\w+", bd)
    check("Phase B: javadoc Business decisions name the tests that enforce them", len(refs) >= 2, f"test refs: {refs}")
    check("Phase B: javadoc reflects the rule added/changed at approval", re.search(follow, bd, re.I), f"pattern {follow!r} in Business decisions")
    if c == "settlement":
        bullets = re.split(r"\n/// - ", bd)
        r1 = [b for b in bullets if re.search(r"3rd charge attempt fails", b, re.I)]
        ok = r1 and all(re.search(r"Test#", b) and not re.search(r"not modell?ed", b, re.I) for b in r1)
        check("Phase B: the supplied retry rule is listed as enforced (with its test), not as 'not modelled yet'", ok,
              f"{len(r1)} bullet(s) for the 3rd-attempt rule")

# --- Iteration 3: architecture test guarding the context boundary ---
arch_ctx = {"pricing-scaffold-from-markdown": "pricing", "settlement-inline-conformist-vague-rule": "settlement",
            "accounts-shared-kernel-exception": "accounts"}.get(name)
if arch_ctx and ((out / "phaseA_plan.md").exists() or arch_ctx == "accounts"):
    at = {p_: c_ for p_, c_ in files.items() if re.search(rf"test/java/.*/velocity/{arch_ctx}/\w+ArchitectureTest\.java$", p_)}
    src = next(iter(at.values()), "")
    two = src.count("dependOnClassesThat") >= 2 and ".api" in src
    check("Architecture test exists with the incoming and outgoing api-only rules", at and two, ", ".join(at) or "none")
    pom = re.search(r"archunit-junit5</artifactId>\s*<version>1\.5\.1", files.get("pom.xml", "")) or re.search(r"^\+.*1\.5\.1", diff, re.M) and "archunit-junit5" in diff
    check("ArchUnit archunit-junit5 1.5.1 added to pom.xml (test scope)", pom, "pom.xml diff")
    ran = re.search(r"Tests run: [1-9]\d*, Failures: 0, Errors: 0.*ArchitectureTest", build)
    check("Architecture test ran green in the verified build", ran, ran.group(0)[:120] if ran else "no green ArchitectureTest line in build_verified.txt")
    exc = re.search(r"DescribedPredicate\.not\(|\bnot\(resideIn|ArchIgnore|ignoreDependency", src)
    if arch_ctx == "accounts":
        check("Exactly the requested exception: rentals' bikestation package excluded, with a DECISION comment in the test",
              exc and "DECISION" in src and re.search(r"velocity\.rentals\.domain\.bikestation", src) and len(re.findall(r"DescribedPredicate\.not\(|\bnot\(resideIn", src)) == 1,
              "exception clauses: %d" % len(re.findall(r"DescribedPredicate\.not\(|\bnot\(resideIn", src)))
        main_dec = [p_ for p_, c_ in files.items() if "main/java" in p_ and "/velocity/accounts/" in p_ and re.search(r"DECISION D\d+", c_)]
        check("DECISION comment at the usage site in Accounts' main code", main_dec, ", ".join(main_dec) or "none")
        plan_txt = next((v for k, v in files.items() if k.endswith("accounts/plan.md")), "") + read(out / "phaseA_plan.md")
        check("Plan records the shared-kernel decision with a D-number and its risk",
              re.search(r"shared kernel", plan_txt, re.I) and re.search(r"\bD\d+\b", plan_txt) and re.search(r"risk", plan_txt, re.I), "plan.md")
        chal = re.search(r"shared kernel", pa_final + final, re.I) and re.search(r"(coupl|risk|refactor|break|instead|recommend|suggest)", pa_final + final, re.I)
        check("Challenged the shared-kernel request before complying", chal, "phaseA/final message")
        uses = [p_ for p_, c_ in files.items() if "/velocity/accounts/domain/" in p_ and "rentals.domain.bikestation.BorrowerId" in c_]
        check("Complied: Accounts' domain uses Rentals' BorrowerId directly", uses, ", ".join(uses) or "none")
    else:
        check("No boundary exceptions without an explicit request", at and not exc and "DECISION" not in src, "exception found" if exc else "none")

p = sum(r["passed"] for r in results)
json.dump({"expectations": results, "summary": {"passed": p, "failed": len(results) - p, "total": len(results),
           "pass_rate": round(p / len(results), 2) if results else 0}}, open(run / "grading.json", "w"), indent=2)
print(f"{name} {run.name}: {p}/{len(results)}")
