package com.titaniumjellyfish.studybuddy;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;
import com.titaniumjellyfish.studybuddy.database.Globals;
import com.titaniumjellyfish.studybuddy.database.NoiseEntry;
import com.titaniumjellyfish.studybuddy.database.RoomLocationEntry;
import com.titaniumjellyfish.studybuddy.database.TitaniumDb;

/**
 * Queries database
 * Displays map with markers of best rooms from query
 * Shows list of best rooms under map in ListView
 * 
 * TODO database query
 * TODO custom ListView entry xml
 * TODO remove button in xml when merged, temp to access DisplayRoomActivity
 * @author Wesley
 *
 */
public class BrowseRoomActivity extends OrmLiteBaseActivity<TitaniumDb>  implements OnMarkerClickListener, 
com.google.android.gms.location.LocationListener,
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener{

	private static final LatLng BAKER_BERRY = new LatLng(43.70546, -72.28884);
	private static final int DEFAULT_MAP_ZOOM = 18;


	private final int SPINNER_NOISE = 0;
	private final int SPINNER_CROWD = 1;
	private final int SPINNER_PRODUCTIVITY = 2;

	private LocationClient mLocationClient;

	GoogleMap mMap;
	RoomEntriesAdapter mListAdapter;
	ListView mRoomView;
	Context mContext; 

	/**
	 * 
	 */
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Log.d(Globals.LOGTAG, "Browse: oncreate");

		setContentView(R.layout.activity_browse_room);

		mContext = this;

		mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
		mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(BAKER_BERRY, DEFAULT_MAP_ZOOM));
		mMap.setOnMarkerClickListener(this);

		//===current location stuff
		mLocationClient = new LocationClient(this, this, this);
		mLocationClient.connect();
		LatLng lastLatLng = null;
		if(mLocationClient.isConnected()) {
			Location lastLoc = mLocationClient.getLastLocation();
			if(lastLoc != null) lastLatLng = new LatLng(lastLoc.getLatitude(), lastLoc.getLongitude());
			else lastLatLng = new LatLng(43.709156,-72.283978); 
			mLocationClient.disconnect();
		} else lastLatLng = new LatLng(43.709156,-72.283978); 

		mRoomView = (ListView) this.findViewById(R.id.browseListView);		
		Dao<NoiseEntry, Long> noiseDao = null;
		try {
			noiseDao = getHelper().dao(NoiseEntry.class, Long.class);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		Comparator<RoomLocationEntry> comp = null;

		int filter = SPINNER_PRODUCTIVITY;
		Bundle extras = getIntent().getExtras();
		if(extras != null){
			filter = extras.getInt(Globals.KEY_FILTER);
		}
		
		switch(filter) {
		case SPINNER_NOISE:
			Log.d(Globals.LOGTAG, "setting comp to NOISE");
			comp = RoomComparators.makeWeightedComparator(RoomComparators.DEFAULT_WEIGHTS_NOISE, lastLatLng, noiseDao);
			break;
		case SPINNER_CROWD:
			Log.d(Globals.LOGTAG, "setting comp to CROWD");
			comp = RoomComparators.makeWeightedComparator(RoomComparators.DEFAULT_WEIGHTS_CROWD, lastLatLng, noiseDao);
			break;
		case SPINNER_PRODUCTIVITY:
			Log.d(Globals.LOGTAG, "setting comp to PROD");
			comp = RoomComparators.makeWeightedComparator(RoomComparators.DEFAULT_WEIGHTS_PRODUCTIVITY, lastLatLng, noiseDao);
			break;
		default:
			break;
		}

		mListAdapter = new RoomEntriesAdapter(this, comp);

		mRoomView.setAdapter(mListAdapter);
		//setRoomMarkers(); 

		mRoomView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, final View view,
					int position, long id) {
				RoomLocationEntry rle = (RoomLocationEntry) mRoomView.getItemAtPosition(position);
				gotoDisplayRoom(rle.getId());
			}
		});
	}

	/**
	 * Place location markers for listed rooms on map
	 * Room name contained in marker title, rowid contained in snippet
	 */
	private void setRoomMarker(double lat, double lng, String title, String id) {
		mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lng))
				.title(title)
				.snippet(id));
	}

	/**
	 * On marker click, smooth scroll to room position on list
	 */
	@Override
	public boolean onMarkerClick(Marker marker) {		
		int position = Integer.parseInt(marker.getSnippet());
		mRoomView.smoothScrollToPosition(position);
		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.getPosition(), mMap.getCameraPosition().zoom));
		marker.showInfoWindow();
		return true;
	}

	private void gotoDisplayRoom(Long id) {

		Intent i = new Intent(this, DisplayRoomActivity.class);

		i.putExtra(Globals.KEY_ROOMLOC_ID, id);

		Bundle translateBundle =
			ActivityOptions.makeCustomAnimation(BrowseRoomActivity.this,
					R.anim.slide_in_left, R.anim.slide_out_left).toBundle();

		startActivity(i, translateBundle);
	}

	/**
	 * Adds slide animation to up button click
	 */
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

	/**
	 * Overrides finish to add slide animation
	 */
	public void finish() {
		super.finish();
		overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right);
	}

	/**
	 * TODO custom listview
	 */
	private class RoomEntriesAdapter implements ListAdapter{

		private LayoutInflater mInflater;
		private List<RoomLocationEntry> roomLocEntries;

		public RoomEntriesAdapter(Context context, Comparator<RoomLocationEntry> roomCompare) {
			mInflater = LayoutInflater.from(context);

			//get the rooms
			try {
				Dao<RoomLocationEntry, Long> roomDao = getHelper().dao(RoomLocationEntry.class, Long.class);
				roomLocEntries = roomDao.queryForAll();
				if(roomCompare != null){
					Collections.sort(roomLocEntries, roomCompare);
					Log.d(Globals.LOGTAG, "Doing sort");
				} else{
					Log.d(Globals.LOGTAG, "No sort available");
				}
				int i = 1;
				for (RoomLocationEntry r:roomLocEntries) {
//					Log.d(Globals.LOGTAG, r.getRoomName() + ": " + r.getLatitude() + "," + r.getLongitude());
					setRoomMarker(r.getLatitude(), r.getLongitude(), r.getBuildingName() + " " + r.getRoomName(), Integer.toString(i));
					i++;
				}

				if(roomLocEntries.size() > 0){
					LatLng first = new LatLng(roomLocEntries.get(0).getLatitude(), roomLocEntries.get(0).getLongitude());
					mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(first, mMap.getCameraPosition().zoom));			
				}
				//				roomDao.queryBuilder().limit(Globals.BROWSE_ACTIVITY_MAX_ROOM);
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				roomLocEntries = null;
			}
		}

		@Override
		public int getCount() {
			if (roomLocEntries == null)
				return 0;

			return roomLocEntries.size();
		}

		@Override
		public Object getItem(int arg0) {
			return roomLocEntries.get(arg0);
		}


		@Override
		public long getItemId(int arg0) {
			return roomLocEntries.get(arg0).getId();
		}


		/// Unneeded Functions
		@Override
		public int getItemViewType(int arg0) {return 0;}
		@Override
		public int getViewTypeCount() {return 1;}


		@Override
		public View getView(int position, View ignored, ViewGroup parent) {
			View v = mInflater.inflate(R.layout.room_list_view, parent, false);

			RoomLocationEntry rle = (RoomLocationEntry) this.getItem(position);
			TextView title = (TextView) v.findViewById(R.id.room_list_title);
			TextView subtitle = (TextView) v.findViewById(R.id.room_list_subtitle);

			title.setText((position+1) + ". " + rle.getBuildingName() + " " + rle.getRoomName());
			subtitle.setText(Globals.parseProductivityLevel((int) rle.getProductivity()));

			return v;
		}





		@Override
		public boolean hasStableIds() {return true;}


		@Override
		public boolean isEmpty() {
			if (roomLocEntries != null)
				return roomLocEntries.size() > 0;
				return true;
		}


		@Override
		public void registerDataSetObserver(DataSetObserver arg0) {
			//		throw new UnsupportedOperationException("Cannot Register Observer for RoomLocationAdapter");
		}


		@Override
		public void unregisterDataSetObserver(DataSetObserver arg0) {
			//			throw new UnsupportedOperationException("Cannot Register Observer for RoomLocationAdapter");	
		}


		@Override
		public boolean areAllItemsEnabled() {
			return true;
		}


		@Override
		public boolean isEnabled(int arg0) {
			return true;
		}

	}

	//=======Google Play Stuff ====
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onConnected(Bundle connectionHint) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onLocationChanged(Location location) {
		// TODO Auto-generated method stub

	}
}
