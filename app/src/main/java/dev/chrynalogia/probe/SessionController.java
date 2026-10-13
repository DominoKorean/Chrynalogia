package dev.chrynalogia.probe;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/** Android lifetime effects shared by launcher, camera controls and notification. */
final class SessionController {
    private static volatile boolean ready;
    static boolean ready(){return ready;}
    static SessionPolicy.Input input(Context c){GridSettings s=new GridSettings(c);var values=s.prefs.getAll();Object paused=values.get("paused"),boot=values.get("sessionBoot");
        return new SessionPolicy.Input(s.onboarding,s.runMode,Boolean.TRUE.equals(paused),boot instanceof Integer n?n:-1,GridSettings.boot(c),c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED,ready);
    }
    static boolean active(Context c){return SessionPolicy.active(input(c));}
    static boolean requestStart(Context c){
        GridSettings state=new GridSettings(c);if(!state.onboarding)return false;
        state.activate(c);
        if(state.runMode.equals("manual")){
            if(c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){CameraConnection.requestRefresh();return false;}
            try{c.startForegroundService(new Intent(c,ManualSessionService.class));}
            catch(RuntimeException error){state.stop();ProbeLog.event(c,"manual_start_error","error",error.toString());CameraConnection.requestRefresh();return false;}
        }
        CameraConnection.requestRefresh();return true;
    }
    static void stop(Context c){new GridSettings(c).stop();ready=false;c.stopService(new Intent(c,ManualSessionService.class));CameraConnection.requestRefresh();}
    static boolean changeMode(Context c,String mode){SettingsStore.update(c,s->s.runMode="manual".equals(mode)?"manual":"always");if(!mode.equals("manual")){ready=false;c.stopService(new Intent(c,ManualSessionService.class));}return requestStart(c);}
    static void foregroundReady(Context c,boolean value){ready=value;CameraConnection.requestRefresh();}
}
