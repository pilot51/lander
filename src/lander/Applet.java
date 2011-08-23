package lander;

import java.awt.FlowLayout;

import javax.swing.JApplet;

public class Applet extends JApplet {
	private static final long serialVersionUID = 1L;
	private LanderView mLanderView;
	
	public void init() {
		setLayout(new FlowLayout());
		mLanderView = new LanderView();
		setJMenuBar(mLanderView.menuBar);
		add(mLanderView);
		setSize(800, 526);
	}
}
