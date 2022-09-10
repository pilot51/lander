package com.pilot51.lander

import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import androidx.preference.PreferenceFragmentCompat

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
		private lateinit var pControls: Preference

		override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
			addPreferencesFromResource(R.xml.options)
			pDefClassic = findPreference<Preference>("DefaultClassic")!!
				.also { it.onPreferenceClickListener = this }
			pControls = findPreference<Preference>("Controls")!!
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
				Toast.makeText(ctx, R.string.classic_options_reset, Toast.LENGTH_LONG).show()
				return true
			} else if (preference === pControls) {
				if (resources.configuration.keyboard == Configuration.KEYBOARD_NOKEYS) {
					findPreference<Preference>("CatKeys")!!.isEnabled = false
					Toast.makeText(ctx, R.string.controls_nokeys, Toast.LENGTH_LONG).show()
					return true
				}
			}
			return false
		}
	}
}
