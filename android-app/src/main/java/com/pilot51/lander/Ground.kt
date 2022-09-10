package com.pilot51.lander

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "maps")
data class Ground(
	@PrimaryKey(autoGenerate = true)
	@ColumnInfo(name = "_id")
	val id: Int? = null,
	@ColumnInfo(name = "name")
	var name: String = "",
	@ColumnInfo(name = "plot")
	var plotString: String = ""
) {
	val plotArray get() = convertPlot(plotString)

	@Ignore
	constructor(plot: IntArray) : this(plotString = convertPlot(plot))

	fun set(name: String, plot: IntArray) {
		this.name = name
		plotString = convertPlot(plot)
	}

	companion object {
		var current: Ground? = null

		fun convertPlot(plotString: String): IntArray {
			return plotString.split(" ").map { it.toInt() }.toIntArray()
		}

		fun convertPlot(plotArray: IntArray): String {
			return plotArray.joinToString(separator = " ")
		}

		fun isPlotValid(plot: IntArray): Boolean {
			return plot.size >= 2
		}
	}
}
