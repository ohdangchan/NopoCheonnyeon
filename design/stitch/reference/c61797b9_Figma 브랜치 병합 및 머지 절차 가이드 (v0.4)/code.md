# 노포 천년 미식가 — Figma 브랜치(Branch) 생성·리뷰·충돌 해결 및 메인 머지(Merge) 절차 가이드 (v0.4)

> **프로젝트**: 노포 천년 미식가 (Nopo-Cheonnyeon Gourmet Passport)  
> **기준 문서**: `DESIGN.md` (v0.4 Production Release Architecture), `Figma 컴포넌트 라이브러리 종합 검수 보고서`  
> **문서 목적**: Figma Organization/Enterprise 환경에서의 브랜칭(Branching) 생명주기, 변경 영향도 리뷰(Review Changes), 컴포넌트·토큰 충돌 해결(Conflict Resolution), 메인 브랜치 머지(Merge into Main) 및 Team Library Re-Publish 거버넌스 규격 제공  
> **버전**: v0.4 Production Release Archive  
> **대상자**: UI/UX 디자이너, 디자인 시스템 엔지니어, 안드로이드 개발팀

---

## 1. Figma 브랜칭(Branching) 아키텍처 및 생명주기 총괄

51개 전체 화면과 109종 컴포넌트 Variants의 안정성을 보장하기 위해, 메인 브랜치(`Main`)에 직접 수정을 가하지 않고 **독립된 기능 브랜치(Branch)에서 작업 후 승인 머지**하는 5단계 Git-Flow형 파이프라인을 운영합니다.

```
[1. Main 파일] ── 브랜치 생성 ──▶ [2. Feature 브랜치 작업] ── 자체 QA ──▶ [3. Review 요청]
 (v0.4 Production)              (feat/map-cluster-v0.5)                 (디자이너 + 개발 리드)
                                                                                  │
                                                                         [승인 및 충돌 해결]
                                                                                  ▼
[5. Team Library Re-Publish] ◀── 1:1 토큰 동기화 ◀── [4. Main 머지 완료 (Merge)]
 (앱 화면 파일에 업데이트 배포)   (./gradlew syncDesignTokens)      (Squash & Merge 기록 보존)
```

---

## 2. 표준 브랜치 네이밍 컨벤션 (Branch Naming Standards)

브랜치명은 작업 목적과 컴포넌트 대상을 명확히 식별할 수 있도록 아래 접두사 규칙을 엄격히 준수합니다.

| 접두사 (Prefix) | 사용 목적 | 브랜치 네이밍 예시 | 대응 도메인 / 컴포넌트 |
|---|---|---|---|
| `feat/` | 신규 컴포넌트, 배리언트, 화면 추가 | `feat/map-marker-clustering`<br>`feat/stamp-gold-ceremony` | 100년 도장 문양, 네이버 지도 마커 |
| `fix/` | 기존 컴포넌트 레이아웃, 오토레이아웃 버그 교정 | `fix/nopo-button-touch-target`<br>`fix/store-card-text-wrap` | 48dp 터치 타깃, 카드 텍스트 래핑 |
| `token/` | `nopo-tokens.json` 컬러/치수 토큰 값 갱신 | `token/color-stamp-dark-contrast`<br>`token/spacing-grid-update` | WCAG 명도 대비비 개선, 그리드 미세조정 |
| `docs/` | 가이드라인, 커버, 핸드오프 주석 수정 | `docs/design-system-spec-v0.5` | 피그마 캔버스 문서 보강 |
| `hotfix/` | 프로덕션 Compose 빌드 깨짐 긴급 롤백 | `hotfix/revert-stamp-variant-props` | 파괴적 변경(Breaking Change) 긴급 복원 |

---

## 3. 브랜치 머지 전 사전 점검 게이트 (10대 머지 체크리스트)

피그마 상단의 `Review and merge changes` 버튼을 누르기 전, 브랜치 작업자가 반드시 검증해야 하는 10대 무결점 체크리스트입니다.

- [ ] **1. Variables 바인딩 100%**: 신규/수정된 모든 레이어에 하드코딩 헥스(`Hex`)가 0건이며 `color/sign`, `color/primary` 등 Variables에 연결되어 있는가?
- [ ] **2. 390px 뷰포트 신축성**: 너비를 소형(`360px`)부터 대형(`430px`)까지 리사이징했을 때 Auto-Layout 깨짐이 없는가?
- [ ] **3. 최소 48dp 터치 타깃 준수**: 모든 버튼(`NopoButton`)과 아이콘 터치 박스가 `48 × 48 px` 이상을 확보하고 있는가?
- [ ] **4. 컴포넌트 프로퍼티 명명 일치**: Variant 프로퍼티명(`Grade`, `Size`, `State`)이 `core/designsystem` Compose 코드와 1:1 일치하는가?
- [ ] **5. 비공개 에셋 접두사(`_` 또는 `.`)**: 내부 하위 서브 레이어는 라이브러리 오염 방지를 위해 `_Atom/Circle` 형태로 명명되었는가?
- [ ] **6. 텍스트/인스턴스 스왑 속성**: 컴포넌트 내 텍스트와 아이콘이 상위에서 바로 변경 가능한 프로퍼티로 노출되어 있는가?
- [ ] **7. 다크 모드(Dark Mode) 대비비**: 다크 모드 전환 시 최소 4.5:1 (본문 12:1 이상) WCAG 명도 대비를 유지하는가?
- [ ] **8. 메인 변경사항 동기화 (Update from Main)**: 작업 도중 메인 브랜치에 반영된 변경 사항을 `Update from main`으로 사전 동기화했는가?
- [ ] **9. 미사용 테스트 프레임 정리**: 작업 중 생성한 임시 스케치나 캔버스 찌꺼기 프레임을 전수 삭제했는가?
- [ ] **10. 머지 코멘트 작성 완비**: 변경 사유, 수정 컴포넌트 목록, Compose 영향도를 포함한 릴리스 노트를 작성하였는가?

---

## 4. 단계별 브랜치 머지 실행 프로토콜 (Step-by-Step Guide)

### 4.1 [1단계] 메인 브랜치 최신화 (`Update from main`)
브랜치 작업 도중 다른 디자이너가 메인에 반영한 작업이 있을 수 있으므로, 머지 전 반드시 메인의 최신 변경 사항을 브랜치로 끌어옵니다:
1. 브랜치 파일 우측 상단 드롭다운 메뉴 또는 파일명 옆 화살표 클릭.
2. **`Update from main...`** 클릭.
3. 메인의 최신 컴포넌트와 토큰이 브랜치에 안전하게 동기화되었는지 확인.

---

### 4.2 [2단계] 리뷰 요청 및 변경 비교 (`Review and merge changes`)
1. 우측 상단의 파란색 **`Review and merge changes...`** 버튼 클릭.
2. 좌측 패널의 **변경 인벤토리(Change Log)** 확인:
   - `Added`: 신규 추가된 컴포넌트/토큰
   - `Modified`: 수정된 컴포넌트/레이어
   - `Deleted`: 삭제된 컴포넌트/레이어
3. 우측 비주얼 뷰어에서 **`Side by side` (양옆 대조)** 또는 **`Overlay` (겹쳐보기)** 모드로 기존 메인과 브랜치의 레이아웃 차이를 1:1 픽셀 검증.
4. **리뷰어 지정**: 디자인 시스템 리드 및 안드로이드 담당 엔지니어를 `Reviewers`에 태그하고 머지 요청 등록.

---

### 4.3 [3단계] 컴포넌트 충돌 해결 (Conflict Resolution Protocol)
메인과 브랜치 양쪽에서 동일한 컴포넌트의 동일한 프로퍼티나 레이어를 수정한 경우 **충돌(Conflict)**이 감지됩니다.

```
[충돌 감지 (Conflict Detected)]
  ├── Option A: Use main version (메인 브랜치 내용 유지 - 브랜치 수정사항 폐기)
  └── Option B: Use branch version (브랜치 작업본 적용 - 메인 덮어쓰기)
```

- **충돌 해결 원칙**:
  - **디자인 토큰 충돌**: `DESIGN.md` 명세서의 최신 헥스 코드를 기준으로 판정.
  - **컴포넌트 세트 충돌**: Compose 코드와 호환되는 최신 Variants 구성을 유지하는 `Use branch version`을 우선 채택.
- 충돌 해결 후 **`Resolve conflicts`**를 눌러 해결 완료 상태로 전환.

---

### 4.4 [4단계] 메인 머지 확정 (`Merge branch`)
1. 리뷰어의 최종 승인(`Approved`) 확인 후, **`Merge branch`** 파란색 버튼 클릭.
2. **머지 커밋 메시지 표준 규격** 작성:
   ```markdown
   Merge: feat/stamp-gold-ceremony (v0.4.1)
   
   - 백년가게 100년 황금 인장 3중 동심원 및 百年 각인 레이어 고도화
   - TitleBadge 5단계 식객 칭호 컴포넌트 오토레이아웃 패딩 교정 (WCAG 48dp 터치 타깃 완비)
   - Compose 매핑: core/designsystem/component/StampView.kt 파라미터 동기화
   ```
3. 확인 버튼을 누르면 브랜치 내용이 메인(`Main`)으로 원자적(Atomic) 병합 완료되며 브랜치는 자동 아카이브 처리됩니다.

---

### 4.5 [5단계] Team Library 재발행 & Compose 자동 동기화
메인 머지가 완료된 즉시 디자인 시스템을 소비하는 앱 화면 및 코드베이스에 전파합니다:
1. **Figma Team Library Publish**: 메인 파일에서 `Publish changes`를 실행하여 팀 전체에 라이브러리 발행.
2. **소비 화면 업데이트**: `Nopo-Cheonnyeon-App-Screens.fig`에서 `Update all` 수락.
3. **Android Studio 코드 자동 동기화**:
   ```bash
   # 터미널에서 Figma 토큰 동기화 Gradle 태스크 실행
   ./gradlew syncDesignTokens
   
   # 도메인 단위 테스트 전수 검증
   ./gradlew testDebugUnitTest
   ```

---

## 5. 비상 롤백(Rollback) 및 복구 절차

잘못 머지된 컴포넌트로 인해 전체 화면 레이아웃이 깨지거나 Compose 빌드 에러가 발생한 경우의 비상 조치 매뉴얼입니다.

1. 메인 파일 메뉴 ➔ **`File` ➔ `Version history`** 클릭.
2. 머지 직전의 안정적인 복원점(예: `v0.4.0 Production Release Master`) 선택.
3. 마우스 우클릭 ➔ **`Restore this version`** 클릭하여 즉시 롤백 복원.
4. `[HOTFIX] Revert branch merge` 타이틀로 라이브러리를 긴급 Re-Publish하여 51개 화면 즉각 정상화.

---
*본 가이드는 노포 천년 미식가 프로젝트의 디자인 브랜치 관리와 메인 병합 절차가 안드로이드 코드베이스와 오차 없이 안전하게 동기화되도록 보장합니다.*
