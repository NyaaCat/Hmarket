plugins {
    `java-library`
    `maven-publish`
    id("xyz.jpenilla.run-paper") version "3.0.2" // Adds runServer and runMojangMappedServer tasks for testing
}

// Paper coordinates for Minecraft/Pewpew 26.2, discovered from official metadata.
val paperApiName = "26.2.build.123-stable"
val mcApiVersion = "26.2"

group = "cat.nyaa"
version = "0.10.3"

tasks.runServer {
    minecraftVersion(mcApiVersion)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://ci.nyaacat.com/maven/")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiName")
    // other nyaa plugins
    compileOnly("cat.nyaa:nyaacore:9.12")
    compileOnly("cat.nyaa:ecore:0.3.6")
    // for debug usage
    // compileOnly(files("lib/nyaacore-9.10.jar"))
    // compileOnly(files("lib/ecore-0.3.5.jar"))
    compileOnly("cat.nyaa:ukit:1.7.5")
    compileOnly("org.jetbrains:annotations:26.1.0")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.4.0-SNAPSHOT")
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
        options.release.set(25)
    }

    processResources {
        filesMatching("**/plugin.yml") {
            expand("version" to project.version, "apiVersion" to mcApiVersion)
        }
    }

    javadoc {
        val javadocPath = System.getenv("JAVADOCS_DIR")
        if (javadocPath != null) setDestinationDir(file("${javadocPath}/${rootProject.name.lowercase()}-${project.version}"))

        (options as StandardJavadocDocletOptions).apply {
            // ci.md-5.net/job/BungeeCord/... and guava 21.0 no longer serve an element-list,
            // which makes the javadoc task fail outright. Point at the live locations instead.
            links("https://docs.oracle.com/en/java/javase/25/docs/api/")
            links("https://jd.papermc.io/paper/26.2/")
            links("https://guava.dev/releases/33.6.0-jre/api/docs/")

            locale = "en_US"
            encoding = "UTF-8"
            addBooleanOption("keywords", true)
            addStringOption("Xdoclint:none", "-quiet")
            addBooleanOption("html5", true)
            windowTitle = "${rootProject.name} Javadoc"
        }
    }
}


