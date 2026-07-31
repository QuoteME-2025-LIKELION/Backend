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
| 공통 | 로그아웃 | `POST` | `/api/auth/logout` | 없음 | `{ resultCode }` | accessToken, refreshToken 쿠키 삭제 |

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
| 내 프로필 | 프로필 수정 | `PUT` | `/api/profile` | `multipart/form-data`<br>- `data` (JSON): `{ nickname?, introduction?, profileImage? }`<br>- `image` (file, optional): 프로필 이미지 | `ProfileResponse` | 이미지 업로드 시 S3 저장 |
| 내 프로필 | 계정 정보 조회 | `GET` | `/api/profile/account` | 없음 | `AccountInfoResponse` | 성별, 생년월일, 소셜 연동 상태 |
| 내 프로필 | 계정 정보 수정 | `PUT` | `/api/profile/account` | `{ email, birthYear, gender }` | `String` ("계정 정보가 수정되었습니다.") | gender: `MALE` \| `FEMALE` |
| 내 프로필 | 회원 탈퇴 | `DELETE` | `/api/profile/account` | 없음 | `String` ("회원 탈퇴가 완료되었습니다.") | |
| 타인 프로필 | 다른 회원 프로필 조회 | `GET` | `/api/profile/{id}` | Path: `id` (회원 ID) | `MemberResponse` | |

### ProfileResponse
```json
{ "id": Long, "email": String, "nickname": String, "introduction": String, "profileImage": String, "quoteCount": Long, "friendCount": Long }
```

### AccountInfoResponse
```json
{ "email": String, "birthYear": String, "gender": "MALE"|"FEMALE", "provider": String, "providerId": String }
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
| 홈/피드 | 피드 명언 목록 조회 | `GET` | `/api/quotes` | Query: `date` (required, `yyyy-MM-dd`), `groupId` (optional) | `QuoteListDto` | groupId 없으면 전체 친구 피드, 있으면 해당 그룹 멤버 피드 |
| 명언 작성 | 명언 작성 | `POST` | `/api/quotes` | `{ content, originalContent?, summary?, taggedMemberIds?: [Long] }` | `QuoteResponse` | HTTP 201 |
| 명언 작성 | AI 요약 (일기→명언) | `POST` | `/api/quotes/summarize` | `{ content }` | `{ "summaries": [String] }` | 하루 3회 제한 |
| 명언 작성 | AI 사용량 조회 | `GET` | `/api/quotes/ai-usage` | 없음 | `{ "usedCount": Long, "maxCount": Long, ... }` | 오늘 AI 사용 횟수 조회 |
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
  "msg": "AI 추천은 하루 3회까지만 사용할 수 있습니다.",
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
| 피드 | 좋아요 취소 | `DELETE` | `/api/quotes/{quoteId}/like` | Path: `quoteId` | 없음 (204) | |
| 피드 | 북마크 추가 | `POST` | `/api/quotes/{quoteId}/bookmark` | Path: `quoteId` | `{ resultCode, data }` | HTTP 201 |
| 피드 | 북마크 취소 | `DELETE` | `/api/quotes/{quoteId}/bookmark` | Path: `quoteId` | `{ resultCode, data }` | |
| 피드 | 태그 요청 전송 | `POST` | `/api/quotes/{quoteId}/tag-request` | Path: `quoteId` | `{ resultCode }` | HTTP 201. 명언 작성자에게 태그 요청 |
| 피드 | 내 태그 요청 상태 조회 | `GET` | `/api/quotes/{quoteId}/my-tag-request` | Path: `quoteId` | `{ "status": "PENDING"\|"ACCEPTED"\|"REJECTED"\|"NONE" }` | 버튼 비활성화 여부 확인용 |
| 명언 상세 | 태그 수정 | `PATCH` | `/api/quotes/{quoteId}/tags` | Path: `quoteId`, Body: `{ taggedMemberIds: [Long] }` | 없음 (200) | 명언 작성자만 가능 |
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
{ "id": Long, "content": String, "groupName": String, "authorNickname": String, "birthYear": String, "taggedNicknames": [String] }
```

### QuoteDetailResponse
```json
{ "id": Long, "content": String, "taggedNicknames": [String], "authorNickname": String, "authorBirthYear": String, "authorProfileImage": String, "authorIntroduction": String, "timeAgo": String, "isLiked": Boolean, "isBookmarked": Boolean, "isFriendQuote": Boolean }
```

### QuoteResponse
```json
{ "id": Long, "content": String, "originalContent": String, "summary": String, "authorName": String, "authorBirthYear": Integer, "taggedMemberNames": [String], "createDate": DateTime }
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
| 아카이브 | 내가 북마크한 명언 | `GET` | `/api/archives/bookmarks` | 없음 | `List<QuoteResponse>` | |

---

## 5. 친구 (Friendship)

Base Path: `/api/friends`

| UI 페이지 | 세부 기능 | 메소드 | API Path | 요청 파라미터/Body | 응답 | 참고 |
|-----------|-----------|--------|----------|-------------------|------|------|
| 친구 관리 | 친구 요청 전송 | `POST` | `/api/friends/request/{targetId}` | Path: `targetId` (대상 회원 ID) | `{ resultCode }` | HTTP 201 |
| 친구 관리 | 받은 친구 요청 목록 | `GET` | `/api/friends/requests` | 없음 | `List<FriendRequestResponse>` | |
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
| 그룹 | 그룹 생성 | `POST` | `/api/groups` | `{ name (max 10자), motto? (max 20자) }` | `GroupResponse` | |
| 그룹 | 내가 가입한 그룹 목록 | `GET` | `/api/groups/me` | 없음 | `List<GroupResponse>` | |
| 그룹 상세 | 그룹 상세 조회 | `GET` | `/api/groups/{groupId}` | Path: `groupId` | `GroupDetailResponse` | 인증 불필요 |
| 그룹 상세 | 그룹 삭제 | `DELETE` | `/api/groups/{groupId}` | Path: `groupId` | `{ resultCode }` | 그룹장만 가능 |
| 그룹 상세 | 그룹 좌우명 수정 | `PATCH` | `/api/groups/{groupId}/motto` | Path: `groupId`, Body: `{ motto (max 20자) }` | `{ resultCode }` | 그룹장만 가능 |
| 그룹 상세 | 그룹 멤버 강제 탈퇴 / 본인 탈퇴 | `DELETE` | `/api/groups/{groupId}/members/{memberId}` | Path: `groupId`, `memberId` | `{ resultCode }` | 그룹장은 강제 퇴장, 본인은 탈퇴 |
| 그룹 초대 | 친구 그룹 초대 | `POST` | `/api/groups/{groupId}/invite/{friendId}` | Path: `groupId`, `friendId` | `{ resultCode }` | |
| 그룹 가입 | 그룹 가입 요청 | `POST` | `/api/groups/{groupId}/join-request` | Path: `groupId` | `{ resultCode }` | |
| 그룹 관리 | 가입 요청 목록 조회 | `GET` | `/api/groups/{groupId}/join-requests` | Path: `groupId` | `List<GroupJoinRequestResponse>` | 그룹장 전용 |
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

### GroupInviteResponse
```json
{ "requestId": Long, "groupId": Long, "groupName": String, "groupMotto": String, "inviterNickname": String }
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
| 설정 | 알림 설정 수정 | `PUT` | `/api/notifications/settings` | `{ groupEnabled, friendEnabled, tagEnabled, pokeEnabled, likeEnabled, quoteReminderEnabled, marketingEnabled }` | `NotificationSettingResponse` | 모두 boolean |

### NotificationResponse
```json
{ "id": Long, "type": String, "message": String, "targetId": Long, "senderName": String, "createDate": String, "isRead": Boolean }
```

### NotificationSettingResponse
```json
{ "groupEnabled": Boolean, "friendEnabled": Boolean, "tagEnabled": Boolean, "pokeEnabled": Boolean, "likeEnabled": Boolean, "quoteReminderEnabled": Boolean, "marketingEnabled": Boolean }
```

---

## 8. 설정 / 검색 (Settings)

Base Path: `/api/settings`

| UI 페이지 | 세부 기능 | 메소드 | API Path | 요청 파라미터/Body | 응답 | 참고 |
|-----------|-----------|--------|----------|-------------------|------|------|
| 설정 | 친구/그룹 통합 검색 | `GET` | `/api/settings/search` | Query: `keyword` | `SearchCombinedResponse` | 닉네임 기반 검색 |
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
| 피드/프로필 | 콕 찌르기 | `POST` | `/api/pokes/{receiverId}` | Path: `receiverId` (대상 회원 ID) | 없음 (200) | |
| 통계 | 내가 받은 콕 찌르기 횟수 | `GET` | `/api/pokes/statistics` | 없음 | `{ "receivedCount": Long }` | |

---

## 공통 응답 형식 (RsData)

```json
{
  "resultCode": "200-1",
  "msg": "성공 메시지",
  "data": { ... }
}
```

- `resultCode` 앞자리: HTTP 상태 코드 의미 (200: 성공, 201: 생성, 400: 잘못된 요청, 401: 인증 실패, 404: 없음)
- 일부 엔드포인트는 `RsData` 래핑 없이 데이터 직접 반환

## 인증 방식

- AccessToken: HttpOnly Cookie `accessToken` (만료: 600초)
- RefreshToken: HttpOnly Cookie `refreshToken` (만료: 30일, UUID)
- SameSite=None, Secure 설정
- 인증 불필요 경로: `/api/auth/**`
