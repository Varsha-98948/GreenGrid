package com.greengrid.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "greengrid.encryption")
public record EncryptionProperties(String key) {
}
