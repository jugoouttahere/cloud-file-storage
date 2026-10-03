package ru.rostislav.cloudfilestorage.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

public class WithMockUserDetailsSecurityContextFactory implements WithSecurityContextFactory<WithMockUserDetails> {
    @Override
    public SecurityContext createSecurityContext(WithMockUserDetails annotation) {
        UserDetailsImpl userDetails = new UserDetailsImpl(1L, "test", "password");

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails,
                userDetails.getPassword(),
                userDetails.getAuthorities()
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        return context;
    }
}
