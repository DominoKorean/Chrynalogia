package dev.chrynalogia.probe;
/** Platform-independent session decisions; foreground readiness is actual service state. */
final class SessionPolicy {
    record Input(boolean onboarding,String runMode,boolean paused,int requestedBoot,int currentBoot,boolean notificationGranted,boolean foregroundReady){}
    enum StartCause {USER,REENTRY,RECREATION}
    static boolean requested(Input i){return i.onboarding&&!i.paused&&i.currentBoot>=0&&i.requestedBoot==i.currentBoot;}
    static boolean active(Input i){return i.onboarding&&!i.paused&&(!i.runMode.equals("manual")||(requested(i)&&i.notificationGranted&&i.foregroundReady));}
    static boolean shouldStart(StartCause cause,Input i){return cause!=StartCause.RECREATION&&i.onboarding&&(!i.runMode.equals("manual")||i.notificationGranted);}
}
