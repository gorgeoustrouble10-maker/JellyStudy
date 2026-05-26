package com.jellystudy.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JellystudyBearerWriteAuthFilterTest {

    private BearerTokenResolver tokenResolver;
    private JellystudyAuthProperties authProperties;
    private JellystudyBearerWriteAuthFilter filter;

    @BeforeEach
    void setUp() {
        tokenResolver = mock(BearerTokenResolver.class);
        authProperties = new JellystudyAuthProperties();
        authProperties.setBearerWriteEnabled(true);
        filter = new JellystudyBearerWriteAuthFilter(tokenResolver, authProperties, new ObjectMapper());
    }

    @Test
    void getQuestionsDoesNotRequireAuth() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/questions");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void postAnswerRequiresBearerToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/answers");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(tokenResolver.resolveUsername(anyString())).thenReturn(Optional.empty());

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
    }

    @Test
    void postAnswerWithValidTokenSetsUserAttribute() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/answers");
        request.addHeader("Authorization", "Bearer abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        when(tokenResolver.resolveUsername("abc123")).thenReturn(Optional.of("32308117"));

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertEquals("32308117", request.getAttribute(JellystudyUserAttributes.USER_ID));
    }

    @Test
    void evaluateEndpointIsNotProtected() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/evaluations/run");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
