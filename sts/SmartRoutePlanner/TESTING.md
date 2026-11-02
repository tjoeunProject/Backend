# Java 테스트 실행

Java 17을 설치하고 JAVA_HOME을 해당 JDK로 설정한다. 프로젝트 Wrapper는 Gradle 8.14를 사용하며 공식 배포판 SHA-256을 고정한다. 최초 실행에는 Gradle/Maven 의존성 다운로드가 필요하다.

SmartRoutePlanner 디렉터리에서 실행한다.

```text
# Windows
gradlew.bat --version
gradlew.bat --no-daemon test

# Linux/macOS (별도 환경 검증은 미실행)
./gradlew --version
./gradlew --no-daemon test
```

## 테스트 환경

SmartRoutePlannerApplicationTests는 @ActiveProfiles("test")로 src/test/resources/application-test.properties를 읽는다. H2는 testRuntimeOnly 의존성이며 운영 애플리케이션에 포함되지 않는다. 테스트는 메모리 DB를 생성·종료하고 각 메서드의 트랜잭션을 롤백한다.

이 프로필의 JWT/Hashids/API 설정은 고정된 테스트 fixture다. 실제 키가 아니며 배포에 사용하면 안 된다. 테스트 파일은 테스트 소스 집합에만 있고 기본 실행 프로필이나 운영 설정을 변경하지 않는다.

검증 항목:

1. Spring 컨텍스트가 기동하고 실제 연결 대상이 H2 메모리 DB인지 확인.
2. 사용자 엔티티를 저장하고 이메일로 다시 조회할 수 있는지 확인.

Oracle 및 외부 API에 연결하지 않는 기본 검증이다. Oracle 고유 SQL/스키마와 실제 공급자 통합이 검증됐다는 뜻은 아니다. 인증·소유권·직렬화 회귀 테스트는 후속 기능 PR에서 추가한다.

## 결과 확인

- build/reports/tests/test/index.html: 사람이 읽는 결과.
- build/test-results/test/TEST-*.xml: 실제 테스트 개수·실패·skip·시간.
- 실패한 첫 실행은 Wrapper 설정 부재, 다음 실행은 datasource 설정 부재였다. 이를 애플리케이션 assertion 재현이나 통과로 보고하지 않는다.

2026-10-30 Windows/JDK 17.0.12에서 Wrapper 기동 계약 검사와 Git ignore 규칙 검사를 통과했고, Gradle의 실제 테스트 2개가 통과했다. 후보 빌드 실행은 26초였으나 기존 상태에서 성공하는 빌드를 만들 수 없어 전후 성능 비교는 BLOCKED다. 캐시 상태·컴파일 범위가 다른 실패/성공 실행 시간을 향상률로 계산하지 않는다. 이번 변경에는 운영 애플리케이션 코드 변경이 없다.

## 설정 파일 추적

루트 .gitignore는 Wrapper 설정과 application-test.properties만 명시적으로 추적 허용한다. 로컬 application.properties와 .env 무시 규칙은 유지한다. 테스트 결과·Gradle 캐시·운영 설정을 PR에 올리지 않는다.

참고:

- [Spring Boot 3.3 빌드 도구 요구사항](https://docs.spring.io/spring-boot/3.3/system-requirements.html)
- [Gradle 배포판 체크섬](https://gradle.org/release-checksums/)
