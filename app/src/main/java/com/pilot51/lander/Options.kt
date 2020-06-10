package com.pilot51.lander

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.preference.CheckBoxPreference
import android.preference.Preference
import android.preference.Preference.OnPreferenceClickListener
import android.preference.PreferenceActivity
import android.preference.PreferenceScreen
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.widget.Toast

class Options : PreferenceActivity(), OnPreferenceClickListener, DialogInterface.OnKeyListener {
	private var pDefClassic: Preference? = null
	private var pDefControls: Preference? = null
	private var pControls: Preference? = null
	private var pKeyThrust: Preference? = null
	private var pKeyLeft: Preference? = null
	private var pKeyRight: Preference? = null
	private var pKeyNew: Preference? = null
	private var pKeyRestart: Preference? = null
	private var pKeyOptions: Preference? = null
	private var pPresetImpClassic: Preference? = null
	private var pPresetImproved: Preference? = null
	private var selectedPref: Preference? = null
	private var pImpEndImg: CheckBoxPreference? = null
	private var pImpLanderAlpha: CheckBoxPreference? = null
	private var pScreenMod: PreferenceScreen? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		addPreferencesFromResource(R.xml.options)
		pDefClassic = findPreference("DefaultClassic").also { it.onPreferenceClickListener = this }
		pDefControls = findPreference("DefaultControls").also { it.onPreferenceClickListener = this }
		pControls = findPreference("Controls").also { it.onPreferenceClickListener = this }
		pKeyThrust = findPreference("KeyThrust").also { it.onPreferenceClickListener = this }
		pKeyLeft = findPreference("KeyLeft").also { it.onPreferenceClickListener = this }
		pKeyRight = findPreference("KeyRight").also { it.onPreferenceClickListener = this }
		pKeyNew = findPreference("KeyNew").also { it.onPreferenceClickListener = this }
		pKeyRestart = findPreference("KeyRestart").also { it.onPreferenceClickListener = this }
		pKeyOptions = findPreference("KeyOptions").also { it.onPreferenceClickListener = this }
		pPresetImpClassic = findPreference("PresetImpClassic").also { it.onPreferenceClickListener = this }
		pPresetImproved = findPreference("PresetImproved").also { it.onPreferenceClickListener = this }
		pImpEndImg = findPreference("ImpEndImg") as CheckBoxPreference
		pImpLanderAlpha = findPreference("ImpLanderAlpha") as CheckBoxPreference
		pScreenMod = (findPreference("ScreenMod") as PreferenceScreen).also { it.onPreferenceClickListener = this }
	}

	override fun onPreferenceClick(preference: Preference): Boolean {
		if (preference === pDefClassic) {
			(findPreference("Gravity") as SeekBarPreference).setToDefault()
			(findPreference("Fuel") as SeekBarPreference).setToDefault()
			(findPreference("Thrust") as SeekBarPreference).setToDefault()
			(findPreference("DrawFlame") as CheckBoxPreference).isChecked = true
			(findPreference("ReverseSideThrust") as CheckBoxPreference).isChecked = false
			Toast.makeText(this, R.string.classic_options_reset, Toast.LENGTH_LONG).show()
		} else if (preference === pDefControls) {
			(findPreference("BtnScale") as SeekBarPreference).setToDefault()
			(findPreference("BtnAlpha") as SeekBarPreference).setToDefault()
			Main.prefs!!.edit()
					.putInt("KeyThrust", KeyEvent.KEYCODE_DPAD_DOWN)
					.putInt("KeyLeft", KeyEvent.KEYCODE_DPAD_LEFT)
					.putInt("KeyRight", KeyEvent.KEYCODE_DPAD_RIGHT)
					.putInt("KeyNew", KeyEvent.KEYCODE_2)
					.putInt("KeyRestart", KeyEvent.KEYCODE_3)
					.putInt("KeyOptions", KeyEvent.KEYCODE_4)
					.commit()
			setResult(1)
			Toast.makeText(this, R.string.controls_reset, Toast.LENGTH_LONG).show()
		} else if (preference === pControls) {
			if (resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS) {
				findPreference("CatKeys").isEnabled = false
				Toast.makeText(this, R.string.controls_nokeys, Toast.LENGTH_LONG).show()
			}
		} else if (preference.key.startsWith("Key")) {
			selectedPref = preference
			assignKey()
		} else if (preference === pPresetImpClassic) {
			pImpEndImg!!.isChecked = false
			pImpLanderAlpha!!.isChecked = false
		} else if (preference === pPresetImproved) {
			pImpEndImg!!.isChecked = true
			pImpLanderAlpha!!.isChecked = true
		}
		return true
	}

	private fun assignKey() {
		AlertDialog.Builder(this).setIcon(resources.getDrawable(R.drawable.icon))
				.setMessage(getString(R.string.current_button) + " " + selectedPref!!.title + ":\n"
						+ getKeyLabel(Main.prefs!!.getInt(selectedPref!!.key, 0)) + "\n\n"
						+ getString(R.string.press_new_button)
				)
				.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
				.setOnKeyListener(this)
				.create().show()
	}

	override fun onKey(dialog: DialogInterface, keyCode: Int, event: KeyEvent): Boolean {
		if (event.deviceId == 0 && event.action == KeyEvent.ACTION_UP) {
			Main.prefs!!.edit().putInt(selectedPref!!.key, keyCode).commit()
			setResult(1)
			dialog.dismiss()
			return true
		}
		return false
	}

	private fun getKeyLabel(keyCode: Int): String {
		val keyCharacterMap = KeyCharacterMap.load(0)
		val savedKey = Main.prefs!!.getInt(selectedPref!!.key, 0)
		return when {
			keyCharacterMap.isPrintingKey(savedKey) -> keyCharacterMap.getDisplayLabel(savedKey).toString()
			savedKey <= 6 -> arrayOf("UNKNOWN", "SOFT LEFT", "SOFT RIGHT", "HOME", "BACK", "CALL", "ENDCALL")[savedKey]
			savedKey in 19..28 -> arrayOf("UP", "DOWN", "LEFT", "RIGHT", "CENTER", "VOLUME UP", "VOLUME DOWN",
					"POWER", "CAMERA", "CLEAR")[savedKey - 19]
			savedKey in 57..67 -> arrayOf("LEFT ALT", "RIGHT ALT", "LEFT SHIFT", "RIGHT SHIFT", "TAB", "SPACE",
					"SYM", "EXPLORER", "ENVELOPE", "ENTER", "DEL")[savedKey - 57]
			savedKey in 78..91 -> arrayOf("NUM", "HEADSETHOOK", "FOCUS", "PLUS", "MENU", "NOTIFICATION", "SEARCH",
					"PLAY/PAUSE", "STOP", "NEXT", "PREVIOUS", "REWIND", "FAST FORWARD", "MUTE")[savedKey - 78]
			else -> "(UNKNOWN)"
		}
	}
}
