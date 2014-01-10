package com.titaniumjellyfish.studybuddy;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.j256.ormlite.dao.Dao;
import com.titaniumjellyfish.studybuddy.database.Globals;
import com.titaniumjellyfish.studybuddy.database.NoiseEntry;
import com.titaniumjellyfish.studybuddy.database.RoomLocationEntry;

public class RoomComparators {

	public static enum FIELD {NOISE_WEEK_OLD, NOISE_DAY_OLD, NOISE_HOUR_OLD,
		PRODUCTIVITY, CROWD, DISTANCE_METERS};
		
		public static final Map<FIELD, Double> DEFAULT_WEIGHTS_NOISE;
		public static final Map<FIELD, Double> DEFAULT_WEIGHTS_CROWD;
		public static final Map<FIELD, Double> DEFAULT_WEIGHTS_PRODUCTIVITY;
		
		static{
			DEFAULT_WEIGHTS_NOISE = new HashMap<RoomComparators.FIELD, Double>();
			DEFAULT_WEIGHTS_CROWD = new HashMap<RoomComparators.FIELD, Double>();
			DEFAULT_WEIGHTS_PRODUCTIVITY = new HashMap<RoomComparators.FIELD, Double>();
			
//			// Keep in mind that negative numbers are necessary to specify that lower values are better
			DEFAULT_WEIGHTS_NOISE.put(FIELD.NOISE_WEEK_OLD, Double.valueOf(-10.0));
			DEFAULT_WEIGHTS_NOISE.put(FIELD.NOISE_DAY_OLD,  Double.valueOf(-5.0));
			DEFAULT_WEIGHTS_NOISE.put(FIELD.NOISE_HOUR_OLD, Double.valueOf(-5.0));
			DEFAULT_WEIGHTS_NOISE.put(FIELD.PRODUCTIVITY,   Double.valueOf(5.0));
			DEFAULT_WEIGHTS_NOISE.put(FIELD.CROWD,  		Double.valueOf(-5.0));
			DEFAULT_WEIGHTS_NOISE.put(FIELD.DISTANCE_METERS,Double.valueOf(-0.1));

			DEFAULT_WEIGHTS_CROWD.put(FIELD.NOISE_WEEK_OLD, Double.valueOf(-5.0));
			DEFAULT_WEIGHTS_CROWD.put(FIELD.NOISE_DAY_OLD,  Double.valueOf(-2.0));
			DEFAULT_WEIGHTS_CROWD.put(FIELD.NOISE_HOUR_OLD, Double.valueOf(-1.0));
			DEFAULT_WEIGHTS_CROWD.put(FIELD.PRODUCTIVITY,   Double.valueOf(5.0));
			DEFAULT_WEIGHTS_CROWD.put(FIELD.CROWD,  		Double.valueOf(-15.0));
			DEFAULT_WEIGHTS_CROWD.put(FIELD.DISTANCE_METERS,Double.valueOf(-0.1));

			DEFAULT_WEIGHTS_PRODUCTIVITY.put(FIELD.NOISE_WEEK_OLD, Double.valueOf(-5.0));
			DEFAULT_WEIGHTS_PRODUCTIVITY.put(FIELD.NOISE_DAY_OLD,  Double.valueOf(-2.0));
			DEFAULT_WEIGHTS_PRODUCTIVITY.put(FIELD.NOISE_HOUR_OLD, Double.valueOf(-1.0));
			DEFAULT_WEIGHTS_PRODUCTIVITY.put(FIELD.PRODUCTIVITY,   Double.valueOf(15.0));
			DEFAULT_WEIGHTS_PRODUCTIVITY.put(FIELD.CROWD,  		Double.valueOf(-5.0));
			DEFAULT_WEIGHTS_PRODUCTIVITY.put(FIELD.DISTANCE_METERS,Double.valueOf(-0.1));
		}
		
		public static Comparator<RoomLocationEntry> makeWeightedComparator(final Map<FIELD, Double> weights){
			return makeWeightedComparator(weights, null, null);
		}
		
		public static Comparator<RoomLocationEntry> makeWeightedComparator(final Map<FIELD, Double> weights, final LatLng cur_location){
			return makeWeightedComparator(weights, cur_location, null);
		}

		public static Comparator<RoomLocationEntry> makeWeightedComparator(final Map<FIELD, Double> weights, final LatLng cur_location, final Dao<NoiseEntry, Long> noiseDao){
			return new Comparator<RoomLocationEntry>(){
				@Override
				public int compare(RoomLocationEntry lhs, RoomLocationEntry rhs) {
					double sum_lhs = 0.0;
					double sum_rhs = 0.0;
					long now = System.currentTimeMillis();
					if(weights.containsKey(FIELD.CROWD)){
						sum_lhs += weights.get(FIELD.CROWD) * lhs.getCrowd();
						sum_rhs += weights.get(FIELD.CROWD) * rhs.getCrowd();
					}
					if(weights.containsKey(FIELD.PRODUCTIVITY)){
						sum_lhs += weights.get(FIELD.PRODUCTIVITY) * lhs.getProductivity();
						sum_rhs += weights.get(FIELD.PRODUCTIVITY) * rhs.getProductivity();
					}
					if(cur_location != null && weights.containsKey(FIELD.DISTANCE_METERS)){
						double ldist = distFrom(lhs.getLatitude(), lhs.getLongitude(), cur_location.latitude, cur_location.longitude);
						double rdist = distFrom(rhs.getLatitude(), rhs.getLongitude(), cur_location.latitude, cur_location.longitude);
						sum_lhs += ldist * weights.get(FIELD.DISTANCE_METERS);
						sum_rhs += rdist * weights.get(FIELD.DISTANCE_METERS);
					}
					if(noiseDao != null && weights.containsKey(FIELD.NOISE_WEEK_OLD)){
						try {
							List<NoiseEntry> lne = noiseDao.queryBuilder().where().eq(NoiseEntry.COLUMN_NAME_PARENT_ID, lhs.getId())
							.and().eq(NoiseEntry.COLUMN_NAME_TIMESTAMP, TimeUtils.floorToHour(now) - TimeUtils.WEEK)
							.query();
							List<NoiseEntry> rne = noiseDao.queryBuilder().where().eq(NoiseEntry.COLUMN_NAME_PARENT_ID, rhs.getId())
							.and().eq(NoiseEntry.COLUMN_NAME_TIMESTAMP, TimeUtils.floorToHour(now) - TimeUtils.WEEK)
							.query();
							if(lne.size() > 0 && rne.size() > 0){
								sum_lhs += lne.get(0).getNoise() * weights.get(FIELD.NOISE_WEEK_OLD);
								sum_rhs += rne.get(0).getNoise() * weights.get(FIELD.NOISE_WEEK_OLD);
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
					if(noiseDao != null && weights.containsKey(FIELD.NOISE_DAY_OLD)){
						try {
							List<NoiseEntry> lne = noiseDao.queryBuilder().where().eq(NoiseEntry.COLUMN_NAME_PARENT_ID, lhs.getId())
							.and().eq(NoiseEntry.COLUMN_NAME_TIMESTAMP, TimeUtils.floorToHour(now) - TimeUtils.DAY)
							.query();
							List<NoiseEntry> rne = noiseDao.queryBuilder().where().eq(NoiseEntry.COLUMN_NAME_PARENT_ID, rhs.getId())
							.and().eq(NoiseEntry.COLUMN_NAME_TIMESTAMP, TimeUtils.floorToHour(now) - TimeUtils.DAY)
							.query();
							if(lne.size() > 0 && rne.size() > 0){
								sum_lhs += lne.get(0).getNoise() * weights.get(FIELD.NOISE_DAY_OLD);
								sum_rhs += rne.get(0).getNoise() * weights.get(FIELD.NOISE_DAY_OLD);
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
					if(noiseDao != null && weights.containsKey(FIELD.NOISE_HOUR_OLD)){
						try {
							List<NoiseEntry> lne = noiseDao.queryBuilder().where().eq(NoiseEntry.COLUMN_NAME_PARENT_ID, lhs.getId())
							.and().eq(NoiseEntry.COLUMN_NAME_TIMESTAMP, TimeUtils.floorToHour(now) - TimeUtils.HOUR)
							.query();
							List<NoiseEntry> rne = noiseDao.queryBuilder().where().eq(NoiseEntry.COLUMN_NAME_PARENT_ID, rhs.getId())
							.and().eq(NoiseEntry.COLUMN_NAME_TIMESTAMP, TimeUtils.floorToHour(now) - TimeUtils.HOUR)
							.query();
							if(lne.size() > 0 && rne.size() > 0){
								sum_lhs += lne.get(0).getNoise() * weights.get(FIELD.NOISE_HOUR_OLD);
								sum_rhs += rne.get(0).getNoise() * weights.get(FIELD.NOISE_HOUR_OLD);
							} else{
								//Log.d(Globals.LOGTAG, "NO NOISE DATA FOR EITHER");
							}
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
					return sum_lhs > sum_rhs ? -1 : (sum_lhs == sum_rhs ? 0 : 1);
				}
			};
		}

		// http://stackoverflow.com/questions/837872/calculate-distance-in-meters-when-you-know-longitude-and-latitude-in-java
		public static double distFrom(double lat1, double lng1, double lat2, double lng2) {
			double earthRadius = 3958.75;
			double dLat = Math.toRadians(lat2-lat1);
			double dLng = Math.toRadians(lng2-lng1);
			double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
			Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
			Math.sin(dLng/2) * Math.sin(dLng/2);
			double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
			double dist = earthRadius * c;

			int meterConversion = 1609;

			return dist * meterConversion;
		}
}
