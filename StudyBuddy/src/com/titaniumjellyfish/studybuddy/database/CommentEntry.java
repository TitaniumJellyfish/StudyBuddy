package com.titaniumjellyfish.studybuddy.database;


import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import android.os.Parcel;
import android.os.Parcelable;

@DatabaseTable
public class CommentEntry implements Parcelable {
	public static final String EMPTY_COMMENT = "Insert comment";
	public static Parcelable.Creator<CommentEntry> CREATOR = new SensorParcelCreator();

	@DatabaseField(generatedId=true)
	private long id;
	public static final String COLUMN_NAME_COMMENT = "comment";
	@DatabaseField(columnName=COLUMN_NAME_COMMENT)
	private String comment;
	public static final String COLUMN_NAME_IS_LOCAL = "is_local";
	@DatabaseField(columnName = COLUMN_NAME_IS_LOCAL)
	private boolean is_local;
	//	@DatabaseField
	//	private long timestamp;
	public static final String PARENT_ID = "parent_id";
	@DatabaseField(foreign=true, foreignAutoRefresh = true, index=true, columnName=PARENT_ID)
	private RoomLocationEntry parent;

	public CommentEntry() {
		id = -1L;
		comment = new String(EMPTY_COMMENT);
		//		timestamp = System.currentTimeMillis();
	}

	//Setters and getters

	//ID
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	//comment
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;	
	}
	
	// Local-ness
	public boolean getLocal(){
		return is_local;
	}
	
	public void setLocal(boolean local){
		this.is_local = local;
	}
	
	public void setParent(RoomLocationEntry parent){
		this.parent = parent;
	}
	public RoomLocationEntry getParent(){
		return parent;
	}
	
	//Time
	//	public long getTime() {
	//		return timestamp;
	//	}
	//	
	//	public void setTime(long time) {
	//		this.timestamp = time;
	//	}


	private static class SensorParcelCreator implements Parcelable.Creator<CommentEntry> {

		@Override
		public CommentEntry createFromParcel(Parcel source) {
			CommentEntry entry = new CommentEntry();
			entry.setId(source.readLong());
			entry.setComment(source.readString());
			entry.setLocal(source.readByte() == (byte) 1);
			//			entry.setTime(source.readLong());
			return entry;
		}

		@Override
		public CommentEntry[] newArray(int size) {
			return new CommentEntry[size];
		}
	}
	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeLong(id);
		dest.writeString(comment);
		dest.writeByte(is_local ? (byte) 1 : (byte) 0);
		//		dest.writeLong(timestamp);
	}

	public static CommentEntry fromServerJson(JsonParser p, RoomLocationEntry parent) throws JsonParseException, IOException {
		CommentEntry entry = new CommentEntry();
		entry.setComment(p.getText());
		entry.setLocal(false);
		entry.parent = parent;
		return entry;
	}

	public void writeJsonIfLocal(JsonGenerator g) throws JsonGenerationException, IOException{
		if(this.is_local)
			g.writeString(this.comment);
	}


}