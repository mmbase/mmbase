package org.mmbase.applications.crontab;


import java.util.*;
import org.mmbase.util.logging.*;

public class JCronEntry {
    
    private static final Logger log = Logging.getLoggerInstance(JCronEntry.class);
    private JCronJob jCronJob;

    private Thread thread;
    
    private String id;
    private String name;
    private String className;
    
    private JCronEntryField minute    ;// 0-59
    private JCronEntryField hour      ;// 0-23
    private JCronEntryField dayOfMonth;//1-31
    private JCronEntryField month     ;//1-12
    private JCronEntryField dayOfWeek ;//0-7 (0 or 7 is sunday)
    
    public JCronEntry(String id, String crontime, String name, String className) throws Exception {
        this.id = id;
        this.name = name;
        this.className = className;
        jCronJob = (JCronJob)Class.forName(className).newInstance();
        jCronJob.init(this);
        minute = new JCronEntryField();
        hour = new JCronEntryField();
        dayOfMonth = new JCronEntryField();
        month = new JCronEntryField();
        dayOfWeek = new JCronEntryField();
        setTimeVal(crontime);
    }
    
    public void kick() {
        if (thread == null) {
            thread = new Thread(jCronJob, "JCronJob(" + toString() + ")");
            thread.setDaemon(true);            
        }
        if (thread.isAlive()) {
            log.warn("Job " + jCronJob + " still running, so not restarting it again.");
        } else {
            thread.start();
        }

    }
    
    public void setTimeVal(String crontime){
        StringTokenizer st = new StringTokenizer(crontime," ");
        minute.setTimeVal(st.nextToken());
        hour.setTimeVal(st.nextToken());
        dayOfMonth.setTimeVal(st.nextToken());
        month.setTimeVal(st.nextToken());
        dayOfWeek.setTimeVal(st.nextToken());
    }
    
    public String getID(){
        return id;
    }
    public String getName(){
        return name;
    }
    
    public boolean mustRun(Date date){
        Calendar cal = Calendar.getInstance();
        if (
        minute.valid(cal.get(cal.MINUTE)) &&
        hour.valid(cal.get(cal.HOUR_OF_DAY)) &&
        dayOfMonth.valid(cal.get(cal.DAY_OF_MONTH)) &&
        month.valid(cal.get(cal.MONTH) + 1) &&
        dayOfWeek.valid(cal.get(cal.DAY_OF_WEEK) -1)){
            return true;
        }
        return false;
    }
    
    public JCronEntryField getMinuteEntry(){
        return minute;
    }
    public JCronEntryField getHourEntry(){
        return hour;
    }
    
    public JCronEntryField getDayOfMonthEntry(){
        return dayOfMonth;
    }
    
    public JCronEntryField getMonthEntry(){
        return month;
    }
    
    public JCronEntryField getDayOfWeekEntry(){
        return dayOfWeek;
    }

    public String toString() {
        return name + ": " + className;
    }
}
