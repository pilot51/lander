/*
 * Copyright 2011-2022 Mark Injerd
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
package lander

import java.awt.*
import java.awt.event.*
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.io.IOException
import java.text.DecimalFormat
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.abs
import kotlin.math.roundToInt

class LanderView : JComponent(), KeyListener, MouseListener {
	/* Physical Settings & Options */
	/** mass of the lander in kg */
	private val fLanderMass = 1000f

	/** kg of fuel to start */
	private var fInitFuel = DEF_FUEL

	/** main engine thrust in Newtons */
	private var fMainForce = DEF_THRUST

	/** attitude thruster force in Newtons */
	private val fAttitudeForce = 2000f

	/** main engine kg of fuel / second */
	private val fMainBurn = 10f

	/** attitude thruster kg of fuel / second */
	private val fAttitudeBurn = 2f

	/** gravity acceleration in m/s² */
	private var fGravity = DEF_GRAVITY

	/** max horizontal velocity on landing */
	private val fMaxLandingX = 1f

	/** max vertical velocity on landing */
	private val fMaxLandingY = 10f

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
	private var bReverseSideThrust = false

	/** draw flame on lander */
	private var bDrawFlame = true

	/** size of full-screen window */
	private val xClient = 800
	private val yClient = 500

	/** lander bitmap */
	private var hLanderPict: Image? = null
	private var hLFlamePict: Image? = null
	private var hRFlamePict: Image? = null
	private var hBFlamePict: Image? = null

	/** size of lander bitmap */
	private var xLanderPict: Int
	private var yLanderPict: Int
	private var hCrash1: Image? = null
	private var hCrash2: Image? = null
	private var hCrash3: Image? = null
	private lateinit var hExpl: Array<Image>

	//private Drawable hExpl[EXPL_SEQUENCE];
	private val xGroundZero = 0
	private var yGroundZero = 0

	/** Lander window state */
	private var byLanderState = LND_NEW

	/** EndGame dialog state */
	private var byEndGameState: Byte = 0
	private var nExplCount: Byte = 0
	private var nFlameCount = FLAME_DELAY
	private var nCount = 0
	private var lastDraw: Long = 0
	private val rand: Random
	private val df2 = DecimalFormat("0.00") // Fixed to 2 decimal places
	private var landerPict: Image? = null
	private var btnLeft: JButton? = null
	private var btnRight: JButton? = null
	private var btnThrust: JButton? = null
	private var safe: ImageIcon? = null
	private var dead: ImageIcon? = null
	private val bLanderBox: Boolean
	private var path: Path2D? = null
	private var groundPlot: ArrayList<Point>? = null
	private var contactPoints: ArrayList<Point>? = null
	private var pointCenter: Point? = null
	private var scaleY = 0f
	private var isFiringMain = false
	private var isFiringLeft = false
	private var isFiringRight = false
	var menuBar = JMenuBar()

	private fun createMenu() {
		val menu = JMenu("Game")
		menu.mnemonic = KeyEvent.VK_G
		menuBar.add(menu)
		var menuItem = JMenuItem(Messages.getString("new"), KeyEvent.VK_N)
		menuItem.addActionListener { byLanderState = LND_NEW }
		menuItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0)
		menu.add(menuItem)
		menuItem = JMenuItem(Messages.getString("restart"), KeyEvent.VK_R)
		menuItem.addActionListener { byLanderState = LND_RESTART }
		menuItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0)
		menu.add(menuItem)
		menuItem = JMenuItem(Messages.getString("options") + "...", KeyEvent.VK_O)
		menuItem.addActionListener {
			byLanderState = LND_INACTIVE
			// TODO Open options here
			byLanderState = LND_RESTART
		}
		menuItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0)
		menuItem.isEnabled = false
		menu.add(menuItem)
		menuItem = JMenuItem("Exit", KeyEvent.VK_X)
		menuItem.isEnabled = false
		menu.add(menuItem)
		menu.addSeparator()
		menuItem = JMenuItem("About Lander...", KeyEvent.VK_B)
		menuItem.isEnabled = false
		menu.add(menuItem)
	}

	public override fun paintComponent(g: Graphics) {
		super.paintComponent(g)
		val g2d = g as Graphics2D
		g.setColor(Color.BLACK)
		g.fillRect(0, 0, width, height)
		updateLander()
		g.setColor(Color.WHITE)
		val identity = AffineTransform()
		g2d.transform = identity
		g2d.fill(path!!.createTransformedShape(null))
		g2d.drawString(Messages.getString("altitude"), xClient - 158, 40)
		g2d.drawString(df2.format(((landerY - yGroundZero) * scaleY).toDouble()), xClient - 100, 40)
		g2d.drawString(Messages.getString("velocity_x"), xClient - 170, 60)
		g2d.drawString(df2.format(landerVx.toDouble()), xClient - 100, 60)
		g2d.drawString(Messages.getString("velocity_y"), xClient - 170, 80)
		g2d.drawString(df2.format(landerVy.toDouble()), xClient - 100, 80)
		g2d.drawString(Messages.getString("fuel"), xClient - 137, 100)
		g2d.drawString(df2.format(fFuel.toDouble()), xClient - 100, 100)
		drawLander(g)
		try {
			Thread.sleep(UPDATE_TIME.toLong())
			repaint()
		} catch (e: InterruptedException) {
			println(e)
		}
	}

	private fun endGameDialog() {
		setFiringThrust(false)
		setFiringLeft(false)
		setFiringRight(false)
		var msg = "${Messages.getString("end_crash")}\n"
		when (byEndGameState) {
			END_SAFE -> msg = Messages.getString("end_safe")
			END_CRASHV -> when (Random().nextInt(3)) {
				0 -> msg += Messages.getString("end_crashv1")
				1 -> msg += Messages.getString("end_crashv2")
				2 -> msg += Messages.getString("end_crashv3")
			}
			END_CRASHH -> msg += Messages.getString("end_crashh")
			END_CRASHS -> msg += Messages.getString("end_crashs")
			END_OUTOFRANGE -> msg = Messages.getString("end_outofrange")
		}
		showDialog(msg)
	}

	private fun showDialog(text: String) {
		SwingUtilities.invokeLater {
			val img = if (byEndGameState == END_SAFE) safe else dead
			JOptionPane.showMessageDialog(this@LanderView, text, null, JOptionPane.PLAIN_MESSAGE, img)
		}
	}

	private fun drawStatus(bOverride: Boolean) {
		if ((nCount >= STATUS_DELAY) || bOverride) {
			//txtAlt.setText(df2.format((landerY - yGroundZero) * scaleY));
			//txtAlt.setText(df2.format(landerVx));
			//txtAlt.setText(df2.format(landerVy));
			//txtAlt.setText(df2.format(fFuel));
			nCount = 0
		} else nCount++
	}

	private fun drawLander(g: Graphics) {
		g.color = Color.BLACK
		xLanderPict = landerPict!!.getWidth(null)
		yLanderPict = landerPict!!.getHeight(null)
		val yTop = invertY(landerY.toInt() + yLanderPict)
		val xLeft = landerX.toInt() - xLanderPict / 2
		if (bLanderBox) g.fillRect(xLeft, yTop, xLanderPict, yLanderPict)
		if ((nFlameCount == 0) && bDrawFlame && (fFuel > 0f) && (byLanderState == LND_ACTIVE)) {
			var yTopF: Int
			var xLeftF: Int
			if (isFiringMain) {
				yTopF = invertY(landerY.toInt() + yLanderPict / 2 - 25 + hBFlamePict!!.getHeight(null) / 2)
				xLeftF = landerX.toInt() - hBFlamePict!!.getWidth(null) / 2
				g.drawImage(
					hBFlamePict,
					xLeftF,
					yTopF,
					hBFlamePict!!.getWidth(null),
					hBFlamePict!!.getHeight(null),
					null
				)
			}
			if (isFiringLeft) {
				yTopF = invertY(landerY.toInt() + yLanderPict / 2 + 5 + hLFlamePict!!.getHeight(null) / 2)
				xLeftF = landerX.toInt() - 29 - hLFlamePict!!.getWidth(null) / 2
				g.drawImage(
					hLFlamePict,
					xLeftF,
					yTopF,
					hLFlamePict!!.getWidth(null),
					hLFlamePict!!.getHeight(null),
					null
				)
			}
			if (isFiringRight) {
				yTopF = invertY(landerY.toInt() + yLanderPict / 2 + 5 + hRFlamePict!!.getHeight(null) / 2)
				xLeftF = landerX.toInt() + 29 - hRFlamePict!!.getWidth(null) / 2
				g.drawImage(
					hRFlamePict,
					xLeftF,
					yTopF,
					hRFlamePict!!.getWidth(null),
					hRFlamePict!!.getHeight(null),
					null
				)
			}
		}
		val now = System.currentTimeMillis()
		if (now - lastDraw >= UPDATE_TIME) {
			if (nFlameCount == 0) nFlameCount = FLAME_DELAY else nFlameCount--
			lastDraw = now
		}
		g.drawImage(landerPict, xLeft, yTop, xLanderPict, yLanderPict, null)
	}

	private fun setFiringThrust(firing: Boolean) {
		isFiringMain = firing
	}

	private fun setFiringLeft(firing: Boolean) {
		if (bReverseSideThrust) isFiringRight = firing else isFiringLeft = firing
	}

	private fun setFiringRight(firing: Boolean) {
		if (bReverseSideThrust) isFiringLeft = firing else isFiringRight = firing
	}

	private var origBtn: Component? = null
	override fun mouseClicked(arg0: MouseEvent) {}
	override fun mouseEntered(e: MouseEvent) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			if (e.component === btnLeft && origBtn === btnLeft) {
				setFiringLeft(true)
			} else if (e.component === btnRight && origBtn === btnRight) {
				setFiringRight(true)
			} else if (e.component === btnThrust && origBtn === btnThrust) {
				setFiringThrust(true)
			}
		}
	}

	override fun mouseExited(e: MouseEvent) {
		if (SwingUtilities.isLeftMouseButton(e) && byLanderState == LND_ACTIVE) {
			if (e.component === btnLeft) {
				setFiringLeft(false)
			} else if (e.component === btnRight) {
				setFiringRight(false)
			} else if (e.component === btnThrust) {
				setFiringThrust(false)
			}
		}
	}

	override fun mousePressed(e: MouseEvent) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			if (byLanderState == LND_HOLD) byLanderState = LND_ACTIVE
			if (e.component === btnLeft) {
				setFiringLeft(true)
			} else if (e.component === btnRight) {
				setFiringRight(true)
			} else if (e.component === btnThrust) {
				setFiringThrust(true)
			}
			origBtn = e.component
		}
	}

	override fun mouseReleased(e: MouseEvent) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			if (byLanderState == LND_ACTIVE) {
				if (e.component === btnLeft) {
					setFiringLeft(false)
				} else if (e.component === btnRight) {
					setFiringRight(false)
				} else if (e.component === btnThrust) {
					setFiringThrust(false)
				}
				origBtn = null
			}
		}
	}

	override fun keyPressed(ke: KeyEvent) {
		if (byLanderState == LND_ACTIVE) {
			when (ke.keyCode) {
				KeyEvent.VK_DOWN -> setFiringThrust(true)
				KeyEvent.VK_LEFT -> setFiringLeft(true)
				KeyEvent.VK_RIGHT -> setFiringRight(true)
			}
		}
	}

	override fun keyReleased(ke: KeyEvent) {
		if (byLanderState == LND_HOLD &&
			(
				(ke.keyCode == KeyEvent.VK_DOWN) ||
				(ke.keyCode == KeyEvent.VK_LEFT) ||
				(ke.keyCode == KeyEvent.VK_RIGHT)
			)
		) {
			byLanderState = LND_ACTIVE
		} else if (byLanderState == LND_ACTIVE) {
			when (ke.keyCode) {
				KeyEvent.VK_DOWN -> setFiringThrust(false)
				KeyEvent.VK_LEFT -> setFiringLeft(false)
				KeyEvent.VK_RIGHT -> setFiringRight(false)
			}
		}
	}

	override fun keyTyped(ke: KeyEvent) {}

	private fun landerMotion() {
		val fMass = fLanderMass + fFuel
		var fBurn = 0f
		var dVx = 0f
		var dVy = -fGravity
		if (fFuel > 0f) {
			if (isFiringMain) {
				fBurn += fMainBurn
				dVy += fMainForce / fMass
			}
			if (isFiringLeft) {
				fBurn += fAttitudeBurn
				dVx += fAttitudeForce / fMass
			}
			if (isFiringRight) {
				fBurn += fAttitudeBurn
				dVx -= fAttitudeForce / fMass
			}
			fBurn *= dt
			if (fBurn > fFuel) fFuel = 0f else fFuel -= fBurn
		}
		landerVy += dVy * dt
		landerVx += dVx * dt
		landerY += landerVy * dt / scaleY
		landerX += landerVx * dt / (scaleY / 2)
	}

	private fun updateLander() {
		var nTimerLoop = 0
		var dwTickCount: Long = 0
		var bTimed = false
		when (byLanderState) {
			LND_NEW -> {
				createGround()
				fFuel = fInitFuel
				landerX = (xClient / 2).toFloat()
				landerY = 1000f / scaleY + yGroundZero
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
				setFiringThrust(false)
				setFiringLeft(false)
				setFiringRight(false)
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
				landerX = (xClient / 2).toFloat()
				landerY = 1000f / scaleY + yGroundZero
				landerVx = 0f
				landerVy = 0f
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
				} else if (((landerY - yGroundZero) * scaleY > 5000f
						) || ((landerY - yGroundZero) * scaleY < -500f
						) || (abs((landerX - xClient / 2) * (scaleY / 2)) > 1000f)
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
			LND_ENDGAME -> byLanderState = if (landedFlat() && abs(landerVy) <= fMaxLandingY
				&& abs(landerVx) <= fMaxLandingX
			) LND_SAFE else LND_CRASH1
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
			LND_EXPLODE -> if (nExplCount < 2 * EXPL_SEQUENCE) {
				landerPict = hExpl[nExplCount / 2]
				nExplCount++
			} else if (nExplCount < 2 * (EXPL_SEQUENCE + 6)) {
				landerPict = if (nExplCount % 2 == 0) hExpl[9] else hExpl[8]
				nExplCount++
			} else {
				landerPict = hCrash3
				byEndGameState =
					if (abs(landerVy) > fMaxLandingY) {
						END_CRASHV
					} else if (abs(landerVx) > fMaxLandingX) {
						END_CRASHH
					} else {
						END_CRASHS
					}
				byLanderState = LND_INACTIVE
				endGameDialog()
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
		return true
	}

	private fun createGround() {
		/** size of landing pad in points. (less than CRG_POINTS) */
		val nPadSize = 4
		/** Maximum height of terrain. (less than ySize) */
		val nMaxHeight = yClient / 6
		var x: Int
		var nDy: Int
		val mctySize = invertY(5)
		var y = mctySize - rand.nextInt(nMaxHeight)
		groundPlot = ArrayList()
		var point = Point(0, yClient)
		groundPlot!!.add(point)
		path = Path2D.Float()
		path!!.moveTo(point.x.toDouble(), point.y.toDouble())
		/** point at which landing pad starts */
		val nLandingStart = rand.nextInt(CRG_POINTS - nPadSize) + 1
		/** number of pixels per point interval */
		val nInc = xClient / (CRG_POINTS - 1)
		val nIncExtra = xClient % (CRG_POINTS - 1)
		for (i in 1..CRG_POINTS) {
			x = (i - 1) * nInc + (i - 1) * nIncExtra / (CRG_POINTS - 1)
			point = Point(x, y)
			groundPlot!!.add(point)
			path!!.lineTo(point.x.toDouble(), point.y.toDouble())
			if (i < nLandingStart || i >= nLandingStart + nPadSize) {
				nDy = rand.nextInt(2 * CRG_STEEPNESS) - CRG_STEEPNESS
				y = if (y + nDy < mctySize && y + nDy > invertY(nMaxHeight)) {
					y + nDy
				} else y - nDy
			} else if (i == nLandingStart) {
				yGroundZero = invertY(y)
				scaleY = 1200f / (yClient - yGroundZero - yLanderPict)
			}
		}
		point = Point(xClient, yClient)
		groundPlot!!.add(point)
		path!!.lineTo(point.x.toDouble(), point.y.toDouble())
		path!!.lineTo(0.0, yClient.toDouble())
		path!!.closePath()
	}

	private fun invertY(y: Int) = yClient - y

	private inner class Point(var x: Int, var y: Int) {
		override fun toString() = "x: $x | y: $y"
	}

	companion object {
		private const val serialVersionUID = 1L
		private const val FLAME_DELAY = 1
		private const val STATUS_DELAY = 5

		/** number of frames in explosion */
		private const val EXPL_SEQUENCE = 10

		/** 50 milliseconds */
		private const val UPDATE_TIME = 50

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

		/** Missed the landing site */
		private const val END_CRASHS: Byte = 4

		/** Lander out of range */
		private const val END_OUTOFRANGE: Byte = 5

		/** about box */
		private const val END_ABOUT: Byte = 6

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

	init {
		preferredSize = Dimension(xClient, yClient)
		createMenu()
		rand = Random(System.currentTimeMillis())
		createGround()
		fGravity = 3f
		fInitFuel = 1000f
		fMainForce = 10000f
		bDrawFlame = true
		bReverseSideThrust = false
		bLanderBox = true
		try {
			val jClass = LanderView::class.java
			btnLeft = JButton(ImageIcon(ImageIO.read(jClass.getResourceAsStream("/img/left.png")))).apply {
				pressedIcon = ImageIcon(ImageIO.read(jClass.getResourceAsStream("/img/ileft.png")))
			}
			btnRight = JButton(ImageIcon(ImageIO.read(jClass.getResourceAsStream("/img/right.png")))).apply {
				pressedIcon = ImageIcon(ImageIO.read(jClass.getResourceAsStream("/img/iright.png")))
			}
			btnThrust = JButton(ImageIcon(ImageIO.read(jClass.getResourceAsStream("/img/thrust.png")))).apply {
				pressedIcon = ImageIcon(ImageIO.read(jClass.getResourceAsStream("/img/ithrust.png")))
			}
			hLanderPict = ImageIO.read(jClass.getResourceAsStream("/img/lander.png"))
			hBFlamePict = ImageIO.read(jClass.getResourceAsStream("/img/bflame.png"))
			hLFlamePict = ImageIO.read(jClass.getResourceAsStream("/img/lflame.png"))
			hRFlamePict = ImageIO.read(jClass.getResourceAsStream("/img/rflame.png"))
			hCrash1 = ImageIO.read(jClass.getResourceAsStream("/img/crash1.png"))
			hCrash2 = ImageIO.read(jClass.getResourceAsStream("/img/crash2.png"))
			hCrash3 = ImageIO.read(jClass.getResourceAsStream("/img/crash3.png"))
			hExpl = arrayOf(
				ImageIO.read(jClass.getResourceAsStream("/img/expl1.png")),
				ImageIO.read(jClass.getResourceAsStream("/img/expl2.png")),
				ImageIO.read(jClass.getResourceAsStream("/img/expl3.png")),
				ImageIO.read(jClass.getResourceAsStream("/img/expl4.png")),
				ImageIO.read(jClass.getResourceAsStream("/img/expl5.png")),
				ImageIO.read(jClass.getResourceAsStream("/img/expl6.png")),
				ImageIO.read(jClass.getResourceAsStream("/img/expl7.png")),
				ImageIO.read(jClass.getResourceAsStream("/img/expl8.png")),
				ImageIO.read(jClass.getResourceAsStream("/img/expl9.png")),
				ImageIO.read(jClass.getResourceAsStream("/img/expl10.png"))
			)
			safe = ImageIcon(ImageIO.read(jClass.getResourceAsStream("/img/safe.png")))
			dead = ImageIcon(ImageIO.read(jClass.getResourceAsStream("/img/dead.png")))
		} catch (e: IOException) {
			e.printStackTrace()
		}
		xLanderPict = hLanderPict!!.getWidth(null)
		yLanderPict = hLanderPict!!.getHeight(null)
		btnLeft!!.let {
			it.setBounds(xClient - 130, 110, 48, 48)
			it.isBorderPainted = false
			it.isFocusable = false
			it.addMouseListener(this)
		}
		add(btnLeft)
		btnRight!!.let {
			it.setBounds(xClient - 80, 110, 48, 48)
			it.isBorderPainted = false
			it.isFocusable = false
			it.addMouseListener(this)
		}
		add(btnRight)
		btnThrust!!.let {
			it.setBounds(xClient - 105, 160, 48, 48)
			it.isBorderPainted = false
			it.isFocusable = false
			it.addMouseListener(this)
		}
		add(btnThrust)
		addKeyListener(this)
		isFocusable = true
	}
}
