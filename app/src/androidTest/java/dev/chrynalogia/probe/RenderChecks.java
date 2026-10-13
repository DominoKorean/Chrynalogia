package dev.chrynalogia.probe;

import android.app.Instrumentation;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.os.Bundle;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Device-side rendering/import checks, with user preferences restored in finally. */
public final class RenderChecks extends Instrumentation {
    private int passed;
    private String suite="all";
    private final ArrayList<File> created=new ArrayList<>();
    @Override public void onCreate(Bundle arguments) {super.onCreate(arguments);if(arguments!=null)suite=arguments.getString("suite","all");start();}
    @Override public void onStart() {
        Context c=getTargetContext();SharedPreferences prefs=c.getSharedPreferences("probe",Context.MODE_PRIVATE);
        Map<String,?> original=new HashMap<>(prefs.getAll());
        boolean originalRunning=ManualSessionService.running;
        Bundle result=new Bundle();
        int resultCode=-1;
        try {
            if(suite.equals("settings")) {
                passed+=SettingsChecks.run(c);
            } else {
            GridSettings settings=new GridSettings(c);settings.hidden=false;settings.rotation=0;settings.flipX=false;settings.flipY=false;
            settings.color=Color.WHITE;settings.opacity=1;settings.thickness=1.2f;settings.outline=false;settings.multi=false;settings.save();
            for(String id:GridGeometry.IDS) {settings.selected=Set.of(id);settings.save();check(nonempty(render(c))>0,"Rendered "+id);}
            settings.selected=Set.of("golden_ratio");settings.save();Bitmap originalGrid=render(c);
            check(alpha(originalGrid,247,100)>100,"Golden line at 61.8%");
            settings.flipX=true;settings.save();Bitmap flipped=render(c);
            check(alpha(flipped,153,100)>100 && alpha(flipped,247,100)==0,"Horizontal reflection");
            settings.flipX=false;settings.rotation=1;settings.save();Bitmap rotated=render(c);
            check(alpha(rotated,153,100)>100,"Quarter turn fits the preview");
            settings.flipX=true;settings.save();Bitmap rotatedFlipped=render(c);
            check(alpha(rotatedFlipped,247,100)>100 && alpha(rotatedFlipped,153,100)==0,"Flip stays horizontal after rotation");
            settings.flipX=false;
            settings.rotation=0;settings.selected=Set.of("rabatment");settings.save();Bitmap horizontalRabatment=render(c);
            check(alpha(horizontalRabatment,100,200)>100&&alpha(horizontalRabatment,133,100)==0,"Rabatment starts with horizontal lines");
            settings.rotation=1;settings.save();Bitmap verticalRabatment=render(c);
            check(alpha(verticalRabatment,133,100)>100&&alpha(verticalRabatment,100,200)==0,"Rabatment quarter turn changes line direction");
            settings.selected=Set.of("spiral");settings.rotation=0;settings.save();Rect spiralBounds=visibleBounds(render(c));
            settings.rotation=1;settings.save();Rect turnedSpiralBounds=visibleBounds(render(c));
            check(Math.abs(turnedSpiralBounds.height()/(float)turnedSpiralBounds.width()-spiralBounds.width()/(float)spiralBounds.height())<0.03f,"Quarter turn preserves the actual spiral shape proportions");
            settings.rotation=0;settings.selected=Set.of("cross");settings.color=Color.BLACK;settings.outline=true;settings.thickness=4;settings.save();
            Bitmap darkBuiltIn=render(c);check(hasColor(darkBuiltIn,Color.WHITE),"Dark built-in line has white outline");
            settings.color=Color.WHITE;settings.save();Bitmap lightBuiltIn=render(c);check(hasColor(lightBuiltIn,Color.BLACK),"Light built-in line has black outline");
            settings.outline=false;settings.thickness=1.2f;
            settings.rotation=0;settings.hidden=true;settings.save();check(nonempty(render(c))==0,"Hidden grid");
            settings.hidden=false;settings.save();
            String svg="<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 100 100\"><path d=\"M 25 0 L 25 100\" stroke=\"red\" fill=\"none\" style=\"stroke-width:9;stroke:red;fill:none\"/></svg>";
            String svgId=GridImporter.importBytes(c,"render-check.svg",svg.getBytes(StandardCharsets.UTF_8));created.add(new File(c.getFilesDir(),"grids/"+svgId+".svg"));
            settings.reload();settings.keepAspect=false;settings.save();Bitmap stretched=render(c);
            check(alpha(stretched,100,30)>100 && alpha(stretched,100,550)>100,"SVG stretch fills height");
            settings.keepAspect=true;settings.save();Bitmap retained=render(c);
            check(alpha(retained,100,30)==0 && alpha(retained,100,150)>100,"SVG retains square aspect");
            settings.keepAspect=false;settings.thickness=4;settings.outline=true;settings.save();check(nonempty(render(c))>nonempty(stretched),"SVG line thickness and outline");
            settings.color=Color.BLACK;settings.save();check(hasColor(render(c),Color.WHITE),"Dark SVG line has white outline");
            settings.color=Color.WHITE;settings.save();check(hasColor(render(c),Color.BLACK),"Light SVG line has black outline");
            Bitmap source=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);Canvas canvas=new Canvas(source);Paint paint=new Paint();paint.setColor(Color.WHITE);paint.setStrokeWidth(5);canvas.drawLine(25,0,25,100,paint);
            ByteArrayOutputStream png=new ByteArrayOutputStream();source.compress(Bitmap.CompressFormat.PNG,100,png);source.recycle();
            String pngId=GridImporter.importBytes(c,"render-check.png",png.toByteArray());created.add(new File(c.getFilesDir(),"grids/"+pngId+".png"));
            settings.reload();settings.thickness=0.3f;settings.outline=false;settings.save();Bitmap thin=render(c);
            settings.thickness=6;settings.save();Bitmap thick=render(c);check(nonempty(thin)==nonempty(thick),"Bitmap ignores line thickness");
            settings.color=Color.RED;settings.save();Bitmap red=render(c);check(Color.red(red.getPixel(100,200))>200 && Color.green(red.getPixel(100,200))==0,"Bitmap monochrome recoloring");
            settings.outline=true;settings.save();check(nonempty(render(c))>nonempty(red),"Bitmap outline keeps image thickness independent");
            settings.color=Color.BLACK;settings.save();check(hasContrastEdge(render(c),true),"Dark bitmap line has light contrast edge");
            settings.color=Color.WHITE;settings.save();check(hasContrastEdge(render(c),false),"Light bitmap line has dark contrast edge");
            reject(c,"invalid.png","not an image".getBytes(StandardCharsets.UTF_8));
            reject(c,"entities.svg","<!DOCTYPE svg [<!ENTITY x 'x'>]><svg xmlns='http://www.w3.org/2000/svg'/>".getBytes(StandardCharsets.UTF_8));
            reject(c,"oversize.svg",new byte[GridImporter.MAX_BYTES+1]);
            settings.runMode="manual";settings.onboarding=true;settings.save();settings.activate(c);ManualSessionService.running=false;
            check(!settings.active(c),"Manual mode never runs without its foreground notification service");
            ManualSessionService.running=true;check(settings.active(c),"Manual session active in current boot");
            prefs.edit().putInt("sessionBoot",GridSettings.boot(c)-1).commit();check(!settings.active(c),"Manual session expires across boot count");
            }
            result.putString("stream","PASS: "+passed+" "+suite+" checks\n");
        } catch(Throwable error) {resultCode=0;result.putString("stream","FAIL after "+passed+" checks: "+error+"\n");}
        finally {
            SharedPreferences.Editor edit=prefs.edit().clear();
            for(var item:original.entrySet()) {
                String k=item.getKey();Object v=item.getValue();
                if(v instanceof String s)edit.putString(k,s);else if(v instanceof Boolean b)edit.putBoolean(k,b);
                else if(v instanceof Integer n)edit.putInt(k,n);else if(v instanceof Float n)edit.putFloat(k,n);
                else if(v instanceof Long n)edit.putLong(k,n);else if(v instanceof Set<?> set){Set<String> strings=new HashSet<>();for(Object s:set)strings.add((String)s);edit.putStringSet(k,strings);}
            }
            edit.commit();ManualSessionService.running=originalRunning;for(File file:created)file.delete();
        }
        finish(resultCode,result);
    }
    private void check(boolean value,String name) {if(!value)throw new AssertionError(name);passed++;}
    private void reject(Context c,String name,byte[] bytes) throws Exception {
        try {GridImporter.importBytes(c,name,bytes);throw new AssertionError("Accepted invalid input: "+name);}
        catch(java.io.IOException|com.caverock.androidsvg.SVGParseException expected){passed++;}
    }
    private Bitmap render(Context c) {
        Bitmap result=Bitmap.createBitmap(400,600,Bitmap.Config.ARGB_8888);
        runOnMainSync(()->{GridView v=new GridView(c);v.layout(0,0,400,600);v.draw(new Canvas(result));});return result;
    }
    private static int alpha(Bitmap b,int x,int y) {return Color.alpha(b.getPixel(x,y));}
    private static Rect visibleBounds(Bitmap b) {Rect bounds=new Rect();for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++)if(alpha(b,x,y)>100)bounds.union(x,y,x+1,y+1);return bounds;}
    private static boolean hasColor(Bitmap b,int color) {for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++){int p=b.getPixel(x,y);if(Color.alpha(p)>100&&(p&0xffffff)==(color&0xffffff))return true;}return false;}
    // Bitmap edges interpolate source texels and overlap the core. Check visible contrast, not an exact unblended RGB.
    private static boolean hasContrastEdge(Bitmap b,boolean light) {for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++){int p=b.getPixel(x,y);if(Color.alpha(p)>100&&(light?Color.red(p)>80:Color.red(p)<175))return true;}return false;}
    private static int nonempty(Bitmap b) {int count=0;for(int y=0;y<b.getHeight();y++)for(int x=0;x<b.getWidth();x++)if(alpha(b,x,y)>0)count++;return count;}
}
