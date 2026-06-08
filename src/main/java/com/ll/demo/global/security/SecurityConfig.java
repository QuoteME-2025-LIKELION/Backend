package com.ll.demo.global.security;

import com.ll.demo.global.rsData.RsData;
import com.ll.demo.global.security.oauth2.OAuth2CookieAuthorizationRequestRepository;
import com.ll.demo.global.security.oauth2.OAuth2FailureHandler;
import com.ll.demo.global.security.oauth2.OAuth2MemberService;
import com.ll.demo.global.security.oauth2.OAuth2SuccessHandler;
import com.ll.demo.global.security.oauth2.OAuth2FailureHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
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
    private final OAuth2CookieAuthorizationRequestRepository cookieAuthorizationRequestRepository;

    private final org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrationRepository;

    private final HttpSessionOAuth2AuthorizationRequestRepository authorizationRequestRepository =
            new HttpSessionOAuth2AuthorizationRequestRepository();

    private org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver customAuthorizationRequestResolver(
            org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clientRegistrationRepository) {

        org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver resolver =
                new org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository, "/oauth2/authorization"
                );

        resolver.setAuthorizationRequestCustomizer(builder -> {
            // 빌더 객체에서 빌드된 중간 요청 결과물을 꺼내서 현재 어떤 클라이언트(kakao/google)인지 안전하게 판별합니다.
            org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest request = builder.build();
            String ngrokBase = "https://boastingly-unthirsty-kannon.ngrok-free.dev";

            if (request.getRedirectUri().contains("kakao")) {
                builder.redirectUri(ngrokBase + "/login/oauth2/code/kakao");
            } else if (request.getRedirectUri().contains("google")) {
                builder.redirectUri(ngrokBase + "/login/oauth2/code/google");
            }
        });

        return resolver;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. CORS 설정 적용
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 2. CSRF 비활성화 (REST API 환경)
            .csrf(csrf -> csrf.disable())
            
            // 3. H2 콘솔 iframe 사용을 위한 헤더 설정
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            )
            
            // 4. 요청별 접근 권한 통제 (화이트리스트 지정)
            .authorizeHttpRequests(auth -> auth
                // 비로그인 허용 화이트리스트
                .requestMatchers("/", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/h2-console/**", "/actuator/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/**").permitAll()
                .requestMatchers("/api/auth/refresh").permitAll()
                .requestMatchers("/oauth2/authorization/**").permitAll()
                .requestMatchers("/api/auth/logout").permitAll()
                
                // 이하 모든 /api/** 요청 및 기타 요청은 인증 필수
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            
            // 5. 폼 로그인 기본 허용
            .formLogin(formLogin -> formLogin.permitAll())
            
            // 6. 예외 핸들러 지정 (인증 실패 시 403-1 커스텀 JSON 응답)
            .exceptionHandling(exceptionHandling -> exceptionHandling
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setContentType("application/json;charset=UTF-8");
                    response.setStatus(403);
                    response.getWriter().write(
                        Ut.json.toString(
                            RsData.of("403-1", "/api/settings/profile, Full authentication is required to access this resource")
                        )
                .headers(
                        headers ->
                                headers.frameOptions(
                                        frameOptions ->
                                                frameOptions.sameOrigin()
                                )
                )

                .csrf(
                        csrf ->
                                csrf.disable() // REST API 사용을 위해 CSRF 비활성화
                )
                .formLogin(
                        formLogin ->
                                formLogin
                                        .permitAll()
                )

                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
                        .sessionFixation(sessionFixation -> sessionFixation.none()) // 세션 고정 보호 임시 비활성화
                )

                .exceptionHandling(
                        exceptionHandling -> exceptionHandling
                                .authenticationEntryPoint(
                                        (request, response, authException) -> {
                                            response.setContentType("application/json;charset=UTF-8");
                                            response.setStatus(403);
                                            response.getWriter().write(
                                                    Ut.json.toString(
                                                            RsData.of("403-1", request.getRequestURI() + ", " + authException.getLocalizedMessage())
                                                    )
                                            );
                                        }
                                )
                )
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(authorization -> authorization
                                .authorizationRequestRepository(authorizationRequestRepository)
                                // 수정: 주입받은 필드 객체를 인자로 전달
                                .authorizationRequestResolver(customAuthorizationRequestResolver(clientRegistrationRepository))
                        )
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

        // 크로스 도메인 허용 주소 명세
        configuration.addAllowedOrigin("http://localhost:5173");
        configuration.addAllowedOrigin("https://quoteme.shop");
        configuration.addAllowedOrigin("https://quote--me.vercel.app");
        configuration.addAllowedOrigin("https://www.quoteme.site");
        configuration.addAllowedOrigin("http://3.38.96.140:8070");

        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true); // 크로스 도메인 간 쿠키 연동 허용

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

                // 응답 헤더의 Set-Cookie를 찾아 SameSite=None; Secure 속성을 강제로 보완
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

