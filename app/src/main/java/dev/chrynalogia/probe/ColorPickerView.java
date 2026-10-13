package dev.chrynalogia.probe;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

/** Live HSV controls for the custom color row in the Figma line panel. */
@android.annotation.SuppressLint("ViewConstructor")
final class ColorPickerView extends View {
    private final boolean hueOnly;
    private final float[] hsv=new float[3];
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds=new RectF();
    private Shader hueShader,saturationShader;
    private final java.util.function.IntConsumer changed;
    ColorPickerView(Context c,boolean hueOnly,int color,java.util.function.IntConsumer changed) {
        super(c);this.hueOnly=hueOnly;this.changed=changed;setColor(color);setClickable(true);setFocusable(true);
        setContentDescription(c.getString(hueOnly?R.string.custom_color:R.string.line_hex));
    }
    void setColor(int color) {Color.colorToHSV(color,hsv);updateShaders();invalidate();}
    private RectF area() {return bounds;}
    @Override protected void onSizeChanged(int w,int h,int oldw,int oldh) {
        float p=Ui.dp(getContext(),10);bounds.set(p,p,w-p,h-p);updateShaders();
    }
    private void updateShaders() {
        RectF r=bounds;if(r.width()<=0||r.height()<=0)return;
        if(hueOnly)hueShader=new LinearGradient(r.left,0,r.right,0,new int[]{Color.RED,Color.YELLOW,Color.GREEN,Color.CYAN,Color.BLUE,Color.MAGENTA,Color.RED},null,Shader.TileMode.CLAMP);
        else {
            int hue=Color.HSVToColor(new float[]{hsv[0],1,1});
            saturationShader=new ComposeShader(new LinearGradient(r.left,0,r.right,0,Color.WHITE,hue,Shader.TileMode.CLAMP),new LinearGradient(0,r.top,0,r.bottom,Color.WHITE,Color.BLACK,Shader.TileMode.CLAMP),PorterDuff.Mode.MULTIPLY);
        }
    }
    @Override protected void onDraw(Canvas c) {
        RectF r=area();if(r.width()<=0||r.height()<=0)return;
        paint.setShader(hueOnly?hueShader:saturationShader);
        c.drawRoundRect(r,Ui.dp(getContext(),12),Ui.dp(getContext(),12),paint);paint.setShader(null);
        float x=r.left+r.width()*(hueOnly?hsv[0]/360f:hsv[1]);float y=hueOnly?r.centerY():r.bottom-r.height()*hsv[2];
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(getContext(),2));paint.setColor(Color.BLACK);c.drawCircle(x,y,Ui.dp(getContext(),8),paint);
        paint.setColor(Color.WHITE);c.drawCircle(x,y,Ui.dp(getContext(),7),paint);paint.setStyle(Paint.Style.FILL);
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if(e.getAction()==MotionEvent.ACTION_DOWN || e.getAction()==MotionEvent.ACTION_MOVE) {
            getParent().requestDisallowInterceptTouchEvent(true);RectF r=area();
            float x=Math.max(0,Math.min(1,(e.getX()-r.left)/Math.max(1,r.width())));
            if(hueOnly){hsv[0]=Math.min(359.9f,x*360);hsv[1]=1;hsv[2]=1;}else {hsv[1]=x;hsv[2]=1-Math.max(0,Math.min(1,(e.getY()-r.top)/Math.max(1,r.height())));}
            changed.accept(Color.HSVToColor(hsv));updateShaders();invalidate();return true;
        }
        if(e.getAction()==MotionEvent.ACTION_UP || e.getAction()==MotionEvent.ACTION_CANCEL) {getParent().requestDisallowInterceptTouchEvent(false);if(e.getAction()==MotionEvent.ACTION_UP)performClick();return true;}
        return super.onTouchEvent(e);
    }
    @Override public boolean performClick(){super.performClick();return true;}
}
