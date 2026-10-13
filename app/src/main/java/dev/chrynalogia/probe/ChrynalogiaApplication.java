package dev.chrynalogia.probe;
public final class ChrynalogiaApplication extends android.app.Application {
    @Override public void onCreate(){super.onCreate();SettingsStore.initialize(this);}
}
