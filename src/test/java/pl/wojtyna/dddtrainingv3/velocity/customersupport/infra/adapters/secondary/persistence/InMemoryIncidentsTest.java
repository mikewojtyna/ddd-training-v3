package pl.wojtyna.dddtrainingv3.velocity.customersupport.infra.adapters.secondary.persistence;

import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.*;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class InMemoryIncidentsTest {

    private final InMemoryIncidents incidents = new InMemoryIncidents();

    @Test
    void secondConcurrentSaveOfTheSameIncidentIsRejected() {
        // given
        var id = new IncidentId("I-1");
        incidents.save(Incident.report(id, new IncidentReport(new RiderId("anna"), Category.OTHER, new Description("x"), Optional.empty()),
                                       Instant.parse("2026-11-08T10:00:00Z")).incident());
        var first = incidents.load(id).orElseThrow();
        var second = incidents.load(id).orElseThrow();
        first.pickUpBy(new AgentId("zoe")).at(Instant.parse("2026-11-08T10:05:00Z"));
        second.pickUpBy(new AgentId("max")).at(Instant.parse("2026-11-08T10:05:00Z"));

        // when
        incidents.save(first);
        var exception = catchThrowable(() -> incidents.save(second));

        // then
        assertThat(exception).isInstanceOf(OptimisticLockingException.class);
    }

    @Test
    void savedIncidentIsLoadedWithItsStateAndNextVersion() {
        // given
        var id = new IncidentId("I-2");
        var incident = Incident.report(id, new IncidentReport(new RiderId("anna"), Category.OTHER, new Description("x"), Optional.empty()),
                                       Instant.parse("2026-11-08T10:00:00Z")).incident();
        incident.pickUpBy(new AgentId("zoe")).at(Instant.parse("2026-11-08T10:05:00Z"));

        // when
        incidents.save(incident);
        var loaded = incidents.load(id).orElseThrow();

        // then
        assertThat(loaded.status()).isEqualTo(IncidentStatus.PICKED_UP);
        assertThat(loaded.assignedAgent()).contains(new AgentId("zoe"));
        assertThat(loaded.version().value()).isEqualTo(1);
    }
}
