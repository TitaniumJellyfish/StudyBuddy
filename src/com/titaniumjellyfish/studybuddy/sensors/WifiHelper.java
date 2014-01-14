package com.titaniumjellyfish.studybuddy.sensors;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

// thanks to this stackoverflow thread: http://stackoverflow.com/questions/7975473/detect-wifi-ip-address-on-android

public class WifiHelper {
	
	private WifiManager manager;
	
	public WifiHelper(Context context){
		manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
	}
	
	public WifiManager getWifiManager(){
		return manager;
	}
	
	public String getIpAddress(){
		WifiInfo info = manager.getConnectionInfo();
		int ip = info.getIpAddress();
		return WifiHelper.ipInt2String(ip);
	}
	
	public static String ipInt2String(int ip){
		String fmt = "%d.%d.%d.%d";
		return String.format(fmt,
				ip & 0xFF,
				(ip >> 8) & 0xFF,
				(ip >> 16) & 0xFF,
				(ip >> 24) & 0xFF);
	}
	
	public static int ipString2Int(String ip_str){
		String[] parts = ip_str.split("[.]");
		int ip = 0;
		for(int i=0; i<parts.length; i++){
			ip = ip << 8;
			ip += Integer.parseInt(parts[i]);
		}
		return ip;
	}
}
