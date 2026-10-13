package dev.chrynalogia.probe;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Chrynalogia's Figma layout, adapted to native responsive controls and One UI tokens. */
@android.annotation.SuppressLint("ViewConstructor")
final class SettingsPanel extends FrameLayout {
    interface Actions {
        void changed();void importGrid();void modeChanged(String mode);void stop();
        default void openCamera() {}
        default void appearanceChanged() {changed();}
        default void startSession() {}
        default void permissions(boolean overlay) {}
    }
    private final GridSettings settings;
    private final Actions actions;
    private final boolean compact;
    private final ScrollView scroll;
    private final LinearLayout content;
    private final ArrayList<GridView> previews=new ArrayList<>();
    private final ArrayList<View> tiles=new ArrayList<>();
    private final String pageKey;
    private int page;
    private View expandedHeader;
    private AlertDialog choiceDialog;
    View dialogView() {return choiceDialog!=null&&choiceDialog.isShowing()?choiceDialog.getWindow().getDecorView():null;}
    void dismissChoices() {if(choiceDialog!=null)choiceDialog.dismiss();choiceDialog=null;}

    SettingsPanel(Context c,Actions actions) {this(c,actions,false);}
    SettingsPanel(Context c,Actions actions,boolean compact) {
        super(Ui.context(c));this.actions=actions;this.compact=compact;c=getContext();settings=new GridSettings(c);
        pageKey=compact?"cameraTab":"appTab";page=Math.max(0,Math.min(2,settings.prefs.getInt(pageKey,0)));
        setBackgroundColor(Ui.bg(c));scroll=new ScrollView(c);scroll.setFillViewport(true);scroll.setClipToPadding(false);
        content=Ui.column(c);content.setPadding(0,0,0,Ui.dp(c,108));scroll.addView(content);addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        build();
        if(compact||page!=0)collapseHeader();
    }
    private String s(int id) {return getContext().getString(id);}
    private void changed() {SettingsStore.saveChanges(settings);actions.changed();for(GridView v:previews)v.applySettings();}
    private void build() {
        settings.reload();content.removeAllViews();previews.clear();tiles.clear();
        setBackgroundColor(Ui.bg(getContext()));
        if(page==0)grids();else if(page==1)lines();else preferences();
        navigation();
    }
    private void heading(String title,boolean logo) {
        Context c=getContext();LinearLayout header=Ui.column(c);header.setGravity(Gravity.CENTER);
        header.setPadding(Ui.dp(c,24),0,Ui.dp(c,24),0);
        TextView t=Ui.centered(c,title,compact?24:36);t.setTypeface(null,Typeface.BOLD);header.addView(t);
        expandedHeader=header;
        content.addView(header,new LinearLayout.LayoutParams(-1,headerHeight()));
        content.setMinimumHeight(getHeight()+headerHeight());
    }
    private int headerHeight() {return compact?Ui.dp(getContext(),56):Math.round((getHeight()>0?getHeight():getResources().getDisplayMetrics().heightPixels)*0.3976f);}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        super.onSizeChanged(w,h,oldw,oldh);
        if(expandedHeader!=null){android.view.ViewGroup.LayoutParams p=expandedHeader.getLayoutParams();p.height=headerHeight();expandedHeader.setLayoutParams(p);}
        content.setMinimumHeight(h+headerHeight());
    }
    private void collapseHeader() {scroll.post(()->scroll.scrollTo(0,expandedHeader==null?0:expandedHeader.getHeight()));}
    private void grids() {
        Context c=getContext();heading("Chrynalogia",!compact);
        if(!compact) {
            LinearLayout camera=new LinearLayout(c);camera.setGravity(Gravity.END);camera.setPadding(Ui.dp(c,24),0,Ui.dp(c,24),Ui.dp(c,12));
            Button b=Ui.button(c,s(R.string.open_camera),actions::openCamera);b.setTextSize(14);b.setTypeface(null,Typeface.NORMAL);b.setCompoundDrawablesRelative(KitIcon.drawable(c,R.raw.kit_camera,Ui.ink(c)),null,null,null);b.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(Ui.ink(c)));b.setCompoundDrawablePadding(Ui.dp(c,8));camera.addView(b);content.addView(camera);
        }
        LinearLayout selection=Ui.section(content);
        Ui.toggle(selection,s(R.string.multi_select),settings.multi,on->{
            settings.multi=on;if(!on&&settings.selected.size()>1){String first=settings.selected.iterator().next();settings.selected.clear();settings.selected.add(first);}changed();rebuildAtSameScroll();
        });
        TextView selectionInfo=Ui.text(c,s(R.string.multi_select_note),14);selectionInfo.setTextColor(Ui.secondary(c));selectionInfo.setPadding(0,Ui.dp(c,2),Ui.dp(c,52),Ui.dp(c,10));selection.addView(selectionInfo);
        ArrayList<String> ids=new ArrayList<>();for(String id:GridGeometry.IDS)if(!id.equals("golden_ratio"))ids.add(id);
        for(int i=0;i<ids.size();i+=2) {
            LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.TOP);row.setPadding(Ui.dp(c,16),Ui.dp(c,8),Ui.dp(c,16),Ui.dp(c,8));
            for(int column=0;column<2;column++) {
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1);if(column==0)p.setMarginEnd(Ui.dp(c,12));
                if(i+column<ids.size()) {String id=ids.get(i+column);row.addView(gridTile(id,gridName(id),gridDescription(id)),p);}else row.addView(new View(c),p);
            }
            content.addView(row);
        }
        LinearLayout files=Ui.section(content);Ui.title(files,s(R.string.files_title));Ui.note(files,s(R.string.files_note));
        for(int i=0;i<settings.imports.length();i++) {
            JSONObject data=settings.imports.optJSONObject(i);if(data==null)continue;String id=data.optString("id");
            files.addView(gridTile(id,data.optString("name"),s(data.optString("extension").equals(".svg")?R.string.svg_grid:R.string.image_grid)));
            Button remove=Ui.button(c,s(R.string.delete_grid),()->removeImport(id));remove.setCompoundDrawablesRelative(KitIcon.drawable(c,R.raw.kit_delete,Ui.ink(c)),null,null,null);remove.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(Ui.ink(c)));files.addView(remove);
        }
        Button add=Ui.addButton(files,s(R.string.import_grid),actions::importGrid);add.setCompoundDrawablesRelative(KitIcon.drawable(c,R.raw.kit_add,Ui.ink(c)),null,null,null);add.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(Ui.ink(c)));
        Ui.toggle(files,s(R.string.keep_aspect),settings.keepAspect,on->{settings.keepAspect=on;changed();});Ui.note(files,s(R.string.keep_aspect_note));
    }
    private String gridName(String id) {return s(switch(id) {
        case "thirds"->R.string.grid_thirds;case "cross"->R.string.grid_cross;case "golden_grid"->R.string.grid_golden_grid;
        case "spiral"->R.string.grid_spiral;case "x"->R.string.grid_x;case "diagonal"->R.string.grid_diagonal;
        case "triangle"->R.string.grid_triangle;case "rabatment"->R.string.grid_rabatment;case "dynamic"->R.string.grid_dynamic;default->R.string.grid_custom;
    });}
    private String gridDescription(String id) {return s(switch(id) {
        case "thirds"->R.string.desc_thirds;case "cross"->R.string.desc_cross;case "golden_grid"->R.string.desc_golden_grid;
        case "spiral"->R.string.desc_spiral;case "x"->R.string.desc_x;case "diagonal"->R.string.desc_diagonal;
        case "triangle"->R.string.desc_triangle;case "rabatment"->R.string.desc_rabatment;case "dynamic"->R.string.desc_dynamic;default->R.string.desc_custom;
    });}
    private LinearLayout gridTile(String id,String name,String description) {
        Context c=getContext();LinearLayout tile=Ui.column(c);tile.setTag(id);tiles.add(tile);tile.setPadding(Ui.dp(c,6),Ui.dp(c,6),Ui.dp(c,6),Ui.dp(c,14));
        GradientDrawable bg=Ui.shape(settings.selected.contains(id)?Ui.card(c):Color.TRANSPARENT,20,c);if(settings.selected.contains(id))bg.setStroke(Ui.dp(c,1),Color.WHITE);tile.setBackground(bg);tile.setSelected(settings.selected.contains(id));
        GridView preview=new GridView(c,id);previews.add(preview);preview.setBackground(Ui.shape(Ui.surface(c),14,c));preview.setClipToOutline(true);
        tile.addView(preview,new LinearLayout.LayoutParams(-1,Ui.dp(c,compact?100:118)));
        TextView title=Ui.text(c,name,16);title.setPadding(Ui.dp(c,2),Ui.dp(c,10),Ui.dp(c,2),Ui.dp(c,3));title.setMaxLines(2);tile.addView(title);
        TextView note=Ui.text(c,description,14);note.setTextColor(Ui.secondary(c));note.setPadding(Ui.dp(c,2),0,Ui.dp(c,2),Ui.dp(c,8));tile.addView(note);
        tile.setContentDescription(name);tile.setClickable(true);tile.setFocusable(true);tile.setOnClickListener(v->{settings.select(id,!settings.selected.contains(id));changed();rebuildAtSameScroll();});
        if(id.equals("custom")) {countInput(tile,s(R.string.columns),true);countInput(tile,s(R.string.rows),false);}
        return tile;
    }
    private void countInput(LinearLayout tile,String label,boolean columns) {
        Context c=getContext();LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,Ui.dp(c,4),0,Ui.dp(c,4));
        TextView text=Ui.text(c,label,14);text.setPadding(0,0,0,0);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));
        EditText number=new EditText(c);number.setTextColor(Ui.ink(c));number.setTextSize(14);number.setSingleLine(true);number.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        number.setSelectAllOnFocus(true);number.setGravity(Gravity.CENTER);number.setPadding(0,0,0,0);number.setBackground(Ui.shape(Ui.surface(c),24,c));number.setContentDescription(label);
        number.setText(String.format(Locale.getDefault(),"%d",columns?settings.columns:settings.rows));row.addView(number,new LinearLayout.LayoutParams(Ui.dp(c,56),Ui.dp(c,48)));tile.addView(row);
        row.setVisibility(settings.selected.contains("custom")?View.VISIBLE:View.INVISIBLE);
        number.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence v,int a,int b,int d){}public void onTextChanged(CharSequence v,int a,int b,int d){}
            public void afterTextChanged(Editable value){try{int n=Integer.parseInt(value.toString());if(n<1||n>16){number.setError("1–16");return;}if(columns)settings.columns=n;else settings.rows=n;changed();}catch(NumberFormatException ignored){}}
        });
    }
    private void lines() {
        Context c=getContext();heading(s(R.string.tab_line),false);LinearLayout colors=Ui.section(content);
        int[] names={R.string.yellow,R.string.white,R.string.black};int[] values={0xffffcb0c,Color.WHITE,Color.BLACK};
        for(int i=0;i<3;i++) {
            int color=values[i];RadioButton choice=new RadioButton(c);choice.setText(s(names[i]));choice.setTextSize(18);choice.setTextColor(Ui.ink(c));choice.setPadding(Ui.dp(c,12),Ui.dp(c,12),Ui.dp(c,12),Ui.dp(c,12));
            choice.setButtonTintList(Ui.checks(c));choice.setChecked(!settings.customColorSelected&&settings.color==color);choice.setMinHeight(Ui.dp(c,52));
            GradientDrawable swatch=Ui.shape(color,20,c);swatch.setStroke(Ui.dp(c,1),Ui.secondary(c));swatch.setBounds(0,0,Ui.dp(c,20),Ui.dp(c,20));choice.setCompoundDrawablesRelative(null,null,swatch,null);
            choice.setOnClickListener(v->{settings.color=color;settings.customColorSelected=false;settings.outline=false;changed();rebuildAtSameScroll();});colors.addView(choice);Ui.separator(colors);
        }
        LinearLayout custom=new LinearLayout(c);custom.setGravity(Gravity.CENTER_VERTICAL);custom.setPadding(Ui.dp(c,12),Ui.dp(c,20),Ui.dp(c,12),Ui.dp(c,20));
        RadioButton customChoice=radio(c,s(R.string.custom_color));customChoice.setChecked(settings.customColorSelected);custom.addView(customChoice,new LinearLayout.LayoutParams(0,-2,1));
        EditText hex=new EditText(c);hex.setSingleLine(true);hex.setTextColor(Ui.ink(c));hex.setTextSize(14);hex.setPadding(Ui.dp(c,12),Ui.dp(c,8),Ui.dp(c,12),Ui.dp(c,8));hex.setBackground(Ui.shape(Ui.surface(c),24,c));
        hex.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(6),(source,start,end,dest,dstart,dend)->source.subSequence(start,end).toString().matches("[0-9a-fA-F]*")?null:""});
        hex.setText(String.format(Locale.ROOT,"%06X",settings.customColor&0xffffff));hex.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        hex.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_DONE|android.view.inputmethod.EditorInfo.IME_FLAG_FORCE_ASCII);hex.setTypeface(Typeface.MONOSPACE);hex.setContentDescription(s(R.string.line_hex));hex.setSelectAllOnFocus(true);
        float sixChars=hex.getPaint().measureText("FFFFFF")+hex.getPaddingLeft()+hex.getPaddingRight();
        custom.addView(hex,new LinearLayout.LayoutParams((int)Math.ceil(sixChars),Ui.dp(c,48)));
        View customSwatch=new View(c);GradientDrawable customSwatchBackground=Ui.shape(settings.customColor,20,c);customSwatchBackground.setStroke(Ui.dp(c,1),Ui.secondary(c));customSwatch.setBackground(customSwatchBackground);
        LinearLayout.LayoutParams swatchParams=new LinearLayout.LayoutParams(Ui.dp(c,20),Ui.dp(c,20));swatchParams.setMarginStart(Ui.dp(c,12));custom.addView(customSwatch,swatchParams);colors.addView(custom);
        ColorPickerView[] pickers=new ColorPickerView[2];
        java.util.function.IntConsumer setColor=color->{settings.color=color;settings.customColor=color;settings.customColorSelected=true;changed();customChoice.setChecked(true);for(int i=0;i<colors.getChildCount();i++)if(colors.getChildAt(i) instanceof RadioButton b)b.setChecked(false);customSwatchBackground.setColor(color);hex.setText(String.format(Locale.ROOT,"%06X",color&0xffffff));for(ColorPickerView p:pickers)if(p!=null)p.setColor(color);};
        customChoice.setOnClickListener(v->{setColor.accept(settings.customColor);});
        pickers[0]=new ColorPickerView(c,true,settings.customColor,setColor);pickers[1]=new ColorPickerView(c,false,settings.customColor,setColor);
        colors.addView(pickers[0],new LinearLayout.LayoutParams(-1,Ui.dp(c,48)));colors.addView(pickers[1],new LinearLayout.LayoutParams(-1,Ui.dp(c,136)));
        Runnable apply=()->{try{String value=hex.getText().toString().trim();if(!value.startsWith("#"))value="#"+value;if(!value.matches("#[0-9a-fA-F]{6}"))throw new IllegalArgumentException();setColor.accept(Color.parseColor(value));}catch(IllegalArgumentException e){hex.setError(s(R.string.invalid_hex));}};
        Ui.addButton(colors,s(R.string.apply_color),apply);
        hex.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_DONE){apply.run();c.getSystemService(android.view.inputmethod.InputMethodManager.class).hideSoftInputFromWindow(hex.getWindowToken(),0);return true;}return false;});
        LinearLayout styling=Ui.section(content);
        Ui.slider(styling,s(R.string.opacity),0,100,Math.round(settings.opacity*100),n->c.getString(R.string.opacity_value,n),n->{settings.opacity=n/100f;changed();});Ui.separator(styling);
        Ui.slider(styling,s(R.string.thickness),3,60,Math.round(settings.thickness*10),n->c.getString(R.string.thickness_value,n/10f),n->{settings.thickness=n/10f;changed();});Ui.separator(styling);
        Ui.toggle(styling,s(R.string.outline),settings.outline,on->{settings.outline=on;changed();});Ui.note(styling,s(R.string.outline_note));
    }
    private void preferences() {
        Context c=getContext();heading(s(R.string.tab_settings),false);LinearLayout appearance=Ui.section(content);
        String tag=AppAppearance.language(c);String language=tag.startsWith("ko")?s(R.string.korean):tag.startsWith("en")?s(R.string.english):s(R.string.system_default);
        preference(appearance,R.raw.kit_language,s(R.string.language),language,()->chooseLanguage());
        Ui.separator(appearance);String theme=AppAppearance.theme(c);
        preference(appearance,R.raw.kit_theme,s(R.string.theme),s(theme.equals("dark")?R.string.dark:theme.equals("light")?R.string.light:R.string.system_default),()->chooseTheme());
        LinearLayout run=Ui.section(content);RadioGroup modes=new RadioGroup(c);
        RadioButton always=radio(c,s(R.string.always));RadioButton manual=radio(c,s(R.string.manual));modes.addView(always);modes.addView(manual);modes.check(settings.runMode.equals("manual")?manual.getId():always.getId());run.addView(modes);
        modes.setOnCheckedChangeListener((group,id)->{settings.runMode=id==manual.getId()?"manual":"always";settings.save();actions.modeChanged(settings.runMode);});Ui.note(run,s(R.string.run_note));
        if(settings.runMode.equals("manual")) {Ui.note(run,s(settings.active(c)?R.string.manual_active:R.string.manual_stopped));Ui.addButton(run,s(settings.active(c)?R.string.stop_session:R.string.start_session),settings.active(c)?actions::stop:actions::startSession);}
        Ui.toggle(run,s(R.string.allow_stop),settings.allowStop,on->{settings.allowStop=on;changed();});Ui.addButton(run,s(R.string.stop_background),actions::stop);
        LinearLayout permissions=Ui.section(content);
        Ui.note(permissions,s(!CameraConnection.connected()?R.string.access_needed:R.string.access_connected));Ui.note(permissions,s(Settings.canDrawOverlays(c)?R.string.overlay_allowed:R.string.overlay_needed));
        if(!Settings.canDrawOverlays(c))Ui.addButton(permissions,s(R.string.allow_overlay),()->actions.permissions(true));
        if(!CameraConnection.connected())Ui.addButton(permissions,s(R.string.allow_access),()->actions.permissions(false));
        Ui.note(permissions,s(R.string.welcome_privacy));
        for(LinearLayout group:new LinearLayout[]{appearance,run,permissions}) {
            LinearLayout.LayoutParams p=(LinearLayout.LayoutParams)group.getLayoutParams();p.setMargins(Ui.dp(c,10),Ui.dp(c,10),Ui.dp(c,10),Ui.dp(c,10));group.setLayoutParams(p);
            for(int i=0;i<group.getChildCount();i++)if(group.getChildAt(i) instanceof TextView t&&!(t instanceof Button))t.setPadding(0,Ui.dp(c,2),0,Ui.dp(c,10));
        }
    }
    private RadioButton radio(Context c,String text) {RadioButton b=new RadioButton(c);b.setId(View.generateViewId());b.setText(text);b.setTextColor(Ui.ink(c));b.setTextSize(18);b.setMinHeight(Ui.dp(c,52));b.setButtonTintList(Ui.checks(c));return b;}
    private void preference(LinearLayout parent,int icon,String title,String value,Runnable action) {
        Context c=getContext();LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,Ui.dp(c,10),0,Ui.dp(c,10));row.setContentDescription(title);Ui.clickable(row,Ui.card(c),20);
        row.addView(new KitIcon(c,icon,Ui.ink(c)),new LinearLayout.LayoutParams(Ui.dp(c,24),Ui.dp(c,24)));LinearLayout labels=Ui.column(c);labels.setPadding(Ui.dp(c,20),0,0,0);TextView primary=Ui.text(c,title,18);primary.setPadding(0,0,0,0);labels.addView(primary);TextView detail=Ui.text(c,value,14);detail.setPadding(0,Ui.dp(c,2),0,0);detail.setTextColor(Ui.ACCENT);labels.addView(detail);row.addView(labels,new LinearLayout.LayoutParams(0,-2,1));row.setMinimumHeight(Ui.dp(c,65));row.setOnClickListener(v->action.run());parent.addView(row);
    }
    private void chooseLanguage() {
        String[] tags={"","ko","en"};String[] labels={s(R.string.system_default),s(R.string.korean),s(R.string.english)};String current=AppAppearance.language(getContext());int selected=current.startsWith("ko")?1:current.startsWith("en")?2:0;
        showChoices(s(R.string.language),labels,selected,n->{AppAppearance.setLanguage(getContext(),tags[n]);actions.appearanceChanged();});
    }
    private void chooseTheme() {
        String[] modes={"system","light","dark"};String[] labels={s(R.string.system_default),s(R.string.light),s(R.string.dark)};String current=AppAppearance.theme(getContext());int selected=current.equals("light")?1:current.equals("dark")?2:0;
        showChoices(s(R.string.theme),labels,selected,n->{AppAppearance.setTheme(getContext(),modes[n]);actions.appearanceChanged();});
    }
    private void showChoices(String title,String[] labels,int selected,java.util.function.IntConsumer choose) {
        dismissChoices();AlertDialog dialog=new AlertDialog.Builder(getContext()).setTitle(title).setSingleChoiceItems(labels,selected,(d,n)->{d.dismiss();choose.accept(n);}).setNegativeButton(s(R.string.close),null).create();
        choiceDialog=dialog;dialog.setOnDismissListener(d->{if(choiceDialog==dialog)choiceDialog=null;});
        if(compact&&dialog.getWindow()!=null)dialog.getWindow().setType(android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
        dialog.show();
    }
    private void navigation() {
        if(getChildCount()>1)removeViewAt(1);Context c=getContext();BackdropBar shell=new BackdropBar(c,content);LinearLayout bar=new LinearLayout(c);bar.setGravity(Gravity.CENTER);bar.setPadding(Ui.dp(c,4),Ui.dp(c,4),Ui.dp(c,4),Ui.dp(c,4));bar.setBackground(Ui.shape(AppAppearance.dark(c)?0x992e2e30:0x99ffffff,79,c));shell.setElevation(Ui.dp(c,8));
        int[] labels={R.string.tab_grid,R.string.tab_line,R.string.tab_settings};int[] icons={R.raw.kit_grid,R.raw.kit_line,R.raw.kit_settings};
        for(int i=0;i<3;i++) {
            int selected=i;LinearLayout tab=Ui.column(c);tab.setGravity(Gravity.CENTER);tab.setContentDescription(s(labels[i]));tab.setSelected(page==i);Ui.clickable(tab,page==i?(AppAppearance.dark(c)?0xb33d3d3d:0xccf8f9f8):Color.TRANSPARENT,39);
            tab.addView(new KitIcon(c,icons[i],page==i?Ui.ink(c):Ui.secondary(c)),new LinearLayout.LayoutParams(Ui.dp(c,22),Ui.dp(c,22)));
            TextView text=Ui.centered(c,s(labels[i]),10);text.setPadding(0,Ui.dp(c,3),0,0);text.setTextColor(page==i?Ui.ink(c):Ui.secondary(c));if(page==i)text.setTypeface(null,Typeface.BOLD);tab.addView(text);tab.setOnClickListener(v->{if(page==selected){collapseHeader();return;}page=selected;settings.prefs.edit().putInt(pageKey,page).apply();build();collapseHeader();});bar.addView(tab,new LinearLayout.LayoutParams(Ui.dp(c,76),Ui.dp(c,46)));
        }
        shell.addView(bar,new FrameLayout.LayoutParams(-1,-1));FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(-2,-2,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL);p.bottomMargin=Ui.dp(c,16);addView(shell,p);
        scroll.setOnScrollChangeListener((v,x,y,ox,oy)->shell.updateBackdrop());shell.updateBackdrop();
    }
    private void rebuildAtSameScroll() {int y=scroll.getScrollY();build();scroll.post(()->scroll.scrollTo(0,y));}
    private void removeImport(String id) {
        try{GridFiles.remove(getContext(),id);settings.reload();actions.changed();rebuildAtSameScroll();}
        catch(GridImportException error){Toast.makeText(getContext(),error.messageRes,Toast.LENGTH_LONG).show();}
    }
}
