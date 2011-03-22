package com.pilot51.lander;

import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

/**
 * View that draws, takes keystrokes, etc. for a simple LunarLander game.
 * 
 * Has a mode which RUNNING, PAUSED, etc. Has a x, y, dx, dy, ... capturing the
 * current ship physics. All x/y etc. are measured with (0,0) at the lower left.
 * updatePhysics() advances the physics based on realtime. draw() renders the
 * ship, and does an invalidate() to prompt another draw() as soon as possible
 * by the system.
 */
class LanderView extends SurfaceView implements SurfaceHolder.Callback {
	class LanderThread extends Thread {
		/*
		 * Physics constants
		 */
		public static final int PHYS_DOWN_ACCEL_SEC = 35;
		public static final int PHYS_FIRE_ACCEL_SEC = 80;
		public static final int PHYS_FUEL_INIT = 60;
		public static final int PHYS_FUEL_MAX = 100;
		public static final int PHYS_FUEL_SEC = 10;
		public static final int PHYS_SPEED_INIT = 30;
		public static final int PHYS_SPEED_MAX = 120;
		/*
		 * State-tracking constants
		 */
		public static final int STATE_LOSE = 1;
		public static final int STATE_PAUSE = 2;
		public static final int STATE_READY = 3;
		public static final int STATE_RUNNING = 4;
		public static final int STATE_WIN = 5;

		/*
		 * Goal condition constants
		 */
		public static final int TARGET_BOTTOM_PADDING = 0; // px below gear
		public static final int TARGET_PAD_HEIGHT = 8; // how high above ground
		public static final int TARGET_SPEED = 28; // > this speed means crash
		public static final double TARGET_WIDTH = 1.6; // width of target
		/*
		 * UI constants (i.e. the speed & fuel bars)
		 */
		public static final int UI_BAR = 100; // width of the bar(s)
		public static final int UI_BAR_HEIGHT = 10; // height of the bar(s)
		private static final String KEY_DX = "mDX";

		private static final String KEY_DY = "mDY";
		private static final String KEY_FUEL = "mFuel";
		private static final String KEY_GOAL_SPEED = "mGoalSpeed";
		private static final String KEY_GOAL_WIDTH = "mGoalWidth";

		private static final String KEY_GOAL_X = "mGoalX";
		private static final String KEY_LANDER_HEIGHT = "mLanderHeight";
		private static final String KEY_LANDER_WIDTH = "mLanderWidth";
		private static final String KEY_WINS = "mWinsInARow";

		private static final String KEY_X = "mX";
		private static final String KEY_Y = "mY";

		/*
		 * Member (state) fields
		 */
		/** The drawable to use as the background of the animation canvas */
		//private Bitmap mBackgroundImage;

		/**
		 * Current height of the surface/canvas.
		 * 
		 * @see #setSurfaceSize
		 */
		private int mCanvasHeight = 1;

		/**
		 * Current width of the surface/canvas.
		 * 
		 * @see #setSurfaceSize
		 */
		private int mCanvasWidth = 1;

		/** What to draw for the Lander when it has crashed */
		private Drawable mCrashedImage;

		/** Velocity dx. */
		private double mDX;

		/** Velocity dy. */
		private double mDY;

		private boolean mFiringMain;
		private boolean mFiringLeft;
		private boolean mFiringRight;

		private Drawable mFiringMainImage;
		private Drawable mFiringLeftImage;
		private Drawable mFiringRightImage;

		/** Fuel remaining */
		private double mFuel;

		/** Allowed speed. */
		private int mGoalSpeed;

		/** Width of the landing pad. */
		private int mGoalWidth;

		/** X of the landing pad. */
		private int mGoalX;

		/** Message handler used by thread to interact with TextView */
		private Handler mHandler;

		/** Pixel height of lander image. */
		private int mLanderHeight;

		/** What to draw for the Lander in its normal state */
		private Drawable mLanderImage;

		/** Pixel width of lander image. */
		private int mLanderWidth;

		/** Used to figure out elapsed time between frames */
		private long mLastTime;

		/** Paint to draw the landing pad on screen. */
		private Paint mLandingPad;

		/** Paint to draw the fuel bar on screen. */
		private Paint mFuelBar;

		/** "Good" speed variant of the line color. */
		private Paint mSpeedBarGood;

		/** "Bad" speed-too-high variant of the line color. */
		private Paint mSpeedBarBad;

		/** The state of the game. One of READY, RUNNING, PAUSE, LOSE, or WIN */
		private int mMode;

		/** Indicate whether the surface has been created & is ready to draw */
		private boolean mRun = false;

		/** Scratch rect object. */
		private RectF mScratchRect;

		/** Handle to the surface manager object we interact with */
		private SurfaceHolder mSurfaceHolder;

		/** Number of wins in a row. */
		private int mWinsInARow;

		/** X of lander center. */
		private double mX;

		/** Y of lander center. */
		private double mY;

		public LanderThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {
			// get handles to some important objects
			mSurfaceHolder = surfaceHolder;
			mHandler = handler;
			mContext = context;

			//Resources res = context.getResources();
			// cache handles to our key sprites & other drawables
			mLanderImage = context.getResources().getDrawable(R.drawable.lander);
			mFiringMainImage = context.getResources().getDrawable(R.drawable.bflame);
			mFiringLeftImage = context.getResources().getDrawable(R.drawable.lflame);
			mFiringRightImage = context.getResources().getDrawable(R.drawable.rflame);
			mCrashedImage = context.getResources().getDrawable(R.drawable.crash3);

			// load background image as a Bitmap instead of a Drawable b/c
			// we don't need to transform it and it's faster to draw this way
			//mBackgroundImage = BitmapFactory.decodeResource(res,
			//      R.drawable.earthrise);

			// Use the regular lander image as the model size for all sprites
			mLanderWidth = mLanderImage.getIntrinsicWidth();
			mLanderHeight = mLanderImage.getIntrinsicHeight();

			// Initialize paints for speedometer
			mLandingPad = new Paint();
			mLandingPad.setAntiAlias(false);
			mLandingPad.setColor(Color.WHITE);

			mFuelBar = new Paint();
			mFuelBar.setAntiAlias(false);
			mFuelBar.setARGB(255, 150, 150, 0);

			mSpeedBarGood = new Paint();
			mSpeedBarGood.setAntiAlias(false);
			mSpeedBarGood.setColor(Color.GREEN);

			mSpeedBarBad = new Paint();
			mSpeedBarBad.setAntiAlias(false);
			mSpeedBarBad.setARGB(255, 120, 180, 0);

			mScratchRect = new RectF(0, 0, 0, 0);

			mWinsInARow = 0;

			// initial show-up of lander (not yet playing)
			mX = mLanderWidth;
			mY = mLanderHeight * 2;
			mFuel = PHYS_FUEL_INIT;
			mDX = 0;
			mDY = 0;
			mFiringMain = true;
			mFiringLeft = true;
			mFiringRight = true;
		}

		/**
		 * Starts the game, setting parameters for the current difficulty.
		 */
		public void doStart() {
			synchronized (mSurfaceHolder) {
				// First set the game for Medium difficulty
				mFuel = PHYS_FUEL_INIT;
				mFiringMain = false;
				mFiringLeft = false;
				mFiringRight = false;
				mGoalWidth = (int) (mLanderWidth + new Random().nextInt(300));
				mGoalSpeed = TARGET_SPEED;

				// pick a convenient initial location for the lander sprite
				mX = mCanvasWidth / 2;
				mY = mCanvasHeight - mLanderHeight / 2;

				mDY = 0;
				mDX = 0;

				// Figure initial spot for landing, not too near center
				while (true) {
					mGoalX = (int) (Math.random() * (mCanvasWidth - mGoalWidth));
					//if (Math.abs(mGoalX - (mX - mLanderWidth / 2)) > mCanvasHeight / 6)
					break;
				}

				mLastTime = System.currentTimeMillis() + 100;
				setState(STATE_RUNNING);
			}
		}

		/**
		 * Pauses the physics update & animation.
		 */
		public void pause() {
			synchronized (mSurfaceHolder) {
				if (mMode == STATE_RUNNING)
					setState(STATE_PAUSE);
			}
		}

		/**
		 * Restores game state from the indicated Bundle. Typically called when
		 * the Activity is being restored after having been previously
		 * destroyed.
		 * 
		 * @param savedState
		 *            Bundle containing the game state
		 */
		public synchronized void restoreState(Bundle savedState) {
			synchronized (mSurfaceHolder) {
				setState(STATE_PAUSE);
				mFiringMain = false;
				mFiringLeft = false;
				mFiringRight = false;
				mX = savedState.getDouble(KEY_X);
				mY = savedState.getDouble(KEY_Y);
				mDX = savedState.getDouble(KEY_DX);
				mDY = savedState.getDouble(KEY_DY);

				mLanderWidth = savedState.getInt(KEY_LANDER_WIDTH);
				mLanderHeight = savedState.getInt(KEY_LANDER_HEIGHT);
				mGoalX = savedState.getInt(KEY_GOAL_X);
				mGoalSpeed = savedState.getInt(KEY_GOAL_SPEED);
				mGoalWidth = savedState.getInt(KEY_GOAL_WIDTH);
				mWinsInARow = savedState.getInt(KEY_WINS);
				mFuel = savedState.getDouble(KEY_FUEL);
			}
		}

		@Override
		public void run() {
			while (mRun) {
				Canvas c = null;
				try {
					c = mSurfaceHolder.lockCanvas(null);
					synchronized (mSurfaceHolder) {
						if (mMode == STATE_RUNNING)
							updatePhysics();
						doDraw(c);
					}
				} finally {
					// do this in a finally so that if an exception is thrown
					// during the above, we don't leave the Surface in an
					// inconsistent state
					if (c != null) {
						mSurfaceHolder.unlockCanvasAndPost(c);
					}
				}
			}
		}

		/**
		 * Dump game state to the provided Bundle. Typically called when the
		 * Activity is being suspended.
		 * 
		 * @return Bundle with this view's state
		 */
		public Bundle saveState(Bundle map) {
			synchronized (mSurfaceHolder) {
				if (map != null) {
					map.putDouble(KEY_X, Double.valueOf(mX));
					map.putDouble(KEY_Y, Double.valueOf(mY));
					map.putDouble(KEY_DX, Double.valueOf(mDX));
					map.putDouble(KEY_DY, Double.valueOf(mDY));
					map.putInt(KEY_LANDER_WIDTH, Integer.valueOf(mLanderWidth));
					map.putInt(KEY_LANDER_HEIGHT, Integer.valueOf(mLanderHeight));
					map.putInt(KEY_GOAL_X, Integer.valueOf(mGoalX));
					map.putInt(KEY_GOAL_SPEED, Integer.valueOf(mGoalSpeed));
					map.putInt(KEY_GOAL_WIDTH, Integer.valueOf(mGoalWidth));
					map.putInt(KEY_WINS, Integer.valueOf(mWinsInARow));
					map.putDouble(KEY_FUEL, Double.valueOf(mFuel));
				}
			}
			return map;
		}

		/**
		 * Sets if the engine is currently firing.
		 */
		public void setFiringMain(boolean firing) {
			synchronized (mSurfaceHolder) {
				mFiringMain = firing;
			}
		}
		public void setFiringLeft(boolean firing) {
			synchronized (mSurfaceHolder) {
				mFiringLeft = firing;
			}
		}
		public void setFiringRight(boolean firing) {
			synchronized (mSurfaceHolder) {
				mFiringRight = firing;
			}
		}

		/**
		 * Used to signal the thread whether it should be running or not.
		 * Passing true allows the thread to run; passing false will shut it
		 * down if it's already running. Calling start() after this was most
		 * recently called with false will result in an immediate shutdown.
		 * 
		 * @param b
		 *            true to run, false to shut down
		 */
		public void setRunning(boolean b) {
			mRun = b;
		}

		/**
		 * Sets the game mode. That is, whether we are running, paused, in the
		 * failure state, in the victory state, etc.
		 * 
		 * @see #setState(int, CharSequence)
		 * @param mode
		 *            one of the STATE_* constants
		 */
		public void setState(int mode) {
			synchronized (mSurfaceHolder) {
				setState(mode, null);
			}
		}

		/**
		 * Sets the game mode. That is, whether we are running, paused, in the
		 * failure state, in the victory state, etc.
		 * 
		 * @param mode
		 *            one of the STATE_* constants
		 * @param message
		 *            string to add to screen or null
		 */
		public void setState(int mode, CharSequence message) {
			/*
			 * This method optionally can cause a text message to be displayed
			 * to the user when the mode changes. Since the View that actually
			 * renders that text is part of the main View hierarchy and not
			 * owned by this thread, we can't touch the state of that View.
			 * Instead we use a Message + Handler to relay commands to the main
			 * thread, which updates the user-text View.
			 */
			synchronized (mSurfaceHolder) {
				mMode = mode;

				if (mMode == STATE_RUNNING) {
					Message msg = mHandler.obtainMessage();
					Bundle b = new Bundle();
					b.putString("text", "");
					b.putInt("viz", View.INVISIBLE);
					msg.setData(b);
					mHandler.sendMessage(msg);
				} else {
					mFiringMain = false;
					mFiringLeft = false;
					mFiringRight = false;
					Resources res = mContext.getResources();
					CharSequence str = "";
					if (mMode == STATE_READY)
						str = res.getText(R.string.mode_ready);
					else if (mMode == STATE_PAUSE)
						str = res.getText(R.string.mode_pause);
					else if (mMode == STATE_LOSE)
						str = res.getText(R.string.mode_lose);
					else if (mMode == STATE_WIN)
						str = res.getString(R.string.mode_win_prefix) + mWinsInARow + " " + res.getString(R.string.mode_win_suffix);

					if (message != null) {
						str = message + "\n" + str;
					}

					if (mMode == STATE_LOSE)
						mWinsInARow = 0;

					Message msg = mHandler.obtainMessage();
					Bundle b = new Bundle();
					b.putString("text", str.toString());
					b.putInt("viz", View.VISIBLE);
					msg.setData(b);
					mHandler.sendMessage(msg);
				}
			}
		}

		/* Callback invoked when the surface dimensions change. */
		public void setSurfaceSize(int width, int height) {
			// synchronized to make sure these all change atomically
			synchronized (mSurfaceHolder) {
				mCanvasWidth = width;
				mCanvasHeight = height;

				// don't forget to resize the background image
				//mBackgroundImage = mBackgroundImage.createScaledBitmap(
				//      mBackgroundImage, width, height, true);
			}
		}

		/**
		 * Resumes from a pause.
		 */
		public void unpause() {
			// Move the real time clock up to now
			synchronized (mSurfaceHolder) {
				mLastTime = System.currentTimeMillis() + 100;
			}
			setState(STATE_RUNNING);
		}

		/**
		 * Handles a key-down event.
		 * 
		 * @param keyCode
		 *            the key that was pressed
		 * @param msg
		 *            the original event object
		 * @return true
		 */
		boolean doKeyDown(int keyCode, KeyEvent msg) {
			synchronized (mSurfaceHolder) {
				boolean okStart = false;
				if (keyCode == KeyEvent.KEYCODE_DPAD_UP)
					okStart = true;
				if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
					okStart = true;
				if (keyCode == KeyEvent.KEYCODE_S)
					okStart = true;

				if (okStart && (mMode == STATE_READY || mMode == STATE_LOSE || mMode == STATE_WIN)) {
					// ready-to-start -> start
					doStart();
					return true;
				} else if (mMode == STATE_PAUSE && okStart) {
					// paused -> running
					unpause();
					return true;
				} else if (mMode == STATE_RUNNING) {
					// center/space -> fire
					if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_SPACE) {
						setFiringMain(true);
						return true;
						// left/q -> left
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_Q) {
						//mRotating = -1;
						setFiringLeft(true);
						return true;
						// right/w -> right
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_W) {
						//mRotating = 1;
						setFiringRight(true);
						return true;
						// up -> pause
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
						pause();
						return true;
					}
				}

				return false;
			}
		}

		/**
		 * Handles a key-up event.
		 * 
		 * @param keyCode
		 *            the key that was pressed
		 * @param msg
		 *            the original event object
		 * @return true if the key was handled and consumed, or else false
		 */
		boolean doKeyUp(int keyCode, KeyEvent msg) {
			boolean handled = false;

			synchronized (mSurfaceHolder) {
				if (mMode == STATE_RUNNING) {
					if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_SPACE) {
						setFiringMain(false);
						handled = true;
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_Q || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_W) {
						setFiringLeft(false);
						setFiringRight(false);
						handled = true;
					}
				}
			}

			return handled;
		}

		/**
		 * Draws the ship, fuel/speed bars, and background to the provided
		 * Canvas.
		 */
		private void doDraw(Canvas canvas) {
			// Draw the background image. Operations on the Canvas accumulate
			// so this is like clearing the screen.
			//canvas.drawBitmap(mBackgroundImage, 0, 0, null);
			canvas.drawColor(Color.BLACK);

			int yTop = mCanvasHeight - ((int) mY + mLanderHeight / 2);
			int xLeft = (int) mX - mLanderWidth / 2;

			// Draw the fuel gauge
			int fuelWidth = (int) (UI_BAR * mFuel / PHYS_FUEL_MAX);
			mScratchRect.set(4, 4, 4 + fuelWidth, 4 + UI_BAR_HEIGHT);
			canvas.drawRect(mScratchRect, mFuelBar);

			// Draw the speed gauge, with a two-tone effect
			double speed = Math.sqrt(mDX * mDX + mDY * mDY);
			int speedWidth = (int) (UI_BAR * speed / PHYS_SPEED_MAX);

			if (speed <= mGoalSpeed) {
				mScratchRect.set(4 + UI_BAR + 4, 4, 4 + UI_BAR + 4 + speedWidth, 4 + UI_BAR_HEIGHT);
				canvas.drawRect(mScratchRect, mSpeedBarGood);
			} else {
				// Draw the bad color in back, with the good color in front of
				// it
				mScratchRect.set(4 + UI_BAR + 4, 4, 4 + UI_BAR + 4 + speedWidth, 4 + UI_BAR_HEIGHT);
				canvas.drawRect(mScratchRect, mSpeedBarBad);
				int goalWidth = (UI_BAR * mGoalSpeed / PHYS_SPEED_MAX);
				mScratchRect.set(4 + UI_BAR + 4, 4, 4 + UI_BAR + 4 + goalWidth, 4 + UI_BAR_HEIGHT);
				canvas.drawRect(mScratchRect, mSpeedBarGood);
			}

			// Draw the landing pad
			canvas.drawLine(mGoalX, 1 + mCanvasHeight - TARGET_PAD_HEIGHT, mGoalX + mGoalWidth, 1 + mCanvasHeight - TARGET_PAD_HEIGHT, mLandingPad);

			// Draw the ship with its current rotation
			canvas.save();
			//canvas.rotate((float) mHeading, (float) mX, mCanvasHeight - (float) mY);
			if (mFiringMain) {
				int yTopF = mCanvasHeight - ((int) mY - 26 + mFiringMainImage.getIntrinsicHeight() / 2);
				int xLeftF = (int) mX - mFiringMainImage.getIntrinsicWidth() / 2;
				mFiringMainImage.setBounds(xLeftF, yTopF, xLeftF + mFiringMainImage.getIntrinsicWidth(), yTopF + mFiringMainImage.getIntrinsicHeight());
				mFiringMainImage.draw(canvas);
			}
			if (mFiringLeft) {
				int yTopF = mCanvasHeight - ((int) mY + 6 + mFiringLeftImage.getIntrinsicHeight() / 2);
				int xLeftF = (int) mX - 27 - mFiringLeftImage.getIntrinsicWidth() / 2;
				mFiringLeftImage.setBounds(xLeftF, yTopF, xLeftF + mFiringLeftImage.getIntrinsicWidth(), yTopF + mFiringLeftImage.getIntrinsicHeight());
				mFiringLeftImage.draw(canvas);
			}
			if (mFiringRight) {
				int yTopF = mCanvasHeight - ((int) mY + 6 + mFiringRightImage.getIntrinsicHeight() / 2);
				int xLeftF = (int) mX + 27 - mFiringRightImage.getIntrinsicWidth() / 2;
				mFiringRightImage.setBounds(xLeftF, yTopF, xLeftF + mFiringRightImage.getIntrinsicWidth(), yTopF + mFiringRightImage.getIntrinsicHeight());
				mFiringRightImage.draw(canvas);
			}
			if (mMode == STATE_LOSE) {
				mCrashedImage.setBounds(xLeft, yTop, xLeft + mLanderWidth, yTop + mLanderHeight);
				mCrashedImage.draw(canvas);
			} else {
				mLanderImage.setBounds(xLeft, yTop, xLeft + mLanderWidth, yTop + mLanderHeight);
				mLanderImage.draw(canvas);
			}
			canvas.restore();
		}

		/**
		 * Figures the lander state (x, y, fuel, ...) based on the passage of
		 * realtime. Does not invalidate(). Called at the start of draw().
		 * Detects the end-of-game and sets the UI to the next state.
		 */
		private void updatePhysics() {
			long now = System.currentTimeMillis();

			// Do nothing if mLastTime is in the future.
			// This allows the game-start to delay the start of the physics
			// by 100ms or whatever.
			if (mLastTime > now)
				return;

			double elapsed = (now - mLastTime) / 1000.0;

			// Base accelerations -- 0 for x, gravity for y
			double ddx = 0.0;
			double ddy = -PHYS_DOWN_ACCEL_SEC * elapsed;

			if (mFiringMain) {
				// taking 0 as up, 90 as to the right
				// cos(deg) is ddy component, sin(deg) is ddx component
				double elapsedFiring = elapsed;
				double fuelUsed = elapsedFiring * PHYS_FUEL_SEC;

				// tricky case where we run out of fuel partway through the
				// elapsed
				if (fuelUsed > mFuel) {
					elapsedFiring = mFuel / fuelUsed * elapsed;
					fuelUsed = mFuel;

					// Oddball case where we adjust the "control" from here
					mFiringMain = false;
				}

				mFuel -= fuelUsed;

				// have this much acceleration from the engine
				double accel = PHYS_FIRE_ACCEL_SEC * elapsedFiring;

				ddy += accel;
			}
			if (mFiringLeft) {
				double elapsedFiring = elapsed;
				double fuelUsed = elapsedFiring * PHYS_FUEL_SEC;

				// tricky case where we run out of fuel partway through the
				// elapsed
				if (fuelUsed > mFuel) {
					elapsedFiring = mFuel / fuelUsed * elapsed;
					fuelUsed = mFuel;

					// Oddball case where we adjust the "control" from here
					mFiringLeft = false;
				}

				mFuel -= fuelUsed;

				// have this much acceleration from the engine
				double accel = PHYS_FIRE_ACCEL_SEC * elapsedFiring;

				ddx = accel;
			} else if (mFiringRight) {
				double elapsedFiring = elapsed;
				double fuelUsed = elapsedFiring * PHYS_FUEL_SEC;

				// tricky case where we run out of fuel partway through the
				// elapsed
				if (fuelUsed > mFuel) {
					elapsedFiring = mFuel / fuelUsed * elapsed;
					fuelUsed = mFuel;

					// Oddball case where we adjust the "control" from here
					mFiringRight = false;
				}

				mFuel -= fuelUsed;

				// have this much acceleration from the engine
				double accel = PHYS_FIRE_ACCEL_SEC * elapsedFiring;

				ddx = -accel;
			}

			double dxOld = mDX;
			double dyOld = mDY;

			// figure speeds for the end of the period
			mDX += ddx;
			mDY += ddy;

			// figure position based on average speed during the period
			mX += elapsed * (mDX + dxOld) / 2;
			mY += elapsed * (mDY + dyOld) / 2;

			mLastTime = now;

			// Evaluate if we have landed ... stop the game
			double yLowerBound = TARGET_PAD_HEIGHT + mLanderHeight / 2 - TARGET_BOTTOM_PADDING;
			if (mY <= yLowerBound) {
				mY = yLowerBound;

				int result = STATE_LOSE;
				CharSequence message = "";
				Resources res = mContext.getResources();
				double speed = Math.sqrt(mDX * mDX + mDY * mDY);
				boolean onGoal = (mGoalX <= mX - mLanderWidth / 2 && mX + mLanderWidth / 2 <= mGoalX + mGoalWidth);

				if (!onGoal) {
					message = res.getText(R.string.message_off_pad);
				} else if (speed > mGoalSpeed) {
					message = res.getText(R.string.message_too_fast);
				} else {
					result = STATE_WIN;
					mWinsInARow++;
				}

				setState(result, message);
			}
		}
	}

	/** Handle to the application context, used to e.g. fetch Drawables. */
	private Context mContext;

	/** Pointer to the text view to display "Paused.." etc. */
	private TextView mStatusText;

	/** The thread that actually draws the animation */
	private LanderThread thread;

	public LanderView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		// create thread only; it's started in surfaceCreated()
		thread = new LanderThread(holder, context, new Handler() {
			@Override
			public void handleMessage(Message m) {
				mStatusText.setVisibility(m.getData().getInt("viz"));
				mStatusText.setText(m.getData().getString("text"));
			}
		});

		setFocusable(true); // make sure we get key events
	}

	/**
	 * Fetches the animation thread corresponding to this LunarView.
	 * 
	 * @return the animation thread
	 */
	public LanderThread getThread() {
		return thread;
	}

	/**
	 * Standard override to get key-press events.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {
		return thread.doKeyDown(keyCode, msg);
	}

	/**
	 * Standard override for key-up. We actually care about these, so we can
	 * turn off the engine or stop rotating.
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent msg) {
		return thread.doKeyUp(keyCode, msg);
	}

	/**
	 * Standard window-focus override. Notice focus lost so we can pause on
	 * focus lost. e.g. user switches to take a call.
	 */
	@Override
	public void onWindowFocusChanged(boolean hasWindowFocus) {
		if (!hasWindowFocus)
			thread.pause();
	}

	/**
	 * Installs a pointer to the text view used for messages.
	 */
	public void setTextView(TextView textView) {
		mStatusText = textView;
	}

	/* Callback invoked when the surface dimensions change. */
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		thread.setSurfaceSize(width, height);
	}

	/*
	 * Callback invoked when the Surface has been created and is ready to be
	 * used.
	 */
	public void surfaceCreated(SurfaceHolder holder) {
		// start the thread here so that we don't busy-wait in run()
		// waiting for the surface to be created
		thread.setRunning(true);
		thread.start();
	}

	/*
	 * Callback invoked when the Surface has been destroyed and must no longer
	 * be touched. WARNING: after this method returns, the Surface/Canvas must
	 * never be touched again!
	 */
	public void surfaceDestroyed(SurfaceHolder holder) {
		// we have to tell thread to shut down & wait for it to finish, or else
		// it might touch the Surface after we return and explode
		boolean retry = true;
		thread.setRunning(false);
		while (retry) {
			try {
				thread.join();
				retry = false;
			} catch (InterruptedException e) {
			}
		}
	}
}
