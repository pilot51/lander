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
import com.pilot51.lander.Platform.Views.Text
import com.pilot51.lander.Res.ResString
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.events.MouseEvent
import kotlin.experimental.and

class LanderView {
	private val vm = LanderViewModel()
	private val drawSurface = DrawSurface(getDiv("container"), vm.xClient, vm.yClient)
	private val btnGame = getButton("btnGame")
	private val menuNew = getAnchor("gameNew")
	private val menuRestart = getAnchor("gameRestart")
	private val gameDropdown = getDiv("gameDropdown")

	init {
		getDiv("navbar").style.width = "${vm.xClient}px"
		vm.keyCodeThrust = 40 // Down
		vm.keyCodeLeft = 37 // Left
		vm.keyCodeRight = 39 // Right
		vm.keyCodeNew = 113 // F2
		vm.keyCodeRestart = 114 // F3
		vm.keyCodeOptions = 115 // F4
		vm.createGround()
		// Status labels
		Text().apply {
			init(getDiv("labelAlt"), vm.xClient - 179, 28)
			text = ResString.ALTITUDE.string
		}
		Text().apply {
			init(getDiv("labelVelX"), vm.xClient - 191, 48)
			text = ResString.VELOCITY_X.string
		}
		Text().apply {
			init(getDiv("labelVelY"), vm.xClient - 191, 68)
			text = ResString.VELOCITY_Y.string
		}
		Text().apply {
			init(getDiv("labelFuel"), vm.xClient - 156, 88)
			text = ResString.FUEL.string
		}
		vm.textAlt.init(getDiv("textAlt"), vm.xClient - 100, 28)
		vm.textVelX.init(getDiv("textVelX"), vm.xClient - 100, 48)
		vm.textVelY.init(getDiv("textVelY"), vm.xClient - 100, 68)
		vm.textFuel.init(getDiv("textFuel"), vm.xClient - 100, 88)
		vm.btnThrust.init(getInput("btnThrust"), vm.xClient - 105, 160,
			Res.ResImage.THRUST, Res.ResImage.ITHRUST, ::onMouse)
		vm.btnLeft.init(getInput("btnLeft"), vm.xClient - 130, 110,
			Res.ResImage.LEFT, Res.ResImage.ILEFT, ::onMouse)
		vm.btnRight.init(getInput("btnRight"), vm.xClient - 80, 110,
			Res.ResImage.RIGHT, Res.ResImage.IRIGHT, ::onMouse)
		setListeners()
		window.onload = {
			CoroutineScope(Dispatchers.Default).launch {
				update()
			}
		}
	}

	private suspend fun update() {
		val now = Platform.currentTimeMillis
		if (now - vm.lastUpdate >= LanderViewModel.UPDATE_TIME) {
			vm.updateLander()
			vm.lastUpdate = now
		}
		withContext(Dispatchers.Main) {
			drawSurface.run {
				// Background
				fillSurface()
				// Ground
				drawPath(vm.path)
				vm.drawLander(this)
			}
		}
		Utils.capFrameRate(120)
		update()
	}

	private fun setListeners() {
		document.addEventListener("keydown", {
			keyPressHandler("keydown", it as KeyboardEvent)
		})
		document.addEventListener("keyup", {
			keyPressHandler("keyup", it as KeyboardEvent)
		})
		btnGame.addEventListener("click", {
			it.stopPropagation()
			toggleGameMenu()
		})
		document.body!!.addEventListener("click", {
			if (gameDropdown.classList.contains("show")) toggleGameMenu()
		})
		menuNew.addEventListener("click", {
			vm.gameNew()
		})
		menuRestart.addEventListener("click", {
			vm.gameRestart()
		})
	}

	private fun keyPressHandler(type: String, event: KeyboardEvent) {
		if (event.which.isAny(
				vm.keyCodeThrust, vm.keyCodeLeft, vm.keyCodeRight,
				vm.keyCodeNew, vm.keyCodeRestart, vm.keyCodeOptions
		)) {
			event.preventDefault()
		} else return
		if (event.repeat) return
		when (type) {
			"keydown" -> vm.keyPressed(event.which)
			"keyup" -> vm.keyReleased(event.which)
		}
	}

	private fun onMouse(event: MouseEvent) {
		val button = Button.get(event.target as HTMLInputElement)
		val isLeftPressed = event.isLeftButtonPressed()
		when (event.type) {
			"mousedown" -> if (isLeftPressed) vm.buttonPressed(button)
			"mouseup" -> if (!isLeftPressed) vm.buttonReleased(button)
			"mouseenter" -> if (isLeftPressed) vm.buttonPressed(button, false)
			"mouseleave" -> if (isLeftPressed) vm.buttonReleased(button, true)
		}
	}

	private fun MouseEvent.isLeftButtonPressed(): Boolean {
		val leftMouseId = 1.toShort()
		val isLeftPressed = buttons and leftMouseId == leftMouseId
		return isLeftPressed && !(metaKey || ctrlKey || altKey || shiftKey)
	}

	private fun toggleGameMenu() {
		gameDropdown.classList.toggle("show")
	}

	private fun getDiv(id: String) = document.getElementById(id) as HTMLDivElement
	private fun getAnchor(id: String) = document.getElementById(id) as HTMLAnchorElement
	private fun getButton(id: String) = document.getElementById(id) as HTMLButtonElement
	private fun getInput(id: String) = document.getElementById(id) as HTMLInputElement

	private fun <T> T.isAny(vararg objects: T) = objects.contains(this)
}
