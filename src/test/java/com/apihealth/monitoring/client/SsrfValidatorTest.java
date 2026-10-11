package com.apihealth.monitoring.client;

import com.apihealth.common.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SsrfValidatorTest {

    private SsrfValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SsrfValidator();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ftp://example.com/file",
            "file:///etc/passwd",
            "gopher://example.com",
            "data:text/html,hello"
    })
    @DisplayName("Should reject non-HTTP(S) schemes")
    void testDisallowedSchemes(String url) {
        ApiException ex = assertThrows(ApiException.class, () -> validator.validate(url));
        assertTrue(ex.getMessage().contains("Only HTTP and HTTPS URLs are allowed") || ex.getMessage().contains("Invalid URL"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost/api",
            "http://localhost:8080/test",
            "http://127.0.0.1/api",
            "http://127.0.0.1:3000",
            "http://0.0.0.0/"
    })
    @DisplayName("Should reject localhost and loopback targets")
    void testLocalhostBlocked(String url) {
        ApiException ex = assertThrows(ApiException.class, () -> validator.validate(url));
        assertTrue(ex.getMessage().contains("localhost or loopback"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://10.0.0.1:8080/metrics",
            "http://192.168.1.1/admin",
            "http://172.16.0.1/status",
            "http://169.254.169.254/latest/meta-data"
    })
    @DisplayName("Should reject private network and cloud metadata IPs")
    void testPrivateIpsBlocked(String url) {
        assertThrows(ApiException.class, () -> validator.validate(url));
    }

    @ParameterizedTest
    @ValueSource(ints = { 22, 25, 3306, 5432, 6379, 27017 })
    @DisplayName("Should reject prohibited infrastructure ports")
    void testBlockedPorts(int port) {
        String url = "http://1.1.1.1:" + port + "/";
        ApiException ex = assertThrows(ApiException.class, () -> validator.validate(url));
        assertTrue(ex.getMessage().contains("port is not allowed"));
    }
}
