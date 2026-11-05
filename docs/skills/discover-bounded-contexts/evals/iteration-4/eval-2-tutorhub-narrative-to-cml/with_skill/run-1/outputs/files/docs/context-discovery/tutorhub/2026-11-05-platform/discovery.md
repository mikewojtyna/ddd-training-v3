# Bounded context discovery: TutorHub (platform)

- **Date:** 2026-11-05
- **Mode:** discover
- **Status:** canvases handed off
- **Input:** `.claude/skills/discover-bounded-contexts/evals/files/tutorhub-narrative.md` (typed-up event-storming stickies plus quotes)
- **Context map:** `src/main/cml/TutorHub-Context-Map.cml`

## Reading of the input
- **Main timeline:**
  - tutor onboarding (Applied → Documents → Qualifications Verified → Approved → Profile → Rate → Slots)
  - student onboarding (Registered → Card Added)
  - booking (Searched → Requested → Price Quoted → Booked → Card Pre-authorized → Reminder)
  - lesson (Room Opened → Started → Ended)
  - money (Student Charged → Commission Deducted → Tutor Earnings Credited)
- **Branches:** Rescheduled; Cancelled → Late Cancellation Fee; No-show Recorded.
- **Clusters on their own clock:**
  - payouts every Monday
  - reviews up to 14 days after
  - complaints at any time
  - Qualification Expired → Tutor Suspended
- **Ambiguities:**
  - "Tutor Suspended" appears twice. The expert confirmed it's one outcome with two reasons and wants a single owner.
  - Stripe and the video provider are external.

The expert didn't correct this reading. They made it more precise in round 1.

## Heuristic trail
### H1: Pivotal events
| Event | Status | Before | After | Expert's answer |
|---|---|---|---|---|
| Tutor Approved | confirmed | Trust & Safety vetting with per-country rules | the tutor runs their own offer | "Tutor Approved is the real change… approval happens once" (r1 Q1) |
| Tutor Profile Published | rejected as pivot | | | the tail of the tutor's own offer, no rules |
| Lesson Booked | confirmed | the student is shopping | there's an appointment | "Lesson Booked is the real commitment" (r1 Q2) |
| Lesson Ended / No-show | confirmed | the appointment | the money side (Billing) | "Lesson Ended means the lesson is over and the money side can start" (r1 Q3) |
| Tutor Earnings Credited | confirmed hand-off | per-lesson charging | weekly payouts | "payouts are a different team… completely separate clock" (r1 Q5) |

### H2: Swimlanes
| Flow | Verdict | Why |
|---|---|---|
| Payouts | lane | every Monday, "no matter what happens with lessons"; its own team and rules |
| Reviews | lane | 14-day window; Community team; its own rating rule |
| Complaints | lane | any time, even weeks later; Complaints team; four outcomes |
| Qualification expiry | variant trigger inside Trust & Safety | same team, same question |
| Live lesson (room, start, end, no-show) | first a separate candidate, then folded into Scheduling (H3) | its only rule is the 10-minute no-show rule; the webhook churn is technical and handled by an ACL |
| Tutor offer & search | folded into Scheduling | no rules except "rate valid at booking applies"; search has no rules |
| Student registration | not a context | no rules; Stripe stores the card |

### H3: Single responsibility
| Duplicated responsibility | Where it appeared | Resolution |
|---|---|---|
| Suspending tutors | Trust & Safety (expiry) and Complaints (upheld) | Trust & Safety is the only owner; Complaints reports upheld complaints (r1 Q4) |
| Price calculation | quote, late fee, final charge | one Pricing calculation; the quote is locked at booking (r1 Q2, r3 Q5) |
| Refund | Complaints decides, Billing executes | the decision goes to Complaints and the money movement to Billing (r2 Q2) |
| Tutor earnings balance | Billing credits it, Payouts pays from it | Payouts owns the balance; Billing publishes per-lesson earnings (r3 Q1; context critics r1) |
| Clawback | Complaints, Billing, Payouts | Complaints decides; Billing reverses the charge and reports reversed earnings; Payouts debits the balance (r3 Q2, r5 Q5) |
| Late-cancellation fee | Scheduling's rule, Pricing's price, Billing's charge | Scheduling decides that a fee is incurred (50%), the base is the locked quote, and Billing charges it |
| Review eligibility | Scheduling (calling only for held lessons) or Community | Community owns it; Scheduling reports every outcome (r6 Q4) |

## Bounded contexts
| Context | Question it answers | Owns (events, decisions, rules) | Heuristic justification | Classification |
|---|---|---|---|---|
| TrustAndSafety | May this tutor teach on TutorHub? | Applied, Documents, Qualifications Verified, Approved, Qualification Expired, Suspended, (Reinstated); per-country docs, accepted qualifications, suspension | H1 pivot Tutor Approved; H3 single owner of suspension | not discussed |
| Scheduling | What is the state of each lesson, from booking to held or no-show? | profile, rate, slots, search, request, booking (locked quote), reminder, reschedule (max 2), cancellation (24h / 50%), live lesson, 10-minute no-show | H1 Lesson Booked→Lesson Ended is one appointment phase; H3 the live lesson and offer have no rules of their own | not discussed |
| Pricing | What does this lesson cost, including the platform commission? | Lesson Price Quoted; 15% / 10% tier (rating ≥ 4.8, ≥ 100 lessons) | H3 changes independently (finance, every few months); one calculation reused | not discussed |
| Billing | What does the student pay for each lesson, and has it been collected? | card, hold, final charge, commission deduction, lesson earnings, refund execution, unpaid; charge within 1h, 2 retries | H1 far side of Lesson Ended; its own collection rules | not discussed |
| Payouts | How much do we owe each tutor, and when is it paid out? | balance, Monday payout, failure/retry, clawbacks, tax forms, Stripe Connect onboarding; 20 EUR minimum | H2 its own weekly clock; H3 single owner of the balance | not discussed |
| Reviews | What is this tutor's rating, based on students' lesson reviews? | review request, review, rating; 14 days, average of the last 50, review eligibility | H2 14-day lane, Community team | not discussed |
| Complaints | How is this complaint resolved? | filed, investigated, outcome, refund decision, upheld report | H2 any-time lane; H3 decides, others execute | not discussed |

External: Stripe (cards, holds, charges, refunds, Connect payouts) and VideoProvider (rooms, webhooks).

## Critic log
| Subject | Round | Verdict | Blocking points / resolution |
|---|---|---|---|
| Reviews | 1 | AGREE | One question with a single owner; the 14-day lane is confirmed. Moderation is improvised; optional questions about how complaints affect ratings. |
| TrustAndSafety | 1 | AGREE | Single owner of suspension. Suggests adding Tutor Reinstated and Application Rejected to the board. |
| Complaints | 1 | AGREE | Decides the refund, Billing executes it. Suggests a Complaint Resolved/Rejected event and warns of a Billing<->Complaints cycle on the map. |
| Scheduling | 1 | AGREE | Folding in the tutor offer (no rules) is justified by H3. Suggests renaming "Late Cancellation Fee Applied" to "Incurred". |
| Payouts | 1 | DISAGREE | The tutor earnings balance has two owners (Billing credits it, Payouts debits it). Ask the expert who keeps the running total. |
| Pricing | 1 | AGREE | A stateless calculator that changes independently. Asks whether the charge uses the quote snapshot or a recalculation. |
| LessonDelivery | 1 | DISAGREE | The split rests on vendor webhook churn (a technical reason, handled by an ACL). Ask whether the live lesson has its own business rules and its own change cadence; otherwise fold it into Scheduling. |
| Billing | 1 | DISAGREE | The same balance-ownership issue as Payouts. Also asks who pre-authorizes the card and whether a refund claws back earnings. |
| Payouts | 2 | AGREE | Payouts is the only owner of the balance, including clawback debits. Pick one source message for the clawback on the map. |
| Billing | 2 | AGREE | One student-side question. Rename Billing's "Tutor Earnings Credited" to a per-lesson fact; put Card Pre-authorization Failed on the board. |
| Complaints | 2 | AGREE | Still the only decider of refunds and clawbacks. Open: what a tutor's no-show dispute can lead to; whether the clawback is per case or automatic. |
| Scheduling (merged with LessonDelivery) | 2 | AGREE | Folding in the live lesson is justified by H3; webhook churn goes behind an ACL. The fold rests on improvised answers, so revisit it if live-lesson rules appear. |
| map + (1) Tutor onboarding | 1 | AGREE | Only TutorEligibility is used; T&S upstream rests on firm answers. Mark Tutor Reinstated as proposed. |
| map + (3) Weekly payout | 1 | DISAGREE | Who collects and checks tax forms and payout details is unknown, and a T&S->Payouts link may be missing. Asked the expert neutrally. |
| map + (4) Complaint to refund and suspension | 1 | DISAGREE | T&S->Complaints is a flip resting on an improvised answer to a leading either/or question (round 5 Q3). Re-ask neutrally; if Complaints is upstream, resolve the indirect cycle Complaints->T&S->Scheduling->Complaints by carrying lesson data. |
| map + (2) Book and take a lesson | 1 | AGREE | All three cycle resolutions follow the prescribed order. Re-confirm who owns the review-eligibility rule. |
| map + (3) Weekly payout | 2 | AGREE | The T&S->Payouts TutorApprovals link fills the tax-form/country gap. |
| map + (4) Complaint to refund and suspension | 2 | AGREE | TutorConductReports now rests on a neutral re-ask (T&S defined the categories). Harmonise the "to re-confirm" wording. |
| map final pass over all 4 processes | 3 | AGREE | Acyclic, every relationship used, no RULE lines. Note: the Reviews OHS must own its own outcome vocabulary; list the 'to re-confirm' answers. |

## Context map
### Process walkthroughs
#### (1) Tutor onboarding
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Applied, Documents, Qualifications Verified, Approved | TrustAndSafety | |
| Profile Published, Hourly Rate Set, Slots Published | Scheduling | TrustAndSafety → Scheduling : TutorEligibility |
| Country for the tax-form rule | Payouts | TrustAndSafety → Payouts : TutorApprovals |
| Qualification Expired → Suspended → hidden from search | TrustAndSafety → Scheduling | TutorEligibility |

#### (2) Book and take a lesson
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Tutor Searched (rating, free slots) | Scheduling | Reviews → Scheduling : TutorRatingAndLessonOutcomes; TrustAndSafety → Scheduling : TutorEligibility |
| Lesson Requested | Scheduling | |
| Lesson Price Quoted | Pricing | Pricing → Scheduling : LessonQuote; Reviews → Pricing : TutorRating |
| Lesson Booked (quote locked) | Scheduling | Scheduling → Billing : LessonOutcomes |
| Card Pre-authorized / hold failed → booking cancelled | Billing (Stripe) → Scheduling | Stripe → Billing : CardPayments; Billing calls Scheduling's Cancel Booking (LessonOutcomes) |
| Reminder, Rescheduled, Cancelled, Late Fee Incurred | Scheduling | LessonOutcomes |
| Room Opened, Started, Ended, No-show | Scheduling | VideoProvider → Scheduling : VideoRooms |
| Student Charged, Commission Deducted | Billing | CardPayments |
| Lesson Earnings Recorded | Billing → Payouts | Billing → Payouts : TutorEarnings |
| Review Requested, Submitted, Rating Updated | Reviews | Scheduling reports the outcome via TutorRatingAndLessonOutcomes; the rating reaches Pricing and search |

#### (3) Weekly payout
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Earnings accumulate on the balance | Payouts | Billing → Payouts : TutorEarnings |
| Tax-form check (country from approval) | Payouts | TrustAndSafety → Payouts : TutorApprovals |
| Weekly Payout Sent / Payout Failed (retry next Monday) | Payouts | Stripe → Payouts : ConnectPayouts |

#### (4) Complaint to refund and suspension
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Complaint Filed | Complaints | |
| Complaint Investigated | Complaints | Scheduling → Complaints : LessonRecords; Billing → Complaints : ChargesAndRefunds |
| Refund Decided → Refund Issued | Complaints → Billing | ChargesAndRefunds (Billing's refund service); CardPayments |
| Tutor share taken back | Billing → Payouts | TutorEarnings (Lesson Earnings Reversed) |
| Complaint Upheld → Tutor Suspended | Complaints → TrustAndSafety | TrustAndSafety → Complaints : TutorConductReports (T&S's form) |
| Suspended tutor hidden, no new bookings | Scheduling | TutorEligibility |

### Relationships
| Upstream | Downstream | Patterns | Messages | Justification (rule) |
|---|---|---|---|---|
| TrustAndSafety | Scheduling | OHS, PL / ACL | Tutor Approved, Suspended, Reinstated (proposed) | Scheduling needs eligibility (r1 Q4); integration rules 3-5 |
| TrustAndSafety | Complaints | OHS, PL / ACL | Report Upheld Complaint (tutor, lesson, category, severity) | T&S defined the categories, and Complaints changes when they're redefined (r6 Q1, neutral re-ask) |
| TrustAndSafety | Payouts | OHS, PL / ACL | Tutor Approved (country) | country comes from vetting (r6 Q3) |
| Reviews | Pricing | OHS, PL / ACL | Tutor Rating Updated | "Pricing reads the rating from there" (r2 Q1) |
| Reviews | Scheduling | OHS, PL / ACL | tutor rating for search; Report Lesson Outcome | search gets the rating from Community; Community owns review eligibility (r5 Q2, r6 Q4); Scheduling as API client breaks a direct cycle |
| Pricing | Scheduling | OHS, PL / ACL | Quote Lesson → Lesson Price Quoted | the calculation is Pricing's; the completed-lesson count is carried in the request (cycle break, step 2) |
| Scheduling | Billing | OHS, PL / ACL | Lesson Booked, Cancelled, Late Fee Incurred, Completed, No-show; Cancel Booking command | Billing reacts to lesson facts; cancellation follows the booking team's rules (r5 Q1); cycle break, step 1 |
| Scheduling | Complaints | OHS, PL / ACL | lesson record snapshot | read-only (r5 Q4, r6 Q2) |
| Billing | Payouts | OHS, PL / ACL | Lesson Earnings Recorded / Reversed | Payouts never hears from Complaints (r5 Q5) |
| Billing | Complaints | OHS, PL / ACL | charge lookup; Refund Lesson | "Billing actually reverses the charge, because Billing knows how" (r2 Q2) |
| Stripe | Billing | OHS, PL / ACL | holds, charges, refunds | external; the ACL keeps the retry and unpaid rules in our model |
| Stripe | Payouts | OHS, PL / ACL | Connect payouts, failures | external |
| VideoProvider | Scheduling | OHS / ACL | room and webhooks | its model changed twice this year (r2 Q3) |

Upstream/downstream with OHS+PL and an ACL is used consistently between internal teams; there is no Shared Kernel, Partnership, Conformist or Separate Ways. Cycles considered and broken:
- Reviews↔Scheduling: Scheduling became a client of Reviews' API.
- Pricing↔Scheduling: the completed-lesson count is carried in the quote request.
- Billing↔Scheduling: Billing calls Scheduling's cancel.
- Billing↔Complaints: Billing's refund OHS.
- Complaints→T&S→Scheduling→Complaints (indirect): T&S owns the conduct-report contract, settled by a neutral re-ask in round 6.

### Validator output
```
RELATIONSHIPS:
  TrustAndSafety [OHS, PL] -> Scheduling [ACL] : TutorEligibility
  TrustAndSafety [OHS, PL] -> Complaints [ACL] : TutorConductReports
  TrustAndSafety [OHS, PL] -> Payouts [ACL] : TutorApprovals
  Reviews [OHS, PL] -> Pricing [ACL] : TutorRating
  Reviews [OHS, PL] -> Scheduling [ACL] : TutorRatingAndLessonOutcomes
  Pricing [OHS, PL] -> Scheduling [ACL] : LessonQuote
  Scheduling [OHS, PL] -> Billing [ACL] : LessonOutcomes
  Scheduling [OHS, PL] -> Complaints [ACL] : LessonRecords
  Billing [OHS, PL] -> Payouts [ACL] : TutorEarnings
  Billing [OHS, PL] -> Complaints [ACL] : ChargesAndRefunds
  Stripe [OHS, PL] -> Billing [ACL] : CardPayments
  Stripe [OHS, PL] -> Payouts [ACL] : ConnectPayouts
  VideoProvider [OHS] -> Scheduling [ACL] : VideoRooms
VALID: 9 bounded contexts, 13 relationships, 0 errors
```
No `RULE` lines, so nothing needed resolving or justifying.

## Q&A with the domain expert
Summary per round (the expert's verbatim answers follow; [improvised] marks answers the expert gave on the spot):
1. **Reading and H1:** Tutor Approved, Lesson Booked and Lesson Ended were confirmed as pivots. Pricing has its own commission rules set by finance. Scheduling owns cancellation; Billing owns the charge and its retries. One owner for suspension (Trust & Safety). Payouts is its own team and clock.
2. **H2:** Reviews belong to Community (14 days, average of the last 50). Complaints decides refunds, Billing executes them, and T&S suspends. The video webhooks changed twice. The tutor offer and search have no rules. Students have no rules.
3. **H3:** Payouts keeps the balance. Refunds take back the tutor's share via Payouts. The live lesson's only rule is the 10-minute no-show rule. A completed lesson is one ended without a no-show. Billing holds the card, a failed hold cancels the booking, and the quote is locked.
4. **Approval:** the seven contexts were approved, and a CML map was requested for 4 processes plus canvases.
5. **Map hand-offs:** hold-failure flow; review-request trigger; conduct-report form; what investigators read; the refund and clawback path.
6. **Neutral re-ask** after the map critics: T&S defined the categories; a snapshot is enough for investigations; Payouts checks tax forms and gets the country at approval; Community owns review eligibility.
7. **Map approved.**

### Verbatim answers
Round 1 questions (summary): 1 Tutor Approved vs Profile Published; 2 Lesson Booked meaning and pricing rules; 3 Lesson Ended/No-show and who decides the charge; 4 Tutor Suspended twice, owner; 5 Earnings credited vs weekly payouts.
Round 2 questions (summary): 1 Reviews; 2 Complaints and refunds; 3 the live lesson; 4 tutor profile/rate/slots/search; 5 students.

#### round 1
1. **Tutor Approved is the real change.** Applying, uploading ID, verifying qualifications and approving are all done by our Trust & Safety team. They have their own rules, such as which documents each country needs and which qualifications count, and approval happens once. After that the tutor runs things: they publish their profile and set their own hourly rate. They can change the rate any time, and the rate valid when a lesson is booked is the one that applies. [improvised] Tutors manage their availability slots themselves, whenever they like.

2. **Lesson Booked is the real commitment.** Before it the student is shopping; after it there's an appointment. The quote isn't just the rate times the duration. It's the tutor's hourly rate times the duration, plus the platform commission. Commission is 15%, or 10% for tutors with a rating of 4.8 or more and at least 100 lessons. Our business/finance people set those commission rules and change them every few months, separately from anything about booking. The same calculation is used for the quote, the late cancellation fee and the final charge.

3. **Yes, Lesson Ended means the lesson is over and the money side can start.** We decide a no-show ourselves from the video provider's webhooks: someone didn't join within 10 minutes. If the tutor no-shows, the student pays nothing. If the student no-shows, they pay in full. The cancellation rules belong to the Scheduling team: free up to 24h before, after that a fee of 50% of the lesson price, and at most 2 reschedules. The Billing team then builds the final charge (the lesson price or the fee) and charges the pre-authorised card. They also deduct the commission and credit the tutor's earnings. Billing's rules are: charge within 1 hour, retry the card twice, then mark it "unpaid" and email the student.

4. **It's the same outcome, but there are two reasons:** an expired qualification, or an upheld complaint. Today both teams suspend tutors on their own, and it's a mess. A tutor suspended by Complaints got re-activated by Trust & Safety after renewing a certificate. We want ONE place deciding whether a tutor may teach, and Trust & Safety should own it. Complaints should only say "complaint upheld against tutor X". A suspended tutor's profile is hidden from search and they can't receive new bookings. [improvised] Lessons already booked and earnings already credited aren't touched automatically by a suspension.

5. **Yes, payouts are a different team (Payouts) on a completely separate clock from lessons.** Every Monday they pay out tutors' accumulated earnings through Stripe Connect. Their rules: a minimum of 20 EUR, a failed payout is retried the next Monday, and tutors in some countries need tax forms first. [improvised] A failed payout doesn't affect lessons or student charges. The money just stays in the tutor's earnings, and the tutor is told to fix their payout details.

#### round 2
1. **Reviews:** A small Community team runs them. Students rate the lesson 1-5 with a comment, up to 14 days after the lesson. A tutor's rating is the average of their last 50 reviews. Pricing reads the rating from there for the commission tier. [improvised] Community also hides abusive comments. The lesson count comes from completed lessons, not from reviews.

2. **Complaints:** The Complaints team investigates. Students or tutors can file one any time. There are four possible outcomes: rejected, a refund, upheld against the tutor, or both a refund and upheld. Complaints decides the refund ("refund lesson L, amount A"), and Billing actually reverses the charge, because Billing knows how. When a complaint is upheld, Complaints just says so, and Trust & Safety decides the suspension.

3. **The lesson itself:** The third-party video provider opens the room 5 minutes before the start. Lesson Started and Lesson Ended come in through their webhooks, and we decide no-shows ourselves (nobody joined within 10 minutes). Their webhook model is messy, and they've changed it twice this year. Booking, reschedule and cancellation rules haven't changed like that. [improvised] Today the Scheduling team also looks after the live-lesson side.

4. **Tutor profile, rate, availability, search:** The tutor maintains their own profile, rate and slots. The only rule is that the rate valid at booking time applies. Search has no business rules. It just finds tutors by subject, price, rating and free slots, showing what others publish, with no special ranking. [improvised] Scheduling stops anyone booking a slot that's already taken.

5. **Students:** [improvised] Nothing much. A student registers and adds a card, and Stripe stores the card. We don't verify students. When a charge fails, Billing marks it "unpaid" and emails the student, but today we don't block them.



Round 3 questions (summary): 1 who keeps the tutor earnings balance; 2 refunds vs tutor earnings/clawback; 3 live-lesson rules and change cadence; 4 no-show consequences, completed lesson; 5 pre-auth owner and failure, quote locked or recalculated.

#### round 3
1. **Earnings balance:** Billing credits the tutor's earnings after each charged lesson, and Payouts pays out the accumulated amount every Monday. [improvised] The Payouts team keeps the running total of what we owe each tutor. Billing just tells them "lesson L earned tutor X amount A", and Payouts updates the balance when a payout goes out or fails. A failed payout simply stays on the balance until the next Monday.

2. **Refunds and the tutor's money:** Complaints decides the refund, and Billing reverses the student's charge. [improvised] The tutor loses their earnings for that lesson. If the money was already paid out, Payouts takes it off the tutor's next payout. Whether the tutor's share is taken back is part of the Complaints decision.

3. **The live lesson:** [improvised] The only rule is the 10-minute no-show rule. There's no minimum length: if both joined, the lesson counts as held, even with dropped connections or someone joining late. Disputes about a no-show go through Complaints like any other complaint. The no-show rule has never changed, while the booking and cancellation rules do get tweaked now and then, for business reasons.

4. **No-show consequences:** [improvised] Beyond the charge, there are none today: no strikes and no warnings. If a tutor keeps no-showing, a student files a complaint. A "completed lesson" for the 100-lesson tier is a lesson that ended without a no-show.

5. **Pre-authorisation and the final amount:** The card is pre-authorised right after booking, which is just Stripe holding the money. [improvised] Billing asks Stripe for the hold. If it fails, the booking is cancelled and the student is asked to use another card. The rate valid at booking time applies, and the same calculation is used for the quote, the late fee and the final charge. [improvised] The price quoted at booking is locked in, so a commission tier change in between doesn't alter it.


#### Expert (round 4 answer)
Expert: yes to both. I approve the seven contexts, and please make the context map in CML.

1. Yes, I approve the seven contexts. They match how our teams actually work, and the names are fine.
2. Yes, please make the map in CML. Call the system TutorHub. Please draw it for these four processes: (1) Tutor onboarding, (2) Book and take a lesson, (3) Weekly payout, and (4) Complaint to refund and suspension. At the end I'd also like the canvases for each context.

#### Facilitator -> user (round 5: map hand-offs)
Thanks. The seven contexts are approved, and I'll build the TutorHub map from your four processes.

I walked each process step by step through the contexts. Most hand-offs are clear from what you've told me. For example, Trust & Safety tells Scheduling who may be booked, Scheduling asks Pricing for the quote, Scheduling hands the lesson outcome to Billing, and Billing reports each lesson's tutor earnings to Payouts. Five hand-offs depend on facts I don't have yet. The direction of each arrow follows whose terms the other side has to understand, so I'd rather ask than guess:

1. **Failed card hold (Book and take a lesson):** when Stripe refuses the hold, what happens step by step? Who cancels the booking, and does the student get a chance to use another card first?
2. **Review request (after a lesson):** what starts the review request, and what does the Community team need to know about the lesson? Is it only that the lesson was held, or also no-shows? Search also shows the tutor's rating: where does it get it?
3. **Complaint upheld → Trust & Safety (Complaint to refund and suspension):** what exactly does Trust & Safety receive when a complaint is upheld against a tutor? Does Trust & Safety set what that report must contain (for example a category or severity that its suspension rules use), or does it take whatever Complaints sends?
4. **Investigating a complaint:** what does Complaints look at about the lesson and the money (lesson details, the no-show record, the amount charged), and where does it get them?
5. **Refund and clawback:** once Complaints decides "refund lesson L, amount A, and take back the tutor's share", how does that reach Billing and Payouts? What does each of them need to know?

#### Expert (round 5 answer)
1. **Failed card hold:** [improvised] Billing asks Stripe for the hold right after booking. If Stripe refuses, the student gets an email and has 1 hour to add another card, and Billing then tries again. If the hold still fails, Billing reports "hold failed for lesson L", and the booking is cancelled under the booking team's rules, which frees the slot for others.

2. **Review request:** [improvised] The request goes out only once a lesson has actually been held, meaning it ended and nobody no-showed. Community needs the lesson, the student, the tutor and the date. No-shows don't get reviews. Search gets the rating from Community, the people who own it.

3. **Complaint upheld:** [improvised] Trust & Safety defines what it needs, because its suspension rules depend on it. That's the tutor, the lesson, a category (safety, conduct or teaching quality) and a severity. A safety case means an immediate suspension, and the others depend on severity and history. Complaints has to report upheld complaints in that form; Trust & Safety doesn't take free text.

4. **Investigating a complaint:** [improvised] Complaints looks at the lesson details and the no-show record from the booking/lesson side, and at the amount charged and any fee from Billing. It only reads them and never changes them.

5. **Refund and clawback:** Complaints sends one decision to Billing: "refund lesson L, amount A", plus whether the tutor's share is taken back. Billing reverses the student's charge. [improvised] If the tutor's share is taken back, Billing then reports the reversed tutor earnings for that lesson to Payouts, the same way it reports earnings. Payouts takes the amount off the tutor's balance, or off the next payout if the money was already paid out. Payouts never hears from Complaints directly.


Round 6 questions (neutral re-ask): 1 what Complaints sends T&S, who defined categories/severity, does Complaints change when T&S policy changes; 2 investigation needs live lesson data or snapshot at filing; 3 tax forms/payout account/tutor country; 4 who decides which lessons can be reviewed.

#### Expert (round 6 answer)
1. **Upheld complaint:** [improvised] Today Complaints sends: complaint upheld against tutor X, for lesson L, with a category and a severity. Trust & Safety came up with the categories and severity levels, because their suspension rules use them. Most suspension policy changes are about what Trust & Safety does with a category (for example, suspending sooner for conduct). Complaints doesn't notice those changes. Only when Trust & Safety adds or redefines a category does Complaints have to start classifying differently, and that has happened once so far.

2. **During an investigation:** [improvised] What we know about the lesson when the complaint is filed is enough. Once a lesson is over, its facts don't change. Investigators sometimes re-open the lesson record to double-check, but they don't need it to be live.

3. **Tax forms and payout details:** [improvised] The Payouts team checks the tax forms. The tutor sets up their payout account through Stripe Connect's onboarding, which Payouts runs. The tutor's country comes from their application, which Trust & Safety checked during vetting, and Payouts gets it when a tutor is approved.

4. **Which lessons can be reviewed:** [improvised] The Community team decides that only held lessons get a review request. If that changed one day, for example allowing reviews after a late cancellation, Community would change it. They'd just need to know about late cancellations too.


#### Expert (round 7 answer)
Expert: yes, I approve the TutorHub map. Please go ahead with one canvas per context.

The hand-offs match how our teams work, and the list of answers to re-confirm with the teams is fair. I'll check those with each team.

## Open questions and assumptions
These answers were given on the spot by the expert and are marked "to re-confirm" in the CML and the canvases:
- Payouts keeps the earnings balance.
- Clawbacks go via Billing to Payouts.
- A failed hold cancels the booking.
- The quote is locked at booking.
- The conduct-report form is defined by T&S.
- The country arrives at approval.
- A snapshot is enough for investigations.
- Community owns review eligibility.
- The live lesson has no other rules.
- Search shows the rating from Community.

Also open:
- Tutor Reinstated (a proposed event) and the reinstatement rules.
- Tax-form countries and negative balances.
- Card-hold renewal for lessons booked far ahead.
- Tutor-filed no-show disputes, whose outcomes the four outcomes don't cover.
- Strategic classification (core/supporting/generic) was not discussed.

## Suggested board changes
- Merge the two "Tutor Suspended" stickies (one owner: Trust & Safety).
- Add these events:
  - Complaint Upheld (with category and severity)
  - Refund Decided
  - Complaint Resolved / Rejected
  - Card Pre-authorization Failed
  - Charge Marked Unpaid
  - Lesson Completed
  - Lesson Earnings Reversed
  - Payout Skipped
  - Tutor Reinstated
  - Application Rejected
- Rename these stickies:
  - "Late Cancellation Fee Applied" to "Late Cancellation Fee Incurred"
  - Billing's "Tutor Earnings Credited" to "Lesson Earnings Recorded" (crediting the balance is a Payouts reaction)
- Split No-show Recorded by who no-showed (tutor or student).

## Next steps
- Canvases, via bounded-context-scaffolder (canvases only, no plan or code yet):
  - `docs/bounded-contexts/tutorhub/trustandsafety/canvas.md`
  - `docs/bounded-contexts/tutorhub/scheduling/canvas.md`
  - `docs/bounded-contexts/tutorhub/pricing/canvas.md`
  - `docs/bounded-contexts/tutorhub/billing/canvas.md`
  - `docs/bounded-contexts/tutorhub/payouts/canvas.md`
  - `docs/bounded-contexts/tutorhub/reviews/canvas.md`
  - `docs/bounded-contexts/tutorhub/complaints/canvas.md`
- Re-confirm the on-the-spot answers with each team.
- Turn the vague rules in the canvases into Given/When/Then examples before scaffolding.
