package org.example.observability_app.config;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class OtelLogAppenderInstaller {

    private final OpenTelemetry openTelemetry;

    public OtelLogAppenderInstaller(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @PostConstruct
    void install() {
        OpenTelemetryAppender.install(openTelemetry);
    }
}
