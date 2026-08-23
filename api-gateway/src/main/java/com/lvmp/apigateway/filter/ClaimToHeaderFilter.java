package com.lvmp.apigateway.filter;

import com.lvmp.apigateway.config.properties.ClaimHeaderProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ClaimToHeaderFilter extends OncePerRequestFilter implements Ordered {
    private final JwtDecoder jwtDecoder;
    private final ClaimHeaderProperties claimHeaderProperties;
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        MutableHeaderRequestWrapper wrapper = new MutableHeaderRequestWrapper(request);
        String authHeader = request.getHeader(AUTH_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        Jwt jwt = jwtDecoder.decode(authHeader.substring(BEARER_PREFIX.length()));

        claimHeaderProperties.getClaimHeaders().forEach((claim, header) -> {
            if (jwt.hasClaim(claim)) {
                wrapper.addHeader(header, jwt.getClaimAsString(claim));
            }
        });

        filterChain.doFilter(wrapper, response);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
