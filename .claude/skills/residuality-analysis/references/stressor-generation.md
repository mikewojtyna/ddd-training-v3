# Generating stressors

The quality of the analysis depends on the variety of the stressors. One person (or one model) left alone produces stressors that cluster around familiar technical failures. Work through the categories below on purpose, and write each stressor as one concrete sentence about *this* domain.

## The test of a good stressor

- **Concrete:** names who or what changes, and how. "Payment processor X raises fees by 300% and requires 3-D Secure on every charge", not "payment issues".
- **External or environmental:** something that happens *to* the business, not a design choice ("we use Kafka" isn't a stressor; "our message broker vendor ends support" is).
- **Not a duplicate:** if two stressors hit the same components for the same reason, merge them. Two stressors that reach the same attractor by *different* routes stay separate, because that's the evidence for the attractor.
- **Not judged by probability:** keep the absurd ones.

## Categories (cover all of them)

| Category | Prompts |
|---|---|
| Business model | price model changes (subscription, free tier, dynamic pricing); new customer segment (corporate, tourists, B2B); new product line; revenue source dries up; merger with or acquisition by a competitor |
| Market and competition | a competitor undercuts by 50%; a big-tech platform enters; a market collapses; demand spikes 10× |
| Regulation and politics | a new law (data, consumer protection, accessibility, tax); a licence is revoked or becomes city-specific; a government ban; elections change policy; a new reporting obligation |
| Partners and suppliers | key supplier bankrupt, acquired or changes terms; external API deprecated; a partner demands exclusivity; a hardware vendor discontinues a device |
| Technology and infrastructure | a cloud region goes down; a dependency is abandoned; mobile OS policy change; connectivity lost in the field; a clock or time-zone bug; data volume grows 100× |
| People and organisation | a key team leaves; a reorganisation splits a team; outsourcing; a strike; support moves to another country and language |
| Operations and physical world | weather, flooding, heatwave; vandalism wave; a fleet recall; an event closes the city centre; seasonal shutdown |
| Data and security | breach; ransomware; fraud ring; GDPR erasure requests at scale; a corrupted data import |
| Expansion | second city; second country (currency, language, law); white-labelling the platform for another operator |
| Absurd | a giant lizard eats the head office; all bikes turn invisible; the city is renamed overnight; time runs backwards for one hour. Absurd stressors are allowed and useful, as long as you can still trace which components they impact. |

## Techniques for the independent test set

The test set must not reuse the reasoning of the training set. In order of preference:

1. **A fresh subagent** that gets only the system description and the category table, never the training stressors or residues. Ask it for N concrete stressors, one sentence each.
2. **Change the point of view.** Generate from the perspective of actors you didn't use before: a regulator, an investor, a competitor's product manager, a disgruntled technician, a journalist, an insurer.
3. **Random-word prompts.** Pick a random noun ("glacier", "auction", "subtitle") and force a stressor out of it.
4. **The user's own stressors** that arrive after the residual architecture was designed.

Remove any test stressor that is essentially a training stressor in other words, and note the removal.

## Domain flavour (VeloCity examples, for calibration only)

Use these to calibrate how concrete a stressor should be. Don't copy them into an analysis wholesale. Generate your own.
- The city replaces its zone map with a live API that changes zones hourly during events.
- The payment processor is acquired and the new owner forbids pre-authorisations under 10 EUR.
- E-bikes join the fleet and must be charged, with battery level affecting availability.
- A university buys a monthly plan for 20,000 students and wants one invoice.
- The national rail operator wants VeloCity rides bookable inside its ticketing app.
