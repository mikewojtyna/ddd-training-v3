I've split TutorHub into 10 bounded contexts plus 2 external systems and written the context map to `src/main/cml/TutorHub-Context-Map.cml`. Context Mapper loads it with no errors (12 contexts, 22 relationships), and it renders to PNG with the repo's generator setup.

**Bounded contexts.** Each one matches a team you named, and each team uses "lesson" in its own sense.

| Context | Team | Owns |
|---|---|---|
| TutorVetting | Trust & Safety | Application, ID documents, qualification checks, approval, qualification expiry, and the single decision on whether a tutor may teach (suspend or reinstate) |
| TutorCatalog | Marketplace | Profile pages and search. It has no rules of its own and only shows what others publish. |
| StudentAccounts | n/a | Student registration. It only knows that a card is on file. |
| Scheduling | Scheduling | Free slots, requests, bookings, reminders, up to 2 reschedules, and the 24h cancellation rule. Here a lesson is an appointment. |
| LessonDelivery | Video integration | Video room, started and ended, no-show detection (not joined within 10 minutes). Its ACL protects the rest of the system from the provider's webhook model, which keeps changing. |
| Pricing | Finance | Hourly rates and which rate applies at booking time. One calculation covers the quote, the 50% late cancellation fee and the final charge, plus the 15%/10% commission tier. |
| Billing | Billing | Card reference, pre-authorization, the charge within 1 hour, retries, unpaid, the commission split, and carrying out refunds. Here a lesson is a chargeable item. |
| Payouts | Payouts | Earnings balance and the Monday run via Stripe Connect: 20 EUR minimum, retry the next Monday, tax forms, refunds deducted from future earnings. |
| Reviews | Community | Reviews within 14 days, and the rating as the average of the last 50 reviews |
| Complaints | Complaints | Complaints and investigation. It publishes "complaint upheld against tutor X" and "refund lesson L, amount A", and never suspends anyone or moves money itself. |

Stripe and VideoProvider are external. Each one sits behind an ACL.

**Key decisions.**
- **Suspensions:** Today both Complaints and Trust & Safety suspend tutors. Now only TutorVetting suspends. Complaints just reports an upheld complaint, so a certificate renewal can no longer undo a complaint suspension.
- **Hourly rate:** It lives in Pricing rather than on the profile, because "the rate valid at booking time" is a pricing rule. The catalog only displays it. You confirmed this.
- **Cycles:** I removed every upstream/downstream cycle.
  - Scheduling uses LessonDelivery's room service and Billing's payment service (pre-authorize, release, late fee). It doesn't feed events back to them.
  - Complaints publishes its decisions, and Billing and TutorVetting follow them.
- **No shared kernel:** The teams said they don't want a shared database or shared library again, so there is no Shared Kernel or Partnership.

**Points the expert improvised (worth checking with the real team):**
- Lessons already booked aren't cancelled when a tutor is suspended.
- Refunds are deducted from future earnings.
- Ratings don't drive suspensions.
- The card reference lives in Billing.
- If a pre-authorization fails, the booking doesn't go through.
- Only complaints trigger refunds.
- What "lesson" means to each team.

To open it, load the file in the Context Mapper plugin for VS Code or Eclipse. You can also copy `VelocityContextMap` and point `INPUT_FILE` at the new file to write a PNG to `target/`.

The file is at `.claude/skills/discover-bounded-contexts-workspace/iteration-4/eval-2-tutorhub-narrative-to-cml/without_skill/run-1/outputs/files/src/main/cml/TutorHub-Context-Map.cml`. The rendered PNG is in that run's `scratch/` folder.
