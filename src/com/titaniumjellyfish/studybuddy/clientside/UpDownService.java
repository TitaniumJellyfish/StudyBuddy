package com.titaniumjellyfish.studybuddy.clientside;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.http.HttpStatus;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.titaniumjellyfish.studybuddy.clientside.HttpHelper.HttpDataWriter;
import com.titaniumjellyfish.studybuddy.clientside.HttpHelper.HttpResponseHandler;
import com.titaniumjellyfish.studybuddy.clientside.UpDownService.OnFinishListener;
import com.titaniumjellyfish.studybuddy.database.CommentEntry;
import com.titaniumjellyfish.studybuddy.database.Globals;
import com.titaniumjellyfish.studybuddy.database.NoiseEntry;
import com.titaniumjellyfish.studybuddy.database.RoomLocationEntry;
import com.titaniumjellyfish.studybuddy.database.RoomLocationEntry.RoomLocationReturn;
import com.titaniumjellyfish.studybuddy.database.TitaniumDb;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class UpDownService extends IntentService {

	public boolean is_running = false;
	private OnFinishListener listener;

	public UpDownService(){
		this(UpDownService.class.getName());
	}

	public UpDownService(String name) {
		super(name);
	}

	TitaniumDb db;

	private static final JsonFactory jFactory = new JsonFactory();
	private long id = 0L;
	private boolean id_set = false;

	@Override
	public IBinder onBind(Intent arg0) {
		return null; // new ServiceBinder();
	}

	TitaniumDb getHelper(){
		if (db == null){
			db = OpenHelperManager.getHelper(this, TitaniumDb.class);
		}
		return db;
	}

	@Override
	public void onDestroy(){
		OpenHelperManager.releaseHelper();
		Log.d(Globals.LOGTAG, "Destroying updownservice");
		is_running = false;
		if(listener != null)
			listener.onFinish();
		super.onDestroy();
	}

	public void syncDatastore(){
		if(!id_set){
			SharedPreferences prefs = getSharedPreferences(Globals.PREFS_SERVER, Context.MODE_PRIVATE);
			id = prefs.getLong(Globals.PARAM_DEVICE_ID, -1L);
			id_set = true;
		}

		uploadToDatastore();

		downloadFromDatastore();
	}

	private void uploadToDatastore(){
		try {
			postRoomData();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void downloadFromDatastore(){
		try {
			// handle large json response (in second parameter?)
			HttpResponseHandler handler = new HttpResponseHandler() {
				@Override
				public void handle(int status, InputStream inStream) {

					try {
						// parse json response stream
						JsonParser p = jFactory.createParser(inStream);
						Dao<RoomLocationEntry, Long> roomDao = getHelper().dao(RoomLocationEntry.class, Long.class);
						final Dao<NoiseEntry, Long> noiseDao = getHelper().dao(NoiseEntry.class, Long.class);
						final Dao<CommentEntry, Long> commDao = getHelper().dao(CommentEntry.class, Long.class);
						if(p.nextToken() == JsonToken.START_OBJECT){
							while(p.nextToken() != JsonToken.END_OBJECT){
								String fieldName = p.getCurrentName();
								if(Globals.PARAM_DEVICE_ID.equals(fieldName)){
									p.nextToken();
									long new_id = p.getLongValue();
									if(new_id != id){
										id = new_id;
										SharedPreferences prefs = getSharedPreferences(Globals.PREFS_SERVER, Context.MODE_PRIVATE);
										SharedPreferences.Editor editor = prefs.edit();
										editor.putLong(Globals.PARAM_DEVICE_ID, new_id);
										editor.commit();
									}
								} else if(Globals.PARAM_JSON_DATA.equals(fieldName)){
									p.nextToken(); // take the ']'
									while(p.nextToken() != JsonToken.END_ARRAY){
										final RoomLocationReturn retval = RoomLocationEntry.fromServerJson(p);
										try {
											roomDao.createOrUpdate(retval.entry);
											noiseDao.callBatchTasks(new Callable<Void>(){
												@Override
												public Void call() throws Exception {
													for(NoiseEntry ne : retval.noises)
														noiseDao.create(ne);
													return null;
												}

											});
											commDao.callBatchTasks(new Callable<Void>(){
												@Override
												public Void call() throws Exception {
													for(CommentEntry ce : retval.comments)
														commDao.create(ce);
													return null;
												}

											});
										} catch (SQLException e) {

											e.printStackTrace();
										} catch (Exception e) {
											e.printStackTrace();
										}
									}
								}
							}

						} else{

						}

					} catch (JsonParseException e) {

					} catch (IOException e) {

					} catch (SQLException e){

					}
				}
			};

			// write data: device id in params
			HttpDataWriter writer = new HttpDataWriter() {
				@Override
				public void write(OutputStream outStream) {
					outStream = new StreamTap(outStream);
					try {
						String param_dev_id = Globals.PARAM_DEVICE_ID + "=" + String.valueOf(id); 
						outStream.write(param_dev_id.getBytes());

						((StreamTap) outStream).flushTap();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};

			HttpHelper.postWithHandlers(Globals.SERVER_URL + "/download", writer, handler);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void postRoomData() throws IOException {

		// do nothing with response
		HttpResponseHandler handler = new HttpResponseHandler() {
			@Override
			public void handle(int status, InputStream inStream) {
				// if the server's response is OK, it means that the server has accepted our data upload.
				//	In this case, we should delete local data so we don't end up uploading it again.
				if(status == HttpStatus.SC_OK){
					try {
						Dao<RoomLocationEntry, Long> roomDao = getHelper().dao(RoomLocationEntry.class, Long.class);
						Dao<NoiseEntry, Long> noiseDao = getHelper().dao(NoiseEntry.class, Long.class);
						Dao<CommentEntry, Long> commDao = getHelper().dao(CommentEntry.class, Long.class);
						List<RoomLocationEntry> all_rooms;
						all_rooms = roomDao.queryForAll();
						for(RoomLocationEntry rle : all_rooms){
							rle.clearLocal(noiseDao, commDao);
							roomDao.update(rle);
						}
					} catch (SQLException e1) {
						e1.printStackTrace();
					}
				}
			}
		};
		// write data: json stream writing
		HttpDataWriter writer = new HttpDataWriter() {
			@Override
			public void write(OutputStream outStream) {
				outStream = new StreamTap(outStream);
				String param_type = Globals.PARAM_UPLOAD_TYPE + "=" + Globals.UPLOAD_TYPE_ROOM;
				try {
					outStream.write(param_type.getBytes());

					String param_data = "&" + Globals.PARAM_JSON_DATA + "=";
					outStream.write(param_data.getBytes());
					//					outStream.flush();

					JsonGenerator g = jFactory.createGenerator(outStream);
					writeAllLocalDataJson(g);
					g.flush();
					g.close();

					((StreamTap) outStream).flushTap();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		};

		HttpHelper.postWithHandlers(Globals.SERVER_URL + "/upload", writer, handler);
	}

	private List<RoomLocationEntry> writeAllLocalDataJson(JsonGenerator g) {
		try {
			Dao<RoomLocationEntry, Long> roomDao = getHelper().dao(RoomLocationEntry.class, Long.class);
			List<RoomLocationEntry> all_rooms = roomDao.queryForAll();
			g.writeStartArray();
			for(RoomLocationEntry entry : all_rooms){
				entry.writeJsonIfLocal(g);
				//				g.flush();
			}
			g.writeEndArray();
			return all_rooms;
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		is_running = true;

		syncDatastore();
		stopSelf();

	}

	public class ServiceBinder extends Binder{
		public UpDownService getService(){
			return UpDownService.this;
		}
	}

	public interface OnFinishListener {
		public void onFinish();
	}

	private class StreamTap extends OutputStream{

		private OutputStream realstream;
		String content = "";

		public StreamTap(OutputStream s){
			realstream = s;
		}

		@Override
		public void write(int b) throws IOException {
			content += (char) b;
			realstream.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException{
			for(byte c : b) content += (char) c;
			realstream.write(b);
		}

		@Override
		public void flush() throws IOException{
			realstream.flush();
		}

		@Override
		public void close() throws IOException{
			realstream.close();
		}

		public void flushTap(){

			content = "";
		}

	}

	public void setOnFinishListener(OnFinishListener onFinishListener) {
		listener = onFinishListener;
	}

}
