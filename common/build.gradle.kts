plugins {
	kotlin("multiplatform")
	id("com.android.library")
}

kotlin {
	android()
	jvm()

	sourceSets {
		val commonMain by getting
		val androidMain by getting {
			dependencies {
				implementation("androidx.core:core-ktx:1.9.0")
				implementation("androidx.preference:preference-ktx:1.2.0")
				implementation("androidx.room:room-common:2.5.0")
				implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.4")
			}
		}
		val jvmMain by getting
	}
}

java {
	toolchain {
		languageVersion.set(JavaLanguageVersion.of(8))
	}
}

android {
	sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
	compileSdk = 33
	defaultConfig {
		minSdk = 14
		targetSdk = 33
	}
}
