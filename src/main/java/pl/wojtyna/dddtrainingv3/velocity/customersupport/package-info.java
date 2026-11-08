/// # CustomerSupport bounded context
///
/// ## Purpose
/// Makes sure no rider's problem is forgotten: every reported incident is routed by its type, worked by a support
/// agent and driven to a resolved state, whether it was accepted, escalated, rejected or reopened. Fleet issues are
/// handed straight to the people who fix bikes.
///
/// ## Strategic classification
/// - Domain: supporting
/// - Business model: engagement
/// - Evolution: custom built
///
/// ## Domain roles
/// - Execution Model: runs each incident to resolution.
///
/// ## Ubiquitous language
/// - **Incident**: something a rider reported: a broken bike, a bike they can't unlock, a wrongful charge, a safety
///   issue, anything else.
/// - **Category**: what the rider picked when reporting: *broken bike*, *can't unlock*, *wrongful charge*, *safety*,
///   *other*.
/// - **Incident type**: *fleet issue*, *billing dispute*, *safety concern*, *other*; detected from the category (DE-50).
/// - **Open**: reported and not yet picked up by any support agent.
/// - **Assigned agent**: the support agent who picked the incident up; only they work it.
/// - **Accepted / Rejected**: the agent's decision on an assigned incident.
/// - **Escalated**: an accepted incident handed to the billing, fleet or safety team.
/// - **Reopened**: a rejected incident the rider pushed back on; it must be resolved, never rejected again.
/// - **Reopenable until**: the end of the 14th calendar day after the day of the rejection, in the city's local time
///   (DE-54, DE-56; `Europe/Warsaw`, configurable as `velocity.customer-support.zone`).
/// - **Resolved**: the terminal state every incident reaches.
///
/// ## Business decisions
/// All enforced by the `Incident` aggregate (`domain/incident`); tests in `IncidentTest` unless noted.
/// - **The incident type follows the category**: *broken bike* or *can't unlock* with a bike → fleet issue (Fleet Issue
///   Reported); *wrongful charge* → billing dispute; *safety* → safety concern; otherwise, or without a bike → other
///   (DE-50). See `IncidentTest.Reporting` (`cantUnlockWithBikeIsFleetIssue`, `safetyReportIsSafetyConcern`,
///   `brokenBikeWithoutBikeIsOtherAndNotHandedOver`, `wrongfulChargeReportIsOpenBillingDispute`,
///   `wrongfulChargeNamingBikeIsNotHandedOver`, `brokenBikeReportIsFleetIssueHandedToFleetMaintenance`,
///   `brokenBikeReportDuringRideIsFleetIssue`).
/// - **A blocked rider can report an incident** (DE-97): see `blockedRiderCanReportIncident`.
/// - **An incident is picked up once**: given I-2 is assigned to Zoe, when Max picks it up, then it is rejected. See
///   `secondPickUpIsRejected`, `agentPicksUpOpenIncident`.
/// - **Only the assigned agent decides, escalates or resolves** (DE-51): see `otherAgentCannotAccept`,
///   `openIncidentCannotBeAcceptedWithoutPickUp`, `otherAgentCannotEscalate`, `otherAgentCannotResolve`.
/// - **A rejection carries a reason; an accepted incident isn't decided again; a reopened incident can be accepted
///   but never rejected again** (DE-54, DE-97): see `assignedAgentRejectsIncidentWithReason`,
///   `acceptedIncidentCannotBeRejected`, `acceptedIncidentCannotBeAcceptedAgain`,
///   `assignedAgentAcceptsReopenedIncident`, `reopenedIncidentCannotBeRejectedAgain`.
/// - **Only an accepted incident is escalated, at most once, to billing, fleet or safety, whatever its type**
///   (DE-53, DE-97): see `IncidentTest.Escalating`; an unknown team is malformed input (`400 UnknownTeam`).
/// - **A rider can reopen a rejected incident within 14 calendar days; only the reporting rider, only a rejected
///   incident; it stays with its agent** (DE-54, DE-56): see `IncidentTest.Reopening` and `ReopeningWindowTest`.
/// - **Only an accepted, escalated or reopened incident is resolved by its agent, once; an escalated incident
///   records the team's outcome**: see `IncidentTest.Resolving`.
/// - **An uncontested rejection is resolved when the window closes** ("rejection not contested", DE-87); a reopened
///   incident isn't: see `uncontestedRejectionIsResolvedWhenWindowEnds`,
///   `rejectionIsStillContestableOnTheLastMinuteOfTheWindow`, `reopenedIncidentIsNotResolvedByTheClock`.
/// - **A fixed bike resolves its fleet issues from any open state; a resolved incident keeps its resolution**
///   ("bike fixed", DE-55): see `IncidentTest.ResolvingFleetIssues`.
///
/// ## Integration
/// - Open Host Service: REST resources under `/customer-support/incidents` (integration-contracts.md), including
///   *Resolve Fleet Issue* for Fleet Maintenance.
/// - Published language: `api.events.FleetIssueReported` (listener: Fleet Maintenance).
///
/// ## Assumptions
/// - DE-50 to DE-56, DE-85, DE-87, DE-97 (role-played domain expert).
/// - The context map's "fleet-issue incident" link (CustomerSupport → FleetMaintenance) is confirmed by DE-52.
///
/// ## Verification metrics
/// - Time from report to resolution, per type.
/// - Share of rejected incidents that get reopened.
///
/// ## Open questions
/// - Billing disputes should pause debt recovery (reference map: deferred).
/// - Can an incident be handed over to another agent (spec support-agent-decides-on-incident Q3)? Not built.
/// - In which time zone does "the end of the day" count (spec rider-reopens-rejected-incident Q2)? Assumed Warsaw.
/// - Can an agent resolve a rejected incident before its window ends (spec support-agent-resolves-incident Q3)? No.
///
/// Source canvas: `docs/bounded-contexts/velocity/customersupport/canvas.md`
@BoundedContext(name = "CustomerSupport")
@Module
package pl.wojtyna.dddtrainingv3.velocity.customersupport;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
