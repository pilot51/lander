package com.pilot51.lander

import android.content.Context
import androidx.room.*

@Database(version = 1, entities = [Ground::class])
abstract class DatabaseHelper : RoomDatabase() {
	abstract fun mapsDao(): MapDao

	companion object {
		private const val DB_NAME = "database.db"
		private var INSTANCE: DatabaseHelper? = null

		fun getInstance(context: Context): DatabaseHelper {
			return INSTANCE ?: Room.databaseBuilder(
				context.applicationContext, DatabaseHelper::class.java, DB_NAME
			).allowMainThreadQueries().build().also { INSTANCE = it }
		}
	}

	@Dao
	interface MapDao {
		@Query("SELECT * FROM maps ORDER BY name COLLATE NOCASE, plot COLLATE NOCASE")
		fun getMaps(): List<Ground>

		@Insert
		fun addMap(map: Ground)

		@Insert(onConflict = OnConflictStrategy.REPLACE)
		fun updateMap(map: Ground)

		@Delete
		fun removeMap(map: Ground)

		@Query("DELETE FROM maps")
		fun clear()
	}
}
