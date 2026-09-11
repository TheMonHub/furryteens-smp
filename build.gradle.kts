import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
	id("net.fabricmc.fabric-loom")
	`maven-publish`
	id("org.jetbrains.kotlin.jvm") version "2.4.0"
}

version = providers.gradleProperty("mod_version").get()
group = providers.gradleProperty("maven_group").get()

repositories {
	// Add repositories to retrieve artifacts from in here.
	// You should only use this when depending on other mods because
	// Loom adds the essential maven repositories to download Minecraft and libraries from automatically.
	// See https://docs.gradle.org/current/userguide/declaring_repositories.html
	// for more information about repositories.

	maven {
		name = "Terraformers"
		url = uri("https://maven.terraformersmc.com/")
	}
}

val minecraftVersion = property("minecraft_version") as String
val loaderVersion = providers.gradleProperty("loader_version").get()
val javaVersion = (property("java_version") as String).toInt()

assert(javaVersion >= 8) { "Java version must be at least 8!" }

val javaCompatMixin: String
	get() {
		return "JAVA_${javaVersion}"
	}

val jvmTargetVersion: JvmTarget
    get() {
		return JvmTarget.entries[javaVersion - 8]
    }

val javaCompatVersion: JavaVersion
	get() {
		return JavaVersion.entries[javaVersion]
	}

dependencies {
	// To change the versions see the gradle.properties file
	minecraft("com.mojang:minecraft:${minecraftVersion}")
	implementation("net.fabricmc:fabric-loader:${loaderVersion}")

	implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_api_version") as String}")
    implementation("net.fabricmc:fabric-language-kotlin:${providers.gradleProperty("fabric_kotlin_version").get()}")

	runtimeOnly("com.ptsmods:devlogin:3.5.1:fabric")
	runtimeOnly("com.terraformersmc:modmenu:${property("modmenu_version") as String}")
}

tasks.processResources {
	inputs.property("version", version)
	inputs.property("minecraft_version", minecraftVersion)
	inputs.property("loader_version", loaderVersion)
	inputs.property("java_version", javaVersion)

	inputs.property("java_compat_version", javaCompatVersion)

	filesMatching("fabric.mod.json") {
		expand(
			"version" to version,
			"minecraft_version" to minecraftVersion,
			"loader_version" to loaderVersion,
			"java_version" to javaVersion
		)
	}

	filesMatching("modid.mixins.json") {
		expand(
			"java_compat_version" to javaCompatVersion
		)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.release = javaVersion
}

kotlin {
	compilerOptions {
		jvmTarget = jvmTargetVersion
	}
}

java {
	withSourcesJar()

	sourceCompatibility = javaCompatVersion
	targetCompatibility = javaCompatVersion
}

tasks.jar {
	val projectName = project.name
	inputs.property("projectName", projectName)

	from("LICENSE") {
		rename { "${it}_$projectName" }
	}
}

// configure the maven publication
publishing {
	publications {
		register<MavenPublication>("mavenJava") {
			from(components["java"])
		}
	}

	// See https://docs.gradle.org/current/userguide/publishing_maven.html for information on how to set up publishing.
	repositories {

	}
}
