Done. Your TutorHub board splits into **9 bounded contexts**, and the context map opens in Context Mapper.

**Files**
- Context map: `src/main/cml/TutorHub-Context-Map.cml`. Valid (0 errors): 9 contexts plus Stripe and the video provider, 20 relationships.
- Discovery report: `docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md`. It's the full trail (heuristics, your answers, reviewer verdicts, process walkthroughs, validator output) and the place to resume from.
- Canvases (drafts for your review; no plan or code yet): `docs/bounded-contexts/tutorhub/{tutortrustandsafety, studentaccounts, scheduling, lessondelivery, pricing, billing, payouts, reviews, complaints}/canvas.md`

**The contexts and the question each answers**
- **Tutor Trust & Safety:** may this tutor teach? It's the only place a tutor gets suspended.
- **Student Accounts:** may this student book?
- **Scheduling:** which lesson happens when, between whom? It owns the 24h rule, the 50% late fee and the 2-reschedule limit.
- **Lesson Delivery:** did the lesson happen, and who showed up?
- **Pricing:** what does a lesson cost, and what's our cut?
- **Billing:** what does the student owe for this lesson, and is it paid?
- **Payouts:** when and how much do we pay each tutor? It owns the tutor's balance.
- **Reviews:** how good is this tutor in students' eyes?
- **Complaints:** was something wrong, and what's the remedy?

Search and notifications aren't contexts. Stripe and the video provider are external.

**The map**
- Every internal relationship is Customer/Supplier: the upstream publishes a documented language and the downstream keeps an anti-corruption layer.
- Stripe is the only Conformist.
- The video provider sits behind an anti-corruption layer.
- There's no shared kernel.
- There are no two-way dependencies between any pair of contexts.
- One loop is kept on purpose, with the reasoning written in the CML: Pricing → Scheduling → Lesson Delivery → Pricing. It's your own feedback loop: lesson history sets the commission tier, and the tier is frozen into the next quote.

**What the reviews changed**
- **The 50% late fee:** Scheduling, Pricing and Billing all claimed it. Your answer gave it to Scheduling.
- **Unpaid lessons:** a reviewer asked whether they needed a debt-recovery context. They don't, because there's no collection process.
- **Quote reference:** my first fix for the loop was a guessed "quote reference" that hid it. A reviewer rejected that, and your answer brought back Pricing → Scheduling with the loop explained.
- **Tax forms:** these added a Trust & Safety → Payouts relationship for the tutor's country.

**Still open** (listed in the report and the canvases)
1. Is the commission added on top of rate × duration, or taken out of it? Your answers said "rate × duration + commission" and also "commission deducted". This affects the numbers in the Pricing and Billing rules.
2. Review moderation, and whether a student can review a tutor who didn't show up.
3. Complaints against students, and complaint investigation rules.
4. Tutor-side cancellations and reschedules.
5. A cap on refunds.
6. Which countries need tax forms.

Several rules are still prose rather than Given/When/Then and are marked that way in the canvases.

**Suggested board changes:** merge the two "Tutor Suspended" stickies, rename "Late Cancellation Fee Applied" to *Lesson Cancelled Late*, split "No-show" into student and tutor, and add the missing events (Lesson Marked Unpaid, Refund Decided, Complaint Upheld Against Tutor, …). The full list is in the report.

Next step: review the canvases. When one is ready, ask me to scaffold that context, and bounded-context-scaffolder will write its plan and stop for your review before any code.
