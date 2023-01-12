plugins {
	id("com.android.application")
	kotlin("android")
	id("kotlin-kapt")
}

android {
	namespace = "com.pilot51.lander"
	compileSdk = 33
	defaultConfig {
		applicationId = "com.pilot51.lander"
		minSdk = 14
		targetSdk = 33
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
	implementation(project(":common"))
	implementation("androidx.preference:preference-ktx:1.2.0")
	implementation("androidx.room:room-ktx:2.5.0")
	kapt("androidx.room:room-compiler:2.5.0")
}
