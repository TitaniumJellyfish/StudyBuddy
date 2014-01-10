package com.titaniumjellyfish.studybuddy.servlets;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.titaniumjellyfish.studybuddy.Datastore;
import com.titaniumjellyfish.studybuddy.ServerGlobals;
import com.titaniumjellyfish.studybuddy.model.Device;
import com.titaniumjellyfish.studybuddy.model.Room;

public class DownloadServlet extends BaseServlet {

	private static Logger log = Logger.getLogger(DownloadServlet.class.getName());

	private static final long serialVersionUID = -7435155733267644134L;

	@Override
	/** Format of JSON object returned is as follows:
	 * 	{ dev_id : <long>,
	 *    json_data   : [<list of rooms' data>]}
	 *    
	 * The device requesting this data should keep track of its own id and send it along with the request
	 * 	using the same key: dev_id 
	 * 
	 * This servlet will respond with data to update the device's database, but only new data since the last
	 * 	sync. Currently there is no way to force a full refresh except to send a new dummy id
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse resp){
		long dev_id = Long.parseLong(this.getParameter(req, ServerGlobals.PARAM_DEVICE_ID, "-1"));
		if(dev_id != -1){
			log.info("Download: looking up device with id " + dev_id);
			// Device exists, just update it
			Device dev = Device.getById(dev_id);
			if(dev == null) dev = new Device(Device.getNextAvailableId(), 0L);
			try{
				updateDevice(dev, resp);
				dev.justUpdated();
			} catch(IOException e){
				log.info("Download: problem sending data to "+dev_id);
			}
		} else{
			// Add device to database, then update
			Device dev = new Device(Device.getNextAvailableId(), 0L);
			try{
				updateDevice(dev, resp);
				dev.justUpdated();
			} catch(IOException e){
				log.info("Download: problem sending data to "+dev_id);
			}
		}
	}
	
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp){
		doGet(req, resp);
	}

	private void updateDevice(Device dev, HttpServletResponse resp) throws IOException{

		JsonFactory f = new JsonFactory();
		JsonGenerator g = f.createGenerator(resp.getOutputStream());

		if(dev != null){
			Filter filter = FilterOperator.GREATER_THAN_OR_EQUAL.of(Room.KEY_LAST_UPDATE, dev.getLastUpdated());
			List<Entity> qResults = Datastore.query(Datastore.KIND_ROOM, null, filter);
			log.info("sending updates for "+qResults.size()+" rooms");

			g.writeStartObject();
			g.writeFieldName(ServerGlobals.PARAM_DEVICE_ID);
			g.writeNumber(dev.getId());
			g.writeArrayFieldStart(ServerGlobals.PARAM_JSON_DATA);
			for(Entity e : qResults)
				(new Room()).fromEntity(e).writeToGenerator(g, Room.FLAG_INCLUDE_INTERPOLATED);
			g.writeEndArray();
			g.writeEndObject();

		} else{
			g.writeStartObject();
			g.writeEndObject();
		}
		g.flush();
		g.close();
	}
}
