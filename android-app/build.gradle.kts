plugins {
	id("com.android.application")
	kotlin("android")
	id("kotlin-kapt")
}

android {
	namespace = "com.pilot51.lander"
	compileSdk = 31
	defaultConfig {
		applicationId = "com.pilot51.lander"
		minSdk = 14
		targetSdk = 31
		versionCode = 5
		versionName = "1.1.2"
		javaCompileOptions {
			annotationProcessorOptions {
				arguments += mapOf(
					"room.incremental" to "true",
					"room.schemaLocation" to "$projectDir/schemas"
				)
			}
		}
	}
	buildTypes {
		getByName("release") {
			isMinifyEnabled = false
		}
	}
}
dependencies {
	implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
	implementation("androidx.preference:preference-ktx:1.2.0")
	implementation("androidx.room:room-ktx:2.4.3")
	kapt("androidx.room:room-compiler:2.4.3")
}
