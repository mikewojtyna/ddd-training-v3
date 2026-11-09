# Answer key: contradictions and gaps in the VeloCity vision (trainer only)

Source: `docs/domains/velocity/velocity-vision.md`. Every quote below is copied verbatim from it (checked with
`grep -F`), including the `**bold**` markers.

**RB** = falls into Returns + Billing, the scope of the exercise. Items without it are useful when a triad wanders off
or when the expert runs prompt card 1.

Not an item: in *Settling the Ride*, the sentence ending "and the account is blocked (see **Account Blocks & Debt
Recovery**)" is complete in the current vision. The dangling text is in *Drop Zone Return* (V-03, V-04).

Types: **contradiction** (two sentences that can't both be true), **gap** (the text is silent where the model needs an
answer), **dangling** (a sentence or bullet that breaks off), **undefined term** (a word the model depends on, never
pinned down).

## Overview

| ID | RB | Type | Topic |
|---|---|---|---|
| V-01 | RB | contradiction | Two ways to end a session, but there are more |
| V-02 | RB | contradiction | "ends anywhere where the city permits it" vs riders can always end their ride |
| V-03 | RB | dangling | Drop-zone lock failure: what follows? |
| V-04 | RB | dangling | Empty bullet at the end of *Drop Zone Return* |
| V-05 | RB | contradiction | Station: lock, then end. Drop zone: end, then lock |
| V-06 | RB | contradiction | Unauthorized zone fee "is a signal" of broken rules, yet it isn't a penalty |
| V-07 | RB | contradiction | Total fee with or without the penalty |
| V-08 | RB | gap | Is a started hour charged? |
| V-09 | RB | contradiction | Quick trips "cost nothing" vs a drop zone fee on every free-floating return |
| V-10 | RB | undefined term | No numbers: free period, fees, maximum rental time, retries |
| V-11 | RB | gap | Session ended by maximum rental time: where is the bike, which return fee, until when do fees run? |
| V-12 | RB | gap | Station lock failure: "not charged extra", but what is charged? |
| V-13 | RB | gap | Prices valid "when the ride started": also return fees, penalty, the zone map? |
| V-14 | RB | contradiction | "We hope that riders will eventually pay" vs every ride is eventually settled |
| V-15 | RB | undefined term | *Settled* means charged, or also "became debt" |
| V-16 | RB | gap | Retries "a few times shortly after the ride" |
| V-17 | RB | gap | Is a station return free of return fees? Is a station inside a drop zone? |
| V-18 | RB | gap | A full station, a station that isn't working |
| V-19 | RB | gap | Rider reports a fault at a station return: does it change the bill? |
| V-20 | RB | gap | Bike left in an unauthorized zone (or outside the service area): who recovers it? |
| V-21 | RB | gap | Debt recovery fails: then what? Blocked forever? |
| V-22 | RB | gap | Wrongful charge, refunds and disputes: no way to correct a settled ride |
| V-23 | RB | gap | A rider blocked during a ride, a second debt during recovery |
| V-24 | | contradiction | Reservation lasts "seconds", but a technician's reservation covers the trip to the bike |
| V-25 | | contradiction | Technician's expired reservation: "exactly as with riders", but a different outcome; who owns the bike then? |
| V-26 | | contradiction | "No unlock without a fully activated account" vs technicians unlocking |
| V-27 | | undefined term | "standing near a bike"; does location validation apply to technicians? |
| V-28 | | undefined term | *Rider* is "activated", yet riders drop out of onboarding; account states overlap |
| V-29 | | gap | Bike states: reserved-while-broken, not returned, fixed |
| V-30 | | gap | Failed unlock: is the bike broken or not? |
| V-31 | | gap | Bike reported broken while reserved or rented |
| V-32 | | gap | Who may a technician reserve? Unrepairable bikes? Who returns a fixed bike? |
| V-33 | | contradiction | Every incident ends resolved, but a rejected incident the rider doesn't push back on |
| V-34 | | gap | Fleet-issue incident: is the bike broken at the report or after the agent accepts? |
| V-35 | | gap | Lifting an invalid-card block; both blocks at once |
| V-36 | | gap | "long-term fleet distribution": announced, never described |

---

## Returns + Billing

### V-01 Two ways to end a session, but there are more (RB, contradiction)

Sections: *Returns*, *Billing*, *Key Business Rules to Know*

> There are two ways to end a rental session, and they are handled differently.

> if a session runs past the **maximum rental time**, the **session is ended automatically**

> If locking **fails**, the session is **ended immediately**

**Why it matters:** the list of "session ended" events and their reasons drives billing (which fees apply) and the bike
state afterwards. A model with only *Station Return* and *Drop Zone Return* misses the automatic end and the forced end
on lock failure.

**Ask:** *"In how many ways can a ride end? Does each one cost the same?"*

### V-02 Where may a ride end? (RB, contradiction)

Sections: *Background*, *Key Business Rules to Know*, *Drop Zone Return*

> The core vision is: **Every ride starts in seconds and ends anywhere where the city permits it.**

> Bikes can be picked up freely, but they must come back to places the city has agreed to.

> Riders can always end their ride; where they end it decides what they pay.

**Why it matters:** either ending in an unauthorized zone is rejected (a rule enforced by a lock or the app), or it is
allowed and priced. The vision's own rules choose pricing, but the core vision says the opposite. Different command
outcomes, different events.

**Ask:** *"Can a rider end a ride outside a station or drop zone? What does the app do?"*

### V-03 Drop-zone lock failure (RB, dangling)

Section: *Drop Zone Return*

> Bike locking can still fail (bike needs to be locked outside the station, too)

Compare with the station flow, which spells out the consequences:

> The rider is not charged extra because our lock failed. The bike is treated as **broken** and waits for a technician.

**Why it matters:** there is no event and no rule for this case. Does the rider still pay the drop zone fee or the
unauthorized zone fee? Is the bike broken? Is it even secured?

**Ask:** *"The bike won't lock in a drop zone. Has the ride ended? What does the rider pay? What happens to the bike?"*

### V-04 Empty bullet at the end of *Drop Zone Return* (RB, dangling)

Section: *Drop Zone Return*, line 163 of the vision: a bullet `- ` with no text, right after

> A much higher **unauthorized zone return fee** is applied.

**Why it matters:** something was meant to follow, perhaps a third zone case (outside the service area?) or the
lock-failure outcome. Good to see whether the AI notices an empty bullet at all.

**Ask:** *"Is there a third case for a free-floating return?"*

### V-05 Order of locking and ending (RB, contradiction)

Sections: *Station Return*, *Drop Zone Return*

> 2. The system attempts to **lock the bike in the station**.
> 3. If locking succeeds, the session ends cleanly.

> 1. The rider **ends the ride** at their current location. The ride always ends where the rider is.

**Why it matters:** at a station the lock comes first and decides the outcome; in a drop zone the ride ends first and
the lock "can still fail" afterwards. The timeline of events differs, and so does what a failed lock can change.

**Ask:** *"Does the ride end when the rider taps 'end', or when the bike is locked?"*

### V-06 Unauthorized zone fee: fee or penalty? (RB, contradiction)

Sections: *Billing*, *Key Business Rules to Know*, *Domain Vocabulary*

> The distinction between an hourly fee and a penalty matters. Hourly fees are the normal cost of the service. A penalty says the rider crossed a rule of the network.

> **The drop zone fee is normal; the unauthorized zone fee is a signal.** One is the price of convenience, the other is the price of breaking the network rules.

> An additional charge applied when a rider breaks a hard rule (e.g., exceeding maximum rental time)

**Why it matters:** by the vision's own definition, the unauthorized zone fee is a penalty ("crossed a rule of the
network"), yet it is always called a fee. "e.g." also suggests more hard rules exist, but only one is named. Whether
it's a penalty decides where it lives (pricing vs a penalty policy) and how the total is composed.

**Ask:** *"Is leaving a bike in an unauthorized zone a penalty? Which other rules carry a penalty?"*

### V-07 Total fee with or without the penalty (RB, contradiction)

Sections: *Billing*, *Domain Vocabulary*

> At the end of the session, the **total fee is calculated** from the applied fees plus any return-related charges.

> The final calculated cost of a rental session, combining hourly fees, return fees, and any penalties

**Why it matters:** the *Billing* section leaves the penalty out of the total, the vocabulary puts it in. Is the penalty
part of the ride's settlement, or charged separately?

**Ask:** *"Is the penalty on the same bill as the ride, or a separate charge?"*

### V-08 Is a started hour charged? (RB, gap)

Section: *Billing*

> Once the free period ends, hour one begins. When it completes, the first-hour fee is applied.

**Why it matters:** read literally, a ride that ends 40 minutes into hour one pays nothing for it. Also unclear: is "hour
one" counted from the start of the ride or from the end of the free period? Without the answer no billing example can
be written.

**Ask:** *"Free period is over, the rider rides 20 more minutes. What do they pay? And at exactly the end of the free
period?"*

### V-09 Free quick trips vs the drop zone fee (RB, contradiction)

Sections: *Billing*, *Drop Zone Return*

> Very quick trips (a few minutes) cost nothing.

> **Approved drop zone.** A **drop zone fee** is applied — small, but real, because free-floating returns cost the operator more to manage than station returns.

**Why it matters:** a 3-minute ride ending in a drop zone: does it "cost nothing" or pay the drop zone fee? Decides
whether the return fee is independent of the billing phases.

**Ask:** *"A 3-minute ride, left in a drop zone. What does the rider pay?"*

### V-10 No numbers anywhere (RB, undefined term)

Sections: *Billing*, *Settling the Ride*

> Every session begins with a short free window.

> The maximum rental time exists so bikes don't get parked on someone's balcony for a week.

**Why it matters:** the free period, the four hourly fees, the drop zone fee, the unauthorized zone fee, the maximum
rental time and the penalty have no values. Are they fixed, or configurable (a tariff that changes over time, see
V-13)? That's a modelling decision, not just a missing number.

**Ask:** *"How long is the free period? The maximum rental time? Who sets the prices and how often do they change?"*

### V-11 Session ended by maximum rental time (RB, gap)

Sections: *Billing*, *Key Business Rules to Know*

> if a session runs past the **maximum rental time**, the **session is ended automatically** and a **penalty is applied** on top of the hourly fees.

> A rental session, once started, belongs to that rider until it is properly returned or forcibly ended by the penalty rule.

**Why it matters:** the bike wasn't returned anywhere. Which return fee applies (none, unauthorized zone)? Are hourly
fees counted up to the limit or until someone notices? What is the bike's state, and who goes to get it? "The penalty
rule" is used as if it were defined.

**Ask:** *"A rider never returns the bike. When does the ride end, what do they pay, and where's the bike?"*

### V-12 Station lock failure: what is charged? (RB, gap)

Section: *Station Return*

> If locking **fails**, the session is **ended immediately** and the rider is told why — the session cannot silently drift. The rider is not charged extra because our lock failed.

**Why it matters:** "not charged extra" doesn't say whether the hourly fees up to that point are charged, or whether
the ride is free as compensation.

**Ask:** *"The station lock fails after a 50-minute ride. What does the rider pay?"*

### V-13 Prices valid when the ride started (RB, gap)

Section: *Billing*

> The rider always pays according to the prices that were valid when the ride started — a price change during a ride never affects that ride.

**Why it matters:** introduces price versions (a tariff with a validity date), but doesn't say if this covers the return
fees and the penalty, or the city's zone map (a drop zone removed mid-ride).

**Ask:** *"The city removes a drop zone while I'm riding to it. Which zone map counts?"*

### V-14 "We hope" vs "every ride is eventually settled" (RB, contradiction)

Sections: *Background*, *Settling the Ride*, *Key Business Rules to Know*

> We hope that riders will eventually pay for their rides.

> We make sure every ride is eventually settled

> **Every ride is eventually settled.** Either the rider is charged successfully, or the unpaid amount becomes outstanding debt.

**Why it matters:** the Background sounds like payment is optional or deferred; the rest is a guarantee. Is there a
model where rides are free or prepaid at launch?

**Ask:** *"Do riders pay for every ride from day one?"*

### V-15 What "settled" means (RB, undefined term)

Sections: *Settling the Ride*, *Domain Vocabulary*

> Once the total fee is known, the ride has to be **settled** — the rider is **charged** for it through their payment method on file.

> Charging the rider for a finished ride; a ride is settled once the charge succeeds or the unpaid amount becomes outstanding debt

**Why it matters:** in the section, settled = charged. In the vocabulary, a ride that became debt is also settled. A
model must decide whether "Ride Settled" means "money received" or "the ride is closed for billing".

**Ask:** *"A charge fails three times and becomes debt. Is that ride settled?"*

### V-16 Retries "a few times shortly after" (RB, gap)

Section: *Settling the Ride*

> if a charge fails, we automatically try again a few times shortly after the ride.

**Why it matters:** how many attempts, at what interval, and when does the amount become debt? Needed for a retry
policy and for the moment the account is blocked.

**Ask:** *"How many times do we retry, how far apart, and when do we give up?"*

### V-17 Station returns and return fees (RB, gap)

Sections: *Billing*, *Drop Zone Return*, *City Operations*

> The city defines where **stations** are placed and where **drop zones** are allowed. Anywhere else counts as an **unauthorized zone**.

**Why it matters:** the vision never says that a station return has no return fee; it only implies it ("cost the
operator more ... than station returns"). It also doesn't say whether a station can lie inside a drop zone, or what
happens when a rider ends a ride free-floating right next to a station.

**Ask:** *"Does a station return cost anything on top of the ride? I end the ride next to a station without docking.
What do I pay?"*

### V-18 Full or broken station (RB, gap)

Section: *Station Return*

> The rider brings the bike back to an official **station**.

**Why it matters:** no capacity, no "station full", no station out of order. Is a full station a lock failure, a reason
to end free-floating, or something the rider must avoid?

**Ask:** *"The station is full. What does the rider do, and what do they pay?"*

### V-19 Fault reported at a station return (RB, gap)

Section: *Broken Bikes & Maintenance*

> A rider reports a fault when returning the bike to a station.

**Why it matters:** does the report change the bill (refund, no fee) or only the bike's state? Does the session end
normally? Can the rider report it at a drop-zone return too?

**Ask:** *"I return a bike with a flat tire and report it. Do I pay normally?"*

### V-20 Bike left in an unauthorized zone (RB, gap)

Sections: *Drop Zone Return*, *Domain Vocabulary*

> **Unauthorized zone.** The bike was left outside the network entirely.

> Anywhere outside the network of stations and drop zones — return is possible but costly

**Why it matters:** is that bike available to the next rider where it stands? Does someone have to fetch it? What if
it's outside the *service area* (a term defined in the vocabulary and used only once in the sections)?

**Ask:** *"After an unauthorized return, can the next rider take the bike? What if it's outside the city?"*

### V-21 Debt recovery fails (RB, gap)

Section: *Account Blocks & Debt Recovery*

> **Fail** — recovery is exhausted (all attempts have been tried and rejected).

> The rider stays blocked until the underlying reason is cleared.

**Why it matters:** after exhaustion, is the debt written off, sold, kept forever? Is the rider blocked for life? What
are "all attempts": how many, how often? Can a rider pay later?

**Ask:** *"Recovery is exhausted. Can the rider ever ride again? What happens to the money?"*

### V-22 Wrongful charges and refunds (RB, gap)

Sections: *Customer Support & Incidents*, *Account Blocks & Debt Recovery*

> Riders can report incidents at any time — a broken brake, a bike they can't unlock, a wrongful charge, an unpleasant experience.

> recovery can be temporarily halted, for example because a dispute is under review

**Why it matters:** a billing dispute can be raised, but nothing describes a refund, a correction of a settled ride or
the link from a support incident to pausing debt recovery. Is "dispute" in debt recovery the same as a "billing dispute"
incident?

**Ask:** *"Support agrees the rider was overcharged. What happens to the money and to the debt recovery?"*

### V-23 Blocked during a ride, a second debt (RB, gap)

Sections: *Account Blocks & Debt Recovery*, *Key Business Rules to Know*

> **A blocked account cannot start new rentals.** Blocks are lifted only when the reason behind them is cleared.

**Why it matters:** a running ride is not a new rental: does it continue? And if a second ride becomes debt while
recovery is running or paused, does it join that recovery or start a new one?

**Ask:** *"My card is cancelled while I'm riding. Can I finish the ride? I already owe money and another ride fails."*

---

## Other parts of the vision

### V-24 How long does a reservation last? (contradiction)

Sections: *Renting a Bike*, *Broken Bikes & Maintenance*, *Key Business Rules to Know*

> The reservation is short-lived; it exists only to prevent someone else from grabbing the same bike in the seconds it takes to unlock.

> Reservations exist to guarantee this during the seconds between "I picked this bike" and "I unlocked this bike."

> A technician **reserves the bike**. The reservation keeps anyone else from grabbing it while the technician is on the way.

**Why it matters:** "seconds" vs the time a technician needs to travel to the bike. One reservation concept with one
duration, or two? Note: the vision gives no duration at all. A 15-minute reservation appears only in a ticket on
another branch, not in the vision, so don't treat it as the vision's answer.

**Ask:** *"How long does a reservation last, for a rider and for a technician?"*

### V-25 Technician's expired reservation (contradiction)

Section: *Broken Bikes & Maintenance*

> Things can go wrong here too, exactly as with riders:

> **The reservation expires** before the technician unlocks the bike. The bike is still broken, so it is not offered to riders — it simply waits until a technician reserves it again.

Compare with the rider case:

> **The reservation expires** before the rider unlocks the bike. The bike returns to the pool and is available to anyone.

**Why it matters:** "exactly as with riders", but the outcome differs. Who notices the expiry (a timer, the technician,
a dispatcher)? Who owns the broken bike in between? Does anyone get reminded that it's waiting?

**Ask:** *"A technician's reservation runs out. Who knows, and who makes sure the bike gets picked up?"*

### V-26 Technicians and activated accounts (contradiction)

Sections: *Who Uses the System*, *Key Business Rules to Know*

> A technician may reserve and unlock a bike just like a rider can, but they do it to repair the bike, not to ride it.

> **No unlock without a fully activated account.** Every step of onboarding — email, phone, personal data, verified payment method, signed agreement — must be complete before a session can begin.

**Why it matters:** read together, a technician needs a verified payment method to unlock. Are technicians riders with
accounts, or a different kind of user with a different check?

**Ask:** *"Does a technician go through onboarding? Do they need a card?"*

### V-27 "Near a bike" (undefined term)

Section: *Renting a Bike*

> The rider's **location is validated** — they must be inside VeloCity's service area, standing near a bike.

**Why it matters:** "near" has no distance. And the maintenance flow "reuses the same steps riders use", yet a
technician reserves a bike while "on the way", so cannot be near it.

**Ask:** *"How near is near? Does a technician have to stand next to the bike to reserve it?"*

### V-28 Who is a rider; account states (undefined term)

Sections: *Who Uses the System*, *User Onboarding*, *Domain Vocabulary*

> A registered, activated individual who can rent VeloCity bikes

> If the rider drops out mid-onboarding, their account stays in an incomplete state until they finish the missing step.

> A rider's identity in VeloCity; can be incomplete, activated, or blocked

**Why it matters:** the vocabulary says a rider is activated; the onboarding section calls a person who isn't yet a
rider. "Incomplete, activated, or blocked" reads as one state, but a blocked account was activated first, and a
rider can be blocked for two reasons at once.

**Ask:** *"Is a blocked rider still activated? Can a blocked account be incomplete?"*

### V-29 Bike states (gap)

Sections: *Domain Vocabulary*, *Broken Bikes & Maintenance*

> A physical vehicle in the VeloCity fleet; it can be available, reserved, rented, broken, or in maintenance

**Why it matters:** a broken bike reserved by a technician is both "broken" and "reserved". A bike from an
automatically ended session (V-11) or left in an unauthorized zone (V-20) has no state. "Bike Fixed" is a moment, but
steps 5 and 6 of maintenance are separate, so is there a fixed-but-not-returned state?

**Ask:** *"What state is a broken bike in once a technician has reserved it?"*

### V-30 Failed unlock: broken or not? (gap)

Sections: *Renting a Bike*, *Broken Bikes & Maintenance*

> **The unlocking fails** (mechanical issue, connectivity problem, or for any other reason). The reservation is then released and the rider is free to try another bike.

> The bike itself signals a problem (for example, a failed lock or a mechanical fault detected at the station).

**Why it matters:** a failed unlock caused by a "mechanical issue" sounds like a fault, yet the rider flow simply
releases the reservation. Is the bike offered to the next rider?

**Ask:** *"A rider's unlock fails. Can the next rider try the same bike?"*

### V-31 Broken while reserved or rented (gap)

Section: *Broken Bikes & Maintenance*

> From that moment on, the bike is broken and riders can no longer rent it.

**Why it matters:** an incident or a bike signal can arrive while a rider holds a reservation or is mid-ride. Is the
reservation cancelled? Is the ride interrupted?

**Ask:** *"The bike reports a fault during a ride. What happens to the ride?"*

### V-32 Technician scope (gap)

Section: *Broken Bikes & Maintenance*

> The **bike is returned** to circulation — it is back at a station or in a drop zone and available to riders again.

**Why it matters:** can a technician reserve a bike that isn't broken? Can a technician report a bike broken? What if
it can't be fixed? Who brings it back, and which station or drop zone?

**Ask:** *"Can a bike be beyond repair? Who decides where a fixed bike goes?"*

### V-33 Every incident resolved (contradiction)

Sections: *Customer Support & Incidents*, *Key Business Rules to Know*

> If **rejected**, the case does not disappear. The rider can push back, and the incident may be **reopened**. A reopened incident is eventually **resolved**.

> Every incident, no matter how it was routed, must end in a **resolved** state. Nothing stays open forever.

> **Every incident has a terminal state.** Accepted, rejected, escalated, or reopened — the flow always converges on resolved.

**Why it matters:** a rejected incident the rider doesn't push back on is never resolved, so it stays open forever.
Needs a deadline or an automatic resolution.

**Ask:** *"An incident is rejected and the rider never answers. When is it resolved?"*

### V-34 Fleet issue: when is the bike broken? (gap)

Section: *Customer Support & Incidents*

> When an incident is about a faulty bike (a fleet issue), the bike is marked as **broken** and handed over to technicians for maintenance.

**Why it matters:** at the moment the type is detected, or only after an agent accepts the incident? A rejected fleet
issue: is the bike available again?

**Ask:** *"A rider reports a broken brake. Is the bike out of service before an agent looks at it?"*

### V-35 Lifting an invalid-card block (gap)

Section: *Account Blocks & Debt Recovery*

> **Invalid card.** The payment method on file is no longer usable (expired, canceled, reported lost).

**Why it matters:** only the debt block has an unblocking path ("the rider pays and the account is unblocked"). What
clears an invalid-card block? If both reasons apply, does paying the debt unblock the rider?

**Ask:** *"My card expired and I add a new one. Am I unblocked? And if I also owe money?"*

### V-36 Fleet distribution (gap)

Section: *City Operations*

> VeloCity honors this map — it shapes returns, pricing, and long-term fleet distribution.

**Why it matters:** rebalancing bikes between stations is announced and never described. A whole subdomain may be
missing.

**Ask:** *"Who moves bikes when one station is empty and another is full?"*

---

## What AI typically misses or invents

- **Inventing rules.** It fills gaps with plausible defaults ("15-minute reservation", "free period of 30 minutes",
  "3 retries") and presents them as if they were in the text. Ask it to quote; invented rules have no quote.
- **Smoothing over contradictions.** It reconciles V-24 or V-33 with a sentence like "reservations are short, but
  technicians get longer ones" and moves on, instead of listing the contradiction as a question.
- **Treating the vocabulary as authoritative.** It answers from *Domain Vocabulary* when the sections disagree (V-07,
  V-15, V-28). The vocabulary is just another inconsistent source.
- **Missing dangling text.** The empty bullet (V-04) and the drop-zone lock sentence (V-03) are easy to read past;
  models often summarise them away.
- **Paraphrasing inside quotes.** "Quote verbatim" isn't always honoured. Spot-check a quote with search.
- **Over-reporting.** Long lists of generic questions ("what about GDPR?", "what about multiple currencies?") that have
  nothing to do with the board. Prompt card 2 asks for at most 10, tied to stickies.
- **Answering instead of asking.** Without the "don't answer" instruction, the agent plays the expert itself. Then the
  triad models the AI's guesses, not the business.
- **Misreading the board.** Screenshots of small stickies lose words and colours; the agent may "find gaps" that are
  only unreadable stickies.

## Decisions the reference run made

The autonomous run on the branch `velocity-full-implementation` had Claude role-play the expert. Its answers are in
`docs/bounded-contexts/velocity/domain-expert-answers.md` (on master). Useful to show that each item above needs a
decision, and that the AI's decisions are only one reading:

| Item | Decision in the reference run |
|---|---|
| V-24 | DE-10, DE-11: rider reservation 2 minutes, technician 30 minutes. |
| V-27 | DE-12, DE-26: "near" = within 50 metres; riders only, technicians reserve without a position. |
| V-26 | DE-26 and DE-16: technicians are a separate actor; only technicians reserve broken bikes. |
| V-08 | DE-31, DE-83: every started billing hour is charged ("when it completes" was sloppy wording); time to the second. |
| V-09 | DE-84: the return fee depends only on where the ride ends; a 10-minute ride ended in a drop zone pays the drop zone fee. |
| V-10 | DE-20, DE-30: maximum rental time 12 hours; free period 15 minutes; PLN 3 / 5 / 7 / 9 per hour; drop zone fee PLN 2; unauthorized zone fee PLN 40; penalty PLN 150. |
| V-11 | DE-21, DE-33, DE-82: the session ends at the 12-hour mark; fees for 12 hours plus the penalty, no return fee; the bike is withdrawn as not returned. |
| V-12, V-03 | DE-23, DE-32: a lock failure at a drop zone is treated like one at a station; hourly fees up to that moment, no return fee, no penalty. |
| V-17 | DE-34: station returns pay no return fee. |
| V-13 | DE-84: the zone map valid at the end of the ride is used; DE-96: tariffs are published with a future *valid from*. |
| V-16 | DE-35: 3 charge attempts, 5 minutes apart; then outstanding debt. |
| V-21 | DE-40, DE-41: a collection attempt every 3 days; exhausted after 5 failed attempts; the rider stays blocked. |
| V-23 | DE-86 (no withdrawal interrupts a ride), DE-88: a new debt joins an open recovery, running or paused. |
| V-31 | DE-25, DE-86: a ride is never interrupted; the bike is withdrawn when the session ends. |
| V-30 | DE-15, DE-85: a failed unlock alone doesn't make the bike broken; a rider's *can't unlock* report does. |
| V-33 | DE-54, DE-87: 14 days to push back; then resolved automatically as "rejection not contested". |
| V-34 | DE-52: the bike is broken as soon as the fleet-issue incident is reported. |
| V-35 | DE-6: a newly verified card lifts the card block; the two blocks are independent. |
| V-29, V-32 | DE-61, DE-91: unrepairable bikes out of scope; returning the bike to circulation is the moment it is fixed. |

Not decided in the reference run: V-02, V-06, V-07, V-14, V-18, V-22 (refunds), V-36. Good material for the debrief:
the AI built a whole system without ever asking whether the unauthorized zone fee is a penalty.
