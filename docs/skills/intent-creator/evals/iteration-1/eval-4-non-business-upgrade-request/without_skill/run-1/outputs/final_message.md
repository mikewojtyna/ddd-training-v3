I wrote the intent at `docs/intents/upgrade-spring-boot-4.2-and-jmolecules.md`. `pom.xml` is unchanged and nothing is committed.

I checked Maven Central on 2026-11-01 before writing it, and both parts of the request run into a problem:

- **Spring Boot 4.2.0 GA isn't released yet.** The newest 4.2 build is `4.2.0-RC1`, and the repo is on `4.1.1`. A `4.1.2` patch is also out.
- **jMolecules is already on the latest version.** `jmolecules-ddd` and `jmolecules-hexagonal-architecture` are both at `2.0.1`, which is the newest release. The jMolecules BOM `2025.0.2` also pins them at `2.0.1`.

So the intent is marked **Draft, blocked until 4.2.0 GA**. The jMolecules part now mostly means keeping both artifacts on one version in one place: preferably by importing `jmolecules-bom`, or with a single `jmolecules.version` property.

The intent covers:
- **Why:** the training should match the current Spring Boot line.
- **Current state:** a table of the versions above.
- **Outcome and scope:** only dependency bumps and the code changes they force; no other library upgrades, no Java version change, no Lombok, no new features.
- **Constraints:** only GA releases on `master`; it must still build on JDK 27.
- **Acceptance criteria:** tests pass, a single jMolecules version in the dependency tree, the app context starts, `VelocityContextMap` still produces its PNG, and the root `CLAUDE.md` says "Spring Boot 4.2".
- **Risks:** Context Mapper's Xtext/EMF dependencies may clash with Boot 4.2's managed versions, and the JPA/Data REST/H2 versions will change.
- **Plan:** one PR, with one commit per dependency.

There was no existing intent template in the repo, so I chose this structure. Change it if you have a house format.

**Decisions for you** (they're also listed in the intent's "Open questions"; I picked a default for each):
1. Wait for 4.2.0 GA, or try `4.2.0-RC1` on a branch now? I assumed GA only on `master`, with an RC spike allowed on a branch.
2. Since jMolecules is already current, should that part only switch to the BOM, or be dropped?
3. Should we take the `4.1.2` patch now, as a separate change, while waiting for 4.2?
4. Is there a house template or naming convention for intents? I used a kebab-case file name and a Draft status.
