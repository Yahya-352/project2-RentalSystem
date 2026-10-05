package com.ga.RentalSystem.security;

import com.ga.RentalSystem.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import java.util.Set;


@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final Integer rateLimit = 5;

    private final RateLimitService rateLimitService;

    private final Set<String> LIMITED_PATHS = Set.of(
            "/users/login",
            "/users/register",
            "/users/register/agency",
            "/users/forgot-password",
            "/users/reset-password");

    @Override
    public void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String ipAddress = request.getRemoteAddr();
        String key = ipAddress + ":" + path;

        if(!LIMITED_PATHS.contains(path)){
            filterChain.doFilter(request, response);
            return;
        }

        int tryAfter = rateLimitService.checkLimit(key , rateLimit , 60);

        if(tryAfter > 0){
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(tryAfter));
            response.setContentType("application/json");
            response.getWriter().write("{\"status\":429," +
                    "\"error\":\"TOO_MANY_REQUESTS\","
                    + "\"message\":\"Too many requests, try again in "
                    + tryAfter + " seconds\"}");
            return;
        }
        filterChain.doFilter(request, response);

    }

}
