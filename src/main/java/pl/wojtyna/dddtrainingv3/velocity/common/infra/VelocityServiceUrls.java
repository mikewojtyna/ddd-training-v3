package pl.wojtyna.dddtrainingv3.velocity.common.infra;

import org.springframework.core.env.Environment;

/// Base URL of the VeloCity contexts' REST resources, used by ACL adapters.
///
/// All contexts run in this one application, so by default the base URL is this application itself
/// (`http://localhost:<port>`). The port is resolved at call time because, with a random port (tests), it is known
/// only after the web server started. Set `velocity.services.base-url` to point the adapters elsewhere.
public class VelocityServiceUrls {

    private final Environment environment;

    public VelocityServiceUrls(Environment environment) {
        this.environment = environment;
    }

    public String baseUrl() {
        var configured = environment.getProperty("velocity.services.base-url");
        if (configured != null && !configured.isBlank()) {
            return configured;
        }
        var port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
        return "http://localhost:" + port;
    }

    public String url(String path) {
        return baseUrl() + path;
    }
}
