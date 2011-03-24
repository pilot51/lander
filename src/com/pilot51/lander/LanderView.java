package com.pilot51.lander;

import java.text.DecimalFormat;
import java.util.Random;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * View that draws, takes keystrokes, etc. for a simple LunarLander game.
 * 
 * All x/y etc. are measured with (0,0) at the lower left.
 * updatePhysics() advances the physics based on realtime. draw() renders the
 * ship, and does an invalidate() to prompt another draw() as soon as possible
 * by the system.
 */
class LanderView extends SurfaceView implements SurfaceHolder.Callback, OnTouchListener {
	/** Handle to the application context, used to e.g. fetch Drawables. */
	private Context mContext;

	private TextView mTextStatus, mTextAlt, mTextVelX, mTextVelY, mTextFuel;
	private Button mBtnThrust, mBtnLeft, mBtnRight;

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
				if (m.getData().getInt("view") == 1) {
					mTextAlt.setText(m.getData().getString("text"));
				} else if (m.getData().getInt("view") == 2) {
					mTextVelX.setText(m.getData().getString("text"));
				} else if (m.getData().getInt("view") == 3) {
					mTextVelY.setText(m.getData().getString("text"));
				} else if (m.getData().getInt("view") == 4) {
					mTextFuel.setText(m.getData().getString("text"));
				} else {
					mTextStatus.setVisibility(m.getData().getInt("viz"));
					mTextStatus.setText(m.getData().getString("text"));
				}
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

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent msg) {
		return thread.doKeyDown(keyCode, msg);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent msg) {
		return thread.doKeyUp(keyCode, msg);
	}

	public void setTextViewMain(TextView textView) {
		mTextStatus = textView;
	}

	public void setTextViewAlt(TextView textView) {
		mTextAlt = textView;
	}

	public void setTextViewVelX(TextView textView) {
		mTextVelX = textView;
	}

	public void setTextViewVelY(TextView textView) {
		mTextVelY = textView;
	}

	public void setTextViewFuel(TextView textView) {
		mTextFuel = textView;
	}

	public void setButtonThrust(Button btn) {
		mBtnThrust = btn;
		btn.setOnTouchListener(this);
	}

	public void setButtonLeft(Button btn) {
		mBtnLeft = btn;
		btn.setOnTouchListener(this);
	}

	public void setButtonRight(Button btn) {
		mBtnRight = btn;
		btn.setOnTouchListener(this);
	}

	@Override
	public boolean onTouch(View src, MotionEvent event) {
		return thread.doBtnTouch(src, event);
	}

	/* Callback invoked when the surface dimensions change. */
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		thread.setSurfaceSize(width, height);
	}

	/* Callback invoked when the Surface has been created and is ready to be used. */
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

	class LanderThread extends Thread {
		private static final byte FLAME_DELAY = 1;
		private static final byte STATUS_DELAY = 5;
		/** number of frames in explosion */
		private static final byte EXPL_SEQUENCE = 10;
		/** 50 milliseconds */
		private static final byte UPDATE_TIME = 50;

		/* Defaults */
		private static final float DEF_GRAVITY = 3f, DEF_FUEL = 1000f, DEF_THRUST = 10000f;

		/* Physical Settings & Options */
		/** mass of the lander in kg */
		private float fLanderMass = 1000f;
		/** kg of fuel to start */
		private float fInitFuel = DEF_FUEL;
		/** main engine thrust in Newtons */
		private float fMainForce = DEF_THRUST;
		/** attitude thruster force in Newtons */
		private float fAttitudeForce = 2000f;
		/** main engine kg of fuel / second */
		private float fMainBurn = 10f;
		/** attitude thruster kg of fuel / second */
		private float fAttitudeBurn = 2f;
		/** gravity acceleration in m/s² */
		private float fGravity = DEF_GRAVITY;
		/** max horizontal velocity on landing */
		private float fMaxLandingX = 1f;
		/** max vertical velocity on landing */
		private float fMaxLandingY = 10f;
		/** Fuel in kilograms */
		private float fFuel;

		/** Lander position in meters */
		private float fLanderX, fLanderY;
		/** Lander velocity in meters */
		private float fLanderVx, fLanderVy;
		/** time increment in seconds */
		private float dt = 0.5f;
		/** Elapsed time */
		private float t;

		/* Other variables */
		/** reverse side thrust buttons */
		private boolean bReverseSideThrust = false;
		/** draw flame on lander */
		private boolean bDrawFlame = true;

		/** size of full-screen window */
		private short xClient, yClient;

		/** lander bitmap */
		private Drawable hLanderPict;
		private Drawable hLFlamePict, hRFlamePict, hBFlamePict;

		/** size of lander bitmap */
		private long xLanderPict, yLanderPict;

		private Drawable hCrash1, hCrash2, hCrash3;
		//private Drawable hExpl[EXPL_SEQUENCE];

		DecimalFormat df2 = new DecimalFormat("0.00"); // Fixed to 2 decimal places

		private Resources res;

		/*
		 * Physics constants
		 */
		public static final int PHYS_DOWN_ACCEL_SEC = 35, PHYS_FIRE_ACCEL_SEC = 80, PHYS_FUEL_MAX = 3000, PHYS_SPEED_MAX = 120;

		/*
		 * State-tracking constants
		 */
		public static final int STATE_READY = 1, STATE_RUNNING = 2, STATE_WIN = 3, STATE_LOSE = 4;

		/*
		 * Goal condition constants
		 */
		public static final int TARGET_BOTTOM_PADDING = 0, // px below gear
				TARGET_PAD_HEIGHT = 8, // how high above ground
				TARGET_SPEED = 28; // > this speed means crash
		public static final double TARGET_WIDTH = 1.6; // width of target
		/*
		 * UI constants (i.e. the speed & fuel bars)
		 */
		public static final int UI_BAR = 100, // width of the bar(s)
				UI_BAR_HEIGHT = 10; // height of the bar(s)

		private static final String KEY_DX = "mDX", KEY_DY = "mDY", KEY_FUEL = "mFuel", KEY_GOAL_SPEED = "mGoalSpeed", KEY_GOAL_WIDTH = "mGoalWidth", KEY_GOAL_X = "mGoalX",
				KEY_LANDER_HEIGHT = "mLanderHeight", KEY_LANDER_WIDTH = "mLanderWidth", KEY_X = "mX", KEY_Y = "mY";

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
		private float mFuel;

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

		/** The state of the game. One of READY, RUNNING, WIN, or LOSE */
		private int mMode;

		/** Indicate whether the surface has been created & is ready to draw */
		private boolean mRun = false;

		/** Handle to the surface manager object we interact with */
		private SurfaceHolder mSurfaceHolder;

		/** X of lander center. */
		private double mX;

		/** Y of lander center. */
		private double mY;

		public LanderThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {
			mSurfaceHolder = surfaceHolder;
			mHandler = handler;
			mContext = context;

			res = context.getResources();
			mLanderImage = res.getDrawable(R.drawable.lander);
			mFiringMainImage = res.getDrawable(R.drawable.bflame);
			mFiringLeftImage = res.getDrawable(R.drawable.lflame);
			mFiringRightImage = res.getDrawable(R.drawable.rflame);
			mCrashedImage = res.getDrawable(R.drawable.crash3);

			mLanderWidth = mLanderImage.getIntrinsicWidth();
			mLanderHeight = mLanderImage.getIntrinsicHeight();

			mLandingPad = new Paint();
			mLandingPad.setAntiAlias(false);
			mLandingPad.setColor(Color.WHITE);

			doStart();
		}

		public void doStart() {
			synchronized (mSurfaceHolder) {
				mFuel = fInitFuel;
				mGoalWidth = (int) (mLanderWidth + new Random().nextInt(300));
				mGoalSpeed = TARGET_SPEED;
				mX = mCanvasWidth / 2;
				mY = mCanvasHeight - mLanderHeight / 2;
				mDY = 0;
				mDX = 0;
				mGoalX = (int) (Math.random() * (mCanvasWidth - mGoalWidth));
				//if (Math.abs(mGoalX - (mX - mLanderWidth / 2)) > mCanvasHeight / 6)
			}
		}

		public void doRestart() {
			synchronized (mSurfaceHolder) {
				mFuel = fInitFuel;
				mX = mCanvasWidth / 2;
				mY = mCanvasHeight - mLanderHeight / 2;
				mDY = 0;
				mDX = 0;
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
				mX = savedState.getDouble(KEY_X);
				mY = savedState.getDouble(KEY_Y);
				mDX = savedState.getDouble(KEY_DX);
				mDY = savedState.getDouble(KEY_DY);

				mLanderWidth = savedState.getInt(KEY_LANDER_WIDTH);
				mLanderHeight = savedState.getInt(KEY_LANDER_HEIGHT);
				mGoalX = savedState.getInt(KEY_GOAL_X);
				mGoalSpeed = savedState.getInt(KEY_GOAL_SPEED);
				mGoalWidth = savedState.getInt(KEY_GOAL_WIDTH);
				mFuel = savedState.getFloat(KEY_FUEL);
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
					map.putFloat(KEY_FUEL, Float.valueOf(mFuel));
				}
			}
			return map;
		}

		public void setFiringMain(boolean firing) {
			synchronized (mSurfaceHolder) {
				mFiringMain = firing;
				mBtnThrust.setPressed(firing);
			}
		}

		public void setFiringLeft(boolean firing) {
			synchronized (mSurfaceHolder) {
				mFiringLeft = firing;
				mBtnLeft.setPressed(firing);
			}
		}

		public void setFiringRight(boolean firing) {
			synchronized (mSurfaceHolder) {
				mFiringRight = firing;
				mBtnRight.setPressed(firing);
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
		 * Sets the game mode. That is, whether we are running,
		 * in the failure state, in the victory state, etc.
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
		 * Sets the game mode. That is, whether we are running,
		 * in the failure state, in the victory state, etc.
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
					setScreenText(0, "", View.INVISIBLE);
				} else {
					mFiringMain = false;
					mFiringLeft = false;
					mFiringRight = false;
					CharSequence str = "";
					if (mMode == STATE_LOSE)
						str = res.getText(R.string.mode_lose);
					else if (mMode == STATE_WIN)
						str = res.getString(R.string.mode_win);
					if (message != null) {
						str = message + "\n" + str;
					}
					setScreenText(0, str.toString(), View.VISIBLE);
				}
			}
		}

		/* Callback invoked when the surface dimensions change. */
		public void setSurfaceSize(int width, int height) {
			// synchronized to make sure these all change atomically
			synchronized (mSurfaceHolder) {
				mCanvasWidth = width;
				mCanvasHeight = height;
				// Set initial position of lander as soon as canvas size set
				mX = mCanvasWidth / 2;
				mY = mCanvasHeight - mLanderHeight / 2;
			}
		}

		boolean doBtnTouch(View src, MotionEvent event) {
			synchronized (mSurfaceHolder) {
				if (mMode == STATE_RUNNING) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						if (src == mBtnThrust) {
							setFiringMain(true);
							return true;
						} else if (src == mBtnLeft) {
							setFiringLeft(true);
							return true;
						} else if (src == mBtnRight) {
							setFiringRight(true);
							return true;
						}
					} else if (event.getAction() == MotionEvent.ACTION_UP) {
						if (src == mBtnThrust) {
							setFiringMain(false);
							return true;
						} else if (src == mBtnLeft) {
							setFiringLeft(false);
							return true;
						} else if (src == mBtnRight) {
							setFiringRight(false);
							return true;
						}
					}
				}
				return false;
			}
		}

		boolean doKeyDown(int keyCode, KeyEvent msg) {
			synchronized (mSurfaceHolder) {
				if (mMode == STATE_READY & (keyCode == KeyEvent.KEYCODE_DPAD_DOWN | keyCode == KeyEvent.KEYCODE_DPAD_LEFT | keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
					setState(STATE_RUNNING);
					mLastTime = System.currentTimeMillis() + 100;
					return true;
				} else if (mMode == STATE_RUNNING) {
					if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
						setFiringMain(true);
						return true;
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
						setFiringLeft(true);
						return true;
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
						setFiringRight(true);
						return true;
					}
				}
				return false;
			}
		}

		boolean doKeyUp(int keyCode, KeyEvent msg) {
			synchronized (mSurfaceHolder) {
				if (mMode == STATE_RUNNING) {
					if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
						setFiringMain(false);
						return true;
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
						setFiringLeft(false);
						return true;
					} else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
						setFiringRight(false);
						return true;
					}
				}
				return false;
			}
		}

		private void setScreenText(int view, String text, int viz) {
			Message msg = mHandler.obtainMessage();
			Bundle b = new Bundle();
			b.putInt("view", view);
			b.putString("text", text);
			b.putInt("viz", viz);
			msg.setData(b);
			mHandler.sendMessage(msg);
		}

		private void doDraw(Canvas canvas) {
			// Draw the background image. Operations on the Canvas accumulate
			// so this is like clearing the screen.
			canvas.drawColor(Color.BLACK);

			setScreenText(1, df2.format(mY - TARGET_PAD_HEIGHT - mLanderHeight / 2), 0);
			setScreenText(2, df2.format(mDX), 0);
			setScreenText(3, df2.format(mDY), 0);
			setScreenText(4, df2.format(mFuel), 0);

			int yTop = mCanvasHeight - ((int) mY + mLanderHeight / 2);
			int xLeft = (int) mX - mLanderWidth / 2;

			// Draw the landing pad
			canvas.drawLine(mGoalX, 1 + mCanvasHeight - TARGET_PAD_HEIGHT, mGoalX + mGoalWidth, 1 + mCanvasHeight - TARGET_PAD_HEIGHT, mLandingPad);
			canvas.save();
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
				double fuelUsed = elapsedFiring * fMainBurn;

				// tricky case where we run out of fuel partway through the elapsed
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
				double fuelUsed = elapsedFiring * fAttitudeBurn;

				// tricky case where we run out of fuel partway through the elapsed
				if (fuelUsed > mFuel) {
					elapsedFiring = mFuel / fuelUsed * elapsed;
					fuelUsed = mFuel;

					// Oddball case where we adjust the "control" from here
					mFiringLeft = false;
				}

				mFuel -= fuelUsed;

				// have this much acceleration from the engine
				double accel = PHYS_FIRE_ACCEL_SEC * elapsedFiring;

				ddx += accel;
			}
			if (mFiringRight) {
				double elapsedFiring = elapsed;
				double fuelUsed = elapsedFiring * fAttitudeBurn;

				// tricky case where we run out of fuel partway through the elapsed
				if (fuelUsed > mFuel) {
					elapsedFiring = mFuel / fuelUsed * elapsed;
					fuelUsed = mFuel;

					// Oddball case where we adjust the "control" from here
					mFiringRight = false;
				}

				mFuel -= fuelUsed;

				// have this much acceleration from the engine
				double accel = PHYS_FIRE_ACCEL_SEC * elapsedFiring;

				ddx += -accel;
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
				double speed = Math.sqrt(mDX * mDX + mDY * mDY);
				boolean onGoal = (mGoalX <= mX - mLanderWidth / 2 && mX + mLanderWidth / 2 <= mGoalX + mGoalWidth);

				if (!onGoal) {
					message = res.getText(R.string.message_off_pad);
				} else if (speed > mGoalSpeed) {
					message = res.getText(R.string.message_too_fast);
				} else {
					result = STATE_WIN;
				}

				setState(result, message);
			}
		}
	}
}
