# Bounded context discovery: TutorHub (event storming)

- **Date:** 2026-11-05
- **Mode:** discover
- **Status:** canvases handed off
- **Input:** `.claude/skills/discover-bounded-contexts/evals/files/tutorhub-narrative.md` (typed-up event-storming stickies + quotes)
- **Context map:** `src/main/cml/TutorHub-Context-Map.cml`

## Reading of the input
- **Main timeline:** tutor onboarding (Applied → Documents Uploaded → Qualifications Verified → Approved → Profile Published → Hourly Rate Set → Availability Slots Published) → student onboarding (Registered → Payment Card Added) → booking (Tutor Searched → Lesson Requested → Price Quoted → Lesson Booked → Card Pre-authorized) → lesson (Reminder → Video Room Opened → Started → Ended) → money (Student Charged → Commission Deducted → Tutor Earnings Credited).
- **Branches:** Rescheduled / Cancelled / Late Cancellation Fee Applied; No-show Recorded.
- **Own clocks:** weekly payouts (Mondays), reviews (≤14 days), complaints (any time).
- **Disconnected cluster:** Qualification Expired → Tutor Suspended.
- **Ambiguities raised:** the duplicated "Tutor Suspended"; whether "Late Cancellation Fee Applied" is a charge; whose no-show it is; Stripe and the video provider as external parties.
- **Expert's corrections (R1):** a no-show can be either side (nobody joined within 10 min, detected by us from the video provider's webhooks); the room opens 5 min before; Started/Ended also come from webhooks.

## Heuristic trail
### H1: Pivotal events
| Event | Status | Before | After | Expert's answer |
|---|---|---|---|---|
| Tutor Approved | confirmed | Trust & Safety vetting (documents per country, qualifications) | tutor manages profile, rate, availability | R1-1 "Tutor Approved is the real change… approved only once" |
| Tutor Profile Published | rejected as pivot | | | R1-1 (Approved is the real change) |
| Lesson Booked | confirmed | student shopping (search, quote) | an appointment with cancellation rules | R1-2 "the real commitment" |
| Lesson Ended / No-show / Late cancellation | confirmed hand-off | Scheduling delivers the lesson | Billing builds the charge | R1-3 "The Billing team builds the final charge after a lesson ends, a no-show or a late cancellation" |
| Tutor Earnings Credited | confirmed hand-off | per-lesson money (Billing) | Monday payout clock (Payouts) | R1-5, R4-3 |

### H2: Swimlanes
| Flow | Verdict | Why |
|---|---|---|
| Weekly payouts | lane | Monday clock "no matter what happens with lessons", own team and rules (R1-5) |
| Reviews | lane | up to 14 days later, Community team (R2-3) |
| Complaints | lane | any time, Complaints team (R2-4) |
| Qualification expiry | not a separate lane | own clock but feeds the same decision as complaint suspensions (may this tutor teach?) → Trust & Safety |
| Lesson delivery (webhooks) | not a lane | Scheduling team owns it and the no-show rule (R2-1) |
| Student vs tutor no-show | variants | same flow, different billing outcome |
| Search | not a context | "Search has no rules of its own" (R2-5) |

### H3: Single responsibility
| Duplicated responsibility | Where it appeared | Resolution |
|---|---|---|
| Suspending a tutor | Trust & Safety and Complaints both suspended (a tutor suspended by Complaints was re-activated by T&S) | one owner, Trust & Safety; Complaints only publishes "complaint upheld" (R1-4, R4-5) |
| Price calculation | quote, late fee, final charge | one Pricing owner, finance's rules change independently (R1-3, R2-2) |
| Refund | Complaints decides, Billing executes | decision owned by Complaints, execution by Billing (R2-4) |
| Is this slot bookable? | Tutor Listing (unbooked) and Scheduling (booked) | critic finding; Scheduling team owns slot rules, booking wins (R4-1); Tutor Listing dissolved, profile and rate have no team or rules (R4-2) |
| Tutor's accumulated earnings | Billing credits and Payouts accumulates | critic finding; Payouts holds the balance (R4-3), Billing publishes per-lesson facts |
| Can this student book? | unpaid check | Billing owns "unpaid" (R3-1), the booking rule belongs to Scheduling (R7-2); no student blocking over complaints (R4-5); student login has no rules → not a context |
| Late-cancellation rule | Scheduling vs Pricing | Scheduling team owns 24h/50% (R3-4); Pricing calculates the price |

## Bounded contexts
| Context | Question it answers | Owns (events, decisions, rules) | Heuristic justification | Classification |
|---|---|---|---|---|
| TrustAndSafety | May this tutor teach? | Tutor Applied, Identity Documents Uploaded, Qualifications Verified, Tutor Approved, Qualification Expired, Qualification Renewed*, Tutor Suspended (reason), Tutor Reinstated*. Documents per country; which qualifications count; approval once; every upheld complaint suspends; expiry suspension until renewed; complaint suspension ends only after T&S review | H1 pivot Tutor Approved; H3 single owner of suspension | not discussed |
| Scheduling | When can this tutor be booked, and what happened to each booked lesson? | Tutor Profile Published, Hourly Rate Set, Availability Slots Published, Lesson Requested, Lesson Price Quoted, Lesson Booked, Booking Refused*, Lesson Reminder Sent, Lesson Rescheduled, Lesson Cancelled, Lesson Cancelled by Platform*, Late Cancellation Fee Due*, Video Room Opened, Lesson Started, Lesson Ended / Lesson Held*, Student No-show*, Tutor No-show*. Book ≥2h before, booking wins, max 2 reschedules, free cancel until 24h then 50% fee due, 10-min no-show, unpaid students can't book, suspended tutor hidden and lessons cancelled free | H1 pivot Lesson Booked (shopping side has no own rules); H3: one team owns slots, booking, delivery (R2-1, R4-1) | not discussed |
| Pricing | What does a lesson cost, and what is our commission? | rate × duration + commission; 15%, 10% for rating ≥ 4.8 and ≥ 100 held lessons; tier worked out fresh per quote | H3: one calculation used three times; finance changes it independently (R1-3, R2-2) | not discussed |
| Billing | What is charged for this lesson, and is it paid? | Payment Card Added, Card Pre-authorized, Student Charged, Charge Failed*, Lesson Marked Unpaid*, Unpaid Lifted*, Platform Commission Deducted, Tutor Earnings Credited, Refund Issued, Tutor Earnings Reversed*. Charge within 1h, 2 retries, then unpaid + email; tutor no-show → no charge; student no-show → full price | H1 hand-off after Lesson Ended / No-show / Fee Due; own rules (R1-3) | not discussed |
| Payouts | What do we owe each tutor, and what do we pay them this Monday? | Weekly Payout Sent, Payout Deferred*, Payout Failed. Balance, 20 EUR minimum, retry next Monday, tax forms (checked by Payouts), clawback refund minus commission, paid even when suspended | H2 Monday lane; own team (R1-5, R4-3, R8-2, R8-3) | not discussed |
| Reviews | How do students rate this tutor? | Review Requested, Review Submitted, Tutor Rating Updated. Held lessons only, ≤14 days, 1-5 + comment, average of last 50 | H2 own clock; Community team (R2-3, R7-4) | not discussed |
| Complaints | Was this complaint justified, and what is the remedy? | Complaint Filed, Complaint Investigated, Complaint Rejected*, Refund Decided*, Complaint Upheld*. Filed by students or tutors any time; outcomes rejected / refund / upheld / both; decides refund amount; never suspends; no student blocking | H2 any-time lane; H3 suspension moved to T&S (R2-4, R4-5) | not discussed |

`*` = suggested new board event. External, not designed: **Stripe** (cards, charges, refunds, Connect payouts), **VideoProvider** (rooms, webhooks). Not contexts: search (read view), student login.

## Critic log
| Subject | Round | Verdict | Blocking points / resolution |
|---|---|---|---|
| Reviews | r1 | AGREE | own question, own 14-day clock and team; note: Scheduling should publish a clear held-lesson fact |
| Complaints | r1 | AGREE | own lane and question; non-blocking Qs: complaint against a student -> student blocking owner? refund cap/clawback from tutor |
| Payouts | r1 | AGREE | own Monday clock and rules; non-blocking Qs: who keeps the earnings balance, refund clawback, tax-form owner, hold on suspension |
| Pricing | r1 | AGREE | finance-owned rules changing independently; reads rate/rating/held count, doesn't own them; non-blocking: where the booking-time snapshot lives |
| Trust & Safety | r1 | AGREE | single owner of may-teach, onboarding+suspension kept together by H3 and expert's explicit wish; add Qualification Renewed; non-blocking: how a complaint suspension ends |
| Tutor Listing | r1 | DISAGREE | 'is this slot bookable?' owned twice (Listing unbooked slots + Scheduling booked slots); fix: whole slot lifecycle to Scheduling; Qs: availability rules/race owner, does profile/rate side have own rules/team |
| Scheduling | r1 | AGREE | owns appointment+delivery; double-booking decision must be Scheduling's alone; ACL to volatile video provider; add Lesson Held, Booking Refused |
| Billing | r1 | DISAGREE | tutor earnings balance has no single owner (Billing credits, Payouts accumulates/carries over); Qs: who holds balance, refund clawback after payout, pre-auth release |
| Billing | r2 | AGREE | balance has one owner (Payouts); Billing computes refund-minus-commission and publishes Tutor Earnings Reversed for every refund; open: pre-auth release |
| Payouts | r2 | AGREE | owns balance, minimum, clawback; open: payouts held while suspended? tax-form owner, negative balance |
| Tutor Listing (dissolution) | r2 | AGREE | dissolution resolves double slot ownership; profile/rate as inputs in Scheduling acceptable, revisit if moderation rules appear |
| Scheduling | r2 | AGREE | absorbing the offer gives 'is this slot bookable' one owner; open: where the booking-time quote snapshot lives (Scheduling storing Pricing's quote is natural) |
| Map v1 + P1 onboarding | r1 | DISAGREE | P1 walk clean; ChargeCalculation (Pricing->Billing) conflicts with booking-time tier vs fresh tier; Qs: does Billing use booked quote or re-ask Pricing; tax forms owner; payouts held during suspension |
| Map v1 + P3 weekly payout | r1 | DISAGREE | P3 walk clean; open assumptions (tax forms, booked quote, no suspension hold) not marked in CML; inline cycle reasons; same 3 expert Qs |
| Map v1 + P4 complaint | r1 | DISAGREE | P4 walk clean, cycles accepted; ChargeCalculation rests on unmarked assumption (which commission Billing uses); inline cycle reasons |
| Map v1 + P2 booking | r1 | DISAGREE | P2 walk clean, cycles accepted; booked-quote vs re-calc assumption unmarked and unasked (decides ChargeCalculation and a cycle); inline cycle reasons |
| Map v2 + P3 weekly payout | r2 | AGREE | tax forms closed (R8-2), no T&S link (R8-3), dropping ChargeCalculation right; note: mention combination cycles in header |
| Map v2 + P1 onboarding | r2 | AGREE | ChargeCalculation drop and Payouts answers resolve r1; note: Pricing statement should say the booked quote freezes the tier |
| Map v2 + P4 complaint | r2 | AGREE | refund-minus-commission sourced from booked commission; note: Billing reads Pricing's 'commission' second-hand via Scheduling |
| Map v2 + P2 booking | r2 | AGREE | all steps owned; note: say Scheduling republishes the quote in its booking record |
| Map v2 final pass (P1-P4) | r1 | AGREE | all steps owned, cross-process interactions fine, RULE lines justified; note: booked quote on Lesson Booked is a design decision, refresh stale candidate text |

## Context map
### Process walkthroughs
Processes the expert judges the map by (R6-1). Reviews and qualification expiry happen but aren't judged.

#### P1 Tutor onboarding
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Tutor Applied … Tutor Approved | TrustAndSafety | |
| Tutor Profile Published, Hourly Rate Set, Availability Slots Published | Scheduling | TrustAndSafety → Scheduling : TutorEligibility (Tutor Approved) |
| Payout account and tax forms | Payouts | Stripe → Payouts : ConnectPayouts (R8-2) |

#### P2 Booking and taking a lesson
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Tutor Searched | (search read view) | — |
| Lesson Requested, Lesson Price Quoted | Scheduling | Pricing → Scheduling : LessonPriceQuote (rate, duration, held count; R6-2, R7-1); Reviews → Pricing : TutorRating |
| Lesson Booked (checks) | Scheduling | TrustAndSafety → Scheduling : TutorEligibility; Billing → Scheduling : StudentPaymentStanding (R7-2) |
| Card Pre-authorized | Billing | Scheduling → Billing : LessonOutcomes (Lesson Booked + booked quote); Stripe → Billing : CardPayments |
| Reminder, Rescheduled | Scheduling | none (R8-4) |
| Cancelled / Late Cancellation Fee Due | Scheduling | Scheduling → Billing : LessonOutcomes |
| Room, Started, Ended, No-shows | Scheduling | VideoProvider → Scheduling : VideoRoomWebhooks (ACL); Scheduling → Billing : LessonOutcomes |
| Student Charged, retries, unpaid | Billing | Stripe → Billing : CardPayments; Billing → Scheduling : StudentPaymentStanding |
| Commission Deducted, Tutor Earnings Credited | Billing | Billing → Payouts : TutorEarnings |
| Lesson Held → review → rating | Reviews | Scheduling → Reviews : HeldLessons; feeds the next quote's tier |

#### P3 Weekly payout
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Earnings Credited / Reversed accumulate | Payouts | Billing → Payouts : TutorEarnings |
| Monday run, 20 EUR minimum, tax forms | Payouts | — |
| Payout Sent / Failed, retry next Monday | Payouts | Stripe → Payouts : ConnectPayouts |

#### P4 Complaint → refund and suspension
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Complaint Filed (lesson details, what was charged) | Complaints | Scheduling → Complaints : LessonDetails; Billing → Complaints : ChargeDetails (R6-4) |
| Refund Decided → Refund Issued | Complaints → Billing | Complaints → Billing : RefundDecisions; Stripe → Billing : CardPayments |
| Tutor Earnings Reversed → clawback | Billing → Payouts | Billing → Payouts : TutorEarnings (R4-4) |
| Complaint Upheld → Tutor Suspended | Complaints → TrustAndSafety | Complaints → TrustAndSafety : UpheldComplaints |
| Tutor hidden, booked lessons cancelled free, hold released | Scheduling → Billing | TrustAndSafety → Scheduling : TutorEligibility; Scheduling → Billing : LessonOutcomes (Lesson Cancelled by Platform) |
| Tutor Reinstated (after T&S review) | TrustAndSafety | TrustAndSafety → Scheduling : TutorEligibility |

### Relationships
| Upstream | Downstream | Patterns | Messages | Justification (rule) |
|---|---|---|---|---|
| Stripe | Billing | OHS, PL → CF | cards, pre-auth, charges, refunds | integration rule 1: industry standard (R6-5) |
| Stripe | Payouts | OHS, PL → CF | Connect payouts, country | integration rule 1 (R6-5, R8-2) |
| VideoProvider | Scheduling | U → ACL | room set-up, webhooks | integration rule 3; "wouldn't let it leak" (R6-5) |
| TrustAndSafety | Scheduling | C/S, PL → ACL | Tutor Approved / Suspended (reason) / Reinstated | T&S decides, Scheduling reacts (R1-4) |
| Pricing | Scheduling | OHS, PL → ACL | quote(rate, duration, held count) | Scheduling asks Pricing (R6-2, R7-1); plain U/D because CML forbids OHS in C/S |
| Reviews | Pricing | C/S, PL → ACL | Tutor Rating Updated | Pricing reads the rating (R6-2) |
| Scheduling | Billing | C/S, PL → ACL | Lesson Booked (+ booked quote), Held, No-shows, Late Cancellation Fee Due, Cancelled | Scheduling's own words (R6-3); booked quote (R8-1) |
| Billing | Scheduling | C/S, PL → ACL | Lesson Unpaid / Paid | Billing announces money facts; booking keeps its own note (R7-2) |
| Scheduling | Reviews | C/S, PL → ACL | Lesson Held | R7-4 |
| Scheduling | Complaints | C/S, PL → ACL | lesson details | R6-4 |
| Billing | Complaints | C/S, PL → ACL | what was charged | R6-4 |
| Billing | Payouts | C/S, PL → ACL | Tutor Earnings Credited / Reversed | R4-3, R4-4 |
| Complaints | Billing | C/S, PL → ACL | Refund Decided | own terms (R7-3) |
| Complaints | TrustAndSafety | C/S, PL → ACL | Complaint Upheld | own terms (R7-3) |

Relationship rules applied:
- **Customer/Supplier between internal teams:** all seven are in the same company (R6-5).
- **No Shared Kernel:** "Nobody wants a shared database or a shared code library" (R6-5).
- **Removed in v2:** `Pricing → Billing : ChargeCalculation`. The charge equals the booked quote (R8-1).
- **Design decision, not an expert fact:** Scheduling republishes the booked quote (price, commission) on Lesson Booked. If Pricing changes its quote format, Scheduling absorbs the change and Billing doesn't. R8-1 leaves the mechanism open ("however it gets them").
- **Not on the map:**
  - TrustAndSafety → Payouts: suspended tutors are still paid (R8-3), and tax forms belong to Payouts (R8-2).
  - Scheduling → Billing for reschedules (R8-4).

### Breaking cycles
| Cycle | Flips tried | Fact that rules each flip out | Outcome |
|---|---|---|---|
| Scheduling ↔ Pricing (v1 draft: Pricing needed Scheduling's held count) | count arrives in the quote request | — (confirmed by a neutral question offering both options, R7-1) | **broken** |
| Pricing → Billing paths | drop ChargeCalculation | R8-1 (charge = booked quote) | **broken** (several indirect cycles removed) |
| Scheduling ↔ Billing | (a) Billing offers an OHS for lesson outcomes; (b) Scheduling offers a "booking block" API for Billing; (c) move the unpaid decision or the booking rule | (a) R6-3: Scheduling announces in its own words; (b) R7-2: Billing only announces money facts, booking keeps its own note; (c) R3-1/R7-2: Billing marks and lifts unpaid, the Scheduling team owns the booking rule | kept |
| Billing ↔ Complaints | (a) RefundDecisions as a Billing OHS form; (b) Billing answers in Complaints' form, or drop ChargeDetails | (a) R7-3: "doesn't fill in anyone else's form"; (b) R6-4: Complaints needs what was charged from Billing | kept |
| Complaints → TrustAndSafety → Scheduling → Complaints | flip UpheldComplaints; Scheduling offers a "close calendar" API to T&S; flip LessonDetails | R7-3; R1-4 (T&S decides, the others react); R6-4 | kept |
| Pricing → Scheduling → Reviews → Pricing | flip LessonPriceQuote; Reviews API called by Scheduling; rating passed in the quote request | R6-2 (Scheduling asks Pricing); R7-4 (Scheduling announces, Community picks up); R6-2 (Pricing gets the rating from reviews) | kept |
| Billing → Scheduling → Complaints → Billing; Billing → Complaints → T&S → Scheduling → Billing | combinations of the arrows above | as above | kept |

The kept cycles are real, two-way dependencies in different, narrow languages (lesson outcomes vs. payment standing; charges vs. refund decisions). The ruling facts are also inlined in CML comments.

### Validator output
Last run, on the final CML, after the review notes were applied:
```
RELATIONSHIPS:
  Stripe [OHS, PL] -> Billing [CF] : CardPayments
RULE conformist-check: Billing conforms to Stripe. OK only if Stripe's language is well established; otherwise it pollutes Billing.
  Stripe [OHS, PL] -> Payouts [CF] : ConnectPayouts
RULE conformist-check: Payouts conforms to Stripe. OK only if Stripe's language is well established; otherwise it pollutes Payouts.
  VideoProvider [] -> Scheduling [ACL] : VideoRoomWebhooks
  TrustAndSafety [PL] -> Scheduling [ACL] (customer/supplier) : TutorEligibility
  Pricing [OHS, PL] -> Scheduling [ACL] : LessonPriceQuote
  Reviews [PL] -> Pricing [ACL] (customer/supplier) : TutorRating
  Scheduling [PL] -> Billing [ACL] (customer/supplier) : LessonOutcomes
  Scheduling [PL] -> Reviews [ACL] (customer/supplier) : HeldLessons
  Scheduling [PL] -> Complaints [ACL] (customer/supplier) : LessonDetails
  Billing [PL] -> Scheduling [ACL] (customer/supplier) : StudentPaymentStanding
  Billing [PL] -> Payouts [ACL] (customer/supplier) : TutorEarnings
  Billing [PL] -> Complaints [ACL] (customer/supplier) : ChargeDetails
  Complaints [PL] -> Billing [ACL] (customer/supplier) : RefundDecisions
  Complaints [PL] -> TrustAndSafety [ACL] (customer/supplier) : UpheldComplaints
RULE ud-cycle: Billing -> Complaints -> Billing. Upstream/downstream going both ways is almost always a design error.
RULE ud-cycle-indirect: Billing -> Complaints -> TrustAndSafety -> Scheduling -> Billing. Check whether each context really depends on the next one's model, or whether one arrow is only event flow drawn the wrong way.
RULE ud-cycle: Billing -> Scheduling -> Billing. Upstream/downstream going both ways is almost always a design error.
RULE ud-cycle-indirect: Billing -> Scheduling -> Complaints -> Billing. Check whether each context really depends on the next one's model, or whether one arrow is only event flow drawn the wrong way.
RULE ud-cycle-indirect: Complaints -> TrustAndSafety -> Scheduling -> Complaints. Check whether each context really depends on the next one's model, or whether one arrow is only event flow drawn the wrong way.
RULE ud-cycle-indirect: Pricing -> Scheduling -> Reviews -> Pricing. Check whether each context really depends on the next one's model, or whether one arrow is only event flow drawn the wrong way.
VALID: 9 bounded contexts, 14 relationships, 0 errors
```
How each RULE line was resolved:
- **conformist-check ×2:** justified by R6-5. Stripe is an industry standard.
- **ud-cycle and ud-cycle-indirect:** see "Breaking cycles". Each was kept with documented flips, and none relies on an assumption.
- **10 WARNING lines** ("ACL in a Customer/Supplier relationship"): deliberate, under integration rule 3 (ACL downstream by default).

## Q&A with the domain expert
Rounds are verbatim. R<n>-<k> in this report means round n, answer k.

### Facilitator -> user (round 1)

Here's how I read the TutorHub stickies. Please correct anything I got wrong.

**How I read the board**
- **Main timeline:** tutor onboarding (Applied → Documents Uploaded → Qualifications Verified → Approved → Profile Published → Hourly Rate Set → Availability Slots Published), then student onboarding (Registered → Payment Card Added), then booking (Tutor Searched → Lesson Requested → Price Quoted → Lesson Booked → Card Pre-authorized), then the lesson itself (Reminder → Video Room Opened → Started → Ended), then money (Student Charged → Commission Deducted → Tutor Earnings Credited).
- **Branches:** Rescheduled / Cancelled / Late Cancellation Fee Applied before the lesson; No-show Recorded instead of a normal lesson.
- **Separate clocks:** weekly payouts (Payout Sent / Payout Failed, every Monday), reviews (up to 14 days later), complaints (any time → Investigated → Refund Issued / Tutor Suspended).
- **Disconnected cluster:** Qualification Expired → Tutor Suspended.
- **Ambiguous:** "Tutor Suspended" appears twice (after a complaint and after an expiry). "Late Cancellation Fee Applied" might be a real charge or just a decision. Whose no-show is "No-show Recorded"? Stripe and the video provider are external parties.

**Heuristic 1: candidate pivotal events.** These look like phase changes. They're candidates until you confirm them.
- **Tutor Approved:** before it, vetting (documents, qualifications); after it, a tutor who offers lessons (profile, rate, slots).
- **Lesson Booked:** before it, searching and quoting; after it, a commitment with a time slot, cancellation rules and money held.
- **Lesson Ended / No-show Recorded:** before it, delivering the lesson; after it, charging and earnings.
- **Tutor Earnings Credited:** before it, per-lesson money; after it, the weekly payout clock.

**Questions**
1. **Tutor Approved vs Tutor Profile Published:** which one is the real change of phase? Do different people or rules handle verification than handle a tutor's profile, rate and availability? *(This decides whether vetting is separate from the tutor's offer.)*
2. **Lesson Booked:** what does it mean in business terms? Does the tutor accept a request, or is a free slot just taken? Who sets the rules for rescheduling, cancelling and the late-cancellation fee? *(This decides where booking ends and lesson scheduling starts.)*
3. **After Lesson Ended or a No-show:** who decides that the student is charged and how much? Is Late Cancellation Fee Applied an actual charge, or a decision someone else carries out? Are Lesson Price Quoted and the final charge the same calculation from the tutor's hourly rate? *(This decides who owns pricing and the per-lesson settlement.)*
4. **Tutor Suspended (twice):** is it the same event with one owner, whatever the reason (complaint or expired qualification)? What does a suspension do to the tutor's published availability and to lessons already booked? *(This decides who owns "can this tutor teach?")*
5. **Earnings and payouts:** what happens after Payout Failed? Do payouts have their own rules (thresholds, retries, holds after a complaint), or do they just send whatever was credited? *(This decides whether payouts are their own context or a mechanism on top of Stripe.)*

### Expert -> facilitator (round 1)

Mostly right. Two things to clarify on the board: a no-show can be either side. If the student or the tutor hasn't joined within 10 minutes, we record a no-show, and we work that out ourselves from the video provider's webhooks. The provider opens the video room 5 minutes before the start, and "Lesson Started" and "Lesson Ended" also come from its webhooks.

1. **Tutor Approved** is the real change. Before it, our Trust & Safety team checks ID and qualifications, and they have their own rules for which documents are needed per country and which qualifications count. A tutor is approved only once. After that, the tutor manages their own profile, hourly rate and availability, and can change the rate any time. The rate valid when the lesson is booked is the one that applies.

2. **Lesson Booked** is the real commitment: before it the student is shopping, after it there's an appointment. The student requests a free slot, sees the quote and books. The tutor doesn't accept each request [improvised]. The Scheduling team owns the rules: cancelling is free up to 24 hours before the lesson. After that, the late cancellation fee is 50% of the lesson price, and a lesson can be rescheduled at most twice.

3. The **Billing team** builds the final charge after a lesson ends, a no-show or a late cancellation. The charge is the lesson price or the fee, taken from the card we pre-authorised. Billing then deducts our commission and credits the tutor's earnings. Their rules are to charge within 1 hour and retry the card twice. If it still fails, the lesson is marked "unpaid" and the student gets an email.
   - **Late cancellation fee:** the late-cancellation rule says a fee is due, and Billing actually takes the money.
   - **No-shows:** if the tutor doesn't show up, the student isn't charged. If the student doesn't show up, they pay in full.
   - **Price calculation:** the quote, the late fee and the final charge all use the same calculation: hourly rate × duration + platform commission. Commission is 15%, or 10% for tutors with a rating of at least 4.8 and at least 100 lessons. The finance people set the commission rules and change them every few months, independently of booking.

4. **Suspension** is the same outcome with two reasons: an expired qualification, which stays a suspension until it's renewed, or an upheld complaint. Today Trust & Safety and Complaints both suspend tutors on their own, and it's a mess. Trust & Safety once re-activated a tutor that Complaints had suspended, just because the tutor renewed a certificate. We want ONE place deciding whether a tutor may teach, and Trust & Safety should own it. Complaints should only say "complaint upheld against tutor X". A suspended tutor's profile is hidden from search and they can't receive new bookings. Lessons already booked with that tutor are cancelled free of charge for the student, and the student is notified [improvised].

5. **Payouts** is a different team with its own rules, on a clock that has nothing to do with lessons. Every Monday they pay out each tutor's accumulated earnings through Stripe Connect.
   - The minimum payout is 20 EUR.
   - A failed payout is retried the next Monday.
   - Tutors in some countries have to send in tax forms before they can be paid.
   - Payouts aren't held automatically after a complaint. Refunds go through Billing [improvised].

### Facilitator -> user (round 2)

Thanks, that moves a lot. Here's what changed:

**Confirmed pivots:** *Tutor Approved* (Trust & Safety before it, the tutor's own offer after it) and *Lesson Booked* (shopping before it, an appointment after it). *Lesson Ended / No-show / Late cancellation* hands off to Billing, which builds the charge.

**What your answers decided:**
- **"May this tutor teach?" gets one owner, Trust & Safety.** It owns approval, expiry, suspension and reinstatement. Complaints only publishes "complaint upheld". That fixes the double-suspension problem (H3).
- **Payouts is its own lane (H2):** a Monday clock, its own rules (20 EUR minimum, retry next Monday, tax forms).
- **Price calculation is used in three places** (quote, late fee, final charge) and its commission rules change on finance's own schedule. That points to one **Pricing** owner that Scheduling and Billing both ask, not three copies (H3).
- **Billing** owns the charge, the retries, "unpaid", the commission deduction and the earnings credit.

**Candidate lanes (H2):** reviews (up to 14 days), complaints (any time), qualification expiry (its own clock) and lesson delivery (driven by the video provider's webhooks).

**Missing events I'd add to the board:** Charge Failed / Lesson Marked Unpaid, Complaint Upheld, Tutor Reinstated, and Lesson Cancelled by Platform (after a suspension). I'd also split "No-show Recorded" into Student No-show and Tutor No-show, because they lead to different charges.

**Questions**
1. **Lesson delivery:** is room → started → ended → no-show (the 10-minute rule) owned by the Scheduling team, or by someone else with its own rules? *(This decides whether lesson delivery is part of Scheduling or a separate context.)*
2. **Pricing:** which team owns the calculation (rate × duration + commission)? Does Billing recalculate at charge time, or use the price fixed when the lesson was booked? *(This decides whether Pricing is a context of its own, and what Billing depends on.)*
3. **Reviews and rating:** who owns reviews and the rating, and what are their rules (only after a held lesson? 14 days)? Who uses the rating besides the commission tier: search, Trust & Safety? *(This decides whether Reviews is a context and who depends on it.)*
4. **Complaints:** do they have their own rules (deadlines, investigation steps)? When a complaint leads to a refund, who decides the amount, Complaints or Billing? *(This decides who owns the refund.)*
5. **Students and the tutor listing:** who owns Student Registered and Payment Card Added? Is the tutor's profile, rate, availability and search one thing the tutor manages, or does Scheduling own the availability slots? *(This decides whether there's a Tutor Profile/Catalog context and a Student Accounts context.)*

### Expert -> facilitator (round 2)

Domain expert answers to round 2 (TutorHub product owner).

1. **Lesson delivery:** The Scheduling team looks after it today [improvised]. It watches the video provider's webhooks and applies our own 10-minute no-show rule. One warning: the provider's webhook model is messy, and they've changed it twice this year.

2. **Pricing:** The business/finance people own the rules: rate × duration + commission. Commission is 15%, or 10% for tutors with a rating of at least 4.8 and at least 100 lessons. The tutor's hourly rate is the one valid at booking time. Billing runs the same calculation for the final charge or the late fee, using that booked rate, and the commission tier is also taken as of booking [improvised].

3. **Reviews and rating:** The small Community team owns them. A student rates the lesson 1-5 with a comment, up to 14 days after it. Only lessons that actually happened can be reviewed [improvised]. The rating is the average of the last 50 reviews. Pricing reads it for the commission tier, and search shows it to students. Trust & Safety doesn't use it [improvised].

4. **Complaints:** Students or tutors can file one at any time. The Complaints team investigates it, and there are no formal deadlines beyond that [improvised]. The outcomes are: rejected, refund, upheld against the tutor, or both refund and upheld. Complaints decides the refund ("refund lesson L, amount A"). Billing only carries it out, because it knows how to reverse a charge.

5. **Students and the tutor listing:** The student registers on the platform. The card is stored with Stripe and is the card Billing pre-authorises and charges later [improvised]. After approval, the tutor manages their own profile, rate and availability slots. When a lesson is booked, Scheduling takes that slot [improvised]. Search has no rules of its own. It only shows profiles, rates, ratings and free slots that others publish.

### Facilitator -> user (round 3)

Thanks. Here's the first H3 draft, where each candidate answers one question:

| Candidate | Question it answers |
|---|---|
| **Trust & Safety** | May this tutor teach? (vetting, approval, expiry, suspension, reinstatement) |
| **Tutor Listing** | What does this tutor offer? (profile, hourly rate, availability slots; search reads it) |
| **Scheduling** | Which lesson happens when, and did it happen? (request, booking, reschedule, cancel with the 24h/50% rule, reminder, delivery and the no-show rule) |
| **Pricing** | What does a lesson cost, and what's our cut? (rate × duration + commission tier) |
| **Billing** | What does the student owe for this lesson, and is it paid? (pre-auth, charge, retries, unpaid, commission deduction, earnings credit, executing refunds) |
| **Payouts** | What do we pay each tutor this Monday? |
| **Reviews** | How do students rate this tutor? |
| **Complaints** | Was this complaint justified, and what's the remedy? |

**What moved:**
- **Lesson delivery stays in Scheduling.** Same team, and the no-show rule is theirs. The provider's messy webhooks become a translation layer at the edge, not a context.
- **Search isn't a context.** It has no rules of its own, so it's a read view of the listing.
- **Refunds:** Complaints decides, and Billing executes. That's one owner per decision.

**Questions**
1. **Students:** does a student account have rules of its own, such as verification, age, or blocking? In particular, can a student with an "unpaid" lesson still book? If not, who decides that block? *(This decides whether there's a Student Accounts context, or whether registration is just a login and the card belongs to Billing.)*
2. **Free slots:** after a booking, who is the source of truth for "this slot is taken": the tutor's listing or Scheduling? Can a tutor delete or change a slot that's already booked? *(This decides whether availability is owned by Tutor Listing or by Scheduling. Today it looks owned twice.)*
3. **"At least 100 lessons" for the commission tier:** which lessons count (held? paid?), and who counts them? *(This decides what Pricing depends on: Scheduling or Billing.)*
4. **Late-cancellation rule:** if finance wanted a 30% fee instead of 50%, or a 12-hour window instead of 24, who would change it: the Scheduling team or finance? *(This decides whether the fee rule lives in Scheduling or in Pricing.)*

### Expert -> facilitator (round 3)

Domain expert answers to round 3 (TutorHub product owner).

1. **Students:** A student account is basically a login plus a card stored with Stripe. There's no verification or age check [improvised]. A student with an "unpaid" lesson can't book again until it's paid. Billing marks the lesson unpaid and is the one that lifts it once the money comes in. Booking just checks whether the student has anything unpaid [improvised].

2. **Free slots:** Once a lesson is booked, the booking is the truth: that slot is taken. The tutor can't delete or change a booked slot. They have to cancel the lesson, which is free for the student, or ask the student to reschedule. The tutor can still freely edit slots that aren't booked [improvised].

3. **"At least 100 lessons":** Only lessons that were actually held count, meaning the lesson ended. No-shows and cancellations don't count. The count comes from the lesson records Scheduling keeps, not from payments [improvised].

4. **Late-cancellation rule:** The Scheduling team owns the cancellation rules, so they'd change the 24-hour window or the 50% figure, even if finance asked for it. The fee is then 50% of the lesson price, calculated the same way as the quote.

### Facilitator -> user (round 4)

I had an independent reviewer check each candidate. Six held up: Trust & Safety, Scheduling, Pricing, Payouts, Reviews and Complaints. Two problems came back, and they need your facts:

- **Free slots look owned twice.** The tutor's listing holds unbooked slots, and Scheduling holds booked ones. Only one place should decide whether a slot can be booked right now.
- **Nobody clearly owns the tutor's balance.** Billing credits earnings per lesson, and Payouts pays out the "accumulated" amount and carries it over.

**Questions**
1. **Availability:** are there rules about slots, such as minimum notice before a lesson, buffers between lessons, or a maximum number of lessons per day? If a tutor removes a free slot just as a student books it, who wins? Which team owns those rules? *(This decides whether the slot calendar belongs to Scheduling, or to the tutor's listing with Scheduling only reserving from it.)*
2. **Profile and rate:** apart from the tutor editing them, do profile and rate have rules or a team of their own? Examples: profile moderation, rate limits, currency rules. *(This decides whether the tutor listing is a context of its own or just data that others publish and search shows.)*
3. **Tutor balance:** who holds "how much we owe tutor X right now", the Billing team or the Payouts team? Who answers a tutor asking "why is my 18 EUR not paid?" *(This decides who owns the balance and the 20 EUR minimum.)*
4. **Refund after payout:** when a refund is issued for a lesson the tutor has already been paid for, does the tutor lose that money (taken off the next payout), or does TutorHub absorb it? *(This decides whether refunds flow on into Payouts.)*
5. **Upheld complaints:** does every complaint upheld against a tutor suspend them, and how does that suspension end? Can a complaint against a student lead to anything, such as blocking the student? *(This decides Trust & Safety's reinstatement rule, and whether anyone owns "may this student book?" besides Billing's unpaid check.)*

### Expert -> facilitator (round 4)

Domain expert answers to round 4 (TutorHub product owner).

1. **Availability:** The Scheduling team owns the slot rules. A slot can be booked up to 2 hours before it starts, there's no buffer between lessons, and there's no daily maximum [improvised]. If a tutor removes a free slot at the same moment a student books it, the booking wins. The tutor then has to cancel the lesson like any other booked lesson [improvised].

2. **Profile and rate:** No team owns them and there are no special rules. The tutor edits their own profile and rate, everything is in EUR, and there's no limit on the rate. Trust & Safety only checks the tutor once, at approval [improvised].

3. **Tutor balance:** The Payouts team holds "what we owe tutor X". Billing just tells them about each earning it credits. A tutor asking "why isn't my 18 EUR paid?" talks to Payouts, because the 20 EUR minimum is their rule [improvised on who holds the balance; the 20 EUR minimum is a fact].

4. **Refund after payout:** The tutor loses it. The refunded amount, minus our commission, is taken off their next payout [improvised].

5. **Upheld complaints:** Every complaint upheld against a tutor suspends them, and Trust & Safety decides that suspension. It ends only when Trust & Safety reviews the case and reinstates the tutor [improvised on how it ends]. A complaint against a student can only end in a refund or a rejection today. We don't block students over complaints. The only thing that stops a student from booking is an unpaid lesson [improvised].

### Facilitator -> user (round 5: candidates for approval)

Your answers settled the last two problems: **the booking wins, so the slot calendar is Scheduling's**, and **Payouts holds the tutor balance**. Profile and rate have no team and no rules, so "Tutor Listing" isn't a context. It's the tutor's offer inside Scheduling. An independent critic agreed to all seven candidates below (two needed a second round).

| Context | Question it answers | Owns | Why it's separate |
|---|---|---|---|
| **Trust & Safety** | May this tutor teach? | application, documents, qualifications, approval (once), expiry, suspension (any reason), reinstatement | pivot *Tutor Approved*; single owner of suspension (H3) |
| **Scheduling** | When can this tutor be booked, and what happened to each booked lesson? | profile, EUR rate, slots, booking (2h cut-off, booking wins), reschedule (max 2), cancellation (24h, 50% fee due), platform cancellation, reminder, delivery, no-shows (10 min), held-lesson count | pivot *Lesson Booked*; one team owns all these rules |
| **Pricing** | What does a lesson cost, and what's our commission? | rate × duration + commission, 15% / 10% tier | finance's rules, changing on their own schedule (H3) |
| **Billing** | What is charged for this lesson, and is it paid? | card, pre-auth, charge within 1h, 2 retries, unpaid status, executing refunds, earnings credited/reversed per lesson | hand-off after *Lesson Ended / No-show / Fee Due*; own rules |
| **Payouts** | What do we owe each tutor, and what do we pay them this Monday? | tutor balance, 20 EUR minimum, retry next Monday, tax forms, refund clawback | Monday clock (H2), own team |
| **Reviews** | How do students rate this tutor? | review window (14 days, held lessons only), rating = average of the last 50 | own clock (H2), Community team |
| **Complaints** | Was this complaint justified, and what's the remedy? | filing, investigation, outcome, refund amount; publishes "upheld" (never suspends) | any-time lane (H2), own team |

**Not contexts:** search (a read view), student login, Stripe and the video provider (external).

**Open assumptions** (none of them moves a boundary):
- Pre-authorisation timing and release when nothing is charged
- Whether payouts are held while a tutor is suspended, and who checks tax forms
- Where the booking-time quote is stored (I assume Scheduling stores Pricing's quote with the booking)

**Suggested board changes:**
- Add: Charge Failed, Lesson Marked Unpaid, Unpaid Lifted, Complaint Upheld, Refund Decided, Tutor Reinstated, Qualification Renewed, Lesson Cancelled by Platform, Lesson Held, Booking Refused, Tutor Earnings Reversed, Payout Deferred.
- Split "No-show Recorded" into Student No-show and Tutor No-show.
- Rename "Late Cancellation Fee Applied" to "Late Cancellation Fee Due".
- Merge the two "Tutor Suspended" stickies into one, with a reason.

**Questions**
1. Do you approve these seven contexts?
2. Do you want a context map in Context Mapper (.cml) format?

### Expert -> facilitator (round 5)

Domain expert answers to round 5 (TutorHub product owner).

1. Yes, I approve the seven. They match how our teams and rules actually work, and the names are fine.
2. Yes, please make the context map in CML. Call the system TutorHub.

### Facilitator -> user (round 6: map facts)

Thanks. The seven contexts are approved. For the map, I'll test it against your business processes. These are the ones I read from the board:

- **P1 Onboard a tutor:** apply → verify → approve → publish the offer
- **P2 Book and hold a lesson:** search → quote → book → pre-authorise → lesson → charge → earnings credited
- **P3 Cancel or reschedule:** including the late fee and a no-show
- **P4 Weekly payout**
- **P5 Review a lesson:** review → rating → commission tier
- **P6 Handle a complaint:** refund and/or suspension → booked lessons cancelled → clawback
- **P7 Qualification expires:** suspension → renewal → reinstatement

Each hand-off between contexts becomes a relationship. Its direction depends on whose terms the other side has to understand, and that's where I need your facts.

**Questions**
1. **Is the process list complete and right?** Is any process missing or wrongly cut? *(Every relationship on the map must serve one of them.)*
2. **The quote:** when a student sees a price before booking, who asks Pricing for it? Where do the inputs come from at that moment: the rate, the tutor's rating and the held-lesson count? Does Pricing keep each tutor's commission tier itself, or work it out from the inputs each time? *(This decides the direction between Pricing, Scheduling and Reviews.)*
3. **Lesson outcomes to Billing:** how does Billing learn that a lesson was booked (for the pre-auth), held, missed, or cancelled late? Whose terms does that message use: does Scheduling send what Billing asks for, or does Billing read Scheduling's lesson records and translate them? *(This decides the direction between Scheduling and Billing, which also has to carry Billing's "unpaid" answer back to booking.)*
4. **Complaints' hand-offs:** what does Complaints need to know about a lesson or a tutor when a complaint is filed, and where does it get that? When it decides a refund or an upheld complaint, does it publish its decision for Billing and Trust & Safety to interpret, or does it use the request format that Billing and Trust & Safety define? *(This decides the directions around Complaints.)*
5. **Teams and outsiders:** are all seven teams inside TutorHub, working towards a shared plan? Do we simply adapt to Stripe's and the video provider's APIs as they are? *(This decides Customer/Supplier relationships and whether we conform to the outsiders or translate them.)*

### Expert -> facilitator (round 6)

Domain expert answers to round 6 (TutorHub product owner).

1. **The process list:** The four processes I care about for the map are (1) tutor onboarding, (2) booking and taking a lesson, (3) the weekly payout, and (4) a complaint leading to a refund and suspension. To me, cancelling, rescheduling and no-shows are part of booking and taking a lesson. Reviews and qualification expiry happen, but they aren't processes I'd judge the map by [improvised].

2. **The quote:** When a student requests a slot, Scheduling asks Pricing for the price. Scheduling provides the tutor's rate and the lesson duration. Pricing gets the tutor's rating from the Community team's reviews and the held-lesson count from Scheduling. Pricing works out the commission tier fresh each time and doesn't keep it [improvised on the count and the tier].

3. **Lesson outcomes to Billing:** Billing needs the price or fee calculation and the facts about each lesson. Scheduling announces what happened in its own words: lesson booked, held, student no-show, tutor no-show, cancelled late. Billing works out what to charge from that. In the other direction, Billing tells booking which students have an unpaid lesson [improvised on how the messages travel].

4. **Complaints' hand-offs:** When a complaint is filed, Complaints needs the lesson details (who, when, what happened) from Scheduling and what was charged from Billing [improvised]. Its decisions are simple statements: "refund lesson L, amount A" and "complaint upheld against tutor X". Billing works out how to reverse the charge, and Trust & Safety decides the suspension.

5. **Teams and outsiders:** Yes, all seven teams are in the same company, working on the same product. Nobody wants a shared database or a shared code library between teams, because the last shared "common" module was a disaster.
   - **Stripe:** Its API is an industry standard, and we're happy to follow it as it is in our payment code.
   - **The video provider:** Its webhook model is messy, and they've changed it twice this year, so I wouldn't let it leak into our own code.

### Facilitator -> user (round 7: direction facts)

Thanks. That fixes most of the arrows:
- Scheduling's lesson facts → Billing
- Billing's earnings → Payouts
- Reviews' rating → Pricing
- Pricing's price → Scheduling and Billing
- Trust & Safety's tutor status → Scheduling
- Stripe as-is (we conform)
- the video provider behind a translation layer

All teams are internal, so the default between them is Customer/Supplier, with no shared kernel.

Walking your four processes, I found three loops where two contexts each depend on the other's terms. A loop like that usually means one arrow is drawn the wrong way, so I need the facts that decide each one:

**Questions**
1. **The held-lesson count:** how does it reach Pricing? Does Pricing look it up in Scheduling's records, or does it arrive with Scheduling's price request, next to the rate and duration? *(Scheduling ↔ Pricing: Scheduling asks Pricing for the price, and Pricing needs Scheduling's count.)*
2. **Unpaid students:** what does booking actually keep or check: Billing's list of unpaid lessons, or a booking block that Billing switches on and off? Who defines what "blocked from booking" means? *(Scheduling ↔ Billing: Billing needs Scheduling's lesson facts, and booking needs Billing's unpaid answer.)*
3. **"Complaint upheld against tutor X":** whose terms does this message use? Does Trust & Safety define what it accepts as a suspension reason, so Complaints fills that in? Or does Complaints announce it in its own terms, and Trust & Safety interprets it? The same question applies to "refund lesson L, amount A" towards Billing. *(This decides the loop Trust & Safety → Scheduling → Complaints → Trust & Safety, and the one between Complaints and Billing.)*
4. **Reviews:** how does the Community team learn that a lesson was held and can be reviewed? Who sends "Review Requested"? *(Scheduling → Reviews → Pricing → Scheduling would be another loop.)*

### Expert -> facilitator (round 7)

Domain expert answers to round 7 (TutorHub product owner).

1. **The held-lesson count:** It comes with Scheduling's price request, next to the rate and the duration. Pricing doesn't look anything up in Scheduling's records [improvised].

2. **Unpaid students:** Billing only announces things about money: "lesson L unpaid by student S", and later "paid". The booking rule, that a student with an unpaid lesson can't book, belongs to the Scheduling team. Booking keeps its own note of who has unpaid lessons and refuses those students [improvised].

3. **Complaint decisions:** Complaints announces its decisions in its own terms: "complaint upheld against tutor X", and "refund lesson L, amount A". Trust & Safety interprets an upheld complaint and decides on the suspension under its own rule. Billing works out how to reverse the charge. Complaints doesn't fill in anyone else's form.

4. **Reviews:** Scheduling announces "lesson held", as it does for everything else. The Community team picks that up and invites the student to review within 14 days, so the review request comes from the Community team [improvised].

### Facilitator -> user (round 8: map assumptions)

I've drafted the map, and it validates in Context Mapper. Four independent reviewers walked your four processes through it. Every step has an owner, and every hand-off has a relationship. They flagged three facts I had assumed instead of asking you. One of them can remove a relationship:

**Questions**
1. **The charge after a lesson:** when Billing charges a held lesson, a student no-show or a late fee, or works out "refund minus commission", where do the price and the commission come from? Is it the quote the student saw at booking, or a new calculation at charge time? If it's a new calculation, whose rating and lesson count decide the tier: today's, or those at booking? *(This decides whether Billing needs to call Pricing at all, and what Scheduling's lesson facts carry.)*
2. **Tax forms and payout accounts:** where does a tutor send the tax forms? Who knows the tutor's country and whether the forms were accepted? Who sets up the tutor's Stripe Connect account? *(This decides whether Payouts needs anything from another context.)*
3. **Suspension and payouts:** you said payouts aren't held after a complaint. What about a tutor suspended for an expired qualification? Does their Monday payout still go out? *(This decides whether Payouts depends on Trust & Safety.)*
4. **Rescheduling and the card:** when a lesson is rescheduled, does anything change for the card pre-authorisation, such as a new hold for the new date? *(This decides whether Billing has to hear about reschedules.)*

### Expert -> facilitator (round 8)

Domain expert answers to round 8 (TutorHub product owner).

1. **The charge after a lesson:** It's the same calculation as the quote, using the inputs as they were at booking: the booked rate, and the rating and lesson count at booking time. So for a held lesson or a student no-show, the charge equals the quote the student saw. The late fee is 50% of that price, and a refund comes back out of it, minus commission [improvised on booking-time rating and count]. Billing has to know that price and that commission, however it gets them.

2. **Tax forms and payout accounts:** The tutor sets up their Stripe Connect payout account and uploads their tax forms in the payouts part of the app. The Payouts team checks the forms. Stripe Connect onboarding asks for the tutor's country. Trust & Safety isn't involved [improvised].

3. **Suspension and payouts:** Yes, a suspended tutor still gets their Monday payout, whatever the reason for the suspension. It's money they already earned [improvised].

4. **Rescheduling and the card:** Nothing changes. The existing hold stays, and Billing only cares about the final outcome of the lesson [improvised].

### Facilitator -> user (round 9: map approval)

Your answers removed one relationship: the charge equals the booked quote, so Billing gets the price and commission with "Lesson Booked" and doesn't call Pricing. The map has 14 relationships. It validates with 0 errors, and an independent final review walked all four of your processes through it with no blocking findings.

| Upstream → Downstream | Pattern | What flows | Your fact |
|---|---|---|---|
| Trust & Safety → Scheduling | Customer/Supplier, PL → ACL | approved / suspended / reinstated | T&S decides, the others react |
| Pricing → Scheduling | OHS, PL → ACL | quote (rate, duration, held lessons) | Scheduling asks Pricing |
| Reviews → Pricing | C/S, PL → ACL | tutor rating | Pricing reads the rating |
| Scheduling → Billing | C/S, PL → ACL | booked (with quote), held, no-shows, late fee due, cancelled | announced in Scheduling's words |
| Billing → Scheduling | C/S, PL → ACL | lesson unpaid / paid | booking keeps its own note |
| Scheduling → Reviews / Complaints | C/S, PL → ACL | lesson held / lesson details | |
| Billing → Complaints | C/S, PL → ACL | what was charged | |
| Complaints → Billing / T&S | C/S, PL → ACL | refund decided / complaint upheld | "fills in nobody's form" |
| Billing → Payouts | C/S, PL → ACL | earnings credited / reversed | Payouts holds the balance |
| Stripe → Billing, Payouts | Conformist | cards, Connect payouts | "industry standard, follow as is" |
| Video provider → Scheduling | ACL | room webhooks | "won't let it leak in" |

**How the validator's warnings were resolved:**
- **Conformist to Stripe:** justified by your answer that its API is an industry standard.
- **ACL warnings on Customer/Supplier links:** deliberate. An ACL downstream is our default.
- **Loops:** one direct loop (Pricing ↔ Scheduling) was broken by your answer that the lesson count comes with the price request. Four loops stay:
  - Scheduling ↔ Billing
  - Billing ↔ Complaints
  - Complaints → Trust & Safety → Scheduling → Complaints
  - Pricing → Scheduling → Reviews → Pricing

  For each of these I tried flipping every arrow, and one of your answers rules out each flip. They're real two-way dependencies in different, narrow languages. The report lists every flip I tried.

**Design decision (not your fact):** Scheduling republishes the booked quote. So if Pricing's format changes, only Scheduling has to absorb it, not Billing.

**Open assumptions:** none on relationships.

Do you approve this map? It would go to `src/main/cml/TutorHub-Context-Map.cml`, which is a new file. Then I'd draft one Bounded Context Canvas per context.

### Expert -> facilitator (round 9)

Domain expert answer to round 9 (TutorHub product owner).

Yes, I approve the map. It matches how our teams work: no shared database or common library, Stripe followed as it is, and the video provider's mess kept out. Please save it as src/main/cml/TutorHub-Context-Map.cml. And yes, I'd like the canvases for each context next.

## Open questions and assumptions
None of these affects a relationship.
- When is the card pre-authorised for lessons booked far ahead (holds expire), and when is it released if nothing is charged? (Billing)
- What happens when a clawback is larger than the next payout: negative balance, limit, write-off? (Payouts)
- Is it one review per held lesson, and can reviews be edited or moderated? (Reviews)
- Can a quote expire between Lesson Price Quoted and Lesson Booked? (Scheduling)
- Strategic classification (core/supporting/generic) wasn't discussed for any context.

## Suggested board changes
- Merge the two "Tutor Suspended" stickies into one event with a reason. Add "Tutor Reinstated" and "Qualification Renewed".
- Split "No-show Recorded" into "Student No-show" and "Tutor No-show".
- Rename "Late Cancellation Fee Applied" to "Late Cancellation Fee Due". Billing takes the money.
- Add these events:
  - Charge Failed, Lesson Marked Unpaid, Unpaid Lifted
  - Complaint Rejected, Refund Decided, Complaint Upheld
  - Lesson Cancelled by Platform, Booking Refused, Lesson Held
  - Tutor Earnings Reversed, Payout Deferred (below minimum)
- Replace "Complaint Investigated" as the outcome with the outcome events listed above.

## Next steps
- Canvases, from bounded-context-scaffolder (canvases only; no plan or code yet):
  - `docs/bounded-contexts/tutorhub/trustandsafety/canvas.md`
  - `docs/bounded-contexts/tutorhub/scheduling/canvas.md`
  - `docs/bounded-contexts/tutorhub/pricing/canvas.md`
  - `docs/bounded-contexts/tutorhub/billing/canvas.md`
  - `docs/bounded-contexts/tutorhub/payouts/canvas.md`
  - `docs/bounded-contexts/tutorhub/reviews/canvas.md`
  - `docs/bounded-contexts/tutorhub/complaints/canvas.md`
- Review the canvases, write the business rules as strict Given/When/Then, settle the strategic classification, then run bounded-context-scaffolder per context.
