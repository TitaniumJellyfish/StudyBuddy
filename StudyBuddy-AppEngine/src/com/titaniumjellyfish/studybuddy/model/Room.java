package com.titaniumjellyfish.studybuddy.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;
import com.titaniumjellyfish.studybuddy.Datastore;

public class Room implements JSONSerializable<Room>, DatastoreEntity<Room> {

	private static final Logger log = Logger.getLogger(Room.class.getName());

	public double lat, lng;
	private String building_name, room_name;
	private String composite_name;
	private double capacity, crowd, rating;
	private int n_surveys;
	private List<String> comments;
	private List<NoiseMap> noise;
	public List<NoiseMap> interpolated;
	private long last_updated;

	public static final byte FLAG_INCLUDE_RAW_NOISE = 1; 	// 0000 0001
	public static final byte FLAG_INCLUDE_INTERPOLATED = 2;	// 0000 0010

	public static final String KEY_LATITUDE  	= "room_lat";
	public static final String KEY_LONGITUDE 	= "room_lng";
	public static final String KEY_BUILDING  	= "room_building";
	public static final String KEY_ROOM 		= "room_name";
	public static final String KEY_COMPOSITE_NAME = "comp_name";
	// NOTE: noise trend is not stored as a field for this entity; rather, each 
	//	noise trend entry is stored with this entity as its parent. still need this
	//	string key for JSON.
	public static final String KEY_NOISE  		= "room_noise";
	public static final String KEY_INTERPOLATED = "room_noise_interp";
	public static final String KEY_RATING       = "room_rating";
	public static final String KEY_CAPACITY  	= "room_capacity";
	public static final String KEY_CROWD 		= "room_crowd";
	public static final String KEY_NUM_SURVEYS  = "num_surveys";
	public static final String KEY_COMMENTS		= "room_comments";
	public static final String KEY_LAST_UPDATE	= "updated";

	public Room(){}

	public Room(double lat, double lng, String bldg, String room, double cap, double crowd,
			double rate, int surveys, long update, List<String> comments, List<NoiseMap> noise,
			List<NoiseMap> interp){
		this.lat = lat;
		this.lng = lng;
		this.building_name = bldg;
		this.room_name = room;
		this.capacity = cap;
		this.crowd = crowd;
		this.rating = rate;
		this.comments = comments;
		this.noise = noise;
		this.last_updated = update;
		this.composite_name = getCompName(this);
		this.n_surveys = surveys;
		this.interpolated = interp;
	}

	public void setInterpolated(List<NoiseMap> interp){
		this.interpolated = interp;
	}

	@Override
	public Room fromJSON(String json) {

		try {
			return fromJSON(new JSONObject(json));
		} catch (JSONException e) {
			log.info("Room: could not parse json string: "+json);;
		}
		return null;
	}

	@Override
	public JSONObject toJSON() {
		JSONObject obj = new JSONObject();

		try {
			obj.put(KEY_LATITUDE, this.lat);
			obj.put(KEY_LONGITUDE, this.lng);
			obj.put(KEY_BUILDING, this.building_name);
			obj.put(KEY_ROOM, this.room_name);
			obj.put(KEY_CAPACITY, this.capacity);
			obj.put(KEY_CROWD, this.crowd);
			obj.put(KEY_RATING, this.rating);
			obj.put(KEY_NUM_SURVEYS, this.n_surveys);
			JSONArray commentArray = new JSONArray();
			if(this.comments != null)
				for(String c : this.comments)
					commentArray.put(c);
			JSONArray noiseArray = new JSONArray();
			if(this.noise != null)
				for(NoiseMap nm : this.noise)
					noiseArray.put(nm.toJSON());
			JSONArray interpArray = new JSONArray();
			if(this.interpolated != null)
				for(NoiseMap nm : this.interpolated)
					interpArray.put(nm.toJSON());
			obj.put(KEY_COMMENTS, commentArray.toString());
			obj.put(KEY_NOISE, noiseArray.toString());
			obj.put(KEY_INTERPOLATED, interpArray.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}

		return obj;
	}

	@Override
	public Entity asEntity(Key parentKey) {
		Entity ent;
		if (parentKey != null)
			ent = new Entity(Datastore.KIND_ROOM, getCompName(this), parentKey);
		else
			ent = new Entity(Datastore.KIND_ROOM, getCompName(this));
		ent.setProperty(KEY_LATITUDE, this.lat);
		ent.setProperty(KEY_LONGITUDE, this.lng);
		ent.setProperty(KEY_BUILDING, this.building_name);
		ent.setProperty(KEY_ROOM, this.room_name);
		ent.setProperty(KEY_CAPACITY, this.capacity);
		ent.setProperty(KEY_CROWD, this.crowd);
		ent.setProperty(KEY_RATING, this.rating);
		ent.setProperty(KEY_COMMENTS, this.comments);
		ent.setProperty(KEY_LAST_UPDATE, this.last_updated);
		ent.setProperty(KEY_COMPOSITE_NAME, this.composite_name);
		ent.setProperty(KEY_NUM_SURVEYS, this.n_surveys);
		return ent;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Room fromEntity(Entity e) {
		List<Entity> noise_entities = Datastore.query(Datastore.KIND_NOISE_ENTRY, e.getKey(), null);
		List<NoiseMap> noise_list = new ArrayList<NoiseMap>();
		List<NoiseMap> interp_list = new ArrayList<NoiseMap>();
		for(Entity noise_e : noise_entities){
			NoiseMap nm = (new NoiseMap()).fromEntity(noise_e);
			if(nm.isRaw())
				noise_list.add((new NoiseMap()).fromEntity(noise_e));
			else
				interp_list.add((new NoiseMap()).fromEntity(noise_e));
		}
		return new Room(
				(Double) e.getProperty(KEY_LATITUDE),
				(Double) e.getProperty(KEY_LONGITUDE),
				(String) e.getProperty(KEY_BUILDING),
				(String) e.getProperty(KEY_ROOM),
				(Double) e.getProperty(KEY_CAPACITY),
				(Double) e.getProperty(KEY_CROWD),
				(Double) e.getProperty(KEY_RATING),
				((Long) e.getProperty(KEY_NUM_SURVEYS)).intValue(),
				(Long) e.getProperty(KEY_LAST_UPDATE),
				(List<String>) e.getProperty(KEY_COMMENTS),
				noise_list, interp_list);
	}

	@Override
	public void insertToDB(Key parentKey) {
		Key room_key = Datastore.insert(this.asEntity(parentKey));
		for(NoiseMap nm : this.noise)
			nm.insertToDB(room_key);
		for(NoiseMap nm : this.interpolated)
			nm.insertToDB(room_key);
	}

	@Override
	public Room fromJSON(JSONObject json) throws JSONException{
		// get comments from JSON array
		ArrayList<String> comments = new ArrayList<String>();
		JSONArray json_comments = json.getJSONArray(KEY_COMMENTS);
		for(int i=0; i<json_comments.length(); i++)
			comments.add(json_comments.getString(i));
		// get nose levels from JSON array
		ArrayList<NoiseMap> noises = new ArrayList<NoiseMap>();
		JSONArray json_noise = json.getJSONArray(KEY_NOISE);
		for(int i=0; i<json_noise.length(); i++)
			noises.add((new NoiseMap()).fromJSON(json_noise.getJSONObject(i)));
		// get nose levels [interpolated] from JSON array
		ArrayList<NoiseMap> interps = new ArrayList<NoiseMap>();
//		json_noise = json.getJSONArray(KEY_INTERPOLATED);
//		for(int i=0; i<json_noise.length(); i++)
//			interps.add((new NoiseMap()).fromJSON(json_noise.get(i).toString()));
		return new Room(
				json.getDouble(KEY_LATITUDE),
				json.getDouble(KEY_LONGITUDE), 
				json.getString(KEY_BUILDING), 
				json.getString(KEY_ROOM), 
				json.getDouble(KEY_CAPACITY), 
				json.getDouble(KEY_CROWD),
				json.getDouble(KEY_RATING),
				json.getInt(KEY_NUM_SURVEYS),
				0L, //json.getLong(KEY_LAST_UPDATE),
				comments, noises, interps);
	}

	public static String getCompName(Room room){
		return room.room_name + "@" + room.building_name;
	}

	public Key getKey(){
		return KeyFactory.createKey(Datastore.room_group, Datastore.KIND_ROOM, getCompName(this));
	}

	public static void addNewData(List<Room> newRooms){
		List<Key> keyList = new ArrayList<Key>(newRooms.size());
		for(Room r : newRooms) keyList.add(r.getKey());
		Map<Key, Entity> storeMap = Datastore.getAllEntites(keyList);
		for(int i = 0; i < newRooms.size(); i++){
			// for each non-null entity, _combine_ the existing one with the new data
			Entity ent = storeMap.get(keyList.get(i));
			if(ent != null){
				log.info("ROOM EXISTS");
				// update all of r1's fields with values from r2, then insert r1 back into the datastore
				Room r1 = (new Room()).fromEntity(ent);
				Room r2 = newRooms.get(i);
				if(r1.n_surveys + r2.n_surveys != 0){
					r1.rating = (r1.rating * r1.n_surveys + r2.rating * r2.n_surveys)
							/ (r1.n_surveys + r2.n_surveys);
					r1.capacity = (r1.capacity * r1.n_surveys + r2.capacity * r2.n_surveys)
							/ (r1.n_surveys + r2.n_surveys);
					r1.crowd = (r1.crowd * r1.n_surveys + r2.crowd * r2.n_surveys)
							/ (r1.n_surveys + r2.n_surveys);
					// TODO - should we update latitude and longitude too? We *could* take a
					//	weighted average of each room's location, but i think we'd rather not
					//	allow them to move around
				}
				if(r1.comments == null) r1.comments = r2.comments;
				else if(r2.comments != null) r1.comments.addAll(r2.comments);
				if(r1.noise == null) r1.noise = r2.noise;
				else if(r2.noise != null) r1.noise.addAll(r2.noise);
				r1.n_surveys += r2.n_surveys;
				r1.last_updated = System.currentTimeMillis() - 100;
				r1.insertToDB(Datastore.room_group);
			} else{
				// this is a new room! how exciting!
				log.info("NEW ROOM");
				Room r1 = newRooms.get(i);
				r1.last_updated = System.currentTimeMillis() - 100;
				r1.insertToDB(Datastore.room_group);
			}
		}
	}

	public void writeToGenerator(JsonGenerator g, byte flag){
		try {
			g.writeStartObject();
			g.writeFieldName(KEY_LATITUDE);
			g.writeNumber(this.lat);
			g.writeFieldName(KEY_LONGITUDE);
			g.writeNumber(this.lng);
			g.writeFieldName(KEY_BUILDING);
			g.writeString(this.building_name);
			g.writeFieldName(KEY_ROOM);
			g.writeString(this.room_name);
			g.writeFieldName(KEY_CAPACITY);
			g.writeNumber(this.capacity);
			g.writeFieldName(KEY_CROWD);
			g.writeNumber(this.crowd);
			g.writeFieldName(KEY_RATING);
			g.writeNumber(this.rating);
//			g.writeFieldName(KEY_LAST_UPDATE);
//			g.writeNumber(this.last_updated);
//			g.writeFieldName(KEY_COMPOSITE_NAME);
//			g.writeString(this.composite_name);
			g.writeFieldName(KEY_NUM_SURVEYS);
			g.writeNumber(this.n_surveys);
			// COMMENT ARRAY
			g.writeArrayFieldStart(KEY_COMMENTS);
			if(this.comments != null)
				for(String c : this.comments) g.writeString(c);
			g.writeEndArray();
			// RAW NOISE ARRAY
			if((flag & FLAG_INCLUDE_RAW_NOISE) != 0){
				g.writeArrayFieldStart(KEY_NOISE);
				if(this.noise != null)
					for(NoiseMap nm : this.noise) nm.writeToGenerator(g);
				g.writeEndArray();
			}
			// INTERPOLATED NOISE ARRAY
			if((flag & FLAG_INCLUDE_INTERPOLATED) != 0){
				g.writeArrayFieldStart(KEY_INTERPOLATED);
				if(this.interpolated != null)
				for(NoiseMap nm : this.interpolated) nm.writeToGenerator(g);
				g.writeEndArray();
			}

			g.writeEndObject();
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
