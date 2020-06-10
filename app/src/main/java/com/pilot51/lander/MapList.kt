package com.pilot51.lander

import android.app.AlertDialog
import android.app.ListActivity
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
import com.pilot51.lander.Database.Companion.addMap
import com.pilot51.lander.Database.Companion.getMaps
import com.pilot51.lander.Database.Companion.removeMap
import com.pilot51.lander.Database.Companion.setMaps
import com.pilot51.lander.Database.Companion.updateMap
import java.util.*

class MapList : ListActivity() {
	private lateinit var lv: ListView
	private lateinit var adapter: MapListAdapter
	private lateinit var list: ArrayList<Ground>

	public override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		Database(this)
		list = getMaps()
		lv = listView
		lv.isTextFilterEnabled = true
		adapter = MapListAdapter(this, list)
		lv.adapter = adapter
		registerForContextMenu(lv)
		lv.onItemClickListener = OnItemClickListener { parent, _, position, _ ->
			val map = parent.getItemAtPosition(position) as Ground
			Toast.makeText(this@MapList, getString(R.string.map_loaded, map.name), Toast.LENGTH_SHORT).show()
			setResult(1)
			Ground.current = map
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
				editMap(Ground.current)
				return true
			}
			R.id.menu_create -> {
				editMap(Ground())
				return true
			}
			R.id.menu_clear -> {
				list.clear()
				setMaps(list)
				adapter.notifyDataSetChanged()
				Toast.makeText(this, getString(R.string.maps_cleared), Toast.LENGTH_SHORT).show()
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
				removeMap(map)
				adapter.notifyDataSetChanged()
				Toast.makeText(this, getString(R.string.map_removed, map.name), Toast.LENGTH_SHORT).show()
			}
			else -> return super.onContextItemSelected(item)
		}
		return true
	}

	private fun sortList() {
		list.sortWith(Comparator { map1, map2 -> map1.name.compareTo(map2.name, true) })
	}

	private fun editMap(map: Ground) {
		val dlgView = (getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
			.inflate(R.layout.edit_map, null) as LinearLayout
		val editName = dlgView.findViewById<View>(R.id.editMapName) as EditText
		val editPlot = dlgView.findViewById<View>(R.id.editMapPlot) as EditText
		val title = when {
			list.contains(map) -> getString(R.string.dlg_edit_map)
			map.isValid() -> getString(R.string.dlg_save_map)
			else -> getString(R.string.dlg_create_map)
		}
		if (map.isValid()) {
			editName.setText(map.name)
			editPlot.setText(map.getPlotString())
		}
		AlertDialog.Builder(this@MapList)
			.setView(dlgView)
			.setTitle(title)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val oldMap = Ground(map.name, map.plot)
				map.set(editName.text.toString(), editPlot.text.toString())
				if (map.isValid()) {
					if (!list.contains(map)) {
						list.add(map)
						addMap(map)
					} else updateMap(oldMap, map)
					sortList()
					adapter.notifyDataSetChanged()
					Toast.makeText(this@MapList, getString(R.string.map_saved, map.name), Toast.LENGTH_SHORT).show()
				} else {
					map.set(oldMap.name, oldMap.plot)
					Toast.makeText(this@MapList, getString(R.string.invalid_plot), Toast.LENGTH_SHORT).show()
				}
			}
			.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
			.show()
	}
}
