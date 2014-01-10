package com.titaniumjellyfish.studybuddy;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import android.app.ActivityOptions;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.series.XYSeries;
import com.androidplot.ui.AnchorPosition;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XLayoutStyle;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.YLayoutStyle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.titaniumjellyfish.studybuddy.SensorCollectionState.Current;
import com.titaniumjellyfish.studybuddy.SensorService.SensorBinder;
import com.titaniumjellyfish.studybuddy.database.Globals;
import com.titaniumjellyfish.studybuddy.database.NoiseEntry;
import com.titaniumjellyfish.studybuddy.database.RoomLocationEntry;
import com.titaniumjellyfish.studybuddy.database.TitaniumDb;

/**
 * Displays data for a single room entry
 * @author Wesley
 *
 */
public class DisplayRoomActivity extends OrmLiteBaseActivity<TitaniumDb> {
	private static final LatLng BAKER_BERRY = new LatLng(43.70546, -72.28884);
	private static final int MINI_MAP_ZOOM = 17;
	
	public boolean mIsBound;
	public SensorService mSensorService;
	private IntentFilter mServiceReceiverFilter;
	private SensorCollectionState sensorCollectionState;
	
	RoomLocationEntry mRoom;
	GoogleMap mMap;
	TextView noiseLevel;
	ImageView noiseJelly;
	TextView crowdLevel;
	ImageView crowdJelly;
	RatingBar roomRating;
	Context mContext;
	
	Number[] noiseValues;
	Number[] timeValues;
	
	private BroadcastReceiver mTargetRoomReceiver = new BroadcastReceiver() {
		
		private boolean enteredJelly = false;
		private boolean enteredDonut = false;
		private boolean takenSurvey = false;
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			System.out.println(intent.getAction());
			
			if (intent.getAction()!= null && intent.getAction() == Globals.ACTION_GEOFENCE_TRANSITION && sensorCollectionState !=null) { 
				
				if (sensorCollectionState.getEnteredJelly()) {
					enteredJelly = true;
				}
				if (sensorCollectionState.getTakenSurvey()) {
					takenSurvey = true;
				}
				if (sensorCollectionState.getEnteredDonut()) {
					enteredDonut = true;
				}
			}
			
			if (intent.getAction()!= null && intent.getAction() == Intent.ACTION_USER_PRESENT && 
					!takenSurvey && enteredJelly) {

				//---pew pew fire intent to start new Survey
				Intent surveyIntent = new Intent(mContext, SurveyActivity.class);
				Bundle extras = new Bundle();
				extras.putLong(Globals.SURVEY_ROOM_ID, mRoom.getId());
				surveyIntent.putExtras(extras);
				sensorCollectionState.setTakenSurvey(true);
				startActivity(surveyIntent);
			}
			
			if (intent.getAction() != null && intent.getAction() == Globals.ACTION_GOOGLE_PLAY_ERROR) {
				Bundle extras = intent.getExtras();
				int errorCode = extras.getInt(Globals.CONNECTION_ERROR_CODE);
				showErrorDialog(errorCode);
			}
			
		}		
	};
	
	
	/**
	 * 
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_display_room);
		
		mContext = this;
		
		Button back = (Button) findViewById(R.id.display_room_back_button);
		back.getBackground().setColorFilter(Color.parseColor("#33B5E5"), PorterDuff.Mode.MULTIPLY);
		
		//Get map and disable interactions
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		mMap.getUiSettings().setZoomControlsEnabled(false);
		mMap.getUiSettings().setAllGesturesEnabled(false);		
		
		//Possibility: Live updating noise bar when inside room
		noiseLevel = (TextView) findViewById(R.id.displayRoomNoiseLabel);
		crowdLevel = (TextView) findViewById(R.id.displayRoomCrowdLabel);
		roomRating = (RatingBar) findViewById(R.id.displayRoomRatingbar);

	  noiseJelly = (ImageView) findViewById(R.id.display_room_noisejelly);
	  crowdJelly = (ImageView) findViewById(R.id.display_room_crowdjelly);
	  
	  
		Bundle extras = getIntent().getExtras();
		long id = extras.getLong(Globals.KEY_ROOMLOC_ID);
		
		loadContent(id);
		
		plotData();
		
		//Register receiver
		mServiceReceiverFilter = new IntentFilter();
		mServiceReceiverFilter.addAction(Globals.ACTION_GEOFENCE_TRANSITION);
		mServiceReceiverFilter.addAction(Intent.ACTION_USER_PRESENT);
		registerReceiver(mTargetRoomReceiver, mServiceReceiverFilter);
		
		//Start service
		doBindService();
		Intent mIntent = new Intent(this, SensorService.class);
		
		//Fill bundle to pass to sensor service
		extras.putDouble(Globals.KEY_SENSOR_LATITUDE, mRoom.getLatitude());
		extras.putDouble(Globals.KEY_SENSOR_LONGITUDE, mRoom.getLongitude());
		extras.putString(Globals.KEY_SENSOR_ROOM, mRoom.getRoomName());
		extras.putString(Globals.KEY_SENSOR_BUILDING, mRoom.getBuildingName());
		
		//Put the extras into the bundle for the service to know the target
		mIntent.putExtras(extras);
		checkGoogleConnection();
		this.startService(mIntent);
		
	}
	
	@Override
	protected void onResume() {
		doBindService();
		registerReceiver(mTargetRoomReceiver, mServiceReceiverFilter);
		super.onResume();
	}
	
	@Override 
	protected void onPause() {
		unregisterReceiver(mTargetRoomReceiver);
		super.onPause();
	}
	
	@Override
	protected void onDestroy() {
		if(mSensorService != null) {
			mSensorService.stopForeground(true);
			doUnbindService();
			stopService(new Intent(this, SensorService.class));
		}
		super.onDestroy();
	}
	
	/**
	 * Gets the room data and sets views
	 * 
	 * 
	 * @param room id
	 */
	private void loadContent(long id) {
		
		if (id == 0L){
			throw new RuntimeException("ID not recieved");
		}
		
		try {
			Dao<RoomLocationEntry, Long> roomDao = getHelper().dao(RoomLocationEntry.class, Long.class);
			mRoom = roomDao.queryForId(id);
			
			if (mRoom == null)
				throw new RuntimeException("RoomLocationEntry w/ ID not found " + id);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		//Set Page Title
		String title = mRoom.getBuildingName() + " " + mRoom.getRoomName();
		this.setTitle(title);
		
		//Set up minimap
		LatLng mLoc = new LatLng(mRoom.getLatitude(), mRoom.getLongitude());
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mLoc, MINI_MAP_ZOOM));
		
		
		mMap.addMarker(new MarkerOptions()
		.position(mLoc)
		.title(title));

		double noiseYDay = 0;
		double noiseWeek = 0;
		
		int noise = (int) ((noiseYDay + noiseWeek)/2 + 0.5);
		
		switch(noise) {
		case 0:
			noiseLevel.setText("Silent");
			noiseJelly.setImageResource(R.drawable.jelly_green);
			break;
		case 1:
			noiseLevel.setText("White noise");
			noiseJelly.setImageResource(R.drawable.jelly_yellow);
			break;
		case 2:
			noiseLevel.setText("Loud");
			noiseJelly.setImageResource(R.drawable.jelly_orange);
			break;
		case 3:
			noiseLevel.setText("Distracting");
			noiseJelly.setImageResource(R.drawable.jelly_red);
			break;
		}
		
		noiseLevel.setText("Noisy");
		noiseJelly.setImageResource(R.drawable.jelly_red);
		
		
		//noiseLevel.setBackgroundColor(Color.RED);
		
	  int crowd = (int) (mRoom.getCrowd() + 0.5);
	  switch(crowd) {
		case 0:
			crowdLevel.setText("Empty");
			crowdJelly.setImageResource(R.drawable.jelly_green);
			break;
		case 1:
			crowdLevel.setText("Signs of life");
			crowdJelly.setImageResource(R.drawable.jelly_yellow);
			break;	
		case 2:
			crowdLevel.setText("Crowded");
			crowdJelly.setImageResource(R.drawable.jelly_orange);
			break;
		case 3:
			crowdLevel.setText("Full");
			crowdJelly.setImageResource(R.drawable.jelly_red);
			break;
		}
		
		roomRating.setEnabled(false);
		roomRating.setRating((float)4.5);
	}
	
	/**
	 * Fills the noise and crowd forecast bars
	 * TODO Get values from database
	 */
	private void getForecast() {
		List<NoiseEntry> a;
		try {
			Dao<NoiseEntry, Long> noiseDao = getHelper().dao(NoiseEntry.class, Long.class);
			a = noiseDao.queryBuilder().where().eq(NoiseEntry.COLUMN_NAME_PARENT_ID, mRoom.getId()).query();
			NoiseEntry[] myNoises = a.toArray(new NoiseEntry[a.size()]);
			
			noiseValues = new Number[myNoises.length];
			timeValues = new Number[myNoises.length];

			for(int i = 0; i < myNoises.length; i++) {
				timeValues[i] = myNoises[i].getTime();
				noiseValues[i] = myNoises[i].getNoise();
				Toast.makeText(this, timeValues[i] + " : " + noiseValues[i], Toast.LENGTH_SHORT).show();
			}
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	private void plotData() {
		//getForecast();
		
		// initialize our XYPlot reference:
		XYPlot noisePlot = (XYPlot) findViewById(R.id.noisePlot);
		// Create a couple arrays of y-values to plot:
		Number[] series1Numbers = { 4, 3, 4, 3, 4, 2, 2 };
		Number[] series2Numbers = { 1, 2, 3, 4, 5, 6, 7 };
		
    // Turn the above arrays into XYSeries':
    XYSeries noiseSeries = new SimpleXYSeries(
            Arrays.asList(series2Numbers),          // SimpleXYSeries takes a List so turn our array into a List
            Arrays.asList(series1Numbers), // Y_VALS_ONLY means use the element index as the x value
            "");                             // Set the display title of the series


    // Create a formatter to use for drawing a series using LineAndPointRenderer:
    LineAndPointFormatter linePointFormat = new LineAndPointFormatter(
            Color.rgb(0, 200, 0), Color.rgb(0, 100, 0), null); //line, fill, point color
    
    // add a new series' to the xyplot:
    noisePlot.addSeries(noiseSeries, linePointFormat);

    // reduce the number of range labels
    noisePlot.setTicksPerRangeLabel(1);
    
    // by default, AndroidPlot displays developer guides to aid in laying out your plot.
    // To get rid of them call disableAllMarkup():
    noisePlot.disableAllMarkup();
    noisePlot.setDrawBorderEnabled(false);
    noisePlot.getLegendWidget().setVisible(false);
    noisePlot.getLayoutManager().remove(noisePlot.getDomainLabelWidget());
    noisePlot.getLayoutManager().remove(noisePlot.getRangeLabelWidget());
    noisePlot.getLayoutManager().remove(noisePlot.getTitleWidget());
    
    noisePlot.getBackgroundPaint().setColor(Color.WHITE);
    noisePlot.getGraphWidget().getBackgroundPaint().setColor(Color.WHITE);
    noisePlot.getGraphWidget().getGridBackgroundPaint().setColor(Color.WHITE);
    noisePlot.getGraphWidget().getDomainLabelPaint().setColor(Color.BLACK);
    noisePlot.getGraphWidget().getRangeLabelPaint().setColor(Color.BLACK);
    noisePlot.getGraphWidget().getDomainOriginLabelPaint().setColor(Color.BLACK);
    noisePlot.getGraphWidget().getDomainOriginLinePaint().setColor(Color.BLACK);
    noisePlot.getGraphWidget().getRangeOriginLinePaint().setColor(Color.BLACK);
    
    noisePlot.setPlotMargins(0, 0, 0, 0);
    noisePlot.setPlotPadding(0, 0, 0, 0);
    noisePlot.setGridPadding(0, 0, 0, 0);
    
    noisePlot.setDomainStepValue(7);
    
    noisePlot.position(noisePlot.getGraphWidget(), 0,
        XLayoutStyle.ABSOLUTE_FROM_LEFT, 0,
        YLayoutStyle.RELATIVE_TO_CENTER,
        AnchorPosition.LEFT_MIDDLE);    
     
	}
	
	/**
	 * onClick for button to go back to BrowseRoomActivity
	 * @param v
	 */
	public void onBackClicked(View v) {
    finish();
	}
	
	public void onSurveyClicked(View v) {
    Bundle translateBundle =
        ActivityOptions.makeCustomAnimation(DisplayRoomActivity.this,
        R.anim.slide_in_left, R.anim.slide_out_left).toBundle();
    
		Intent surveyLaunch = new Intent(this, SurveyActivity.class);
		surveyLaunch.putExtra(Globals.KEY_ROOMLOC_ID, mRoom.getId());
		startActivity(surveyLaunch, translateBundle);
	}
	
	/**
	 * Adds slide animation to up button click
	 */
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// Respond to the action bar's Up/Home button
		case android.R.id.home:
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
	
	/**
	 * Overrides finish to add slide animation
	 */
  public void finish() {
    super.finish();
    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
  }
  
  private ServiceConnection connection = new ServiceConnection() {
	
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		mSensorService = ((SensorBinder) service).getService();
		sensorCollectionState = mSensorService.getCollectionStateObject();
		
	}
	
	@Override
	public void onServiceDisconnected(ComponentName name) {
		mSensorService = null;
		
	}
	
};

  private void doBindService() {
	  if( !mIsBound) {
		  bindService(new Intent(this, SensorService.class), connection, Context.BIND_AUTO_CREATE);
		  mIsBound = true;
	  }
  }
  
  private void doUnbindService() {
	  if(mIsBound) {
		  unbindService(connection);
		  mIsBound = false;
	  }
  }
  
  //============== Methods to error check GooglePlayServices======
  
  private void resolveConnectionError(ConnectionResult connectionResult) {
	     if (connectionResult.hasResolution()) {
	            try {

	                // Start an Activity that tries to resolve the error
	                connectionResult.startResolutionForResult(
	                        this,
	                        Globals.CONNECTION_FAILURE_RESOLUTION_REQUEST);

	                /*
	                * Thrown if Google Play services canceled the original
	                * PendingIntent
	                */

	            } catch (IntentSender.SendIntentException e) {

	                // Log the error
	                e.printStackTrace();
	            }
	        } else {

	            // If no resolution is available, display a dialog to the user with the error.
	            showErrorDialog(connectionResult.getErrorCode());
	        }
  }
  
  private boolean checkGoogleConnection() {
	  int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	  
	  if (resultCode == ConnectionResult.SUCCESS) {
		  return true;
	  }
	  else {
		  showErrorDialog(resultCode);
		  return false;
	  }
  }
  
  private void showErrorDialog(int errorCode) {

      // Get the error dialog from Google Play services
      Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
          errorCode,
          this,
          Globals.CONNECTION_FAILURE_RESOLUTION_REQUEST);

      // If Google Play services can provide an error dialog
      if (errorDialog != null) {

          // Create a new DialogFragment in which to show the error dialog
          ErrorDialogFragment errorFragment = new ErrorDialogFragment();

          // Set the dialog in the DialogFragment
          errorFragment.setDialog(errorDialog);

          // Show the error dialog in the DialogFragment
          errorFragment.show(getFragmentManager(), Globals.APP_TAG);
      }
  }

  /**
   * Define a DialogFragment to display the error dialog generated in
   * showErrorDialog.
   */
  public static class ErrorDialogFragment extends DialogFragment {

      // Global field to contain the error dialog
      private Dialog mDialog;

      /**
       * Default constructor. Sets the dialog field to null
       */
      public ErrorDialogFragment() {
          super();
          mDialog = null;
      }

      /**
       * Set the dialog to display
       *
       * @param dialog An error dialog
       */
      public void setDialog(Dialog dialog) {
          mDialog = dialog;
      }

      /*
       * This method must return a Dialog to the DialogFragment.
       */
      @Override
      public Dialog onCreateDialog(Bundle savedInstanceState) {
          return mDialog;
      }
  }
}
