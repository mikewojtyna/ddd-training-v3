/// Code shared by all VeloCity bounded contexts. Not a bounded context itself: it holds only technical plumbing
/// (clock, service URLs, async event delivery, scheduling switch, REST error body), never domain concepts.
package pl.wojtyna.dddtrainingv3.velocity.common;
