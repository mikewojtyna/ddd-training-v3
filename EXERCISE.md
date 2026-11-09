# Exercise 10: Debate: who writes the tests?

**Module:** M5 Testing strategies with AI · **Time:** 30 min · **Form:** structured debate, two camps · **Start:** tag `ex10/start`

## Situation

Your survey answers split into two camps:

- **Camp A:** "AI must never write tests of business logic, or tests for its own code."
- **Camp B:** "A different agent should write the tests."

Both camps want the same thing: tests you can trust when an agent wrote the code. They disagree on who may type
them. This repo has tests written by agents, some good and one deliberately bad. Argue with them, not with
opinions.

## Your task

1. The trainer assigns you to a camp. You may land in the camp you disagree with. That's on purpose.
2. Read `handouts/debate-brief.md`: the format, the rules and the code pointers.
3. **Prep (8 min):** in your camp, pick your two strongest arguments. Each one must point at a file in this repo.
   Also prepare one question for the other camp.
4. Debate in the format of the brief. The trainer keeps time.
5. **Together (last minutes):** write one sentence the whole room can sign: *"A test of business logic can be
   trusted when ..."*.

## Done when

- Each camp made at least two arguments that cite a file (a test, a spec, a skill).
- The room has its one sentence on the whiteboard.
- Everyone can say which part of the sentence their own team doesn't do today.

## Debrief questions

- Did anyone change camp? What made them move?
- In your team today, who picks the scenarios a test checks? Who reviews them?
- Which arguments were really about **who types** the test, and which about **who decides what it checks**?
- What would you need to see in a PR to trust an agent-written test?

## Rules

- Argue with the code in front of you. Every claim cites a file or a line.
- No "it depends" without a concrete example of what it depends on.
- Attack arguments, not people. The camps were assigned, not chosen.
