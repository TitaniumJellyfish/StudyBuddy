package com.titaniumjellyfish.studybuddy;

import java.sql.SQLException;
import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.stmt.QueryBuilder;
import com.titaniumjellyfish.studybuddy.database.CommentEntry;
import com.titaniumjellyfish.studybuddy.database.Globals;
import com.titaniumjellyfish.studybuddy.database.NoiseEntry;
import com.titaniumjellyfish.studybuddy.database.RoomLocationEntry;
import com.titaniumjellyfish.studybuddy.database.TitaniumDb;

public class RoomDef extends OrmLiteBaseActivity<TitaniumDb> implements OnMapClickListener{
	private static final LatLng BAKER_BERRY = new LatLng(43.70546, -72.28884);
	private static final int DEFAULT_MAP_ZOOM = 18;
	
	View mapView;
	GoogleMap mMap;
	
	Marker eLocMarker;
	AutoCompleteTextView eBuildingName, eRoomName;
	EditText eRoomCap, eRoomDesc;
	SeekBar eNoise, eCrowd;
	
	@Override 
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_room_def);
		
		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BAKER_BERRY, DEFAULT_MAP_ZOOM));
		
		mMap.setOnMapClickListener(this);
		
		mapView = findViewById(R.id.room_def_mapview);
		
		eRoomCap = (EditText) findViewById(R.id.room_def_capacity);
		eRoomName = (AutoCompleteTextView) findViewById(R.id.room_def_room);
		eRoomDesc = (EditText) findViewById(R.id.room_def_desc);
		
		// Make them choose an existing building
		eBuildingName = ((AutoCompleteTextView) findViewById(R.id.room_def_building));
		String[] building_names = getResources().getStringArray(R.array.campus_buildings);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, building_names);
		eBuildingName.setAdapter(adapter);
		
		// When the focus of this field has changed, update the room listings
		eBuildingName.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus){
					Toast.makeText(RoomDef.this,  "Getting Room List", Toast.LENGTH_SHORT).show();					
					
					try {
						
						Dao<RoomLocationEntry, Long> roomDao = getHelper().dao(RoomLocationEntry.class, Long.class);
						QueryBuilder<RoomLocationEntry, Long> qb = roomDao.queryBuilder();
						qb.where().like(RoomLocationEntry.COLUMN_NAME_BUILDING_NAME, eBuildingName.getText().toString());
						CloseableIterator<RoomLocationEntry> similarRooms = qb.iterator();
						
						ArrayList<String> roomSuggs = new ArrayList<String>();
						
						while (similarRooms.hasNext()){
							roomSuggs.add(similarRooms.next().getRoomName());
						}
						
						ArrayAdapter<String> adapt = new ArrayAdapter<String>(RoomDef.this, android.R.layout.simple_list_item_1, roomSuggs);
						eRoomName.setAdapter(adapt);
						
						//Log.d(Globals.LOGTAG, "set new array adapter for rooms");
						
						
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						//Log.e(Globals.LOGTAG, "Couldn't get list of rooms for roomdef");
						return;
						
					}
				}
			}
		});
		
		eNoise = (SeekBar) findViewById(R.id.noiseSeekbar);
		eNoise.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekbar, int position, boolean fromUser) {
				int noise = position;
				TextView noiseLabel = (TextView) findViewById(R.id.noiseBarLabel);
				String label = getString(R.string.ui_room_def_noise) + " ("	+ (noise+1) + ": ";
				switch (noise) {
					case 0:
						label += "Silent)";
						break;
					case 1:
						label += "White Noise)";
						break;
					case 2:
						label += "Noisy)";
						break;
					case 3:
						label += "Distracting)";
						break;
				}
				noiseLabel.setText(label);
			}
			public void onStopTrackingTouch(SeekBar seekBar) {}
			public void onStartTrackingTouch(SeekBar seekBar) {}
		});
		
		eCrowd = (SeekBar) findViewById(R.id.crowdSeekbar);
		eCrowd.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			public void onProgressChanged(SeekBar seekbar, int position, boolean fromUser) {
				int crowdedness = position;
				TextView crowdLabel = (TextView) findViewById(R.id.crowdBarLabel);
				String label = getString(R.string.ui_room_def_crowd) + " ("	+ (crowdedness+1) + ": ";
				switch (crowdedness) {
					case 0:
						label += "Empty)";
						break;
					case 1:
						label += "Signs of life)";
						break;
					case 2:
						label += "Crowded)";
						break;
					case 3:
						label += "Full)";
						break;
				}
				crowdLabel.setText(label);
			}
			public void onStopTrackingTouch(SeekBar seekBar) {}
			public void onStartTrackingTouch(SeekBar seekBar) {}
		});
	}
	
	/**
	 * Sets marker on map, gets lat/lng of room to add
	 * @param point
	 */
	public void onMapClick(LatLng point) {
		if (eLocMarker != null)
			eLocMarker.remove();
		eLocMarker = mMap.addMarker(new MarkerOptions().position(point).title(point.toString()));
	}
	
	/**
	 * Checks if there is any missing information
	 * @return true if all fields are filled
	 */
	private boolean fieldsFilled() {		
		boolean isEmpty = (eLocMarker == null ||
		eBuildingName.getText().toString().equals("") ||
		eRoomName.getText().toString().equals("") ||
		eRoomCap.getText().toString().equals("") ||
		eRoomName.getText().toString().equals("") ||
		eRoomCap.getText().toString().equals("") ||
		eRoomDesc.getText().toString().equals(""));
		
		return !isEmpty;
	}
	
	/**
	 * Saves new room data to local db
	 * Will not save if all fields aren't filled
	 * @param view
	 */
	public void onClickSave(View view) {
		//Make sure the fields work
		if(!fieldsFilled()){
			Toast.makeText(this, "Please fill in every field and place a location on the map", Toast.LENGTH_SHORT).show();
			return;
		}
		
		//insert to DB
		try {
			// Create room information
			Dao<RoomLocationEntry, Long> roomDao = getHelper().dao(RoomLocationEntry.class, Long.class);
			RoomLocationEntry rle = new RoomLocationEntry();
			rle.setBuildingName(eBuildingName.getText().toString());
			rle.setRoomName(eRoomName.getText().toString());
			rle.setCapacity(Integer.valueOf(eRoomCap.getText().toString()));
			rle.setAglCrowd(eCrowd.getProgress());
			rle.setLatitude(eLocMarker.getPosition().latitude);
			rle.setLongitude(eLocMarker.getPosition().longitude);
			rle.setLocal(true);
			roomDao.create(rle);
			
			// Enter noise information
			Dao<NoiseEntry,Long> noiseDao = getHelper().dao(NoiseEntry.class, Long.class);
			NoiseEntry ne = new NoiseEntry();
			ne.setLocal(true);
			ne.setNoise(eNoise.getProgress());
			ne.setParent(rle);
			ne.setTime(System.currentTimeMillis());
			noiseDao.create(ne);
			
			//Enter comment information
			Dao<CommentEntry,Long> commentDao = getHelper().dao(CommentEntry.class,  Long.class);
			CommentEntry ce = new CommentEntry();
			ce.setComment(eRoomDesc.getText().toString());
			ce.setLocal(true);
			ce.setParent(rle);
			commentDao.create(ce);

			
			Toast.makeText(this, "Room saved", Toast.LENGTH_SHORT).show();
			finish();
		} catch (SQLException e) {
			Log.w(Globals.LOGTAG, e);
			e.printStackTrace();
			Toast.makeText(this, "Could not define room. Does it already exist?", Toast.LENGTH_SHORT).show();
		}		
	}
	
	public void onClickCancel(View view) {
		Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();
		finish();
	}
	
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// Respond to the action bar's Up/Home button
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
  public void finish() {
    super.finish();
    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
  }
}