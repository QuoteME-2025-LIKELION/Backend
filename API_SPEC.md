# QUOTEME API 명세서

> 기준: 실제 구현된 컨트롤러 코드 기반 (2026-03-06)
> 인증: JWT AccessToken (HttpOnly Cookie `accessToken`)
> 인증 필요 여부: 별도 표기 없으면 **인증 필요** / `인증 불필요` 명시된 경우만 예외

---

## 1. 인증 (Auth)

Base Path: `/api/auth`

| UI 페이지 | 세부 기능 | 메소드 | API Path | 요청 Body | 응답 | 참고 |
|-----------|-----------|--------|----------|-----------|------|------|
| 회원가입 | 회원가입 | `POST` | `/api/auth/signup` | `{ email, password, birthYear }` | `{ resultCode, data: { item: MemberDto, accessToken } }` | 인증 불필요. accessToken + refreshToken 쿠키 세팅 |
| 로그인 | 이메일 로그인 | `POST` | `/api/auth/login` | `{ email, password }` | `{ resultCode, data: { item: MemberDto, accessToken } }` | 인증 불필요. accessToken + refreshToken 쿠키 세팅 |
| 로그인 | 게스트 로그인 | `POST` | `/api/auth/guest-login` | 없음 | `{ resultCode, data: { item: MemberDto, accessToken } }` | 인증 불필요. 더미 데이터 포함 게스트 계정 자동 생성/재사용 |
| 공통 | 토큰 재발급 | `POST` | `/api/auth/refresh` | 없음 (쿠키의 refreshToken 사용) | `{ resultCode, data: { item: MemberDto, accessToken } }` | 인증 불필요. 새 accessToken 쿠키 세팅 |
| 공통 | 로그아웃 | `POST` | `/api/auth/logout` | 없음 | `{ resultCode }` | accessToken, refreshToken 쿠키 삭제, 서버의 refresh 토큰 무효화 (access 토큰이 만료돼도 refresh 쿠키로 동작). 이후 같은 refresh 토큰은 `401-3` |

### MemberDto
```json
{ "id": Long, "email": String, "nickname": String, "birthYear": String, "createDate": DateTime }
```

---

## 2. 프로필 (Profile)

Base Path: `/api/profile`

| UI 페이지 | 세부 기능 | 메소드 | API Path | 요청 파라미터/Body | 응답 | 참고 |
|-----------|-----------|--------|----------|-------------------|------|------|
| 내 프로필 | 내 프로필 조회 | `GET` | `/api/profile` | 없음 | `ProfileResponse` | |
| 내 프로필 | 프로필 수정 | `PUT` | `/api/profile` | `multipart/form-data`<br>- `data` (JSON): `{ nickname?, introduction?, profileImage? }`<br>- `image` (file, optional): 프로필 이미지 | `ProfileResponse` | 이미지 업로드 시 S3 저장. JSON `profileImage`는 우리 버킷 URL(`https://{bucket}.s3.ap-northeast-2.amazonaws.com/profile/...`) 또는 빈 문자열(이미지 제거)만 허용 (위반 시 400), 변경하지 않으면 생략. 닉네임 10자·자기소개 20자(코드포인트 기준), 이미지는 jpeg·png·webp·gif·heic만 5MB까지 (초과 413), 게스트 403. 닉네임은 다른 회원과 같아도 된다 (중복 허용) |
| 내 프로필 | 계정 정보 조회 | `GET` | `/api/profile/account` | 없음 | `AccountInfoResponse` | 성별, 생년월일, 소셜 연동 상태 |
| 내 프로필 | 계정 정보 수정 | `PUT` | `/api/profile/account` | `{ email, birthYear, gender }` | `String` ("계정 정보가 수정되었습니다.") | gender: `MALE` \| `FEMALE`, birthYear: 숫자 4자리, 1900~현재 연도 (위반 시 400), 사용 중인 이메일 409. 게스트 403 |
| 내 프로필 | 소셜 계정 연동 시작 | `GET` | `/api/profile/account/link/{provider}` | Path: `provider` (`google` \| `kakao`) | 302 → `/oauth2/authorization/{provider}` | XHR이 아닌 브라우저 이동으로 호출 (쿠키 인증). 잘못된 provider 400, 게스트 403. 성공하면 `redirect-uri?linked={provider}`로 이동하고 토큰은 새로 발급하지 않음. 실패 reason: `already_linked`, `provider_already_linked`, `account_not_found` |
| 내 프로필 | 회원 탈퇴 | `DELETE` | `/api/profile/account` | 없음 | `String` ("회원 탈퇴가 완료되었습니다.") | 탈퇴자가 받은/보낸 알림과 알림 설정도 함께 삭제, 응답에서 accessToken, refreshToken 쿠키 만료. 탈퇴 후 기존 토큰은 미인증(403) 처리 |
| 타인 프로필 | 다른 회원 프로필 조회 | `GET` | `/api/profile/{id}` | Path: `id` (회원 ID) | `MemberResponse` | |

### ProfileResponse
```json
{ "id": Long, "email": String, "nickname": String, "introduction": String, "profileImage": String, "quoteCount": Long, "friendCount": Long }
```

### AccountInfoResponse
```json
{ "email": String, "birthYear": String, "gender": "MALE"|"FEMALE", "provider": String, "providerId": String, "linkedAccounts": [{ "provider": "GOOGLE"|"KAKAO", "linkedAt": DateTime }] }
```

### MemberResponse
```json
{ "id": Long, "nickname": String, "profileImage": String, "introduction": String }
```

---

## 3. 명언 (Quote)

Base Path: `/api/quotes`

| UI 페이지 | 세부 기능 | 메소드 | API Path | 요청 파라미터/Body | 응답 | 참고 |
|-----------|-----------|--------|----------|-------------------|------|------|
| 홈/피드 | 피드 명언 목록 조회 | `GET` | `/api/quotes` | Query: `date` (required, `yyyy-MM-dd`), `groupId` (optional) | `QuoteListDto` | groupId 없으면 전체 친구 피드, 있으면 해당 그룹 멤버 피드; `unwrittenMembers` 필드 추가 |
| 명언 작성 | 명언 작성 | `POST` | `/api/quotes` | `{ content, originalContent?, summary?, taggedMemberIds?: [Long] }` | `QuoteResponse` | HTTP 201. 내용 30자(코드포인트 기준). `taggedMemberIds`는 작성자의 수락된 친구만 가능, 본인 제외 (위반 시 400). 이모지·숫자·특수문자만으로도 작성 가능, 한글 초성만 있는 글은 400 |
| 명언 작성 | AI 요약 (일기→명언) | `POST` | `/api/quotes/summarize` | `{ content }` | `{ "summaries": [String] }` | 하루 1회 제한 (서버 설정 `custom.ai.daily-limit`, 초과 시 429), 최대 3개, 항목당 30자 이하. 추천이 1개 이상일 때만 횟수 차감 (0개면 빈 목록 200, 차감 없음). 일반 문자(유니코드 글자, 한글 자음·모음만은 제외)가 없으면 400 (`400-7`), 15자 이하면 400 (`400-4`). 입력 검사는 횟수 확인·Gemini 호출보다 먼저 하며 거부 시 횟수를 차감하지 않음 |
| 명언 작성 | AI 사용량 조회 | `GET` | `/api/quotes/ai-usage` | 없음 | `{ "used": Integer, "remaining": Integer, "limit": Integer }` | 오늘 AI 사용 횟수 조회. `limit`은 1 (KST 자정에 초기화) |
| 피드 | 좋아요 등록 | `POST` | `/api/quotes/{quoteId}/like` | Path: `quoteId` | 없음 (204) | |

#### AI 요약 요청 예시
```json
{ "content": "오늘은 길고 피곤한 하루였지만, 결국 작은 성취들이 모여 나를 앞으로 나아가게 했다." }
```

#### AI 요약 응답 예시
```json
{ "summaries": ["작은 성취가 나를 앞으로 나아가게 한다.", "피곤함 속에서도 한 걸음씩 나아가는 것이 진짜 변화다.", "하루의 끝에 남는 것은 포기하지 않은 나의 발자국이다."] }
```

#### 에러 응답 예시
- `429 Too Many Requests` (일일 제한 초과)
```json
{
  "resultCode": "429-1",
  "msg": "AI 추천은 하루 1회까지만 사용할 수 있습니다.",
  "data": {}
}
```
- `400 Bad Request` (잘못된 입력/요청)
```json
{
  "resultCode": "400-1",
  "msg": "내용을 입력해주세요.",
  "data": {}
}
```
- `400 Bad Request` (일반 문자 없음: 이모지·숫자·특수문자·한글 자모로만 된 일기)
```json
{
  "resultCode": "400-7",
  "msg": "글자가 포함된 내용만 AI 추천을 받을 수 있어요.",
  "data": {}
}
```
| 피드 | 좋아요 취소 | `DELETE` | `/api/quotes/{quoteId}/like` | Path: `quoteId` | 없음 (204) | |
| 피드 | 북마크 추가 | `POST` | `/api/quotes/{quoteId}/bookmark` | Path: `quoteId` | `{ resultCode, data }` | HTTP 201 |
| 피드 | 북마크 취소 | `DELETE` | `/api/quotes/{quoteId}/bookmark` | Path: `quoteId` | `{ resultCode, data }` | |
| 피드 | 태그 요청 전송 | `POST` | `/api/quotes/{quoteId}/tag-request` | Path: `quoteId` | `{ resultCode }` | HTTP 201. 명언 작성자에게 태그 요청. 거절된 요청은 다시 보낼 수 있음 |
| 피드 | 내 태그 요청 상태 조회 | `GET` | `/api/quotes/{quoteId}/my-tag-request` | Path: `quoteId` | `{ "status": "PENDING"\|"ACCEPTED"\|"REJECTED"\|"NONE" }` | 버튼 비활성화 여부 확인용 |
| 명언 상세 | 태그 수정 | `PATCH` | `/api/quotes/{quoteId}/tags` | Path: `quoteId`, Body: `{ taggedMemberIds: [Long] }` | 없음 (200) | 명언 작성자만 가능. 새로 추가하는 대상은 수락된 친구만 가능, 본인 제외 (위반 시 400). 이미 태그된 회원은 유지 가능 |
| 명언 상세 | 태그 요청 목록 조회 | `GET` | `/api/quotes/{quoteId}/requests` | Path: `quoteId` | `List<QuoteTagRequestResponse>` | 명언 작성자에게 들어온 태그 요청 목록 |
| 알림 | 태그 요청 수락 | `POST` | `/api/quotes/requests/{requestId}/accept` | Path: `requestId` | `{ resultCode }` | |
| 알림 | 태그 요청 거절 | `POST` | `/api/quotes/requests/{requestId}/reject` | Path: `requestId` | `{ resultCode }` | |

### QuoteListDto
```json
{
  "myQuotes": [ MyQuoteResponse ],
  "otherQuotes": [ QuoteDetailResponse ]
}
```

### MyQuoteResponse
```json
{ "id": Long, "content": String, "groupName": String, "authorNickname": String, "birthYear": String, "taggedNicknames": [String], "isBookmarked": Boolean, "tags": [{ "memberId": Long, "nickname": String, "profileImage": String }] }
```

### QuoteDetailResponse
```json
{ "id": Long, "content": String, "taggedNicknames": [String], "authorNickname": String, "authorBirthYear": String, "authorProfileImage": String, "authorIntroduction": String, "timeAgo": String, "isLiked": Boolean, "isBookmarked": Boolean, "isFriendQuote": Boolean, "myTagStatus": String, "tags": [{ "memberId": Long, "nickname": String, "profileImage": String }] }
```

- `myTagStatus`: `TAGGED` (태그됨), `REQUESTED` (태그 요청 대기 중), `NONE` (없음, 거절된 요청 포함)

### QuoteResponse
```json
{ "id": Long, "content": String, "originalContent": String, "summary": String, "authorName": String, "authorBirthYear": Integer, "taggedMemberNames": [String], "createDate": DateTime, "tags": [{ "memberId": Long, "nickname": String, "profileImage": String }], "isBookmarked": Boolean }
```

### QuoteTagRequestResponse
```json
{ "requestId": Long, "requesterId": Long, "requesterName": String }
```

---

## 4. 아카이브 (Archive)

Base Path: `/api/archives`

| UI 페이지 | 세부 기능 | 메소드 | API Path | 요청 파라미터/Body | 응답 | 참고 |
|-----------|-----------|--------|----------|-------------------|------|------|
| 아카이브 | 날짜별 내 명언 조회 | `GET` | `/api/archives` | Query: `date` (`yyyy-MM-dd`) | `List<QuoteResponse>` | 캘린더 화면용 |
| 아카이브 | 내가 작성한 명언 전체 | `GET` | `/api/archives/me` | 없음 | `List<QuoteResponse>` | originalContent + summary 포함 |
| 아카이브 | 내가 좋아요한 명언 | `GET` | `/api/archives/likes` | 없음 | `List<QuoteResponse>` | |
| 아카이브 | 내가 북마크한 명언 | `GET` | `/api/archives/bookmarks` | Query: `month` (선택, `yyyy-MM`) | `List<QuoteResponse>` | month 지정 시 명언 작성일 기준 해당 월만, 형식 오류 400 |

---

## 5. 친구 (Friendship)

Base Path: `/api/friends`

| UI 페이지 | 세부 기능 | 메소드 | API Path | 요청 파라미터/Body | 응답 | 참고 |
|-----------|-----------|--------|----------|-------------------|------|------|
| 친구 관리 | 친구 요청 전송 | `POST` | `/api/friends/request/{targetId}` | Path: `targetId` (대상 회원 ID) | `{ resultCode }` | HTTP 201. 거절된 요청은 양방향 모두 다시 보낼 수 있음 |
| 친구 관리 | 보낸 친구 요청 취소 | `DELETE` | `/api/friends/request/{receiverId}` | Path: `receiverId` (요청을 받은 회원 ID) | `{ resultCode }` | 내가 보낸 대기(REQUESTED) 요청만 삭제하고 상대에게 간 친구 요청 알림도 삭제. 요청 없음 404, 상대가 나에게 보낸 요청 403, 이미 처리된 요청 400. 취소 후 다시 요청 가능, 푸시 없음 |
| 친구 관리 | 받은 친구 요청 목록 | `GET` | `/api/friends/requests` | 없음 | `List<FriendRequestResponse>` | 받은 요청(`direction: RECEIVED`)과 보낸 대기 요청(`direction: SENT`)을 함께 반환. 취소 버튼은 `SENT` 항목의 `receiverId`로 호출 |
| 알림 | 친구 요청 수락 | `POST` | `/api/friends/requests/{requestId}/accept` | Path: `requestId` | `{ resultCode }` | |
| 알림 | 친구 요청 거절 | `POST` | `/api/friends/requests/{requestId}/reject` | Path: `requestId` | `{ resultCode }` | |
| 친구 관리 | 친구 삭제 | `DELETE` | `/api/friends/{friendId}` | Path: `friendId` (친구 회원 ID) | `{ resultCode }` | |

### FriendRequestResponse
```json
{ "requestId": Long, "senderId": Long, "senderNickname": String, "senderProfileImage": String, "senderIntroduction": String }
```

---

## 6. 그룹 (Group)

Base Path: `/api/groups`

| UI 페이지 | 세부 기능 | 메소드 | API Path | 요청 파라미터/Body | 응답 | 참고 |
|-----------|-----------|--------|----------|-------------------|------|------|
| 그룹 | 그룹 생성 | `POST` | `/api/groups` | `{ name (max 10자), motto? (max 20자) }` | `GroupResponse` | 이름 10자·좌우명 20자(코드포인트 기준), 이름은 한글·영문·숫자·공백·이모지만 허용 (위반 시 400). 앞뒤 공백을 빼고 영문 대소문자를 무시해 같은 이름의 그룹이 있으면 409 (`409-1`, "이미 사용 중인 그룹 이름입니다.") |
| 그룹 | 내가 가입한 그룹 목록 | `GET` | `/api/groups/me` | 없음 | `List<GroupResponse>` | |
| 그룹 상세 | 그룹 상세 조회 | `GET` | `/api/groups/{groupId}` | Path: `groupId` | `GroupDetailResponse` | 인증 불필요 |
| 그룹 상세 | 그룹 삭제 | `DELETE` | `/api/groups/{groupId}` | Path: `groupId` | `{ resultCode }` | 그룹장만 가능 |
| 그룹 상세 | 그룹 좌우명 수정 | `PATCH` | `/api/groups/{groupId}/motto` | Path: `groupId`, Body: `{ motto (max 20자) }` | `{ resultCode }` | 그룹원 누구나 가능. 20자는 코드포인트 기준 |
| 그룹 상세 | 그룹 멤버 강제 탈퇴 / 본인 탈퇴 | `DELETE` | `/api/groups/{groupId}/members/{memberId}` | Path: `groupId`, `memberId` | `{ resultCode }` | 그룹장은 강제 퇴장, 본인은 탈퇴 |
| 그룹 초대 | 친구 그룹 초대 | `POST` | `/api/groups/{groupId}/invite/{friendId}` | Path: `groupId`, `friendId` | `{ resultCode }` | |
| 그룹 관리 | 특정 그룹의 보낸 초대 대기 목록 조회 | `GET` | `/api/groups/{groupId}/invitations/sent` | Path: `groupId` | `List<GroupJoinRequestResponse>` | 그룹장 전용, PENDING 초대만 조회 |
| 그룹 관리 | 보낸 초대 취소 | `DELETE` | `/api/groups/{groupId}/invitations/{requestId}` | Path: `groupId`, `requestId` | `{ resultCode }` | 그룹장 전용, PENDING 초대만 취소 |
| 그룹 가입 | 그룹 가입 요청 | `POST` | `/api/groups/{groupId}/join-request` | Path: `groupId` | `{ resultCode }` | |
| 그룹 관리 | 가입 요청 목록 조회 | `GET` | `/api/groups/{groupId}/join-requests` | Path: `groupId` | `List<GroupJoinRequestResponse>` | 그룹장 전용 |
| 그룹 가입 | 보낸 가입 신청 목록 | `GET` | `/api/groups/join-requests/sent` | 없음 | `List<GroupJoinRequestSentResponse>` | 내가 보낸 대기(PENDING) 가입 신청만, 최신순 |
| 그룹 가입 | 가입 신청 취소 | `DELETE` | `/api/groups/join-requests/{requestId}` | Path: `requestId` | `{ resultCode }` | 신청자 본인의 대기 가입 신청만 삭제하고 그룹장에게 간 가입 요청 알림도 삭제. 없음 404, 본인 아님 403, 초대이거나 이미 처리됨 400. 취소 후 다시 신청 가능, 푸시 없음 |
| 알림 | 가입 요청 수락 | `POST` | `/api/groups/join-requests/{requestId}/accept` | Path: `requestId` | `{ resultCode }` | |
| 알림 | 가입 요청 거절 | `POST` | `/api/groups/join-requests/{requestId}/reject` | Path: `requestId` | `{ resultCode }` | |
| 알림 | 내게 온 그룹 초대 목록 | `GET` | `/api/groups/invitations` | 없음 | `List<GroupInviteResponse>` | |
| 알림 | 그룹 초대 수락 | `POST` | `/api/groups/invitations/{requestId}/accept` | Path: `requestId` | `{ resultCode }` | |
| 알림 | 그룹 초대 거절 | `POST` | `/api/groups/invitations/{requestId}/reject` | Path: `requestId` | `{ resultCode }` | |

### GroupResponse
```json
{ "id": Long, "name": String, "motto": String, "leaderNickname": String, "memberCount": Long }
```

### GroupDetailResponse
```json
{ "id": Long, "name": String, "motto": String, "leaderNickname": String, "memberCount": Long, "totalQuoteCount": Long, "createdAt": DateTime, "members": [ { "id": Long, "nickname": String, "profileImage": String, "introduction": String } ] }
```

### GroupJoinRequestResponse
```json
{ "requestId": Long, "requesterId": Long, "requesterNickname": String, "requesterProfileImage": String, "requesterIntroduction": String }
```

### GroupJoinRequestSentResponse
```json
{ "requestId": Long, "groupId": Long, "groupName": String, "requestedAt": String }
```

### GroupInviteResponse
```json
{ "requestId": Long, "groupId": Long, "groupName": String, "groupMotto": String, "inviterNickname": String, "leaderNickname": String, "memberCount": Integer }
```

---

## 7. 알림 (Notification)

Base Path: `/api/notifications`

| UI 페이지 | 세부 기능 | 메소드 | API Path | 요청 파라미터/Body | 응답 | 참고 |
|-----------|-----------|--------|----------|-------------------|------|------|
| 알림 | 알림 목록 조회 | `GET` | `/api/notifications` | Query: `category` (optional): `GROUP` \| `FRIEND` \| `TAG` | `List<NotificationResponse>` | category 없으면 전체 조회 |
| 홈 (뱃지) | 미읽음 알림 개수 | `GET` | `/api/notifications/unread-count` | 없음 | `{ "count": Long }` | |
| 알림 | 알림 읽음 처리 | `PATCH` | `/api/notifications/{id}/read` | Path: `id` | 없음 (200) | |
| 설정 | 알림 설정 조회 | `GET` | `/api/notifications/settings` | 없음 | `NotificationSettingResponse` | |
| 설정 | 알림 설정 수정 | `PUT` | `/api/notifications/settings` | `{ groupEnabled, friendEnabled, tagEnabled, pokeEnabled, likeEnabled, quoteReminderEnabled, marketingEnabled, quoteReminderTime }` | `NotificationSettingResponse` | boolean 7개는 필수. `quoteReminderTime`은 선택, `"HH:mm"`(KST), 생략하면 기존 값 유지, 기본값 `"10:00"` |

### NotificationResponse
```json
{ "id": Long, "type": String, "message": String, "targetId": Long, "senderName": String, "createDate": String, "isRead": Boolean }
```

타입 `QUOTE_REMINDER`(오늘의 명언 작성 알림)는 발신자가 없어 `senderName`과 `targetId`가 `null`이며, 카테고리 필터에는 포함되지 않고 전체 조회에서만 나온다.

### NotificationSettingResponse
```json
{ "groupEnabled": Boolean, "friendEnabled": Boolean, "tagEnabled": Boolean, "pokeEnabled": Boolean, "likeEnabled": Boolean, "quoteReminderEnabled": Boolean, "marketingEnabled": Boolean, "quoteReminderTime": "HH:mm" }
```

---

## 8. 설정 / 검색 (Settings)

Base Path: `/api/settings`

| UI 페이지 | 세부 기능 | 메소드 | API Path | 요청 파라미터/Body | 응답 | 참고 |
|-----------|-----------|--------|----------|-------------------|------|------|
| 설정 | 친구/그룹 통합 검색 | `GET` | `/api/settings/search` | Query: `keyword` | `SearchCombinedResponse` | 닉네임 기반 검색. 공백뿐인 검색어는 빈 결과, `%`·`_`는 문자 그대로 검색. 응답의 회원 항목에 `email` 없음 (이메일 앞부분 검색은 유지) |
| 설정 | 친구 목록 조회 | `GET` | `/api/settings/friends-list` | 없음 | `List<FriendResponse>` | |

### FriendResponse
```json
{ "id": Long, "nickname": String, "profileImage": String, "introduction": String, "isGroupMember": Boolean }
```

---

## 9. 콕 찌르기 (Poke)

Base Path: `/api/pokes`

| UI 페이지 | 세부 기능 | 메소드 | API Path | 요청 파라미터/Body | 응답 | 참고 |
|-----------|-----------|--------|----------|-------------------|------|------|
| 피드/프로필 | 콕 찌르기 | `POST` | `/api/pokes/{receiverId}` | Path: `receiverId` (대상 회원 ID) | 없음 (200) | 수락된 친구만 가능 (위반 시 400). 수신자의 friendEnabled, pokeEnabled가 모두 켜져 있을 때만 알림 생성 |
| 통계 | 내가 받은 콕 찌르기 횟수 | `GET` | `/api/pokes/statistics` | 없음 | `{ "receivedCount": Long }` | |

---

## 10. 공지 (Notice)

공지는 운영팀이 노션 공지 데이터베이스에 작성하면 서버가 10분마다 가져와 반영한다. `게시`가 체크된 페이지만 공지가 되고, 체크를 해제하거나 페이지를 삭제하면 서버에서도 삭제된다. `유형`(공지→`NOTICE`, 업데이트→`UPDATE`, 없으면 `NOTICE`), `중요`→`important`. 본문은 일반 텍스트이며 목록 항목은 `• `로 시작하고 줄바꿈(`
`)으로 구분된다. 제목·본문은 255자까지 저장된다.

| UI 페이지 | 세부 기능 | 메소드 | API Path | 요청 파라미터/Body | 응답 | 참고 |
|-----------|-----------|--------|----------|-------------------|------|------|
| 공지 | 공지 목록 | `GET` | `/api/notices` | 없음 | `List<NoticeResponse>` | 중요 공지 먼저, 그다음 최신순 |
| 공지 | 공지 상세 | `GET` | `/api/notices/{id}` | Path: `id` | `NoticeDetailResponse` | 없으면 404 |

### NoticeResponse
```json
{ "id": Long, "type": String, "title": String, "content": String, "createDate": String, "important": Boolean }
```

### NoticeDetailResponse
```json
{ "id": Long, "type": String, "title": String, "content": String, "createDate": String, "modifyDate": String, "important": Boolean }
```

---

## 11. 푸시 (Push)

Base Path: `/api/push/tokens`

| UI 페이지 | 세부 기능 | 메소드 | API Path | 요청 파라미터/Body | 응답 | 참고 |
|-----------|-----------|--------|----------|-------------------|------|------|
| 로그인 후 | 기기 토큰 등록 | `POST` | `/api/push/tokens` | `{ token, platform }` (`platform`: `WEB` \| `ANDROID` \| `IOS`) | `{ resultCode }` | 인증 필요. 같은 토큰이 이미 있으면 소유자를 현재 회원으로 옮긴다. 게스트 403, 빈 토큰·잘못된 platform 400 |
| 로그아웃 | 기기 토큰 삭제 | `DELETE` | `/api/push/tokens` | `{ token }` | `{ resultCode }` | 본인 토큰만 삭제 (없거나 남의 토큰이면 아무것도 하지 않고 200) |

- 인앱 알림이 만들어질 때마다 같은 내용을 푸시로도 보낸다. 알림 설정에서 꺼진 카테고리는 인앱 알림과 푸시 모두 오지 않는다
- 푸시 내용: `notification.title` = `"QuoteME"`, `notification.body` = 인앱 알림 문구, `data` = `{ notificationId, type, targetId }` (모두 문자열, `targetId`가 없으면 빈 문자열)
- 신청 취소(친구·그룹 가입)는 푸시를 보내지 않는다

---

## 공통 응답 형식 (RsData)

```json
{
  "resultCode": "200-1",
  "message": "성공 메시지",
  "data": { ... }
}
```

- `resultCode` 앞자리: HTTP 상태 코드 의미 (200: 성공, 201: 생성, 400: 잘못된 요청, 401: 인증 실패, 403: 권한 없음·미인증, 404: 없음, 405: 허용되지 않은 메서드, 409: 중복, 413: 업로드 초과, 429: 한도 초과)
- 일부 엔드포인트는 `RsData` 래핑 없이 데이터 직접 반환
- 응답이 `{ resultCode }`로 표기된 엔드포인트는 안내 문구를 `message`에 담고 `data`는 `null`이다
- 오류 응답도 같은 형식이며 HTTP 상태 코드가 `resultCode`와 일치한다. 파라미터 누락·타입 오류·본문 파싱 오류는 400
- 처리되지 않은 오류는 500이며 `data`에는 `path`만 담긴다 (스택 트레이스 미포함)

## 인증 방식

- AccessToken: HttpOnly Cookie `accessToken` (만료: 300초, 소셜 로그인 포함)
- RefreshToken: HttpOnly Cookie `refreshToken` (만료: 30일)
- SameSite=None, Secure 설정
- 인증 불필요 경로: `/api/auth/**`
- 소셜 로그인 시작: `GET /oauth2/authorization/{google|kakao}?mode=login|signup`
  - `mode=login`: 가입되지 않은 계정이면 실패 처리한다
  - `mode=signup` 또는 생략: 없으면 가입한다. 이미 가입된 계정이면 새로 만들지 않고 로그인 처리한다 (`isNewUser` 없음)
- 소셜 로그인 성공: 두 쿠키를 설정하고 `redirect-uri?accessToken=...`로 이동한다. refreshToken은 URL에 없고, 신규 가입이면 `&isNewUser=true`가 붙는다
- 소셜 로그인 실패: `failure-uri`로 이동하며 원인을 아는 경우 `reason`이 붙는다 (`account_not_found`, `email_not_verified`, 연동 시 `already_linked`, `provider_already_linked`)
- 쿠키 인증의 변경 요청은 허용 목록에 없는 `Origin`이면 403 (`Origin: null` 포함). `Origin`이 없는 요청은 통과

## 앱(RN) 인증

상세 정책·흐름·오류 코드는 **Swagger 상단 가이드**(`/swagger-ui.html`, 원문 `src/main/resources/openapi/app-guide.md`)를 따른다. 여기에는 경로만 둔다.

- 앱 요청 헤더: `X-Client-Type: app` (refreshToken을 본문으로 주고받고 쿠키를 쓰지 않음)
- 앱 소셜 로그인 시작: `GET /oauth2/authorization/{google|kakao}?mode=...&client=app&redirect_uri=...&code_challenge=...&code_challenge_method=S256` (시스템 인증 브라우저)
- 교환: `POST /api/auth/oauth2/token` `{ code, codeVerifier }`
- 앱 계정 연동: `POST /api/profile/account/link/{provider}/start` → `authorizationUrl` → `POST /api/profile/account/link/confirm` `{ code, codeVerifier }`
- 첫 프로필 설정: `PUT /api/profile/setup` `{ nickname, birthYear, introduction? }` (성공 시 `profileCompleted=true`)
- 인증 실패: 토큰 없음 `401`(`401-4`), 무효·만료 `401`(`401-5`), 권한 거절 `403`
