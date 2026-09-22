package org.example.observability_app.config;

import io.micrometer.tracing.Tracer;
import org.springframework.stereotype.Component;

@Component
public class SpanTagger {

    private final Tracer tracer;

    public SpanTagger(Tracer tracer) {
        this.tracer = tracer;
    }

    public void tag(String key, String value) {
        var span = tracer.currentSpan();
        if (span != null && value != null) {
            span.tag(key, value);
        }
    }

    public void tag(String key, long value) {
        var span = tracer.currentSpan();
        if (span != null) {
            span.tag(key, String.valueOf(value));
        }
    }
}