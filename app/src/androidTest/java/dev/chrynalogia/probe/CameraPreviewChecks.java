package dev.chrynalogia.probe;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

/** Inspects the real foreground camera without waiting for constantly changing exposure labels. */
final class CameraPreviewChecks {
    static int inspect(Context context,Instrumentation runner) throws Exception {
        var automation=runner.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        var info=automation.getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                |android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                |android.accessibilityservice.AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        automation.setServiceInfo(info);
        StringBuilder tree=new StringBuilder();int roots=0;
        for(var window:automation.getWindows()){
            var root=window.getRoot();if(root==null)continue;
            if(!CameraApps.supports(root.getPackageName())&&!"dev.chrynalogia.probe".contentEquals(root.getPackageName()))continue;
            tree.append("WINDOW ").append(window.getTitle()).append('\n');dump(root,tree,0,new int[]{0});roots++;
        }
        java.nio.file.Files.writeString(new java.io.File(context.getFilesDir(),"camera-windows-ui.txt").toPath(),tree);
        SettingsChecks.require(roots>0,"No supported camera or app windows to inspect");return 1;
    }
    static int run(Context context,Instrumentation runner) throws Exception {
        var automation=runner.getUiAutomation(android.app.UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES);
        var info=automation.getServiceInfo();info.flags|=android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
                |android.accessibilityservice.AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
                |android.accessibilityservice.AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        automation.setServiceInfo(info);
        AccessibilityNodeInfo root=automation.getRootInActiveWindow();
        for(int i=0;i<30&&root==null;i++){Thread.sleep(100);root=automation.getRootInActiveWindow();}
        if(root==null)throw new AssertionError("Foreground camera root is unavailable");
        SettingsChecks.require("com.samsung.android.app.galaxyraw".contentEquals(root.getPackageName()),"Open Expert RAW before the live preview check");
        StringBuilder tree=new StringBuilder();dump(root,tree,0,new int[]{0});
        java.nio.file.Files.writeString(new java.io.File(context.getFilesDir(),"camera-live-ui.txt").toPath(),tree);
        Rect window=new Rect();root.getBoundsInScreen(window);
        PreviewLocator.Result result=new PreviewLocator().locate(root,window);
        java.nio.file.Files.writeString(new java.io.File(context.getFilesDir(),"camera-live-result.txt").toPath(),
                String.valueOf(root.getPackageName())+" "+window+"\n"+result);
        SettingsChecks.require(!result.bounds().isEmpty(),"Expert RAW must expose its actual preview bounds: "+result);
        SettingsChecks.require(window.contains(result.bounds()),"The preview must remain inside the camera window");
        SettingsChecks.require("Expert RAW".equals(result.mode()),"Expert RAW must be recognized as a supported camera mode");
        return 4;
    }
    private static void dump(AccessibilityNodeInfo node,StringBuilder output,int depth,int[] count){
        if(node==null||depth>40||++count[0]>1200)return;
        Rect bounds=new Rect();node.getBoundsInScreen(bounds);
        output.append("  ".repeat(depth)).append(node.getViewIdResourceName()).append(" | ")
                .append(node.getClassName()).append(" | ").append(node.isVisibleToUser()).append(" | ")
                .append(bounds.toShortString()).append(" | ").append(node.getText()).append(" | ")
                .append(node.getContentDescription()).append('\n');
        for(int i=0;i<node.getChildCount();i++)dump(node.getChild(i),output,depth+1,count);
    }
}
