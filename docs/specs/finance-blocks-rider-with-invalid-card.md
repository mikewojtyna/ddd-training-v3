---
name: finance-blocks-rider-with-invalid-card
intent: finance-blocks-rider-with-invalid-card
system: velocity
status: approved
date: 2026-11-08
---

# Spec: Finance wants a rider whose card became invalid blocked from new rentals

Concrete rules, examples and contract for [the intent](../intents/finance-blocks-rider-with-invalid-card.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Current card | The rider's most recently verified card; the one VeloCity charges (DE-5, DE-71). |
| Invalidated | The payment processor reported the card is no longer usable: expired, cancelled or reported lost (DE-72). |
| Block | A reason why a rider can't start new rentals: *invalid card* or *outstanding debt*. A rider can hold both at once, and each is lifted on its own (vision, DE-6). It can be placed during onboarding too (DE-93). |
| Eligible | Activated **and** holding no block (DE-93). |
| Block notification | The message telling the rider a block was placed, why, and what to do next; one per block reason placed (vision, DE-7). |

## Rules

### R1: When the rider's current card is invalidated, the rider is blocked for *invalid card* (vision, DE-72)

- **E1.1** Given Anna's account is activated, not blocked, and her current card ends 4242, When on 2026-11-20 at 08:00 the payment processor reports the card ending 4242 expired, Then the card is invalidated, Anna has no current card and is blocked for *invalid card* (Payment Method Invalidated, Account Blocked).
- **E1.2** Given Anna is riding bike B-17 since 07:50 and her current card ends 4242, When at 08:00 the payment processor reports the card ending 4242 reported lost, Then Anna is blocked for *invalid card* and her ride on B-17 continues (Account Blocked) (vision: a block prevents new rentals; Q5).
- **E1.3** Given Carla's card ending 7777 was verified on 2026-11-18 and she hasn't signed the user agreement, When on 2026-11-20 the payment processor reports the card ending 7777 expired, Then Carla is blocked for *invalid card*, her account stays incomplete, and if she signs on 2026-11-21 she is activated but not eligible (Payment Method Invalidated, Account Blocked) (DE-93).

### R2: The rider gets a block notification for every block reason placed (vision, DE-7)

- **E2.1** Given Anna is not blocked, When she is blocked for *invalid card* at 08:00, Then Anna receives one block notification saying her card is no longer usable and she should add a new card.
- **E2.2** Given Ben is already blocked for *outstanding debt*, When at 09:00 the payment processor reports Ben's current card ending 1111 cancelled, Then Ben is blocked for *invalid card* as well and receives a second block notification, for *invalid card* (Account Blocked).
- **E2.3** Given Anna was blocked for *invalid card* at 08:00 after the card ending 4242 was reported expired, When at 08:30 the payment processor reports the card ending 4242 expired again, Then nothing changes and Anna gets no second notification (DE-94).

### R3: Invalidating a card that is no longer the rider's current card changes nothing else (DE-73)

- **E3.1** Given Anna replaced her card ending 4242 with the card ending 5555, which is now her current card, When the payment processor reports the card ending 4242 expired, Then the card ending 4242 is recorded as invalid, Anna keeps the card ending 5555 as her current card, isn't blocked and gets no notification.

### R4: A newly verified card lifts the *invalid card* block, and only that block (DE-6)

- **E4.1** Given Anna is blocked for *invalid card* only since 2026-11-20, When on 2026-11-21 Anna adds her card ending 5555 and it is verified, Then the card ending 5555 is her current card, the *invalid card* block is lifted and Anna is eligible again (Payment Method Verified, Account Unblocked naming *invalid card*, eligible: yes).
- **E4.2** Given Ben is blocked for *invalid card* and for *outstanding debt*, When Ben adds his card ending 2222 and it is verified, Then the *invalid card* block is lifted and Ben stays blocked for *outstanding debt* (Account Unblocked naming *invalid card*, eligible: no) (DE-94).
- **E4.3** Given Anna is blocked for *invalid card*, When Anna adds her card ending 0002 and its pre-authorization is declined, Then Anna stays blocked for *invalid card* (Payment Method Declined).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Invalidate Payment Method | Payment processor (external, vision "Payment Processors") | card, reason (expired, cancelled, reported lost) | Payment Method Invalidated, Account Blocked | none; an old card only gets recorded (R3) |
| Add Payment Method | Rider (as in `rider-adds-payment-method`) | rider, card | Payment Method Verified, Account Unblocked | none; a declined card ends with Payment Method Declined and the block stays (R4) |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Payment Method Invalidated | the payment processor reported the rider's current card no longer usable | rider, card, reason, invalidated at |
| Account Blocked | a block reason was placed on the rider | rider, reason (*invalid card*), blocked at |
| Account Unblocked | the *invalid card* block was lifted by a newly verified card | rider, lifted reason (*invalid card*), eligible now (Yes/No), unblocked at |
| Payment Method Verified | a newly added card passed the pre-authorization (defined in `rider-adds-payment-method`) | rider, card, verified at |
| Payment Method Declined | a newly added card failed the pre-authorization (defined in `rider-adds-payment-method`) | rider, card, declined at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Card invalidation | reason | one of {expired, cancelled, reported lost} | from the payment processor (DE-72) |
| Block | reason | one of {invalid card, outstanding debt} | a rider holds each reason at most once; placing it again changes nothing (DE-94); only *invalid card* is placed and lifted here |

## Out of scope

- Adding and verifying a card as such (`rider-adds-payment-method`).
- Blocking for outstanding debt and lifting that block (`finance-retries-failed-charge`, `rider-pays-off-outstanding-debt`).
- Refusing a reservation to a blocked rider (`rider-reserves-bike`).
- Charging a rider who has no current card (`finance-charges-finished-ride`).

## Open questions

- ~~**Q1** How does VeloCity learn a card became invalid?~~ The payment processor notifies it (expired, cancelled, reported lost) (role-played domain expert, DE-72, 2026-11-08)
- ~~**Q2** Is a rider blocked when an old, replaced card expires?~~ No; only invalidating the current card leaves them without a way to pay, an old card is recorded and changes nothing else (role-played domain expert, DE-73, 2026-11-08)
- ~~**Q3** Who gets a block notification?~~ The rider, every time a block is placed, one notification per block reason (role-played domain expert, DE-7, 2026-11-08)
- ~~**Q4** When is a card block lifted?~~ When the rider adds a new card and it is verified; card and debt blocks are independent (role-played domain expert, DE-6, 2026-11-08)
- **Q5** Does a running ride continue when its rider is blocked? (E1.2) The vision says a block prevents *new* rentals, so E1.2 lets it continue; the ride's charge will then fail (DE-71).
- ~~**Q6** Is Account Unblocked announced when one reason is lifted but another remains?~~ Yes, every time one reason is lifted, naming it and whether the rider is now eligible (role-played domain expert, DE-94, 2026-11-08); see E4.1, E4.2.
- ~~**Q7** What happens when the card of a rider still in onboarding is invalidated?~~ The *invalid card* block is placed anyway; eligibility needs activated and unblocked, and a newly verified card lifts it (role-played domain expert, DE-93, 2026-11-08); see E1.3.
- ~~**Q8** Can a rider who reserved a bike just before being blocked still unlock it?~~ No, eligibility is checked again at unlock (role-played domain expert, DE-80, revising DE-14, 2026-11-08); specified with the unlock intent.
- ~~**Q9** The processor reports the same card invalid twice: is the second report ignored?~~ Yes, it changes nothing and sends no notification (role-played domain expert, DE-94, 2026-11-08); see E2.3.

## Notes for the plan

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
- DE-72: the payment processor notifies VeloCity (expired, cancelled, reported lost).
- Add Payment Method is the same command as in `rider-adds-payment-method`; this spec only adds the unblocking it causes.
