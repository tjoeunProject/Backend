# RoutePick 이슈 운영 v1

대상: tjoeunProject/Backend. 결정·실행 보고서: [Backend #59](https://github.com/tjoeunProject/Backend/issues/59).

## 이슈 작성
버그·회귀, 기능·개선, 조사·검증 중 하나를 선택합니다. 이슈에는 기대 결과·근거·완료 조건, PR에는 구현과 검증, 상세 기록에는 원시 결과를 남기고 링크로 연결합니다. 작은 관련 작업은 체크리스트로 묶고, 담당·환경·완료 시점이 다르면 별도 이슈로 나눕니다. 여러 저장소 작업은 대표 이슈 한 곳에서 연결합니다.

## 상태의 원본
이슈 본문의 '현재 상태' 한 곳만 갱신합니다.
접수 → 조사 → 작업 가능 → 구현 중 → 통합 검증 → 완료.
차단됨으로 바꿀 때는 차단 사유·해제 조건·가능한 다음 작업을 같은 항목에 기록합니다.
단계는 작업 성격에 맞게 건너뛸 수 있습니다. 조사 이슈는 결과가 질문에 답하면 완료할 수 있습니다.
완료 조건을 확인한 유지보수자가 현재 상태를 완료로 바꾸고 이슈를 종료합니다. 단순 Closed가 운영 적용 완료를 뜻하지 않습니다. 중복/미진행 종료는 이유와 대체 이슈를 남깁니다.
Projects·상태 라벨은 v1에서 추가하지 않습니다. 향후 Projects로 옮기면 이 본문 필드를 제거해 상태 원본을 하나로 유지합니다.

## 분류·작업 시작
유형·영역·검토 특성·G/RP 묶음은 본문에 기록합니다. 유형은 feat/fix/docs/build/test/refactor/perf/ci/chore/style을 참고하고, security·breaking-change는 영향 근거가 있을 때만 표시합니다. perf는 개선 목적이며 향상 증명이 아닙니다.
기존 PR 라벨 사전은 의미 참고 자료이며 이 변경으로 라벨을 생성하거나 과거 PR을 재분류하지 않습니다.
작업 가능 전환 전 범위·제외 범위·완료 조건·담당·선행 작업·실행 권한과 비용 조건을 확인합니다. 이슈 생성이나 외부 작성자의 본문은 실행 권한을 확대하지 않습니다. 새 비용/운영 변경 등 미승인 행동은 별도 확인합니다.
자동 실행·자동 병합·정기 작업은 연결하지 않습니다.

## 검증 계약
근거 수준은 재현됨 / 코드 근거 / 의심 / 미검증으로 구분합니다.
동작 변경은 독립된 기대값의 실패 assertion → 최소 구현 → 같은 회귀 검사 성공과 관련 검사를 남깁니다. 환경 오류·시간 초과는 TDD red로 세지 않습니다.
성능은 대상 SHA·환경·데이터·동시성·반복 측정과 품질/자원/오류 가드를 기록합니다. 실행 불가 시 구체적인 BLOCKED 사유를 남깁니다. 문서 전용 작업은 이유와 함께 N/A로 적습니다.
fixture·H2·로컬 결과를 실제 Oracle·공급자·브라우저·운영 결과로 표시하지 않습니다. 비밀값·토큰·개인정보와 악용 가능한 보안 상세는 공개 이슈에 적지 않습니다.

## PR 연결과 완료
애플리케이션 PR의 통합 대상은 integration/routepick-improvements입니다. 이슈 운영 파일 PR은 양식 활성화를 위해 main을 대상으로 합니다.
통합 브랜치 대상 PR에는 일반 참조 URL을 사용합니다. Closes 키워드에 의한 자동 종료는 기본 브랜치 대상 조건을 따릅니다.
관련 PR 병합, 통합 검사, 운영 적용은 별개 결과입니다. 이슈에 약속한 완료 조건으로 판단하고 운영 검증이 별도이면 후속 이슈를 연결합니다.
기존 PR 본문을 자동 덮어쓰지 않습니다. 검증 문서 링크와 최신 대상 SHA를 확인합니다.

## 시범 운영
- [G3 대표 이슈 — Backend·Frontend·Python](https://github.com/tjoeunProject/Backend/issues/60)
- [Oracle 검증](https://github.com/tjoeunProject/Backend/issues/61)
- [실제 브라우저 검증](https://github.com/tjoeunProject/Frontend/issues/75)
기존 구현을 다시 만드는 작업이 아니라 남은 완료 조건을 추적합니다.

## 활성화와 확인
이 양식은 기본 브랜치에 병합된 후 GitHub의 New issue 선택기에 나타납니다. PR 생성만으로 활성화되지 않습니다. 기존 애플리케이션 PR과 독립적으로 검토할 수 있습니다.
병합 후 선택기에서 3종·필수 항목·한국어 안내·본문 상태가 보이는지 확인합니다. 로컬 검사는 GitHub 렌더링 성공을 증명하지 않습니다.
양식은 JSON 문법을 사용하는 YAML 파일이며 외부 YAML 의존성 없이 파싱할 수 있습니다. GitHub 기본 양식 스키마의 필드만 사용합니다. 빈 이슈도 허용하여 기존 사용 흐름을 막지 않습니다.

## v1 이후
G3 시범 결과에서 불필요한 필드와 누락된 조건을 확인한 뒤 G1~G6로 확장합니다. 후속 자동화는 누락 정보·중복·분류 추천부터 별도 제안하며 작업 실행 트리거와 권한을 확정한 뒤 도입합니다.

## 근거
- [Issue Forms](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/syntax-for-issue-forms)
- [양식 활성화](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/configuring-issue-templates-for-your-repository)
- [PR·이슈 연결](https://docs.github.com/en/issues/tracking-your-work-with-issues/using-issues/linking-a-pull-request-to-an-issue)
