import org.gradle.kotlin.dsl.runServer

plugins {
    `java-library`
    `maven-publish`
    id("xyz.jpenilla.run-paper") version "2.3.0" // Adds runServer and runMojangMappedServer tasks for testing
}

val paperApiName = "1.21.1-R0.1-SNAPSHOT"

group = "cat.nyaa"
version = "0.10.1"

tasks.runServer {
    minecraftVersion("1.21.1")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://ci.nyaacat.com/maven/")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiName")
    // other nyaa plugins
    compileOnly("cat.nyaa:nyaacore:9.4")
    compileOnly("cat.nyaa:ecore:0.3.4")
    compileOnly("cat.nyaa:ukit:1.7")
    compileOnly("org.jetbrains:annotations:23.0.0")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = group.toString()
            artifactId = rootProject.name.lowercase()
            version = project.version.toString()
        }
    }
    repositories {
        maven {
            name = "NyaaCatCILocal"
            //local maven repository
            url = uri("file://${System.getenv("MAVEN_DIR")}")
        }
    }
}

tasks {
    compileJava {
        options.compilerArgs.add("-Xlint:deprecation")
        options.encoding = "UTF-8"
    }

    processResources {
        filesMatching("**/plugin.yml") {
            expand("version" to project.version)
        }
    }

    javadoc {
        val javadocPath = System.getenv("JAVADOCS_DIR")
        if (javadocPath != null) setDestinationDir(file("${javadocPath}/${rootProject.name.lowercase()}-${project.version}"))

        (options as StandardJavadocDocletOptions).apply {
            links("https://docs.oracle.com/en/java/javase/17/docs/api/")
            links("https://hub.spigotmc.org/javadocs/spigot/")
            links("https://guava.dev/releases/21.0/api/docs/")
            links("https://ci.md-5.net/job/BungeeCord/ws/chat/target/apidocs/")

            locale = "en_US"
            encoding = "UTF-8"
            addBooleanOption("keywords", true)
            addStringOption("Xdoclint:none", "-quiet")
            addBooleanOption("html5", true)
            windowTitle = "${rootProject.name} Javadoc"
        }
    }
}


