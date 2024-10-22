package ae.rakbank.otel.config;


import ae.rakbank.otel.CustomAttributeProvider;
import ae.rakbank.otel.interceptor.BaseTraceInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;

@Configuration
@EnableConfigurationProperties(ObservabilityConfigProperties.class)
public class ObservabilityAutoConfiguration implements WebMvcConfigurer {

    private final CustomAttributeProvider attributeProvider;
    private final ObservabilityConfigProperties configProperties;

    public ObservabilityAutoConfiguration(CustomAttributeProvider attributeProvider, ObservabilityConfigProperties configProperties) {
        this.attributeProvider = attributeProvider;
        this.configProperties = configProperties;
    }

    @Bean
    @ConditionalOnMissingBean
    public CustomAttributeProvider defaultAttributeProvider() {
        return request -> Map.of();  // Default: no custom attributes
    }

    @Bean
    public BaseTraceInterceptor baseTraceInterceptor() {
        return new BaseTraceInterceptor(attributeProvider, configProperties);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(baseTraceInterceptor());
    }
}

