package com.pilot51.lander

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class OptionsMods : PreferenceFragmentCompat() {
	override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
		addPreferencesFromResource(R.xml.options_mods)
	}
}
