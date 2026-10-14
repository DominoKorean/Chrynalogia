package dev.chrynalogia.probe;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

final class SessionChecks {
    static int run(Context c,Instrumentation runner) throws Exception {
        SettingsStore.update(c,s->{s.onboarding=true;s.runMode="manual";});
        // startActivitySync waits for creation; singleTop reuse only delivers onNewIntent.
        MainActivity activity=(MainActivity)runner.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        runner.runOnMainSync(()->SessionController.stop(c));
        runner.waitForIdleSync();
        SettingsChecks.require(!SessionController.active(c),"Stop did not deactivate manual session");
        runner.runOnMainSync(activity::recreate);runner.waitForIdleSync();Thread.sleep(500);runner.waitForIdleSync();
        SettingsChecks.require(!SessionController.active(c),"Recreation restarted stopped manual session");
        SettingsChecks.require(Boolean.TRUE.equals(c.getSharedPreferences("probe",0).getAll().get("paused")),"Recreation cleared explicit pause");
        runner.runOnMainSync(()->SessionController.changeMode(c,"always"));
        runner.waitForIdleSync();SettingsChecks.require(SessionController.active(c),"Always mode remained dependent on manual service");
        runner.runOnMainSync(()->SessionController.changeMode(c,"manual"));waitReady(runner);
        runner.runOnMainSync(()->{SessionController.changeMode(c,"always");SessionController.changeMode(c,"manual");});
        Thread.sleep(500);waitReady(runner);
        SettingsChecks.require(!Boolean.TRUE.equals(c.getSharedPreferences("probe",0).getAll().get("paused"))&&SessionController.active(c),"Older service destruction cancelled latest manual session");
        runner.runOnMainSync(()->SessionController.stop(c));runner.waitForIdleSync();
        return 5;
    }
    private static void waitReady(Instrumentation runner) throws Exception {long end=System.currentTimeMillis()+5000;do{runner.waitForIdleSync();if(SessionController.ready())return;Thread.sleep(20);}while(System.currentTimeMillis()<end);throw new AssertionError("Latest manual session never became ready");}
}
