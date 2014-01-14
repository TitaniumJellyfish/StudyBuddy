package com.titaniumjellyfish.studybuddy.database;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable
public class NoiseEntry {
	
	
	@DatabaseField
	private long id;
	public static final String COLUMN_NAME_NOISE = "noise";
	@DatabaseField(columnName=COLUMN_NAME_NOISE)
	private double noise;
	public static final String COLUMN_NAME_IS_LOCAL = "is_local";
	@DatabaseField(columnName = COLUMN_NAME_IS_LOCAL)
	private boolean is_local;
	public static final String COLUMN_NAME_TIMESTAMP = "timestamp";
	@DatabaseField(columnName=COLUMN_NAME_TIMESTAMP)
	private long timestamp;
	public static final String COLUMN_NAME_PARENT_ID = "parent_id";
	@DatabaseField(index=true, foreign=true, foreignAutoRefresh = true, columnName=COLUMN_NAME_PARENT_ID)
	RoomLocationEntry parentRoom;

	public NoiseEntry(){   //RoomLocationEntry parent){
		timestamp = System.currentTimeMillis();
		noise = -1;
		is_local = true;
//		parentRoom = parent;
	}
	public NoiseEntry(RoomLocationEntry parent){
		timestamp = System.currentTimeMillis();
		noise = -1;
		is_local = true;
//		parentRoom = parent;
	}

	//ID
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	//Noise 
	public double getNoise() {
		return noise;
	}

	public void setNoise(double noise) {
		this.noise = noise;	
	}
	//Time
	public long getTime() {
		return timestamp;
	}

	public void setTime(long time) {
		this.timestamp = time;
	}
	// Local-ness
	public boolean getLocal(){
		return is_local;
	}

	public void setLocal(boolean local){
		this.is_local = local;
	}
	public void setParent (RoomLocationEntry parent){
		this.parentRoom = parent;
	}
	public RoomLocationEntry getParent(){
		return parentRoom;
	}


	// COPIED FROM DATASTORE
	private static final String KEY_NOISE_LEVEL = "noisemap_noise";
	private static final String KEY_TIMESTAMP = "noisemap_timestamp";


	public static NoiseEntry fromServerJson(JsonParser p, RoomLocationEntry parent) throws JsonParseException, IOException {
		NoiseEntry entry = new NoiseEntry();
		entry.setParent(parent);

		while(p.nextToken() != JsonToken.END_OBJECT){
			String fieldName = p.getCurrentName();

			if(KEY_NOISE_LEVEL.equals(fieldName)){
				p.nextToken();
				entry.setNoise(p.getDoubleValue());
			} else if(KEY_TIMESTAMP.equals(fieldName)){
				p.nextToken();
				entry.setTime(p.getLongValue());
			}
		}

		entry.setLocal(false);
		return entry;
	}

	public void writeJsonIfLocal(JsonGenerator g) throws JsonGenerationException, IOException{
		if(this.is_local){
			g.writeStartObject();
			g.writeFieldName(KEY_NOISE_LEVEL);
			g.writeNumber(this.noise);
			g.writeFieldName(KEY_TIMESTAMP);
			g.writeNumber(this.timestamp);
			g.writeEndObject();
		}
	}
}
