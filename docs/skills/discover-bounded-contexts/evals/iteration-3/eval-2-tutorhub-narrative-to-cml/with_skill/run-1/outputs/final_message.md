Discovery is done, and the context map is approved.

**Files**
- Report: `docs/context-discovery/tutorhub/2026-11-05-event-storming/discovery.md` (Q&A, heuristic trail, critic log, cycle analysis, validator output)
- Context map: `src/main/cml/TutorHub-Context-Map.cml` (opens in Context Mapper; validator: 9 contexts including Stripe and VideoProvider, 14 relationships, 0 errors)
- Canvases: `docs/bounded-contexts/tutorhub/{trustandsafety,scheduling,pricing,billing,payouts,reviews,complaints}/canvas.md`

**Result:** 7 bounded contexts and 14 relationships. 10 are internal Customer/Supplier or OHS links with an ACL downstream. Stripe is followed as is (Conformist), and the video provider sits behind an ACL. There's no shared kernel.

**Critic rounds that changed something**
- Tutor Listing: "is this slot bookable?" had two owners. Your answer (the booking wins) moved the slots into Scheduling, and Tutor Listing was dissolved.
- Billing: the tutor's balance had no single owner. Payouts now holds it, and Billing only publishes per-lesson earnings.
- Map v1: the Pricing → Billing link rested on an assumption I hadn't asked about. You confirmed the charge equals the booked quote, so the link was dropped, and several loops went with it.

**Still open**
- Four dependency loops remain, among them Scheduling ↔ Billing and Billing ↔ Complaints. Every flip was tried and contradicts one of your answers. The report lists them all.
- No business rule is a strict Given/When/Then yet. Each canvas marks them, ready for your concrete examples.
- Strategic classification (core, supporting or generic) wasn't discussed.
- Smaller questions are in the report: pre-authorisation timing, negative tutor balances, review moderation, refund caps.

Review the canvases, then run bounded-context-scaffolder per context to get a plan.
