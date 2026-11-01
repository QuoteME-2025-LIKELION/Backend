-- 중복 행 점검·정리 SQL (MySQL 8.0)
-- 1부 SELECT는 읽기 전용이다. 2부 정리 DELETE는 초안이며 주석 처리되어 있다.
-- 정리 전에 반드시 스냅샷을 만들고, 1부 결과를 검토한 뒤 트랜잭션 안에서 실행한다.

-- ── 1부. 중복 조회 ─────────────────────────────────────────

-- 같은 회원이 같은 명언에 누른 좋아요
SELECT quote_id, member_id, COUNT(*) AS cnt, MIN(id) AS keep_id
FROM quote_like
GROUP BY quote_id, member_id
HAVING COUNT(*) > 1;

-- 같은 회원이 같은 명언에 중복 태그됨
SELECT quote_id, member_id, COUNT(*) AS cnt, MIN(id) AS keep_id
FROM quote_tag
GROUP BY quote_id, member_id
HAVING COUNT(*) > 1;

-- 같은 회원이 같은 명언에 보낸 태그 요청
SELECT quote_id, requester_id, COUNT(*) AS cnt, MIN(id) AS keep_id
FROM quote_tag_request
GROUP BY quote_id, requester_id
HAVING COUNT(*) > 1;

-- 같은 방향의 친구 관계 (친구 관계는 방향별로 한 행이므로 양방향 쌍은 중복이 아니다)
SELECT member_id, friend_id, COUNT(*) AS cnt, MIN(id) AS keep_id
FROM friendships
GROUP BY member_id, friend_id
HAVING COUNT(*) > 1;

-- ── 2부. 정리 초안 (검토 후 주석을 풀어 실행) ─────────────────

-- START TRANSACTION;

-- 가장 먼저 만든 행만 남긴다
-- DELETE dup FROM quote_like dup
-- JOIN quote_like keep
--   ON keep.quote_id = dup.quote_id AND keep.member_id = dup.member_id AND keep.id < dup.id;

-- DELETE dup FROM quote_tag dup
-- JOIN quote_tag keep
--   ON keep.quote_id = dup.quote_id AND keep.member_id = dup.member_id AND keep.id < dup.id;

-- 처리 결과를 보존하도록 ACCEPTED > PENDING > REJECTED, 같으면 먼저 만든 행을 남긴다
-- DELETE dup FROM quote_tag_request dup
-- JOIN quote_tag_request keep
--   ON keep.quote_id = dup.quote_id AND keep.requester_id = dup.requester_id
--  AND (FIELD(keep.status, 'ACCEPTED', 'PENDING', 'REJECTED') < FIELD(dup.status, 'ACCEPTED', 'PENDING', 'REJECTED')
--       OR (keep.status = dup.status AND keep.id < dup.id));

-- ACCEPTED > REQUESTED > REJECTED, 같으면 먼저 만든 행을 남긴다
-- DELETE dup FROM friendships dup
-- JOIN friendships keep
--   ON keep.member_id = dup.member_id AND keep.friend_id = dup.friend_id
--  AND (FIELD(keep.status, 'ACCEPTED', 'REQUESTED', 'REJECTED') < FIELD(dup.status, 'ACCEPTED', 'REQUESTED', 'REJECTED')
--       OR (keep.status = dup.status AND keep.id < dup.id));

-- 1부를 다시 실행해 0건임을 확인한 뒤 COMMIT;
-- ROLLBACK;
