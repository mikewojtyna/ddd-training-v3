I didn't write an intent for this, and I didn't create or change any files.

An intent records a business need: someone in the business (a Rider, an Investor, Fleet Operations) wants something, and the intent says why. Upgrading Spring Boot from 4.1 (currently `4.1.1` in `pom.xml`) to 4.2, and jMolecules from `2.0.1` to the latest version, has no business actor behind it. No domain expert would recognise "now X can Y" in it. If I wrote it as an intent, it would sit on the story map as a fake business step and break the history and value-stream readings that `docs/intents/` is meant to give.

**Where it belongs instead**
- Normally I'd suggest putting the upgrade in the plan of an existing intent. But `docs/intents/` doesn't exist yet, so there's no intent or plan to attach it to.
- So treat it as a plain maintenance task: a branch or PR, a ticket, or a short note in your backlog. Before merging, run `./mvnw compile` and `./mvnw test` on JDK 27, keep Lombok out, and check that the Context Mapper `main` classes still run.
- I left `pom.xml` alone as you asked, and I didn't look up what the latest jMolecules version is.

**Questions for you**
1. Is there a business reason behind the upgrade? For example, a security fix, a vulnerability that compliance or customers flagged, or a 4.1 end-of-support date. If so, tell me what it is. I can then write an intent around that outcome and put the version bump in its `## Notes for the spec`.
2. Should the upgrade go into the plan of an upcoming intent (for example, the first VeloCity or CrowdSorcery intent you add), or be done separately right away?
3. If you do want `docs/intents/` set up now (with `story-map.md` and the generated `README.md`), should I backfill intents for what VeloCity Rentals and CrowdSorcery already do?
