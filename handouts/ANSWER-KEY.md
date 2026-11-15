# Exercise 17: answer key

For the debrief only. The reference intents are in `docs/intents/`. They are exactly the four intents
Exercise 7 starts from, renumbered 1–4. Related intents that don't exist in this branch are marked as candidates.
They came from an autonomous run of the vision with role-played expert answers (DE-n, restored in
`docs/bounded-contexts/velocity/domain-expert-answers.md`), not from VELO-207. So treat them as **one good split**, not
the only one.

## The reference split

| # | Intent | Actor | Activity | Why it is its own intent |
|---|---|---|---|---|
| 1 | `rider-reserves-bike` | Rider | Get a bike | The core need: nobody grabs *my* bike while I unlock it. |
| 2 | `rider-unlocks-reserved-bike` | Rider | Get a bike | A different moment with its own outcome: the ride starts, or the unlock fails and the reservation is released. |
| 3 | `fleet-operations-releases-expired-reservations` | Fleet Operations | Get a bike | Nobody "wants" an expiry as a rider. The business owner of "no bike stays blocked" is Fleet Operations, not the system. |
| 4 | `technician-reserves-broken-bike` | Technician | Keep the fleet healthy | Same mechanism, different actor, purpose, bikes (broken only) and duration. It sits on a different activity of the story map. |

Acceptable variations, if the pair can defend them:
- **Failed unlock as its own intent** (5 intents). It's defensible, since its outcome differs. The reference keeps it in
  the unlock intent because the rider's goal ("start riding") is the same.
- **Technician expiry inside intent 3**: the reference covers it in both 3 and 4. Watch for duplication.
- **Cancelling as a candidate intent** (`rider-cancels-reservation`, not written) is fine. Writing it as a full intent is
  scope creep: the ticket only says "may", and Support complains about something else.

Weak splits to challenge:
- One intent "Manage reservations": an epic, with several actors and goals.
- "System releases expired reservations": `intents.py check` rejects the actor `system`.
- Technician folded into the rider's intent with an "and": two actors, two goals.
- One intent per technical piece ("Redis lock", "Kafka event"): a solution, not a need.

## Where each ticket line should end up

| Ticket line | Where it belongs | Typical mistake |
|---|---|---|
| "Manage reservations" (title) | Split into intents 1–4 | Kept as one intent, or the word "manage" in a title |
| `POST /api/reservations`, Redis TTL lock, `reservation_expired` Kafka topic | *Notes for the spec* only, or dropped | Leaks into *Intent* or *Business rules*; "TTL" ends up as a rule |
| "The system releases expired reservations automatically" | Intent 3, actor Fleet Operations | Actor "System" |
| "Only when they're close to the bike" | Rule in intent 1, no distance | Agent invents "50 m" (the expert said that in DE-12, which isn't in this branch) |
| Marketing: 15 minutes / Ops: "the seconds it takes to unlock" | **Open question** in intent 1 (and 3) | Agent picks 15 minutes because it's the only number; or participants "decide" in the intent |
| Support lead: complaints are about the bike next to the rider | Supports Ops; goes into *Why* | Ignored |
| "If the lock doesn't open, the rider shouldn't be stuck" | Intent 2, outcome "Unlock Failed" | Missed entirely |
| "A reservation nobody uses should not keep the bike off the street" | Intent 3's *so that* | Written as "reservations expire after N minutes" |
| Technicians "need longer, they're driving across town" | Intent 4; duration an open question | A number invented ("30 minutes" is DE-11, not in this branch) |
| Countdown timer, Figma frame | *Notes for the spec* or dropped: a UI detail of intent 1 | Its own intent "Rider sees countdown" |
| "Riders may cancel" | Out of scope, or candidate | Silently added as a rule |
| Postgres 17 upgrade | Dropped: not a business need (the skill should say so) | An intent "Upgrade reservations table" |
| "Block reservations for riders who owe money?" | Open question in intent 1 (Payments vs Product) | Settled by the agent; the reference answer is DE-14 (account checked at reserve) |
| Acceptance criteria checkboxes | Dropped: too vague, and GWT belongs to the spec | Copied into the intent |

## What to listen for in the debrief

- **Invented numbers** are the main failure. Every number in an intent turns into a fact in the spec (Exercise 7), so an
  invented one ends up in the code. The reference has 2 min, 30 min and 50 m only because an expert said so (DE-10,
  DE-11, DE-12).
- **The actor of an automatic process.** "Who would complain if this didn't happen?" gives Fleet Operations.
- **The same mechanism isn't the same intent.** The technician's reservation reuses the rider's steps, but the need, the
  bikes and the story-map activity differ. Exercise 7 marks it "hard" for the same reason.
- **The canvases already expect this split** (`docs/bounded-contexts/velocity/rentals/canvas.md`,
  `fleetmaintenance/canvas.md`). The intents came first in the real run. Ask whether a different split would have
  changed those contexts.
