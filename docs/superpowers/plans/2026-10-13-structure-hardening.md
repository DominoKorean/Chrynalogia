# Chrynalogia Structure Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. 권장 실행 방식은 Native이며 사용자 선택을 기다린다.

**Goal:** 기존 UI와 카메라 오버레이 동작을 유지하면서 설정 덮어쓰기, 세션 판단 중복, 렌더링 중 파일 처리, 현재 UI와 맞지 않는 테스트를 개선한다.

**Architecture:** SharedPreferences와 GridSettings의 기존 필드를 유지하고 최신 상태에 대한 항목별 변경을 SettingsStore에 모은다. SessionPolicy/SessionController/CameraConnection으로 정책과 Android 수명주기를 연결한다. 가져온 파일 관리는 GridFiles, 비동기 준비는 ImportAssets로 분리하고 View 갱신 요청은 프레임 단위로 합친다.

**Tech Stack:** Java 17, Android 네이티브 View, SDK 37/min 34, AndroidSVG 1.4, JUnit 4.13.2, PowerShell. 새 런타임·테스트 라이브러리는 추가하지 않는다.

**Spec:** [승인된 설계](../specs/2026-10-13-structure-hardening-design.md)

## Global Constraints

- Java 17, compile/target SDK 37, min SDK 34를 유지한다. 새 런타임 라이브러리는 추가하지 않는다.
- applicationId와 namespace는 `dev.chrynalogia.probe`, 설정 저장소 이름은 `probe`를 유지한다. 이번 구조 개선에서 버전 번호는 변경하지 않는다.
- 삼성 카메라의 실제 `camera_preview` 좌표를 사용한다. 임의의 화면 비율로 좌표를 추정하지 않는다.
- 그리드 창은 `TYPE_ACCESSIBILITY_OVERLAY`와 `NOT_TOUCHABLE | NOT_FOCUSABLE`을 유지한다. 메뉴·팝업 창만 입력을 받는다.
- 잠금, 화면 꺼짐, 다른 앱, 촬영 미리보기가 없는 화면에서는 그리드를 숨긴다. 접근성 연결과 실행 활성 상태를 구분한다.
- 앱의 명시적인 실행과 재진입은 기존 실행 동작을 유지한다. 테마·언어·화면 회전으로 Activity가 재생성되는 것만으로 중지된 수동 세션이 다시 시작되어서는 안 된다.
- 수동 세션은 알림 권한과 실제 foreground service 실행을 필요로 한다. 재부팅이나 서비스 종료 후 자동 복구하지 않는다.
- `DESIGN.md`와 `AGENTS.md`의 Figma 배치, Grid/Line/Settings 탭, 두 열 카드, 커스텀 칸 수 입력의 예약 공간, One UI SVG 아이콘, 앱 로고를 유지한다.
- 한국어·영어·시스템 언어와 시스템/밝게/어둡게 테마를 앱·카메라 설정에 유지한다.
- 기본값은 흰색, 두께 1.2dp, 불투명도 90%, 아웃라인 없음이다. 아웃라인은 기존 대비 계산을 사용한다.
- 기존 9종·커스텀 직각 격자, 회전·반전, 다중 선택, 가져온 파일의 비율 유지·늘리기를 보존한다. 커스텀 칸 수는 1~16이다.
- 단일 `golden_ratio`는 지원 목록에 복귀시키지 않는다. 기존 선택은 `golden_grid`로 이전한다.
- 가져오기는 파일당 4MB, 최대 12개, 비트맵 긴 변 최대 1024픽셀 수준의 샘플링과 앱 내부 저장을 유지한다. 추가 저장소·카메라·마이크·네트워크 권한을 요청하지 않는다.
- 제품 소스 기준은 `809847d`다. 구현 작업 공간에는 승인된 설계·계획 커밋도 포함한다. 사용자의 `logo` 변경은 작업에 포함하거나 되돌리지 않는다.

## Review Focus

- 저장된 preferences의 잘못된 타입·NaN·비객체 JSON 항목: 앱이 중단되지 않고 유효한 설정을 읽는다. Task 2에서 검사한다.
- 가져오기 중 사용자 색·선택 방식 변경 및 12번째/13번째 파일 경쟁: 최신 설정과 개수 제한을 유지한다. Task 3에서 검사한다.
- 중지된 세션에서 권한 다이얼로그·테마·언어·화면 재생성: 명시적인 시작 없이 활성화되지 않는다. Task 4에서 검사한다.
- 파일 삭제나 View 분리 후 비동기 로드 완료: 오래된 파일을 다시 표시하거나 종료된 View를 갱신하지 않는다. Task 5에서 검사한다.
- 테스트 본문 실패와 복원 단계 실패가 함께 발생: 나머지 복원을 시도하고 두 오류를 기록한다. Task 1에서 검사한다.

## 공통 검증 명령

Files의 경로 약어는 다음과 같이 확장한다: main Java=`app/src/main/java/dev/chrynalogia/probe/`, debug Java=`app/src/debug/java/dev/chrynalogia/probe/`, unit=`app/src/test/java/dev/chrynalogia/probe/`, androidTest=`app/src/androidTest/java/dev/chrynalogia/probe/`. Manifest는 `app/src/main/AndroidManifest.xml`, strings는 `app/src/main/res/values/strings.xml` 및 `app/src/main/res/values-ko/strings.xml`이다. 모든 Java 클래스는 기존 `dev.chrynalogia.probe` 패키지에 둔다.

각 새 instrumentation 검사 클래스는 `static int run(Context context, android.app.Instrumentation instrumentation)`를 제공하고 통과한 assertion 개수를 반환한다. 실패는 AssertionError로 전달한다. RenderChecks가 suite를 선택하고 사용자 설정·테스트 파일을 finally에서 복원한다. 원래 테스트와 새 테스트 사이에 설정을 초기화하여 실행 순서에 의존하지 않게 한다.

프로젝트 작업 공간에서 `GRADLE_USER_HOME=<workspace>/.tools/gradle-home`과 유효한 JAVA_HOME을 사용한다. 격리된 작업 공간은 원본의 SDK·Gradle 캐시 경로를 명시적으로 재사용하며 해당 경로를 Git에 기록하지 않는다.

- Host: `./gradlew.bat :app:testDebugUnitTest --rerun-tasks --offline --no-daemon --console=plain`. 기대: BUILD SUCCESSFUL, XML 결과의 failures/errors=0.
- Build: `./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:processReleaseMainManifest :app:lintDebug --offline --no-daemon --console=plain`. 기대: BUILD SUCCESSFUL, lint 오류 0; release merged manifest에 DebugReceiver 없음.
- Device: debug 앱과 AndroidTest APK를 `adb install -r`로 설치 후 `adb shell am instrument -w -e suite <suite> dev.chrynalogia.probe.test/dev.chrynalogia.probe.RenderChecks`. 기대: 선택 suite의 PASS와 정상 종료. `suite=all`은 모든 검사를 수행한다.
- 기기 없음: APK 빌드·Host 검사까지 수행하고 Device 검사를 미실행으로 기록한다. 컴파일만으로 플랫폼 동작을 통과했다고 표시하지 않는다.

## Task 1: 현재 UI 검사와 실패 복원 기반

**Files:** Create `scripts/Test-State.ps1`, `scripts/Script-Checks.ps1`, `app/src/debug/java/dev/chrynalogia/probe/DebugState.java`; Modify `DebugReceiver.java`, `scripts/Device.ps1`, `scripts/App-Ui.ps1`, `scripts/OneUI-Test.ps1`, `scripts/Redesign-Line-Test.ps1`, `scripts/Redesign-Appearance-Test.ps1`, `scripts/Redesign-Test.ps1`, `scripts/Ratio-Test.ps1`, `scripts/Mode-Test.ps1`, `scripts/Zoom-Test.ps1`, `scripts/Quick-Controls-Test.ps1`, `scripts/Smoke-Test.ps1`, `scripts/Screen-Size-Test.ps1`, `app/src/androidTest/java/dev/chrynalogia/probe/RenderChecks.java`.

**Interfaces:**
- `Get-ProbeDisplay`: returns `{ Width, Height, DensityScale }` from current wm overrides or physical values, swapping axes for current rotation.
- `Get-ProbeSliderPoint($Node, [int]$Value, [int]$Min, [int]$Max, [double]$DensityScale)`: returns `{X,Y}`; rail inset 12dp.
- `Find-ProbeSlider($Nodes, [string]$Label)`: visible class ending in SeekBar or KitSlider; use intersected viewport and navigation bounds.
- `Invoke-ProbeTest([scriptblock]$Body, [scriptblock[]]$Restore)`: preserve primary error and always run every registered restoration step. Attempt all callbacks and throw AggregateException containing body/restore errors after all attempts.
- `DebugState.backup(Context): void`, `DebugState.restore(Context): void`: debug-only fixed-name internal backup of typed prefs, app locales, manual-session status, and initial validated file inventory.
- DebugReceiver commands `backup-state`, `restore-state` work even when accessibility is disconnected; existing DUMP protection remains. Restore only new validated app grid files, not initial user files; use backup only from current boot, report expired/missing backup.
- RenderChecks accepts suite argument `all/settings/files/session/render`; `render` initially runs existing checks. New groups are wired by their owning tasks.

- [ ] **Write failing host checks** in Script-Checks using fixture nodes: KitSlider at [100,200][500,248], density=1, value=50 in 0..100 gives X=300/Y=224; density=2 and min value gives X=124; invisible/occluded nodes are rejected. Stub ADB in an isolated PowerShell process. Inject body failure and one restore failure; assert all later restore callbacks still run and both errors remain in the report.
- [ ] **Run** `pwsh -NoProfile -File scripts/Script-Checks.ps1`; confirm failure due to current selector/restore behavior or missing helpers before implementation.
- [ ] **Implement helpers and debug backup**. Keep serialization inside app-owned files and typed preferences; apply same per-app language on restore. Interim manual service restoration uses existing lifecycle API; Task 4 replaces it with SessionController. Do not start an Activity merely to restore prefs. Manual restart requires previously active session and currently granted notification permission; otherwise report restoration failure.
- [ ] **Update active scripts** to fresh baseline and shared finally restoration, dynamic display coordinates, current class/padding, independent restoration of size/density/rotation and camera ratio/mode where modified. Nested scripts inherit one outer backup; only its owner restores it. Saved screens/trees remain in artifacts. No global storage deletion.
- [ ] **Verify** Script-Checks passes; parse every scripts/*.ps1 with PowerShell Parser and require zero errors; Build succeeds. On a device deliberately throw after changing one line setting and verify prefs/locales/session and display match baseline. Check merged release manifest contains no DebugReceiver.
- [ ] **Commit** only this task's files: `test: align UI checks and restore device state`.

## Task 2: 설정 저장과 검증

**Files:** Create main Java `SettingsStore.java`, `ImportedGrid.java`, `ChrynalogiaApplication.java`; Create androidTest `SettingsChecks.java`; Modify `GridSettings.java`, `AppAppearance.java`, `SettingsPanel.java`, `CameraControls.java`, `MainActivity.java`, `AndroidManifest.xml`, `RenderChecks.java`.

**Interfaces:**
- `SettingsStore.initialize(Context): void`: idempotent migration at Application startup; no write from GridSettings constructor/reload.
- `SettingsStore.update(Context, java.util.function.Consumer<GridSettings>): GridSettings`: under one process-wide lock, load latest validated snapshot, apply mutation, persist only differing keys, return latest result.
- `ImportedGrid` record `(String id, String name, String extension)`; `parse(JSONObject): ImportedGrid` returns null for invalid input. Id matches `file_[0-9a-f]{32}`, extension only `.svg`/`.png`; same key names as existing stored JSON.
- `GridSettings.reload(): void`: read-only; retain current fields/JSONArray representation for incremental callers. `save(): void` compares with the object's captured original values and writes only changed keys under the same store lock for existing instrumentation setup; product mutation callers use SettingsStore.update.

- [ ] **Add failing SettingsChecks**, invoked by suite=settings. Methods `staleSnapshotsPreserveUnchangedFields`, `styleSavePreservesNewImport`, `readsDoNotWrite`, `migrationIsIdempotent` take Context. Cases: two snapshots A/B; B rotates to 1, A changes color to BLACK => both values retained; another operation adds an import while a stale style snapshot saves => import retained. Read-only construction/reload must not change prefs; explicit initialize maps golden_ratio to golden_grid exactly once.
- [ ] **Add invalid-data assertions**: columns=99→16, rows=0→1, opacity=NaN→0.9, thickness=NaN→1.2, rotation=-1→3, unknown runMode→always, unknown appTheme→system; wrong preference types use defaults without ClassCastException. Imports containing null/string/object with path traversal or bad extension are ignored, valid entries preserved, duplicate ids deduplicated. Invalid selected ids are removed; a deliberately empty selection stays empty.
- [ ] **Run** suite=settings against unchanged implementation and record failing assertions. Without a device, compile new tests and mark RED evidence pending rather than claiming execution.
- [ ] **Implement** startup migration and read validation; single-lock mutation and changed-key writes; migrate UI actions and appTheme writes to the store. Preserve atomic preset mutation and current customColor behavior. Captured baselines must deep-copy selected/imports so in-place changes are detected.
- [ ] **Verify** settings suite, Host and Build. Source search shows no complete preference snapshot save in product callers; migration is absent from read-only reload. Preserve existing pref keys and UI input ranges.
- [ ] **Commit** `refactor: serialize settings changes and validate stored data`.

## Task 3: 파일 목록 트랜잭션과 번역된 오류

**Files:** Create main Java `GridFiles.java`, `GridImportException.java`; Create androidTest `ImportChecks.java`; Modify `GridImporter.java`, `SvgLines.java`, `ImportActivity.java`, `SettingsPanel.java`, `RenderChecks.java`, values/strings.xml and values-ko/strings.xml.

**Interfaces:**
- `GridFiles.register(Context, String name, String extension, File prepared): String throws GridImportException`: generated validated id, current count/selection inside SettingsStore lock, final file installation and metadata registration, cleanup on failure.
- `GridFiles.remove(Context, String id): void throws GridImportException`: validated internal path; nonexistent file can remove stale metadata; failed deletion preserves metadata and reports localized failure.
- `GridFiles.FileOps` interface: `boolean move(File from, File to)`, `boolean delete(File file)`. Package-private register/remove overloads take FileOps for failure injection; normal overloads use actual filesystem operations.
- `GridImportException extends IOException`: resource id `messageRes`, optional underlying cause; no provider/parser exception text as user copy.
- Existing `GridImporter.importUri/importBytes` signatures remain; conversion occurs outside store lock. 4MB check applies to input and normalized SVG, 12 count rechecked at register.

- [ ] **Write failing ImportChecks**, suite=files: prepare file, alter color/multi/rotation before register, assert latest style and selection semantics retained; parallel registrations with 11 existing records => only one succeeds and total=12; corrupt bytes, BOM SVG, unsupported dimensions, DOCTYPE and ENTITY are rejected; transparent bitmap retains alpha and sampled longest side≤1024.
- [ ] **Test deletion/failure** using an injected file-operation adapter on GridFiles: failed rename/write removes newly owned temp files, failed delete keeps old metadata, deleting one id leaves all other files. Inspect error resource ids rather than exact parser wording. Restore original prefs and only test-owned files in finally.
- [ ] **Run** files suite and capture failing behavior before modifying importer.
- [ ] **Implement** bounded temporary conversion and store registration/removal; map file open/size/count/type/conversion/save/delete failures to English and Korean resources. SvgLines produces typed errors; raw cause goes to ProbeLog. ImportActivity checks alive/not-finishing before UI completion, retains camera-return behavior, and does not retain Activity in worker beyond processing.
- [ ] **Verify** files suite and existing render/import tests, Build, and matching resource keys/placeholders. `%` descriptions use formatted=false. Remove filesystem deletion from SettingsPanel.
- [ ] **Commit** `fix: preserve concurrent settings during grid imports`.

## Task 4: 세션 정책과 연결 상태

**Files:** Create main Java `SessionPolicy.java`, `SessionController.java`, `CameraConnection.java`; Create unit `SessionPolicyTest.java`; Create androidTest `SessionChecks.java`; Modify `GridSettings.java`, `MainActivity.java`, `ManualSessionService.java`, `CameraWatchService.java`, `CameraControls.java`, `ImportActivity.java`, debug `DebugState.java`, `DebugReceiver.java`, androidTest `RenderChecks.java`.

**Interfaces:**
- `SessionPolicy.Input` record `(boolean onboarding, String runMode, boolean paused, int requestedBoot, int currentBoot, boolean notificationGranted, boolean foregroundReady)`.
- `SessionPolicy.active(Input): boolean`; `requested(Input): boolean`. Always ignores boot/service readiness after onboarding/unpaused; manual additionally requires matching nonnegative boot numbers, granted notification and foregroundReady.
- `SessionPolicy.StartCause` enum `USER/REENTRY/RECREATION`; `shouldStart(StartCause, Input): boolean` requires onboarding and manual permission when applicable, and always returns false for RECREATION. USER/REENTRY may clear paused via controller; readiness is still required before active becomes true.
- `SessionController.requestStart(Context): boolean`: returns false if manual permission missing; Activity owns runtime request. `stop(Context): void`, `changeMode(Context,String): boolean`, `foregroundReady(Context,boolean): void`, `active(Context): boolean`; all consumers share these actions. Service readiness is set only after successful startForeground and cleared on stop/destroy.
- `CameraConnection.attach(CameraWatchService): void`, `detach(CameraWatchService): void`, `connected(): boolean`, `requestRefresh(): void`, `service(): CameraWatchService` for DUMP debug commands. All product notifications use requestRefresh, no direct instance fields; detach only removes matching service.

- [ ] **Write failing unit cases**: always/onboarding/unpaused is active; manual requires all guards; new boot, unknown boot=-2, pending notification, denied notification or terminated foreground session are inactive. Write instrumentation assertions for stopped-session recreation and manual↔always transition. Replace artificial ManualSessionService.running writes in RenderChecks with policy inputs and real service lifecycle checks.
- [ ] **Run** Host; record policy failures before code. Run session suite on available device for Activity/service assertions; no device means report unavailable.
- [ ] **Implement** controller and bridge; route launcher/camera/notification actions; keep pending permission state through recreation, discard late permission results after stop/mode change. Identify explicit launcher/reentry separately from configuration recreation; bind delayed callbacks to Activity/popup lifecycle and remove them on teardown. Service initialization alone is not active readiness.
- [ ] **Verify** Host/session suite and appearance UI scripts; stopped manual state survives language/theme/orientation changes, explicit reopen starts it, stop notification clears readiness, no accessibility connection means safe no-op refresh. Existing overlay flags and preview locator are unchanged.
- [ ] **Commit** `refactor: centralize overlay session transitions`.

## Task 5: 갱신 분류와 비동기 자산 준비

**Files:** Create main Java `GridRenderKey.java`, `SettingsChange.java`, `ImportAssets.java`, `FrameRefresh.java`; Create unit `GridRenderKeyTest.java`, `SettingsChangeTest.java`; Create androidTest `RenderHardeningChecks.java`; Modify `GridView.java`, `CameraWatchService.java`, `CameraControls.java`, `SettingsPanel.java`, `KitIcon.java`, `ProbeLog.java`, `RenderChecks.java`.

**Interfaces:**
- `GridRenderKey` record `(String id, int width, int height, int rotation, int columns, int rows, float thicknessPx, boolean keepAspect, boolean outline)`; color/opacity excluded. Path key can use geometry-only subset; SVG picture key uses actual rendering viewport/stroke.
- `SettingsChange.State` record `(int color, float opacity, float thickness, boolean outline, boolean hidden, java.util.Set<String> selected, int rotation, int columns, int rows, boolean keepAspect, boolean allowStop, String runMode)`; `GridSettings.changeState(): SettingsChange.State` copies selection.
- `SettingsChange.classify(SettingsChange.State before, SettingsChange.State after): SettingsChange`; booleans render/menu/session. hidden/selection/geometry/style→render; hidden/allowStop/runMode→menu; activation/runMode→session. Preference keys paused/sessionBoot handled as session changes by listener. The State record keeps unit tests independent of Android construction.
- `FrameRefresh.Scheduler` interface `void post(Runnable callback)`, `void remove(Runnable callback)`; constructor `FrameRefresh(Scheduler, java.util.function.IntConsumer callback)`; `request(int mask): void`, `cancel(): void`. OR masks and run one callback per next Choreographer frame, latest state wins; cancel at disconnect. Service supplies a Choreographer adapter, tests supply a deterministic scheduler.
- `ImportAssets.load(ImportedGrid, java.util.function.Consumer<Result>): void`, `close(): void`; Result carries decoded Bitmap or parsed SVG or error. application Context, bounded executor, per-id shared futures, generation checks and main-thread publication; onDraw never opens/decodes/parses files. No mutation of parsed SVG shared across Views; create View-owned rendering wrappers or keep SVG ownership per cache entry/consumer.
- `GridView.awaitImportsForTest(long timeoutMillis): boolean` called off main thread for instrumentation; drawing is done by runOnMainSync after preparation. Normal app never blocks for loading.

- [ ] **Write failing key/classification tests**: color/opacity changes reuse picture key and set render=true/menu=false/session=false; stroke/aspect/size/rotation invalidate picture; hidden updates menu. Inject fake scheduler to FrameRefresh and request 100 changes => one callback containing latest values, cancel=>zero callback.
- [ ] **Write RenderHardeningChecks**: retained SVG picture recolors correctly without new preparation; thickness/outline changes alter pixels; rotate and resize use fresh viewport; delayed load then remove selection/delete/detach does not publish stale result. Failed bitmap decode followed by valid basic grid does not leak Paint color filter. Cache owns/recycles only assets no live consumer uses.
- [ ] **Run** Host/render suite; record failures or unavailable device before implementation.
- [ ] **Implement** classification and frame coalescing; style-only refresh updates GridView and previews without reconcile or toolbar reconstruction. Session/window events retain current reconciliation behavior and 1s fallback. Record menu/asset-build counters in debug diagnostics for meaningful assertions. Probe status persists only changed JSON, explicit capture forces write.
- [ ] **Implement** background file preparation and selective caches; retain existing stroke/alpha/aspect math, original SVG icons and rabatment transform. Use copied Path when transforming cached geometry. Update render tests to wait for async data, never block main thread. Bound asset caches and clear/release on detach/deletion; avoid accumulating one bitmap per thumbnail indefinitely.
- [ ] **Verify** Host/render suites and OneUI camera scripts. Assert 100 style updates cannot produce 100 menu rebuilds/reconciles; capture still reflects final color. Build/lint pass. Performance outcome is reduced work counts; frame time/battery claims require measured device evidence.
- [ ] **Commit** `perf: coalesce overlay refreshes and prepare imports off draw`.

## Task 6: 지원 목록·화면 책임·최종 회귀

**Files:** Create main Java `GridCatalog.java`; Create unit `GridCatalogTest.java`; Modify `GridGeometry.java`, `SettingsPanel.java`, `GridSettings.java`, `SettingsStore.java`, `GridView.java`, androidTest `RenderChecks.java`, `README.md`, `TODO.md`, `scripts/Build.ps1`.

**Interfaces:**
- `GridCatalog.Entry` record `(String id, int nameRes, int descriptionRes)`; `entries(): java.util.List<Entry>` immutable and ordered: thirds, cross, golden_grid, spiral, x, diagonal, triangle, rabatment, dynamic, custom. `contains(String): boolean`.
- Resource identifiers remain existing R.string ids; migration recognizes golden_ratio separately from supported render catalog.

- [ ] **Write failing catalog test** asserting list length=10, unique ids, supported golden_grid/spiral, no golden_ratio. Replace legacy rotation/flip checks with SVG fixture `viewBox='0 0 100 100'`, path `M25 0 L25 100 M0 25 L100 25`, stretch=true, opacity=1, outline=false, thickness=1.2dp on 400×600 canvas. Original alpha(100,400)>100 and alpha(300,400)=0; flipX reverses these; rotation=1 has alpha(300,400)>100/alpha(100,400)=0; rotation=1+flipX reverses them again. Keep a separate migration test from Task 2.
- [ ] **Run** Host/catalog assertions before removing legacy data.
- [ ] **Implement** catalog-based cards and names; delete unused NAMES/DESCRIPTIONS and legacy rendering branch. Split SettingsPanel construction into focused private methods, keeping callback delegation to SettingsStore/SessionController/GridFiles; preserve selected border, count INVISIBLE reservation, current scroll and navigation behavior. Do not replace native widgets/layout.
- [ ] **Document** active script entry points and retired v0.9 tests, current architecture, exact device requirements, unavailable verification and new unit/device suite counts from actual results. Build script includes AndroidTest compilation; do not auto-install or alter devices during a host build.
- [ ] **Verify entire result**: Host, Build, Script-Checks, scripts syntax, `git diff --check`, matching strings/placeholders and release merged manifest. On a device run suite=all and current UI/camera checks with backup/restore; compare original/final settings. Record all test counts, lint warnings and unrun manual checks.
- [ ] **Review full diff** against spec, especially touch flags, session lifecycle and asynchronous cancellation. Apply superpowers:requesting-code-review before integration; resolve actionable findings and rerun affected checks. No push/deploy without separate request.
- [ ] **Commit** `refactor: consolidate grid catalog and document hardening` and report changed files, test evidence and remaining device limitations.

## Execution handoff

계획 상태: 사용자의 직접 코드 적용 지시에 따라 Native 방식으로 구현했다. 실제 결과·검증·판단은 `../implementation/2026-10-13-structure-hardening.md`와 실행 기록에 남긴다. 아래 방식 비교는 계획 작성 당시의 기록이다.

- Native 권장: 이 세션에서 구현자가 순서대로 진행하고 마지막에 독립 리뷰를 수행한다. 저장·세션·렌더링의 인터페이스가 연결되어 있어 맥락을 유지하기 쉽다.
- Subagent-driven: 작업마다 구현·리뷰를 별도로 수행한다. 단계마다 독립 리뷰를 받을 수 있고 컨텍스트 비용은 더 크다.

선택 후 using-git-worktrees로 기존 적합한 작업 공간을 확인하거나 승인된 문서 커밋을 기반으로 새 작업 공간을 만든다. Native는 executing-plans, Subagent-driven은 subagent-driven-development를 적용한다. 구현 중 숨은 동작 변경이 필요하면 설계를 먼저 갱신하고 검토한다.
