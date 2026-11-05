Discovery is done, and the map and canvases are ready for you to review.

**Files**
- Report (heuristic trail, Q&A, critic log, validator output): `docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md`
- Context map: `src/main/cml/TutorHub-Context-Map.cml`. It opens in Context Mapper, and the validator reports 0 errors.
- Canvases: `docs/bounded-contexts/tutorhub/<context>/canvas.md` for tutorvetting, tutorcatalog, scheduling, pricing, billing, payouts, reviews and complaints. They are canvases only, with no plan or code.

**Size:** 8 contexts plus Stripe and the video provider, connected by 19 relationships. No cycles, no shared kernel. Billing and Payouts follow Stripe as-is, because you called it an industry standard.

**Critic rounds that changed something**
- Lesson Delivery merged into Scheduling. The webhook churn is a translation-layer concern.
- Free slots stayed in Scheduling, so no slot is ever double-booked. Tutor Profiles became Tutor Catalog, with search as its view.
- The map critics split Scheduling -> Billing into lesson outcomes and payment-guarantee commands. They also had me confirm the "open calendar" call on approval.
- With your answers, three loops were broken: Pricing/Scheduling, Trust & Safety/Scheduling/Complaints, and Billing/Scheduling.

**Still open**
- Is commission added on top of the rate or deducted from it? How are refunds and the late fee split?
- Is a qualification expiry per subject? Do complaints have deadlines? What happens when a refund fails, or a tutor files a complaint?
- Where does Payouts get a tutor's country and Stripe Connect account?
- Several canvas rules are marked "needs GWT".

Next: review the canvases, then run bounded-context-scaffolder on each context to get its plan.
