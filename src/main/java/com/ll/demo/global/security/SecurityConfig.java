package com.ll.demo.global.security;

import com.ll.demo.global.rsData.RsData;
import com.ll.demo.global.security.oauth2.OAuth2FailureHandler;
import com.ll.demo.global.security.oauth2.OAuth2MemberService;
import com.ll.demo.global.security.oauth2.OAuth2SuccessHandler;
import com.ll.demo.standard.dto.util.Ut;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.ForwardedHeaderFilter;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final CustomAuthenticationFilter customAuthenticationFilter;
    private final OAuth2MemberService oAuth2MemberService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    @Value("${custom.frontend.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private String allowedFrontendOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/h2-console/**", "/actuator/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                        .requestMatchers("/api/auth/refresh").permitAll()
                        .requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**").permitAll()
                        .requestMatchers("/api/auth/logout").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(formLogin -> formLogin.permitAll())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                        .sessionFixation(sessionFixation -> sessionFixation.none())
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(403);
                            response.getWriter().write(
                                    Ut.json.toString(
                                            RsData.of("403-1", request.getRequestURI() + ", " + authException.getLocalizedMessage())
                                    )
                            );
                        })
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2MemberService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler)
                )
                .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        for (String origin : allowedFrontendOrigins.split(",")) {
            String trimmed = origin.trim();
            if (!trimmed.isEmpty()) {
                configuration.addAllowedOrigin(trimmed);
            }
        }

        configuration.addAllowedOriginPattern("http://localhost:*");
        configuration.addAllowedOriginPattern("https://localhost:*");
        configuration.addAllowedOrigin("https://quoteme.shop");
        configuration.addAllowedOrigin("https://quote--me.vercel.app");
        configuration.addAllowedOrigin("https://www.quoteme.site");
        configuration.addAllowedOrigin("http://3.38.96.140:8070");

        configuration.addAllowedMethod("GET");
        configuration.addAllowedMethod("POST");
        configuration.addAllowedMethod("PUT");
        configuration.addAllowedMethod("PATCH");
        configuration.addAllowedMethod("DELETE");
        configuration.addAllowedMethod("OPTIONS");

        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public org.springframework.web.filter.OncePerRequestFilter cookieSameSiteFilter() {
        return new org.springframework.web.filter.OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(
                    jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response,
                    jakarta.servlet.FilterChain filterChain) throws jakarta.servlet.ServletException, java.io.IOException {

                filterChain.doFilter(request, response);

                java.util.Collection<String> headers = response.getHeaders("Set-Cookie");
                boolean first = true;
                for (String header : headers) {
                    if (header != null && header.contains("JSESSIONID")) {
                        if (!header.contains("SameSite")) {
                            header += "; SameSite=None";
                        }
                        if (!header.contains("Secure")) {
                            header += "; Secure";
                        }
                        if (first) {
                            response.setHeader("Set-Cookie", header);
                            first = false;
                        } else {
                            response.addHeader("Set-Cookie", header);
                        }
                    }
                }
            }
        };
    }

    @Bean
    public ForwardedHeaderFilter forwardedHeaderFilter() {
        return new ForwardedHeaderFilter();
    }
}
