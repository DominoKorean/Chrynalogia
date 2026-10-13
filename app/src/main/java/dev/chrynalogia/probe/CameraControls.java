package dev.chrynalogia.probe;

import android.content.Intent;
import android.graphics.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;

/** Only these small trusted accessibility windows accept touch; the grid never does. */
final class CameraControls {
    private final CameraWatchService service;
    private final WindowManager manager;
    private LinearLayout toolbar, menu, popup;
    private SettingsPanel settingsPanel;
    private boolean expanded;
    private int builds;
    int buildCount(){return builds;}
    private Rect window;
    private Rect header;
    private String layoutKey="";
    private final android.os.Handler uiHandler=new android.os.Handler(android.os.Looper.getMainLooper());

    CameraControls(CameraWatchService service,WindowManager manager) {this.service=service;this.manager=manager;}
    boolean popupOpen() {return popup!=null;}
    boolean attached() {return toolbar!=null;}
    void dumpUi(StringBuilder output) {dumpView(toolbar,output,0);dumpView(menu,output,0);dumpView(popup,output,0);if(settingsPanel!=null)dumpView(settingsPanel.dialogView(),output,0);}
    private void dumpView(View view,StringBuilder out,int depth) {
        if(view==null || depth>40)return;
        Rect bounds=new Rect();boolean visible=view.isShown()&&view.getGlobalVisibleRect(bounds);
        if(visible) {
            View root=view.getRootView();int[] rootPosition=new int[2];root.getLocationOnScreen(rootPosition);
            Rect rootBounds=new Rect();root.getGlobalVisibleRect(rootBounds);bounds.offset(rootPosition[0]-rootBounds.left,rootPosition[1]-rootBounds.top);
        }
        if(!visible) {int[] xy=new int[2];view.getLocationOnScreen(xy);bounds.set(xy[0],xy[1],xy[0]+view.getWidth(),xy[1]+view.getHeight());}
        String text=view instanceof TextView t?t.getText().toString().replace('\n',' '):"null";
        out.append("  ".repeat(depth)).append(view.getClass().getName()).append(" id=null bounds=").append(bounds.toShortString())
            .append(" visible=").append(visible).append(" clickable=").append(view.isClickable()).append(" selected=").append(view.isSelected())
            .append(" checked=").append(view instanceof CompoundButton b && b.isChecked()).append(" text=").append(text)
            .append(" desc=").append(view.getContentDescription()).append('\n');
        if(view instanceof android.view.ViewGroup group)for(int i=0;i<group.getChildCount();i++)dumpView(group.getChildAt(i),out,depth+1);
    }
    void show(Rect bounds,AccessibilityNodeInfo root) {
        window=new Rect(bounds);header=null;findHeader(root,0);
        String key=bounds.toShortString()+":"+(header==null?"none":header.toShortString())+":"+new GridSettings(service).allowStop+":"+new GridSettings(service).runMode;
        if(toolbar==null || !key.equals(layoutKey)) {layoutKey=key;buildToolbar();}
    }
    private void findHeader(AccessibilityNodeInfo node,int depth) {
        if(node==null||depth>40)return;
        if(node.isVisibleToUser() && String.valueOf(node.getViewIdResourceName()).endsWith("/quick_setting_button_main")) {
            Rect b=new Rect();node.getBoundsInScreen(b);
            if(b.top<window.top+window.height()/4 && !b.isEmpty() && (header==null||b.top<header.top))header=b;
        }
        for(int i=0;i<node.getChildCount();i++)findHeader(node.getChild(i),depth+1);
    }
    void refresh() {if(toolbar!=null)buildToolbar();}
    @android.annotation.SuppressLint("RtlHardcoded") // Camera anchors are physical display coordinates.
    private WindowManager.LayoutParams params(int w,int h,int x,int y,boolean focus,String title) {
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                |(focus?0:WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE),PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.LEFT;p.x=x;p.y=y;p.setFitInsetsTypes(0);
        p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        p.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;p.setTitle(title);return p;
    }
    private void buildToolbar() {
        if(window==null)return;builds++;remove(toolbar);remove(menu);menu=null;toolbar=Ui.column(Ui.context(service));
        int openerSize=Ui.dp(service,36),size=Ui.dp(service,48);
        toolbar.setGravity(Gravity.CENTER);toolbar.setContentDescription(service.getString(expanded?R.string.close_grid_menu:R.string.open_grid_menu));toolbar.setFocusable(true);toolbar.setOnClickListener(v->{expanded=!expanded;buildToolbar();});
        ImageView logo=new ImageView(toolbar.getContext());logo.setImageResource(R.drawable.app_logo);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        toolbar.setClipChildren(false);toolbar.addView(logo,new LinearLayout.LayoutParams(Ui.dp(service,44),Ui.dp(service,44)));
        if(expanded) {
            menu=Ui.column(Ui.context(service));menu.setBackground(Ui.shape(0xad000000,24,service));
            GridSettings s=new GridSettings(service);
            addIcon(s.hidden?R.raw.kit_show:R.raw.kit_hide,service.getString(s.hidden?R.string.show_grid:R.string.hide_grid),service.getString(R.string.visibility),false,()->{GridSettings g=new GridSettings(service);g.hidden=!g.hidden;g.save();service.refreshSettings();},size);
            addIcon(R.raw.kit_rotate,service.getString(R.string.rotate_grid),service.getString(R.string.rotate),false,()->{GridSettings g=new GridSettings(service);g.rotation=(g.rotation+1)%4;g.save();service.refreshSettings();},size);
            addIcon(R.raw.kit_flip,service.getString(R.string.flip_horizontal),service.getString(R.string.flip_x_short),false,()->{GridSettings g=new GridSettings(service);g.flipX=!g.flipX;g.save();service.refreshSettings();},size);
            addIcon(R.raw.kit_flip,service.getString(R.string.flip_vertical),service.getString(R.string.flip_y_short),true,()->{GridSettings g=new GridSettings(service);g.flipY=!g.flipY;g.save();service.refreshSettings();},size);
            addIcon(R.raw.kit_settings,service.getString(R.string.open_grid_settings),service.getString(R.string.tab_settings),false,this::openPopup,size);
            if(s.allowStop||s.runMode.equals("manual"))addIcon(R.raw.kit_power,service.getString(R.string.stop_overlay),service.getString(R.string.stop),false,()->{service.setPaused(true);service.stopService(new Intent(service,ManualSessionService.class));},size);
        }
        Insets bars=manager.getCurrentWindowMetrics().getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
        int x=window.left+bars.left+Ui.dp(service,8);int y=header==null?window.top+bars.top+Ui.dp(service,8):header.top;
        if(window.width()>window.height())y=Math.max(window.top+bars.top,window.bottom-bars.bottom-Ui.dp(service,8)-openerSize);
        manager.addView(toolbar,params(openerSize,openerSize,x,y,false,"Chrynalogia-CameraControls"));
        if(menu!=null){int height=menu.getChildCount()*size;int my=y+openerSize+Ui.dp(service,8);if(my+height>window.bottom-bars.bottom)my=y-height-Ui.dp(service,8);manager.addView(menu,params(size,height,x+(openerSize-size)/2,Math.max(window.top+bars.top,my),false,"Chrynalogia-CameraMenu"));}
    }
    private void addIcon(int resource,String description,String label,boolean rotate,Runnable action,int size) {
        android.content.Context c=menu.getContext();LinearLayout cell=Ui.column(c);cell.setGravity(Gravity.CENTER);cell.setContentDescription(description);cell.setFocusable(true);
        KitIcon image=new KitIcon(c,resource,Color.WHITE);if(rotate)image.setRotation(90);cell.addView(image,new LinearLayout.LayoutParams(Ui.dp(c,22),Ui.dp(c,22)));
        if(!label.isEmpty()){TextView text=Ui.centered(c,label,9);text.setTextColor(Color.WHITE);text.setPadding(0,Ui.dp(c,2),0,0);text.setMaxLines(1);cell.addView(text);}
        cell.setOnClickListener(v->action.run());menu.addView(cell,new LinearLayout.LayoutParams(size,size));
    }
    void openPopup() {
        if(popup!=null) {closePopup();return;}
        popup=Ui.column(Ui.context(service));popup.setBackground(Ui.shape(Ui.bg(service),28,service));popup.setClipToOutline(true);
        LinearLayout top=new LinearLayout(service);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=Ui.text(service,service.getString(R.string.app_name),20);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        KitIcon close=new KitIcon(service,R.raw.kit_close,Ui.ink(service));close.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);close.setContentDescription(service.getString(R.string.close));close.setPadding(Ui.dp(service,12),Ui.dp(service,12),Ui.dp(service,12),Ui.dp(service,12));close.setOnClickListener(v->closePopup());top.addView(close,new LinearLayout.LayoutParams(Ui.dp(service,48),Ui.dp(service,48)));popup.addView(top);
        settingsPanel=new SettingsPanel(service,new SettingsPanel.Actions() {
            public void changed(){service.refreshSettings();}
            public void importGrid(){closePopup();Intent i=new Intent(service,ImportActivity.class).putExtra("returnCamera",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);service.startActivity(i);}
            public void modeChanged(String mode){
                GridSettings s=new GridSettings(service);
                if(mode.equals("manual")) {
                    // The activity handles the runtime notification permission before creating a session.
                    closePopup();service.startActivity(new Intent(service,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } else SessionController.changeMode(service,mode);
                service.refreshSettings();
            }
            public void stop(){service.setPaused(true);service.stopService(new Intent(service,ManualSessionService.class));}
            public void startSession(){closePopup();service.startActivity(new Intent(service,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
            public void appearanceChanged(){closePopup();service.refreshSettings();uiHandler.postDelayed(()->{if(toolbar!=null)openPopup();},400);}
            public void permissions(boolean overlay){closePopup();service.startActivity(new Intent(service,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
        },true);popup.addView(settingsPanel,new LinearLayout.LayoutParams(-1,0,1));
        popup.setFocusableInTouchMode(true);
        popup.setOnKeyListener((view,code,event)->{if(code==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP)closePopup();return true;}return false;});
        Insets bars=manager.getCurrentWindowMetrics().getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
        int x=window.left+bars.left+Ui.dp(service,8),y=window.top+bars.top+Ui.dp(service,48);
        int w=Math.min(Ui.dp(service,420),window.width()-bars.left-bars.right-Ui.dp(service,16));
        int h=window.bottom-bars.bottom-y-Ui.dp(service,12);
        if(h<Ui.dp(service,140)) {y=window.top+bars.top+Ui.dp(service,4);h=window.height()-bars.top-bars.bottom-Ui.dp(service,8);}
        manager.addView(popup,params(w,h,x,y,true,"Chrynalogia-CameraSettings"));popup.requestFocus();
    }
    void closePopup() {uiHandler.removeCallbacksAndMessages(null);if(settingsPanel!=null)settingsPanel.dismissChoices();settingsPanel=null;remove(popup);popup=null;}
    void hide() {uiHandler.removeCallbacksAndMessages(null);closePopup();remove(toolbar);remove(menu);toolbar=null;menu=null;expanded=false;layoutKey="";}
    private void remove(View v) {if(v!=null)try{manager.removeViewImmediate(v);}catch(IllegalArgumentException ignored){}}

}

