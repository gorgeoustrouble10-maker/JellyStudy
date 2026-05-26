package com.jellystudy.common.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ApiKeyAuthFilterTest {

    @Test
    void acceptsValidApiKey() throws Exception {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("secret-key");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(ApiKeyAuthFilter.API_KEY_HEADER, "secret-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsMissingApiKey() throws Exception {
        ApiKeyAuthFilter filter = new ApiKeyAuthFilter("secret-key");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
    }
}
