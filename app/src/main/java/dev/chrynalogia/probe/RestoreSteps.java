package dev.chrynalogia.probe;

/** Attempt every independent restoration, retaining each failure. */
final class RestoreSteps {
    interface Step {void run() throws Exception;}
    static void run(Step... steps) throws Exception {
        Exception failure=null;
        for(Step step:steps)try{step.run();}catch(Exception error){if(failure==null)failure=error;else if(error!=failure)failure.addSuppressed(error);}
        if(failure!=null)throw failure;
    }
}
