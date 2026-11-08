---
name: rider-adds-payment-method
intent: rider-adds-payment-method
system: velocity
status: approved
date: 2026-11-08
---

# Spec: A rider wants to add a payment method

Concrete rules, examples and contract for [the intent](../intents/rider-adds-payment-method.md).

## Vocabulary

| Term | Meaning |
|------|---------|
| Payment method | A rider's card, handled by the external payment processor; VeloCity doesn't hold its details (vision "Payment Processors"). |
| Pre-authorization | Holding PLN 1.00 on the card to prove it works, released at once by the processor (DE-70). |
| Verified | The pre-authorization succeeded. |
| Declined | The processor refused the pre-authorization; the card isn't verified (DE-3). |
| Current card | The rider's most recently verified card; the one VeloCity charges (DE-5, DE-71). |

## Rules

### R1: A rider who provided their personal data adds a card, and it is verified by a PLN 1.00 pre-authorization (DE-70)

- **E1.1** Given Anna provided her personal data on 2026-11-08 at 10:08 and has no card, When Anna adds her card ending 4242 at 10:10 and the processor accepts the PLN 1.00 pre-authorization, Then the card ending 4242 is verified and is Anna's current card, the PLN 1.00 is released, and her next onboarding step is signing the user agreement (Payment Method Verified).
- **E1.2** Given Ben verified his phone but hasn't provided his personal data, When Ben adds his card ending 1111, Then the step is rejected, no pre-authorization is made and his next onboarding step is still providing personal data.

### R2: A declined card isn't verified; the rider stays at the payment step and can add another card (DE-3)

- **E2.1** Given Anna provided her personal data and has no card, When Anna adds her card ending 0002 and the processor declines the pre-authorization, Then the card isn't verified, Anna has no current card and her next onboarding step is still adding a payment method (Payment Method Declined).
- **E2.2** Given Anna's card ending 0002 was declined at 10:10, When Anna adds her card ending 4242 at 10:12 and the processor accepts the pre-authorization, Then the card ending 4242 is Anna's current card and her next onboarding step is signing the user agreement (Payment Method Verified).

### R3: After activation, a newly verified card replaces the current card (DE-5)

- **E3.1** Given Anna's account is activated and her current card ends 4242, When on 2026-12-01 Anna adds her card ending 5555 and the processor accepts the pre-authorization, Then the card ending 5555 is Anna's current card and the card ending 4242 is no longer charged (Payment Method Verified).
- **E3.2** Given Anna's account is activated and her current card ends 4242, When Anna adds her card ending 0002 and the processor declines the pre-authorization, Then the card ending 4242 stays Anna's current card (Payment Method Declined).

## Contract

### Commands

| Command | Issued by | Input | On success | Rejected when |
|---------|-----------|-------|------------|---------------|
| Add Payment Method | Rider | rider, card (as known to the payment processor) | Payment Method Verified | R1 (personal data not provided yet); a declined card ends with Payment Method Declined instead (R2) |

### Queries

| Query | Asked by | Input | Result |
|-------|----------|-------|--------|
| None. | | | |

### Events

| Event | When | Data |
|-------|------|------|
| Payment Method Verified | the pre-authorization of an added card succeeded; the card is now the rider's current card | rider, card, verified at |
| Payment Method Declined | the processor declined the pre-authorization of an added card | rider, card, declined at |

## Data

| Shape | Field | Type | Constraint |
|-------|-------|------|------------|
| Payment method | card | Identifier (card at the payment processor) | required; no card details are held by VeloCity (vision) |
| Pre-authorization | amount | Money | PLN 1.00, released at once (DE-70) |

## Out of scope

- Providing personal data (`rider-provides-personal-data`) and signing the user agreement (`rider-signs-user-agreement`).
- A card becoming invalid, the block it causes, and lifting that block with a newly verified card (`finance-blocks-rider-with-invalid-card`).
- Charging rides to the current card (`finance-charges-finished-ride`).
- Refunds.

## Open questions

- ~~**Q1** What is the pre-authorization amount?~~ PLN 1.00, released at once by the processor (role-played domain expert, DE-70, 2026-11-08)
- ~~**Q2** What happens when the pre-authorization is declined?~~ The payment step isn't completed; the rider stays at it and adds another card (role-played domain expert, DE-3, 2026-11-08)
- ~~**Q3** Can a rider replace their card later?~~ Yes, at any time after activation; a newly verified card becomes the one charged (role-played domain expert, DE-5, 2026-11-08)
- **Q4** Can a rider keep several verified cards and choose which one is charged? (R3) DE-5 reads as one current card; E3.1 assumes the old card simply stops being charged.
- **Q5** Is there a limit on declined attempts (for example to stop someone testing stolen cards)? Doesn't block R2's examples.
- **Q6** Can a rider replace their card while onboarding, after it was verified but before signing the agreement? (R3 only covers activated riders.)

## Notes for the plan

- Accepted under the trainer's standing instruction of 2026-11-08 (autonomous run), to be reviewed in the PR.
- The vision says VeloCity doesn't hold card details itself; the card is known only through the payment processor.
