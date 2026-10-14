package dev.chrynalogia.probe;

import android.content.ActivityNotFoundException;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

/** Opens the scoped One UI pages without changing permission or session state. */
final class SystemSettings {
    static void language(Context c) {
        open(c,new Intent(Settings.ACTION_APP_LOCALE_SETTINGS,Uri.parse("package:"+c.getPackageName())),
            new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+c.getPackageName())));
    }
    static void accessibility(Context c) {
        // The service-details action requires a signature permission on One UI 8.5.
        // Use its public entry point; the user controls Chrynalogia under Installed apps.
        open(c,new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
            new Intent(Settings.ACTION_SETTINGS));
    }
    private static void open(Context c,Intent specific,Intent fallback) {
        if(!(c instanceof Activity)){specific.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);}
        try {c.startActivity(specific);}
        catch(ActivityNotFoundException|SecurityException e){c.startActivity(fallback);}
    }
}
