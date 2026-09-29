# Domain Expert Guide for VeloCity — City Bike Rental

> **Workshop role instructions**: You are playing the role of a domain expert - a senior product manager at a European city bike-share operator who has been deeply involved in designing VeloCity. You know the business inside-out. Answer questions naturally, using the language of the business. You do NOT need to mention technical terms, classes, or patterns. Just describe how the business works.
>
> **You do not need to read the entire document at once. Start with reading the `Background` section and just skim over the rest of the document. When being asked a question or in doubt, refer to the corresponding section in the document to understand the details. At the end of the file you can find the `Domain Vocabulary`. Have fun!**

---

## Background

VeloCity is a new city bike rental service we are designing from the ground up. Our ambition is to give every resident and visitor a bike within a short walk of wherever they are, and let them ride to wherever they need to go — with as little friction as possible between the moment they want a bike and the moment they are on one.

The core vision is: **"Bikes are for freedom, not friction — every ride starts in seconds and ends where the city wants it."**

Two ideas shape everything we are building:

1. **Trust is earned once, then honored.** A rider proves who they are and how they will pay when they join VeloCity. After that, unlocking a bike is a single tap.
2. **The city sets the shape of the network.** Bikes can be picked up freely, but they must come back to places the city has agreed to.

We serve **individual riders** only. There is no corporate or group offering at launch.

---

## Who Uses the System

### Riders

A rider is a person who wants to move through the city on a bike. They register once, complete onboarding, and from then on can walk up to any available VeloCity bike and start a ride. Riders interact with VeloCity primarily through the mobile app.

### Support Agents

Support agents work for VeloCity. They handle incidents reported by riders, decide whether to accept or reject each report, and follow up until every incident reaches a resolved state.

### Technicians

Technicians work for VeloCity. They look after broken bikes. A technician may reserve and unlock a bike just like a rider can, but they do it to repair the bike, not to ride it. Once the bike is fixed, the technician puts it back into circulation.

### City Operations

The city itself is a stakeholder, not a user. The city defines where **stations** are placed and where **drop zones** are allowed. Anywhere else counts as an **unauthorized zone**. VeloCity honors this map — it shapes returns, pricing, and long-term fleet distribution.

### Payment Processors

VeloCity does not hold card details itself. Payment methods are added, verified, and later charged through an external payment processor. From the domain's perspective, a payment method is either verified or it isn't, and a charge either succeeds or it fails.

---

## User Onboarding

Before anyone can unlock a bike, they go through an onboarding sequence. Each step must succeed before the next begins — we never skip ahead. The steps are:

1. The person **registers** with VeloCity (creates an account).
2. Their **email is confirmed**.
3. Their **phone number is verified**.
4. They provide **basic personal data** (name, date of birth, residency).
5. They **add a payment method**.
6. The payment method is **verified** (a small pre-authorization against the card).
7. They **sign the user agreement** (terms of service, liability, city rules).
8. The **account is activated**.

Only an activated account can reserve and unlock a bike. This sequential trust-building is deliberate: an unverified card must never be able to start a ride. If the rider drops out mid-onboarding, their account stays in an incomplete state until they finish the missing step. They can resume later.

---

## Renting a Bike

Renting a bike is meant to feel instant, but a lot happens under the surface.

1. The rider's **location is validated** — they must be inside VeloCity's service area, standing near a bike.
2. The bike is **reserved** for that rider. The reservation is short-lived; it exists only to prevent someone else from grabbing the same bike in the seconds it takes to unlock.
3. The rider **unlocks the bike**.
4. The **rental session starts**.

Two things can go wrong before a session starts:

- **The reservation expires** before the rider unlocks the bike. The bike returns to the pool and is available to anyone.
- **The unlock fails** (mechanical issue, connectivity problem). The reservation is then released and the rider is free to try another bike.

A rental session, once started, belongs to that rider until it is properly returned or forcibly ended by the penalty rule.

Riders are not the only ones who reserve and unlock bikes. Technicians go through the same reserve-and-unlock steps when they pick up a broken bike — see **Broken Bikes & Maintenance** below.

---

## Broken Bikes & Maintenance

Bikes break. A flat tire, a loose brake, a lock that no longer responds. When that happens, the bike must stop being offered to riders and must get fixed as quickly as possible.

A bike can be **reported broken** in several ways:

- A rider reports a fault when returning the bike to a station.
- A rider reports an incident about the bike (e.g. a broken brake) to customer support.
- The bike itself signals a problem (for example, a failed lock or a mechanical fault detected at the station).

From that moment on, the bike is broken and riders can no longer rent it.

The maintenance flow reuses the same steps riders use to get a bike:

1. The **bike is reported broken**.
2. A technician **reserves the bike**. The reservation keeps anyone else from grabbing it while the technician is on the way.
3. The technician **unlocks the bike**.
4. **Maintenance is started.** Unlike a rider's unlock, a technician's unlock never starts a rental session.
5. The **bike is fixed**.
6. The **bike is returned** to circulation — it is back at a station or in a drop zone and available to riders again.

Things can go wrong here too, exactly as with riders:

- **The reservation expires** before the technician unlocks the bike. The bike is still broken, so it is not offered to riders — it simply waits until a technician reserves it again.
- **The unlock fails.** The reservation is released, the bike stays broken, and a technician tries again later.

Maintenance is never billed. It is not a ride — it is how we keep the fleet healthy.

---

## Billing

VeloCity uses **time-based, tiered pricing**. The idea is simple: encourage short trips, let long trips get progressively more expensive, and never let a bike be held forever.

A rental session moves through the following billing phases:

1. **Free period.** Every session begins with a short free window. Very quick trips (a few minutes) cost nothing. This is deliberate — it keeps the barrier to using VeloCity essentially zero.
2. **First hour billing period.** Once the free period ends, hour one begins. When it completes, the first-hour fee is applied.
3. **Second hour billing period** with its second-hour fee.
4. **Third hour billing period** with its third-hour fee.
5. **Fourth hour and beyond.** From this point the same hourly fee is applied for each additional hour.

At the end of the session, the **total fee is calculated** from the applied fees plus any return-related charges. The rider always pays according to the prices that were valid when the ride started — a price change during a ride never affects that ride.

There is a hard ceiling: if a session runs past the **maximum rental time**, the **session is ended automatically** and a **penalty is applied** on top of the hourly fees. The maximum rental time exists so bikes don't get parked on someone's balcony for a week. It is a business signal, not a technical timeout: it means "at this point, this is no longer a rental — it is a problem."

The distinction between an hourly fee and a penalty matters. Hourly fees are the normal cost of the service. A penalty says the rider crossed a rule of the network.

### Settling the Ride

Once the total fee is known, the ride has to be **settled** — the rider is **charged** for it through their payment method on file. We make sure every ride is eventually settled: if a charge fails, we automatically try again a few times shortly after the ride. If the money still cannot be collected, the unpaid amount stays on the rider's account as **outstanding debt**, and the account is blocked (see **Account Blocks & Debt Recovery**).

---

## Returns

There are two ways to end a rental session, and they are handled differently.

### Station Return

The rider brings the bike back to an official **station**.

1. The bike is **returned to the station**.
2. The system attempts to **lock the bike in the station**.
3. If locking succeeds, the session ends cleanly. If the bike shows signs of a fault (damage, mechanical issue detected), a **fault is reported**. The bike is then considered broken, is no longer offered to riders, and waits for a technician (see **Broken Bikes & Maintenance**).
4. If locking **fails**, the rider is prompted to **contact customer support** — the session cannot silently drift. Every ended session must have a clear resolution.

### Drop Zone Return (Free-Floating)

VeloCity is not station-only. The city has approved certain **drop zones** where a bike may be left without a station.

1. The rider **ends the ride** at their current location. The ride always ends where the rider is — we never leave a rider stranded.
2. The location is then checked against the city's zone map to decide what the rider pays:
   - **Approved drop zone.** A **drop zone fee** is applied — small, but real, because free-floating returns cost the operator more to manage than station returns.
   - **Unauthorized zone.** The bike was left outside the network entirely. A much higher **unauthorized zone return fee** is applied. This is how pricing enforces the shape of the network without requiring physical infrastructure everywhere.

---

## Account Blocks & Debt Recovery

A rider's account can be **blocked**, preventing new rentals. Two situations trigger this:

- **Invalid card.** The payment method on file is no longer usable (expired, canceled, reported lost).
- **Outstanding debt.** The rider owes VeloCity money from a previous ride that could not be charged, even after retrying (see **Settling the Ride**).

Whenever an account is blocked, a **block notification** is sent to the rider so they know what happened and what to do next.

If the block is due to owed money, the **debt recovery process** begins. It is different from the automatic retries right after a ride: those are quick, silent charge attempts on the card. Debt recovery is an active pursuit of the money — reminding the rider, asking them to pay or update their card, negotiating payment plans, and handling disputes. This process can:

- **Succeed** — the rider pays and the account is unblocked.
- **Fail** — recovery is exhausted (all attempts have been tried and rejected).
- **Pause and later resume** — recovery can be temporarily halted, for example because a dispute is under review, a payment plan is being negotiated, or a regulatory hold applies. Once the pause is lifted, recovery resumes and eventually reaches success or failure.

The rider stays blocked until the underlying reason is cleared. Debt recovery is initiated automatically, but the ability to pause it is important: it lets the business handle disputes and exceptional cases without abandoning the process entirely.

---

## Customer Support & Incidents

Riders can report incidents at any time — a broken brake, a bike they can't unlock, a wrongful charge, an unpleasant experience.

The incident flow is:

1. An **incident is reported** by the rider.
2. Its **type is detected** (fleet issue, billing dispute, safety concern, etc.) so it can be routed correctly.
3. It is **assigned to a support agent**.
4. The agent either **accepts** or **rejects** the incident.
   - If **accepted**, the agent either **resolves** it directly, or **escalates** it to a specialized team and it is resolved from there.
   - If **rejected**, the case does not disappear. The rider can push back, and the incident may be **reopened**. A reopened incident is eventually **resolved**.

Every incident, no matter how it was routed, must end in a **resolved** state. Nothing stays open forever.

When an incident is about a faulty bike (a fleet issue), the bike is marked as **broken** and handed over to technicians for maintenance.

---

## Key Business Rules to Know

- **A bike can never be in two active rental sessions at the same time.** Reservations exist to guarantee this during the seconds between "I picked this bike" and "I unlocked this bike."
- **No unlock without a fully activated account.** Every step of onboarding — email, phone, personal data, verified payment method, signed agreement — must be complete before a session can begin.
- **A rental session must eventually end.** Either through a proper return, or automatically when the maximum rental time is exceeded — with a penalty. There is no such thing as an eternal ride.
- **Every ride is eventually settled.** Either the rider is charged successfully, or the unpaid amount becomes outstanding debt.
- **Returns outside authorized zones cost more.** The shape of the VeloCity network is enforced by pricing, not by locks. Riders can always end their ride; where they end it decides what they pay.
- **The drop zone fee is normal; the unauthorized zone fee is a signal.** One is the price of convenience, the other is the price of breaking the network rules.
- **Debt collection runs automatically, but can be paused.** Disputes, regulatory holds, and negotiated payment plans are legitimate reasons to pause without giving up on recovery.
- **Every incident has a terminal state.** Accepted, rejected, escalated, or reopened — the flow always converges on resolved.
- **A blocked account cannot start new rentals.** Blocks are lifted only when the reason behind them is cleared.
- **A broken bike is never offered to riders.** It stays out of circulation until a technician fixes it.
- **Only riders and technicians reserve and unlock bikes.** A rider's unlock starts a rental session; a technician's unlock starts maintenance.
- **A bike returns to circulation only after it is fixed.** Maintenance is never billed.

---
<div style="page-break-after: always;"></div>

## Domain Vocabulary

| Term | Meaning |
|------|---------|
| **Rider** | A registered, activated individual who can rent VeloCity bikes |
| **Account** | A rider's identity in VeloCity; can be incomplete, activated, or blocked |
| **Onboarding** | The sequential trust-building steps a person completes before their account is activated |
| **Payment Method** | A card or equivalent linked to a rider's account and verified through a pre-authorization |
| **Technician** | A VeloCity employee allowed to reserve and unlock bikes in order to repair them |
| **Reservation** | A brief, exclusive hold on a specific bike for a specific rider or technician between selection and unlock |
| **Rental Session** | A confirmed, active ride from unlock until proper return or forced end |
| **Bike** | A physical vehicle in the VeloCity fleet; it can be available, reserved, rented, broken, or in maintenance |
| **Broken Bike** | A bike reported as faulty; it is not offered to riders until it is fixed |
| **Maintenance** | The work a technician does on a broken bike after unlocking it; never billed |
| **Bike Fixed** | The moment maintenance succeeds and the bike is ready to return to circulation |
| **Station** | An official location where bikes are docked and locked |
| **Drop Zone** | A city-approved area where a bike may be left without a station |
| **Unauthorized Zone** | Anywhere outside the network of stations and drop zones — return is possible but costly |
| **Service Area** | The overall geographic region where VeloCity operates |
| **Free Period** | The initial minutes of a rental session during which no fee is applied |
| **Hourly Fee** | The per-hour charge applied after the free period, tiered across hours 1, 2, 3, and 4+ |
| **Drop Zone Fee** | The small fee applied for ending a ride in an approved drop zone rather than a station |
| **Unauthorized Zone Return Fee** | The higher fee applied for ending a ride outside all approved locations |
| **Total Fee** | The final calculated cost of a rental session, combining hourly fees, return fees, and any penalties |
| **Maximum Rental Time** | The upper limit beyond which a session is treated as a problem rather than a ride |
| **Penalty** | An additional charge applied when a rider breaks a hard rule (e.g., exceeding maximum rental time) |
| **Settlement** | Charging the rider for a finished ride; a ride is settled once the charge succeeds or the unpaid amount becomes outstanding debt |
| **Outstanding Debt** | Money a rider owes VeloCity for a ride that could not be charged |
| **Fault Report** | A record that a bike shows signs of damage or malfunction and makes it a broken bike until a technician fixes it |
| **Account Block** | A state that prevents a rider from starting new rentals, triggered by an invalid card or outstanding debt |
| **Debt Recovery Process** | The business process that pursues an outstanding balance until it is paid, exhausted, or paused |
| **Incident** | A rider-reported issue that must be routed, worked, and resolved by support |
| **Escalation** | Handing an accepted incident to a specialized team for resolution |
| **Reopening** | Reactivating a rejected incident so it can be worked again toward a resolution |
