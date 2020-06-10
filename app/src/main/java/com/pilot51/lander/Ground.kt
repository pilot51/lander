package com.pilot51.lander

class Ground(var name: String = "", var plot: IntArray? = null) {
	constructor(name: String, plot: String?) : this(name) {
		this.plot = setPlot(plot)
	}

	fun set(name: String, plot: IntArray?) {
		this.name = name
		this.plot = plot
	}

	fun set(name: String, plot: String?) {
		this.name = name
		setPlot(plot)
	}

	private fun setPlot(data: String?): IntArray? {
		if (data == null) {
			plot = null
			return null
		}
		val sMap = data.split(" ").toTypedArray()
		plot = IntArray(sMap.size)
		for (i in sMap.indices) {
			plot!![i] = sMap[i].toInt()
		}
		return plot
	}

	fun getPlotString(): String? {
		if (plot == null) return null
		var str = String()
		for (i in plot!!.indices) {
			if (i > 0) str += " "
			str += plot!![i]
		}
		return str
	}

	/** @return true if map data can be loaded, otherwise false.
	 */
	fun isValid() = plot?.size ?: 0 >= 2

	fun clear() {
		name = ""
		plot = null
	}

	companion object {
		var current = Ground()
	}
}
