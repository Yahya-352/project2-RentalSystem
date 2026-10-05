package com.ga.RentalSystem.security;

import com.ga.RentalSystem.service.NotificationService;
import com.ga.RentalSystem.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final Integer rateLimit = 5;

    private final RateLimitService rateLimitService;

    @Override
    public void doFilterInternal(HttpServletRequest request,
                                 HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        String ipAddress = request.getRemoteAddr();
        String key = ipAddress + ":" + path;

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
