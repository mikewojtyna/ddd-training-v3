# Exercise 3: sorting sheet

Put each card in exactly one column. If a card fits two columns, put it in the one you'd defend first and write the second one next to it with a short "only if ...".

| Archetype | Entity | Design pattern | Process |
|---|---|---|---|
| A recurring business shape with known roles and rules. It turns up in many companies and industries. You recognise it; you don't invent it. | One thing with its own identity and lifecycle **inside one model**. Two of them can look the same and still be different things. | A technical solution structure. It says how to build software, not what the business does. | A flow of activities over time, with a trigger, steps and an outcome. |
| *Ask: "Have I seen these rules in another business?"* | *Ask: "Do we need to tell this one apart from another that looks the same?"* | *Ask: "Would a business person ever say this word?"* | *Ask: "Does it have a start, steps and an end?"* |
| | | | |
| | | | |
| | | | |
| | | | |
| | | | |
| | | | |

**Archetypes used in this training:** Party, Product, Inventory, Pricing, Template & Execution, Group Decision Making. You don't need their details for this sort; the question is only whether a card is a *whole shape* or something smaller or different.

Note: an entity can be one element of an archetype, and a process can run inside one. That doesn't make it the archetype.

## After your own sort: ask the AI

Only when your sort is finished. Paste this prompt and the 12 cards:

```text
Sort these 12 cards into exactly one of four columns: archetype, entity,
design pattern, process. Use these definitions:
- archetype: a recurring business shape with known roles and rules, seen in many
  businesses (for example Party, Product, Inventory, Pricing, Template & Execution,
  Group Decision Making);
- entity: one thing with its own identity and lifecycle inside one model;
- design pattern: a technical solution structure;
- process: a flow of activities with a trigger, steps and an outcome.
The domain is CAD/PLM, engineering simulations and knowledge bases.
For each card give: the column, one sentence why, and a second column it could
belong to and under which condition. Mark every card whose answer depends on
who is speaking (business or developer).

<paste the 12 cards here>
```

Then compare card by card. For **each** difference between your sort and the AI's:

1. Each of you argues for one side for one minute.
2. Decide who wins and write the deciding question on the card.
3. If neither wins, write the condition: "archetype if ..., entity if ...".
