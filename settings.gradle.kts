pluginManagement {
	repositories {
		maven {
			name = "Fabric"
			url = uri("https://maven.fabricmc.net/")
		}
		maven {
			name = "KikuGie Snapshots"
			url = uri("https://maven.kikugie.dev/snapshots")
		}
		mavenCentral()
		gradlePluginPortal()
	}

	plugins {
		id("net.fabricmc.fabric-loom") version providers.gradleProperty("loom_version")
	}
}

plugins {
	id("dev.kikugie.stonecutter") version "0.9.6"
}

stonecutter {
	// The root project is the Tree
	create(rootProject) {
		versions("26.2", "26.1")
		vcsVersion = "26.2"
	}
}

// Should match your modid
rootProject.name = "modid"
