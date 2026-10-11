package com.apihealth.monitoring.client;

import com.apihealth.common.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.net.*;
import java.util.Set;

/**
 * SSRF Protection: Validates URLs to prevent Server-Side Request Forgery.
 *
 * <p>This validates at the application layer. Network-level egress controls (firewall rules,
 * security groups) are recommended as additional defense. Application-level DNS checks alone
 * cannot fully eliminate DNS rebinding / TOCTOU risks.</p>
 *
 * <p><b>Residual Risks:</b></p>
 * <ul>
 *   <li>DNS rebinding: A target may resolve to a safe IP during validation, then re-resolve
 *       to an internal IP during the actual HTTP request.</li>
 *   <li>IPv6 mapped IPv4 addresses may bypass some checks depending on JVM networking config.</li>
 *   <li>HTTP redirects to internal hosts (mitigated by disabling redirects).</li>
 * </ul>
 * <p>For production use, combine with network-level egress restrictions.</p>
 */
@Component
public class SsrfValidator {

    private static final Logger log = LoggerFactory.getLogger(SsrfValidator.class);

    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");
    private static final Set<Integer> BLOCKED_PORTS = Set.of(
            22, 25, 53, 110, 143, 445, 587, 993, 995, 3306, 5432, 6379, 27017
    );

    /**
     * Validates a URL for SSRF safety. Throws ApiException if the URL is unsafe.
     */
    public void validate(String urlString) {
        URI uri;
        try {
            uri = new URI(urlString);
        } catch (URISyntaxException e) {
            throw new ApiException("Invalid URL format", HttpStatus.BAD_REQUEST);
        }

        // Check scheme
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new ApiException("Only HTTP and HTTPS URLs are allowed", HttpStatus.BAD_REQUEST);
        }

        // Check host
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new ApiException("URL must have a valid host", HttpStatus.BAD_REQUEST);
        }

        // Reject localhost and loopback
        if (isLocalhost(host)) {
            throw new ApiException("URLs targeting localhost or loopback addresses are not allowed", HttpStatus.BAD_REQUEST);
        }

        // Check port
        int port = uri.getPort();
        if (port != -1 && BLOCKED_PORTS.contains(port)) {
            throw new ApiException("The specified port is not allowed for monitoring", HttpStatus.BAD_REQUEST);
        }

        // Resolve DNS and check for private/reserved IPs
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                validateIpAddress(addr);
            }
        } catch (UnknownHostException e) {
            throw new ApiException("Could not resolve hostname: " + sanitizeHost(host), HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isLocalhost(String host) {
        String lower = host.toLowerCase();
        return lower.equals("localhost")
                || lower.equals("127.0.0.1")
                || lower.equals("[::1]")
                || lower.equals("::1")
                || lower.equals("0.0.0.0")
                || lower.startsWith("127.");
    }

    private void validateIpAddress(InetAddress addr) {
        if (addr.isLoopbackAddress()) {
            throw new ApiException("Loopback addresses are not allowed", HttpStatus.BAD_REQUEST);
        }
        if (addr.isSiteLocalAddress()) {
            throw new ApiException("Private/site-local addresses are not allowed", HttpStatus.BAD_REQUEST);
        }
        if (addr.isLinkLocalAddress()) {
            throw new ApiException("Link-local addresses are not allowed", HttpStatus.BAD_REQUEST);
        }
        if (addr.isMulticastAddress()) {
            throw new ApiException("Multicast addresses are not allowed", HttpStatus.BAD_REQUEST);
        }
        if (addr.isAnyLocalAddress()) {
            throw new ApiException("Wildcard addresses are not allowed", HttpStatus.BAD_REQUEST);
        }

        // Check for cloud metadata addresses (169.254.169.254)
        byte[] bytes = addr.getAddress();
        if (bytes.length == 4) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            int b2 = bytes[2] & 0xFF;
            int b3 = bytes[3] & 0xFF;

            // 169.254.169.254 - cloud metadata
            if (b0 == 169 && b1 == 254 && b2 == 169 && b3 == 254) {
                throw new ApiException("Cloud metadata addresses are not allowed", HttpStatus.BAD_REQUEST);
            }

            // 10.x.x.x - RFC 1918
            if (b0 == 10) {
                throw new ApiException("Private network addresses (10.x.x.x) are not allowed", HttpStatus.BAD_REQUEST);
            }

            // 172.16.0.0 - 172.31.255.255 - RFC 1918
            if (b0 == 172 && b1 >= 16 && b1 <= 31) {
                throw new ApiException("Private network addresses (172.16-31.x.x) are not allowed", HttpStatus.BAD_REQUEST);
            }

            // 192.168.x.x - RFC 1918
            if (b0 == 192 && b1 == 168) {
                throw new ApiException("Private network addresses (192.168.x.x) are not allowed", HttpStatus.BAD_REQUEST);
            }

            // 0.0.0.0/8 - reserved
            if (b0 == 0) {
                throw new ApiException("Reserved addresses are not allowed", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private String sanitizeHost(String host) {
        // Only keep alphanumeric, dots, hyphens, colons (for IPv6), brackets
        if (host.length() > 100) {
            host = host.substring(0, 100);
        }
        return host.replaceAll("[^a-zA-Z0-9.:\\-\\[\\]]", "");
    }
}
