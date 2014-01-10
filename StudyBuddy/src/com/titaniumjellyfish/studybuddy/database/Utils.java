package com.titaniumjellyfish.studybuddy.database;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

public class Utils {
	
	public static byte[] fromTrendToByteArray(long[] time, double[] noise) {
		
		//Store time and noise/crowd as longs for simplicity
		long[] trendArray = new long[time.length * 2];
		
		int j = 0;
		for (int i = 0; i < trendArray.length; i = i + 2) {
			trendArray[i] = (long)time[j];
			trendArray[i + 1] = (long)noise[j];
			j++;
		}
		
		ByteBuffer byteBuffer = ByteBuffer.allocate(trendArray.length * Long.SIZE);
		LongBuffer longBuffer = byteBuffer.asLongBuffer();
		longBuffer.put(trendArray);
		
		return byteBuffer.array();
	}
	
	public static byte[] fromTrendToByteArray(long[] time, int[] crowd) {
		
		long[] trendArray = new long[time.length * 2];
		
		int j = 0;
		for (int i = 0; i < trendArray.length; i = i + 2) {
			trendArray[i] = (long)time[j];
			trendArray[i + 1] = (long)crowd[j];
			j++;
		}
		
		ByteBuffer byteBuffer = ByteBuffer.allocate(trendArray.length * Long.SIZE);
		LongBuffer longBuffer = byteBuffer.asLongBuffer();
		longBuffer.put(trendArray);
		
		return byteBuffer.array();
	}
	
	public static String fromStringArraytoString(String[] comments) {
		String allComments = "";
		for (int i = 0; i < comments.length; i++) {
			allComments += comments[i] + "| ";
		}
		return allComments;
	}
	
	public static String[] fromStringtoStringArray(String allComments) {
		String[] array = allComments.split("|");
		return array;
	}
	
	
	public static long[] fromByteArrayToTimeArray(byte[] bytePointArray) {
		
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytePointArray);
		LongBuffer longBuffer = byteBuffer.asLongBuffer();
		
		long[] longArray = new long[bytePointArray.length / Long.SIZE]; 
		longBuffer.get(longArray);
		
		long [] time = new long[longArray.length/2];
		assert(time != null && time.length <= Globals.TREND_FREQUENCY);
		
		for (int i = 0; i < time.length; i++) {
			time[i] = (long)longArray[i*2];
		}
		return time;
		
	}
	
	public static double[] fromByteArrayToNoiseArray(byte[] bytePointArray) {
		
		ByteBuffer byteBuffer = ByteBuffer.wrap(bytePointArray);
		LongBuffer longBuffer = byteBuffer.asLongBuffer();
		
		long[] longArray = new long[bytePointArray.length / Long.SIZE];
		longBuffer.get(longArray);
		
		double [] noise = new double[longArray.length/2];
		assert(noise != null && noise.length <= Globals.TREND_FREQUENCY);
		
		for (int i = 0; i < noise.length; i++) {
			noise[i] = (double)longArray[i*2 + 1]; 
		}
		return noise;
	}
	
	
	
}