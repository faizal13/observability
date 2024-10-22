package ae.rakbank.otel;


import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@FunctionalInterface
public interface CustomAttributeProvider {
    Map<String, String> getAttributes(HttpServletRequest request);
}

