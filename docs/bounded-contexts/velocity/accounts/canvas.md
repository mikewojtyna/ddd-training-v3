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
| Block Notification | Command | Rider notifications (messaging provider) | yes |
| Account Activated, Account Blocked, Account Unblocked | Event | none outside Accounts yet | n/a |

## Ubiquitous Language
- **Rider**: a person with an account at VeloCity.
- **Account**: a rider's identity; it is *incomplete*, *activated*, or activated and *blocked*.
- **Onboarding**: the fixed sequence of steps from registration to activation; a step is accepted only when every earlier step is complete.
- **Onboarding step**: registered → email confirmed → phone verified → personal data provided → payment method added → payment method verified → user agreement signed → activated.
- **Payment method**: the rider's card, known to us only by the reference Payments gives us.
- **Verified**: Payments pre-authorized the card successfully.
- **Activated**: every onboarding step is complete; set automatically when the agreement is signed.
- **Block**: a reason why an activated rider can't start rentals: *invalid card* or *outstanding debt*. An account can hold both at once.
- **Rental eligibility**: activated and holding no block.

## Business Decisions

### Onboarding steps are completed in order
- **Given** Anna registered with anna@example.com and hasn't confirmed her email
- **When** Anna verifies her phone +48 600 100 200
- **Then** the step is rejected and Anna's account still waits for the email confirmation

### Signing the agreement activates the account
- **Given** Anna completed every step up to a verified payment method
- **When** Anna signs the user agreement
- **Then** the agreement is recorded and Anna's account is activated (Account Activated)

### A declined card doesn't complete the payment step
- **Given** Anna provided her personal data
- **When** Anna adds card tok_declined_4000 and Payments declines its pre-authorization
- **Then** the payment method isn't verified and Anna still has to add a payment method (DE-3)

### A rider must be at least 16 years old
- **Given** Anna confirmed her phone on 2026-11-08
- **When** on 2026-11-08 Anna provides her personal data with date of birth 2010-11-09
- **Then** the step is rejected, because Anna is 15 (DE-2)
- *Boundary:* date of birth 2010-11-08 is accepted (16 today).

### An invalid card blocks the account
- **Given** Anna's account is activated and not blocked
- **When** Payments reports Anna's payment method invalidated
- **Then** Anna's account is blocked with reason *invalid card* (Account Blocked) and Anna is notified

### A debt unblock doesn't lift a card block
- **Given** Anna is blocked for *invalid card* and for *outstanding debt*
- **When** Settlement unblocks Anna for *outstanding debt*
- **Then** Anna is still blocked for *invalid card* and isn't eligible to rent (DE-6)

### A newly verified card lifts the card block
- **Given** Anna is blocked for *invalid card* only
- **When** Anna adds card tok_visa_4242 and Payments verifies it
- **Then** the *invalid card* block is lifted and Anna is eligible to rent (Account Unblocked)

### Only an activated, unblocked rider is eligible to rent
- **Given** Ben registered but stopped before signing the agreement
- **When** Rentals asks whether Ben may rent
- **Then** the answer is *no, onboarding incomplete*

## Assumptions
- 🎭 DE-1: email and phone confirmations are trusted facts from the messaging provider; no codes are checked here.
- 🎭 DE-4: there is no separate activation step.
- 🎭 DE-7: one block notification per block placed.
- Placing a block that is already in place changes nothing and sends no second notification.

## Verification Metrics
- Share of registrations that reach activation, and the step where riders drop out.
- Number of rentals rejected because of eligibility (should be low: the app checks it up front).

## Open Questions
- Can a rider close their account? (Not in the vision.)
- Should a block for invalid card be placed when the rider has no running rental only? (We block at once.)
