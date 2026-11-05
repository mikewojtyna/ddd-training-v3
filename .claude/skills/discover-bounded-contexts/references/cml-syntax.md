# Context Mapper (CML) syntax for context maps

The repo's Context Mapper DSL is version 6.10.0, from `pom.xml`. The house style follows `src/main/cml/VeloCity-Context-Map.cml`. Every form below was checked with `scripts/validate_cml.sh`.

## File skeleton

```cml
/* <System> — context map from discover-bounded-contexts, <date>. Report: docs/context-discovery/<system>/<date>-<slug>/discovery.md */
ContextMap <System>ContextMap {
	type = SYSTEM_LANDSCAPE
	state = TO_BE

	contains Accounts
	contains Rentals
	contains PaymentProcessor          // external parties are contexts in CML too

	/* Rentals */

	// Rental Session Ended (start, end, location, reason)
	Rentals [U,PL]->[D,ACL] Settlement : RentalSessionEnded
}

BoundedContext Rentals {
	type = FEATURE
	domainVisionStatement = "Answers: who has which bike, and when? Reserve & unlock, rental sessions, returns. Key language: reservation, unlock, rental session."
	responsibilities = "Reserve and unlock", "Rental sessions", "Returns"
}

BoundedContext PaymentProcessor {
	type = SYSTEM
	domainVisionStatement = "External payment processor (not designed by us)."
}
```

Group the relationships by upstream context, with a `/* Context */` header comment, and put a `//` comment above each one listing the messages that flow across it.

## Relationship forms

| Pattern | CML | Notes |
|---|---|---|
| Upstream/Downstream | `Up [U]->[D] Down : Name` | Also written `Down [D]<-[U] Up`. |
| U/D with OHS + PL, downstream ACL | `Up [U,OHS,PL]->[D,ACL] Down : Name` | The healthy default for internal integration. |
| U/D, downstream Conformist | `Up [U,OHS,PL]->[D,CF] Down : Name` | Justify it in a comment: the upstream language must be well established. |
| Customer/Supplier | `Supplier [U,S]->[D,C] Customer : Name` or `Supplier [S]->[C] Customer` | **CF and OHS are errors** in C/S. PL is allowed. An ACL is allowed but gives a warning. |
| Partnership | `A [P]<->[P] B : Name` or `A Partnership B` | Early stage only. Add a comment saying why. |
| Shared Kernel | `A [SK]<->[SK] B : Name` | Almost always wrong (see the rules). |
| Implementation technology | `Up [U]->[D] Down : Name { implementationTechnology = "Kafka" }` | Optional. |

CML has **no syntax for Separate Ways or Big Ball of Mud**:
- **Separate Ways:** keep the context in `contains` with no relationship. Add a comment such as `// Separate Ways: R&D silo, integrate if the experiment succeeds`.
- **Big Ball of Mud:** never model one on purpose. When you validate an existing system that has one, describe it in the report, not in CML.

## Bounded context attributes

The attributes go in this order. It matters: the grammar rejects some orders, for example `evolution` before `businessModel`.

```cml
BoundedContext Settlement implements SettlementDomain {
	type = FEATURE                       // FEATURE | APPLICATION | SYSTEM | TEAM
	domainVisionStatement = "..."
	responsibilities = "...", "..."
	knowledgeLevel = CONCRETE            // optional: META | CONCRETE
	businessModel = "REVENUE"            // optional, free text
	evolution = CUSTOM_BUILT             // optional: GENESIS | CUSTOM_BUILT | PRODUCT | COMMODITY
}
```

Put the context's **key question** ("Answers: …") and its key language in `domainVisionStatement`. That keeps the H3 result visible in the model.

Optional domain classification, when the user has given one:

```cml
Domain VeloCity {
	Subdomain SettlementDomain { type = SUPPORTING_DOMAIN }   // CORE_DOMAIN | SUPPORTING_DOMAIN | GENERIC_SUBDOMAIN
}
```

## Gotchas
- **Names must not be CML keywords.** Short names such as `C`, `D`, `U`, `S`, `P` and `X`, and many single words, fail with "expecting RULE_ID". Use PascalCase business names (`DebtRecovery`), and relationship names in the same style (`DebtCollection`).
- **Every context in a relationship must be in `contains`, and must be declared as a `BoundedContext`.** Otherwise the validator reports "not part of the Context Map" or "Couldn't resolve reference".
- **Unconfirmed assumptions go in comments.** Copy the reference map's convention, for example `// default assumption, NOT confirmed with the domain expert`, and also add a suffix such as `_Unconfirmed` to the relationship name.
- **To render a PNG**, use the repo's `VelocityContextMap` main class as a pattern. It needs Graphviz `dot`. Validation doesn't need Graphviz.
