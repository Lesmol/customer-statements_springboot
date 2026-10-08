package com.lvmp.apigateway.filter;

import com.lvmp.apigateway.config.properties.ClaimHeaderProperties;
import com.lvmp.apigateway.security.DexSubjectDecoder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaimToHeaderFilter extends OncePerRequestFilter implements Ordered {
    private final JwtDecoder jwtDecoder;
    private final ClaimHeaderProperties claimHeaderProperties;
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String SUB_CLAIM = "sub";

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

        try {
            claimHeaderProperties.claimHeaders().forEach((claim, header) -> {
                if (!jwt.hasClaim(claim)) {
                    return;
                }
                String value = claim.equals(SUB_CLAIM)
                        ? DexSubjectDecoder.toUserId(jwt.getClaimAsString(SUB_CLAIM)).toString()
                        : jwt.getClaimAsString(claim);
                wrapper.addHeader(header, value);
            });
        } catch (IllegalArgumentException e) {
            log.warn("Could not retrieve user id from sub claim '{}'", jwt.getClaimAsString(SUB_CLAIM), e);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token subject");
            return;
        }

        filterChain.doFilter(wrapper, response);
    }

    @Override
    public int getOrder() {
        return 0;
    }
}