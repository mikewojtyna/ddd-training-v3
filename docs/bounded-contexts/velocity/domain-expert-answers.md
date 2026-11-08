# VeloCity: role-played domain-expert answers (2026-11-08)

> 🎭 **These answers were faked by Claude**, because the trainer asked for a fully autonomous run and couldn't answer
> questions. Each one fills a gap or resolves a contradiction in `docs/domains/velocity/velocity-vision.md`.
> Canvases, specs and plans cite them by ID (`DE-7`). To overrule one, change it here and follow the references.
>
> The vision is deliberately inconsistent. The answers pick one reading so the system can be built; they don't "fix"
> the vision, which stays as it is for the workshop.

## Onboarding and accounts

| ID | Question | 🎭 Answer |
|---|---|---|
| DE-1 | How are email and phone confirmed? | The rider follows the link in the email and types the SMS code. Both are trusted confirmations from our messaging provider; Accounts only records them, in order. |
| DE-2 | What personal data is required, and are there age limits? | Full name, date of birth and country of residency. A rider must be at least **16 years old** on the day they provide the data. Any residency is accepted. |
| DE-3 | What happens when the card's pre-authorization is declined? | The step "payment method verified" isn't reached. The rider stays at the payment-method step and adds another card. |
| DE-4 | Is activation a separate step? | No. The account is **activated automatically the moment the rider signs the user agreement**, because every earlier step is then complete. |
| DE-5 | Can a rider replace their card later? | Yes, at any time after activation. A newly verified card becomes the one we charge. |
| DE-6 | When is a card block lifted? | When the rider adds a new card and it is verified. A debt block is lifted only when the outstanding debt is cleared. The two blocks are independent: one being lifted never lifts the other. |
| DE-7 | Who gets a block notification? | The rider, every time a block is placed (one notification per block reason). |

## Getting a bike

| ID | Question | 🎭 Answer |
|---|---|---|
| DE-10 | How long does a rider's reservation last? | **2 minutes.** |
| DE-11 | How long does a technician's reservation last? | **30 minutes**, because the technician has to get to the bike. |
| DE-12 | What does "standing near a bike" mean? | The rider is within **50 metres** of the bike. Bikes are only ever inside the service area, so being near one is the service-area check. |
| DE-13 | Can a rider hold several bikes? | No: **one rider, one bike.** A rider with a reservation or a running rental session can't reserve another bike. |
| DE-14 | Is the account checked again at unlock? | No. It's checked when reserving; the reservation lasts 2 minutes. |
| DE-15 | Does a failed unlock make the bike broken? | No. The reservation is released and the bike is available again. If the bike really is faulty it signals the fault itself. |
| DE-16 | Who can reserve a broken bike? | Only a technician. Technicians reserve only broken bikes. |
| DE-17 | Does a reservation cost anything? | No. Reservations and expired reservations are free. |
| DE-18 | Where do bikes come from? | A **fleet manager** puts a new bike into service at a station. From then on it is available. |

## Rides and returns

| ID | Question | 🎭 Answer |
|---|---|---|
| DE-20 | What is the maximum rental time? | **12 hours.** At 12:00:00 of riding the session is still a ride; it is ended automatically once it runs longer. |
| DE-21 | Where does an automatically ended session end? | Nowhere known: the bike wasn't returned. The bike is withdrawn as **not returned** and Fleet Maintenance treats it as broken (it has to be recovered and checked). |
| DE-22 | Does a station return need anything from the rider? | The rider names the station they're at. The client-reported location is trusted (reference map decision). |
| DE-23 | Lock failure at a drop-zone return? | Treated exactly like a lock failure at a station: the session ends at that moment, the bike is withdrawn, and **no return fee** applies, wherever the bike is. |
| DE-24 | A rider reports a fault when returning at a station. | The session ends normally (normal fees) and the bike is withdrawn as faulty. |
| DE-26 | Does the 50 m rule apply to technicians? How is a bike that wasn't returned found? | Riders only. A technician reserves a broken bike without giving a position. Finding a bike that wasn't returned, from its last signalled position, is the technician's job and not modelled. |
| DE-25 | The bike signals a fault during a ride. | The ride isn't interrupted. The bike is withdrawn as soon as the session ends. Outside a ride it is withdrawn at once (a reservation on it is cancelled). |

## Billing

| ID | Question | 🎭 Answer |
|---|---|---|
| DE-30 | The tariff at launch? | Free period **15 minutes**; hour 1 **PLN 3.00**; hour 2 **PLN 5.00**; hour 3 **PLN 7.00**; every hour from the 4th on **PLN 9.00**; drop zone fee **PLN 2.00**; unauthorized zone return fee **PLN 40.00**; penalty for exceeding the maximum rental time **PLN 150.00**. |
| DE-31 | The vision says an hour's fee is applied "when it completes". Is a started hour charged? | **Every started billing hour is charged.** "When it completes" was sloppy wording; nobody rides 59 minutes for free. A ride of exactly 15:00 minutes is free; 15:01 starts hour 1. A ride of exactly 75:00 minutes is hour 1 only. |
| DE-32 | What is charged when the lock fails? | The hourly fees up to the failed lock, nothing extra: no return fee, no penalty. |
| DE-33 | What is charged when the maximum rental time is exceeded? | The hourly fees for the full 12 hours plus the penalty. No return fee, because the bike wasn't returned anywhere. |
| DE-34 | Is a station return free of return fees? | Yes. Only free-floating returns pay a drop zone fee or an unauthorized zone return fee. |
| DE-35 | How are failed charges retried? | **3 charge attempts in total**: right after the ride, then 5 minutes after each failed attempt. After the 3rd failure the fee becomes outstanding debt. |
| DE-36 | Can a rider pay their debt themselves? | Yes, from the app: we charge the full outstanding debt to their current card. |
| DE-37 | A ride priced at PLN 0.00? | It is settled at once, without charging the card. |

## Debt recovery

| ID | Question | 🎭 Answer |
|---|---|---|
| DE-40 | How does recovery work? | It starts when a rental fee is unpaid. A **collection attempt** sends the rider a reminder and tries to collect the whole outstanding debt. The first attempt happens when recovery starts, then **every 3 days**. |
| DE-41 | When is recovery exhausted? | After **5 failed collection attempts**. The rider stays blocked and the debt stays outstanding; handing it to an external agency is out of scope. |
| DE-42 | Why is recovery paused, and what happens to the count? | For a dispute, a payment plan being negotiated, or a regulatory hold. Nothing is attempted while paused. On resume the next attempt is due at once; the failed-attempt count is kept. |
| DE-43 | Payment plans? | Negotiated by phone for now. The system only needs to pause recovery while a plan is negotiated. Instalments are out of scope. |
| DE-45 | The debt is cleared after recovery was exhausted, or the "cleared" news arrives twice? | A clearance after exhaustion still counts: the recovery is recorded as succeeded. A second clearance for an already succeeded recovery changes nothing. |
| DE-44 | A second unpaid fee while recovery runs? | It joins the running recovery; the debt grows, the attempt count isn't reset. |

## Customer support

| ID | Question | 🎭 Answer |
|---|---|---|
| DE-50 | How is the incident type detected? | From the category the rider picks: *broken bike* or *can't unlock* with a bike named → **fleet issue**; *wrongful charge* → **billing dispute**; *safety* → **safety concern**; anything else → **other**. A broken-bike report without a bike is **other**. |
| DE-51 | How are incidents assigned? | A support agent picks up an open incident; it is then assigned to them. Only the assigned agent accepts, rejects, escalates or resolves it. |
| DE-52 | When does a fleet issue make the bike broken? | Immediately when the incident is reported and its type detected. Safety first. |
| DE-53 | Escalated to whom? | One of the teams **billing**, **fleet** or **safety**. The assigned agent records the team's resolution. |
| DE-54 | How long can a rider push back on a rejection? | **14 days** from the rejection. A reopened incident can't be rejected again; it must be resolved. |
| DE-56 | Who works a reopened incident? | It stays assigned to the agent who rejected it. The 14-day window counts calendar days: rejected on 1 November, reopening is possible until the end of 15 November. |
| DE-55 | What happens to a fleet-issue incident when the bike is fixed? | It is resolved automatically ("bike fixed"), whatever state it is in, unless it is already resolved. |

## Fleet maintenance

| ID | Question | 🎭 Answer |
|---|---|---|
| DE-60 | Where do fixed bikes go? | The technician brings the bike to a station or a drop zone and returns it to circulation there. |
| DE-61 | Can a bike be unrepairable? | Out of scope for now. |
| DE-62 | Fault report from a technician? | A technician can report a bike broken during a routine check; it is withdrawn from riders at once. |

## Payments

| ID | Question | 🎭 Answer |
|---|---|---|
| DE-70 | What is the pre-authorization amount? | **PLN 1.00**, released at once by the processor. |
| DE-71 | Which card is charged? | The rider's current verified card. A rider whose card was invalidated has none until they add a new one, so a charge fails. |
| DE-73 | An old, replaced card expires. Is the rider blocked? | No. Only invalidating the rider's **current** card leaves them without a way to pay; an old card being invalidated is recorded and changes nothing else. |
| DE-72 | How do we learn a card became invalid? | The payment processor notifies us (expired, cancelled, reported lost). |
