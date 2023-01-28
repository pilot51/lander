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

import com.pilot51.lander.Platform.Rendering.Color
import com.pilot51.lander.Platform.Resources.image
import com.pilot51.lander.Platform.Resources.src
import com.pilot51.lander.Res.ResImage
import com.pilot51.lander.Res.ResString
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.*
import org.w3c.dom.pointerevents.PointerEvent
import org.w3c.xhr.XMLHttpRequest
import kotlin.js.Date
import org.w3c.dom.Image as JsImage

actual object Platform {
	actual val currentTimeMillis get() = Date.now().toLong()

	actual object Utils {
		actual fun Number.formatFixed(precision: Int) = asDynamic().toFixed(precision) as String
	}

	actual object Resources {
		actual val ResString.string: String get() = loadJSON("strings.json")[stringName] as String
		actual fun ResString.getString(vararg args: Any) = loadJSON("strings.json")[stringName] as String
		internal val ResImage.src get() = "img/$fileName.png"
		val ResImage.image get() = JsImage().also { it.src = src }

		private fun loadJSON(filePath: String) = JSON.parse<dynamic>(
			XMLHttpRequest().run {
				open("GET", filePath, false)
				overrideMimeType("application/json")
				send()
				if (status.toInt() == 200 && readyState.toInt() == 4) responseText
				else throw RuntimeException("Failed to load file: $filePath")
			}
		)

		actual class Image actual constructor(resImg: ResImage) {
			private val jsImage: JsImage = resImg.image
			actual val width: Int get() = jsImage.width
			actual val height: Int get() = jsImage.height

			actual fun draw(
				drawSurface: Rendering.DrawSurface,
				xLeft: Int, yTop: Int, width: Int, height: Int
			) {
				drawSurface.jsCanvas2D.drawImage(jsImage, xLeft.toDouble(), yTop.toDouble())
			}

			actual fun draw(
				drawSurface: Rendering.DrawSurface,
				xLeft: Int, yTop: Int, width: Int, height: Int,
				angle: Float, centerX: Float, centerY: Float, screenWidth: Int
			) {
				if (angle != 0f) TODO()
				drawSurface.jsCanvas2D.drawImage(jsImage, xLeft.toDouble(), yTop.toDouble())
			}
		}

		actual class Icon actual constructor(resImg: ResImage) {
			internal val jsIcon = resImg.image
		}
	}

	actual object Views {
		actual class Text {
			private lateinit var textArea: HTMLElement
			actual var text: String
				get() = textArea.textContent!!
				set(value) {
					textArea.textContent = value
				}

			fun init(element: HTMLDivElement, x: Int, y: Int) {
				textArea = element
				textArea.style.apply {
					left = "${x}px"
					top = "${y}px"
					color = Color.WHITE.hex
					fontWeight = "bold"
				}
			}
		}

		actual class Button {
			private lateinit var button: HTMLInputElement
			private lateinit var unpressedImgSrc: String
			private lateinit var pressedImgSrc: String
			actual var isPressed = false
				set(value) {
					field = value
					button.src = if (value) pressedImgSrc else unpressedImgSrc
				}

			fun init(
				btn: HTMLInputElement, x: Int, y: Int,
				unpressedImgRes: ResImage,
				pressedImgRes: ResImage,
				listener: (event: PointerEvent) -> Unit
			) {
				unpressedImgSrc = unpressedImgRes.src
				pressedImgSrc = pressedImgRes.src
				button = btn.apply {
					src = unpressedImgRes.image.src
					style.apply {
						left = "${x}px"
						top = "${y}px"
					}
					oncontextmenu = { false }
					addEventListener("pointerdown", {
						listener(it as PointerEvent)
					})
					addEventListener("pointerup", {
						listener(it as PointerEvent)
					})
					addEventListener("pointerenter", {
						listener(it as PointerEvent)
					})
					addEventListener("pointerleave", {
						listener(it as PointerEvent)
					})
				}
				buttons.add(this)
			}

			fun btnEquals(other: HTMLInputElement?) = button == other

			actual companion object {
				internal actual val buttons = mutableListOf<Button>()

				fun get(button: HTMLInputElement) = buttons.single { it.btnEquals(button) }
			}
		}
	}


	actual object Rendering {
		actual class Color(
			internal val hex: String
		) {
			actual companion object {
				actual val BLACK = Color("#000000")
				actual val WHITE = Color("#FFFFFF")
			}
		}

		actual class Path {
			internal val path = Path2D()

			actual fun lineTo(x: Int, y: Int) {
				path.lineTo(x.toDouble(), y.toDouble())
			}

			actual fun moveTo(x: Int, y: Int) {
				path.moveTo(x.toDouble(), y.toDouble())
			}

			actual fun close() {
				path.closePath()
			}
		}

		actual class DrawSurface(
			container: HTMLDivElement,
			width: Int,
			height: Int,
		) {
			private val jsCanvas: HTMLCanvasElement =
				(document.createElement("canvas") as HTMLCanvasElement).also {
					it.width = width
					it.height = height
					it.style.background = Color.BLACK.hex
					container.append(it)
				}
			internal val jsCanvas2D = (jsCanvas.getContext("2d") as CanvasRenderingContext2D).apply {
				font = "bold 13px sans-serif"
			}

			actual fun fillSurface(color: Color) {
				jsCanvas2D.fillStyle = color.hex
				jsCanvas2D.fillRect(0.0, 0.0, jsCanvas.width.toDouble(), jsCanvas.height.toDouble())
			}

			actual fun fillArea(
				xLeft: Int, yTop: Int, width: Int, height: Int, color: Color
			) {
				jsCanvas2D.fillStyle = color.hex
				jsCanvas2D.fillRect(xLeft.toDouble(), yTop.toDouble(), width.toDouble(), height.toDouble())
			}

			actual fun drawPath(path: Path, color: Color) {
				jsCanvas2D.fillStyle = color.hex
				jsCanvas2D.fill(path.path)
			}

			fun drawString(text: String, x: Int, y: Int, color: Color = Color.WHITE) {
				jsCanvas2D.fillStyle = color.hex
				jsCanvas2D.fillText(text, x.toDouble(), y.toDouble())
			}
		}
	}

	/**
	 * Shows a dialog with an OK button.
	 */
	actual fun showMessageDialog(
		icon: Resources.Icon,
		title: String,
		message: String,
		neutralStringRes: ResString?,
		onNeutral: (() -> Unit)?,
		positiveStringRes: ResString?,
		onPositive: (() -> Unit)?,
		negativeStringRes: ResString?,
		onNegative: (() -> Unit)?
	) {
		window.alert("$title\n$message")
	}
}
