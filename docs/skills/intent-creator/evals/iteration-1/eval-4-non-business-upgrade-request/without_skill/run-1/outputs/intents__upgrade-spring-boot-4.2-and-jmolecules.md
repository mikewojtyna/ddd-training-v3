# Intent: Upgrade Spring Boot to 4.2 and bump jMolecules to the latest version

- **Status:** Draft. Blocked until Spring Boot 4.2.0 GA is released (see "Current state").
- **Author:** Mike Wojtyna
- **Date:** 2026-11-01
- **Type:** Technical / dependency maintenance (no domain or business behaviour change)

## Why

The training material should run on the current Spring Boot minor line, so participants
who clone the repo get the same dependencies they'd pick for a new project, and the
examples don't fall behind the Spring docs they read during the workshop. jMolecules
annotations (`@AggregateRoot`, `@ValueObject`, `@PrimaryPort`, ...) mark DDD and hexagonal
roles across both systems, so they should also be on the newest release.

## Current state (checked against Maven Central on 2026-11-01)

| Dependency | In `pom.xml` | Latest available | Notes |
|---|---|---|---|
| `spring-boot-starter-parent` | `4.1.1` | `4.2.0-RC1` (latest 4.1.x: `4.1.2`) | **4.2.0 GA isn't on Central yet**, only `4.2.0-M1`, `-M2` and `-RC1`. |
| `org.jmolecules:jmolecules-ddd` | `2.0.1` | `2.0.1` | Already the latest. |
| `org.jmolecules:jmolecules-hexagonal-architecture` | `2.0.1` | `2.0.1` | Already the latest. |
| `org.jmolecules:jmolecules-bom` | not used | `2025.0.2` | Manages both artifacts above at `2.0.1`. |

So, as of today:

1. The Spring Boot part of the intent can only target a pre-release (`4.2.0-RC1`) until
   4.2.0 GA ships.
2. The jMolecules part of the intent is already done. There's no newer version to bump to.

## Desired outcome

- The project builds and all tests pass on **Spring Boot 4.2.x GA** (the newest 4.2 patch
  release when the work is done).
- jMolecules artifacts are on the **newest release on Maven Central** when the work is done.
- Both jMolecules artifacts get their version from **one place**, so they can't drift
  apart. Preferred: import `org.jmolecules:jmolecules-bom` in `<dependencyManagement>` and
  drop the explicit `<version>` tags. Acceptable alternative: a single
  `jmolecules.version` property.
- Nothing changes for workshop participants other than the dependency versions:
  - same Java level (`java.version` 25),
  - same commands (`./mvnw compile`, `./mvnw test`),
  - the context map generators (`VelocityContextMap`, `VelocityBoundedContexts`,
    `ContextMapperExample`) still produce their PNGs.

## Scope

In scope:
- `pom.xml`: Spring Boot parent version, jMolecules version management.
- Source and test changes that the upgrade *forces* (deprecations that turned into
  removals, renamed starters or properties, changed test auto-configuration).
- `CLAUDE.md` files that mention the Spring Boot version (the root `CLAUDE.md` says
  "Spring Boot 4.1").

Out of scope:
- Upgrading other dependencies (`context-mapper-dsl`, `context-map-generator`,
  `joda-money`, H2) unless 4.2 requires it. If one does, record it here and keep it a
  separate commit.
- Changing the Java version, adding Lombok (forbidden, see root `CLAUDE.md`), or
  refactoring domain code.
- Adopting new Spring Boot 4.2 features.

## Constraints

- Only GA releases go on `master`. Milestones and RCs may be tried on a branch to find
  breakages early, but must not be merged.
- The build has to keep working on the only local JDK (27) while targeting Java 25.
- jMolecules is used only as annotations (`org.jmolecules.ddd.annotation.*`,
  `org.jmolecules.architecture.hexagonal.*`, about 33 files). A jMolecules major-version
  bump that renames or removes any of these annotations must be handled in the same
  change, so every bounded context keeps its role markers.

## Acceptance criteria

- [ ] `pom.xml` parent is `spring-boot-starter-parent` 4.2.x (GA).
- [ ] jMolecules versions are managed in one place and resolve to the latest release.
- [ ] `./mvnw clean test` passes on JDK 27.
- [ ] `./mvnw dependency:tree` shows a single jMolecules version, and the Spring Framework
      version is the one Boot 4.2 manages, with no stray 4.1-era overrides.
- [ ] The application context starts (`DddTrainingV3Application`, at least through the
      existing Spring Boot test).
- [ ] `VelocityContextMap` still generates its PNG in `target/`. This guards against
      transitive conflicts between Context Mapper (Xtext/EMF) and the newer Spring Boot
      dependency set.
- [ ] The root `CLAUDE.md` says "Spring Boot 4.2".

## Risks and things to check

- **Transitive conflicts with Context Mapper.** `context-mapper-dsl` pulls in a large
  Eclipse/Xtext/Guava dependency tree. Boot 4.2's managed versions (Jackson, Guava-adjacent
  libraries, logging) may override what it expects. Mitigation: run the context map mains,
  not only the tests.
- **JPA / Data REST changes.** The app uses `spring-boot-starter-data-jpa`,
  `-data-rest`, `-webmvc` and H2. Check the Boot 4.2 release notes for Hibernate, Spring
  Data and H2 version jumps.
- **Release timing.** If 4.2.0 GA ships with known regressions, wait for 4.2.1.

## Plan (when unblocked)

1. Read the Spring Boot 4.2 release notes and migration guide, and note anything that
   affects the starters listed above.
2. Bump the parent to the latest 4.2.x GA. Replace the explicit jMolecules versions with
   the jMolecules BOM, or a property, at the latest release.
3. `./mvnw clean test`, then run the three context map mains.
4. Fix forced breakages, then update `CLAUDE.md`.
5. One PR, with one commit per dependency (Spring Boot, jMolecules) so either can be
   reverted on its own.

## Open questions

- Should we wait for 4.2.0 GA, or move to `4.2.0-RC1` now on a branch for early feedback?
  (Default in this intent: wait for GA, but allow an RC spike on a branch.)
- With jMolecules already on the latest `2.0.1`, should that part shrink to "centralise
  version management via the BOM", or be dropped from this intent?
- Should we also take the `4.1.2` patch release now, as a separate change, while waiting
  for 4.2?
