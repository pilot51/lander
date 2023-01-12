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

import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat
import com.pilot51.lander.Platform.Resources.string
import com.pilot51.lander.Res.ResString

class Options : FragmentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			supportFragmentManager.beginTransaction()
				.replace(android.R.id.content, OptionsFragment()).commit()
		}
	}

	class OptionsFragment : PreferenceFragmentCompat(), OnPreferenceClickListener {
		private lateinit var pDefClassic: Preference

		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			addPreferencesFromResource(R.xml.options)
			pDefClassic = findPreference<Preference>("DefaultClassic")!!
				.also { it.onPreferenceClickListener = this }
		}

		override fun onPreferenceClick(preference: Preference): Boolean {
			val ctx = requireContext()
			if (preference === pDefClassic) {
				findPreference<SeekBarPreference>("Gravity")!!.setToDefault()
				findPreference<SeekBarPreference>("Fuel")!!.setToDefault()
				findPreference<SeekBarPreference>("Thrust")!!.setToDefault()
				findPreference<CheckBoxPreference>("DrawFlame")!!.isChecked = true
				findPreference<CheckBoxPreference>("ReverseSideThrust")!!.isChecked = false
				Toast.makeText(ctx, ResString.CLASSIC_OPTIONS_RESET.string, Toast.LENGTH_LONG).show()
				return true
			}
			return false
		}
	}
}
