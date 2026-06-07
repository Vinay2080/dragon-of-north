package org.miniProjectTwo.DragonOfNorth.security.filter;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CsrfCookieFilterTest {

    private final CsrfCookieFilter filter = new CsrfCookieFilter();

    @Test
    void doFilterInternal_shouldCallGetToken_whenCsrfTokenAttributePresent() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        CsrfToken csrfToken = mock(CsrfToken.class);
        request.setAttribute(CsrfToken.class.getName(), csrfToken);

        filter.doFilterInternal(request, response, chain);

        verify(csrfToken).getToken();
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_shouldNotFail_whenNoCsrfTokenAttribute() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
