package com.titaniumjellyfish.studybuddy.sensors;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

public class LocationHelper {


	private LocationManager manager;
	private String provider;
	private Location lastLocation;

	public LocationHelper(Context context){
		manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
	    criteria.setAccuracy(Criteria.ACCURACY_FINE);
	    criteria.setPowerRequirement(Criteria.POWER_LOW);
	    criteria.setAltitudeRequired(false);
	    criteria.setBearingRequired(false);
	    criteria.setSpeedRequired(false);
	    criteria.setCostAllowed(true);
	    provider = manager.getBestProvider(criteria, true);
	    manager.requestLocationUpdates(provider, 2000, 10, new LocationUpdater());
	}

	public LocationManager getLocationManager(){
		return manager;
	}
	
	public void forceRefreshLastLocation(){
		if(manager != null)
			lastLocation = manager.getLastKnownLocation(provider);
	}

	public double getLastLatitude(){
		if(lastLocation != null)
			return lastLocation.getLatitude();
		else return 0.0;
	}

	public double getLastLongitude(){
		if(lastLocation != null)
			return lastLocation.getLongitude();
		else return 0.0;
	}
	
	private class LocationUpdater implements LocationListener{

		@Override
		public void onLocationChanged(Location arg0) {
			lastLocation = arg0;
		}

		@Override
		public void onProviderDisabled(String arg0) {
			// TODO
		}

		@Override
		public void onProviderEnabled(String arg0) {
			// TODO
		}

		@Override
		public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
			// TODO
		}
	}

}
