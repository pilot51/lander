plugins {
	kotlin("js")
}

kotlin {
	js(LEGACY) {
		browser()
		binaries.executable()
	}
}

dependencies {
	implementation(project(":common"))
	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.6.4")
}

tasks.register<Copy>("copyResources") {
	from("../java-app/src/main/resources/img")
	into("build/distributions/img")
}

tasks.named("processResources") {
	finalizedBy("copyResources")
}
