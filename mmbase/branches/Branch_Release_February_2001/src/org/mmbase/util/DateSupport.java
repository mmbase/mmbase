/*

This software is OSI Certified Open Source Software.
OSI Certified is a certification mark of the Open Source Initiative.

The license (Mozilla version 1.0) can be read at the MMBase site.
See http://www.MMBase.org/license

*/
package org.mmbase.util;

import java.util.*;
import java.text.*;
import java.net.*;
import java.util.StringTokenizer;

/**
 * Some routines to support dates better<br><br>
 *
 * The problem that generally occurs is with timezones. Therefore, we have made the following structure:<br>
 * <ul>
 * <li> If a date is stored in a database, it is in GMT
 * <li> If a date is displayed, it happens in the timezone of the machine that is calling.
 * </ul>
 * This means that some timezone conversions have to be made. 
 * We assume nothing about timezones, we just read the value specified by the system (Timezone.getDefault() call).
 * 
 * @author Rico Jansen, bugfixes by Johannes Verelst
 * @version 21 Mar 1997, 14 Sept 2000
 */
public class DateSupport {

	static int offset=0;
	static boolean dooffset=false;

	static {
		Calendar cal = Calendar.getInstance();
		dooffset = true;
		offset = (cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET))/ 1000;
		offset = 0;
		//System.out.println("Using offset " + offset);
	}

	/**
	 * Return the numer of days in the month in a specified year. Leap years have to be taken into account
	 *
	 * @param year The year
	 * @param month The month
	 * @return The numbers of days in the month of that year
	 * @see DateSupport#secondInYear
	 * @see DateSupport#dayInYear
	 * @see DateSupport#weekInYear
	 */
    static public int daysInMonth(int year, int month) {
        int months[]={ 31,28,31,30,31,30,30,31,30,31,30,31 };
		int days = months[month];
		year = (year<90) ? year+2000 : year+1900;

		// Make an exception for the intercalary day. 
		if (month==1) {
				if(year%4==0 && year%100!=0 || year%400==0) days=29;
		} 
		return days;
	}

	/**
	 * Return the number of seconds that have elapsed from the beginning of the year to the given date
	 *
	 * @param d The date 
	 * @return The number of secods from January 1 to the given date
	 * @see DateSupport#daysInMonth
	 * @see DateSupport#dayInYear
	 * @see DateSupport#weekInYear
	 */
	static public int secondInYear(Date d) {
		Date b = new Date ((d.getYear()),0,0);
		return((int)((d.getTime()-b.getTime())/1000));
	}

	/**
	 * Return the number of days that have elapsed from the beginning of the year to the given date
	 *
	 * @param d The date
	 * @return The number of days from January 1 to the given date
	 * @see DateSupport#daysInMonth
	 * @see DateSupport#secondInYear
	 * @see DateSupport#weekInYear
	 */
	static public int dayInYear(Date d) {
		return((int)(secondInYear(d)/(3600*24)));
	}

	/**     
	 * Return the number of weeks that have elapsed from the beginning of the year to the given date
	 *
	 * @param d The date
	 * @return The number of weeks from January 1 to the given date
	 * @see DateSupport#daysInMonth
	 * @see DateSupport#secondInYear
	 * @see DateSupport#dayInYear
	 */
	static public int weekInYear(Date d) {
		return((dayInYear(d)/7)+1);
	}

	/**     
	 * Return the number milliseconds elapsed from 1-Jan-1970 to the beginning of the given week.
	 *
	 * @param year The year
	 * @param week The number of the week
	 * @return The number of milliseconds between 1-Jan-1970 and the begin of the given week.
	 */
	static public long milliDate(int year,int week) {
		Date d;
		d=new Date(year,0,0);
		return(d.getTime()+(((long)(week-1))*7*24*3600*1000));
	}

	/**
	 * Return a date, based on a year, a week and the day of that week  <br>
	 * For instance: 1999, 40, 4 = The 4th day of the 40th week of 1999
	 *
	 * @param year The year
	 * @param week The week
	 * @param day The number of the day in the week
	 * @return A date-object for the given date
	 */
	static public Date Date(int year,int week,int day) {
		Date d;
		int dag;
		d=new Date(milliDate(year,week));
		day%=7;
		dag=d.getDay();
		while (day!=dag) {
				d=new Date(milliDate(year,week)+((day-dag)*24*3600*1000));
				dag=d.getDay();
		}
		return(d);
	}

	/**
	 * Create date strings in the form yyyy-mm-dd for a given Date object
	 * <br>This format is used in several database (dbm's)
	 * @param da The date input
	 * @return A string in the form yyyy-mm-dd
	 * @see DateSupport#parsedbmdate
	 */
	public static String makedbmdate(Date da) {
		int m,d,y;
		m=da.getMonth()+1;
		d=da.getDate();
		y=da.getYear()+1900;
		return(""+y+"-"+(m<10 ? "0"+m : ""+m)+"-"+(d<10 ? "0"+d : ""+d));
	}

	/**
	 * Parse date strings in the form yyyy-mm-dd
	 *  <br>This format is used in several database (dbm's)
	 * @param wh The string representing the date in 'yyyy-mm-dd' format
	 * @return A Date object for the given date
	 * @see DateSupport#makedbmdate
	 */
	public static Date parsedbmdate(String wh) {
		Date thedate;
		int y=0,m=0,d=0;
		StringTokenizer tok=new StringTokenizer(wh,"- /");
		// The date is in the form yyyy-mm-dd
		try {
			y=Integer.parseInt(tok.nextToken())-1900;
			m=Integer.parseInt(tok.nextToken())-1;
			d=Integer.parseInt(tok.nextToken());
			thedate=new Date(y,m,d);
		} catch (Exception e) {
			thedate=new Date();
		}
		return(thedate);
	}

	/**
	 * Puts a colon between a time of RFC-1223 format
	 *
	 * @param time A string in RFC-1223 format
	 * @return A string with an extra colon 
	 */
	public static String colontime(String time) {
		if (time.length()==4) {
			return(time.substring(0,2)+":"+time.substring(2,4));
		}
		return(time);
	}

	/**
	 * Returns the number of seconds from 1-Jan-1970 to a given date
	 *
	 * @param sDate String in the form 'yyyyMMdd'
	 * @return Number of seconds from 1-Jan-1970 
	 * @see DateSupport#parsetime
	 * @see DateSupport#parsedatetime
	 */
	public static int parsedate( String sDate ){
		SimpleDateFormat df = (SimpleDateFormat)DateFormat.getDateTimeInstance();
		TimeZone tz;
		df.applyLocalizedPattern("yyyyMMdd");

		tz=TimeZone.getDefault() ;
		df.setTimeZone(tz);

		Date date = null;
		try
		{
			date = df.parse(sDate); 
		}
		catch( java.text.ParseException e )
		{
			System.out.println(e.toString());
		}

		if( date != null)
			return  (int)((date.getTime()-getMilliOffset())/1000);
		else
			return -1;
	}

	/**
	 * Returns the number of seconds from 00:00:00 to a given time 
	 *
	 * @param wh Time in the form 'hhmmss'
	 * @return Number of seconds from 00:00:00 to the given time 
	 * @see DateSupport#parsedate
	 * @see DateSupport#parsedatetime
	 */
	public static int parsetime(String wh) {
		int h=0,m=0,s=0;
		try {
			h=Integer.parseInt(wh.substring(0,2));
			m=Integer.parseInt(wh.substring(2,4));
			s=Integer.parseInt(wh.substring(4,6));
		} catch (Exception e) {
			System.out.println("DateSupport: maketime ("+wh+")");
		}
		return(s+60*(m+60*h));
	}

	/**
	 * Returns the number of seconds from 1-Jan-1970 00:00:00 to a given time
	 *
	 * @param wh Date in the form 'yyyymmddhhmmss'
	 * @return Number of seconds from 1-Jan-1970 00:00:00 to the given time
	 * @see DateSupport#parsedate
	 * @see DateSupport#parsetime
	 */
	public static int parsedatetime(String wh) {
		return(parsedate(wh.substring(0,8))+parsetime(wh.substring(8,14)));
	}


	/**
	 * Takes an integer representing the number of seconds from 1-Jan-1970 00:00:00 and returns the time as a string
	 *
	 * @param val Number of seconds from 1-Jan-1970 00:00:00
	 * @return String in the form 'hhmm' for the given time
	 * @see DateSupport#getTimeSec
	 * @see DateSupport#getTimeSecLen
	 * @see DateSupport#getMonthDay
	 * @see DateSupport#getMonth
	 * @see DateSupport#getYear
	 * @see DateSupport#getMonthInt
	 * @see DateSupport#getWeekDayInt
	 * @see DateSupport#getDayInt
	 */
	public static String  getTime(int val) {
		if (dooffset) {
			val+=offset;
		}
		Date v=new Date((long)val*1000);
		String result;
		int h=v.getHours();
		if (h<10) {
			result="0"+h;
		} else {
			result=""+h;
		}
		int m=v.getMinutes();
		if (m<10) {
			result+=":0"+m;
		} else {
			result+=":"+m;
        }
        return(result);
	}

	/**
	 * Takes an integer representing the number of seconds from 1-Jan-1970 00:00:00 and returns the time as a string
	 *
	 * @param val Number of seconds from 1-Jan-1970 00:00:00
	 * @return String in the form 'hhmmss' for the given time
	 * @see DateSupport#getTime
	 * @see DateSupport#getTimeSecLen
	 * @see DateSupport#getMonthDay
	 * @see DateSupport#getMonth
	 * @see DateSupport#getYear
	 * @see DateSupport#getMonthInt
	 * @see DateSupport#getWeekDayInt
	 * @see DateSupport#getDayInt
	 */
	public static String getTimeSec(int val) {
        Date v;
        if (val == -1) {
            // WHY? This behaviour leads to incorrect displaying of MMEvents!!
            v = new Date ();
        }
        else {
            if (dooffset) {
                val+=offset;
            }
            v = new Date((long)val*1000);
        }

        String result;
        int h=v.getHours();
        if (h<10) {
            result="0"+h;
        } else {
            result=""+h;
        }
        int m=v.getMinutes();
        if (m<10) {
            result+=":0"+m;
        } else {
            result+=":"+m;
        }
        int s=v.getSeconds();
        if (s<10) {
            result+=":0"+s;
        } else {
            result+=":"+s;
        }
        return(result);
	}


	/**
	 * Takes an integer representing the number of seconds from 00:00:00 and returns the time as a string
	 *
	 * @param val Number of seconds from 00:00:00
	 * @return String in the form 'hhmmss' for the given time
	 * @see DateSupport#getTime
	 * @see DateSupport#getTimeSec
	 * @see DateSupport#getMonthDay
	 * @see DateSupport#getMonth
	 * @see DateSupport#getYear
	 * @see DateSupport#getMonthInt
	 * @see DateSupport#getWeekDayInt
	 * @see DateSupport#getDayInt
	 */
	public static String  getTimeSecLen(int val) {
        String result;
        int h=(val/3600);
        if (h<10) {
            result="0"+h;
        } else {
            result=""+h;
        }
        val-=(h*3600);


        int m=(val/60);
        if (m<10) {
            result+=":0"+m;
        } else {
            result+=":"+m;
        }
        val-=(m*60);

        int s=val;
        if (s<10) {
            result+=":0"+s;
        } else {
            result+=":"+s;
        }
        return(result);
	}


	/**
	 * Takes an integer representing the number of seconds from 1-Jan-1970 00:00:00 and returns the day in the month 
	 * 
	 * @param val Number of seconds from 1-Jan-1970 00:00:00
	 * @return String containing the day of the month (1 to 31)
	 * @see DateSupport#getTime
	 * @see DateSupport#getTimeSec
	 * @see DateSupport#getTimeSecLen
	 * @see DateSupport#getMonth
	 * @see DateSupport#getYear
	 * @see DateSupport#getMonthInt
	 * @see DateSupport#getWeekDayInt
	 * @see DateSupport#getDayInt
	 */
	public static String getMonthDay(int val) {
        if (dooffset) {
            val+=offset;
        }
        Date v=new Date((long)val*1000);
        String result;
        int d=v.getDate();
        if (d<10) {
            result="0"+d;
        } else {
            result=""+d;
        }
        return(result);
	}

	/**
	 * Takes an integer representing the number of seconds from 1-Jan-1970 00:00:00 and returns the number of the month          
	 * 
	 * @param val Number of seconds from 1-Jan-1970 00:00:00
	 * @return String containing the number of the month (1 to 12)
	 * @see DateSupport#getTime
	 * @see DateSupport#getTimeSec
	 * @see DateSupport#getTimeSecLen
	 * @see DateSupport#getMonthDay
	 * @see DateSupport#getYear
	 * @see DateSupport#getMonthInt
	 * @see DateSupport#getWeekDayInt
	 * @see DateSupport#getDayInt
	 */
	public static String getMonth(int val) {
			if (dooffset) {
					val+=offset;
			}
			Date v=new Date((long)val*1000);
			String result;
			int m=v.getMonth();
			m++;
			if (m<10) {
					result="0"+m;
			} else {
					result=""+m;
			}
			return(result);
	}

	/**
	 * Takes an integer representing the number of seconds from 1-Jan-1970 00:00:00 and returns the year 
	 *
	 * @param val Number of seconds from 1-Jan-1970 00:00:00
	 * @return String containing the year (1900 to ....)
	 * @see DateSupport#getTime
	 * @see DateSupport#getTimeSec
	 * @see DateSupport#getTimeSecLen
	 * @see DateSupport#getMonthDay
	 * @see DateSupport#getMonth
	 * @see DateSupport#getMonthInt
	 * @see DateSupport#getWeekDayInt
	 * @see DateSupport#getDayInt
	 */
	public static String getYear(int val) {
			//System.out.println(val);
			if (dooffset) {
					val+=offset;
			}
			Date v=new Date(((long)val)*1000);
			int m=v.getYear();
			return(""+(m+1900));
	}


	/**
	 * Takes an integer representing the number of seconds from 1-Jan-1970 00:00:00 and returns the month as an integer
	 *
	 * @param val Number of seconds from 1-Jan-1970 00:00:00
	 * @return Integer containing the value of the month (1 to 12)
	 * @see DateSupport#getTime
	 * @see DateSupport#getTimeSec
	 * @see DateSupport#getTimeSecLen
	 * @see DateSupport#getMonthDay
	 * @see DateSupport#getMonth
	 * @see DateSupport#getYear
	 * @see DateSupport#getWeekDayInt
	 * @see DateSupport#getDayInt
	 */
	public static int getMonthInt(int val) {
			if (dooffset) {
					val+=offset;
			}
			Date v=new Date((long)val*1000);
			String result;
			int m=v.getMonth();
			return(m);
	}


	/**     
	 * Takes an integer representing the number of seconds from 1-Jan-1970 00:00:00 
     * and returns the number of the day in the week as an integer
	 *      
	 * @param val Number of seconds from 1-Jan-1970 00:00:00        
	 * @return Integer containing the number of the day in the week (1 to 7) 
	 * @see DateSupport#getTime
	 * @see DateSupport#getTimeSec
	 * @see DateSupport#getTimeSecLen
	 * @see DateSupport#getMonthDay
	 * @see DateSupport#getMonth
	 * @see DateSupport#getYear
	 * @see DateSupport#getMonthInt
	 * @see DateSupport#getDayInt
	 */     
	public static int getWeekDayInt(int val) {
        if (dooffset) {
            val+=offset;
        }
        Date v=new Date((long)val*1000);
        int m=v.getDay();
        return(m);
	}

	/**
	 * Takes an integer representing the number of seconds from 1-Jan-1970 00:00:00 
     * and returns the number of the day in the month as an integer
	 *     
	 * @param val Number of seconds from 1-Jan-1970 00:00:00
	 * @return Integer containing the number of the day in the month (1 to 31)
	 * @see DateSupport#getTime
	 * @see DateSupport#getTimeSec
	 * @see DateSupport#getTimeSecLen
	 * @see DateSupport#getMonthDay
	 * @see DateSupport#getMonth
	 * @see DateSupport#getYear
	 * @see DateSupport#getMonthInt
	 * @see DateSupport#getWeekDayInt
	 */
	public static int getDayInt(int val) {
        if (dooffset) {
            val+=offset;
        }
        Date v=new Date((long)val*1000);
        int m=v.getDate();
        return(m);
	}

	/**
	 * Return the time-difference between our timezone and GMT
	 *
	 * @return Integer containing the number of milliseconds representing the time-difference between us and GMT
	 */
	public static long getMilliOffset() {
        if (!dooffset) {
            // Do not worry about the code below, since it will never be called

            TimeZone tz1,tz2;
            long off=5400;
            int off1,off2;
            Date d=new Date();

            tz1=TimeZone.getDefault(); // This is MET but they think it's the Middle East
            tz2=TimeZone.getTimeZone("ECT"); //Apparently we live there ?
            off1=tz1.getRawOffset();
            off2=tz2.getRawOffset();
            if (tz1.inDaylightTime(d)) {
                if (System.getProperty("os.name").equals("Linux")) {
                    off1+=(3600*1000); // Activate before sunday morning
                } else {
                    off1+=(3600*1000);
                }
            }
            if (tz2.inDaylightTime(d)) {
                off2+=(3600*1000);
            }

            off=off1-off2;
            return(off);
        } else {
            return((long)offset*1000);
        }
	}

	/**
	 * Return the current time in milliseconds (for the current-timezone!!)
	 *
	 * @return Integer containing the number of milliseconds representing the current time
	 */
	public static long currentTimeMillis() {
        return(System.currentTimeMillis()-getMilliOffset());
	}


	/**
	* Convert a string (like "12:42:15 1/2/97") to milliseconds from 1970
	* The timezone used is 'GMT' 
	* @param date String which contains the date and time in the format "hour:minutes:sec day/month/year"
	* @return the elapsed milliseconds since 1970 from this date
	*/
	public static long convertDateToLong(String date) {
        // Next line was the old code:
        // return (convertStringToLong(date));
        System.out.println("Converting " + date);
        Calendar cal = Calendar.getInstance();
        TimeZone tz  = TimeZone.getDefault();

        cal.setTimeZone(tz);
        cal = parseDate( cal, date );

        Date d = cal.getTime ();
        long l = d.getTime ();

        return l;
	 }


	/**
	* Convert date to long with timezone-offset <br>
	* example : <br>convertDateToLongWithTimeZone ( "14:12:56 3/5/1998", 3, 30 ) <br>
	*           will convert the date to milliseconds passes from 1970 untill this date with -3:30 timezone 
	* @param date Date to be converted in format:  hour:minute:second day/month/year
	* @param hour Hour-part of the timezone-offset (int)
	* @param minutes Minutes-part of the timezone-offset (int)
	* @obsolete Do not use this code ever!
	*/
	public static long convertDateToLongWithTimeZone( String date, int hour, int minutes ) {
        return(convertStringToLongWithTimeZone( date, hour, minutes ));
	}



	/*
	* ----- private functions used by convertDateToLong --------
	*/


	/**
	 * Convert a string with a fixed (3:30) timezone offset
	 * @obsolete Do not use this method ever!!
	 */
	private static long convertStringToLong( String date ) {
        // Set timezone to local timezone (Netherlands = 3:30 difference)
        // NEVER, NEVER CALL THIS METHOD!!! IT IS OBSOLETE!!!
        System.out.println ("Warning: DateSupport::converStringToLong   Obsolete code!");
        return ( convertDateToLongWithTimeZone( date, 3, 30) );   
	}

	/**
	 * @obsolete Do not use this method ever!!
	 */
	private static long convertStringToLongWithTimeZone( String date , int hour, int minutes)       {
        // Set timezone 
        Calendar calendar = setTimeZone(hour,minutes);

        // Now convert the datestring to calendardate 
        calendar = parseDate( calendar, date);

        // calculate the milliseconds since 1970
        Date mydate = calendar.getTime();

        // return this calculation
        return(mydate.getTime());
    }

	/**
	 * @obsolete Do not use this method ever!!
	 */
	private static Calendar setTimeZone(int hours, int minutes) {
        System.out.println ("Warning: obsolete setTimeZone was used!!");

        // get the supported ids for GMT-08:00 (Pacific Standard Time)
        String[] ids = TimeZone.getAvailableIDs((hours * 60 + minutes) * 60 * 1000);

        // if no ids were returned, something is wrong. get out.
        if (ids.length == 0){
            System.out.println("Timezone is wrong...");
            System.exit(0);
        }
        System.out.println("Current Time");

        // create a Pacific Standard Time time zone
        SimpleTimeZone pdt = new SimpleTimeZone((hours * 60+minutes) * 60 * 1000, ids[0]);

        // set up rules for daylight savings time
        pdt.setStartRule(Calendar.APRIL, 1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);
        pdt.setEndRule(Calendar.OCTOBER, -1, Calendar.SUNDAY, 2 * 60 * 60 * 1000);

        // create a GregorianCalendar with the Pacific Daylight time zone
        // and the current date and time
        Calendar calendar = new GregorianCalendar(pdt);
        return (calendar);
	}

	/**
	 * Parse a string containing a date and put it in a calendar
	 * @param cal Calander object that is used for storing the parsed date
	 * @param date String in the form:  hour:minute:second day/month/year
	 * @return Calander object representing the parsed date
	 * @see DateSupport parseDateRev
	 */
	public static Calendar parseDate(Calendar cal, String date) {
        StringTokenizer tok = new StringTokenizer(date, "-\n\r:/ ");
        String token = null;

        cal.clear(Calendar.HOUR_OF_DAY);

        token = tok.nextToken();
        cal.set(Calendar.HOUR_OF_DAY, new Integer(token).intValue());
        token = tok.nextToken();
        cal.set(Calendar.MINUTE, new Integer(token).intValue());
        token = tok.nextToken();
        cal.set(Calendar.SECOND, new Integer(token).intValue());
        token = tok.nextToken();
        cal.set(Calendar.DAY_OF_MONTH, new Integer(token).intValue());
        token = tok.nextToken();
        cal.set(Calendar.MONTH, new Integer(token).intValue()-1);
        token = tok.nextToken();
        cal.set(Calendar.YEAR, new Integer(token).intValue());
        return (cal);
	}

	/**
	 * Parse a string containing a date and put it in a calendar, the string is in reversed order
	 * @param cal Calander object that is used for storing the parsed date
	 * @param date String in the form:  year/month/day hour:minute:second
	 * @return Calander object representing the parsed date
	 * @see DateSupport parseDate   
	 */
	public static Calendar parseDateRev(Calendar cal, String date) {
        StringTokenizer tok = new StringTokenizer(date, "-\n\r:/ ");
        String token = null;

        cal.clear(Calendar.HOUR_OF_DAY);

        token = tok.nextToken();
        cal.set(Calendar.YEAR,         new Integer(token).intValue());
        token = tok.nextToken();
        cal.set(Calendar.MONTH,        new Integer(token).intValue()-1);
        token = tok.nextToken();
        cal.set(Calendar.DAY_OF_MONTH, new Integer(token).intValue());
        token = tok.nextToken();
        cal.set(Calendar.HOUR_OF_DAY,         new Integer(token).intValue());
        token = tok.nextToken();
        cal.set(Calendar.MINUTE,      new Integer(token).intValue());
        token = tok.nextToken();
        cal.set(Calendar.SECOND,      new Integer(token).intValue());
        return (cal);
	}

	/**
	 * Return a string for a given date
	 * @param time Integer representing the time in seconds since 1-Jan-1970 00:00:00
	 * @return String in the form 'hhmmss day/month/year'
	 * @see DateSupport#date2day
	 * @see DateSupport#date2date
	 */
	public static String date2string (int time) {
        return(getTimeSec(time)+" "+getMonthDay(time)+"/"+getMonth(time)+"/"+getYear(time));                            
	}

	/**
	 * Return a string for a given date
	 * @param time Integer representing the time in seconds since 1-Jan-1970 00:00:00
	 * @return String in the form 'year-month-day'
	 * @see DateSupport#date2string
	 * @see DateSupport#date2date
	 */
	public static String date2day(int time) {
        return(getYear(time)+"-"+getMonth(time)+"-"+getMonthDay(time));
	}

	/**
	 * Return a string for a given date
	 * @param time Integer representing the time in seconds since 1-Jan-1970 00:00:00
	 * @return String in the form 'year-month-day hhmmss'
	 * @see DateSupport#date2string
	 * @see DateSupport#date2day
	 */
	public static String date2date(int time) {
        return(getYear(time)+"-"+getMonth(time)+"-"+getMonthDay(time)+" "+getTimeSec(time));
	}

	/**
	 * Dump a date as string
	 * @param time Integer representing the time in seconds since 1-Jan-1970 00:00:00
	 * @return String with a date
	 */
	private static String dumpdate(int d) {
        Date dd=new Date((long)d*1000);
        StringBuffer b=new StringBuffer();

        b.append(" Year "+dd.getYear());
        b.append(" Month "+(dd.getMonth()+1));
        b.append(" Day "+dd.getDate());
        b.append(" Weekday "+dd.getDay());
        b.append(" Hours "+dd.getHours());
        b.append(" Minutes "+dd.getMinutes());
        b.append(" Seconds "+dd.getSeconds());
        b.append(" Time "+dd.getTime());
        return(b.toString());
	}

	/**
	 * Main method used for testing purposes
	 * @param args[] Array of arguments
	 */
	public static void main(String args[]) {
        System.out.println("Date (without corr)"+date2string((int)(System.currentTimeMillis()/1000))+
                           " "+System.currentTimeMillis()/1000);
        System.out.println("Date (with corr)"+date2string((int)(DateSupport.currentTimeMillis()/1000))+
                           " : "+DateSupport.currentTimeMillis()/1000);
        System.out.println("Date "+args[0]+" "+date2string(Integer.parseInt(args[0])));
        System.out.println("Date "+args[0]+" "+dumpdate(Integer.parseInt(args[0])));
        String ID = System.getProperty("user.timezone", "GMT");
        System.out.println("ID "+ID+" : "+getMilliOffset());
        System.out.println("ParseDate "+parsedate(args[1]));
	}
}






