package nl.ma.utopiaserver;
/**
 * Class to provide the timeStamp information needed for the messages
 */
/*
 * Copyright (c) MindAffect B.V. 2018
 * For internal use only.  Distribution prohibited.
 */
public class TimeStampClock {
    private static long t0;
    /**
     * construct a new time-stamp clock.
     */
    public TimeStampClock(){
        t0 = 0;//getAbsTime();
	 }
	 public void setZeroTime(long t0){ this.t0=t0; }
    /**
     * get the current time, relative to clock construction time.
     */   
    public static int getTime(){
        return (int)(getAbsTime()-t0);
    }
    /**
     * get the current absolute time -- i.e. from nanoTime
     */
    public static long getAbsTime(){
        return System.currentTimeMillis();//System.nanoTime()/1000000;//
    }
};
