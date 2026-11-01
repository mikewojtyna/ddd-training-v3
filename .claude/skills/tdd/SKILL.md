---
name: tdd
description: Drive Java code test-first, one Given/When/Then scenario per red-green-refactor cycle. Ask the user for exactly one concrete scenario, write one failing test for it, make the smallest possible production change that passes it (faking or hard-coding is fine when nothing forces more), then run the whole unit test suite to prove nothing else broke, and report and ask for the next scenario. Use this whenever the user says TDD, test-first, test-driven, red/green, "write a failing test first", "baby steps", "let's drive this with tests", "fake it till you make it", or hands over a single Given/When/Then example and wants just enough code to satisfy it, for VeloCity, CrowdSorcery or any other code in this repo, even if they never say "TDD". To design a whole aggregate from 1-3 invariants, use ddd-tactical-building-blocks instead if it is available; to scaffold a new bounded context, use bounded-context-scaffolder.
---

# TDD: one scenario, smallest change

This skill runs a strict test-driven micro-cycle:

1. Get **one** Given/When/Then scenario from the user.
2. **Red:** write one test for it and watch it fail for the right reason.
3. **Green:** make the smallest production change that makes it pass.
4. Run **the whole unit test suite** to prove nothing else broke.
5. **Refactor** only what this cycle introduced, with the suite still green.
6. Report, then ask for the next scenario.

The point is the discipline more than the result. Each scenario pins down one piece of behaviour the domain expert actually asked for, and the code grows only as far as the scenarios push it. Code that no scenario demands is a guess, and guesses in a domain model are exactly what this training teaches people to avoid. So the skill prefers a hard-coded value that passes today's scenario over a "proper" implementation that nothing tests yet. The next scenario will force the generalisation, and that's when it gets written.

Before writing code, read the repo's `CLAUDE.md`, the target system's `CLAUDE.md` (e.g. `src/main/java/pl/wojtyna/dddtrainingv3/velocity/CLAUDE.md`), and the existing test class nearest to the code you'll touch. They set package layout and conventions, and they take priority over generic habits.

---

## Step 1: Get exactly one scenario

Check the request **before** exploring the code: a request that stops here doesn't need the code yet.

A usable scenario has:
- **Given:** a concrete starting state ("a station with 1 bike", not "a station with some bikes").
- **When:** one action, ideally one command or method call.
- **Then:** an observable outcome you can assert: a returned value, an event, an exception, a state visible through the public API.
- **Concrete values** wherever a number, name or duration matters ("12 hours", "5.00 PLN").

Handle the input like this:

- **No scenario** ("let's TDD the return flow", "add capacity limits test-first"): ask for one. Show the shape with an example from the area they named, and if useful propose a candidate scenario for them to confirm or correct. Don't write a test or code yet. A scenario you made up encodes your assumptions, not the domain expert's knowledge.
- **More than one scenario:** ask the user which one to start with. Recommend one, usually the simplest or degenerate case (empty, zero, happy path), because it gives the smallest first step and later scenarios build on it. List the others as a backlog for later cycles. Don't silently pick one and don't do several in one cycle: one scenario per cycle keeps each change small enough to review.
- **Vague scenario** ("when the rental is too long, then it's rejected"): ask for the concrete value. You may propose one ("too long = more than 11 hours?"), but wait for confirmation. A silently chosen threshold buries an assumption in the code.
- **Exactly one concrete scenario:** restate it in one line as Given / When / Then and carry on without waiting. The user has already given you everything.

If it's unclear *where* the behaviour lives (which system, context or class), work it out from the code. Ask only if it's still ambiguous, and in the same message as any other question.

## Step 2: Check the baseline

Run the full unit test suite **before** changing anything:

```bash
./mvnw test 2>&1 | grep -E "Tests run:|FAIL|ERROR|BUILD"
```

If it's already red, stop and report which tests fail. Starting from red makes it impossible to tell whether your change broke something, so fixing the existing failure is a separate task the user should decide on.

## Step 3: Red - one failing test

Write **one** test method for the scenario.

**Where and how.** Put it in the test class for the class under test, in the same package under `src/test/java`. Create the class if it doesn't exist. Follow the style of the existing tests in this repo (see `BikeStationTest`):

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

**Make it compile, nothing more.** If the test calls something that doesn't exist yet, add only what's needed to compile: the class, record, method signature or exception type. Give a new method the dumbest body that compiles (return `null`, `DomainEvents.empty()`, `0`, `false`), so the test fails on its assertion. Follow the repo conventions for any new type: records for value objects, jMolecules annotations (`@ValueObject`, `@AggregateRoot`, ...), commands returning `DomainEvents`, and no Lombok.

**Run just this test and read the failure:**

```bash
./mvnw test -Dtest='BikeStationTest#returningRentedBikePublishesBikeReturned' 2>&1 | grep -E "Tests run:|FAIL|ERROR|expected|Expect|Exception|BUILD" | head -30
```

It has to fail **for the right reason**:
- **Right:** the assertion fails, an expected exception isn't thrown, or the step under test hits an existing "not implemented" stub. These show the behaviour is missing.
- **Wrong:** a compile error, an exception in the *given* part, or a typo in the setup. Fix the test or the compile-only scaffolding and run it again.

**If it passes straight away**, the behaviour already exists. Don't touch production code. Tell the user, keep the test as documentation, and ask for the next scenario. Never claim a red you didn't see.

## Step 4: Green - the smallest change

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

## Step 5: Run the whole suite

```bash
./mvnw test 2>&1 | grep -E "Tests run:|FAIL|ERROR|BUILD"
```

Every test has to pass, not just the new one. This is the check that the smallest change was really safe. Fakes and hard-coded values are the usual culprits here.

**If another test breaks**, the change was too small or went the wrong way. Revise the **production** change: move one step down the list in Step 4 and run everything again. Never delete, skip, `@Disabled` or edit the other test to get green.

**If the new scenario contradicts an existing test**, meaning both can't be true for any implementation (for example, one expects rejection at 12 hours and the other expects acceptance), stop. Show the user both scenarios side by side and ask which one the domain expert means. That's a domain question, not a coding one. Leave the new test in place and report the state as red.

## Step 6: Refactor, lightly

With everything green, look only at the code this cycle touched:
- duplication introduced in this cycle, including between the test and production code
- unclear names
- leftovers from the compile-only step

Apply a refactor only if it's small and clearly better, then run the full suite again. Anything bigger, or anything in code this cycle didn't touch, goes into the report as a suggestion. Don't generalise a fake here: replacing a hard-coded value is the next scenario's job, and doing it now means writing untested behaviour.

## Step 7: Report and ask for the next scenario

Keep the report short. Use this structure:

```
## Cycle: <scenario in one line>

**Test:** `<TestClass#method>` in `<path>`
**Red:** <what failed and the key line of the failure message>
**Green:** <the production change in one or two sentences, plus files touched>
**Suite:** <N tests run, 0 failures>  (single test + full suite both green)
**Refactor:** <what was done, or "none">

**Shortcuts in the code now:** <hard-coded values, ignored parameters, missing branches.
These are deliberate, so name each one.>

**Next scenario suggestion:** <one concrete GWT that would force the most obvious
shortcut above to become real code>
```

Then ask the user for their next scenario. Don't start another cycle on your own suggestion; wait for them to confirm or give their own. Don't commit unless they ask.
