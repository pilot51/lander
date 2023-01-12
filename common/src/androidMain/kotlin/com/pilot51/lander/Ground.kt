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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.pilot51.lander.GroundUtils.convertPlot

@Entity(tableName = "maps")
actual data class Ground actual constructor(
	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "_id")
	actual val id: Int?,
	@ColumnInfo(name = "name")
	actual var name: String,
	@ColumnInfo(name = "plot")
	actual var plotString: String
) {
	@Ignore
	actual constructor(plot: IntArray) : this(plotString = convertPlot(plot))
}
