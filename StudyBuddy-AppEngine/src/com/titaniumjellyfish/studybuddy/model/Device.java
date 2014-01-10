package com.titaniumjellyfish.studybuddy.model;

import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.titaniumjellyfish.studybuddy.Datastore;

public class Device implements DatastoreEntity<Device> {

	private static Logger log = Logger.getLogger(Device.class.getName());
	
	private long id;
	private long last_updated;

	public static final String KEY_ID = "_id";
	public static final String KEY_LAST_UPDATED = "updated";
	
	private Device(){}
	
	public Device(long i, long u){
		id = i;
		last_updated = u;
	}
	
	public void justUpdated(){
		last_updated = System.currentTimeMillis() - 1000L;
		insertToDB(Datastore.device_group);
	}
	
	public long getLastUpdated() {
		return this.last_updated;
	}

	public long getId() {
		return this.id;
	}
	
	@Override
	public Entity asEntity(Key parentKey) {
		Entity ent = new Entity(Datastore.KIND_DEVICE, id, parentKey);
		ent.setProperty(KEY_ID, id);
		ent.setProperty(KEY_LAST_UPDATED, last_updated);
		return ent;
	}

	@Override
	public Device fromEntity(Entity e) {
		return new Device(
				(Long) e.getProperty(KEY_ID),
				(Long) e.getProperty(KEY_LAST_UPDATED));
	}

	@Override
	public void insertToDB(Key parentKey) {
		Datastore.insert(this.asEntity(parentKey));
	}
	
	public static Device getById(long id){
		Filter filter = FilterOperator.EQUAL.of(KEY_ID, id);
		List<Entity> qResults = Datastore.query(Datastore.KIND_DEVICE, Datastore.device_group, filter);
		if(qResults.size() == 1) return (new Device()).fromEntity(qResults.get(0));
		else{
			if(qResults.size() > 1) log.info("Device.getById("+id+") returned "+qResults.size()+" devices..");
			return null;
		}
	}
	
	public static long getNextAvailableId(){
		long rLong = 0L;
		do{
			rLong = (new Random()).nextLong();
		} while(rLong == -1 || Device.getById(rLong) != null);
		
		return rLong;
	}
}
