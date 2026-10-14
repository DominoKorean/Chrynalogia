package dev.chrynalogia.probe;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

/** Captures only app content behind the navigation; foreground labels stay sharp. */
@android.annotation.SuppressLint("ViewConstructor") // Requires the sibling content to capture.
final class BackdropBar extends FrameLayout {
    private final View source;
    private final ImageView backdrop;
    private Bitmap bitmap;
    private boolean queued;
    private final android.view.ViewTreeObserver.OnPreDrawListener contentChanges;
    BackdropBar(Context c,View source) {
        super(c);this.source=source;contentChanges=()->{if(this.source.isDirty())updateBackdrop();return true;};setClipToOutline(true);setBackground(Ui.shape(Color.TRANSPARENT,79,c));
        setOutlineProvider(new android.view.ViewOutlineProvider(){@Override public void getOutline(View view,Outline outline){outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),Ui.dp(c,79));outline.setAlpha(1f);}});
        backdrop=new ImageView(c);backdrop.setScaleType(ImageView.ScaleType.FIT_XY);
        backdrop.setRenderEffect(RenderEffect.createBlurEffect(Ui.dp(c,14),Ui.dp(c,14),Shader.TileMode.CLAMP));
        addView(backdrop,new LayoutParams(-1,-1));
    }
    void updateBackdrop() {
        if(queued)return;queued=true;post(()->{queued=false;capture();});
    }
    @Override protected void onSizeChanged(int w,int h,int ow,int oh){super.onSizeChanged(w,h,ow,oh);updateBackdrop();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();source.getViewTreeObserver().addOnPreDrawListener(contentChanges);updateBackdrop();}
    @Override protected void onDetachedFromWindow(){if(source.getViewTreeObserver().isAlive())source.getViewTreeObserver().removeOnPreDrawListener(contentChanges);super.onDetachedFromWindow();}
    private void capture() {
        if(getWidth()==0||getHeight()==0||!isAttachedToWindow())return;
        int bw=Math.max(1,getWidth()/2),bh=Math.max(1,getHeight()/2);
        if(bitmap==null||bitmap.getWidth()!=bw||bitmap.getHeight()!=bh)bitmap=Bitmap.createBitmap(bw,bh,Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(Ui.bg(getContext()));Canvas canvas=new Canvas(bitmap);
        int[] origin=new int[2],target=new int[2];source.getLocationOnScreen(origin);getLocationOnScreen(target);
        canvas.scale(bw/(float)getWidth(),bh/(float)getHeight());canvas.translate(origin[0]-target[0],origin[1]-target[1]);source.draw(canvas);
        backdrop.setImageBitmap(bitmap);backdrop.invalidate();
    }
}
