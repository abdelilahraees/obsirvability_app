package org.example.observability_app.config;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public MeterFilter renameHttpServerRequestsToOtelConvention() {
        return new MeterFilter() {
            @Override
            public Meter.Id map(Meter.Id id) {
                if ("http.server.requests".equals(id.getName())) {
                    return id.withName("http.server.request.duration");
                }
                return id;
            }
        };
    }
}
