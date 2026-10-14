package dev.chrynalogia.probe;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.function.BooleanSupplier;

/** First-install advisory only: it never restricts permissions, sessions or onboarding. */
final class CompatibilityNotice {
    private final Activity activity;
    private final SharedPreferences prefs;
    private final BooleanSupplier oneUi;
    private AlertDialog dialog;
    CompatibilityNotice(Activity activity,BooleanSupplier oneUi) {
        this.activity=activity;this.oneUi=oneUi;prefs=activity.getSharedPreferences("probe",0);
    }
    AlertDialog showIfNeeded() {
        if(dialog!=null&&dialog.isShowing())return dialog;
        if(activity.isFinishing()||activity.isDestroyed()||prefs.getBoolean("onboarding",false)
                ||prefs.getBoolean("compatibilityNoticeSeen",false))return null;
        if(oneUi.getAsBoolean()){acknowledge();return null;}
        AlertDialog warning=new AlertDialog.Builder(activity).create();
        LinearLayout body=Ui.column(activity);
        body.setPadding(Ui.dp(activity,24),Ui.dp(activity,24),Ui.dp(activity,24),Ui.dp(activity,12));
        body.setBackground(Ui.shape(Ui.card(activity),28,activity));
        TextView heading=Ui.text(activity,activity.getString(R.string.compatibility_title),24);
        heading.setTypeface(null,Typeface.BOLD);heading.setPadding(0,0,0,Ui.dp(activity,16));body.addView(heading);
        TextView message=Ui.text(activity,activity.getString(R.string.compatibility_message),18);
        message.setPadding(0,0,0,Ui.dp(activity,16));body.addView(message);
        TextView proceed=Ui.text(activity,activity.getString(R.string.compatibility_continue),18);
        proceed.setTextColor(Ui.ACCENT);proceed.setTypeface(null,Typeface.BOLD);
        proceed.setGravity(Gravity.END|Gravity.CENTER_VERTICAL);proceed.setMinimumHeight(Ui.dp(activity,48));
        proceed.setFocusable(true);Ui.clickable(proceed,Color.TRANSPARENT,24);
        proceed.setOnClickListener(v->warning.dismiss());body.addView(proceed);
        ScrollView scroll=new ScrollView(activity);scroll.addView(body);
        warning.setView(scroll,0,0,0,0);warning.setCanceledOnTouchOutside(true);
        warning.setOnDismissListener(d->{acknowledge();if(dialog==warning)dialog=null;});
        dialog=warning;warning.show();
        android.view.Window window=warning.getWindow();
        if(window!=null){
            window.setBackgroundDrawable(Ui.shape(Ui.card(activity),28,activity));window.getDecorView().setPadding(0,0,0,0);
            window.setLayout(Math.min(Ui.dp(activity,420),activity.getResources().getDisplayMetrics().widthPixels-Ui.dp(activity,48)),WindowManager.LayoutParams.WRAP_CONTENT);
        }
        return warning;
    }
    private void acknowledge(){prefs.edit().putBoolean("compatibilityNoticeSeen",true).apply();}
    void destroy() {
        // Activity teardown (including rotation) is not the user's acknowledgment.
        if(dialog!=null){dialog.setOnDismissListener(null);dialog.dismiss();dialog=null;}
    }
}
