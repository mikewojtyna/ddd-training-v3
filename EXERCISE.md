# Exercise 2: Interview the expert

**Module:** M6 Domain exploration + business archetypes · **Time:** 45 min · **Form:** triads · **Start:** tag `ex02/start`

## Situation

VeloCity is a city bike rental service. Its product vision, `docs/domains/velocity/velocity-vision.md`, was written
by people who don't fully agree with each other. It has gaps and contradictions, like most real requirements.

Your triad models two parts of it: **Returns** and **Billing**. How does a ride end, and what does the rider pay?

- One of you is the **domain expert**. Read `handouts/expert-role-card.md` and the vision. You answer questions.
- Two of you are the **modellers**. You don't read the vision. You learn the domain only by asking the expert.

**Don't open `docs/bounded-contexts/`, `src/` or the git history.** They contain one possible set of answers, and the
point is to find your own questions.

## Your task

1. **Event Storming (15 min).** The modellers run a short Event Storming on Miro for Returns + Billing.
   - Start with orange domain events in the past tense ("Bike Returned to Station", "Charge Failed").
   - Order them on a timeline. Add commands, actors, policies and hot spots (pink) where you're unsure.
   - Ask the expert whenever you're stuck. The expert answers only from the vision (see the role card).
2. **Export the board (3 min).** Take a screenshot or export the sticky notes as CSV.
   See `handouts/miro-export-howto.md`.
3. **Ask AI for gaps (7 min).** Give the agent your board, not the vision. Use prompt card 2 in
   `handouts/contradiction-hunter-prompt.md`. You get a list of questions.
4. **Back to the expert (10 min).** Pick the **three** questions you think matter most for the model.
   Ask the expert. Put each answer on the board. Mark every answer the expert had to invent.
5. **Expert's turn (5 min).** The expert runs prompt card 1 on the vision itself and compares it with the decisions
   they had to make.

**Extension (if time is left):** run prompt card 3. The agent writes your board's vocabulary as a glossary.
Compare it with *Domain Vocabulary* at the end of the vision. Where do the meanings differ?

## Done when

- Your board covers a ride ending at a station and in a drop zone, the fees, and what happens when a charge fails.
- You have three questions from the agent, each with the expert's answer on the board.
- The expert has a short list of decisions they made where the vision was silent or contradicted itself.

## Debrief questions

- Which of the agent's questions found something your board really missed? Which ones were noise?
- Did the agent invent a rule that isn't in the vision, or smooth over a contradiction?
- Which expert decision would you most want a real stakeholder to confirm before anybody writes code?
- Is the unauthorized zone return fee a fee or a penalty? Does the answer change your model?

## Rules

- The agent widens the questions. The expert answers them. Never take the agent's answer as the business's answer.
- Don't paste real customer data into the agent. VeloCity is fictional; keep it that way.
