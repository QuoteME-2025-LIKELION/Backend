## 앱(RN) 인증 가이드

이 가이드가 앱 인증 정책의 원문입니다. `/oauth2/authorization/...`과 `/oauth2/link`는 컨트롤러가 아니라서 아래 API 목록에 나오지 않습니다.

### 1. 앱 소셜 로그인 (시스템 인증 브라우저 + 일회용 코드 + PKCE)
1. `code_verifier`를 만듭니다: 32바이트 이상 난수를 base64url(패딩 없음)로 인코딩한 43~128자 문자열.
2. `code_challenge = BASE64URL(SHA256(code_verifier))` (패딩 없음, 43자). 방식은 `S256`만 허용합니다.
3. **시스템 인증 브라우저**(iOS `ASWebAuthenticationSession`, Android Custom Tabs)로 엽니다. WebView는 쓰지 않습니다.
   `GET /oauth2/authorization/{google|kakao}?mode=login|signup&client=app&redirect_uri=<앱 URI>&code_challenge=<challenge>&code_challenge_method=S256`
   - `redirect_uri`는 서버 허용 목록과 **정확히 일치**해야 합니다. 검사에 실패하면 리다이렉트 없이 400(`400-31`)입니다.
4. 앱 복귀
   - 성공: `<redirect_uri>?code=<교환 코드>`
   - 실패: `<redirect_uri>?reason=<사유>` (`account_not_found`, `email_not_verified`, `already_linked`, `provider_already_linked`, 그 밖은 `oauth2`)
5. 교환: `POST /api/auth/oauth2/token` 본문 `{code, codeVerifier}`, 인증 없음
   - 응답 `data`: `{accessToken, refreshToken, isNewUser, profileCompleted, item}`, 쿠키 없음
   - 교환 코드는 **60초, 1회용**입니다.

### 2. `X-Client-Type: app` 헤더
- 이 헤더가 있으면 refresh 토큰을 **응답 본문으로 받고 요청 본문으로 보내며**, 쿠키를 설정하지 않습니다. 없으면 웹(쿠키) 동작입니다.
- 적용 API: `POST /api/auth/login`, `POST /api/auth/signup`, `POST /api/auth/guest-login`(응답에 `refreshToken` 추가), `POST /api/auth/refresh`·`POST /api/auth/logout`(본문 `{refreshToken}`).
- 교환 API(`/api/auth/oauth2/token`)는 헤더와 관계없이 본문으로만 토큰을 줍니다.

### 3. 토큰 정책
- access 토큰 5분, refresh 토큰 30일, refresh 토큰은 **회전하지 않습니다**(재발급은 access만 새로 줍니다).
- refresh 토큰은 **회원당 1개**입니다. 새로 로그인하면(웹·앱 포함) 이전 refresh 토큰은 무효입니다.
- `Authorization: Bearer`에는 **accessToken만** 보냅니다. refresh 토큰을 Bearer로 보내면 `401-5`입니다.

### 4. 인증 실패 기준
| 상태 | resultCode / message 접두 | 의미 | 앱 처리 |
|---|---|---|---|
| 401 | resultCode `401-4` | 토큰 없음 | 로그인 화면 |
| 401 | resultCode `401-5` | 토큰 무효·만료·용도 불일치, 탈퇴 회원 | `POST /api/auth/refresh` 후 재시도, 재발급 실패 시 로그인 화면 |
| 401 | message `401-2` / `401-3` | 재발급 시 refresh 없음 / 무효 | 저장한 토큰 삭제 후 로그인 화면 |
| 403 | | 권한 없음(게스트 제한, 남의 리소스) | 재발급하지 말고 안내 |

### 5. 앱 계정 연동
1. `POST /api/profile/account/link/{provider}/start` (Bearer) 본문 `{redirectUri, codeChallenge, codeChallengeMethod: "S256"}` → `{authorizationUrl}`
2. `authorizationUrl`을 시스템 인증 브라우저로 엽니다. 연동 URL은 **5분, 1회용**입니다.
3. 복귀: 성공 `<redirectUri>?code=<코드>&linked=<provider>`, 실패 `<redirectUri>?reason=<사유>`
4. `POST /api/profile/account/link/confirm` (Bearer) 본문 `{code, codeVerifier}` → `{linkedAccounts}`. **확정 API를 호출해야 연결이 완료됩니다.**
   - 이유: 연동 URL이 유출되어도 PKCE verifier와 본인 토큰이 없으면 남의 소셜 계정이 연결되지 않게 하기 위함입니다.

### 6. `profileCompleted`
- `PUT /api/profile/setup`(필수 `nickname`, `birthYear`, 선택 `introduction`) 저장에 성공해야만 true가 됩니다.
  - 이유: 소셜 가입 때 닉네임이 자동으로 채워지므로, 기존 수정 API로는 사용자가 직접 설정했는지 알 수 없습니다.
- 기존 회원과 게스트는 true, 새로 가입한 회원(소셜·이메일)은 false로 시작합니다. false면 첫 프로필 설정 화면으로 보냅니다.

### 7. `isAiGenerated`
- 명언의 `summary`가 비어 있지 않으면(null·빈 문자열·공백 제외) true입니다.
- 명언을 게시할 때는 사용자가 AI 추천을 선택한 경우에만 `summary`를 보냅니다.

### 8. 앱 인증 오류 코드 (GlobalException 형식: `resultCode`는 앞 3자리, `message`가 코드로 시작)
- `400-30`: 교환·확정 코드가 없음·만료·재사용·verifier 불일치·다른 회원 (사유는 구분하지 않음)
- `400-31`: 앱 요청 검증 실패(복귀 URI 미허용, PKCE 누락·형식 오류, `S256` 아님)
- `400-32`: 연동 URL(티켓) 무효·만료·재사용
- `409-2`: 확정 시점에 소셜 계정이 이미 다른 회원 또는 같은 서비스의 다른 계정에 연결됨
