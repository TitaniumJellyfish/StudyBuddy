package com.titaniumjellyfish.studybuddy.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

public interface JSONSerializable<T> {
	
	/**
	 * Convert from JSON to object of type T
	 * @param json
	 * @return
	 */
	public T fromJSON(String json);
	
	public T fromJSON(JSONObject json) throws JSONException;
	
	/**
	 * Serialize this object to a JSON string
	 * @return
	 */
	public JSONObject toJSON();
	
}
