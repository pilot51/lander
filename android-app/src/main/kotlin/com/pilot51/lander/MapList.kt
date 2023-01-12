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

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.AdapterView.OnItemClickListener
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.Toast
import com.pilot51.lander.GroundUtils.set
import com.pilot51.lander.Platform.Resources.getString
import com.pilot51.lander.Platform.Resources.string
import com.pilot51.lander.Res.ResString

class MapList : Activity() {
	private lateinit var lv: ListView
	private lateinit var adapter: MapListAdapter
	private lateinit var list: MutableList<Ground>
	private lateinit var dao: DatabaseHelper.MapDao

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		dao = DatabaseHelper.getInstance(this).mapsDao()
		list = dao.getMaps().toMutableList()
		lv = ListView(this)
		setContentView(lv)
		lv.isTextFilterEnabled = true
		adapter = MapListAdapter(this, list)
		lv.adapter = adapter
		registerForContextMenu(lv)
		lv.onItemClickListener = OnItemClickListener { parent, _, position, _ ->
			val map = parent.getItemAtPosition(position) as Ground
			Toast.makeText(this@MapList, ResString.MAP_LOADED.getString(map.name), Toast.LENGTH_SHORT).show()
			setResult(1)
			GroundUtils.currentGround = map
			finish()
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.map_list_menu, menu)
		return true
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.menu_save_current -> {
				editMap(GroundUtils.currentGround)
				return true
			}
			R.id.menu_create -> {
				editMap()
				return true
			}
			R.id.menu_clear -> {
				list.clear()
				dao.clear()
				adapter.notifyDataSetChanged()
				Toast.makeText(this, ResString.MAPS_CLEARED.string, Toast.LENGTH_SHORT).show()
				return true
			}
		}
		return false
	}

	override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo)
		menuInflater.inflate(R.menu.map_list_context, menu)
	}

	override fun onContextItemSelected(item: MenuItem): Boolean {
		val info = item.menuInfo as AdapterContextMenuInfo
		val map = lv.getItemAtPosition(info.position) as Ground
		when (item.itemId) {
			R.id.ctxtEdit -> editMap(map)
			R.id.ctxtRem -> {
				list.remove(map)
				dao.removeMap(map)
				adapter.notifyDataSetChanged()
				Toast.makeText(this, ResString.MAP_REMOVED.getString(map.name), Toast.LENGTH_SHORT).show()
			}
			else -> return super.onContextItemSelected(item)
		}
		return true
	}

	private fun sortList() {
		list.sortWith { map1, map2 -> map1.name.compareTo(map2.name, true) }
	}

	private fun editMap(selectedMap: Ground? = null) {
		val dlgView = (getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
			.inflate(R.layout.edit_map, null) as LinearLayout
		val editName = dlgView.findViewById<View>(R.id.editMapName) as EditText
		val editPlot = dlgView.findViewById<View>(R.id.editMapPlot) as EditText
		val title = when {
			selectedMap == null -> ResString.DLG_CREATE_MAP.string
			list.contains(selectedMap) -> ResString.DLG_EDIT_MAP.string
			else -> ResString.DLG_SAVE_MAP.string
		}
		val map = selectedMap ?: Ground()
		editName.setText(map.name)
		editPlot.setText(map.plotString)
		AlertDialog.Builder(this@MapList)
			.setView(dlgView)
			.setTitle(title)
			.setPositiveButton(ResString.OK.string) { _, _ ->
				val newPlot = GroundUtils.convertPlot(editPlot.text.toString())
				if (GroundUtils.isPlotValid(newPlot)) {
					map.set(editName.text.toString(), newPlot)
					if (list.contains(map)) dao.updateMap(map)
					else {
						list.add(map)
						dao.addMap(map)
					}
					sortList()
					adapter.notifyDataSetChanged()
					Toast.makeText(this@MapList, ResString.MAP_SAVED.getString(map.name), Toast.LENGTH_SHORT).show()
				} else {
					Toast.makeText(this@MapList, ResString.INVALID_PLOT.string, Toast.LENGTH_SHORT).show()
				}
			}
			.setNegativeButton(ResString.CANCEL.string) { dialog, _ -> dialog.cancel() }
			.show()
	}
}
