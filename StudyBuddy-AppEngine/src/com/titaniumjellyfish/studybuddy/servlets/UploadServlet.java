package com.titaniumjellyfish.studybuddy.servlets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.labs.repackaged.org.json.JSONArray;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.titaniumjellyfish.studybuddy.ServerGlobals;
import com.titaniumjellyfish.studybuddy.model.NoiseMap;
import com.titaniumjellyfish.studybuddy.model.Room;

public class UploadServlet extends BaseServlet {

	private static final Logger log = Logger.getLogger(UploadServlet.class.getName());

	private static final long serialVersionUID = 2530859128675204650L;

	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp){
		String type = null;
		String json_content = null;
		Map<String, String[]> params = req.getParameterMap();
		for(Map.Entry<String, String[]> ent : params.entrySet()){
			if(ent.getKey().equals(ServerGlobals.PARAM_UPLOAD_TYPE)){
				String[] vals = ent.getValue();
				type = (vals != null && vals.length > 0 ? vals[0] : null);
				log.info("UT: type is " + type + " and " + (type instanceof String ? "is" : "is not")+ " a string");
			}
			else if(ent.getKey().equals(ServerGlobals.PARAM_JSON_DATA)){
				String[] vals = ent.getValue();
				json_content = (vals != null && vals.length > 0 ? vals[0] : null);
				log.info("JC: vals is " + vals[0] + " and " + (vals[0] instanceof String ? "is" : "is not")+ " a string");
			}
		}
		resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);		
		if(type == null){
			log.info("upload: no type given in param '"+ServerGlobals.PARAM_UPLOAD_TYPE+"'");
			noResponse(resp);
		}
		else if(json_content == null){
			log.info("upload: no data given with param '"+ServerGlobals.PARAM_JSON_DATA+"'");
			noResponse(resp);
		}
		else{
			try {
				JSONArray arr = new JSONArray(json_content);
				// insert a bunch of parentless noisemap entries
				if(type.equals(ServerGlobals.UPLOAD_TYPE_NOISE)){
					for(int i=0; i<arr.length(); i++){
						(new NoiseMap()).fromJSON(arr.getJSONObject(i)).insertToDB(null);
					}
				}
				// insert a bunch of room entries. see Room.java for how noisemap data should
				//	be included as a child of this room
				else if (type.equals(ServerGlobals.UPLOAD_TYPE_ROOM)){
					ArrayList<Room> room_list = new ArrayList<Room>();
					for(int i=0; i<arr.length(); i++){
						room_list.add((new Room()).fromJSON(arr.getJSONObject(i)));
					}
					Room.addNewData(room_list);
				}
				resp.setStatus(HttpServletResponse.SC_OK);
			} catch (JSONException e) {
				log.info("upload: error parsing json-array from content string: "+json_content);
				log.info(e.getMessage());
			}
		}
	}

	private void noResponse(HttpServletResponse resp){
		try {
			resp.getWriter().write("{}");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
