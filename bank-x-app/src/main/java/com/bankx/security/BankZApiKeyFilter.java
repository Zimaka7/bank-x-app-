package com.bankx.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security filter for Bank Z API endpoints
 * Validates API key for all Bank Z integration requests
 */
@Component
public class BankZApiKeyFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(BankZApiKeyFilter.class);
    
    private static final String API_KEY_HEADER = "X-API-Key";
    private static final String BANKZ_PATH_PREFIX = "/api/bankz";
    
    @Value("${bankz.api.key:BANKZ-API-KEY-12345}")
    private String validApiKey;
    
    @Value("${bankz.security.enabled:false}")
    private boolean securityEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestPath = request.getRequestURI();
        
        // Only apply security to Bank Z endpoints
        if (!requestPath.startsWith(BANKZ_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        // Skip security if disabled (for development/testing)
        if (!securityEnabled) {
            logger.debug("Bank Z API security is disabled, allowing request to: {}", requestPath);
            filterChain.doFilter(request, response);
            return;
        }
        
        String providedApiKey = request.getHeader(API_KEY_HEADER);
        
        if (providedApiKey == null || providedApiKey.trim().isEmpty()) {
            logger.warn("Bank Z API request without API key from IP: {} to path: {}", 
                       getClientIpAddress(request), requestPath);
            sendUnauthorizedResponse(response, "API key is required for Bank Z endpoints");
            return;
        }
        
        if (!validApiKey.equals(providedApiKey)) {
            logger.warn("Bank Z API request with invalid API key from IP: {} to path: {}", 
                       getClientIpAddress(request), requestPath);
            sendUnauthorizedResponse(response, "Invalid API key");
            return;
        }
        
        logger.info("Bank Z API request authenticated successfully from IP: {} to path: {}", 
                   getClientIpAddress(request), requestPath);
        
        // API key is valid, continue with the request
        filterChain.doFilter(request, response);
    }
    
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format(
            "{\"success\":false,\"message\":\"%s\",\"data\":null,\"timestamp\":\"%s\"}", 
            message, java.time.LocalDateTime.now()));
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}