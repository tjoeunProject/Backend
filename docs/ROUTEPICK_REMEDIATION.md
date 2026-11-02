# RoutePick 수정 목록과 순차 PR 계획

검토일: 2026-10-30 (Asia/Seoul)

검토 기준: Backend `main@22e209745faaf8f40c6e238bbaf168aed59ca7b1`, Frontend `fe_main@0d0f4cd29387fdf7f3f7937b2021cdd1efa54422`.
두 저장소의 실제 기본 브랜치를 확인했다. 로컬 Backend 아래의 미추적 Frontend 폴더와 사용자 작업은 업로드하지 않는다.

이 문서는 처리 순서와 완료 조건이다. 체크되지 않은 항목은 수정 완료가 아니다. `재현`은 독립 기대값과 실제 실행 결과가 있고, `코드 근거`는 구현에서 확인했으나 실제 HTTP/DB 통합 검증은 하지 않았다는 뜻이다. 성능 후보는 실측 전까지 개선 효과를 주장하지 않는다.

## 처리 목록

| ID | 우선순위 | 문제·수정 방향 | 근거·상태 | 완료 조건 |
| --- | --- | --- | --- | --- |
| RP-01 | P0·검증 선행 | Java 빌드와 테스트 재현성 복구: Gradle Wrapper 설정과 비밀값 없는 테스트 설정 확보 | Wrapper properties 부재로 `gradlew.bat --version` 실패 재현. `.gitignore`가 모든 properties/resources를 제외 | 프로젝트와 호환되는 Wrapper 버전을 확인해 복구하고 Java 17에서 단위 테스트 실행. 운영 키·DB 계정을 커밋하지 않음 |
| RP-02 | P1 | 일정 생성·수정·삭제에서 로그인 사용자와 작성자 검증 | RouteController/RouteService가 요청 ID와 routeId를 사용하며 사용자 검증을 전달하지 않음. 코드 근거 | 본인 성공·다른 사용자 거절·미로그인 거절·거절 시 DB 변경/외부 호출 없음 검사 |
| RP-03 | P1 | 좋아요의 사용자 ID와 인증 사용자 일치 보장 | MemberLikeRouteController/Service의 경로 userId 사용. 코드 근거 | 본인 추가·취소·조회 및 다른 사용자 거절 검사. 중복 요청 처리도 확인 |
| RP-04 | P1 | 사용자·일정·좋아요 엔티티 직접 응답을 DTO로 제한 | TravelUserController의 엔티티 응답 및 TravelUser의 password/tokens/관계 getter. HTTP 노출 실측은 미실행 | 실제 직렬화/API 검사에서 허용된 필드만 반환하며 인증정보·순환 관계가 응답에 없음을 검증 |
| RP-05 | P1 | Access/Refresh 용도·폐기·탈퇴 계정 정책 정리 | AuthenticationService.refreshToken, JwtService, JwtAuthenticationFilter, TravelUserService. 코드 근거 | 토큰 용도 혼동·만료·로그아웃·탈퇴·재사용을 각각 검증. 기존 로그인·갱신 성공 유지 |
| RP-06 | P2 | 프론트 경로 스텝 API 함수의 import/export 계약 복구 | RoutesStepsPolyline이 computeRouteStepsViaBackend를 import하지만 computeRouteSteps.js는 Direct만 export. 코드 근거; 현재 진입점에서 참조는 찾지 못했으며 실제 빌드 장애로 단정하지 않음 | 백엔드 프록시 계약을 확인해 adapter 구현 후 fixture 응답 검사와 실제 Vite 빌드 통과 |
| RP-07 | P1 | Google E5 Polyline 좌표 배율 수정 | 공식 예제에서 38.5/-120.2 대신 3.85/-12.02 반환 재현. Frontend 초안 PR #73에서 수정·검증, 미병합 | 같은 회귀 테스트 red→green, 경계 조건, 전후 성능 예산 확인 |
| RP-08 | P2 | Hashids 설정 경로 통일 | hashidUtils.js의 import.meta.SALT와 useRouteLogic.jsx의 import.meta.env.VITE_SALT 혼재. 코드 근거 | 기존 ID 호환성·동일 설정 encode/decode·누락 설정 처리 검사. 값은 공개하지 않음 |
| RP-09 | P2 | 토큰 갱신 HTTP 클라이언트와 경로 통일 | axiosConfig.js는 /sts/api/v1/auth, useRouteLogic.jsx는 /api/v1/auth. 코드 근거; 배포 프록시 영향 미검증 | 개발/배포 baseURL·프록시를 확인하고 만료 후 재시도·동시 갱신·갱신 실패 시 종료 검사 |
| RP-10 | P1 | Python 일정 생성의 요청 간 상태 격리 | 전역 generator와 인스턴스 used_restaurant_ids.clear/add 공유. 경쟁 상태는 미재현 | 외부 공급자를 fixture로 대체하고 두 요청을 교차 실행해 결과가 오염되지 않음을 검증 |
| RP-11 | P2 | 검색 캐시의 범위·만료·크기·복사 정책 | generator.py 전역 _RAM_CACHE와 누적 pool 저장. 코드 근거 | 지역별 분리·TTL·상한·입력/출력 변경 격리 검사, 메모리·외부 호출 수 기록 |
| RP-12 | P1 | 유료 API 입력·호출 예산·시간 제한과 오류 응답 | server.py의 days/destination 및 dict 기반 optimize/nearby, 앱 내부 호출 제한·인증 없음. 실제 노출은 배포 게이트웨이 확인 필요 | 빈 지역·음수/과대 일수·과대 장소·공급자 오류/시간 초과를 유료 호출 없이 검사. 키·내부 예외를 응답에 노출하지 않음 |
| RP-13 | P2·성능 후보 | 거리 행렬의 대칭 계산 중복 제거 검토 | optimizer_v2.py가 (i,j)/(j,i)를 모두 geodesic 계산 | 동일 거리·경로 품질을 유지하면서 고정 데이터에서 계산 횟수·시간·메모리 측정. 직선거리와 실제 도로시간 구분 |
| RP-14 | P2·성능 후보 | 일정 목록 조회의 반복 쿼리와 외부 호출 중 DB 트랜잭션 범위 검토 | RouteService 목록 map에서 추가 조회, 쓰기 트랜잭션에서 PlaceService 외부 조회. 코드 근거 | 대표 데이터의 쿼리 수·DB 점유시간·오류 원자성을 전후 비교. 동작 계약 유지 |
| RP-15 | P1·검증 선행 | 실행 정보·회귀 테스트·환경변수 문서 확보 | Java contextLoads만 존재, Frontend test script/의존성 없음, Python 실행 의존성 명세 부족 | 비밀값 없는 설정 예시와 검증된 설치·테스트 명령. 단위/통합/실제 공급자 검증 결과를 구분 |

## 분야별 실제 PR 현황 — 2026-11-02

분류 문서만 만든 것이 아니라 각 묶음에 실제 소스·회귀 검사가 있는 PR을 생성했다. 이 #53은 색인 문서이며 코드 PR을 대체하지 않는다.

| 묶음 | 실제 코드 PR | 확인 결과 / 남은 작업 |
| --- | --- | --- |
| G1 빌드·테스트 | [Backend #54](https://github.com/tjoeunProject/Backend/pull/54) | Java 환경 테스트 2개 통과 |
| G2 권한·응답 | [Backend #55](https://github.com/tjoeunProject/Backend/pull/55) | Java 10개 통과; G1 기반 |
| G3 인증·토큰·ID | [Backend #56](https://github.com/tjoeunProject/Backend/pull/56), [Frontend #74](https://github.com/tjoeunProject/Frontend/pull/74) | Java 17개 / 최종 프론트18개, 인증 합성 Java23개 통과. Oracle 토큰 컬럼·재로그인 및 실제 브라우저 확인 필요 |
| G4 지도 경로 | [Frontend #73](https://github.com/tjoeunProject/Frontend/pull/73) | RP-06·07 구현, 테스트 10개 / Vite build 통과; 실제 지도 통합 미검증 |
| G5 Python 안정성 | [Backend #58](https://github.com/tjoeunProject/Backend/pull/58) | 요청된 코드 구현·Python21개 통과. 인증/예산/단계별 timeout/본문/nearby 제한과 SDK 교체. 실제 공급자·운영 검증은 미실행 |
| G6 일정 성능 | [Backend #57](https://github.com/tjoeunProject/Backend/pull/57) | 외부 조회/원자 저장 분리 구현. Java16개/인증 합성23개, Python1개 통과. 실제 Oracle는 미검증 |

모두 초안·미병합이며 자동 병합하지 않는다. 테스트 개수는 PR별 실행 결과로, 공통 검사를 포함하므로 합산하지 않는다. 코드 구현·로컬 통과와 실제 배포 완료를 구분한다. G5/G6의 요청된 코드 범위도 구현했다. 실제 운영 검증 완료와 구분하며 [최종 보고서](verification/COMPLETION-20261102.md)에 차단 사항을 남긴다.

### 의존 관계와 처리 순서

- Backend: main → integration/routepick-improvements → G1 #54 → G2 #55 → G3 #56 / G6 #57. G3와 G6는 G2에서 각각 갈라진 별도 PR이다.
- Python G5 #58의 병합 대상은 integration/routepick-improvements이며 독립 진행한다.
- Frontend: fe_main → integration/routepick-improvements → G4 #73 → G3 #74. 두 저장소의 G3를 함께 검토하되 PR은 별개다.
- 선행 PR을 통합 브랜치에 병합한 후 후행 base도 통합 브랜치로 바꾸며, 이 문서의 병합을 코드 의존성으로 만들지 않는다.

각 PR 본문과 docs/verification 또는 trip/docs에 실제 red/green, 성능 예산·원시값 또는 비교 차단 사유를 기록했다. Oracle·실제 공급자·브라우저 E2E는 완료한 로컬 검증과 구분한다. 사용자 미추적 파일과 작업 중 변경은 업로드하지 않았다.

## 최초 검토 범위 — 2026-10-30

- GitHub에서 양쪽 저장소의 기본 브랜치와 현재 코드를 확인했고 열린 기존 PR은 발견하지 못했다.
- Java Wrapper 실행은 properties 파일 부재로 실패했다. 이 실패는 애플리케이션 테스트 통과나 운영 장애의 증거가 아니다.
- 프론트 node_modules가 없어 전체 빌드·Lint·브라우저 통합은 미실행이다. Node 내장 테스트로 순수 좌표 함수를 검사한다.
- 실제 DB·인증 공격 시나리오·외부 유료 API 호출·실서비스 부하 검증은 수행하지 않았다. 보안 항목은 코드 근거이며 공개 문서에 공격용 요청이나 비밀값을 담지 않는다.

## 필요한 설정

코드에서 확인한 이름만 기록한다. 실제 값과 배포 설정 상태는 이 문서로 판단하지 않는다.

- Python: GEMINI_API_KEY, SERPAPI_KEY, GOOGLE_MAPS_API_KEY.
- Frontend: VITE_GOOGLE_MAPS_API_KEY, VITE_SALT, VITE_HASHIDS_MIN_LENGTH.
- Spring: google.places.api-key, google.maps.apiKey, hashids.salt, hashids.min-length, application.security.jwt.secret-key 및 Access/Refresh 만료 설정, Oracle datasource 연결 설정.
- 로컬 설정 파일이 없다는 사실은 배포 키가 없다는 뜻이 아니다. 이 설정 목록을 채우기 위해 실제 유료 API를 호출하지 않는다.

좌표 기대값 출처: [Google Encoded Polyline 문서](https://developers.google.com/maps/documentation/utilities/polylinealgorithm).

최신 코드 PR과 미완료 항목은 위 2026-11-02 현황을 기준으로 확인한다.


## 최종 보고서 — 2026-11-02

[수정·검증 보고서](verification/COMPLETION-20261102.md). Java 합성23, Python21, 프론트18, ORTools1, 실제 로컬 인증4계약 통과. 공통 검사를 포함해 개수를 합산하지 않는다. G5와 G6의 남은 코드 수정은 PR에 반영했으며, Oracle migration/실제 공급자/로그인 지도 browser E2E는 미검증이다. 초안 PR을 자동 병합하지 않는다.


## 통합 브랜치 운영

사용자 승인으로 양쪽 저장소에 `integration/routepick-improvements`를 생성했다. Backend는 main@22e209745faaf8f40c6e238bbaf168aed59ca7b1, Frontend는 fe_main@0d0f4cd29387fdf7f3f7937b2021cdd1efa54422에서 시작한다.

- Backend #53·#54·#58, Frontend #73의 base는 통합 브랜치다.
- Backend #55는 #54, #56·#57은 #55, Frontend #74는 #73을 선행 base로 유지한다. 선행 PR이 통합 브랜치에 병합되면 후행 base를 통합 브랜치로 재지정한다.
- 이번 작업은 브랜치 생성·PR 대상/문서 조정뿐이며 PR 병합이나 기본 브랜치 변경은 하지 않았다.
- 통합 브랜치에서 검증 후 기본 브랜치로 가는 최종 PR은 별도 검토한다. 자동 병합하지 않는다.
