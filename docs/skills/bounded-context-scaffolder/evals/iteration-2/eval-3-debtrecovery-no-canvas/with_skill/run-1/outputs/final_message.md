I haven't generated any code yet. This skill builds a bounded context from a canvas, and all I have is the name and one sentence. If I scaffolded from that, the module would be built on my guesses about the debt rules, not on your domain knowledge.

I've started a canvas for you here:
`docs/bounded-contexts/velocity/debtrecovery/canvas.md`

It's pre-filled where I could:
- **Name:** `DebtRecovery`, which matches the CML `BoundedContext`. The package will be `velocity.debtrecovery`.
- **Purpose:** your sentence.
- **Ubiquitous language:** candidate terms from the vision glossary (Outstanding Debt, Account Block, Debt Recovery Process). Each one is marked to confirm.
- **Messages:** candidate inbound and outbound messages (Rental Fee Unpaid, Outstanding Debt Cleared), also marked to confirm.

**One thing to decide before you fill it in:** your description doesn't match the reference context map.
- **Your description:** DebtRecovery blocks and unblocks accounts.
- **The reference map** (`velocity/velocity-context-map.md` and the CML): Accounts owns account blocks and unblocks. Settlement drives the debt block and unblock through *Rental Fee Unpaid* and *Outstanding Debt Cleared*. DebtRecovery is the active pursuit of the debt: reminders, payment plans, pause and resume.
- **Accepted Alternatives:** the map accepts "Debt Recovery → Accounts for unblocking", but not DebtRecovery owning the block itself.

I'll build whichever version the canvas describes and record the difference in the plan. The canvas lists three options in its Purpose section: (a) DebtRecovery owns blocking, (b) it pursues the debt and Accounts blocks, or (c) both.

**To make the canvas usable, I need at least:**
1. **Purpose:** the business benefit, written once you've settled the question above.
2. **Ubiquitous language:** confirm or rewrite the candidate definitions. They become class names.
3. **Business decisions as strict Given/When/Then with concrete values**, for example: "given rider R1 with 45 PLN of outstanding debt, when Settlement reports the debt cleared, then R1's account is unblocked". Only rules written this way become aggregates and tests. Prose rules are listed as awaiting GWT. I've added some prompts to the canvas: Is there a debt threshold? Does a partial payment unblock the rider? What if they're also blocked for an invalid card?

Optional but useful: strategic classification, domain roles (traits from `docs/model-traits-worksheet.md`), assumptions, and open questions.

Once you've filled it in, ask me again, for example "canvas is ready, scaffold DebtRecovery". I'll then write the plan to `docs/bounded-contexts/velocity/debtrecovery/plan.md` for you to review before I generate any code.
