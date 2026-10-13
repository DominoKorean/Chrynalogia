package dev.chrynalogia.probe;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Locale;

/** Read Samsung's actual preview rectangle; never infer it from movable camera buttons. */
final class PreviewLocator {
    record Result(Rect bounds, String source, String ratio, String mode) {}
    private Rect preview;
    private boolean captureControls;
    private boolean modePicker;
    private String mode;
    private int visited;

    Result locate(AccessibilityNodeInfo root, Rect window) {
        preview = null; captureControls = false; modePicker = false;
        mode = "unknown"; visited = 0;
        if (root != null) walk(root, window, 0);
        // More/settings screens can leave the camera SurfaceView behind their UI.
        if (modePicker || !captureControls) {
            return new Result(new Rect(), "camera-preview-ui-unavailable", "unknown", mode);
        }
        if (preview == null) {
            return new Result(new Rect(), "camera-preview-node-unavailable", "unknown", mode);
        }
        return new Result(preview, "camera-preview-view", describeRatio(preview, window), mode);
    }

    private void walk(AccessibilityNodeInfo node, Rect window, int depth) {
        if (node == null || depth > 40 || ++visited > 1200) return;
        if (node.isVisibleToUser()) {
            String id = String.valueOf(node.getViewIdResourceName());
            if (id.endsWith("/camera_preview")) {
                Rect bounds = new Rect(); node.getBoundsInScreen(bounds);
                if (!bounds.isEmpty() && window.contains(bounds)) preview = bounds;
            }
            if (id.endsWith("/center_button_container") || id.endsWith("/recording_stop_button")) {
                captureControls = true;
            }
            if (id.endsWith("/more_grid_list_view") || id.endsWith("/chooser_menu")) modePicker = true;
            if (id.endsWith("/shooting_mode_list")) {
                String description = String.valueOf(node.getContentDescription());
                if (!description.equals("null")) mode = description.split(",", 2)[0].trim();
            }
            if (id.endsWith("/instant_mode_title") && node.getText() != null) {
                mode = node.getText().toString().trim();
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) walk(node.getChild(i), window, depth + 1);
    }

    static String describeRatio(Rect bounds, Rect window) {
        if (bounds.equals(window)) return "FULL";
        double aspect = Math.max(bounds.width(), bounds.height()) / (double) Math.min(bounds.width(), bounds.height());
        if (Math.abs(aspect - 1.0) < 0.015) return "1:1";
        if (Math.abs(aspect - 4.0 / 3.0) < 0.015) return "3:4";
        if (Math.abs(aspect - 16.0 / 9.0) < 0.015) return "9:16";
        return String.format(Locale.ROOT, "%d:%d", bounds.width(), bounds.height());
    }
}
