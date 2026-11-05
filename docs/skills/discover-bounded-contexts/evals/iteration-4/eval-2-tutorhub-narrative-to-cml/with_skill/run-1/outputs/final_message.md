Discovery is done. The map was approved, and I've written the canvases.

**Files**
- Report (rounds, critic log, open points): `docs/context-discovery/tutorhub/2026-11-05-platform/discovery.md`
- Context map: `src/main/cml/TutorHub-Context-Map.cml`. Context Mapper parses it with 0 errors and no rule warnings.
- Canvases, one per context: `docs/bounded-contexts/tutorhub/{trustandsafety,scheduling,pricing,billing,payouts,reviews,complaints}/canvas.md`. These are canvases only; there's no plan or code yet.

**Result:** 7 contexts and 13 relationships. 10 of the relationships are internal, and 3 go to Stripe or the video provider. There are no cycles.

**Critic rounds that changed something**
- Payouts and Billing (contexts): the tutor's earnings balance had two owners. Your answer gave it to Payouts, and Billing now only reports what each lesson earned.
- LessonDelivery (context): it was separate only because the video provider's webhooks keep changing, which is a technical reason. Your answers folded it into Scheduling, with a translation layer in front of the video provider.
- Weekly payout (map): there was a gap in who supplies the tutor's country for tax forms. I added Trust & Safety → Payouts.
- Complaint process (map): my earlier question about the upheld-complaint report was an either/or. I asked again neutrally, and Trust & Safety stays upstream because it defines the categories.
- After your round-6 answer, review eligibility moved to Community. Scheduling now reports every lesson outcome and Reviews decides which ones get a review.

**Still open**
- Several answers you gave on the spot are marked "to re-confirm" with the teams. They're listed in the report.
- Reinstatement rules and the "Tutor Reinstated" event.
- Which countries need tax forms, and whether a tutor's balance can go negative.
- Renewing card holds for lessons booked far ahead.
- What a tutor's no-show dispute can lead to.
- The strategic classification of each context.
- Several rules in the canvases still need concrete Given/When/Then examples before scaffolding.
