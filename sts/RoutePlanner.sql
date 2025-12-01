-- 기존에 같은 이름이 있다면 삭제 (에러 방지용)
DROP TABLE REVIEW CASCADE CONSTRAINTS;
DROP TABLE PLACE CASCADE CONSTRAINTS;
DROP TABLE MEMBER_LIKE_ROUTE CASCADE CONSTRAINTS;
DROP TABLE TOKEN CASCADE CONSTRAINTS;
DROP TABLE ROUTE CASCADE CONSTRAINTS;
DROP TABLE "MEMBER" CASCADE CONSTRAINTS;

DROP SEQUENCE SEQ_MEMBER_ID;
DROP SEQUENCE SEQ_ROUTE_ID;
DROP SEQUENCE SEQ_REVIEW_NO;
DROP SEQUENCE SEQ_TOKEN_ID;
DROP SEQUENCE SEQ_PLACE_PK;
DROP SEQUENCE SEQ_LIKE_ID;

CREATE SEQUENCE SEQ_MEMBER_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_ROUTE_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_REVIEW_NO START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_TOKEN_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_PLACE_PK START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_LIKE_ID START WITH 1 INCREMENT BY 1;

CREATE TABLE TRAVEL_USER (
    memberid    NUMBER              NOT NULL, -- 시퀀스 사용
    nickname    VARCHAR2(50),
    gender      CHAR(1),
    age         NUMBER,
    email       VARCHAR2(100),
    is_owner    VARCHAR2(20),       -- 'USER', 'ADMIN' 등
    enrolldate  DATE                DEFAULT SYSDATE NOT NULL,
    delflag     CHAR(1)             DEFAULT 'N',
    deletedate  DATE,
    regflag     CHAR(1),
    password    VARCHAR2(300),
    -- like_route는 별도 테이블로 분리함 (아래 MEMBER_LIKE_ROUTE 참고)
    CONSTRAINT PK_MEMBER PRIMARY KEY (memberid)
);

CREATE TABLE ROUTE (
    rt_id           NUMBER          NOT NULL, -- 시퀀스 사용
    fk_memberid     NUMBER          NOT NULL,
    rt_title        VARCHAR2(100),
    rt_content      VARCHAR2(2000),
    rt_date_start   DATE,
    rt_date_end     DATE,
    rt_count        NUMBER          DEFAULT 0,
    rt_category     VARCHAR2(50),
    allow_view_user VARCHAR2(500),  -- 공유할 유저 ID들 (문자열)
    lock_flag       CHAR(1),
    rt_date         DATE            DEFAULT SYSDATE NOT NULL,
    rt_date_update  DATE,
    rt_date_delete  DATE,
    rt_link         VARCHAR2(500),
    rt_img_name     VARCHAR2(2000),
    rt_visible      CHAR(1)         DEFAULT '1' NOT NULL,
    CONSTRAINT PK_ROUTE PRIMARY KEY (rt_id),
    CONSTRAINT FK_ROUTE_MEMBER FOREIGN KEY (fk_memberid) REFERENCES "MEMBER"(memberid) ON DELETE CASCADE
);

COMMENT ON COLUMN ROUTE.allow_view_user IS '볼 수 있는 유저 mID들';
COMMENT ON COLUMN ROUTE.rt_visible IS '1이면 공개, 0이면 삭제됨';

CREATE TABLE MEMBER_LIKE_ROUTE (
    like_id     NUMBER NOT NULL,
    memberid    NUMBER NOT NULL,
    rt_id       NUMBER NOT NULL,
    like_date   DATE DEFAULT SYSDATE,
    CONSTRAINT PK_LIKE PRIMARY KEY (like_id),
    CONSTRAINT FK_LIKE_MEMBER FOREIGN KEY (memberid) REFERENCES "MEMBER"(memberid) ON DELETE CASCADE,
    CONSTRAINT FK_LIKE_ROUTE FOREIGN KEY (rt_id) REFERENCES ROUTE(rt_id) ON DELETE CASCADE
);

CREATE TABLE REVIEW (
    rv_no           NUMBER          NOT NULL, -- 시퀀스 사용
    fk_rt_id        NUMBER          NOT NULL,
    fk_memberid     NUMBER,
    rv_content      VARCHAR2(1000),
    rv_date         DATE            DEFAULT SYSDATE NOT NULL,
    rv_date_update  DATE,
    CONSTRAINT PK_REVIEW PRIMARY KEY (rv_no),
    CONSTRAINT FK_REVIEW_ROUTE FOREIGN KEY (fk_rt_id) REFERENCES ROUTE(rt_id) ON DELETE CASCADE,
    CONSTRAINT FK_REVIEW_MEMBER FOREIGN KEY (fk_memberid) REFERENCES "MEMBER"(memberid) ON DELETE SET NULL
);

CREATE TABLE TOKEN (
    ID          NUMBER          NOT NULL, -- 시퀀스 사용
    user_id     NUMBER          NOT NULL,
    TOKEN       VARCHAR2(2000)  NOT NULL, 
    TOKEN_TYPE  VARCHAR2(50)    DEFAULT 'BEARER',
    REVOKED     NUMBER(1)       DEFAULT 0 NOT NULL,
    EXPIRED     NUMBER(1)       DEFAULT 0 NOT NULL,
    CONSTRAINT PK_TOKEN PRIMARY KEY (ID),
    CONSTRAINT FK_TOKEN_USER FOREIGN KEY (user_id) REFERENCES "MEMBER"(memberid) ON DELETE CASCADE
);

COMMENT ON COLUMN TOKEN.TOKEN_TYPE IS 'BEARER 고정';
COMMENT ON COLUMN TOKEN.REVOKED IS '철회 1, 0 -> true/false';

CREATE TABLE PLACE (
    place_pk        NUMBER          NOT NULL, -- 이 테이블만의 고유 ID (시퀀스)
    fk_rt_id        NUMBER          NOT NULL, -- 어느 여행 루트에 속했는지
    google_place_id VARCHAR2(500)   NOT NULL, -- 구글 맵 API의 ID
    pl_title        VARCHAR2(100),
    pl_address      VARCHAR2(2000),
    pl_type         VARCHAR2(50),
    lat             NUMBER(10, 6),  -- 위도 (정밀도 높음)
    lng             NUMBER(10, 6),  -- 경도 (정밀도 높음)
    rating          NUMBER(3, 1),
    total_review    NUMBER,
    photos_info     CLOB,           -- JSON 데이터가 길어서 CLOB 사용
    CONSTRAINT PK_PLACE PRIMARY KEY (place_pk),
    CONSTRAINT FK_PLACE_ROUTE FOREIGN KEY (fk_rt_id) REFERENCES ROUTE(rt_id) ON DELETE CASCADE
);

COMMENT ON COLUMN PLACE.photos_info IS '사진 정보 리스트(JSON)';
COMMENT ON COLUMN PLACE.place_pk IS '테이블 고유 식별자';
COMMENT ON COLUMN PLACE.google_place_id IS 'Google Maps Place ID';







