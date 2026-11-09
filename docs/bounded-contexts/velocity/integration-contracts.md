# VeloCity: published languages and integration contracts

Written on 2026-11-08 during the autonomous run, before the implementation plans, so that every plan uses the same
contracts. It is the technical side of the context map (`src/main/cml/VeloCity-Context-Map.cml`): which REST
resources each Open Host Service offers and which integration events each context publishes.

## Integration style (the trainer's requirements)

- **Commands and queries between contexts go over REST.** The caller has a secondary port in its own language and an
  adapter in an `@AntiCorruptionLayer` package (`infra/adapters/secondary/<ctx>ctxacl`) that calls the upstream's REST
  resource with Spring's `RestClient`. The adapter keeps its own request/response records; it never imports the
  upstream's classes. The base URL comes from `VelocityServiceUrls` (`velocity/common/infra`), which points to this
  same application (`http://localhost:<server port>`) unless `velocity.services.base-url` is set.
- **Integration events go over the in-process Spring event bus.** The publisher maps its domain event to a record in
  its `api/events` package (primitives and strings only) and publishes it through `ApplicationEventPublisher`. The
  consumer listens in an `@AntiCorruptionLayer` primary adapter (`infra/adapters/primary/<ctx>ctxacl`) with
  `@Async @EventListener`, translates the event into its own command and calls its use case. Listeners are
  asynchronous on purpose: tests must wait with Awaitility, so they would keep working with a real broker.
- **Rejections over REST:** `409 Conflict` for a broken business rule, `404 Not Found` for an unknown resource,
  `400 Bad Request` for malformed input. Body: `{"error": "<RuleName>", "message": "..."}`. ACL adapters translate
  them into the caller's own exceptions or results.
- **Money** travels as a decimal string in PLN (`"5.00"`), **times** as ISO-8601 instants (`"2026-11-08T10:00:00Z"`),
  **positions** as `latitude`/`longitude` decimals.
- **Clock:** every context uses the shared `java.time.Clock` bean (`velocityClock`). Time-driven work (reservation
  expiry, overdue sessions, charge retries, collection attempts) is a use case taking "now", triggered by a
  `@Scheduled` job in `infra/adapters/primary` that is disabled when `velocity.scheduling.enabled=false` (tests).

## Accounts (OHS/PL)

| Message | REST | Body / result |
|---|---|---|
| Register (rider app) | `POST /accounts/riders` | `{email}` → `201 {riderId}` |
| Confirm Email | `POST /accounts/riders/{riderId}/email-confirmation` | – |
| Verify Phone | `POST /accounts/riders/{riderId}/phone-verification` | `{phoneNumber}` |
| Provide Personal Data | `POST /accounts/riders/{riderId}/personal-data` | `{fullName, dateOfBirth, countryOfResidency}` |
| Add Payment Method | `POST /accounts/riders/{riderId}/payment-methods` | `{cardToken}` → `200 {verified, paymentMethodId?}` |
| Sign User Agreement | `POST /accounts/riders/{riderId}/agreement-signature` | – |
| Account Status (query) | `GET /accounts/riders/{riderId}` | `{riderId, onboardingStep, activated, blocks: [..]}` |
| **Rental Eligibility** (query, Rentals) | `GET /accounts/riders/{riderId}/rental-eligibility` | `{riderId, eligible, reason}`; reason one of `ELIGIBLE`, `ONBOARDING_INCOMPLETE`, `BLOCKED`, `UNKNOWN_RIDER` (unknown rider answers `200` with `eligible=false`) |
| **Block Account** (Settlement) | `POST /accounts/riders/{riderId}/blocks` | `{reason: "OUTSTANDING_DEBT"}` |
| **Unblock Account** (Settlement) | `DELETE /accounts/riders/{riderId}/blocks/OUTSTANDING_DEBT` | – |

Consumes: `payments.api.events.PaymentMethodInvalidated`.

## Payments (OHS/PL)

| Message | REST | Body / result |
|---|---|---|
| **Verify Payment Method** (Accounts) | `POST /payments/payment-methods` | `{riderId, cardToken}` → `200 {paymentMethodId, verified, declineReason?}` |
| **Charge** (Settlement) | `POST /payments/charges` | `{riderId, amount, reference}` → `200 {chargeId, status: "SUCCEEDED"\|"FAILED", failureReason?}` |
| Card Invalidated (processor notification) | `POST /payments/processor-notifications/card-invalidations` | `{cardToken, reason}` |

Publishes `PaymentMethodInvalidated {riderId, paymentMethodId, reason, occurredAt}`.

External processor: port `PaymentProcessor` in Payments' language, adapter in `infra/adapters/secondary/paymentprocessoracl`
backed by a simulated processor: pre-authorization is declined for tokens starting with `tok_declined`, charges fail
for tokens starting with `tok_nofunds`, everything else succeeds.

## Rentals (OHS/PL)

| Message | REST | Body / result |
|---|---|---|
| Add Bike to Fleet (fleet manager) | `POST /rentals/bikes` | `{bikeId, stationId}` |
| Bike Availability (query) | `GET /rentals/bikes/{bikeId}` | `{bikeId, availability: "AVAILABLE"\|"WITHDRAWN", hold: "FREE"\|"RESERVED"\|"UNLOCKED", holderType?, holderId?, latitude, longitude, stationId?}` |
| Reserve Bike (rider app) | `POST /rentals/bikes/{bikeId}/reservations` | `{holderType: "RIDER", holderId, latitude, longitude}` |
| **Reserve Bike for service** (Fleet Maintenance) | `POST /rentals/bikes/{bikeId}/reservations` | `{holderType: "TECHNICIAN", holderId}` |
| Unlock Bike (rider app, Fleet Maintenance) | `POST /rentals/bikes/{bikeId}/unlock` | `{holderType, holderId}`; a rider's eligibility is re-checked in Accounts (DE-80) |
| Return Bike at Station | `POST /rentals/bikes/{bikeId}/station-return` | `{riderId, stationId, faultDescription?}` |
| End Ride Here (free-floating) | `POST /rentals/bikes/{bikeId}/free-floating-return` | `{riderId, latitude, longitude}` |
| Signal Fault (bike telemetry) | `POST /rentals/bikes/{bikeId}/fault-signals` | `{faultCode}` |
| **Withdraw Bike** (Fleet Maintenance) | `POST /rentals/bikes/{bikeId}/withdrawal` | `{}` (reason recorded as `REQUESTED`) |
| **Make Bike Available** (Fleet Maintenance) | `POST /rentals/bikes/{bikeId}/availability` | `{stationId}` or `{latitude, longitude}` |
| Rider's Active Rental (query) | `GET /rentals/riders/{riderId}/active-rental` | `{riderId, bikeId, kind: "RESERVATION"\|"SESSION", sessionId?, since, expiresAt?}` or `404` |

Unlock and lock talk to the bikes through the port `BikeLocks` (adapter `infra/adapters/secondary/bikelocksacl`,
simulated: a lock fails for bikes registered as faulty through `SimulatedBikeLocks`). Stations are reference data in
Rentals (`Central` 52.2300/21.0110, `Old Town` 52.2490/21.0122, `Riverside` 52.2400/21.0300).

Publishes (only what others listen to):

| Event | Data | Listener |
|---|---|---|
| `RentalSessionEnded` | `sessionId, bikeId, riderId, startedAt, endedAt, endReason ("STATION_RETURN"\|"FREE_FLOATING_RETURN"\|"LOCK_FAILURE"\|"MAX_RENTAL_TIME_EXCEEDED"), stationId?, latitude?, longitude?` | Settlement |
| `BikeWithdrawn` | `bikeId, reason ("FAULT_REPORTED_AT_RETURN"\|"LOCK_FAILED"\|"FAULT_SIGNALLED"\|"NOT_RETURNED"\|"REQUESTED"), withdrawnAt` | Fleet Maintenance |
| `BikeUnlocked` | `bikeId, holderType, holderId, unlockedAt` | Fleet Maintenance (technicians only) |
| `ReservationExpired` | `bikeId, holderType, holderId, expiredAt` | Fleet Maintenance (technicians only) |
| `UnlockFailed` | `bikeId, holderType, holderId, failedAt` | Fleet Maintenance (technicians only) |

## Pricing (OHS/PL)

| Message | REST | Body / result |
|---|---|---|
| **Price for Session** (Settlement) | `GET /pricing/session-price?startedAt=&endedAt=&ending=&latitude=&longitude=` | `ending` one of `STATION`, `FREE_FLOATING`, `LOCK_FAILURE`, `MAX_RENTAL_TIME`; → `{tariffId, hourlyFees, returnFee, returnFeeKind: "NONE"\|"DROP_ZONE"\|"UNAUTHORIZED_ZONE", penalty, total}` |
| Publish Tariff (pricing manager) | `POST /pricing/tariffs` | `{tariffId, validFrom, freeMinutes, firstHourFee, secondHourFee, thirdHourFee, furtherHourFee, dropZoneFee, unauthorizedZoneFee, penalty}` |
| Tariffs (query) | `GET /pricing/tariffs` | list |

The launch tariff (DE-30) is published at start-up, valid from `2026-01-01T00:00:00Z`. Zone map: port `ZoneMap`,
adapter `infra/adapters/secondary/cityacl` backed by a configured City zone map with the drop zone *Old Town Square*
(centre 52.2297/21.0122, radius 150 m) and *Riverside Park* (centre 52.2400/21.0300, radius 200 m).

## Settlement

| Message | REST | Body / result |
|---|---|---|
| **Collect Outstanding Debt** (Debt Recovery) | `POST /settlement/riders/{riderId}/debt-collections` | → `200 {collected, collectedAmount, outstandingDebt}` |
| Pay Outstanding Debt (rider app) | `POST /settlement/riders/{riderId}/debt-payment` | → same result |
| Rider Balance (query) | `GET /settlement/riders/{riderId}/balance` | `{riderId, outstandingDebt, unpaidRides: [..]}` |
| Ride Settlement (query) | `GET /settlement/rides/{sessionId}` | `{sessionId, riderId, fee, status: "PENDING"\|"SETTLED"\|"UNPAID", failedAttempts}` |

Consumes `rentals.api.events.RentalSessionEnded`. Publishes:

| Event | Data | Listener |
|---|---|---|
| `RentalFeeUnpaid` | `riderId, sessionId, amount, outstandingDebt, occurredAt` | Debt Recovery |
| `OutstandingDebtCleared` | `riderId, occurredAt` | Debt Recovery |

## Debt Recovery

| Message | REST | Body / result |
|---|---|---|
| Pause Recovery | `POST /debt-recovery/riders/{riderId}/recovery/pause` | `{reason: "DISPUTE"\|"PAYMENT_PLAN"\|"REGULATORY_HOLD"}` |
| Resume Recovery | `POST /debt-recovery/riders/{riderId}/recovery/resume` | – |
| Recovery Process (query) | `GET /debt-recovery/riders/{riderId}/recovery` | `{riderId, status, failedAttempts, nextAttemptDueAt?, pauseReason?}` |

Consumes `settlement.api.events.RentalFeeUnpaid`, `settlement.api.events.OutstandingDebtCleared`.

## Customer Support (OHS/PL)

| Message | REST | Body / result |
|---|---|---|
| Report Incident (rider app) | `POST /customer-support/incidents` | `{riderId, category, description, bikeId?}` → `201 {incidentId, type}` |
| Pick Up Incident | `POST /customer-support/incidents/{incidentId}/assignment` | `{agentId}` |
| Accept / Reject | `POST /customer-support/incidents/{incidentId}/acceptance` / `rejection` | `{agentId}` / `{agentId, reason}` |
| Escalate | `POST /customer-support/incidents/{incidentId}/escalation` | `{agentId, team}` |
| Resolve | `POST /customer-support/incidents/{incidentId}/resolution` | `{agentId, resolution}` |
| Reopen (rider app) | `POST /customer-support/incidents/{incidentId}/reopening` | `{riderId}` |
| **Resolve Fleet Issue** (Fleet Maintenance) | `POST /customer-support/incidents/{incidentId}/fleet-resolution` | `{bikeId}` |
| Incident (query) | `GET /customer-support/incidents/{incidentId}` | `{incidentId, riderId, type, status, assignedAgentId?, team?, resolution?}` |

Publishes `FleetIssueReported {incidentId, bikeId, description, reportedAt}` (listener: Fleet Maintenance).
A `@Scheduled` job resolves rejected incidents whose 14-day reopening window has closed (DE-87).

## Fleet Maintenance

| Message | REST | Body / result |
|---|---|---|
| Report Fault (technician) | `POST /fleet-maintenance/bikes/{bikeId}/fault-reports` | `{technicianId, description}` |
| Reserve Broken Bike | `POST /fleet-maintenance/bikes/{bikeId}/reservation` | `{technicianId}` |
| Unlock Broken Bike | `POST /fleet-maintenance/bikes/{bikeId}/unlock` | `{technicianId}` |
| Return Bike to Circulation | `POST /fleet-maintenance/bikes/{bikeId}/return-to-circulation` | `{technicianId, stationId}` or `{technicianId, latitude, longitude}` |
| Broken Bike (query) | `GET /fleet-maintenance/bikes/{bikeId}` | `{bikeId, status: "BROKEN"\|"UNDER_MAINTENANCE"\|"IN_CIRCULATION", technicianId?, incidentIds}` |
| Broken Bikes (query) | `GET /fleet-maintenance/broken-bikes` | list |

Consumes `rentals.api.events.BikeWithdrawn` (all reasons except `REQUESTED`), `BikeUnlocked`, `ReservationExpired`,
`UnlockFailed` (technicians only) and `customersupport.api.events.FleetIssueReported`.

## Additions from the planning round (CONTRACT GAPs, 2026-11-08)

The planners found these gaps; they are part of the contract and override the tables above where they differ.

**Accounts**
- Account Status: `onboardingStep` is the *next* step to do: `CONFIRM_EMAIL | VERIFY_PHONE | PROVIDE_PERSONAL_DATA | ADD_PAYMENT_METHOD | SIGN_USER_AGREEMENT | NONE`; `blocks` values `INVALID_CARD | OUTSTANDING_DEBT`.
- Block notifications: Accounts port `RiderNotifications`, simulated recording adapter in `infra/adapters/secondary/messagingprovideracl`.
- Signing the agreement twice → `409`.

**Payments**
- `GET /payments/riders/{riderId}/payment-methods` → `{riderId, currentPaymentMethodId?, paymentMethods: [{paymentMethodId, cardToken, status: "VERIFIED"|"DECLINED"|"REPLACED"|"INVALIDATED"}]}`.
- Charge `failureReason`: `DECLINED | NO_VALID_PAYMENT_METHOD`.
- Processor notification `reason`: `EXPIRED | CANCELLED | REPORTED_LOST`; an unknown card token → `200`, nothing happens.
- The simulated processor records the charges it received and lets tests script the next outcome for a card token, in addition to the token prefixes.

**Rentals**
- `POST /rentals/bikes/{bikeId}/reservations` → `201 {bikeId, holderType, holderId, reservedAt, expiresAt}`.
- `POST /rentals/bikes/{bikeId}/unlock` → `200 {bikeId, outcome: "UNLOCKED"|"UNLOCK_FAILED", sessionId?}`.
- Station return and free-floating return → `200 {sessionId, bikeId, endReason, endedAt}`; the end reason tells the rider why the session ended (vision: "told why").
- `GET /rentals/bikes/{bikeId}`: `latitude`/`longitude` are optional (a bike that wasn't returned has no known position).
- `BikeWithdrawn` may carry an optional `faultDescription` (the rider's description or the bike's fault code). It is added as the event's last field when Rentals is implemented.

**Pricing**
- Price answer adds `billingHours`; `hourlyFees` is the sum of the hourly fees.
- `POST /pricing/tariffs` adds `publishedBy`.
- The zone map answers "zone at a position at a moment"; the simulated City map lets tests withdraw a zone for a time window.

**Settlement**
- Ride Settlement query adds `nextAttemptDueAt?`, `lastFailureReason?`.
- Rider Balance: `unpaidRides: [{sessionId, amount}]`.

**Debt Recovery**
- Pause body `{agentId, reason}`; Resume body `{agentId}`.
- Recovery Process query: `{riderId, status: "RUNNING"|"PAUSED"|"SUCCEEDED"|"EXHAUSTED", outstandingDebt, failedAttempts, nextAttemptDueAt?, pauseReason?}`.
- Payment reminders: port `PaymentReminders`, simulated recording adapter in `infra/adapters/secondary/messagingacl`.

**Customer Support**
- `GET /customer-support/incidents?status=OPEN` (open incidents) and `?riderId=` (a rider's incidents).
- Incident query adds `rejectionReason?`, `rejectedAt?`, `reopenableUntil?`. Calendar days in the reopening window use Europe/Warsaw.

**Fleet Maintenance**
- `GET /fleet-maintenance/bikes/{bikeId}` adds `faultSource`, `reportedAt`, `faultDescription?`, `reservedUntil?`.
- `GET /fleet-maintenance/broken-bikes` → list of the same shape as the single-bike query.
- `POST /fleet-maintenance/bikes/{bikeId}/unlock` → `200 {bikeId, outcome: "UNLOCKED"|"UNLOCK_FAILED"}`.
