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
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat

class OptionsImprovements : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {
	private lateinit var pPresetImpClassic: Preference
	private lateinit var pPresetImproved: Preference
	private lateinit var pImpEndImg: CheckBoxPreference
	private lateinit var pImpLanderAlpha: CheckBoxPreference

	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.options_improvements)
		pPresetImpClassic = findPreference<Preference>("PresetImpClassic")!!
			.also { it.onPreferenceClickListener = this }
		pPresetImproved = findPreference<Preference>("PresetImproved")!!
			.also { it.onPreferenceClickListener = this }
		pImpEndImg = findPreference("ImpEndImg")!!
		pImpLanderAlpha = findPreference("ImpLanderAlpha")!!
	}

	override fun onPreferenceClick(preference: Preference): Boolean {
		if (preference === pPresetImpClassic) {
			pImpEndImg.isChecked = false
			pImpLanderAlpha.isChecked = false
			return true
		} else if (preference === pPresetImproved) {
			pImpEndImg.isChecked = true
			pImpLanderAlpha.isChecked = true
			return true
		}
		return false
	}
}
