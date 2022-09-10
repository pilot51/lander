package com.pilot51.lander

import android.app.AlertDialog
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.*
import androidx.preference.PreferenceManager
import android.util.AttributeSet
import android.view.*
import android.view.View.OnTouchListener
import android.widget.Button
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.BlendModeColorFilterCompat
import androidx.core.graphics.BlendModeCompat
import java.text.DecimalFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class LanderView(
	context: Context, attrs: AttributeSet?
) : SurfaceView(context, attrs), SurfaceHolder.Callback, OnTouchListener {
	private var textAlt: TextView? = null
	private var textVelX: TextView? = null
	private var textVelY: TextView? = null
	private var textFuel: TextView? = null
	private var btnThrust: Button? = null
	private var btnLeft: Button? = null
	private var btnRight: Button? = null

	/* Physical Settings & Options */
	/** mass of the lander in kg  */
	private val fLanderMass = 1000f

	/** kg of fuel to start  */
	private var fInitFuel = DEF_FUEL

	/** main engine thrust in Newtons  */
	private var fMainForce = DEF_THRUST

	/** attitude thruster force in Newtons  */
	private val fAttitudeForce = 2000f

	/** main engine kg of fuel / second  */
	private val fMainBurn = 10f

	/** attitude thruster kg of fuel / second  */
	private val fAttitudeBurn = 2f

	/** gravity acceleration in m/s²  */
	private var fGravity = DEF_GRAVITY

	/** max horizontal velocity on landing  */
	private val fMaxLandingX = 1f

	/** max vertical velocity on landing  */
	private val fMaxLandingY = 10f

	/** max angle degrees on landing  */
	private val fMaxLandingA = 5f

	/** Fuel in kilograms  */
	private var fFuel = 0f

	/** Lander horizontal position in meters */
	private var landerX = 0f
	/** Lander altitude in meters */
	private var landerY = 0f

	/** Lander velocity in meters/sec  */
	private var landerVx = 0f
	private var landerVy = 0f

	/** time increment in seconds  */
	private var dt = 0.5f
	/* Other variables */
	/** reverse side thrust buttons  */
	private var bReverseSideThrust = false

	/** draw flame on lander  */
	private var bDrawFlame = true

	/** size of full-screen window  */
	private var xClient = 0
	private var yClient = 0

	/** lander bitmap  */
	private var hLanderPict: Drawable? = null
	private var hLFlamePict: Drawable? = null
	private var hRFlamePict: Drawable? = null
	private var hBFlamePict: Drawable? = null

	/** size of lander bitmap  */
	private var xLanderPict = 0
	private var yLanderPict = 0
	private var hCrash1: Drawable? = null
	private var hCrash2: Drawable? = null
	private var hCrash3: Drawable? = null
	private var hExpl = arrayOf(
		ContextCompat.getDrawable(context, R.drawable.expl1),
		ContextCompat.getDrawable(context, R.drawable.expl2),
		ContextCompat.getDrawable(context, R.drawable.expl3),
		ContextCompat.getDrawable(context, R.drawable.expl4),
		ContextCompat.getDrawable(context, R.drawable.expl5),
		ContextCompat.getDrawable(context, R.drawable.expl6),
		ContextCompat.getDrawable(context, R.drawable.expl7),
		ContextCompat.getDrawable(context, R.drawable.expl8),
		ContextCompat.getDrawable(context, R.drawable.expl9),
		ContextCompat.getDrawable(context, R.drawable.expl10))

	//private Drawable hExpl[EXPL_SEQUENCE];
	private val xGroundZero = 0
	private var yGroundZero = 0

	/** Lander window state  */
	var byLanderState = LND_NEW

	/** EndGame dialog state  */
	private var byEndGameState: Byte = 0
	private var nExplCount: Byte = 0
	private var nFlameCount = FLAME_DELAY
	private var nCount = 0
	private var lastUpdate: Long = 0
	private var lastDraw: Long = 0
	private var rand: Random? = null
	private val df2 = DecimalFormat("0.00") // Fixed to 2 decimal places
	private var landerPict: Drawable? = null
	private var safe: Drawable? = null
	private var dead: Drawable? = null
	private var bColorEndImg = false
	private var bLanderBox = false
	private var bRotation = false
	private var path: Path? = null
	private val paintWhite = Paint()
	private var groundPlot: ArrayList<Point>? = null
	private var contactPoints: ArrayList<Point>? = null
	private var pointCenter: Point? = null
	private var angle = 0f
	private var yScale = 0f
	private var densityScale = 0f
	private var isFiringMain = false
	private var isFiringLeft = false
	private var isFiringRight = false

	/** The thread that actually draws the animation  */
	private var thread: LanderThread

	/** Message handler used by thread to interact with TextView  */
	private val handler = object : Handler(Looper.myLooper()!!) {
		override fun handleMessage(m: Message) {
			val data = m.data
			when (data.getInt("id")) {
				HANDLE_ALT -> textAlt!!.text = data.getString("text")
				HANDLE_VELX -> textVelX!!.text = data.getString("text")
				HANDLE_VELY -> textVelY!!.text = data.getString("text")
				HANDLE_FUEL -> textFuel!!.text = data.getString("text")
				HANDLE_DIALOG -> {
					val msg = data.getString("text")
					val title = msg!!.substring(0, msg.indexOf("\n"))
					val message = msg.substring(msg.indexOf("\n") + 1, msg.length)
					val img = if (byEndGameState == END_SAFE) safe else dead
					AlertDialog.Builder(context)
						.setIcon(img)
						.setTitle(title)
						.setMessage(message)
						.setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
						.setNeutralButton(R.string.word_new) { dialog, _ ->
							dialog.dismiss()
							Ground.current = null
							byLanderState = LND_NEW
						}
						.setNegativeButton(R.string.restart) { dialog, _ ->
							dialog.dismiss()
							byLanderState = LND_RESTART
						}
						.create()
						.show()
				}
				HANDLE_THRUST -> btnThrust!!.isPressed = data.getBoolean("pressed")
				HANDLE_LEFT -> btnLeft!!.isPressed = data.getBoolean("pressed")
				HANDLE_RIGHT -> btnRight!!.isPressed = data.getBoolean("pressed")
			}
		}
	}

	/** Handle to the surface manager object we interact with  */
	private var surfaceHolder = holder.also { it.addCallback(this) }

	init {
		// create thread only; it's started in surfaceCreated()
		thread = LanderThread()
		isFocusable = true
		isFocusableInTouchMode = true
		setOnTouchListener(this)
	}

	override fun onKeyDown(keyCode: Int, msg: KeyEvent) =
		thread.doKeyDown(keyCode)

	override fun onKeyUp(keyCode: Int, msg: KeyEvent) =
		thread.doKeyUp(keyCode)

	fun setTextViewAlt(textView: TextView?) {
		textAlt = textView
	}

	fun setTextViewVelX(textView: TextView?) {
		textVelX = textView
	}

	fun setTextViewVelY(textView: TextView?) {
		textVelY = textView
	}

	fun setTextViewFuel(textView: TextView?) {
		textFuel = textView
	}

	fun setButtonThrust(btn: Button) {
		btnThrust = btn
		btn.setOnTouchListener(this)
	}

	fun setButtonLeft(btn: Button) {
		btnLeft = btn
		btn.setOnTouchListener(this)
	}

	fun setButtonRight(btn: Button) {
		btnRight = btn
		btn.setOnTouchListener(this)
	}

	fun setBtnMod() {
		val btnAlpha = Main.prefs!!.getFloat("BtnAlpha", 0f).toInt()
		btnThrust!!.background.alpha = btnAlpha
		btnLeft!!.background.alpha = btnAlpha
		btnRight!!.background.alpha = btnAlpha
		val scaledSize = (48 * densityScale * Main.prefs!!.getFloat("BtnScale", 0f)).roundToInt()
		val lpBtnThrust = btnThrust!!.layoutParams
		val lpBtnLeft = btnLeft!!.layoutParams
		val lpBtnRight = btnRight!!.layoutParams
		lpBtnRight.height = scaledSize
		lpBtnRight.width = lpBtnRight.height
		lpBtnLeft.height = lpBtnRight.width
		lpBtnLeft.width = lpBtnLeft.height
		lpBtnThrust.height = lpBtnLeft.width
		lpBtnThrust.width = lpBtnThrust.height
		(lpBtnThrust as RelativeLayout.LayoutParams).leftMargin = scaledSize / 2 + 1
	}

	override fun onTouch(src: View, event: MotionEvent): Boolean {
		return thread.doBtnTouch(src, event)
	}

	/* Callback invoked when the surface dimensions change. */
	override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
		thread.setSurfaceSize(width, height)
		if (!thread.isAlive) {
			thread.setRunning(true)
			thread.start()
		}
	}

	/* Callback invoked when the Surface has been created and is ready to be used. */
	override fun surfaceCreated(holder: SurfaceHolder) {
		if (thread.state == Thread.State.TERMINATED) thread = LanderThread()
	}

	/*
	 * Callback invoked when the Surface has been destroyed and must no longer
	 * be touched. WARNING: after this method returns, the Surface/Canvas must
	 * never be touched again!
	 */
	override fun surfaceDestroyed(holder: SurfaceHolder) {
		// we have to tell thread to shut down & wait for it to finish, or else
		// it might touch the Surface after we return and explode
		var retry = true
		thread.setRunning(false)
		while (retry) {
			try {
				thread.join()
				retry = false
			} catch (e: InterruptedException) {
			}
		}
	}

	inner class LanderThread : Thread() {
		/** Indicate whether the surface has been created and is ready to draw  */
		private var run = false
		private val keyThrust: Int
		private val keyLeft: Int
		private val keyRight: Int

		init {
			val prefs = PreferenceManager.getDefaultSharedPreferences(context)
			fGravity = prefs.getFloat("Gravity", 0f)
			fInitFuel = prefs.getFloat("Fuel", 0f)
			fMainForce = prefs.getFloat("Thrust", 0f)
			bDrawFlame = prefs.getBoolean("DrawFlame", false)
			bReverseSideThrust = prefs.getBoolean("ReverseSideThrust", false)
			bColorEndImg = prefs.getBoolean("ImpEndImg", false)
			bLanderBox = !prefs.getBoolean("ImpLanderAlpha", false)
			bRotation = prefs.getBoolean("ModRotation", false)
			keyThrust = prefs.getInt("KeyThrust", 0)
			keyLeft = prefs.getInt("KeyLeft", 0)
			keyRight = prefs.getInt("KeyRight", 0)
			rand = Random(System.currentTimeMillis())
			hLanderPict = ContextCompat.getDrawable(context, R.drawable.lander)
			hBFlamePict = ContextCompat.getDrawable(context, R.drawable.bflame)
			hLFlamePict = ContextCompat.getDrawable(context, R.drawable.lflame)
			hRFlamePict = ContextCompat.getDrawable(context, R.drawable.rflame)
			hCrash1 = ContextCompat.getDrawable(context, R.drawable.crash1)
			hCrash2 = ContextCompat.getDrawable(context, R.drawable.crash2)
			hCrash3 = ContextCompat.getDrawable(context, R.drawable.crash3)
			hExpl = arrayOf(ContextCompat.getDrawable(context, R.drawable.expl1),
				ContextCompat.getDrawable(context, R.drawable.expl2),
				ContextCompat.getDrawable(context, R.drawable.expl3),
				ContextCompat.getDrawable(context, R.drawable.expl4),
				ContextCompat.getDrawable(context, R.drawable.expl5),
				ContextCompat.getDrawable(context, R.drawable.expl6),
				ContextCompat.getDrawable(context, R.drawable.expl7),
				ContextCompat.getDrawable(context, R.drawable.expl8),
				ContextCompat.getDrawable(context, R.drawable.expl9),
				ContextCompat.getDrawable(context, R.drawable.expl10))
			if (bColorEndImg) {
				safe = ContextCompat.getDrawable(context, R.drawable.safe_color)
				dead = ContextCompat.getDrawable(context, R.drawable.dead_color)
			} else {
				safe = ContextCompat.getDrawable(context, R.drawable.safe)
				dead = ContextCompat.getDrawable(context, R.drawable.dead)
			}
			xLanderPict = hLanderPict!!.intrinsicWidth
			yLanderPict = hLanderPict!!.intrinsicHeight
			densityScale = context.resources.displayMetrics.density
			paintWhite.color = Color.WHITE
			paintWhite.style = Paint.Style.FILL
		}

		override fun run() {
			while (run) {
				var c: Canvas? = null
				try {
					c = surfaceHolder.lockCanvas(null)
					synchronized(surfaceHolder) {
						val now = System.currentTimeMillis()
						if (now - lastUpdate >= UPDATE_TIME) {
							updateLander()
							lastUpdate = now
						}
						if (run) doDraw(c)
					}
				} finally {
					// do this in a finally so that if an exception is thrown
					// during the above, we don't leave the Surface in an
					// inconsistent state
					if (c != null) surfaceHolder.unlockCanvasAndPost(c)
				}
			}
		}

		private fun setFiringThrust(firing: Boolean) {
			synchronized(surfaceHolder!!) {
				isFiringMain = firing
				setBtnState(HANDLE_THRUST, firing)
			}
		}

		private fun setFiringLeft(firing: Boolean) {
			synchronized(surfaceHolder!!) {
				if (bReverseSideThrust) isFiringRight = firing else isFiringLeft = firing
				setBtnState(HANDLE_LEFT, firing)
			}
		}

		private fun setFiringRight(firing: Boolean) {
			synchronized(surfaceHolder!!) {
				if (bReverseSideThrust) isFiringLeft = firing else isFiringRight = firing
				setBtnState(HANDLE_RIGHT, firing)
			}
		}

		private fun setBtnState(handleId: Int, pressed: Boolean) {
			val msg = handler.obtainMessage()
			val b = Bundle()
			b.putInt("id", handleId)
			b.putBoolean("pressed", pressed)
			msg.data = b
			handler.sendMessage(msg)
		}

		/**
		 * Used to signal the thread whether it should be running or not.
		 * Passing true allows the thread to run; passing false will shut it
		 * down if it's already running. Calling start() after this was most
		 * recently called with false will result in an immediate shutdown.
		 *
		 * @param b
		 * true to run, false to shut down
		 */
		fun setRunning(b: Boolean) {
			run = b
		}

		/* Callback invoked when the surface dimensions change. */
		fun setSurfaceSize(width: Int, height: Int) {
			// synchronized to make sure these all change atomically
			synchronized(surfaceHolder!!) {
				xClient = width
				yClient = height
			}
		}

		fun doBtnTouch(src: View, event: MotionEvent): Boolean {
			synchronized(surfaceHolder!!) {
				if (byLanderState == LND_HOLD && event.action == MotionEvent.ACTION_DOWN) {
					byLanderState = LND_ACTIVE
				}
				if (byLanderState == LND_ACTIVE) {
					if (event.action == MotionEvent.ACTION_DOWN) {
						if (src === btnThrust) {
							setFiringThrust(true)
							return true
						} else if (src === btnLeft) {
							setFiringLeft(true)
							return true
						} else if (src === btnRight) {
							setFiringRight(true)
							return true
						}
					} else if (event.action == MotionEvent.ACTION_UP) {
						if (src === btnThrust) {
							setFiringThrust(false)
							return true
						} else if (src === btnLeft) {
							setFiringLeft(false)
							return true
						} else if (src === btnRight) {
							setFiringRight(false)
							return true
						}
					}
				}
				return false
			}
		}

		fun doKeyDown(keyCode: Int): Boolean {
			synchronized(surfaceHolder!!) {
				if (byLanderState == LND_ACTIVE) {
					when (keyCode) {
						keyThrust -> {
							setFiringThrust(true)
							return true
						}
						keyLeft -> {
							setFiringLeft(true)
							return true
						}
						keyRight -> {
							setFiringRight(true)
							return true
						}
					}
				}
				return false
			}
		}

		fun doKeyUp(keyCode: Int): Boolean {
			synchronized(surfaceHolder!!) {
				if (byLanderState == LND_HOLD
					&& ((keyCode == keyThrust) || (keyCode == keyLeft) || (keyCode == keyRight))
				) {
					byLanderState = LND_ACTIVE
					return true
				} else if (byLanderState == LND_ACTIVE) {
					when (keyCode) {
						keyThrust -> {
							setFiringThrust(false)
							return true
						}
						keyLeft -> {
							setFiringLeft(false)
							return true
						}
						keyRight -> {
							setFiringRight(false)
							return true
						}
					}
				}
				return false
			}
		}

		private fun endGameDialog() {
			synchronized(surfaceHolder!!) {
				setFiringThrust(false)
				setFiringLeft(false)
				setFiringRight(false)
				var msg = "${resources.getString(R.string.end_crash)}\n"
				when (byEndGameState) {
					END_SAFE -> msg = resources.getString(R.string.end_safe)
					END_CRASHV -> when (Random().nextInt(3)) {
						0 -> msg += resources.getString(R.string.end_crashv1)
						1 -> msg += resources.getString(R.string.end_crashv2)
						2 -> msg += resources.getString(R.string.end_crashv3)
					}
					END_CRASHH -> msg += resources.getString(R.string.end_crashh)
					END_CRASHA -> msg += resources.getString(R.string.end_crasha)
					END_CRASHS -> msg += resources.getString(R.string.end_crashs)
					END_OUTOFRANGE -> msg = resources.getString(R.string.end_outofrange)
				}
				setScreenText(HANDLE_DIALOG, msg)
			}
		}

		private fun setScreenText(handleId: Int, text: String) {
			val msg = handler.obtainMessage()
			val b = Bundle()
			b.putInt("id", handleId)
			b.putString("text", text)
			msg.data = b
			handler.sendMessage(msg)
		}

		private fun drawStatus(bOverride: Boolean) {
			if (nCount >= STATUS_DELAY || bOverride) {
				setScreenText(HANDLE_ALT, df2.format((landerY - yGroundZero) * yScale.toDouble()))
				setScreenText(HANDLE_VELX, df2.format(landerVx.toDouble()))
				setScreenText(HANDLE_VELY, df2.format(landerVy.toDouble()))
				setScreenText(HANDLE_FUEL, df2.format(fFuel.toDouble()))
				nCount = 0
			} else nCount++
		}

		private fun doDraw(canvas: Canvas?) {
			canvas!!.drawColor(Color.BLACK)
			// Draw the ground
			canvas.drawPath(path!!, paintWhite)
			xLanderPict = landerPict!!.intrinsicWidth
			yLanderPict = landerPict!!.intrinsicHeight
			val yTop = invertY(landerY.toInt() + yLanderPict)
			val xLeft = landerX.toInt() - xLanderPict / 2
			if (bLanderBox) {
				landerPict!!.colorFilter = BlendModeColorFilterCompat
					.createBlendModeColorFilterCompat(Color.BLACK, BlendModeCompat.DST_OVER)
			} else landerPict!!.colorFilter = null
			landerPict!!.setBounds(xLeft, yTop, xLeft + xLanderPict, yTop + yLanderPict)
			canvas.rotate(angle, landerX, yClient - landerY)
			landerPict!!.draw(canvas)
			if ((nFlameCount == 0) && bDrawFlame && (fFuel > 0f) && (byLanderState == LND_ACTIVE)) {
				var yTopF: Int
				var xLeftF: Int
				if (isFiringMain) {
					yTopF = invertY(landerY.toInt() - (11 * densityScale).toInt() + hBFlamePict!!.intrinsicHeight / 2)
					xLeftF = landerX.toInt() - hBFlamePict!!.intrinsicWidth / 2
					hBFlamePict!!.setBounds(xLeftF, yTopF, xLeftF + hBFlamePict!!.intrinsicWidth, yTopF + hBFlamePict!!.intrinsicHeight)
					hBFlamePict!!.draw(canvas)
				}
				if (isFiringLeft) {
					yTopF = invertY(landerY.toInt() + (21 * densityScale).toInt() + hLFlamePict!!.intrinsicHeight / 2)
					xLeftF = landerX.toInt() - (27 * densityScale).toInt() - hLFlamePict!!.intrinsicWidth / 2
					hLFlamePict!!.setBounds(xLeftF, yTopF, xLeftF + hLFlamePict!!.intrinsicWidth, yTopF + hLFlamePict!!.intrinsicHeight)
					hLFlamePict!!.draw(canvas)
				}
				if (isFiringRight) {
					yTopF = invertY(landerY.toInt() + (21 * densityScale).toInt() + hRFlamePict!!.intrinsicHeight / 2)
					xLeftF = landerX.toInt() + (27 * densityScale).toInt() - hRFlamePict!!.intrinsicWidth / 2
					hRFlamePict!!.setBounds(xLeftF, yTopF, xLeftF + hRFlamePict!!.intrinsicWidth, yTopF + hRFlamePict!!.intrinsicHeight)
					hRFlamePict!!.draw(canvas)
				}
			}
			val now = System.currentTimeMillis()
			if (now - lastDraw >= UPDATE_TIME) {
				if (nFlameCount == 0) nFlameCount = FLAME_DELAY else nFlameCount--
				lastDraw = now
			}
		}

		private fun landerMotion() {
			val fMass = fLanderMass + fFuel
			var fBurn = 0f
			var dVx = 0f
			var dVy = -fGravity
			var accel = 0f
			if (fFuel > 0f) {
				if (isFiringMain) {
					fBurn += fMainBurn
					if (bRotation) accel = fMainForce / fMass else dVy += fMainForce / fMass
				}
				if (isFiringLeft) {
					fBurn += fAttitudeBurn
					if (bRotation) angle++ else dVx += fAttitudeForce / fMass
				}
				if (isFiringRight) {
					fBurn += fAttitudeBurn
					if (bRotation) angle-- else dVx -= fAttitudeForce / fMass
				}
				fBurn *= dt
				if (fBurn > fFuel) fFuel = 0f else fFuel -= fBurn
			}
			if (bRotation) {
				if (angle <= -180) angle += 360f else if (angle > 180) angle -= 360f
				val radians = 2 * Math.PI.toFloat() * angle / 360
				dVx = sin(radians.toDouble()).toFloat() * accel
				dVy += cos(radians.toDouble()).toFloat() * accel
			}
			landerVy += dVy * dt
			landerVx += dVx * dt
			landerY += landerVy * dt / yScale
			landerX += landerVx * dt / (yScale / 2)
		}

		private fun updateLander() {
			var nTimerLoop = 0
			var dwTickCount: Long = 0
			var bTimed = false
			when (byLanderState) {
				LND_NEW -> {
					createGround()
					fFuel = fInitFuel
					landerX = xClient / 2.toFloat()
					landerY = 1000f / yScale + yGroundZero
					run {
						angle = 0f
						landerVy = angle
						landerVx = landerVy
					}
					landerPict = hLanderPict
					if (!bTimed) {
						nTimerLoop = 0
						dwTickCount = System.currentTimeMillis()
						byLanderState = LND_TIMING
					} else {
						drawStatus(false)
						byLanderState = LND_HOLD
					}
					drawStatus(true)
					setFiringThrust(false)
					setFiringLeft(false)
					setFiringRight(false)
					byLanderState = LND_HOLD
				}
				LND_TIMING -> {
					drawStatus(false)
					nTimerLoop++
					if (nTimerLoop == Companion.MAX_TIMER) {
						dt = (7.5 * (System.currentTimeMillis() - dwTickCount) / (1000 * nTimerLoop)).toFloat()
						bTimed = true
						byLanderState = LND_HOLD
					}
				}
				LND_RESTART -> {
					fFuel = fInitFuel
					landerX = xClient / 2.toFloat()
					landerY = 1000f / yScale + yGroundZero
					run {
						angle = 0f
						landerVy = angle
						landerVx = landerVy
					}
					landerPict = hLanderPict
					drawStatus(true)
					setFiringThrust(false)
					setFiringLeft(false)
					setFiringRight(false)
					byLanderState = LND_HOLD
				}
				LND_HOLD -> {}
				LND_ACTIVE -> {
					landerMotion()
					drawStatus(false)
					if (contactGround()) {
						drawStatus(true)
						byLanderState = LND_ENDGAME
					} else if (((landerY - yGroundZero) * yScale > 5000f)
						|| ((landerY - yGroundZero) * yScale < -500f)
						|| (abs((landerX - xClient / 2) * (yScale / 2)) > 1000f))
					{
						byLanderState = LND_OUTOFRANGE
						drawStatus(true)
					}
				}
				LND_OUTOFRANGE -> {
					byEndGameState = END_OUTOFRANGE
					byLanderState = LND_INACTIVE
					endGameDialog()
				}
				LND_ENDGAME -> byLanderState = if (landedFlat() && abs(landerVy) <= fMaxLandingY
					&& abs(landerVx) <= fMaxLandingX) LND_SAFE else LND_CRASH1
				LND_SAFE -> {
					byEndGameState = END_SAFE
					byLanderState = LND_INACTIVE
					endGameDialog()
				}
				LND_CRASH1 -> {
					while (landerY > 0 && landerY.toInt() > pointCenter!!.y) {
						landerY--
					}
					landerPict = hCrash1
					byLanderState = LND_CRASH2
				}
				LND_CRASH2 -> {
					landerPict = hCrash2
					byLanderState = LND_CRASH3
				}
				LND_CRASH3 -> {
					landerPict = hCrash3
					nExplCount = 0
					byLanderState = LND_EXPLODE
				}
				LND_EXPLODE -> when {
					nExplCount < 2 * EXPL_SEQUENCE -> {
						landerPict = hExpl[nExplCount / 2]
						nExplCount++
					}
					nExplCount < 2 * (EXPL_SEQUENCE + 6) -> {
						landerPict = if (nExplCount % 2 == 0) hExpl[9] else hExpl[8]
						nExplCount++
					}
					else -> {
						landerPict = hCrash3
						byEndGameState = when {
							abs(landerVy) > fMaxLandingY -> END_CRASHV
							abs(landerVx) > fMaxLandingX -> END_CRASHH
							angle > abs(fMaxLandingA) -> END_CRASHA
							else -> END_CRASHS
						}
						byLanderState = LND_INACTIVE
						endGameDialog()
					}
				}
				LND_INACTIVE -> {}
			}
		}

		private fun contactGround(): Boolean {
			var bTouchDown = false
			val left = landerX - xLanderPict / 2
			val right = landerX + xLanderPict / 2
			var y1: Float
			var y2: Float
			contactPoints = ArrayList()
			var point: Point
			var point2: Point
			pointCenter = Point(landerX.toInt(), 0)
			for (i in groundPlot!!.indices) {
				point = groundPlot!![i]
				point2 = if (i + 1 < groundPlot!!.size) groundPlot!![i + 1] else Point(0, 0)
				y1 = invertY(point.y).toFloat()
				y2 = invertY(point2.y).toFloat()
				if (left <= point.x && point.x <= right) {
					contactPoints!!.add(point)
					if (landerY <= y1 + 1) bTouchDown = true
				}
				if (point.x <= left && left <= point2.x) {
					val yGroundLeft = y2 - (y1 - y2) / (point.x - point2.x) * (point2.x - left)
					contactPoints!!.add(Point(left.toInt(), invertY(yGroundLeft.roundToInt())))
					if (landerY - yGroundLeft <= 0) bTouchDown = true
				}
				if (point.x <= landerX && landerX <= point2.x) {
					val yGroundCenter = y2 - (y1 - y2) / (point.x - point2.x) * (point2.x - landerX)
					pointCenter!!.y = yGroundCenter.roundToInt()
				}
				if (point.x <= right && right <= point2.x) {
					val yGroundRight = y2 - (y1 - y2) / (point.x - point2.x) * (point2.x - right)
					contactPoints!!.add(Point(right.toInt(), invertY(yGroundRight.roundToInt())))
					if (landerY - yGroundRight <= 0) bTouchDown = true
				}
				if (right < point.x) break
			}
			if (landerY <= 0) bTouchDown = true
			return bTouchDown
		}

		private fun landedFlat(): Boolean {
			var pointY: Int
			var yLevel = 0
			for (i in contactPoints!!.indices) {
				pointY = contactPoints!![i].y
				if (i == 0) yLevel = pointY else if (yLevel != pointY) return false
			}
			return angle <= abs(fMaxLandingA)
		}

		private fun createGround() {
			/** number of pixels per point interval  */
			val nInc: Int
			val nIncExtra: Int
			groundPlot = ArrayList()
			var point = Point(0, yClient)
			groundPlot!!.add(point)
			path = Path()
			path!!.fillType = Path.FillType.EVEN_ODD
			path!!.moveTo(point.x.toFloat(), point.y.toFloat())
			val plot = Ground.current?.plotArray
			if (plot != null) {
				nInc = xClient / (plot.size - 1)
				nIncExtra = xClient % (plot.size - 1)
				for (i in 1..plot.size) {
					val x = (i - 1) * nInc + (i - 1) * nIncExtra / (plot.size - 1)
					point = Point(x, invertY(plot[i - 1]))
					groundPlot!!.add(point)
					path!!.lineTo(point.x.toFloat(), point.y.toFloat())
				}
				yGroundZero = 0
				yScale = 1200f / (yClient - yGroundZero - yLanderPict)
			} else {
				/** size of landing pad in points. (less than CRG_POINTS)  */
				val nPadSize = 4

				/** Maximum height of terrain. (less than ySize)  */
				val nMaxHeight = yClient / 6

				var x: Int
				var nDy: Int
				val mctySize = invertY(5)
				var y = mctySize - rand!!.nextInt(nMaxHeight)

				/** point at which landing pad starts  */
				val nLandingStart: Int = rand!!.nextInt(CRG_POINTS - nPadSize) + 1
				nInc = xClient / (CRG_POINTS - 1)
				nIncExtra = xClient % (CRG_POINTS - 1)
				val currentPlot = IntArray(CRG_POINTS)
				for (i in 1..CRG_POINTS) {
					x = (i - 1) * nInc + (i - 1) * nIncExtra / (CRG_POINTS - 1)
					point = Point(x, y)
					currentPlot[i - 1] = invertY(y)
					groundPlot!!.add(point)
					path!!.lineTo(point.x.toFloat(), point.y.toFloat())
					if (i < nLandingStart || i >= nLandingStart + nPadSize) {
						nDy = rand!!.nextInt(2 * CRG_STEEPNESS) - CRG_STEEPNESS
						y = if (y + nDy < mctySize && y + nDy > invertY(nMaxHeight)) y + nDy else y - nDy
					} else if (i == nLandingStart) {
						yGroundZero = invertY(y)
						yScale = 1200f / (yClient - yGroundZero - yLanderPict)
					}
				}
				Ground.current = Ground(currentPlot)
			}
			point = Point(xClient, yClient)
			groundPlot!!.add(point)
			path!!.lineTo(point.x.toFloat(), point.y.toFloat())
			path!!.lineTo(0f, yClient.toFloat())
			path!!.close()
		}

		private fun invertY(y: Int) = yClient - y
	}

	private class Point(var x: Int, var y: Int) {
		override fun toString() = "x: $x | y: $y"
	}

	companion object {
		private const val HANDLE_ALT = 1
		private const val HANDLE_VELX = 2
		private const val HANDLE_VELY = 3
		private const val HANDLE_FUEL = 4
		private const val HANDLE_DIALOG = 5
		private const val HANDLE_THRUST = 6
		private const val HANDLE_LEFT = 7
		private const val HANDLE_RIGHT = 8
		private const val FLAME_DELAY = 1
		private const val STATUS_DELAY = 5

		/** number of frames in explosion  */
		private const val EXPL_SEQUENCE = 10

		/** 50 milliseconds  */
		private const val UPDATE_TIME = 50

		/** New: begin new game  */
		const val LND_NEW: Byte = 1

		/** Timing: timing loop to determine time interval  */
		private const val LND_TIMING: Byte = 2

		/** Restart: same terrain, start again  */
		const val LND_RESTART: Byte = 3

		/** Active state: lander is in the air  */
		private const val LND_ACTIVE: Byte = 4

		/** End: lander touches ground  */
		private const val LND_ENDGAME: Byte = 5

		/** Safe state: lander touches down safely  */
		private const val LND_SAFE: Byte = 6

		/** Crash state: lander crashed on surface  */
		private const val LND_CRASH1: Byte = 7
		private const val LND_CRASH2: Byte = 8
		private const val LND_CRASH3: Byte = 9

		/** Explode state: lander has crashed, explosion  */
		private const val LND_EXPLODE: Byte = 10

		/** Out of range: lander out of bounds  */
		private const val LND_OUTOFRANGE: Byte = 11

		/** Inactive state: lander on the ground  */
		const val LND_INACTIVE: Byte = 12

		/** Inactive state: lander not doing anything  */
		private const val LND_HOLD: Byte = 13
		/* EndGame states */
		/** landed safely  */
		private const val END_SAFE: Byte = 1

		/** Too much vertical velocity  */
		private const val END_CRASHV: Byte = 2

		/** Too much horizontal velocity  */
		private const val END_CRASHH: Byte = 3

		/** Too steep of an angle  */
		private const val END_CRASHA: Byte = 4

		/** Missed the landing site  */
		private const val END_CRASHS: Byte = 5

		/** Lander out of range  */
		private const val END_OUTOFRANGE: Byte = 6

		/** about box  */
		private const val END_ABOUT: Byte = 7

		/* Defaults */
		private const val DEF_GRAVITY = 3f
		private const val DEF_FUEL = 1000f
		private const val DEF_THRUST = 10000f

		private const val MAX_TIMER = 10

		/** number of points across including two end-points (must be greater than one).  */
		private const val CRG_POINTS = 31

		/** maximum y-variation of terrain  */
		private const val CRG_STEEPNESS = 25
	}
}
