/*
 * This file was generated by the Gradle 'init' task.
 */

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.regex.Pattern
import java.nio.file.Path as JPath

plugins {
    id("io.github.seggan.myxal.kotlin-application-conventions")

    id("com.github.johnrengelman.shadow").version("7.1.2")
    id("antlr")
}

dependencies {
    implementation(project(":compiler"))

    implementation("com.guardsquare:proguard-base:7.2.1")

    antlr("org.antlr:antlr4:4.10.1")
}

val dest = JPath.of("$buildDir/runtimeClasses")

application {
    // Define the main class for the application.
    mainClass.set("io.github.seggan.myxal.app.Main")
}

tasks.generateGrammarSource {
    maxHeapSize = "128m"
    val path = JPath.of("$buildDir/generated-src/")
    val fullPath = path.resolve("antlr/main/io/github/seggan/myxal/antlr/")
    doFirst {
        Files.createDirectories(fullPath)
    }
    arguments = arguments + listOf(
        "-lib", fullPath.toAbsolutePath().toString(),
        "-visitor",
        "-no-listener",
        "-encoding", "UTF-8",
        "-package", "io.github.seggan.myxal.antlr"
    )
    outputDirectory = fullPath.toFile()
}

java.sourceSets["main"].java {
    srcDir("$buildDir/generated-src/antlr/main/")
}

tasks.compileKotlin {
    dependsOn("generateGrammarSource")
}

tasks.register("copyRuntime") {
    dependsOn(":runtime:extractLibs", "compileKotlin")
    doLast {
        val src = JPath.of("${project(":runtime").buildDir}/runtimeLibs")
        val list = src.resolve("runtime.list")
        val names = HashSet(Files.readAllLines(list))
        val regex = Pattern.compile(".+/classes/(java|kotlin)/main/")
        Files.walk(JPath.of("${project(":runtime").buildDir}/classes"))
            .filter(Files::isRegularFile)
            .filter { it.toString().endsWith(".class") }
            .forEach {
                val rel = regex.matcher(it.toString().replace("\\", "/")).replaceAll("")
                val d = dest.resolve(rel)
                Files.createDirectories(d.parent)
                Files.copy(it, d, StandardCopyOption.REPLACE_EXISTING)
                names.add(rel)
            }
        names.add("dictLong.txt")
        names.add("dictShort.txt")
        val out = dest.resolve("runtime.list")
        if (!Files.exists(out)) {
            Files.createDirectories(out.parent)
        }
        Files.write(out, names)
    }
}

tasks.test {
    dependsOn("copyRuntime")
}

tasks.shadowJar {
    dependsOn("test")

    from(dest.toFile().path) {
        include("**/*.*")
    }

    for (f in sourceSets["main"].resources.srcDirs) {
        from(f) {
            include("**/*.*")
        }
    }

    from(rootDir) {
        include("LICENSE")
    }

    archiveFileName.set("Myxal.jar")
}
