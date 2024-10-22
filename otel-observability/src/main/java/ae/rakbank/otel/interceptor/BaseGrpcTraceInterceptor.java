package ae.rakbank.otel.interceptor;


import ae.rakbank.otel.CustomGrpcAttributeProvider;
import ae.rakbank.otel.config.ObservabilityConfigProperties;
import io.grpc.*;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import org.slf4j.MDC;

import java.util.Map;

public class BaseGrpcTraceInterceptor implements ServerInterceptor {

    private final CustomGrpcAttributeProvider attributeProvider;
    private final ObservabilityConfigProperties configProperties;

    public BaseGrpcTraceInterceptor(CustomGrpcAttributeProvider attributeProvider,
                                    ObservabilityConfigProperties configProperties) {
        this.attributeProvider = attributeProvider;
        this.configProperties = configProperties;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {

        Context extractedContext = GlobalOpenTelemetry.getPropagators()
                .getTextMapPropagator()
                .extract(Context.current(), headers, new GrpcMetadataGetter());

        Tracer tracer = getTracer();
        SpanBuilder spanBuilder = tracer.spanBuilder(call.getMethodDescriptor().getFullMethodName())
                .setParent(extractedContext);

        // Set correlationId if available
        String correlationId = headers.get(Metadata.Key.of(
                configProperties.getCorrelationIdHeader(), Metadata.ASCII_STRING_MARSHALLER));
        if (correlationId != null) {
            spanBuilder.setAttribute("trace.correlationId", correlationId);
            MDC.put("correlationId", correlationId);  // Add to MDC
        }

        Span span = spanBuilder.startSpan();
        MDC.put("traceId", span.getSpanContext().getTraceId());  // Add traceId to MDC

        Map<String, String> customAttributes = attributeProvider.getAttributes(headers);
        customAttributes.forEach(span::setAttribute);

        try (Scope scope = span.makeCurrent()) {
            return next.startCall(call, headers);
        } finally {
            MDC.remove("traceId");
            MDC.remove("correlationId");
            span.end();
        }
    }

    private static class GrpcMetadataGetter implements TextMapGetter<Metadata> {
        @Override
        public Iterable<String> keys(Metadata metadata) {
            return metadata.keys();
        }

        @Override
        public String get(Metadata metadata, String key) {
            Metadata.Key<String> metadataKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
            return metadata.get(metadataKey);
        }
    }

    private Tracer getTracer() {
        return GlobalOpenTelemetry.getTracer("otel-grpc-tracer");
    }
}
