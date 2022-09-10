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
				editMap()
				return true
			}
			R.id.menu_clear -> {
				list.clear()
				dao.clear()
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
				dao.removeMap(map)
				adapter.notifyDataSetChanged()
				Toast.makeText(this, getString(R.string.map_removed, map.name), Toast.LENGTH_SHORT).show()
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
			selectedMap == null -> getString(R.string.dlg_create_map)
			list.contains(selectedMap) -> getString(R.string.dlg_edit_map)
			else -> getString(R.string.dlg_save_map)
		}
		val map = selectedMap ?: Ground()
		editName.setText(map.name)
		editPlot.setText(map.plotString)
		AlertDialog.Builder(this@MapList)
			.setView(dlgView)
			.setTitle(title)
			.setPositiveButton(android.R.string.ok) { _, _ ->
				val newPlot = Ground.convertPlot(editPlot.text.toString())
				if (Ground.isPlotValid(newPlot)) {
					map.set(editName.text.toString(), newPlot)
					if (list.contains(map)) dao.updateMap(map)
					else {
						list.add(map)
						dao.addMap(map)
					}
					sortList()
					adapter.notifyDataSetChanged()
					Toast.makeText(this@MapList, getString(R.string.map_saved, map.name), Toast.LENGTH_SHORT).show()
				} else {
					Toast.makeText(this@MapList, getString(R.string.invalid_plot), Toast.LENGTH_SHORT).show()
				}
			}
			.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
			.show()
	}
}
