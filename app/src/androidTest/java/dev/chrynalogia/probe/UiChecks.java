package dev.chrynalogia.probe;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

/** Exercises custom widgets against real Android drawing and invalidation behavior. */
final class UiChecks {
    static int run(Context c,Instrumentation runner) {
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        runner.runOnMainSync(()->{
          try {
            Context themed=Ui.context(c);
            KitSwitch toggle=new KitSwitch(themed);
            toggle.setText("Switch");
            toggle.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(80,View.MeasureSpec.EXACTLY));
            toggle.layout(0,0,320,80);
            Bitmap off=Bitmap.createBitmap(320,80,Bitmap.Config.ARGB_8888);
            toggle.draw(new Canvas(off));
            SettingsChecks.require(!toggle.isDirty(),"Switch baseline draw must clear dirty state");
            toggle.performClick();
            SettingsChecks.require(toggle.isChecked(),"Native switch click changes checked state");
            SettingsChecks.require(toggle.isDirty(),"Checked custom SVG switch must invalidate without rebuilding its tab");
            Bitmap on=Bitmap.createBitmap(320,80,Bitmap.Config.ARGB_8888);
            toggle.draw(new Canvas(on));
            SettingsChecks.require(!off.sameAs(on),"Checked custom SVG must visibly redraw");
            toggle.setChecked(false);
            SettingsChecks.require(toggle.isDirty(),"Programmatic switch change must redraw too");
            off.recycle();on.recycle();
            c.getSharedPreferences("probe",0).edit().putInt("cameraTab",1).putFloat("opacity",0.45f).putFloat("thickness",3.3f).putInt("rotation",0).commit();
            SettingsPanel panel=new SettingsPanel(c,new SettingsPanel.Actions(){
                public void changed(){}public void importGrid(){}public void modeChanged(String mode){}public void stop(){}
            },true);
            // A second writer changes geometry while the style panel still has its snapshot.
            c.getSharedPreferences("probe",0).edit().putInt("rotation",2).commit();
            View reset=find(panel,c.getString(R.string.reset_line_style));
            SettingsChecks.require(reset!=null,"Line style reset action is available");reset.performClick();
            GridSettings saved=new GridSettings(c);
            SettingsChecks.require(saved.opacity==0.9f&&saved.thickness==1.2f,"One reset restores both line style defaults");
            SettingsChecks.require(saved.rotation==2,"Line style reset preserves another writer's geometry");
            SettingsChecks.require(((SeekBar)find(panel,c.getString(R.string.opacity))).getProgress()==90,"Reset updates visible opacity control");
            SettingsChecks.require(((SeekBar)find(panel,c.getString(R.string.thickness))).getProgress()==12,"Reset updates visible thickness control");
            c.getSharedPreferences("probe",0).edit().putInt("cameraTab",2).putBoolean("onboarding",true).putString("runMode","always").putBoolean("paused",false).commit();
            SettingsPanel execution=new SettingsPanel(c,new SettingsPanel.Actions(){
                public void changed(){}public void importGrid(){}public void modeChanged(String mode){}
                public void stop(){SessionController.stop(c);}
                public void startSession(){SessionController.requestStart(c);}
            },true);
            View control=find(execution,c.getString(R.string.stop_background));
            SettingsChecks.require(control instanceof android.widget.CompoundButton,"Background activity must expose a checked switch");
            android.widget.CompoundButton active=(android.widget.CompoundButton)control;
            SettingsChecks.require(active.isChecked(),"Active session starts checked");
            active.performClick();
            SettingsChecks.require(!new GridSettings(c).active(c),"Background switch stops the session");
            active.performClick();
            SettingsChecks.require(new GridSettings(c).active(c),"Background switch starts the session");
            c.getSharedPreferences("probe",0).edit().putInt("appTab",0).commit();
            int[] launched={0,0};
            SettingsPanel cameras=new SettingsPanel(c,new SettingsPanel.Actions(){
                public void changed(){}public void importGrid(){}public void modeChanged(String mode){}public void stop(){}
                public void openCamera(){launched[0]++;}public void openExpertRaw(){launched[1]++;}
            });
            findText(cameras,c.getString(R.string.open_camera)).performClick();
            SettingsChecks.require(launched[0]==1&&launched[1]==0,"Samsung Camera keeps its own launch action");
            View expert=findText(cameras,c.getString(R.string.open_expert_raw));
            boolean installed=c.getPackageManager().getLaunchIntentForPackage(CameraApps.EXPERT_RAW)!=null;
            SettingsChecks.require((expert!=null)==installed,"Expert RAW launch is offered only when installed");
            if(expert!=null){expert.performClick();SettingsChecks.require(launched[0]==1&&launched[1]==1,"Expert RAW launches through its own action");}
            c.getSharedPreferences("probe",0).edit().putInt("cameraTab",0).commit();
            SettingsPanel cameraPopup=new SettingsPanel(c,new SettingsPanel.Actions(){
                public void changed(){}public void importGrid(){}public void modeChanged(String mode){}public void stop(){}
            },true);
            SettingsChecks.require(findText(cameraPopup,c.getString(R.string.open_expert_raw))==null,"In-camera settings do not add a camera launcher");
          } catch(Throwable error){failure.set(error);}
        });
        if(failure.get()!=null)throw new AssertionError("UI regression",failure.get());
        return c.getPackageManager().getLaunchIntentForPackage(CameraApps.EXPERT_RAW)!=null?18:17;
    }
    private static View findText(View root,String text){return PermissionSetupChecks.find(root,text);}
    private static View find(View root,String description){
        if(description.contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;
        if(root instanceof ViewGroup group)for(int i=0;i<group.getChildCount();i++){View result=find(group.getChildAt(i),description);if(result!=null)return result;}
        return null;
    }
}
