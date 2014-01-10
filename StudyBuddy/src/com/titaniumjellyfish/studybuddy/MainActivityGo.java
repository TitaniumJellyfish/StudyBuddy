package com.titaniumjellyfish.studybuddy;

import com.titaniumjellyfish.studybuddy.clientside.UpDownService;
import com.titaniumjellyfish.studybuddy.database.Globals;
import android.os.Bundle;
import android.os.IBinder;
import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * Start screen
 * Spinner to select search parameter, button goes to BrowseRoomActivity
 * 
 * @author Wesley
 *
 */
public class MainActivityGo extends Activity {
	private UpDownService syncService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main_go);


		//Start the service to sync with the server
		//		startService(new Intent(this, UpDownService.class));
	}

	/**
	 * 
	 */
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			// This is called when the Home (Up) button is pressed
			// in the Action Bar.
			finish();
			return true;
		case R.id.settings_room_def:
			Intent intent = new Intent(this, RoomDef.class);


			Bundle translateBundle =
				ActivityOptions.makeCustomAnimation(MainActivityGo.this,
						R.anim.slide_in_left, R.anim.slide_out_left).toBundle();

			startActivity(intent, translateBundle);
			return true;
		case R.id.settings_force_sync:
			doSync();
			return true;	        	
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Launches room BrowseRoomActivity with filter results
	 * @param v
	 */
	public void onGoClicked(View v) {
		Spinner filterSpinner = (Spinner) findViewById(R.id.spinnerResultsFilter);
		int filter = filterSpinner.getSelectedItemPosition();
		//
		//		SharedPreferences.Editor dataEditor = getPreferences(MODE_PRIVATE).edit();
		//		dataEditor.putInt(Globals.KEY_FILTER, filter);
		//		dataEditor.commit();

		Bundle translateBundle =
			ActivityOptions.makeCustomAnimation(MainActivityGo.this,
					R.anim.slide_in_left, R.anim.slide_out_left).toBundle();

		Intent searchLaunch = new Intent(this, BrowseRoomActivity.class);
		searchLaunch.putExtra(Globals.KEY_FILTER, filter);
		startActivity(searchLaunch, translateBundle);
	}

	public void doSync() {
		if(syncService != null){
			Toast.makeText(this, "Sync in progres..", Toast.LENGTH_SHORT).show();
		} else{
			Intent target = new Intent(this, UpDownService.class);
			startService(target);
			bindService(target, new SyncConnection(), BIND_NOT_FOREGROUND | BIND_AUTO_CREATE);
		}
	}

	private class SyncConnection implements ServiceConnection{

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			syncService = ((UpDownService.ServiceBinder) service).getService();
			syncService.setOnFinishListener(new UpDownService.OnFinishListener(){
				@Override
				public void onFinish() {
					syncService = null;
				}
			});
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {}

	}
}
