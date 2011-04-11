package com.pilot51.lander;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.widget.Toast;

public class Options extends PreferenceActivity implements OnPreferenceClickListener, OnKeyListener {
	private SharedPreferences prefs;
	private Preference
		pDefault,
		pKeyThrust,
		pKeyLeft,
		pKeyRight,
		pKeyNew,
		pKeyRestart,
		selectedPref;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.options);
		
		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		pDefault = (Preference)findPreference("Default");
		pKeyThrust = (Preference)findPreference("KeyThrust");
		pKeyLeft = (Preference)findPreference("KeyLeft");
		pKeyRight = (Preference)findPreference("KeyRight");
		pKeyNew = (Preference)findPreference("KeyNew");
		pKeyRestart = (Preference)findPreference("KeyRestart");

		pDefault.setOnPreferenceClickListener(this);
		pKeyThrust.setOnPreferenceClickListener(this);
		pKeyLeft.setOnPreferenceClickListener(this);
		pKeyRight.setOnPreferenceClickListener(this);
		pKeyNew.setOnPreferenceClickListener(this);
		pKeyRestart.setOnPreferenceClickListener(this);
	}
	
	public boolean onPreferenceClick(Preference preference) {
		if (preference == pDefault) {
			Context context = getBaseContext();
			prefs.edit().clear().commit();
			PreferenceManager.setDefaultValues(context, R.xml.options, true);
			prefs.edit()
			.putInt(pKeyThrust.getKey(), KeyEvent.KEYCODE_DPAD_DOWN)
			.putInt(pKeyLeft.getKey(), KeyEvent.KEYCODE_DPAD_LEFT)
			.putInt(pKeyRight.getKey(), KeyEvent.KEYCODE_DPAD_RIGHT)
			.putInt(pKeyNew.getKey(), KeyEvent.KEYCODE_2)
			.putInt(pKeyRestart.getKey(), KeyEvent.KEYCODE_3)
			.commit();
			finish();
			startActivity(getIntent());
			Toast.makeText(context, R.string.options_reset, Toast.LENGTH_LONG).show();
			return true;
		} else if (preference.getKey().startsWith("Key")) {
			selectedPref = preference;
			setControl();
		}
		return true;
	}
	
	private void setControl() {
		new AlertDialog.Builder(this).setIcon(getResources().getDrawable(R.drawable.icon))
		.setMessage(getString(R.string.current_button) + " " + selectedPref.getTitle() + ":\n" + getKeyLabel(prefs.getInt(selectedPref.getKey(), 0)) + "\n\n" + getString(R.string.press_new_button))
		.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		})
		.setOnKeyListener(this)
		.create().show();
	}
	
	@Override
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		if (event.getDeviceId() == 0) {
			if (event.getAction() == KeyEvent.ACTION_DOWN) {
				prefs.edit().putInt(selectedPref.getKey(), keyCode).commit();
				getKeyLabel(keyCode);
			} else if (event.getAction() == KeyEvent.ACTION_UP) {
				dialog.dismiss();
			}
			return true;
		}
		return false;
	}
	
	private String getKeyLabel(int keyCode) {
		KeyCharacterMap keyCharacterMap = KeyCharacterMap.load(0);
		String label;
		int savedKey = prefs.getInt(selectedPref.getKey(), 0);
		if (keyCharacterMap.isPrintingKey(savedKey)) {
			label = Character.toString(keyCharacterMap.getDisplayLabel(savedKey));
		} else if (savedKey <= 6) {
			label = (new String[] { "UNKNOWN", "SOFT LEFT", "SOFT RIGHT", "HOME", "BACK", "CALL", "ENDCALL" })[savedKey];
		} else if (savedKey >= 19 & savedKey <= 28) {
			label = (new String[] { "UP", "DOWN", "LEFT", "RIGHT", "CENTER", "VOLUME UP", "VOLUME DOWN", "POWER", "CAMERA", "CLEAR" })[savedKey - 19];
		} else if (savedKey >= 57 & savedKey <= 67) {
			label = (new String[] { "LEFT ALT", "RIGHT ALT", "LEFT SHIFT", "RIGHT SHIFT", "TAB", "SPACE", "SYM", "EXPLORER", "ENVELOPE", "ENTER", "DEL" })[savedKey - 57];
		} else if (savedKey >= 78 & savedKey <= 91) {
			label = (new String[] { "NUM", "HEADSETHOOK", "FOCUS", "PLUS", "MENU", "NOTIFICATION", "SEARCH", "PLAY/PAUSE", "STOP", "NEXT", "PREVIOUS", "REWIND", "FAST FORWARD", "MUTE" })[savedKey - 78];
		} else {
			label = "(UNKNOWN)";
		}
		return label;
	}
}
