# Bounded context discovery: TutorHub (whole platform)

- **Date:** 2026-11-05
- **Mode:** discover
- **Status:** canvases handed off
- **Input:** `.claude/skills/discover-bounded-contexts/evals/files/tutorhub-narrative.md` (typed-up event-storming stickies + quotes)
- **Context map:** `src/main/cml/TutorHub-Context-Map.cml`

## Reading of the input
- **Main timeline (one lesson):** Tutor Searched -> Lesson Requested -> Lesson Price Quoted -> Lesson Booked -> Card Pre-authorized -> Lesson Reminder Sent -> Video Room Opened -> Lesson Started -> Lesson Ended -> Student Charged -> Platform Commission Deducted -> Tutor Earnings Credited -> Review Requested -> Review Submitted -> Tutor Rating Updated.
- **Branches:** Lesson Rescheduled; Lesson Cancelled -> Late Cancellation Fee Applied; No-show Recorded as an alternative ending.
- **Prerequisite flows:** tutor onboarding (Applied ... Availability Slots Published) and student onboarding (Registered, Payment Card Added), independent of each other.
- **Disconnected clusters:** weekly payouts (Monday), complaints (anytime), qualification expiry.
- **External:** Stripe (cards, payouts), video provider.
- **Ambiguities raised:** duplicated Tutor Suspended; whether fees/charges are real charges; nothing after Payout Failed; whose no-show.
- **Expert's corrections:** "Mostly right." Tutor Suspended is one event with two reasons; the late fee and the charge are real charges; a failed payout is retried next Monday; no-show can be either side.

## Heuristic trail
### H1: Pivotal events
| Event | Status | Before | After | Expert's answer |
|---|---|---|---|---|
| Tutor Approved | confirmed | Trust & Safety vets a person (country document rules, accepted qualifications) | tutor manages profile, rate, slots and sells lessons | "Approval is the big moment, and it happens once." |
| Tutor Profile Published | rejected as pivot | | | First step after approval. |
| Lesson Booked | confirmed | student shopping (search, request, quote) | an appointment under Scheduling's rules | "Lesson Booked is the real commitment." A request isn't a commitment. |
| Lesson Ended / No-show Recorded / late Lesson Cancelled | confirmed hand-off | appointment | Billing charges (within 1 h, retries, unpaid) | "The real charge comes after Lesson Ended, a no-show or a late cancellation." |
| Lesson Started | rejected | | | "Once a lesson starts, it's still the same appointment ... nothing new kicks in until Lesson Ended." |
| Tutor Earnings Credited (renamed Lesson Earnings Determined) | confirmed hand-off | per-lesson earnings (Billing) | balance and Monday payout (Payouts) | "Payouts answers 'how much do we owe tutor X'." |

### H2: Swimlanes
| Flow | Verdict | Why |
|---|---|---|
| Weekly payouts | lane | "every Monday, no matter what happens with lessons"; own team and rules |
| Reviews | lane | up to 14 days after the lesson; Community team |
| Complaints | lane | "anytime, even weeks later"; Complaints team |
| Qualification expiry | not a lane of its own | Trust & Safety's rule on "may teach"; stays in Tutor Vetting |
| Student vs tutor no-show; on-time vs late cancellation; technical failure | variants | endings of the same appointment, same owner |
| Lesson delivery (video room, no-show) | not a lane | strictly sequential inside the appointment; same team and rules (critic r1) |

### H3: Single responsibility
| Duplicated responsibility | Where it appeared | Resolution |
|---|---|---|
| Suspending a tutor | Trust & Safety (expiry) and Complaints (upheld complaint); a complaint suspension was once lifted by a certificate renewal | Tutor Vetting is the single owner (expert: "ONE place ... owned by Trust & Safety"); Complaints only publishes Complaint Upheld |
| Price calculation | quote, late-cancellation fee, final charge | One Pricing context ("the same calculation gives the quote, the late cancellation fee and the final charge"); Finance changes it independently |
| Tutor balance | Billing "credits earnings", Payouts pays | Payouts owns the balance; Billing's event renamed Lesson Earnings Determined |
| Refund | Complaints decides, Billing reverses | Complaints owns the decision, Billing the execution |
| Lesson Delivery vs Scheduling | split only by the video provider's webhook churn | merged into Scheduling; churn handled by an ACL (critic r1 DISAGREE, expert reply 3.1-3.3) |
| Free slots | tutor self-service vs booking | Scheduling owns slots: "never double-book", first booking wins, a booked slot can't be withdrawn (reply 3.4) |
| Search, reminder, student registration | candidates | not contexts: no rules (search is a view in Tutor Catalog, reminder is a mechanism, registration a generic account) |

## Bounded contexts
| Context | Question it answers | Owns (events, decisions, rules) | Heuristic justification | Classification |
|---|---|---|---|---|
| TutorVetting | May this tutor teach on TutorHub? | Tutor Applied, Identity Documents Uploaded, Qualifications Verified, Tutor Approved, Qualification Expired, Qualification Renewed, Tutor Suspended (reason), Tutor Reinstated; country document rules, accepted qualifications, automatic suspension on Complaint Upheld, reinstated only when all reasons are cleared; opens/closes/reopens the tutor's calendar | Pivot Tutor Approved; H3 single owner of "may teach" | not given |
| TutorCatalog | What does each tutor offer, and which tutors can a student find? | Tutor Profile Published, Hourly Rate Set/Changed; only verified subjects; EUR; hides suspended tutors; search view | Owned far side of Tutor Approved; tutor's own reasons to change; critic: supporting/generic at most | supporting/generic (critic) |
| Scheduling | What is the state of each lesson appointment, from booking to how it ended? | Slots, Lesson Requested, Lesson Booked, Booking Confirmed/Expired, Rescheduled (max 2; technical failures don't count), Cancelled (on time/late/by platform; free up to 24 h), Video Room Opened, Lesson Started/Ended, No-show Recorded (10 min), Lesson Failed Technically; no double booking; 1 h to fix a failed card | Pivot Lesson Booked; shopping side has no rules; delivery merged (same owner and rules) | not given |
| Pricing | What does this lesson cost, and what is the platform's commission? | Lesson Price Quoted (frozen at booking); commission 15% / 10% (rating >= 4.8 and >= 100 lessons that took place); late fee 50% | H3 single calculation; Finance changes it every few months independently | not given |
| Billing | Which charge applies to how this lesson ended, and has it been collected (or reversed)? | Card Pre-authorized/Failed/Released, Student Charged, Late Cancellation Fee Charged, Payment Failed, Lesson Marked Unpaid, Platform Commission Deducted, Lesson Earnings Determined, Refund Issued; charge within 1 h, 2 retries, then unpaid + email; tutor no-show = no charge; refunds reverse commission; owns the student card | Far side of the appointment-ended hand-off; own rules | not given |
| Payouts | How much do we owe each tutor, and has it been paid out? | balance, Weekly Payout Sent, Payout Withheld (< 20 EUR, tax forms, suspended), Payout Failed, Payout Retried (next Monday); refund deductions, negative balance; tax forms | H2 Monday lane; owns the balance | not given |
| Reviews | What do students think of this tutor? | Review Requested, Review Submitted (1-5 + comment, 14 days), Tutor Rating Updated (average of last 50) | H2 lane; Community team | not given |
| Complaints | Is this complaint justified, and what remedy follows? | Complaint Filed, Investigated, Rejected, Upheld, Refund Decided; disputed no-shows | H2 lane; H3: no suspension, no money movement | not given |

External systems: **Stripe** (cards, Stripe Connect payouts), **VideoProvider**. Not contexts: search (view in TutorCatalog), lesson reminder (mechanism), student accounts (generic; card belongs to Billing, assumption A1 confirmed).

## Critic log
| Subject | Round | Verdict | Blocking points / resolution |
|---|---|---|---|
| Tutor Vetting | 1 | AGREE | Notes: add Qualification Renewed; Qs on automatic suspension and two reasons (answered in reply 3.7) |
| Tutor Profiles | 1 | DISAGREE | Free slots might belong to the tutor's profile. Asked expert (reply 3.4-3.5): never double-book -> slots stay in Scheduling |
| Tutor Profiles -> Tutor Catalog | 2 | AGREE | Barely a context (supporting/generic); renamed Tutor Catalog with search as its view; downstream of Vetting for verified subjects |
| Scheduling | 1 | AGREE | Record the H3 reason for straddling Lesson Booked; suggested split cancellation events |
| Lesson Delivery | 1 | DISAGREE | Only reason to split was the provider's webhook churn (an ACL concern); Lesson Started not a confirmed pivot. Asked expert (reply 3.1-3.3) -> merged into Scheduling |
| Scheduling (merged) | 2 | AGREE (x2) | Both critics agree; Billing owns the single outcome-to-charge mapping |
| Pricing | 1 | AGREE | Qs on frozen tier and late-fee owner (answered in reply 3.6) |
| Billing | 1 | AGREE | Reworded question to separate it from Pricing |
| Payouts | 1 | AGREE | Rename Tutor Earnings Credited -> Lesson Earnings Determined; Qs on suspension hold and tax forms (reply 3.8) |
| Reviews | 1 | AGREE | none |
| Complaints | 1 | AGREE | none |
| Map + Tutor onboarding | 1 | AGREE | "open calendar" on approval was inferred -> asked expert (reply 7: yes) |
| Map + Book and take a lesson | 1 | AGREE | Split Scheduling->Billing into LessonOutcomes (PL) + PaymentGuarantee (OHS); FrozenLessonPrice travels in Lesson Booked |
| Map + Weekly payout | 1 | AGREE | Fixed citation of the Stripe CF justification |
| Map + Complaint to refund and suspension | 1 | AGREE | none |
| Map, final pass (all processes) | 2 | AGREE | Notes applied: Scheduling's vision statement now says open/close/reopen calendar; the two Scheduling -> Billing links documented as one U/D pair |

## Context map
### Process walkthroughs
#### 1. Tutor onboarding
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Tutor Applied, Identity Documents Uploaded, Qualifications Verified, Tutor Approved | TutorVetting | — |
| Calendar opened | Scheduling | TutorVetting calls `TutorCalendar` (Scheduling OHS) |
| Tutor Profile Published (verified subjects only) | TutorCatalog | `TutorStanding` (TutorVetting PL) |
| Hourly Rate Set | TutorCatalog | registered with Pricing via `TutorRateRegistration` (Pricing OHS) |
| Availability Slots Published | Scheduling | shown in search via `FreeSlots` |

#### 2. Book and take a lesson
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Tutor Searched | TutorCatalog (view) | `FreeSlots`, `TutorRating`, `TutorStanding` |
| Lesson Requested, Lesson Price Quoted | Scheduling / Pricing | `LessonQuoting` (Pricing OHS), price frozen at booking |
| Lesson Booked | Scheduling | `LessonOutcomes` to Billing, carrying `FrozenLessonPrice` |
| Card Pre-authorized / Failed | Billing | Stripe `CardPayments` (CF); result via `PaymentGuarantee` (Scheduling OHS) |
| Booking Confirmed / Expired, reschedule, cancel, reminder | Scheduling | `LessonOutcomes` |
| Video Room Opened, Lesson Started/Ended, No-show Recorded, Lesson Failed Technically | Scheduling | `VideoRooms` (ACL) |
| Lesson took place recorded for the tier | Pricing | `LessonQuoting` (Pricing OHS) |
| Student Charged / Late Cancellation Fee Charged / Payment Failed / Unpaid, Commission Deducted, Lesson Earnings Determined | Billing | `LessonOutcomes`, `FrozenLessonPrice`, `CardPayments` |
| Review Requested / Submitted, Tutor Rating Updated | Reviews | `LessonTookPlace`; rating to Pricing via `TutorRatingRegistration`, to the catalog via `TutorRating` |

#### 3. Weekly payout
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Lesson Earnings Determined, Refund Issued (tutor's share) | Billing | `LessonEarnings` |
| Balance kept, negative allowed | Payouts | — |
| Payout Withheld (< 20 EUR, tax forms, suspended) | Payouts | `TutorSuspension` (TutorVetting PL) |
| Weekly Payout Sent / Payout Failed / Payout Retried | Payouts | Stripe `ConnectPayouts` (CF) |

#### 4. Complaint to refund and suspension
| Step | Owning context | Hand-off / relationship |
|---|---|---|
| Complaint Filed, Investigated | Complaints | `LessonRecord` (Scheduling OHS), charges via `Refunds` (Billing OHS) |
| Refund Decided -> Refund Issued | Complaints -> Billing | `Refunds` (Billing OHS) |
| Tutor share deducted | Payouts | `LessonEarnings` |
| Complaint Upheld -> Tutor Suspended | Complaints -> TutorVetting | `ComplaintUpheld` (Complaints PL) |
| Future lessons cancelled by platform, holds released | Scheduling, Billing | `TutorCalendar` (Scheduling OHS), `LessonOutcomes` |
| Tutor hidden; payouts held | TutorCatalog, Payouts | `TutorStanding`, `TutorSuspension` |

### Relationships
| Upstream | Downstream | Patterns | Messages | Justification (rule) |
|---|---|---|---|---|
| Stripe | Billing | OHS, PL / CF | pre-authorise, charge, reverse | Integration rule 1: CF only to a well-established language; expert reply 5.6 "industry standard" |
| Stripe | Payouts | OHS, PL / CF | Connect payouts | same |
| VideoProvider | Scheduling | U / ACL | room, join/leave, started/ended webhooks | Integration rule 3; expert reply 5.6 "wrap it, messy, changed twice" |
| Pricing | Scheduling | OHS, PL / ACL | quote lesson; record lesson took place | Pricing's API in its own language (reply 5.1, 6.1); breaks cycle A |
| Pricing | TutorCatalog | OHS, PL / ACL | register/change hourly rate | reply 6.1; breaks cycle A |
| Pricing | Reviews | OHS, PL / ACL | register tutor rating | reply 6.1; breaks cycle A |
| Pricing | Billing | PL / ACL | frozen price breakdown (inside Lesson Booked) | reply 3.6 (charge = quote) |
| Scheduling | Billing | PL / ACL | LessonOutcomes events | Billing reacts to Scheduling's facts (reply 1.5) |
| Scheduling | Billing | OHS, PL / ACL | payment guaranteed / guarantee failed | reply 5.2; breaks the Billing/Scheduling cycle |
| Scheduling | TutorVetting | OHS, PL / ACL | open / close / reopen tutor calendar | replies 6.2, 7; breaks cycle B |
| Scheduling | Complaints | OHS, PL / ACL | lesson record (who joined, when) | reply 5.5 |
| Scheduling | Reviews | PL / ACL | Lesson Ended (took place) | reply 1.10 (reviews after the lesson) |
| Scheduling | TutorCatalog | OHS, PL / ACL | free slots | reply 1 ("search shows ... free slots"), 3.4 |
| Billing | Complaints | OHS, PL / ACL | refund lesson L amount A; what was charged | reply 5.4 |
| Billing | Payouts | PL / ACL | Lesson Earnings Determined, Refund Issued | replies 2.3, 2.4, 3.10 |
| Complaints | TutorVetting | PL / ACL | Complaint Upheld | reply 5.3 (T&S listens) |
| TutorVetting | TutorCatalog | PL / ACL | Tutor Approved (+subjects), Qualification Expired, Suspended, Reinstated | replies 1.8, 3.5 |
| TutorVetting | Payouts | PL / ACL | Tutor Suspended / Reinstated | reply 3.8 |
| Reviews | TutorCatalog | PL / ACL | Tutor Rating Updated | reply 1.10 |

The two Scheduling -> Billing relationships (LessonOutcomes events, PaymentGuarantee commands) are one upstream/downstream pair with two channels, not two dependencies.

All internal teams work for one product, so relationship rule 5 (Customer/Supplier) describes their collaboration. CML forbids OHS in a Customer/Supplier relationship, so they are modelled as U/D with upstream OHS/PL and downstream ACL. There is no Shared Kernel, Partnership, Separate Ways or unconfirmed relationship.

### Cycle-breaking record
- **Cycle A: Pricing <-> Scheduling** (and Pricing -> Scheduling -> Reviews -> Pricing, Pricing -> Scheduling -> TutorCatalog -> Pricing). Pricing's tier needs lessons that took place, ratings and the hourly rate, all downstream of Scheduling, while Scheduling needs the quote.
  - Flip "Scheduling stops asking for the quote": rejected by the expert (reply 5.1, the quote is part of the booking flow).
  - Flip "take the count from Billing or Reviews": rejected, both learn about lessons from Scheduling, so the cycle stays indirect.
  - Accepted flip ("called context offers an API"): Pricing offers one OHS in its own language; TutorCatalog registers rates, Reviews registers ratings, Scheduling records lessons that took place. Ownership is unchanged. Expert reply 6.1: yes.
- **Cycle B: TutorVetting -> Scheduling -> Complaints -> TutorVetting.** Scheduling -> Complaints (reply 5.5) and Complaints -> TutorVetting (reply 5.3) are expert facts. Accepted flip: Scheduling offers open/close/reopen calendar commands that TutorVetting calls (replies 6.2, 7).
- **Billing <-> Scheduling (pre-authorisation result).** Accepted flip: Scheduling's PaymentGuarantee OHS, called by Billing (reply 5.2).
- Final graph is acyclic: Pricing -> Scheduling -> {Billing, Reviews, Complaints, TutorVetting, TutorCatalog}; Billing -> {Complaints, Payouts}; Complaints -> TutorVetting; TutorVetting -> {TutorCatalog, Payouts}; Reviews -> TutorCatalog.

### Validator output
```
RELATIONSHIPS:
  Stripe [OHS, PL] -> Billing [CF] : CardPayments
RULE conformist-check: Billing conforms to Stripe. OK only if Stripe's language is well established; otherwise it pollutes Billing.
  Stripe [OHS, PL] -> Payouts [CF] : ConnectPayouts
RULE conformist-check: Payouts conforms to Stripe. OK only if Stripe's language is well established; otherwise it pollutes Payouts.
  VideoProvider [] -> Scheduling [ACL] : VideoRooms
  Pricing [OHS, PL] -> Scheduling [ACL] : LessonQuoting
  Pricing [OHS, PL] -> TutorCatalog [ACL] : TutorRateRegistration
  Pricing [OHS, PL] -> Reviews [ACL] : TutorRatingRegistration
  Pricing [PL] -> Billing [ACL] : FrozenLessonPrice
  Scheduling [PL] -> Billing [ACL] : LessonOutcomes
  Scheduling [OHS, PL] -> Billing [ACL] : PaymentGuarantee
  Scheduling [OHS, PL] -> TutorVetting [ACL] : TutorCalendar
  Scheduling [OHS, PL] -> Complaints [ACL] : LessonRecord
  Scheduling [PL] -> Reviews [ACL] : LessonTookPlace
  Scheduling [OHS, PL] -> TutorCatalog [ACL] : FreeSlots
  Billing [OHS, PL] -> Complaints [ACL] : Refunds
  Billing [PL] -> Payouts [ACL] : LessonEarnings
  Complaints [PL] -> TutorVetting [ACL] : ComplaintUpheld
  TutorVetting [PL] -> TutorCatalog [ACL] : TutorStanding
  TutorVetting [PL] -> Payouts [ACL] : TutorSuspension
  Reviews [PL] -> TutorCatalog [ACL] : TutorRating
VALID: 10 bounded contexts, 19 relationships, 0 errors
```
RULE lines: two `conformist-check` lines (Billing and Payouts conform to Stripe), both justified by expert reply 5.6 and a CML comment. No `ud-cycle`, `no-acl`, `isolated`, `shared-kernel` or `partnership` lines.

## Q&A with the domain expert
The full verbatim Q&A (7 facilitator messages, 7 expert replies) is kept in the session transcript; the decisive answers are:
1. **Round 1 (reading + H1):** Trust & Safety owns applying, documents, qualifications and approval with country rules; approval happens once. Students can search without a card but need one to book. Lesson Booked is the commitment. Price = rate x duration + commission (15%, or 10% at rating >= 4.8 and >= 100 lessons); Finance changes the rules every few months; the same calculation gives quote, late fee and final charge. Billing charges after Lesson Ended, no-show or late cancellation (within 1 h, two retries, then unpaid + email); the late fee is 50% under 24 h. No-show is either side, decided from video webhooks (10 min). Payouts pays the balance every Monday via Stripe Connect (20 EUR minimum, tax forms, retry next Monday). Suspension: two reasons, two teams today, "it's a mess"; "We want ONE place deciding whether a tutor may teach, owned by Trust & Safety." Complaints decides refunds, Billing reverses. Reviews (Community): 1-5 with comment, 14 days, average of last 50; rating decides the commission tier. Search has no rules. Scheduling owns 24 h free cancellation and max two reschedules.
2. **Round 2 (H2):** tutors manage profile, rate and slots themselves; rate valid at booking applies. Scheduling team also runs the video part; the provider's webhook model changed twice this year. Payouts owns the balance. Refund after payout comes off the next payout, balance may go negative. Unpaid lessons don't block the student. Only lessons that took place count for the tier. Suspension cancels all future lessons, students not charged. Missing events accepted.
3. **Round 3 (H3 doubts):** the 10-minute rule is Scheduling's and doesn't change on its own; a lesson took place when both joined; disputes go to Complaints. Lesson Started is not a phase change. Technical failure = free reschedule, not counted, decided by Scheduling. Never double-book, first booking wins, booked slots can't be withdrawn. Profile: only verified subjects, EUR, no team owns it. Whole price frozen at booking. Automatic suspension on upheld complaint; reinstated only when both reasons are cleared. Payouts held while suspended; Payouts collects tax forms. Failed pre-auth: 1 h to fix, then Scheduling cancels; free cancellation releases the hold. Refunds reverse the commission too.
4. **Approval:** 8 contexts, A1 and A2 approved; CML wanted (system TutorHub) for four processes; canvases wanted.
5. **Map directions:** Scheduling needs the quote inside booking. Billing reports the pre-auth result to Scheduling. Trust & Safety listens to Complaint Upheld. Billing offers the refund service. Complaints read the lesson record. Stripe as-is (industry standard); wrap the video provider.
6. **Cycles:** yes to Pricing's feeding API; yes to Trust & Safety calling close/reopen calendar.
7. **Open calendar:** yes, Trust & Safety calls "open" on approval.
8. **Map approval:** "Yes, I approve the map." Search price = hourly rate. Canvases requested.

## Open questions and assumptions
- A1 (confirmed): student accounts are a generic account component, not a designed context; the card is Billing's. Where Billing gets the student's email for the "unpaid" email is not decided.
- A2 (confirmed): only lessons that took place (both joined) count towards the 100-lesson tier.
- Search "price" is the tutor's hourly rate, not Pricing's quote (confirmed in the map approval: "search shows what tutors publish").
- Non-blocking questions from critics, for the canvases: can a refund fail and does Complaints hear about it; remedies for tutor-filed complaints; who caps refund amount A; which endings open a review and can reviews be removed; is the pre-authorisation renewed on reschedule; how Payouts learns a tutor's country and Connect account.

## Suggested board changes
- Add: Qualification Renewed, Tutor Reinstated, Complaint Upheld, Complaint Rejected, Refund Decided, Booking Confirmed, Booking Expired, Pre-authorization Failed, Pre-authorization Released, Payment Failed, Lesson Marked Unpaid, Lesson Failed Technically, Payout Withheld, Payout Retried, Hourly Rate Changed.
- Rename: Tutor Earnings Credited -> Lesson Earnings Determined; Late Cancellation Fee Applied -> Late Cancellation Fee Charged.
- Split Lesson Cancelled into on time / late / by platform; merge the two Tutor Suspended stickies into one event with a reason.

## Next steps
- Canvases created via bounded-context-scaffolder (canvas only, no plan or code): `docs/bounded-contexts/tutorhub/{tutorvetting,tutorcatalog,scheduling,pricing,billing,payouts,reviews,complaints}/canvas.md`. Review them, confirm the rules marked "needs GWT", then run the scaffolder per context for its plan.
- Confirm in the Pricing canvas whether commission is added on top of the rate or taken from it (the expert's formula says "+ commission", Billing "deducts commission").
