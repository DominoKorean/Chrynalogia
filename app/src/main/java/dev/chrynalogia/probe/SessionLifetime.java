package dev.chrynalogia.probe;

/** A previous service instance cannot terminate a later user request. */
final class SessionLifetime {
    private long generation;
    private Object owner;
    private long ownerGeneration=-1;
    synchronized long request(){generation++;owner=null;ownerGeneration=-1;return generation;}
    synchronized long generation(){return generation;}
    synchronized boolean current(long value){return value==generation;}
    synchronized boolean ready(){return owner!=null&&ownerGeneration==generation;}
    synchronized void ready(Object service,long value){if(current(value)){owner=service;ownerGeneration=value;}}
    synchronized boolean ended(Object service,long value){
        boolean ownsRequest=owner==null||owner==service;
        if(owner==service&&ownerGeneration==value){owner=null;ownerGeneration=-1;}
        return current(value)&&ownsRequest;
    }
}
