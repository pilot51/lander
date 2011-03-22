package com.pilot51.lander;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.TextView;

import com.pilot51.lander.LanderView.LanderThread;

/**
 * This is a simple LunarLander activity that houses a single LunarView. It
 * demonstrates...
 * <ul>
 * <li>animating by calling invalidate() from draw()
 * <li>loading and drawing resources
 * <li>handling onPause() in an animation
 * </ul>
 */
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

	/**
	 * Invoked during init to give the Activity a chance to set up its Menu.
	 * 
	 * @param menu
	 *            the Menu to which entries may be added
	 * @return true
	 */

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    super.onCreateOptionsMenu(menu);

	    menu.add(0, MENU_NEW, 0, R.string.menu_new);
	    menu.add(0, MENU_RESTART, 0, R.string.menu_restart);
	    menu.add(0, MENU_OPTIONS, 0, R.string.menu_options);
	    menu.add(0, MENU_ABOUT, 0, R.string.menu_about);

	    return true;
	}

	/**
	 * Invoked when the user selects an item from the Menu.
	 * 
	 * @param item
	 *            the Menu entry which was selected
	 * @return true if the Menu item was legit (and we consumed it), false
	 *         otherwise
	 */

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_NEW:
			
			return true;
		case MENU_RESTART:
			mLanderThread.doStart();
			return true;
		case MENU_OPTIONS:
			
			return true;
		case MENU_ABOUT:
			
			return true;
		}

		return false;
	}

	/**
	 * Invoked when the Activity is created.
	 * 
	 * @param savedInstanceState
	 *            a Bundle containing state saved from a previous execution, or
	 *            null if this is a new execution
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// turn off the window's title bar
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// tell system to use the layout defined in our XML file
		setContentView(R.layout.lander_layout);

		// get handles to the LunarView from XML, and its LunarThread
		mLanderView = (LanderView) findViewById(R.id.lander);
		mLanderThread = mLanderView.getThread();

		// give the LunarView a handle to the TextView used for messages
		mLanderView.setTextView((TextView) findViewById(R.id.text));

		if (savedInstanceState == null) {
			// we were just launched: set up a new game
			mLanderThread.setState(LanderThread.STATE_READY);
			Log.w(this.getClass().getName(), "SIS is null");
		} else {
			// we are being restored: resume a previous game
			mLanderThread.restoreState(savedInstanceState);
			Log.w(this.getClass().getName(), "SIS is nonnull");
		}
	}

	/**
	 * Invoked when the Activity loses user focus.
	 */
	@Override
	protected void onPause() {
		super.onPause();
		mLanderView.getThread().pause(); // pause game when Activity pauses
	}

	/**
	 * Notification that something is about to happen, to give the Activity a
	 * chance to save state.
	 * 
	 * @param outState
	 *            a Bundle into which this Activity should save its state
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// just have the View's thread save its state into our Bundle
		super.onSaveInstanceState(outState);
		mLanderThread.saveState(outState);
		Log.w(this.getClass().getName(), "SIS called");
	}
}
