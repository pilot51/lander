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

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.pilot51.lander.Platform.Resources.drawable
import com.pilot51.lander.Platform.Resources.string
import com.pilot51.lander.Platform.prefs
import com.pilot51.lander.Res.ResImage
import com.pilot51.lander.Res.ResString

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
			Toast.makeText(context, ResString.CONTROLS_NOKEYS.string, Toast.LENGTH_LONG).show()
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
			prefs.edit()
				.putInt("KeyThrust", KeyEvent.KEYCODE_DPAD_DOWN)
				.putInt("KeyLeft", KeyEvent.KEYCODE_DPAD_LEFT)
				.putInt("KeyRight", KeyEvent.KEYCODE_DPAD_RIGHT)
				.putInt("KeyNew", KeyEvent.KEYCODE_2)
				.putInt("KeyRestart", KeyEvent.KEYCODE_3)
				.putInt("KeyOptions", KeyEvent.KEYCODE_4)
				.apply()
			requireActivity().setResult(1)
			Toast.makeText(preference.context, ResString.CONTROLS_RESET.string, Toast.LENGTH_LONG).show()
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
		AlertDialog.Builder(ctx)
			.setIcon(ResImage.ICON.drawable)
			.setMessage("${ResString.CURRENT_BUTTON.string} ${selectedPref.title}:\n" +
				"${getKeyLabel()}\n\n${ResString.PRESS_NEW_BUTTON.string}")
			.setNegativeButton(ResString.CANCEL.string) { dialog, _ -> dialog.cancel() }
			.setOnKeyListener(this)
			.create().show()
	}

	override fun onKey(dialog: DialogInterface, keyCode: Int, event: KeyEvent): Boolean {
		if (event.deviceId == 0 && event.action == KeyEvent.ACTION_UP) {
			prefs.edit().putInt(selectedPref.key, keyCode).apply()
			requireActivity().setResult(1)
			dialog.dismiss()
			return true
		}
		return false
	}

	private fun getKeyLabel(): String {
		val keyCharacterMap = KeyCharacterMap.load(0)
		val savedKey = prefs.getInt(selectedPref.key, 0)
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
