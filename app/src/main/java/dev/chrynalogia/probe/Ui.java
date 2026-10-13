package dev.chrynalogia.probe;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** One UI kit tokens shared by the app and the camera settings window. */
final class Ui {
    static final int ACCENT=0xff387aff;
    static int bg(Context c) {return AppAppearance.dark(c)?0xff010102:0xfff1f1f3;}
    static int card(Context c) {return AppAppearance.dark(c)?0xff17171a:0xfffcfcff;}
    static int surface(Context c) {return AppAppearance.dark(c)?0xff2e2e30:0xffeaeaea;}
    static int ink(Context c) {return AppAppearance.dark(c)?Color.WHITE:Color.BLACK;}
    static int secondary(Context c) {return AppAppearance.dark(c)?0xffaaaab0:0xff64646a;}
    static int divider(Context c) {return AppAppearance.dark(c)?0xff2e2e30:0xffeaeaea;}
    static Context context(Context c) {return new ContextThemeWrapper(c,AppAppearance.dark(c)?R.style.AppDark:R.style.AppLight);}
    static ColorStateList checks(Context c) {return new ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{ACCENT,0xff848487});}
    static int dp(Context c,float value) {return Math.round(c.getResources().getDisplayMetrics().density*value);}
    static GradientDrawable shape(int color,int radius,Context c) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(c,radius));return d;
    }
    static void clickable(View v,int color,int radius) {
        Context c=v.getContext();v.setBackground(new RippleDrawable(ColorStateList.valueOf(AppAppearance.dark(c)?0x22ffffff:0x18000000),shape(color,radius,c),shape(Color.WHITE,radius,c)));
    }
    static LinearLayout column(Context c) {LinearLayout v=new LinearLayout(c);v.setOrientation(LinearLayout.VERTICAL);return v;}
    static TextView text(Context c,String label,int size) {
        TextView v=new TextView(c);v.setText(label);v.setTextSize(size);v.setTextColor(ink(c));v.setIncludeFontPadding(false);
        v.setPadding(dp(c,16),dp(c,8),dp(c,16),dp(c,8));return v;
    }
    static void title(LinearLayout parent,String title) {TextView t=text(parent.getContext(),title,24);t.setTypeface(null,Typeface.BOLD);parent.addView(t);}
    static Button button(Context c,String label,Runnable action) {
        Button b=new Button(c);b.setText(label);b.setTextSize(18);b.setTextColor(ink(c));b.setAllCaps(false);
        b.setTypeface(null,Typeface.BOLD);clickable(b,card(c),28);b.setMinHeight(dp(c,48));b.setMinimumHeight(dp(c,48));
        b.setPadding(dp(c,20),dp(c,12),dp(c,20),dp(c,12));b.setOnClickListener(v->action.run());return b;
    }
    static Button addButton(LinearLayout parent,String label,Runnable action) {
        Button b=button(parent.getContext(),label,action);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(parent.getContext(),12),dp(parent.getContext(),4),dp(parent.getContext(),12),dp(parent.getContext(),4));parent.addView(b,p);return b;
    }
    static void note(LinearLayout parent,String text) {TextView v=text(parent.getContext(),text,14);v.setTextColor(secondary(parent.getContext()));parent.addView(v);}
    static CompoundButton toggle(LinearLayout parent,String label,boolean value,java.util.function.Consumer<Boolean> change) {
        Context c=parent.getContext();KitSwitch s=new KitSwitch(c);s.setText(label);s.setChecked(value);
        s.setOnCheckedChangeListener((v,on)->change.accept(on));parent.addView(s,new LinearLayout.LayoutParams(-1,-2));return s;
    }
    static LinearLayout section(LinearLayout parent) {
        Context c=parent.getContext();LinearLayout box=column(c);box.setPadding(dp(c,20),dp(c,10),dp(c,20),dp(c,10));box.setBackground(shape(card(c),28,c));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.setMargins(dp(c,16),dp(c,12),dp(c,16),dp(c,12));parent.addView(box,p);return box;
    }
    static void separator(LinearLayout parent) {
        Context c=parent.getContext();View v=new View(c);v.setBackgroundColor(divider(c));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(c,1));p.setMargins(dp(c,12),dp(c,4),dp(c,12),dp(c,4));parent.addView(v,p);
    }
    static void slider(LinearLayout parent,String label,int min,int max,int value,java.util.function.IntConsumer change) {
        slider(parent,label,min,max,value,n->Integer.toString(n),change);
    }
    static void slider(LinearLayout parent,String label,int min,int max,int value,java.util.function.IntFunction<String> format,java.util.function.IntConsumer change) {
        Context c=parent.getContext();TextView title=text(c,label,18);parent.addView(title);
        TextView t=text(c,format.apply(value),14);t.setTextColor(ACCENT);t.setPadding(dp(c,16),0,dp(c,16),0);parent.addView(t);
        SeekBar bar=new KitSlider(c);bar.setMin(min);bar.setMax(max);bar.setProgress(value);bar.setContentDescription(label);
        parent.addView(bar,new LinearLayout.LayoutParams(-1,dp(c,48)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onStartTrackingTouch(SeekBar b) {} public void onStopTrackingTouch(SeekBar b) {}
            public void onProgressChanged(SeekBar b,int n,boolean user) {t.setText(format.apply(n));if(user)change.accept(n);}
        });
    }
    static TextView centered(Context c,String text,int size) {TextView t=text(c,text,size);t.setGravity(Gravity.CENTER);return t;}
}
