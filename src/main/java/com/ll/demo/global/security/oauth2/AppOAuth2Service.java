package com.ll.demo.global.security.oauth2;

import com.ll.demo.AppConfig;
import com.ll.demo.domain.member.member.entity.Member;
import com.ll.demo.domain.member.member.service.MemberService;
import com.ll.demo.domain.member.member.type.MemberProvider;
import com.ll.demo.global.exceptions.GlobalException;
import com.ll.demo.global.security.AuthTokenService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

// 앱(RN) 소셜 인증: 시스템 브라우저 + 일회용 코드 + PKCE(S256)
// 코드·verifier·토큰은 로그에 남기지 않음
@Service
@RequiredArgsConstructor
public class AppOAuth2Service {

    public static final String CLIENT_APP = "app";

    // 세션 속성: 앱 인증 진행 중 표시와 복귀 정보
    public static final String APP_CLIENT_ATTRIBUTE = "OAUTH2_APP_CLIENT";
    public static final String APP_REDIRECT_URI_ATTRIBUTE = "OAUTH2_APP_REDIRECT_URI";
    public static final String APP_CODE_CHALLENGE_ATTRIBUTE = "OAUTH2_APP_CODE_CHALLENGE";
    public static final String APP_LINK_ATTRIBUTE = "OAUTH2_APP_LINK";

    public static final String INVALID_CODE = "400-30";
    public static final String INVALID_CODE_MESSAGE = "유효하지 않은 인증 코드입니다.";
    public static final String INVALID_APP_REQUEST = "400-31";
    public static final String INVALID_APP_REQUEST_MESSAGE = "허용되지 않은 앱 인증 요청입니다.";

    public static final String INVALID_LINK_TICKET = "400-32";
    public static final String INVALID_LINK_TICKET_MESSAGE = "유효하지 않은 연동 요청입니다.";
    public static final String LINK_CONFLICT = "409-2";

    public static final int LOGIN_CODE_TTL_SEC = 60;
    public static final int LINK_TICKET_TTL_SEC = 60 * 5;
    public static final int LINK_RESULT_TTL_SEC = 60;

    private static final String CHALLENGE_METHOD_S256 = "S256";
    private static final Pattern CODE_CHALLENGE = Pattern.compile("^[A-Za-z0-9_-]{43,128}$");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OAuth2ExchangeCodeRepository exchangeCodeRepository;
    private final MemberService memberService;
    private final AuthTokenService authTokenService;

    // 쉼표 구분, 정확히 일치. 비어 있으면 앱 인증 요청은 모두 거절
    @Value("${custom.oauth2.app-redirect-uris:}")
    private String appRedirectUris;

    public record AppSession(String redirectUri, String codeChallenge, boolean link) {
    }

    public record LoginExchangeResult(Member member, boolean newUser, String accessToken, String refreshToken) {
    }

    public boolean isAllowedRedirectUri(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank() || appRedirectUris == null) {
            return false;
        }
        return Arrays.stream(appRedirectUris.split(","))
                .map(String::trim)
                .filter(uri -> !uri.isEmpty())
                .anyMatch(redirectUri::equals);
    }

    public static boolean isValidCodeChallenge(String codeChallenge, String codeChallengeMethod) {
        return CHALLENGE_METHOD_S256.equals(codeChallengeMethod)
                && codeChallenge != null
                && CODE_CHALLENGE.matcher(codeChallenge).matches();
    }

    public boolean isValidAppRequest(String redirectUri, String codeChallenge, String codeChallengeMethod) {
        return isAllowedRedirectUri(redirectUri) && isValidCodeChallenge(codeChallenge, codeChallengeMethod);
    }

    public static void startAppSession(HttpSession session, String redirectUri, String codeChallenge, boolean link) {
        clearAppSession(session);
        session.setAttribute(APP_CLIENT_ATTRIBUTE, CLIENT_APP);
        session.setAttribute(APP_REDIRECT_URI_ATTRIBUTE, redirectUri);
        session.setAttribute(APP_CODE_CHALLENGE_ATTRIBUTE, codeChallenge);
        if (link) {
            session.setAttribute(APP_LINK_ATTRIBUTE, Boolean.TRUE);
        }
    }

    public static boolean isAppLinkSession(HttpSession session) {
        return session != null && session.getAttribute(APP_LINK_ATTRIBUTE) != null;
    }

    public static void clearAppSession(HttpSession session) {
        if (session == null) {
            return;
        }
        session.removeAttribute(APP_CLIENT_ATTRIBUTE);
        session.removeAttribute(APP_REDIRECT_URI_ATTRIBUTE);
        session.removeAttribute(APP_CODE_CHALLENGE_ATTRIBUTE);
        session.removeAttribute(APP_LINK_ATTRIBUTE);
    }

    // 앱 인증 세션이면 읽고 지움 (사용 후 제거). 아니면 null
    public static AppSession takeAppSession(HttpSession session) {
        if (session == null || !CLIENT_APP.equals(session.getAttribute(APP_CLIENT_ATTRIBUTE))) {
            return null;
        }
        Object redirectUri = session.getAttribute(APP_REDIRECT_URI_ATTRIBUTE);
        Object codeChallenge = session.getAttribute(APP_CODE_CHALLENGE_ATTRIBUTE);
        boolean link = session.getAttribute(APP_LINK_ATTRIBUTE) != null;
        clearAppSession(session);
        if (!(redirectUri instanceof String uri) || !(codeChallenge instanceof String challenge)) {
            return null;
        }
        return new AppSession(uri, challenge, link);
    }

    @Transactional
    public String issueLoginCode(Long memberId, boolean newUser, String codeChallenge) {
        return issue(OAuth2ExchangeCode.builder()
                .memberId(memberId)
                .newUser(newUser)
                .codeChallenge(codeChallenge)
                .purpose(OAuth2ExchangeCode.Purpose.LOGIN), LOGIN_CODE_TTL_SEC);
    }

    // 교환: PKCE 확인 후 원자적으로 사용 처리하고 토큰 발급
    @Transactional
    public LoginExchangeResult exchangeLoginCode(String code, String codeVerifier) {
        OAuth2ExchangeCode exchangeCode = consume(OAuth2ExchangeCode.Purpose.LOGIN, code, codeVerifier, null);
        Member member = memberService.findById(exchangeCode.getMemberId()).orElseThrow(AppOAuth2Service::invalidCode);

        String accessToken = authTokenService.genToken(member, AppConfig.getAccessTokenExpirationSec());
        String refreshToken = memberService.genRefreshToken(member);
        return new LoginExchangeResult(member, exchangeCode.isNewUser(), accessToken, refreshToken);
    }

    // 앱 연동 시작: 브라우저로 넘길 1회용 연동 티켓 (5분)
    @Transactional
    public String issueLinkTicket(Long memberId, String provider, String redirectUri, String codeChallenge) {
        return issue(OAuth2ExchangeCode.builder()
                .memberId(memberId)
                .codeChallenge(codeChallenge)
                .purpose(OAuth2ExchangeCode.Purpose.LINK_TICKET)
                .redirectUri(redirectUri)
                .provider(provider), LINK_TICKET_TTL_SEC);
    }

    // 브라우저 진입: 티켓을 원자적으로 사용 처리 (provider가 다르면 무효)
    @Transactional
    public OAuth2ExchangeCode consumeLinkTicket(String ticket, String provider) {
        if (ticket == null || ticket.isBlank()) {
            throw invalidLinkTicket();
        }
        String ticketHash = sha256Hex(ticket);
        OAuth2ExchangeCode linkTicket = exchangeCodeRepository.findByCodeHashAndPurpose(ticketHash, OAuth2ExchangeCode.Purpose.LINK_TICKET)
                .orElseThrow(AppOAuth2Service::invalidLinkTicket);
        if (provider != null && !provider.equals(linkTicket.getProvider())) {
            throw invalidLinkTicket();
        }
        if (exchangeCodeRepository.markUsed(ticketHash, OAuth2ExchangeCode.Purpose.LINK_TICKET, LocalDateTime.now()) != 1) {
            throw invalidLinkTicket();
        }
        return linkTicket;
    }

    // 소셜 인증 성공: 아직 연결하지 않고 확정용 코드만 발급 (60초)
    @Transactional
    public String issueLinkResult(Long memberId, String provider, String providerId, String codeChallenge) {
        return issue(OAuth2ExchangeCode.builder()
                .memberId(memberId)
                .codeChallenge(codeChallenge)
                .purpose(OAuth2ExchangeCode.Purpose.LINK_RESULT)
                .provider(provider)
                .providerId(providerId), LINK_RESULT_TTL_SEC);
    }

    // 확정: PKCE·현재 회원 일치·1회용을 확인한 뒤 연결 (중복 제한은 저장 시점에 다시 검사)
    @Transactional
    public Member confirmLink(Long memberId, String code, String codeVerifier) {
        OAuth2ExchangeCode linkResult = consume(OAuth2ExchangeCode.Purpose.LINK_RESULT, code, codeVerifier, memberId);
        MemberProvider provider = MemberProvider.valueOf(linkResult.getProvider().toUpperCase(Locale.ROOT));
        try {
            return memberService.linkSocialAccount(memberId, provider, linkResult.getProviderId());
        } catch (OAuth2AuthenticationException e) {
            throw new GlobalException(LINK_CONFLICT, e.getMessage());
        }
    }

    // 만료되거나 사용된 코드 정리 (하루 1회)
    @Scheduled(cron = "0 30 4 * * *")
    @Transactional
    public void deleteExpiredCodes() {
        exchangeCodeRepository.deleteExpiredOrUsed(LocalDateTime.now());
    }

    private String issue(OAuth2ExchangeCode.OAuth2ExchangeCodeBuilder builder, int ttlSec) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        exchangeCodeRepository.save(builder
                .codeHash(sha256Hex(code))
                .expiresAt(LocalDateTime.now().plusSeconds(ttlSec))
                .build());
        return code;
    }

    // 실패 사유(없음·만료·재사용·verifier 불일치·회원 불일치)는 드러내지 않고 같은 오류로 응답
    private OAuth2ExchangeCode consume(OAuth2ExchangeCode.Purpose purpose, String code, String codeVerifier, Long expectedMemberId) {
        if (code == null || code.isBlank() || codeVerifier == null || codeVerifier.isBlank()) {
            throw invalidCode();
        }
        String codeHash = sha256Hex(code);
        OAuth2ExchangeCode exchangeCode = exchangeCodeRepository.findByCodeHashAndPurpose(codeHash, purpose)
                .orElseThrow(AppOAuth2Service::invalidCode);
        if (!matchesChallenge(codeVerifier, exchangeCode.getCodeChallenge())) {
            throw invalidCode();
        }
        if (expectedMemberId != null && !expectedMemberId.equals(exchangeCode.getMemberId())) {
            throw invalidCode();
        }
        if (exchangeCodeRepository.markUsed(codeHash, purpose, LocalDateTime.now()) != 1) {
            throw invalidCode();
        }
        return exchangeCode;
    }

    private static GlobalException invalidLinkTicket() {
        return new GlobalException(INVALID_LINK_TICKET, INVALID_LINK_TICKET_MESSAGE);
    }

    private static GlobalException invalidCode() {
        return new GlobalException(INVALID_CODE, INVALID_CODE_MESSAGE);
    }

    static boolean matchesChallenge(String codeVerifier, String codeChallenge) {
        String computed = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(sha256(codeVerifier.getBytes(StandardCharsets.US_ASCII)));
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.US_ASCII),
                codeChallenge.getBytes(StandardCharsets.US_ASCII));
    }

    static String sha256Hex(String value) {
        return HexFormat.of().formatHex(sha256(value.getBytes(StandardCharsets.UTF_8)));
    }

    private static byte[] sha256(byte[] value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
