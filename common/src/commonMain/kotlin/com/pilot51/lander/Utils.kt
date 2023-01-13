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

import kotlinx.coroutines.delay

object Utils {
	private var lastFrameTime = 0L

	suspend fun capFrameRate(fps: Int) {
		val wait = 1000 / fps
		val diff = Platform.currentTimeMillis - lastFrameTime
		if (diff < wait) delay(wait - diff)
		lastFrameTime = Platform.currentTimeMillis
	}
}
