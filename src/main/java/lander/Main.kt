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

import java.awt.FlowLayout
import javax.swing.JFrame

class Main : JFrame("Lander") {
	private val landerView = LanderView()

	init {
		layout = FlowLayout()
		jMenuBar = landerView.menuBar
		add(landerView)
		setSize(800, 556)
		isVisible = true
		defaultCloseOperation = EXIT_ON_CLOSE
	}

	companion object {
		private const val serialVersionUID = 1L

		@JvmStatic
		fun main(args: Array<String>) {
			Main()
		}
	}
}
