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

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.*
import android.view.View.OnTouchListener
import com.pilot51.lander.LanderViewModel.PlatformType.ANDROID
import com.pilot51.lander.Platform.Rendering.Color
import com.pilot51.lander.Platform.Rendering.DrawSurface
import com.pilot51.lander.Platform.Views.Button
import com.pilot51.lander.Platform.prefs
import kotlin.math.roundToInt
import android.widget.Button as AndButton
import com.pilot51.lander.LanderViewModel.Companion as VM

class LanderView(
	context: Context, attrs: AttributeSet?
) : SurfaceView(context, attrs), SurfaceHolder.Callback, OnTouchListener {
	val vm = LanderViewModel(ANDROID)

	/** The thread that actually draws the animation */
	private var thread = LanderThread() // create thread only; it's started in surfaceCreated()

	/** Handle to the surface manager object we interact with */
	private var surfaceHolder = holder.also { it.addCallback(this) }

	init {
		vm.fGravity = prefs.getFloat("Gravity", 0f)
		vm.fInitFuel = prefs.getFloat("Fuel", 0f)
		vm.fMainForce = prefs.getFloat("Thrust", 0f)
		vm.bDrawFlame = prefs.getBoolean("DrawFlame", false)
		vm.bReverseSideThrust = prefs.getBoolean("ReverseSideThrust", false)
		vm.bColorEndImg = prefs.getBoolean("ImpEndImg", false)
		vm.bLanderBox = !prefs.getBoolean("ImpLanderAlpha", false)
		vm.bRotation = prefs.getBoolean("ModRotation", false)
		vm.keyCodeThrust = prefs.getInt("KeyThrust", 0)
		vm.keyCodeLeft = prefs.getInt("KeyLeft", 0)
		vm.keyCodeRight = prefs.getInt("KeyRight", 0)
		vm.densityScale = context.resources.displayMetrics.density
		isFocusable = true
		isFocusableInTouchMode = true
		setOnTouchListener(this)
	}

	override fun onKeyDown(keyCode: Int, msg: KeyEvent): Boolean {
		synchronized(surfaceHolder!!) {
			return vm.keyPressed(keyCode)
		}
	}

	override fun onKeyUp(keyCode: Int, msg: KeyEvent): Boolean {
		synchronized(surfaceHolder!!) {
			return vm.keyReleased(keyCode)
		}
	}

	fun setBtnMod() {
		val btnAlpha = prefs.getFloat("BtnAlpha", 0f).toInt()
		vm.btnThrust.setBackgroundAlpha(btnAlpha)
		vm.btnLeft.setBackgroundAlpha(btnAlpha)
		vm.btnRight.setBackgroundAlpha(btnAlpha)
		val scaledSize = (48 * vm.densityScale * prefs.getFloat("BtnScale", 0f)).roundToInt()
		vm.btnRight.setSize(scaledSize)
		vm.btnLeft.setSize(scaledSize)
		vm.btnThrust.setSize(scaledSize)
		vm.btnThrust.leftMargin = scaledSize / 2 + 1
	}

	override fun onTouch(src: View, event: MotionEvent): Boolean {
		if (src !is AndButton) return false
		synchronized(surfaceHolder!!) {
			return when (event.action) {
				MotionEvent.ACTION_DOWN -> {
					vm.buttonPressed(Button.get(src))
					true
				}
				MotionEvent.ACTION_UP -> {
					vm.buttonReleased(Button.get(src))
					true
				}
				else -> false
			}
		}
	}

	/* Callback invoked when the surface dimensions change. */
	override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
		thread.setSurfaceSize(width, height)
		if (!thread.isAlive) {
			thread.setRunning(true)
			thread.start()
		}
	}

	/* Callback invoked when the Surface has been created and is ready to be used. */
	override fun surfaceCreated(holder: SurfaceHolder) {
		if (thread.state == Thread.State.TERMINATED) thread = LanderThread()
	}

	/*
	 * Callback invoked when the Surface has been destroyed and must no longer
	 * be touched. WARNING: after this method returns, the Surface/Canvas must
	 * never be touched again!
	 */
	override fun surfaceDestroyed(holder: SurfaceHolder) {
		// we have to tell thread to shut down & wait for it to finish, or else
		// it might touch the Surface after we return and explode
		var retry = true
		thread.setRunning(false)
		while (retry) {
			try {
				thread.join()
				retry = false
			} catch (e: InterruptedException) {
				e.printStackTrace()
			}
		}
	}

	inner class LanderThread : Thread() {
		/** Indicate whether the surface has been created and is ready to draw */
		private var run = false

		override fun run() {
			while (run) {
				var c: Canvas? = null
				try {
					c = surfaceHolder.lockCanvas(null)
					synchronized(surfaceHolder) {
						val now = System.currentTimeMillis()
						if (now - vm.lastUpdate >= VM.UPDATE_TIME) {
							vm.updateLander()
							vm.lastUpdate = now
						}
						if (run) {
							val drawSurface = DrawSurface(c)
							// Background
							drawSurface.fillSurface(Color.BLACK)
							// Draw the ground
							drawSurface.drawPath(vm.path, Color.WHITE)
							vm.drawLander(drawSurface)
						}
					}
				} finally {
					// do this in a finally so that if an exception is thrown
					// during the above, we don't leave the Surface in an
					// inconsistent state
					if (c != null) surfaceHolder.unlockCanvasAndPost(c)
				}
			}
		}

		/**
		 * Used to signal the thread whether it should be running or not.
		 * Passing true allows the thread to run; passing false will shut it
		 * down if it's already running. Calling start() after this was most
		 * recently called with false will result in an immediate shutdown.
		 *
		 * @param b
		 * true to run, false to shut down
		 */
		fun setRunning(b: Boolean) {
			run = b
		}

		/* Callback invoked when the surface dimensions change. */
		fun setSurfaceSize(width: Int, height: Int) {
			// synchronized to make sure these all change atomically
			synchronized(surfaceHolder!!) {
				vm.xClient = width
				vm.yClient = height
			}
		}
	}
}
