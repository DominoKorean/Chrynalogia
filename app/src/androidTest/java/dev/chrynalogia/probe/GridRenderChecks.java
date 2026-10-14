package dev.chrynalogia.probe;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/** Checks rendered Android paths as well as the independent geometry fixtures. */
final class GridRenderChecks {
    static int run(Context c,Instrumentation runner) {
        AtomicReference<Throwable> failure=new AtomicReference<>();
        runner.runOnMainSync(()->{
          try {
            GridSettings s=new GridSettings(c);s.hidden=false;s.rotation=0;s.flipX=false;s.flipY=false;s.selected=Set.of("spiral");
            s.color=Color.RED;s.opacity=1;s.outline=false;s.thickness=1.2f;s.save();
            GridView camera=new GridView(c);Bitmap portrait=draw(camera,400,600);
            camera.setDisplayQuarterTurn(1);Bitmap landscape=draw(camera,600,400);
            // Compare the physical rectangle's extrema in each UI coordinate frame.
            Rect a=bounds(portrait),b=bounds(landscape);
            require(Math.abs(a.width()-b.height())<=2&&Math.abs(a.height()-b.width())<=2,"Display rotation preserves physical spiral dimensions");
            require(Math.abs(a.left-(400-b.bottom))<=2&&Math.abs(a.top-b.left)<=2,"Spiral rotates inversely in display coordinates");
            s.rotation=1;s.save();camera.applySettings();Rect manual=bounds(draw(camera,600,400));
            require(manual.width()>manual.height(),"Explicit user rotation still changes spiral orientation");
            s.rotation=0;s.selected=Set.of("cross");s.save();
            AppAppearance.setTheme(c,"light");Bitmap light=draw(new GridView(Ui.context(c),"cross"),400,272);
            require(ink(light,Color.BLACK)&&!ink(light,Color.RED),"Light preview uses black independent of camera color");
            AppAppearance.setTheme(c,"dark");GridView preview=new GridView(Ui.context(c),"cross");Bitmap dark=draw(preview,400,272);
            require(ink(dark,Color.WHITE)&&!ink(dark,Color.RED),"Dark preview uses white independent of camera color");
            preview.setDisplayQuarterTurn(1);require(dark.sameAs(draw(preview,400,272)),"Thumbnail does not inherit camera display compensation");
            require(portrait.getPixel(200,300)==Color.TRANSPARENT,"Spiral partition outline keeps the empty square interior transparent");
            camera.setDisplayQuarterTurn(0);camera.applySettings();require(ink(draw(camera,400,600),Color.RED),"Camera retains custom line color");
          } catch(Throwable e){failure.set(e);}
        });
        if(failure.get()!=null)throw new AssertionError("Grid render regression",failure.get());
        return 8;
    }
    private static Bitmap draw(GridView view,int w,int h){view.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));view.layout(0,0,w,h);Bitmap b=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);view.draw(new Canvas(b));return b;}
    private static boolean ink(Bitmap b,int color){for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++){int p=b.getPixel(x,y);if(Color.alpha(p)>180&&(p&0xffffff)==(color&0xffffff))return true;}return false;}
    private static Rect bounds(Bitmap b){Rect r=new Rect(b.getWidth(),b.getHeight(),0,0);for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++)if(Color.alpha(b.getPixel(x,y))>100){r.left=Math.min(r.left,x);r.top=Math.min(r.top,y);r.right=Math.max(r.right,x+1);r.bottom=Math.max(r.bottom,y+1);}return r;}
    private static void require(boolean value,String text){if(!value)throw new AssertionError(text);}
}
