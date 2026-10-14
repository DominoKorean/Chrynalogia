package dev.chrynalogia.probe;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.RectF;
import android.widget.CompoundButton;
import com.caverock.androidsvg.SVG;

/** Keeps native checked, keyboard and accessibility behavior with the kit's 40 × 22 SVG. */
final class KitSwitch extends CompoundButton {
    private final Picture on,off;
    private final RectF switchBounds=new RectF();
    KitSwitch(Context c) {
        super(c);setButtonDrawable(null);setTextSize(18);setTextColor(Ui.ink(c));
        setPadding(0,Ui.dp(c,12),Ui.dp(c,60),Ui.dp(c,12));setMinHeight(Ui.dp(c,52));
        setGravity(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL);setBackground(null);
        setClickable(true);setFocusable(true);
        try {on=SVG.getFromResource(c,R.raw.kit_switch_on).renderToPicture();off=SVG.getFromResource(c,R.raw.kit_switch_off).renderToPicture();}
        catch(Exception e){throw new IllegalArgumentException("Invalid switch SVG",e);}
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);float w=Ui.dp(getContext(),40),h=Ui.dp(getContext(),22);
        float x=getLayoutDirection()==LAYOUT_DIRECTION_RTL?0:getWidth()-w;
        switchBounds.set(x,(getHeight()-h)/2,x+w,(getHeight()+h)/2);c.drawPicture(isChecked()?on:off,switchBounds);
    }
    @Override public void setChecked(boolean checked) {
        boolean changed=isChecked()!=checked;
        super.setChecked(checked);
        // There is no native button drawable to invalidate our custom SVG.
        if(changed)invalidate();
    }
    @Override public CharSequence getAccessibilityClassName(){return "android.widget.Switch";}
}
