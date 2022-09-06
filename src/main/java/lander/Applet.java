/*
 * Copyright 2011 Mark Injerd
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
