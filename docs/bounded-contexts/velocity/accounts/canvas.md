# Bounded Context Canvas: Accounts

<!--
Based on the ddd-crew Bounded Context Canvas v5: https://github.com/ddd-crew/bounded-context-canvas
Written by Claude on 2026-11-08 from docs/domains/velocity/velocity-vision.md ("User Onboarding",
"Account Blocks & Debt Recovery"), docs/domains/velocity/velocity-context-map.md and src/main/cml/VeloCity-Context-Map.cml,
during an autonomous run. Gaps were filled with role-played domain-expert answers (DE-n), see
docs/bounded-contexts/velocity/domain-expert-answers.md.
-->

## Name
Accounts

## Purpose
Earns the rider's trust once: walks a person through onboarding in a fixed order until their account is activated,
and keeps track of whether an activated rider may currently rent, by placing and lifting account blocks with a
reason. Everyone else asks Accounts one question: *may this rider rent right now?*

## Strategic Classification
- Domain: supporting
- Business model: compliance
- Evolution: custom built

## Domain Roles
- Approver: decides whether a rider may start rentals (activated and not blocked).

## Inbound Communication
| Collaborator | Type (Query/Command/Event) | Message | Notes |
|---|---|---|---|
| Rider app | Command | Register | Creates an incomplete account |
| Rider app | Command | Confirm Email | Onboarding step 2 |
| Rider app | Command | Verify Phone | Onboarding step 3 |
| Rider app | Command | Provide Personal Data | Onboarding step 4 |
| Rider app | Command | Add Payment Method | Onboarding steps 5 and 6 (verification through Payments); also replaces a card later |
| Rider app | Command | Sign User Agreement | Onboarding step 7, activates the account (DE-4) |
| Rider app | Query | Account Status | Onboarding progress, activation, blocks |
| Rentals | Query | Rental Eligibility | "May this rider rent right now?" (OHS) |
| Settlement | Command | Block Account (reason: outstanding debt) | OHS; Accounts knows no Settlement events |
| Settlement | Command | Unblock Account (reason: outstanding debt) | OHS |
| Payments | Event | Payment Method Invalidated | Accounts blocks the account with reason *invalid card* |

## Outbound Communication
| Message | Type (Query/Command/Event) | Collaborator | ACL? (default: yes) |
|---|---|---|---|
| Verify Payment Method | Command | Payments | yes: Accounts conforms to Payments' language (CF in the map); the adapter still lives in an `@AntiCorruptionLayer` package, the repo's single place for cross-context calls |
| Block Notification | Command | Rider notifications (messaging provider) | yes: port `RiderNotifications`, adapter in `infra/adapters/secondary/messagingprovideracl` |
| Account Activated, Account Blocked, Account Unblocked | Event | none outside Accounts yet | n/a |

## Ubiquitous Language
- **Rider**: a person with an account at VeloCity.
- **Account**: a rider's identity; it is *incomplete*, *activated*, or activated and *blocked*.
- **Onboarding**: the fixed sequence of steps from registration to activation; a step is accepted only when every earlier step is complete.
- **Onboarding step**: registered → email confirmed → phone verified (records the phone number given at that step, DE-98) → personal data provided → payment method added → payment method verified → user agreement signed → activated.
- **Next onboarding step**: the first onboarding step the account hasn't completed yet; right after registration it is confirming the email.
- **Payment method**: the rider's card, known to us only by the reference Payments gives us.
- **Verified**: Payments pre-authorized the card successfully.
- **Activated**: every onboarding step is complete; set automatically when the agreement is signed.
- **Block**: a reason why a rider can't start rentals (placed during onboarding too, DE-93): *invalid card* or *outstanding debt*. An account can hold both at once.
- **Rental eligibility**: activated and holding no block.

## Business Decisions

### A person registers with an email address (spec: person-registers-with-velocity#E1.1)
- **Given** nobody has registered with anna@example.com
- **When** Anna registers with anna@example.com on 2026-11-08 at 10:00
- **Then** Anna has an incomplete account (Account Registered)

### A new account waits for the email confirmation (spec: person-registers-with-velocity#E2.1)
- **Given** Anna registered at 10:00
- **When** she looks at her account status at 10:01
- **Then** her account is incomplete and her next onboarding step is confirming her email

### An incomplete account waits without a deadline (spec: person-registers-with-velocity#E3.1)
- **Given** Anna registered on 2026-11-01 and did nothing else
- **When** she opens the app on 2026-11-20
- **Then** her account is still incomplete and her next onboarding step is confirming her email

### One account per email address (spec: person-registers-with-velocity#E4.1)
- **Given** Anna registered with anna@example.com
- **When** Ben registers with anna@example.com
- **Then** the registration is rejected and no second account is created (DE-98)

### Onboarding steps are completed in order (spec: rider-confirms-contact-details#E2.2, person-registers-with-velocity#E2.2)
- **Given** Anna registered with anna@example.com and hasn't confirmed her email
- **When** Anna verifies her phone +48 600 100 200
- **Then** the step is rejected and Anna's account still waits for the email confirmation
- *Second example (person-registers-with-velocity#E2.2):* Given Anna registered and hasn't confirmed her email, When she provides her personal data, Then the step is rejected and her next onboarding step is still confirming her email.

### Following the emailed link confirms the email (spec: rider-confirms-contact-details#E1.1)
- **Given** Anna registered with anna@example.com at 10:00
- **When** she follows the confirmation link at 10:03
- **Then** her email is confirmed and her next onboarding step is verifying her phone (Email Confirmed)

### Confirming the email twice changes nothing (spec: rider-confirms-contact-details#E1.2)
- **Given** Anna confirmed her email at 10:03
- **When** she follows the same link again at 10:20
- **Then** nothing changes and no second Email Confirmed is recorded (DE-98)

### Typing the SMS code verifies the phone (spec: rider-confirms-contact-details#E2.1)
- **Given** Anna confirmed her email
- **When** she gives +48 600 100 200 and types the SMS code at 10:05
- **Then** her phone is verified and her next onboarding step is providing personal data (Phone Verified)

### A confirmation can wait (spec: rider-confirms-contact-details#E3.1)
- **Given** Anna confirmed her email on 2026-11-01
- **When** she types the SMS code on 2026-11-06
- **Then** her phone is verified (Phone Verified)

### A rider with a verified phone provides personal data (spec: rider-provides-personal-data#E1.1)
- **Given** Anna verified her phone at 10:05
- **When** she provides "Anna Nowak", born 1995-04-12, residing in Poland, at 10:08
- **Then** her personal data is recorded and her next onboarding step is adding a payment method (Personal Data Provided)

### Personal data needs a verified phone (spec: rider-provides-personal-data#E1.2)
- **Given** Anna confirmed her email but hasn't verified her phone
- **When** she provides her personal data
- **Then** the step is rejected and her next onboarding step is still verifying her phone

### A rider must be at least 16 years old (spec: rider-provides-personal-data#E2.1, rider-provides-personal-data#E2.2)
- **Given** Anna confirmed her phone on 2026-11-08
- **When** on 2026-11-08 Anna provides her personal data with date of birth 2010-11-09
- **Then** the step is rejected, because Anna is 15 (DE-2)
- *Boundary (rider-provides-personal-data#E2.2):* date of birth 2010-11-08 is accepted (16 today). Someone born on 29 February turns 16 on 1 March in a non-leap year (DE-98).

### Any residency is accepted (spec: rider-provides-personal-data#E3.1)
- **Given** Dieter verified his phone
- **When** on 2026-11-08 he provides "Dieter Weber", born 1988-02-03, residing in Germany
- **Then** his personal data is recorded (Personal Data Provided)

### A verified card completes the payment step (spec: rider-adds-payment-method#E1.1, rider-adds-payment-method#E2.2)
- **Given** Anna provided her personal data
- **When** she adds tok_visa_4242 and Payments verifies it
- **Then** her next onboarding step is signing the user agreement

### No card before personal data (spec: rider-adds-payment-method#E1.2)
- **Given** Ben verified his phone but hasn't provided his personal data
- **When** he adds tok_visa_1111
- **Then** the step is rejected, Payments isn't asked and his next onboarding step is still providing personal data

### A declined card doesn't complete the payment step (spec: rider-adds-payment-method#E2.1)
- **Given** Anna provided her personal data
- **When** Anna adds card tok_declined_4000 and Payments declines its pre-authorization
- **Then** the payment method isn't verified and Anna still has to add a payment method (DE-3)

### An activated rider can add a new card (spec: rider-adds-payment-method#E3.1, rider-adds-payment-method#E3.2)
- **Given** Anna is activated
- **When** she adds a card, verified or declined
- **Then** her account stays activated (DE-5)

### Signing the agreement activates the account (spec: rider-signs-user-agreement#E1.1)
- **Given** Anna completed every step up to a verified payment method
- **When** Anna signs the user agreement
- **Then** the agreement is recorded and Anna's account is activated (Account Activated)

### No signature before a verified card (spec: rider-signs-user-agreement#E2.1)
- **Given** Ben provided his personal data and his only card tok_declined_0002 was declined
- **When** he signs the user agreement
- **Then** the step is rejected, his account stays incomplete and his next onboarding step is still adding a payment method

### Signing can wait (spec: rider-signs-user-agreement#E3.1)
- **Given** Carla's card tok_visa_7777 was verified on 2026-11-01
- **When** she signs on 2026-11-15
- **Then** her account is activated on 2026-11-15 (Account Activated)

### An invalid card blocks the account (spec: finance-blocks-rider-with-invalid-card#E1.1, finance-blocks-rider-with-invalid-card#E2.1)
- **Given** Anna's account is activated and not blocked
- **When** Payments reports Anna's payment method invalidated
- **Then** Anna's account is blocked with reason *invalid card* (Account Blocked) and Anna is notified

### A block during onboarding (spec: finance-blocks-rider-with-invalid-card#E1.3)
- **Given** Carla's card was verified and she hasn't signed
- **When** Payments reports her payment method invalidated
- **Then** she is blocked for *invalid card*, stays incomplete, and once she signs she is activated but not eligible (DE-93)

### A block doesn't end a running ride (spec: finance-blocks-rider-with-invalid-card#E1.2)
- **Given** Anna is riding B-17
- **When** she is blocked for *invalid card*
- **Then** her ride continues (a block prevents new rentals)

### A second reason, a second notification (spec: finance-blocks-rider-with-invalid-card#E2.2)
- **Given** Ben is blocked for *outstanding debt*
- **When** Payments reports his payment method invalidated
- **Then** he is blocked for *invalid card* as well and gets a second block notification

### The same reason twice changes nothing (spec: finance-blocks-rider-with-invalid-card#E2.3)
- **Given** Anna is blocked for *invalid card*
- **When** the block is placed again
- **Then** nothing changes and no notification is sent (DE-94)

### Outstanding debt blocks the account (spec: finance-retries-failed-charge#E2.2)
- **Given** Ben is activated and not blocked
- **When** Settlement blocks Ben for *outstanding debt*
- **Then** Ben is blocked for *outstanding debt* (Account Blocked) and notified (DE-7)

### A debt block already in place sends no second notification (spec: finance-retries-failed-charge#E3.1)
- **Given** Ben is blocked for *outstanding debt*
- **When** Settlement blocks him for *outstanding debt* again
- **Then** nothing changes and no notification is sent (DE-94)

### A cleared debt lifts the debt block (spec: rider-pays-off-outstanding-debt#E1.1)
- **Given** Anna is blocked for *outstanding debt* only
- **When** Settlement unblocks her for *outstanding debt*
- **Then** the block is lifted and Anna is eligible to rent (Account Unblocked naming *outstanding debt*, eligible: yes) (DE-94)

### A debt unblock doesn't lift a card block
- **Given** Anna is blocked for *invalid card* and for *outstanding debt*
- **When** Settlement unblocks Anna for *outstanding debt*
- **Then** Anna is still blocked for *invalid card* and isn't eligible to rent (DE-6)

### A newly verified card lifts the card block (spec: finance-blocks-rider-with-invalid-card#E4.1)
- **Given** Anna is blocked for *invalid card* only
- **When** Anna adds card tok_visa_4242 and Payments verifies it
- **Then** the *invalid card* block is lifted and Anna is eligible to rent (Account Unblocked)

### A card unblock doesn't lift a debt block (spec: finance-blocks-rider-with-invalid-card#E4.2)
- **Given** Ben is blocked for *invalid card* and *outstanding debt*
- **When** he adds tok_visa_2222 and Payments verifies it
- **Then** *invalid card* is lifted (Account Unblocked, eligible: no) and he stays blocked for *outstanding debt*

### A declined card keeps the block (spec: finance-blocks-rider-with-invalid-card#E4.3)
- **Given** Anna is blocked for *invalid card*
- **When** she adds tok_declined_0002 and Payments declines it
- **Then** she stays blocked

### Only an activated, unblocked rider is eligible to rent
- **Given** Ben registered but stopped before signing the agreement
- **When** Rentals asks whether Ben may rent
- **Then** the answer is *no, onboarding incomplete*

## Assumptions
- 🎭 DE-1: email and phone confirmations are trusted facts from the messaging provider; no codes are checked here.
- 🎭 DE-4: there is no separate activation step.
- 🎭 DE-7: one block notification per block placed.
- 🎭 DE-94: placing a block that is already in place changes nothing and sends no second notification; Account Unblocked is recorded each time one block reason is lifted.
- 🎭 DE-93: a card invalidated during onboarding still places the block.

## Verification Metrics
- Share of registrations that reach activation, and the step where riders drop out.
- Number of rentals rejected because of eligibility (should be low: the app checks it up front).

## Open Questions
- Can a rider close their account? (Not in the vision.)
- Should a block for invalid card be placed when the rider has no running rental only? (We block at once.)
