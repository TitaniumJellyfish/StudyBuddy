package com.titaniumjellyfish.studybuddy;

public class SensorCollectionState {
	
	public enum Current {
		BAKERY,
		DONUT,
		JELLY
	}
	
	private boolean hasEnteredJelly;
	private boolean hasEnteredDonut;
	private boolean hasTakenSurvey;
	private Current current; 
	
	private boolean audioStarted;
	private long startTime;
	
	
	public SensorCollectionState() {
		
		current = Current.BAKERY;
		this.hasEnteredJelly = false;
		this.hasEnteredDonut = false;
		this.hasTakenSurvey = false;
		this.audioStarted = false;
		startTime = 0;
	}
	
	public void setHasEnteredJelly(boolean jelly) {
		hasEnteredJelly = jelly;
	}
	
	public boolean getEnteredJelly() {
		return hasEnteredJelly;
	}
	
	public  void setHasEnteredDonut(boolean donut) {
		hasEnteredDonut = donut;
	}
	
	public  boolean getEnteredDonut() {
		return hasEnteredDonut;
	}
	
	public  void setTakenSurvey(boolean survey) {
		hasTakenSurvey = survey;
	}
	
	public  boolean getTakenSurvey() {
		return hasTakenSurvey;
	}
	
	public  Current getState() {
		return current;
	}
	
	public  void setCurrent(Current state) {
		current = state;
	}
	
	public  boolean getAudioStarted() {
		return audioStarted;
	}
	
	public  void setAudioStarted(boolean isStarted) {
		audioStarted = isStarted;
	}
	
	public  long getStartTime() {
		return startTime;
	}
	
	public  void setStartTime(long start) {
		startTime = start;
	}
	
}