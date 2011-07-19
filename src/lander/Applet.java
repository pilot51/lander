package lander;

import java.awt.FlowLayout;

import javax.swing.JApplet;

public class Applet extends JApplet {
	private static final long serialVersionUID = 1L;
	private LanderView mLanderView;
	
	public void init() {
		setSize(800, 550);
		setLayout(new FlowLayout());
		mLanderView = new LanderView();
		add(mLanderView);
		add(mLanderView.panel);
	}
}
