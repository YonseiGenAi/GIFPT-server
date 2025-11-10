package com.gifpt.security.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.gifpt.security.auth.service.JwtService;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;

  // Swagger, Health, Auth 등 인증 없이 통과시킬 경로 접두사
  private static final List<String> WHITELIST_PREFIXES = List.of(
      "/healthz",
      "/actuator/health",
      "/swagger-ui",          // /swagger-ui/index.html 등
      "/swagger-ui.html",
      "/v3/api-docs",         // /v3/api-docs, /v3/api-docs/swagger-config 등
      "/api/v1/auth",         // 로그인/회원가입
      "/api/v1/analysis"      // 분석 완료 콜백
  );

  public JwtAuthFilter(JwtService jwtService, UserDetailsService uds) {
    this.jwtService = jwtService;
    this.userDetailsService = uds;
  }

  /**
   * 특정 요청 URI가 화이트리스트에 해당되면 필터 자체를 건너뛴다.
   */
  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    String uri = request.getRequestURI();
    return WHITELIST_PREFIXES.stream().anyMatch(uri::startsWith);
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest req,
      @NonNull HttpServletResponse res,
      @NonNull FilterChain chain
  ) throws ServletException, IOException {

    String auth = req.getHeader("Authorization");
    if (auth != null && auth.startsWith("Bearer ")) {
      String token = auth.substring(7);
      try {
        String username = jwtService.extractUsername(token);
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
          UserDetails ud = userDetailsService.loadUserByUsername(username);
          if (jwtService.isValid(token, ud.getUsername())) {
            var authToken = new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
            SecurityContextHolder.getContext().setAuthentication(authToken);
          }
        }
      } catch (Exception ignored) { }
    }

    chain.doFilter(req, res);
  }
}
