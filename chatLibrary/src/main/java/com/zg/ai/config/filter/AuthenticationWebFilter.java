package com.zg.ai.config.filter;

import com.zg.ai.utils.JwtUtil;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthenticationWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        if (request.getMethod().name().equals("OPTIONS")) {
            return chain.filter(exchange);
        }

        if (path.startsWith("/user/login") ||
            path.startsWith("/user/register") || 
            path.startsWith("/v3/api-docs") || 
            path.startsWith("/webjars") ||
            path.startsWith("/swagger-ui")) {
            return chain.filter(exchange);
        }

        String token = null;
        
        // 1. Header
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        }

        // 2. Cookie
        if (token == null) {
            List<HttpCookie> cookies = request.getCookies().get("token");
            if (cookies != null && !cookies.isEmpty()) {
                token = cookies.get(0).getValue();
            }
        }

        if (token != null) {
            String userId = JwtUtil.validateTokenAndGetUserId(token);
            if (userId != null) {
                exchange.getAttributes().put("userId", userId);
                return chain.filter(exchange);
            }
        }

        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }
}