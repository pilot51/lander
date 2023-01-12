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

object GroundUtils{
	val Ground.plotArray get() = convertPlot(plotString)

	fun Ground.set(name: String, plot: IntArray) {
		this.name = name
		plotString = convertPlot(plot)
	}

	var currentGround: Ground? = null

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
