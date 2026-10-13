package dev.chrynalogia.probe;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import org.json.JSONObject;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public final class CameraWatchService extends AccessibilityService {
    static final String CAMERA_PACKAGE = "com.sec.android.app.camera";
    static CameraWatchService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager manager;
    private GridView grid;
    private CameraControls controls;
    private GridSettings settings;
    private android.content.SharedPreferences.OnSharedPreferenceChangeListener preferenceListener;
    private Rect lastBounds;
    private Rect debugBounds;
    private Rect systemWindowBounds;
    private Rect contentWindowBounds;
    private String foreground = "unknown";
    private String locator = "none";
    private String reason = "starting";
    private String previewRatio = "unknown";
    private String cameraMode = "unknown";
    private boolean paused;
    private float alpha;
    private long lastClickLog;
    private final Runnable reconcileTask = () -> reconcile("event");
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (paused) return;
            reconcile("heartbeat");
            handler.postDelayed(this, 1000);
        }
    };

    @Override protected void onServiceConnected() {
        instance = this;
        manager = getSystemService(WindowManager.class);
        settings = new GridSettings(this);
        controls = new CameraControls(this, manager);
        paused = !settings.active(this);
        preferenceListener = (prefs, key) -> {
            if ("paused".equals(key) || "sessionBoot".equals(key) || "runMode".equals(key)) handler.post(() -> refreshSettings());
        };
        settings.prefs.registerOnSharedPreferenceChangeListener(preferenceListener);
        // Accessibility overlays are trusted windows. Pixel opacity is controlled by the renderer.
        float maximum = getSystemService(InputManager.class).getMaximumObscuringOpacityForTouch();
        alpha = 1f;
        ProbeLog.event(this, "service_connected", "sdk", android.os.Build.VERSION.SDK_INT,
            "model", android.os.Build.MODEL, "maximumObscuringOpacity", maximum, "windowAlpha", alpha);
        handler.post(poll);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (manager == null || event == null) return;
        if (paused) return;
        boolean camera = CAMERA_PACKAGE.contentEquals(event.getPackageName() == null ? "" : event.getPackageName());
        int type = event.getEventType();
        if (camera && type == AccessibilityEvent.TYPE_VIEW_CLICKED
                && android.os.SystemClock.uptimeMillis() - lastClickLog > 100) {
            lastClickLog = android.os.SystemClock.uptimeMillis();
            AccessibilityNodeInfo source = event.getSource();
            ProbeLog.event(this, "camera_ui_click", "viewId", source == null ? "unavailable" : source.getViewIdResourceName(),
                "class", String.valueOf(event.getClassName()));
            // A click event is supporting evidence, not proof that all touches pass through.
        }
        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || type == AccessibilityEvent.TYPE_WINDOWS_CHANGED || camera) {
            // Do not cancel an already queued update: continuously changing UI must not starve it.
            if (!handler.hasCallbacks(reconcileTask)) handler.postDelayed(reconcileTask, 100);
        }
    }

    private record CameraWindow(AccessibilityNodeInfo root, Rect bounds, String packageName) {}
    private CameraWindow focusedWindow() {
        List<AccessibilityWindowInfo> windows = getWindows();
        AccessibilityWindowInfo application = null;
        for (AccessibilityWindowInfo window : windows) {
            // Our focusable settings panel and keyboard leave the camera application underneath.
            if (window.getType() == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY
                    || window.getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) continue;
            if (window.isFocused()) { application = window; break; }
            if (application == null && window.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) application = window;
        }
        if (application != null) {
            AccessibilityWindowInfo window = application;
            AccessibilityNodeInfo root = window.getRoot();
            String pkg = root == null ? "unavailable" : String.valueOf(root.getPackageName());
            Rect bounds = new Rect(); window.getBoundsInScreen(bounds);
            systemWindowBounds = new Rect(bounds);
            // A system dialog / notification shade must hide the grid even if camera stays behind it.
            if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) {
                return new CameraWindow(null, bounds, "system-window");
            }
            // After logical display/DPI changes Samsung can report window bounds in
            // a different scale. The root and preview nodes share screen coordinates.
            if (root != null) {
                Rect rootBounds = new Rect(); root.getBoundsInScreen(rootBounds);
                if (!rootBounds.isEmpty()) bounds = rootBounds;
            }
            contentWindowBounds = new Rect(bounds);
            return new CameraWindow(root, bounds, pkg);
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return new CameraWindow(null, new Rect(), "unavailable");
        Rect bounds = new Rect(); root.getBoundsInScreen(bounds);
        if (bounds.isEmpty()) bounds = manager.getCurrentWindowMetrics().getBounds();
        contentWindowBounds = new Rect(bounds);
        return new CameraWindow(root, bounds, String.valueOf(root.getPackageName()));
    }

    private void reconcile(String trigger) {
        try {
            CameraWindow focused = focusedWindow();
            boolean ownPopupRoot = controls != null && controls.popupOpen() && focused.root() != null
                && getPackageName().equals(focused.packageName())
                && !MainActivity.foreground();
            if (!ownPopupRoot && !focused.packageName().equals(foreground)) {
                foreground = focused.packageName();
                ProbeLog.event(this, "foreground_changed", "package", foreground, "trigger", trigger);
            }
            if (paused) { hide("paused"); return; }
            if (!getSystemService(android.os.PowerManager.class).isInteractive()
                    || getSystemService(android.app.KeyguardManager.class).isKeyguardLocked()) {
                hide("screen-off-or-locked"); return;
            }
            if (!Settings.canDrawOverlays(this)) { hide("overlay-permission-missing"); return; }
            if (ownPopupRoot) {
                // During IME transitions the active accessibility root may briefly be our panel.
                if (!handler.hasCallbacks(reconcileTask)) handler.postDelayed(reconcileTask,200);
                return;
            }
            if (!CAMERA_PACKAGE.equals(foreground)) { hide("camera-not-focused"); return; }
            if (focused.bounds().isEmpty()) { hide("camera-bounds-unavailable"); return; }
            if (controls.popupOpen() && lastBounds != null) {
                // Our panel/keyboard can hide native capture nodes. The camera remains underneath.
                // Rotation closes the panel before this path can reuse old coordinates.
                show(new Rect(lastBounds), "settings-popup-held");
                return;
            }
            PreviewLocator.Result result = new PreviewLocator().locate(focused.root(), focused.bounds());
            Rect bounds = result.bounds();
            String source = result.source();
            boolean infoChanged = !previewRatio.equals(result.ratio()) || !cameraMode.equals(result.mode());
            previewRatio = result.ratio(); cameraMode = result.mode();
            if (bounds.isEmpty()) { hide(source); return; }
            if (debugBounds != null) {
                bounds = new Rect(debugBounds);
                if (!focused.bounds().contains(bounds)) { hide("debug-bounds-outside-camera"); return; }
                source = "adb-calibrated";
            }
            show(bounds, source);
            controls.show(focused.bounds(), focused.root());
            if (infoChanged) writeStatus();
        } catch (RuntimeException error) {
            ProbeLog.event(this, "reconcile_error", "error", error.toString());
            hide("reconcile-error");
        }
    }

    @android.annotation.SuppressLint("RtlHardcoded") // These are physical screen coordinates, independent of locale.
    private void show(Rect bounds, String source) {
        boolean changed = grid == null || !bounds.equals(lastBounds) || !source.equals(locator);
        if (!changed) return;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(bounds.width(), bounds.height(),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = bounds.left; params.y = bounds.top;
        params.alpha = alpha;
        params.setFitInsetsTypes(0);
        params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        params.setTitle("CameraGridProbe-GoldenGrid");
        try {
            if (grid == null) {
                GridView candidate = new GridView(this);
                manager.addView(candidate, params);
                grid = candidate;
            } else { manager.updateViewLayout(grid, params); }
            lastBounds = new Rect(bounds); locator = source; reason = "camera-focused";
            ProbeLog.event(this, "overlay_shown", "bounds", bounds.toShortString(), "source", source,
                "alpha", alpha, "flags", params.flags, "touchable", false);
            writeStatus();
        } catch (RuntimeException error) {
            ProbeLog.event(this, "overlay_error", "error", error.toString());
            hide("overlay-add-failed");
        }
    }

    private void hide(String why) {
        if (controls != null) controls.hide();
        boolean changed = grid != null || !why.equals(reason);
        if (grid != null) {
            try { manager.removeViewImmediate(grid); }
            catch (RuntimeException error) { ProbeLog.event(this, "overlay_remove_error", "error", error.toString()); }
            grid = null;
        }
        reason = why;
        if (changed) {
            ProbeLog.event(this, "overlay_hidden", "reason", why);
            writeStatus();
        }
    }

    void setPaused(boolean value) {
        if (settings == null) settings = new GridSettings(this);
        if (value) settings.stop(); else settings.activate(this);
        paused = !settings.active(this);
        ProbeLog.event(this, "pause_changed", "paused", value);
        reconcile("pause-command");
    }
    void refreshSettings() {
        if (settings == null) return;
        settings.reload(); paused = !settings.active(this);
        handler.removeCallbacks(poll);
        if (!paused) handler.postDelayed(poll,1000);
        if (grid != null) grid.applySettings();
        if (controls != null) controls.refresh();
        reconcile("settings-changed"); writeStatus();
    }
    void setDebugBounds(Rect bounds) { debugBounds = bounds; reconcile("bounds-command"); }
    String shortStatus() { return "상태: " + reason + "\n영역: " + locator + "\n비율: " + previewRatio + " · 모드: " + cameraMode; }
    String diagnosticStatus() {
        try {
            return new JSONObject().put("connected", true).put("paused", paused)
                .put("foreground", foreground).put("overlayAttached", grid != null)
                .put("reason", reason).put("locator", locator)
                .put("previewRatio", previewRatio).put("cameraMode", cameraMode)
                .put("systemWindowBounds", systemWindowBounds == null ? JSONObject.NULL : systemWindowBounds.toShortString())
                .put("contentWindowBounds", contentWindowBounds == null ? JSONObject.NULL : contentWindowBounds.toShortString())
                .put("ratioSource", "preview-geometry")
                .put("bounds", lastBounds == null ? JSONObject.NULL : lastBounds.toShortString())
                .put("windowAlpha", alpha).put("overlayPermission", Settings.canDrawOverlays(this))
                .put("controlsAttached", controls != null && controls.attached())
                .put("settingsPopupOpen", controls != null && controls.popupOpen())
                .put("gridHidden", settings != null && settings.hidden)
                .put("selectedGrids", settings == null ? JSONObject.NULL : new org.json.JSONArray(settings.selected))
                .put("gridRotation", settings == null ? 0 : settings.rotation)
                .put("flipX", settings != null && settings.flipX).put("flipY", settings != null && settings.flipY)
                .put("runMode", settings == null ? "unknown" : settings.runMode)
                .put("columns", settings == null ? 0 : settings.columns).put("rows", settings == null ? 0 : settings.rows)
                .put("lineColor", settings == null ? 0 : settings.color).put("lineOpacity", settings == null ? 0 : settings.opacity)
                .put("lineThickness", settings == null ? 0 : settings.thickness).put("outline", settings != null && settings.outline)
                .put("outlineColor", settings == null ? 0 : OutlineColor.forLine(settings.color))
                .put("appTheme", AppAppearance.theme(this)).put("appLanguage", AppAppearance.language(this))
                .put("keepAspect", settings != null && settings.keepAspect).put("importCount", settings == null ? 0 : settings.imports.length())
                .put("sessionActive", settings != null && settings.active(this))
                .put("touchPassThroughVerified", false)
                .put("note", "Attached/drawn does not prove visible or touch delivery; inspect screenshot and device interactions.")
                .toString(2);
        } catch (Exception error) { return error.toString(); }
    }
    private void writeStatus() { ProbeLog.write(this, "probe-status.json", diagnosticStatus()); }

    String dumpCameraTree() {
        CameraWindow focused = focusedWindow();
        if (!CAMERA_PACKAGE.equals(focused.packageName())) return "Camera is not focused: " + focused.packageName();
        StringBuilder output = new StringBuilder("Samsung camera accessibility tree\n");
        dumpNode(focused.root(), output, 0, new int[]{0});
        return output.toString();
    }
    private void dumpNode(AccessibilityNodeInfo node, StringBuilder out, int depth, int[] count) {
        if (node == null || depth > 40 || ++count[0] > 1200) return;
        Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
        out.append("  ".repeat(depth)).append(node.getClassName())
            .append(" id=").append(node.getViewIdResourceName()).append(" bounds=").append(bounds.toShortString())
            .append(" visible=").append(node.isVisibleToUser()).append(" clickable=").append(node.isClickable())
            .append(" selected=").append(node.isSelected()).append(" checked=").append(node.isChecked())
            .append(" text=").append(node.getText()).append(" desc=").append(node.getContentDescription()).append('\n');
        for (int i = 0; i < node.getChildCount(); i++) dumpNode(node.getChild(i), out, depth + 1, count);
    }
    void captureDiagnostics() {
        reconcile("diagnostic-command");
        writeStatus();
        ProbeLog.write(this, "camera-ui.txt", dumpCameraTree());
        StringBuilder own = new StringBuilder("Chrynalogia UI\n");
        for (AccessibilityWindowInfo window : MainActivity.foreground() ? getWindows() : java.util.List.<AccessibilityWindowInfo>of()) {
            if (window.getType() == AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY) continue;
            AccessibilityNodeInfo root = window.getRoot();
            if (root != null && getPackageName().contentEquals(root.getPackageName() == null ? "" : root.getPackageName())) {
                dumpNode(root, own, 0, new int[]{0});
            }
        }
        if (controls != null) controls.dumpUi(own);
        ProbeLog.write(this, "app-ui.txt", own.toString());
        ProbeLog.event(this, "diagnostics_saved", "foreground", foreground);
    }
    @Override protected void dump(FileDescriptor fd, PrintWriter writer, String[] args) {
        writer.println(diagnosticStatus());
    }
    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        debugBounds = null; // calibrated portrait coordinates must never survive rotation
        if (controls != null) controls.hide();
        handler.postDelayed(reconcileTask, 200);
    }
    @Override public void onInterrupt() { hide("accessibility-interrupted"); }
    @Override public boolean onUnbind(Intent intent) { cleanup(); return super.onUnbind(intent); }
    @Override public void onDestroy() { cleanup(); super.onDestroy(); }
    private void cleanup() {
        if (settings != null && preferenceListener != null) settings.prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        handler.removeCallbacksAndMessages(null);
        hide("service-disconnected");
        if (instance == this) instance = null;
        ProbeLog.write(this, "probe-status.json", "{\"connected\":false,\"overlayAttached\":false}");
    }
}
