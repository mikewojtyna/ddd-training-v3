/// # Pricing bounded context
///
/// ## Purpose
/// Turns a finished rental session into a fair, explainable total fee: cheap short trips, progressively more
/// expensive long ones, a clear signal for breaking network rules. It is a pure calculator: it moves no money and
/// keeps no rides, only tariffs.
///
/// ## Strategic classification
/// - Domain: core
/// - Business model: revenue
/// - Evolution: custom built
///
/// ## Domain roles
/// - Execution Model: performs one job, calculating and explaining the fee from the facts it is given.
///
/// ## Ubiquitous language
/// - **Tariff**: the prices valid from a point in time: free period, hourly fees per hour, drop zone fee,
///   unauthorized zone return fee, penalty (`Tariff`, all kept in the `TariffSchedule`).
/// - **Valid from**: the moment from which a tariff applies to newly started sessions; it lies in the future when
///   the tariff is published (DE-96).
/// - **Launch tariff**: the tariff VeloCity starts with (DE-30, `LaunchTariff`): 15 min free; PLN 3.00, 5.00, 7.00,
///   then 9.00 per hour; drop zone fee PLN 2.00; unauthorized zone return fee PLN 40.00; penalty PLN 150.00; valid
///   from 2026-01-01.
/// - **Free period**: the first 15 minutes of a session, free of charge (DE-30).
/// - **Billing hour**: each started hour after the free period (DE-31), measured to the second (DE-83). Hours 1, 2, 3
///   have their own fee; every hour from the 4th on costs the same.
/// - **Hourly fee**: the fee of one billing hour.
/// - **Ending**: how the session ended: *station*, *free-floating*, *lock failure*, *maximum rental time* (`Ending`).
/// - **Drop zone**: a city-approved area for free-floating returns.
/// - **Unauthorized zone**: any position outside every drop zone.
/// - **Zone map**: the City's drop zones valid at a moment (`ZoneMap`); Pricing uses the one valid at the end of the
///   session (DE-84). A position on a zone's border is inside.
/// - **Return fee**: drop zone fee or unauthorized zone return fee; only for free-floating endings (DE-34).
/// - **Penalty**: charged when the maximum rental time was exceeded (DE-33).
/// - **Total fee**: hourly fees + return fee + penalty (`SessionFee`, calculated by `FeeCalculator`).
///
/// ## Business decisions
/// Publishing tariffs, enforced by the `TariffSchedule` aggregate and the `Tariff` value object:
/// - **A complete tariff valid from a future moment is published**: given it is 2026-11-08 10:00, when Paula
///   publishes T-2 valid from 2026-12-01 00:00, then T-2 is published and listed (Tariff Published). See
///   `TariffScheduleTest#completeTariffValidFromFutureMomentIsPublished`.
/// - **No retroactive tariff**: given it is 10:00 and a session started at 09:30, when Paula publishes T-3 valid
///   from 09:00, then the publication is rejected and the session keeps the launch tariff. See
///   `TariffScheduleTest#retroactiveTariffIsRejectedAndStartedRideKeepsLaunchTariff`.
/// - **Valid from "now" is not in the future**: given it is 10:00, when Paula publishes T-3 valid from 10:00, then
///   the publication is rejected. See `TariffScheduleTest#tariffValidFromNowIsRejected`.
/// - **Two tariffs can't share a valid from**: given T-2 valid from 2026-12-01 00:00, when Paula publishes T-5 valid
///   from the same moment, then it is rejected and T-2 stays. See
///   `TariffScheduleTest#secondTariffWithSameValidFromIsRejected`.
/// - **A tariff id is unique** (spec data constraint): see `TariffScheduleTest#secondTariffWithSameIdIsRejected`.
/// - **A tariff states every price**: a tariff without a penalty is rejected. See
///   `TariffTest#tariffWithoutPenaltyIsRejected`.
/// - **Fees can't be negative**: a drop zone fee of PLN -1.00 is rejected. See
///   `TariffTest#tariffWithNegativeDropZoneFeeIsRejected`.
/// - **The tariff valid at session start applies**: given T-2 valid from 2026-12-01 00:00, a session started at
///   2026-11-30 23:50 keeps the launch tariff and one started at 00:05 uses T-2. See
///   `TariffScheduleTest#rideStartedBeforeNewTariffKeepsLaunchTariff`, `TariffScheduleTest#rideStartedAfterNewTariffUsesIt`,
///   `FeeCalculatorTest#rideStartedBeforeNewTariffUsesLaunchTariff`, `FeeCalculatorTest#rideStartedAfterNewTariffUsesIt`.
///
/// Pricing a session, enforced by the `FeeCalculator` domain service:
/// - **A ride within the free period is free**: 10:00-10:12 and 10:00:00-10:15:00 ending at a station cost PLN 0.00.
///   See `FeeCalculatorTest#rideOfTwelveMinutesIsFree`, `FeeCalculatorTest#rideOfExactlyFifteenMinutesIsFree`.
/// - **Every started billing hour is charged**: 10:00:00-10:15:01 is 1 billing hour, PLN 3.00; 10:00:00-11:15:00 is
///   still 1 billing hour; 10:00:00-11:15:01 is 2 billing hours, PLN 8.00. See
///   `FeeCalculatorTest#oneSecondAfterFreePeriodStartsFirstBillingHour`, `FeeCalculatorTest#seventyFiveMinutesIsOneBillingHour`,
///   `FeeCalculatorTest#oneSecondMoreStartsSecondBillingHour`.
/// - **Hourly fees are tiered**: 10:00-13:15 is PLN 3 + 5 + 7 = 15.00; 10:00-14:20 is PLN 3 + 5 + 7 + 9 + 9 = 33.00.
///   See `FeeCalculatorTest#threeBillingHoursCostFifteen`, `FeeCalculatorTest#fourthAndFurtherHoursCostTheSame`.
/// - **A station return and a lock failure pay no return fee**: 10:00-10:30 costs PLN 3.00, wherever a lock failure
///   happened. See `FeeCalculatorTest#stationReturnPaysNoReturnFee`, `FeeCalculatorTest#lockFailurePaysHourlyFeesOnly`,
///   `ReturnFeeTest#lockFailureOutsideDropZonesPaysNoReturnFee`, `ReturnFeeTest#lockFailureInDropZonePaysNoDropZoneFee`.
/// - **A free-floating return in a drop zone adds the drop zone fee**: 10:00-10:30 at (52.2298, 21.0123) costs
///   PLN 3.00 + 2.00 = 5.00; a 10-minute trip still pays PLN 2.00 (DE-84); the border counts as inside. See
///   `ReturnFeeTest#returnInDropZoneAddsDropZoneFee`, `ReturnFeeTest#quickReturnInDropZoneStillPaysDropZoneFee`,
///   `ReturnFeeTest#returnOnDropZoneBorderPaysDropZoneFee`, `CityZoneMapAdapterTest#positionAtBorderIsInside`.
/// - **A free-floating return outside every drop zone adds the unauthorized zone return fee**: 10:00-10:30 at
///   (52.2500, 21.1000) costs PLN 3.00 + 40.00 = 43.00. See `ReturnFeeTest#returnOutsideDropZonesAddsUnauthorizedZoneFee`.
/// - **The zone map valid at the end of the session applies**: Old Town Square removed at 10:20, a session ending
///   there at 10:30 pays PLN 40.00. See `ReturnFeeTest#zoneMapValidAtRideEndApplies`,
///   `CityZoneMapAdapterTest#withdrawnZoneCountsOnlyDuringItsWindow`.
/// - **The return fee comes from the tariff valid at session start**: see
///   `ReturnFeeTest#returnFeeFromLaunchTariffWhenStartedBeforeNewTariff`, `ReturnFeeTest#returnFeeFromNewTariffWhenStartedAfterIt`.
/// - **Exceeding the maximum rental time adds the penalty**: 08:00-20:00 ended for exceeding the maximum rental time
///   costs PLN 96.00 (12 billing hours) + 150.00 = 246.00, no return fee; the same 12 hours ending at a station cost
///   PLN 96.00; the penalty comes from the tariff valid at session start. See
///   `PenaltyTest#overdueRentalPaysTwelveHoursPlusPenalty`, `PenaltyTest#overdueRentalNoticedLateIsPricedToTwelveHourMark`,
///   `PenaltyTest#overdueRentalPaysNoReturnFee`, `PenaltyTest#rideOfExactlyTwelveHoursHasNoPenalty`,
///   `PenaltyTest#penaltyFromTariffValidAtStart`.
///
/// ## Integration
/// - Open Host Service: `GET /pricing/session-price` (Settlement), `POST /pricing/tariffs`, `GET /pricing/tariffs`.
/// - City (external) through the `cityacl` anti-corruption layer, backed by a simulated City zone map.
///
/// ## Assumptions
/// - 🎭 DE-30: launch tariff values.
/// - 🎭 DE-31, DE-83: started hours are charged, measured to the second.
/// - 🎭 DE-84: a position on a zone border is inside; the zone map valid at the end of the ride applies.
/// - 🎭 DE-96: a tariff's valid-from lies in the future when published; two tariffs with the same valid-from are
///   rejected.
/// - Money is in PLN only.
/// - The zone map comes from the City; until a real feed exists, it is a configured list of circular drop zones.
///
/// ## Verification metrics
/// - Share of rides inside the free period (should be high: short trips are encouraged).
/// - Share of free-floating returns paying the unauthorized zone fee (should fall over time).
///
/// ## Open questions
/// - Are tariffs ever retracted before they become valid?
/// - Should a station also be a valid free-floating return (bike left next to a full station)?
/// - Must the unauthorized zone return fee be higher than the drop zone fee? (spec Q4)
///
/// Source canvas: `docs/bounded-contexts/velocity/pricing/canvas.md`
@BoundedContext(name = "Pricing")
@Module
package pl.wojtyna.dddtrainingv3.velocity.pricing;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
