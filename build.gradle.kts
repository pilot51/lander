buildscript {
	repositories {
		mavenCentral()
		google()
	}
	dependencies {
		classpath("com.android.tools.build:gradle:7.2.2")
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.0")
	}
}

allprojects {
	repositories {
		mavenCentral()
		google()
	}
}
