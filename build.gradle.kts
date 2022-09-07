plugins {
	kotlin("jvm") version "1.7.10"
}

buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.10")
	}
}

repositories {
	mavenCentral()
}

tasks.compileKotlin {
	kotlinOptions.jvmTarget = "1.8"
}
