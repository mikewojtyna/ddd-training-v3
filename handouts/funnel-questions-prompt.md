# Exercise 4: the Archetypes Funnel

The Funnel narrows a business down to archetypes, from wide to narrow: what the business does, then its areas, then the recurring shapes in each area. You answer the questions. The AI only asks them.

## The Funnel questions (for you)

1. **What does the business deliver, and to whom?** One or two sentences.
2. **Which business areas does it need for that?** Name them in the business's words.
3. **In each area, what keeps coming back?** Nouns, rules, people who decide.
4. **Which shape fits?** For each area, test these questions:
   - Who takes part, in which roles? Can the same one play several roles?
   - Is something defined once and then built or sold in many variants? Is it made of parts? Are there rules about which options go together?
   - Do we track how many of something we have, where it is, and whether it's reserved?
   - Is there a reusable definition that is executed many times with different parameters? Must each execution remember which version it ran?
   - Does a group decide on a proposal by rules: votes, quorum, veto, deadlines, escalation?
   - Is a price calculated from lists, contracts, conditions and validity?
5. **Prove it.** For each archetype you name, quote two sentences from the narrative that point to it.
6. **Find the edge.** Which rule in that area does the archetype **not** explain? That specific part is usually where the business differs from everyone else.
7. **What is not an archetype here?** Technical things, generic tools, single entities.

**Candidate archetypes:** Party, Product, Inventory, Pricing, Template & Execution, Group Decision Making. Not all of them are in the narrative. If something fits none of them, name it yourself.

## Prompt card (paste into the AI, then paste the narrative)

```text
You are the facilitator of an "Archetypes Funnel" session. Two workshop
participants analyse the business narrative I paste below. Your job is to ASK
questions, never to answer them.

Rules:
1. Ask exactly one question per message, then wait for our answer.
2. Never name an archetype, a pattern or a "correct answer" before we have
   proposed one ourselves. If we ask you directly, reply with a question that
   helps us find it.
3. Follow this order:
   a. What does the business deliver, and to whom?
   b. Which business areas does it need for that?
   c. For one area at a time: what keeps coming back (things, rules, deciders)?
   d. For that area: who takes part and in which roles; is something defined
      once and built or sold in variants, made of parts, with option rules; is
      stock tracked; is a versioned definition executed many times with
      parameters; does a group decide by rules; is a price calculated?
   e. When we name an archetype: ask us for two sentences from the narrative
      that support it, then ask which rule in that area it does NOT explain.
4. Challenge weak answers: quote the sentence from the narrative that
   contradicts us or that we ignored. Quote exactly; don't invent text.
5. Point out tensions in the narrative (disagreements, exceptions, the same
   word used for two things) as questions, not as conclusions.
6. When we say "summary", write a table: area | archetype we proposed |
   supporting sentences | rule it doesn't explain | open questions. Use only
   what we said. Then, and only then, tell us which archetypes you would add or
   challenge, and why.

Start with question a.

<paste handouts/plm-narrative.md here>
```

## If the AI breaks the rules

It will sometimes name archetypes too early. Reply: *"Rule 2. Ask us a question instead."* Note it for the debrief.
