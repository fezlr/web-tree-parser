package com.fezlr.web_tree_parser.auth.security;

import com.fezlr.web_tree_parser.user.entity.Role;
import com.fezlr.web_tree_parser.user.entity.UserEntity;
import com.fezlr.web_tree_parser.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Optional;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserFilterStaticResourcesTest {
    @Mock
    private UserService userService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/css/style.css", "/js/app.js", "/images/logo.png", "/favicon.ico"})
    void shouldNotQueryUser_whenStaticResourcesAreRequested(String path) throws Exception {
        authenticatedUser();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockFilterChain chain = new MockFilterChain();

        new CurrentUserFilter(userService).doFilter(request, new MockHttpServletResponse(), chain);

        verifyNoInteractions(userService);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/history", "/results/1", "/admin/users", "/cssx/style.css"})
    void shouldQueryUser_whenOtherPathsAreRequested(String path) throws Exception {
        UserEntity user = authenticatedUser();
        when(userService.findById(1L)).thenReturn(Optional.of(user));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        MockFilterChain chain = new MockFilterChain();

        new CurrentUserFilter(userService).doFilter(request, new MockHttpServletResponse(), chain);

        verify(userService).findById(1L);
        assertThat(chain.getRequest()).isSameAs(request);
    }

    private UserEntity authenticatedUser() {
        UserEntity user = UserEntity.builder().username("alice").passwordHash("hash").role(Role.USER).build();
        ReflectionTestUtils.setField(user, "id", 1L);

        UserDetailsImpl principal = new UserDetailsImpl(user);
        SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));

        return user;
    }
}
