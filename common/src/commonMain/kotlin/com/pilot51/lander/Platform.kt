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

import com.pilot51.lander.Res.ResImage
import com.pilot51.lander.Res.ResString

expect object Platform {
	val currentTimeMillis: Long

	object Utils {
		fun Number.formatFixed(precision: Int): String
	}

	object Resources {
		val ResString.string: String
		fun ResString.getString(vararg args: Any): String

		class Image(resImg: ResImage) {
			val width: Int
			val height: Int

			fun draw(
				drawSurface: Rendering.DrawSurface,
				xLeft: Int, yTop: Int,
				width: Int = this.width,
				height: Int = this.height
			)

			fun draw(
				drawSurface: Rendering.DrawSurface,
				xLeft: Int, yTop: Int, width: Int, height: Int,
				angle: Float, centerX: Float, centerY: Float, screenWidth: Int
			)
		}

		class Icon(resImg: ResImage)
	}

	object Views {
		class Text() {
			var text: String
		}

		class Button() {
			var isPressed: Boolean

			companion object {
				internal val buttons: MutableList<Button>
			}
		}
	}

	object Rendering {
		class Color {
			companion object {
				val BLACK: Color
				val WHITE: Color
			}
		}

		class Path() {
			fun lineTo(x: Int, y: Int)
			fun moveTo(x: Int, y: Int)
			fun close()
		}

		class DrawSurface {
			fun fillSurface(color: Color = Color.BLACK)

			fun fillArea(
				xLeft: Int, yTop: Int, width: Int, height: Int, color: Color = Color.BLACK
			)

			fun drawPath(path: Path, color: Color = Color.WHITE)
		}
	}

	fun showMessageDialog(
		icon: Resources.Icon,
		title: String,
		message: String,
		neutralStringRes: ResString? = null,
		onNeutral: (() -> Unit)? = null,
		positiveStringRes: ResString? = null,
		onPositive: (() -> Unit)? = null,
		negativeStringRes: ResString? = null,
		onNegative: (() -> Unit)? = null
	)
}
