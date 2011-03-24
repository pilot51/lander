package com.pilot51.lander;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.pilot51.lander.LanderView.LanderThread;

public class Lander extends Activity {
	private static final int
		MENU_NEW = 1,
		MENU_RESTART = 2,
		MENU_OPTIONS = 3,
		MENU_ABOUT = 4;

	/** A handle to the thread that's actually running the animation. */
	private LanderThread mLanderThread;

	/** A handle to the View in which the game is running. */
	private LanderView mLanderView;

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
			mLanderThread.doStart();
			mLanderThread.setState(LanderThread.STATE_READY);
			return true;
		case MENU_RESTART:
			mLanderThread.doRestart();
			mLanderThread.setState(LanderThread.STATE_READY);
			return true;
		case MENU_OPTIONS:
			return true;
		case MENU_ABOUT:
			return true;
		}
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Fullscreen);
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.lander_layout);

		mLanderView = (LanderView) findViewById(R.id.lander);
		mLanderThread = mLanderView.getThread();

		mLanderView.setTextViewMain((TextView) findViewById(R.id.text));
		mLanderView.setTextViewAlt((TextView) findViewById(R.id.valueAlt));
		mLanderView.setTextViewVelX((TextView) findViewById(R.id.valueVelX));
		mLanderView.setTextViewVelY((TextView) findViewById(R.id.valueVelY));
		mLanderView.setTextViewFuel((TextView) findViewById(R.id.valueFuel));
		mLanderView.setButtonThrust((Button) findViewById(R.id.btnThrust));
		mLanderView.setButtonLeft((Button) findViewById(R.id.btnLeft));
		mLanderView.setButtonRight((Button) findViewById(R.id.btnRight));

		if (savedInstanceState == null) {
			// we were just launched: set up a new game
			mLanderThread.setState(LanderThread.STATE_READY);
		} else {
			// we are being restored: resume a previous game
			mLanderThread.restoreState(savedInstanceState);
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// just have the View's thread save its state into our Bundle
		super.onSaveInstanceState(outState);
		mLanderThread.saveState(outState);
	}
}
