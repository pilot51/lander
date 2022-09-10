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
