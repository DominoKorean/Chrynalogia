package dev.chrynalogia.probe;

import android.app.ActivityManager;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

/** Checks actual task ownership rather than only inspecting intent flags. */
final class SettingsTaskChecks {
    static int run(Context c,Instrumentation runner) {
        c.getSharedPreferences("probe",0).edit().putBoolean("onboarding",false).commit();
        MainActivity activity=(MainActivity)runner.startActivitySync(new Intent(c,MainActivity.class)
                .putExtra(MainActivity.PERMISSION_GUIDE,true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        try {
            runner.runOnMainSync(()->SystemSettings.restrictedSettings(activity));runner.waitForIdleSync();
            ActivityManager.RecentTaskInfo own=null;
            for(ActivityManager.AppTask task:c.getSystemService(ActivityManager.class).getAppTasks())
                if(task.getTaskInfo().taskId==activity.getTaskId())own=task.getTaskInfo();
            SettingsChecks.require(own!=null,"Guide task remains in Recent apps");
            SettingsChecks.require(own.topActivity!=null&&c.getPackageName().equals(own.topActivity.getPackageName()),
                    "Settings must not replace the top of the guide task");
            // waitForIdleSync covers our process, not the Settings launch animation.
            String foreground="unavailable";long deadline=android.os.SystemClock.uptimeMillis()+5000;
            do{
                android.view.accessibility.AccessibilityNodeInfo root=runner.getUiAutomation().getRootInActiveWindow();
                if(root!=null&&root.getPackageName()!=null)foreground=root.getPackageName().toString();
                if("com.android.settings".equals(foreground))break;
                android.os.SystemClock.sleep(100);
            }while(android.os.SystemClock.uptimeMillis()<deadline);
            SettingsChecks.require("com.android.settings".equals(foreground),
                    "Native Settings is foreground in a separate task; observed "+foreground);
        } finally {runner.runOnMainSync(activity::finish);}
        return 3;
    }
}
