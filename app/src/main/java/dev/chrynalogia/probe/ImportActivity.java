package dev.chrynalogia.probe;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

/** System document picker grants access only to the user-selected file. */
public final class ImportActivity extends Activity {
    private boolean returnCamera;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);returnCamera=getIntent().getBooleanExtra("returnCamera",false);
        if(state==null) {
            Intent picker=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE);
            // SVG MIME types vary between document providers. Validate the selected bytes ourselves.
            try {startActivityForResult(picker,1);}catch(android.content.ActivityNotFoundException e){Toast.makeText(this,this.getString(R.string.picker_missing),Toast.LENGTH_LONG).show();finish();}
        }
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK || data==null || data.getData()==null) {done();return;}
        android.net.Uri uri=data.getData();
        new Thread(()->{
            String message;
            try {GridImporter.importUri(this,uri);message=this.getString(R.string.grid_added);}
            catch(Exception error){message=error.getMessage()==null?this.getString(R.string.file_read_error):error.getMessage();}
            String finalMessage=message;runOnUiThread(()->{Toast.makeText(this,finalMessage,Toast.LENGTH_LONG).show();done();});
        },"grid-import").start();
    }
    private void done() {
        if(CameraWatchService.instance!=null)CameraWatchService.instance.refreshSettings();
        if(returnCamera) {
            Intent camera=getPackageManager().getLaunchIntentForPackage(CameraWatchService.CAMERA_PACKAGE);
            if(camera!=null)startActivity(camera);
        }
        finish();
    }
}

