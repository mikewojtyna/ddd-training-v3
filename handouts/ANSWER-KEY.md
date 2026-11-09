# Exercise 3: answer key (trainer)

The point of the sort isn't the "right" column. It's the **deciding question** behind each card. Most cards have one best column and one defensible alternative under a stated condition. Accept the alternative when the pair can state the condition; push back when they can't.

## Definitions this key assumes

| Term | Definition used here |
|---|---|
| Archetype | A recurring business shape with known roles and rules, seen across companies and industries. It's bigger than one class: a small set of concepts plus the rules between them. |
| Entity | One thing with identity and lifecycle inside one model (one bounded context). |
| Design pattern | A technical solution structure (GoF, DDD tactical patterns, architectural patterns). |
| Process | A flow of activities over time: trigger, steps, outcome. |

The domain archetypes the training uses:

| Archetype | Core of the shape |
|---|---|
| **Party** | People and organisations, and the **roles** they play (customer, supplier, plant, employee), plus relationships between parties. One party, many roles. |
| **Product** | What is defined, built or offered: product type vs. product instance, composition (structure, BOM), features and options with rules about valid combinations (configurable products, variants). |
| **Inventory** | What is available, how much and where: instances or quantities, locations, reservations. |
| **Pricing** | How a price is found: price lists, contract prices, conditions, validity periods, surcharges and discounts. |
| **Template & Execution** | A reusable, **versioned** definition, and many executions. Each execution runs one version with concrete parameters, produces results, and keeps the version it ran. |
| **Group Decision Making** | A proposal decided by a group under rules: members, votes, quorum, majority or veto, deadline, escalation, outcome. |

Party, Product, Inventory and Pricing follow Arlow & Neustadt's archetype patterns (*Enterprise Patterns and MDA*). Template & Execution and Group Decision Making are the names from the trainer's earlier programme. The definitions above are short working definitions.

Don't mix this level with **business-model archetypes** (Neal Cabage: Product, Service, Trade, Subscription, Brokerage, Marketplace, Ecosystem), used in `docs/archetypes/*-analysis.md`. Cabage's "Product" is a way of earning money; the Product domain archetype is a model shape. Same word, different level. If someone brings up Cabage, that's a good moment to say it.

## Per card

| # | Card | Best column | Why | Also defensible, when |
|---|---|---|---|---|
| 1 | User with first and last name | **Entity** | An account with an identity, in an identity/access model. First and last name are attributes, not a shape with rules. | *Party* only if the card means the person behind the account, with roles (engineer, customer contact). Then "user" is the wrong name: the archetype is the party, "user" is one technical role. |
| 2 | Factory | **Depends on who says it.** Developer: **design pattern**. Business: **entity** (a plant) | GoF/DDD Factory creates complex objects or aggregates and hides how. In a plant manager's mouth "factory" is a plant: a party (an organisation or site) that plays roles like *manufacturing plant* or *test lab*. | Placing "Factory" as *archetype* is acceptable only if they say "a factory is a Party in a plant role", which shows they see the shape behind the word. The lesson is ubiquitous language: the same word means two things in one meeting. |
| 3 | Product structure / BOM | **Archetype** (Product) | Composition of a product from assemblies and parts, type vs. instance, versions. The same shape appears in cars, furniture, software packages and insurance products. | *Entity* inside one model: "the BOM of unit X, revision C" has identity. Fine, but then the archetype is the whole Product shape it belongs to. |
| 4 | Variant configuration | **Archetype** (Product) | Options plus rules about valid combinations ("high-power motor needs the large housing") is the configurable-product part of Product. | *Process* if they mean the act of configuring with a customer (a configurator session). *Entity* for one saved configuration with an ID. |
| 5 | Simulation template → simulation run | **Archetype** (Template & Execution) | A versioned definition executed many times with parameters; each run keeps the version it used and has its own results. Same shape: recipes and batches, test plans and test runs, CI pipeline definitions and builds. | *Entity*: a SimulationRun is an entity in that model. *Process*: running a simulation is a process, but the arrow on the card (template → run) is the shape. Not the GoF *Template Method* pattern; same word, unrelated. |
| 6 | Approval of an engineering change | **Process** | A flow with a trigger (a change request), steps (review, vote) and an outcome (approved, rejected, escalated). | *Archetype* (Group Decision Making) when a **group** decides by rules: members, quorum, majority, veto, escalation. A single manager clicking "approve" is just a process step, not GDM. |
| 7 | Price list | **Entity** | One document with identity, validity and entries. It's one element of the Pricing archetype. | *Archetype* if they mean the whole Pricing shape (lists, contract prices, conditions, surcharges). Then ask them to name the archetype, not one of its parts. |
| 8 | Party (customer, supplier, plant) | **Archetype** (Party) | The card names the shape: one party, several roles. A company can be customer and supplier at once. | *Entity* only inside one model: a `Customer` in Sales is an entity. That's the point of the archetype: each context models the roles it needs, not one shared "Party table" for everyone. |
| 9 | Repository | **Design pattern** | DDD repository: collection-like access to aggregates, hides persistence. | *Entity* (or even a system) if they mean a business repository: a PDM vault, a knowledge repository, a git repo. In CAD/PLM "repository" often means the vault. Ask: "Would a design engineer say it?" |
| 10 | Knowledge article | **Entity** | An article with identity, versions and a lifecycle (draft, reviewed, published). | *Process* for authoring and reviewing it. Not an archetype on its own: "knowledge base" is a product category, not a recurring business shape with its own rules. If articles are reviewed by a panel with voting rules, the review is Group Decision Making. |
| 11 | State machine | **Design pattern** | A technique to implement a lifecycle (states, transitions, guards). | *Process* only if they mean the business lifecycle itself; then call it the lifecycle or the process, and keep "state machine" for the implementation. |
| 12 | Engineering change request | **Entity** | It has a number, an author, affected items and a status. In Group Decision Making it's the **proposal** being decided. | *Process* when people say "the ECR process" (ECR → ECO → ECN in PLM speak). *Archetype* is not defensible on its own: the archetype is the group decision around it, not the request. |

**Expected spread:** archetype 3, 4, 5, 8; entity 1, 7, 10, 12; design pattern 9, 11; process 6; Factory split.

### Cards 6 and 12 together

Both relate to **Group Decision Making**, and neither is the archetype on its own. The request (12) is the proposal: an entity. The approval (6) is the decision process. The archetype is the rule set around them: who votes, quorum, majority, veto, escalation, what happens to a rejected request. Ask: "If you only had cards 6 and 12, where would the quorum rule live?" That's the archetype.

## Typical confusions

- **Archetype = entity.** "Party is an entity, I have a Party table." The archetype is the shape (party + roles + relationships), and it says the opposite of one shared table: each context models the roles it needs.
- **Archetype = design pattern.** "Template & Execution is the Template Method pattern" or "Factory is an archetype". Patterns solve technical problems; archetypes describe business rules you'd find even without software.
- **Archetype = process.** "Approval is an archetype." Only the group-decision rules make it an archetype; the flow itself is a process.
- **Part for the whole.** Price list for Pricing, BOM for Product, ECR for Group Decision Making. A card can be one element of an archetype. Ask: "What other concepts and rules come with it?"
- **Same word, two meanings.** Factory, Repository, Template, Product (domain archetype vs. Cabage's business model). Ask who is speaking.
- **"User" as a domain concept.** In most domains nobody is a "user"; they are an engineer, a customer contact, a plant manager. "User" is a technical role.

## What the AI usually does

- It sorts confidently and gets most cards "right", which can stop the discussion. Insist on the argument step.
- It often puts Factory only in *design pattern* and Repository only in *design pattern*, missing the plant and the vault. That's a good moment: the AI doesn't know who is speaking unless you tell it.
- It tends to call cards 6 and 12 "Group Decision Making" without separating proposal, decision process and rules.
- It may mention Cabage's business-model archetypes or the GoF Template Method. Use it to show the level confusion.

## Debrief tips

- Start with Factory: ask for a show of hands per column. The split itself is the lesson.
- Then cards 6 and 12 together (see above): it leads straight into Exercise 4, where the engineering change board is in the narrative.
- Ask for one card where a pair kept their answer against the AI. Praise the reasoning, not the column.
- Close with the four deciding questions from the sorting sheet; they're what people should remember.
