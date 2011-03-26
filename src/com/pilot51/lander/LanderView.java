package com.pilot51.lander;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
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
import android.util.Log;
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
		private static final int FLAME_DELAY = 1;
		private static final int STATUS_DELAY = 5;
		/** number of frames in explosion */
		private static final int EXPL_SEQUENCE = 10;
		/** 50 milliseconds */
		private static final int UPDATE_TIME = 50;
		
		private static final int EXP_INTERVAL = 10;
		private static final int EXP_MAXRADIUS = 100;
		private static final int MAX_TIMER = 10;

		/** New: begin new game */
		protected static final byte LND_NEW = 1;
		/** Timing: timing loop to determine time interval */
		private static final byte LND_TIMING = 2;
		/** Restart: same terrain, start again */
		protected static final byte LND_RESTART = 3;
		/** Active state: lander is in the air */
		private static final byte LND_ACTIVE = 4;
		/** End: lander touches ground */
		private static final byte LND_ENDGAME = 5;
		/** Safe state: lander touches down safely */
		private static final byte LND_SAFE = 6;
		/** Crash state: lander crashed on surface */
		private static final byte LND_CRASH1 = 7, LND_CRASH2 = 8, LND_CRASH3 = 9;
		/** Explode state: lander has crashed, explosion */
		private static final byte LND_EXPLODE = 10;
		/** Out of range: lander out of bounds */
		private static final byte LND_OUTOFRANGE = 11;
		/** Inactive state: lander on the ground */
		private static final byte LND_INACTIVE = 12;
		/** Inactive state: lander not doing anything */
		private static final byte LND_HOLD = 13;
		
		/* EndGame states */
		/** landed safely */
		private static final byte END_SAFE = 1;
		/** Too much vertical velocity */
		private static final byte END_CRASHV = 2;
		/** Too much horizontal velocity */
		private static final byte END_CRASHH = 3;
		/** Missed the landing site */
		private static final byte END_CRASHS = 4;
		/** Lander out of range */
		private static final byte END_OUTOFRANGE = 5;
		/** about box */
		private static final byte END_ABOUT = 6;
		
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
		private float LanderX, LanderY;
		/** Lander velocity in meters/sec */
		private float LanderVx, LanderVy;
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
		private int xClient, yClient;

		/** lander bitmap */
		private Drawable hLanderPict;
		private Drawable hLFlamePict, hRFlamePict, hBFlamePict;

		/** size of lander bitmap */
		private int xLanderPict, yLanderPict;

		private Drawable hCrash1, hCrash2, hCrash3;
		//private Drawable hExpl[EXPL_SEQUENCE];
		
		private int xGroundZero, yGroundZero;
		/** Lander window state */
		protected byte byLanderState;
		/** EndGame dialog state */
		private byte byEndGameState;
		private byte nExplCount;

		private DecimalFormat df2 = new DecimalFormat("0.00"); // Fixed to 2 decimal places

		private Resources res;
		
		private List<Line> groundPlot, landingPlot;

		/*
		 * Physics constants
		 */
		public static final int PHYS_FUEL_MAX = 3000, PHYS_SPEED_MAX = 120;

		private static final String
			KEY_STATE = "byLanderState",
			KEY_END_STATE = "byEndGameState",
			KEY_DX = "LanderVx",
			KEY_DY = "LanderVy",
			KEY_LANDER_HEIGHT = "yLanderPict",
			KEY_LANDER_WIDTH = "xLanderPict",
			KEY_X = "LanderX",
			KEY_Y = "LanderY",
			KEY_FUEL = "fFuel";

		private boolean mFiringMain;
		private boolean mFiringLeft;
		private boolean mFiringRight;

		/** Message handler used by thread to interact with TextView */
		private Handler mHandler;

		/** Used to figure out elapsed time between frames */
		private long mLastTime;

		/** Paint to draw the landing pad on screen. */
		private Paint mLandingPad;

		/** Indicate whether the surface has been created & is ready to draw */
		private boolean mRun = false;

		/** Handle to the surface manager object we interact with */
		private SurfaceHolder mSurfaceHolder;

		public LanderThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {
			mSurfaceHolder = surfaceHolder;
			mHandler = handler;
			mContext = context;

			res = context.getResources();
			hLanderPict = res.getDrawable(R.drawable.lander);
			hBFlamePict = res.getDrawable(R.drawable.bflame);
			hLFlamePict = res.getDrawable(R.drawable.lflame);
			hRFlamePict = res.getDrawable(R.drawable.rflame);
			hCrash3 = res.getDrawable(R.drawable.crash3);

			xLanderPict = hLanderPict.getIntrinsicWidth();
			yLanderPict = hLanderPict.getIntrinsicHeight();

			mLandingPad = new Paint();
			mLandingPad.setAntiAlias(false);
			mLandingPad.setColor(Color.WHITE);
			
			byLanderState = LND_NEW;
			doStart();
		}

		public void doStart() {
			synchronized (mSurfaceHolder) {
				fFuel = fInitFuel;
				LanderX = xClient / 2;
				LanderY = yClient - yLanderPict / 2;
				LanderVy = 0;
				LanderVx = 0;
				if (byLanderState == LND_NEW) {
					createGround();
				}
				byLanderState = LND_HOLD;
				byEndGameState = 0;
				setScreenText(0, "", View.INVISIBLE);
			}
		}

		@Override
		public void run() {
			while (mRun) {
				Canvas c = null;
				try {
					c = mSurfaceHolder.lockCanvas(null);
					synchronized (mSurfaceHolder) {
						if (byLanderState == LND_ACTIVE)
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
					map.putByte(KEY_STATE, Byte.valueOf(byLanderState));
					map.putByte(KEY_END_STATE, Byte.valueOf(byEndGameState));
					map.putFloat(KEY_X, Float.valueOf(LanderX));
					map.putFloat(KEY_Y, Float.valueOf(LanderY));
					map.putFloat(KEY_DX, Float.valueOf(LanderVx));
					map.putFloat(KEY_DY, Float.valueOf(LanderVy));
					map.putInt(KEY_LANDER_WIDTH, Integer.valueOf(xLanderPict));
					map.putInt(KEY_LANDER_HEIGHT, Integer.valueOf(yLanderPict));
					map.putFloat(KEY_FUEL, Float.valueOf(fFuel));
				}
			}
			return map;
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
				byLanderState = savedState.getByte(KEY_STATE);
				byEndGameState = savedState.getByte(KEY_END_STATE);
				LanderX = savedState.getFloat(KEY_X);
				LanderY = savedState.getFloat(KEY_Y);
				LanderVx = savedState.getFloat(KEY_DX);
				LanderVy = savedState.getFloat(KEY_DY);
				xLanderPict = savedState.getInt(KEY_LANDER_WIDTH);
				yLanderPict = savedState.getInt(KEY_LANDER_HEIGHT);
				fFuel = savedState.getFloat(KEY_FUEL);
			}
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
		
		public void endGame() {
			/*
			 * This method optionally can cause a text message to be displayed
			 * to the user when the mode changes. Since the View that actually
			 * renders that text is part of the main View hierarchy and not
			 * owned by this thread, we can't touch the state of that View.
			 * Instead we use a Message + Handler to relay commands to the main
			 * thread, which updates the user-text View.
			 */
			synchronized (mSurfaceHolder) {
				mFiringMain = false;
				mFiringLeft = false;
				mFiringRight = false;
				String str = res.getString(R.string.end_crash) + "\n";
				switch (byEndGameState) {
				case END_CRASHV:
					switch (new Random().nextInt(3)) {
					case 0:
						str += res.getString(R.string.end_crashv1);
						break;
					case 1:
						str += res.getString(R.string.end_crashv2);
						break;
					case 2:
						str += res.getString(R.string.end_crashv3);
						break;
					}
					break;
				case END_CRASHH:
					str += res.getString(R.string.end_crashh);
					break;
				case END_CRASHS:
					str += res.getString(R.string.end_crashs);
					break;
				case END_OUTOFRANGE:
					str = res.getString(R.string.end_outofrange);
					break;
				case END_SAFE:
					str = res.getString(R.string.end_safe);
					break;
				}
				setScreenText(0, str.toString(), View.VISIBLE);
			}
		}

		/* Callback invoked when the surface dimensions change. */
		public void setSurfaceSize(int width, int height) {
			// synchronized to make sure these all change atomically
			synchronized (mSurfaceHolder) {
				xClient = width;
				yClient = height;
				// Set initial position of lander as soon as canvas size set
				LanderX = xClient / 2;
				LanderY = yClient - yLanderPict / 2;
				createGround();
			}
		}

		boolean doBtnTouch(View src, MotionEvent event) {
			synchronized (mSurfaceHolder) {
				if (byLanderState == LND_HOLD && event.getAction() == MotionEvent.ACTION_DOWN) {
					byLanderState = LND_ACTIVE;
					mLastTime = System.currentTimeMillis() + 100;
				}
				if (byLanderState == LND_ACTIVE) {
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
				if (byLanderState == LND_ACTIVE) {
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
				if (byLanderState == LND_HOLD & (keyCode == KeyEvent.KEYCODE_DPAD_DOWN | keyCode == KeyEvent.KEYCODE_DPAD_LEFT | keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
					byLanderState = LND_ACTIVE;
					mLastTime = System.currentTimeMillis() + 100;
					return true;
				} else if (byLanderState == LND_ACTIVE) {
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

			setScreenText(1, df2.format(LanderY - yGroundZero - yLanderPict / 2), 0);
			setScreenText(2, df2.format(LanderVx), 0);
			setScreenText(3, df2.format(LanderVy), 0);
			setScreenText(4, df2.format(fFuel), 0);

			int yTop = yClient - ((int) LanderY + yLanderPict / 2);
			int xLeft = (int) LanderX - xLanderPict / 2;

			// Draw the landing pad
			Line line;
			for(int i = 0; i < groundPlot.size(); i++) {
				line = groundPlot.get(i);
				canvas.drawLine(line.xStart, line.yStart, line.xEnd, line.yEnd, mLandingPad);
			}
			if (mFiringMain) {
				int yTopF = yClient - ((int) LanderY - 26 + hBFlamePict.getIntrinsicHeight() / 2);
				int xLeftF = (int) LanderX - hBFlamePict.getIntrinsicWidth() / 2;
				hBFlamePict.setBounds(xLeftF, yTopF, xLeftF + hBFlamePict.getIntrinsicWidth(), yTopF + hBFlamePict.getIntrinsicHeight());
				hBFlamePict.draw(canvas);
			}
			if (mFiringLeft) {
				int yTopF = yClient - ((int) LanderY + 6 + hLFlamePict.getIntrinsicHeight() / 2);
				int xLeftF = (int) LanderX - 27 - hLFlamePict.getIntrinsicWidth() / 2;
				hLFlamePict.setBounds(xLeftF, yTopF, xLeftF + hLFlamePict.getIntrinsicWidth(), yTopF + hLFlamePict.getIntrinsicHeight());
				hLFlamePict.draw(canvas);
			}
			if (mFiringRight) {
				int yTopF = yClient - ((int) LanderY + 6 + hRFlamePict.getIntrinsicHeight() / 2);
				int xLeftF = (int) LanderX + 27 - hRFlamePict.getIntrinsicWidth() / 2;
				hRFlamePict.setBounds(xLeftF, yTopF, xLeftF + hRFlamePict.getIntrinsicWidth(), yTopF + hRFlamePict.getIntrinsicHeight());
				hRFlamePict.draw(canvas);
			}
			if (byEndGameState == END_CRASHV || byEndGameState == END_CRASHH || byEndGameState == END_CRASHS) {
				hCrash3.setBounds(xLeft, yTop, xLeft + xLanderPict, yTop + yLanderPict);
				hCrash3.draw(canvas);
			} else {
				hLanderPict.setBounds(xLeft, yTop, xLeft + xLanderPict, yTop + yLanderPict);
				hLanderPict.draw(canvas);
			}
		}
		
		/**
		 * Figures the lander state (x, y, fuel, ...) based on the passage of
		 * realtime. Does not invalidate(). Called at the start of draw().
		 * Detects the end-of-game and sets the UI to the next state.
		 */
		private void updatePhysics() {
			LanderMotion();
			float yLowerBound = yGroundZero + yLanderPict / 2;
			boolean bTouchDown = false;
			if (LanderY <= yLowerBound) {
				LanderY = yLowerBound;
				byLanderState = LND_ENDGAME;
				bTouchDown = true;
			}
			boolean outOfRange = (LanderY - yGroundZero - yLanderPict / 2 > 5000f) || (LanderY < -500f) || (Math.abs(LanderX) > 1000f);
			if (bTouchDown || outOfRange) {
				Line pad = landingPlot.get(0);
				boolean onGoal = pad.xStart <= LanderX - xLanderPict / 2 && LanderX + xLanderPict / 2 <= pad.xEnd;
				if (outOfRange)
					byEndGameState = END_OUTOFRANGE;
				else if (!onGoal)
					byEndGameState = END_CRASHS;
				else if (Math.abs(LanderVy) > fMaxLandingY)
					byEndGameState = END_CRASHV;
				else if (Math.abs(LanderVx) > fMaxLandingX)
					byEndGameState = END_CRASHH;
				else
					byEndGameState = END_SAFE;
				byLanderState = LND_INACTIVE;
				endGame();
			}
		}
		
		private void LanderMotion() {
			dt = 0.1f;
			float fMass, fBurn;
			float dVx, dVy;
			fMass = fLanderMass + fFuel;
			dVx = 0f;
			dVy = -fGravity;
			if (fFuel > 0f) {
				fBurn = 0f;
				if (mFiringMain) {
					fBurn = fBurn + fMainBurn;
					dVy += (fMainForce / fMass);
				}
				if (mFiringLeft) {
					fBurn = fBurn + fAttitudeBurn;
					dVx += (fAttitudeForce / fMass);
				}
				if (mFiringRight) {
					fBurn = fBurn + fAttitudeBurn;
					dVx -= (fAttitudeForce / fMass);
				}
				fBurn = fBurn * dt;
				if (fBurn > fFuel) fFuel = 0f;
				else fFuel = fFuel - fBurn;
			}
			LanderVy = LanderVy + (dVy * dt);
			LanderVx = LanderVx + (dVx * dt);
			LanderY = LanderY + (LanderVy * dt);
			LanderX = LanderX + (LanderVx * dt);
		}
		
		/** number of points across including two end-points (must be greater than one). */
		private static final int CRG_POINTS = 31;
		/** maximum y-variation of terrain */
		private static final int CRG_STEEPNESS = 25;
		
		void createGround() {
			groundPlot = new ArrayList<Line>();
			landingPlot = new ArrayList<Line>();
			/** Maximum height of terrain. (less than ySize) */
			int nMaxHeight = 120;
			/** size of landing pad in points. (less than CRG_POINTS) */
			int nPadSize = 4;
			int newX, newY = 0, oldX = 0, oldY = yClient - 20 - new Random().nextInt(nMaxHeight), nDy, mctySize = yClient - 20;
			int nLandingStart = new Random().nextInt(CRG_POINTS - nPadSize) + 1;
			int nInc = xClient / (CRG_POINTS - 1);
			int nIncExtra = xClient % (CRG_POINTS - 1);
			oldX = (-1 * nInc) + ((-1 * nIncExtra) / (CRG_POINTS - 1));
			for (int i = 1; i <= CRG_POINTS; i++) {
				newX = ((i - 1) * nInc) + (((i - 1) * nIncExtra) / (CRG_POINTS - 1));
				if ((i < nLandingStart) || (i >= (nLandingStart + nPadSize))) {
					nDy = new Random().nextInt(2 * CRG_STEEPNESS) - CRG_STEEPNESS;
					if (((oldY + nDy) < mctySize) && ((oldY + nDy) > (yClient - nMaxHeight)))
						newY = oldY + nDy;
					else
						newY = oldY - nDy;
				} else if (i == nLandingStart) {
					yGroundZero = yClient - oldY;
					Line line = new Line();
					line.xStart = oldX;
					line.yStart = oldY;
					line.xEnd = oldX + (nInc * nPadSize);
					line.yEnd = oldY;
					//Log.d("Lander", line.toString());
					landingPlot.add(line);
				}
				Line line = new Line();
				line.xStart = oldX;
				line.yStart = oldY;
				line.xEnd = newX;
				line.yEnd = newY;
				groundPlot.add(line);
				oldX = newX;
				oldY = newY;
			}
		}
	}
	
	class Line {
		private int xStart, yStart, xEnd, yEnd, yStartPos, yEndPos;
		@Override
		public String toString() {
			return "xStart: " + xStart + " | yStart: " + yStart + " | xEnd: " + xEnd + " | yEnd: " + yEnd;
		}
		public void translateY(int yScreen) {
			yStartPos = yScreen - yStart;
			yEndPos = yScreen - yEnd;
		}
	}
}
