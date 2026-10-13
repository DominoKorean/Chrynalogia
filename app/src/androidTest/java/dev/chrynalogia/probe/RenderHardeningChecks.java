package dev.chrynalogia.probe;
import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

final class RenderHardeningChecks {
    static void waitAssets(Instrumentation runner) throws Exception {
        try {var count=Class.forName("dev.chrynalogia.probe.ImportAssets").getDeclaredMethod("pendingCount");count.setAccessible(true);
            long end=System.currentTimeMillis()+5000;while((Integer)count.invoke(null)>0){if(System.currentTimeMillis()>end)throw new AssertionError("Import preparation timed out");Thread.sleep(10);}
        }catch(ClassNotFoundException absent){/* Current synchronous renderer is the RED baseline. */}
        runner.waitForIdleSync();
    }
    static int run(Context c,Instrumentation runner) throws Exception {
        GridSettings state=new GridSettings(c);state.imports=new org.json.JSONArray();state.multi=false;state.hidden=false;state.outline=false;state.opacity=1;state.save();
        String id=GridImporter.importBytes(c,"cache-check.svg","<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'><path d='M25 0 L25 100'/></svg>".getBytes(StandardCharsets.UTF_8));
        AtomicReference<GridView> view=new AtomicReference<>();Bitmap image=Bitmap.createBitmap(400,600,Bitmap.Config.ARGB_8888);
        try {
            runner.runOnMainSync(()->{view.set(new GridView(c));view.get().layout(0,0,400,600);});waitAssets(runner);
            runner.runOnMainSync(()->view.get().draw(new Canvas(image)));
            var field=GridView.class.getDeclaredField("pictures");field.setAccessible(true);Map<?,?> pictures=(Map<?,?>)field.get(view.get());Object original=pictures.values().iterator().next();
            state=new GridSettings(c);state.color=0xffff0000;state.save();
            runner.runOnMainSync(()->{view.get().applySettings();image.eraseColor(0);view.get().draw(new Canvas(image));});
            SettingsChecks.require(pictures.values().contains(original),"Color change rebuilt SVG picture");
            SettingsChecks.require((image.getPixel(100,300)&0xffffff)==0xff0000,"Cached picture failed to recolor");
            state=new GridSettings(c);state.thickness=4;state.save();runner.runOnMainSync(()->{view.get().applySettings();view.get().draw(new Canvas(image));});
            SettingsChecks.require(!pictures.values().contains(original),"Thickness failed to invalidate SVG picture");
            SettingsChecks.require(KitIcon.bitmap(c,R.raw.kit_grid)==KitIcon.bitmap(c,R.raw.kit_grid),"Repeated menu icons were reparsed");
            String delayedId=GridImporter.importBytes(c,"cancel-check.svg","<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 10 10'><path d='M0 0 L10 10'/></svg>".getBytes(StandardCharsets.UTF_8));
            java.util.ArrayList<Runnable> jobs=new java.util.ArrayList<>();int[] published={0};ImportAssets pending=new ImportAssets(c,jobs::add);
            try{
                ImportedGrid file=ImportedGrid.parse(new GridSettings(c).imports.optJSONObject(new GridSettings(c).imports.length()-1));
                pending.load(file,result->published[0]++);pending.close();GridFiles.remove(c,delayedId);
                for(Runnable job:jobs)job.run();waitAssets(runner);SettingsChecks.require(published[0]==0,"Deleted/detached import published a late result");
            }finally{pending.close();GridFiles.remove(c,delayedId);}
            return 5;
        }finally{detach(runner,view.get());image.recycle();GridFiles.remove(c,id);}
    }
    static void detach(Instrumentation runner,GridView view){runner.runOnMainSync(()->{try{var method=android.view.View.class.getDeclaredMethod("onDetachedFromWindow");method.setAccessible(true);method.invoke(view);}catch(Exception error){throw new AssertionError(error);}});}
}
