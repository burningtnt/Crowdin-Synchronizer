import java.util.Objects

plugins {
    id("java")
    id("checkstyle")
}

group = "net.burningtnt"
version = "1.0-SNAPSHOT"

checkstyle {
    sourceSets = mutableSetOf()
}

repositories {
    mavenCentral()
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.getByName("build") {
    dependsOn(tasks.getByName("checkstyleMain") {
        group = "build"
    })
    dependsOn(tasks.getByName("checkstyleTest") {
        group = "build"
    })
}

tasks.create("sync", JavaExec::class) {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.burningtnt.hmclcrowdinsynchronizer.Main")
    this.jvmArgs = listOf(
        "-Dhmcl.cs.crowdinToken=${Objects.requireNonNullElse(System.getenv("HMCL_CROWDIN_TOKEN"), "")}",
        "-Dhmcl.cs.githubToken=${Objects.requireNonNullElse(System.getenv("HMCL_GITHUB_TOKEN"), "")}"
    )
    this.workingDir = rootProject.rootDir
}

dependencies {
    implementation(rootProject)

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
}