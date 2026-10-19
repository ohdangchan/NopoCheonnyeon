# 노포 천년 미식가 — 서버·DB 설정 가이드 (Firebase) v0.4

> 최우선 목표는 APK를 폰에 설치했을 때 모든 기능이 버그 없이 동작하는 것입니다. 계정과 서버는 처음부터 실제 Firebase로 연결해, 가입·평점·프로필을 실기기에서 그대로 확인합니다.

이 앱의 서버는 Firebase 하나입니다. **로그인(Authentication)** 과 **데이터베이스(Cloud Firestore)**, **크래시 수집(Crashlytics)** 만 씁니다. 노포 카탈로그는 앱 안에 들어 있고, 서버에는 유저가 만든 데이터(계정·방문·리뷰)만 저장합니다. 무료 요금제(Spark)로 시작합니다.

소요 시간: 약 30분. 준비물: Google 계정, 패키지명 `com.nopo.cheonnyeon`.

---

## 1. 프로젝트 만들기
1. https://console.firebase.google.com 에서 **프로젝트 추가** → 이름 `nopo-cheonnyeon` (아무 이름이나 가능)
2. Google 애널리틱스는 켜도 되고 꺼도 됩니다 (Crashlytics만 쓸 거면 꺼도 됨)

## 2. Android 앱 등록
1. 프로젝트 개요 → Android 아이콘
2. **Android 패키지 이름**: `com.nopo.cheonnyeon` (앱의 `applicationId`와 정확히 같아야 함)
3. 디버그 서명 SHA-1은 이메일 로그인만 쓰면 없어도 됩니다. 나중에 구글 로그인을 붙일 때 `./gradlew signingReport`로 확인해 추가
4. **`google-services.json` 다운로드** → Android Studio 프로젝트의 `app/` 폴더에 넣기
5. Gradle 설정은 Claude Code에게 맡깁니다 (S-006 / SPK-5)

> 콘솔 메뉴 이름과 위치는 개편 때마다 바뀝니다. 아래에서 메뉴를 못 찾으면 콘솔 위쪽 검색창에 제품 이름(Authentication, Firestore, Crashlytics)을 입력하세요.

## 3. 로그인(Authentication)
1. 왼쪽 메뉴에서 **Authentication** → **시작하기**
2. 로그인 방법 → **이메일/비밀번호** → 사용 설정 → 저장 (이메일 링크 로그인은 끔)
3. 템플릿 → 비밀번호 재설정 메일의 언어를 한국어로 바꾸면 테스트 사용자가 덜 헷갈립니다

## 0. 이 저장소의 실제 연결 정보 (2026-10-19 설정)
| 항목 | 값 |
|---|---|
| 계정 | songpa24@iceu.kr |
| 프로젝트 ID | `nopocheonnyeon` (표시 이름 NopoCheonnyeon, `firebase/.firebaserc`) |
| Android 앱 | `com.nopo.cheonnyeon`, App ID `1:401283218421:android:1cf2a80d59a68bf853bebc` |
| 설정 파일 | `app/google-services.json` (git 제외, `firebase apps:sdkconfig ANDROID <App ID> -o app/google-services.json`로 다시 받기) |
| Firestore | `(default)`, **asia-northeast3 (서울)**, Standard |

> ⚠️ **데이터베이스를 만들기 전에 `firebase deploy --only firestore`를 실행하지 않는다.** 데이터베이스가 없으면 CLI가 자동으로 만드는데, 위치가 **nam5(미국)**로 정해지고 나중에 바꿀 수 없다(2026-10-19 실제로 발생 → 삭제 후 재생성, 같은 ID는 삭제 5분 뒤 재사용 가능).
> 순서: `firebase firestore:databases:create "(default)" --location asia-northeast3` → 위치 확인(`firebase firestore:databases:get "(default)"`) → `firebase deploy --only firestore` (프로젝트 루트가 아니라 `firebase/` 폴더에서 실행)

## 4. 데이터베이스(Cloud Firestore)
1. 왼쪽 메뉴에서 **Firestore Database** → **데이터베이스 만들기** (에디션을 고르라고 하면 **Standard**, 데이터베이스 ID는 기본값 `(default)`)
2. 위치: **`asia-northeast3` (서울)** — 한 번 정하면 바꿀 수 없음
3. **프로덕션 모드**로 시작
4. **규칙** 탭 → 패키지의 `firebase/firestore.rules` 내용을 전부 붙여 넣기 → **게시**
5. **색인** 탭 → 복합 색인 추가 2개

| 컬렉션 | 필드 1 | 필드 2 | 범위 |
|---|---|---|---|
| `reviews` | `storeId` 오름차순 | `updatedAt` 내림차순 | 컬렉션 |
| `reviews` | `uid` 오름차순 | `updatedAt` 내림차순 | 컬렉션 |

> 색인을 빠뜨리면 앱 로그에 `FAILED_PRECONDITION ... create it here: https://console.firebase.google.com/...` 링크가 나옵니다. 그 링크를 열면 같은 색인이 만들어집니다.

## 5. 크래시 수집(Crashlytics)
1. 왼쪽 메뉴에서 **Crashlytics** → **사용 설정**
2. 앱에 Crashlytics 플러그인이 들어간 뒤 첫 실행(또는 테스트 크래시) 이후 대시보드가 보입니다

## 6. (선택) 명령줄로 규칙·색인 배포
Node.js가 있으면 콘솔에 붙여 넣는 대신 명령으로 배포할 수 있습니다.
```bash
npm install -g firebase-tools
firebase login
cd firebase
firebase use --add            # 만든 프로젝트 선택
firebase deploy --only firestore:rules,firestore:indexes
```

## 7. 규칙이 제대로 막는지 확인 (규칙 플레이그라운드)
Firestore → 규칙 → **규칙 플레이그라운드**에서 아래를 시험합니다 (`nopo-04`의 I-1~I-10).

| 시험 | 설정 | 기대 |
|---|---|---|
| 남의 프로필 수정 | update `/users/OTHER`, 인증 uid=`ME` | 거부 |
| 업력 29 방문 | create `/users/ME/visits/BN-1`, `yearsAwarded: 29` | 거부 |
| 미방문 가게 리뷰 | create `/reviews/BN-2_ME` (visits/BN-2 없음) | 거부 |
| 영수증 해시 재사용 | 같은 `/receiptHashes/H`에 두 번째 쓰기 | 거부 |
| 비로그인 닉네임 확인 | get `/nicknames/hong`, 인증 없음 | 허용 |
| 남의 진단 문서 | write `/diagnostics/OTHER`, 인증 uid=`ME` | 거부 |

## 8. 데이터 구조 요약
```
users/{uid}                     nickname, nicknameLower, titleId, totalYears, visitCount, reviewCount, createdAt, updatedAt
users/{uid}/visits/{storeId}    storeId, storeName, sigungu, yearsAwarded, grade, method, photoVerified, firstVisitedAt, lastVisitedAt, visitCount
nicknames/{소문자닉네임}          uid
reviews/{storeId}_{uid}         storeId, storeName, uid, nickname, titleId, rating, text, visitCount, createdAt, updatedAt
receiptHashes/{hash}            uid, storeId, createdAt
reports/{autoId}                reviewId, reason, reporterUid, createdAt   (P1)
diagnostics/{uid}               1일차 기능 검증용 쓰기·읽기 시험 문서
```
- 방문 문서의 `firstVisitedAt`·`lastVisitedAt`과 `receiptHashes.createdAt`은 서울 기준 그날 0시로 잘라 저장합니다(인증 시각의 시:분은 서버에 남기지 않음).

## 9. 무료 요금제 한도와 설계
- Spark 요금제 Firestore 무료 한도는 하루 **읽기 5만, 쓰기 2만** (Firebase 요금제 문서 기준). 한도를 넘으면 그달 말까지 해당 제품이 멈춥니다.
- 그래서 노포 카탈로그(1,000곳 이상)는 서버에 두지 않고 앱에 넣었습니다. 지도를 열 때마다 1,000번씩 읽으면 테스트 몇십 번에 한도가 찹니다.
- 가게 평균 평점은 집계 쿼리(개수·평균)로 계산합니다. 리뷰 문서를 전부 내려받지 않습니다.
- Cloud Functions(서버 판정)는 종량제(Blaze)가 필요해 이번 버전에서는 쓰지 않습니다.

## 10. 테스트 데이터 정리
- Authentication → 사용자 탭에서 테스트 계정 삭제
- Firestore → 데이터 탭에서 `users/{uid}`, `nicknames/{이름}`, 해당 유저의 `reviews` 문서 삭제
- 실제 사용자 데이터가 쌓이기 전, QA 마지막 날 한 번 비우는 것을 권장
