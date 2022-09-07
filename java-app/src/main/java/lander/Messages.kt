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

import java.util.ResourceBundle
import java.util.MissingResourceException

object Messages {
	private const val BUNDLE_NAME = "messages"
	private val RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME)

	fun getString(key: String): String = try {
		RESOURCE_BUNDLE.getString(key)
	} catch (e: MissingResourceException) {
		"!$key!"
	}
}
