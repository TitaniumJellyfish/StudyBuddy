package com.titaniumjellyfish.studybuddy.database;


import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.CloseableIterator;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.stmt.DeleteBuilder;
import com.j256.ormlite.stmt.PreparedDelete;
import com.j256.ormlite.stmt.QueryBuilder;
import com.j256.ormlite.stmt.Where;
import com.j256.ormlite.table.DatabaseTable;
import com.titaniumjellyfish.studybuddy.AngryJulienException;
import com.titaniumjellyfish.studybuddy.TimeUtils;
//import com.julien.blanchet.cs65.databasetestapp.Globals;

@DatabaseTable
public class RoomLocationEntry implements Parcelable{

	public static Parcelable.Creator<RoomLocationEntry> CREATOR =
			new RoomLocationParcelCreator();

	@DatabaseField(generatedId=true)
	private long id; // phone-specific
	
	public final static String COLUMN_NAME_LATITUDE = "latitude";
	@DatabaseField
	private double latitude;
	public final static String COLUMN_NAME_LONGTITUDE = "longitude";
	@DatabaseField
	private double longitude;
	public final static String COLUMN_NAME_BUILDING_NAME = "buildingName";
	@DatabaseField(uniqueCombo=true, columnName=COLUMN_NAME_BUILDING_NAME)
	private String buildingName;
	public final static String COLUMN_NAME_ROOM_NAME = "roomName";
	@DatabaseField(uniqueCombo=true, columnName=COLUMN_NAME_ROOM_NAME)
	private String roomName;
	public final static String COLUMN_NAME_PRODUCTIVITY = "productivity";
	@DatabaseField(columnName = COLUMN_NAME_PRODUCTIVITY)
	private double productivity;
	public final static String COLUMN_NAME_CAPACITY = "capacity";
	@DatabaseField(columnName = COLUMN_NAME_CAPACITY)
	private double capacity;
	public final static String COLUMN_NAME_CROWD = "crowd";
	@DatabaseField(columnName = COLUMN_NAME_CROWD)
	private double crowd;
	public final static String COLUMN_NAME_N_SURVEYS = "n_surveys";
	@DatabaseField(columnName = COLUMN_NAME_N_SURVEYS)
	private int n_surveys;
	public final static String COLUMN_NAME_MY_PRODUCIVITY = "my_productivity";
	@DatabaseField(columnName = COLUMN_NAME_MY_PRODUCIVITY)
	public double my_productivity;
	public final static String COLUMN_NAME_MY_CAPACITY = "my_capacity";
	@DatabaseField(columnName = COLUMN_NAME_MY_CAPACITY)
	public double my_capacity;
	public final static String COLUMN_NAME_MY_CROWD = "my_crowd";
	@DatabaseField(columnName = COLUMN_NAME_MY_CROWD)
	public double my_crowd;
	public final static String COLUMN_NAME_MY_N_SURVEYS = "my_n_surveys";
	@DatabaseField(columnName = COLUMN_NAME_MY_N_SURVEYS)
	public int my_n_surveys;
	public final static String COLUMN_NAME_IS_LOCAL = "is_local";
	@DatabaseField(columnName = COLUMN_NAME_IS_LOCAL)
	private boolean is_local;
	

	//	@ForeignCollectionField
	//	private ForeignCollection<CrowdEntry> crowdEntries;
	@ForeignCollectionField
	private ForeignCollection<NoiseEntry> noiseEntries;
	@ForeignCollectionField
	private ForeignCollection<CommentEntry> commentEntries;


	public RoomLocationEntry(){
		id = -1L;
		latitude = 0.0D;
		longitude = 0.0D;
		buildingName = "";
		roomName = "";
		productivity = Globals.PRODUCTIVITY_AVERAGE;
		capacity = Globals.CAPACITY_SINGLE;
		crowd = Globals.CROWD_LOW;
	}

	/**
	 * Get the average productivity for this room (includes both data from allgomerate and personal sources)
	 * @return The average productivity
	 */
	public double getProductivity() {
		return (productivity*n_surveys + my_productivity*my_n_surveys) / (n_surveys + my_n_surveys);
	}
	
	/**
	 * Set the alloglomerate productivity
	 * @param productivity
	 */
	public void setProductivity(double productivity) {
		this.productivity = productivity;
	}

	//	public ForeignCollection<CrowdEntry> getCrowdEntries(){
	//		return crowdEntries;
	//	}
	
	/**
	 * Get all noiseentries associated with this room
	 * @return
	 */
	public ForeignCollection<NoiseEntry> getNoiseEntries(){
		return noiseEntries;
	}
	
	/**
	 * Get all comments associated with this room
	 * @return
	 */
	public ForeignCollection<CommentEntry> getCommentEntries(){
		return commentEntries;
	}
	
	/**
	 * Get average capacity for this room using both agglomerate and personal data
	 * @return
	 */
	public double getCapacity() {
		return (capacity*n_surveys + my_capacity*my_n_surveys) / (n_surveys + my_n_surveys);
	}

	/**
	 * Set agglomerate capacity
	 * @param capacity
	 */
	public void setCapacity(double capacity) {
		this.capacity = capacity;
	}
	
	/**
	 * Get full name as is <Building Name> <Room Name>. Space included!
	 * @return
	 */
	public String getFullName(){
		return getBuildingName() + " " + getRoomName();
	}
	
	/**
	 * Get average crowdness data, including from agglomerate sources
	 * @return
	 */
	public double getCrowd() {
		return (crowd*n_surveys + my_crowd*my_n_surveys) / (n_surveys + my_n_surveys);
	}
	
	
	public double getAglCrowd(){
		return crowd;
	}
	public double getMyCrowd(){
		return my_crowd;
	}
	public void setMyCrowd(double crowd){
		this.my_crowd = crowd;
	}
	public void setAglCrowd(double crowd) {
		this.crowd = crowd;
	}

	public long getId() {
		return id;
	}
	//	public void setId(long id) {
	//		this.id = id;
	//	}
	public double getLatitude() {
		return latitude;
	}
	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}
	public double getLongitude() {
		return longitude;
	}
	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}
	public String getBuildingName() {
		return buildingName;
	}
	public void setBuildingName(String buildingName) {
		this.buildingName = buildingName;
	}
	public String getRoomName() {
		return roomName;
	}
	public void setRoomName(String roomName) {
		this.roomName = roomName;
	}
	// Local-ness
	public boolean getLocal(){
		return is_local;
	}

	public void setLocal(boolean local){
		this.is_local = local;
	}
	// Num surveys
	public int getTotalSurveys(){
		return n_surveys + my_n_surveys;
	}
	public void setAglSurveys(int s){
		this.n_surveys = s;
	}
	public void setMySurveys(int s){
		this.my_n_surveys = s;
	}
	



	// Stuff for the parcelable interface
	@Override
	public int describeContents() {
		// no special flags
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeDouble(latitude);
		dest.writeDouble(longitude);
		dest.writeString(buildingName);
		dest.writeString(roomName);
		dest.writeDouble(productivity);
		dest.writeDouble(capacity);
		dest.writeDouble(crowd);
		dest.writeByte(is_local ? (byte) 1 : (byte) 0);
	}

	private static class RoomLocationParcelCreator implements Parcelable.Creator<RoomLocationEntry>{

		@Override
		public RoomLocationEntry createFromParcel(Parcel source) {
			RoomLocationEntry mEntry = new RoomLocationEntry();
			mEntry.setLatitude(source.readDouble());
			mEntry.setLongitude(source.readDouble());
			mEntry.setBuildingName(source.readString());
			mEntry.setRoomName(source.readString());
			mEntry.setProductivity(source.readInt());
			mEntry.setCapacity(source.readInt());
			mEntry.setAglCrowd(source.readInt());
			mEntry.setLocal(source.readByte() == (byte) 1);
			return mEntry;
		}

		@Override
		public RoomLocationEntry[] newArray(int size) {
			return new RoomLocationEntry[size];
		}

	}


	// CONSTANTS COPIED FROM ROOM.JAVA
	private static final String KEY_LATITUDE  	= "room_lat";
	private static final String KEY_LONGITUDE 	= "room_lng";
	private static final String KEY_BUILDING  	= "room_building";
	private static final String KEY_ROOM 		= "room_name";
	private static final String KEY_NOISE  		= "room_noise";
	private static final String KEY_INTERPOLATED = "room_noise_interp";
	private static final String KEY_RATING       = "room_rating";
	private static final String KEY_CAPACITY  	= "room_capacity";
	private static final String KEY_CROWD 		= "room_crowd";
	private static final String KEY_NUM_SURVEYS  = "num_surveys";
	private static final String KEY_COMMENTS		= "room_comments";

	public static class RoomLocationReturn{
		public RoomLocationEntry entry;
		public List<NoiseEntry> noises;
		public List<CommentEntry> comments;
	}
	
	
	public static RoomLocationReturn fromServerJson(JsonParser p) throws JsonParseException, IOException{

		RoomLocationReturn ret = new RoomLocationReturn();
		
		RoomLocationEntry entry = new RoomLocationEntry();

		while(p.nextToken() != JsonToken.END_OBJECT){

			String fieldName = p.getCurrentName();
			if(KEY_BUILDING.equals(fieldName)){
				p.nextToken();
				entry.setBuildingName(p.getText());
			} else if(KEY_ROOM.equals(fieldName)){
				p.nextToken();
				entry.setRoomName(p.getText());
			} else if(KEY_LATITUDE.equals(fieldName)){
				p.nextToken();
				entry.setLatitude(p.getDoubleValue());
			} else if(KEY_LONGITUDE.equals(fieldName)){
				p.nextToken();
				entry.setLongitude(p.getDoubleValue());
			} else if(KEY_RATING.equals(fieldName)){
				p.nextToken();
				// We assume that rating <--> productivity (they are interchangeable)
				entry.setProductivity(p.getDoubleValue());
			} else if(KEY_CAPACITY.equals(fieldName)){
				p.nextToken();
				entry.setCapacity(p.getDoubleValue());
			} else if(KEY_CROWD.equals(fieldName)){
				p.nextToken();
				entry.setAglCrowd(p.getDoubleValue());
			} else if (KEY_NUM_SURVEYS.equals(fieldName)){
				p.nextToken();
				entry.setAglSurveys(p.getIntValue());

			} else if(KEY_INTERPOLATED.equals(fieldName)){
				p.nextToken(); // take the "[" token
				List<NoiseEntry> noises = new ArrayList<NoiseEntry>();
				while(p.nextToken() != JsonToken.END_ARRAY){
					noises.add(NoiseEntry.fromServerJson(p, entry));
				}
				ret.noises = noises;
			} else if(KEY_COMMENTS.equals(fieldName)){
				p.nextToken(); // take the "[" token
				List<CommentEntry> comments = new ArrayList<CommentEntry>();
				while(p.nextToken() != JsonToken.END_ARRAY){
					comments.add(CommentEntry.fromServerJson(p, entry));
				}
				ret.comments = comments;
			} 
		}
		entry.setLocal(false);
		
		ret.entry = entry;
		
		return ret;
	}

	public void writeJsonIfLocal(JsonGenerator g) throws JsonGenerationException, IOException{
		if(this.is_local){
			g.writeStartObject();
			g.writeFieldName(KEY_LATITUDE);
			g.writeNumber(this.latitude);
			g.writeFieldName(KEY_LONGITUDE);
			g.writeNumber(this.longitude);
			g.writeFieldName(KEY_BUILDING);
			g.writeString(this.buildingName);
			g.writeFieldName(KEY_ROOM);
			g.writeString(this.roomName);
			g.writeFieldName(KEY_RATING);
			g.writeNumber(this.my_productivity);
			g.writeFieldName(KEY_CAPACITY);
			g.writeNumber(this.my_capacity);
			g.writeFieldName(KEY_CROWD);
			g.writeNumber(this.my_crowd);
			g.writeFieldName(KEY_NUM_SURVEYS);
			g.writeNumber(this.my_n_surveys);
			g.writeFieldName(KEY_COMMENTS);
			g.writeStartArray();
			
			CloseableIterator<CommentEntry> commIterator = this.commentEntries.closeableIterator();
			while(commIterator.hasNext())
				commIterator.next().writeJsonIfLocal(g);
			try {
				commIterator.close();
			} catch (SQLException e1) {
				e1.printStackTrace();
			}
			g.writeEndArray();
			g.writeFieldName(KEY_NOISE);
			g.writeStartArray();
			
			CloseableIterator<NoiseEntry> noiseIterator = this.noiseEntries.closeableIterator();
			while(noiseIterator.hasNext())
				noiseIterator.next().writeJsonIfLocal(g);
			try {
				noiseIterator.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			g.writeEndArray();
			g.writeEndObject();
		}
	}
	
//	public List<NoiseEntry> getDayNoises(TitaniumDb helper) throws SQLException{
//		Dao<NoiseEntry, Long> dao = helper.dao(NoiseEntry.class,  Long.class);
//		QueryBuilder<NoiseEntry, Long> qb = dao.queryBuilder();
//		long curTime = System.currentTimeMillis();
//		
//		qb.where
//		
//		
//		return null;
//	}
	
	
	public double getCurrentNoise(TitaniumDb helper) throws SQLException, AngryJulienException{
		Dao<NoiseEntry, Long> dao = helper.dao(NoiseEntry.class,  Long.class);
		QueryBuilder<NoiseEntry, Long> qb = dao.queryBuilder();
		long curTime = System.currentTimeMillis();
		
		double weekAgo = 0.0, dayAgo=0.0, hourAgo=0.0;
		
		
		//// WEEK AGO
		int count = 0;
		qb.where().eq(NoiseEntry.COLUMN_NAME_PARENT_ID, this.id).and().between(NoiseEntry.COLUMN_NAME_TIMESTAMP, curTime - TimeUtils.WEEK - TimeUtils.HOUR, curTime - TimeUtils.WEEK + TimeUtils.HOUR);
		CloseableIterator<NoiseEntry> it = qb.iterator();
		while (it.hasNext()){
			weekAgo += it.next().getNoise();
			count ++;
		}
		it.close();
		if (count == 0){
			weekAgo = -1.1;
		}else{
			weekAgo /= count;
		}
		
		
		/// DAY AGO
		count = 0;
		qb = dao.queryBuilder();
		qb.where().eq(NoiseEntry.COLUMN_NAME_PARENT_ID, this.id).and().between(NoiseEntry.COLUMN_NAME_TIMESTAMP, curTime - TimeUtils.DAY - TimeUtils.HOUR, curTime - TimeUtils.DAY + TimeUtils.HOUR);
		it = qb.iterator();
		while (it.hasNext()){
			dayAgo += it.next().getNoise();
			count ++;
		}
		it.close();
		if (count == 0){
			dayAgo = -1.1;
		}else{
			dayAgo /= count;
		}
		
		//// HOUR AGO
		count = 0;
		qb = dao.queryBuilder();
		qb.where().eq(NoiseEntry.COLUMN_NAME_PARENT_ID, this.id).and().between(NoiseEntry.COLUMN_NAME_TIMESTAMP, curTime - TimeUtils.HOUR - (5 *TimeUtils.MINUTE), curTime - TimeUtils.HOUR + (5 *TimeUtils.MINUTE));
		it = qb.iterator();
		while (it.hasNext()){
			hourAgo += it.next().getNoise();
			count ++;
		}
		it.close();
		if (count == 0){
			hourAgo = -1.1;
		}else{
			hourAgo /= count;
		}
		
		double time = 0;
		count = 0;
		if (dayAgo != -1.1){
			count++;
			time += dayAgo;
		}
		if (weekAgo != -1.1){
			count++;
			time += weekAgo;
		}
		if (hourAgo != -1.1){
			count++;
			time += hourAgo;
		}
		
		if (count > 0){
			return time / count;
		}
		
		time = 0.0;
		count = 0;
		
		it = noiseEntries.closeableIterator();
		while (it.hasNext()){
			time += it.next().getNoise();
			count ++;
		}
		
		if (count > 0)
			return time / count;
		throw new AngryJulienException("I am so mad at this point b/c you ain't got anything");
	}
	
	public void clearLocal(Dao<NoiseEntry, Long> noiseDao, Dao<CommentEntry, Long> commDao) {
		this.is_local = false;
		this.my_capacity = 0.0;
		this.my_crowd = 0.0;
		this.my_productivity = 0.0;
		this.my_n_surveys = 0;
		try {
			// delete local noise data
			DeleteBuilder<NoiseEntry, Long> deletion = noiseDao.deleteBuilder();
			deletion.where().eq(NoiseEntry.COLUMN_NAME_IS_LOCAL, Boolean.TRUE);
			noiseDao.delete(deletion.prepare());
			// delete local comment data
			DeleteBuilder<CommentEntry, Long> deletion2 = commDao.deleteBuilder();
			deletion2.where().eq(CommentEntry.COLUMN_NAME_IS_LOCAL, Boolean.TRUE);
			commDao.delete(deletion2.prepare());
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
