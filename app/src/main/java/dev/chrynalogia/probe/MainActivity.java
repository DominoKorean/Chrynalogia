package dev.chrynalogia.probe;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.*;

public final class MainActivity extends Activity {
    static final String PERMISSION_GUIDE="permissionGuide";
    private static MainActivity visibleActivity;
    static boolean foreground(){return visibleActivity!=null;}
    static void refreshSessionState(){
        MainActivity a=visibleActivity;if(a!=null)a.uiHandler.post(()->{if(visibleActivity==a){
            if(a.permissionHelp||!a.settings.onboarding)a.render();else if(a.panel!=null)a.panel.refreshExecutionState();
        }});
    }
    private GridSettings settings;
    private boolean pendingManual;
    private boolean returningFromSystemSettings;
    private boolean permissionHelp;
    private int guidePage;
    private final android.os.Handler uiHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    private SettingsPanel panel;
    private CompatibilityNotice compatibilityNotice;
    @Override public void onCreate(Bundle state) {
        setTheme(AppAppearance.dark(this)?R.style.AppDark:R.style.AppLight);
        super.onCreate(state);settings=new GridSettings(this);
        compatibilityNotice=new CompatibilityNotice(this,()->DeviceCompatibility.hasOneUi(this));
        pendingManual=state!=null&&state.getBoolean("pendingManual",false);
        returningFromSystemSettings=state!=null&&state.getBoolean("returningFromSystemSettings",false);
        permissionHelp=state!=null?state.getBoolean(PERMISSION_GUIDE,false):getIntent().getBooleanExtra(PERMISSION_GUIDE,false);
        guidePage=state!=null?state.getInt("guidePage",0):0;
        getIntent().removeExtra(PERMISSION_GUIDE);
        if(settings.onboarding&&!permissionHelp&&state==null&&getLastNonConfigurationInstance()==null) {
            settings.activate(this);if(settings.runMode.equals("manual"))startManual();else refreshService();
        }
        render();
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);setIntent(intent);settings.reload();
        boolean guide=intent.getBooleanExtra(PERMISSION_GUIDE,false);intent.removeExtra(PERMISSION_GUIDE);
        if(guide){permissionHelp=true;guidePage=0;}
        else if(settings.onboarding){settings.activate(this);if(settings.runMode.equals("manual"))startManual();else refreshService();}render();
    }
    private void render() {
        if(panel!=null)panel.dismissChoices();settings.reload();LinearLayout root=Ui.column(this);root.setBackgroundColor(Ui.bg(this));
        root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());v.setPadding(bars.left,bars.top,bars.right,bars.bottom);return insets;});
        if(permissionHelp||(!settings.onboarding&&!settings.prefs.getBoolean("permissionSetupSeen",false))) {
            panel=null;
            root.addView(new PermissionSetupView(this,guidePage,!settings.onboarding,
                    Settings.canDrawOverlays(this),CameraConnection.connected(),new PermissionSetupView.Actions(){
                public void applications(){returningFromSystemSettings=true;SystemSettings.restrictedSettings(MainActivity.this);}
                public void accessibility(){returningFromSystemSettings=true;SystemSettings.accessibility(MainActivity.this);}
                public void overlay(){openOverlayPermission();}
                public void finish(){
                    if(!settings.onboarding)settings.prefs.edit().putBoolean("permissionSetupSeen",true).apply();
                    permissionHelp=false;guidePage=0;render();
                }
            },page->guidePage=page),new LinearLayout.LayoutParams(-1,0,1));
        } else if(!settings.onboarding) {
            panel=null;
            ScrollView scroll=new ScrollView(this);LinearLayout welcome=Ui.column(this);welcome.setPadding(0,Ui.dp(this,48),0,Ui.dp(this,24));scroll.addView(welcome);root.addView(scroll);
            ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.app_logo);welcome.addView(logo,new LinearLayout.LayoutParams(-1,Ui.dp(this,120)));
            Ui.title(welcome,getString(R.string.app_name));Ui.note(welcome,getString(R.string.welcome_intro));Ui.title(welcome,getString(R.string.welcome_mode));
            Ui.note(welcome,getString(R.string.welcome_disclosure));Ui.note(welcome,getString(R.string.welcome_privacy));
            Ui.addButton(welcome,getString(R.string.always),()->chooseMode("always"));Ui.addButton(welcome,getString(R.string.choose_manual),()->chooseMode("manual"));
        } else {panel=new SettingsPanel(this,new SettingsPanel.Actions() {
            public void changed(){refreshService();}
            public void importGrid(){startActivity(new Intent(MainActivity.this,ImportActivity.class));}
            public void modeChanged(String mode){pendingManual=false;SessionController.changeMode(MainActivity.this,mode);settings.reload();if(mode.equals("manual"))startManual();render();}
            public void stop(){stopSession();render();}
            public void startSession(){settings.reload();if(settings.runMode.equals("manual"))startManual();else SessionController.requestStart(MainActivity.this);render();}
            public void openCamera(){MainActivity.this.openCamera();}
            public void appearanceChanged(){refreshService();uiHandler.postDelayed(()->{if(!isFinishing()&&!isDestroyed())recreate();},200);}
            public void permissions(boolean overlay){returningFromSystemSettings=true;if(overlay)SystemSettings.overlay(MainActivity.this);else SystemSettings.accessibility(MainActivity.this);}
            public void openPermissionGuide(){permissionHelp=true;guidePage=0;render();}
            public void openLanguageSettings(){returningFromSystemSettings=true;SystemSettings.language(MainActivity.this);}
            public void disableAccessibility(){returningFromSystemSettings=true;SystemSettings.accessibility(MainActivity.this);}
        });root.addView(panel,new LinearLayout.LayoutParams(-1,0,1));}
        setContentView(root);
        int bars=WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS|WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        getWindow().getInsetsController().setSystemBarsAppearance(AppAppearance.dark(this)?0:bars,bars);
    }
    private void openOverlayPermission(){
        returningFromSystemSettings=true;
        SystemSettings.overlay(this);
    }
    private void openCamera() {
        if(!Settings.canDrawOverlays(this)||!CameraConnection.connected()){Toast.makeText(this,R.string.permissions_first,Toast.LENGTH_LONG).show();return;}
        settings.activate(this);if(settings.runMode.equals("manual")&&!startManual())return;refreshService();
        Intent camera=getPackageManager().getLaunchIntentForPackage(CameraWatchService.CAMERA_PACKAGE);
        if(camera!=null)startActivity(camera);else Toast.makeText(this,R.string.camera_missing,Toast.LENGTH_LONG).show();
    }
    private void chooseMode(String mode) {settings.runMode=mode;settings.onboarding=true;settings.save();settings.activate(this);if(mode.equals("manual"))startManual();refreshService();render();}
    private boolean startManual() {
        if(pendingManual)return false;
        if(checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){SessionController.requestStart(this);pendingManual=true;requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);return false;}
        if(!SessionController.requestStart(this))return false;
        uiHandler.postDelayed(()->{if(!isFinishing()&&!isDestroyed())render();},250);return true;
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==10&&pendingManual){pendingManual=false;settings.reload();if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED&&settings.runMode.equals("manual")&&settings.requested(this))startManual();else{SessionController.stop(this);Toast.makeText(this,R.string.notification_required,Toast.LENGTH_LONG).show();}render();}
    }
    private void stopSession(){pendingManual=false;SessionController.stop(this);}
    private void refreshService(){CameraConnection.requestRefresh();}
    @Override protected void onSaveInstanceState(Bundle state){state.putBoolean("pendingManual",pendingManual);state.putBoolean("returningFromSystemSettings",returningFromSystemSettings);state.putBoolean(PERMISSION_GUIDE,permissionHelp);state.putInt("guidePage",guidePage);super.onSaveInstanceState(state);}
    @Override public Object onRetainNonConfigurationInstance(){return Boolean.TRUE;}
    @Override protected void onRestart(){super.onRestart();settings.reload();if(!permissionHelp&&!returningFromSystemSettings&&settings.onboarding&&settings.runMode.equals("manual")){settings.activate(this);startManual();}}
    @Override protected void onResume(){super.onResume();returningFromSystemSettings=false;visibleActivity=this;if(settings!=null)render();if(compatibilityNotice!=null)compatibilityNotice.showIfNeeded();}
    @Override protected void onPause(){if(visibleActivity==this)visibleActivity=null;super.onPause();}
    @Override protected void onDestroy(){uiHandler.removeCallbacksAndMessages(null);if(compatibilityNotice!=null)compatibilityNotice.destroy();if(panel!=null)panel.dismissChoices();if(visibleActivity==this)visibleActivity=null;super.onDestroy();}
}
