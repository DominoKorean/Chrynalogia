package dev.chrynalogia.probe;

/** Layout in screen coordinates. Kept independent of Android for frame-edge regressions. */
final class CameraMenuPlacement {
    record Box(int left,int top,int right,int bottom) {
        int width(){return right-left;}int height(){return bottom-top;}
    }
    record Placement(Box opener,Box menu,boolean horizontal,int cellWidth,int cellHeight) {}
    static Placement place(Box safe,Box preview,int inset,int headerY,int size,int openerHeight,int gap,int count,boolean landscape) {
        return place(safe,preview,inset,headerY,size,openerHeight,gap,count,landscape,safe.right());
    }
    static Placement place(Box safe,Box preview,int inset,int headerY,int size,int openerHeight,int gap,int count,boolean landscape,int menuRightLimit) {
        int x=clamp(safe.left()+inset,safe.left(),safe.right()-size);
        int y=clamp(headerY,safe.top(),safe.bottom()-openerHeight);
        int cellHeight=size;
        if(landscape) {
            // A logically rotated camera has a side strip; keep the whole group in that strip.
            Box band=null;
            if(preview!=null&&preview.left()-safe.left()>=size)band=new Box(safe.left(),safe.top(),preview.left(),safe.bottom());
            else if(preview!=null&&safe.right()-preview.right()>=size)band=new Box(preview.right(),safe.top(),safe.right(),safe.bottom());
            if(band!=null) {
                x=clamp(band.left()+inset,band.left(),band.right()-size);
                cellHeight=Math.min(size,Math.max(1,(band.height()-openerHeight-gap)/count));
                y=clamp(y,band.top(),band.bottom()-openerHeight-gap-count*cellHeight);
                return new Placement(new Box(x,y,x+size,y+openerHeight),
                    new Box(x,y+openerHeight+gap,x+size,y+openerHeight+gap+count*cellHeight),false,size,cellHeight);
            }
            // Samsung normally locks the window in portrait while its own icons turn.
            // Use the native top-button strip horizontally in that case.
            int available=Math.min(safe.right(),menuRightLimit)-x-size-gap;
            int cellWidth=Math.min(size,Math.max(1,available/count));
            int topLimit=preview==null||preview.top()<=safe.top()?safe.bottom():preview.top();
            cellHeight=Math.min(openerHeight,Math.max(1,topLimit-safe.top()));
            y=clamp(y,safe.top(),topLimit-cellHeight);
            return new Placement(new Box(x,y,x+size,y+cellHeight),
                new Box(x+size+gap,y,x+size+gap+count*cellWidth,y+cellHeight),true,cellWidth,cellHeight);
        }
        int menuY=y+openerHeight+gap;
        if(menuY+count*cellHeight>safe.bottom())menuY=Math.max(safe.top(),y-gap-count*cellHeight);
        return new Placement(new Box(x,y,x+size,y+openerHeight),new Box(x,menuY,x+size,Math.min(safe.bottom(),menuY+count*cellHeight)),false,size,cellHeight);
    }
    private static int clamp(int n,int min,int max){return Math.max(min,Math.min(n,Math.max(min,max)));}
}
