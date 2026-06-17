package com.example.eventfinder.web;

import com.example.eventfinder.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class AdminAuthInterceptor implements HandlerInterceptor {
    
    private final JwtTokenProvider jwtTokenProvider;

    public AdminAuthInterceptor(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

   @Override
public boolean preHandle(HttpServletRequest request,
                         HttpServletResponse response,
                         Object handler) throws Exception {

    // IMPORTANT: allow preflight requests
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
        return true;
    }

    String authHeader = request.getHeader("Authorization");

    if (!jwtTokenProvider.validateHeader(authHeader)) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    return true;
}
}
