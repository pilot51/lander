package com.pilot51.lander;

import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class Database extends SQLiteOpenHelper {
	private static Database database;
	private static final int DB_VERSION = 1;
	private static final String
		DB_NAME = "database.db",
		TABLE_NAME = "maps",
		COLUMN_NAME = "name",
		COLUMN_PLOT = "plot",
		CREATE_TBL_MAPS = "create table if not exists " + TABLE_NAME + "(" + BaseColumns._ID
			+ " integer primary key autoincrement, " + COLUMN_NAME + " text not null, "
			+ COLUMN_PLOT + " text not null);";

	protected Database(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
		if (database != null) {
			Log.w(Main.TAG, "Database already initialized!");
			return;
		}
		database = this;
	}
	
	/** @return Previously initialized static instance of this class. */
	protected static Database getInstance() {
		return database;
	}
	
	/** @return A new ArrayList containing all maps from the database. */
	protected static synchronized ArrayList<Ground> getMaps() {
		SQLiteDatabase db = database.getReadableDatabase();
		Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null,
			COLUMN_NAME + " COLLATE NOCASE, " + COLUMN_PLOT + " COLLATE NOCASE");
		ArrayList<Ground> list = new ArrayList<Ground>();
		while (cursor.moveToNext()) {
			list.add(new Ground(
				cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
				cursor.getString(cursor.getColumnIndex(COLUMN_PLOT))));
		}
		cursor.close();
		db.close();
		return list;
	}
	
	/**
	 * Clears and sets all maps in database.
	 * @param list The list of maps to add in the database.
	 */
	protected static synchronized void setMaps(ArrayList<Ground> list) {
		SQLiteDatabase db = database.getWritableDatabase();
		db.delete(TABLE_NAME, null, null);
		ContentValues values;
		Ground map;
		for (int i = 0; i < list.size(); i++) {
			values = new ContentValues();
			map = list.get(i);
			values.put(COLUMN_NAME,  map.getName());
			values.put(COLUMN_PLOT,  map.getPlotString());
			db.insert(TABLE_NAME, null, values);
		}
		db.close();
	}
	
	/**
	 * Adds map to database.
	 * @param map The map to add to database.
	 */
	protected static synchronized void addMap(Ground map) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_NAME,  map.getName());
		values.put(COLUMN_PLOT,  map.getPlotString());
		SQLiteDatabase db = database.getWritableDatabase();
		db.insert(TABLE_NAME, null, values);
		db.close();
	}
	
	/**
	 * Updates map in database matching oldMap or adds if no match found.
	 * @param oldMap The old map to match in the database.
	 * @param newMap The updated map to put in the database.
	 */
	protected static synchronized void updateMap(Ground oldMap, Ground newMap) {
		ContentValues values = new ContentValues();
		values.put(COLUMN_NAME,  newMap.getName());
		values.put(COLUMN_PLOT,  newMap.getPlotString());
		SQLiteDatabase db = database.getWritableDatabase();
		if (oldMap == null || db.update(TABLE_NAME, values, COLUMN_NAME + " = ? and " + COLUMN_PLOT + " = ?",
				new String[] {oldMap.getName(), oldMap.getPlotString()}) == 0)
			db.insert(TABLE_NAME, null, values);
		db.close();
	}
	
	/**
	 * Removes map from database matching map name.
	 * @param map The map to remove from the database.
	 */
	protected static synchronized void removeMap(Ground map) {
		SQLiteDatabase db = database.getWritableDatabase();
		db.delete(TABLE_NAME, COLUMN_NAME + " = ? and " + COLUMN_PLOT + " = ?", new String[] {map.getName(), map.getPlotString()});
		db.close();
	}
	
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CREATE_TBL_MAPS);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}