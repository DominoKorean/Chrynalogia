package dev.chrynalogia.probe;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.widget.SeekBar;

/** Native range input, drawn using Sliders / Regular proportions from the kit. */
final class KitSlider extends SeekBar {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    KitSlider(Context c) {
        super(c);setProgressDrawable(new ColorDrawable(Color.TRANSPARENT));
        setThumb(new ColorDrawable(Color.TRANSPARENT));setSplitTrack(false);
        setPadding(Ui.dp(c,12),0,Ui.dp(c,12),0);setBackground(null);
    }
    @Override protected synchronized void onDraw(Canvas canvas) {
        float left=getPaddingLeft(),right=getWidth()-getPaddingRight(),y=getHeight()/2f;
        float ratio=(getProgress()-getMin())/(float)Math.max(1,getMax()-getMin());
        if(getLayoutDirection()==LAYOUT_DIRECTION_RTL)ratio=1-ratio;
        float x=left+(right-left)*ratio,half=Ui.dp(getContext(),9),radius=Ui.dp(getContext(),12);
        paint.setStyle(Paint.Style.FILL);paint.setColor(0xff848487);
        canvas.drawRoundRect(left,y-half,right,y+half,half,half,paint);
        paint.setColor(Ui.ACCENT);
        canvas.drawRoundRect(getLayoutDirection()==LAYOUT_DIRECTION_RTL?x:left,y-half,getLayoutDirection()==LAYOUT_DIRECTION_RTL?right:x,y+half,half,half,paint);
        paint.setColor(Color.BLACK);canvas.drawCircle(x,y,radius,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setColor(Ui.ACCENT);paint.setStrokeWidth(Ui.dp(getContext(),2));canvas.drawCircle(x,y,radius-Ui.dp(getContext(),1),paint);
    }
}
