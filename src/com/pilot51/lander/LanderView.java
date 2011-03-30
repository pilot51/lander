package com.pilot51.lander;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
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

class LanderView extends SurfaceView implements SurfaceHolder.Callback, OnTouchListener {
	private static final int
		HANDLE_ALT = 1,
		HANDLE_VELX = 2,
		HANDLE_VELY = 3,
		HANDLE_FUEL = 4,
		HANDLE_DIALOG = 5,
		HANDLE_THRUST = 6,
		HANDLE_LEFT = 7,
		HANDLE_RIGHT = 8;
	private TextView mTextAlt, mTextVelX, mTextVelY, mTextFuel;
	private Button mBtnThrust, mBtnLeft, mBtnRight;

	/** The thread that actually draws the animation */
	private LanderThread thread;

	public LanderView(final Context context, AttributeSet attrs) {
		super(context, attrs);

		// register our interest in hearing about changes to our surface
		SurfaceHolder holder = getHolder();
		holder.addCallback(this);

		// create thread only; it's started in surfaceCreated()
		thread = new LanderThread(holder, context, new Handler() {
			@Override
			public void handleMessage(Message m) {
				Bundle data = m.getData();
				int id = data.getInt("id");
				if (id == HANDLE_ALT) {
					mTextAlt.setText(data.getString("text"));
				} else if (id == HANDLE_VELX) {
					mTextVelX.setText(data.getString("text"));
				} else if (id == HANDLE_VELY) {
					mTextVelY.setText(data.getString("text"));
				} else if (id == HANDLE_FUEL) {
					mTextFuel.setText(data.getString("text"));
				} else if (id == HANDLE_DIALOG) {
					String msg = data.getString("text"),
						title = msg.substring(0, msg.indexOf("\n")),
						message = msg.substring(msg.indexOf("\n") + 1, msg.length());
					new AlertDialog.Builder(context)
						.setIcon(getResources().getDrawable(R.drawable.icon))
						.setTitle(title)
						.setMessage(message)
						.setNeutralButton("OK", new DialogInterface.OnClickListener() {
				            public void onClick(DialogInterface dialog, int which) {
				            	dialog.cancel();
				            }})
						.create()
						.show();
				} else if (id == HANDLE_THRUST) {
					mBtnThrust.setPressed(data.getBoolean("pressed"));
				} else if (id == HANDLE_LEFT) {
					mBtnLeft.setPressed(data.getBoolean("pressed"));
				} else if (id == HANDLE_RIGHT) {
					mBtnRight.setPressed(data.getBoolean("pressed"));
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
		protected static final byte LND_INACTIVE = 12;
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

		/**
		 * Lander position in meters
		 * @param landerY
		 * 		altitude
		 */
		private float landerX, landerY;
		/** Lander velocity in meters/sec */
		private float landerVx, landerVy;
		/** time increment in seconds */
		private float dt = 0.5f;

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
		private Drawable[] hExpl;
		//private Drawable hExpl[EXPL_SEQUENCE];

		private int xGroundZero, yGroundZero;
		/** Lander window state */
		protected byte byLanderState;
		/** EndGame dialog state */
		private byte byEndGameState;
		private byte nExplCount;
		
		private int nFlameCount = FLAME_DELAY;
		private int nCount = 0;
		private long lastUpdate, lastDraw;
		
		private DecimalFormat df2 = new DecimalFormat("0.00"); // Fixed to 2 decimal places

		private Resources res;
		private Drawable landerPict;
		private Path path;
		private Paint paintWhite = new Paint();
		private LandingPad padCoords = new LandingPad();
		private ArrayList<Point> groundPlot;

		private static final String
			KEY_STATE = "byLanderState",
			KEY_END_STATE = "byEndGameState",
			KEY_DX = "landerVx",
			KEY_DY = "landerVy",
			KEY_LANDER_HEIGHT = "yLanderPict",
			KEY_LANDER_WIDTH = "xLanderPict",
			KEY_X = "landerX",
			KEY_Y = "landerY",
			KEY_FUEL = "fFuel";

		private boolean mFiringMain;
		private boolean mFiringLeft;
		private boolean mFiringRight;

		/** Message handler used by thread to interact with TextView */
		private Handler mHandler;

		/** Indicate whether the surface has been created & is ready to draw */
		private boolean mRun = false;

		/** Handle to the surface manager object we interact with */
		private SurfaceHolder mSurfaceHolder;

		private LanderThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {
			mSurfaceHolder = surfaceHolder;
			mHandler = handler;

			res = context.getResources();
			hLanderPict = res.getDrawable(R.drawable.lander);
			hBFlamePict = res.getDrawable(R.drawable.bflame);
			hLFlamePict = res.getDrawable(R.drawable.lflame);
			hRFlamePict = res.getDrawable(R.drawable.rflame);
			hCrash1 = res.getDrawable(R.drawable.crash1);
			hCrash2 = res.getDrawable(R.drawable.crash2);
			hCrash3 = res.getDrawable(R.drawable.crash3);
			hExpl = new Drawable[] {res.getDrawable(R.drawable.expl1),
					res.getDrawable(R.drawable.expl2),
					res.getDrawable(R.drawable.expl3),
					res.getDrawable(R.drawable.expl4),
					res.getDrawable(R.drawable.expl5),
					res.getDrawable(R.drawable.expl6),
					res.getDrawable(R.drawable.expl7),
					res.getDrawable(R.drawable.expl8),
					res.getDrawable(R.drawable.expl9),
					res.getDrawable(R.drawable.expl10)};
			landerPict = hLanderPict;

			xLanderPict = hLanderPict.getIntrinsicWidth();
			yLanderPict = hLanderPict.getIntrinsicHeight();

			paintWhite.setColor(Color.WHITE);
			paintWhite.setStyle(Paint.Style.FILL);

			byLanderState = LND_NEW;
		}

		@Override
		public void run() {
			while (mRun) {
				Canvas c = null;
				try {
					c = mSurfaceHolder.lockCanvas(null);
					synchronized (mSurfaceHolder) {
						//if (byLanderState == LND_ACTIVE)
						//	updatePhysics();
						long now = System.currentTimeMillis();
						if (now - lastUpdate >= UPDATE_TIME) {
							updateLander();
							lastUpdate = now;
						}
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
					map.putFloat(KEY_X, Float.valueOf(landerX));
					map.putFloat(KEY_Y, Float.valueOf(landerY));
					map.putFloat(KEY_DX, Float.valueOf(landerVx));
					map.putFloat(KEY_DY, Float.valueOf(landerVy));
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
				landerX = savedState.getFloat(KEY_X);
				landerY = savedState.getFloat(KEY_Y);
				landerVx = savedState.getFloat(KEY_DX);
				landerVy = savedState.getFloat(KEY_DY);
				xLanderPict = savedState.getInt(KEY_LANDER_WIDTH);
				yLanderPict = savedState.getInt(KEY_LANDER_HEIGHT);
				fFuel = savedState.getFloat(KEY_FUEL);
			}
		}

		private void setFiringThrust(boolean firing) {
			synchronized (mSurfaceHolder) {
				mFiringMain = firing;
				setBtnState(HANDLE_THRUST, firing);
			}
		}

		private void setFiringLeft(boolean firing) {
			synchronized (mSurfaceHolder) {
				mFiringLeft = firing;
				setBtnState(HANDLE_LEFT, firing);
			}
		}

		private void setFiringRight(boolean firing) {
			synchronized (mSurfaceHolder) {
				mFiringRight = firing;
				setBtnState(HANDLE_RIGHT, firing);
			}
		}
		
		private void setBtnState(int handleId, boolean pressed) {
			Message msg = mHandler.obtainMessage();
			Bundle b = new Bundle();
			b.putInt("id", handleId);
			b.putBoolean("pressed", pressed);
			msg.setData(b);
			mHandler.sendMessage(msg);
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
		private void setRunning(boolean b) {
			mRun = b;
		}

		/* Callback invoked when the surface dimensions change. */
		private void setSurfaceSize(int width, int height) {
			// synchronized to make sure these all change atomically
			synchronized (mSurfaceHolder) {
				xClient = width;
				yClient = height;
				// Set initial position of lander as soon as canvas size set
				landerX = xClient / 2;
				landerY = invertY(yLanderPict);
				createGround();
			}
		}

		private boolean doBtnTouch(View src, MotionEvent event) {
			synchronized (mSurfaceHolder) {
				if (byLanderState == LND_HOLD && event.getAction() == MotionEvent.ACTION_DOWN) {
					byLanderState = LND_ACTIVE;
				}
				if (byLanderState == LND_ACTIVE) {
					if (event.getAction() == MotionEvent.ACTION_DOWN) {
						if (src == mBtnThrust) {
							setFiringThrust(true);
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
							setFiringThrust(false);
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

		private boolean doKeyDown(int keyCode, KeyEvent msg) {
			synchronized (mSurfaceHolder) {
				if (byLanderState == LND_ACTIVE) {
					if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
						setFiringThrust(true);
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

		private boolean doKeyUp(int keyCode, KeyEvent msg) {
			synchronized (mSurfaceHolder) {
				if (byLanderState == LND_HOLD & (keyCode == KeyEvent.KEYCODE_DPAD_DOWN | keyCode == KeyEvent.KEYCODE_DPAD_LEFT | keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
					byLanderState = LND_ACTIVE;
					return true;
				} else if (byLanderState == LND_ACTIVE) {
					if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
						setFiringThrust(false);
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
		
		private void endGameDialog() {
			synchronized (mSurfaceHolder) {
				setFiringThrust(false);
				setFiringLeft(false);
				setFiringRight(false);
				String msg = res.getString(R.string.end_crash) + "\n";
				switch (byEndGameState) {
				case END_SAFE:
					msg = res.getString(R.string.end_safe);
					break;
				case END_CRASHV:
					switch (new Random().nextInt(3)) {
					case 0:
						msg += res.getString(R.string.end_crashv1);
						break;
					case 1:
						msg += res.getString(R.string.end_crashv2);
						break;
					case 2:
						msg += res.getString(R.string.end_crashv3);
						break;
					}
					break;
				case END_CRASHH:
					msg += res.getString(R.string.end_crashh);
					break;
				case END_CRASHS:
					msg += res.getString(R.string.end_crashs);
					break;
				case END_OUTOFRANGE:
					msg = res.getString(R.string.end_outofrange);
					break;
				}
				setScreenText(HANDLE_DIALOG, msg);
			}
		}
		
		private void setScreenText(int handleId, String text) {
			Message msg = mHandler.obtainMessage();
			Bundle b = new Bundle();
			b.putInt("id", handleId);
			b.putString("text", text);
			msg.setData(b);
			mHandler.sendMessage(msg);
		}
		
		private void drawStatus(boolean bOverride) {
			if ((nCount >= STATUS_DELAY) | bOverride) {
				setScreenText(HANDLE_ALT, df2.format(landerY - yGroundZero));
				setScreenText(HANDLE_VELX, df2.format(landerVx));
				setScreenText(HANDLE_VELY, df2.format(landerVy));
				setScreenText(HANDLE_FUEL, df2.format(fFuel));
				nCount = 0;
			} else nCount++;
		}

		private void doDraw(Canvas canvas) {
			// Draw the background image. Operations on the Canvas accumulate
			// so this is like clearing the screen.
			canvas.drawColor(Color.BLACK);

			xLanderPict = landerPict.getIntrinsicWidth();
			yLanderPict = landerPict.getIntrinsicHeight();

			int yTop = invertY((int)landerY + yLanderPict);
			int xLeft = (int)landerX - xLanderPict / 2;

			// Draw the landing pad
			canvas.drawPath(path, paintWhite);
			if (nFlameCount == 0 & bDrawFlame & fFuel > 0f & byLanderState == LND_ACTIVE) {
				int yTopF, xLeftF;
				if (mFiringMain) {
					yTopF = invertY((int)landerY - 11 + hBFlamePict.getIntrinsicHeight() / 2);
					xLeftF = (int)landerX - hBFlamePict.getIntrinsicWidth() / 2;
					hBFlamePict.setBounds(xLeftF, yTopF, xLeftF + hBFlamePict.getIntrinsicWidth(), yTopF + hBFlamePict.getIntrinsicHeight());
					hBFlamePict.draw(canvas);
				}
				if (mFiringLeft) {
					yTopF = invertY((int)landerY + 21 + hLFlamePict.getIntrinsicHeight() / 2);
					xLeftF = (int)landerX - 27 - hLFlamePict.getIntrinsicWidth() / 2;
					hLFlamePict.setBounds(xLeftF, yTopF, xLeftF + hLFlamePict.getIntrinsicWidth(), yTopF + hLFlamePict.getIntrinsicHeight());
					hLFlamePict.draw(canvas);
				}
				if (mFiringRight) {
					yTopF = invertY((int)landerY + 21 + hRFlamePict.getIntrinsicHeight() / 2);
					xLeftF = (int)landerX + 27 - hRFlamePict.getIntrinsicWidth() / 2;
					hRFlamePict.setBounds(xLeftF, yTopF, xLeftF + hRFlamePict.getIntrinsicWidth(), yTopF + hRFlamePict.getIntrinsicHeight());
					hRFlamePict.draw(canvas);
				}
			}
			long now = System.currentTimeMillis();
			if (now - lastDraw >= UPDATE_TIME) {
				if (nFlameCount == 0) nFlameCount = FLAME_DELAY;
				else nFlameCount--;
				lastDraw = now;
			}
			
			landerPict.setBounds(xLeft, yTop, xLeft + xLanderPict, yTop + yLanderPict);
			landerPict.draw(canvas);
		}

		private void landerMotion() {
			//dt = 0.1f;
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
			landerVy = landerVy + (dVy * dt);
			landerVx = landerVx + (dVx * dt);
			landerY = landerY + (landerVy * dt);
			landerX = landerX + (landerVx * dt);
		}
		
		private static final int MAX_TIMER = 10;
		
		private void updateLander() {
			boolean bTouchDown = false;
			for(int i = 0; i < groundPlot.size(); i++) {
				Point point = groundPlot.get(i);
				if (landerX - xLanderPict / 2 <= point.x & landerX + xLanderPict / 2 >= point.x) {
					if (landerY <= invertY(point.y)) {
						landerY = invertY(point.y);
						bTouchDown = true;
					}
				} else if (landerX + xLanderPict / 2 < point.x) break;
			}
			int x = 0, y = 0, z;
			int nTimerLoop = 0;
			long dwTickCount = 0;
			boolean bTimed = false;
			switch (byLanderState) {
				case LND_NEW:
					fFuel = fInitFuel;
					//landerX = 0f;
					landerX = xClient / 2;
					//landerY = 1000f;
					landerY = invertY(yLanderPict);
					landerVx = 0f;
					landerVy = 0f;
					xGroundZero = xClient / 2;
					yGroundZero = yClient - 100;
					createGround();
					landerPict = hLanderPict;
					if (!bTimed) {
						nTimerLoop = 0;
						dwTickCount = System.currentTimeMillis();
						byLanderState = LND_TIMING;
					} else {
						drawStatus(false);
						byLanderState = LND_HOLD;
					}
					drawStatus(true);
					byLanderState = LND_HOLD;
					break;
				case LND_TIMING:
					drawStatus(false);
					nTimerLoop++;
					if (nTimerLoop == MAX_TIMER) {
						dt = (float)(7.5 * (System.currentTimeMillis() - dwTickCount) / (1000 * nTimerLoop));
						bTimed = true;
						byLanderState = LND_HOLD;
					}
					break;
				case LND_RESTART:
					fFuel = fInitFuel;
					//landerX = 0f;
					landerX = xClient / 2;
					//landerY = 1000f;
					landerY = invertY(yLanderPict);
					landerVx = 0f;
					landerVy = 0f;
					landerPict = hLanderPict;
					byLanderState = LND_HOLD;
					break;
				case LND_HOLD:
					break;
				case LND_ACTIVE:
					landerMotion();
					drawStatus(false);
					if (bTouchDown) {
						drawStatus(true);
						byLanderState = LND_ENDGAME;
					} else if ((landerY > 5000f)
						|| (landerY < -500f)
						|| (Math.abs(landerX) > 1000f)) {
						byLanderState = LND_OUTOFRANGE;
						drawStatus(true);
					}
					break;
				case LND_OUTOFRANGE:
					setFiringThrust(false);
					setFiringLeft(false);
					setFiringRight(false);
					byEndGameState = END_OUTOFRANGE;
					byLanderState = LND_INACTIVE;
					endGameDialog();
					break;
				case LND_ENDGAME:
					x = xGroundZero + (int)(xGroundZero * (landerX / 600));
					y = yGroundZero - (int)(yGroundZero * (landerY / 1200));
					if (/*PtInRegion (hTerrainRgn, x + (xLanderPict / 2), y + yLanderPict)
						 && PtInRegion (hTerrainRgn, x, y + yLanderPict)
						 && PtInRegion (hTerrainRgn, x + xLanderPict, y + yLanderPict)
						 &&*/padCoords.xStart <= landerX - xLanderPict / 2
						 && landerX + xLanderPict / 2 <= padCoords.xEnd
						 && (Math.abs(landerVy) <= fMaxLandingY)
						 && (Math.abs(landerVx) <= fMaxLandingX)) {
						byLanderState = LND_SAFE;
					} else {
						byLanderState = LND_CRASH1;
					}
					break;
				case LND_SAFE:
					setFiringThrust(false);
					setFiringLeft(false);
					setFiringRight(false);
					byEndGameState = END_SAFE;
					byLanderState = LND_INACTIVE;
					endGameDialog();
					break;
				case LND_CRASH1:
					while ((y < yClient)/* && (!PtInRegion (hTerrainRgn, x + (xLanderPict /2), y + yLanderPict))*/) {
						y++;
					}
					landerPict = hCrash1;
					byLanderState = LND_CRASH2;
					break;
				case LND_CRASH2:
					landerPict = hCrash2;
					byLanderState = LND_CRASH3;
					break;
				case LND_CRASH3:
					landerPict = hCrash3;
					nExplCount = 0;
					byLanderState = LND_EXPLODE;
					break;
				case LND_EXPLODE:
					z = y - 34;
					if (nExplCount < 2*EXPL_SEQUENCE) {
						landerPict = hExpl[nExplCount/2];
						nExplCount++;
					} else if (nExplCount < 2*(EXPL_SEQUENCE+6)) {
						if (nExplCount % 2 == 0) {
							landerPict = hExpl[9];
						} else {
							landerPict = hExpl[8];
						}
						nExplCount++;
					} else {
						landerPict = hCrash3;
						setFiringThrust(false);
						setFiringLeft(false);
						setFiringRight(false);
						if (Math.abs(landerVy) > fMaxLandingY)
							byEndGameState = END_CRASHV;
						else if (Math.abs(landerVx) > fMaxLandingX)
							byEndGameState = END_CRASHH;
						else byEndGameState = END_CRASHS;
						byLanderState = LND_INACTIVE;
						endGameDialog();
					}
					break;
				case LND_INACTIVE:
					break;
			}
		}

		/** number of points across including two end-points (must be greater than one). */
		private static final int CRG_POINTS = 31;
		/** maximum y-variation of terrain */
		private static final int CRG_STEEPNESS = 25;
		
		private void createGround() {
			/** size of landing pad in points. (less than CRG_POINTS) */
			int nPadSize = 4;
			/** Maximum height of terrain. (less than ySize) */
			int nMaxHeight = 120;
			/** point at which landing pad starts */
			int nLandingStart;
			/** number of pixels per point interval */
			int nInc, nIncExtra;
			int i, x, y, nDy, mctySize;
			mctySize = yClient - 20;
			groundPlot = new ArrayList<Point>();
			Point point = new Point();
			point.x = 0;
			point.y = yClient;
			groundPlot.add(point);
			path = new Path();
			path.setFillType(Path.FillType.EVEN_ODD);
			path.moveTo(point.x, point.y);
			nLandingStart = (new Random().nextInt(32767) % (CRG_POINTS - nPadSize)) + 1;
			y = mctySize - (new Random().nextInt(32767) % nMaxHeight);
			nInc = xClient / (CRG_POINTS - 1);
			nIncExtra = xClient % (CRG_POINTS - 1);
			for (i = 1; i <= CRG_POINTS; i++) {
				x = ((i - 1) * nInc) + (((i - 1) * nIncExtra) / (CRG_POINTS - 1));
				point = new Point();
				point.x = x;
				point.y = y;
				groundPlot.add(point);
				path.lineTo(x, y);
				if ((i < nLandingStart) || (i >= (nLandingStart + nPadSize))) {
					nDy = (new Random().nextInt(32767) % (2 * CRG_STEEPNESS)) - CRG_STEEPNESS;
					if (((y + nDy) < mctySize) && ((y + nDy) > (yClient - nMaxHeight)))
						y = y + nDy;
					else
						y = y - nDy;
				} else if (i == nLandingStart) {
					yGroundZero = invertY(y);
					padCoords.xStart = x;
					padCoords.xEnd = x + (nInc * nPadSize);
					padCoords.y = y;
				}
			}
			point = new Point();
			point.x = xClient;
			point.y = yClient;
			groundPlot.add(point);
			path.lineTo(point.x, point.y);
			path.lineTo(0, yClient);
			path.close();
		}
		
		private int invertY(int y) {
			return yClient - y;
		}
	}

	private class LandingPad {
		private int xStart, xEnd, y;
		@Override
		public String toString() {
			return "start: " + xStart + " | end: " + xEnd + " | y: " + y;
		}
	}
	
	private class Point {
		private int x, y;
		@Override
		public String toString() {
			return "x: " + x + " | y: " + y;
		}
	}
}
