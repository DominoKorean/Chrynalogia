# Chrynalogia

삼성 공식 카메라 위에 구도 그리드를 표시하는 Android 앱입니다. 실제 `camera_preview`의 접근성 좌표를 읽으므로 촬영 비율과 모드가 바뀌어도 미리보기 영역을 따라갑니다. Galaxy S23 / One UI 8.5에서 개발·검증합니다.

## 사용

첫 실행에서 **항상 켜기** 또는 **수동으로 켜기**를 선택합니다. 다른 앱 위에 표시와 접근성을 허용한 뒤 삼성 카메라를 엽니다.

- 항상 켜기: 접근성 연결이 유지되면 카메라에서 자동으로 표시합니다.
- 수동으로 켜기: 앱을 열면 실행하고, 실행 중 알림과 끄기 버튼을 제공합니다. 알림 권한이 필요합니다. 재부팅하거나 알림 서비스를 종료하면 수동 실행은 끝납니다.
- 실행을 꺼도 Android가 관리하는 접근성 연결 자체는 유지됩니다. 꺼진 동안 카메라 탐색과 주기 검사는 멈춥니다. 접근성 연결까지 해제하려면 시스템 설정에서 끕니다.

카메라 왼쪽 위의 앱 아이콘을 누르면 그 아래의 별도 둥근 패널 안에서 숨기기, 90° 회전, 좌우·상하 반전, 설정이 세로 메뉴로 펼쳐집니다. 가로 화면에서는 왼쪽 아래에 표시합니다. 앱 끄기 버튼은 설정에서 활성화할 수 있고 수동 모드에서는 항상 표시됩니다. 설정은 카메라 위 팝업으로 열리며 변경은 즉시 적용·저장됩니다. 닫기를 누르면 카메라로 돌아갑니다. 다른 앱·알림창·잠금·촬영 미리보기가 없는 화면에서는 오버레이를 숨깁니다.

## 그리드와 라인

3분할법, 중앙 십자선, 황금비 격자, 황금 나선, 대각선 X자, 대각선법, 황금 삼각형, 라바트망, 동적 대칭을 제공합니다. 각 항목에 미리보기와 설명이 있습니다. 별도 커스텀 직각 격자는 가로·세로 1~16칸으로 조절합니다. 여러 그리드를 겹칠 수도 있습니다.

기본 선은 흰색, 1.2dp, 불투명도 90%, 아웃라인 없음입니다. 불투명도 0~100%, 두께 0.3~6dp, HEX 색상과 터치 색상 선택기, 자동 대비 아웃라인을 조절합니다. 노란색·흰색·검은색 프리셋은 아웃라인 없이 적용됩니다. 황금 나선은 형태를 보존하도록 미리보기 안에 맞춥니다. 회전 전 대각선법은 실제 화면에서 45°로 그리고 삼각형의 보조선은 대각선에 수직입니다. 회전하면 구성 방향을 바꾼 뒤 프레임에 맞춥니다.

UI는 사용자 Figma의 두 열 카드, 3개 하단 탭, 색상·라인 카드 배치를 기반으로 합니다. One UI 키트의 색상·글자 크기·비율을 적용하고 키트 Icons 페이지의 SVG 원본을 사용합니다. 설계 기준은 DESIGN.md에 기록했습니다. 설정 탭에서 언어(한국어·영어·시스템 기본값)와 앱 테마(밝게·어둡게·시스템 기본값)를 바꿀 수 있습니다. 아웃라인은 라인색과 대비가 더 높은 흰색 또는 검은색으로 자동 선택됩니다. 삼성의 비공개 UI 컴포넌트를 사용하는 것은 아닙니다. 앱 아이콘은 제공된 `logo/logo.svg`를 렌더링한 이미지입니다.

## 커스텀 파일

시스템 파일 선택기로 SVG 선 또는 PNG·JPEG·WebP를 가져옵니다. 저장소 전체 권한 없이 선택한 파일만 읽고 앱 내부에 복사합니다.

- 기본은 미리보기에 맞춰 늘리기이며, 비율 유지 토글로 가운데 맞춤할 수 있습니다.
- SVG는 단색 선으로 표시합니다. 선 두께·색·아웃라인이 적용됩니다. 원본의 채우기·선 색·두께는 가져올 때 제거해 앱 설정이 우선하도록 변환합니다. UTF-8 BOM을 지원합니다. `viewBox` 또는 양수 `width`·`height`가 필요합니다. XML 엔티티·DOCTYPE 선언이 있는 파일은 설명과 함께 거부합니다. 필터·애니메이션 등 AndroidSVG가 지원하지 않는 기능은 제외됩니다.
- 비트맵은 단색으로 변환하며 선 두께의 영향을 받지 않습니다. 투명 PNG는 알파를 유지하고, 불투명 이미지는 모서리 밝기를 기준으로 밝은/어두운 배경을 제거합니다. 투명 배경 PNG가 가장 예측 가능합니다.
- 파일당 4MB, 최대 12개입니다. 비트맵은 긴 변이 최대 1024픽셀 수준이 되도록 샘플링합니다. 추가한 파일은 앱에서 삭제할 수 있습니다.
- 카메라 팝업에서 가져오면 시스템 파일 선택기로 전환했다가 카메라로 복귀합니다.

## 빌드·설치

compile/target SDK 37, min SDK 34, AGP 9.1.1, Gradle 9.3.1, JDK 17 이상입니다. Java와 Android 플랫폼 UI, AndroidSVG 1.4를 사용합니다. SDK와 Gradle 캐시는 `.tools` 아래에 둡니다.

```powershell
.\scripts\Bootstrap.ps1
.\scripts\Build.ps1
.\scripts\Install.ps1
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. 기존 프로토타입 위에 설치하면 접근성·오버레이 권한을 유지하고 첫 실행 선택 화면을 표시합니다. 삼성의 사이드로드 접근성 제한 안내가 나오면 기기에서 제한된 설정을 허용합니다.

## 검사·디버깅

USB 디버깅을 허용한 S23 한 대를 연결하고 테스트 중에는 휴대폰 조작을 잠시 멈춥니다. 아래 UI 검사는 사진이나 동영상을 촬영하지 않습니다. 메뉴 이름은 한국어 One UI 기준입니다.

```powershell
.\scripts\OneUI-Test.ps1
.\scripts\Redesign-Appearance-Test.ps1
.\scripts\Redesign-Line-Test.ps1
.\scripts\Ratio-Test.ps1
.\scripts\Mode-Test.ps1
.\scripts\Zoom-Test.ps1
.\scripts\Quick-Controls-Test.ps1
.\scripts\Smoke-Test.ps1
.\scripts\Screen-Size-Test.ps1
.\scripts\Collect-Diagnostics.ps1
```

OneUI-Test는 키트 스위치·슬라이더의 터치, HEX, 탭 초기 스크롤, 직각 격자의 예약 공간, 앱 로고와 메뉴의 고정 위치, 라바트망 회전을 검사합니다. S23의 현재 420dpi 배율과 한국어 UI를 기준으로 합니다. Redesign-Appearance-Test는 카메라 팝업의 언어·테마와 수동 종료 상태를, Redesign-Line-Test는 터치 색상 선택기와 라인 슬라이더를 검사합니다. 이전 Feature/Style/Session/Lifecycle/Import 스크립트는 v0.9 레이아웃에 대응하므로 새 탭 UI에 그대로 실행하지 않습니다. 기존 카메라 검사는 4개 비율, 12개 촬영 대기 모드, 줌·퀵 컨트롤·화면 전환을 다룹니다. Screen-Size-Test는 S23의 논리 화면 크기와 배율을 변경한 뒤 원래 값으로 복원합니다. 이는 실제 다른 기종의 검증을 대신하지 않습니다. scrcpy의 영상 축소는 앱 레이아웃 크기를 변경하지 않습니다.

기하·대비 색 검사 6개는 Build 스크립트에 포함됩니다. 실제 Android 렌더링·파일 검사는 별도 테스트 APK로 실행합니다.

```powershell
.\gradlew.bat :app:assembleDebugAndroidTest
& .\.tools\android-sdk\platform-tools\adb.exe install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& .\.tools\android-sdk\platform-tools\adb.exe shell am instrument -w dev.chrynalogia.probe.test/dev.chrynalogia.probe.RenderChecks
```

렌더링 검사는 사용자 설정을 복원하고 테스트 파일을 삭제하지만 Android가 테스트 종료 때 앱 프로세스를 중단하므로 접근성 서비스를 다시 연결해야 할 수 있습니다. 시스템 설정의 Chrynalogia 스위치만 껐다 켜거나 `Restore-Accessibility.ps1`을 사용합니다. 이 도우미는 기존에 허용한 Chrynalogia만 재연결하는 한국어 S23 테스트용입니다.

진단은 앱 내부 순환 JSONL, 상태 JSON, 카메라 UI 트리와 앱 UI 트리로 기록합니다. ADB 진단 스크립트는 화면·시스템 상태를 `artifacts`에 저장합니다. 촬영 대상과 UI 텍스트가 포함될 수 있으므로 `.gitignore`에서 제외합니다. 앱에는 카메라·마이크·네트워크 권한이 없습니다.

디버그 APK의 명령 수신기는 `android.permission.DUMP`로 보호되며 접근성 연결이 필요합니다. `capture`, `pause`, `resume`, `bounds`, `reset-bounds`를 지원합니다. 그리드와 버튼은 별도 접근성 오버레이 창이며 그리드 창은 `NOT_TOUCHABLE | NOT_FOCUSABLE`입니다. 자동 ADB 터치 검사는 실제 손가락의 핀치·초점·셔터 전달을 대신하지 않습니다.

## 참고

- [접근성 전체 노드 포함](https://developer.android.com/reference/android/accessibilityservice/AccessibilityServiceInfo#FLAG_INCLUDE_NOT_IMPORTANT_VIEWS)
- [오버레이와 터치 전달](https://developer.android.com/reference/android/view/WindowManager.LayoutParams#FLAG_NOT_TOUCHABLE)
- [수동 실행의 foreground service 타입](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use)
- [AndroidSVG 기능과 라이선스](https://bigbadaboom.github.io/androidsvg/)
- [scrcpy 공식 문서](https://github.com/Genymobile/scrcpy)


## v0.9 검증 결과

Galaxy S23 / One UI 8.5에서 기하 단위 검사 4개, 실제 Android 렌더링·파일·수명 검사 28개, 메뉴 검사 24개, 입력·알림·화면 재생성·파일 선택기 각 5개가 통과했습니다. 네 촬영 비율, 12개 촬영 대기 모드, 줌·퀵 컨트롤·화면 전환 7개, 논리 화면 3개 조건의 12개 비율 검사도 통과했습니다. 전체 집계는 `artifacts/validation-summary.json`입니다.

검사 후 1080×2340 / 기존 배율 override 420dpi / 자동 회전 설정으로 복원했습니다. 앱은 항상 켜기, 흰색 황금비 격자, 두께 1.2dp, 불투명도 90%, 아웃라인 없음으로 두었습니다. 테스트용 가져오기 파일과 별도 테스트 APK는 정리했습니다. 빌드·lint는 오류 없이 통과했고 Gradle 최신 버전 안내 경고 1개가 있습니다.

실제 다른 기종과 녹화 중 모든 UI 조합은 검증하지 않았습니다. 이번 변경 후 실제 손가락으로 초점·핀치·셔터 입력을 확인하는 단계는 자동 ADB 검사를 대신해 직접 확인합니다.



## v0.11 UI 변경과 검증

사용자의 Chrynalogia Figma 레이아웃을 바탕으로 두 열의 그리드 카드, 선택 시 펼쳐지는 커스텀 칸 수 입력, 그리드·라인·설정 하단 탭, 카메라 왼쪽 세로 메뉴를 적용했습니다. One UI 키트의 색상·글자 크기·비율을 공유하고, 인앱 액션에는 Google 공식 Material 벡터를 사용합니다. 언어와 앱 테마는 설정 탭에 있으며 카메라 팝업에서도 바꿀 수 있습니다. 단일 황금비 항목은 제거하고 기존 선택은 황금비 격자로 이전합니다. 대비 아웃라인은 기본·SVG·이미지 모두에 적용됩니다.

S23 / One UI 8.5에서 단위 검사 6개, 기기 렌더링·가져오기·세션 검사 34개, 새 UI 검사 17개, 팝업 외관·수동 세션 검사 9개, 색상 선택기·슬라이더 검사 4개가 통과했습니다. 빌드와 lint는 오류 없이 완료했습니다. 이번 검사는 카메라의 모든 모드 조합을 다시 실행한 것이 아니며, 실제 다른 기기와 손가락의 핀치·셔터 검증은 포함하지 않습니다. 상세 결과와 화면은 `artifacts/redesign-checks`에 있습니다.

일반 `uiautomator dump`는 접근성 서비스를 일시 중단할 수 있어 카메라 팝업 검사에는 앱 자체 View 진단을 사용합니다. 팝업에서 테마·언어 선택 창을 닫거나 앱 실행을 종료하면 관련 창도 함께 정리합니다.

## v0.12

One UI 키트 Switches·Regular Sliders·Containers Info·Icons·In-App Navigation을 반영했습니다. 원본 SVG 아이콘, 실제 배경 블러, 흰색 선택 테두리, 다중 선택 설명, 6자리 HEX·커스텀 색상 선택·미리보기, 확장 공간을 예약하는 직각 격자, 고정된 앱 로고 아래 별도 카메라 메뉴를 제공합니다. 삼성 가이드 p28의 확장 제목 비율을 사용하고 다른 탭은 첫 기능부터 보이도록 스크롤합니다. 그리드 탭 배치 패널과 설정 그룹 제목은 제거했습니다. 라바트망의 90° 회전은 방향을 바꾸며 선 두께를 유지합니다. 다른 기하와 황금 나선 비율은 보존합니다.

S23에서 단위 6개, 렌더링·가져오기·세션 37개, UI·테마·최종 APK 20개를 검증했습니다. lint 오류는 없습니다. 결과와 화면은 `artifacts/v12-checks`에 기록했습니다.