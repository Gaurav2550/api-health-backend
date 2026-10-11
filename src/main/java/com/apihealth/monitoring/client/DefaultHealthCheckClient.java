package com.apihealth.monitoring.client;

import com.apihealth.monitoring.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

@Component
public class DefaultHealthCheckClient implements HealthCheckClient {

    private static final Logger log = LoggerFactory.getLogger(DefaultHealthCheckClient.class);

    private final SsrfValidator ssrfValidator;
    private final int maxResponseBodyBytes;

    public DefaultHealthCheckClient(
            SsrfValidator ssrfValidator,
            @Value("${app.monitoring.max-response-body-bytes:1048576}") int maxResponseBodyBytes) {
        this.ssrfValidator = ssrfValidator;
        this.maxResponseBodyBytes = maxResponseBodyBytes;
    }

    @Override
    public HealthCheck execute(MonitoredApi api) {
        HealthCheck result = new HealthCheck();
        result.setMonitoredApi(api);
        result.setCheckedAt(Instant.now());

        try {
            // SSRF validation
            ssrfValidator.validate(api.getUrl());

            // Build HTTP client with no redirects
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(api.getTimeoutMillis()))
                    .followRedirects(HttpClient.Redirect.NEVER)
                    .build();

            // Build request
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(api.getUrl()))
                    .timeout(Duration.ofMillis(api.getTimeoutMillis()));

            // Set HTTP method and body
            HttpRequest.BodyPublisher bodyPublisher = api.getRequestBody() != null && !api.getRequestBody().isBlank()
                    ? HttpRequest.BodyPublishers.ofString(api.getRequestBody())
                    : HttpRequest.BodyPublishers.noBody();

            switch (api.getHttpMethod()) {
                case GET -> reqBuilder.GET();
                case POST -> reqBuilder.POST(bodyPublisher);
                case PUT -> reqBuilder.PUT(bodyPublisher);
                case DELETE -> reqBuilder.DELETE();
                case PATCH -> reqBuilder.method("PATCH", bodyPublisher);
                case HEAD -> reqBuilder.method("HEAD", HttpRequest.BodyPublishers.noBody());
                case OPTIONS -> reqBuilder.method("OPTIONS", HttpRequest.BodyPublishers.noBody());
            }

            // Parse and set custom headers (JSON-like: "Header1: value1\nHeader2: value2")
            if (api.getRequestHeaders() != null && !api.getRequestHeaders().isBlank()) {
                String[] lines = api.getRequestHeaders().split("\n");
                for (String line : lines) {
                    int colonIdx = line.indexOf(':');
                    if (colonIdx > 0) {
                        String headerName = line.substring(0, colonIdx).trim();
                        String headerValue = line.substring(colonIdx + 1).trim();
                        // Don't forward sensitive headers
                        if (!headerName.equalsIgnoreCase("Authorization")
                                && !headerName.equalsIgnoreCase("Cookie")) {
                            reqBuilder.header(headerName, headerValue);
                        }
                    }
                }
            }

            HttpRequest request = reqBuilder.build();

            // Measure latency
            long startNanos = System.nanoTime();
            HttpResponse<InputStream> response = client.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());
            long elapsedNanos = System.nanoTime() - startNanos;
            long responseTimeMillis = elapsedNanos / 1_000_000;

            // Read limited response body to avoid OOM
            try (InputStream is = response.body()) {
                is.readNBytes(maxResponseBodyBytes);
            }

            int statusCode = response.statusCode();
            result.setHttpStatusCode(statusCode);
            result.setResponseTimeMillis(responseTimeMillis);

            // Determine status
            boolean expectedStatus = api.isExpectedStatusCode(statusCode);
            if (!expectedStatus) {
                result.setStatus(HealthStatus.DOWN);
                result.setErrorType(ErrorType.UNEXPECTED_STATUS);
                result.setErrorMessage("Unexpected status code: " + statusCode);
            } else if (responseTimeMillis > api.getLatencyThresholdMillis()) {
                // UP but SLOW
                result.setStatus(HealthStatus.SLOW);
                result.setErrorType(ErrorType.NONE);
            } else {
                result.setStatus(HealthStatus.UP);
                result.setErrorType(ErrorType.NONE);
            }

        } catch (java.net.http.HttpTimeoutException e) {
            result.setStatus(HealthStatus.DOWN);
            result.setErrorType(ErrorType.TIMEOUT);
            result.setErrorMessage("Request timed out");
        } catch (UnknownHostException e) {
            result.setStatus(HealthStatus.DOWN);
            result.setErrorType(ErrorType.DNS_RESOLUTION);
            result.setErrorMessage("DNS resolution failed");
        } catch (ConnectException e) {
            result.setStatus(HealthStatus.DOWN);
            result.setErrorType(ErrorType.CONNECTION_REFUSED);
            result.setErrorMessage("Connection refused");
        } catch (javax.net.ssl.SSLException e) {
            result.setStatus(HealthStatus.DOWN);
            result.setErrorType(ErrorType.SSL_ERROR);
            result.setErrorMessage("SSL/TLS error");
        } catch (com.apihealth.common.exception.ApiException e) {
            // SSRF blocked
            result.setStatus(HealthStatus.DOWN);
            result.setErrorType(ErrorType.SSRF_BLOCKED);
            result.setErrorMessage("URL blocked by security policy");
        } catch (IOException e) {
            result.setStatus(HealthStatus.DOWN);
            result.setErrorType(ErrorType.IO_ERROR);
            result.setErrorMessage("I/O error during request");
            log.debug("IO error checking {}: {}", sanitizeUrl(api.getUrl()), e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.setStatus(HealthStatus.DOWN);
            result.setErrorType(ErrorType.IO_ERROR);
            result.setErrorMessage("Request interrupted");
        } catch (Exception e) {
            result.setStatus(HealthStatus.DOWN);
            result.setErrorType(ErrorType.UNKNOWN);
            result.setErrorMessage("Unexpected error during health check");
            log.error("Unexpected error checking {}: {}", sanitizeUrl(api.getUrl()), e.getMessage());
        }

        return result;
    }

    private String sanitizeUrl(String url) {
        if (url == null) return "null";
        // Only show scheme + host, not query params or auth info
        try {
            URI uri = new URI(url);
            return uri.getScheme() + "://" + uri.getHost();
        } catch (Exception e) {
            return "[invalid-url]";
        }
    }
}
