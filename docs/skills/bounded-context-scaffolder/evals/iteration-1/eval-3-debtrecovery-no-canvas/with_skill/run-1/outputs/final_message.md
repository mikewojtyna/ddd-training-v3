I haven't generated any code for DebtRecovery yet. This skill builds a context from a Bounded Context Canvas, and your request has a name and one sentence of purpose but no domain terms or business rules. If I scaffolded now, the module would contain my guesses about your domain, not your knowledge of it.

Instead, I made a canvas draft for you to fill in:

`docs/bounded-contexts/velocity/debtrecovery/canvas.md`

It's partly filled in from your request, `velocity/velocity-vision.md` and `src/main/cml/VeloCity-*.cml`:
- **Name:** DebtRecovery. It matches the CML `BoundedContext`, and the package will be `velocity/debtrecovery`.
- **Purpose:** your sentence, as written.
- **Ubiquitous language:** Outstanding Debt, Account Block and Debt Recovery Process, with the vision's definitions marked "(confirm)". Paused/Resumed, Exhausted, Payment Plan and Reminder are listed without definitions.
- **Possible messages:** Rental Fee Unpaid and Outstanding Debt Cleared from Settlement, block and unblock to Accounts, and collect to Settlement. All are marked TODO/confirm.
- **Rule skeletons:** block, unblock, partial payment, a second debt while already blocked, and pause/resume/exhausted. They're empty Given/When/Then blocks for you to fill with concrete values.

**One thing to decide first.** Your purpose and the reference context map disagree. You describe DebtRecovery as the context that blocks and unblocks accounts. In `velocity/velocity-context-map.md`, Accounts owns blocks. Settlement drives both the debt block and the unblock ("Rental Fee Unpaid" and "Outstanding Debt Cleared"), and DebtRecovery handles the pursuit itself: reminders, payment plans, pause and resume. The map does accept "Debt Recovery -> Accounts for unblocking" as an alternative, with the debrief question "whose fact is 'the debt is paid'?". I'll build whatever the canvas says. I just want this to be a deliberate choice, not an accident.

To go ahead, please:
1. Choose the purpose: (a) blocking/unblocking as you wrote it, (b) pursuing the debt as in the reference map, or (c) both. Then reword it as a business benefit.
2. Confirm or correct the term definitions, and define the ones that are still empty.
3. Write the business rules as Given/When/Then with concrete values. For example: "given rider R1 is blocked with 12.50 PLN outstanding, when R1 pays 5.00 PLN, then R1 stays blocked with 7.50 PLN outstanding". These are the only rules that become aggregate logic and tests.
4. Fill in the inbound and outbound tables. Most important: who tells this context the debt is paid, and who actually blocks the account?
5. Optionally, add the strategic classification and domain roles. Enforcer or Execution Model from `docs/model-traits-worksheet.md` would fit, depending on your answer to step 1.

When the canvas is ready, tell me and I'll write the plan (`docs/bounded-contexts/velocity/debtrecovery/plan.md`) and then scaffold the module.
