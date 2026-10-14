package dev.chrynalogia.probe;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

/** Opens the scoped One UI pages without changing permission or session state. */
final class SystemSettings {
    static Intent restrictedSettingsEntry() {
        // Start at the native Settings home in its own task. App-detail shortcuts
        // and inherited app tasks can hide One UI's restricted-settings menu.
        return new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }
    static void restrictedSettings(Context c) {
        open(c,restrictedSettingsEntry(),new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS));
    }
    static void overlay(Context c) {
        open(c,new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+c.getPackageName())),
            new Intent(Settings.ACTION_SETTINGS));
    }
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
        // Even Activity callers must leave the guide in its own recent-app card.
        specific.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {c.startActivity(specific);}
        catch(ActivityNotFoundException|SecurityException e){c.startActivity(fallback);}
    }
}
