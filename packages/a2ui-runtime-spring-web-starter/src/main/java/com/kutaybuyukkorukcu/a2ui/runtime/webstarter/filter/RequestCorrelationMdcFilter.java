package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.filter;

import com.kutaybuyukkorukcu.a2ui.runtime.webstarter.service.RequestCorrelationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class RequestCorrelationMdcFilter extends OncePerRequestFilter {

    private final RequestCorrelationService requestCorrelationService;

    public RequestCorrelationMdcFilter(RequestCorrelationService requestCorrelationService) {
        this.requestCorrelationService = requestCorrelationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = requestCorrelationService.resolveRequestId(request.getHeader(RequestCorrelationService.REQUEST_ID_HEADER));
        try {
            MDC.put("a2uiRequestId", requestId);
            if (response != null) {
                response.setHeader(RequestCorrelationService.REQUEST_ID_HEADER, requestId);
            }
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("a2uiRequestId");
        }
    }
}