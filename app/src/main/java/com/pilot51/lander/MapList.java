package com.pilot51.lander;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

public class MapList extends ListActivity {
	
	private ListView lv;
	private MapListAdapter adapter;
	private ArrayList<Ground> list;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		new Database(this);
		list = Database.getMaps();
		lv = getListView();
		lv.setTextFilterEnabled(true);
		adapter = new MapListAdapter(this, list);
		lv.setAdapter(adapter);
		registerForContextMenu(lv);
		lv.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				final Ground map = (Ground)parent.getItemAtPosition(position);
				Toast.makeText(MapList.this, getString(R.string.map_loaded, map.getName()), Toast.LENGTH_SHORT).show();
				MapList.this.setResult(1);
				Ground.current = map;
				finish();
			}
		});
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.map_list_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_save_current:
				editMap(Ground.current);
				return true;
			case R.id.menu_create:
				editMap(new Ground());
				return true;
			case R.id.menu_clear:
				list.clear();
				Database.setMaps(list);
				adapter.notifyDataSetChanged();
				Toast.makeText(MapList.this, getString(R.string.maps_cleared), Toast.LENGTH_SHORT).show();
				return true;
		}
		return false;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.map_list_context, menu);
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
		Ground map = (Ground)lv.getItemAtPosition(info.position);
		if (item.getItemId() == R.id.ctxtEdit) {
			editMap(map);
		} else if (item.getItemId() == R.id.ctxtRem) {
			list.remove(map);
			Database.removeMap(map);
			adapter.notifyDataSetChanged();
			Toast.makeText(MapList.this, getString(R.string.map_removed, map.getName()), Toast.LENGTH_SHORT).show();
		} else return super.onContextItemSelected(item);
		return true;
	}
	
	private void sortList() {
		Collections.sort(list, new Comparator<Ground>() {
			@Override
			public int compare(Ground map1, Ground map2) {
				return map1.getName().compareToIgnoreCase(map2.getName());
			}
		});
	}
	
	private void editMap(final Ground map) {
		LinearLayout dlgView =
			(LinearLayout)((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE))
			.inflate(R.layout.edit_map, null);
		final EditText editName = (EditText)dlgView.findViewById(R.id.editMapName),
			editPlot = (EditText)dlgView.findViewById(R.id.editMapPlot);
		final String title;
		if (list.contains(map))
			title = getString(R.string.dlg_edit_map);
		else if (map.isValid())
			title = getString(R.string.dlg_save_map);
		else title = getString(R.string.dlg_create_map);
		if (map.isValid()) {
			editName.setText(map.getName());
			editPlot.setText(map.getPlotString());
		}
		new AlertDialog.Builder(MapList.this)
		.setView(dlgView)
		.setTitle(title)
		.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
	    		Ground oldMap = new Ground(map.getName(), map.getPlot());
		    	map.set(editName.getText().toString(), editPlot.getText().toString());
		    	if (map.isValid()) {
			    	if (!list.contains(map)) {
			    		list.add(map);
			    		Database.addMap(map);
			    	} else Database.updateMap(oldMap, map);
			    	sortList();
					adapter.notifyDataSetChanged();
					Toast.makeText(MapList.this, getString(R.string.map_saved, map.getName()), Toast.LENGTH_SHORT).show();
		    	} else {
		    		map.set(oldMap.getName(), oldMap.getPlot());
		    		Toast.makeText(MapList.this, getString(R.string.invalid_plot), Toast.LENGTH_SHORT).show();
		    	}
		    }
		})
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int whichButton) {
		    	dialog.cancel();
		    }
		})
		.show();
	}
}
