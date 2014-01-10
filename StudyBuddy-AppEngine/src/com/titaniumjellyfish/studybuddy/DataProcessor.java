package com.titaniumjellyfish.studybuddy;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.titaniumjellyfish.studybuddy.model.Room;
import com.titaniumjellyfish.studybuddy.model.NoiseMap;

/**
 * This class will contain all the methods to do server-side data crunching before redistributing out
 * 	to phones
 */
public class DataProcessor {

	private static final Logger log = Logger.getLogger(DataProcessor.class.getName());

	/**
	 * Populate room.interpolated with values
	 * @param room the room to update
	 * @param span the total time in the past (from now) to go back (in milliseconds)
	 * @param interval the spacing of interpolated times (in milliseconds)
	 */
	public static void interpolateRoomData(Room room, long span, long interval){
		// floor to nearest hour (integer division is magic)
		long recent_hour = (System.currentTimeMillis() / ServerGlobals.HOUR) * ServerGlobals.HOUR;
		Filter f = FilterOperator.EQUAL.of(NoiseMap.KEY_RAW, true);
		Key room_key = room.getKey();
		List<Entity> entities = Datastore.query(Datastore.KIND_NOISE_ENTRY, room_key, 
				f, NoiseMap.KEY_TIMESTAMP);
		List<NoiseMap> sortedNoises = new ArrayList<NoiseMap>(entities.size());
		for(Entity ent : entities) sortedNoises.add((new NoiseMap()).fromEntity(ent));
		if(sortedNoises.size() > 0){
			List<NoiseMap> interpolated = new ArrayList<NoiseMap>();
			for(long center = recent_hour - span; center <= recent_hour; center += interval){
				List<NoiseMap> nearby = getNoisesInWindowOrClosest(sortedNoises, center, interval + ServerGlobals.MINUTE);
				interpolated.add(new NoiseMap(
						noiseAverage(nearby),
						center,
						false));
			}
			if(room.interpolated != null){
				Key[] oldKeys = new Key[room.interpolated.size()];
				for(int i=0; i<oldKeys.length; i++)
					oldKeys[i] = room.interpolated.get(i).getKey(room_key);
				Datastore.remove(oldKeys);
			}
			room.setInterpolated(interpolated);
			room.insertToDB(Datastore.room_group);
		}
	}

	public static double noiseAverage(List<NoiseMap> noises){
		if(noises != null && noises.size() > 0){
			double tot = 0.0;
			for(NoiseMap nm : noises)
				tot += nm.noise_level;
			return tot / (double) noises.size();
		} else return 0.0;
	}

	public static double weightNoiseTimeAverage(List<NoiseMap> noises, long center){
		if (noises.size() == 0) return -1.0;
		else if (noises.size() == 1) return noises.get(0).noise_level;
		else{
			double ttotal = 0;
			for(NoiseMap nm : noises)
				ttotal += Math.abs(center - nm.timestamp) + 1;
			double navg = 0.0;
			for(NoiseMap nm : noises)
				navg += nm.noise_level * (1 - (double) (Math.abs(center - nm.timestamp) + 1) / ttotal);
			return navg / (noises.size() - 1);
		}
	}

	/**
	 * Make and return a list of noises within plus-or-minus 'window' milliseconds from 'center'. If none
	 * 	are in that window, return the NoiseMap with closest time
	 */
	private static List<NoiseMap> getNoisesInWindowOrClosest(List<NoiseMap> sortedNoises, long center, long window){
		int i = binarySearchNoiseTime(sortedNoises, center);
		List<NoiseMap> ret = new ArrayList<NoiseMap>();
		ret.add(sortedNoises.get(i));
		// search higher than i
		int j = i + 1;
		while(j < sortedNoises.size() && Math.abs(sortedNoises.get(j).timestamp - center) < window)
			ret.add(sortedNoises.get(j++));
		// search lower than i
		j = i - 1;
		while(j >= 0 && Math.abs(sortedNoises.get(j).timestamp - center) < window)
			ret.add(sortedNoises.get(j--));
		return ret;
	}

	//	/**
	//	 * Make a list of the k noise entries that are closest (temporally) to the given time. the input list
	//	 * 	of noises must be sorted.
	//	 */
	//	private static List<NoiseMap> getKNearestNoises(List<NoiseMap> sortedNoises, long center, int K){
	//		// Binary search to find nearest index
	//		int i = binarySearchNoiseTime(sortedNoises, center);
	//		List<NoiseMap> ret = new ArrayList<NoiseMap>(K);
	//		// Close times can be either side. keep track of how far we've gone hi and lo
	//		int i_hi = i+1;
	//		int i_lo = i-1;
	//		ret.add(sortedNoises.get(i));
	//		for(int k = 1; k < K; k++){
	//			if (i_lo < 0) ret.add(sortedNoises.get(i_hi++));
	//			else if (i_hi > sortedNoises.size() - 1) ret.add(sortedNoises.get(i_lo--));
	//			else{
	//				long dist_hi = Math.abs(sortedNoises.get(i_hi).timestamp - center);
	//				long dist_lo = Math.abs(sortedNoises.get(i_lo).timestamp - center);
	//				if(dist_hi < dist_lo) ret.add(sortedNoises.get(i_hi++));
	//				else ret.add(sortedNoises.get(i_lo--));
	//			}
	//		}
	//		return ret;
	//	}

	/**
	 * search for the index of the noise-map value with time closest to 'query', return its index in the list
	 */
	private static int binarySearchNoiseTime(List<NoiseMap> sortedNoises, long query){
		int i = 0, j = sortedNoises.size()-1;
		return binarySearchNoiseTime(sortedNoises, query, i, j);
	}

	private static int binarySearchNoiseTime(List<NoiseMap> sortedNoises, long query, int i, int j){
		log.info("binary search: "+i+", "+j);
		if(i < j-1){
			int q = (i + j) / 2;
			if(sortedNoises.get(q).timestamp == query)
				return i;
			else if(sortedNoises.get(q).timestamp < query)
				return binarySearchNoiseTime(sortedNoises, query, q+1, j);
			else if(sortedNoises.get(q).timestamp > query)
				return binarySearchNoiseTime(sortedNoises, query, i, q-1);
		} 
		else  if(Math.abs(sortedNoises.get(i).timestamp - query) < Math.abs(sortedNoises.get(j).timestamp - query))
			return i;
		return j;

	}

	public static void reprocessAll() {
		List<Entity> allRooms = Datastore.query(Datastore.KIND_ROOM, Datastore.room_group, null);
		for(Entity ent : allRooms){
			interpolateRoomData(
					(new Room()).fromEntity(ent),
					ServerGlobals.WEEK,
					ServerGlobals.HOUR);
		}
	}
}
