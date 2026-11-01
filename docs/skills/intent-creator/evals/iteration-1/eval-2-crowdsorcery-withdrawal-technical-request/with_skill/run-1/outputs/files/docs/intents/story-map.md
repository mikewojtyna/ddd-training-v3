# Story map

The backbone of each system: who the actors are and the ordered activities of their journey. Intents (`docs/intents/*.md`) hang under these activities through their `activity` and `task` fields. The order of activities here **is** the value stream, and `README.md` uses it to lay intents out. Keep activity names stable, because intents reference them word for word.

Format rules, which `intents.py` parses:
- `## <system>` for each system, matching the `system` field of the intents.
- `### <n>. <Activity>` for each activity, numbered in journey order.
- Under an activity, one `- <Task>` line per known user task. Tasks with no intent yet are allowed and help show the gaps.
- An optional `Actors:` line under the system heading.

## crowdsorcery

Actors: Investor, Borrower, Project Verifier

<!-- So far only the Investor's journey is mapped, taken from crowdsorcery/CrowdSorcery.md.
     The Borrower's journey (propose a project, raise funds, receive funds, repay the loan) is not mapped yet. -->

### 1. Join CrowdSorcery
- Open an account
- Verify a bank account

### 2. Fund the account
- Deposit money

### 3. Choose how to invest
- Run an investment simulation

### 4. Invest
- Invest using an automated strategy
- Invest manually into an asset
- Invest manually into a project

### 5. Earn returns
- Receive returns
- Recover money after a default

### 6. Take money out
- Withdraw money to a bank account
