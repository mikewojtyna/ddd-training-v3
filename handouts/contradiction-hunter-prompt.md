# Prompt cards: the contradiction hunter

Copy a card into your agent (Claude Code, Copilot Chat or similar). Replace the parts in `<angle brackets>`.

**What to do with the answers:** the agent widens the questions, the expert answers them. Treat every answer from the
agent as a question to ask, never as a business rule.

---

## Card 1: contradictions and gaps in the vision (for the expert)

```text
Read docs/domains/velocity/velocity-vision.md.
List the contradictions and gaps in this document. Focus on the sections Billing, Settling the Ride and Returns,
then the rest.

For each item:
- type: contradiction, gap, unfinished sentence or undefined term;
- the section(s);
- the exact sentence(s), quoted word for word. For a contradiction, quote both sentences;
- one question a modeller should ask the domain expert about it.

Rules:
- Quote only text that is really in the document. Don't paraphrase inside quotes.
- Don't resolve anything and don't suggest answers. I only want the questions.
- Don't treat the Domain Vocabulary as more authoritative than the sections. Check it against them.
```

## Card 2: gaps in our board (for the modellers)

```text
This is our Event Storming board for the Returns and Billing part of a city bike rental service.
<attach: path to the screenshot, e.g. ./board.png, or the CSV file with the sticky notes>

Sticky colours: orange = domain event, blue = command, yellow = actor, lilac = policy, pink = hot spot.

Find the gaps in our board:
- events that must happen but are missing (for example a failure, a timeout, a retry);
- commands without an actor, and events nothing reacts to;
- places where the order of events is unclear;
- terms we use with two meanings, or two terms for one thing;
- the unhappy paths: what if a step fails or never happens?

Answer with at most 10 questions for our domain expert, most important first.
Each question must point at a sticky or a gap between stickies.
Don't answer the questions yourself and don't invent business rules.
```

## Card 3: glossary (extension)

```text
From our board <attach the same screenshot or CSV>, write the vocabulary we used as a glossary:
a table with Term and Meaning, one line per term, in the words our board uses.
Include only terms that appear on the board. If a term's meaning isn't clear from the board, write "unclear"
instead of guessing.
```

Then compare it with *Domain Vocabulary* at the end of `docs/domains/velocity/velocity-vision.md`:

- terms on your board that the vision doesn't have;
- terms in the vision your board never needed;
- the same word with a different meaning, for example *Settlement*, *Total Fee* or *Penalty*.
