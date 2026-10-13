package dev.chrynalogia.probe;
import org.junit.Test;
import static org.junit.Assert.*;

public class SessionPolicyTest {
    private Object input(boolean onboarding,String mode,boolean paused,int requested,int boot,boolean permission,boolean ready) throws Exception {
        Class<?> type=Class.forName("dev.chrynalogia.probe.SessionPolicy$Input");
        var constructor=type.getDeclaredConstructor(boolean.class,String.class,boolean.class,int.class,int.class,boolean.class,boolean.class);
        constructor.setAccessible(true);return constructor.newInstance(onboarding,mode,paused,requested,boot,permission,ready);
    }
    private boolean active(boolean onboarding,String mode,boolean paused,int requested,int boot,boolean permission,boolean ready) throws Exception {
        Object input=input(onboarding,mode,paused,requested,boot,permission,ready);
        var method=Class.forName("dev.chrynalogia.probe.SessionPolicy").getDeclaredMethod("active",input.getClass());method.setAccessible(true);
        return (Boolean)method.invoke(null,input);
    }
    @Test public void alwaysModeRequiresOnboardingAndUnpausedButNotManualService() throws Exception {
        assertTrue(active(true,"always",false,-1,3,false,false));
        assertFalse(active(false,"always",false,-1,3,false,false));
        assertFalse(active(true,"always",true,-1,3,true,true));
    }
    @Test public void manualModeRequiresNotificationAndActualForegroundReadiness() throws Exception {
        assertTrue(active(true,"manual",false,3,3,true,true));
        assertFalse(active(true,"manual",false,3,3,true,false));
        assertFalse(active(true,"manual",false,3,3,false,true));
        assertFalse(active(true,"manual",true,3,3,true,true));
    }
    @Test public void manualRequestCannotSurviveRebootOrUnknownBootCount() throws Exception {
        assertFalse(active(true,"manual",false,2,3,true,true));
        assertFalse(active(true,"manual",false,-2,-2,true,true));
    }
    @Test @SuppressWarnings({"unchecked","rawtypes"}) public void recreationNeverStartsStoppedSession() throws Exception {
        Object input=input(true,"manual",true,3,3,true,false);
        Class<?> cause=Class.forName("dev.chrynalogia.probe.SessionPolicy$StartCause");
        var method=Class.forName("dev.chrynalogia.probe.SessionPolicy").getDeclaredMethod("shouldStart",cause,input.getClass());method.setAccessible(true);
        assertFalse((Boolean)method.invoke(null,Enum.valueOf((Class)cause,"RECREATION"),input));
        assertTrue((Boolean)method.invoke(null,Enum.valueOf((Class)cause,"USER"),input));
    }
}
