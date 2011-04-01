package com.pilot51.lander;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceClickListener;
import android.widget.Toast;

public class Options extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	SharedPreferences prefs;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.options);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		((Preference)findPreference("Default")).setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				Context context = getBaseContext();
				PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
				PreferenceManager.setDefaultValues(context, R.xml.options, true);
				finish();
				startActivity(getIntent());
				Toast.makeText(context, "Options reset", Toast.LENGTH_LONG).show();
				return true;
			}
		});
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() {
		super.onPause();
		prefs.unregisterOnSharedPreferenceChangeListener(this);
	}
	
	public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
		if (key.equals("Gravity")) {
			try {
				Float.parseFloat(sp.getString("Gravity", null));
			} catch (Exception e) {
				sp.edit().putString("Gravity", "3").commit();
				((EditTextPreference)findPreference("Gravity")).setText("3");
				Toast.makeText(this, "Cannot leave Gravity blank.\nReset to default (3)", Toast.LENGTH_SHORT).show();
			}
		} else if (key.equals("Fuel")) {
			try {
				Integer.parseInt(sp.getString("Fuel", null));
			} catch (Exception e) {
				sp.edit().putString("Fuel", "1000").commit();
				((EditTextPreference)findPreference("Fuel")).setText("1000");
				Toast.makeText(this, "Cannot leave Fuel blank.\nReset to default (1000)", Toast.LENGTH_SHORT).show();
			}
		} else if (key.equals("Thrust")) {
			try {
				Integer.parseInt(sp.getString("Thrust", null));
			} catch (Exception e) {
				sp.edit().putString("Thrust", "10000").commit();
				((EditTextPreference)findPreference("Fuel")).setText("10000");
				Toast.makeText(this, "Cannot leave Thrust blank.\nReset to default (10000)", Toast.LENGTH_SHORT).show();
			}
		}
	}
}
