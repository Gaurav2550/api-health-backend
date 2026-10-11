package com.apihealth.monitoring.service;

import com.apihealth.auth.service.AuthService;
import com.apihealth.common.exception.ApiException;
import com.apihealth.common.exception.ResourceNotFoundException;
import com.apihealth.monitoring.client.SsrfValidator;
import com.apihealth.monitoring.dto.*;
import com.apihealth.monitoring.entity.MonitoredApi;
import com.apihealth.monitoring.repository.MonitoredApiRepository;
import com.apihealth.user.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonitoredApiService {

    private final MonitoredApiRepository apiRepository;
    private final AuthService authService;
    private final SsrfValidator ssrfValidator;

    @Value("${app.monitoring.max-apis-per-user:50}")
    private int maxApisPerUser;

    @Value("${app.monitoring.min-interval-seconds:30}")
    private int minIntervalSeconds;

    @Value("${app.monitoring.max-interval-seconds:3600}")
    private int maxIntervalSeconds;

    @Value("${app.monitoring.default-interval-seconds:60}")
    private int defaultIntervalSeconds;

    @Value("${app.monitoring.default-timeout-millis:10000}")
    private int defaultTimeoutMillis;

    public MonitoredApiService(MonitoredApiRepository apiRepository, AuthService authService,
                               SsrfValidator ssrfValidator) {
        this.apiRepository = apiRepository;
        this.authService = authService;
        this.ssrfValidator = ssrfValidator;
    }

    @Transactional
    public MonitoredApiResponse create(CreateApiRequest request, String userEmail) {
        User owner = authService.getUserByEmail(userEmail);

        // Check API limit
        long count = apiRepository.countByOwnerId(owner.getId());
        if (count >= maxApisPerUser) {
            throw new ApiException("Maximum number of monitored APIs (" + maxApisPerUser + ") reached",
                    HttpStatus.BAD_REQUEST);
        }

        // Validate URL for SSRF
        ssrfValidator.validate(request.getUrl());

        MonitoredApi api = new MonitoredApi();
        api.setName(request.getName());
        api.setUrl(request.getUrl());
        api.setHttpMethod(request.getHttpMethod() != null ? request.getHttpMethod()
                : com.apihealth.monitoring.entity.HttpMethod.GET);
        api.setRequestHeaders(request.getRequestHeaders());
        api.setRequestBody(request.getRequestBody());
        api.setCheckIntervalSeconds(clampInterval(request.getCheckIntervalSeconds()));
        api.setTimeoutMillis(request.getTimeoutMillis() != null ? request.getTimeoutMillis() : defaultTimeoutMillis);
        api.setLatencyThresholdMillis(request.getLatencyThresholdMillis() != null
                ? request.getLatencyThresholdMillis() : 500);
        api.setExpectedStatusCodes(request.getExpectedStatusCodes() != null
                ? request.getExpectedStatusCodes() : "200");
        api.setActive(true);
        api.setOwner(owner);

        api = apiRepository.save(api);
        return MonitoredApiResponse.fromEntity(api);
    }

    @Transactional(readOnly = true)
    public Page<MonitoredApiResponse> findAll(String userEmail, Pageable pageable) {
        User owner = authService.getUserByEmail(userEmail);
        return apiRepository.findByOwnerId(owner.getId(), pageable)
                .map(MonitoredApiResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public MonitoredApiResponse findById(Long id, String userEmail) {
        MonitoredApi api = getOwnedApi(id, userEmail);
        return MonitoredApiResponse.fromEntity(api);
    }

    @Transactional
    public MonitoredApiResponse update(Long id, UpdateApiRequest request, String userEmail) {
        MonitoredApi api = getOwnedApi(id, userEmail);

        if (request.getName() != null) api.setName(request.getName());
        if (request.getUrl() != null) {
            ssrfValidator.validate(request.getUrl());
            api.setUrl(request.getUrl());
        }
        if (request.getHttpMethod() != null) api.setHttpMethod(request.getHttpMethod());
        if (request.getRequestHeaders() != null) api.setRequestHeaders(request.getRequestHeaders());
        if (request.getRequestBody() != null) api.setRequestBody(request.getRequestBody());
        if (request.getCheckIntervalSeconds() != null)
            api.setCheckIntervalSeconds(clampInterval(request.getCheckIntervalSeconds()));
        if (request.getTimeoutMillis() != null) api.setTimeoutMillis(request.getTimeoutMillis());
        if (request.getLatencyThresholdMillis() != null)
            api.setLatencyThresholdMillis(request.getLatencyThresholdMillis());
        if (request.getExpectedStatusCodes() != null)
            api.setExpectedStatusCodes(request.getExpectedStatusCodes());

        api = apiRepository.save(api);
        return MonitoredApiResponse.fromEntity(api);
    }

    @Transactional
    public void delete(Long id, String userEmail) {
        MonitoredApi api = getOwnedApi(id, userEmail);
        apiRepository.delete(api);
    }

    @Transactional
    public MonitoredApiResponse activate(Long id, String userEmail) {
        MonitoredApi api = getOwnedApi(id, userEmail);
        api.setActive(true);
        api = apiRepository.save(api);
        return MonitoredApiResponse.fromEntity(api);
    }

    @Transactional
    public MonitoredApiResponse deactivate(Long id, String userEmail) {
        MonitoredApi api = getOwnedApi(id, userEmail);
        api.setActive(false);
        api = apiRepository.save(api);
        return MonitoredApiResponse.fromEntity(api);
    }

    /**
     * Returns the owned MonitoredApi entity for use by other services (e.g., manual check).
     */
    @Transactional(readOnly = true)
    public MonitoredApi getOwnedApi(Long id, String userEmail) {
        User owner = authService.getUserByEmail(userEmail);
        MonitoredApi api = apiRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monitored API not found with id: " + id));
        if (!api.getOwner().getId().equals(owner.getId())) {
            throw new com.apihealth.common.exception.AccessDeniedException("You do not have permission to access this monitored API");
        }
        return api;
    }

    private int clampInterval(Integer interval) {
        if (interval == null) return defaultIntervalSeconds;
        return Math.max(minIntervalSeconds, Math.min(maxIntervalSeconds, interval));
    }
}
