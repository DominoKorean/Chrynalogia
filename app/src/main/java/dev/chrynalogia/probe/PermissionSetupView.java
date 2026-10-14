package dev.chrynalogia.probe;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.*;
import java.util.function.IntConsumer;

/** In-app illustrated walkthrough; it works before either sensitive permission is granted. */
@android.annotation.SuppressLint("ViewConstructor")
final class PermissionSetupView extends LinearLayout {
    interface Actions {void applications();void accessibility();void overlay();void finish();}
    private record Step(int title,int description,String image,Rect crop,RectF target,int settings) {}
    private static Rect crop(int l,int t,int r,int b){return new Rect(l,t,r,b);}
    private static RectF target(int l,int t,int r,int b){return new RectF(l,t,r,b);}
    private static final Step[] STEPS={
        new Step(R.string.guide_check_title,R.string.guide_check_body,"restricted",crop(26,1350,1054,2210),target(80,1510,1000,1650),1),
        new Step(R.string.guide_home_title,R.string.guide_home_body,"settings_home",crop(26,840,1054,1155),target(48,1005,1030,1138),0),
        new Step(R.string.guide_apps_title,R.string.guide_apps_body,"apps",crop(26,480,1054,760),target(48,570,1030,732),0),
        new Step(R.string.guide_info_title,R.string.guide_info_body,"app_info",crop(26,110,1054,460),target(934,128,1030,279),0),
        new Step(R.string.guide_restricted_title,R.string.guide_restricted_body,"allow_restricted",crop(595,115,1054,300),target(626,132,1030,265),0),
        new Step(R.string.guide_installed_title,R.string.guide_installed_body,"accessibility",crop(26,1210,1054,1580),target(50,1372,1030,1550),1),
        new Step(R.string.guide_select_title,R.string.guide_select_body,"installed_apps",crop(26,658,1054,828),target(48,676,1030,812),1),
        new Step(R.string.guide_switch_title,R.string.guide_switch_body,"access_switch",crop(26,119,1054,450),target(885,305,1020,415),1),
        new Step(R.string.guide_confirm_title,R.string.guide_confirm_body,"access_confirm",crop(65,2010,1015,2170),target(746,2044,991,2140),1),
        new Step(R.string.guide_overlay_title,R.string.guide_overlay_body,"overlay",crop(26,1559,1054,1743),target(885,1590,1020,1700),2)
    };
    private final Actions actions;
    private final IntConsumer pageChanged;
    private final boolean overlayAllowed,accessConnected,firstSetup;
    private int page;
    PermissionSetupView(Context c,int page,boolean firstSetup,boolean overlayAllowed,boolean accessConnected,
            Actions actions,IntConsumer pageChanged) {
        super(c);setOrientation(VERTICAL);this.page=Math.max(0,Math.min(STEPS.length-1,page));
        this.firstSetup=firstSetup;this.overlayAllowed=overlayAllowed;this.accessConnected=accessConnected;
        this.actions=actions;this.pageChanged=pageChanged;render();
    }
    int page(){return page;}
    static int stepCount(){return STEPS.length;}
    private void move(int target){page=Math.max(0,Math.min(STEPS.length-1,target));pageChanged.accept(page);render();}
    private void render() {
        removeAllViews();Context c=getContext();Step step=STEPS[page];
        ScrollView scroll=new ScrollView(c);scroll.setFillViewport(true);
        LinearLayout content=Ui.column(c);content.setPadding(0,Ui.dp(c,16),0,Ui.dp(c,16));scroll.addView(content);
        addView(scroll,new LayoutParams(-1,0,1));
        Ui.title(content,c.getString(R.string.guide_title));
        Ui.note(content,c.getString(R.string.guide_switch_apps));
        if(page==0){Ui.note(content,c.getString(R.string.guide_reason));Ui.note(content,c.getString(R.string.guide_privacy));}
        LinearLayout card=Ui.section(content);
        TextView count=Ui.text(c,c.getString(R.string.guide_step,page+1,STEPS.length),14);count.setTextColor(Ui.ACCENT);card.addView(count);
        TextView heading=Ui.text(c,c.getString(step.title()),24);heading.setTypeface(null,Typeface.BOLD);card.addView(heading);
        TextView body=Ui.text(c,c.getString(step.description()),18);card.addView(body);
        PermissionGuideImage screenshot=new PermissionGuideImage(c,step.image(),step.crop(),step.target(),
                c.getString(R.string.guide_image_description,c.getString(step.title())));
        LayoutParams photo=new LayoutParams(-1,-2);photo.setMargins(0,Ui.dp(c,12),0,Ui.dp(c,12));card.addView(screenshot,photo);
        Ui.note(card,c.getString(R.string.guide_example));
        int label=step.settings()==0?R.string.guide_open_settings:step.settings()==1?R.string.allow_access:R.string.allow_overlay;
        // Keep the actual settings action reachable while the illustration scrolls.
        Ui.addButton(this,c.getString(label),()->{
            if(step.settings()==0)actions.applications();else if(step.settings()==1)actions.accessibility();else actions.overlay();
        });
        if(page==0)Ui.addButton(card,c.getString(R.string.guide_not_blocked),()->move(5));
        Ui.note(content,c.getString(accessConnected?R.string.access_connected:R.string.access_needed));
        Ui.note(content,c.getString(overlayAllowed?R.string.overlay_allowed:R.string.overlay_needed));
        LinearLayout navigation=new LinearLayout(c);navigation.setGravity(Gravity.CENTER_VERTICAL);
        navigation.setPadding(Ui.dp(c,16),Ui.dp(c,8),Ui.dp(c,16),Ui.dp(c,4));
        Button previous=Ui.button(c,c.getString(R.string.guide_previous),()->move(page-1));previous.setEnabled(page>0);previous.setAlpha(page>0?1f:0.4f);
        navigation.addView(previous,new LayoutParams(0,-2,1));
        boolean last=page==STEPS.length-1;
        Button next=Ui.button(c,c.getString(last?(firstSetup?R.string.guide_choose_mode:R.string.close):R.string.guide_next),()->{if(last)actions.finish();else move(page+1);});
        next.setTextColor(Ui.ACCENT);LayoutParams nextParams=new LayoutParams(0,-2,1);nextParams.leftMargin=Ui.dp(c,8);navigation.addView(next,nextParams);addView(navigation);
        Button skip=Ui.button(c,c.getString(firstSetup?R.string.guide_later:R.string.close),actions::finish);
        skip.setTextSize(14);skip.setTextColor(Ui.secondary(c));skip.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        if(!last||firstSetup)addView(skip,new LayoutParams(-1,-2));
    }
}
