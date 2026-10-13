package dev.chrynalogia.probe;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;

final class SessionChecks {
    static int run(Context c,Instrumentation runner) throws Exception {
        SettingsStore.update(c,s->{s.onboarding=true;s.runMode="manual";});
        MainActivity activity=(MainActivity)runner.startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        runner.runOnMainSync(()->SessionController.stop(c));
        runner.waitForIdleSync();
        SettingsChecks.require(!SessionController.active(c),"Stop did not deactivate manual session");
        runner.runOnMainSync(activity::recreate);runner.waitForIdleSync();Thread.sleep(500);runner.waitForIdleSync();
        SettingsChecks.require(!SessionController.active(c),"Recreation restarted stopped manual session");
        SettingsChecks.require(Boolean.TRUE.equals(c.getSharedPreferences("probe",0).getAll().get("paused")),"Recreation cleared explicit pause");
        runner.runOnMainSync(()->SessionController.changeMode(c,"always"));
        runner.waitForIdleSync();SettingsChecks.require(SessionController.active(c),"Always mode remained dependent on manual service");
        runner.runOnMainSync(()->SessionController.stop(c));runner.waitForIdleSync();
        return 4;
    }
}
