package dev.chrynalogia.probe;

import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** Unsupported firmware is injected through the detector, without spoofing the device OS. */
final class CompatibilityChecks {
    static int run(Context c, Instrumentation runner) {
        SharedPreferences prefs=c.getSharedPreferences("probe",0);
        prefs.edit().putBoolean("onboarding",false).remove("compatibilityNoticeSeen").commit();
        MainActivity activity=(MainActivity)runner.startActivitySync(new Intent(c,MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        CompatibilityNotice[] notice=new CompatibilityNotice[1];
        AlertDialog[] shown=new AlertDialog[1];
        try {
            onMain(runner,()->{
                SettingsChecks.require(DeviceCompatibility.hasOneUi(c),"S23 must identify actual One UI firmware");
                SettingsChecks.require(prefs.getBoolean("compatibilityNoticeSeen",false),"Supported first launch completes compatibility check silently");
                prefs.edit().remove("compatibilityNoticeSeen").commit();
                notice[0]=new CompatibilityNotice(activity,()->false);
                AlertDialog dialog=notice[0].showIfNeeded();
                SettingsChecks.require(dialog!=null&&dialog.isShowing(),"Non-One UI first launch displays a warning");
                SettingsChecks.require(find(dialog.getWindow().getDecorView(),activity.getString(R.string.compatibility_message))!=null,"Warning explains Samsung Camera requirement");
                SettingsChecks.require(notice[0].showIfNeeded()==dialog,"Repeated resume must not stack warnings");
                notice[0].destroy();
                SettingsChecks.require(!prefs.getBoolean("compatibilityNoticeSeen",false),"Activity recreation must not acknowledge an unseen warning");
                notice[0]=new CompatibilityNotice(activity,()->false);dialog=notice[0].showIfNeeded();shown[0]=dialog;
                View proceed=find(dialog.getWindow().getDecorView(),activity.getString(R.string.compatibility_continue));
                SettingsChecks.require(proceed!=null&&proceed.performClick(),"Continue action is available on an unsupported device");
            });
            // Dialog's dismiss listener is dispatched on the next main-loop turn.
            runner.waitForIdleSync();
            onMain(runner,()->{
                SettingsChecks.require(!shown[0].isShowing()&&prefs.getBoolean("compatibilityNoticeSeen",false),"Continue dismisses and remembers the warning");
                SettingsChecks.require(!prefs.getBoolean("onboarding",true),"Warning does not choose a mode or alter onboarding");
                SettingsChecks.require(find(activity.getWindow().getDecorView(),activity.getString(R.string.choose_manual))!=null,"Normal onboarding remains available after warning");
                SettingsChecks.require(notice[0].showIfNeeded()==null,"Acknowledged warning never repeats on launch");
                View mode=find(activity.getWindow().getDecorView(),activity.getString(R.string.always));
                SettingsChecks.require(mode!=null&&mode.performClick()&&prefs.getBoolean("onboarding",false),"Unsupported warning does not block normal mode selection");
                prefs.edit().putBoolean("onboarding",false).commit();
                prefs.edit().remove("compatibilityNoticeSeen").commit();
                notice[0].showIfNeeded().cancel();
            });
            runner.waitForIdleSync();
            onMain(runner,()->{
                SettingsChecks.require(prefs.getBoolean("compatibilityNoticeSeen",false),"Back or outside dismissal also allows continuing without repeating");
                prefs.edit().remove("compatibilityNoticeSeen").putBoolean("onboarding",true).commit();
                SettingsChecks.require(notice[0].showIfNeeded()==null,"Existing configured installations are not interrupted by a first-install warning");
            });
        } finally {onMain(runner,()->{if(notice[0]!=null)notice[0].destroy();activity.finish();});}
        return 14;
    }
    private static void onMain(Instrumentation runner,Runnable action) {
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        runner.runOnMainSync(()->{try{action.run();}catch(Throwable error){failure.set(error);}});
        if(failure.get()!=null)throw new AssertionError("Compatibility notice regression",failure.get());
    }
    private static View find(View v,String text) {
        if(v instanceof TextView t&&text.contentEquals(t.getText()))return v;
        if(v instanceof ViewGroup g)for(int i=0;i<g.getChildCount();i++){View found=find(g.getChildAt(i),text);if(found!=null)return found;}
        return null;
    }
}
