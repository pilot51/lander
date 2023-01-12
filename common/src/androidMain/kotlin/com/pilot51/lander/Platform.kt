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

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.view.View.OnTouchListener
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.pilot51.lander.Platform.Resources.string
import com.pilot51.lander.Res.ResImage
import com.pilot51.lander.Res.ResString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import android.graphics.Canvas as AndCanvas
import android.graphics.Color as AndColor
import android.graphics.Path as AndPath
import android.widget.Button as AndButton

actual object Platform {
	private lateinit var weakAppContext: WeakReference<Context>
	private val appContext get() = weakAppContext.get()!!
	private lateinit var weakActContext: WeakReference<Context>
	private val actContext get() = weakActContext.get()!!
	lateinit var prefs: SharedPreferences
		private set

	fun init(activity: Activity) {
		weakAppContext = WeakReference(activity.applicationContext)
		weakActContext = WeakReference(activity)
		prefs = PreferenceManager.getDefaultSharedPreferences(appContext)
	}

	actual object Resources {
		private fun getStringId(resString: ResString) =
			appContext.resources.getIdentifier(resString.stringName, "string", appContext.packageName)
		actual val ResString.string
			get() = appContext.getString(getStringId(this))
		actual fun ResString.getString(vararg args: Any) =
			appContext.getString(getStringId(this), args)
		val ResImage.drawable get() = ContextCompat.getDrawable(appContext,
			appContext.resources.getIdentifier(fileName, "drawable", appContext.packageName))!!

		actual class Image actual constructor(resImg: ResImage) {
			private val drawable: Drawable
			actual val width get() = drawable.intrinsicWidth
			actual val height get() = drawable.intrinsicHeight

			init {
				drawable = resImg.drawable
			}

			actual fun draw(
				drawSurface: Rendering.DrawSurface,
				xLeft: Int, yTop: Int, width: Int, height: Int
			) {
				drawable.setBounds(xLeft, yTop, xLeft + width, yTop + height)
				drawable.draw(drawSurface.canvas)
			}

			actual fun draw(
				drawSurface: Rendering.DrawSurface,
				xLeft: Int, yTop: Int, width: Int, height: Int,
				angle: Float, centerX: Float, centerY: Float, screenWidth: Int
			) {
				drawable.setBounds(xLeft, yTop, xLeft + width, yTop + height)
				drawSurface.canvas.rotate(angle, centerX, screenWidth - centerY)
				drawable.draw(drawSurface.canvas)
			}
		}

		actual class Icon actual constructor(resImg: ResImage) {
			internal val drawable = resImg.drawable
		}
	}

	actual object Views {
		actual class Text {
			private lateinit var textView: TextView
			actual var text: String get() = textView.text.toString()
				set(value) { textView.text = value }

			fun init(view: TextView) {
				textView = view
			}
		}

		actual class Button {
			private lateinit var button: AndButton
			var leftMargin: Int get() = throw UnsupportedOperationException()
				set(value) {
					(button.layoutParams as RelativeLayout.LayoutParams).leftMargin = value
				}
			actual var isPressed get() = button.isPressed
				set(value) {
					CoroutineScope(Dispatchers.Main).launch {
						button.isPressed = value
					}
				}

			fun init(view: AndButton, listener: OnTouchListener) {
				button = view
				button.setOnTouchListener(listener)
				buttons.add(this)
			}

			fun setBackgroundAlpha(alpha: Int) {
				button.background.alpha = alpha
			}

			fun setSize(width: Int, height: Int = width) {
				button.layoutParams.let {
					it.width = width
					it.height = height
				}
			}

			fun btnEquals(other: AndButton?) = button == other

			actual companion object {
				internal actual val buttons = mutableListOf<Button>()

				fun get(button: AndButton) = buttons.single { it.btnEquals(button) }
			}
		}
	}

	actual object Rendering {
		actual object Color {
			actual val BLACK: Int = AndColor.BLACK
			actual val WHITE: Int = AndColor.WHITE
		}

		actual class Path {
			internal val path = AndPath().apply {
				fillType = AndPath.FillType.EVEN_ODD
			}

			actual fun lineTo(x: Int, y: Int) {
				path.lineTo(x.toFloat(), y.toFloat())
			}

			actual fun moveTo(x: Int, y: Int) {
				path.moveTo(x.toFloat(), y.toFloat())
			}

			actual fun close() {
				path.close()
			}
		}

		actual class DrawSurface(
			internal val canvas: AndCanvas
		) {
			actual fun fillSurface(color: Int) {
				canvas.drawColor(color)
			}

			actual fun fillArea(
				xLeft: Int, yTop: Int, width: Int, height: Int, color: Int
			) {
				canvas.drawRect(
					xLeft.toFloat(),
					yTop.toFloat(),
					(xLeft + width).toFloat(),
					(yTop + height).toFloat(),
					Paint().also {
						it.style = Paint.Style.FILL
						it.color = color
					}
				)
			}

			actual fun drawPath(path: Path, color: Int) {
				val paint = Paint().also {
					it.style = Paint.Style.FILL
					it.color = color
				}
				canvas.drawPath(path.path, paint)
			}
		}
	}

	/**
	 * Shows a dialog.
	 * All buttons dismiss the dialog and run an optional callback.
	 * Buttons are only included if their respective string is non-null.
	 * If all button strings are `null`, [neutralStringRes] defaults to "OK".
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
		var neutralStrRes = neutralStringRes
		if (neutralStrRes == null && positiveStringRes == null && negativeStringRes == null) {
			neutralStrRes = ResString.OK
		}
		AlertDialog.Builder(actContext).run {
			setIcon(icon.drawable)
			setTitle(title)
			setMessage(message)
			neutralStrRes?.let {
				setNeutralButton(it.string) { dialog, _ ->
					dialog.dismiss()
					onNeutral?.invoke()
				}
			}
			positiveStringRes?.let {
				setPositiveButton(it.string) { dialog, _ ->
					dialog.dismiss()
					onPositive?.invoke()
				}
			}
			negativeStringRes?.let {
				setNegativeButton(it.string) { dialog, _ ->
					dialog.dismiss()
					onNegative?.invoke()
				}
			}
			CoroutineScope(Dispatchers.Main).launch {
				show()
			}
		}
	}
}
