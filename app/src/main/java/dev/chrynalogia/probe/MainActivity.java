package dev.chrynalogia.probe;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.*;

public final class MainActivity extends Activity {
    private static MainActivity visibleActivity;
    static boolean foreground(){return visibleActivity!=null;}
    private GridSettings settings;
    private boolean pendingManual;
    @Override public void onCreate(Bundle state) {
        setTheme(AppAppearance.dark(this)?R.style.AppDark:R.style.AppLight);
        super.onCreate(state);settings=new GridSettings(this);
        pendingManual=state!=null&&state.getBoolean("pendingManual",false);
        if(settings.onboarding&&getLastNonConfigurationInstance()==null) {
            settings.activate(this);if(settings.runMode.equals("manual"))startManual();else refreshService();
        }
        render();
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);setIntent(intent);settings.reload();
        if(settings.onboarding){settings.activate(this);if(settings.runMode.equals("manual"))startManual();else refreshService();}render();
    }
    private void render() {
        settings.reload();LinearLayout root=Ui.column(this);root.setBackgroundColor(Ui.bg(this));
        root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        if(!settings.onboarding) {
            ScrollView scroll=new ScrollView(this);LinearLayout welcome=Ui.column(this);welcome.setPadding(0,Ui.dp(this,48),0,Ui.dp(this,24));scroll.addView(welcome);root.addView(scroll);
            ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.app_logo);welcome.addView(logo,new LinearLayout.LayoutParams(-1,Ui.dp(this,120)));
            Ui.title(welcome,"Chrynalogia");Ui.note(welcome,getString(R.string.welcome_intro));Ui.title(welcome,getString(R.string.welcome_mode));
            Ui.note(welcome,getString(R.string.welcome_disclosure));Ui.note(welcome,getString(R.string.welcome_privacy));
            Ui.addButton(welcome,getString(R.string.always),()->chooseMode("always"));Ui.addButton(welcome,getString(R.string.choose_manual),()->chooseMode("manual"));
        } else root.addView(new SettingsPanel(this,new SettingsPanel.Actions() {
            public void changed(){refreshService();}
            public void importGrid(){startActivity(new Intent(MainActivity.this,ImportActivity.class));}
            public void modeChanged(String mode){settings.reload();settings.activate(MainActivity.this);if(mode.equals("manual"))startManual();else stopService(new Intent(MainActivity.this,ManualSessionService.class));refreshService();render();}
            public void stop(){stopSession();render();}
            public void startSession(){settings.activate(MainActivity.this);startManual();render();}
            public void openCamera(){MainActivity.this.openCamera();}
            public void appearanceChanged(){refreshService();new android.os.Handler(getMainLooper()).postDelayed(()->{if(!isFinishing()&&!isDestroyed())recreate();},200);}
            public void permissions(boolean overlay){startActivity(overlay?new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())):new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));}
        }),new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
        int bars=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        getWindow().getInsetsController().setSystemBarsAppearance(AppAppearance.dark(this)?0:bars,bars);
    }
    private void openCamera() {
        if(!Settings.canDrawOverlays(this)||CameraWatchService.instance==null){Toast.makeText(this,R.string.permissions_first,Toast.LENGTH_LONG).show();return;}
        settings.activate(this);if(settings.runMode.equals("manual")&&!startManual())return;refreshService();
        Intent camera=getPackageManager().getLaunchIntentForPackage(CameraWatchService.CAMERA_PACKAGE);
        if(camera!=null)startActivity(camera);else Toast.makeText(this,R.string.camera_missing,Toast.LENGTH_LONG).show();
    }
    private void chooseMode(String mode) {settings.runMode=mode;settings.onboarding=true;settings.save();settings.activate(this);if(mode.equals("manual"))startManual();refreshService();render();}
    private boolean startManual() {
        if(pendingManual)return false;
        if(checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){pendingManual=true;requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);return false;}
        startForegroundService(new Intent(this,ManualSessionService.class));refreshService();
        new android.os.Handler(getMainLooper()).postDelayed(()->{if(!isFinishing()&&!isDestroyed())render();},250);return true;
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==10&&pendingManual){pendingManual=false;settings.reload();if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED&&settings.runMode.equals("manual")&&settings.requested(this))startManual();else{settings.stop();refreshService();Toast.makeText(this,R.string.notification_required,Toast.LENGTH_LONG).show();}render();}
    }
    private void stopSession(){settings.stop();stopService(new Intent(this,ManualSessionService.class));refreshService();}
    private void refreshService(){if(CameraWatchService.instance!=null)CameraWatchService.instance.refreshSettings();}
    @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("pendingManual",pendingManual);super.onSaveInstanceState(state);}
    @Override public Object onRetainNonConfigurationInstance(){return Boolean.TRUE;}
    @Override protected void onRestart(){super.onRestart();settings.reload();if(settings.onboarding&&settings.runMode.equals("manual")){settings.activate(this);startManual();}}
    @Override protected void onResume(){super.onResume();visibleActivity=this;if(settings!=null)render();}
    @Override protected void onPause(){if(visibleActivity==this)visibleActivity=null;super.onPause();}
    @Override protected void onDestroy(){if(visibleActivity==this)visibleActivity=null;super.onDestroy();}
}
