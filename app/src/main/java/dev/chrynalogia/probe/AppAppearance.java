package dev.chrynalogia.probe;

import android.app.LocaleManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.LocaleList;

/** App-only appearance and Android's persisted per-app language selection. */
final class AppAppearance {
    static String theme(Context c) {return c.getSharedPreferences("probe",0).getString("appTheme","system");}
    static boolean dark(Context c) {
        String mode=theme(c);
        return mode.equals("dark") || (mode.equals("system") &&
            (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES);
    }
    static void setTheme(Context c,String mode) {c.getSharedPreferences("probe",0).edit().putString("appTheme",mode).apply();}
    static String language(Context c) {return c.getSystemService(LocaleManager.class).getApplicationLocales().toLanguageTags();}
    static void setLanguage(Context c,String tag) {c.getSystemService(LocaleManager.class).setApplicationLocales(LocaleList.forLanguageTags(tag));}
}
