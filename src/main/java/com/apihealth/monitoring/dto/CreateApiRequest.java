package com.apihealth.monitoring.dto;

import com.apihealth.monitoring.entity.HttpMethod;
import jakarta.validation.constraints.*;

public class CreateApiRequest {

    @NotBlank(message = "Name is required")
    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    @NotBlank(message = "URL is required")
    @Size(max = 2048, message = "URL must not exceed 2048 characters")
    private String url;

    private HttpMethod httpMethod = HttpMethod.GET;

    @Size(max = 5000, message = "Request headers must not exceed 5000 characters")
    private String requestHeaders;

    @Size(max = 10000, message = "Request body must not exceed 10000 characters")
    private String requestBody;

    @Min(value = 10, message = "Check interval must be at least 10 seconds")
    @Max(value = 3600, message = "Check interval must not exceed 3600 seconds")
    private Integer checkIntervalSeconds;

    @Min(value = 1000, message = "Timeout must be at least 1000 ms")
    @Max(value = 30000, message = "Timeout must not exceed 30000 ms")
    private Integer timeoutMillis;

    @Min(value = 50, message = "Latency threshold must be at least 50 ms")
    @Max(value = 60000, message = "Latency threshold must not exceed 60000 ms")
    private Integer latencyThresholdMillis;

    @Size(max = 100, message = "Expected status codes must not exceed 100 characters")
    private String expectedStatusCodes;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public HttpMethod getHttpMethod() { return httpMethod; }
    public void setHttpMethod(HttpMethod httpMethod) { this.httpMethod = httpMethod; }

    public String getRequestHeaders() { return requestHeaders; }
    public void setRequestHeaders(String requestHeaders) { this.requestHeaders = requestHeaders; }

    public String getRequestBody() { return requestBody; }
    public void setRequestBody(String requestBody) { this.requestBody = requestBody; }

    public Integer getCheckIntervalSeconds() { return checkIntervalSeconds; }
    public void setCheckIntervalSeconds(Integer checkIntervalSeconds) { this.checkIntervalSeconds = checkIntervalSeconds; }

    public Integer getTimeoutMillis() { return timeoutMillis; }
    public void setTimeoutMillis(Integer timeoutMillis) { this.timeoutMillis = timeoutMillis; }

    public Integer getLatencyThresholdMillis() { return latencyThresholdMillis; }
    public void setLatencyThresholdMillis(Integer latencyThresholdMillis) { this.latencyThresholdMillis = latencyThresholdMillis; }

    public String getExpectedStatusCodes() { return expectedStatusCodes; }
    public void setExpectedStatusCodes(String expectedStatusCodes) { this.expectedStatusCodes = expectedStatusCodes; }
}
