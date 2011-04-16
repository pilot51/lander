package lander;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class Main extends JFrame {
	LanderView mLanderView;
	JButton btnNew, btnRestart, btnOptions;
	JPanel panel;
	private static final int
		HANDLE_ALT = 1,
		HANDLE_VELX = 2,
		HANDLE_VELY = 3,
		HANDLE_FUEL = 4,
		HANDLE_DIALOG = 5,
		HANDLE_THRUST = 6,
		HANDLE_LEFT = 7,
		HANDLE_RIGHT = 8;
	
	public static void main(String[] args) {
		new Main();
	}

	Main() {
		super("Lander");
		panel = new JPanel();
		btnNew = new JButton("New");
		btnRestart = new JButton("Restart");
		btnOptions = new JButton("Options");
		setLayout(new FlowLayout());
		mLanderView = new LanderView();
		panel.add(btnNew);
		panel.add(btnRestart);
		panel.add(btnOptions);
		add(mLanderView);
		add(panel);
		setSize(800, 600);
		setVisible(true);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	public class LanderView extends JComponent implements KeyListener, ActionListener {
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
		private Image hLanderPict;
		private Image hLFlamePict, hRFlamePict, hBFlamePict;

		/** size of lander bitmap */
		private int xLanderPict, yLanderPict;

		private Image hCrash1, hCrash2, hCrash3;
		private Image[] hExpl;
		//private Drawable hExpl[EXPL_SEQUENCE];

		private int xGroundZero, yGroundZero;
		/** Lander window state */
		protected byte byLanderState = LND_NEW;
		/** EndGame dialog state */
		private byte byEndGameState;
		private byte nExplCount;
		
		private int nFlameCount = FLAME_DELAY;
		private int nCount = 0;
		private long lastUpdate, lastDraw;
		private Random rand;
		
		private DecimalFormat df2 = new DecimalFormat("0.00"); // Fixed to 2 decimal places

		private Image landerPict;
		private boolean bLanderBox;
		private Rectangle2D landerRect = new Rectangle();
		private Path2D path;
		private Paint paintWhite,
			paintBlack;
		private ArrayList<Point> groundPlot, contactPoints;
		private Point pointCenter;
		
		private float scaleY;

		private boolean mFiringMain;
		private boolean mFiringLeft;
		private boolean mFiringRight;

		LanderView() {
			fGravity = 3;
			fInitFuel = 1000;
			fMainForce = 10000;
			bDrawFlame = true;
			bReverseSideThrust = false;
			bLanderBox = true;
			btnNew.addActionListener(this);
			btnRestart.addActionListener(this);
			btnOptions.addActionListener(this);
			rand = new Random(System.currentTimeMillis());
			try {
				hLanderPict = ImageIO.read(new File("img/lander.png"));
				hBFlamePict = ImageIO.read(new File("img/bflame.png"));
				hLFlamePict = ImageIO.read(new File("img/lflame.png"));
				hRFlamePict = ImageIO.read(new File("img/rflame.png"));
				hCrash1 = ImageIO.read(new File("img/crash1.png"));
				hCrash2 = ImageIO.read(new File("img/crash2.png"));
				hCrash3 = ImageIO.read(new File("img/crash3.png"));
				hExpl = new Image[] {ImageIO.read(new File("img/expl1.png")),
						ImageIO.read(new File("img/expl2.png")),
						ImageIO.read(new File("img/expl3.png")),
						ImageIO.read(new File("img/expl4.png")),
						ImageIO.read(new File("img/expl5.png")),
						ImageIO.read(new File("img/expl6.png")),
						ImageIO.read(new File("img/expl7.png")),
						ImageIO.read(new File("img/expl8.png")),
						ImageIO.read(new File("img/expl9.png")),
						ImageIO.read(new File("img/expl10.png"))};
			} catch (IOException e) {
				e.printStackTrace();
			}
			xLanderPict = hLanderPict.getWidth(null);
			yLanderPict = hLanderPict.getHeight(null);
			addKeyListener(this);
			xClient = 780;
			yClient = 500;
			setPreferredSize(new Dimension(xClient, yClient));
			setFocusable(true);
			createGround();
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D)g;
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, getWidth(), getHeight());
			updateLander();
			g.setColor(Color.WHITE);
			AffineTransform identity = new AffineTransform();
	        g2d.setTransform(identity);
			g2d.fill(path.createTransformedShape(null));
			drawLander(g);
			try {
				Thread.sleep(30);
				repaint();
			} catch (Exception e) {
				System.out.println(e);
			}
		}
		
		private void drawLander(Graphics g) {
			g.setColor(Color.BLACK);
			xLanderPict = landerPict.getWidth(null);
			yLanderPict = landerPict.getHeight(null);
			int yTop = invertY((int)landerY + yLanderPict);
			int xLeft = (int)landerX - xLanderPict / 2;
			if (bLanderBox) g.fillRect(xLeft, yTop, xLanderPict, yLanderPict);
			if (nFlameCount == 0 & bDrawFlame & fFuel > 0f & byLanderState == LND_ACTIVE) {
				int yTopF, xLeftF;
				if (mFiringMain) {
					yTopF = invertY((int)landerY + (yLanderPict / 2) - 25 + hBFlamePict.getHeight(null) / 2);
					xLeftF = (int)landerX - hBFlamePict.getWidth(null) / 2;
					g.drawImage(hBFlamePict, xLeftF, yTopF, hBFlamePict.getWidth(null), hBFlamePict.getHeight(null), null);
				}
				if (mFiringLeft) {
					yTopF = invertY((int)landerY + (yLanderPict / 2) + 5 + hLFlamePict.getHeight(null) / 2);
					xLeftF = (int)landerX - 29 - hLFlamePict.getWidth(null) / 2;
					g.drawImage(hLFlamePict, xLeftF, yTopF, hLFlamePict.getWidth(null), hLFlamePict.getHeight(null), null);
				}
				if (mFiringRight) {
					yTopF = invertY((int)landerY + (yLanderPict / 2) + 5 + hRFlamePict.getHeight(null) / 2);
					xLeftF = (int)landerX + 29 - hRFlamePict.getWidth(null) / 2;
					g.drawImage(hRFlamePict, xLeftF, yTopF, hRFlamePict.getWidth(null), hRFlamePict.getHeight(null), null);
				}
			}
			long now = System.currentTimeMillis();
			if (now - lastDraw >= UPDATE_TIME) {
				if (nFlameCount == 0) nFlameCount = FLAME_DELAY;
				else nFlameCount--;
				lastDraw = now;
			}
			g.drawImage(landerPict, xLeft, yTop, xLanderPict, yLanderPict, null);
		}
		
		private void setFiringThrust(boolean firing) {
			mFiringMain = firing;
			//setBtnState(HANDLE_THRUST, firing);
		}

		private void setFiringLeft(boolean firing) {
			if (bReverseSideThrust)
				mFiringRight = firing;
			else mFiringLeft = firing;
			//setBtnState(HANDLE_LEFT, firing);
		}

		private void setFiringRight(boolean firing) {
			if (bReverseSideThrust)
				mFiringLeft = firing;
			else mFiringRight = firing;
			//setBtnState(HANDLE_RIGHT, firing);
		}
		
		/*private void setBtnState(int handleId, boolean pressed) {
			Message msg = mHandler.obtainMessage();
			Bundle b = new Bundle();
			b.putInt("id", handleId);
			b.putBoolean("pressed", pressed);
			msg.setData(b);
			mHandler.sendMessage(msg);
		}*/
		
		public void actionPerformed(ActionEvent ae) {
			if (ae.getSource() == btnNew) {
				byLanderState = LND_NEW;
			} else if (ae.getSource() == btnRestart) {
				byLanderState = LND_RESTART;
			} else if (ae.getSource() == btnOptions) {
				byLanderState = LND_INACTIVE;
				// Open options here
				byLanderState = LND_RESTART;
			}
			requestFocusInWindow();
		}

		public void keyPressed(KeyEvent ke) {
			if (byLanderState == LND_ACTIVE) {
				switch(ke.getKeyCode()) {
				case KeyEvent.VK_DOWN:
					setFiringThrust(true);
					break;
				case KeyEvent.VK_LEFT:
					setFiringLeft(true);
					break;
				case KeyEvent.VK_RIGHT:
					setFiringRight(true);
					break;
				}
			}
			switch(ke.getKeyCode()) {
			case KeyEvent.VK_F2:
				byLanderState = LND_NEW;
				break;
			case KeyEvent.VK_F3:
				byLanderState = LND_RESTART;
				break;
			case KeyEvent.VK_F4:
				byLanderState = LND_INACTIVE;
				// Open options here
				byLanderState = LND_RESTART;
				break;
			}
		}

		public void keyReleased(KeyEvent ke) {
			if (byLanderState == LND_HOLD & (ke.getKeyCode() == KeyEvent.VK_DOWN | ke.getKeyCode() == KeyEvent.VK_LEFT | ke.getKeyCode() == KeyEvent.VK_RIGHT)) {
				byLanderState = LND_ACTIVE;
			} else if (byLanderState == LND_ACTIVE) {
				switch(ke.getKeyCode()) {
				case KeyEvent.VK_DOWN:
					setFiringThrust(false);
					break;
				case KeyEvent.VK_LEFT:
					setFiringLeft(false);
					break;
				case KeyEvent.VK_RIGHT:
					setFiringRight(false);
					break;
				}
			}
		}

		public void keyTyped(KeyEvent ke) {}
		
		private void landerMotion() {
			float fMass, fBurn = 0f;
			float dVx, dVy;
			fMass = fLanderMass + fFuel;
			dVx = 0f;
			dVy = -fGravity;
			if (fFuel > 0f) {
				if (mFiringMain) {
					fBurn += fMainBurn;
					dVy += fMainForce / fMass;
				}
				if (mFiringLeft) {
					fBurn += fAttitudeBurn;
					dVx += fAttitudeForce / fMass;
				}
				if (mFiringRight) {
					fBurn += fAttitudeBurn;
					dVx -= fAttitudeForce / fMass;
				}
				fBurn = fBurn * dt;
				if (fBurn > fFuel) fFuel = 0f;
				else fFuel -= fBurn;
			}
			landerVy += dVy * dt;
			landerVx += dVx * dt;
			landerY += landerVy * dt / scaleY;
			landerX += landerVx * dt / (scaleY / 2);
		}
		
		private static final int MAX_TIMER = 10;
		
		private void updateLander() {
			int nTimerLoop = 0;
			long dwTickCount = 0;
			boolean bTimed = false;
			switch (byLanderState) {
				case LND_NEW:
					createGround();
					fFuel = fInitFuel;
					landerX = xClient / 2;
					landerY = (1000f / scaleY) + yGroundZero;
					landerVx = 0f;
					landerVy = 0f;
					landerPict = hLanderPict;
					if (!bTimed) {
						nTimerLoop = 0;
						dwTickCount = System.currentTimeMillis();
						byLanderState = LND_TIMING;
					} else {
						//drawStatus(false);
						byLanderState = LND_HOLD;
					}
					//drawStatus(true);
					setFiringThrust(false);
					setFiringLeft(false);
					setFiringRight(false);
					byLanderState = LND_HOLD;
					break;
				case LND_TIMING:
					//drawStatus(false);
					nTimerLoop++;
					if (nTimerLoop == MAX_TIMER) {
						dt = (float)(7.5 * (System.currentTimeMillis() - dwTickCount) / (1000 * nTimerLoop));
						bTimed = true;
						byLanderState = LND_HOLD;
					}
					break;
				case LND_RESTART:
					fFuel = fInitFuel;
					landerX = xClient / 2;
					landerY = (1000f / scaleY) + yGroundZero;
					landerVx = 0f;
					landerVy = 0f;
					landerPict = hLanderPict;
					//drawStatus(true);
					setFiringThrust(false);
					setFiringLeft(false);
					setFiringRight(false);
					byLanderState = LND_HOLD;
					break;
				case LND_HOLD:
					break;
				case LND_ACTIVE:
					landerMotion();
					//drawStatus(false);
					if (contactGround()) {
						//drawStatus(true);
						byLanderState = LND_ENDGAME;
					} else if ((landerY - yGroundZero) * scaleY > 5000f
							| (landerY - yGroundZero) * scaleY < -500f
							| Math.abs((landerX - (xClient / 2)) * (scaleY / 2)) > 1000f) {
						byLanderState = LND_OUTOFRANGE;
						//drawStatus(true);
					}
					break;
				case LND_OUTOFRANGE:
					byEndGameState = END_OUTOFRANGE;
					byLanderState = LND_INACTIVE;
					//endGameDialog();
					break;
				case LND_ENDGAME:
					if (landedFlat() && (Math.abs(landerVy) <= fMaxLandingY)
							&& (Math.abs(landerVx) <= fMaxLandingX))
						byLanderState = LND_SAFE;
					else
						byLanderState = LND_CRASH1;
					break;
				case LND_SAFE:
					byEndGameState = END_SAFE;
					byLanderState = LND_INACTIVE;
					//endGameDialog();
					break;
				case LND_CRASH1:
					while (landerY > 0 & landerY > pointCenter.y) {
						landerY--;
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
					if (nExplCount < 2*EXPL_SEQUENCE) {
						landerPict = hExpl[nExplCount/2];
						nExplCount++;
					} else if (nExplCount < 2*(EXPL_SEQUENCE+6)) {
						if (nExplCount % 2 == 0)
							landerPict = hExpl[9];
						else
							landerPict = hExpl[8];
						nExplCount++;
					} else {
						landerPict = hCrash3;
						if (Math.abs(landerVy) > fMaxLandingY)
							byEndGameState = END_CRASHV;
						else if (Math.abs(landerVx) > fMaxLandingX)
							byEndGameState = END_CRASHH;
						else byEndGameState = END_CRASHS;
						byLanderState = LND_INACTIVE;
						//endGameDialog();
					}
					break;
				case LND_INACTIVE:
					break;
			}
		}
		
		private boolean contactGround() {
			boolean bTouchDown = false;
			float left = landerX - xLanderPict / 2,
				right = landerX + xLanderPict / 2;
			float y1, y2;
			contactPoints = new ArrayList<Point>();
			Point point, point2;
			pointCenter = new Point((int)landerX, 0);
			for(int i = 0; i < groundPlot.size(); i++) {
				point = groundPlot.get(i);
				if (i+1 < groundPlot.size()) point2 = groundPlot.get(i+1);
				else point2 = new Point(0, 0);
				y1 = invertY(point.y);
				y2 = invertY(point2.y);
				if (left <= point.x & point.x <= right) {
					contactPoints.add(point);
					if (landerY <= y1 + 1)
						bTouchDown = true;
				}
				if (point.x <= left & left <= point2.x) {
					float yGroundLeft = y2 - ((y1 - y2) / (point.x - point2.x)) * (point2.x - left);
					contactPoints.add(new Point((int)left, invertY(Math.round(yGroundLeft))));
					if (landerY - yGroundLeft <= 0)
						bTouchDown = true;
				}
				if (point.x <= landerX & landerX <= point2.x) {
					float yGroundCenter = y2 - ((y1 - y2) / (point.x - point2.x)) * (point2.x - landerX);
					pointCenter.y = Math.round(yGroundCenter);
				}
				if (point.x <= right & right <= point2.x) {
					float yGroundRight = y2 - ((y1 - y2) / (point.x - point2.x)) * (point2.x - right);
					contactPoints.add(new Point((int)right, invertY(Math.round(yGroundRight))));
					if (landerY - yGroundRight <= 0)
						bTouchDown = true;
				}
				if (right < point.x) break;
			}
			if (landerY <= 0)
				bTouchDown = true;
			return bTouchDown;
		}
		
		private boolean landedFlat() {
			int pointY, yLevel = 0;
			for (int i = 0; i < contactPoints.size(); i++) {
				pointY = contactPoints.get(i).y;
				if (i == 0)
					yLevel = pointY;
				else if (yLevel != pointY)
					return false;
			}
			return true;
		}
		
		/** number of points across including two end-points (must be greater than one). */
		private static final int CRG_POINTS = 31;
		/** maximum y-variation of terrain */
		private static final int CRG_STEEPNESS = 25;
		
		private void createGround() {
			/** size of landing pad in points. (less than CRG_POINTS) */
			int nPadSize = 4;
			/** Maximum height of terrain. (less than ySize) */
			int nMaxHeight = yClient / 6;
			/** point at which landing pad starts */
			int nLandingStart;
			/** number of pixels per point interval */
			int nInc, nIncExtra;
			int x, nDy,
				mctySize = invertY(5),
				y = mctySize - rand.nextInt(nMaxHeight);
			groundPlot = new ArrayList<Point>();
			Point point = new Point(0, yClient);
			groundPlot.add(point);
			path = new Path2D.Float();
			path.moveTo(point.x, point.y);
			nLandingStart = rand.nextInt(CRG_POINTS - nPadSize) + 1;
			nInc = xClient / (CRG_POINTS - 1);
			nIncExtra = xClient % (CRG_POINTS - 1);
			for (int i = 1; i <= CRG_POINTS; i++) {
				x = ((i - 1) * nInc) + (((i - 1) * nIncExtra) / (CRG_POINTS - 1));
				point = new Point(x, y);
				groundPlot.add(point);
				path.lineTo(point.x, point.y);
				if (i < nLandingStart || i >= nLandingStart + nPadSize) {
					nDy = rand.nextInt(2 * CRG_STEEPNESS) - CRG_STEEPNESS;
					if (y + nDy < mctySize && y + nDy > invertY(nMaxHeight))
						y = y + nDy;
					else
						y = y - nDy;
				} else if (i == nLandingStart) {
					yGroundZero = invertY(y);
					scaleY = 1200f / (yClient - yGroundZero - yLanderPict);
				}
			}
			point = new Point(xClient, yClient);
			groundPlot.add(point);
			path.lineTo(point.x, point.y);
			path.lineTo(0, yClient);
			path.closePath();
		}
		
		private int invertY(int y) {
			return yClient - y;
		}
		
		private class Point {
			private int x, y;
			
			public Point(int x, int y) {
				this.x = x;
				this.y = y;
			}
			
			@Override
			public String toString() {
				return "x: " + x + " | y: " + y;
			}
		}
	}
}
