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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import androidx.preference.PreferenceManager
import com.pilot51.lander.Platform.Resources.Icon
import com.pilot51.lander.Platform.Resources.string
import com.pilot51.lander.Platform.prefs
import com.pilot51.lander.Res.ResImage
import com.pilot51.lander.Res.ResString

class Main : Activity() {
	/** A handle to the View in which the game is running.  */
	private lateinit var landerView: LanderView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		Platform.init(this)
		// Load default preferences from xml if not saved
		PreferenceManager.setDefaultValues(this, R.xml.options, true)
		PreferenceManager.setDefaultValues(this, R.xml.options_controls, true)
		PreferenceManager.setDefaultValues(this, R.xml.options_improvements, true)
		PreferenceManager.setDefaultValues(this, R.xml.options_mods, true)
		if (prefs.getInt("KeyThrust", 0) == 0) prefs.edit()
			.putInt("KeyThrust", KeyEvent.KEYCODE_DPAD_DOWN)
			.putInt("KeyLeft", KeyEvent.KEYCODE_DPAD_LEFT)
			.putInt("KeyRight", KeyEvent.KEYCODE_DPAD_RIGHT)
			.putInt("KeyNew", KeyEvent.KEYCODE_2)
			.putInt("KeyRestart", KeyEvent.KEYCODE_3)
			.putInt("KeyOptions", KeyEvent.KEYCODE_4)
			.apply()
		setContentView(R.layout.lander_layout)
		landerView = findViewById<LanderView>(R.id.lander).also {
			it.vm.textAlt.init(findViewById(R.id.valueAlt))
			it.vm.textVelX.init(findViewById(R.id.valueVelX))
			it.vm.textVelY.init(findViewById(R.id.valueVelY))
			it.vm.textFuel.init(findViewById(R.id.valueFuel))
			it.vm.btnThrust.init(findViewById(R.id.btnThrust), it)
			it.vm.btnLeft.init(findViewById(R.id.btnLeft), it)
			it.vm.btnRight.init(findViewById(R.id.btnRight), it)
			it.setBtnMod()
			it.vm.keyCodeNew = prefs.getInt("KeyNew", 0)
			it.vm.keyCodeRestart = prefs.getInt("KeyRestart", 0)
			it.vm.keyCodeOptions = prefs.getInt("KeyOptions", 0)
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {
		menu.findItem(R.id.menu_maps).isVisible = prefs.getBoolean("ModMapList", false)
		return super.onPrepareOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.menu_new -> {
				landerView.vm.gameNew()
				return true
			}
			R.id.menu_restart -> {
				landerView.vm.gameRestart()
				return true
			}
			R.id.menu_maps -> {
				landerView.vm.gameInactive()
				startActivityForResult(Intent(this, MapList::class.java), 2)
				return true
			}
			R.id.menu_options -> {
				landerView.vm.gameInactive()
				startActivityForResult(Intent(this, Options::class.java), 1)
				return true
			}
			R.id.menu_about -> {
				val byOldState = landerView.vm.byLanderState
				landerView.vm.gameInactive()
				Platform.showMessageDialog(
					icon = Icon(ResImage.ICON),
					title = "${ResString.ABOUT.string} ${ResString.APP_NAME.string} v${BuildConfig.VERSION_NAME}",
					message = ResString.ABOUT_TEXT.string,
					neutralStringRes = ResString.OK,
					onNeutral = {
						landerView.vm.byLanderState = byOldState
					}
				)
				return true
			}
		}
		return false
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
		landerView.vm.run {
			when (keyCode) {
				keyCodeNew -> gameNew()
				keyCodeRestart -> gameRestart()
				keyCodeOptions -> {
					gameInactive()
					startActivityForResult(Intent(this@Main, Options::class.java), 1)
				}
				else -> return super.onKeyDown(keyCode, event)
			}
			return true
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == 1) {
			if (resultCode == 1) {
				landerView.vm.run {
					keyCodeNew = prefs.getInt("KeyNew", 0)
					keyCodeRestart = prefs.getInt("KeyRestart", 0)
					keyCodeOptions = prefs.getInt("KeyOptions", 0)
				}
			}
			landerView.setBtnMod()
			landerView.vm.gameRestart()
		} else if (requestCode == 2) {
			landerView.vm.run {
				if (resultCode == 1) gameNew() else gameRestart()
			}
		}
	}
}
