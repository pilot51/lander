package com.pilot51.lander;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

public class Main extends Activity {
	private int keyNew, keyRestart, keyOptions;
	public static final String TAG = "Lander";
	public static SharedPreferences prefs;
	protected static Resources res;
	
	/** A handle to the View in which the game is running. */
	private LanderView mLanderView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		res = getResources();
		// Load default preferences from xml if not saved
		PreferenceManager.setDefaultValues(this, R.xml.options, true);
		prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
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
		setContentView(R.layout.lander_layout);
		mLanderView = (LanderView) findViewById(R.id.lander);
		mLanderView.setTextViewAlt((TextView) findViewById(R.id.valueAlt));
		mLanderView.setTextViewVelX((TextView) findViewById(R.id.valueVelX));
		mLanderView.setTextViewVelY((TextView) findViewById(R.id.valueVelY));
		mLanderView.setTextViewFuel((TextView) findViewById(R.id.valueFuel));
		mLanderView.setButtonThrust((Button) findViewById(R.id.btnThrust));
		mLanderView.setButtonLeft((Button) findViewById(R.id.btnLeft));
		mLanderView.setButtonRight((Button) findViewById(R.id.btnRight));
		mLanderView.setBtnMod();
		keyNew = prefs.getInt("KeyNew", 0);
		keyRestart = prefs.getInt("KeyRestart", 0);
		keyOptions = prefs.getInt("KeyOptions", 0);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.findItem(R.id.menu_maps).setVisible(prefs.getBoolean("ModMapList", false));
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_new:
				Ground.current.clear();
				mLanderView.byLanderState = LanderView.LND_NEW;
				return true;
			case R.id.menu_restart:
				mLanderView.byLanderState = LanderView.LND_RESTART;
				return true;
			case R.id.menu_maps:
				mLanderView.byLanderState = LanderView.LND_INACTIVE;
				startActivityForResult(new Intent(this, MapList.class), 2);
				return true;
			case R.id.menu_options:
				mLanderView.byLanderState = LanderView.LND_INACTIVE;
				startActivityForResult(new Intent(this, Options.class), 1);
				return true;
			case R.id.menu_about:
				final byte byOldState = mLanderView.byLanderState;
				mLanderView.byLanderState = LanderView.LND_INACTIVE;
				new AlertDialog.Builder(this)
					.setIcon(getResources().getDrawable(R.drawable.icon))
					.setTitle(
						getString(R.string.about) + " " + getString(R.string.app_name) + " v"
							+ BuildConfig.VERSION_NAME).setMessage(R.string.about_text)
					.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
							mLanderView.byLanderState = byOldState;
						}
					}).create().show();
				return true;
		}
		return false;
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == keyNew) {
			Ground.current.clear();
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
		return super.onKeyDown(keyCode, event);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 1) {
			if (resultCode == 1) {
				keyNew = prefs.getInt("KeyNew", 0);
				keyRestart = prefs.getInt("KeyRestart", 0);
				keyOptions = prefs.getInt("KeyOptions", 0);
			}
			mLanderView.setBtnMod();
			mLanderView.byLanderState = LanderView.LND_RESTART;
		} else if (requestCode == 2) {
			if (resultCode == 1)
				mLanderView.byLanderState = LanderView.LND_NEW;
			else mLanderView.byLanderState = LanderView.LND_RESTART;
		}
	}
}