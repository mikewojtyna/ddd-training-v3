# Exercise 12: Review the agent

**Module:** M5 Testing strategies with AI · **Time:** 45 min · **Form:** pairs · **Start:** tag `ex12/start`

## Situation

An agent delivered a new business need, **`rider-cancels-reservation`**, in one commit. Product approved the spec.
The build is green and every test passes. The agent says it's done.

```bash
git show ex12/start --stat     # what changed
git show ex12/start            # the whole PR
```

The spec it worked from: `docs/specs/rider-cancels-reservation.md` (R1–R3, E1.1–E3.1).

## Your task

1. **Review it yourselves first (25 min), with the checklist below.** Don't run an agent yet. Write each finding as:
   file and line, what's wrong, which spec rule or architecture rule it breaks, and what goes wrong in practice.
2. **Then run a skeptic agent (10 min)** in a fresh session that has never seen this conversation, for example:
   *"Review the last commit against docs/specs/rider-cancels-reservation.md and the rules in CLAUDE.md. For every spec
   example find the test that proves it; flag domain logic outside `domain/`; flag tests that check the implementation
   instead of behaviour."*
3. **Compare (10 min):** what did you find that the agent missed, and the other way round?

## Review checklist for an agent's PR

**Spec coverage**
- [ ] Every rule and every example in the spec has a test that fails when the rule is broken.
- [ ] The test names and `@DisplayName`s use the spec's IDs (E1.1, …) and its language.
- [ ] Nothing was built that the spec doesn't ask for.

**Placement**
- [ ] Every business decision is in `domain/`: no "if" about business rules in controllers, listeners, jobs or use cases.
- [ ] Every parameter of a domain method is used. An ignored parameter often means a rule that lives somewhere else.
- [ ] Use cases only load, call the domain, save and publish.
- [ ] Cross-context calls go through `@AntiCorruptionLayer` packages, and the CML context map is still true.

**Tests**
- [ ] Tests assert observable behaviour: events, replies, what another actor can do next. They don't assert private
      state, snapshots, versions or mocks of the code's own internals.
- [ ] Each test would fail if you deleted the line that implements its rule. Try it on one.
- [ ] Unhappy paths are tested, not only the happy path.

**Process**
- [ ] The intent, spec (and plan) statuses were changed by the scripts, and they match what was delivered.
- [ ] `./mvnw test` and the `*.py check` scripts pass, but treat that as the minimum. Green isn't enough on its own.

## Done when

- You have a written list of findings, each tied to a rule.
- You know which findings the skeptic agent also found, and which it missed.

## Debrief questions

- Which finding would have caused a production bug first? How would it have shown up?
- Why did every test pass?
- Reference answers: `git show ex12/solution:REVIEW-ANSWERS.md`, and the fix: `git show ex12/solution`.

**Extension:** turn the skeptic prompt into a reusable command (`.claude/commands/review.md`) or a subagent
(`.claude/agents/architecture-reviewer.md`) for your own repository.
