package dev.chrynalogia.probe;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/** Android lifetime effects shared by launcher, camera controls and notification. */
final class SessionController {
    static final String GENERATION="sessionGeneration";
    private static final SessionLifetime lifetime=new SessionLifetime();
    static boolean ready(){return lifetime.ready();}
    static long generation(){return lifetime.generation();}
    static boolean current(long value){return lifetime.current(value);}
    static SessionPolicy.Input input(Context c){GridSettings s=new GridSettings(c);var values=s.prefs.getAll();Object paused=values.get("paused"),boot=values.get("sessionBoot");
        return new SessionPolicy.Input(s.onboarding,s.runMode,Boolean.TRUE.equals(paused),boot instanceof Integer n?n:-1,GridSettings.boot(c),c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED,ready());
    }
    static boolean active(Context c){return SessionPolicy.active(input(c));}
    static synchronized boolean requestStart(Context c){
        GridSettings state=new GridSettings(c);if(!state.onboarding)return false;
        long generation=lifetime.request();state.activate(c);
        if(state.runMode.equals("manual")){
            if(!SessionPolicy.shouldStart(SessionPolicy.StartCause.USER,input(c))){CameraConnection.requestRefresh();return false;}
            try{c.startForegroundService(new Intent(c,ManualSessionService.class).putExtra(GENERATION,generation));}
            catch(RuntimeException error){state.stop();ProbeLog.event(c,"manual_start_error","error",error.toString());CameraConnection.requestRefresh();return false;}
        }
        CameraConnection.requestRefresh();return true;
    }
    static synchronized void stop(Context c){lifetime.request();new GridSettings(c).stop();c.stopService(new Intent(c,ManualSessionService.class));CameraConnection.requestRefresh();}
    static synchronized void clearForeground(Context c){lifetime.request();c.stopService(new Intent(c,ManualSessionService.class));CameraConnection.requestRefresh();}
    static synchronized boolean changeMode(Context c,String mode){lifetime.request();SettingsStore.update(c,s->s.runMode="manual".equals(mode)?"manual":"always");if(!"manual".equals(mode))c.stopService(new Intent(c,ManualSessionService.class));return requestStart(c);}
    static void foregroundReady(Context c,long generation){lifetime.ready(c,generation);CameraConnection.requestRefresh();}
    static synchronized void destroyed(Context c,long generation){if(lifetime.ended(c,generation)&&new GridSettings(c).runMode.equals("manual"))new GridSettings(c).stop();CameraConnection.requestRefresh();}
}
