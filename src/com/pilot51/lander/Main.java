package com.pilot51.lander;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

public class Main extends Activity {
	private static final int
		MENU_NEW = 1,
		MENU_RESTART = 2,
		MENU_OPTIONS = 3,
		MENU_ABOUT = 4;
	private int keyNew, keyRestart, keyOptions;
	
	/** A handle to the View in which the game is running. */
	private LanderView mLanderView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Fullscreen);
		super.onCreate(savedInstanceState);
		// Load default preferences from xml if not saved
		PreferenceManager.setDefaultValues(this, R.xml.options, true);
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (prefs.getInt("KeyThrust", 0) == 0) {
			prefs.edit()
			.putInt("KeyThrust", KeyEvent.KEYCODE_DPAD_DOWN)
			.putInt("KeyLeft", KeyEvent.KEYCODE_DPAD_LEFT)
			.putInt("KeyRight", KeyEvent.KEYCODE_DPAD_RIGHT)
			.putInt("KeyNew", KeyEvent.KEYCODE_2)
			.putInt("KeyRestart", KeyEvent.KEYCODE_3)
			.putInt("KeyOptions", KeyEvent.KEYCODE_4)
			.commit();
		}
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.lander_layout);
		mLanderView = (LanderView) findViewById(R.id.lander);
		mLanderView.setTextViewAlt((TextView) findViewById(R.id.valueAlt));
		mLanderView.setTextViewVelX((TextView) findViewById(R.id.valueVelX));
		mLanderView.setTextViewVelY((TextView) findViewById(R.id.valueVelY));
		mLanderView.setTextViewFuel((TextView) findViewById(R.id.valueFuel));
		mLanderView.setButtonThrust((Button) findViewById(R.id.btnThrust));
		mLanderView.setButtonLeft((Button) findViewById(R.id.btnLeft));
		mLanderView.setButtonRight((Button) findViewById(R.id.btnRight));
		keyNew = prefs.getInt("KeyNew", 0);
		keyRestart = prefs.getInt("KeyRestart", 0);
		keyOptions = prefs.getInt("KeyOptions", 0);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_NEW, 0, R.string.menu_new);
		menu.add(0, MENU_RESTART, 0, R.string.menu_restart);
		menu.add(0, MENU_OPTIONS, 0, R.string.menu_options);
		menu.add(0, MENU_ABOUT, 0, R.string.menu_about);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_NEW:
			mLanderView.byLanderState = LanderView.LND_NEW;
			return true;
		case MENU_RESTART:
			mLanderView.byLanderState = LanderView.LND_RESTART;
			return true;
		case MENU_OPTIONS:
			mLanderView.byLanderState = LanderView.LND_INACTIVE;
			startActivityForResult(new Intent(this, Options.class), 1);
			return true;
		case MENU_ABOUT:
			final byte byOldState = mLanderView.byLanderState;
			mLanderView.byLanderState = LanderView.LND_INACTIVE;
			//AboutDialog (hWnd);
		new AlertDialog.Builder(this)
			.setIcon(getResources().getDrawable(R.drawable.icon))
			.setTitle(getString(R.string.menu_about) + " " + getString(R.string.app_name) + " " + getString(R.string.app_version))
			.setMessage(R.string.about_text)
			.setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int which) {
	            	dialog.cancel();
	            	mLanderView.byLanderState = byOldState;
	            }})
			.create()
			.show();
			return true;
		}
		return false;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {
		if (keyCode == keyNew) {
			mLanderView.byLanderState = LanderView.LND_NEW;
			return true;
		} else if (keyCode == keyRestart) {
			mLanderView.byLanderState = LanderView.LND_RESTART;
			return true;
		} else if (keyCode == keyOptions) {
			mLanderView.byLanderState = LanderView.LND_INACTIVE;
			startActivityForResult(new Intent(this, Options.class), 1);
			return true;
		}
		return false;
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == 1)
			mLanderView.byLanderState = LanderView.LND_RESTART;
	}
}
