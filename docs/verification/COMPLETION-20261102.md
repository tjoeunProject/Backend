# RoutePick 수정·검증 보고서

작성일: 2026-11-02 (Asia/Seoul)

## 판정

G1~G6의 요청된 코드 수정과 로컬 검증을 마쳤다. 실제 소스가 있는 분야별 PR 7개로 유지했으며 자동 병합·운영 배포·운영 DB 변경은 하지 않았다. 운영 준비 완료 또는 테스트의 완벽성을 주장하지 않는다. Oracle, 실제 유료 공급자, 로그인 상태의 지도 브라우저 E2E는 미검증이다.

## 수정 내용과 PR

| 분야 | PR | 최종 변경 |
|---|---|---|
| G1 빌드·테스트 | [Backend #54](https://github.com/tjoeunProject/Backend/pull/54) | Wrapper/Java17/H2 테스트 프로필 복구 |
| G2 권한·응답 | [Backend #55](https://github.com/tjoeunProject/Backend/pull/55) | 일정·좋아요 소유권, 거절 시 무변경, DTO 응답 |
| G3 인증·ID | [Backend #56](https://github.com/tjoeunProject/Backend/pull/56), [Frontend #74](https://github.com/tjoeunProject/Frontend/pull/74) | Access/Refresh 용도, 회전·폐기, 401 단일 갱신, Hashids, Python 요청 인증 전달 |
| G4 지도 | [Frontend #73](https://github.com/tjoeunProject/Frontend/pull/73) | 경로 adapter export/프록시 계약, E5 배율 |
| G5 Python | [Backend #58](https://github.com/tjoeunProject/Backend/pull/58) | 요청 격리·bounded TTL 캐시, 입력·본문·실행 제한, Spring 실제 세션 인증, 오류 정리, 새 Gemini SDK |
| G6 성능·트랜잭션 | [Backend #57](https://github.com/tjoeunProject/Backend/pull/57) | 대표 장소 batch 조회, 대칭 거리 재사용, 외부 조회와 원자적 DB 저장 분리 |

### 이번에 마무리한 동작

- Python은 Spring `/api/user/me`에 전달받은 Bearer를 검증한다. 서명·토큰 용도·폐기·탈퇴 상태 판정을 Spring이 수행하며 인증 서버 장애 시 503으로 닫힌다. 프론트의 생성/최적화/맛집 호출은 공통 인증 클라이언트를 사용한다.
- 프로세스당 전역 요청 10회/분, 실제 작업 2개, 대기열 없는 거절 정책이다. 작업당 공급자 예산 40단위이며 Maps SDK 내부 HTTP 재시도도 계수한다. Maps 논리 호출과 전송 시도가 모두 차감되므로 실제 HTTP 상한보다 보수적이다.
- 실행 응답 대기 60초, 공급자 전송 5초, 인증 조회 3초, 본문 수신 5초/256KiB를 제한한다. 이는 단계별 제한이며 수신부터 응답까지 정확히 60초 SLA라는 뜻은 아니다. 타임아웃된 Python 스레드를 강제 종료하지 않는다. 종료될 때까지 슬롯을 유지하므로 무한 스레드/대기열 증가는 막지만 재시작이 필요한 공급자/네이티브 정지가 있을 수 있다.
- `nearby`는 최대 14일/전체 200개 장소, 정상 숫자 좌표를 검사한다. 생성은 1~14일/1~5개 지역/태그 20개로 제한한다. 캐시는 인스턴스별 최대 128개/600초, 복사·잠금 정책을 적용했다.
- 장소 정보를 모두 조회한 다음 일정·장소·연결 정보를 하나의 DB 트랜잭션으로 저장한다. 수정 시 잠금 안에서 소유권을 다시 확인한다. 공급자 실패 시 저장 전 상태를 확인했다. 저장 실패는 두 번째 PlaceService 저장에 예외를 주입하고 실제 H2 전체 롤백을 확인했으며 실제 Oracle 제약 위반 검사는 아니다. HTTP 테스트 fixture는 실제 커밋·정리 방식으로 맞췄고 원래 권한 기대값은 유지했다.
- 사진 응답에 서버 Maps 키를 포함하지 않는다. 프론트가 공개 브라우저용 키와 photo_reference로 URL을 구성한다. Gemini는 google-genai 2.34.0으로 교체하고 전송 timeout/내부 retry를 명시했다. [공식 SDK](https://github.com/googleapis/python-genai)를 참고했다.

## 실행 결과

| 검증 | 결과 | 경계 |
|---|---|---|
| G6 Java 전체 | 16개 통과 | 실제 Spring/H2, 공급자 mock |
| G3+G6 통합 Java | 23개 통과 | 두 PR 최종 소스 합성본, 실패/오류/skip 0 |
| Python | 21개 통과 | 실제 FastAPI TestClient, 요청 교차 실행·예산·내부 재시도·타임아웃·본문·SDK adapter |
| 프론트 | 18개 통과 | 실제 모듈/axios + HTTP fixture |
| 프론트 production build | 통과 | Vite, 기존 500kB chunk 경고 유지 |
| 경로 최적화 | 1개 통과 | 실제 ORTools, 고정 기대 순서/거리 계산 수 |
| 실제 로컬 인증 연동 | 4개 계약 통과 | Spring HTTP 서버 + H2 + Python; 생성 공급자만 fixture |
| 결함 주입 | 선택한 2개 모두 탐지 | 인증 우회/예산 제거를 메모리에서 주입; 전체 mutation coverage 아님 |

공통 테스트가 중복되므로 표의 수를 단순 합산하지 않는다. 새 환경에서 requirements-test.txt만으로 설치한 뒤 Python 검사를 수행했다. SDK 설치 중 기존 공유 namespace가 교체되는 준비 오류는 clean venv로 해결했고 테스트 성공으로 세지 않았다. SDK 전환의 별도 baseline 검사도 이 준비 오류로 차단됐으며 신규 adapter 호환성 검사만 통과했다고 구분한다.

### TDD 근거

기존 권한 응답 200/기대 403, 액세스 토큰 갱신 200/기대 401, 누락 export, E5 좌표, SQL 37/기대 <=2, 거리 계산 30/기대 15의 기존 red/green을 보존했다. 이번에는 외부 호출 중 트랜잭션 활성 assertion 실패, Python 무인증 200/기대 401, 과대 본문 200/기대 413을 먼저 확인한 뒤 동일 검사로 통과했다. 도구/설치/소켓 준비 실패는 red가 아니다.

## 성능 측정

| 조건 | 전 | 후 | 해석 |
|---|---:|---:|---|
| 일정 12개 대표 장소 SQL | 37회 | 2회 | 쿼리 수 감소; 실제 Oracle 지연 개선률 아님 |
| 장소 20개/1일 최적화 중앙값 | 29.7416ms | 17.18165ms | AB/BA 각10회, 전체 출력 동일; fixture 실행시간 약42.2% 감소 |
| 같은 거리 계산 횟수 | 380회 | 190회 | 대칭 geodesic 중복 제거; 도로 소요시간과 구분 |
| 20ms 공급자 fixture 대기 중 DB 트랜잭션 점유 | 중앙값 26.7569ms | 0ms | 2회 warmup/5회 측정, 실제 서비스·저장소. 전체 DB 점유시간이 0이라는 뜻 아님 |
| 같은 생성 전체 중앙값 | 38.3077ms | 32.2987ms | 순차 JVM/소표본; 운영 향상 주장 없음 |
| 20개 장소 hot cache 조회 | 0.0007715ms | 0.1599515ms | 복사 비용 증가; 사전 허용 예산 +1ms 이내 |
| 실행 제한 fixture 중앙값 | 0.00762ms | 0.15148ms | 워커/제한 오버헤드 증가; 예산 base*2+2ms 이내 |

과거 JSON의 Python 3.13 표기는 실제 실행파일 확인 결과 3.12.14로 정정했다. 새 raw JSON에 정정 사실을 남긴다. 시간 감소를 전체 서비스 개선률로 일반화하지 않는다. 인증 HTTP 요청의 운영 비용/지연, Oracle 동시성·인덱스/실행계획, 실제 공급자 비용은 측정하지 않았다.

## 실행 명령·환경

- Windows, Java 17.0.12, Gradle 8.14, Python 3.12.14, Node 24.13.0.
- 각 Java 프로젝트: `gradlew.bat --no-daemon test`.
- Python: `python -m venv .venv`, 가상환경 Python으로 `pip install -r requirements-test.txt`, `python -m unittest discover -s tests`.
- 프론트 trip: `npm ci --ignore-scripts`, `node --test tests/*.test.mjs`, `node node_modules/vite/bin/vite.js build`.
- 로컬 인증: test profile Spring 서버를 127.0.0.1:18080에 임시 실행, Python TestClient에서 정상/무인증/refresh 오용/로그아웃 자격증명 확인. 검증 후 서버 종료. 운영 DB·파일은 변경하지 않았다.

## 배포 전 남은 확인 사항

1. Oracle TOKEN.TOKEN 길이 1024 이상, 스키마 소유자·실행계획·잠금/동시성을 실제 환경에서 검증해야 한다. SQL 가이드를 제공하지만 운영 SQL을 실행하지 않았다. 기존 무용도 토큰은 재로그인이 필요하다.
2. Python BACKEND_AUTH_URL을 실제 Spring 프로필 인증 URL로 설정한다. 기본값은 `http://127.0.0.1:8080/api/user/me`. CORS_ORIGINS와 프론트 /py 프록시를 실제 배포에 맞춘다. 제한은 프로세스 단위이므로 기본 운영 명령은 workers=1이다. 여러 인스턴스에서 전역 한도가 필요하면 공유 limiter가 필요하다.
3. GEMINI_API_KEY, SERPAPI_KEY, GOOGLE_MAPS_API_KEY는 격리 검증 환경에서 미설정, 배포 상태는 알 수 없다. 브라우저 VITE_GOOGLE_MAPS_API_KEY와 VITE_SALT/VITE_HASHIDS_MIN_LENGTH, Spring JWT/Oracle 설정도 실제 값 확인은 수행하지 않았다. 비밀값은 보고서·PR에 없다.
4. 실제 Google/Gemini/Serp 응답, 브라우저 로그인·갱신·지도·사진 표시를 아직 검증하지 않았다. 유료 호출/운영 자격증명 없이 통과했다고 대체 주장하지 않는다.
5. FastAPI TestClient의 httpx 경고와 프론트 bundle 크기 경고가 남는다. 테스트는 통과하며 이 경고가 해결됐다고 표시하지 않는다.

## 리뷰와 보존

요구사항 검토에서는 실제 호출 경로, 거절 시 부작용, 저장 실패 원자성, 설정 누락 시 동작을 확인했다. 유지보수성 검토에서는 공통 인증/예산 구현, 무제한 큐 방지, SDK 예외 전파, 캐시 복사를 별도로 확인했다. 같은 에이전트가 수행한 검토이며 독립 리뷰어의 승인으로 표현하지 않는다.

기존 사용자 변경과 미추적 파일은 원래 상태를 보존했고 PR에 포함하지 않았다. PR들은 초안·미병합이다. 증거는 각 PR docs/verification 및 로컬 artifacts의 JSON/로그에 보존한다.

