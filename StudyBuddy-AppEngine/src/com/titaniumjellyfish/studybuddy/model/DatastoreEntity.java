package com.titaniumjellyfish.studybuddy.model;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;

public interface DatastoreEntity<T> {

	public Entity asEntity(Key parentKey);
	
	public T fromEntity(Entity e);
	
	public void insertToDB(Key parentKey);
	
}