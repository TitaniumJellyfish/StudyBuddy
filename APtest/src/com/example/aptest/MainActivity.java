package com.example.aptest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener{

	private final static String TAG = "APtest";	
	WifiManager wifi;
	BroadcastReceiver receiver;
	
	Button buttonScan; 
	ListView listView;
	TextView textView;
	
	SimpleAdapter adapter;
	ArrayList<HashMap<String, String>> arraylist = new ArrayList<HashMap<String, String>>();
	List<ScanResult> results;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		buttonScan = (Button) findViewById(R.id.buttonscan);
		buttonScan.setOnClickListener(this);
		listView = (ListView) findViewById(R.id.listview);
		textView = (TextView) findViewById(R.id.scanSummary);
		
		wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		
		if (receiver == null) {
			receiver = new WifiScanReceiver(this);
		}
		
		registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		


	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.buttonscan) {
			wifi.startScan();
		}
	}
	
	@Override 
	public void onPause() {
		super.onPause();
		if (receiver != null) {
			unregisterReceiver(receiver);
		}
	}
	
	@Override 
	public void onResume() {
		super.onResume();
		if (receiver == null) {
			registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
		}
	}

	@Override 
	public void onDestroy() {
		super.onDestroy();
		if (receiver != null) {
			unregisterReceiver(receiver);
		}
	}
	
	private class WifiScanReceiver extends BroadcastReceiver {
		MainActivity main;

		public WifiScanReceiver(MainActivity main) {
			this.main = main;
		}
		
		@Override
		public void onReceive(Context context, Intent intent) {
			//Get the results from the scan
			List<ScanResult> results = main.wifi.getScanResults();
			String[] display = new String[results.size()];
			//print results to logcat
			Log.d(TAG, "Total number of AP's: " + results.size());
			int strongSignal = 0;
			int i = 0;
			while (i < results.size()) {
				Log.d(TAG, "AP name: " + results.get(i).BSSID + " signal strength = " + results.get(i).frequency);
				if (results.get(i).frequency > 3000) {
					strongSignal++;
				}
				display[i] = "Name: " + results.get(i).BSSID + "\nsignal strength = " + results.get(i).frequency;
				i++;
			}
			//Display results into listView
			main.textView.setText("Total number of AP's: " + results.size() + ". Number of Strong signals: " + strongSignal);
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(main, android.R.layout.simple_list_item_1, display);
			main.listView.setAdapter(adapter);
		}
		
		
	}
}
