/*
 * Copyright 2011-2023 Mark Injerd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
