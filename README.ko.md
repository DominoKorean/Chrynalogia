<p align="center">
  <img src="logo/logo.svg" width="400" height="400" alt="Chrynalogia 로고">
</p>

<h1 align="center">Chrynalogia</h1>

<p align="center">
  <a href="README.md">English</a> · <strong>한국어</strong>
</p>

<p align="center">
  삼성 카메라 미리보기 위에 구도 그리드를 표시하는 앱입니다. 기본 카메라의 촬영 기능을 그대로 사용하면서 원하는 그리드로 구도를 잡을 수 있습니다.
</p>

## 주요 기능

- 3분할법, 황금비 격자, 황금 나선, 대각선, 동적 대칭, 커스텀 직각 격자
- 여러 그리드 동시 표시와 SVG·PNG·JPEG·WebP 파일 가져오기
- 선 색상, 불투명도, 두께, 대비 아웃라인 조절
- 카메라 안에서 그리드 숨기기·회전·반전 — 그리드 영역의 터치는 카메라에 전달
- 항상 켜기·수동 실행, 한국어·영어, 시스템·밝은·어두운 테마

## 지원 환경

Android 14 이상에서 최신 One UI의 삼성 카메라를 대상으로 개발합니다. **Galaxy S23 / One UI 8.5**에서 검증했으며 다른 기기는 아직 확인하지 않았습니다. One UI가 아닌 기기에는 첫 실행 안내를 표시하지만 특수한 환경을 위해 사용을 차단하지 않습니다. 다른 카메라 앱은 지원하지 않습니다.

## 사용 방법

1. APK를 직접 설치합니다. 플레이스토어에는 배포하지 않습니다.
2. **항상 켜기** 또는 **수동으로 켜기**를 선택합니다.
3. 다른 앱 위에 표시를 허용하고 접근성에서 **Chrynalogia**를 켭니다. 수동 실행에는 알림 권한도 필요합니다.
4. 삼성 카메라를 열고 Chrynalogia 로고를 눌러 그리드 메뉴를 사용합니다.

그리드는 화면에만 표시되며 저장되는 사진에는 포함되지 않습니다. 접근성은 기기 안에서 카메라 UI를 감지하는 데 사용하며 화면이나 사진을 업로드하지 않습니다.

## 빌드

Windows에서 PowerShell과 JDK 17 이상을 준비한 뒤 실행합니다.

```powershell
.\scripts\Bootstrap.ps1
.\scripts\Build.ps1
```

APK 생성 경로: `app/build/outputs/apk/debug/app-debug.apk`

[디자인 기준](DESIGN.md) · [개발·검증 기록](docs/DEVELOPMENT.ko.md)
