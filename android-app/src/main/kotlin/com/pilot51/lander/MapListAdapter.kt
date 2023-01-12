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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView

class MapListAdapter(
	private val context: Context,
	private var data: List<Ground>
) : BaseAdapter(), Filterable {
	private var filter: SimpleFilter? = null
	private var unfilteredData: List<Ground>? = null

	override fun getCount() = data.size

	override fun getItem(position: Int): Any {
		return data[position]
	}

	override fun getItemId(position: Int): Long {
		return position.toLong()
	}

	override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
		val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
		val view = convertView ?: inflater.inflate(R.layout.map_list_item, parent, false)
		return view.apply {
			val ground = data[position]
			findViewById<TextView>(R.id.map_name).text = ground.name
			findViewById<TextView>(R.id.map_plot).text = ground.plotString
		}
	}

	override fun getFilter(): Filter {
		return filter ?: SimpleFilter().also { filter = it }
	}

	private inner class SimpleFilter : Filter() {
		override fun performFiltering(prefix: CharSequence): FilterResults {
			val results = FilterResults()
			if (unfilteredData == null) unfilteredData = ArrayList(data)
			if (prefix.isEmpty()) {
				val list = unfilteredData!!
				results.values = list
				results.count = list.size
			} else {
				val prefixString = prefix.toString().lowercase()
				val newValues = ArrayList<Ground>(unfilteredData!!.size)
				var map: Ground
				for (i in unfilteredData!!.indices) {
					map = unfilteredData!![i]
					if (map.name.lowercase().contains(prefixString)
						|| map.plotString.contains(prefixString)) newValues.add(map)
				}
				results.values = newValues
				results.count = newValues.size
			}
			return results
		}

		override fun publishResults(constraint: CharSequence, results: FilterResults) {
			@Suppress("UNCHECKED_CAST")
			data = results.values as List<Ground>
			if (results.count > 0) notifyDataSetChanged() else notifyDataSetInvalidated()
		}
	}
}
