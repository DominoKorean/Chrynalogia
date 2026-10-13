package dev.chrynalogia.probe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;

/** Debug build only, protected by DUMP permission (ADB shell/system). */
public final class DebugReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String requested=intent.getStringExtra("command");
        if("backup-state".equals(requested)||"restore-state".equals(requested)) {
            try {
                if("backup-state".equals(requested)){DebugState.save(context);setResultCode(0);setResultData("State backed up");}
                else {
                    org.json.JSONObject state=DebugState.read(context);
                    if(!state.getBoolean("running")&&SessionController.ready()) {
                        context.stopService(new Intent(context,ManualSessionService.class));
                        PendingResult result=goAsync();android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
                        handler.post(new Runnable(){int attempts;public void run(){
                            if(SessionController.ready()&&attempts++<50){handler.postDelayed(this,100);return;}
                            try{if(SessionController.ready())throw new java.io.IOException("Session did not stop");DebugState.restore(context,state);result.setResultCode(0);result.setResultData("State restored");}
                            catch(Exception error){result.setResultCode(3);result.setResultData(error.toString());}finally{result.finish();}
                        }});
                    } else {DebugState.restore(context,state);setResultCode(0);setResultData("State restored");}
                }
            }catch(Exception error){setResultCode(3);setResultData(error.toString());}
            return;
        }
        CameraWatchService service = CameraConnection.service();
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
