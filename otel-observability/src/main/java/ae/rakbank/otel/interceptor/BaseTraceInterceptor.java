package ae.rakbank.otel.interceptor;



import ae.rakbank.otel.CustomAttributeProvider;
import ae.rakbank.otel.config.ObservabilityConfigProperties;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

public class BaseTraceInterceptor implements HandlerInterceptor {

    private final CustomAttributeProvider attributeProvider;
    private final ObservabilityConfigProperties configProperties;

    public BaseTraceInterceptor(CustomAttributeProvider attributeProvider,
                                ObservabilityConfigProperties configProperties) {
        this.attributeProvider = attributeProvider;
        this.configProperties = configProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Extract trace context from request headers
        Context extractedContext = GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), request, new HttpServletRequestGetter());

        Tracer tracer = getTracer();
        SpanBuilder spanBuilder = tracer.spanBuilder(request.getRequestURI())
                .setParent(extractedContext);

        // Set correlationId if available
        String correlationId = request.getHeader(configProperties.getCorrelationIdHeader());
        if (correlationId != null) {
            spanBuilder.setAttribute("trace.correlationId", correlationId);
            MDC.put("correlationId", correlationId);  // Add to MDC
        }

        // Add trace ID to MDC for logging
        Span span = spanBuilder.startSpan();
        MDC.put("traceId", span.getSpanContext().getTraceId());

        // Add any custom attributes to the span
        Map<String, String> customAttributes = attributeProvider.getAttributes(request);
        customAttributes.forEach(span::setAttribute);

        // Make the span current in the context
        try (Scope scope = span.makeCurrent()) {
            return true;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        // Remove traceId and correlationId from MDC
        MDC.remove("traceId");
        MDC.remove("correlationId");

        // End the current span
        Span.current().end();
    }

    private static class HttpServletRequestGetter implements TextMapGetter<HttpServletRequest> {
        @Override
        public Iterable<String> keys(HttpServletRequest request) {
            return request.getHeaderNames()::asIterator;
        }

        @Override
        public String get(HttpServletRequest request, String key) {
            return request.getHeader(key);
        }
    }

    private Tracer getTracer() {
        return GlobalOpenTelemetry.getTracer("otel-tracer");
    }
}
