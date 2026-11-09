---
name: tdd
description: Drive Java code test-first, one Given/When/Then scenario per red-green-refactor cycle, with the user in control. At the start it asks whether to stop at every checkpoint or run autonomously. In checkpoint mode it asks for one concrete scenario, writes one failing test, opens it in the IDE and asks whether the user implements the step or Claude generates the smallest change that passes (faking or hard-coding is fine when nothing forces more). Either way, it runs the whole suite, has an independent critic agent review the cycle's diff, opens the implementation in the IDE, suggests refactors or missing scenarios, and asks for the next scenario until the user says the feature is done. Use this whenever the user says TDD, test-first, test-driven, red/green, "write a failing test first", "baby steps", "let's drive this with tests", "fake it till you make it", or hands over a Given/When/Then example and wants just enough code to satisfy it, for VeloCity, CrowdSorcery or any other code in this repo, even if they never say "TDD". To design a whole aggregate from 1-3 invariants, use ddd-tactical-building-blocks instead if it is available; to scaffold a new bounded context, use bounded-context-scaffolder.
---

# TDD: one scenario, smallest change, user in control

This skill runs a strict test-driven micro-cycle and, by default, stops after every test and every implementation step so the user sees the change in the IDE and decides what happens next:

```
choose mode → ask for one scenario → RED: write one failing test
   → Checkpoint A: open the test in the IDE, ask "implement it yourself or should I?"
   → GREEN: the user or Claude makes the smallest change, whole suite green
   → independent critic reviews the cycle's diff
   → Checkpoint B: open the implementation in the IDE, show the critic's findings,
     ask "next scenario / change something / apply refactor / feature is done"
   → repeat until the user says the feature is done → final summary
```

The point is the discipline more than the result. Each scenario pins down one piece of behaviour the domain expert actually asked for, and the code grows only as far as the scenarios push it. Code that no scenario demands is a guess, and guesses in a domain model are exactly what this training teaches people to avoid. So the skill prefers a hard-coded value that passes today's scenario over a "proper" implementation that nothing tests yet. The next scenario will force the generalisation, and that's when it gets written.

The checkpoints exist because this is a training repo. Writing the implementation is often exactly the exercise the user wants to do, and seeing each test and each change in their own IDE keeps their attention on the domain logic and keeps every design step theirs. Moving on before they've seen and accepted the current step takes that away. Each change also stays small enough to review in a few seconds.

Before writing code, read the repo's `CLAUDE.md`, the target system's `CLAUDE.md` (e.g. `src/main/java/pl/wojtyna/dddtrainingv3/velocity/CLAUDE.md`), and the existing test class nearest to the code you'll touch. They set package layout and conventions, and they take priority over generic habits.

---

## Step 0: Choose the mode

At the very start of every invocation, before any test is written, ask how the user wants to work. Use `AskUserQuestion` if it's available, otherwise ask in plain text and end your turn:

- **"Stop at every checkpoint (Recommended)"**: you stop after each test and each implementation step, show it in the IDE, and the user decides who implements each step. Full control, and the best way to learn.
- **"Fully autonomous"**: you write the tests and the implementations for all the scenarios without stopping, and the user reviews everything at the end. Faster, but wrong turns surface later.

If the request has no scenario yet (see Step 1), ask for it **in the same message**, so the user answers once. Don't do a deep exploration or run anything yet, but a quick read of the class the user named is fine when you want to propose a candidate scenario, so the candidate uses the real types and vocabulary.

Skip the question only when the request already names the mode explicitly ("autonomous", "don't stop", "just run through them", "stop after each test", "checkpoints"). Then say in one line which mode you took. If the user switches mode later ("ok, just finish the rest yourself"), follow the switch from the next step on.

**Autonomous mode** keeps the same red/green discipline: one scenario per cycle, a red seen before every green, the "fake it first" ladder, the whole suite after every cycle, and the critic after every green. What changes:
- you write every implementation, with no Checkpoint A or B;
- you don't stop between scenarios while the user's scenario backlog lasts, working through it in the order from Step 1;
- you don't open files in the IDE per step; open the test class and the main production file once, at the end;
- you collect the critic's agreed suggestions and apply none of them on your own;
- if a scenario contradicts an existing test (Step 6), stop anyway, because that's a domain question only the user can answer;
- when the backlog is empty, report (Step 10's format, plus the collected suggestions) and ask for more scenarios or "feature is done".

The rest of this file describes checkpoint mode. Autonomous mode is the same steps with the stops removed.

## Step 1: Get exactly one scenario

Check the request **before** exploring the code: a request that stops here doesn't need a full exploration. If you propose a candidate scenario, first glance at the class under test (its public methods, value objects and events) so the candidate fits the domain as it is coded, not a generic guess.

A usable scenario has:
- **Given:** a concrete starting state ("a station with 1 bike", not "a station with some bikes").
- **When:** one action, ideally one command or method call.
- **Then:** an observable outcome you can assert: a returned value, an event, an exception, a state visible through the public API.
- **Concrete values** wherever a number, name or duration matters ("12 hours", "5.00 PLN").

Handle the input like this:

- **No scenario** ("let's TDD the return flow", "add capacity limits test-first"): ask for one. Show the shape with an example from the area they named, and if useful propose a candidate scenario for them to confirm or correct. Don't write a test or code yet. A scenario you made up encodes your assumptions, not the domain expert's knowledge.
- **More than one scenario:** in checkpoint mode, ask the user which one to start with. Recommend one, usually the simplest or degenerate case (empty, zero, happy path), because it gives the smallest first step and later scenarios build on it. List the others as a backlog for later cycles. In autonomous mode, take them in that recommended order without asking. Either way, one scenario per cycle keeps each change small enough to review.
- **Vague scenario** ("when the rental is too long, then it's rejected"): ask for the concrete value. You may propose one ("too long = more than 11 hours?"), but wait for confirmation, in autonomous mode too. A silently chosen threshold buries an assumption in the code.
- **Exactly one concrete scenario:** restate it in one line as Given / When / Then and carry on without waiting. The user has already given you everything.

If it's unclear *where* the behaviour lives (which system, context or class), work it out from the code. Ask only if it's still ambiguous, and in the same message as any other question.

## Step 2: Check the baseline

Run the full unit test suite **before** changing anything, once per run (not per cycle):

```bash
./mvnw test 2>&1 | grep -E "Tests run:|FAIL|ERROR|BUILD"
```

If it's already red, stop and report which tests fail. Starting from red makes it impossible to tell whether a change broke something, so fixing the existing failure is a separate task the user should decide on.

## Step 3: Red - one failing test

Write **one** test method for the scenario.

**Where and how.** Put it in the test class for the class under test, in the same package under `src/test/java`. Create the class if it doesn't exist. Follow the style of the existing tests in this repo (see Rentals' `BikeReservationTest`):

```java
// @formatter:off
@DisplayName(
    """
     given bike station with 1 bike rented by George,
     when George returns the bike,
     then bike is returned
    """
)
// @formatter:on
@Test
void returningRentedBikePublishesBikeReturned() {
    // given
    ...

    // when
    ...

    // then
    assertThat(...)...;
}
```

- Use the user's scenario wording in `@DisplayName`, keeping their concrete values.
- Give the method a descriptive name, not `test0`.
- Use AssertJ (`assertThat`, `catchThrowable`).

**The assertion must actually assert.** A boolean helper on its own checks nothing. For example, `events.hasAtLeastOneEventMatching(...)` alone is a no-op statement: wrap it as `assertThat(events.hasAtLeastOneEventMatching(...)).isTrue()`. A test that can't fail can't drive anything.

**Make it compile, nothing more.** If the test calls something that doesn't exist yet, add only what's needed to compile: the class, record, method signature or exception type. Give a new method the dumbest body that compiles (return `null`, `DomainEvents.empty()`, `0`, `false`), so the test fails on its assertion. Follow the repo conventions for any new type: records for value objects, jMolecules annotations (`@ValueObject`, `@AggregateRoot`, ...), commands returning `DomainEvents`, and no Lombok. Write no behaviour beyond that: the behaviour is the implementation step, and in checkpoint mode the user may want to write it.

**Run just this test and read the failure:**

```bash
./mvnw test -Dtest='BikeReservationTest#eligibleRiderNearbyReservesAvailableBikeForTwoMinutes' 2>&1 | grep -E "Tests run:|FAIL|ERROR|expected|Expect|Exception|BUILD" | head -30
```

It has to fail **for the right reason**:
- **Right:** the assertion fails, an expected exception isn't thrown, or the step under test hits an existing "not implemented" stub. These show the behaviour is missing.
- **Wrong:** a compile error, an exception in the *given* part, or a typo in the setup. Fix the test or the compile-only scaffolding and run it again.

**If it passes straight away**, the behaviour already exists. Don't touch production code, and never claim a red you didn't see. Say so at Checkpoint A and offer "Keep it, next scenario" instead of "who implements".

## Step 4: Checkpoint A - show the test, then stop and ask

1. **Open the test in the IDE.** Try, in order, and fall through on any failure. A failed attempt never blocks the cycle:
   1. a JetBrains MCP tool for opening files, if one is connected (e.g. `mcp__idea__open_file_in_editor`; load it with ToolSearch if it's deferred). Pass the **absolute** path and the project path: when you work in a git worktree, the file may sit outside the IDE's project root, and a relative path would open the wrong copy;
   2. the `idea` launcher: `idea --line <N> <absolute path>`, which opens the file in the running IDE;
   3. a clickable `path:line` in your message.
2. **Show in the message:** the test method itself, the red you observed (the key line of the failure message), and a list of the compile-only scaffolding you added.
3. **Ask with `AskUserQuestion`.** Make the question self-contained, because the user may see only the dialog: name the test (`BikeStationTest#returningRentedBikePublishesBikeReturned`) and its file. Offer:
   - **"I'll implement it myself"**: the user codes it and tells you when it's done.
   - **"Generate it for me"**: you write the smallest change and show it.

   The user may answer something else, such as changing the scenario or the test. Treat that as a change to this cycle's red step: adjust, run it again, and come back to Checkpoint A.
4. **End your turn.** Don't write any implementation and don't start another test until the user has answered.

## Step 5: Green - the smallest change

### 5a. The user implements it

When they say they're done, run the new test, then the whole suite (Step 6).
- **Green:** acknowledge briefly what they built and go on to the critic (Step 7).
- **Red:** show the failing test(s) and the key line of each failure. Don't fix their code unasked. Ask: **"I'll fix it myself"** / **"Fix it for me"** (then follow 5b from their current code).
- **They changed a test** to make it pass: point it out. Tests are the spec, so a changed test needs their explicit OK.

### 5b. You generate it

Change production code as little as possible to make this one test pass. Take the first option on this list that works:

1. **Return a constant or hard-code the answer** ("fake it"). E.g. `return DomainEvents.of(new BikeReturned());`
2. **Add a condition for exactly this case**, e.g. one `if` with the scenario's threshold.
3. **Use an existing value or field** that's already there.
4. **Add new state or a general algorithm**, only when the options above can't pass the test without breaking another one.

Then:
- Don't touch any other test, and don't edit the new test to fit the code.
- Don't add fields, parameters, classes or branches the scenario doesn't need, even if they'd be "needed eventually". The scenario that needs them will bring them.
- Don't refactor yet.

Run the single test again and confirm it passes.

## Step 6: Run the whole suite

```bash
./mvnw test 2>&1 | grep -E "Tests run:|FAIL|ERROR|BUILD"
```

Every test has to pass, not just the new one. This is the check that the smallest change was really safe. Fakes and hard-coded values are the usual culprits here.

**If another test breaks** after your change, the change was too small or went the wrong way. Revise the **production** change: move one step down the ladder in 5b and run everything again. Never delete, skip, `@Disabled` or edit the other test to get green. (If the user's code broke it, report it as in 5a.)

**If the new scenario contradicts an existing test**, meaning both can't be true for any implementation (for example, one expects rejection at 12 hours and the other expects acceptance), stop, in either mode. Show the user both scenarios side by side and ask which one the domain expert means. That's a domain question, not a coding one. Leave the new test in place and report the state as red.

## Step 7: Independent critic

Once the suite is green, have the cycle reviewed by someone who didn't write it, whether the code came from the user or from you. You're anchored on your own reasoning, and a fresh reader catches the untested branch, the assertion that can't fail, or the convention you forgot.

- Spawn **one** critic with the Agent tool (`general-purpose`). Its prompt contains:
  - the absolute path of `agents/code-critic.md` (next to this file), with the instruction to read it first;
  - the scenario, verbatim as the user gave it;
  - the test class path and test method name;
  - the cycle's diff: `git diff` of the test and production files this cycle touched (for new files, `git diff --no-index /dev/null <file>` or the file content), pasted in or saved to a file whose path you pass;
  - the paths of the repo `CLAUDE.md` and the system's `CLAUDE.md`.

  Give it the evidence, not your conclusions: don't tell it what you think is fine or which shortcuts are intentional. Its value is that it isn't anchored on you.
- **Weigh every finding yourself.** For each one, agree or reject with a one-line reason. Don't relay findings blindly: the critic doesn't know the next scenarios, and it may push you to generalise a fake that the next scenario is meant to force. Reject a finding when it asks for behaviour no scenario demands yet, repeats a shortcut you already named, or touches code outside this cycle. Agreeing with everything usually means you didn't weigh anything. Agreed refactors become a **refactor suggestion**. Agreed gaps (an untested boundary, a missing rejection) become **next-scenario suggestions**, phrased as concrete Given/When/Then.
- **No Agent tool available:** say so, and run the checklist in `agents/code-critic.md` yourself as a separate pass.
- In autonomous mode, run the critic every cycle as well, but collect the agreed suggestions for the final report instead of acting on them.

## Step 8: Checkpoint B - show the implementation, then stop and ask

This happens after **every** green, including when the user wrote the code themselves.

1. **Open the main changed production file in the IDE**, the same way as at Checkpoint A, at the changed method's line.
2. **Show in the message:**
   - the production change (the diff or the changed methods);
   - the suite result (`N tests, 0 failures`);
   - the **shortcuts in the code now**: hard-coded values, ignored parameters, missing branches. These are deliberate, so name each one;
   - the critic's verdict and, for each finding, your take (agree / reject, one line why).
3. **Ask with `AskUserQuestion`**, naming the test and the changed file:
   - **"Next scenario"**: put your one suggested scenario in the description, the one that forces the most obvious shortcut or closes an agreed gap. The user may confirm it or type their own;
   - **"Apply suggested refactor"**: only if you agree with at least one critic finding or see a worthwhile clean-up yourself. Summarise it in the description; the details are in the message;
   - **"Change something"**;
   - **"Feature is done"**.
4. **End your turn.**

What happens next:
- **Next scenario:** go back to Step 3 with it (Step 1's rules apply if it's vague). Don't start a cycle on your own suggestion without the user's confirmation.
- **Change something:** apply it, rerun the test and the suite, and come back to Checkpoint B. If they changed the scenario itself, go back to Step 3.
- **Apply suggested refactor:** Step 9.
- **Feature is done:** Step 10.

## Step 9: Refactor, only when chosen

Refactor only when the user picked it at Checkpoint B (or asked for it), and only in code this cycle touched:
- duplication introduced in this cycle, including between the test and production code;
- unclear names;
- leftovers from the compile-only step.

Run the whole suite again afterwards; it has to stay green. Then come back to Checkpoint B with the refactor shown and opened in the IDE. Anything bigger, or anything in code this cycle didn't touch, goes into the final summary as a suggestion. Don't generalise a fake here: replacing a hard-coded value is the next scenario's job, and doing it now means writing untested behaviour.

## Step 10: Feature done - final summary

When the user says the feature is done (or, in autonomous mode, the backlog is finished), run the whole suite once more, then report briefly:

```
## TDD run: <feature in one line>   (mode: checkpoints | autonomous)

| # | Scenario | Test | Red seen | Implemented by | Green |
|---|----------|------|----------|----------------|-------|
| 1 | <one line> | `<TestClass#method>` | <key failure line> | user / Claude | yes |

**Suite:** <N tests run, 0 failures>
**Shortcuts still in the code:** <hard-coded values, ignored parameters, missing branches>
**Open suggestions:** <critic findings you agreed with but weren't applied; next scenarios worth adding>
```

Keep the per-cycle log as you go so this table is accurate. Never claim a red you didn't observe. Don't commit unless the user asks.
