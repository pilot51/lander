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

import com.pilot51.lander.Platform.Resources.string
import com.pilot51.lander.Res.ResImage
import com.pilot51.lander.Res.ResString
import java.awt.Dimension
import java.awt.Graphics
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*

class LanderView : JComponent(), KeyListener, MouseListener {
	private val vm = LanderViewModel(LanderViewModel.PlatformType.JAVA)

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
		val menu = JMenu(ResString.GAME.string)
		menu.mnemonic = KeyEvent.VK_G
		menuBar.add(menu)
		var menuItem = JMenuItem(ResString.NEW.string, KeyEvent.VK_N)
		menuItem.addActionListener { vm.gameNew() }
		menuItem.accelerator = KeyStroke.getKeyStroke(vm.keyCodeNew, 0)
		menu.add(menuItem)
		menuItem = JMenuItem(ResString.RESTART.string, KeyEvent.VK_R)
		menuItem.addActionListener { vm.gameRestart() }
		menuItem.accelerator = KeyStroke.getKeyStroke(vm.keyCodeRestart, 0)
		menu.add(menuItem)
		menuItem = JMenuItem(ResString.OPTIONS_ELLIPSIS.string, KeyEvent.VK_O)
		menuItem.addActionListener {
			vm.gameInactive()
			// TODO Open options here
			vm.gameRestart()
		}
		menuItem.accelerator = KeyStroke.getKeyStroke(vm.keyCodeOptions, 0)
		menuItem.isEnabled = false
		menu.add(menuItem)
		menuItem = JMenuItem(ResString.EXIT.string, KeyEvent.VK_X)
		menuItem.isEnabled = false
		menu.add(menuItem)
		menu.addSeparator()
		menuItem = JMenuItem(ResString.ABOUT_LANDER.string, KeyEvent.VK_B)
		menuItem.isEnabled = false
		menu.add(menuItem)
	}

	public override fun paintComponent(g: Graphics) {
		super.paintComponent(g)
		vm.updateLander()
		val drawSurface = Platform.Rendering.DrawSurface(g)
		// Background
		drawSurface.fillSurface(Platform.Rendering.Color.BLACK)
		// Draw the ground
		drawSurface.drawPath(vm.path, Platform.Rendering.Color.WHITE)
		// Status labels
		drawSurface.drawString(ResString.ALTITUDE.string, vm.xClient - 158, 40, Platform.Rendering.Color.WHITE)
		drawSurface.drawString(ResString.VELOCITY_X.string, vm.xClient - 170, 60, Platform.Rendering.Color.WHITE)
		drawSurface.drawString(ResString.VELOCITY_Y.string, vm.xClient - 170, 80, Platform.Rendering.Color.WHITE)
		drawSurface.drawString(ResString.FUEL.string, vm.xClient - 137, 100, Platform.Rendering.Color.WHITE)
		vm.drawLander(drawSurface)
		try {
			Thread.sleep(LanderViewModel.UPDATE_TIME.toLong())
			repaint()
		} catch (e: InterruptedException) {
			println(e)
		}
	}

	override fun mouseClicked(arg0: MouseEvent) {}
	override fun mouseEntered(e: MouseEvent) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			vm.buttonPressed(Platform.Views.Button.get(e.component as JButton), false)
		}
	}

	override fun mouseExited(e: MouseEvent) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			vm.buttonReleased(Platform.Views.Button.get(e.component as JButton), true)
		}
	}

	override fun mousePressed(e: MouseEvent) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			vm.buttonPressed(Platform.Views.Button.get(e.component as JButton))
		}
	}

	override fun mouseReleased(e: MouseEvent) {
		if (SwingUtilities.isLeftMouseButton(e)) {
			vm.buttonReleased(Platform.Views.Button.get(e.component as JButton))
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