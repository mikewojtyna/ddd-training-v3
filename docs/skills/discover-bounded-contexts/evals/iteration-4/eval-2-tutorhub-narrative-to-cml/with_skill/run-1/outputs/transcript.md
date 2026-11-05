# Transcript: TutorHub discovery (discover-bounded-contexts)

## User (first message)
"I typed up the stickies from our TutorHub event storming in /home/goobar/IdeaProjects/ddd-training-v3/.claude/skills/discover-bounded-contexts/evals/files/tutorhub-narrative.md. Can you figure out where the bounded context boundaries are and then draw me a context map I can open in Context Mapper?"

## Facilitator -> user (round 1: reading + H1)
Thanks for the stickies. Before applying any heuristic, here's how I read your board, so you can correct me cheaply.

**Main timeline**
- Tutor onboarding: Tutor Applied → Identity Documents Uploaded → Qualifications Verified → Tutor Approved → Tutor Profile Published → Hourly Rate Set → Availability Slots Published
- Student onboarding: Student Registered → Payment Card Added
- Booking: Tutor Searched → Lesson Requested → Lesson Price Quoted → Lesson Booked → Card Pre-authorized → Lesson Reminder Sent
- Lesson: Video Room Opened → Lesson Started → Lesson Ended
- Money: Student Charged → Platform Commission Deducted → Tutor Earnings Credited

**Branches**
- After booking: Lesson Rescheduled, or Lesson Cancelled (→ Late Cancellation Fee Applied)
- Instead of a lesson: No-show Recorded

**Clusters on their own clock**
- Payouts: Weekly Payout Sent → Payout Failed (every Monday, "no matter what happens with lessons")
- Reviews: Review Requested → Review Submitted → Tutor Rating Updated (up to 14 days after)
- Complaints: Complaint Filed → Complaint Investigated → Refund Issued → Tutor Suspended (anytime, even weeks later)
- Qualification Expired → Tutor Suspended

**Ambiguous**
- "Tutor Suspended" appears twice.
- Who decides what a student pays when there's a no-show or a late cancellation.
- Stripe (cards, payouts) and the video provider are external parties, not things we design.

**First heuristic: pivotal events.** A pivotal event changes the business phase: vocabulary, actors, rules or clock change across it. My candidates are Tutor Approved, Lesson Booked, Lesson Ended / No-show Recorded, and Tutor Earnings Credited. Only you can confirm them.

Questions:
1. **Tutor Approved vs Tutor Profile Published:** which one is the real phase change? Do the steps after approval (profile, hourly rate, slots) follow rules of their own, or are they just the tail of approval? This decides whether vetting tutors is separate from what a tutor offers.
2. **Lesson Booked:** what does it mean in business terms? Who produces the quote, and does pricing have rules of its own (who sets the price, commission, discounts), or is it just the hourly rate times the duration? This decides whether quoting belongs to booking or stands apart.
3. **Lesson Ended / No-show Recorded:** do these close the lesson and hand off to charging? Who decides what the student is actually charged: the full price, a late cancellation fee, a no-show? This decides who owns the charge decision.
4. **Tutor Suspended (twice):** is it the same event? Is there one owner that decides suspension for any reason (complaint, expired qualification), and what does a suspension stop: new bookings, existing lessons, payouts? This decides who owns tutor eligibility.
5. **Tutor Earnings Credited → Weekly Payout Sent:** is crediting earnings the hand-off to a separate payout phase? When a payout fails, are there rules of its own (retries, holding), and does anything flow back? This decides whether payouts stand apart from charging students.

## Expert (round 1 answer)
1. **Tutor Approved is the real change.** Applying, uploading ID, verifying qualifications and approving are all done by our Trust & Safety team. They have their own rules, such as which documents each country needs and which qualifications count, and approval happens once. After that the tutor runs things: they publish their profile and set their own hourly rate. They can change the rate any time, and the rate valid when a lesson is booked is the one that applies. [improvised] Tutors manage their availability slots themselves, whenever they like.

2. **Lesson Booked is the real commitment.** Before it the student is shopping; after it there's an appointment. The quote isn't just the rate times the duration. It's the tutor's hourly rate times the duration, plus the platform commission. Commission is 15%, or 10% for tutors with a rating of 4.8 or more and at least 100 lessons. Our business/finance people set those commission rules and change them every few months, separately from anything about booking. The same calculation is used for the quote, the late cancellation fee and the final charge.

3. **Yes, Lesson Ended means the lesson is over and the money side can start.** We decide a no-show ourselves from the video provider's webhooks: someone didn't join within 10 minutes. If the tutor no-shows, the student pays nothing. If the student no-shows, they pay in full. The cancellation rules belong to the Scheduling team: free up to 24h before, after that a fee of 50% of the lesson price, and at most 2 reschedules. The Billing team then builds the final charge (the lesson price or the fee) and charges the pre-authorised card. They also deduct the commission and credit the tutor's earnings. Billing's rules are: charge within 1 hour, retry the card twice, then mark it "unpaid" and email the student.

4. **It's the same outcome, but there are two reasons:** an expired qualification, or an upheld complaint. Today both teams suspend tutors on their own, and it's a mess. A tutor suspended by Complaints got re-activated by Trust & Safety after renewing a certificate. We want ONE place deciding whether a tutor may teach, and Trust & Safety should own it. Complaints should only say "complaint upheld against tutor X". A suspended tutor's profile is hidden from search and they can't receive new bookings. [improvised] Lessons already booked and earnings already credited aren't touched automatically by a suspension.

5. **Yes, payouts are a different team (Payouts) on a completely separate clock from lessons.** Every Monday they pay out tutors' accumulated earnings through Stripe Connect. Their rules: a minimum of 20 EUR, a failed payout is retried the next Monday, and tutors in some countries need tax forms first. [improvised] A failed payout doesn't affect lessons or student charges. The money just stays in the tutor's earnings, and the tutor is told to fix their payout details.

## Facilitator -> user (round 2: H2 swimlanes)
Thanks, that moved a lot. Here's what changed:

- **Confirmed pivots:** Tutor Approved (vetting by Trust & Safety → the tutor runs their own offer), Lesson Booked (shopping → appointment), Lesson Ended (lesson over → money side).
- **Pricing stands apart from booking:** one calculation (rate × duration + commission) serves the quote, the late-cancellation fee and the final charge, and finance changes the commission rules on its own schedule.
- **Billing owns the charge** (build it, charge the card, deduct commission, credit earnings, retries, "unpaid"). **Scheduling owns the cancellation and reschedule rules.**
- **Suspension gets one owner, Trust & Safety.** Complaints only publishes "complaint upheld against tutor X".
- **Payouts is its own lane** on the Monday clock.

Suggested board changes so far: add "Complaint Upheld"; merge the two "Tutor Suspended" stickies; add a "Charge Failed / Marked Unpaid" event.

**Second heuristic: swimlanes.** Flows that run in parallel, on their own clock or in no fixed order are candidates for their own contexts. I still have five flows I can't place:

1. **Reviews and ratings:** who runs them, and do they have rules of their own (the 14-day window, how the rating is computed, moderation)? The commission rule needs the tutor's rating and lesson count: where does pricing get those numbers today? This decides whether Reviews is its own context and how Pricing depends on it.
2. **Complaints:** who investigates, and what can the outcome be? When a complaint ends in a refund, who decides the amount and who actually pays it back? This decides what Complaints owns versus Billing.
3. **The lesson itself** (Video Room Opened, Lesson Started, Lesson Ended, No-show Recorded): which team owns it? Is it the same team and rules as booking, rescheduling and cancelling, or does running the live lesson change separately? This decides whether delivering a lesson is separate from scheduling it.
4. **Tutor profile, hourly rate, availability and search:** who owns these after approval? Are there rules of their own (for example how search ranks tutors), and who stops a booking in a slot that's already taken? This decides whether there's a separate "tutor offer/catalogue" context or it belongs to Scheduling.
5. **Students:** Student Registered and Payment Card Added. Is there anything with rules there (verification, blocking unpaid students), and who owns it? This decides whether student accounts are a context or just a step.

## Expert (round 2 answer)
1. **Reviews:** A small Community team runs them. Students rate the lesson 1-5 with a comment, up to 14 days after the lesson. A tutor's rating is the average of their last 50 reviews. Pricing reads the rating from there for the commission tier. [improvised] Community also hides abusive comments. The lesson count comes from completed lessons, not from reviews.

2. **Complaints:** The Complaints team investigates. Students or tutors can file one any time. There are four possible outcomes: rejected, a refund, upheld against the tutor, or both a refund and upheld. Complaints decides the refund ("refund lesson L, amount A"), and Billing actually reverses the charge, because Billing knows how. When a complaint is upheld, Complaints just says so, and Trust & Safety decides the suspension.

3. **The lesson itself:** The third-party video provider opens the room 5 minutes before the start. Lesson Started and Lesson Ended come in through their webhooks, and we decide no-shows ourselves (nobody joined within 10 minutes). Their webhook model is messy, and they've changed it twice this year. Booking, reschedule and cancellation rules haven't changed like that. [improvised] Today the Scheduling team also looks after the live-lesson side.

4. **Tutor profile, rate, availability, search:** The tutor maintains their own profile, rate and slots. The only rule is that the rate valid at booking time applies. Search has no business rules. It just finds tutors by subject, price, rating and free slots, showing what others publish, with no special ranking. [improvised] Scheduling stops anyone booking a slot that's already taken.

5. **Students:** [improvised] Nothing much. A student registers and adds a card, and Stripe stores the card. We don't verify students. When a charge fails, Billing marks it "unpaid" and emails the student, but today we don't block them.

## Facilitator -> user (round 3: H3, after context critics round 1)
Thanks. Here's what changed with your answers:

- **Reviews** (Community) is its own context. It has its own 14-day window and rating rule, and Pricing only reads the rating.
- **Complaints** decides the outcome and the refund amount. Billing pays the money back, and Trust & Safety decides any suspension. Each decision now has exactly one owner.
- **The tutor's profile, rate, slots and search** have no rules of their own, so they fold into Scheduling. Search just shows what others publish.
- **Students** aren't a context: there are no rules. Payment Card Added goes to Billing.

**Third heuristic: single responsibility.** Every context must answer exactly one question, and no state or decision may live in two places. My independent reviewer flagged two spots where that isn't settled yet, and only you can settle them:

1. **The tutor's earnings balance.** Billing credits earnings after each lesson, and Payouts pays out the accumulated amount every Monday. A failed payout leaves the money "in the tutor's earnings". Who actually keeps that running total of what TutorHub owes each tutor, and which team updates it when a lesson is charged, a payout goes out, or a payout fails? This decides whether the balance belongs to Billing or to Payouts.
2. **Refunds and the tutor's money.** When a refund is issued for a lesson, what happens to the tutor's earnings for it, including when they were already paid out on Monday? Who decides that? This decides who owns any clawback.
3. **The live lesson.** Apart from "nobody joined within 10 minutes", does the live lesson have rules of its own, for example a minimum length to count as held, dropped connections, someone joining late, or disputing a no-show? When those rules change, do they change together with the booking and cancellation rules, and for the same reasons? This decides whether running the lesson is its own context or part of Scheduling. The video provider's changing webhooks alone don't decide it: we'd shield ourselves from those with a translation layer either way.
4. **Consequences of a no-show.** Does a tutor or student no-show have consequences beyond the charge, such as strikes, warnings, or input to Trust & Safety? And is a "completed lesson" (counted for the 100-lesson commission tier) simply a lesson that ended without a no-show? This decides who publishes the lesson outcome and who reacts to it.
5. **Pre-authorisation and the final amount.** Who pre-authorises the card, and what happens to the booking if the pre-authorisation fails? Is the final charge the price quoted at booking, or is it recalculated at charge time, for example if the tutor's commission tier changed in between? This decides how Scheduling, Pricing and Billing depend on each other.

## Expert (round 3 answer)
1. **Earnings balance:** Billing credits the tutor's earnings after each charged lesson, and Payouts pays out the accumulated amount every Monday. [improvised] The Payouts team keeps the running total of what we owe each tutor. Billing just tells them "lesson L earned tutor X amount A", and Payouts updates the balance when a payout goes out or fails. A failed payout simply stays on the balance until the next Monday.

2. **Refunds and the tutor's money:** Complaints decides the refund, and Billing reverses the student's charge. [improvised] The tutor loses their earnings for that lesson. If the money was already paid out, Payouts takes it off the tutor's next payout. Whether the tutor's share is taken back is part of the Complaints decision.

3. **The live lesson:** [improvised] The only rule is the 10-minute no-show rule. There's no minimum length: if both joined, the lesson counts as held, even with dropped connections or someone joining late. Disputes about a no-show go through Complaints like any other complaint. The no-show rule has never changed, while the booking and cancellation rules do get tweaked now and then, for business reasons.

4. **No-show consequences:** [improvised] Beyond the charge, there are none today: no strikes and no warnings. If a tutor keeps no-showing, a student files a complaint. A "completed lesson" for the 100-lesson tier is a lesson that ended without a no-show.

5. **Pre-authorisation and the final amount:** The card is pre-authorised right after booking, which is just Stripe holding the money. [improvised] Billing asks Stripe for the hold. If it fails, the booking is cancelled and the student is asked to use another card. The rate valid at booking time applies, and the same calculation is used for the quote, the late fee and the final charge. [improvised] The price quoted at booking is locked in, so a commission tier change in between doesn't alter it.

## Facilitator -> user (round 4: candidates for approval + map yes/no)
Here's the candidate list. An independent reviewer approved every context. Your round-3 answers moved two boundaries: the live lesson folded into Scheduling (its only rule is the 10-minute no-show rule, and that never changes), and the tutor's earnings balance moved wholly to Payouts.

1. **Trust & Safety:** "May this tutor teach on TutorHub?" It owns applying, documents, qualifications, approval, expiry, and the only suspension decision (it reacts to "complaint upheld"). Why separate: Tutor Approved is the pivot, with its own per-country rules.
2. **Scheduling:** "What's the state of each lesson, from booking to held or no-show?" It owns the tutor's profile, rate, slots and search (no rules of their own), plus booking with the locked quote, reminders, reschedules (max 2), cancellations (free until 24h before, then a fee of 50%), and the live lesson with the 10-minute no-show rule. We'd shield it from the video provider's changing webhooks with a translation layer. Why one context: Lesson Booked to Lesson Ended is one appointment phase.
3. **Pricing:** "What does this lesson cost, including our commission?" It owns the quote: rate × duration + 15%, or 10% at a rating of 4.8 or more and 100+ completed lessons. Why separate: finance changes it on its own schedule, and the quote, the fee and the charge all use it.
4. **Billing:** "What does the student pay for each lesson, and has it been collected?" It owns the card, the hold after booking, the charge (the full price, the late fee, the full price on a student no-show, nothing on a tutor no-show), commission, retries and "unpaid", and carrying out refunds. It reports each lesson's tutor earnings to Payouts. Why separate: Lesson Ended is the pivot, and Billing has its own collection rules.
5. **Payouts:** "How much do we owe each tutor, and when is it paid?" It owns the tutor's balance, the Monday payouts (20 EUR minimum, retry next Monday, tax forms) and clawbacks. Why separate: its own weekly clock.
6. **Reviews (Community):** "What's this tutor's rating?" It owns reviews within 14 days and the average of the last 50. Why separate: its own lane and team.
7. **Complaints:** "How is this complaint resolved?" Rejected, refund, upheld, or both. It decides the refund amount and whether the tutor's share is taken back, but it doesn't suspend or move money.

**Not contexts:** student sign-up (no rules), notifications, and Stripe and the video provider (external).

**Open assumptions** (you made these up on the spot): Payouts keeps the balance; clawbacks; a failed card hold cancels the booking; the quote is locked in; the live lesson has no other rules.

**Suggested board changes:**
- merge the two "Tutor Suspended" stickies
- add Complaint Upheld, Refund Decided, Card Pre-authorization Failed, Charge Marked Unpaid, Lesson Completed, Tutor Reinstated
- rename "Late Cancellation Fee Applied" to "Incurred"
- rename Billing's "Tutor Earnings Credited" to "Lesson Earnings Recorded"

Questions:
1. Do you approve these seven contexts?
2. Do you want a context map in Context Mapper (.cml) format?

## Expert (round 4 answer)
Expert: yes to both. I approve the seven contexts, and please make the context map in CML.

1. Yes, I approve the seven contexts. They match how our teams actually work, and the names are fine.
2. Yes, please make the map in CML. Call the system TutorHub. Please draw it for these four processes: (1) Tutor onboarding, (2) Book and take a lesson, (3) Weekly payout, and (4) Complaint to refund and suspension. At the end I'd also like the canvases for each context.

## Facilitator -> user (round 5: map hand-offs)
Thanks. The seven contexts are approved, and I'll build the TutorHub map from your four processes.

I walked each process step by step through the contexts. Most hand-offs are clear from what you've told me. For example, Trust & Safety tells Scheduling who may be booked, Scheduling asks Pricing for the quote, Scheduling hands the lesson outcome to Billing, and Billing reports each lesson's tutor earnings to Payouts. Five hand-offs depend on facts I don't have yet. The direction of each arrow follows whose terms the other side has to understand, so I'd rather ask than guess:

1. **Failed card hold (Book and take a lesson):** when Stripe refuses the hold, what happens step by step? Who cancels the booking, and does the student get a chance to use another card first?
2. **Review request (after a lesson):** what starts the review request, and what does the Community team need to know about the lesson? Is it only that the lesson was held, or also no-shows? Search also shows the tutor's rating: where does it get it?
3. **Complaint upheld → Trust & Safety (Complaint to refund and suspension):** what exactly does Trust & Safety receive when a complaint is upheld against a tutor? Does Trust & Safety set what that report must contain (for example a category or severity that its suspension rules use), or does it take whatever Complaints sends?
4. **Investigating a complaint:** what does Complaints look at about the lesson and the money (lesson details, the no-show record, the amount charged), and where does it get them?
5. **Refund and clawback:** once Complaints decides "refund lesson L, amount A, and take back the tutor's share", how does that reach Billing and Payouts? What does each of them need to know?

## Expert (round 5 answer)
1. **Failed card hold:** [improvised] Billing asks Stripe for the hold right after booking. If Stripe refuses, the student gets an email and has 1 hour to add another card, and Billing then tries again. If the hold still fails, Billing reports "hold failed for lesson L", and the booking is cancelled under the booking team's rules, which frees the slot for others.

2. **Review request:** [improvised] The request goes out only once a lesson has actually been held, meaning it ended and nobody no-showed. Community needs the lesson, the student, the tutor and the date. No-shows don't get reviews. Search gets the rating from Community, the people who own it.

3. **Complaint upheld:** [improvised] Trust & Safety defines what it needs, because its suspension rules depend on it. That's the tutor, the lesson, a category (safety, conduct or teaching quality) and a severity. A safety case means an immediate suspension, and the others depend on severity and history. Complaints has to report upheld complaints in that form; Trust & Safety doesn't take free text.

4. **Investigating a complaint:** [improvised] Complaints looks at the lesson details and the no-show record from the booking/lesson side, and at the amount charged and any fee from Billing. It only reads them and never changes them.

5. **Refund and clawback:** Complaints sends one decision to Billing: "refund lesson L, amount A", plus whether the tutor's share is taken back. Billing reverses the student's charge. [improvised] If the tutor's share is taken back, Billing then reports the reversed tutor earnings for that lesson to Payouts, the same way it reports earnings. Payouts takes the amount off the tutor's balance, or off the next payout if the money was already paid out. Payouts never hears from Complaints directly.

## Facilitator -> user (round 6: map follow-ups after map critics round 1)
Thanks. The first draft of the map is valid, and the reviewer approved it for tutor onboarding and for booking and taking a lesson. For the complaint and payout processes it found two hand-offs I haven't pinned down properly. One of them is my fault: last time I asked about the upheld-complaint report as an either/or, which nudged your answer. So let me ask again, openly:

1. **Upheld complaint against a tutor:** what does Complaints actually send to Trust & Safety today? Who came up with the categories and severity levels? When Trust & Safety changes its suspension policy, does the Complaints team have to change how it classifies complaints?
2. **During an investigation:** does Complaints keep going back to the lesson and its no-show record while it works, or is what was known about the lesson when the complaint was filed enough?
3. **Tax forms and payout details:** when a tutor in a country that needs tax forms sends them in, who checks them? Who sets up the tutor's payout account, and how does Payouts learn the tutor's country?
4. **Which lessons can be reviewed:** who decides that only held lessons get a review request? If that changed one day (for example, allowing a review after a late cancellation), which team would change it?

## Expert (round 6 answer)
1. **Upheld complaint:** [improvised] Today Complaints sends: complaint upheld against tutor X, for lesson L, with a category and a severity. Trust & Safety came up with the categories and severity levels, because their suspension rules use them. Most suspension policy changes are about what Trust & Safety does with a category (for example, suspending sooner for conduct). Complaints doesn't notice those changes. Only when Trust & Safety adds or redefines a category does Complaints have to start classifying differently, and that has happened once so far.

2. **During an investigation:** [improvised] What we know about the lesson when the complaint is filed is enough. Once a lesson is over, its facts don't change. Investigators sometimes re-open the lesson record to double-check, but they don't need it to be live.

3. **Tax forms and payout details:** [improvised] The Payouts team checks the tax forms. The tutor sets up their payout account through Stripe Connect's onboarding, which Payouts runs. The tutor's country comes from their application, which Trust & Safety checked during vetting, and Payouts gets it when a tutor is approved.

4. **Which lessons can be reviewed:** [improvised] The Community team decides that only held lessons get a review request. If that changed one day, for example allowing reviews after a late cancellation, Community would change it. They'd just need to know about late cancellations too.

## Facilitator -> user (round 7: map for approval)
Here's the TutorHub map: `src/main/cml/TutorHub-Context-Map.cml`. Context Mapper parses it with 0 errors and no rule warnings, and the reviewer approved it for all four processes. Upstream means the other side has to understand its terms. Every upstream offers an open API or published events, and every downstream translates them through its own layer.

| Upstream → Downstream | What flows | Why this direction |
|---|---|---|
| Trust & Safety → Scheduling | Tutor approved / suspended / reinstated | Scheduling needs to know who may teach |
| Trust & Safety → Complaints | Upheld-complaint report (category, severity) | T&S defines the categories its rules use |
| Trust & Safety → Payouts | Tutor approved, with country | tax-form rule |
| Reviews → Pricing | Tutor rating | commission tier |
| Reviews → Scheduling | Rating for search; Scheduling reports each lesson outcome | Community decides which lessons get a review |
| Pricing → Scheduling | Quote (rate, duration, completed lessons → price, commission) | Pricing's calculation is the contract |
| Scheduling → Billing | Booked (locked quote), cancelled/late fee, held, no-show; Billing asks to cancel on a failed hold | the booking team's rules decide the cancellation |
| Scheduling → Complaints | Lesson record snapshot | read-only |
| Billing → Payouts | Lesson earnings recorded / reversed | Payouts never hears from Complaints |
| Billing → Complaints | Charge lookup; refund service | Billing knows how to reverse charges |
| Stripe → Billing, Stripe → Payouts, Video provider → Scheduling | external | translation layers; the video webhooks changed twice this year |

**Three loops were avoided:**
- search needs the rating while Reviews needs lesson outcomes
- Pricing needs the completed-lesson count, so Scheduling sends it in the quote request
- a failed card hold cancels the booking, so Billing calls Scheduling's cancel

**Open: answers you gave on the spot, to re-confirm with the teams:**
- Payouts keeps the balance
- clawbacks via Billing
- cancellation on a failed hold
- the locked quote
- the conduct-report form
- the country arriving at approval
- the snapshot for investigations
- Community owning review eligibility
- "Tutor Reinstated" as a real event

Do you approve the map? If you do, I'll write one canvas per context.

## Expert (round 7 answer)
Expert: yes, I approve the TutorHub map. Please go ahead with one canvas per context.

The hand-offs match how our teams work, and the list of answers to re-confirm with the teams is fair. I'll check those with each team.

## Facilitator -> user (final message)
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


## Critic verdicts (one line each)
- Critic: Reviews, context round 1: AGREE. One question with a single owner; the 14-day lane is confirmed. Moderation is improvised; optional questions about how complaints affect ratings.
- Critic: TrustAndSafety, context round 1: AGREE. Single owner of suspension. Suggests adding Tutor Reinstated and Application Rejected to the board.
- Critic: Complaints, context round 1: AGREE. Decides the refund, Billing executes it. Suggests a Complaint Resolved/Rejected event and warns of a Billing<->Complaints cycle on the map.
- Critic: Scheduling, context round 1: AGREE. Folding in the tutor offer (no rules) is justified by H3. Suggests renaming "Late Cancellation Fee Applied" to "Incurred".
- Critic: Payouts, context round 1: DISAGREE. The tutor earnings balance has two owners (Billing credits it, Payouts debits it). Ask the expert who keeps the running total.
- Critic: Pricing, context round 1: AGREE. A stateless calculator that changes independently. Asks whether the charge uses the quote snapshot or a recalculation.
- Critic: LessonDelivery, context round 1: DISAGREE. The split rests on vendor webhook churn (a technical reason, handled by an ACL). Ask whether the live lesson has its own business rules and its own change cadence; otherwise fold it into Scheduling.
- Critic: Billing, context round 1: DISAGREE. The same balance-ownership issue as Payouts. Also asks who pre-authorizes the card and whether a refund claws back earnings.
- Critic: Payouts, context round 2: AGREE. Payouts is the only owner of the balance, including clawback debits. Pick one source message for the clawback on the map.
- Critic: Billing, context round 2: AGREE. One student-side question. Rename Billing's "Tutor Earnings Credited" to a per-lesson fact; put Card Pre-authorization Failed on the board.
- Critic: Complaints, context round 2: AGREE. Still the only decider of refunds and clawbacks. Open: what a tutor's no-show dispute can lead to; whether the clawback is per case or automatic.
- Critic: Scheduling (merged with LessonDelivery), context round 2: AGREE. Folding in the live lesson is justified by H3; webhook churn goes behind an ACL. The fold rests on improvised answers, so revisit it if live-lesson rules appear.
- Critic: map + (1) Tutor onboarding, round 1: AGREE. Only TutorEligibility is used; T&S upstream rests on firm answers. Mark Tutor Reinstated as proposed.
- Critic: map + (3) Weekly payout, round 1: DISAGREE. Who collects and checks tax forms and payout details is unknown, and a T&S->Payouts link may be missing. Asked the expert neutrally.
- Critic: map + (4) Complaint to refund and suspension, round 1: DISAGREE. T&S->Complaints is a flip resting on an improvised answer to a leading either/or question (round 5 Q3). Re-ask neutrally; if Complaints is upstream, resolve the indirect cycle Complaints->T&S->Scheduling->Complaints by carrying lesson data.
- Critic: map + (2) Book and take a lesson, round 1: AGREE. All three cycle resolutions follow the prescribed order. Re-confirm who owns the review-eligibility rule.
- Critic: map + (3) Weekly payout, round 2: AGREE. The T&S->Payouts TutorApprovals link fills the tax-form/country gap.
- Critic: map + (4) Complaint to refund and suspension, round 2: AGREE. TutorConductReports now rests on a neutral re-ask (T&S defined the categories). Harmonise the "to re-confirm" wording.
- Critic: map final pass over all 4 processes, round 3: AGREE. Acyclic, every relationship used, no RULE lines. Note: the Reviews OHS must own its own outcome vocabulary; list the 'to re-confirm' answers.
