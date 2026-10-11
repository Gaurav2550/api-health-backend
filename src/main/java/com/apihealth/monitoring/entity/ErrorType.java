package com.apihealth.monitoring.entity;

public enum ErrorType {
    NONE,
    TIMEOUT,
    DNS_RESOLUTION,
    CONNECTION_REFUSED,
    CONNECTION_RESET,
    SSL_ERROR,
    UNEXPECTED_STATUS,
    IO_ERROR,
    SSRF_BLOCKED,
    UNKNOWN
}
