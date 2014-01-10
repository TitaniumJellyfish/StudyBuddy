package com.titaniumjellyfish.studybuddy.model;

import java.io.IOException;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.titaniumjellyfish.studybuddy.Datastore;


public class NoiseMap implements JSONSerializable<NoiseMap>, DatastoreEntity<NoiseMap> {

	private static final Logger log = Logger.getLogger(NoiseMap.class.getName());

//	private double lat, lng;
	public double noise_level;
	public long timestamp;
	private boolean is_raw;

//	public static final String KEY_LATITUDE = "noisemap_lat";
//	public static final String KEY_LONGITUDE = "noisemap_lng";
	public static final String KEY_NOISE_LEVEL = "noisemap_noise";
	public static final String KEY_TIMESTAMP = "noisemap_timestamp";
	public static final String KEY_RAW = "noisemap_raw";

	public NoiseMap(){
		this(-1D, 0L, true);
	}

	public NoiseMap(
			//double lat, double lng,
			double noise, long time, boolean raw){
//		this.lat = lat;
//		this.lng = lng;
		this.noise_level = noise;
		this.timestamp = time;
		this.is_raw = raw;
	}

	@Override
	public NoiseMap fromJSON(String json) {
		try {
			return fromJSON(new JSONObject(json));
		} catch (JSONException e) {
			log.info("NoiseMap: could not parse json string: "+json);
		}
		return null;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		try {
//			json.put(KEY_LATITUDE, lat);
//			json.put(KEY_LONGITUDE, lng);
			json.put(KEY_NOISE_LEVEL, noise_level);
			json.put(KEY_TIMESTAMP, timestamp);
			json.put(KEY_RAW, is_raw);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return json;
	}

	@Override
	public Entity asEntity(Key parentKey) {
		Entity ent;
		if(parentKey != null)
			ent = new Entity(Datastore.KIND_NOISE_ENTRY, timestamp, parentKey);
		else
			ent = new Entity(Datastore.KIND_NOISE_ENTRY, timestamp);
//		ent.setProperty(KEY_LATITUDE, lat);
//		ent.setProperty(KEY_LONGITUDE, lng);
		ent.setProperty(KEY_NOISE_LEVEL, noise_level);
		ent.setProperty(KEY_TIMESTAMP, timestamp);
		ent.setProperty(KEY_RAW, is_raw);
		return ent;
	}

	@Override
	public NoiseMap fromEntity(Entity ent) {
		return new NoiseMap(
//				(Double) ent.getProperty(KEY_LATITUDE),
//				(Double) ent.getProperty(KEY_LONGITUDE),
				(Double) ent.getProperty(KEY_NOISE_LEVEL),
				(Long) ent.getProperty(KEY_TIMESTAMP),
				(Boolean) ent.getProperty(KEY_RAW));
	}

	@Override
	public void insertToDB(Key parentKey) {
		Datastore.insert(this.asEntity(parentKey));
	}

	@Override
	public NoiseMap fromJSON(JSONObject json) throws JSONException {
		return new NoiseMap(
//				json.getDouble(KEY_LATITUDE),
//				json.getDouble(KEY_LONGITUDE),
				json.getDouble(KEY_NOISE_LEVEL),
				json.getLong(KEY_TIMESTAMP),
				true);
	}

	public boolean isRaw() {
		return is_raw;
	}

	public void writeToGenerator(JsonGenerator g) throws JsonGenerationException, IOException {
		g.writeStartObject();

//		g.writeFieldName(KEY_LATITUDE);
//		g.writeNumber(lat);
//		g.writeFieldName(KEY_LONGITUDE);
//		g.writeNumber(lng);
		g.writeFieldName(KEY_NOISE_LEVEL);
		g.writeNumber(noise_level);
		g.writeFieldName(KEY_TIMESTAMP);
		g.writeNumber(timestamp);
//		g.writeFieldName(KEY_RAW);
//		g.writeBoolean(is_raw);

		g.writeEndObject();
	}
	
	public Key getKey(Key parent){
		return KeyFactory.createKey(parent, Datastore.KIND_NOISE_ENTRY, this.timestamp);
	}
}
