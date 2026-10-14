<p align="center">
  <img src="logo/logo.svg" width="400" height="400" alt="Chrynalogia logo">
</p>

<h1 align="center">Chrynalogia</h1>

<p align="center">
  <strong>English</strong> · <a href="README.ko.md">한국어</a>
</p>

<p align="center">
  Composition guides over Samsung Camera. Chrynalogia adds customizable grids to the camera preview so you can frame a shot while using Samsung's camera controls.
</p>

## Features

- Rule of thirds, golden ratio, golden spiral, diagonal guides, dynamic symmetry, and custom rectangular grids.
- Display several grids together or import SVG, PNG, JPEG, and WebP files.
- Adjust line color, opacity, thickness, and contrast outlines.
- Hide, rotate, and flip grids from the in-camera menu. Grid touches pass through to the camera.
- Always-on and manual modes, Korean and English, and system/light/dark themes.

## Compatibility

Designed for Samsung Camera on the latest One UI, with Android 14 or newer. Tested on **Galaxy S23 / One UI 8.5**; other devices have not been verified. Non-One UI devices show a first-launch warning, but you can continue for special setups. Other camera apps are not supported.

## Getting started

1. Install the APK manually. The app is not distributed through Google Play.
2. Follow the illustrated setup guide to allow display over other apps and enable **Chrynalogia** in Accessibility.
3. Choose **Always on** or **Start manually**. Manual mode also needs notification permission.
4. Open Samsung Camera, then tap the Chrynalogia logo to access the grid menu.

Grids are on-screen guides and do not appear in saved photos. Accessibility detects camera UI on your device; screens and photos are not uploaded.

### Google Play Protect

> [!note]
> This app uses an accessibility service, so installation from a browser or file manager _**may be blocked by Play Protect.**_ We are preparing an appeal to request a review of this block. Thank you for your patience until the review is complete; approval and its timing are not guaranteed.

Keeping Play Protect enabled is recommended. If you choose to install a release APK you trust, you can temporarily change the setting:

1. Open **Google Play Store → profile icon in the upper right → Play Protect → Settings in the upper right**.
2. Turn off **Scan apps with Play Protect** and complete any confirmation yourself.
3. Install the APK, then turn **Scan apps with Play Protect** back on.

## Build

On Windows with PowerShell and JDK 17 or newer:

```powershell
.\scripts\Bootstrap.ps1
.\scripts\Release.ps1
```

Signed release APK: `artifacts/Chrynalogia-v1.0.4.apk`. Keep a private backup of the `.signing` folder created on the first build; future updates require the same signing key. This folder is excluded from Git.

For a debug build, use `.\scripts\Build.ps1`. The first release uses a different certificate, so uninstall the debug app before installing it.

[Design notes](DESIGN.md) · [Development and verification history (한국어)](docs/DEVELOPMENT.ko.md)
