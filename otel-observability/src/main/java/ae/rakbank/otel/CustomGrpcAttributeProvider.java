package ae.rakbank.otel;


import io.grpc.Metadata;

import java.util.Map;

@FunctionalInterface
public interface CustomGrpcAttributeProvider {
    Map<String, String> getAttributes(Metadata headers);
}

