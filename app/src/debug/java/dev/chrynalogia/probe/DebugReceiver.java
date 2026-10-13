package dev.chrynalogia.probe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;

/** Debug build only, protected by DUMP permission (ADB shell/system). */
public final class DebugReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        CameraWatchService service = CameraWatchService.instance;
        if (service == null) {
            ProbeLog.write(context, "probe-status.json", "{\"connected\":false,\"overlayAttached\":false}");
            setResultCode(1); setResultData("Accessibility service is not connected"); return;
        }
        String command = intent.getStringExtra("command");
        if (command == null) command = "capture";
        switch (command) {
            case "pause" -> service.setPaused(true);
            case "resume" -> service.setPaused(false);
            case "bounds" -> {
                int left = intent.getIntExtra("left", -1), top = intent.getIntExtra("top", -1);
                int right = intent.getIntExtra("right", -1), bottom = intent.getIntExtra("bottom", -1);
                if (left < 0 || top < 0 || right <= left || bottom <= top) {
                    setResultCode(2); setResultData("Invalid bounds"); return;
                }
                service.setDebugBounds(new Rect(left, top, right, bottom));
            }
            case "reset-bounds" -> service.setDebugBounds(null);
            case "capture" -> { }
            default -> { setResultCode(2); setResultData("Unknown command"); return; }
        }
        service.captureDiagnostics();
        setResultCode(0); setResultData(service.diagnosticStatus());
    }
}
