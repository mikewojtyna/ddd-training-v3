# Bounded context discovery: TutorHub (whole platform)

- **Date:** 2026-11-05
- **Mode:** discover
- **Status:** canvases handed off
- **Input:** `.claude/skills/discover-bounded-contexts/evals/files/tutorhub-narrative.md` (typed-up event-storming stickies + quotes)
- **Context map:** `src/main/cml/TutorHub-Context-Map.cml`

## Reading of the input
- **Main timeline:** tutor onboarding (Tutor Applied → Identity Documents Uploaded → Qualifications Verified → Tutor Approved → Tutor Profile Published → Hourly Rate Set → Availability Slots Published), student onboarding (Student Registered → Payment Card Added), booking (Tutor Searched → Lesson Requested → Lesson Price Quoted → Lesson Booked → Card Pre-authorized → Lesson Reminder Sent), lesson (Video Room Opened → Lesson Started → Lesson Ended), money (Student Charged → Platform Commission Deducted → Tutor Earnings Credited → Weekly Payout Sent), feedback (Review Requested → Review Submitted → Tutor Rating Updated).
- **Branches:** Lesson Rescheduled / Lesson Cancelled (→ Late Cancellation Fee Applied); No-show Recorded instead of Lesson Ended; Payout Failed instead of Weekly Payout Sent.
- **Apparently disconnected clusters:** Complaint Filed → Investigated → Refund Issued → Tutor Suspended; Qualification Expired → Tutor Suspended.
- **Ambiguities raised:** the duplicated "Tutor Suspended"; three different clocks (Monday payouts, 14-day reviews, anytime complaints); Stripe and the video provider as external parties.
- **Expert corrections (round 1):** the complaints cluster is connected (refund executed by Billing, suspension decided by Trust & Safety); search has no rules; Billing does charging, commission and crediting while Payouts is a separate team; tutor and student onboarding are independent.

## Heuristic trail
### H1: Pivotal events
| Event | Status | Before | After | Expert's answer |
|---|---|---|---|---|
| Tutor Approved | confirmed | Trust & Safety vets documents and qualifications | tutor's self-service listing (profile, rate, slots) | "Tutor Approved is the real phase change ... approves a tutor once" (r1 Q1) |
| Tutor Profile Published | rejected as pivot | | | "the tutor publishes their own profile" after approval (r1 Q1) |
| Lesson Booked | confirmed | shopping (search, quote) | an appointment with Scheduling's rules | "Before it the student is shopping; after it there's an appointment" (r1 Q2) |
| Lesson Ended / No-show Recorded | confirmed | delivering the lesson | the money side | "Lesson Ended means the lesson is over and the money side can start" (r1 Q3) |
| Tutor Earnings Credited | rejected as pivot | | | hands a balance to a separate clock; became an H2 lane (r1 Q5) |
| Complaint Filed | rejected as pivot | | | "filed anytime"; became an H2 lane (r1 Q8) |

### H2: Swimlanes
| Flow | Verdict | Why |
|---|---|---|
| Weekly payouts | lane | own Monday clock, "nothing to do with lessons", own team, own rules (20 EUR minimum, tax forms, retry next Monday) |
| Reviews and rating | lane | 14-day window, Community team, rating = average of last 50 |
| Complaints | lane | anytime, by students or tutors, own team, own outcomes |
| Qualification expiry → suspension | lane, same owner as vetting | follows the tutor's standing, not any lesson |
| Tutor onboarding vs student onboarding | parallel, independent | different actors (r1 Q8) |
| Lesson Ended vs No-show (student/tutor) | variants | same lesson, endings feed the same charge rule |
| Lesson charge vs late-cancellation charge | variants | "Billing charges it the same way as a lesson" (r1 Q4) |

### H3: Single responsibility
| Duplicated responsibility | Where it appeared | Resolution |
|---|---|---|
| Tutor suspension | after Complaint Investigated and after Qualification Expired; both teams suspended on their own ("it's a mess") | **Trust & Safety** is the single owner of "may this tutor teach?"; Complaints only reports "complaint upheld against tutor X" (r1 Q6) |
| Price calculation | quote, late fee, final charge | one calculation in **Pricing**, rules owned by finance, change independently (r1 Q4) |
| Student block for unpaid lessons | would have been in Billing and Scheduling | **Student Accounts** is the single owner of "may this student book?"; Billing only reports unpaid / settled (r2 Q4) |
| Tutor earnings balance | credited by Billing, paid out and carried forward by Payouts | **Payouts** owns the ledger; Billing publishes per-lesson earnings facts (r3 Q2) |
| The 50% late-cancellation fee | Scheduling, Pricing and Billing (found by the critics) | **Scheduling** owns the 50% with its cancellation policy; Pricing supplies the price base and the commission split; Billing only charges (r4 Q1, Q2) |
| Tutor listing (profile, rate, slots + search) | candidate "Tutor Listings" | no rules and no owner (r2 Q3), so not a context: slots → Scheduling (it prevents double booking, r2 Q1), rate → Pricing (rate valid at booking time), profile → Trust & Safety (r3 Q1), search → read model |
| Scheduling vs lesson delivery | one candidate straddling Lesson Booked → Lesson Ended | split: delivery rules change with the video provider's webhooks, Scheduling's for business reasons, different team (r2 Q2) |
| Billing ↔ Pricing dependency (lesson count) | Pricing counting charged lessons from Billing while Billing uses Pricing | count from Lesson Delivery's outcomes instead (r3 Q3, r4 Q4c) |
| Unpaid lessons: hidden debt-recovery context? | Billing's "unpaid" had an empty far side (critic) | no collection process: student updates card, Billing charges again (r4 Q3); stays in Billing |

## Bounded contexts
| Context | Question it answers | Owns (events, decisions, rules) | Heuristic justification | Classification |
|---|---|---|---|---|
| TutorTrustAndSafety | May this tutor teach? | Tutor Applied, Identity Documents Uploaded, Qualifications Verified (documents per country, which qualifications count), Tutor Approved, Qualification Expired, Tutor Suspended (any reason), Tutor Reinstated; hosts the self-service profile; knows the tutor's country | H1 Tutor Approved; H3 single owner of suspension | core (with Reviews): "students trust our tutors: vetted, with honest ratings" |
| StudentAccounts | May this student book? | Student Registered, Payment Card Added/Updated (card reference; card at Stripe), Student Blocked / Unblocked on unpaid / settled | H3 single owner of the student block | supporting |
| Scheduling | Which lesson happens when, between whom? | Availability Slots Published, Lesson Booked (instant, no double booking, quote from Pricing, no rate → no booking), Lesson Reminder Sent, Lesson Rescheduled (max 2, keeps quote and pre-auth), Lesson Cancelled (free ≥ 24h) / Lesson Cancelled Late (50% of quoted price), booking flagged on failed payment guarantee and cancelled free if not fixed within 24h or before the lesson, suspended tutor's lessons cancelled free | H1 Lesson Booked; H3 own rules, changed for business reasons | supporting |
| LessonDelivery | Did the lesson happen, and who showed up? | Video Room Opened (5 min before), Lesson Started / Ended (provider webhooks), Student No-show / Tutor No-show (10-minute rule) | H1 Lesson Ended; H3 changes when the provider changes, own team | supporting |
| Pricing | What does a lesson cost, and what is our cut? | Hourly Rate Set (validity), Lesson Price Quoted (firm, tier frozen), commission tiers 15% / 10% (rating ≥ 4.8 and ≥ 100 delivered lessons; counting rule), commission split of any charged or refunded amount | H3 one calculation reused three times; finance rules change every few months | supporting ("competitors do similar") |
| Billing | What does the student owe for this lesson, and is it paid? | card pre-authorization, Student Charged (within 1 h; full / none on tutor no-show / late-cancellation share), 2 card retries → Lesson Marked Unpaid, Outstanding Payment Settled, Platform Commission Deducted, Lesson Earnings Recorded / Reversed, Refund Issued (amount from Complaints) | H1 far side of Lesson Ended; own timing and retry rules | supporting |
| Payouts | When and how much do we pay each tutor? | tutor earnings balance (ledger), Weekly Payout Sent (Monday, Stripe Connect, 20 EUR minimum, tax forms by country), Payout Failed → retried next Monday, negative balance carried forward, Connect account set-up | H2 own Monday clock and team | supporting |
| Reviews | How good is this tutor in students' eyes? | Review Requested, Review Submitted (1–5 stars + comment, up to 14 days), Tutor Rating Updated (average of last 50) | H2 14-day lane, Community team | core |
| Complaints | Was something wrong, and what is the remedy? | Complaint Filed (anytime, students or tutors), Complaint Investigated, Complaint Rejected / Refund Decided (lesson, amount, may be partial) / Complaint Upheld Against Tutor | H2 anytime lane, own team; H3 decides remedies only | supporting |

**Not contexts:** tutor search (a read model over facts published by Trust & Safety (profile, suspended), Pricing (rate), Reviews (rating) and Scheduling (free slots); no rules, r1 Q8 / r3 Q1); notifications (reminders, unpaid and payout-failed emails: a mechanism). **External:** Stripe, video provider.

## Critic log
| Subject | Round | Verdict | Blocking points / resolution |
|---|---|---|---|
| Reviews | round 1 | AGREE | one question, own 14-day lane, core; notes: who may review after tutor no-show, review moderation, rename to Tutor Rating Recalculated |
| Payouts | round 1 | AGREE | sole owner of balance ledger, own Monday lane; notes: who owns tax forms / Connect account, held payouts for suspended tutors, missing events (Earnings Reversed, Payout Skipped/Held) |
| Tutor Trust & Safety | round 1 | AGREE | single owner of "may this tutor teach?"; straddling Tutor Approved acceptable (profile side has no rules); notes: missing Application Rejected, Qualification Renewed, Tutor Reinstated; say profile hosting is convenience |
| Student Accounts | round 1 | AGREE | single owner of "may this student book?", small supporting; notes: add Student Blocked/Unblocked, Outstanding Charge Settled, Payment Card Updated; card reference owned here, not in Billing |
| Complaints | round 1 | AGREE | own lane and team, publishes outcome only; questions: who sets refund amount, complaints against students; add outcome events |
| Lesson Delivery | round 1 | AGREE | own rules/team/reason to change, wraps video provider via ACL; notes: publish facts not the tier count, split No-show into Student/Tutor No-show |
| Pricing | round 1 | DISAGREE | 50% late-cancellation rule owned by Pricing, Scheduling and Billing; questions: who changes the 50%, commission on late fees |
| Scheduling | round 1 | DISAGREE | same 50% late-fee ownership duplication with Pricing; Scheduling should own only the lateness decision; notes: add Pre-authorization Failed/Succeeded, cancellation reasons |
| Billing | round 1 | DISAGREE | 50% late fee has three owners; unpaid lesson has empty far side (how it gets collected); rename Tutor Earnings Credited, pre-auth failure as Billing fact, Scheduling flags booking |
| Pricing | round 2 | AGREE | 50% now owned once (Scheduling); counting rule and commission split in Pricing; note: late-fee split uses tier frozen in the original quote |
| Scheduling | round 2 | AGREE | lateness + 50% owned once; notes: rename Late Cancellation Fee Applied to Lesson Cancelled Late, explicit Booking Flagged, fold Lesson Requested into booking |
| Billing | round 2 | AGREE | 50% gone, unpaid far side closed (no collection process); notes: partial refund split via Pricing, add Card Pre-authorized after fix, watch Billing<->Scheduling direction on the map |
| Map + P4 Weekly payout | round 1 | DISAGREE | tax-form rule needs tutor country/tax-form status with no relationship, assumption unmarked; ask who collects tax forms / Connect account |
| Map + P1 Tutor onboarding | round 1 | AGREE | every step owned, TutorStanding carries hand-off; question: must a rate exist before slots are bookable (would reopen Pricing->Scheduling) |
| Map + P6 Complaint | round 1 | AGREE | every hand-off carried, no cycle; note: mark ComplaintReporting/Refunds directions as unconfirmed facilitator choices |
| Map + P2 Book and take a lesson | round 1 | DISAGREE | opaque quote reference (no Pricing->Scheduling) is an unconfirmed assumption that hides a cycle; ask whether Scheduling must check the quote; phrase pre-auth feedback in Scheduling's API language |
| Map v3 | (facilitator) | n/a | after expert round 6: Pricing->Scheduling restored, cycle kept and justified, TutorCountry added |
| Map v3 + all processes P1-P8 | final pass (round 2) | AGREE | every step owned, no unused relationship, kept indirect cycle adequately justified; notes: fix 'core' wording, rename DeliveredLessonCount, mark reschedule-keeps-quote and pre-auth feedback direction as unconfirmed |

Critic rounds that changed something: the per-context round 1 on Pricing/Scheduling/Billing (50% ownership, the unpaid far side → expert round 4), the map critic on P2 (refused the v2 opaque-quote assumption that hid a cycle → expert round 6, cycle restored and justified), the map critic on P4 (tax forms / tutor country → new TutorCountry relationship).

## Context map
### Process walkthroughs
#### P1 Tutor onboarding
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Tutor Applied, Identity Documents Uploaded, Qualifications Verified, Tutor Approved | TutorTrustAndSafety | — |
| Tutor Profile Published | TutorTrustAndSafety | read by search (not a context) |
| Hourly Rate Set | Pricing | — |
| Availability Slots Published (only approved tutors) | Scheduling | TutorTrustAndSafety → Scheduling : TutorStanding |
| Payout account and tax forms | Payouts | TutorTrustAndSafety → Payouts : TutorCountry; Stripe → Payouts : ConnectPayouts (CF) |

#### P2 Book and take a lesson
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Tutor Searched | search read model | — |
| Lesson Price Quoted | Pricing | Pricing → Scheduling : LessonQuotation |
| Lesson Booked (not blocked, not suspended) | Scheduling | StudentAccounts → Scheduling : StudentStanding; TutorTrustAndSafety → Scheduling : TutorStanding |
| Card Pre-authorized / payment guarantee failed → booking flagged | Billing / Scheduling | Scheduling → Billing : BookedLessons (Billing reports in Scheduling's terms); StudentAccounts → Billing : StudentPaymentStanding; Stripe → Billing : CardPayments (CF) |
| Lesson Reminder Sent | Scheduling | notification mechanism |
| Video Room Opened, Lesson Started, Lesson Ended / No-show | LessonDelivery | Scheduling → LessonDelivery : LessonAppointments; VideoProvider → LessonDelivery : VideoSessions (ACL) |
| Student Charged, Platform Commission Deducted | Billing | LessonDelivery → Billing : LessonOutcomes; Pricing → Billing : CommissionSplit |
| Lesson Earnings Recorded | Billing → Payouts | Billing → Payouts : LessonEarnings |
| Review Requested | Reviews | LessonDelivery → Reviews : DeliveredLessons |

#### P3 Cancel or reschedule
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Lesson Rescheduled (max 2, keeps quote and pre-auth) | Scheduling | Scheduling → LessonDelivery : LessonAppointments |
| Lesson Cancelled (free) → pre-auth released | Scheduling / Billing | Scheduling → Billing : BookedLessons |
| Lesson Cancelled Late (50%) → charged, split, earnings | Scheduling / Billing / Payouts | BookedLessons; CommissionSplit; LessonEarnings |

#### P4 Weekly payout
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Lesson Earnings Recorded / Reversed booked into the balance | Payouts | Billing → Payouts : LessonEarnings |
| Tax forms required? | Payouts | TutorTrustAndSafety → Payouts : TutorCountry |
| Weekly Payout Sent (Monday, ≥ 20 EUR) / Payout Failed → next Monday | Payouts | Stripe → Payouts : ConnectPayouts (CF) |

#### P5 Review → rating → tier
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Review Requested / Submitted, Tutor Rating Updated | Reviews | LessonDelivery → Reviews : DeliveredLessons |
| Commission tier (rating ≥ 4.8, ≥ 100 delivered lessons) | Pricing | Reviews → Pricing : TutorRating; LessonDelivery → Pricing : DeliveredLessonOutcomes |

#### P6 Complaint → refund and suspension
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Complaint Filed about lesson L, Investigated | Complaints | Scheduling → Complaints : LessonDetails |
| Refund Decided → Refund Issued, Lesson Earnings Reversed | Complaints → Billing → Payouts | Billing → Complaints : Refunds; Pricing → Billing : CommissionSplit; Billing → Payouts : LessonEarnings |
| Complaint Upheld Against Tutor → Tutor Suspended | Complaints → TutorTrustAndSafety | TutorTrustAndSafety → Complaints : UpheldComplaintIntake |
| Profile hidden, future lessons cancelled free, pre-auths released | search / Scheduling / Billing | TutorStanding; BookedLessons |

#### P7 Qualification expiry
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Qualification Expired → Tutor Suspended | TutorTrustAndSafety | — |
| Lessons cancelled free, pre-auths released; payouts continue | Scheduling / Billing | TutorStanding; BookedLessons; (deliberately no TutorStanding → Payouts, r4 Q4a) |

#### P8 Unpaid lesson
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| 2 retries → Lesson Marked Unpaid → Student Blocked | Billing → StudentAccounts | StudentAccounts → Billing : StudentPaymentStanding (Billing reports in Student Accounts' terms) |
| Blocked student can't book | Scheduling | StudentAccounts → Scheduling : StudentStanding |
| Payment Card Updated → re-charge → Outstanding Payment Settled → Student Unblocked | StudentAccounts / Billing | StudentPaymentStanding; Stripe → StudentAccounts : CardRegistration (CF) |

### Relationships
| Upstream | Downstream | Patterns | Messages | Justification (rule) |
|---|---|---|---|---|
| Stripe | Billing | U OHS PL / D CF | card pre-auth, charges, refunds | integration rule 1: Stripe is an industry standard (r5 Q4) |
| Stripe | Payouts | U OHS PL / D CF | Connect payouts | same |
| Stripe | StudentAccounts | U OHS PL / D CF | card registration | same |
| VideoProvider | LessonDelivery | U OHS PL / D ACL | room / join / leave webhooks | integration rule 3: messy, changed twice this year (r2 Q2, r5 Q4) |
| TutorTrustAndSafety | Scheduling | S PL / C ACL | Tutor Approved / Suspended / Reinstated | relationship rule 5 (internal teams, r5 Q3) + integration rules 3, 5 |
| TutorTrustAndSafety | Complaints | S PL / C ACL | "complaint upheld against tutor X" intake | Complaints speaks T&S's terms (r6 Q3); avoids cycle T&S → Scheduling → Complaints → T&S |
| TutorTrustAndSafety | Payouts | S PL / C ACL | tutor's country | r6 Q2 |
| StudentAccounts | Scheduling | S PL / C ACL | Student Blocked / Unblocked | r2 Q4 |
| StudentAccounts | Billing | S PL / C ACL | card reference, Payment Card Updated; unpaid / settled reported in SA's terms | single owner of the block; avoids a direct cycle |
| Pricing | Scheduling | S PL / C ACL | Lesson Price Quoted | r6 Q1, Q4 |
| Pricing | Billing | S PL / C ACL | quoted price, commission split | r3 Q3, r4 Q2 |
| Reviews | Pricing | S PL / C ACL | Tutor Rating Updated | r1 Q7 |
| Scheduling | LessonDelivery | S PL / C ACL | booked / rescheduled / cancelled lessons | r2 Q2 |
| Scheduling | Billing | S PL / C ACL | Lesson Booked, Lesson Cancelled Late, free cancellations; payment guarantee failed/confirmed reported in Scheduling's terms | r2 Q5, r4 Q1, r7 Q2 |
| Scheduling | Complaints | S PL / C ACL | lesson details | P6 needs who taught whom |
| LessonDelivery | Billing | S PL / C ACL | Lesson Ended, Student / Tutor No-show | r1 Q3 |
| LessonDelivery | Reviews | S PL / C ACL | Lesson Ended | review request |
| LessonDelivery | Pricing | S PL / C ACL | lesson outcomes for the tier count | r3 Q3, r4 Q4c |
| Billing | Payouts | S PL / C ACL | Lesson Earnings Recorded / Reversed | r3 Q2 |
| Billing | Complaints | S PL / C ACL | "refund lesson L, amount A" | Complaints speaks Billing's terms (r6 Q3) |

No Shared Kernel (expert r5 Q3: no shared database or library), no Partnership, no Separate Ways, no direct U/D cycle.

### Validator output
`scripts/validate_cml.sh src/main/cml/TutorHub-Context-Map.cml` (final, exit 0):

```
RELATIONSHIPS:
  Stripe [OHS, PL] -> Billing [CF] : CardPayments
RULE conformist-check: Billing conforms to Stripe. OK only if Stripe's language is well established; otherwise it pollutes Billing.
  Stripe [OHS, PL] -> Payouts [CF] : ConnectPayouts
RULE conformist-check: Payouts conforms to Stripe. OK only if Stripe's language is well established; otherwise it pollutes Payouts.
  Stripe [OHS, PL] -> StudentAccounts [CF] : CardRegistration
RULE conformist-check: StudentAccounts conforms to Stripe. OK only if Stripe's language is well established; otherwise it pollutes StudentAccounts.
  VideoProvider [OHS, PL] -> LessonDelivery [ACL] : VideoSessions
  TutorTrustAndSafety [PL] -> Scheduling [ACL] (customer/supplier) : TutorStanding
  TutorTrustAndSafety [PL] -> Complaints [ACL] (customer/supplier) : UpheldComplaintIntake
  TutorTrustAndSafety [PL] -> Payouts [ACL] (customer/supplier) : TutorCountry
  StudentAccounts [PL] -> Scheduling [ACL] (customer/supplier) : StudentStanding
  StudentAccounts [PL] -> Billing [ACL] (customer/supplier) : StudentPaymentStanding
  Pricing [PL] -> Scheduling [ACL] (customer/supplier) : LessonQuotation
  Pricing [PL] -> Billing [ACL] (customer/supplier) : CommissionSplit
  Reviews [PL] -> Pricing [ACL] (customer/supplier) : TutorRating
  Scheduling [PL] -> LessonDelivery [ACL] (customer/supplier) : LessonAppointments
  Scheduling [PL] -> Billing [ACL] (customer/supplier) : BookedLessons
  Scheduling [PL] -> Complaints [ACL] (customer/supplier) : LessonDetails
  LessonDelivery [PL] -> Billing [ACL] (customer/supplier) : LessonOutcomes
  LessonDelivery [PL] -> Reviews [ACL] (customer/supplier) : DeliveredLessons
  LessonDelivery [PL] -> Pricing [ACL] (customer/supplier) : DeliveredLessonOutcomes
  Billing [PL] -> Payouts [ACL] (customer/supplier) : LessonEarnings
  Billing [PL] -> Complaints [ACL] (customer/supplier) : Refunds
RULE ud-cycle-indirect: LessonDelivery -> Pricing -> Scheduling -> LessonDelivery. Check whether each context really depends on the next one's model, or whether one arrow is only event flow drawn the wrong way.
RULE ud-cycle-indirect: LessonDelivery -> Reviews -> Pricing -> Scheduling -> LessonDelivery. Check whether each context really depends on the next one's model, or whether one arrow is only event flow drawn the wrong way.
VALID: 11 bounded contexts, 20 relationships, 0 errors
```

Plus 15 Context Mapper warnings "Are you sure you need an ANTICORRUPTION LAYER here? ... Customer-Supplier relationship" (one per internal relationship).

How each RULE / warning was resolved:
- **conformist-check ×3 (Stripe):** justified. Stripe's API is an industry standard and the expert is happy to follow it (r5 Q4). Integration rule 1.
- **ud-cycle-indirect ×2** (Pricing → Scheduling → LessonDelivery (→ Reviews) → Pricing): kept and justified in a CML comment. Every arrow is a real model dependency on a different concept (quote; appointment; lesson outcomes / rating), confirmed by the expert (r6 Q1, Q4; r1 Q7; r4 Q4c). It is the business's own slow feedback loop (lesson history → commission tier, frozen into each quote). Alternatives rejected: counting from Billing (allowed by the expert, r6 Q1) creates a direct cycle with CommissionSplit; making Lesson Delivery and Reviews report into a Pricing intake only flips the arrow cosmetically. History: v1 had the same cycle; v2 hid it behind an unconfirmed "opaque quote reference" assumption, which the P2 critic rejected; the expert then confirmed Scheduling needs Pricing's quote (v3). The final-pass critic agreed the justification is adequate.
- **ACL-in-Customer/Supplier warnings ×15:** kept deliberately. The trainer's integration rule 3 makes an ACL downstream the default; with internal teams it keeps each team's model safe from a supplier's changes.

## Q&A with the domain expert
Verbatim questions and answers (7 rounds) are in the session transcript. Summary:
1. **Round 1 (reading + H1):** Tutor Approved is the pivot; Lesson Booked is the commitment (24h / 50% / 2 reschedules); Lesson Ended starts the money side; no-show either side (10 min), tutor no-show → no charge; one pricing calculation (15% / 10% commission), finance-owned; earnings build a balance, Monday payouts (20 EUR minimum, tax forms, retry); one owner of suspension: Trust & Safety; rating feeds Pricing and search; complaints connected via Billing and Trust & Safety.
2. **Round 2 (H2/H3):** slots are the tutor's offer, Scheduling prevents double booking; delivery is a separate team and changes with the provider; listing is self-service without rules; one owner of the student block; pre-auth in Billing, unfixed failures cancelled free by Scheduling; refunds reverse tutor share and commission, negative balance carried; suspension hides the profile and cancels lessons.
3. **Round 3:** profile with the tutor's account, search a read model; Payouts owns the balance; price fixed at booking incl. tier; lesson count from Lesson Delivery; instant booking; core = Trust & Safety + Reviews.
4. **Round 4 (critic findings):** the 50% is Scheduling's; commission taken from late fees; no collection process; suspended tutors still paid; Complaints decides refund amounts (partial); student no-shows count.
5. **Round 5:** nine contexts approved; system name TutorHub; key processes P1, P2, P4, P6; all internal teams who plan together, no shared database or library; Stripe conformist OK, video provider at arm's length; canvases wanted.
6. **Round 6 (map critics):** Scheduling gets the quote from Pricing and refuses slots without a rate; Payouts collects tax forms and sets up Connect, country from Trust & Safety; Complaints speaks Trust & Safety's and Billing's terms.
7. **Round 7:** map approved; reschedule keeps quote and pre-auth; Billing reports "payment guarantee failed for booking B" in Scheduling's terms; canvases only, no code.

## Open questions and assumptions
Many expert answers were marked as improvised by the expert (they go beyond what the board recorded); they are treated as the expert's decisions but are worth re-checking with the teams concerned:
- Who may review a tutor after a tutor no-show? Can reviews be moderated or removed, and by whom (Reviews)?
- Can a complaint be upheld against a student, and would that block them (Complaints → Student Accounts)?
- Does Trust & Safety always suspend after an upheld complaint, or weigh the case (warning, temporary suspension)? What lifts a complaint-based suspension?
- Investigation rules in Complaints (SLA, escalation, reopening) are unknown.
- Does a tutor cancellation or reschedule have its own fee or limit?
- Refund cap: may a refund exceed the amount charged? (Billing's rule if so.)

No unconfirmed assumptions remain in the CML.

## Suggested board changes
- Merge the two "Tutor Suspended" stickies into one event with a reason (qualification expired, complaint upheld).
- Rename "Late Cancellation Fee Applied" → **Lesson Cancelled Late** (Scheduling); the charge is Billing's "Student Charged".
- Rename "Tutor Earnings Credited" → **Lesson Earnings Recorded**; add **Lesson Earnings Reversed**.
- Split "No-show Recorded" into **Student No-show Recorded** / **Tutor No-show Recorded**.
- Fold "Lesson Requested" into **Lesson Booked** (booking is instant).
- Add: Tutor Application Rejected, Qualification Renewed, Tutor Reinstated; Student Blocked / Unblocked, Payment Card Updated; Booking Payment Guarantee Failed / Confirmed (booking flagged / cleared); Card Pre-authorization Failed; Lesson Marked Unpaid, Outstanding Payment Settled; Complaint Rejected, Refund Decided, Complaint Upheld Against Tutor; Payout Skipped (below minimum) / Payout Held (tax form missing).
- Consider "Tutor Rating Recalculated" instead of "Tutor Rating Updated" (it's derived).

## Next steps
- Canvases (via bounded-context-scaffolder, canvases only, no plan or code yet), for review:
  - `docs/bounded-contexts/tutorhub/tutortrustandsafety/canvas.md`
  - `docs/bounded-contexts/tutorhub/studentaccounts/canvas.md`
  - `docs/bounded-contexts/tutorhub/scheduling/canvas.md`
  - `docs/bounded-contexts/tutorhub/lessondelivery/canvas.md`
  - `docs/bounded-contexts/tutorhub/pricing/canvas.md`
  - `docs/bounded-contexts/tutorhub/billing/canvas.md`
  - `docs/bounded-contexts/tutorhub/payouts/canvas.md`
  - `docs/bounded-contexts/tutorhub/reviews/canvas.md`
  - `docs/bounded-contexts/tutorhub/complaints/canvas.md`
- After review, invoke bounded-context-scaffolder per canvas to write its plan (it stops for review before code).
