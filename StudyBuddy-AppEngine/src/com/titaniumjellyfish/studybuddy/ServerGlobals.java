package com.titaniumjellyfish.studybuddy;

public class ServerGlobals {
	
	// Millisecond constants
	public static final long MINUTE = 1000*60;
	public static final long HOUR = MINUTE*60;
	public static final long DAY = HOUR*24;
	public static final long WEEK = DAY*7;
	public static final long MONTH30 = DAY*30;
	public static final long MONTH31 = DAY*31;
	public static final long MONTH28 = DAY*28;
	
	// Control how averaging is done
	public static final int K_NEAREST = 3; // number of nearby noisemap entries to consider for averaging
	
	public static final String PARAM_UPLOAD_TYPE = "upload_type";
	public static final String UPLOAD_TYPE_NOISE = "ut_noise";
	public static final String UPLOAD_TYPE_ROOM = "ut_room";
	
	public static final String PARAM_JSON_DATA = "json_data";
	public static final String PARAM_DEVICE_ID = "dev_id";
	
}
