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

import com.pilot51.lander.Platform.Resources.image
import com.pilot51.lander.Res.ResImage
import com.pilot51.lander.Res.ResString
import java.awt.Component
import java.awt.Container
import java.awt.Graphics2D
import java.awt.event.MouseListener
import java.awt.geom.Path2D
import javax.imageio.ImageIO
import javax.swing.*
import java.awt.Color as JavaColor
import java.awt.Graphics as JavaGraphics
import java.awt.Image as JavaImage

actual object Platform {
	private lateinit var parentComponent: Component

	fun init(component: Component) {
		parentComponent = component
	}

	actual object Resources {
		actual val ResString.string: String
			get() = Messages.getString(stringName)
		actual fun ResString.getString(vararg args: Any) =
			String.format(string, args)
		val ResImage.image get() = ImageIO.read(
			javaClass.getResourceAsStream("/img/$fileName.png"))

		actual class Image actual constructor(resImg: ResImage) {
			private val javaImage: JavaImage = resImg.image
			actual val width get() = javaImage.getWidth(null)
			actual val height get() = javaImage.getHeight(null)

			actual fun draw(
				drawSurface: Rendering.DrawSurface,
				xLeft: Int, yTop: Int, width: Int, height: Int
			) {
				drawSurface.g.drawImage(javaImage, xLeft, yTop, width, height, null)
			}

			actual fun draw(
				drawSurface: Rendering.DrawSurface,
				xLeft: Int, yTop: Int, width: Int, height: Int,
				angle: Float, centerX: Float, centerY: Float, screenWidth: Int
			) {
				if (angle != 0f) TODO()
				drawSurface.g.drawImage(javaImage, xLeft, yTop, width, height, null)
			}
		}

		actual class Icon actual constructor(resImg: ResImage) {
			internal val javaIcon = ImageIcon(resImg.image)
		}
	}

	actual object Views {
		actual class Text {
			private val textArea = JTextArea().apply {
				isOpaque = false
				foreground = JavaColor.WHITE
			}
			actual var text: String
				get() = textArea.text
				set(value) {
					textArea.text = value
				}

			fun init(container: Container, x: Int, y: Int, width: Int, height: Int) {
				textArea.setBounds(x, y, width, height)
				container.add(textArea)
			}
		}

		actual class Button {
			private lateinit var button: JButton
			actual var isPressed get() = button.model.isPressed
				set(value) {
					button.model.isPressed = value
				}

			fun init(
				container: Container,
				x: Int, y: Int, width: Int, height: Int,
				unpressedImgRes: ResImage,
				pressedImgRes: ResImage,
				mouseListener: MouseListener
			) {
				button = JButton(ImageIcon(unpressedImgRes.image)).apply {
					pressedIcon = ImageIcon(pressedImgRes.image)
					isBorderPainted = false
					isFocusable = false
					setBounds(x, y, width, height)
					container.add(this)
					addMouseListener(mouseListener)
				}
				buttons.add(this)
			}

			fun btnEquals(other: JButton?) = button == other

			actual companion object {
				internal actual val buttons = mutableListOf<Button>()

				fun get(button: JButton) = buttons.single { it.btnEquals(button) }
			}
		}
	}


	actual object Rendering {
		actual object Color {
			actual val BLACK: Int = JavaColor.BLACK.rgb
			actual val WHITE: Int = JavaColor.WHITE.rgb
		}

		actual class Path {
			internal val path = Path2D.Float()

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
			internal val g: JavaGraphics
		) {
			private val g2d = g as Graphics2D

			actual fun fillSurface(color: Int) {
				g.color = JavaColor(color)
				val rect = g.clipBounds
				g.fillRect(0, 0, rect.width, rect.height)
			}

			actual fun fillArea(
				xLeft: Int, yTop: Int, width: Int, height: Int, color: Int
			) {
				g.color = JavaColor(color)
				g.fillRect(xLeft, yTop, width, height)
			}

			actual fun drawPath(path: Path, color: Int) {
				g.color = JavaColor(color)
				g2d.fill(path.path.createTransformedShape(null))
			}

			fun drawString(text: String, x: Int, y: Int, color: Int) {
				g.color = JavaColor(color)
				g2d.drawString(text, x, y)
			}
		}
	}

	/**
	 * Shows a dialog with an OK button.
	 * The neutral/positive/negative parameters are currently unused in Java.
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
		SwingUtilities.invokeLater {
			JOptionPane.showMessageDialog(
				parentComponent, "$title\n$message", null,
				JOptionPane.PLAIN_MESSAGE, icon.javaIcon
			)
		}
	}
}
