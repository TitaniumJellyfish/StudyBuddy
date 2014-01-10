package com.titaniumjellyfish.studybuddy.database;

import java.sql.SQLException;
import java.util.ArrayList;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteCursorDriver;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteQuery;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.titaniumjellyfish.studybuddy.AngryJulienException;

/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
public class TitaniumDb extends OrmLiteSqliteOpenHelper {

	// name of the database file for your application -- change to something appropriate for your app
	private static final String DATABASE_NAME = "helloNoBase.db";
	// any time you make changes to your database objects, you may have to increase the database version
	private static final int DATABASE_VERSION = 19;

	private ArrayList<Dao<?, ?>> daos = new ArrayList<Dao<?, ?>>();

	
	private Context mContext;
	
	private static Class<?> [] TABLE_CLASSES = {
		RoomLocationEntry.class,
		NoiseEntry.class,
		CommentEntry.class,
		CrowdEntry.class
	};

	public TitaniumDb(Context context) {
		super(
			context,
			DATABASE_NAME,
			null,
			DATABASE_VERSION);
		mContext = context;
	}

	/**
	 * This is called when the database is first created. Usually you should call createTable statements here to create
	 * the tables that will store your data.
	 */
	@Override
	public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
		try {
			Log.i(this.getClass().getName(), "onCreate");

			for (Class<?> cls: TABLE_CLASSES){
				TableUtils.createTable(connectionSource, cls);
			}

		} catch (SQLException e) {
			Log.e(this.getClass().getName(), "Can't create database", e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
	 * the various data to match the new version number.
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
		try {
			Log.i(this.getClass().getName(), "onUpgrade");

			for (Class<?> cls: TABLE_CLASSES){
				TableUtils.dropTable(connectionSource, cls,  true);
			}
			
			
			boolean works = mContext.getSharedPreferences(Globals.PREFS_SERVER,  Context.MODE_PRIVATE)
						.edit().putLong(Globals.PARAM_DEVICE_ID,  -1).commit();
			
			if (!works)
				throw new AngryJulienException("CHANGING DEVICE ID DIDN'T WORK");

			onCreate(db, connectionSource);
		} catch (SQLException e) {
			Log.e(this.getClass().getName(), "Can't drop databases", e);
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public <T, I> Dao<T, I> dao(Class<T> entryCls, Class<I> entryIdCls) throws SQLException{
		//make sure the class is registered with this db helper
	
//		assert(TABLE_CLASSES.contains(entryCls));

		//if dao has already been created, return it
		for (Dao<?, ?> d : daos){
			if (d.getDataClass().equals(entryCls))

				return (Dao<T, I>) d;
		}

		//if dao hasn't been created, make it, add it, return it
		Dao<T, I> dao = getDao(entryCls);
		daos.add(dao);
		return dao;
	}

	/**
	 * Close the database connections and clear any cached DAOs.
	 */
	@Override
	public void close() {
		super.close();
		daos = null;
	}
}
