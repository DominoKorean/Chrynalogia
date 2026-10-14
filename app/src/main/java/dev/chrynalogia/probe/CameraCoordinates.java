package dev.chrynalogia.probe;

/** Camera anchors use natural-device coordinates; only the display projection changes. */
final class CameraCoordinates {
    static CameraMenuPlacement.Box rotate(CameraMenuPlacement.Box box,CameraMenuPlacement.Box frame,int quarter){
        int l=box.left()-frame.left(),t=box.top()-frame.top(),r=box.right()-frame.left(),b=box.bottom()-frame.top();
        int x=frame.left(),y=frame.top(),w=frame.width(),h=frame.height();
        return switch(Math.floorMod(quarter,4)){
            case 1->new CameraMenuPlacement.Box(x+t,y+w-r,x+b,y+w-l);
            case 2->new CameraMenuPlacement.Box(x+w-r,y+h-b,x+w-l,y+h-t);
            case 3->new CameraMenuPlacement.Box(x+h-b,y+l,x+h-t,y+r);
            default->box;
        };
    }
    // OrientationEventListener measures physical clockwise turns; Display uses the opposite direction.
    static int iconAngle(int displayQuarter,int sensorQuarter){return -Math.floorMod(displayQuarter+sensorQuarter,4)*90;}
}
