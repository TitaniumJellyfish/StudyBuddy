package com.titaniumjellyfish.studybuddy;

public class TimeUtils {

	public static final long MINUTE = 1000*60;
	public static final long HOUR = MINUTE*60;
	public static final long DAY = HOUR*24;
	public static final long WEEK = DAY*7;
	public static final long MONTH30 = DAY*30;
	public static final long MONTH31 = DAY*31;
	public static final long MONTH28 = DAY*28;
	
	public static long floorToNearest(long time, long interval){
		return (time / interval) * interval;
	}
	
	public static long floorToHour(long time){
		return floorToNearest(time, HOUR);
	}
	
}
