package com.titaniumjellyfish.studybuddy;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.output.NullOutputStream;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.Time;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.titaniumjellyfish.studybuddy.SensorCollectionState.Current;
import com.titaniumjellyfish.studybuddy.database.Globals;
import com.titaniumjellyfish.studybuddy.database.NoiseEntry;
import com.titaniumjellyfish.studybuddy.database.RoomLocationEntry;
import com.titaniumjellyfish.studybuddy.database.TitaniumDb;

/**
 * TODO Bluetooth implementation
 * Collect snapshots of current # of devices
 * DB agglomerate data keeps total # of devices for a given study space
 * Calculate % (current devices/DB total) to judge crowdedness
 * If current devices > DB total, update DB total
 * 
 * When a study space is added, take initial snapshot of bluetooth devices
 * Ask user how crowded the space is at that time through survey
 * ex: Full (100%), 1/2 capacity (50%), Empty (0%)
 * Use that to "extrapolate" total capacity
 * ex: if user picks 1/2 capacity on survey, multiply snapshot by 2 for the
 * total to upload to agglomerate data
 * 
 * thoughts? - Wesley
 *
 */

public class SensorService extends Service implements com.google.android.gms.location.LocationListener,  
	GooglePlayServicesClient.ConnectionCallbacks,
	GooglePlayServicesClient.OnConnectionFailedListener {
	
	SensorService mContext;
	SensorCollectionState sensorCollectionState; 

	//================Sound Variables==================

	//update sound every thirty seconds
	private static final int RECORDRATE = 30;

	TitaniumDb db;

	public static final int SAMPLE_RATE = 44100;

	private AudioRecord mRecorder;
	private File mRecording;
	private short[] mBuffer;
	private Thread audioThread;

	private int SILENTTHRESH = 300;
	private int QUIETTHRESH = 600;
	private int MEDIUMTHRESH = 1200;
	private int LOUDTHRESH = 1800;

	public static final NullOutputStream NULL_OUTPUT_STREAM = new NullOutputStream();

	private  boolean isRecording = false;

	public double amplitude = 0.0;
	int mVolume = -1;
	public String TAG = "TAG";	

	//===============Location Variables=================

	private LocationRequest mLocationRequest;
	private LocationClient mLocationClient;
	private Intent mGeofenceTransitionBroadcast;
	private Intent mGoogleConnectionErrorBroadcast;
	
	public double latitude;
	public double longitude;

	private Location targetLocation;
	private String ID = "targetID";

	private double targetLat = 43.706725;
	private double targetLong = -72.287196;
	
	private RoomLocationEntry targetRoom;

	private final IBinder binder = new SensorBinder();

	//===============Binder ??==========================
	public class SensorBinder extends Binder {
		SensorService getService() {
			return SensorService.this;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mContext = this;
		sensorCollectionState = new SensorCollectionState();
		mLocationClient = new LocationClient(this, this, this);
	}
	private TitaniumDb getHelper(){
		if (db == null)
			db = OpenHelperManager.getHelper(this, TitaniumDb.class);
		return db;
	}

	@Override 
	public int onStartCommand(Intent intent, int flags, int startId) {

		//Parse out information about target location
		Bundle extras = intent.getExtras();
		String targetRoomName = "", targetBuildingName = "";
		if (extras != null) {
			targetLat = extras.getDouble(Globals.KEY_SENSOR_LATITUDE);
			targetLong = extras.getDouble(Globals.KEY_SENSOR_LONGITUDE);
			targetRoomName = extras.getString(Globals.KEY_SENSOR_ROOM);
			targetBuildingName = extras.getString(Globals.KEY_SENSOR_BUILDING);

			TitaniumDb db_helper = getHelper();

			try {
				Dao<RoomLocationEntry, Long> room_dao = db_helper.dao(RoomLocationEntry.class, Long.class);
				Map<String, Object> property_map = new HashMap<String, Object>();
				property_map.put(RoomLocationEntry.COLUMN_NAME_BUILDING_NAME, targetBuildingName);
				property_map.put(RoomLocationEntry.COLUMN_NAME_ROOM_NAME, targetRoomName);
				List<RoomLocationEntry> q_result = room_dao.queryForFieldValues(property_map);
				if(q_result.size() > 0){
					targetRoom = q_result.get(0);
					targetRoom.setLocal(true);
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		if(targetRoom == null){
			Log.e(Globals.LOGTAG, "SENSOR SERVICE CANNOT FIND ROOM '"+targetRoomName+"@"+targetBuildingName+"'; stopping.");
			stopSelf();
		} else{
			targetLocation = new Location("TARGET");
			targetLocation.setLatitude(targetLat);
			targetLocation.setLongitude(targetLong);

			//============Location Stuff==================
			//Get the location client
			mLocationRequest = LocationRequest.create();
			mLocationRequest.setInterval(Globals.UPDATE_INTERVAL_MILLISECONDS);
			mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
			mLocationClient.connect();

			//request location updates set it to return every 5 minutes

			//=============Sound stuff====================

			//==================Notification==================
			//Create the notification 
			Intent i = new Intent(mContext, com.titaniumjellyfish.studybuddy.DisplayRoomActivity.class); //notification reopens main
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP); 
			PendingIntent pIntent = PendingIntent.getActivity(mContext, 0, i, 0);

			//Build the notification
			Notification notification = new Notification.Builder(this)
			.setContentTitle(getString(R.string.ui_main_notification_title))
			.setContentText(getString(R.string.ui_main_notification_description))
			.setSmallIcon(R.drawable.tijelly)
			.setContentIntent(pIntent).setAutoCancel(true).build();

			//keep notification persistent
			notification.flags = notification.flags | Notification.FLAG_ONGOING_EVENT;

			//If the user clicks on the notification, remove it
			notification.flags |= Notification.FLAG_AUTO_CANCEL;

			int id = 1234;
			startForeground(id, notification);
		}
		return START_STICKY;
	}

	public void onDestroy() {

		sensorCollectionState.setCurrent(Current.BAKERY);
		if(targetRoom != null){
			try {
				Dao<RoomLocationEntry, Long> roomDao = getHelper().dao(RoomLocationEntry.class, Long.class);
				roomDao.update(targetRoom);
				Log.i(Globals.LOGTAG, "Updated room in database!");
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}

		if (mLocationClient.isConnected()) {
			mLocationClient.disconnect(); //Only disconnect if it was connected
		}
		
		if (mRecorder != null && mRecorder.getState() == AudioRecord.STATE_INITIALIZED) { 
			mRecorder.stop();
			mRecorder.release();
		}
		
		stopForeground(true);
		((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();
		if (db != null){
			OpenHelperManager.releaseHelper();
		}
		super.onDestroy();
	}

	//-------sound helpers ----------

	private void startRecording(){
		initRecorder();

		mRecorder.startRecording();
		mRecording = getFile("raw");
		sensorCollectionState.setAudioStarted(true);
		
		audioThread = startBufferedWrite(mRecording);
	}


	private Thread startBufferedWrite(final File file) {
		Log.i(TAG, "WRITING");
		Thread th = new Thread(
				new Runnable() {
					@Override
					public void run() {
						DataOutputStream output = null;
						Log.i(TAG, "running");
						try {
							output = new DataOutputStream(NULL_OUTPUT_STREAM);
							//Log.i(TAG, "outputset");
							while(sensorCollectionState.getState() == Current.JELLY){
								// Loop as long as we're in the jelly, saving to DB every RECORDRATE seconds
								long tstart = System.currentTimeMillis();
								//creates array mReads storing [0] sum of amplitudes, and [1] number of recordings
								double[] mReads = new double[2];
								double smallamp = 0;
								while (System.currentTimeMillis() - tstart < RECORDRATE * 1000) {

									double sum = 0;
									//reads from microphone
									int readSize = mRecorder.read(mBuffer, 0, mBuffer.length);
									//writes to the buffer and pulls out the amplitude
									for (int i = 0; i < readSize; i++) {
										output.writeShort(mBuffer[i]);
										sum += mBuffer[i] * mBuffer[i];
									}
									if (readSize > 0) {
										/*						Log.i(TAG, "readSize= "+readSize);
						Log.i(TAG, "setting progress");*/
										//gets average of microphone reading
										smallamp = sum / readSize;
										//adds sum to total
										mReads[0] += smallamp;
										//pings number of entries
										mReads[1] += 1;
										/*						Log.i(TAG, "amplitude= " + smallamp);
						Log.i(TAG, "sqrt= " + Math.sqrt(smallamp));*/
									}
									else {
										//Log.i(TAG, "readsize <= 0");

									}
									//wait RECORDRATE seconds before measuring again
									try {
										Thread.sleep(10);
									} catch (InterruptedException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
								// while loop has ended; process data
								if (mReads[1] != 0){
									amplitude = mReads[0]/mReads[1];
									Log.i(TAG, "final amplitude= " + amplitude);
									Log.i(TAG, "final sqrt= " + Math.sqrt(amplitude));
									Log.i(TAG, "readnumber= " + mReads[1]);
									processAudioData(); //process the data and stuff
								}
								else {
									Log.i(TAG, "readsize <= 0");
								}
							}

						} catch (IOException e) {
							Log.e(TAG, e.getMessage());
						} finally {
							if (output != null) {
								try {
									output.flush();
								} catch (IOException e) {
									Log.e(TAG, e.getMessage());

								} finally {
									try {
										output.close();
									} catch (IOException e) {
										Log.e(TAG, e.getMessage());
									}
								}
							}
						}
					}
				});
		th.setDaemon(true);
		th.start();
		return th;
	}




	private void initRecorder() {
		int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT);
		mBuffer = new short[bufferSize];
		mRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
				AudioFormat.ENCODING_PCM_16BIT, bufferSize);
	}

	private File getFile(final String suffix) {
		Time time = new Time();
		time.setToNow();
		return new File(Environment.getExternalStorageDirectory(), 	time.format("%Y%m%d%H%M%S") + "." + suffix);
	}


	@Override
	public void onLocationChanged(Location location) {
		latitude = location.getLatitude();
		longitude = location.getLongitude();


		//Check the donut range
		if (withinTargetRange(latitude, longitude, Globals.RADIUS_RECORD)) {
			
			sensorCollectionState.setHasEnteredDonut(true);
			
			if (withinTargetRange(latitude, longitude, Globals.RADIUS_STORE_DATA)) {
				sensorCollectionState.setHasEnteredJelly(true);
				sensorCollectionState.setCurrent(Current.JELLY);
			}
			else {
				sensorCollectionState.setCurrent(Current.DONUT);
			}
		}
		else { //Outside of the donut
			sensorCollectionState.setCurrent(Current.BAKERY);
			if (sensorCollectionState.getEnteredDonut()) {
				this.stopSelf();
			}
			else if (System.currentTimeMillis() - sensorCollectionState.getStartTime() > 1800000){ //if 30 mintues has passed since starting the service, stop
				this.stopSelf();
			}
		}

		if (sensorCollectionState.getState() == Current.JELLY && !sensorCollectionState.getAudioStarted()) {
			//Start thread for audio;
			startRecording();

		}

		if (sensorCollectionState.getState() == Current.DONUT && sensorCollectionState.getEnteredJelly()) {
			if(audioThread != null){
				try {
					audioThread.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				audioThread = null;
			}
			sensorCollectionState.setAudioStarted(false); //allows re-entry into jelly zone
			if (mRecorder.getState() == AudioRecord.STATE_INITIALIZED) { 
				mRecorder.stop();
				mRecorder.release();
			}

		}

		//Let the activity know what's happening to update the ui
		mGeofenceTransitionBroadcast = new Intent();
		mGeofenceTransitionBroadcast.setAction(Globals.ACTION_GEOFENCE_TRANSITION);
		sendBroadcast(mGeofenceTransitionBroadcast);
	}

	/**
	 * Inserts to database
	 * Note: May be better to call this after a certain time (ie 30 or 60 sec)
	 * to make sure BluetoothReceiver has collected all the connections
	 * 
	 * TODO does database have a value stored for max connections?
	 * TODO test optimal time to wait
	 */



	private void processAudioData() {
		//Insert things

		//		//give the while loop in recording time to catch up
		//		try {synchronized (this){
		//			wait(3000);
		//		}} catch (InterruptedException e) {
		//			e.printStackTrace();
		//		}
		//========= Process all the data from the sound buffer=====

		Log.i(TAG, "final sqrtdb= " + Math.sqrt(amplitude));

		if (Math.sqrt(amplitude) > SILENTTHRESH)
			mVolume = 0;
		if (Math.sqrt(amplitude) > QUIETTHRESH)
			mVolume = 1;
		if (Math.sqrt(amplitude) > MEDIUMTHRESH)
			mVolume = 2;
		if (Math.sqrt(amplitude) > LOUDTHRESH)
			mVolume = 3;
		if (Math.sqrt(amplitude) == 0)
			mVolume = -1;



		//====== Insert to database =======
		NoiseEntry ne = new NoiseEntry();
		ne.setParent(targetRoom);
		ne.setLocal(true);
		ne.setNoise(mVolume);
		ne.setTime(System.currentTimeMillis());


	}

	public void onProviderDisabled(String provider) {}
	public void onProviderEnabled(String provider) {}
	public void onStatusChanged(String provider, int status, Bundle extras) {}

	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		// If we cannot connect, send a broadcast to display the error dialog
		mGoogleConnectionErrorBroadcast = new Intent();
		Bundle extras = new Bundle();
		extras.putInt(Globals.CONNECTION_ERROR_CODE, connectionResult.getErrorCode());
		mGoogleConnectionErrorBroadcast.setAction(Globals.ACTION_GOOGLE_PLAY_ERROR);


	}

	@Override
	public void onConnected(Bundle connectionHint) {
		mLocationClient.requestLocationUpdates(mLocationRequest, this);

	}

	@Override
	public void onDisconnected() {
		mLocationClient.removeLocationUpdates(this);
	}
	
	public SensorCollectionState getCollectionStateObject() {
		return sensorCollectionState;
	}

	private boolean withinTargetRange(double lat, double longitude, int radius) {

		Location currLocation = new Location("CURRENT_LOC");
		currLocation.setLatitude(lat);
		currLocation.setLongitude(longitude);

		double currRadius = targetLocation.distanceTo(currLocation);

		if (currRadius < radius) {

			return true;
		}
		else {
			return false;
		}
	}

}