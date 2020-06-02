package com.pilot51.lander;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.widget.Toast;

public class Options extends PreferenceActivity implements OnPreferenceClickListener, OnKeyListener {
	private Preference
		pDefClassic,
		pDefControls,
		pControls,
		pKeyThrust,
		pKeyLeft,
		pKeyRight,
		pKeyNew,
		pKeyRestart,
		pKeyOptions,
		pPresetImpClassic,
		pPresetImproved,
		selectedPref;
	private CheckBoxPreference
		pImpEndImg,
		pImpLanderAlpha;
	private PreferenceScreen pScreenMod;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.options);
		pDefClassic = findPreference("DefaultClassic");
		pDefControls = findPreference("DefaultControls");
		pControls = findPreference("Controls");
		pKeyThrust = findPreference("KeyThrust");
		pKeyLeft = findPreference("KeyLeft");
		pKeyRight = findPreference("KeyRight");
		pKeyNew = findPreference("KeyNew");
		pKeyRestart = findPreference("KeyRestart");
		pKeyOptions = findPreference("KeyOptions");
		pPresetImpClassic = findPreference("PresetImpClassic");
		pPresetImproved = findPreference("PresetImproved");
		pImpEndImg = (CheckBoxPreference)findPreference("ImpEndImg");
		pImpLanderAlpha = (CheckBoxPreference)findPreference("ImpLanderAlpha");
		pScreenMod = (PreferenceScreen)findPreference("ScreenMod");
		pDefClassic.setOnPreferenceClickListener(this);
		pDefControls.setOnPreferenceClickListener(this);
		pControls.setOnPreferenceClickListener(this);
		pKeyThrust.setOnPreferenceClickListener(this);
		pKeyLeft.setOnPreferenceClickListener(this);
		pKeyRight.setOnPreferenceClickListener(this);
		pKeyNew.setOnPreferenceClickListener(this);
		pKeyRestart.setOnPreferenceClickListener(this);
		pKeyOptions.setOnPreferenceClickListener(this);
		pPresetImpClassic.setOnPreferenceClickListener(this);
		pPresetImproved.setOnPreferenceClickListener(this);
		pScreenMod.setOnPreferenceClickListener(this);
	}
	
	public boolean onPreferenceClick(Preference preference) {
		if (preference == pDefClassic) {
			((SeekBarPreference)findPreference("Gravity")).setToDefault();
			((SeekBarPreference)findPreference("Fuel")).setToDefault();
			((SeekBarPreference)findPreference("Thrust")).setToDefault();
			((CheckBoxPreference)findPreference("DrawFlame")).setChecked(true);
			((CheckBoxPreference)findPreference("ReverseSideThrust")).setChecked(false);
			Toast.makeText(this, R.string.classic_options_reset, Toast.LENGTH_LONG).show();
		} else if (preference == pDefControls) {
			((SeekBarPreference)findPreference("BtnScale")).setToDefault();
			((SeekBarPreference)findPreference("BtnAlpha")).setToDefault();
			Main.prefs.edit()
			.putInt("KeyThrust", KeyEvent.KEYCODE_DPAD_DOWN)
			.putInt("KeyLeft", KeyEvent.KEYCODE_DPAD_LEFT)
			.putInt("KeyRight", KeyEvent.KEYCODE_DPAD_RIGHT)
			.putInt("KeyNew", KeyEvent.KEYCODE_2)
			.putInt("KeyRestart", KeyEvent.KEYCODE_3)
			.putInt("KeyOptions", KeyEvent.KEYCODE_4)
			.commit();
			setResult(1);
			Toast.makeText(this, R.string.controls_reset, Toast.LENGTH_LONG).show();
		} else if (preference == pControls) {
			if (getResources().getConfiguration().keyboard == Configuration.KEYBOARD_NOKEYS) {
				findPreference("CatKeys").setEnabled(false);
				Toast.makeText(this, R.string.controls_nokeys, Toast.LENGTH_LONG).show();
			}
		} else if (preference.getKey().startsWith("Key")) {
			selectedPref = preference;
			assignKey();
		} else if (preference == pPresetImpClassic) {
			pImpEndImg.setChecked(false);
			pImpLanderAlpha.setChecked(false);
		} else if (preference == pPresetImproved) {
			pImpEndImg.setChecked(true);
			pImpLanderAlpha.setChecked(true);
		}
		return true;
	}
	
	private void assignKey() {
		new AlertDialog.Builder(this).setIcon(getResources().getDrawable(R.drawable.icon))
		.setMessage(getString(R.string.current_button) + " " + selectedPref.getTitle() + ":\n" + getKeyLabel(Main.prefs.getInt(selectedPref.getKey(), 0)) + "\n\n" + getString(R.string.press_new_button))
		.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		})
		.setOnKeyListener(this)
		.create().show();
	}
	
	@Override
	public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
		if (event.getDeviceId() == 0 & event.getAction() == KeyEvent.ACTION_UP) {
			Main.prefs.edit().putInt(selectedPref.getKey(), keyCode).commit();
			setResult(1);
			dialog.dismiss();
			return true;
		}
		return false;
	}
	
	private String getKeyLabel(int keyCode) {
		KeyCharacterMap keyCharacterMap = KeyCharacterMap.load(0);
		String label;
		int savedKey = Main.prefs.getInt(selectedPref.getKey(), 0);
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
