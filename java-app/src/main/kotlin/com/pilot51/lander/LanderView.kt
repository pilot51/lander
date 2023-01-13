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

import com.pilot51.lander.Platform.Rendering.DrawSurface
import com.pilot51.lander.Platform.Resources.string
import com.pilot51.lander.Platform.Views.Button
import com.pilot51.lander.Res.ResImage
import com.pilot51.lander.Res.ResString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*

class LanderView : JComponent(), KeyListener, MouseListener {
	private val vm = LanderViewModel()

	var menuBar = JMenuBar()

	init {
		Platform.init(this)
		vm.keyCodeThrust = KeyEvent.VK_DOWN
		vm.keyCodeLeft = KeyEvent.VK_LEFT
		vm.keyCodeRight = KeyEvent.VK_RIGHT
		vm.keyCodeNew = KeyEvent.VK_F2
		vm.keyCodeRestart = KeyEvent.VK_F3
		vm.keyCodeOptions = KeyEvent.VK_F4
		preferredSize = Dimension(vm.xClient, vm.yClient)
		createMenu()
		vm.textAlt.init(this, vm.xClient - 100, 28, 100, 20)
		vm.textVelX.init(this, vm.xClient - 100, 48, 100, 20)
		vm.textVelY.init(this, vm.xClient - 100, 68, 100, 20)
		vm.textFuel.init(this, vm.xClient - 100, 88, 100, 20)
		vm.createGround()
		vm.btnThrust.init(this, vm.xClient - 105, 160, 48, 48,
			ResImage.THRUST, ResImage.ITHRUST, this)
		vm.btnLeft.init(this, vm.xClient - 130, 110, 48, 48,
			ResImage.LEFT, ResImage.ILEFT, this)
		vm.btnRight.init(this, vm.xClient - 80, 110, 48, 48,
			ResImage.RIGHT, ResImage.IRIGHT, this)
		isFocusable = true
		addKeyListener(this)
	}

	private fun createMenu() {
		menuBar.add(JMenu(ResString.GAME.string).apply {
			mnemonic = KeyEvent.VK_G
			add(JMenuItem(ResString.NEW.string, KeyEvent.VK_N).apply {
				addActionListener { vm.gameNew() }
				accelerator = KeyStroke.getKeyStroke(vm.keyCodeNew, 0)
			})
			add(JMenuItem(ResString.RESTART.string, KeyEvent.VK_R).apply {
				addActionListener { vm.gameRestart() }
				accelerator = KeyStroke.getKeyStroke(vm.keyCodeRestart, 0)
			})
			add(JMenuItem(ResString.OPTIONS_ELLIPSIS.string, KeyEvent.VK_O).apply {
				addActionListener {
					vm.gameInactive()
					// TODO Open options here
					vm.gameRestart()
				}
				accelerator = KeyStroke.getKeyStroke(vm.keyCodeOptions, 0)
				isEnabled = false
			})
			add(JMenuItem(ResString.EXIT.string, KeyEvent.VK_X).apply {
				isEnabled = false
			})
			addSeparator()
			add(JMenuItem(ResString.ABOUT_LANDER.string, KeyEvent.VK_B).apply {
				isEnabled = false
			})
		})
	}

	public override fun paintComponent(g: Graphics) {
		super.paintComponent(g)
		CoroutineScope(Dispatchers.Unconfined).launch {
			val now = Platform.currentTimeMillis
			if (now - vm.lastUpdate >= LanderViewModel.UPDATE_TIME) {
				vm.updateLander()
				vm.lastUpdate = now
			}
			runBlocking(Dispatchers.Default) {
				DrawSurface(g).apply {
					// Background
					fillSurface()
					// Ground
					drawPath(vm.path)
					// Status labels
					drawString(ResString.ALTITUDE.string, vm.xClient - 178, 40)
					drawString(ResString.VELOCITY_X.string, vm.xClient - 189, 60)
					drawString(ResString.VELOCITY_Y.string, vm.xClient - 189, 80)
					drawString(ResString.FUEL.string, vm.xClient - 156, 100)
					vm.drawLander(this)
				}
			}
			Utils.capFrameRate(120)
			repaint()
		}
	}

	override fun mouseClicked(arg0: MouseEvent) {}
	override fun mouseEntered(e: MouseEvent) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			vm.buttonPressed(Button.get(e.component as JButton), false)
		}
	}

	override fun mouseExited(e: MouseEvent) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			vm.buttonReleased(Button.get(e.component as JButton), true)
		}
	}

	override fun mousePressed(e: MouseEvent) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			vm.buttonPressed(Button.get(e.component as JButton))
		}
	}

	override fun mouseReleased(e: MouseEvent) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			vm.buttonReleased(Button.get(e.component as JButton))
		}
	}

	override fun keyPressed(ke: KeyEvent) {
		vm.keyPressed(ke.keyCode)
	}

	override fun keyReleased(ke: KeyEvent) {
		vm.keyReleased(ke.keyCode)
	}

	override fun keyTyped(ke: KeyEvent) {}

	companion object {
		private const val serialVersionUID = 1L
	}
}