package com.rikkeibank.gateway.filter;

import com.rikkeibank.gateway.util.JwtUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@Component
@Slf4j
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final JwtUtil jwtUtil;
    private final ReactiveStringRedisTemplate redisTemplate;

    public AuthenticationFilter(JwtUtil jwtUtil, ReactiveStringRedisTemplate redisTemplate) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.redisTemplate = redisTemplate;
    }

    @Getter
    @Setter
    public static class Config {
        private boolean skipAuth = false;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // Skip public auth endpoints
            if (config.isSkipAuth() || path.startsWith("/api/v1/auth/")) {
                return chain.filter(exchange);
            }

            // Check Authorization header
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return onError(exchange, "Missing Authorization Header", HttpStatus.UNAUTHORIZED);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Invalid Authorization Header Format. Must start with Bearer ", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // Validate token signature and expiration
            if (!jwtUtil.validateToken(token)) {
                return onError(exchange, "JWT Token is invalid or has expired", HttpStatus.UNAUTHORIZED);
            }

            String userId = jwtUtil.extractUserId(token);
            String username = jwtUtil.extractUsername(token);
            List<String> roles = jwtUtil.extractRoles(token);
            String rolesStr = roles != null ? String.join(",", roles) : "";

            // Check token blacklist / force logout in Redis
            String blacklistTokenKey = "blacklist:token:" + token;
            String blacklistUserKey = "blacklist:user:" + userId;

            return redisTemplate.hasKey(blacklistTokenKey)
                    .flatMap(isTokenBlacklisted -> {
                        if (Boolean.TRUE.equals(isTokenBlacklisted)) {
                            log.warn("Access rejected: Token {} is in Redis blacklist (Revoked / Logged out)", token);
                            return onError(exchange, "Session has been revoked or logged out", HttpStatus.UNAUTHORIZED);
                        }
                        return redisTemplate.hasKey(blacklistUserKey)
                                .flatMap(isUserBlacklisted -> {
                                    if (Boolean.TRUE.equals(isUserBlacklisted)) {
                                        log.warn("Access rejected: User ID {} was forcibly logged out by Admin", userId);
                                        return onError(exchange, "Your account access has been revoked by Administrator", HttpStatus.UNAUTHORIZED);
                                    }

                                    // Mutate request with user headers for downstream services
                                    ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                                            .header("X-User-Id", userId)
                                            .header("X-User-Email", username)
                                            .header("X-User-Roles", rolesStr)
                                            .build();

                                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                                });
                    })
                    .onErrorResume(e -> {
                        log.error("Redis check failed during authentication filter: {}", e.getMessage());
                        // Fallback: if Redis is temporarily unreachable, proceed with JWT validation
                        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                                .header("X-User-Id", userId)
                                .header("X-User-Email", username)
                                .header("X-User-Roles", rolesStr)
                                .build();
                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    });
        };
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonError = String.format(
                "{\"timestamp\":\"%s\",\"status\":%d,\"error\":\"%s\",\"message\":\"%s\",\"path\":\"%s\"}",
                Instant.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                exchange.getRequest().getURI().getPath()
        );

        DataBuffer buffer = response.bufferFactory().wrap(jsonError.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
