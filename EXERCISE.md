# Exercise 1: Spot the vibe

**Module:** M1+2 Why vibe coding doesn't work · **Time:** 20 min · **Form:** pairs · **Start:** tag `ex01/start` in
the repository [`vibe-code-velocity`](https://github.com/mikewojtyna/vibe-code-velocity)

## Situation

A developer generated an empty Spring Boot project, ran `/init` and typed one prompt into a fresh AI session, with no
DDD guidance, skills or existing code:

> Create city bike rental system

They committed what came back. The build is green.

This exercise lives in its own repository, so that the AI's output isn't influenced by this repo's skills and
`CLAUDE.md`, and so you can read it without DDD scaffolding around it.

## Getting started

```bash
git clone https://github.com/mikewojtyna/vibe-code-velocity.git
cd vibe-code-velocity
git switch -c my-ex01 ex01/start
cat EXERCISE.md          # the task, the done-when and the debrief questions
./mvnw test              # green
```

The answer key is the tag `ex01/solution` in that repository (`git diff ex01/start ex01/solution`). Look at it only in
the debrief.
