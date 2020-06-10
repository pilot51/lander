package com.pilot51.lander

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import java.util.*

class Database(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
	override fun onCreate(db: SQLiteDatabase) {
		db.execSQL(CREATE_TBL_MAPS)
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

	init {
		if (instance != null) {
			Log.w(Main.TAG, "Database already initialized!")
		} else {
			instance = this
		}
	}

	companion object {
		/** @return Previously initialized static instance of this class. */
		private var instance: Database? = null
		private const val DB_VERSION = 1
		private const val DB_NAME = "database.db"
		private const val TABLE_NAME = "maps"
		private const val COLUMN_NAME = "name"
		private const val COLUMN_PLOT = "plot"
		private const val CREATE_TBL_MAPS = ("create table if not exists " + TABLE_NAME + "(" + BaseColumns._ID
				+ " integer primary key autoincrement, " + COLUMN_NAME + " text not null, "
				+ COLUMN_PLOT + " text not null);")

		/** @return A new ArrayList containing all maps from the database. */
		@Synchronized
		fun getMaps(): ArrayList<Ground> {
			val db = instance!!.readableDatabase
			val cursor = db.query(TABLE_NAME, null, null, null, null, null,
					"$COLUMN_NAME COLLATE NOCASE, $COLUMN_PLOT COLLATE NOCASE")
			val list = ArrayList<Ground>()
			while (cursor.moveToNext()) {
				list.add(Ground(
					cursor.getString(cursor.getColumnIndex(COLUMN_NAME)),
					cursor.getString(cursor.getColumnIndex(COLUMN_PLOT))
				))
			}
			cursor.close()
			db.close()
			return list
		}

		/**
		 * Clears and sets all maps in database.
		 * @param list The list of maps to add in the database.
		 */
		@Synchronized
		fun setMaps(list: ArrayList<Ground>) {
			val db = instance!!.writableDatabase
			db.delete(TABLE_NAME, null, null)
			var values: ContentValues
			list.forEach { map ->
				values = ContentValues()
				values.put(COLUMN_NAME, map.name)
				values.put(COLUMN_PLOT, map.getPlotString())
				db.insert(TABLE_NAME, null, values)
			}
			db.close()
		}

		/**
		 * Adds map to database.
		 * @param map The map to add to database.
		 */
		@Synchronized
		fun addMap(map: Ground) {
			val values = ContentValues()
			values.put(COLUMN_NAME, map.name)
			values.put(COLUMN_PLOT, map.getPlotString())
			val db = instance!!.writableDatabase
			db.insert(TABLE_NAME, null, values)
			db.close()
		}

		/**
		 * Updates map in database matching oldMap or adds if no match found.
		 * @param oldMap The old map to match in the database.
		 * @param newMap The updated map to put in the database.
		 */
		@Synchronized
		fun updateMap(oldMap: Ground?, newMap: Ground) {
			val values = ContentValues()
			values.put(COLUMN_NAME, newMap.name)
			values.put(COLUMN_PLOT, newMap.getPlotString())
			val db = instance!!.writableDatabase
			if (oldMap == null || db.update(TABLE_NAME, values, "$COLUMN_NAME = ? and $COLUMN_PLOT = ?",
					arrayOf(oldMap.name, oldMap.getPlotString())) == 0) {
				db.insert(TABLE_NAME, null, values)
			}
			db.close()
		}

		/**
		 * Removes map from database matching map name.
		 * @param map The map to remove from the database.
		 */
		@Synchronized
		fun removeMap(map: Ground) {
			val db = instance!!.writableDatabase
			db.delete(TABLE_NAME, "$COLUMN_NAME = ? and $COLUMN_PLOT = ?", arrayOf(map.name, map.getPlotString()))
			db.close()
		}
	}
}
