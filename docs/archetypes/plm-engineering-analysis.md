# Business archetypes of an engineering-analysis and PLM business

Answer key of Exercise 4 (Archetypes Funnel on PLM). Input: `handouts/plm-narrative.md` on branch `exercise/04-archetypes-funnel-plm` (Corvane Drive Systems, a fictional company).

Source of archetype definitions: Party, Product, Inventory and Pricing follow Arlow & Neustadt's archetype patterns (*Enterprise Patterns and MDA*); Template & Execution and Group Decision Making are named as in the trainer's *Context Mapping with Archetypes* programme. The one-line definitions below are working definitions for this training. The business-model level uses Neal Cabage, *Business Model Archetypes*, https://nealcabage.com/business-model-archetypes/

## Answer

Corvane has four domain archetypes, one per main business area, plus a fifth in the extension:

| Business area | Archetype |
|---|---|
| Units: base designs, options, BOMs | **Product** |
| Simulations: templates and runs | **Template & Execution** |
| Changes: the engineering change board | **Group Decision Making** |
| Customers, suppliers, plants, contacts | **Party** |
| Selling analysis (extension) | **Pricing** |

**Inventory is not there:** units are built to order and nothing is stocked.

**Assumed scope:** the narrative as written. The compute cluster, the vault and the knowledge base are supporting tools, not business areas with their own rules.

**The other level, for the record:** on Cabage's business-model level, Corvane combines **Product** (it develops and sells units) with **Service** (simulation studies paid per job). It's not an Ecosystem: there's no partner or platform layer. Don't confuse Cabage's *Product* business model with the *Product* domain archetype above. The word is the same, but one is about how money is earned and the other is a model shape.

## How I tested each archetype

Each archetype is a set of concepts plus the rules between them. For each archetype I asked:

1. Does the narrative contain the archetype's **concepts** (not only its name)?
2. Does it contain the archetype's typical **rules**, stated by the business ("must", "never", "only", "needs")?
3. Which rule in that area does the archetype **not** explain? That's where Corvane is specific, and where generic models stop helping.

A word alone is weak evidence. "Template" appears in the narrative because the business uses it, but the evidence for Template & Execution is the rule that runs keep their version.

## Analysis per archetype

| # | Archetype | Definition | Analysis (quotes from the narrative) | Result |
|---|---|---|---|---|
| 1 | **Product** | Product type vs. instance, composition (structure, BOM), features and options with rules about valid combinations | Type vs. instance and options: *"We have about forty base designs, and every OEM order picks options"*. Configuration rules: *"Not every combination works. A high-power motor needs the large housing"*. Composition: *"Every unit is a structure of assemblies and parts, down to the screws: the bill of materials."* Possible vs. actual instances: *"thousands of possible BOMs, but we only ever build a few hundred"*. **Not explained:** the boundary between a configuration and new design (*"it's no longer a configuration. Engineering has to design it"*), and the two BOMs (see judgment calls). | **YES** |
| 2 | **Template & Execution** | A reusable, versioned definition; many executions, each running one version with concrete parameters, producing results and keeping the version it ran | Definition: *"A template fixes the mesh rules, the load cases, the solver settings and the acceptance criteria."* Execution with parameters: *"An engineer starts a simulation run from a template and fills in the parameters for a concrete unit"*. Versioning: *"we released version 4 of the template"*, *"which version did that run from two years ago use?"*, *"we must never upgrade an old run silently"*. Executions are immutable: *"it's a new run with its own results, not a correction of the old one."* **Not explained:** the four-eyes review of results (*"a second engineer reviews them and either signs them off or sends them back"*) is Corvane's own step on top of the execution. | **YES** |
| 3 | **Group Decision Making** | A proposal decided by a group under rules: members, votes, quorum, majority or veto, deadline, escalation, outcome | Proposal: *"Anyone can raise an engineering change request"*. Members: *"five voting members"*. Quorum and majority: *"at least four members present and a simple majority"*. Veto: *"Quality can veto anything that touches safety-relevant parts."* Escalation and deadline: *"If the vote is tied, or a request stays undecided for three meetings, it goes up to the chief engineer"*. Resubmission: *"A rejected request can come back only with new information."* **Not explained:** the phone approval of urgent changes by two members with later confirmation, and what happens when the board doesn't confirm (*"we have built units with a change that was never approved"*). | **YES** |
| 4 | **Party** | People and organisations, the roles they play, and relationships between them; one party, many roles | One company, several roles: *"the same company often shows up in more than one way"*; *"Tillgard is our biggest customer for units, but they also supply us with cast housings, and they buy simulation studies"*. Internal party in a supplier role: *"runs the vibration test lab, which our other departments order from as if it were an external supplier"*. Person roles: *"a technical contact who may raise change requests and a purchasing contact who signs contracts. Sometimes that's the same person."* The pain without it: *"Tillgard exists in ours six times, with three different addresses."* **Not explained:** the plant manager's seat on the change board depends on which plant is affected; that's a rule of the board, using a Party role. | **YES** |
| 5 | **Pricing** (extension) | How a price is found: price lists, contract prices, conditions, validity, surcharges, discounts | List prices: *"a price list per template and complexity"*. Price drivers: *"a fatigue study is priced per load case"*. Contract prices: *"frame contracts with their own prices and a yearly volume discount"*. Surcharge: *"Rush jobs delivered within 48 hours cost 40% more."* **Not explained:** free repeats *"because of our own mistake"*. That rule ties Pricing to template versions (the old flange template) and needs a decision: *"Who decides what counts as our mistake is a long story."* | **YES** |
| 6 | **Inventory** | What is available, how much and where; instances or quantities, locations, reservations | *"We don't keep units in stock; every unit is built to order."* Nothing in the narrative is counted, located or reserved. Compute-cluster capacity could become Inventory-like if runs had to reserve slots, but the narrative doesn't say so. | **NO** |

## The judgment calls

- **One BOM or two?** *"Engineering says it's the same BOM with extras. Brno says it's a different BOM."* The Product archetype doesn't settle it, but it gives the right question: is the plant's BOM a different view of the same product structure, or a different structure for a different purpose (making vs. designing)? Most PLM practice says two models (engineering BOM and manufacturing BOM) linked to each other. That's also a likely **bounded context** boundary: same word, two models. Good bridge to M3.
- **New run or corrected run?** *"some engineers want to fix the old run and keep its number"*. Template & Execution says executions are immutable and keep their version. The customer report referencing the old number is a reporting problem (the report could refer to a study with several runs), not a reason to mutate runs. Accept pairs that spot the tension; push back on pairs that model run correction as normal.
- **Is the phone approval part of the archetype?** No. Group Decision Making gives the normal rules (quorum, majority, veto, escalation). The urgent path is a Corvane-specific exception, and its failure (*"a change that was never approved"*) is a real business risk. Exceptions like this are usually where the interesting modelling happens.
- **Configuration vs. new design.** A request outside the configuration rules leaves the Product archetype's configurable part and enters Group Decision Making (*"that goes through the change board"*). The handover between two archetypes is a relationship, not a reason to merge them.
- **Party or Product for the plant?** A plant is a **Party** (an organisation or site playing roles: manufacturing plant, test lab). It's not part of the product, even though the plant's BOM is. Pairs who come from Exercise 3 sometimes still call it a factory pattern; that's the Factory card again.
- **Template & Execution vs. Product.** Both have "definition and instances". The difference: a product instance is built and delivered; an execution is run once, produces results and is kept as evidence. A unit built from a configuration is not an execution of a template.

### Not archetypes

- **The vault** (*"a big file store with check-in and check-out"*): technical file storage. Check-in/check-out is a locking mechanism, not a business shape. The business rules about what may change live in the change board, not in the vault.
- **The knowledge base** (*"short articles about failures and how we fixed them"*): a supporting tool. Its articles are entities; nothing in the narrative gives them rules of their own. *"Nobody agrees who owns them"* is an ownership question for the context map, not an archetype.
- **The compute cluster:** infrastructure that executes runs. The execution in Template & Execution is the business record of a run, not the job on the cluster.
- **The configurator and the PLM system:** tools that implement parts of Product. Naming the tool isn't naming the archetype.
- **Engineering change request** on its own: an entity, the proposal inside Group Decision Making (see Exercise 3, card 12).
- **Review / sign-off of results:** a process step attached to an execution.

### Terminology assumed in this key

The repo's earlier analyses (`docs/archetypes/*-analysis.md`) use only Cabage's business-model archetypes. This key adds domain archetypes with these working definitions: Party, Product, Inventory, Pricing as in Arlow & Neustadt; Template & Execution as a versioned definition with immutable executions; Group Decision Making as a rule-governed group decision on a proposal. The Funnel questions in `handouts/funnel-questions-prompt.md` are this training's version of the Archetypes Funnel: business → areas → recurring shapes → evidence → edge → non-archetypes.
