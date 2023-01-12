/*
 * Copyright 2011-2023 Mark Injerd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pilot51.lander

import com.pilot51.lander.GroundUtils.plotArray
import com.pilot51.lander.Platform.Rendering.Color
import com.pilot51.lander.Platform.Rendering.DrawSurface
import com.pilot51.lander.Platform.Rendering.Path
import com.pilot51.lander.Platform.Resources.Icon
import com.pilot51.lander.Platform.Resources.Image
import com.pilot51.lander.Platform.Resources.string
import com.pilot51.lander.Platform.Views.Button
import com.pilot51.lander.Platform.Views.Text
import com.pilot51.lander.Res.ResImage
import com.pilot51.lander.Res.ResString
import java.text.DecimalFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

class LanderViewModel(
	private val platform: PlatformType
) {
	/* Physical Settings & Options */
	/** mass of the lander in kg */
	private val fLanderMass = 1000f

	/** kg of fuel to start */
	var fInitFuel = DEF_FUEL

	/** main engine thrust in Newtons */
	var fMainForce = DEF_THRUST

	/** attitude thruster force in Newtons */
	private val fAttitudeForce = 2000f

	/** main engine kg of fuel / second */
	private val fMainBurn = 10f

	/** attitude thruster kg of fuel / second */
	private val fAttitudeBurn = 2f

	/** gravity acceleration in m/s² */
	var fGravity = DEF_GRAVITY

	/** max horizontal velocity on landing */
	private val fMaxLandingX = 1f

	/** max vertical velocity on landing */
	private val fMaxLandingY = 10f

	/** max angle degrees on landing  */
	private val fMaxLandingA = 5f

	/** Fuel in kilograms */
	private var fFuel = 0f

	/** Lander horizontal position in meters */
	private var landerX = 0f
	/** Lander altitude in meters */
	private var landerY = 0f

	/** Lander velocity in meters/sec */
	private var landerVx = 0f
	private var landerVy = 0f

	/** time increment in seconds */
	private var dt = 0.5f

	/* Other variables */
	/** reverse side thrust buttons */
	var bReverseSideThrust = false

	/** draw flame on lander */
	var bDrawFlame = true

	/** size of full-screen window */
	var xClient = 800
	var yClient = 500

	/** lander bitmap */
	private val hLanderPict = Image(ResImage.LANDER)
	private val hBFlamePict = Image(ResImage.BFLAME)
	private val hLFlamePict = Image(ResImage.LFLAME)
	private val hRFlamePict = Image(ResImage.RFLAME)

	/** size of lander bitmap  */
	private var xLanderPict = hLanderPict.width
	private var yLanderPict = hLanderPict.height

	private val hCrash1 = Image(ResImage.CRASH1)
	private val hCrash2 = Image(ResImage.CRASH2)
	private val hCrash3 = Image(ResImage.CRASH3)

	private val hExpl = arrayOf(
		Image(ResImage.EXPL1),
		Image(ResImage.EXPL2),
		Image(ResImage.EXPL3),
		Image(ResImage.EXPL4),
		Image(ResImage.EXPL5),
		Image(ResImage.EXPL6),
		Image(ResImage.EXPL7),
		Image(ResImage.EXPL8),
		Image(ResImage.EXPL9),
		Image(ResImage.EXPL10)
	)

	private val xGroundZero = 0
	private var yGroundZero = 0

	/** Lander window state  */
	var byLanderState = LND_NEW

	/** EndGame dialog state  */
	private var byEndGameState: Byte = 0
	private var nExplCount: Byte = 0
	private var nFlameCount = FLAME_DELAY
	private var nCount = 0
	var lastUpdate: Long = 0
	private var lastDraw: Long = 0
	private var rand: Random = Random(System.currentTimeMillis())
	private val df2 = DecimalFormat("0.00") // Fixed to 2 decimal places
	private lateinit var landerPict: Image
	private var safe = Icon(ResImage.SAFE)
	private var dead = Icon(ResImage.DEAD)
	var bColorEndImg = false
		set(value) {
			field = value
			safe = Icon(if (value) ResImage.SAFE_COLOR else ResImage.SAFE)
			dead = Icon(if (value) ResImage.DEAD_COLOR else ResImage.DEAD)
		}
	var bLanderBox = true
	var bRotation = false
	lateinit var path: Path
	private lateinit var groundPlot: ArrayList<Point>
	private lateinit var contactPoints: ArrayList<Point>
	private lateinit var pointCenter: Point
	private var angle = 0f
	private var yScale = 0f
	var densityScale = 0f
	private var isFiringMain = false
	private var isFiringLeft = false
	private var isFiringRight = false

	val textAlt = Text()
	val textVelX = Text()
	val textVelY = Text()
	val textFuel = Text()
	val btnThrust = Button()
	val btnLeft = Button()
	val btnRight = Button()

	var keyCodeThrust = 0
	var keyCodeLeft = 0
	var keyCodeRight = 0
	var keyCodeNew = 0
	var keyCodeRestart = 0
	var keyCodeOptions = 0
	
	private var origBtn: Button? = null

	fun gameNew() {
		GroundUtils.currentGround = null
		byLanderState = LND_NEW
	}

	fun gameRestart() {
		byLanderState = LND_RESTART
	}

	fun gameInactive() {
		byLanderState = LND_INACTIVE
	}

	private fun setFiringThrust(firing: Boolean) {
		isFiringMain = firing
		btnThrust.isPressed = firing
	}

	private fun setFiringLeft(firing: Boolean) {
		if (bReverseSideThrust) isFiringRight = firing else isFiringLeft = firing
		btnLeft.isPressed = firing
	}

	private fun setFiringRight(firing: Boolean) {
		if (bReverseSideThrust) isFiringLeft = firing else isFiringRight = firing
		btnRight.isPressed = firing
	}

	private fun releaseAllButtons() {
		setFiringThrust(false)
		setFiringLeft(false)
		setFiringRight(false)
	}

	/**
	 * @param newClick If `true` and the lander state is [LND_HOLD], the state is changed to [LND_ACTIVE].
	 * If `false`, the target button is reactivated only if [origBtn] is set.
	 */
	fun buttonPressed(button: Button, newClick: Boolean = true) {
		if (newClick && byLanderState == LND_HOLD) byLanderState = LND_ACTIVE
		else if (byLanderState != LND_ACTIVE) return
		when (button) {
			btnThrust -> if (newClick || origBtn == btnThrust) {
				setFiringThrust(true)
			}
			btnLeft -> if (newClick || origBtn == btnLeft) {
				setFiringLeft(true)
			}
			btnRight -> if (newClick || origBtn == btnRight) {
				setFiringRight(true)
			}
		}
	}

	/**
	 * @param remember If `true`, the button is remembered so it can be reactivated
	 * if the cursor reenters the button while the mouse is still clicked.
	 */
	fun buttonReleased(button: Button, remember: Boolean = false) {
		if (byLanderState != LND_ACTIVE) return
		when (button) {
			btnThrust -> setFiringThrust(false)
			btnLeft -> setFiringLeft(false)
			btnRight -> setFiringRight(false)
		}
		if (!remember) origBtn = null
	}

	/** @return `true` if key press handled */
	fun keyPressed(keyCode: Int): Boolean {
		if (byLanderState != LND_ACTIVE) return false
		when (keyCode) {
			keyCodeThrust -> setFiringThrust(true)
			keyCodeLeft -> setFiringLeft(true)
			keyCodeRight -> setFiringRight(true)
			else -> return false
		}
		return true
	}

	/** @return `true` if key release handled */
	fun keyReleased(keyCode: Int): Boolean {
		if (byLanderState == LND_HOLD && (
			(keyCode == keyCodeThrust) ||
			(keyCode == keyCodeLeft) ||
			(keyCode == keyCodeRight)
		)) {
			byLanderState = LND_ACTIVE
			return true
		} else if (byLanderState != LND_ACTIVE) return false
		when (keyCode) {
			keyCodeThrust -> setFiringThrust(false)
			keyCodeLeft -> setFiringLeft(false)
			keyCodeRight -> setFiringRight(false)
			else -> return false
		}
		return true
	}

	private fun endGameDialog() {
		releaseAllButtons()
		var msg = "${ResString.END_CRASH.string}\n"
		when (byEndGameState) {
			END_SAFE -> msg = ResString.END_SAFE.string
			END_CRASHV -> when (Random().nextInt(3)) {
				0 -> msg += ResString.END_CRASHV1.string
				1 -> msg += ResString.END_CRASHV2.string
				2 -> msg += ResString.END_CRASHV3.string
			}
			END_CRASHH -> msg += ResString.END_CRASHH.string
			END_CRASHA -> msg += ResString.END_CRASHA.string
			END_CRASHS -> msg += ResString.END_CRASHS.string
			END_OUTOFRANGE -> msg = ResString.END_OUTOFRANGE.string
		}
		showDialog(msg)
	}

	private fun showDialog(text: String) {
		val title = text.substring(0, text.indexOf("\n"))
		val message = text.substring(text.indexOf("\n") + 1, text.length)
		val icon = if (byEndGameState == END_SAFE) safe else dead
		Platform.showMessageDialog(
			icon, title, message,
			positiveStringRes = ResString.OK,
			neutralStringRes = ResString.NEW,
			onNeutral = { gameNew() },
			negativeStringRes = ResString.RESTART,
			onNegative = { gameRestart() }
		)
	}

	private fun drawStatus(bOverride: Boolean) {
		if (nCount >= STATUS_DELAY || bOverride) {
			textAlt.text = df2.format((landerY - yGroundZero) * yScale)
			textVelX.text = df2.format(landerVx)
			textVelY.text = df2.format(landerVy)
			textFuel.text = df2.format(fFuel)
			nCount = 0
		} else nCount++
	}

	fun drawLander(drawSurface: DrawSurface) {
		xLanderPict = landerPict.width
		yLanderPict = landerPict.height
		val yTop = invertY(landerY.toInt() + yLanderPict)
		val xLeft = landerX.toInt() - xLanderPict / 2
		if (bLanderBox) {
			drawSurface.fillArea(xLeft, yTop, xLanderPict, yLanderPict, Color.BLACK)
		}
		if ((nFlameCount == 0) && bDrawFlame && (fFuel > 0f) && (byLanderState == LND_ACTIVE)) {
			var yTopF: Int
			var xLeftF: Int
			if (isFiringMain) {
				yTopF = invertY(
					landerY.toInt() + when (platform) {
						PlatformType.ANDROID -> -(11 * densityScale).toInt()
						else -> yLanderPict / 2 - 25
					} + hBFlamePict.height / 2
				)
				xLeftF = landerX.toInt() - hBFlamePict.width / 2
				hBFlamePict.draw(drawSurface, xLeftF, yTopF)
			}
			if (isFiringLeft) {
				yTopF = invertY(
					landerY.toInt() + when (platform) {
						PlatformType.ANDROID -> (21 * densityScale).toInt()
						else -> yLanderPict / 2 + 5
					} + hLFlamePict.height / 2
				)
				xLeftF = landerX.toInt() - when (platform) {
					PlatformType.ANDROID -> (27 * densityScale).toInt()
					else -> 29
				} - hLFlamePict.width / 2
				hLFlamePict.draw(drawSurface, xLeftF, yTopF)
			}
			if (isFiringRight) {
				yTopF = invertY(landerY.toInt() + when (platform) {
					PlatformType.ANDROID -> (21 * densityScale).toInt()
					else -> yLanderPict / 2 + 5
				} + hRFlamePict.height / 2)
				xLeftF = landerX.toInt() + when (platform) {
					PlatformType.ANDROID -> (27 * densityScale).toInt()
					else -> 29
				} - hRFlamePict.width / 2
				hRFlamePict.draw(drawSurface, xLeftF, yTopF)
			}
		}
		val now = System.currentTimeMillis()
		if (now - lastDraw >= UPDATE_TIME) {
			if (nFlameCount == 0) nFlameCount = FLAME_DELAY else nFlameCount--
			lastDraw = now
		}
		landerPict.draw(
			drawSurface,
			xLeft, yTop, xLanderPict, yLanderPict,
			angle, landerX, landerY, yClient
		)
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
			dVy += kotlin.math.cos(radians.toDouble()).toFloat() * accel
		}
		landerVy += dVy * dt
		landerVx += dVx * dt
		landerY += landerVy * dt / yScale
		landerX += landerVx * dt / (yScale / 2)
	}

	fun updateLander() {
		var nTimerLoop = 0
		var dwTickCount: Long = 0
		var bTimed = false
		when (byLanderState) {
			LND_NEW -> {
				createGround()
				fFuel = fInitFuel
				landerX = xClient / 2f
				landerY = 1000f / yScale + yGroundZero
				angle = 0f
				landerVx = 0f
				landerVy = 0f
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
				releaseAllButtons()
				byLanderState = LND_HOLD
			}
			LND_TIMING -> {
				drawStatus(false)
				nTimerLoop++
				if (nTimerLoop == MAX_TIMER) {
					dt = (7.5 * (System.currentTimeMillis() - dwTickCount) / (1000 * nTimerLoop)).toFloat()
					bTimed = true
					byLanderState = LND_HOLD
				}
			}
			LND_RESTART -> {
				fFuel = fInitFuel
				landerX = xClient / 2f
				landerY = 1000f / yScale + yGroundZero
				angle = 0f
				landerVx = 0f
				landerVy = 0f
				landerPict = hLanderPict
				drawStatus(true)
				releaseAllButtons()
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
					|| (abs((landerX - xClient / 2) * (yScale / 2)) > 1000f)
				) {
					byLanderState = LND_OUTOFRANGE
					drawStatus(true)
				}
			}
			LND_OUTOFRANGE -> {
				byEndGameState = END_OUTOFRANGE
				byLanderState = LND_INACTIVE
				endGameDialog()
			}
			LND_ENDGAME -> byLanderState = if (
				landedFlat() && abs(landerVy) <= fMaxLandingY
				&& abs(landerVx) <= fMaxLandingX
			) LND_SAFE else LND_CRASH1
			LND_SAFE -> {
				byEndGameState = END_SAFE
				byLanderState = LND_INACTIVE
				endGameDialog()
			}
			LND_CRASH1 -> {
				while (landerY > 0 && landerY.toInt() > pointCenter.y) {
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
		for (i in groundPlot.indices) {
			point = groundPlot[i]
			point2 = if (i + 1 < groundPlot.size) groundPlot[i + 1] else Point(0, 0)
			y1 = invertY(point.y).toFloat()
			y2 = invertY(point2.y).toFloat()
			if (left <= point.x && point.x <= right) {
				contactPoints.add(point)
				if (landerY <= y1 + 1) bTouchDown = true
			}
			if (point.x <= left && left <= point2.x) {
				val yGroundLeft = y2 - (y1 - y2) / (point.x - point2.x) * (point2.x - left)
				contactPoints.add(Point(left.toInt(), invertY(yGroundLeft.roundToInt())))
				if (landerY - yGroundLeft <= 0) bTouchDown = true
			}
			if (point.x <= landerX && landerX <= point2.x) {
				val yGroundCenter = y2 - (y1 - y2) / (point.x - point2.x) * (point2.x - landerX)
				pointCenter.y = yGroundCenter.roundToInt()
			}
			if (point.x <= right && right <= point2.x) {
				val yGroundRight = y2 - (y1 - y2) / (point.x - point2.x) * (point2.x - right)
				contactPoints.add(Point(right.toInt(), invertY(yGroundRight.roundToInt())))
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
		for (i in contactPoints.indices) {
			pointY = contactPoints[i].y
			if (i == 0) yLevel = pointY else if (yLevel != pointY) return false
		}
		return angle <= abs(fMaxLandingA)
	}

	fun createGround() {
		/** number of pixels per point interval */
		val nInc: Int
		val nIncExtra: Int
		groundPlot = ArrayList()
		var point = Point(0, yClient)
		groundPlot.add(point)
		path = Path()
		path.moveTo(point.x, point.y)
		val plot = GroundUtils.currentGround?.plotArray
		if (plot != null) {
			nInc = xClient / (plot.size - 1)
			nIncExtra = xClient % (plot.size - 1)
			for (i in 1..plot.size) {
				val x = (i - 1) * nInc + (i - 1) * nIncExtra / (plot.size - 1)
				point = Point(x, invertY(plot[i - 1]))
				groundPlot.add(point)
				path.lineTo(point.x, point.y)
			}
			yGroundZero = 0
			yScale = 1200f / (yClient - yGroundZero - yLanderPict)
		} else {
			/** size of landing pad in points. (less than CRG_POINTS) */
			val nPadSize = 4

			/** Maximum height of terrain. (less than ySize) */
			val nMaxHeight = yClient / 6
			var x: Int
			var nDy: Int
			val mctySize = invertY(5)
			var y = mctySize - rand.nextInt(nMaxHeight)

			/** point at which landing pad starts */
			val nLandingStart = rand.nextInt(CRG_POINTS - nPadSize) + 1
			nInc = xClient / (CRG_POINTS - 1)
			nIncExtra = xClient % (CRG_POINTS - 1)
			val currentPlot = IntArray(CRG_POINTS)
			for (i in 1..CRG_POINTS) {
				x = (i - 1) * nInc + (i - 1) * nIncExtra / (CRG_POINTS - 1)
				point = Point(x, y)
				currentPlot[i - 1] = invertY(y)
				groundPlot.add(point)
				path.lineTo(point.x, point.y)
				if (i < nLandingStart || i >= nLandingStart + nPadSize) {
					nDy = rand.nextInt(2 * CRG_STEEPNESS) - CRG_STEEPNESS
					y = if (y + nDy < mctySize && y + nDy > invertY(nMaxHeight)) {
						y + nDy
					} else y - nDy
				} else if (i == nLandingStart) {
					yGroundZero = invertY(y)
					yScale = 1200f / (yClient - yGroundZero - yLanderPict)
				}
			}
			GroundUtils.currentGround = Ground(currentPlot)
		}
		point = Point(xClient, yClient)
		groundPlot.add(point)
		path.lineTo(point.x, point.y)
		path.lineTo(0, yClient)
		path.close()
	}

	private fun invertY(y: Int) = yClient - y

	private class Point(var x: Int, var y: Int) {
		override fun toString() = "x: $x | y: $y"
	}

	enum class PlatformType {
		ANDROID,
		JAVA
	}

	companion object {
		private const val FLAME_DELAY = 1
		private const val STATUS_DELAY = 5

		/** number of frames in explosion */
		private const val EXPL_SEQUENCE = 10

		/** 50 milliseconds */
		const val UPDATE_TIME = 50

		/** New: begin new game */
		private const val LND_NEW: Byte = 1

		/** Timing: timing loop to determine time interval */
		private const val LND_TIMING: Byte = 2

		/** Restart: same terrain, start again */
		private const val LND_RESTART: Byte = 3

		/** Active state: lander is in the air */
		private const val LND_ACTIVE: Byte = 4

		/** End: lander touches ground */
		private const val LND_ENDGAME: Byte = 5

		/** Safe state: lander touches down safely */
		private const val LND_SAFE: Byte = 6

		/** Crash state: lander crashed on surface */
		private const val LND_CRASH1: Byte = 7
		private const val LND_CRASH2: Byte = 8
		private const val LND_CRASH3: Byte = 9

		/** Explode state: lander has crashed, explosion */
		private const val LND_EXPLODE: Byte = 10

		/** Out of range: lander out of bounds */
		private const val LND_OUTOFRANGE: Byte = 11

		/** Inactive state: lander on the ground */
		private const val LND_INACTIVE: Byte = 12

		/** Inactive state: lander not doing anything */
		private const val LND_HOLD: Byte = 13
		/* EndGame states */
		/** landed safely */
		private const val END_SAFE: Byte = 1

		/** Too much vertical velocity */
		private const val END_CRASHV: Byte = 2

		/** Too much horizontal velocity */
		private const val END_CRASHH: Byte = 3

		/** Too steep of an angle */
		private const val END_CRASHA: Byte = 4

		/** Missed the landing site */
		private const val END_CRASHS: Byte = 5

		/** Lander out of range */
		private const val END_OUTOFRANGE: Byte = 6

		/** about box */
		private const val END_ABOUT: Byte = 7

		/* Defaults */
		private const val DEF_GRAVITY = 3f
		private const val DEF_FUEL = 1000f
		private const val DEF_THRUST = 10000f

		private const val MAX_TIMER = 10

		/** number of points across including two end-points (must be greater than one). */
		private const val CRG_POINTS = 31

		/** maximum y-variation of terrain */
		private const val CRG_STEEPNESS = 25
	}
}
