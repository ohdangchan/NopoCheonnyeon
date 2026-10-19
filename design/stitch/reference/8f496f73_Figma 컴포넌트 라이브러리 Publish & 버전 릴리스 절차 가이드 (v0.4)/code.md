# 노포 천년 미식가 — Figma 컴포넌트 라이브러리 Publish & 버전 릴리스 거버넌스 가이드 (v0.4)

> **프로젝트**: 노포 천년 미식가 (Nopo-Cheonnyeon Gourmet Passport)  
> **문서 목적**: Figma Team Library 및 Community 디자인 시스템 발행(Publish), 시맨틱 버저닝(SemVer), 변경 영향도 검증(Impact Analysis), Android Jetpack Compose 코드베이스와의 1:1 동기화 릴리스 프로토콜 및 비상 롤백 절차 규정  
> **버전**: v0.4 Production Release Archive  
> **대상자**: UI/UX 디자이너, 디자인 시스템 엔지니어, 안드로이드 개발팀

---

## 1. 컴포넌트 라이브러리 Publish 생명주기 및 릴리스 파이프라인

컴포넌트의 단순 수정부터 전사 프로덕션 배포까지 총 5단계 게이트웨이를 거칩니다.

```
[1. 로컬 작업 & 브랜치] ➔ [2. 사전 검수 게이트] ➔ [3. Team Library Publish] ➔ [4. 앱 파일 동기화] ➔ [5. Compose 코드 검증]
 (feat/component-v0.4)    (Auto-Layout, Variables)   (Release Note & Tag)     (Accept Updates)        (Android Studio Build)
```

### 5단계 상세 프로세스
1. **Branch 격리 작업**: 마스터(`v0.4-production-release`)에서 작업 브랜치(`feat/xxx` 또는 `fix/xxx`) 생성 후 컴포넌트 Variants 작업 진행.
2. **사전 검수 (Pre-flight QA)**: 하드코딩 색상 잔존 여부, 48dp 터치 타깃, 모바일 뷰포트(`390px`) 반응형 신축성 검증.
3. **라이브러리 정식 Publish**: 변경된 컴포넌트 선택, 변경 사유 및 마이그레이션 가이드를 포함한 릴리스 노트 작성 후 발행.
4. **소비 파일(Screens File) 업데이트 수락**: `Nopo-Cheonnyeon-App-Screens.fig`에서 `Updates available` 알림 확인 후 인스턴스 스왑 및 레이아웃 깨짐 검수.
5. **Jetpack Compose 1:1 토큰 매핑 커밋**: `core/designsystem` 내 Composable 파라미터 및 `Color.kt` 동기화 후 단위 테스트 `./gradlew testDebugUnitTest` 통과 확인.

---

## 2. 시맨틱 버저닝(Semantic Versioning) 정책 및 변경 영향도 매트릭스

Figma 라이브러리와 Android 코드베이스는 동일한 `vX.Y.Z` 버전을 공유하며 동기화됩니다.

| 변경 유형 | 버전 증가 예시 | Figma 라이브러리 변경 내용 | Android Jetpack Compose 영향도 | 조치 요구사항 |
|---|---|---|---|---|
| **Major (X.0.0)** | `v0.4.0` ➔ `v1.0.0` | • 컴포넌트 레이어 구조 전면 개편<br>• 기존 프로퍼티 삭제 및 파괴적 변경(Breaking Change)<br>• 디자인 토큰 컬렉션 아키텍처 재설계 | • Composable 함수 시그니처 변경<br>• 지원 중단(Deprecated) 파라미터 삭제<br>• Navigation 라우트 변경 | • 마이그레이션 가이드 필수 배포<br>• 브랜치 머지 전 양측 리드 승인 |
| **Minor (0.X.0)** | `v0.4.0` ➔ `v0.5.0` | • 신규 컴포넌트 추가 (`NaverMapClusterCell`)<br>• 기존 컴포넌트에 신규 배리언트 추가 (`TitleBadge` 6단계 칭호 추가)<br>• 하위 호환되는 신규 토큰 추가 | • 신규 Composable 추가<br>• 기존 Composable에 기본값(`default argument`)이 있는 파라미터 추가 | • 라이브러리 업데이트 즉시 수락 가능<br>• 기존 화면 레이아웃 영향 없음 |
| **Patch (0.4.X)** | `v0.4.0` ➔ `v0.4.1` | • 색상 헥스 미세 조정 (`color/stamp` 명도 개선)<br>• 텍스트 행간/자간 버그 수정<br>• 컴포넌트 내부 오토레이아웃 패딩 교정 | • `Color.kt`, `Type.kt` 토큰 값 단독 변경<br>• 레이아웃 수치 1~2dp 조정 | • 무중단 배포 및 즉각 동기화 |

---

## 3. 라이브러리 Publish 전 필수 체크리스트 (10대 무결점 게이트)

Figma 상단 `Publish library` 버튼을 누르기 전, 아래 10개 항목이 100% 충족되어야 합니다.

- [ ] **1. Variables 바인딩 100%**: 모든 Fill, Stroke, Text Color가 `color/sign`, `color/primary`, `color/stamp` 등 등록된 Variables에 연결되어 있는가? (하드코딩 헥스 0건)
- [ ] **2. Auto-Layout 무손실 신축성**: 너비 축소(`360px`) 및 확대(`430px`) 시 텍스트 래핑과 버튼 Hug/Fill 속성이 정상 동작하는가?
- [ ] **3. 터치 타깃 최소 안전 규격 (WCAG 48dp)**: 모든 버튼과 클릭 가능한 아이콘의 프레임이 최소 `48 × 48 px` 이상을 점유하고 있는가?
- [ ] **4. 컴포넌트 설명(Description) 및 문서 링크**: 컴포넌트 프로퍼티 패널에 기능 설명 및 Compose 대응 클래스명(`core/designsystem/component/...`)이 명시되었는가?
- [ ] **5. 비공개 에셋 접두사(`_` 또는 `.`) 적용**: 내부 빌딩 블록이나 서브 레이어는 외부로 노출되지 않도록 `_Atom/Circle` 형태로 네이밍되었는가?
- [ ] **6. 인스턴스 스왑 속성(Instance Swap) 검증**: `LeadingIcon`, `TrailingIcon` 등 아이콘 프로퍼티가 정상적으로 스왑 가능한 슬롯으로 등록되었는가?
- [ ] **7. 텍스트 프로퍼티 바인딩**: 버튼 라벨, 상단 타이틀 등의 문자열이 컴포넌트 레벨에서 바로 수정되도록 `Text Property`로 노출되어 있는가?
- [ ] **8. 3대 원형 인장(StampView) 업력 폰트**: `Black Han Sans` 폰트가 올바르게 렌더링되며 텍스트 중심 정렬이 완벽한가?
- [ ] **9. 다크 모드(Dark Mode) 대비비 검증**: `Tokens Studio` 또는 모드 스위칭 시 전 컴포넌트가 최소 4.5:1(WCAG AA) 명도 대비를 유지하는가?
- [ ] **10. 릴리스 노트 명세 완비**: 추가/수정/삭제된 배리언트 목록과 마이그레이션 주의사항이 작성되었는가?

---

## 4. 표준 릴리스 노트 작성 템플릿 (Figma Publish Note Standard)

Figma의 `Publish changes` 대화상자에는 아래 표준 마크다운 형식으로 변경 사항을 기재합니다.

```markdown
[Release] v0.4.0 — 노포 천년 미식가 프로덕션 마스터 컴포넌트 라이브러리 릴리스

🚀 신규 추가 (Added)
- StampView: 100년 이상 백년가게 전용 Gold Grade 배리언트 및 '百年' 각인 문양 추가
- TitleBadge: 새싹/단골/동네어른/장인/천년미식가 5대 식객 칭호 배지 컴포넌트 세트 신규 등록
- YearsGauge: 패스포트 상단 1,000년 세월 적립 게이지 카드(DashboardCard) 등록

🛠️ 개선 및 수정 (Changed & Fixed)
- NopoButton: 대형 CTA 터치 타깃 52px 고정 및 Pressed 상태의 98% 스케일 트랜지션 모션 정의
- NopoTopAppBar: 간판 청록(#17504A) 테마와 서피스 라이트(#F8F9F5) 모드 분리
- Color Variables: 다크 모드 고대비 텍스트(#E7EEEA) 명도 개선 (대비비 12:1 달성)

⚠️ 마이그레이션 주의사항 (Breaking Changes)
- 기존 단일 원형 인장 프레임을 사용하는 경우, Component Set `.components/StampView`의 `Grade` 및 `Size` 프로퍼티로 일괄 스왑 필요
- Compose 대응: core/designsystem/component/StampView.kt (v0.4 동기화 필수)
```

---

## 5. 비상 롤백 및 복구 프로토콜 (Emergency Rollback)

잘못된 컴포넌트 변경이 Publish되어 전체 51개 화면의 레이아웃이 깨지거나 Compose 빌드 에러가 발생한 경우의 비상 조치 매뉴얼입니다.

```
[장애 감지 (Layout Break)] ➔ [Figma 버전 히스토리 진입] ➔ [이전 안정 릴리스 복원] ➔ [Re-Publish] ➔ [Compose 긴급 핫픽스]
```

1. **버전 히스토리 탐색**: Figma 파일 메뉴 ➔ `File` ➔ `Version history` 클릭.
2. **직전 릴리스 복원점 확인**: 장애 발생 직전의 안정 버전(예: `v0.4.0 Stable Production`) 선택.
3. **`Restore this version` 실행**: 해당 안정 버전으로 캔버스를 롤백.
4. **Emergency Re-Publish**:
   - 릴리스 노트에 `[HOTFIX-ROLLBACK] v0.4.1 — Revert StampView breaking layout changes` 명시 후 재발행.
5. **팀 전체 전파 및 앱 화면 업데이트**: 앱 화면 파일에서 라이브러리 업데이트를 일괄 수락하여 51개 화면 즉시 정상화.

---

## 6. Figma 라이브러리 ➔ Jetpack Compose 릴리스 동기화 파이프라인

디자인 시스템 라이브러리가 정식 발행된 후, 안드로이드 개발팀이 진행하는 검증 체크리스트입니다.

```bash
# 1. 디자인 시스템 토큰 일치성 검증 (Color, Type, Shape)
./gradlew testDebugUnitTest --tests "com.nopo.cheonnyeon.core.designsystem.*"

# 2. 컴포넌트 Preview 스크린샷 렌더링 회귀 검증
./gradlew verifyRoborazziDebug

# 3. 51개 전체 화면 Composable 컴파일 및 릴리스 APK 패키징
./gradlew assembleRelease
```

---
*본 가이드는 노포 천년 미식가 프로젝트의 디자인 라이브러리와 안드로이드 코드베이스가 상호 불일치 없이 완벽한 단일 진실 공급원(Single Source of Truth)을 유지하도록 보장합니다.*
