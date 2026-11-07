# `analysis.json` schema

The script `scripts/residuality_matrix.py` reads this file. Unknown keys are ignored, so you can add notes freely.

```json
{
  "system": "velocity",
  "scope": "reference context map",
  "date": "2026-11-01",
  "naive_architecture": {
    "source": "docs/domains/velocity/velocity-context-map.md + src/main/cml/VeloCity-Context-Map.cml",
    "components": [
      {"id": "ACC", "name": "Accounts", "kind": "context"},
      {"id": "PROC", "name": "Payment Processor", "kind": "external"}
    ],
    "relationships": [
      {"from": "PROC", "to": "PAY", "pattern": "U -> D,ACL", "notes": "PaymentProcessing"},
      {"from": "PAY", "to": "ACC", "pattern": "U,OHS,PL -> D,CF"}
    ]
  },
  "attractors": [
    {"id": "A1", "name": "Ride-to-money flow stops", "description": "Rides happen but cannot be charged."}
  ],
  "stressors": [
    {
      "id": "S1",
      "text": "The payment processor is acquired and the new owner bans pre-authorisations under 10 EUR.",
      "category": "partners",
      "source": "claude",
      "attractor": "A1",
      "impacts": ["PAY", "ACC", "SET"],
      "impact_notes": "Pre-authorisation is part of Payments' published language; ACC and SET conform to it.",
      "naive_survives": false,
      "naive_reason": "Change leaks through the PL into two Conformists.",
      "residue": "Payments publishes 'payment method verified' without exposing the pre-authorisation mechanism.",
      "residue_components": ["PAY"],
      "withdrawn": false
    }
  ],
  "residual_architecture": {
    "summary": "...",
    "components": [ ... same shape as naive ... ],
    "relationships": [ ... ],
    "changes": [
      {"id": "C1", "change": "...", "stressors": ["S1", "S7"], "attractors": ["A1"], "cost": "..."}
    ]
  },
  "test_stressors": [
    {
      "id": "T1",
      "text": "...",
      "category": "regulation",
      "generated_by": "independent subagent",
      "naive_survives": false,
      "naive_reason": "...",
      "residual_survives": true,
      "residual_reason": "..."
    }
  ],
  "changelog": [
    {"date": "2026-11-01", "entry": "Initial analysis."}
  ]
}
```

Rules:
- `id`s are unique per list. Stressors use `S<n>`, test stressors `T<n>`, attractors `A<n>`, changes `C<n>`.
- `impacts` and `residue_components` refer to **naive** component IDs. A residue may introduce a new component. Add it to `residual_architecture.components`, and list it in `residue_components` with a `+` prefix (`"+GEO"`) so the script doesn't flag it as unknown.
- `withdrawn: true` keeps a stressor in the history but leaves it out of the matrix and the totals.
- `naive_survives` / `residual_survives` are booleans. Leave a test stressor out of the index until both verdicts are set. The script reports any stressor whose verdicts are incomplete.
