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
2. Choose **Always on** or **Start manually**.
3. Allow display over other apps and enable **Chrynalogia** in Accessibility. Manual mode also needs notification permission.
4. Open Samsung Camera, then tap the Chrynalogia logo to access the grid menu.

Grids are on-screen guides and do not appear in saved photos. Accessibility detects camera UI on your device; screens and photos are not uploaded.

## Build

On Windows with PowerShell and JDK 17 or newer:

```powershell
.\scripts\Bootstrap.ps1
.\scripts\Build.ps1
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

[Design notes](DESIGN.md) · [Development and verification history (한국어)](docs/DEVELOPMENT.ko.md)
