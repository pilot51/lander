package com.pilot51.lander

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class OptionsControls : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener, DialogInterface.OnKeyListener {
	private lateinit var pDefControls: Preference
	private lateinit var pKeyThrust: Preference
	private lateinit var pKeyLeft: Preference
	private lateinit var pKeyRight: Preference
	private lateinit var pKeyNew: Preference
	private lateinit var pKeyRestart: Preference
	private lateinit var pKeyOptions: Preference
	private lateinit var selectedPref: Preference

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.options_controls)
		pDefControls = findPreference<Preference>("DefaultControls")!!
			.also { it.onPreferenceClickListener = this }
		if (resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS) {
			findPreference<Preference>("CatKeys")!!.isVisible = false
			Toast.makeText(context, R.string.controls_nokeys, Toast.LENGTH_LONG).show()
		} else {
			pKeyThrust = findPreference<Preference>("KeyThrust")!!
				.also { it.onPreferenceClickListener = this }
			pKeyLeft = findPreference<Preference>("KeyLeft")!!
				.also { it.onPreferenceClickListener = this }
			pKeyRight = findPreference<Preference>("KeyRight")!!
				.also { it.onPreferenceClickListener = this }
			pKeyNew = findPreference<Preference>("KeyNew")!!
				.also { it.onPreferenceClickListener = this }
			pKeyRestart = findPreference<Preference>("KeyRestart")!!
				.also { it.onPreferenceClickListener = this }
			pKeyOptions = findPreference<Preference>("KeyOptions")!!
				.also { it.onPreferenceClickListener = this }
		}
	}

	override fun onPreferenceClick(preference: Preference): Boolean {
		if (preference === pDefControls) {
			findPreference<SeekBarPreference>("BtnScale")!!.setToDefault()
			findPreference<SeekBarPreference>("BtnAlpha")!!.setToDefault()
			Main.prefs!!.edit()
				.putInt("KeyThrust", KeyEvent.KEYCODE_DPAD_DOWN)
				.putInt("KeyLeft", KeyEvent.KEYCODE_DPAD_LEFT)
				.putInt("KeyRight", KeyEvent.KEYCODE_DPAD_RIGHT)
				.putInt("KeyNew", KeyEvent.KEYCODE_2)
				.putInt("KeyRestart", KeyEvent.KEYCODE_3)
				.putInt("KeyOptions", KeyEvent.KEYCODE_4)
				.apply()
			requireActivity().setResult(1)
			Toast.makeText(preference.context, R.string.controls_reset, Toast.LENGTH_LONG).show()
			return true
		} else if (preference.key.startsWith("Key")) {
			selectedPref = preference
			assignKey()
			return true
		}
		return false
	}

	private fun assignKey() {
		val ctx = requireContext()
		AlertDialog.Builder(ctx).setIcon(ContextCompat.getDrawable(ctx, R.drawable.icon))
			.setMessage(getString(R.string.current_button) + " " + selectedPref.title + ":\n"
				+ getKeyLabel() + "\n\n" + getString(R.string.press_new_button)
			)
			.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.cancel() }
			.setOnKeyListener(this)
			.create().show()
	}

	override fun onKey(dialog: DialogInterface, keyCode: Int, event: KeyEvent): Boolean {
		if (event.deviceId == 0 && event.action == KeyEvent.ACTION_UP) {
			Main.prefs!!.edit().putInt(selectedPref.key, keyCode).commit()
			requireActivity().setResult(1)
			dialog.dismiss()
			return true
		}
		return false
	}

	private fun getKeyLabel(): String {
		val keyCharacterMap = KeyCharacterMap.load(0)
		val savedKey = Main.prefs!!.getInt(selectedPref.key, 0)
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
