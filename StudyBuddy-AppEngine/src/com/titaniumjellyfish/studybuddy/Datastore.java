package com.titaniumjellyfish.studybuddy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Transaction;

public class Datastore {
	private static final Logger log = Logger.getLogger(Datastore.class.getName());

	public static final String KIND_ROOM = "Room";
	public static final String KIND_NOISE_ENTRY = "NoiseEntry";
	public static final String KIND_DEVICE = "device";
	public static final String KIND_DEVICE_GROUP = "dev-group";
	
	public static final Key device_group = KeyFactory.createKey(KIND_DEVICE_GROUP, 1L);
	public static final Key room_group = KeyFactory.createKey(KIND_ROOM, 1L);
	
	// TODO - make sure we can get Rooms with ancestor queries so that they are strongly consistent.
	// 	I think this means we need to make some sort of super-type to be the parent of all room entries.
	//	Possible solution: make an entry for each building, and put rooms under the building type?
	//	Other possible solution: make a single dummy root entry that is the parent of everything else in the datastore.
	// TODO - migrate objects to use Objectify (DAO for Datastore): https://code.google.com/p/objectify-appengine/
	
	public static final DatastoreService datastore = 
			DatastoreServiceFactory.getDatastoreService();
	
	public static Key insert(Entity e){
		Transaction txn = datastore.beginTransaction();
		Key ekey = datastore.put(e);
		txn.commit();
		log.info("Datastore: inserted entity with key "+ekey + ": "+e);
		return ekey;
	}
	
	public static void remove(Key k){
		Transaction txn = datastore.beginTransaction();
		datastore.delete(k);
		txn.commit();
		log.info("Datastore: deleted entity with key "+k);
	}
	
	public static List<Entity> query(String kind, Key parent, Filter f){
		return query(kind, parent, f, null);
	}
	
	public static List<Entity> query(String kind, Key parent, Filter f, String sort_key){
		Query q = new Query(kind);
		if(sort_key != null) q.addSort(sort_key, SortDirection.ASCENDING);
		if(parent != null) q.setAncestor(parent);
		q.setFilter(f);
		PreparedQuery pq = datastore.prepare(q);
		return pq.asList(FetchOptions.Builder.withDefaults());
	}
	
	public static Map<Key, Entity> getAllEntites(List<Key> keys){
		// note - this is modeled after the datastore.get(keys) function, but we don't want
		//	errors when a key is invalid.. instead, we just set the Entity to null for
		//	invalid keys.
		Map<Key, Entity> ret  = new HashMap<Key, Entity>(keys.size());
		for(Key k : keys){
			try {
				ret.put(k, datastore.get(k));
			} catch (EntityNotFoundException e) {
				log.info("could not find entity with key "+k.toString());
				ret.put(k,  null);
			}
		}
		return ret;
	}

	public static void remove(Key[] dyingKeys) {
		Transaction txn = datastore.beginTransaction();
		datastore.delete(dyingKeys);
		txn.commit();
	}
}
