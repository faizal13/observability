package ae.rakbank.otel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "observability")
public class ObservabilityConfigProperties {

    /**
     * The header key used to extract the correlation ID.
     */
    private String correlationIdHeader = "correlationId"; // Default value

    public String getCorrelationIdHeader() {
        return correlationIdHeader;
    }

    public void setCorrelationIdHeader(String correlationIdHeader) {
        this.correlationIdHeader = correlationIdHeader;
    }
}
