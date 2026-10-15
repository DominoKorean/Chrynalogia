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
    private Rect preview;
    private Rect safe,headerCandidate;
    private int rightInset;
    private KitIcon logo, visibilityIcon;
    private LinearLayout visibilityCell, stopCell;
    private final java.util.ArrayList<View> rotatingContent=new java.util.ArrayList<>();
    private final java.util.ArrayList<LinearLayout> cells=new java.util.ArrayList<>();
    private final java.util.IdentityHashMap<View,WindowManager.LayoutParams> windows=new java.util.IdentityHashMap<>();
    private CameraMenuPlacement.Placement placement;
    private CameraMenuPlacement.Placement naturalPlacement;
    private int layoutQuarter=-1;
    private int physicalTurn, debugTurn=-1;
    private final OrientationEventListener orientation;
    private final android.os.Handler uiHandler=new android.os.Handler(android.os.Looper.getMainLooper());

    CameraControls(CameraWatchService service,WindowManager manager) {
        this.service=service;this.manager=manager;
        physicalTurn=Math.floorMod(-displayTurn(),4);
        orientation=new OrientationEventListener(service,android.hardware.SensorManager.SENSOR_DELAY_UI){
            @Override public void onOrientationChanged(int degrees){
                if(degrees==ORIENTATION_UNKNOWN)return;
                int nearest=((degrees+45)/90)%4;
                int distance=Math.abs(degrees-nearest*90);distance=Math.min(distance,360-distance);
                if(distance<=30&&physicalTurn!=nearest){physicalTurn=nearest;if(toolbar!=null&&debugTurn<0)rotateIcons();}
            }
        };
    }
    @SuppressWarnings("deprecation") private int displayTurn(){return manager.getDefaultDisplay().getRotation();}
    void debugOrientation(int turn){debugTurn=turn<0?-1:Math.floorMod(turn,4);if(toolbar!=null)rotateIcons();}
    boolean expanded(){return expanded;}
    String menuBounds(){return placement==null?"none":boxRect(placement.menu()).toShortString();}
    String openerBounds(){return placement==null?"none":boxRect(placement.opener()).toShortString();}
    String naturalOpenerBounds(){return naturalPlacement==null?"none":boxRect(naturalPlacement.opener()).toShortString();}
    String naturalMenuBounds(){return naturalPlacement==null?"none":boxRect(naturalPlacement.menu()).toShortString();}
    float iconRotation(){return logo==null?0:logo.getRotation();}
    int uiQuarterTurn(){return debugTurn<0?physicalTurn:debugTurn;}
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
    void show(Rect bounds,Rect frame,AccessibilityNodeInfo root) {
        int quarter=displayTurn();
        if(toolbar!=null&&root==null){if(layoutQuarter!=quarter)layout();else rotateIcons();return;}
        WindowMetrics metrics=manager.getCurrentWindowMetrics();
        // The full-screen camera root can be scaled by its launch animation, or
        // still expose portrait bounds during rotation. Neither is a new device frame.
        if(!bounds.equals(metrics.getBounds())){
            if(toolbar!=null){if(layoutQuarter!=quarter)layout();else rotateIcons();}return;
        }
        Rect nextWindow=boxRect(CameraCoordinates.rotate(box(bounds),box(bounds),-quarter));
        Rect nextPreview=boxRect(CameraCoordinates.rotate(box(frame),box(bounds),-quarter));
        boolean resized=!nextWindow.equals(window),frameChanged=!nextPreview.equals(preview);
        window=nextWindow;preview=nextPreview;
        if(resized||safe==null){
            Insets bars=metrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
            safe=boxRect(CameraCoordinates.rotate(new CameraMenuPlacement.Box(bounds.left+bars.left,bounds.top+bars.top,bounds.right-bars.right,bounds.bottom-bars.bottom),box(bounds),-quarter));
            header=null;rightInset=Ui.dp(service,16);
        }
        headerCandidate=null;findHeader(root,0,bounds,quarter);
        boolean calibrated=header==null&&headerCandidate!=null;
        if(calibrated){header=new Rect(headerCandidate);rightInset=Math.max(0,window.right-header.right);}
        if(toolbar==null)buildToolbar();else if(resized||frameChanged||calibrated||layoutQuarter!=quarter)layout();else rotateIcons();
    }
    private void findHeader(AccessibilityNodeInfo node,int depth,Rect screenWindow,int quarter) {
        if(node==null||depth>40)return;
        if(node.isVisibleToUser() && String.valueOf(node.getViewIdResourceName()).endsWith("/quick_setting_button_main")) {
            Rect screen=new Rect();node.getBoundsInScreen(screen);
            Rect b=boxRect(CameraCoordinates.rotate(box(screen),box(screenWindow),-quarter));
            if(window.contains(b)&&b.top<window.top+window.height()/4&&b.right>window.left+window.width()*0.75f
                    &&window.right-b.right<=Ui.dp(service,32)&&!b.isEmpty()) {
                if(headerCandidate==null)headerCandidate=new Rect(b);else headerCandidate.union(b);
            }
        }
        for(int i=0;i<node.getChildCount();i++)findHeader(node.getChild(i),depth+1,screenWindow,quarter);
    }
    void refresh() {if(toolbar!=null){int visibility=stopCell.getVisibility();updateActions();if(visibility!=stopCell.getVisibility())layout();else rotateIcons();}}
    void resetCameraAnchor(){window=null;preview=null;safe=null;header=null;headerCandidate=null;}
    void configurationChanged(){if(toolbar!=null){if(layoutQuarter!=displayTurn())layout();else rotateIcons();}}
    private void flip(boolean horizontal){
        boolean changeX=horizontal==CameraCoordinates.horizontalFlipChangesX(uiQuarterTurn());
        GridSettings g=new GridSettings(service);if(changeX)g.flipX=!g.flipX;else g.flipY=!g.flipY;
        g.save();service.refreshSettings();
    }
    @android.annotation.SuppressLint("RtlHardcoded") // Camera anchors are physical display coordinates.
    private WindowManager.LayoutParams params(int w,int h,int x,int y,boolean focus,String title) {
        WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                |(focus?0:WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE),PixelFormat.TRANSLUCENT);
        p.gravity=Gravity.TOP|Gravity.LEFT;p.x=x;p.y=y;p.setFitInsetsTypes(0);
        p.layoutInDisplayCutoutMode=WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        p.softInputMode=WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;p.setTitle(title);return p;
    }
    private void buildToolbar() {
        if(window==null)return;builds++;toolbar=Ui.column(Ui.context(service));
        toolbar.setGravity(Gravity.CENTER);toolbar.setFocusable(true);toolbar.setClipChildren(false);
        toolbar.setOnClickListener(v->{expanded=!expanded;updateActions();layout();});
        logo=new KitIcon(toolbar.getContext(),R.raw.camera_circle);toolbar.addView(logo,new LinearLayout.LayoutParams(Ui.dp(service,44),Ui.dp(service,44)));
        menu=Ui.column(Ui.context(service));menu.setBackground(Ui.shape(0xad000000,24,service));
        rotatingContent.clear();cells.clear();
        visibilityCell=addIcon(R.raw.kit_hide,R.string.hide_grid,R.string.visibility,()->{GridSettings g=new GridSettings(service);g.hidden=!g.hidden;g.save();service.refreshSettings();});
        visibilityIcon=(KitIcon)((LinearLayout)visibilityCell.getChildAt(0)).getChildAt(0);
        addIcon(R.raw.kit_rotate,R.string.rotate_grid,R.string.rotate,()->{GridSettings g=new GridSettings(service);g.rotation=(g.rotation+1)%4;g.save();service.refreshSettings();});
        addIcon(R.raw.kit_perspective_vertical,R.string.flip_horizontal,R.string.flip_x_short,()->flip(true));
        addIcon(R.raw.kit_perspective_horizontal,R.string.flip_vertical,R.string.flip_y_short,()->flip(false));
        addIcon(R.raw.kit_settings,R.string.open_grid_settings,R.string.tab_settings,this::openPopup);
        stopCell=addIcon(R.raw.kit_power,R.string.stop_overlay,R.string.stop,()->{service.setPaused(true);service.stopService(new Intent(service,ManualSessionService.class));});
        updateActions();layout();if(orientation.canDetectOrientation())orientation.enable();
    }
    private void updateActions(){
        GridSettings s=new GridSettings(service);
        toolbar.setContentDescription(service.getString(expanded?R.string.close_grid_menu:R.string.open_grid_menu));
        visibilityCell.setContentDescription(service.getString(s.hidden?R.string.show_grid:R.string.hide_grid));
        visibilityIcon.setImageDrawable(KitIcon.fittedDrawable(service,s.hidden?R.raw.kit_show:R.raw.kit_hide,Color.WHITE,20));
        stopCell.setVisibility(s.allowStop||s.runMode.equals("manual")?View.VISIBLE:View.GONE);
    }
    private void layout(){
        if(window==null||toolbar==null)return;
        int top=header==null?safe.top:Math.min(safe.top,header.top);
        CameraMenuPlacement.Box available=new CameraMenuPlacement.Box(safe.left,top,safe.right,safe.bottom);
        int count=stopCell.getVisibility()==View.VISIBLE?6:5;
        int rightLimit=header!=null&&header.left>available.left()+rightInset+Ui.dp(service,56)?header.left-Ui.dp(service,8):available.right();
        naturalPlacement=CameraMenuPlacement.place(available,preview==null?null:box(preview),rightInset,header==null?available.top()+Ui.dp(service,8):header.top,
            Ui.dp(service,48),Ui.dp(service,36),Ui.dp(service,8),count,window.width()>window.height(),rightLimit);
        int quarter=displayTurn();boolean odd=quarter%2!=0;
        placement=new CameraMenuPlacement.Placement(CameraCoordinates.rotate(naturalPlacement.opener(),box(window),quarter),
            CameraCoordinates.rotate(naturalPlacement.menu(),box(window),quarter),naturalPlacement.horizontal()^odd,
            odd?naturalPlacement.cellHeight():naturalPlacement.cellWidth(),odd?naturalPlacement.cellWidth():naturalPlacement.cellHeight());
        boolean reverse=naturalPlacement.horizontal()?(quarter==1||quarter==2):(quarter==2||quarter==3);
        for(int i=0;i<cells.size();i++){
            View wanted=cells.get(reverse?cells.size()-1-i:i);
            if(menu.getChildAt(i)!=wanted){menu.removeView(wanted);menu.addView(wanted,i);}
        }
        updateWindow(toolbar,placement.opener(),false,"Chrynalogia-CameraControls");
        menu.setOrientation(placement.horizontal()?LinearLayout.HORIZONTAL:LinearLayout.VERTICAL);
        for(int i=0;i<menu.getChildCount();i++){
            menu.getChildAt(i).setLayoutParams(new LinearLayout.LayoutParams(placement.cellWidth(),placement.cellHeight()));
            ((LinearLayout)((LinearLayout)menu.getChildAt(i)).getChildAt(0)).getChildAt(1).setVisibility(View.VISIBLE);
        }
        if(expanded)updateWindow(menu,placement.menu(),false,"Chrynalogia-CameraMenu");else if(windows.containsKey(menu))remove(menu);
        rotateIcons();
        if(popup!=null)layoutPopup();
        layoutQuarter=quarter;
        ProbeLog.event(service,"camera_controls_layout","display",quarter,"naturalOpener",naturalOpenerBounds(),"opener",openerBounds());
    }
    private void rotateIcons(){
        float target=CameraCoordinates.iconAngle(displayTurn(),uiQuarterTurn());
        turn(logo,target);for(View v:rotatingContent)turn(v,target);
    }
    private void turn(View v,float angle){
        float current=v.getRotation();float delta=((angle-current)%360+540)%360-180;
        if(Math.abs(delta)>0.5f)v.animate().rotation(current+delta).setDuration(180).start();
    }
    private static CameraMenuPlacement.Box box(Rect r){return new CameraMenuPlacement.Box(r.left,r.top,r.right,r.bottom);}
    private static Rect boxRect(CameraMenuPlacement.Box b){return new Rect(b.left(),b.top(),b.right(),b.bottom());}
    private void updateWindow(View view,CameraMenuPlacement.Box b,boolean focus,String title){
        WindowManager.LayoutParams next=params(b.width(),b.height(),b.left(),b.top(),focus,title);
        WindowManager.LayoutParams old=windows.get(view);
        // addView registers synchronously, but View attachment occurs on a later traversal.
        // Sensor/configuration callbacks can arrive before that first traversal.
        if(old==null){manager.addView(view,next);windows.put(view,next);}
        else {
            if(old.x!=next.x||old.y!=next.y||old.width!=next.width||old.height!=next.height){manager.updateViewLayout(view,next);windows.put(view,next);}
        }
    }
    private LinearLayout addIcon(int resource,int description,int label,Runnable action) {
        android.content.Context c=menu.getContext();LinearLayout cell=Ui.column(c);cell.setGravity(Gravity.CENTER);cell.setClipChildren(false);cell.setContentDescription(service.getString(description));cell.setFocusable(true);
        LinearLayout content=Ui.column(c);content.setGravity(Gravity.CENTER);cell.addView(content);rotatingContent.add(content);
        content.addView(new KitIcon(c,resource,Color.WHITE),new LinearLayout.LayoutParams(Ui.dp(c,22),Ui.dp(c,22)));
        TextView text=Ui.centered(c,service.getString(label),9);text.setTextColor(Color.WHITE);text.setPadding(0,Ui.dp(c,2),0,0);text.setMaxLines(1);content.addView(text);
        cell.setOnClickListener(v->action.run());menu.addView(cell);cells.add(cell);return cell;
    }
    void openPopup() {
        if(popup!=null) {closePopup();return;}
        popup=Ui.column(Ui.context(service));popup.setBackground(Ui.shape(Ui.bg(service),28,service));popup.setClipToOutline(true);
        populatePopup();layoutPopup();popup.requestFocus();
    }
    void appearanceChanged(){
        if(popup==null)return;
        int y=settingsPanel==null?0:settingsPanel.scrollPosition();
        if(settingsPanel!=null)settingsPanel.dismissChoices();
        popup.setBackground(Ui.shape(Ui.bg(service),28,service));popup.removeAllViews();populatePopup();settingsPanel.restoreScrollPosition(y);
    }
    private void populatePopup(){
        LinearLayout top=new LinearLayout(service);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title=Ui.text(service,service.getString(R.string.app_name),20);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        KitIcon close=new KitIcon(service,R.raw.kit_close,Ui.ink(service));close.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_YES);close.setContentDescription(service.getString(R.string.close));close.setPadding(Ui.dp(service,12),Ui.dp(service,12),Ui.dp(service,12),Ui.dp(service,12));close.setOnClickListener(v->closePopup());top.addView(close,new LinearLayout.LayoutParams(Ui.dp(service,48),Ui.dp(service,48)));popup.addView(top);
        settingsPanel=new SettingsPanel(service,new SettingsPanel.Actions() {
            public void changed(){service.refreshSettings();}
            public void importGrid(){closePopup();Intent i=new Intent(service,ImportActivity.class).putExtra("returnCamera",true)
                    .putExtra(ImportActivity.RETURN_CAMERA_PACKAGE,service.cameraPackage()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);service.startActivity(i);}
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
            public void appearanceChanged(){service.refreshSettings();}
            public void permissions(boolean overlay){closePopup();service.startActivity(new Intent(service,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
            public void openPermissionGuide(){closePopup();service.startActivity(new Intent(service,MainActivity.class).putExtra(MainActivity.PERMISSION_GUIDE,true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));}
            public void openLanguageSettings(){closePopup();SystemSettings.language(service);}
            public void disableAccessibility(){closePopup();SystemSettings.accessibility(service);}
        },true);popup.addView(settingsPanel,new LinearLayout.LayoutParams(-1,0,1));
        popup.setFocusableInTouchMode(true);
        popup.setOnKeyListener((view,code,event)->{if(code==KeyEvent.KEYCODE_BACK){if(event.getAction()==KeyEvent.ACTION_UP)closePopup();return true;}return false;});
    }
    private void layoutPopup(){
        int x=safe.left+rightInset,y=safe.top+Ui.dp(service,48);
        int w=Math.min(Ui.dp(service,420),safe.width()-Ui.dp(service,16));
        x=Math.min(x,safe.right-w-Ui.dp(service,8));
        int h=safe.bottom-y-Ui.dp(service,12);
        if(h<Ui.dp(service,140)){y=safe.top+Ui.dp(service,4);h=safe.height()-Ui.dp(service,8);}
        updateWindow(popup,CameraCoordinates.rotate(new CameraMenuPlacement.Box(x,y,x+w,y+h),box(window),displayTurn()),true,"Chrynalogia-CameraSettings");
    }
    void closePopup() {uiHandler.removeCallbacksAndMessages(null);if(settingsPanel!=null)settingsPanel.dismissChoices();settingsPanel=null;remove(popup);popup=null;}
    void hide() {orientation.disable();uiHandler.removeCallbacksAndMessages(null);closePopup();remove(toolbar);remove(menu);toolbar=null;menu=null;logo=null;rotatingContent.clear();cells.clear();placement=null;naturalPlacement=null;layoutQuarter=-1;expanded=false;}
    private void remove(View v) {if(v!=null&&windows.remove(v)!=null)try{manager.removeViewImmediate(v);}catch(IllegalArgumentException ignored){}}

}

