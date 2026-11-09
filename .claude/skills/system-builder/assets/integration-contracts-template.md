# <System>: published languages and integration contracts

Written on <YYYY-MM-DD>, before the implementation plans, so that every plan uses the same contracts. This is the
technical side of the context map (`<map.cml>`): which resources each Open Host Service offers, and which integration
events each context publishes. Planners and builders treat it as binding. They report gaps; they don't change it.

## Integration style

- **Commands and queries between contexts:** <REST>. The caller has a secondary port in its own language and an
  adapter in an `@AntiCorruptionLayer` package (`infra/adapters/secondary/<ctx>ctxacl`) with its own request and
  response records. It never imports the upstream's classes. Base URL: <service-URL helper>, the app itself by
  default.
- **Integration events:** <in-process Spring bus>. The publisher maps its domain event to a record in its `api/events`
  (primitives and strings only). The consumer listens with `@Async @EventListener` in an ACL primary adapter
  (`infra/adapters/primary/<ctx>ctxacl`) and translates the event into its own command. Tests wait with Awaitility.
- **Rejections:** `409` broken business rule, `404` unknown resource, `400` malformed input. Body
  `{"error": "<RuleName>", "message": "..."}`.
- **Formats:** money as a decimal string (`"5.00"`) in <currency>; times as ISO-8601 instants; <other shapes>.
- **Clock and time-driven work:** the shared `Clock` bean. Time-driven work is a use case taking "now", triggered by a
  `@Scheduled` job that is disabled in tests.

## Event-only relationships

Relationships where the downstream only listens (no command or query calls). These don't order the build:
`waves.py --event-only "<Up->Down>,..."`.

## <Context> (OHS/PL | –)

| Message | Resource | Body / result |
|---|---|---|
| <Command> (<caller>) | `POST /<context-kebab>/<resource>/{id}/<action>` | `{...}` → `200 {...}` |
| <Query> (<caller>) | `GET /<context-kebab>/<resource>/{id}` | `{...}` |

Publishes:

| Event | Data | Listener(s) |
|---|---|---|
| `<EventName>` | `field, field, occurredAt` | <Context> |

Consumes: `<producer>.api.events.<EventName>`.

## Addendum <YYYY-MM-DD>: gaps reported by the planners/builders

| # | Reported by | Gap | Resolution |
|---|---|---|---|
