package dev.chrynalogia.probe;

import android.app.*;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

/** Foreground lifetime for a user-started manual session; never starts at boot. */
public final class ManualSessionService extends Service {
    static boolean running;
    private static final int ID=91;
    @Override public void onCreate() {super.onCreate();running=true;}
    @Override public int onStartCommand(Intent intent,int flags,int startId) {
        GridSettings settings=new GridSettings(this);
        if(intent!=null && "stop".equals(intent.getAction())) {
            settings.stop();if(CameraWatchService.instance!=null)CameraWatchService.instance.refreshSettings();
            stopForeground(STOP_FOREGROUND_REMOVE);stopSelf();return START_NOT_STICKY;
        }
        if(!settings.runMode.equals("manual") || !settings.requested(this)) {stopSelf();return START_NOT_STICKY;}
        NotificationManager manager=getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel("manual",this.getString(R.string.manual_channel),NotificationManager.IMPORTANCE_LOW));
        PendingIntent stop=PendingIntent.getService(this,0,new Intent(this,ManualSessionService.class).setAction("stop"),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification=new Notification.Builder(this,"manual").setSmallIcon(android.graphics.drawable.Icon.createWithBitmap(KitIcon.bitmap(this,R.raw.kit_grid)))
            .setContentTitle(this.getString(R.string.running_title)).setContentText(this.getString(R.string.running_description))
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(new Notification.Action.Builder(null,this.getString(R.string.stop),stop).build()).build();
        startForeground(ID,notification,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        if(CameraWatchService.instance!=null)CameraWatchService.instance.refreshSettings();
        return START_NOT_STICKY;
    }
    @Override public void onDestroy() {
        running=false;
        // In manual mode a killed foreground session must not leave an invisible active session.
        GridSettings settings=new GridSettings(this);
        if(settings.runMode.equals("manual")) {settings.stop();if(CameraWatchService.instance!=null)CameraWatchService.instance.refreshSettings();}
        super.onDestroy();
    }
    @Override public void onConfigurationChanged(android.content.res.Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if(new GridSettings(this).active(this))onStartCommand(null,0,0);
    }
    @Override public IBinder onBind(Intent intent) {return null;}
}

