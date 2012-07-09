package com.pilot51.lander;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class MapListAdapter extends BaseAdapter implements Filterable {
	private ArrayList<Ground> data = new ArrayList<Ground>();
	private int mResource = R.layout.map_list_item;
	private LayoutInflater mInflater;
	private SimpleFilter mFilter;
	private ArrayList<Ground> mUnfilteredData;

	public MapListAdapter(Context context, ArrayList<Ground> list) {
		data = list;
		mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public int getCount() {
		return data.size();
	}

	public Object getItem(int position) {
		return data.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		View view;
		if (convertView == null)
			view = mInflater.inflate(mResource, parent, false);
		else view = convertView;
		((TextView)view.findViewById(R.id.map_name)).setText(data.get(position).getName());
		((TextView)view.findViewById(R.id.map_plot)).setText(data.get(position).getPlotString());
		return view;
	}

	public Filter getFilter() {
		if (mFilter == null)
			mFilter = new SimpleFilter();
		return mFilter;
	}

	public static interface ViewBinder {
		boolean setViewValue(View view, Object data, String textRepresentation);
	}

	private class SimpleFilter extends Filter {
		@Override
		protected FilterResults performFiltering(CharSequence prefix) {
			FilterResults results = new FilterResults();
			if (mUnfilteredData == null)
				mUnfilteredData = new ArrayList<Ground>(data);
			if (prefix == null || prefix.length() == 0) {
				ArrayList<Ground> list = mUnfilteredData;
				results.values = list;
				results.count = list.size();
			} else {
				String prefixString = prefix.toString().toLowerCase();
				ArrayList<Ground> newValues = new ArrayList<Ground>(mUnfilteredData.size());
				Ground map;
				for (int i = 0; i < mUnfilteredData.size(); i++) {
					map = mUnfilteredData.get(i);
					if (map.getName().toLowerCase().contains(prefixString)
						|| map.getPlotString().contains(prefixString))
							newValues.add(map);
				}
				results.values = newValues;
				results.count = newValues.size();
			}
			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			data = (ArrayList<Ground>) results.values;
			if (results.count > 0)
				notifyDataSetChanged();
			else notifyDataSetInvalidated();
		}
	}
}
