package com.pilot51.lander

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem

class Main : Activity() {
	private var keyNew = 0
	private var keyRestart = 0
	private var keyOptions = 0

	/** A handle to the View in which the game is running.  */
	private lateinit var landerView: LanderView

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		// Load default preferences from xml if not saved
		PreferenceManager.setDefaultValues(this, R.xml.options, true)
		prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext).also {
			if (it.getInt("KeyThrust", 0) == 0) {
				it.edit()
						.putInt("KeyThrust", KeyEvent.KEYCODE_DPAD_DOWN)
						.putInt("KeyLeft", KeyEvent.KEYCODE_DPAD_LEFT)
						.putInt("KeyRight", KeyEvent.KEYCODE_DPAD_RIGHT)
						.putInt("KeyNew", KeyEvent.KEYCODE_2)
						.putInt("KeyRestart", KeyEvent.KEYCODE_3)
						.putInt("KeyOptions", KeyEvent.KEYCODE_4)
						.commit()
			}
			keyNew = it.getInt("KeyNew", 0)
			keyRestart = it.getInt("KeyRestart", 0)
			keyOptions = it.getInt("KeyOptions", 0)
		}
		setContentView(R.layout.lander_layout)
		landerView = findViewById<LanderView>(R.id.lander).also {
			it.setTextViewAlt(findViewById(R.id.valueAlt))
			it.setTextViewVelX(findViewById(R.id.valueVelX))
			it.setTextViewVelY(findViewById(R.id.valueVelY))
			it.setTextViewFuel(findViewById(R.id.valueFuel))
			it.setButtonThrust(findViewById(R.id.btnThrust))
			it.setButtonLeft(findViewById(R.id.btnLeft))
			it.setButtonRight(findViewById(R.id.btnRight))
			it.setBtnMod()
		}
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean {
		menuInflater.inflate(R.menu.menu, menu)
		return super.onCreateOptionsMenu(menu)
	}

	override fun onPrepareOptionsMenu(menu: Menu): Boolean {
		menu.findItem(R.id.menu_maps).isVisible = prefs!!.getBoolean("ModMapList", false)
		return super.onPrepareOptionsMenu(menu)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			R.id.menu_new -> {
				Ground.current.clear()
				landerView.byLanderState = LanderView.LND_NEW
				return true
			}
			R.id.menu_restart -> {
				landerView.byLanderState = LanderView.LND_RESTART
				return true
			}
			R.id.menu_maps -> {
				landerView.byLanderState = LanderView.LND_INACTIVE
				startActivityForResult(Intent(this, MapList::class.java), 2)
				return true
			}
			R.id.menu_options -> {
				landerView.byLanderState = LanderView.LND_INACTIVE
				startActivityForResult(Intent(this, Options::class.java), 1)
				return true
			}
			R.id.menu_about -> {
				val byOldState = landerView.byLanderState
				landerView.byLanderState = LanderView.LND_INACTIVE
				AlertDialog.Builder(this)
						.setIcon(resources.getDrawable(R.drawable.icon))
						.setTitle(getString(R.string.about) + " " + getString(R.string.app_name) + " v"
										+ BuildConfig.VERSION_NAME).setMessage(R.string.about_text)
						.setNeutralButton(android.R.string.ok) { dialog, _ ->
							dialog.cancel()
							landerView.byLanderState = byOldState
						}.create().show()
				return true
			}
		}
		return false
	}

	override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
		when (keyCode) {
			keyNew -> {
				Ground.current.clear()
				landerView.byLanderState = LanderView.LND_NEW
				return true
			}
			keyRestart -> {
				landerView.byLanderState = LanderView.LND_RESTART
				return true
			}
			keyOptions -> {
				landerView.byLanderState = LanderView.LND_INACTIVE
				startActivityForResult(Intent(this, Options::class.java), 1)
				return true
			}
			else -> return super.onKeyDown(keyCode, event)
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == 1) {
			if (resultCode == 1) {
				prefs!!.run {
					keyNew = getInt("KeyNew", 0)
					keyRestart = getInt("KeyRestart", 0)
					keyOptions = getInt("KeyOptions", 0)
				}
			}
			landerView.setBtnMod()
			landerView.byLanderState = LanderView.LND_RESTART
		} else if (requestCode == 2) {
			landerView.byLanderState = if (resultCode == 1) {
				LanderView.LND_NEW
			} else LanderView.LND_RESTART
		}
	}

	companion object {
		const val TAG = "Lander"
		@JvmField
		var prefs: SharedPreferences? = null
	}
}
