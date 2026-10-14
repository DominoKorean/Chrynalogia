package dev.chrynalogia.probe;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** Runs against a separate test installation, never changing the user's permissions. */
final class PermissionSetupChecks {
    static int run(Context c, Instrumentation runner) {
        c.getSharedPreferences("probe",0).edit().putBoolean("onboarding",false)
                .remove("permissionSetupSeen").commit();
        MainActivity[] current={(MainActivity)runner.startActivitySync(new Intent(c,MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK))};
        try {
            onMain(runner,()->{
                MainActivity activity=current[0];View root=activity.getWindow().getDecorView();
                SettingsChecks.require(find(root,activity.getString(R.string.allow_access))!=null,
                        "First launch must offer accessibility setup before choosing a run mode");
                SettingsChecks.require(find(root,activity.getString(R.string.always))==null,
                        "Always on must not precede the permission explanation");
                Intent apps=SystemSettings.restrictedSettingsEntry();
                SettingsChecks.require(android.provider.Settings.ACTION_SETTINGS.equals(apps.getAction())&&apps.getData()==null,
                        "Restricted settings help must enter native Settings home, not direct app details");
                SettingsChecks.require((apps.getFlags()&Intent.FLAG_ACTIVITY_NEW_TASK)!=0,
                        "Settings must open as a separate recent task so the guide remains available");
                find(root,activity.getString(R.string.guide_not_blocked)).performClick();
                SettingsChecks.require(find(root,activity.getString(R.string.guide_installed_title))!=null,
                        "Unrestricted users can skip directly to Installed apps");
                find(root,activity.getString(R.string.guide_previous)).performClick();
                android.os.Bundle state=new android.os.Bundle();runner.callActivityOnSaveInstanceState(activity,state);
                SettingsChecks.require(state.getInt("guidePage",-1)==4,"Current guide page is saved for recreation");
            });
            current[0]=recreate(current[0],runner);
            onMain(runner,()->{
                MainActivity activity=current[0];View root=activity.getWindow().getDecorView();
                SettingsChecks.require(find(root,activity.getString(R.string.guide_restricted_title))!=null,
                        "Recreated activity keeps the illustrated step");
                find(root,activity.getString(R.string.guide_later)).performClick();
                SettingsChecks.require(find(activity.getWindow().getDecorView(),activity.getString(R.string.always))!=null,
                        "Deferring setup reveals run-mode choice");
                SettingsChecks.require(!c.getSharedPreferences("probe",0).getBoolean("onboarding",true),
                        "Finishing the guide must not choose a mode or start a session");
                SettingsChecks.require(c.getSharedPreferences("probe",0).getBoolean("permissionSetupSeen",false),
                        "Guide completion survives process recreation");
            });
            current[0]=recreate(current[0],runner);
            onMain(runner,()->{
                SettingsChecks.require(find(current[0].getWindow().getDecorView(),current[0].getString(R.string.always))!=null,
                        "Run-mode choice stays available after recreation");
            });
            c.getSharedPreferences("probe",0).edit().putBoolean("onboarding",true).putString("runMode","manual")
                    .putBoolean("paused",true).commit();
            current[0]=(MainActivity)runner.startActivitySync(new Intent(c,MainActivity.class)
                    .putExtra(MainActivity.PERMISSION_GUIDE,true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
            onMain(runner,()->{
                MainActivity activity=current[0];
                SettingsChecks.require(find(activity.getWindow().getDecorView(),activity.getString(R.string.guide_title))!=null,
                        "Configured users can reopen the guide");
                SettingsChecks.require(c.getSharedPreferences("probe",0).getBoolean("paused",false),
                        "Opening help does not restart a stopped manual session");
                find(activity.getWindow().getDecorView(),activity.getString(R.string.close)).performClick();
                SettingsChecks.require(find(activity.getWindow().getDecorView(),activity.getString(R.string.guide_title))==null,
                        "Closing optional help returns to the configured app");
                SettingsChecks.require(c.getSharedPreferences("probe",0).getBoolean("paused",false),
                        "Closing help also preserves the stopped session");
                SettingsChecks.require("manual".equals(new GridSettings(c).runMode),"Help preserves the selected run mode");
                for(String language:new String[]{"ko","en"})for(boolean dark:new boolean[]{false,true}){
                    android.content.res.Configuration config=new android.content.res.Configuration(c.getResources().getConfiguration());
                    config.setLocale(java.util.Locale.forLanguageTag(language));
                    config.uiMode=(config.uiMode&~android.content.res.Configuration.UI_MODE_NIGHT_MASK)|
                            (dark?android.content.res.Configuration.UI_MODE_NIGHT_YES:android.content.res.Configuration.UI_MODE_NIGHT_NO);
                    Context localized=c.createConfigurationContext(config);
                    for(int page=0;page<PermissionSetupView.stepCount();page++){
                        PermissionSetupView guide=new PermissionSetupView(localized,page,true,false,false,new PermissionSetupView.Actions(){
                            public void applications(){}public void accessibility(){}public void overlay(){}public void finish(){}
                        },ignored->{});
                        int w=Ui.dp(localized,320),h=Ui.dp(localized,640);
                        guide.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));
                        guide.layout(0,0,w,h);
                        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(w,h,android.graphics.Bitmap.Config.ARGB_8888);
                        guide.draw(new android.graphics.Canvas(bitmap));bitmap.recycle();
                        PermissionGuideImage image=findImage(guide);
                        String[] images={"restricted","settings_home","apps","app_info","allow_restricted",
                                "accessibility","installed_apps","access_switch","access_confirm","overlay"};
                        try(java.io.InputStream in=localized.getAssets().open("permission_guide/"+
                                (language.equals("ko")?"":"en/")+images[page]+".png")){
                            android.graphics.Bitmap expected=android.graphics.BitmapFactory.decodeStream(in);
                            java.lang.reflect.Field field=PermissionGuideImage.class.getDeclaredField("screenshot");
                            field.setAccessible(true);
                            SettingsChecks.require(image!=null&&expected.sameAs((android.graphics.Bitmap)field.get(image)),
                                    "Guide screenshot must match the app locale: "+language+" / "+images[page]);
                            expected.recycle();
                        } catch(Exception error){throw new AssertionError("Localized guide image",error);}
                        SettingsChecks.require(find(guide,localized.getString(R.string.guide_next))!=null||
                                find(guide,localized.getString(R.string.guide_choose_mode))!=null,"Every illustrated step has navigation");
                    }
                }
            });
        } finally {onMain(runner,current[0]::finish);}
        return 96;
    }
    private static PermissionGuideImage findImage(View view){
        if(view instanceof PermissionGuideImage image)return image;
        if(view instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){
            PermissionGuideImage image=findImage(group.getChildAt(i));if(image!=null)return image;
        }
        return null;
    }
    private static MainActivity recreate(MainActivity activity,Instrumentation runner){
        Instrumentation.ActivityMonitor monitor=runner.addMonitor(MainActivity.class.getName(),null,false);
        try{
            onMain(runner,activity::recreate);
            MainActivity result=(MainActivity)runner.waitForMonitorWithTimeout(monitor,5000);
            if(result==null)throw new AssertionError("Recreated activity did not resume");
            runner.waitForIdleSync();return result;
        } finally {runner.removeMonitor(monitor);}
    }
    private static void onMain(Instrumentation runner,Runnable action) {
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        runner.runOnMainSync(()->{try{action.run();}catch(Throwable error){failure.set(error);}});
        if(failure.get()!=null)throw new AssertionError("Permission setup regression",failure.get());
    }
    static View find(View v,String text) {
        if(v instanceof TextView t&&text.contentEquals(t.getText()))return v;
        if(v instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){
            View found=find(group.getChildAt(i),text);if(found!=null)return found;
        }
        return null;
    }
}
